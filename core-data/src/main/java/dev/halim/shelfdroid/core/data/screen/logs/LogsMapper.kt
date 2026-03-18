package dev.halim.shelfdroid.core.data.screen.logs

import dev.halim.core.network.response.LogsResponse
import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.datetime.LocalDateTime

class LogsMapper @Inject constructor(val helper: Helper) {
  fun items(response: LogsResponse): List<LogsUiState.LogItem> {
    val result = mutableListOf<LogsUiState.LogItem>()
    var lastHour: String? = null
    var nextId = 0

    response.currentDailyLogs.forEach { log ->
      val dateTime = helper.toLocalDateTime(log.timestamp)
      val hour = dateTime?.let { helper.formatHour(it) }

      if (hour != null && hour != lastHour) {
        result.add(hourHeader(nextId++, hour))
        lastHour = hour
      }

      result.add(log(nextId++, log, dateTime))
    }

    return result.reversed()
  }

  private fun hourHeader(id: Int, hour: String): LogsUiState.LogItem.HourHeader {
    return LogsUiState.LogItem.HourHeader(id, hour)
  }

  private fun log(
    id: Int,
    log: LogsResponse.CurrentDailyLog,
    ldt: LocalDateTime?,
  ): LogsUiState.LogItem.Log {
    val logLevel = LogLevel.from(log.level)
    val time = ldt?.let { "%02d:%02d:%02d".format(ldt.hour, ldt.minute, ldt.second) }
    val result = LogsUiState.LogItem.Log(id, logLevel, log.message, time)
    return result
  }
}
