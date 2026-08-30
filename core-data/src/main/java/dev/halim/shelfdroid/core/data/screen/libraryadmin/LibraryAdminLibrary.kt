package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.toDomain
import dev.halim.shelfdroid.core.MediaType

data class LibraryAdminLibrary(
  val id: String,
  val name: String,
  val mediaType: MediaType,
  val displayOrder: Int,
  val icon: String = "audiobookshelf",
)

/** Converts the server Library payload used by both administration requests and socket events. */
internal fun Library.toAdministrationLibrary(): LibraryAdminLibrary =
  LibraryAdminLibrary(
    id = id,
    name = name,
    mediaType = mediaType.toDomain(),
    displayOrder = displayOrder,
    icon = icon.ifBlank { "audiobookshelf" },
  )
