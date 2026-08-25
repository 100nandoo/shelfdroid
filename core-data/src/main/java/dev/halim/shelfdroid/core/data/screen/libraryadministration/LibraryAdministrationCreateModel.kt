package dev.halim.shelfdroid.core.data.screen.libraryadministration

/** A provider exposed by the Audiobookshelf server for a library media type. */
data class LibraryAdministrationProvider(
  val id: String,
  val name: String,
)

/** A directory returned by the server filesystem browser. */
data class LibraryAdministrationDirectory(
  val path: String,
  val name: String,
  val level: Int,
)

data class LibraryAdministrationFilesystem(
  val isPosix: Boolean,
  val directories: List<LibraryAdministrationDirectory>,
)

/**
 * Draft values for the reusable create/edit library Details section.
 *
 * Provider choices are kept per media type so switching between Book and Podcast does not discard
 * the hidden media-specific value.
 */
data class LibraryAdministrationDraft(
  val mediaType: LibraryAdministrationMediaType = LibraryAdministrationMediaType.BOOK,
  val name: String = "",
  val icon: String = DEFAULT_LIBRARY_ICON,
  val folders: List<String> = emptyList(),
  val bookProvider: String? = null,
  val podcastProvider: String? = null,
) {
  val provider: String?
    get() = if (mediaType == LibraryAdministrationMediaType.PODCAST) podcastProvider else bookProvider

  fun withMediaType(value: LibraryAdministrationMediaType): LibraryAdministrationDraft =
    copy(mediaType = value)

  fun withProvider(value: String?): LibraryAdministrationDraft =
    if (mediaType == LibraryAdministrationMediaType.PODCAST) copy(podcastProvider = value)
    else copy(bookProvider = value)

  companion object {
    const val DEFAULT_LIBRARY_ICON = "audiobookshelf"

    /** IDs supported by Audiobookshelf's MediaIconPicker. */
    val ICON_IDS =
      listOf(
        "database",
        "audiobookshelf",
        "books-1",
        "books-2",
        "book-1",
        "microphone-1",
        "microphone-3",
        "radio",
        "podcast",
        "rss",
        "headphones",
        "music",
        "file-picture",
        "rocket",
        "power",
        "star",
        "heart",
      )
  }
}

sealed interface LibraryAdministrationCreateResult {
  data class Created(val library: LibraryAdministrationLibrary) : LibraryAdministrationCreateResult

  /** The server accepted the mutation, but local Library data synchronization failed. */
  data class CreatedButNotSynchronized(
    val library: LibraryAdministrationLibrary,
    val error: Throwable,
  ) : LibraryAdministrationCreateResult
}

const val DEFAULT_LIBRARY_ICON = LibraryAdministrationDraft.DEFAULT_LIBRARY_ICON
