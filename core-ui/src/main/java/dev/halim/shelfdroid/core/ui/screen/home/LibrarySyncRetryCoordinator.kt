package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.data.sync.SyncEvent

internal class LibrarySyncRetryCoordinator {
  private var isRefreshing = false
  private var networkRefreshPending = false

  fun tryStart(event: SyncEvent): Boolean {
    if (isRefreshing) {
      if (event == SyncEvent.NetworkAvailable) {
        networkRefreshPending = true
      }
      return false
    }

    isRefreshing = true
    return true
  }

  fun finish(hadTransportFailure: Boolean, hasInternet: Boolean): Boolean {
    isRefreshing = false
    val shouldRetry = networkRefreshPending && hadTransportFailure && hasInternet
    networkRefreshPending = false
    return shouldRetry
  }
}
