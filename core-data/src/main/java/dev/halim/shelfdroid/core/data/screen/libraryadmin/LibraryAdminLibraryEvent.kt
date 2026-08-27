package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Library
import dev.halim.socketio.SocketEvent
import kotlinx.serialization.json.Json

/** The server-side Library events that affect the catalog or administration order. */
enum class LibraryAdminLibraryEventType {
  ADDED,
  UPDATED,
  REMOVED,
  REFRESHED,
}

/**
 * A reconciled Library event. Only the catalog-facing Library projection is exposed here; rich
 * administration settings remain server-backed and are never retained in this event model.
 */
data class LibraryAdminLibraryEvent(
  val type: LibraryAdminLibraryEventType,
  val library: LibraryAdminLibrary? = null,
  val libraries: List<LibraryAdminLibrary> = emptyList(),
  val synchronized: Boolean = true,
  internal val fingerprint: Int = 0,
)

internal fun parseLibraryAdminLibraryEvent(
  eventName: SocketEvent,
  args: Array<Any>,
  json: Json,
): LibraryAdminLibraryEvent? {
  val type =
    when (eventName) {
      SocketEvent.Library.Added -> LibraryAdminLibraryEventType.ADDED
      SocketEvent.Library.Updated -> LibraryAdminLibraryEventType.UPDATED
      SocketEvent.Library.Removed -> LibraryAdminLibraryEventType.REMOVED
      else -> return null
    }
  val payload = args.firstOrNull()?.toString()?.takeIf { it.isNotBlank() } ?: return null
  val library = runCatching { json.decodeFromString<Library>(payload) }.getOrNull() ?: return null
  if (library.id.isBlank()) return null

  return LibraryAdminLibraryEvent(
    type = type,
    library = library.toAdministrationLibrary(),
    // Hash the normalized server model rather than the raw JSON so duplicate deliveries with
    // different whitespace/property ordering still collapse to one reconciliation.
    fingerprint = json.encodeToString(library).hashCode(),
  )
}

internal fun LibraryAdminLibraryEvent.deduplicationKey(): String =
  if (type == LibraryAdminLibraryEventType.REFRESHED) {
    "refresh"
  } else {
    "${type.name}:${library?.id}:${fingerprint}"
  }
