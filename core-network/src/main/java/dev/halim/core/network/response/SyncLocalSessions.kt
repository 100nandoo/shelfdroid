package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncLocalAllSessionResponse(@SerialName("results") val results: List<Result>)

@Serializable
data class Result(
  @SerialName("id") val id: String,
  @SerialName("success") val success: Boolean,
  @SerialName("error") val error: Boolean? = null,
  @SerialName("progressSynced") val progressSynced: Boolean? = null,
)
