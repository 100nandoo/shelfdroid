package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncLocalAllSessionRequest(
  @SerialName("sessions") val sessions: List<SyncLocalSessionRequest>
)
