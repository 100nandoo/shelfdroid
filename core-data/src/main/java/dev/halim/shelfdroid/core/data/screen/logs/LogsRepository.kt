package dev.halim.shelfdroid.core.data.screen.logs

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.UpdateServerSettingsRequest
import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

class LogsRepository
@Inject
constructor(private val api: ApiService, private val logsMapper: LogsMapper) {

  suspend fun item(event: MutableSharedFlow<LogsUiEvent>): LogsUiState {
    val response = api.logs()
    val result =
      response.getOrElse {
        event.emit(LogsUiEvent.GetLogDataError)
        return LogsUiState(GenericState.Failure(it.message))
      }
    val logs = logsMapper.items(result)
    return LogsUiState(GenericState.Success, logs)
  }

  suspend fun changeLogLevel(
    uiState: LogsUiState,
    event: MutableSharedFlow<LogsUiEvent>,
    logLevel: LogLevel,
  ): LogsUiState {
    val request = UpdateServerSettingsRequest(logLevel.value)
    val response = api.updateSettings(request)
    response.getOrElse {
      event.emit(LogsUiEvent.ChangeLogLevelError)
      return uiState.copy(state = GenericState.Failure(it.message))
    }
    event.emit(LogsUiEvent.ChangeLogLevelSuccess)
    return uiState.copy(state = GenericState.Success)
  }
}
