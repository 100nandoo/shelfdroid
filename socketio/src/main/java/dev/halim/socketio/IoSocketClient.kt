package dev.halim.socketio

import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.util.IdentityHashMap

internal class IoSocketClient(private val socket: Socket) : SocketClient {
  private val listenerLock = Any()
  private val listenerAdapters =
    mutableMapOf<String, IdentityHashMap<SocketEventListener, Emitter.Listener>>()

  override fun connected(): Boolean = socket.connected()

  override fun connect() {
    socket.connect()
  }

  override fun disconnect() {
    socket.disconnect()
  }

  override fun emit(event: String, vararg args: Any) {
    socket.emit(event, *args)
  }

  override fun on(event: String, listener: SocketEventListener) {
    synchronized(listenerLock) {
      val adapters = listenerAdapters.getOrPut(event) { IdentityHashMap() }
      if (adapters.containsKey(listener)) return

      val adapter = Emitter.Listener { args -> listener(args) }
      adapters[listener] = adapter
      socket.on(event, adapter)
    }
  }

  override fun off(event: String, listener: SocketEventListener) {
    synchronized(listenerLock) {
      listenerAdapters[event]?.remove(listener)?.let { adapter -> socket.off(event, adapter) }
    }
  }
}
