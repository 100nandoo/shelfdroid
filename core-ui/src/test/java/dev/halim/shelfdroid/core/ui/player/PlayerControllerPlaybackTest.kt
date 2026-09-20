package dev.halim.shelfdroid.core.ui.player

import dagger.Lazy
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.player.PreparedPlayback
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.media.di.MediaControllerManager
import dev.halim.shelfdroid.media.playback.PlayerStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PlayerControllerPlaybackTest {

  @Test
  fun invalidChapterDoesNotStartPlayback() {
    val repository = mock(PlayerRepository::class.java)
    val store = mock(PlayerStore::class.java)
    val initial = PlayerUiState()
    val uiState = MutableStateFlow(initial)
    `when`(store.uiState).thenReturn(uiState)
    doReturn(PlayerUiState(state = PlayerState.Hidden(Error("Invalid chapter"))))
      .`when`(repository)
      .changeChapter(initial, -1)
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val controller =
      PlayerController(
        mediaControl = Lazy<MediaControllerManager> { error("Unused") },
        playerRepository = repository,
        playerStore = store,
        scope = scope,
        mainScope = scope,
      )

    controller.onEvent(PlayerEvent.ChangeChapter(-1))

    assertTrue(uiState.value.state is PlayerState.Hidden)
    verify(store, never()).playContent()
  }

  @Test
  fun failedBookPreparationDoesNotStartPlayback() = runBlocking {
    val repository = mock(PlayerRepository::class.java)
    val store = mock(PlayerStore::class.java)
    val mediaController = mock(MediaControllerManager::class.java)
    val initial = PlayerUiState()
    val uiState = MutableStateFlow(initial)
    val failure = IllegalStateException("Book not found")
    `when`(store.uiState).thenReturn(uiState)
    doReturn(Result.failure<PreparedPlayback>(failure))
      .`when`(repository)
      .prepareBookPlayback("missing", initial.advancedControl, ChangeBehaviour.Book)
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val controller =
      PlayerController(
        mediaControl = Lazy { mediaController },
        playerRepository = repository,
        playerStore = store,
        scope = scope,
        mainScope = scope,
      )

    controller.onEvent(PlayerEvent.PlayBook("missing"))

    assertTrue((uiState.value.state as PlayerState.Hidden).error === failure)
    verify(store, never()).playContent()
    verify(mediaController).clearAndStop()
  }

  @Test
  fun failedPodcastPreparationDoesNotStartPlayback() = runBlocking {
    val repository = mock(PlayerRepository::class.java)
    val store = mock(PlayerStore::class.java)
    val mediaController = mock(MediaControllerManager::class.java)
    val initial = PlayerUiState()
    val uiState = MutableStateFlow(initial)
    val failure = IllegalStateException("Episode not found")
    `when`(store.uiState).thenReturn(uiState)
    doReturn(Result.failure<PreparedPlayback>(failure))
      .`when`(repository)
      .preparePodcastPlayback("podcast", "episode", initial.advancedControl, ChangeBehaviour.Type)
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val controller =
      PlayerController(
        mediaControl = Lazy { mediaController },
        playerRepository = repository,
        playerStore = store,
        scope = scope,
        mainScope = scope,
      )

    controller.onEvent(PlayerEvent.PlayPodcast("podcast", "episode"))

    assertTrue((uiState.value.state as PlayerState.Hidden).error === failure)
    verify(store, never()).playContent()
    verify(mediaController).clearAndStop()
  }

  @Test
  fun olderBookPreparationCannotReplaceNewerPlayback() = runBlocking {
    val repository = mock(PlayerRepository::class.java)
    val store = mock(PlayerStore::class.java)
    val initial = PlayerUiState()
    val uiState = MutableStateFlow(initial)
    val slow = PreparedPlayback(PlayerUiState(id = "slow"), "slow-session")
    val fast = PreparedPlayback(PlayerUiState(id = "fast"), "fast-session")
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    `when`(store.uiState).thenReturn(uiState)
    doAnswer {
        started.countDown()
        check(release.await(5, TimeUnit.SECONDS))
        Result.success(slow)
      }
      .`when`(repository)
      .prepareBookPlayback("slow", initial.advancedControl, ChangeBehaviour.Book)
    doReturn(Result.success(fast))
      .`when`(repository)
      .prepareBookPlayback("fast", initial.advancedControl, ChangeBehaviour.Book)
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val controller =
      PlayerController(
        mediaControl = Lazy<MediaControllerManager> { error("Unused") },
        playerRepository = repository,
        playerStore = store,
        scope = scope,
        mainScope = scope,
      )

    val slowRequest = Thread { controller.onEvent(PlayerEvent.PlayBook("slow")) }
    slowRequest.start()
    assertTrue(started.await(5, TimeUnit.SECONDS))
    controller.onEvent(PlayerEvent.PlayBook("fast"))
    release.countDown()
    slowRequest.join(5_000)

    assertFalse(slowRequest.isAlive)
    assertEquals("fast", uiState.value.id)
    verify(repository).activatePlayback(fast)
    verify(repository, never()).activatePlayback(slow)
  }
}
