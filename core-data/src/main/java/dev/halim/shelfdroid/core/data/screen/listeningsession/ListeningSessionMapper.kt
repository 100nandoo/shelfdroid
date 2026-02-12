package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.response.MediaType
import dev.halim.core.network.response.Session as NetworkSession
import dev.halim.core.network.response.SessionsResponse
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Device
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Item
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.PageInfo
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.SessionTime
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USER
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.UserAndCountFilter
import dev.halim.shelfdroid.core.database.UserEntity
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class ListeningSessionMapper @Inject constructor(private val helper: Helper) {

  fun combine(
    response: SessionsResponse,
    users: List<User>,
    userId: String?,
  ): ListeningSessionUiState {
    val pageInfo = pageInfo(response)
    val selectedUser = users.firstOrNull { it.id == userId } ?: ALL_USER
    val userAndCountFilter = UserAndCountFilter(selectedUser = selectedUser, users = users)
    val sessions = sessions(response)

    return ListeningSessionUiState(
      GenericState.Success,
      sessions,
      pageInfo,
      userAndCountFilter = userAndCountFilter,
    )
  }

  fun map(
    response: SessionsResponse,
    users: List<UserEntity>,
    userId: String?,
  ): ListeningSessionUiState {
    val pageInfo = pageInfo(response)
    val sessions = sessions(response)
    val users = users.filter { it.id.isNotBlank() }.map { user -> User(user.id, user.username) }

    val combineUsers = listOf(ALL_USER) + users
    val selectedUser = users.firstOrNull { it.id == userId } ?: ALL_USER
    val userAndCountFilter = UserAndCountFilter(users = combineUsers, selectedUser = selectedUser)

    return ListeningSessionUiState(
      GenericState.Success,
      sessions,
      pageInfo,
      userAndCountFilter = userAndCountFilter,
    )
  }

  private fun sessions(response: SessionsResponse): List<Session> {
    val sessions = response.sessions
    val result =
      sessions.map { session ->
        val item = item(session)
        val device = device(session)
        val sessionTime = sessionTime(session)
        val user = user(session)
        Session(session.id, item, device, sessionTime, user)
      }
    return result
  }

  private fun pageInfo(response: SessionsResponse): PageInfo {
    return PageInfo(
      total = response.total,
      numPages = response.numPages,
      page = response.page,
      inputPage = response.page + 1,
    )
  }

  private fun item(session: NetworkSession): Item {
    val narrator =
      if (MediaType.isBook(session.mediaType))
        runCatching { (session.mediaMetadata as BookMetadata).narrators.joinToString() }
          .getOrElse { "" }
      else ""
    return Item(session.displayAuthor, session.displayTitle, narrator)
  }

  private fun device(session: NetworkSession): Device {
    return Device(
      deviceName = session.deviceInfo.deviceName,
      clientName = session.deviceInfo.clientName,
      clientVersion = session.deviceInfo.clientVersion,
      ip = session.deviceInfo.ipAddress,
    )
  }

  private fun sessionTime(session: NetworkSession): SessionTime {
    return SessionTime(
      helper.formatDurationShort(session.timeListening),
      session.currentTime,
      session.startedAt,
      session.updatedAt,
      helper.formatSessionTimeRange(session.startedAt, session.updatedAt),
    )
  }

  private fun user(session: NetworkSession): User {
    return User(session.user?.id, session.user?.username)
  }
}
