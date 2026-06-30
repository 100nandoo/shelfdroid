package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.response.SearchProvider
import dev.halim.core.network.response.SearchProviders
import org.junit.Assert.assertEquals
import org.junit.Test

class EditItemRepositoryHelpersTest {

  private val searchProviders =
    SearchProviders(
      books = listOf(SearchProvider(value = "google", text = "Google Books")),
      booksCovers = listOf(SearchProvider(value = "openlibrary", text = "Open Library")),
      podcasts = listOf(SearchProvider(value = "itunes", text = "Apple Podcasts")),
    )

  @Test
  fun matchProvidersFor_bookUsesBookProviders() {
    assertEquals(
      listOf(MatchProvider(value = "google", text = "Google Books")),
      matchProvidersFor(searchProviders, EditItemMediaKind.Book),
    )
  }

  @Test
  fun matchProvidersFor_podcastUsesPodcastProviders() {
    assertEquals(
      listOf(MatchProvider(value = "itunes", text = "Apple Podcasts")),
      matchProvidersFor(searchProviders, EditItemMediaKind.Podcast),
    )
  }

  @Test
  fun defaultMatchProviderFor_prefersAudibleWhenAvailable() {
    assertEquals(
      "audible",
      defaultMatchProviderFor(
        listOf(
          MatchProvider(value = "google", text = "Google Books"),
          MatchProvider(value = "audible", text = "Audible.com"),
        ),
        EditItemMediaKind.Book,
      ),
    )
  }

  @Test
  fun defaultMatchProviderFor_fallsBackToFirstProviderWhenAudibleMissing() {
    assertEquals(
      "itunes",
      defaultMatchProviderFor(
        listOf(MatchProvider(value = "itunes", text = "Apple Podcasts")),
        EditItemMediaKind.Podcast,
      ),
    )
  }

  @Test
  fun coverProvidersFor_bookUsesBookCoverProviders() {
    assertEquals(
      listOf(MatchProvider(value = "openlibrary", text = "Open Library")),
      coverProvidersFor(searchProviders, EditItemMediaKind.Book),
    )
  }

  @Test
  fun coverProvidersFor_podcastUsesPodcastProviders() {
    assertEquals(
      listOf(MatchProvider(value = "itunes", text = "Apple Podcasts")),
      coverProvidersFor(searchProviders, EditItemMediaKind.Podcast),
    )
  }

  @Test
  fun coverSearchPodcastFlag_matchesMediaKind() {
    assertEquals(0, coverSearchPodcastFlag(EditItemMediaKind.Book))
    assertEquals(1, coverSearchPodcastFlag(EditItemMediaKind.Podcast))
  }

  @Test
  fun resolveCoverFilename_keepsExistingImageFilename() {
    assertEquals(
      "andrew-svk-150fZ07GQqs-unsplash.jpg",
      resolveCoverFilename("andrew-svk-150fZ07GQqs-unsplash.jpg", "image/jpeg"),
    )
  }

  @Test
  fun resolveCoverFilename_addsExtensionFromMime() {
    assertEquals("cover.jpg", resolveCoverFilename("cover", "image/jpeg"))
  }

  @Test
  fun resolveCoverFilename_usesFallbackNameForBlankFilename() {
    assertEquals("cover.png", resolveCoverFilename("", "image/png"))
  }

  @Test
  fun resolveCoverFilename_defaultsToJpgForUnknownMime() {
    assertEquals("cover.jpg", resolveCoverFilename("", "image/*"))
  }
}
