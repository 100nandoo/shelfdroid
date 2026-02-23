package dev.halim.shelfdroid.core.data

sealed interface GenericUiEvent {
  data class ShowErrorSnackbar(val message: String = "") : GenericUiEvent

  data class ShowSuccessSnackbar(val message: String = "") : GenericUiEvent

  data class ShowPlainSnackbar(val message: String = "") : GenericUiEvent

  data object NavigateBack : GenericUiEvent
}
