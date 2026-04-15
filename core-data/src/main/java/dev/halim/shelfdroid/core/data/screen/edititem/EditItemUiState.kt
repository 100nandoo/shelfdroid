package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.shelfdroid.core.data.GenericState

data class EditItemUiState(
  val state: GenericState = GenericState.Loading,
  val itemId: String = "",
  val coverUrl: String = "",
  val webBaseUrl: String = "",
  val currentTab: EditItemTab = EditItemTab.Details,
  val details: DetailsForm = DetailsForm(),
  val originalDetails: DetailsForm = DetailsForm(),
  val chapters: List<ChapterRow> = emptyList(),
  val libraryFiles: List<LibraryFileRow> = emptyList(),
  val match: MatchState = MatchState(),
  val coverSearch: CoverSearchState = CoverSearchState(),
  val isSaving: Boolean = false,
  val isCoverWorking: Boolean = false,
  val isToolWorking: Boolean = false,
  val seriesSuggestions: List<String> = emptyList(),
)

data class CoverSearchState(
  val state: GenericState = GenericState.Idle,
  val title: String = "",
  val author: String = "",
  val provider: String = "best",
  val providers: List<MatchProvider> = emptyList(),
  val results: List<String> = emptyList(),
)

data class ChapterRow(val id: Int, val title: String, val start: Double, val end: Double)

data class LibraryFileRow(val path: String, val size: Long, val fileType: String)

data class MatchResultRow(
  val cover: String,
  val title: String,
  val author: String,
  val description: String,
)

enum class EditItemTab {
  Details,
  Cover,
  Chapters,
  Files,
  Match,
  Tools,
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
)

data class MatchState(
  val providers: List<MatchProvider> = emptyList(),
  val selectedProvider: String = "audible",
  val title: String = "",
  val author: String = "",
  val results: List<MatchResultRow> = emptyList(),
  val rawResults: List<SearchBookMatchResponse> = emptyList(),
  val isSearching: Boolean = false,
)

data class MatchProvider(val value: String, val text: String)

data class SeriesEntry(val name: String, val sequence: String = "")
