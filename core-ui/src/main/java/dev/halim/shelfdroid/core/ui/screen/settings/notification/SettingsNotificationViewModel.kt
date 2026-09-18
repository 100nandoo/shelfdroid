package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationRepository
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedToggleTarget
import dev.halim.shelfdroid.core.playback.normalizeSleepTimerCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsNotificationViewModel
@Inject
constructor(private val repository: SettingsNotificationRepository) : ViewModel() {

  val uiState: StateFlow<SettingsNotificationUiState> =
    repository.notificationPrefs
      .map {
        SettingsNotificationUiState(
          sleepTimerMinutes = it.sleepTimerMinutes,
          sleepTimerMode = it.sleepTimerMode,
          sleepTimerCycle = normalizeSleepTimerCycle(it.sleepTimerCycle),
          firstAction = it.firstAction,
          secondAction = it.secondAction,
          playbackSpeedCycle = normalizePlaybackSpeedCycle(it.playbackSpeedCycle),
          playbackSpeedMode = it.playbackSpeedMode,
          playbackSpeedToggleTarget = normalizePlaybackSpeedToggleTarget(it.playbackSpeedToggleTarget),
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsNotificationUiState())

  fun onEvent(event: SettingsNotificationEvent) {
    when (event) {
      is SettingsNotificationEvent.ChangeSleepTimerMinutes ->
        viewModelScope.launch { repository.updateDefaultSleepTimerMinutes(event.minutes) }
      is SettingsNotificationEvent.ChangeSleepTimerMode ->
        viewModelScope.launch { repository.updateSleepTimerMode(event.mode) }
      is SettingsNotificationEvent.ChangeSleepTimerCycle ->
        viewModelScope.launch { repository.updateSleepTimerCycle(event.minutes) }
      is SettingsNotificationEvent.ChangeActionSlot ->
        viewModelScope.launch { repository.updateActionSlot(event.slot, event.action) }
      is SettingsNotificationEvent.ChangePlaybackSpeedCycle ->
        viewModelScope.launch { repository.updatePlaybackSpeedCycle(event.speeds) }
      is SettingsNotificationEvent.ChangePlaybackSpeedMode ->
        viewModelScope.launch { repository.updatePlaybackSpeedMode(event.mode) }
      is SettingsNotificationEvent.ChangePlaybackSpeedToggleTarget ->
        viewModelScope.launch { repository.updatePlaybackSpeedToggleTarget(event.speed) }
    }
  }
}

sealed interface SettingsNotificationEvent {
  data class ChangeSleepTimerMinutes(val minutes: Int) : SettingsNotificationEvent

  data class ChangeSleepTimerMode(val mode: SleepTimerNotificationMode) : SettingsNotificationEvent

  data class ChangeSleepTimerCycle(val minutes: List<Int>) : SettingsNotificationEvent

  data class ChangeActionSlot(val slot: Int, val action: MediaNotificationAction) :
    SettingsNotificationEvent

  data class ChangePlaybackSpeedCycle(val speeds: List<Float>) : SettingsNotificationEvent

  data class ChangePlaybackSpeedMode(val mode: PlaybackSpeedNotificationMode) : SettingsNotificationEvent

  data class ChangePlaybackSpeedToggleTarget(val speed: Float) : SettingsNotificationEvent
}
