package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BookmarkRequest
import dev.halim.shelfdroid.core.AdvancedControl
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.RawPlaybackProgress
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.PodcastEpisodeRepository
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class PlayerRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepository,
  private val podcastEpisodeRepo: PodcastEpisodeRepository,
  private val progressRepo: ProgressRepository,
  private val bookmarkRepo: BookmarkRepository,
  private val helper: Helper,
  private val apiService: ApiService,
  private val mapper: PlayerMapper,
  private val finder: PlayerFinder,
  private val downloadRepo: DownloadRepo,
  private val state: PlayerInternalStateHolder,
  private val prefsRepository: PrefsRepository,
  private val playbackSessionResolver: PlaybackSessionResolver,
) {

  suspend fun prepareBookPlayback(
    id: String,
    advancedControl: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): Result<PreparedPlayback> {
    return try {
      val playerUiState =
        buildBookPlaybackState(id, advancedControl, changeBehaviour)
          ?: return Result.failure(IllegalStateException("Book not found"))
      val sessionId =
        playbackSessionResolver.resolve(playerUiState.downloadState) {
          val request = mapper.toPlayRequest()
          val response = apiService.playBook(id, request)
          response.id
        }
      Result.success(PreparedPlayback(playerUiState, sessionId))
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      Result.failure(exception)
    }
  }

  suspend fun preparePodcastPlayback(
    itemId: String,
    episodeId: String,
    advancedControl: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): Result<PreparedPlayback> {
    return try {
      val playerUiState =
        buildPodcastPlaybackState(itemId, episodeId, advancedControl, changeBehaviour)
          ?: return Result.failure(IllegalStateException("Podcast episode not found"))
      val sessionId =
        playbackSessionResolver.resolve(playerUiState.downloadState) {
          val request = mapper.toPlayRequest()
          val response = apiService.playPodcast(itemId, episodeId, request)
          response.id
        }
      Result.success(PreparedPlayback(playerUiState, sessionId))
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      Result.failure(exception)
    }
  }

  fun activatePlayback(playback: PreparedPlayback) {
    state.changeMedia(playback.uiState, playback.sessionId)
  }

  private suspend fun buildBookPlaybackState(
    id: String,
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState? {
    val result = libraryItemRepo.byId(id)
    val media = libraryItemRepo.bookById(id)
    val progress = progressRepo.bookById(id)
    val bookmarks = bookmarkRepo.byLibraryItemId(id)
    val playerBookmarks = bookmarks.map { mapper.toPlayerBookmark(it) }
    val playerPrefs = prefsRepository.playerPrefs.first()
    return if (result != null && media != null) {
      val chapters =
        media.chapters.mapIndexed { i, bookChapter ->
          mapper.toPlayerChapter(i, bookChapter, media.chapters.size)
        }
      val isSingleTrack = media.audioTracks.size == 1
      val localTrackUris =
        downloadRepo.localBookTrackUris(result.title, result.author, media.audioTracks)
      val playerTracks =
        media.audioTracks.map { track ->
          mapper.toPlayerTrack(track, localTrackUris[track.index])
        }
      val currentTrack = finder.trackFromChapter(playerTracks, progress?.currentTime ?: 0.0)
      val currentChapter = finder.playerChapter(chapters, progress?.currentTime ?: 0.0)
      val currentTime =
        if (currentChapter != null) (progress?.currentTime ?: 0.0) - currentChapter.startTimeSeconds
        else progress?.currentTime ?: 0.0

      val downloadState =
        if (isSingleTrack) {
          downloadRepo.bookItem(id, result.title, result.author, media.audioTracks.first()).state
        } else {
          downloadRepo
            .multipleTrackItem(
              itemId = id,
              title = result.title,
              author = result.author,
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
        chapterTitleLine = playerPrefs.chapterTitleLine,
        chapterTimeDisplay = playerPrefs.chapterTimeDisplay,
      )
    } else null
  }

  private suspend fun buildPodcastPlaybackState(
    itemId: String,
    episodeId: String,
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): PlayerUiState? {
    val result = libraryItemRepo.byId(itemId)
    val progress = progressRepo.episodeById(episodeId)
    val playerPrefs = prefsRepository.playerPrefs.first()
    return if (result != null && result.isBook.toBoolean().not()) {
      val episode =
        podcastEpisodeRepo.byId(episodeId)?.takeIf { it.libraryItemId == itemId } ?: return null

      val downloadState =
        downloadRepo
          .item(
            itemId = itemId,
            episodeId = episodeId,
            url = episode.audioTrack.contentUrl,
            title = episode.title,
            secondaryLabel = result.title,
            filename = episode.audioTrack.metadata.filename,
          )
          .state

      val localUri =
        downloadRepo.localPodcastEpisodeUri(result.title, episode.audioTrack.metadata.filename)
      val playerTrack = episode.audioTrack.let { mapper.toPlayerTrack(it, localUri) }
      val advancedControl = decideAdvanceControl(existing, changeBehaviour)

      val currentTime =
        if (progress?.isFinished?.toBoolean() == true) 0.0 else progress?.currentTime ?: 0.0

      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        episodeId = episode.id,
        author = result.author,
        title = episode.title,
        cover = result.cover,
        playerTracks = listOf(playerTrack),
        currentTrack = playerTrack,
        currentTime = currentTime,
        downloadState = downloadState,
        advancedControl = advancedControl,
        chapterTitleLine = playerPrefs.chapterTitleLine,
        chapterTimeDisplay = playerPrefs.chapterTimeDisplay,
      )
    } else null
  }

  suspend fun decideAdvanceControl(
    existing: AdvancedControl,
    changeBehaviour: ChangeBehaviour,
  ): AdvancedControl {
    val playbackPrefs = prefsRepository.playbackPrefs.first()
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

  suspend fun deleteBookmark(itemId: String, bookmark: PlayerBookmark): Boolean {
    val result = apiService.deleteBookmark(itemId, bookmark.time.toInt()).getOrNull()
    if (result == null) return false
    bookmarkRepo.delete(itemId, bookmark.time)
    return true
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
    itemId: String,
    bookmark: PlayerBookmark,
    title: String,
  ): Boolean {
    val request = BookmarkRequest(bookmark.time, title)
    val result = apiService.updateBookmark(itemId, request).getOrNull()
    if (result == null) return false
    bookmarkRepo.updateTitle(itemId, bookmark.time, title)
    return true
  }

  fun newBookmarkTime(uiState: PlayerUiState, currentTime: Long): PlayerUiState {
    val newBookmarkTime = state.startOffset() + currentTime
    val readableTime = helper.formatChapterTime(newBookmarkTime)
    val bookmark = PlayerBookmark(time = newBookmarkTime.toLong(), readableTime = readableTime)
    return uiState.copy(newBookmarkTime = bookmark)
  }

  suspend fun createBookmark(itemId: String, time: Long, title: String): PlayerBookmark? {
    val request = BookmarkRequest(time, title)
    val result = apiService.createBookmark(itemId, request).getOrNull() ?: return null
    val entity = bookmarkRepo.insertAndConvert(result)
    return mapper.toPlayerBookmark(entity)
  }
}
