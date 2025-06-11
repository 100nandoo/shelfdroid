package dev.halim.shelfdroid.core.data.screen.book

import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class BookRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val helper: Helper,
) {

  fun item(id: String): BookUiState {
    val result = libraryItemRepo.byId(id)
    val progressEntity = progressRepo.bookById(id)
    return if (result != null && result.isBook == 1L) {
      val media = Json.decodeFromString<Book>(result.media)
      val subtitle = media.metadata.subtitle ?: ""
      val description = media.metadata.description ?: ""
      val narrator = media.metadata.narrators.joinToString()
      val publishYear = media.metadata.publishedYear ?: ""
      val publisher = media.metadata.publisher ?: ""
      val genres = media.metadata.genres.joinToString()
      val language = media.metadata.language ?: ""

      val progress = progressEntity?.progress?.toFloat() ?: 0f
      val remaining = helper.calculateRemaining(media.duration ?: 0.0, progress)
      BookUiState(
        state = GenericState.Success,
        author = result.author,
        narrator = narrator,
        title = result.title,
        subtitle = subtitle,
        duration = result.duration,
        remaining = remaining,
        cover = result.cover,
        description = description,
        publishYear = publishYear,
        publisher = publisher,
        genres = genres,
        language = language,
        progress = progress,
      )
    } else {
      BookUiState(state = GenericState.Failure("Failed to fetch book"))
    }
  }
}
