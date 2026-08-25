package dev.halim.shelfdroid.core.data.screen.libraryadministration

interface LibraryAdministrationContract {
  suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>>
}
