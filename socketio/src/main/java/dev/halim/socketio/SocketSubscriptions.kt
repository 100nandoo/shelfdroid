package dev.halim.socketio

/** Tracks event subscriptions independently from the socket connection. */
internal class SocketSubscriptions {
  private data class Subscription(
    val event: String,
    val listener: SocketEventListener,
  )

  private val subscriptions = LinkedHashMap<Long, Subscription>()
  private var nextSubscriptionId = 0L
  private var attachedClient: SocketClient? = null

  fun subscribe(event: String, listener: SocketEventListener): AutoCloseable {
    synchronized(this) {
      val id = nextSubscriptionId++
      subscriptions[id] = Subscription(event, listener)
      attachedClient?.on(event, listener)

      return AutoCloseable {
        synchronized(this) {
          subscriptions.remove(id)?.let { removed ->
            attachedClient?.off(removed.event, removed.listener)
          }
        }
      }
    }
  }

  /** Attaches every active subscription to a newly-created client exactly once. */
  fun attach(client: SocketClient) {
    synchronized(this) {
      if (attachedClient === client) return
      val previousClient = attachedClient
      attachedClient = client
      subscriptions.values.forEach { subscription ->
        previousClient?.off(subscription.event, subscription.listener)
        client.on(subscription.event, subscription.listener)
      }
    }
  }

  fun detach(client: SocketClient) {
    synchronized(this) {
      if (attachedClient !== client) return
      attachedClient = null
      subscriptions.values.forEach { subscription ->
        client.off(subscription.event, subscription.listener)
      }
    }
  }
}
