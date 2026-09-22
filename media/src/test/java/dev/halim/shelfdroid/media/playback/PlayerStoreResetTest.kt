package dev.halim.shelfdroid.media.playback

import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.shelfdroid.core.PlayPauseControlStateHolder
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.PlayerPrefs
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.exoplayer.PlayerEventListener
import dev.halim.shelfdroid.media.mediaitem.MediaItemMapper
import dev.halim.shelfdroid.media.misc.SessionManager
import dev.halim.shelfdroid.media.misc.TimerManager
import dev.halim.shelfdroid.media.playback.controls.PlayPauseControlStateMapper
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerStoreResetTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun emptyStateStopsSleepTimerUpdates() =
    runTest(dispatcher) {
      val timerManager = mock(TimerManager::class.java)
      val timerDuration = MutableStateFlow(30.seconds)
      `when`(timerManager.duration).thenReturn(timerDuration)
      val playerManager = mock(ExoPlayerManager::class.java)
      val player = mock(ExoPlayer::class.java)
      `when`(playerManager.player).thenReturn(Lazy { player })
      val store = createStore(timerManager, playerManager)
      store.isChapterTransitioning.value = true
      store.uiState.value = PlayerUiState(id = "book-id")
      store.sleepTimer(30.seconds)
      runCurrent()
      assertEquals(30.seconds, store.uiState.value.advancedControl.sleepTimerLeft)

      val empty = store.emptyState()
      store.uiState.value = empty
      timerDuration.value = 20.seconds
      runCurrent()

      verify(timerManager).clear()
      assertEquals(false, store.isChapterTransitioning.value)
      assertEquals("", empty.id)
      assertEquals(0.seconds, store.uiState.value.advancedControl.sleepTimerLeft)
    }

  @Test
  fun emptyStateStopsPlaybackProgressAndPlayerListener() =
    runTest(dispatcher) {
      val timerManager = mock(TimerManager::class.java)
      `when`(timerManager.duration).thenReturn(MutableStateFlow(0.seconds))
      val player = mock(ExoPlayer::class.java)
      val playerManager = mock(ExoPlayerManager::class.java)
      `when`(playerManager.player).thenReturn(Lazy { player })
      val eventListener = mock(PlayerEventListener::class.java)
      val listenerJob = Job()
      doReturn(listenerJob).`when`(eventListener).listen(anyMock(), anyMock(), anyMock())
      val store = createStore(timerManager, playerManager, eventListener)
      store.uiState.value = PlayerUiState(id = "book-id")

      store.playContent()
      runCurrent()
      assertEquals(false, listenerJob.isCancelled)
      verify(player).addListener(anyMock())

      store.emptyState()
      runCurrent()

      assertEquals(true, listenerJob.isCancelled)
      verify(player).removeListener(anyMock())
    }

  private fun createStore(
    timerManager: TimerManager,
    playerManager: ExoPlayerManager,
    eventListener: PlayerEventListener = mock(PlayerEventListener::class.java),
  ): PlayerStore {
    val prefsRepository = mock(PrefsRepository::class.java)
    `when`(prefsRepository.playerPrefs).thenReturn(MutableStateFlow(PlayerPrefs()))
    `when`(prefsRepository.notificationPrefs).thenReturn(MutableStateFlow(NotificationPrefs()))
    return PlayerStore(
      playerEventListener = Lazy { eventListener },
      playerManager = playerManager,
      playPauseControlStateHolder = mock(PlayPauseControlStateHolder::class.java),
      playPauseControlStateMapper = PlayPauseControlStateMapper(),
      playerRepository = mock(PlayerRepository::class.java),
      mediaItemMapper = mock(MediaItemMapper::class.java),
      timerManager = timerManager,
      sessionManager = mock(SessionManager::class.java),
      state = mock(PlayerInternalStateHolder::class.java),
      prefsRepository = prefsRepository,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> anyMock(): T = org.mockito.ArgumentMatchers.any<T>() as T
}
