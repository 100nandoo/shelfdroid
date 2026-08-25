package dev.halim.shelfdroid.core.data.screen.libraryadministration

sealed interface LibraryAdministrationProviderState {
  data object Loading : LibraryAdministrationProviderState

  data class Success(val providers: List<LibraryAdministrationProvider>) :
    LibraryAdministrationProviderState

  data class Failure(val message: String?) : LibraryAdministrationProviderState
}

sealed interface LibraryAdministrationFilesystemState {
  data object Closed : LibraryAdministrationFilesystemState

  data class Loading(val path: String?) : LibraryAdministrationFilesystemState

  data class Success(val path: String?, val filesystem: LibraryAdministrationFilesystem) :
    LibraryAdministrationFilesystemState

  data class Failure(val path: String?, val message: String?) : LibraryAdministrationFilesystemState
}

sealed interface LibraryAdministrationCreateSubmissionState {
  data object Idle : LibraryAdministrationCreateSubmissionState

  data object Submitting : LibraryAdministrationCreateSubmissionState

  data class ServerFailure(val message: String?) : LibraryAdministrationCreateSubmissionState

  data class LocalSyncFailure(
    val library: LibraryAdministrationLibrary,
    val message: String?,
  ) : LibraryAdministrationCreateSubmissionState
}

enum class LibraryAdministrationCreateTab {
  DETAILS,
  SETTINGS,
  SCANNER,
}

sealed interface LibraryAdministrationCreateNavigation {
  data object Back : LibraryAdministrationCreateNavigation

  data class Created(val library: LibraryAdministrationLibrary) : LibraryAdministrationCreateNavigation
}

data class LibraryAdministrationCreateUiState(
  val draft: LibraryAdministrationDraft = LibraryAdministrationDraft(),
  val providerState: LibraryAdministrationProviderState =
    LibraryAdministrationProviderState.Loading,
  val filesystemState: LibraryAdministrationFilesystemState =
    LibraryAdministrationFilesystemState.Closed,
  val manualFolderDraft: String = "",
  val selectedTab: LibraryAdministrationCreateTab = LibraryAdministrationCreateTab.DETAILS,
  val validation: LibraryAdministrationValidation = LibraryAdministrationValidation(),
  val focusField: LibraryAdministrationCreateField? = null,
  val isDirty: Boolean = false,
  val submissionState: LibraryAdministrationCreateSubmissionState =
    LibraryAdministrationCreateSubmissionState.Idle,
  val discardDialog: Boolean = false,
  val navigation: LibraryAdministrationCreateNavigation? = null,
) {
  val isSubmitting: Boolean
    get() = submissionState is LibraryAdministrationCreateSubmissionState.Submitting
}
