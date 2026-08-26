package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType

data class LibraryAdministrationLibrary(
  val id: String,
  val name: String,
  val mediaType: LibraryAdministrationMediaType,
  val displayOrder: Int,
)

/** Converts the server Library payload used by both administration requests and socket events. */
internal fun Library.toAdministrationLibrary(): LibraryAdministrationLibrary =
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
