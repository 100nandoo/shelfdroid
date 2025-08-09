@file:OptIn(ExperimentalTime::class)

package dev.halim.shelfdroid.core.data.session

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.SyncLocalAllSessionRequest
import dev.halim.core.network.request.SyncLocalSessionRequest
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.MEDIA_TYPE_BOOK
import dev.halim.shelfdroid.core.Device
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.screen.player.PlayerFinder
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.LocalSessionEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
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
  private val playerInternalStateHolder: PlayerInternalStateHolder,
  db: MyDatabase,
) {

  private fun getDeviceId(): String = runBlocking { dataStoreManager.getDeviceId() }

  private val queries = db.localSessionEntityQueries
  private val progressQueries = db.progressEntityQueries
  private val syncMutex = Mutex()

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
        val media = Json.Default.decodeFromString<Book>(book.media)
        val mediaMetadata = Json.Default.encodeToString(media.metadata)
        val entity = createEntity(media, uiState, userPrefs, book, mediaMetadata, serverPrefs)
        queries.insert(entity)
      }
  }

  fun syncLocal(uiState: PlayerUiState, rawPositionMs: Long) {
    val now = helper.nowMilis()
    val currentTime = finder.bookPosition(playerInternalStateHolder.startOffset(), rawPositionMs)

    queries.update(currentTime, now, playerInternalStateHolder.sessionId())
    progressQueries.updateBookCurrentTime(currentTime = currentTime, uiState.id)
  }

  suspend fun syncToServer() {
    if (!syncMutex.tryLock()) return
    try {
      val entities = queries.all().executeAsList()
      val isOne = entities.size == 1

      if (isOne) {
        val entity = entities.first()
        val result = api.syncLocalSession(toRequest(entity))
        if (result.isSuccess && playerInternalStateHolder.sessionId() != entity.id) {
          queries.deleteById(entity.id)
        }
      } else {
        val listRequest = entities.map { toRequest(it) }
        val response = api.syncLocalAllSession(SyncLocalAllSessionRequest(listRequest)).getOrNull()
        response?.results?.forEach {
          if (it.success && it.id != playerInternalStateHolder.sessionId()) {
            queries.deleteById(it.id)
          }
        }
      }
    } finally {
      syncMutex.unlock()
    }
  }

  private fun calculateDateDayAndStartAt(): Triple<String, String, Long> {
    val now = Clock.System.now()
    val nowLocal = now.toLocalDateTime(TimeZone.Companion.currentSystemDefault())
    val currentDateString = nowLocal.date.toString()
    val dayOfWeek = nowLocal.dayOfWeek.toString()
    val startAt = now.toEpochMilliseconds()
    return Triple(currentDateString, dayOfWeek, startAt)
  }

  private fun createDeviceInfoString(): String {
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
    val deviceInfoString = Json.Default.encodeToString(deviceInfo)
    return deviceInfoString
  }

  private fun createEntity(
    media: Book,
    uiState: PlayerUiState,
    userPrefs: UserPrefs,
    book: LibraryItemEntity,
    mediaMetadata: String,
    serverPrefs: ServerPrefs,
  ): LocalSessionEntity {
    val chapters = Json.Default.encodeToString(media.chapters)
    val startTimeServer = uiState.currentTime + (uiState.currentChapter?.startTimeSeconds ?: 0.0)

    val deviceInfoString = createDeviceInfoString()
    val (currentDateString, dayOfWeek, startAt) = calculateDateDayAndStartAt()

    val entity =
      LocalSessionEntity(
        id = playerInternalStateHolder.sessionId(),
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
    return entity
  }

  private fun toRequest(entity: LocalSessionEntity): SyncLocalSessionRequest {
    val mediaMetadata = Json.Default.decodeFromString<BookMetadata>(entity.mediaMetadata)
    val chapters = Json.Default.decodeFromString<List<BookChapter>>(entity.chapters)
    val deviceInfo = Json.Default.decodeFromString<DeviceInfo>(entity.deviceInfo)
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
