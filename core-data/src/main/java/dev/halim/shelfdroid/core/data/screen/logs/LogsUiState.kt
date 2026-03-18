package dev.halim.shelfdroid.core.data.screen.logs

import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.data.GenericState

data class LogsUiState(
  val state: GenericState = GenericState.Loading,
  val logs: List<LogItem> = emptyList(),
  val logLevel: LogLevel = LogLevel.DEBUG,
  val filterLogLevel: LogLevel = LogLevel.DEBUG,
) {

  sealed interface LogItem {
    val id: Int

    data class Log(
      override val id: Int,
      val level: LogLevel,
      val message: String,
      val time: String?,
    ) : LogItem

    data class HourHeader(override val id: Int, val hour: String) : LogItem
  }
}

sealed interface LogsUiEvent {
  data object ChangeLogLevelSuccess : LogsUiEvent

  data object ChangeLogLevelError : LogsUiEvent

  data object GetLogDataError : LogsUiEvent
}
