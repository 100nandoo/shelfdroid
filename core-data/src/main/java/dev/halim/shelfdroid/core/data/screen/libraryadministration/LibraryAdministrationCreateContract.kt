package dev.halim.shelfdroid.core.data.screen.libraryadministration

interface LibraryAdministrationCreateContract {
  suspend fun loadLibraryProviders(
    mediaType: LibraryAdministrationMediaType
  ): Result<List<LibraryAdministrationProvider>>

  suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdministrationFilesystem>

  suspend fun createLibrary(
    draft: LibraryAdministrationDraft
  ): Result<LibraryAdministrationCreateResult>

  /** Validates a five-field cron expression through Audiobookshelf before creation. */
  suspend fun validateLibrarySchedule(expression: String): Result<Unit> = Result.success(Unit)

  suspend fun synchronizeLibraries(): Result<Unit>
}

sealed class LibraryAdministrationScheduleValidationException(message: String? = null) :
  RuntimeException(message) {
  class Invalid(message: String? = null) : LibraryAdministrationScheduleValidationException(message)

  class Unavailable(message: String? = null) : LibraryAdministrationScheduleValidationException(message)
}
