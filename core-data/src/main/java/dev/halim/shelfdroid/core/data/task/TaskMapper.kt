package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** Maps both HTTP snapshots and socket payloads into the same stable task model. */
internal fun NetworkServerTask.toDomainTask(): Task {
  val taskData = data ?: emptyMap()
  val libraryId = taskData["libraryId"]?.jsonPrimitive?.content
  val scanResults = taskData["scanResults"]?.jsonObject
  val result = scanResults?.let {
    TaskResult(
      added = it["added"]?.jsonPrimitive?.intOrNull,
      updated = it["updated"]?.jsonPrimitive?.intOrNull,
      missing = it["missing"]?.jsonPrimitive?.intOrNull,
      elapsedMillis = it["elapsed"]?.jsonPrimitive?.longOrNull,
    )
  }
  val status =
    when {
      !isFinished -> TaskStatus.ACTIVE
      isFailed -> TaskStatus.FAILED
      descriptionKey == "MessageTaskCanceledByUser" ||
        description?.contains("canceled", ignoreCase = true) == true -> TaskStatus.CANCELLED
      else -> TaskStatus.COMPLETED
    }
  return Task(
    id = id,
    action = TaskAction.fromRaw(action),
    libraryId = libraryId,
    title = title,
    status = status,
    startedAt = startedAt,
    finishedAt = finishedAt,
    result = result,
    error = error.toTaskError(),
  )
}

private fun String?.toTaskError(): TaskError? {
  if (isNullOrBlank()) return null
  return if (length <= 240 && !contains("Exception", ignoreCase = true) && !contains(" at ")) {
    TaskError.SafeMessage(this)
  } else {
    TaskError.Generic
  }
}
