package dev.halim.shelfdroid.core.data.library

import java.io.IOException

data class LibraryDataSyncResult(
  val libraries: Result<Unit>,
  val items: LibraryItemRefreshResult?,
) {
  val isSuccess: Boolean
    get() = libraries.isSuccess && items?.isSuccess == true

  val error: Throwable?
    get() =
      libraries.exceptionOrNull()
        ?: items?.failures?.firstOrNull { !it.error.isTransportFailure() }?.error
        ?: items?.failures?.firstOrNull()?.error

  /** Whether synchronization failed while communicating with the Audiobookshelf server. */
  val isTransportFailure: Boolean
    get() =
      when {
        libraries.isFailure -> libraries.exceptionOrNull()?.isTransportFailure() == true
        items?.failures.isNullOrEmpty() -> false
        else -> items.failures.all { it.error.isTransportFailure() }
      }
}

private fun Throwable.isTransportFailure(): Boolean {
  var current: Throwable? = this
  while (current != null) {
    if (current is IOException) return true
    current = current.cause
  }
  return false
}
