package dev.halim.shelfdroid.core.ui.screen.userinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoRepository
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoUiState
import dev.halim.shelfdroid.core.ui.navigation.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = UserInfoViewModel.Factory::class)
class UserInfoViewModel
@AssistedInject
constructor(@Assisted navKey: UserInfo, repository: UserInfoRepository) : ViewModel() {
  val userId: String = navKey.userId

  private val _uiState = MutableStateFlow(UserInfoUiState(state = GenericState.Loading))
  val uiState: StateFlow<UserInfoUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch { _uiState.value = repository.item(userId) }
  }

  fun onEvent(event: ListeningStatEvent) {
    when (event) {
      ListeningStatEvent.OnInit -> {}
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: UserInfo): UserInfoViewModel
  }
}

sealed interface ListeningStatEvent {

  data object OnInit : ListeningStatEvent
}
