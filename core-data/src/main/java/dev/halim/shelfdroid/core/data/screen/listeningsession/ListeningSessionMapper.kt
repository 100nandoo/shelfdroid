package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.response.MediaType
import dev.halim.core.network.response.Session
import dev.halim.core.network.response.SessionsResponse
import dev.halim.core.network.response.User
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USER
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class ListeningSessionMapper @Inject constructor(private val helper: Helper) {

  fun combine(
    response: SessionsResponse,
    users: List<ListeningSessionUiState.User>,
    userId: String?,
  ): ListeningSessionUiState {
    val pageInfo =
      pageInfo(response).copy(selectedUser = users.firstOrNull { it.id == userId } ?: ALL_USER)
    val sessions = sessions(response)
    return ListeningSessionUiState(GenericState.Success, sessions, pageInfo, users = users)
  }

  fun map(response: SessionsResponse, users: List<User>): ListeningSessionUiState {
    val pageInfo = pageInfo(response)
    val sessions = sessions(response)
    val users =
      users
        .filter { it.id.isNotBlank() }
        .map { user -> ListeningSessionUiState.User(user.id, user.username) }
    return ListeningSessionUiState(
      GenericState.Success,
      sessions,
      pageInfo,
      users = listOf(ListeningSessionUiState.User(null, "All")) + users,
    )
  }

  private fun sessions(response: SessionsResponse): List<ListeningSessionUiState.Session> {
    val sessions = response.sessions
    val result =
      sessions.map { session ->
        val item = item(session)
        val device = device(session)
        val sessionTime = sessionTime(session)
        val user = user(session)
        ListeningSessionUiState.Session(session.id, item, device, sessionTime, user)
      }
    return result
  }

  private fun pageInfo(response: SessionsResponse): ListeningSessionUiState.PageInfo {
    return ListeningSessionUiState.PageInfo(
      response.total,
      response.numPages,
      response.page,
      response.itemsPerPage,
      response.page + 1,
    )
  }

  private fun item(session: Session): ListeningSessionUiState.Item {
    val narrator =
      if (MediaType.isBook(session.mediaType))
        runCatching { (session.mediaMetadata as BookMetadata).narrators.joinToString() }
          .getOrElse { "" }
      else ""
    return ListeningSessionUiState.Item(session.displayAuthor, session.displayTitle, narrator)
  }

  private fun device(session: Session): ListeningSessionUiState.Device {
    return ListeningSessionUiState.Device(
      deviceName = session.deviceInfo.deviceName,
      clientName = session.deviceInfo.clientName,
      clientVersion = session.deviceInfo.clientVersion,
      ip = session.deviceInfo.ipAddress,
    )
  }

  private fun sessionTime(session: Session): ListeningSessionUiState.SessionTime {
    return ListeningSessionUiState.SessionTime(
      helper.formatDurationShort(session.timeListening),
      session.currentTime,
      session.startedAt,
      session.updatedAt,
      helper.formatSessionTimeRange(session.startedAt, session.updatedAt),
    )
  }

  private fun user(session: Session): ListeningSessionUiState.User {
    return ListeningSessionUiState.User(session.user?.id, session.user?.username)
  }
}
