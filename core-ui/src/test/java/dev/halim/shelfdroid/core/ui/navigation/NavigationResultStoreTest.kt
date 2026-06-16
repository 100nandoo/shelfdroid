package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import dev.halim.shelfdroid.core.navigation.NavResultKey
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationResultStoreTest {
  @Test
  fun consume_returns_published_value_once() {
    val store = NavigationResultStore()
    val result = CreatePodcastNavResult(id = "podcast-id", feedUrl = "feed-url")

    store.publish(NavResultKey.CREATE_PODCAST, result)

    assertEquals(result, store.consume<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST))
    assertNull(store.consume<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST))
  }

  @Test
  fun create_podcast_success_pops_form_and_opens_podcast() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), SearchPodcast("library-id"))
    val navigator = ShelfNavigator(backStack)
    val store = NavigationResultStore()
    val result = CreatePodcastNavResult(id = "podcast-id", feedUrl = "feed-url")

    navigator.navigate(AddPodcast(PodcastFeedNavPayload(libraryId = "library-id", feedUrl = "feed-url")))
    handleCreatePodcastSuccess(navigator, store, result)

    assertEquals(
      listOf(Home(false), SearchPodcast("library-id"), Podcast("podcast-id")),
      backStack.toList(),
    )
    assertEquals(result, store.consume<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST))
  }

  @Test
  fun api_key_success_pops_editor_and_stores_refresh_signal() {
    val backStack = NavBackStack<ShelfNavKey>(Home(false), NavApiKeys)
    val navigator = ShelfNavigator(backStack)
    val store = NavigationResultStore()

    navigator.navigate(EditApiKeys(NavEditApiKeys()))
    handleApiKeyMutationSuccess(navigator, store)

    assertEquals(listOf(Home(false), NavApiKeys), backStack.toList())
    assertTrue(checkNotNull(store.consume<Boolean>(NavResultKey.API_KEY_CHANGED)))
  }
}
