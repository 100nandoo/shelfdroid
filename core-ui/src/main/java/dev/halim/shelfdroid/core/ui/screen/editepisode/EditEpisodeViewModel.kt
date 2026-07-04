package dev.halim.shelfdroid.core.ui.screen.editepisode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeRepository
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeTab
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeUiState
import dev.halim.shelfdroid.core.data.screen.editepisode.EpisodeDetailsForm
import dev.halim.shelfdroid.core.data.screen.editepisode.EpisodeMatchState
import dev.halim.shelfdroid.core.ui.navigation.EditEpisode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditEpisodeViewModel.Factory::class)
class EditEpisodeViewModel
@AssistedInject
constructor(
  private val repository: EditEpisodeRepository,
  @Assisted navKey: EditEpisode,
) : ViewModel() {
  private val itemId = navKey.itemId
  private val episodeId = navKey.episodeId

  private val _uiState =
    MutableStateFlow(EditEpisodeUiState(itemId = itemId, episodeId = episodeId))
  val uiState: StateFlow<EditEpisodeUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _uiState.value)

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

  init {
    load()
  }

  fun onEvent(event: EditEpisodeEvent) {
    when (event) {
      is EditEpisodeEvent.UpdateDetails ->
        _uiState.update { state ->
          val updatedDetails = event.transform(state.details)
          val syncedSearchTerm =
            if (state.match.searchTerm == state.details.title) updatedDetails.title
            else state.match.searchTerm
          state.copy(
            details = updatedDetails,
            match = state.match.copy(searchTerm = syncedSearchTerm),
          )
        }
      is EditEpisodeEvent.ChangeTab -> _uiState.update { it.copy(currentTab = event.tab) }
      is EditEpisodeEvent.UpdateMatch ->
        _uiState.update { it.copy(match = event.transform(it.match)) }
      EditEpisodeEvent.RunMatchSearch -> runMatchSearch()
      is EditEpisodeEvent.ApplyMatch -> applyMatch(event.index)
      EditEpisodeEvent.Save -> save()
    }
  }

  private fun load() {
    viewModelScope.launch {
      _uiState.value = repository.item(itemId = itemId, episodeId = episodeId)
    }
  }

  private fun save() {
    if (_uiState.value.canSave().not()) return
    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      _uiState.value = repository.save(state = _uiState.value, events = _events)
    }
  }

  private fun runMatchSearch() {
    val state = _uiState.value
    if (state.state != GenericState.Success || state.isSaving) return
    val searchTerm = state.match.searchTerm.trim()
    if (searchTerm.isBlank()) return
    _uiState.update {
      it.copy(
        match =
          it.match.copy(
            searchTerm = searchTerm,
            results = emptyList(),
            hasSearched = false,
            isSearching = true,
            errorMessage = null,
          )
      )
    }
    viewModelScope.launch {
      _uiState.value = repository.searchMatches(state = _uiState.value, events = _events)
    }
  }

  private fun applyMatch(index: Int) {
    val state = _uiState.value
    if (state.state != GenericState.Success || state.isSaving || state.match.isSearching) return
    if (state.match.results.getOrNull(index) == null) return
    _uiState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      _uiState.value =
        repository.applyMatch(state = _uiState.value, index = index, events = _events)
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: EditEpisode): EditEpisodeViewModel
  }
}

sealed interface EditEpisodeEvent {
  data class ChangeTab(val tab: EditEpisodeTab) : EditEpisodeEvent

  data class UpdateDetails(val transform: (EpisodeDetailsForm) -> EpisodeDetailsForm) :
    EditEpisodeEvent

  data class UpdateMatch(val transform: (EpisodeMatchState) -> EpisodeMatchState) : EditEpisodeEvent

  data object RunMatchSearch : EditEpisodeEvent

  data class ApplyMatch(val index: Int) : EditEpisodeEvent

  data object Save : EditEpisodeEvent
}
