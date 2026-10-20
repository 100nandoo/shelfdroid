package dev.halim.shelfdroid.media.playback

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayPauseControlStateHolder
import dev.halim.shelfdroid.core.PlayerChapter
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
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerStoreWidgetChapterTest {
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
  fun `next podcast chapter seeks directly and leaves playback state alone`() =
    runTest(dispatcher) {
      val player = mock(ExoPlayer::class.java)
      `when`(player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)).thenReturn(true)
      val store = createStore(player)
      val chapters = podcastChapters()
      store.uiState.value =
        PlayerUiState(
          id = "podcast",
          episodeId = "episode",
          currentTime = 10.0,
          episodeChapters = chapters,
          currentEpisodeChapter = chapters.first(),
        )

      assertTrue(store.nextChapterFromWidget())

      verify(player).seekTo(20_000L)
      verify(player, never()).play()
      verify(player, never()).pause()
      assertEquals("Middle", store.uiState.value.currentEpisodeChapter?.title)
      assertEquals(20.0, store.uiState.value.currentTime, 0.0)
    }

  @Test
  fun `podcast chapter navigation respects the first chapter boundary`() =
    runTest(dispatcher) {
      val player = mock(ExoPlayer::class.java)
      val store = createStore(player)
      val chapters = podcastChapters()
      store.uiState.value =
        PlayerUiState(
          id = "podcast",
          episodeId = "episode",
          episodeChapters = chapters,
          currentEpisodeChapter = chapters.first(),
        )

      assertFalse(store.previousChapterFromWidget())

      verify(player, never()).seekTo(org.mockito.ArgumentMatchers.anyLong())
    }

  private fun createStore(player: ExoPlayer): PlayerStore {
    val playerManager = mock(ExoPlayerManager::class.java)
    `when`(playerManager.player).thenReturn(Lazy { player })
    val timerManager = mock(TimerManager::class.java)
    `when`(timerManager.duration).thenReturn(MutableStateFlow(Duration.ZERO))
    val prefsRepository = mock(PrefsRepository::class.java)
    `when`(prefsRepository.playerPrefs).thenReturn(MutableStateFlow(PlayerPrefs()))
    `when`(prefsRepository.notificationPrefs).thenReturn(MutableStateFlow(NotificationPrefs()))

    return PlayerStore(
      playerEventListener = Lazy { mock(PlayerEventListener::class.java) },
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

  private fun podcastChapters() =
    listOf(
      PlayerChapter(
        id = 1,
        title = "First",
        startTimeSeconds = 0.0,
        chapterPosition = ChapterPosition.First,
      ),
      PlayerChapter(
        id = 2,
        title = "Middle",
        startTimeSeconds = 20.0,
        chapterPosition = ChapterPosition.Middle,
      ),
      PlayerChapter(
        id = 3,
        title = "Last",
        startTimeSeconds = 40.0,
        chapterPosition = ChapterPosition.Last,
      ),
    )
}
