package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogsResponse(
  @SerialName("currentDailyLogs") val currentDailyLogs: List<CurrentDailyLog>
) {
  @Serializable
  data class CurrentDailyLog(
    @SerialName("timestamp") val timestamp: String,
    @SerialName("source") val source: String,
    @SerialName("message") val message: String,
    @SerialName("levelName") val levelName: String,
    @SerialName("level") val level: Int,
  )
}
