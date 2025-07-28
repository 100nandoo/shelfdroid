package dev.halim.shelfdroid.core.ui.screen.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.book.BookRepository
import dev.halim.shelfdroid.core.data.screen.book.BookUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.media.download.DownloadTracker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BookViewModel
@Inject
constructor(
  private val repository: BookRepository,
  private val downloadTracker: DownloadTracker,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  private val _uiState = MutableStateFlow(BookUiState())
  val uiState: StateFlow<BookUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BookUiState())

  fun onEvent(event: BookEvent) {
    when (event) {
      is BookEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            downloadTracker.download(event.downloadEvent.downloadId, event.downloadEvent.url)
          }
          is CommonDownloadEvent.DeleteDownload -> {
            downloadTracker.delete(event.downloadEvent.downloadId)
          }
        }
      }
    }
  }

  private fun initUiState() {
    viewModelScope.launch { _uiState.update { repository.item(id) } }
  }
}

sealed class BookEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : BookEvent()
}
