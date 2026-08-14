package dev.halim.core.network

import dev.halim.core.network.response.LibraryItemsResponse

internal enum class LibraryItemsSortKey(val queryValue: String) {
  ADDED_AT("addedAt"),
  SIZE("size"),
  BIRTHTIME_MS("birthtimeMs"),
  MTIME_MS("mtimeMs"),
  DURATION("media.duration"),
  PUBLISHED_YEAR("media.metadata.publishedYear"),
  AUTHOR_NAME_LAST_FIRST("media.metadata.authorNameLF"),
  AUTHOR_NAME("media.metadata.authorName"),
  PODCAST_AUTHOR("media.metadata.author"),
  TITLE("media.metadata.title"),
  SEQUENCE("sequence"),
  PROGRESS("progress"),
  PROGRESS_CREATED_AT("progress.createdAt"),
  PROGRESS_FINISHED_AT("progress.finishedAt"),
  NUM_TRACKS("media.numTracks"),
  RANDOM("random"),
}

sealed interface LibraryItemsSort {
  enum class Book(internal val queryKey: LibraryItemsSortKey) : LibraryItemsSort {
    AddedAt(LibraryItemsSortKey.ADDED_AT),
    Size(LibraryItemsSortKey.SIZE),
    BirthtimeMs(LibraryItemsSortKey.BIRTHTIME_MS),
    MtimeMs(LibraryItemsSortKey.MTIME_MS),
    Duration(LibraryItemsSortKey.DURATION),
    PublishedYear(LibraryItemsSortKey.PUBLISHED_YEAR),
    AuthorNameLastFirst(LibraryItemsSortKey.AUTHOR_NAME_LAST_FIRST),
    AuthorName(LibraryItemsSortKey.AUTHOR_NAME),
    Title(LibraryItemsSortKey.TITLE),
    Sequence(LibraryItemsSortKey.SEQUENCE),
    Progress(LibraryItemsSortKey.PROGRESS),
    ProgressCreatedAt(LibraryItemsSortKey.PROGRESS_CREATED_AT),
    ProgressFinishedAt(LibraryItemsSortKey.PROGRESS_FINISHED_AT),
    Random(LibraryItemsSortKey.RANDOM);

    override fun toString(): String = queryKey.queryValue
  }

  enum class Podcast(internal val queryKey: LibraryItemsSortKey) : LibraryItemsSort {
    AddedAt(LibraryItemsSortKey.ADDED_AT),
    Size(LibraryItemsSortKey.SIZE),
    BirthtimeMs(LibraryItemsSortKey.BIRTHTIME_MS),
    MtimeMs(LibraryItemsSortKey.MTIME_MS),
    Author(LibraryItemsSortKey.PODCAST_AUTHOR),
    Title(LibraryItemsSortKey.TITLE),
    NumTracks(LibraryItemsSortKey.NUM_TRACKS),
    Random(LibraryItemsSortKey.RANDOM);

    override fun toString(): String = queryKey.queryValue
  }
}

data class LibraryItemsQuery(
  val limit: Int? = null,
  val page: Int? = null,
  val minified: Boolean? = null,
  val sort: LibraryItemsSort? = null,
  val desc: Boolean? = null,
)

internal fun Boolean?.toAudiobookshelfFlag(): Int? {
  return when (this) {
    true -> 1
    false -> 0
    null -> null
  }
}

suspend fun ApiService.libraryItems(
  libraryId: String,
  query: LibraryItemsQuery,
): Result<LibraryItemsResponse> {
  return libraryItems(
    libraryId = libraryId,
    limit = query.limit,
    page = query.page,
    minified = query.minified.toAudiobookshelfFlag(),
    sort = query.sort,
    desc = query.desc.toAudiobookshelfFlag(),
  )
}
