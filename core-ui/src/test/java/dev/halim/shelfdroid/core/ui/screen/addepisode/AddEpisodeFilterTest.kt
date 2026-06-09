package dev.halim.shelfdroid.core.ui.screen.addepisode

import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddEpisodeFilterTest {
  private lateinit var originalTimeZone: TimeZone

  @Before
  fun setUp() {
    originalTimeZone = TimeZone.getDefault()
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  @After
  fun tearDown() {
    TimeZone.setDefault(originalTimeZone)
  }

  @Test
  fun filters_by_title_and_inclusive_published_date_range() {
    val episodes =
      listOf(
        addEpisode(title = "Android Weekly", year = 2024, month = 6, day = 1),
        addEpisode(title = "Kotlin Roundup", year = 2024, month = 6, day = 2),
        addEpisode(title = "Android Q&A", year = 2024, month = 6, day = 3),
      )

    val filtered =
      filter(
        episodes = episodes,
        filterState =
          AddEpisodeFilterState(
            titleQuery = "android",
            publishedStartDateMillis = pickerDateMillis(year = 2024, month = 6, day = 1),
            publishedEndDateMillis = pickerDateMillis(year = 2024, month = 6, day = 2),
          ),
      )

    assertEquals(listOf("Android Weekly"), filtered.map(AddEpisode::title))
  }

  @Test
  fun filters_by_end_date_only() {
    val episodes =
      listOf(
        addEpisode(title = "Episode 1", year = 2024, month = 5, day = 31),
        addEpisode(title = "Episode 2", year = 2024, month = 6, day = 1),
        addEpisode(title = "Episode 3", year = 2024, month = 6, day = 2),
      )

    val filtered =
      filter(
        episodes = episodes,
        filterState =
          AddEpisodeFilterState(
            publishedEndDateMillis = pickerDateMillis(year = 2024, month = 6, day = 1)
          ),
      )

    assertEquals(listOf("Episode 1", "Episode 2"), filtered.map(AddEpisode::title))
  }

  private fun addEpisode(title: String, year: Int, month: Int, day: Int) =
    AddEpisode(
      episodeId = "$title-$year-$month-$day",
      title = title,
      description = "",
      publishedDate = "$day/$month/$year",
      publishedAt = episodeMillis(year = year, month = month, day = day),
      url = "https://example.com/$title",
      state = AddEpisodeDownloadState.NotDownloaded,
    )

  private fun episodeMillis(year: Int, month: Int, day: Int): Long =
    utcMillis(year = year, month = month, day = day, hour = 9, minute = 0)

  private fun pickerDateMillis(year: Int, month: Int, day: Int): Long =
    utcMillis(year = year, month = month, day = day, hour = 0, minute = 0)

  private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, day)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      timeInMillis
    }
}
