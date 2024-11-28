package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.halim.shelfdroid.expect.PlatformContext
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

expect fun createDataStore(platformContext: PlatformContext): DataStore<Preferences>

internal const val dataStoreFileName = "shelfdroid.preferences_pb"

sealed class DataStoreEvent {
    data class MediaItemChanged(val shelfdroidMediaItem: ShelfdroidMediaItemImpl) : DataStoreEvent()
    data class UpdateCurrentPosition(val value: Long): DataStoreEvent()
}

class DataStoreManager(
    private val dataStore: DataStore<Preferences>, private val json: Json, private val io: CoroutineScope
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val CURRENT_POSITION = longPreferencesKey("current_position")

        fun <T> serializedKey(name: String) = stringPreferencesKey("serialized_$name")
    }

    fun onEvent(event: DataStoreEvent) {
        when (event) {
            is DataStoreEvent.MediaItemChanged -> {
                io.launch {
                    val mediaItem = event.shelfdroidMediaItem
                    writeSerializable(
                        ::ShelfdroidMediaItemImpl.name,
                        mediaItem,
                        ShelfdroidMediaItemImpl.serializer()
                    )
                    setCurrentPosition(mediaItem.seekTime)
                }
            }

            is DataStoreEvent.UpdateCurrentPosition -> {
                io.launch { setCurrentPosition(event.value) }
            }
        }
    }


    private fun readString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    private suspend fun writeString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    private fun readLong(key: Preferences.Key<Long>, default: Long = 0): Flow<Long> =
        dataStore.data.map { it[key] ?: default }

    private suspend fun writeLong(key: Preferences.Key<Long>, value: Long) {
        dataStore.edit { it[key] = value }
    }

    private fun readBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> {
        return dataStore.data.map { it[key] ?: default }
    }

    private suspend fun writeBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun <T> readSerializable(name: String, serializer: KSerializer<T>): Flow<T?> {
        return dataStore.data.map { preferences ->
            preferences[Keys.serializedKey<T>(name)]?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrNull()
            }
        }
    }

    fun <T> readSerializableBlocking(name: String, serializer: KSerializer<T>): T? {
        return runBlocking {
            dataStore.data.firstOrNull()?.get(Keys.serializedKey<T>(name))?.let {
                runCatching { json.decodeFromString(serializer, it) }.getOrNull()
            }
        }
    }

    suspend fun <T> writeSerializable(name: String, value: T, serializer: KSerializer<T>) {
        val jsonString = json.encodeToString(serializer, value)
        writeString(Keys.serializedKey<T>(name), jsonString)
    }

    suspend fun clear() = dataStore.edit { it.clear() }


    val baseUrl: Flow<String> = readString(Keys.BASE_URL)
    suspend fun setBaseUrl(value: String) = writeString(Keys.BASE_URL, value)

    val deviceId: Flow<String> = readString(Keys.DEVICE_ID)
    val deviceIdBlocking: String = runBlocking { dataStore.data.firstOrNull()?.get(Keys.DEVICE_ID) ?: "" }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun generateDeviceId() {
        val uuid = Uuid.random().toString()
        if (readString(Keys.DEVICE_ID).first().isBlank()) {
            writeString(Keys.DEVICE_ID, uuid)
        }
    }

    val token: Flow<String> = readString(Keys.TOKEN)
    suspend fun setToken(value: String) = writeString(Keys.TOKEN, value)
    val tokenBlocking: String? = runBlocking {
        dataStore.data.firstOrNull()?.get(Keys.TOKEN)
    }

    val isDarkMode: Flow<Boolean> = readBoolean(Keys.IS_DARK_MODE, true)
    suspend fun setDarkMode(value: Boolean) = writeBoolean(Keys.IS_DARK_MODE, value)

    val currentPosition: Flow<Long> = readLong(Keys.CURRENT_POSITION, 0)
    val currentPositionBlocking: Long = runBlocking { dataStore.data.firstOrNull()?.get(Keys.CURRENT_POSITION) ?: 0 }
    suspend private fun setCurrentPosition(value: Long) = writeLong(Keys.CURRENT_POSITION, value)

}