package dev.halim.shelfdroid.core.data.screen.settings.podcast

import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.UserPrefs

data class SettingsPodcastUiState(
  val crudPrefs: CrudPrefs = CrudPrefs(),
  val userPrefs: UserPrefs = UserPrefs(),
)
