@file:OptIn(ExperimentalTime::class)

package dev.halim.shelfdroid.core.data.response

import android.util.Log
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.SyncLocalSessionRequest
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.MEDIA_TYPE_BOOK
import dev.halim.shelfdroid.core.Device
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.screen.player.PlayerFinder
import dev.halim.shelfdroid.core.database.LocalSessionEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

class LocalSessionRepo
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  private val libraryItemRepo: LibraryItemRepo,
  private val finder: PlayerFinder,
  private val helper: Helper,
  private val device: Device,
  private val api: ApiService,
  db: MyDatabase,
) {

  private suspend fun getDeviceId(): String =
    withContext(Dispatchers.IO) { dataStoreManager.getDeviceId() }

  private val queries = db.localSessionEntityQueries

  fun count(): Flow<Long?> = queries.count().asFlow().mapToOneOrNull(Dispatchers.IO)

  suspend fun startBook(uiState: PlayerUiState) {
    val userPrefs = dataStoreManager.userPrefs.firstOrNull()
    val serverPrefs = dataStoreManager.serverPrefs.firstOrNull()
    val book = libraryItemRepo.byId(uiState.id)
    if (userPrefs == null) return
    if (serverPrefs == null) return
    if (book == null) return

    book
      .takeIf { it.isBook == 1L }
      .let {
        val media = Json.decodeFromString<Book>(book.media)
        val mediaMetadata = Json.encodeToString(media.metadata)
        val chapters = Json.encodeToString(media.chapters)
        val startTimeServer =
          uiState.currentTime + (uiState.currentChapter?.startTimeSeconds ?: 0.0)

        val deviceInfo =
          DeviceInfo(
            deviceId = getDeviceId(),
            manufacturer = device.manufacturer,
            model = device.model,
            osVersion = device.osVersion,
            sdkVersion = device.sdkVersion,
            clientName = device.clientName,
            clientVersion = device.clientVersion,
          )
        val deviceInfoString = Json.encodeToString(deviceInfo)

        val now = Clock.System.now()
        val nowLocal = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDateString = nowLocal.date.toString()
        val dayOfWeek = nowLocal.dayOfWeek.toString()
        val startAt = now.toEpochMilliseconds()
        val entity =
          LocalSessionEntity(
            id = uiState.sessionId,
            userId = userPrefs.id,
            libraryId = book.libraryId,
            libraryItemId = book.id,
            episodeId = null,
            mediaType = MEDIA_TYPE_BOOK,
            mediaMetadata = mediaMetadata,
            chapters = chapters,
            displayTitle = book.title,
            displayAuthor = book.author,
            coverPath = media.coverPath ?: "",
            duration = media.duration ?: 0.0,
            playMethod = 0L,
            mediaPlayer = device.mediaPlayer,
            deviceInfo = deviceInfoString,
            serverVersion = serverPrefs.version,
            date = currentDateString,
            dayOfWeek = dayOfWeek,
            timeListening = 0L,
            startTime = startTimeServer,
            currentTime = startTimeServer,
            startedAt = startAt,
            updatedAt = startAt,
          )
        queries.insert(entity)
      }
  }

  fun syncLocal(uiState: PlayerUiState, rawPositionMs: Long) {
    val now = helper.nowMilis()
    val currentTime = finder.bookPosition(uiState, rawPositionMs)

    queries.update(currentTime.toDouble(), now, uiState.sessionId)
  }

  suspend fun syncToServer() {
    val entities = queries.all().executeAsList()
    val isOne = entities.size == 1
    if (isOne) {
      val entity = entities.first()
      val result = api.syncLocalSession(toRequest(entity))
      Log.d("LocalSessionRepo", "${result.exceptionOrNull()?.toString()}")
      if (result.isSuccess) {
        queries.deleteById(entity.id)
      }
    } else {
      //      val listRequest = entities.map { toRequest(it) }
      //      val result = api.syncLocalSessions(listRequest)
    }
  }

  private fun toRequest(entity: LocalSessionEntity): SyncLocalSessionRequest {
    val mediaMetadata = Json.decodeFromString<BookMetadata>(entity.mediaMetadata)
    val chapters = Json.decodeFromString<List<BookChapter>>(entity.chapters)
    val deviceInfo = Json.decodeFromString<DeviceInfo>(entity.deviceInfo)
    return SyncLocalSessionRequest(
      entity.id,
      entity.userId,
      entity.libraryId,
      entity.libraryItemId,
      entity.episodeId,
      entity.mediaType,
      mediaMetadata,
      chapters,
      entity.displayTitle,
      entity.displayAuthor,
      entity.coverPath,
      entity.duration,
      entity.playMethod.toInt(),
      entity.mediaPlayer,
      deviceInfo,
      entity.serverVersion,
      entity.date,
      entity.dayOfWeek,
      entity.timeListening.toInt(),
      entity.startTime,
      entity.currentTime,
      entity.startedAt,
      entity.updatedAt,
    )
  }
}
