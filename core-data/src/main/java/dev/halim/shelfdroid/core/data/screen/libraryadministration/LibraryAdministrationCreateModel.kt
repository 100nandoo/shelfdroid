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

/** Settings shared by the Audiobookshelf book-library create form. */
data class LibraryAdministrationBookSettings(
  val coverAspectRatio: Int = SQUARE_COVER_ASPECT_RATIO,
  val disableWatcher: Boolean = false,
  val audiobooksOnly: Boolean = false,
  val skipMatchingMediaWithAsin: Boolean = false,
  val skipMatchingMediaWithIsbn: Boolean = false,
  val epubsAllowScriptedContent: Boolean = false,
  val hideSingleBookSeries: Boolean = false,
  val onlyShowLaterBooksInContinueSeries: Boolean = false,
  val markAsFinishedPercentComplete: Int? = null,
  val markAsFinishedTimeRemaining: Int? = DEFAULT_FINISH_TIME_REMAINING,
)

/** Settings shared by the Audiobookshelf podcast-library create form. */
data class LibraryAdministrationPodcastSettings(
  val coverAspectRatio: Int = SQUARE_COVER_ASPECT_RATIO,
  val disableWatcher: Boolean = false,
  val podcastSearchRegion: String = DEFAULT_PODCAST_SEARCH_REGION,
  val markAsFinishedPercentComplete: Int? = null,
  val markAsFinishedTimeRemaining: Int? = DEFAULT_FINISH_TIME_REMAINING,
)

/**
 * The mutually exclusive finish threshold used by both media types.
 *
 * Draft settings retain the two server fields because that is the shape accepted by the
 * Audiobookshelf API. Transitions through this model always write only the selected field, while
 * using the other field as the initial value when changing modes.
 */
data class LibraryAdministrationFinishThreshold(
  val percentComplete: Int? = null,
  val timeRemaining: Int? = DEFAULT_FINISH_TIME_REMAINING,
) {
  /** The value shown by the form for the currently selected mode. */
  val value: Int
    get() = percentComplete ?: timeRemaining ?: DEFAULT_FINISH_TIME_REMAINING

  /** Returns a threshold with only the requested mode populated. */
  fun selectMode(percentCompleteMode: Boolean): LibraryAdministrationFinishThreshold {
    val selectedValue =
      if (percentCompleteMode) {
        percentComplete ?: timeRemaining ?: DEFAULT_FINISH_TIME_REMAINING
      } else {
        timeRemaining ?: percentComplete ?: DEFAULT_FINISH_TIME_REMAINING
      }
    return if (percentCompleteMode) {
      copy(percentComplete = selectedValue, timeRemaining = null)
    } else {
      copy(percentComplete = null, timeRemaining = selectedValue)
    }
  }

  /** Returns a threshold value for whichever mode is currently selected. */
  fun updateValue(value: Int): LibraryAdministrationFinishThreshold {
    val normalized = value.coerceAtLeast(0)
    return if (percentComplete != null) {
      copy(percentComplete = normalized, timeRemaining = null)
    } else {
      copy(percentComplete = null, timeRemaining = normalized)
    }
  }

  /** Values sent to the API, normalized if a draft was constructed with both fields populated. */
  val serializedPercentComplete: Int?
    get() = percentComplete

  val serializedTimeRemaining: Int?
    get() = timeRemaining.takeIf { percentComplete == null }
}

val LibraryAdministrationBookSettings.finishThreshold: LibraryAdministrationFinishThreshold
  get() =
    LibraryAdministrationFinishThreshold(
      percentComplete = markAsFinishedPercentComplete,
      timeRemaining = markAsFinishedTimeRemaining,
    )

fun LibraryAdministrationBookSettings.withFinishThreshold(
  threshold: LibraryAdministrationFinishThreshold
): LibraryAdministrationBookSettings =
  copy(
    markAsFinishedPercentComplete = threshold.serializedPercentComplete,
    markAsFinishedTimeRemaining = threshold.serializedTimeRemaining,
  )

val LibraryAdministrationPodcastSettings.finishThreshold: LibraryAdministrationFinishThreshold
  get() =
    LibraryAdministrationFinishThreshold(
      percentComplete = markAsFinishedPercentComplete,
      timeRemaining = markAsFinishedTimeRemaining,
    )

fun LibraryAdministrationPodcastSettings.withFinishThreshold(
  threshold: LibraryAdministrationFinishThreshold
): LibraryAdministrationPodcastSettings =
  copy(
    markAsFinishedPercentComplete = threshold.serializedPercentComplete,
    markAsFinishedTimeRemaining = threshold.serializedTimeRemaining,
  )

/** One of the six metadata sources supported by Audiobookshelf's book scanner. */
data class LibraryAdministrationMetadataSource(
  val id: String,
  val name: String,
  val enabled: Boolean = true,
)

const val SQUARE_COVER_ASPECT_RATIO = 1
const val DEFAULT_FINISH_TIME_REMAINING = 10
const val DEFAULT_PODCAST_SEARCH_REGION = "us"

/**
 * The website renders these rows highest-first, while the server payload stores scanner
 * application order lowest-first. The draft keeps the website display order and reverses enabled
 * rows only when serializing.
 */
val DEFAULT_LIBRARY_METADATA_SOURCES: List<LibraryAdministrationMetadataSource> =
  listOf(
      LibraryAdministrationMetadataSource("folderStructure", "Folder structure"),
      LibraryAdministrationMetadataSource(
        "audioMetatags",
        "Audio file meta tags OR ebook metadata",
      ),
      LibraryAdministrationMetadataSource("nfoFile", "NFO file"),
      LibraryAdministrationMetadataSource("txtFiles", "desc.txt & reader.txt files"),
      LibraryAdministrationMetadataSource("opfFile", "OPF file"),
      LibraryAdministrationMetadataSource("absMetadata", "Audiobookshelf metadata file"),
    )
    .asReversed()

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
  val bookSettings: LibraryAdministrationBookSettings = LibraryAdministrationBookSettings(),
  val podcastSettings: LibraryAdministrationPodcastSettings = LibraryAdministrationPodcastSettings(),
  val schedule: LibraryAdministrationScheduleDraft = LibraryAdministrationScheduleDraft(),
  /** Rows are kept in the website's displayed (highest-priority first) order. */
  val metadataSources: List<LibraryAdministrationMetadataSource> =
    DEFAULT_LIBRARY_METADATA_SOURCES,
) {
  val provider: String?
    get() = if (mediaType == LibraryAdministrationMediaType.PODCAST) podcastProvider else bookProvider

  /** Returns the server payload order (lowest application priority first). */
  val metadataPrecedence: List<String>
    get() = metadataSources.filter { it.enabled }.asReversed().map { it.id }

  /** Website priority number for an enabled source, where 1 is the highest priority. */
  fun metadataPriority(id: String): Int? =
    metadataSources.filter { it.enabled }.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)

  fun withMediaType(value: LibraryAdministrationMediaType): LibraryAdministrationDraft =
    copy(mediaType = value)

  /** Applies the same finish-threshold transition to whichever media type is active. */
  fun withFinishThreshold(
    update: (LibraryAdministrationFinishThreshold) -> LibraryAdministrationFinishThreshold
  ): LibraryAdministrationDraft =
    when (mediaType) {
      LibraryAdministrationMediaType.BOOK ->
        copy(bookSettings = bookSettings.withFinishThreshold(update(bookSettings.finishThreshold)))
      LibraryAdministrationMediaType.PODCAST ->
        copy(
          podcastSettings =
            podcastSettings.withFinishThreshold(update(podcastSettings.finishThreshold))
        )
      LibraryAdministrationMediaType.UNKNOWN -> this
    }

  fun withProvider(value: String?): LibraryAdministrationDraft =
    if (mediaType == LibraryAdministrationMediaType.PODCAST) copy(podcastProvider = value)
    else copy(bookProvider = value)

  fun withMetadataSource(id: String, enabled: Boolean): LibraryAdministrationDraft =
    copy(
      metadataSources =
        metadataSources.map { source ->
          if (source.id == id) source.copy(enabled = enabled) else source
        }
    )

  fun moveMetadataSource(id: String, delta: Int): LibraryAdministrationDraft {
    val index = metadataSources.indexOfFirst { it.id == id }
    if (index < 0) return this
    val destination = (index + delta).coerceIn(0, metadataSources.lastIndex)
    if (index == destination) return this
    val reordered = metadataSources.toMutableList()
    val source = reordered.removeAt(index)
    reordered.add(destination, source)
    return copy(metadataSources = reordered)
  }

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
