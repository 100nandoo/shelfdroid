package dev.halim.shelfdroid.core.data.screen.settings.player

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
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

class SettingsPlayerRepositoryTest {

  @Test
  fun concurrentSeekIntervalUpdates_preserveBothDirections() = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("settings-player", ".preferences_pb").toFile()
    file.deleteOnExit()
    try {
      val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
      val repository = SettingsPlayerRepository(PrefsRepository(DataStoreManager(dataStore)))

      coroutineScope {
        repeat(100) {
          launch { repository.updateSeekBackSeconds(60) }
          launch { repository.updateSeekForwardSeconds(15) }
        }
      }

      val prefs = repository.playerPrefs.first()
      assertEquals(60, prefs.seekBackSeconds)
      assertEquals(15, prefs.seekForwardSeconds)
    } finally {
      scope.cancel()
      file.delete()
    }
  }
}
