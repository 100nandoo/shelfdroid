package dev.halim.shelfdroid.core.data.task

import dev.halim.socketio.SocketEvent
import dev.halim.socketio.SocketManager
import javax.inject.Inject

interface ServerTaskSocket {
  fun isConnected(): Boolean = false

  fun acquire(): AutoCloseable

  fun subscribe(event: SocketEvent, listener: (Array<Any>) -> Unit): AutoCloseable
}

class SocketManagerServerTaskSocket @Inject constructor(private val manager: SocketManager) :
  ServerTaskSocket {
  override fun isConnected(): Boolean = manager.isConnected()

  override fun acquire(): AutoCloseable = manager.acquire()

  override fun subscribe(event: SocketEvent, listener: (Array<Any>) -> Unit): AutoCloseable =
    manager.subscribe(event, listener)
}
