package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BookmarkRequest
import dev.halim.core.network.request.SyncSessionRequest
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.PlaybackProgress
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.RawPlaybackProgress
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import javax.inject.Inject
import kotlin.time.Duration
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
) {

  suspend fun playBook(id: String): PlayerUiState {
    val playerUiState = book(id)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    val result =
      runCatching {
          val request = mapper.toPlayRequest()
          val response = apiService.playBook(id, request)
          val sessionId = response.id
          val playerTracks = response.audioTracks.map { mapper.toPlayerTrack(it) }

          val currentTrack = findCurrentPlayerTrack(playerTracks, response.currentTime)
          val currentChapter = playerUiState.currentChapter
          val currentTime =
            if (currentChapter != null) response.currentTime - currentChapter.startTimeSeconds
            else response.currentTime
          playerUiState.copy(
            playerTracks = playerTracks,
            currentTrack = currentTrack,
            currentTime = currentTime,
            sessionId = sessionId,
          )
        }
        .getOrNull()

    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Book")))
  }

  suspend fun playPodcast(itemId: String, episodeId: String): PlayerUiState {
    val playerUiState = podcast(itemId, episodeId)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    val result =
      runCatching {
          val request = mapper.toPlayRequest()
          val response = apiService.playPodcast(itemId, episodeId, request)
          val sessionId = response.id
          val playerTracks = response.audioTracks.map { mapper.toPlayerTrack(it) }

          val currentTrack = findCurrentPlayerTrack(playerTracks, response.currentTime)
          playerUiState.copy(
            playerTracks = playerTracks,
            currentTrack = currentTrack,
            currentTime = response.currentTime,
            sessionId = sessionId,
          )
        }
        .getOrNull()
    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Podcast Episode")))
  }

  fun book(id: String): PlayerUiState {
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
      val chapter = findCurrentPlayerChapter(chapters, progress?.currentTime ?: 0.0)
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        author = result.author,
        title = chapter?.title ?: result.title,
        cover = result.cover,
        currentTime = progress?.currentTime ?: 0.0,
        playerChapters = chapters,
        currentChapter = chapter,
        playerBookmarks = playerBookmarks,
        playbackProgress =
          PlaybackProgress(
            formattedDuration = helper.formatChapterTime(chapter?.endTimeSeconds ?: 0.0, true)
          ),
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  fun podcast(itemId: String, episodeId: String): PlayerUiState {
    val result = libraryItemRepo.byId(itemId)
    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)

      val episode =
        media.episodes.find { it.id == episodeId }
          ?: return PlayerUiState(state = PlayerState.Hidden(Error("Failed to find episode")))
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        episodeId = episode.id,
        author = result.author,
        title = episode.title,
        cover = result.cover,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  private fun findCurrentPlayerTrack(
    playerTracks: List<PlayerTrack>,
    currentTime: Double,
  ): PlayerTrack {
    // Find the last track that starts at or before the current time
    return playerTracks.sortedBy { it.startOffset }.lastOrNull { it.startOffset <= currentTime }
      ?: playerTracks.first()
  }

  private fun findCurrentPlayerChapter(
    playerChapters: List<PlayerChapter>,
    currentTime: Double,
  ): PlayerChapter? {
    // Find the last chapter that starts at or before the current time
    if (playerChapters.isEmpty()) return null
    return playerChapters
      .sortedBy { it.startTimeSeconds }
      .lastOrNull { it.startTimeSeconds <= currentTime } ?: playerChapters.first()
  }

  fun changeChapter(uiState: PlayerUiState, target: Int): PlayerUiState {
    val chapters = uiState.playerChapters
    return if (target in chapters.indices) {
      val targetChapter = chapters[target]
      val targetTrack = findTrackFromChapter(uiState, targetChapter)
      val currentTime =
        if (uiState.playerTracks.size > 1) targetChapter.startTimeSeconds - targetTrack.startOffset
        else 0.0
      uiState.copy(
        title = targetChapter.title,
        currentChapter = targetChapter,
        currentTrack = targetTrack,
        currentTime = currentTime,
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
    val isBook = uiState.episodeId.isBlank()
    val playbackProgress =
      if (isBook) mapper.toPlaybackProgressBook(uiState, raw)
      else mapper.toPlaybackProgressPodcast(raw)

    return uiState.copy(playbackProgress = playbackProgress)
  }

  fun seekTo(uiState: PlayerUiState, target: Float, rawDurationMs: Long): PlayerUiState {
    val isBook = uiState.episodeId.isBlank()
    val durationMs =
      if (isBook) {
        (finder.bookDuration(uiState) * 1000).toLong()
      } else {
        rawDurationMs
      }
    val positionMs = finder.bookRawPositionMs(uiState, target, durationMs)

    val currentTime = (positionMs / 1000)
    val raw = RawPlaybackProgress(positionMs, durationMs, 0)
    val playbackProgress =
      if (isBook) mapper.toPlaybackProgressBook(uiState, raw)
      else mapper.toPlaybackProgressPodcast(raw)
    return uiState.copy(playbackProgress = playbackProgress, currentTime = currentTime.toDouble())
  }

  fun changeSpeed(uiState: PlayerUiState, speed: Float): PlayerUiState {
    val advancedControl = uiState.advancedControl.copy(speed = speed)
    return uiState.copy(advancedControl = advancedControl)
  }

  private fun findTrackFromChapter(uiState: PlayerUiState, chapter: PlayerChapter): PlayerTrack {
    val tracks = uiState.playerTracks
    return if (tracks.size == 1) {
      return tracks.first()
    } else {
      val track = tracks.lastOrNull { it.startOffset <= chapter.startTimeSeconds } ?: tracks.first()
      track
    }
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
    val newBookmarkTime = finder.startTime(uiState) + currentTime
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

  suspend fun syncSession(uiState: PlayerUiState, rawPositionMs: Long, duration: Duration) {
    val currentTime = finder.bookPosition(uiState, rawPositionMs)
    val request = SyncSessionRequest(currentTime, duration.inWholeSeconds)
    val result = apiService.syncSession(uiState.sessionId, request).getOrNull()
    if (result == null) return
    val isBook = uiState.episodeId.isBlank()
    val entity =
      if (isBook) progressRepo.bookById(uiState.id) else progressRepo.episodeById(uiState.episodeId)
    entity?.let {
      val progress = currentTime / it.duration
      val updated = it.copy(currentTime = currentTime.toDouble(), progress = progress)
      progressRepo.updateProgress(updated)
    }
  }
}
