package dev.halim.shelfdroid.core.data.screen.settings

data class SettingsUiState(
  val settingsState: SettingsState = SettingsState.NotLoggedOut,
  val isDarkMode: Boolean = true,
  val isDynamicTheme: Boolean = false,
  val isListView: Boolean = false,
  val isOnlyDownloaded: Boolean = false,
  val isAdmin: Boolean = false,
  val username: String = "",
)

sealed class SettingsState {
  data object NotLoggedOut : SettingsState()

  data object Loading : SettingsState()

  data object Success : SettingsState()

  data class Failure(val errorMessage: String?) : SettingsState()
}
