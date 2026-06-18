package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.helper.formatFileSize

internal data class MappedEditItemMedia(
  val mediaKind: EditItemMediaKind,
  val details: DetailsForm,
  val chapters: List<ChapterRow>,
  val episodes: List<EpisodeRow>,
  val episodeUpdate: EpisodeUpdateState,
)

internal object EditItemMapper {

  fun mapMedia(item: LibraryItem): MappedEditItemMedia =
    when (val media = item.media) {
      is Book -> {
        val metadata = media.metadata
        MappedEditItemMedia(
          mediaKind = EditItemMediaKind.Book,
          details =
            DetailsForm(
              title = metadata.title.orEmpty(),
              subtitle = metadata.subtitle.orEmpty(),
              authors = metadata.authors.map { it.name },
              narrators = metadata.narrators,
              series = metadata.series.map { SeriesEntry(it.name, it.sequence.orEmpty()) },
              genres = metadata.genres,
              tags = media.tags,
              publishedYear = metadata.publishedYear.orEmpty(),
              publisher = metadata.publisher.orEmpty(),
              description = metadata.description.orEmpty(),
              isbn = metadata.isbn.orEmpty(),
              asin = metadata.asin.orEmpty(),
              language = metadata.language.orEmpty(),
              explicit = metadata.explicit,
              abridged = false,
            ),
          chapters = media.chapters.map { ChapterRow(it.id, it.title, it.start, it.end) },
          episodes = emptyList(),
          episodeUpdate = EpisodeUpdateState(),
        )
      }

      is Podcast -> {
        val metadata = media.metadata
        MappedEditItemMedia(
          mediaKind = EditItemMediaKind.Podcast,
          details =
            DetailsForm(
              title = metadata.title.orEmpty(),
              genres = metadata.genres,
              tags = media.tags,
              description = metadata.description.orEmpty(),
              language = metadata.language.orEmpty(),
              explicit = metadata.explicit,
              podcastAuthor = metadata.author.orEmpty(),
              rssFeedUrl = metadata.feedUrl.orEmpty(),
              releaseDate = metadata.releaseDate.orEmpty(),
              itunesId = metadata.itunesId?.toString().orEmpty(),
              podcastType = metadata.type?.ifBlank { DEFAULT_PODCAST_TYPE } ?: DEFAULT_PODCAST_TYPE,
            ),
          chapters = emptyList(),
          episodes = mapEpisodes(media.episodes),
          episodeUpdate =
            EpisodeUpdateState(
              persistedCutoffMillis = media.lastEpisodeCheck,
              selectedCutoffMillis = media.lastEpisodeCheck.takeIf { it > 0L },
              limitInput = media.maxNewEpisodesToDownload.toString(),
            ),
        )
      }

      else ->
        MappedEditItemMedia(
          mediaKind = EditItemMediaKind.Book,
          details = DetailsForm(),
          chapters = emptyList(),
          episodes = emptyList(),
          episodeUpdate = EpisodeUpdateState(),
        )
    }

  private fun mapEpisodes(episodes: List<PodcastEpisode>): List<EpisodeRow> =
    episodes
      .mapIndexed { index, episode -> IndexedValue(index, episode) }
      .sortedWith(
        compareByDescending<IndexedValue<PodcastEpisode>> { it.value.publishedAt != null }
          .thenByDescending { it.value.publishedAt ?: Long.MIN_VALUE }
          .thenBy { it.index }
      )
      .map { (_, episode) ->
        EpisodeRow(
          id = episode.id,
          title = episode.title,
          secondaryText =
            listOfNotNull(
                formatDuration(episode.audioTrack.duration).ifBlank { null },
                episode.enclosure
                  ?.length
                  ?.toLongOrNull()
                  ?.takeIf { it > 0 }
                  ?.let { it.formatFileSize() },
              )
              .joinToString(" ∙ "),
        )
      }

  fun buildUpdateRequest(
    mediaKind: EditItemMediaKind,
    original: DetailsForm,
    current: DetailsForm,
  ): UpdateLibraryItemMediaRequest {
    fun <T> delta(orig: T, cur: T): T? = if (cur != orig) cur else null
    fun deltaNames(
      orig: List<String>,
      cur: List<String>,
    ): List<UpdateLibraryItemMediaRequest.NameRef>? =
      if (cur != orig) cur.map { UpdateLibraryItemMediaRequest.NameRef(it) } else null

    fun deltaSeries(
      orig: List<SeriesEntry>,
      cur: List<SeriesEntry>,
    ): List<UpdateLibraryItemMediaRequest.SeriesRef>? =
      if (cur != orig)
        cur.map { UpdateLibraryItemMediaRequest.SeriesRef(it.name, it.sequence.ifBlank { null }) }
      else null

    fun deltaPodcastType(orig: String, cur: String): String? {
      val normalizedOrig = orig.ifBlank { DEFAULT_PODCAST_TYPE }
      val normalizedCur = cur.ifBlank { DEFAULT_PODCAST_TYPE }
      return if (normalizedCur != normalizedOrig) normalizedCur else null
    }

    fun deltaIntString(orig: String, cur: String): Int? {
      val normalizedOrig = orig.trim()
      val normalizedCur = cur.trim()
      return if (normalizedCur != normalizedOrig) normalizedCur.toIntOrNull() else null
    }

    val metadata =
      when (mediaKind) {
        EditItemMediaKind.Book ->
          UpdateLibraryItemMediaRequest.Metadata(
            title = delta(original.title, current.title),
            subtitle = delta(original.subtitle, current.subtitle),
            authors = deltaNames(original.authors, current.authors),
            narrators = delta(original.narrators, current.narrators),
            series = deltaSeries(original.series, current.series),
            genres = delta(original.genres, current.genres),
            publishedYear = delta(original.publishedYear, current.publishedYear),
            publisher = delta(original.publisher, current.publisher),
            description = delta(original.description, current.description),
            isbn = delta(original.isbn, current.isbn),
            asin = delta(original.asin, current.asin),
            language = delta(original.language, current.language),
            explicit = delta(original.explicit, current.explicit),
            abridged = delta(original.abridged, current.abridged),
          )

        EditItemMediaKind.Podcast ->
          UpdateLibraryItemMediaRequest.Metadata(
            title = delta(original.title, current.title),
            author = delta(original.podcastAuthor, current.podcastAuthor),
            genres = delta(original.genres, current.genres),
            description = delta(original.description, current.description),
            releaseDate = delta(original.releaseDate, current.releaseDate),
            feedUrl = delta(original.rssFeedUrl, current.rssFeedUrl),
            itunesId = deltaIntString(original.itunesId, current.itunesId),
            language = delta(original.language, current.language),
            explicit = delta(original.explicit, current.explicit),
            type = deltaPodcastType(original.podcastType, current.podcastType),
          )
      }

    return UpdateLibraryItemMediaRequest(
      metadata = metadata,
      tags = delta(original.tags, current.tags),
    )
  }
}

private const val DEFAULT_PODCAST_TYPE = "episodic"

private fun formatDuration(seconds: Double): String {
  val totalSeconds = seconds.toLong()
  val hours = totalSeconds / 3600
  val minutes = (totalSeconds % 3600) / 60

  return when {
    hours > 0 && minutes > 0 -> "$hours hours $minutes minutes"
    hours > 0 -> "$hours hours"
    minutes > 0 -> "$minutes minutes"
    else -> ""
  }
}
