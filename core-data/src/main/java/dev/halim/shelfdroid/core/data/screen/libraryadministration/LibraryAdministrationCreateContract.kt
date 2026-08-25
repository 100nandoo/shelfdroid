package dev.halim.shelfdroid.core.data.screen.libraryadministration

interface LibraryAdministrationCreateContract {
  suspend fun loadLibraryProviders(
    mediaType: LibraryAdministrationMediaType
  ): Result<List<LibraryAdministrationProvider>>

  suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdministrationFilesystem>

  suspend fun createLibrary(
    draft: LibraryAdministrationDraft
  ): Result<LibraryAdministrationCreateResult>

  suspend fun synchronizeLibraries(): Result<Unit>
}
