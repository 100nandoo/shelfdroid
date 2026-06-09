package dev.halim.shelfdroid.core.ui.screen.addepisode

import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import java.util.Calendar
import java.util.TimeZone

fun filter(episodes: List<AddEpisode>, filterState: AddEpisodeFilterState): List<AddEpisode> {
  val titleQuery = filterState.titleQuery.trim()
  val publishedStartDate = filterState.publishedStartDateMillis?.toPickerDate()
  val publishedEndDate = filterState.publishedEndDateMillis?.toPickerDate()

  return episodes.filter { episode ->
    val passesDownloaded =
      !filterState.hideDownloaded || episode.state != AddEpisodeDownloadState.Downloaded

    val passesTitle = titleQuery.isBlank() || episode.title.contains(titleQuery, ignoreCase = true)

    val publishedDate = episode.publishedAt.toPublishedDate()
    val passesPublishedDate =
      when {
        publishedStartDate != null && publishedEndDate != null ->
          publishedDate in publishedStartDate..publishedEndDate

        publishedStartDate != null -> publishedDate >= publishedStartDate

        publishedEndDate != null -> publishedDate <= publishedEndDate

        else -> true
      }

    passesDownloaded && passesTitle && passesPublishedDate
  }
}

private fun Long.toPublishedDate(): Int = toDateKey(TimeZone.getDefault())

private fun Long.toPickerDate(): Int = toDateKey(TimeZone.getTimeZone("UTC"))

private fun Long.toDateKey(timeZone: TimeZone): Int {
  val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = this@toDateKey }
  val year = calendar.get(Calendar.YEAR)
  val month = calendar.get(Calendar.MONTH) + 1
  val day = calendar.get(Calendar.DAY_OF_MONTH)
  return year * 10_000 + month * 100 + day
}
