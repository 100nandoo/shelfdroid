package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.PodcastScheduleMode
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.PodcastScheduleSimpleBuilder

data class EditItemUiState(
  val state: GenericState = GenericState.Loading,
  val itemId: String = "",
  val mediaKind: EditItemMediaKind = EditItemMediaKind.Book,
  val coverUrl: String = "",
  val pendingCoverUrl: String? = null,
  val webBaseUrl: String = "",
  val currentTab: EditItemTab = EditItemTab.Details,
  val details: DetailsForm = DetailsForm(),
  val originalDetails: DetailsForm = DetailsForm(),
  val schedule: PodcastScheduleForm = PodcastScheduleForm(),
  val originalSchedule: PodcastScheduleForm = PodcastScheduleForm(),
  val scheduleMode: PodcastScheduleMode = PodcastScheduleMode.Advanced,
  val simpleScheduleBuilder: PodcastScheduleSimpleBuilder = PodcastScheduleSimpleBuilder(),
  val scheduleCronError: String? = null,
  val chapters: List<ChapterRow> = emptyList(),
  val episodes: List<EpisodeRow> = emptyList(),
  val episodeUpdate: EpisodeUpdateState = EpisodeUpdateState(),
  val libraryFiles: List<LibraryFileRow> = emptyList(),
  val match: MatchState = MatchState.Book(),
  val coverSearch: CoverSearchState = CoverSearchState(),
  val isSaving: Boolean = false,
  val isCoverWorking: Boolean = false,
  val isToolWorking: Boolean = false,
  val activeFileActionIno: String? = null,
  val pendingDeleteFile: LibraryFileRow? = null,
  val seriesSuggestions: List<String> = emptyList(),
)

fun EditItemUiState.supportedTabs(): List<EditItemTab> = mediaKind.supportedTabs()

fun EditItemUiState.normalized(): EditItemUiState =
  copy(currentTab = currentTab.coerceFor(mediaKind))

data class CoverSearchState(
  val state: GenericState = GenericState.Idle,
  val title: String = "",
  val author: String = "",
  val provider: String = "best",
  val providers: List<MatchProvider> = emptyList(),
  val results: List<String> = emptyList(),
)

data class ChapterRow(val id: Int, val title: String, val start: Double, val end: Double)

data class EpisodeRow(
  val id: String,
  val title: String,
  val secondaryText: String = "",
)

data class EpisodeUpdateState(
  val persistedCutoffMillis: Long = 0L,
  val selectedCutoffMillis: Long? = null,
  val limitInput: String = "3",
  val isRunning: Boolean = false,
)

data class PodcastScheduleForm(
  val autoDownloadEpisodes: Boolean = false,
  val cronExpression: String = "",
  val maxEpisodesToKeepInput: String = "0",
  val maxNewEpisodesToDownloadInput: String = "0",
)

data class LibraryFileRow(
  val ino: String,
  val path: String,
  val filename: String,
  val sizeText: String,
  val fileType: String,
)

data class MatchResultRow(
  val cover: String,
  val title: String,
  val author: String,
  val description: String,
)

const val DEFAULT_BOOK_MATCH_PROVIDER = "audible"
const val DEFAULT_PODCAST_MATCH_PROVIDER = "itunes"

data class PodcastMatchResultRow(
  val cover: String,
  val title: String,
  val author: String,
  val genres: List<String> = emptyList(),
  val episodeCount: Int = 0,
  val feedUrl: String = "",
  val itunesId: String = "",
  val releaseDate: String = "",
  val explicit: Boolean = false,
  val description: String = "",
)

enum class EditItemTab {
  Details,
  Cover,
  Chapters,
  Episodes,
  Files,
  Match,
  Schedule,
  Tools,
}

fun EditItemTab.coerceFor(mediaKind: EditItemMediaKind): EditItemTab =
  if (this in mediaKind.supportedTabs()) this else EditItemTab.Details

enum class EditItemMediaKind {
  Book,
  Podcast,
}

fun EditItemMediaKind.supportedTabs(): List<EditItemTab> =
  when (this) {
    EditItemMediaKind.Book ->
      listOf(
        EditItemTab.Details,
        EditItemTab.Cover,
        EditItemTab.Chapters,
        EditItemTab.Files,
        EditItemTab.Match,
        EditItemTab.Tools,
      )
    EditItemMediaKind.Podcast ->
      listOf(
        EditItemTab.Details,
        EditItemTab.Cover,
        EditItemTab.Episodes,
        EditItemTab.Files,
        EditItemTab.Match,
        EditItemTab.Schedule,
      )
  }

data class DetailsForm(
  val title: String = "",
  val subtitle: String = "",
  val authors: List<String> = emptyList(),
  val narrators: List<String> = emptyList(),
  val series: List<SeriesEntry> = emptyList(),
  val genres: List<String> = emptyList(),
  val tags: List<String> = emptyList(),
  val publishedYear: String = "",
  val publisher: String = "",
  val description: String = "",
  val isbn: String = "",
  val asin: String = "",
  val language: String = "",
  val explicit: Boolean = false,
  val abridged: Boolean = false,
  val podcastAuthor: String = "",
  val rssFeedUrl: String = "",
  val releaseDate: String = "",
  val itunesId: String = "",
  val podcastType: String = "episodic",
)

fun DetailsForm.primaryAuthor(mediaKind: EditItemMediaKind): String =
  when (mediaKind) {
    EditItemMediaKind.Book -> authors.joinToString()
    EditItemMediaKind.Podcast -> podcastAuthor
  }

sealed interface MatchState {
  val providers: List<MatchProvider>
  val selectedProvider: String
  val isSearching: Boolean

  data class Book(
    override val providers: List<MatchProvider> = emptyList(),
    override val selectedProvider: String = DEFAULT_BOOK_MATCH_PROVIDER,
    val title: String = "",
    val author: String = "",
    val results: List<MatchResultRow> = emptyList(),
    val rawResults: List<SearchBookMatchResponse> = emptyList(),
    override val isSearching: Boolean = false,
  ) : MatchState

  data class Podcast(
    override val providers: List<MatchProvider> = emptyList(),
    override val selectedProvider: String = DEFAULT_PODCAST_MATCH_PROVIDER,
    val searchTerm: String = "",
    val results: List<PodcastMatchResultRow> = emptyList(),
    val review: PodcastMatchReviewState? = null,
    val hasSearched: Boolean = false,
    override val isSearching: Boolean = false,
  ) : MatchState
}

data class PodcastMatchReviewState(
  val result: PodcastMatchResultRow,
  val draft: PodcastMatchDraft = PodcastMatchDraft(),
  val selectedFields: Set<PodcastMatchField> = emptySet(),
)

data class PodcastMatchDraft(
  val title: String = "",
  val author: String = "",
  val feedUrl: String = "",
  val itunesId: String = "",
  val releaseDate: String = "",
  val explicit: Boolean = false,
)

enum class PodcastMatchField {
  Cover,
  Title,
  Author,
  Genres,
  RssFeedUrl,
  ItunesId,
  ReleaseDate,
  Explicit,
}

data class MatchProvider(val value: String, val text: String)

data class SeriesEntry(val name: String, val sequence: String = "")

fun EditItemUiState.displayCoverUrl(): String = pendingCoverUrl ?: coverUrl

fun EditItemUiState.canConfigureSchedule(): Boolean =
  mediaKind == EditItemMediaKind.Podcast &&
    (originalDetails.rssFeedUrl.isNotBlank() || originalSchedule.autoDownloadEpisodes)

fun EditItemUiState.hasScheduleChanges(): Boolean =
  schedule.asComparable() != originalSchedule.asComparable()

private fun PodcastScheduleForm.asComparable() =
  ComparablePodcastSchedule(
    autoDownloadEpisodes = autoDownloadEpisodes,
    cronExpression = cronExpression.takeIf { autoDownloadEpisodes }?.trim(),
    maxEpisodesToKeep = normalizedNonNegativeInt(maxEpisodesToKeepInput),
    maxNewEpisodesToDownload = normalizedNonNegativeInt(maxNewEpisodesToDownloadInput),
  )

internal fun normalizedNonNegativeInt(input: String): Int =
  input.trim().toIntOrNull()?.takeIf { it >= 0 } ?: 0

private data class ComparablePodcastSchedule(
  val autoDownloadEpisodes: Boolean,
  val cronExpression: String?,
  val maxEpisodesToKeep: Int,
  val maxNewEpisodesToDownload: Int,
)
