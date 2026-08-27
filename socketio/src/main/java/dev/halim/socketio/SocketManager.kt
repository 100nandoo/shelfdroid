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
  private val socketClientFactory: SocketClientFactory,
) {

  /** An owner keeps the shared socket alive until its handle is closed. */
  fun interface Owner : AutoCloseable {
    override fun close()
  }

  /** A single event subscription that can be removed without affecting other consumers. */
  fun interface Subscription : AutoCloseable {
    override fun close()
  }

  private val lock = Any()
  private val subscriptions = SocketSubscriptions()
  private val ownership = SocketOwnership(::connectSocket, ::disconnectSocket)
  private var socket: SocketClient? = null
  private var observedToken: String? = null

  init {
    appScope.launch {
      dataStoreManager.userPrefs
        .map { it.accessToken }
        .distinctUntilChanged()
        .collect { token ->
          val previousToken = synchronized(lock) {
            val previous = observedToken
            observedToken = token
            previous
          }

          if (previousToken == null) return@collect

          when {
            token.isBlank() -> disconnectSocket()
            previousToken != token && ownership.hasOwners() -> reconnectSocket()
            token.isNotBlank() && ownership.hasOwners() && currentSocket() == null ->
              connectSocket()
          }
        }
    }
  }

  /** Acquires shared connection ownership. Releasing this owner cannot affect other owners. */
  fun acquire(): Owner {
    val handle = ownership.acquire()
    return Owner { handle.close() }
  }

  /** Registers an independent event subscription. It remains active across reconnects. */
  fun subscribe(event: SocketEvent, listener: SocketEventListener): Subscription {
    val handle = subscriptions.subscribe(event.name, listener)
    return Subscription { handle.close() }
  }

  /** Returns the current shared connection state without taking ownership. */
  fun isConnected(): Boolean = currentSocket()?.connected() == true

  private fun currentSocket(): SocketClient? = synchronized(lock) { socket }

  private fun connectSocket() {
    val client: SocketClient
    synchronized(lock) {
      socket?.let { existing ->
        if (!existing.connected()) existing.connect()
        return
      }

      val baseUrl = dataStoreManager.baseUrl()
      val token = dataStoreManager.accessToken()
      if (baseUrl.isEmpty() || token.isEmpty()) return

      val parsedBaseUrl = AudiobookshelfBaseUrl.parse(baseUrl) ?: return
      val options =
        IO.Options.builder()
          .setForceNew(true)
          .setReconnection(true)
          .setReconnectionDelayMax(15000)
          .setUpgrade(false)
          .setQuery("token=$token")
          .setPath(parsedBaseUrl.socketPath())
          .build()

      client = try {
        socketClientFactory.create(parsedBaseUrl.origin, options)
      } catch (e: URISyntaxException) {
        Log.d("SocketManager", "Connection creation error ${e.message}")
        return
      }
      socket = client
      subscriptions.attach(client)

      client.on(Socket.EVENT_CONNECT) {
        client.emit("auth", dataStoreManager.accessToken())
        Log.d("SocketManager", "Connection successful")
      }
      client.on(Socket.EVENT_DISCONNECT) { Log.d("SocketManager", "Disconnected") }
      client.on(Socket.EVENT_CONNECT_ERROR) { Log.d("SocketManager", "Connection error") }
    }
    client.connect()
  }

  private fun disconnectSocket() {
    val client = synchronized(lock) {
      val current = socket ?: return
      socket = null
      current
    }
    subscriptions.detach(client)
    client.disconnect()
  }

  private fun reconnectSocket() {
    disconnectSocket()
    if (ownership.hasOwners()) connectSocket()
  }
}
