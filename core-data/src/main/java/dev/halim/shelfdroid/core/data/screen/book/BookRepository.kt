package dev.halim.shelfdroid.core.data.screen.book

import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class BookRepository @Inject constructor(private val libraryItemRepo: LibraryItemRepo) {

  fun item(id: String): BookUiState {
    val result = libraryItemRepo.byId(id)
    return if (result != null && result.isBook == 1L) {
      val media = Json.decodeFromString<Book>(result.media)
      val subtitle = media.metadata.subtitle ?: ""
      val description = media.metadata.description ?: ""
      val narrator = media.metadata.narrators.joinToString()
      val publishYear = media.metadata.publishedYear ?: ""
      val publisher = media.metadata.publisher ?: ""
      val genres = media.metadata.genres.joinToString()
      val language = media.metadata.language ?: ""
      BookUiState(
        state = GenericState.Success,
        author = result.author,
        narrator = narrator,
        title = result.title,
        subtitle = subtitle,
        duration = result.duration,
        cover = result.cover,
        description = description,
        publishYear = publishYear,
        publisher = publisher,
        genres = genres,
        language = language,
      )
    } else {
      BookUiState(state = GenericState.Failure("Failed to fetch book"))
    }
  }
}
