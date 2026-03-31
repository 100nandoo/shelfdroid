package dev.halim.shelfdroid.core.data.screen.serversettings

import dev.halim.shelfdroid.core.data.GenericState

data class CoverProvider(val value: String, val text: String)

data class ServerSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: ServerSettingsApiState = ServerSettingsApiState.Idle,
  // Snapshot of the server state (used to detect changes)
  val savedState: ServerSettingsData = ServerSettingsData(),
  // Current editable state
  val currentState: ServerSettingsData = ServerSettingsData(),
  // Providers
  val coverProviders: List<CoverProvider> = emptyList(),
) {
  val hasChanges: Boolean
    get() = savedState != currentState
}

data class ServerSettingsData(
  // General
  val storeCoverWithItem: Boolean = false,
  val storeMetadataWithItem: Boolean = false,
  val sortingIgnorePrefix: Boolean = false,
  // Scanner
  val scannerParseSubtitle: Boolean = false,
  val scannerFindCovers: Boolean = false,
  val scannerCoverProvider: String = "",
  val scannerPreferMatchedMetadata: Boolean = false,
  val watchForChanges: Boolean = true,
  // Web Client
  val chromecastEnabled: Boolean = false,
  val allowIframe: Boolean = false,
  // Display
  val homeBookshelfView: Boolean = false,
  val bookshelfView: Boolean = false,
  val dateFormat: String = "MM/DD/YYYY",
  val timeFormat: String = "HH:mm",
  val language: String = "en-us",
)

sealed interface ServerSettingsApiState {
  data object Idle : ServerSettingsApiState

  data object Loading : ServerSettingsApiState

  data object SettingsSuccess : ServerSettingsApiState

  data class SettingsFailure(val message: String?) : ServerSettingsApiState

  data object PurgeCacheSuccess : ServerSettingsApiState

  data class PurgeCacheFailure(val message: String?) : ServerSettingsApiState

  data object PurgeItemsCacheSuccess : ServerSettingsApiState

  data class PurgeItemsCacheFailure(val message: String?) : ServerSettingsApiState
}
