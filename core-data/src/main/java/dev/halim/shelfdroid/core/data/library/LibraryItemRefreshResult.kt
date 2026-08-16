package dev.halim.shelfdroid.core.data.library

data class LibraryItemRefreshResult(
  val refreshedLibraryIds: Set<String>,
  val failures: List<LibraryItemRefreshFailure>,
) {
  val isSuccess: Boolean
    get() = failures.isEmpty()
}
