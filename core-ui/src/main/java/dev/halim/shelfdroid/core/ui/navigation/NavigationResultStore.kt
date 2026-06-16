package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.NavResultKey

class NavigationResultStore(
  private val values: SnapshotStateMap<String, Any?> = mutableStateMapOf(),
) {
  fun <T> publish(key: String, value: T) {
    values[key] = value
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> consume(key: String): T? = values.remove(key) as? T
}

@Composable
fun rememberNavigationResultStore(): NavigationResultStore {
  return remember { NavigationResultStore() }
}

fun handleCreatePodcastSuccess(
  navigator: ShelfNavigator,
  resultStore: NavigationResultStore,
  result: CreatePodcastNavResult,
) {
  resultStore.publish(NavResultKey.CREATE_PODCAST, result)
  navigator.pop()
  navigator.navigate(Podcast(result.id))
}

fun handleApiKeyMutationSuccess(
  navigator: ShelfNavigator,
  resultStore: NavigationResultStore,
) {
  resultStore.publish(NavResultKey.API_KEY_CHANGED, true)
  navigator.pop()
}
