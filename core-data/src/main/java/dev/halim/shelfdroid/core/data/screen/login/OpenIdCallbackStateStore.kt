package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PendingOpenIdCallback(
  val normalizedServer: String,
  val state: String,
  val code: String,
  val receivedAtEpochMillis: Long,
)

class PendingOpenIdCallbackStore
@Inject
constructor(private val dataStoreManager: DataStoreManager) {

  suspend fun save(pendingOpenIdCallback: PendingOpenIdCallback) {
    dataStoreManager.updatePendingOpenIdCallback(json.encodeToString(pendingOpenIdCallback))
  }

  suspend fun current(): PendingOpenIdCallback? {
    val serialized = dataStoreManager.pendingOpenIdCallback.firstOrNull() ?: return null
    return runCatching { json.decodeFromString<PendingOpenIdCallback>(serialized) }.getOrNull()
  }

  suspend fun clear() {
    dataStoreManager.updatePendingOpenIdCallback(null)
  }
}

@Serializable
data class OpenIdLoginFailure(
  val normalizedServer: String? = null,
  val serverAccessMode: ServerAccessMode = ServerAccessMode.Internet,
  val errorMessage: String,
)

class OpenIdLoginFailureStore @Inject constructor(private val dataStoreManager: DataStoreManager) {

  suspend fun save(failure: OpenIdLoginFailure) {
    dataStoreManager.updateOpenIdLoginFailure(json.encodeToString(failure))
  }

  suspend fun consume(): OpenIdLoginFailure? {
    val serialized = dataStoreManager.openIdLoginFailure.firstOrNull() ?: return null
    dataStoreManager.updateOpenIdLoginFailure(null)
    return runCatching { json.decodeFromString<OpenIdLoginFailure>(serialized) }.getOrNull()
  }

  suspend fun clear() {
    dataStoreManager.updateOpenIdLoginFailure(null)
  }
}

internal suspend fun recordOpenIdLoginFailure(
  pendingOpenIdLoginStore: PendingOpenIdLoginStore,
  pendingOpenIdCallbackStore: PendingOpenIdCallbackStore,
  openIdLoginFailureStore: OpenIdLoginFailureStore,
  normalizedServer: String?,
  serverAccessMode: ServerAccessMode,
  errorMessage: String,
): OpenIdLoginFailure {
  pendingOpenIdLoginStore.clear()
  pendingOpenIdCallbackStore.clear()
  val failure = OpenIdLoginFailure(normalizedServer, serverAccessMode, errorMessage)
  openIdLoginFailureStore.save(failure)
  return failure
}

private val json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
}
