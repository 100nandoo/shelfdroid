package dev.halim.shelfdroid.core.data.screen.libraryadministration

interface LibraryAdministrationContract {
  suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>>

  /** Persists the requested order and returns the complete server-authoritative order. */
  suspend fun reorderLibraries(
    libraries: List<LibraryAdministrationLibrary>
  ): Result<List<LibraryAdministrationLibrary>> =
    Result.failure(UnsupportedOperationException("Library reorder is unavailable"))
}

enum class LibraryAdministrationConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class LibraryAdministrationTaskState {
  UNKNOWN,
  IDLE,
  ACTIVE,
}
