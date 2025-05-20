package dev.halim.shelfdroid.core.data.book

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import javax.inject.Inject

class BookRepository @Inject constructor(private val api: ApiService, private val helper: Helper) {

  suspend fun item(id: String): BookUiState {
    val result = api.item(id)
    result.onSuccess { response ->
      val media = response.media
      if (media is Book) {
        val cover = helper.generateItemCoverUrl(response.id)
        val subtitle = media.metadata.subtitle ?: ""
        val author = media.metadata.authors.joinToString { it.name }
        val description = media.metadata.description ?: ""
        val narrator = media.metadata.narrators.joinToString()
        val duration = helper.formatDuration(media.duration ?: 0.0)
        val publishYear = media.metadata.publishedYear ?: ""
        val publisher = media.metadata.publisher ?: ""
        val genres = media.metadata.genres.joinToString()
        val language = media.metadata.language ?: ""
        return BookUiState(
          state = GenericState.Success,
          author = author,
          narrator = narrator,
          title = media.metadata.title ?: "",
          subtitle = subtitle,
          duration = duration,
          cover = cover,
          description = description,
          publishYear = publishYear,
          publisher = publisher,
          genres = genres,
          language = language,
        )
      }

      return BookUiState(state = GenericState.Failure("Failed to parse book media"))
    }
    return BookUiState(state = GenericState.Failure("Failed to fetch book"))
  }
}
