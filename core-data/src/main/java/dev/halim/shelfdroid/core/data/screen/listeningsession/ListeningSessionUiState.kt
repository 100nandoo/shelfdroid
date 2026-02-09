package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.shelfdroid.core.data.GenericState

data class ListeningSessionUiState(
  val state: GenericState = GenericState.Idle,
  val sessions: List<Session> = emptyList(),
) {

  data class Session(
    val id: String,
    val pageInfo: PageInfo,
    val item: Item,
    val device: Device,
    val sessionTime: SessionTime,
    val user: User,
  )

  data class PageInfo(
    val total: Int,
    val numPages: Int,
    val page: Int,
    val itemsPerPage: Int,
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

  data class User(val id: String, val username: String)
}
