package dev.halim.shelfdroid.core.data.screen.libraryadmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminCreateModelTest {

  @Test
  fun defaults_matchAudiobookshelf2360BookAndPodcastSettings() {
    val draft = LibraryAdminDraft()

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
    val draft = LibraryAdminDraft()

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
      LibraryAdminDraft()
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
      LibraryAdminDraft(
        bookSettings =
          LibraryAdminBookSettings(
            audiobooksOnly = true,
            epubsAllowScriptedContent = true,
          ),
        podcastSettings = LibraryAdminPodcastSettings(podcastSearchRegion = "gb"),
      )

    assertTrue(draft.withMediaType(LibraryAdminMediaType.PODCAST).bookSettings.audiobooksOnly)
    assertTrue(
      draft.withMediaType(LibraryAdminMediaType.BOOK).bookSettings.epubsAllowScriptedContent
    )
    assertEquals(
      "gb",
      draft.withMediaType(LibraryAdminMediaType.PODCAST).podcastSettings.podcastSearchRegion,
    )
  }

  @Test
  fun finishThresholdTransitionsKeepOneModeAndCarryTheSelectedValue() {
    val timeRemaining = LibraryAdminFinishThreshold(timeRemaining = 42)

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
      LibraryAdminDraft(
        bookSettings = LibraryAdminBookSettings(markAsFinishedTimeRemaining = 18),
        podcastSettings = LibraryAdminPodcastSettings(markAsFinishedTimeRemaining = 31),
      )

    val updatedBook =
      draft
        .withFinishThreshold { it.selectMode(percentCompleteMode = true) }
        .withMediaType(LibraryAdminMediaType.PODCAST)

    assertEquals(18, updatedBook.bookSettings.markAsFinishedPercentComplete)
    assertNull(updatedBook.bookSettings.markAsFinishedTimeRemaining)
    assertEquals(31, updatedBook.podcastSettings.markAsFinishedTimeRemaining)
  }

  @Test
  fun createSettingsSerializeOnlyFinalMediaTypeAndOneThresholdMode() {
    val bookSettings =
      LibraryAdminDraft(
        mediaType = LibraryAdminMediaType.BOOK,
        bookSettings =
          LibraryAdminBookSettings(
            markAsFinishedPercentComplete = 80,
            markAsFinishedTimeRemaining = 20,
          ),
        podcastSettings = LibraryAdminPodcastSettings(podcastSearchRegion = "gb"),
      ).toCreateSettings()

    assertEquals(80, bookSettings.markAsFinishedPercentComplete)
    assertNull(bookSettings.markAsFinishedTimeRemaining)
    assertNull(bookSettings.podcastSearchRegion)

    val podcastSettings =
      LibraryAdminDraft(
        mediaType = LibraryAdminMediaType.PODCAST,
        bookSettings = LibraryAdminBookSettings(audiobooksOnly = true),
        podcastSettings =
          LibraryAdminPodcastSettings(
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
