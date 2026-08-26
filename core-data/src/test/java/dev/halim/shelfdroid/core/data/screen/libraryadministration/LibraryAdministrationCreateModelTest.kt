package dev.halim.shelfdroid.core.data.screen.libraryadministration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationCreateModelTest {

  @Test
  fun defaults_matchAudiobookshelf2360BookAndPodcastSettings() {
    val draft = LibraryAdministrationDraft()

    assertEquals(SQUARE_COVER_ASPECT_RATIO, draft.bookSettings.coverAspectRatio)
    assertFalse(draft.bookSettings.disableWatcher)
    assertFalse(draft.bookSettings.audiobooksOnly)
    assertFalse(draft.bookSettings.skipMatchingMediaWithAsin)
    assertFalse(draft.bookSettings.skipMatchingMediaWithIsbn)
    assertFalse(draft.bookSettings.epubsAllowScriptedContent)
    assertEquals(DEFAULT_FINISH_TIME_REMAINING, draft.bookSettings.markAsFinishedTimeRemaining)
    assertEquals(DEFAULT_PODCAST_SEARCH_REGION, draft.podcastSettings.podcastSearchRegion)
    assertFalse(draft.podcastSettings.disableWatcher)
    assertEquals(DEFAULT_FINISH_TIME_REMAINING, draft.podcastSettings.markAsFinishedTimeRemaining)
  }

  @Test
  fun metadataSources_displayHighestFirstButSerializeServerOrder() {
    val draft = LibraryAdministrationDraft()

    assertEquals("absMetadata", draft.metadataSources.first().id)
    assertEquals(1, draft.metadataPriority("absMetadata"))
    assertEquals(6, draft.metadataPriority("folderStructure"))
    assertEquals(
      listOf("folderStructure", "audioMetatags", "nfoFile", "txtFiles", "opfFile", "absMetadata"),
      draft.metadataPrecedence,
    )
  }

  @Test
  fun metadataSources_preserveEnabledOrderWhenReorderedAndToggled() {
    val draft =
      LibraryAdministrationDraft()
        .moveMetadataSource("folderStructure", -1)
        .withMetadataSource("nfoFile", false)

    assertEquals(
      listOf("audioMetatags", "folderStructure", "txtFiles", "opfFile", "absMetadata"),
      draft.metadataPrecedence,
    )
    assertTrue(draft.metadataSources.any { it.id == "nfoFile" && !it.enabled })
  }

  @Test
  fun settingsValuesRemainAvailableWhenMediaTypeChanges() {
    val draft =
      LibraryAdministrationDraft(
        bookSettings =
          LibraryAdministrationBookSettings(
            audiobooksOnly = true,
            epubsAllowScriptedContent = true,
          ),
        podcastSettings = LibraryAdministrationPodcastSettings(podcastSearchRegion = "gb"),
      )

    assertTrue(draft.withMediaType(LibraryAdministrationMediaType.PODCAST).bookSettings.audiobooksOnly)
    assertTrue(
      draft.withMediaType(LibraryAdministrationMediaType.BOOK).bookSettings.epubsAllowScriptedContent
    )
    assertEquals(
      "gb",
      draft.withMediaType(LibraryAdministrationMediaType.PODCAST).podcastSettings.podcastSearchRegion,
    )
  }

  @Test
  fun finishThresholdTransitionsKeepOneModeAndCarryTheSelectedValue() {
    val timeRemaining = LibraryAdministrationFinishThreshold(timeRemaining = 42)

    val percentComplete = timeRemaining.selectMode(percentCompleteMode = true)
    assertEquals(42, percentComplete.percentComplete)
    assertNull(percentComplete.timeRemaining)

    val updatedPercent = percentComplete.updateValue(75)
    assertEquals(75, updatedPercent.percentComplete)
    assertNull(updatedPercent.timeRemaining)

    val updatedTime = updatedPercent.selectMode(percentCompleteMode = false)
    assertNull(updatedTime.percentComplete)
    assertEquals(75, updatedTime.timeRemaining)
  }

  @Test
  fun finishThresholdTransitionTargetsOnlyTheActiveMediaSettings() {
    val draft =
      LibraryAdministrationDraft(
        bookSettings = LibraryAdministrationBookSettings(markAsFinishedTimeRemaining = 18),
        podcastSettings = LibraryAdministrationPodcastSettings(markAsFinishedTimeRemaining = 31),
      )

    val updatedBook =
      draft
        .withFinishThreshold { it.selectMode(percentCompleteMode = true) }
        .withMediaType(LibraryAdministrationMediaType.PODCAST)

    assertEquals(18, updatedBook.bookSettings.markAsFinishedPercentComplete)
    assertNull(updatedBook.bookSettings.markAsFinishedTimeRemaining)
    assertEquals(31, updatedBook.podcastSettings.markAsFinishedTimeRemaining)
  }

  @Test
  fun createSettingsSerializeOnlyFinalMediaTypeAndOneThresholdMode() {
    val bookSettings =
      LibraryAdministrationDraft(
        mediaType = LibraryAdministrationMediaType.BOOK,
        bookSettings =
          LibraryAdministrationBookSettings(
            markAsFinishedPercentComplete = 80,
            markAsFinishedTimeRemaining = 20,
          ),
        podcastSettings = LibraryAdministrationPodcastSettings(podcastSearchRegion = "gb"),
      ).toCreateSettings()

    assertEquals(80, bookSettings.markAsFinishedPercentComplete)
    assertNull(bookSettings.markAsFinishedTimeRemaining)
    assertNull(bookSettings.podcastSearchRegion)

    val podcastSettings =
      LibraryAdministrationDraft(
        mediaType = LibraryAdministrationMediaType.PODCAST,
        bookSettings = LibraryAdministrationBookSettings(audiobooksOnly = true),
        podcastSettings =
          LibraryAdministrationPodcastSettings(
            podcastSearchRegion = "gb",
            markAsFinishedTimeRemaining = 25,
          ),
      ).toCreateSettings()

    assertEquals("gb", podcastSettings.podcastSearchRegion)
    assertEquals(25, podcastSettings.markAsFinishedTimeRemaining)
    assertNull(podcastSettings.markAsFinishedPercentComplete)
    assertNull(podcastSettings.audiobooksOnly)
    assertNull(podcastSettings.metadataPrecedence)
  }
}
