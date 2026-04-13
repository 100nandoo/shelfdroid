package dev.halim.shelfdroid.core.ui.screen.settings.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.settings.player.SettingsPlayerRepository
import dev.halim.shelfdroid.core.data.screen.settings.player.SettingsPlayerUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPlayerViewModel
@Inject
constructor(private val repository: SettingsPlayerRepository) : ViewModel() {

  val uiState: StateFlow<SettingsPlayerUiState> =
    repository.playerPrefs
      .map { SettingsPlayerUiState(chapterTitleLine = it.chapterTitleLine) }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsPlayerUiState())

  fun onEvent(event: SettingsPlayerEvent) {
    when (event) {
      is SettingsPlayerEvent.ChangeChapterTitleLine ->
        viewModelScope.launch { repository.updateChapterTitleLine(event.line) }
    }
  }
}

sealed interface SettingsPlayerEvent {
  data class ChangeChapterTitleLine(val line: Int) : SettingsPlayerEvent
}
