package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.response.SessionsResponse
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.mapper.SessionMapper
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.PageInfo
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USER
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.UserAndCountFilter
import dev.halim.shelfdroid.core.database.UserEntity
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class ListeningSessionMapper
@Inject
constructor(private val helper: Helper, private val sessionMapper: SessionMapper) {

  fun combine(
    response: SessionsResponse,
    users: List<User>,
    userId: String?,
  ): ListeningSessionUiState {
    val pageInfo = pageInfo(response)
    val selectedUser = users.firstOrNull { it.id == userId } ?: ALL_USER
    val userAndCountFilter = UserAndCountFilter(selectedUser = selectedUser, users = users)
    val sessions = sessionMapper.sessions(response.sessions)

    return ListeningSessionUiState(
      GenericState.Success,
      ListeningSessionApiState.Idle,
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
    val sessions = sessionMapper.sessions(response.sessions)
    val users = users.filter { it.id.isNotBlank() }.map { user -> User(user.id, user.username) }

    val combineUsers = listOf(ALL_USER) + users
    val selectedUser = users.firstOrNull { it.id == userId } ?: ALL_USER
    val userAndCountFilter = UserAndCountFilter(users = combineUsers, selectedUser = selectedUser)

    return ListeningSessionUiState(
      GenericState.Success,
      ListeningSessionApiState.Idle,
      sessions,
      pageInfo,
      userAndCountFilter = userAndCountFilter,
    )
  }

  private fun pageInfo(response: SessionsResponse): PageInfo {
    return PageInfo(
      total = response.total,
      numPages = response.numPages,
      page = response.page,
      inputPage = response.page + 1,
    )
  }
}
