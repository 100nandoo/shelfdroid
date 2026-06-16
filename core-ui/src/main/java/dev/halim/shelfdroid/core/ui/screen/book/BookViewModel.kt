package dev.halim.shelfdroid.core.ui.screen.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.book.BookRepository
import dev.halim.shelfdroid.core.data.screen.book.BookUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.core.ui.navigation.Book
import dev.halim.shelfdroid.download.DownloadRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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

  private val _uiState = MutableStateFlow(BookUiState())
  val uiState: StateFlow<BookUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BookUiState())

  fun onEvent(event: BookEvent) {
    val isSingleTrack = _uiState.value.isSingleTrack
    val download = _uiState.value.download
    when (event) {
      is BookEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            viewModelScope.launch {
              downloadRepo.downloadBook(
                itemId = id,
                title = _uiState.value.title,
                author = _uiState.value.author,
                tracks = if (isSingleTrack) listOf(download) else _uiState.value.downloads.items,
              )
            }
          }
          is CommonDownloadEvent.DeleteDownload -> {
            viewModelScope.launch {
              downloadRepo.deleteBook(
                title = _uiState.value.title,
                author = _uiState.value.author,
                tracks = if (isSingleTrack) listOf(download) else _uiState.value.downloads.items,
              )
            }
          }
        }
      }
    }
  }

  private fun initUiState() {
    viewModelScope.launch {
      repository.item(id).collect { bookUiState -> _uiState.value = bookUiState }
    }
  }

  @AssistedFactory interface Factory {
    fun create(navKey: Book): BookViewModel
  }
}

sealed interface BookEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : BookEvent
}
