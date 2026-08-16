package dev.halim.shelfdroid.core.data.library

data class LibraryDataSyncResult(
  val libraries: Result<Unit>,
  val items: LibraryItemRefreshResult?,
) {
  val isSuccess: Boolean
    get() = libraries.isSuccess && items?.isSuccess == true

  val error: Throwable?
    get() = libraries.exceptionOrNull() ?: items?.failures?.firstOrNull()?.error
}
