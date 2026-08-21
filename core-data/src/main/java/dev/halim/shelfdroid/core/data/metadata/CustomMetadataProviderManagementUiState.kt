package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.GenericState

data class CustomMetadataProviderManagementUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: CustomMetadataProviderManagementApiState =
    CustomMetadataProviderManagementApiState.Idle,
  val providers: List<CustomMetadataProvider> = emptyList(),
  val nameDraft: String = "",
  val urlDraft: String = "",
  val authHeaderDraft: String = "",
  val authHeaderVisible: Boolean = false,
  val revealedProviderIds: Set<String> = emptySet(),
  val dialog: CustomMetadataProviderManagementDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is CustomMetadataProviderManagementApiState.Mutating
}

sealed interface CustomMetadataProviderManagementDialog {
  data class Delete(val provider: CustomMetadataProvider) :
    CustomMetadataProviderManagementDialog
}

sealed interface CustomMetadataProviderManagementApiState {
  data object Idle : CustomMetadataProviderManagementApiState
  data object Loading : CustomMetadataProviderManagementApiState
  data class Mutating(val operation: CustomMetadataProviderOperation) :
    CustomMetadataProviderManagementApiState
  data object CreateSuccess : CustomMetadataProviderManagementApiState
  data object DeleteSuccess : CustomMetadataProviderManagementApiState
  data class Failure(
    val message: String?,
    val accessDenied: Boolean = false,
    val operation: CustomMetadataProviderOperation? = null,
  ) : CustomMetadataProviderManagementApiState
}

enum class CustomMetadataProviderOperation { Create, Delete }
