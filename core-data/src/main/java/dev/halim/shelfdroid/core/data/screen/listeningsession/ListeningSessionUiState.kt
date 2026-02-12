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
    val id: String,
    val item: Item,
    val device: Device,
    val sessionTime: SessionTime,
    val user: User,
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

  data class Item(val author: String, val title: String, val narrator: String)

  data class Device(
    val deviceName: String?,
    val clientName: String?,
    val clientVersion: String?,
    val ip: String?,
  )

  data class SessionTime(
    val duration: String,
    val currentTime: Double,
    val startedAt: Long,
    val updatedAt: Long,
    val timeRange: String = "",
  )

  data class User(val id: String?, val username: String?) {
    companion object {
      const val ALL_USERNAME = "all"
      val ALL_USER = User(null, ALL_USERNAME)
    }
  }
}
