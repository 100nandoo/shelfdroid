package dev.halim.shelfdroid.core.data.auth

import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class AuthStateRepository
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  prefsRepository: PrefsRepository,
) {
  val token = prefsRepository.userPrefs.map { it.accessToken }
  val authPromptReason = dataStoreManager.authPromptReason

  suspend fun startManualReLogin() {
    dataStoreManager.beginForcedReLogin(AuthPromptReason.ManualReLogin)
  }
}
