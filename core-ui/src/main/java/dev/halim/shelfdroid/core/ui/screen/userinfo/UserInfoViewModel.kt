package dev.halim.shelfdroid.core.ui.screen.userinfo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoRepository
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserInfoViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, repository: UserInfoRepository) : ViewModel() {
  val userId: String = checkNotNull(savedStateHandle.get<String>("userId"))

  private val _uiState = MutableStateFlow(UserInfoUiState())
  val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch { _uiState.value = repository.item(userId) }
  }

  fun onEvent(event: ListeningStatEvent) {
    when (event) {
      ListeningStatEvent.OnInit -> {}
    }
  }
}

sealed interface ListeningStatEvent {

  data object OnInit : ListeningStatEvent
}
