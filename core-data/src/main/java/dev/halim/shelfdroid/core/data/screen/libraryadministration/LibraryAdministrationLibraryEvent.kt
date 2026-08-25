package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType
import dev.halim.socketio.SocketManager.Event.Library as LibrarySocketEvent
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
  eventName: String,
  args: Array<Any>,
  json: Json,
): LibraryAdministrationLibraryEvent? {
  val type =
    when (eventName) {
      LibrarySocketEvent.ADDED -> LibraryAdministrationLibraryEventType.ADDED
      LibrarySocketEvent.UPDATED -> LibraryAdministrationLibraryEventType.UPDATED
      LibrarySocketEvent.REMOVED -> LibraryAdministrationLibraryEventType.REMOVED
      else -> return null
    }
  val payload = args.firstOrNull()?.toString()?.takeIf { it.isNotBlank() } ?: return null
  val library = runCatching { json.decodeFromString<Library>(payload) }.getOrNull() ?: return null
  if (library.id.isBlank()) return null

  return LibraryAdministrationLibraryEvent(
    type = type,
    library = library.toAdministrationProjection(),
    // Hash the normalized server model rather than the raw JSON so duplicate deliveries with
    // different whitespace/property ordering still collapse to one reconciliation.
    fingerprint = json.encodeToString(library).hashCode(),
  )
}

internal fun Library.toAdministrationProjection(): LibraryAdministrationLibrary =
  LibraryAdministrationLibrary(
    id = id,
    name = name,
    mediaType =
      when (mediaType) {
        MediaType.BOOK -> LibraryAdministrationMediaType.BOOK
        MediaType.PODCAST -> LibraryAdministrationMediaType.PODCAST
        MediaType.UNKNOWN -> LibraryAdministrationMediaType.UNKNOWN
      },
    displayOrder = displayOrder,
  )

internal fun LibraryAdministrationLibraryEvent.deduplicationKey(): String =
  if (type == LibraryAdministrationLibraryEventType.REFRESHED) {
    "refresh"
  } else {
    "${type.name}:${library?.id}:${fingerprint}"
  }
