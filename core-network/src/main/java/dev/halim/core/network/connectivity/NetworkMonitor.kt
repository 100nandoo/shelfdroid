package dev.halim.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _status = MutableStateFlow(initialStatus())
  val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

  private val callback =
    object : ConnectivityManager.NetworkCallback() {

      override fun onAvailable(network: Network) {
        updateStatus()
      }

      override fun onLost(network: Network) {
        updateStatus()
      }

      override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities,
      ) {
        updateStatus(networkCapabilities)
      }
    }

  init {
    connectivityManager.registerDefaultNetworkCallback(callback)
    updateStatus()
  }

  private fun updateStatus(capabilities: NetworkCapabilities? = null) {
    val currentCapabilities =
      capabilities ?: connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    val hasInternet =
      currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val isMetered =
      currentCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)?.not()
        ?: true

    _status.update {
      if (it.hasInternet != hasInternet || it.isMetered != isMetered) {
        it.copy(hasInternet = hasInternet, isMetered = isMetered)
      } else {
        it
      }
    }
  }

  fun initialStatus(): ConnectivityStatus {
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    val isMetered =
      capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)?.not() ?: true
    val hasInternet =
      capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    return ConnectivityStatus(isMetered = isMetered, hasInternet = hasInternet)
  }
}
