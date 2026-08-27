package dev.halim.shelfdroid.core.data.screen.libraryadmin

sealed interface LibraryAdminProviderState {
  data object Loading : LibraryAdminProviderState

  data class Success(val providers: List<LibraryAdminProvider>) :
    LibraryAdminProviderState

  data class Failure(val message: String?) : LibraryAdminProviderState
}

sealed interface LibraryAdminFilesystemState {
  data object Closed : LibraryAdminFilesystemState

  data class Loading(val path: String?) : LibraryAdminFilesystemState

  data class Success(val path: String?, val filesystem: LibraryAdminFilesystem) :
    LibraryAdminFilesystemState

  data class Failure(val path: String?, val message: String?) : LibraryAdminFilesystemState
}

sealed interface LibraryAdminCreateSubmissionState {
  data object Idle : LibraryAdminCreateSubmissionState

  data object Submitting : LibraryAdminCreateSubmissionState

  data class ServerFailure(val message: String?) : LibraryAdminCreateSubmissionState

  data class LocalSyncFailure(
    val library: LibraryAdminLibrary,
    val message: String?,
  ) : LibraryAdminCreateSubmissionState
}

sealed interface LibraryAdminScheduleValidationState {
  data object Idle : LibraryAdminScheduleValidationState

  data object Validating : LibraryAdminScheduleValidationState

  data object Valid : LibraryAdminScheduleValidationState

  data class Invalid(val message: String? = null) : LibraryAdminScheduleValidationState

  data class Unavailable(val message: String? = null) : LibraryAdminScheduleValidationState
}

enum class LibraryAdminCreateTab {
  DETAILS,
  SETTINGS,
  SCANNER,
  SCHEDULE,
}

sealed interface LibraryAdminCreateNavigation {
  data object Back : LibraryAdminCreateNavigation

  data class Created(val library: LibraryAdminLibrary) : LibraryAdminCreateNavigation
}

data class LibraryAdminCreateUiState(
  val draft: LibraryAdminDraft = LibraryAdminDraft(),
  val providerState: LibraryAdminProviderState =
    LibraryAdminProviderState.Loading,
  val filesystemState: LibraryAdminFilesystemState =
    LibraryAdminFilesystemState.Closed,
  val manualFolderDraft: String = "",
  val selectedTab: LibraryAdminCreateTab = LibraryAdminCreateTab.DETAILS,
  val validation: LibraryAdminValidation = LibraryAdminValidation(),
  val focusField: LibraryAdminCreateField? = null,
  val isDirty: Boolean = false,
  val submissionState: LibraryAdminCreateSubmissionState =
    LibraryAdminCreateSubmissionState.Idle,
  val scheduleValidation: LibraryAdminScheduleValidationState =
    LibraryAdminScheduleValidationState.Idle,
  val discardDialog: Boolean = false,
  val navigation: LibraryAdminCreateNavigation? = null,
) {
  val isSubmitting: Boolean
    get() = submissionState is LibraryAdminCreateSubmissionState.Submitting

  val isBusy: Boolean
    get() = isSubmitting || scheduleValidation is LibraryAdminScheduleValidationState.Validating
}
