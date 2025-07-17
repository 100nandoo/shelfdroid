package dev.halim.core.network.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ConnectivityManager @Inject constructor(@ApplicationContext private val context: Context) {

  fun observe(): Flow<ConnectivityStatus> = _status

  fun currentStatus(): ConnectivityStatus {
    _status.value = initialStatus()
    return _status.value
  }

  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _status = MutableStateFlow(initialStatus())

  private val callback =
    object : ConnectivityManager.NetworkCallback() {

      override fun onAvailable(network: Network) {
        _status.value = ConnectivityStatus(false)
      }

      override fun onLost(network: Network) {
        _status.value = ConnectivityStatus(true)
      }
    }

  init {
    val request =
      NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).build()
    connectivityManager.registerNetworkCallback(request, callback)
  }

  private fun initialStatus(): ConnectivityStatus {
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    val isMetered =
      capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)?.not() ?: true
    return ConnectivityStatus(isMetered)
  }
}
