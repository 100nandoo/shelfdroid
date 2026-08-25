package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** The operation-agnostic task payload returned by Audiobookshelf's task manager. */
@Serializable
data class TasksResponse(
  @SerialName("tasks") val tasks: List<ServerTask> = emptyList(),
)

@Serializable
data class ServerTask(
  @SerialName("id") val id: String = "",
  @SerialName("action") val action: String = "",
  @SerialName("data") val data: JsonObject? = null,
  @SerialName("title") val title: String? = null,
  @SerialName("titleKey") val titleKey: String? = null,
  @SerialName("titleSubs") val titleSubs: List<String>? = null,
  @SerialName("description") val description: String? = null,
  @SerialName("descriptionKey") val descriptionKey: String? = null,
  @SerialName("descriptionSubs") val descriptionSubs: List<String>? = null,
  @SerialName("error") val error: String? = null,
  @SerialName("errorKey") val errorKey: String? = null,
  @SerialName("errorSubs") val errorSubs: List<String>? = null,
  @SerialName("showSuccess") val showSuccess: Boolean = false,
  @SerialName("isFailed") val isFailed: Boolean = false,
  @SerialName("isFinished") val isFinished: Boolean = false,
  @SerialName("startedAt") val startedAt: Long? = null,
  @SerialName("finishedAt") val finishedAt: Long? = null,
)
