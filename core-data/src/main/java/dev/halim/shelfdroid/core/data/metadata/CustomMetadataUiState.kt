package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.GenericState

data class CustomMetadataUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: CustomMetadataApiState = CustomMetadataApiState.Idle,
  val providers: List<CustomMetadataProvider> = emptyList(),
  val nameDraft: String = "",
  val urlDraft: String = "",
  val authHeaderDraft: String = "",
  val authHeaderVisible: Boolean = false,
  val revealedProviderIds: Set<String> = emptySet(),
  val dialog: CustomMetadataDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is CustomMetadataApiState.Mutating
}

sealed interface CustomMetadataDialog {
  data class Delete(val provider: CustomMetadataProvider) : CustomMetadataDialog
}

sealed interface CustomMetadataApiState {
  data object Idle : CustomMetadataApiState

  data object Loading : CustomMetadataApiState

  data class Mutating(val operation: CustomMetadataOperation) : CustomMetadataApiState

  data object CreateSuccess : CustomMetadataApiState

  data object DeleteSuccess : CustomMetadataApiState

  data class Failure(
    val message: String?,
    val accessDenied: Boolean = false,
    val operation: CustomMetadataOperation? = null,
  ) : CustomMetadataApiState
}

enum class CustomMetadataOperation {
  Create,
  Delete,
}
