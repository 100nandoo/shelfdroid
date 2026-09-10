package dev.halim.shelfdroid.media.service

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import dagger.Lazy
import dev.halim.shelfdroid.core.MediaStructure
import dev.halim.shelfdroid.core.PlayPauseControlStateHolder
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.SeekControlsState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.playback.nextPlaybackSpeed
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.PlayerPrefs
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.exoplayer.PlayerEventListener
import dev.halim.shelfdroid.media.exoplayer.playbackProgressFlow
import dev.halim.shelfdroid.media.mediaitem.MediaItemMapper
import dev.halim.shelfdroid.media.misc.SessionManager
import dev.halim.shelfdroid.media.misc.TimerManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
@Singleton
class PlayerStore
@Inject
constructor(
  private val playerEventListener: Lazy<PlayerEventListener>,
  private val playerManager: ExoPlayerManager,
  private val playPauseControlStateHolder: PlayPauseControlStateHolder,
  private val playPauseControlStateMapper: PlayPauseControlStateMapper,
  private val playerRepository: PlayerRepository,
  private val mediaItemMapper: MediaItemMapper,
  private val timerManager: TimerManager,
  private val sessionManager: SessionManager,
  private val state: PlayerInternalStateHolder,
  private val prefsRepository: PrefsRepository,
) {
  val uiState = MutableStateFlow(PlayerUiState())
  val isChapterTransitioning = MutableStateFlow(false)
  val notificationPrefs = MutableStateFlow(NotificationPrefs())
  val playerPrefs = MutableStateFlow(PlayerPrefs())
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  init {
    syncScope.launch {
      prefsRepository.playerPrefs.collect { prefs ->
        playerPrefs.value = prefs
        uiState.update {
          it.copy(
            chapterTitleLine = prefs.chapterTitleLine,
            chapterTimeDisplay = prefs.chapterTimeDisplay,
          )
        }
      }
    }
    syncScope.launch(Dispatchers.Main) {
      playerPrefs.collect { prefs ->
        playerManager.player.get().apply {
          setSeekBackIncrementMs(prefs.seekBackSeconds * 1000L)
          setSeekForwardIncrementMs(prefs.seekForwardSeconds * 1000L)
        }
      }
    }
    syncScope.launch { prefsRepository.notificationPrefs.collect { notificationPrefs.value = it } }
  }

  fun playContent(shouldPlay: Boolean = true) {
    playerManager.player.get().apply {
      changeContent()
      if (shouldPlay) {
        play()
      } else {
        pause()
      }
      collectPlaybackProgress()
      listenPlayer()
      syncSession()
    }
  }

  fun nextChapterFromMediaNotification(): Boolean {
    val currentUiState = uiState.value
    if (isChapterTransitioning.value || !nextChapterControlState(currentUiState).enabled) {
      return false
    }

    isChapterTransitioning.value = true
    return try {
      val shouldPlay = playerManager.player.get().playWhenReady
      uiState.value = playerRepository.previousNextChapter(currentUiState, false)
      playContent(shouldPlay)
      true
    } catch (exception: RuntimeException) {
      isChapterTransitioning.value = false
      throw exception
    }
  }

  fun completeChapterTransition() {
    isChapterTransitioning.value = false
  }

  fun changeContent() {
    playerManager.player.get().apply {
      val multiTrackWithChapters = state.mediaStructure() == MediaStructure.MultiTrackWithChapters
      if (multiTrackWithChapters) {
        val mediaItems = mediaItemMapper.toMediaItemList(uiState.value)
        val positionMs = uiState.value.currentTime.toLong() * 1000
        setMediaItems(mediaItems, 0, positionMs)
      } else {
        val mediaItem = mediaItemMapper.toMediaItem(uiState.value, state)
        val positionMs = uiState.value.currentTime.toLong() * 1000
        setMediaItem(mediaItem, positionMs)
      }
      setPlaybackSpeed(uiState.value.advancedControl.speed)
      if (uiState.value.advancedControl.sleepTimerLeft > Duration.ZERO) {
        sleepTimer(uiState.value.advancedControl.sleepTimerLeft)
      } else {
        clearTimer()
      }
      prepare()
    }
  }

  fun sleepTimer(duration: Duration) {
    uiState.update {
      val advancedControl = uiState.value.advancedControl.copy(sleepTimerLeft = duration)
      it.copy(advancedControl = advancedControl)
    }
    timerManager.start(duration, { playerManager.player.get().pause() })
    collectSleepTimer()
  }

  fun clearTimer() {
    timerManager.clear()
  }

  fun startDefaultSleepTimer() {
    val minutes = runBlocking { prefsRepository.notificationPrefs.first().sleepTimerMinutes }
    sleepTimer(minutes.minutes)
  }

  fun changeSpeedFromMediaNotification(): Boolean {
    val speed =
      nextPlaybackSpeed(
        uiState.value.advancedControl.speed,
        notificationPrefs.value.playbackSpeedCycle,
      )
    uiState.update { playerRepository.changeSpeed(it, speed) }
    playerManager.player.get().setPlaybackSpeed(speed)
    return true
  }

  fun emptyState(): PlayerUiState {
    isChapterTransitioning.value = false
    playPauseControlStateHolder.update(playPauseControlStateMapper.map(emptyControlSnapshot()))
    return PlayerUiState()
  }

  fun syncSession() {
    sessionManager.start(uiState.value)
  }

  private var playbackProgressJob: Job? = null
  private var listenPlayer: Job? = null
  private var sleepTimerJob: Job? = null

  private fun listenPlayer() {
    listenPlayer?.cancel()
    listenPlayer =
      playerEventListener
        .get()
        .listen(
          uiState.value,
          changeChapterCallback(),
          { events ->
            handleSeekSliderState(events)
            handleSeekBackState(events)
            handleSeekForwardState(events)
            handlePlayPauseState(events)
          },
        )
    syncControlStates()
  }

  private fun handleSeekSliderState(events: Player.Events) {
    with(playerManager.player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val seekControls =
          uiState.value.seekControls.copy(
            seekSliderEnabled = isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
          )
        uiState.update { it.copy(seekControls = seekControls) }
      }
    }
  }

  private fun handleSeekBackState(events: Player.Events) {
    with(playerManager.player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val seekControls =
          uiState.value.seekControls.copy(
            seekBackEnabled = isCommandAvailable(Player.COMMAND_SEEK_BACK)
          )
        uiState.update { it.copy(seekControls = seekControls) }
      }
    }
  }

  private fun handleSeekForwardState(events: Player.Events) {
    with(playerManager.player.get()) {
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        val seekControls =
          uiState.value.seekControls.copy(
            seekForwardEnabled = isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
          )
        uiState.update { it.copy(seekControls = seekControls) }
      }
    }
  }

  @OptIn(UnstableApi::class)
  private fun handlePlayPauseState(events: Player.Events) {
    with(playerManager.player.get()) {
      if (
        events.containsAny(
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          Player.EVENT_PLAY_WHEN_READY_CHANGED,
          Player.EVENT_IS_LOADING_CHANGED,
          Player.EVENT_IS_PLAYING_CHANGED,
          Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
        )
      ) {
        val playPause =
          playPauseControlStateMapper.map(
            PlayerControlSnapshot(
              isPlaying = isPlaying,
              playWhenReady = playWhenReady,
              isLoading = isLoading,
              playbackState = playbackState,
              playPauseEnabled = Util.shouldEnablePlayPauseButton(this),
              showPlayIcon = Util.shouldShowPlayButton(this),
            )
          )
        playPauseControlStateHolder.update(playPause)
        uiState.update { it.copy(playPause = playPause) }
      }
    }
  }

  private fun syncControlStates() {
    with(playerManager.player.get()) {
      val playPause =
        playPauseControlStateMapper.map(
          PlayerControlSnapshot(
            isPlaying = isPlaying,
            playWhenReady = playWhenReady,
            isLoading = isLoading,
            playbackState = playbackState,
            playPauseEnabled = Util.shouldEnablePlayPauseButton(this),
            showPlayIcon = Util.shouldShowPlayButton(this),
          )
        )
      val seekControls =
        SeekControlsState(
          seekBackEnabled = isCommandAvailable(Player.COMMAND_SEEK_BACK),
          seekForwardEnabled = isCommandAvailable(Player.COMMAND_SEEK_FORWARD),
          seekSliderEnabled = isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
        )
      playPauseControlStateHolder.update(playPause)
      uiState.update { it.copy(playPause = playPause, seekControls = seekControls) }
    }
  }

  private fun emptyControlSnapshot() =
    PlayerControlSnapshot(
      isPlaying = false,
      playWhenReady = false,
      isLoading = false,
      playbackState = Player.STATE_IDLE,
      playPauseEnabled = false,
      showPlayIcon = true,
    )

  private fun changeChapterCallback() = {
    uiState.update { playerRepository.previousNextChapter(uiState.value, false) }
    playContent()
  }

  private fun collectPlaybackProgress() {
    playbackProgressJob?.cancel()
    playbackProgressJob =
      CoroutineScope(Dispatchers.Main).launch {
        playerManager.player.get().playbackProgressFlow().collect { raw ->
          uiState.update { playerRepository.toPlayback(uiState.value, raw) }
        }
      }
  }

  private fun collectSleepTimer() {
    sleepTimerJob?.cancel()
    sleepTimerJob =
      CoroutineScope(Dispatchers.Default).launch {
        timerManager.duration.collect { currentDuration ->
          uiState.update {
            val updatedAdvancedControl = it.advancedControl.copy(sleepTimerLeft = currentDuration)
            it.copy(advancedControl = updatedAdvancedControl)
          }
        }
      }
  }
}
