package dev.halim.shelfdroid.core.data.screen.listeningstat

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.database.ListeningStatEntity
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class ListeningStatMapper @Inject constructor(private val helper: Helper) {
  fun toUiState(entity: ListeningStatEntity): ListeningStatUiState {
    val totalTime = helper.formatDurationLong(entity.totalTime)
    val today = helper.formatDurationLong(entity.today)
    return ListeningStatUiState(
      state = GenericState.Success,
      totalTime = totalTime,
      today = today,
      days = entity.days ?: emptyMap(),
      dayOfWeek = entity.dayOfWeek ?: emptyMap(),
    )
  }
}
