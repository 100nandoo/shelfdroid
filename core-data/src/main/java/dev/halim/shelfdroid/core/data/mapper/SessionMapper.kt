package dev.halim.shelfdroid.core.data.mapper

import dev.halim.core.network.response.DeviceInfo
import dev.halim.core.network.response.MediaType
import dev.halim.core.network.response.Session as NetworkSession
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Device
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Item
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.SessionTime
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class SessionMapper @Inject constructor(private val helper: Helper) {

  fun sessions(sessions: List<NetworkSession>): List<Session> {
    val result =
      sessions.map { session ->
        val item = item(session)
        val device = device(session)
        val sessionTime = sessionTime(session)
        val user = user(session)
        val playerInfo = playerInfo(session)
        Session(session.id, item, device, sessionTime, user, playerInfo)
      }
    return result
  }

  private fun item(session: NetworkSession): Item {
    val narrator =
      if (MediaType.isBook(session.mediaType))
        runCatching { (session.mediaMetadata as BookMetadata).narrators.joinToString() }
          .getOrElse { "" }
      else ""

    val cover = session.libraryItemId?.let { helper.generateItemCoverUrl(it) } ?: ""
    return Item(session.displayAuthor, session.displayTitle, narrator, cover)
  }

  private fun device(session: NetworkSession): Device {
    val deviceInfo = session.deviceInfo
    val isWeb = deviceInfo.browserName != null && deviceInfo.browserVersion != null
    return if (isWeb) mapWebDevice(deviceInfo) else mapAndroidDevice(deviceInfo)
  }

  private fun mapWebDevice(deviceInfo: DeviceInfo): Device {
    val deviceName = "${deviceInfo.osName} ${deviceInfo.osVersion}"
    return Device(
      device = deviceName,
      client = "${deviceInfo.clientName} ${deviceInfo.clientVersion}",
      browser = "${deviceInfo.browserName} ${deviceInfo.browserVersion}",
      ip = deviceInfo.ipAddress,
    )
  }

  private fun mapAndroidDevice(deviceInfo: DeviceInfo): Device {
    val deviceName = "${deviceInfo.manufacturer} ${deviceInfo.model}"
    return Device(
      device = deviceName,
      client = "${deviceInfo.clientName} ${deviceInfo.clientVersion}",
      ip = deviceInfo.ipAddress,
    )
  }

  private fun sessionTime(session: NetworkSession): SessionTime {
    return SessionTime(
      helper.formatDurationShort(session.timeListening),
      session.currentTime,
      helper.toReadableDate(session.startedAt, true),
      helper.toReadableDate(session.updatedAt, true),
      helper.formatChapterTime(session.startTime),
      helper.formatChapterTime(session.currentTime),
      helper.formatSessionTimeRange(session.startedAt, session.updatedAt),
    )
  }

  private fun user(session: NetworkSession): User {
    return User(session.user?.id, session.user?.username)
  }

  private fun playerInfo(session: NetworkSession): ListeningSessionUiState.PlayerInfo {
    return ListeningSessionUiState.PlayerInfo(
      session.mediaPlayer,
      helper.toReadablePlayMethod(session.playMethod),
    )
  }
}
