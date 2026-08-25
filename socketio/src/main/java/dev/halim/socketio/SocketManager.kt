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

  /** Keeps the pre-lease constructor available to non-Hilt callers. */
  constructor(dataStoreManager: DataStoreManager, appScope: CoroutineScope) :
    this(
      dataStoreManager,
      appScope,
      SocketClientFactory { url, options -> IoSocketClient(IO.socket(url, options)) },
    )

  companion object Event {
    object Episode {
      const val DOWNLOAD_QUEUED = "episode_download_queued"
      const val DOWNLOAD_STARTED = "episode_download_started"
      const val DOWNLOAD_FINISHED = "episode_download_finished"
    }
  }

  /** An owner keeps the shared socket alive until its handle is closed. */
  fun interface Owner : AutoCloseable {
    override fun close()
  }

  /** A single event subscription that can be removed without affecting other consumers. */
  fun interface Subscription : AutoCloseable {
    override fun close()
  }

  private val lock = Any()
  private val subscriptions = SocketSubscriptionRegistry()
  private val ownership = SocketOwnership(::connectSocket, ::disconnectSocket)
  private val legacySubscriptions = mutableMapOf<String, Subscription>()
  private var socket: SocketClient? = null
  private var observedToken: String? = null
  private var legacyOwner: Owner? = null

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
  fun subscribe(event: String, listener: SocketEventListener): Subscription {
    val handle = subscriptions.subscribe(event, listener)
    return Subscription { handle.close() }
  }

  /**
   * Compatibility entry point for consumers that still use the old connect/on/off API.
   * New consumers should use [acquire] and [subscribe].
   */
  fun connect() {
    synchronized(lock) {
      if (legacyOwner != null) return
      legacyOwner = acquire()
    }
  }

  /** Releases only this manager's compatibility owner; other owners remain connected. */
  fun disconnect() {
    val owner = synchronized(lock) {
      val current = legacyOwner
      legacyOwner = null
      current
    }
    owner?.close()
  }

  fun send(event: String, vararg args: Any) {
    currentSocket()?.emit(event, *args)
  }

  /** Returns the current shared connection state without taking ownership. */
  fun isConnected(): Boolean = currentSocket()?.connected() == true

  /** Compatibility listener registration. Re-registering this legacy event replaces only itself. */
  fun on(event: String, listener: SocketEventListener): SocketManager {
    synchronized(lock) {
      legacySubscriptions.remove(event)?.close()
      legacySubscriptions[event] = subscribe(event, listener)
    }
    Log.d("SocketManager", "on: $event")
    return this
  }

  /** Removes only a listener registered through the compatibility [on] method. */
  fun off(event: String) {
    synchronized(lock) { legacySubscriptions.remove(event)?.close() }
  }

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
