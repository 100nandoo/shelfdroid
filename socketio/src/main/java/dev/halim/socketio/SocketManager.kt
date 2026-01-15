package dev.halim.socketio

import android.util.Log
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(private val dataStoreManager: DataStoreManager) {

  companion object Event {
    object Episode {
      const val DOWNLOAD_QUEUED = "episode_download_queued"
      const val DOWNLOAD_STARTED = "episode_download_started"
      const val DOWNLOAD_FINISHED = "episode_download_finished"
    }
  }

  private var socket: Socket? = null
  private val listeners = mutableMapOf<String, (Array<Any>) -> Unit>()

  fun connect() {
    if (socket?.connected() == true) return

    val baseUrl = dataStoreManager.baseUrl()
    val token = dataStoreManager.accessToken()

    if (baseUrl.isEmpty() || token.isEmpty()) {
      return
    }

    val url = "https://$baseUrl"

    val options =
      IO.Options.builder()
        .setForceNew(true)
        .setReconnection(true)
        .setReconnectionDelayMax(15000)
        .setUpgrade(false)
        .setQuery("token=$token")
        .setPath("/socket.io")
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
