package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListeningStatResponse(
  @SerialName("totalTime") val totalTime: Double,
  @SerialName("today") val today: Double,
  @SerialName("days") val days: Map<String, Double>,
  @SerialName("dayOfWeek") val dayOfWeek: Map<String, Double>,
  @SerialName("recentSessions") val recentSessions: List<Session>,
)
