package dev.halim.shelfdroid.core.ui.screen.settings.search

import dev.halim.shelfdroid.core.ui.navigation.SettingsSearchDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchAccessTest {
  @Test
  fun restrictedSettingsAreRejectedWhenPermissionChanges() {
    assertFalse(
      canSelectSettingsSearchEntry(entry(SettingsSearchSettingId.PERMANENT_DELETE), false, true)
    )
    assertFalse(
      canSelectSettingsSearchEntry(
        entry(SettingsSearchSettingId.PODCAST_PERMANENT_DELETE),
        canAccessPodcastSettings = false,
        canManagePodcastSettings = true,
      )
    )
  }

  @Test
  fun adminOnlyPodcastSettingsRequireBothPodcastAccessAndAdminAccess() {
    val hiddenDownloadedEpisodes = entry(SettingsSearchSettingId.HIDE_DOWNLOADED_EPISODES)

    assertFalse(
      canSelectSettingsSearchEntry(
        hiddenDownloadedEpisodes,
        canAccessPodcastSettings = true,
        canManagePodcastSettings = false,
      )
    )
    assertTrue(
      canSelectSettingsSearchEntry(
        hiddenDownloadedEpisodes,
        canAccessPodcastSettings = true,
        canManagePodcastSettings = true,
      )
    )
  }

  @Test
  fun ordinaryPodcastPreferencesNeedPodcastAccess() {
    val autoSelectFinishedEpisodes = entry(SettingsSearchSettingId.AUTO_SELECT_FINISHED_EPISODES)

    assertTrue(
      canSelectSettingsSearchEntry(
        autoSelectFinishedEpisodes,
        canAccessPodcastSettings = true,
        canManagePodcastSettings = false,
      )
    )
    assertFalse(
      canSelectSettingsSearchEntry(
        autoSelectFinishedEpisodes,
        canAccessPodcastSettings = false,
        canManagePodcastSettings = true,
      )
    )
  }

  private fun entry(id: String) =
    SettingsSearchEntry(
      id = id,
      destination =
        if (id == SettingsSearchSettingId.PERMANENT_DELETE) SettingsSearchDestination.HOME
        else SettingsSearchDestination.PODCAST,
      title = id,
      path = listOf("Settings", "Podcast"),
    )
}
