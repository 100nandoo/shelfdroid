package dev.halim.socketio

import io.socket.client.IO

typealias SocketEventListener = (Array<Any>) -> Unit

/** The part of a Socket.IO client that the shared lifecycle needs. */
interface SocketClient {
  fun connected(): Boolean

  fun connect()

  fun disconnect()

  fun emit(event: String, vararg args: Any)

  fun on(event: String, listener: SocketEventListener)

  fun off(event: String, listener: SocketEventListener)
}

fun interface SocketClientFactory {
  fun create(url: String, options: IO.Options): SocketClient
}
