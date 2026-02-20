package dev.halim.shelfdroid.core.datastore

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.ListeningSessionPrefs
import dev.halim.shelfdroid.core.PlaybackPrefs
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.UserPrefs
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

private object Keys {
  val BASE_URL = stringPreferencesKey("base_url")
  val DARK_MODE = booleanPreferencesKey("dark_mode")
  val DYNAMIC_THEME = booleanPreferencesKey("dynamic_theme")
  val DEVICE_ID = stringPreferencesKey("device_id")
  val USER_PREFS = stringPreferencesKey("user_prefs")
  val SERVER_PREFS = stringPreferencesKey("server_prefs")
  val DISPLAY_PREFS = stringPreferencesKey("display_prefs")
  val PLAYBACK_PREFS = stringPreferencesKey("playback_prefs")
  val CRUD_PREFS = stringPreferencesKey("crud_prefs")
  val LISTENING_SESSION_PREFS = stringPreferencesKey("listening_session_prefs")

  val TAGS = stringSetPreferencesKey("tags")
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

  fun baseUrl(): String = runBlocking { baseUrl.firstOrNull() ?: "" }

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
        runCatching { Json.decodeFromString<UserPrefs>(json) }.getOrNull()
      } ?: UserPrefs()
    }

  fun accessToken(): String = runBlocking { userPrefs.firstOrNull()?.accessToken ?: "" }

  suspend fun updateUserPrefs(userPrefs: UserPrefs) {
    val json = Json.encodeToString(userPrefs)
    dataStore.edit { prefs -> prefs[Keys.USER_PREFS] = json }
  }

  val serverPrefs: Flow<ServerPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.SERVER_PREFS]?.let { json ->
        runCatching { Json.decodeFromString<ServerPrefs>(json) }.getOrNull()
      } ?: ServerPrefs()
    }

  suspend fun updateServerPrefs(serverPrefs: ServerPrefs) {
    val json = Json.encodeToString(serverPrefs)
    dataStore.edit { prefs -> prefs[Keys.SERVER_PREFS] = json }
  }

  val displayPrefs: Flow<DisplayPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.DISPLAY_PREFS]?.let { json ->
        runCatching { Json.decodeFromString<DisplayPrefs>(json) }.getOrNull()
      } ?: DisplayPrefs()
    }

  suspend fun updateDisplayPrefs(displayPrefs: DisplayPrefs) {
    val json = Json.encodeToString(displayPrefs)
    dataStore.edit { prefs -> prefs[Keys.DISPLAY_PREFS] = json }
  }

  val playbackPrefs: Flow<PlaybackPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.PLAYBACK_PREFS]?.let { json ->
        runCatching { Json.decodeFromString<PlaybackPrefs>(json) }.getOrNull()
      } ?: PlaybackPrefs()
    }

  suspend fun updatePlaybackPrefs(playbackPrefs: PlaybackPrefs) {
    val json = Json.encodeToString(playbackPrefs)
    dataStore.edit { prefs -> prefs[Keys.PLAYBACK_PREFS] = json }
  }

  val crudPrefs: Flow<CrudPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.CRUD_PREFS]?.let { json ->
        runCatching { Json.decodeFromString<CrudPrefs>(json) }.getOrNull()
      } ?: CrudPrefs()
    }

  suspend fun updateCrudPrefs(crudPrefs: CrudPrefs) {
    val json = Json.encodeToString(crudPrefs)
    dataStore.edit { prefs -> prefs[Keys.CRUD_PREFS] = json }
  }

  val listeningSessionPrefs: Flow<ListeningSessionPrefs> =
    dataStore.data.map { prefs ->
      prefs[Keys.LISTENING_SESSION_PREFS]?.let { json ->
        runCatching { Json.decodeFromString<ListeningSessionPrefs>(json) }.getOrNull()
      } ?: ListeningSessionPrefs()
    }

  suspend fun updateListeningSessionPrefs(prefs: ListeningSessionPrefs) {
    val json = Json.encodeToString(prefs)
    dataStore.edit { prefs -> prefs[Keys.LISTENING_SESSION_PREFS] = json }
  }

  suspend fun updateListView(listView: Boolean) {
    val current = displayPrefs.firstOrNull()?.copy(listView = listView)
    current?.let { updateDisplayPrefs(it) }
  }

  suspend fun updateFilter(filter: Filter) {
    val current = displayPrefs.firstOrNull()?.copy(filter = filter)
    current?.let { updateDisplayPrefs(it) }
  }

  suspend fun updateBookSort(bookSort: BookSort) {
    val current = displayPrefs.firstOrNull()?.copy(bookSort = bookSort)
    current?.let { updateDisplayPrefs(it) }
  }

  suspend fun updatePodcastSort(podcastSort: PodcastSort) {
    val current = displayPrefs.firstOrNull()?.copy(podcastSort = podcastSort)
    current?.let { updateDisplayPrefs(it) }
  }

  suspend fun updateSortOrder(sortOrder: SortOrder) {
    val current = displayPrefs.firstOrNull()?.copy(sortOrder = sortOrder)
    current?.let { updateDisplayPrefs(it) }
  }

  suspend fun updatePodcastSortOrder(podcastSortOrder: SortOrder) {
    val current = displayPrefs.firstOrNull()?.copy(podcastSortOrder = podcastSortOrder)
    current?.let { updateDisplayPrefs(it) }
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

  val tags: Flow<Set<String>> = dataStore.preferenceFlow(Keys.TAGS, emptySet())

  suspend fun updateTags(tags: Set<String>) = dataStore.updatePreference(Keys.TAGS, tags)

  suspend fun clear() = dataStore.edit { preferences -> preferences.clear() }
}
