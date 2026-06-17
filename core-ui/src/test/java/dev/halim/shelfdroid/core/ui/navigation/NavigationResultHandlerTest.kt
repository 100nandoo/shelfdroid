package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationResultHandlerTest {
  @Test
  fun create_podcast_success_pops_form_and_opens_podcast() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), SearchPodcast("library-id"))
    val navigator = ShelfNavigator(backStack)
    val result = CreatePodcastNavResult(id = "podcast-id", feedUrl = "feed-url")

    navigator.navigate(
      AddPodcast(PodcastFeedNavPayload(libraryId = "library-id", feedUrl = "feed-url"))
    )
    completeCreatePodcastNavigation(navigator, result)

    assertEquals(
      listOf(Home(false), SearchPodcast("library-id"), Podcast("podcast-id")),
      backStack.toList(),
    )
  }

  @Test
  fun api_key_success_pops_editor_back_to_api_keys() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), NavApiKeys)
    val navigator = ShelfNavigator(backStack)

    navigator.navigate(EditApiKeys(NavEditApiKeys()))
    completeApiKeyEditNavigation(navigator)

    assertEquals(listOf(Home(false), NavApiKeys), backStack.toList())
  }
}
