package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsNotificationRepository
@Inject
constructor(private val prefsRepository: PrefsRepository) {
  val notificationPrefs = prefsRepository.notificationPrefs

  suspend fun updateDefaultSleepTimerMinutes(minutes: Int) {
    val current = notificationPrefs.first().copy(sleepTimerMinutes = minutes)
    prefsRepository.updateNotificationPrefs(current)
  }
}
