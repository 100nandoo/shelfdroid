package dev.halim.shelfdroid.core.ui.screen.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.book.BookApiState
import dev.halim.shelfdroid.core.data.screen.book.BookRepository
import dev.halim.shelfdroid.core.data.screen.book.BookUiState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedDetails
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.core.ui.navigation.Book
import dev.halim.shelfdroid.download.DownloadRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = BookViewModel.Factory::class)
class BookViewModel
@AssistedInject
constructor(
  private val repository: BookRepository,
  private val downloadRepo: DownloadRepo,
  @Assisted navKey: Book,
) : ViewModel() {

  val id: String = navKey.id

  private val apiState = MutableStateFlow<BookApiState>(BookApiState.Idle)
  val uiState: StateFlow<BookUiState> =
    combine(repository.item(id), apiState) { uiState, apiState ->
        uiState.copy(apiState = apiState)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BookUiState())

  fun onEvent(event: BookEvent) {
    val isSingleTrack = uiState.value.isSingleTrack
    val download = uiState.value.download
    when (event) {
      is BookEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            viewModelScope.launch {
              downloadRepo.downloadBook(
                itemId = id,
                title = uiState.value.title,
                author = uiState.value.author,
                tracks = if (isSingleTrack) listOf(download) else uiState.value.downloads.items,
              )
            }
          }
          is CommonDownloadEvent.DeleteDownload -> {
            viewModelScope.launch {
              downloadRepo.deleteBook(
                title = uiState.value.title,
                author = uiState.value.author,
                tracks = if (isSingleTrack) listOf(download) else uiState.value.downloads.items,
              )
            }
          }
        }
      }

      is BookEvent.OpenGeneratedRssFeed -> {
        apiState.update { BookApiState.OpenRssFeedLoading }
        viewModelScope.launch {
          val result =
            repository.openGeneratedRssFeed(
              itemId = id,
              details = event.details,
            )
          apiState.update {
            result.fold(
              onSuccess = { BookApiState.OpenRssFeedSuccess },
              onFailure = {
                BookApiState.OpenRssFeedFailure(it.message ?: "Failed to open generated RSS feed")
              },
            )
          }
        }
      }

      is BookEvent.CloseGeneratedRssFeed -> {
        apiState.update { BookApiState.CloseRssFeedLoading }
        viewModelScope.launch {
          val result = repository.closeGeneratedRssFeed(itemId = id, feedId = event.feedId)
          apiState.update {
            result.fold(
              onSuccess = { BookApiState.CloseRssFeedSuccess },
              onFailure = {
                BookApiState.CloseRssFeedFailure(it.message ?: "Failed to close generated RSS feed")
              },
            )
          }
        }
      }

      BookEvent.ResetApiState -> {
        apiState.update { BookApiState.Idle }
      }
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Book): BookViewModel
  }
}

sealed interface BookEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : BookEvent

  data class OpenGeneratedRssFeed(val details: GeneratedRssFeedDetails) : BookEvent

  data class CloseGeneratedRssFeed(val feedId: String) : BookEvent

  data object ResetApiState : BookEvent
}
