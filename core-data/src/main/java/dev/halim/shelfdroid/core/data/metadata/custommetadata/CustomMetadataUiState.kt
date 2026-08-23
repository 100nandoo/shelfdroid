package dev.halim.shelfdroid.core.data.metadata.custommetadata

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.core.network.response.libraryitem.MEDIA_TYPE_BOOK

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
    val serverDetail: String? = null,
    val validationError: MetadataValidationError? = null,
    val operation: CustomMetadataOperation? = null,
  ) : CustomMetadataApiState
}

enum class CustomMetadataOperation {
  Create,
  Delete,
}

data class CustomMetadataProvider(
  val id: String,
  val name: String,
  val url: String,
  val mediaType: String = MEDIA_TYPE_BOOK,
  val slug: String = "",
  val authHeaderValue: String? = null,
)

enum class MetadataValidationError {
  TagNameRequired,
  GenreNameRequired,
  CustomMetadataProviderNameRequired,
  CustomMetadataProviderUrlRequired,
  CustomMetadataProviderIdRequired,
}

class MetadataValidationException(val error: MetadataValidationError) : IllegalArgumentException()
