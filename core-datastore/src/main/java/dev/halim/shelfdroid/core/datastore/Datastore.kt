package dev.halim.shelfdroid.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.halim.shelfdroid.core.UserPrefs
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private object Keys {
  val BASE_URL = stringPreferencesKey("base_url")
  val DARK_MODE = booleanPreferencesKey("dark_mode")
  val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
  val LIST_VIEW = booleanPreferencesKey("list_view")
  val DEVICE_ID = stringPreferencesKey("device_id")
  val USER_PREFS = stringPreferencesKey("user_prefs")
}

private fun <T> DataStore<Preferences>.preferenceFlow(
  key: Preferences.Key<T>,
  defaultValue: T,
): Flow<T> = data.map { preferences -> preferences[key] ?: defaultValue }

private suspend fun <T> DataStore<Preferences>.updatePreference(key: Preferences.Key<T>, value: T) {
  edit { preferences -> preferences[key] = value }
}

class DataStoreManager @Inject constructor(private val dataStore: DataStore<Preferences>) {
  companion object {
    var BASE_URL = "www.audiobookshelf.org"
  }

  val baseUrl: Flow<String> = dataStore.preferenceFlow(Keys.BASE_URL, "")

  suspend fun updateBaseUrl(baseUrl: String) = dataStore.updatePreference(Keys.BASE_URL, baseUrl)

  suspend fun getDeviceId(): String {
    val currentDeviceId = dataStore.preferenceFlow(Keys.DEVICE_ID, "").first()
    return currentDeviceId.ifEmpty {
      val newDeviceId = generateDeviceId()
      dataStore.updatePreference(Keys.DEVICE_ID, newDeviceId)
      newDeviceId
    }
  }

  val darkMode: Flow<Boolean> = dataStore.preferenceFlow(Keys.DARK_MODE, true)

  suspend fun updateDarkMode(darkMode: Boolean) =
    dataStore.updatePreference(Keys.DARK_MODE, darkMode)

  val dynamicTheme: Flow<Boolean> = dataStore.preferenceFlow(Keys.DYNAMIC_THEME, false)

  suspend fun updateDynamicTheme(dynamicTheme: Boolean) =
    dataStore.updatePreference(Keys.DYNAMIC_THEME, dynamicTheme)

  val listView: Flow<Boolean> = dataStore.preferenceFlow(Keys.LIST_VIEW, false)

  suspend fun updateListView(listView: Boolean) =
    dataStore.updatePreference(Keys.LIST_VIEW, listView)

  private fun generateDeviceId(): String {
    val uuid = UUID.randomUUID()
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(uuid.mostSignificantBits)
    byteBuffer.putLong(uuid.leastSignificantBits)
    return Base64.encodeToString(
      byteBuffer.array(),
      Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
    )
  }

  val userPrefs: Flow<UserPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.USER_PREFS]?.let { json ->
        try {
          Json.decodeFromString<UserPrefs>(json)
        } catch (e: Exception) {
          UserPrefs()
        }
      } ?: UserPrefs()
    }

  suspend fun updateUserPrefs(userPrefs: UserPrefs) {
    val json = Json.encodeToString(userPrefs)
    dataStore.edit { prefs -> prefs[Keys.USER_PREFS] = json }
  }

  suspend fun updateAccessToken(newToken: String) {
    val currentPrefs = userPrefs.first()
    val updatedPrefs = currentPrefs.copy(accessToken = newToken)
    updateUserPrefs(updatedPrefs)
  }

  suspend fun updateRefreshToken(newToken: String) {
    val updated = userPrefs.first().copy(refreshToken = newToken)
    updateUserPrefs(updated)
  }

  suspend fun clear() = dataStore.edit { preferences -> preferences.clear() }
}
