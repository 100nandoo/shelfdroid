package dev.halim.shelfdroid.core.data.screen.libraryadmin.create

import dev.halim.shelfdroid.core.MediaType

interface LibraryAdminCreateContract {
  suspend fun loadLibraryProviders(mediaType: MediaType): Result<List<LibraryAdminProvider>>

  suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdminFilesystem>

  suspend fun createLibrary(draft: LibraryAdminDraft): Result<LibraryAdminCreateResult>

  /** Validates a five-field cron expression through Audiobookshelf before creation. */
  suspend fun validateLibrarySchedule(expression: String): Result<Unit> = Result.success(Unit)

  suspend fun synchronizeLibraries(): Result<Unit>
}

sealed class LibraryAdminScheduleValidationException(message: String? = null) :
  RuntimeException(message) {
  class Invalid(message: String? = null) : LibraryAdminScheduleValidationException(message)

  class Unavailable(message: String? = null) : LibraryAdminScheduleValidationException(message)
}
