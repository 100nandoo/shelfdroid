package dev.halim.shelfdroid.core.data.screen.usersettings.changepassword

import dev.halim.shelfdroid.core.data.GenericState

data class ChangePasswordUiState(
  val state: GenericState = GenericState.Idle,
  val old: String = "",
  val new: String = "",
  val confirm: String = "",
)

sealed interface ChangePasswordUiEvent {
  data object Success : ChangePasswordUiEvent

  data object NotMatchError : ChangePasswordUiEvent

  data class ApiError(val isInvalid: Boolean, val message: String?) : ChangePasswordUiEvent
}
