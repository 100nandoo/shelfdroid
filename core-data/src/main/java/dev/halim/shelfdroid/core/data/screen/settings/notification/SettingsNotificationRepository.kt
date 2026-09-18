package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.playback.normalizeSleepTimerCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedToggleTarget
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

  suspend fun updateSleepTimerMode(mode: SleepTimerNotificationMode) {
    updateMutex.withLock {
      val current = notificationPrefs.first().copy(sleepTimerMode = mode)
      prefsRepository.updateNotificationPrefs(current)
    }
  }

  suspend fun updateSleepTimerCycle(minutes: List<Int>) {
    updateMutex.withLock {
      val current = notificationPrefs.first().copy(sleepTimerCycle = normalizeSleepTimerCycle(minutes))
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

  suspend fun updatePlaybackSpeedMode(mode: PlaybackSpeedNotificationMode) {
    updateMutex.withLock {
      prefsRepository.updateNotificationPrefs(notificationPrefs.first().copy(playbackSpeedMode = mode))
    }
  }

  suspend fun updatePlaybackSpeedToggleTarget(speed: Float) {
    updateMutex.withLock {
      val current = notificationPrefs.first()
      prefsRepository.updateNotificationPrefs(
        current.copy(playbackSpeedToggleTarget = normalizePlaybackSpeedToggleTarget(speed))
      )
    }
  }
}
