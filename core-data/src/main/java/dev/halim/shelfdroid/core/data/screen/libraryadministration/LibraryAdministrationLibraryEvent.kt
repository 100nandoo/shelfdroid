package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.core.network.response.Library
import dev.halim.socketio.SocketEvent
import kotlinx.serialization.json.Json

/** The server-side Library events that affect the catalog or administration order. */
enum class LibraryAdministrationLibraryEventType {
  ADDED,
  UPDATED,
  REMOVED,
  REFRESHED,
}

/**
 * A reconciled Library event. Only the catalog-facing Library projection is exposed here; rich
 * administration settings remain server-backed and are never retained in this event model.
 */
data class LibraryAdministrationLibraryEvent(
  val type: LibraryAdministrationLibraryEventType,
  val library: LibraryAdministrationLibrary? = null,
  val libraries: List<LibraryAdministrationLibrary> = emptyList(),
  val synchronized: Boolean = true,
  internal val fingerprint: Int = 0,
)

internal fun parseLibraryAdministrationLibraryEvent(
  eventName: SocketEvent,
  args: Array<Any>,
  json: Json,
): LibraryAdministrationLibraryEvent? {
  val type =
    when (eventName) {
      SocketEvent.Library.Added -> LibraryAdministrationLibraryEventType.ADDED
      SocketEvent.Library.Updated -> LibraryAdministrationLibraryEventType.UPDATED
      SocketEvent.Library.Removed -> LibraryAdministrationLibraryEventType.REMOVED
      else -> return null
    }
  val payload = args.firstOrNull()?.toString()?.takeIf { it.isNotBlank() } ?: return null
  val library = runCatching { json.decodeFromString<Library>(payload) }.getOrNull() ?: return null
  if (library.id.isBlank()) return null

  return LibraryAdministrationLibraryEvent(
    type = type,
    library = library.toAdministrationLibrary(),
    // Hash the normalized server model rather than the raw JSON so duplicate deliveries with
    // different whitespace/property ordering still collapse to one reconciliation.
    fingerprint = json.encodeToString(library).hashCode(),
  )
}

internal fun LibraryAdministrationLibraryEvent.deduplicationKey(): String =
  if (type == LibraryAdministrationLibraryEventType.REFRESHED) {
    "refresh"
  } else {
    "${type.name}:${library?.id}:${fingerprint}"
  }
