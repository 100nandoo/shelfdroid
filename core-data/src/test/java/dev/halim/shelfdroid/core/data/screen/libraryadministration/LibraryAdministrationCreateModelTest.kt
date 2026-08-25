package dev.halim.shelfdroid.core.data.screen.libraryadministration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
