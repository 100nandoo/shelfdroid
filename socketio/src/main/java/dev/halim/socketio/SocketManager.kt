package dev.halim.socketio

import android.util.Log
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class SocketManager
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  @Named("io") private val appScope: CoroutineScope,
) {

  companion object Event {
    object Episode {
      const val DOWNLOAD_QUEUED = "episode_download_queued"
      const val DOWNLOAD_STARTED = "episode_download_started"
      const val DOWNLOAD_FINISHED = "episode_download_finished"
    }
  }

  private var socket: Socket? = null
  private val listeners = mutableMapOf<String, (Array<Any>) -> Unit>()
  private var observedToken: String? = null

  init {
    appScope.launch {
      dataStoreManager.userPrefs
        .map { it.accessToken }
        .distinctUntilChanged()
        .collect { token ->
          val previousToken = observedToken
          observedToken = token

          if (previousToken == null) {
            return@collect
          }
          if (socket == null) {
            return@collect
          }

          when {
            token.isBlank() -> disconnect()
            previousToken != token -> reconnect()
          }
        }
    }
  }

  fun connect() {
    if (socket?.connected() == true) return

    val baseUrl = dataStoreManager.baseUrl()
    val token = dataStoreManager.accessToken()

    if (baseUrl.isEmpty() || token.isEmpty()) {
      return
    }

    val parsedBaseUrl = AudiobookshelfBaseUrl.parse(baseUrl) ?: return
    val url = parsedBaseUrl.origin

    val options =
      IO.Options.builder()
        .setForceNew(true)
        .setReconnection(true)
        .setReconnectionDelayMax(15000)
        .setUpgrade(false)
        .setQuery("token=$token")
        .setPath(parsedBaseUrl.socketPath())
        .build()

    try {
      socket = IO.socket(url, options)

      socket
        ?.on(Socket.EVENT_CONNECT) {
          socket?.emit("auth", token)
          Log.d("SocketManager", "Connection successful")
        }
        ?.on(Socket.EVENT_DISCONNECT) { Log.d("SocketManager", "Disconnected") }
        ?.on(Socket.EVENT_CONNECT_ERROR) { Log.d("SocketManager", "Connection error") }

      listeners.forEach { (event, listener) -> socket?.on(event, listener) }

      socket?.connect()
    } catch (e: URISyntaxException) {
      Log.d("SocketManager", "Connection creation error ${e.message}")
    }
  }

  fun disconnect() {
    socket?.disconnect()
    socket = null
  }

  private fun reconnect() {
    disconnect()
    connect()
  }

  fun send(event: String, vararg args: Any) {
    socket?.emit(event, *args)
  }

  fun on(event: String, listener: (Array<Any>) -> Unit): SocketManager {
    listeners[event] = listener
    socket?.on(event, listener)
    Log.d("SocketManager", "on: $event")
    return this
  }

  fun off(event: String) {
    listeners.remove(event)?.let { listener -> socket?.off(event, listener) }
  }
}
