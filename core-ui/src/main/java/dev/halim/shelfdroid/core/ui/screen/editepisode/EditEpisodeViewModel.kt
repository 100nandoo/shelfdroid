package dev.halim.shelfdroid.core.ui.screen.editepisode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeRepository
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeUiState
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
      is EditEpisodeEvent.UpdateTitle -> {
        _uiState.update { it.copy(title = event.title) }
      }
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

  @AssistedFactory
  interface Factory {
    fun create(navKey: EditEpisode): EditEpisodeViewModel
  }
}

sealed interface EditEpisodeEvent {
  data class UpdateTitle(val title: String) : EditEpisodeEvent

  data object Save : EditEpisodeEvent
}
