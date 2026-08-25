package dev.halim.shelfdroid.core.data.screen.libraryadministration

data class LibraryAdministrationLibrary(
  val id: String,
  val name: String,
  val mediaType: LibraryAdministrationMediaType,
  val displayOrder: Int,
)
