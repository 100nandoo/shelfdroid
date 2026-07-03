package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.request.UpdatePodcastEpisodeRequest
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object EditEpisodeMapper {

  fun mapState(
    itemId: String,
    episodeId: String,
    podcastTitle: String,
    episode: PodcastEpisode,
  ): EditEpisodeUiState {
    val details = mapDetails(episode)
    return EditEpisodeUiState(
      state = GenericState.Success,
      itemId = itemId,
      episodeId = episodeId,
      podcastTitle = podcastTitle,
      details = details,
      originalDetails = details,
    )
  }

  fun mapDetails(episode: PodcastEpisode): EpisodeDetailsForm =
    EpisodeDetailsForm(
      season = episode.season.orEmpty(),
      episode = episode.episode.orEmpty(),
      episodeType = episode.episodeType.orEmpty(),
      publishedAtMillis = parsePublishedAtMillis(episode.pubDate, episode.publishedAt),
      title = episode.title,
      subtitle = episode.subtitle.orEmpty(),
      description = episode.description.orEmpty(),
      enclosureUrl = episode.enclosure?.url.orEmpty(),
    )

  fun buildUpdateRequest(
    original: EpisodeDetailsForm,
    current: EpisodeDetailsForm,
  ): UpdatePodcastEpisodeRequest? {
    val normalizedOriginal = original.normalized()
    val normalizedCurrent = current.normalized()
    val publishedDateChanged =
      normalizedCurrent.publishedAtMillis != normalizedOriginal.publishedAtMillis

    return UpdatePodcastEpisodeRequest(
      season = delta(normalizedOriginal.season, normalizedCurrent.season),
      episode = delta(normalizedOriginal.episode, normalizedCurrent.episode),
      episodeType = delta(normalizedOriginal.episodeType, normalizedCurrent.episodeType),
      title = delta(normalizedOriginal.title, normalizedCurrent.title),
      subtitle = delta(normalizedOriginal.subtitle, normalizedCurrent.subtitle),
      description = delta(normalizedOriginal.description, normalizedCurrent.description),
      pubDate =
        if (publishedDateChanged) normalizedCurrent.publishedAtMillis?.let(::formatPubDate) else null,
      publishedAt = if (publishedDateChanged) normalizedCurrent.publishedAtMillis else null,
    ).takeUnless { it.isEmpty() }
  }

  internal fun parsePublishedAtMillis(pubDate: String?, publishedAt: Long?): Long? =
    pubDate
      ?.takeIf { it.isNotBlank() }
      ?.let(::parsePubDateMillis)
      ?: publishedAt

  internal fun formatPubDate(millis: Long): String =
    pubDateFormatter().format(Date(millis))

  private fun EpisodeDetailsForm.normalized(): EpisodeDetailsForm =
    copy(
      season = season.trim(),
      episode = episode.trim(),
      episodeType = episodeType.trim(),
      title = title.trim(),
      subtitle = subtitle.trim(),
    )

  private fun parsePubDateMillis(pubDate: String): Long? =
    runCatching {
      ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
        .toInstant()
        .toEpochMilli()
    }.getOrNull()

  private fun delta(original: String, current: String): String? = current.takeIf { it != original }

  private fun pubDateFormatter() =
    SimpleDateFormat(PUB_DATE_PATTERN, Locale.ENGLISH).apply {
      timeZone = TimeZone.getDefault()
    }
}

private fun UpdatePodcastEpisodeRequest.isEmpty(): Boolean =
  season == null &&
    episode == null &&
    episodeType == null &&
    title == null &&
    subtitle == null &&
    description == null &&
    pubDate == null &&
    publishedAt == null

private const val PUB_DATE_PATTERN = "EEE, d MMM yyyy HH:mm:ssZ"
