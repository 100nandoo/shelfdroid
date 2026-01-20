package dev.halim.shelfdroid.core.data.screen.settings

import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.DisplayPrefs

data class SettingsUiState(
  val settingsState: SettingsState = SettingsState.NotLoggedOut,
  val isDarkMode: Boolean = true,
  val isDynamicTheme: Boolean = false,
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val crudPrefs: CrudPrefs = CrudPrefs(),
  val isAdmin: Boolean = false,
  val canDelete: Boolean = false,
  val username: String = "",
)

sealed class SettingsState {
  data object NotLoggedOut : SettingsState()

  data object Loading : SettingsState()

  data object Success : SettingsState()

  data class Failure(val errorMessage: String?) : SettingsState()
}
