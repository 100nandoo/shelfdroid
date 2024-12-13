package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.expect.PlatformContext
import dev.halim.shelfdroid.ui.MediaItemType
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.MediaItemBook
import dev.halim.shelfdroid.ui.MediaItemPodcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

expect fun createDataStore(platformContext: PlatformContext): DataStore<Preferences>

internal const val dataStoreFileName = "shelfdroid.preferences_pb"

sealed class DataStoreEvent {
    data class MediaItemChanged(val item: ShelfdroidMediaItem) : DataStoreEvent()
    data class UpdateCurrentPosition(val value: Long) : DataStoreEvent()
}

class DataStoreManager(
    private val dataStore: DataStore<Preferences>, private val json: Json, private val io: CoroutineScope
) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val CURRENT_POSITION = longPreferencesKey("current_position")
        val CURRENT_ITEM = stringPreferencesKey("current_item")
        val CURRENT_ITEM_TYPE = stringPreferencesKey("current_item_type")

        fun <T> serializedKey(name: String) = stringPreferencesKey("serialized_$name")
    }

    fun onEvent(event: DataStoreEvent) {
        when (event) {
            is DataStoreEvent.MediaItemChanged -> {
                io.launch {
                    val mediaItem = event.item
                    if (mediaItem is MediaItemPodcast) {
                        writeItemPodcast(mediaItem)
                    } else if (mediaItem is MediaItemBook) {
                        writeItemBook(mediaItem)
                    }
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

    suspend fun writeItemBook(value: MediaItemBook) {
        setCurrentItemType(value.type.name)
        val jsonString = json.encodeToString(value)
        writeString(Keys.CURRENT_ITEM, jsonString)
    }

    suspend fun writeItemPodcast(value: MediaItemPodcast) {
        setCurrentItemType(value.type.name)
        val jsonString = json.encodeToString(value)
        writeString(Keys.CURRENT_ITEM, jsonString)
    }

    fun getCurrentItemBlocking(): ShelfdroidMediaItem {
        return runBlocking {
            val jsonString = readString(Keys.CURRENT_ITEM).first()
            if (currentItemType.first() == MediaItemType.Podcast.name) {
                json.decodeFromString<MediaItemPodcast>(jsonString)
            } else if (currentItemType.first() == MediaItemType.Book.name) {
                json.decodeFromString<MediaItemBook>(jsonString)
            }
            json.decodeFromString<ShelfdroidMediaItem>(jsonString)
        }
    }

    val currentItemType: Flow<String> = readString(Keys.CURRENT_ITEM_TYPE)
    suspend fun setCurrentItemType(value: String) {
        writeString(Keys.CURRENT_ITEM_TYPE, value)
    }

    suspend fun clear() = dataStore.edit { it.clear() }


    val baseUrl: Flow<String> = readString(Keys.BASE_URL)
    suspend fun setBaseUrl(value: String) {
        Holder.baseUrl = value
        writeString(Keys.BASE_URL, value)
    }

    val deviceId: Flow<String> = readString(Keys.DEVICE_ID)
    val deviceIdBlocking: String = runBlocking { dataStore.data.firstOrNull()?.get(Keys.DEVICE_ID) ?: "" }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun generateDeviceId() {
        val uuid = Uuid.random().toString()
        if (readString(Keys.DEVICE_ID).first().isBlank()) {
            Holder.deviceId = uuid
            writeString(Keys.DEVICE_ID, uuid)
        }
    }

    val token: Flow<String> = readString(Keys.TOKEN)
    suspend fun setToken(value: String) {
        Holder.token = value
        writeString(Keys.TOKEN, value)
    }

    val tokenBlocking: String? = runBlocking {
        dataStore.data.firstOrNull()?.get(Keys.TOKEN)
    }

    val userId: Flow<String> = readString(Keys.USER_ID)
    suspend fun setUserId(value: String) {
        Holder.userId = value
        writeString(Keys.USER_ID, value)
    }

    val isDarkMode: Flow<Boolean> = readBoolean(Keys.IS_DARK_MODE, true)
    suspend fun setDarkMode(value: Boolean) = writeBoolean(Keys.IS_DARK_MODE, value)

    val currentPosition: Flow<Long> = readLong(Keys.CURRENT_POSITION, 0)
    val currentPositionBlocking: Long = runBlocking { dataStore.data.firstOrNull()?.get(Keys.CURRENT_POSITION) ?: 0 }
    suspend private fun setCurrentPosition(value: Long) = writeLong(Keys.CURRENT_POSITION, value)

}