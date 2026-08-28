package dev.halim.socketio

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocketLifecycleTest {
  @Test
  fun subscriptions_toTheSameEvent_areIndependent() {
    val client = FakeSocketClient()
    val registry = SocketSubscriptions()
    var firstCalls = 0
    var secondCalls = 0

    val first = registry.subscribe("task", { firstCalls++ })
    val second = registry.subscribe("task", { secondCalls++ })
    registry.attach(client)
    client.emit("task")

    assertEquals(1, firstCalls)
    assertEquals(1, secondCalls)

    first.close()
    client.emit("task")

    assertEquals(1, firstCalls)
    assertEquals(2, secondCalls)
    second.close()
  }

  @Test
  fun ownership_disconnectsOnlyAfterTheLastOwnerReleases() {
    var connects = 0
    var disconnects = 0
    val ownership = SocketOwnership(onFirstOwner = { connects++ }, onLastOwner = { disconnects++ })

    val first = ownership.acquire()
    val second = ownership.acquire()
    assertEquals(1, connects)
    assertTrue(ownership.hasOwners())

    first.close()
    assertEquals(0, disconnects)
    assertTrue(ownership.hasOwners())

    second.close()
    second.close()
    assertEquals(1, disconnects)
    assertFalse(ownership.hasOwners())
  }

  @Test
  fun replacingTheSocket_rebindsActiveSubscriptionsOnce() {
    val registry = SocketSubscriptions()
    val firstClient = FakeSocketClient()
    val secondClient = FakeSocketClient()
    var calls = 0
    registry.subscribe("task", { calls++ })

    registry.attach(firstClient)
    registry.attach(secondClient)
    registry.attach(secondClient)
    firstClient.emit("task")
    secondClient.emit("task")

    assertEquals(1, calls)
    assertEquals(1, secondClient.listenerCount("task"))
  }

  @Test
  fun addingAndRemovingSubscriptions_whileAttached_updatesTheClientExactlyOnce() {
    val registry = SocketSubscriptions()
    val client = FakeSocketClient()
    registry.attach(client)

    val first = registry.subscribe("task", {})
    val second = registry.subscribe("task", {})
    assertEquals(2, client.listenerCount("task"))

    first.close()
    assertEquals(1, client.listenerCount("task"))
    second.close()
    assertEquals(0, client.listenerCount("task"))
  }

  @Test
  fun rebinding_serializesSubscriptionChanges() {
    val registry = SocketSubscriptions()
    val client = BlockingSocketClient()
    registry.subscribe("task", {})

    val attachThread = thread(start = true) { registry.attach(client) }
    assertTrue(client.onStarted.await(5, SECONDS))

    val subscribeFinished = CountDownLatch(1)
    val subscribeThread =
      thread(start = true) {
        registry.subscribe("task", {})
        subscribeFinished.countDown()
      }
    assertFalse(subscribeFinished.await(100, MILLISECONDS))

    client.allowOn.countDown()
    attachThread.join(5_000)
    subscribeThread.join(5_000)
    assertTrue(subscribeFinished.count == 0L)
    assertEquals(2, client.listenerCount("task"))
  }

  private open class FakeSocketClient : SocketClient {
    private val listeners = mutableMapOf<String, MutableList<SocketEventListener>>()

    override fun connected(): Boolean = true

    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun emit(event: String, vararg args: Any) {
      listeners[event]?.toList()?.forEach { listener -> listener(emptyArray()) }
    }

    open override fun on(event: String, listener: SocketEventListener) {
      listeners.getOrPut(event) { mutableListOf() }.add(listener)
    }

    override fun off(event: String, listener: SocketEventListener) {
      listeners[event]?.removeAll { it === listener }
    }

    fun listenerCount(event: String): Int = listeners[event]?.size ?: 0
  }

  private class BlockingSocketClient : FakeSocketClient() {
    val onStarted = CountDownLatch(1)
    val allowOn = CountDownLatch(1)

    override fun on(event: String, listener: SocketEventListener) {
      onStarted.countDown()
      allowOn.await(5, SECONDS)
      super.on(event, listener)
    }
  }
}
