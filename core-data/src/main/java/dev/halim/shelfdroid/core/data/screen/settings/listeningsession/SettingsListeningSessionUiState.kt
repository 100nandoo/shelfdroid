package dev.halim.shelfdroid.core.data.screen.settings.listeningsession

import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.core.prefs.ListeningSessionPrefs

data class SettingsListeningSessionUiState(
  val users: List<User> = emptyList(),
  val listeningSessionPrefs: ListeningSessionPrefs = ListeningSessionPrefs(),
)
