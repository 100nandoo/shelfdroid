package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.data.download.ManagedDownload

sealed interface GenericUiEvent {
  data class ShowErrorSnackbar(val message: String = "") : GenericUiEvent

  data class ShowSuccessSnackbar(val message: String = "") : GenericUiEvent

  data class ShowPlainSnackbar(val message: String = "") : GenericUiEvent

  data class RequestManagedDownload(val download: ManagedDownload) : GenericUiEvent

  data object NavigateBack : GenericUiEvent
}
