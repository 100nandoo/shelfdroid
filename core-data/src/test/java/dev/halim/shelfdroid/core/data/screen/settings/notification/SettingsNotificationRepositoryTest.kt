package dev.halim.shelfdroid.core.data.screen.settings.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNotificationRepositoryTest {
  @Test
  fun concurrentOpeningPreferenceUpdatesPreserveEachOtherAndExistingSettings() = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("settings-notification", ".preferences_pb").toFile()
    file.deleteOnExit()
    try {
      val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
      val prefsRepository = PrefsRepository(DataStoreManager(dataStore))
      val repository = SettingsNotificationRepository(prefsRepository)
      prefsRepository.updateNotificationPrefs(
        NotificationPrefs(
          firstAction = MediaNotificationAction.PlaybackSpeed,
          sleepTimerMinutes = 15,
        )
      )

      coroutineScope {
        launch {
          repository.updatePlayerPresentation(MediaNotificationPlayerPresentation.MiniPlayer)
        }
        launch { repository.updateOpeningScreen(MediaNotificationOpeningScreen.MediaDetails) }
      }

      val prefs = repository.notificationPrefs.first()
      assertEquals(MediaNotificationPlayerPresentation.MiniPlayer, prefs.playerPresentation)
      assertEquals(MediaNotificationOpeningScreen.MediaDetails, prefs.openingScreen)
      assertEquals(MediaNotificationAction.PlaybackSpeed, prefs.firstAction)
      assertEquals(15, prefs.sleepTimerMinutes)
    } finally {
      scope.cancel()
      file.delete()
    }
  }
}
