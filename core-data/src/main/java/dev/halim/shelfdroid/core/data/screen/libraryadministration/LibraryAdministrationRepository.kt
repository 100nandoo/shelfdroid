package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import javax.inject.Inject

class LibraryAdministrationRepository
@Inject
constructor(private val libraryRepository: LibraryRepository) : LibraryAdministrationContract {

  override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> {
    return libraryRepository.fetchLibraries().map { libraries ->
      libraries.map { library -> library.toAdministrationLibrary() }
    }
  }
}

private fun Library.toAdministrationLibrary(): LibraryAdministrationLibrary =
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
