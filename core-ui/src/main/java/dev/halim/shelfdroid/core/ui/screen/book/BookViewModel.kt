package dev.halim.shelfdroid.core.ui.screen.book

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.book.BookRepository
import dev.halim.shelfdroid.core.data.screen.book.BookUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BookViewModel
@Inject
constructor(
  private val repository: BookRepository,
  private val downloadRepo: DownloadRepo,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

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
            if (isSingleTrack) {
              downloadRepo.download(id = download.id, url = download.url, message = download.title)
            } else {
              _uiState.value.downloads.items.forEach {
                downloadRepo.download(id = it.id, url = it.url, message = it.title)
              }
            }
          }
          is CommonDownloadEvent.DeleteDownload -> {
            if (isSingleTrack) {
              downloadRepo.delete(download.id)
            } else {
              _uiState.value.downloads.items.forEach { downloadRepo.delete(id = it.id) }
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
}

sealed class BookEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : BookEvent()
}
