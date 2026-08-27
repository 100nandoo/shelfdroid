package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType

data class LibraryAdminLibrary(
  val id: String,
  val name: String,
  val mediaType: LibraryAdminMediaType,
  val displayOrder: Int,
)

/** Converts the server Library payload used by both administration requests and socket events. */
internal fun Library.toAdministrationLibrary(): LibraryAdminLibrary =
  LibraryAdminLibrary(
    id = id,
    name = name,
    mediaType =
      when (mediaType) {
        MediaType.BOOK -> LibraryAdminMediaType.BOOK
        MediaType.PODCAST -> LibraryAdminMediaType.PODCAST
        MediaType.UNKNOWN -> LibraryAdminMediaType.UNKNOWN
      },
    displayOrder = displayOrder,
  )
