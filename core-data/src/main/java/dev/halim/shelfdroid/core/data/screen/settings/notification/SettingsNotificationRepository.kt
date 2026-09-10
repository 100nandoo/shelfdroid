package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsNotificationRepository
@Inject
constructor(private val prefsRepository: PrefsRepository) {
  val notificationPrefs = prefsRepository.notificationPrefs
  private val updateMutex = Mutex()

  suspend fun updateDefaultSleepTimerMinutes(minutes: Int) {
    updateMutex.withLock {
      val current = notificationPrefs.first().copy(sleepTimerMinutes = minutes)
      prefsRepository.updateNotificationPrefs(current)
    }
  }

  suspend fun updateActionSlot(slot: Int, action: MediaNotificationAction) {
    updateMutex.withLock {
      val current = notificationPrefs.first()
      val updated =
        when (slot) {
          1 ->
            current.copy(
              firstAction = action,
              secondAction =
                if (action != MediaNotificationAction.None && current.secondAction == action) {
                  MediaNotificationAction.None
                } else {
                  current.secondAction
                },
            )
          2 ->
            current.copy(
              firstAction =
                if (action != MediaNotificationAction.None && current.firstAction == action) {
                  MediaNotificationAction.None
                } else {
                  current.firstAction
                },
              secondAction = action,
            )
          else -> current
        }
      prefsRepository.updateNotificationPrefs(updated)
    }
  }

  suspend fun updatePlaybackSpeedCycle(speeds: List<Float>) {
    updateMutex.withLock {
      val current = notificationPrefs.first()
      val updated = current.copy(playbackSpeedCycle = normalizePlaybackSpeedCycle(speeds))
      prefsRepository.updateNotificationPrefs(updated)
    }
  }
}
