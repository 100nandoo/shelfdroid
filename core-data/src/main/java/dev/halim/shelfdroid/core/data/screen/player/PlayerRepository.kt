package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BookmarkRequest
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.AdvancedControl
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.RawPlaybackProgress
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class PlayerRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val helper: Helper,
  private val apiService: ApiService,
  private val mapper: PlayerMapper,
  private val finder: PlayerFinder,
  private val downloadRepo: DownloadRepo,
  private val state: PlayerInternalStateHolder,
  private val dataStoreManager: DataStoreManager,
) {

  suspend fun playBook(
    id: String,
    isDownloaded: Boolean,
    advancedControl: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState {
    val playerUiState = book(id, advancedControl, changeBehaviour)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    var sessionId: String
    val result =
      runCatching {
          if (isDownloaded) {
            sessionId = UUID.randomUUID().toString()
          } else {
            val request = mapper.toPlayRequest()
            val response = apiService.playBook(id, request)
            sessionId = response.id
          }

          state.changeMedia(playerUiState, sessionId)
          playerUiState
        }
        .getOrNull()

    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Book")))
  }

  suspend fun playPodcast(
    itemId: String,
    episodeId: String,
    isDownloaded: Boolean,
    advancedControl: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState {
    val playerUiState = podcast(itemId, episodeId, advancedControl, changeBehaviour)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    var sessionId: String
    val result =
      runCatching {
          if (isDownloaded) {
            sessionId = UUID.randomUUID().toString()
          } else {
            val request = mapper.toPlayRequest()
            val response = apiService.playPodcast(itemId, episodeId, request)
            sessionId = response.id
          }

          state.changeMedia(playerUiState, sessionId)
          playerUiState
        }
        .getOrNull()
    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Podcast Episode")))
  }

  suspend fun book(
    id: String,
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState {
    val result = libraryItemRepo.byId(id)
    val progress = progressRepo.bookById(id)
    val bookmarks = bookmarkRepo.byLibraryItemId(id)
    val playerBookmarks = bookmarks.map { mapper.toPlayerBookmark(it) }
    return if (result != null) {
      val media = Json.decodeFromString<Book>(result.media)
      val chapters =
        media.chapters.mapIndexed { i, bookChapter ->
          mapper.toPlayerChapter(i, bookChapter, media.chapters.size)
        }
      val isSingleTrack = media.audioTracks.size == 1
      val playerTracks = media.audioTracks.map { mapper.toPlayerTrack(it) }
      val currentTrack = finder.trackFromChapter(playerTracks, progress?.currentTime ?: 0.0)
      val currentChapter = finder.playerChapter(chapters, progress?.currentTime ?: 0.0)
      val currentTime =
        if (currentChapter != null) (progress?.currentTime ?: 0.0) - currentChapter.startTimeSeconds
        else progress?.currentTime ?: 0.0

      val downloadState =
        if (isSingleTrack) {
          val url = media.audioTracks.first().contentUrl
          downloadRepo
            .item(itemId = id, url = url, title = currentChapter?.title ?: result.title)
            .state
        } else {
          downloadRepo
            .multipleTrackItem(
              itemId = id,
              title = currentChapter?.title ?: result.title,
              tracks = media.audioTracks,
            )
            .state
        }

      val advancedControl = decideAdvanceControl(existing, changeBehaviour)

      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        author = result.author,
        title = currentChapter?.title ?: result.title,
        cover = result.cover,
        currentTime = currentTime,
        playerChapters = chapters,
        currentChapter = currentChapter,
        playerTracks = playerTracks,
        currentTrack = currentTrack,
        playerBookmarks = playerBookmarks,
        downloadState = downloadState,
        advancedControl = advancedControl,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  suspend fun podcast(
    itemId: String,
    episodeId: String,
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState {
    val result = libraryItemRepo.byId(itemId)
    val progress = progressRepo.episodeById(episodeId)
    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)

      val episode =
        media.episodes.find { it.id == episodeId }
          ?: return PlayerUiState(state = PlayerState.Hidden(Error("Failed to find episode")))

      val downloadState =
        downloadRepo
          .item(
            itemId = itemId,
            episodeId = episodeId,
            url = episode.audioTrack.contentUrl,
            title = episode.title,
          )
          .state

      val playerTrack = episode.audioTrack.let { mapper.toPlayerTrack(it) }
      val advancedControl = decideAdvanceControl(existing, changeBehaviour)

      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        episodeId = episode.id,
        author = result.author,
        title = episode.title,
        cover = result.cover,
        playerTracks = listOf(playerTrack),
        currentTrack = playerTrack,
        currentTime = progress?.currentTime ?: 0.0,
        downloadState = downloadState,
        advancedControl = advancedControl,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  suspend fun decideAdvanceControl(
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): AdvancedControl {
    val playbackPrefs = dataStoreManager.playbackPrefs.first()
    return when (changeBehaviour) {
      ChangeBehaviour.Type -> {
        val speed = if (playbackPrefs.keepSpeed) existing.speed else 1f
        val sleepTimerLeft =
          if (playbackPrefs.keepSleepTimer) existing.sleepTimerLeft else Duration.ZERO
        existing.copy(speed = speed, sleepTimerLeft = sleepTimerLeft)
      }
      ChangeBehaviour.Episode -> {
        val speed = if (playbackPrefs.episodeKeepSpeed) existing.speed else 1f
        val sleepTimerLeft =
          if (playbackPrefs.episodeKeepSleepTimer) existing.sleepTimerLeft else Duration.ZERO
        existing.copy(speed = speed, sleepTimerLeft = sleepTimerLeft)
      }
      ChangeBehaviour.Book -> {
        val speed = if (playbackPrefs.bookKeepSpeed) existing.speed else 1f
        val sleepTimerLeft =
          if (playbackPrefs.bookKeepSleepTimer) existing.sleepTimerLeft else Duration.ZERO
        existing.copy(speed = speed, sleepTimerLeft = sleepTimerLeft)
      }
    }
  }

  fun changeChapter(uiState: PlayerUiState, target: Int): PlayerUiState {
    val chapters = uiState.playerChapters
    return if (target in chapters.indices) {
      val targetChapter = chapters[target]
      state.changeChapter(targetChapter)
      val targetTrack =
        finder.trackFromChapter(uiState.playerTracks, targetChapter.startTimeSeconds)

      uiState.copy(
        title = targetChapter.title,
        currentChapter = targetChapter,
        currentTrack = targetTrack,
        currentTime = 0.0,
      )
    } else {
      uiState.copy(state = PlayerState.Hidden(Error("Failed to change chapter")))
    }
  }

  fun previousNextChapter(uiState: PlayerUiState, isPrevious: Boolean = true): PlayerUiState {
    val direction = if (isPrevious) -1 else +1
    val label = if (isPrevious) "previous" else "next"
    val targetChapter = uiState.playerChapters.indexOf(uiState.currentChapter) + direction
    return if (targetChapter in uiState.playerChapters.indices) {
      changeChapter(uiState, targetChapter)
    } else {
      uiState.copy(state = PlayerState.Hidden(Error("No $label chapter available")))
    }
  }

  fun toPlayback(uiState: PlayerUiState, raw: RawPlaybackProgress): PlayerUiState {
    val playbackProgress = mapper.toPlaybackProgress(raw)

    return uiState.copy(playbackProgress = playbackProgress)
  }

  fun seekTo(uiState: PlayerUiState, target: Float): PlayerUiState {
    val durationMs = (state.duration() * 1000).toLong()
    val positionMs = (target * durationMs).toLong()

    val currentTime = (positionMs / 1000)
    val raw = RawPlaybackProgress(positionMs, 0)
    val playbackProgress = mapper.toPlaybackProgress(raw)
    return uiState.copy(playbackProgress = playbackProgress, currentTime = currentTime.toDouble())
  }

  fun changeSpeed(uiState: PlayerUiState, speed: Float): PlayerUiState {
    val advancedControl = uiState.advancedControl.copy(speed = speed)
    return uiState.copy(advancedControl = advancedControl)
  }

  suspend fun deleteBookmark(uiState: PlayerUiState, bookmark: PlayerBookmark): PlayerUiState {
    val id = uiState.id
    val result = apiService.deleteBookmark(uiState.id, bookmark.time.toInt()).getOrNull()
    if (result == null) return uiState
    bookmarkRepo.delete(id, bookmark.time)
    val bookmarks = uiState.playerBookmarks.toMutableList()
    bookmarks.remove(bookmark)
    return uiState.copy(playerBookmarks = bookmarks)
  }

  fun goToBookmark(uiState: PlayerUiState, time: Long): PlayerUiState {
    val targetChapter = finder.playerChapter(uiState.playerChapters, time.toDouble())
    return if (targetChapter != null) {
      val positionMs = (time - targetChapter.startTimeSeconds) * 1000
      val rawPlaybackProgress = RawPlaybackProgress(positionMs.toLong(), 0)
      val result = changeChapter(uiState, uiState.playerChapters.indexOf(targetChapter))
      val playbackProgress = mapper.toPlaybackProgress(rawPlaybackProgress)
      result.copy(
        currentTime = time - targetChapter.startTimeSeconds,
        playbackProgress = playbackProgress,
      )
    } else {
      val targetTrack = finder.trackFromCurrentTime(uiState, time.toDouble())
      val rawPlaybackProgress = RawPlaybackProgress(time * 1000, 0)
      val playbackProgress = mapper.toPlaybackProgress(rawPlaybackProgress)
      uiState.copy(
        currentTime = time.toDouble(),
        currentTrack = targetTrack,
        playbackProgress = playbackProgress,
      )
    }
  }

  suspend fun updateBookmark(
    uiState: PlayerUiState,
    bookmark: PlayerBookmark,
    title: String,
  ): PlayerUiState {
    val id = uiState.id
    val request = BookmarkRequest(bookmark.time, title)
    val result = apiService.updateBookmark(uiState.id, request).getOrNull()
    if (result == null) return uiState
    bookmarkRepo.updateTitle(id, bookmark.time, title)
    val bookmarks =
      uiState.playerBookmarks.map { if (it.time == bookmark.time) it.copy(title = title) else it }
    return uiState.copy(playerBookmarks = bookmarks)
  }

  fun newBookmarkTime(uiState: PlayerUiState, currentTime: Long): PlayerUiState {
    val newBookmarkTime = state.startOffset() + currentTime
    val readableTime = helper.formatChapterTime(newBookmarkTime)
    val bookmark = PlayerBookmark(time = newBookmarkTime.toLong(), readableTime = readableTime)
    return uiState.copy(newBookmarkTime = bookmark)
  }

  suspend fun createBookmark(uiState: PlayerUiState, time: Long, title: String): PlayerUiState {
    val request = BookmarkRequest(time, title)
    val result = apiService.createBookmark(uiState.id, request).getOrNull()
    if (result == null) return uiState
    val entity = bookmarkRepo.insertAndConvert(result)
    val playerBookmark = mapper.toPlayerBookmark(entity)
    val bookmarks = uiState.playerBookmarks.toMutableList()
    bookmarks.add(playerBookmark)
    return uiState.copy(playerBookmarks = bookmarks)
  }
}
