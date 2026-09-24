package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModelStore
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsNotificationViewModelTest {
  @Test
  fun preferenceEventsUpdateUiStateAndPreserveUnrelatedSettings() = runTest {
    val mainDispatcher = UnconfinedTestDispatcher(testScheduler)
    Dispatchers.setMain(mainDispatcher)
    val dataStoreScope = CoroutineScope(SupervisorJob() + mainDispatcher)
    val file = Files.createTempFile("settings-notification-viewmodel", ".preferences_pb").toFile()
    file.deleteOnExit()
    val viewModelStore = ViewModelStore()
    var collection: Job? = null
    try {
      val dataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = dataStoreScope, produceFile = { file })
      val prefsRepository = PrefsRepository(DataStoreManager(dataStore))
      val repository = SettingsNotificationRepository(prefsRepository)
      prefsRepository.updateNotificationPrefs(
        NotificationPrefs(
          firstAction = MediaNotificationAction.PlaybackSpeed,
          sleepTimerMinutes = 15,
        )
      )
      val viewModel = SettingsNotificationViewModel(repository)
      viewModelStore.put("settings-notification", viewModel)
      collection =
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
      advanceUntilIdle()

      viewModel.onEvent(
        SettingsNotificationEvent.ChangePlayerPresentation(
          MediaNotificationPlayerPresentation.MiniPlayer
        )
      )
      viewModel.onEvent(
        SettingsNotificationEvent.ChangeOpeningScreen(MediaNotificationOpeningScreen.MediaDetails)
      )
      advanceUntilIdle()

      assertEquals(
        MediaNotificationPlayerPresentation.MiniPlayer,
        viewModel.uiState.value.playerPresentation,
      )
      assertEquals(
        MediaNotificationOpeningScreen.MediaDetails,
        viewModel.uiState.value.openingScreen,
      )
      assertEquals(MediaNotificationAction.PlaybackSpeed, viewModel.uiState.value.firstAction)
      assertEquals(15, viewModel.uiState.value.sleepTimerMinutes)
    } finally {
      collection?.cancel()
      viewModelStore.clear()
      dataStoreScope.cancel()
      Dispatchers.resetMain()
      file.delete()
    }
  }
}
