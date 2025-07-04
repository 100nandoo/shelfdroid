package dev.halim.shelfdroid.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private object Keys {
  val BASE_URL = stringPreferencesKey("base_url")
  val TOKEN = stringPreferencesKey("token")
  val DARK_MODE = booleanPreferencesKey("dark_mode")
  val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
  val DEVICE_ID = stringPreferencesKey("device_id")
  val USER_ID = stringPreferencesKey("user_id")
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

  val token: Flow<String> = dataStore.preferenceFlow(Keys.TOKEN, "")

  suspend fun updateToken(token: String) = dataStore.updatePreference(Keys.TOKEN, token)

  suspend fun getDeviceId(): String {
    val currentDeviceId = dataStore.preferenceFlow(Keys.DEVICE_ID, "").first()
    return currentDeviceId.ifEmpty {
      val newDeviceId = generateDeviceId()
      dataStore.updatePreference(Keys.DEVICE_ID, newDeviceId)
      newDeviceId
    }
  }

  val userId: Flow<String> = dataStore.preferenceFlow(Keys.USER_ID, "")

  suspend fun updateUserId(userId: String) = dataStore.updatePreference(Keys.USER_ID, userId)

  val darkMode: Flow<Boolean> = dataStore.preferenceFlow(Keys.DARK_MODE, true)

  suspend fun updateDarkMode(darkMode: Boolean) =
    dataStore.updatePreference(Keys.DARK_MODE, darkMode)

  val dynamicTheme: Flow<Boolean> = dataStore.preferenceFlow(Keys.DYNAMIC_THEME, false)

  suspend fun updateDynamicTheme(dynamicTheme: Boolean) =
    dataStore.updatePreference(Keys.DYNAMIC_THEME, dynamicTheme)

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

  suspend fun clear() =
    dataStore.edit { preferences ->
      val deviceId = preferences[Keys.DEVICE_ID]
      preferences.clear()
      deviceId?.let { preferences[Keys.DEVICE_ID] = it }
    }
}
