package dev.halim.core.network.connectivity

data class ConnectivityStatus(val isMetered: Boolean, val hasInternet: Boolean)

sealed class NetworkType {
  object Wifi : NetworkType()

  object Cellular : NetworkType()

  object Unknown : NetworkType()

  override fun toString(): String = this::class.simpleName ?: "Unknown"
}
