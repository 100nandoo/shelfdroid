package dev.halim.shelfdroid.core.ui.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemRepository
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemTab
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.navigation.EditItem
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditItemViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: EditItemRepository) :
  ViewModel() {

  private val route: EditItem = savedStateHandle.toRoute()

  private val _uiState = MutableStateFlow(EditItemUiState(itemId = route.itemId))
  val uiState: StateFlow<EditItemUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        EditItemUiState(itemId = route.itemId),
      )

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

  private fun load() {
    viewModelScope.launch { _uiState.value = repository.load(route.itemId) }
  }

  fun onEvent(event: EditItemEvent) {
    when (event) {
      is EditItemEvent.ChangeTab -> _uiState.update { it.copy(currentTab = event.tab) }
      is EditItemEvent.UpdateDetails ->
        _uiState.update { it.copy(details = event.transform(it.details)) }
      EditItemEvent.Save -> save()
      EditItemEvent.QuickMatch -> quickMatch()
      EditItemEvent.ReScan -> reScan()
      is EditItemEvent.UploadCover -> uploadCover(event.uri, event.contentResolver)
      is EditItemEvent.SetCoverUrl -> setCoverUrl(event.url)
      EditItemEvent.DeleteCover -> deleteCover()
      is EditItemEvent.UpdateMatchProvider ->
        _uiState.update { it.copy(match = it.match.copy(selectedProvider = event.provider)) }
      is EditItemEvent.UpdateMatchTitle ->
        _uiState.update { it.copy(match = it.match.copy(title = event.title)) }
      is EditItemEvent.UpdateMatchAuthor ->
        _uiState.update { it.copy(match = it.match.copy(author = event.author)) }
      EditItemEvent.RunMatchSearch -> runMatchSearch()
      is EditItemEvent.ApplyMatchResult ->
        _uiState.update { repository.applyMatch(it, event.index) }
      is EditItemEvent.UpdateCoverSearchProvider ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(provider = event.provider)) }
      is EditItemEvent.UpdateCoverSearchTitle ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(title = event.title)) }
      is EditItemEvent.UpdateCoverSearchAuthor ->
        _uiState.update { it.copy(coverSearch = it.coverSearch.copy(author = event.author)) }
      EditItemEvent.RunCoverSearch -> runCoverSearch()
      EditItemEvent.EmbedMetadata -> embedMetadata()
    }
  }

  private fun save() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    val result = repository.save(_uiState.value, _events)
    _uiState.value = result
  }

  private fun quickMatch() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.quickMatch(_uiState.value, _events)
  }

  private fun reScan() = viewModelScope.launch {
    _uiState.update { it.copy(isSaving = true) }
    _uiState.value = repository.reScan(_uiState.value, _events)
  }

  private fun uploadCover(uri: Uri, contentResolver: ContentResolver) = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value = repository.uploadCover(_uiState.value, uri, contentResolver, _events)
  }

  private fun setCoverUrl(url: String) = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value = repository.setCoverUrl(_uiState.value, url, _events)
  }

  private fun deleteCover() = viewModelScope.launch {
    _uiState.update { it.copy(isCoverWorking = true) }
    _uiState.value = repository.deleteCover(_uiState.value, _events)
  }

  private fun runMatchSearch() = viewModelScope.launch {
    _uiState.update { it.copy(match = it.match.copy(isSearching = true)) }
    _uiState.value = repository.searchMatches(_uiState.value, _events)
  }

  private fun runCoverSearch() = viewModelScope.launch {
    _uiState.update { it.copy(coverSearch = it.coverSearch.copy(state = GenericState.Loading)) }
    _uiState.value = repository.searchCovers(_uiState.value, _events)
  }

  private fun embedMetadata() = viewModelScope.launch {
    _uiState.update { it.copy(isToolWorking = true) }
    _uiState.value = repository.embedMetadata(_uiState.value, _events)
  }
}

sealed interface EditItemEvent {
  data class ChangeTab(val tab: EditItemTab) : EditItemEvent

  data class UpdateDetails(val transform: (DetailsForm) -> DetailsForm) : EditItemEvent

  data object Save : EditItemEvent

  data object QuickMatch : EditItemEvent

  data object ReScan : EditItemEvent

  data class UploadCover(val uri: Uri, val contentResolver: ContentResolver) : EditItemEvent

  data class SetCoverUrl(val url: String) : EditItemEvent

  data object DeleteCover : EditItemEvent

  data class UpdateMatchProvider(val provider: String) : EditItemEvent

  data class UpdateMatchTitle(val title: String) : EditItemEvent

  data class UpdateMatchAuthor(val author: String) : EditItemEvent

  data object RunMatchSearch : EditItemEvent

  data class ApplyMatchResult(val index: Int) : EditItemEvent

  data class UpdateCoverSearchProvider(val provider: String) : EditItemEvent

  data class UpdateCoverSearchTitle(val title: String) : EditItemEvent

  data class UpdateCoverSearchAuthor(val author: String) : EditItemEvent

  data object RunCoverSearch : EditItemEvent

  data object EmbedMetadata : EditItemEvent
}
