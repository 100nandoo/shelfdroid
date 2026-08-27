package dev.halim.socketio

/** Runs connection callbacks only when the first owner arrives or last owner leaves. */
internal class SocketOwnership(
  private val onFirstOwner: () -> Unit,
  private val onLastOwner: () -> Unit,
) {
  private var nextOwnerId = 0L
  private val owners = mutableSetOf<Long>()

  fun acquire(): AutoCloseable {
    val shouldConnect: Boolean
    val ownerId: Long
    synchronized(this) {
      ownerId = nextOwnerId++
      shouldConnect = owners.isEmpty()
      owners += ownerId
    }
    if (shouldConnect) onFirstOwner()

    return AutoCloseable {
      val shouldDisconnect: Boolean
      synchronized(this) {
        if (!owners.remove(ownerId)) return@AutoCloseable
        shouldDisconnect = owners.isEmpty()
      }
      if (shouldDisconnect) onLastOwner()
    }
  }

  fun hasOwners(): Boolean = synchronized(this) { owners.isNotEmpty() }
}
