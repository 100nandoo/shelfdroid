package dev.halim.shelfdroid.core.data.screen.settings.home

import dev.halim.shelfdroid.core.prefs.CrudPrefs
import dev.halim.shelfdroid.core.prefs.DisplayPrefs

data class SettingsHomeUiState(
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val crudPrefs: CrudPrefs = CrudPrefs(),
  val canDelete: Boolean = false,
)
