package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USER

data class ListeningSessionUiState(
  val state: GenericState = GenericState.Idle,
  val sessions: List<Session> = emptyList(),
  val pageInfo: PageInfo = PageInfo(),
  val userAndCountFilter: UserAndCountFilter = UserAndCountFilter(),
) {

  data class Session(
    val id: String = "",
    val item: Item = Item(),
    val device: Device = Device(),
    val sessionTime: SessionTime = SessionTime(),
    val user: User = User(),
  )

  data class PageInfo(
    val total: Int = 0,
    val numPages: Int = 0,
    val page: Int = 0,
    val inputPage: Int = 1,
  )

  data class UserAndCountFilter(
    val selectedUser: User = ALL_USER,
    val itemsPerPage: Int = 10,
    val users: List<User> = emptyList(),
  )

  data class Item(
    val author: String = "",
    val title: String = "",
    val narrator: String = "",
    val cover: String = "",
  )

  data class Device(
    val client: String? = null,
    val device: String? = null,
    val browser: String? = null,
    val ip: String? = null,
  )

  data class SessionTime(
    val duration: String = "",
    val currentTime: Double = 0.0,
    val startedAt: String = "",
    val updatedAt: String = "",
    val startTime: String = "",
    val lastTime: String = "",
    val timeRange: String = "",
  )

  data class User(val id: String? = null, val username: String? = null) {
    companion object {
      const val ALL_USERNAME = "all"
      val ALL_USER = User(null, ALL_USERNAME)
    }
  }
}
