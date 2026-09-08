package dev.halim.shelfdroid.core.data.screen.settings.home

import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.DisplayPrefs

data class SettingsHomeUiState(
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val crudPrefs: CrudPrefs = CrudPrefs(),
  val canDelete: Boolean = false,
)
