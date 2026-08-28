package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Maps both HTTP snapshots and socket payloads into the same stable task model. */
internal fun NetworkServerTask.toDomainTask(): ServerTask {
  val taskData = data ?: emptyMap()
  val libraryId = taskData["libraryId"]?.jsonPrimitive?.content
  val scanResults = taskData["scanResults"]?.jsonObject
  val result = scanResults?.let {
    ServerTaskResult(
      added = it["added"]?.jsonPrimitive?.intOrNull,
      updated = it["updated"]?.jsonPrimitive?.intOrNull,
      missing = it["missing"]?.jsonPrimitive?.intOrNull,
      elapsedMillis = it["elapsed"]?.jsonPrimitive?.longOrNull,
    )
  }
  val status =
    when {
      !isFinished -> ServerTaskStatus.ACTIVE
      isFailed -> ServerTaskStatus.FAILED
      descriptionKey == "MessageTaskCanceledByUser" ||
        description?.contains("canceled", ignoreCase = true) == true -> ServerTaskStatus.CANCELLED
      else -> ServerTaskStatus.COMPLETED
    }
  return ServerTask(
    id = id,
    action = ServerTaskAction.fromRaw(action),
    libraryId = libraryId,
    title = title,
    status = status,
    startedAt = startedAt,
    finishedAt = finishedAt,
    result = result,
    error = error.toServerTaskError(),
  )
}

private fun String?.toServerTaskError(): ServerTaskError? {
  if (isNullOrBlank()) return null
  return if (length <= 240 && !contains("Exception", ignoreCase = true) && !contains(" at ")) {
    ServerTaskError.SafeMessage(this)
  } else {
    ServerTaskError.Generic
  }
}
