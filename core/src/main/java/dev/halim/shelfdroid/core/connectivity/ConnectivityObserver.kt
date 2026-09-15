package dev.halim.shelfdroid.core.connectivity

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
  /** Validated internet connectivity; does not guarantee Audiobookshelf server reachability. */
  val isConnected: StateFlow<Boolean>
}
