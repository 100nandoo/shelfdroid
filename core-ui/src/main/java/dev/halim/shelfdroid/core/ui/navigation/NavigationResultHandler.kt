package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult

fun completeCreatePodcastNavigation(
  navigator: ShelfNavigator,
  result: CreatePodcastNavResult,
) {
  navigator.pop()
  navigator.navigate(Podcast(result.id))
}

fun completeApiKeyEditNavigation(navigator: ShelfNavigator) {
  navigator.pop()
}
