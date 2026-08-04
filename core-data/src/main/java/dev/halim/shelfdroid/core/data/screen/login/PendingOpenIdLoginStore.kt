package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PendingOpenIdLogin(
  val normalizedServer: String,
  val state: String,
  val codeVerifier: String,
  val createdAtEpochMillis: Long,
)

class PendingOpenIdLoginStore @Inject constructor(private val dataStoreManager: DataStoreManager) {

  suspend fun save(pendingOpenIdLogin: PendingOpenIdLogin) {
    dataStoreManager.updatePendingOpenIdLogin(json.encodeToString(pendingOpenIdLogin))
  }

  suspend fun current(): PendingOpenIdLogin? {
    val serialized = dataStoreManager.pendingOpenIdLogin.firstOrNull() ?: return null
    return runCatching { json.decodeFromString<PendingOpenIdLogin>(serialized) }.getOrNull()
  }

  suspend fun clear() {
    dataStoreManager.updatePendingOpenIdLogin(null)
  }

  private companion object {
    val json =
      Json {
        ignoreUnknownKeys = true
        explicitNulls = false
      }
  }
}
