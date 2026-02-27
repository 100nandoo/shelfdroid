package dev.halim.shelfdroid.core.data.screen.listeningstat

import dev.halim.shelfdroid.core.data.GenericState

data class ListeningStatUiState(
  val state: GenericState = GenericState.Loading,
  val totalTime: String = "",
  val today: String = "",
  val days: Map<String, Int> = emptyMap(),
  val dayOfWeek: Map<String, Int> = emptyMap(),
  val total: Total = Total(),
  val thisWeek: ThisWeek = ThisWeek(),
) {
  data class ThisWeek(
    val days: String = "",
    val daysDelta: Int = 0,
    val minutes: String = "",
    val minutesDelta: Float = 0.0f,
    val mostMinutes: String = "",
    val streak: String = "",
    val streakDelta: Int = 0,
    val dailyAverage: String = "",
    val dailyAverageDelta: Float = 0.0f,
  )

  data class Total(val days: String = "", val minutes: String = "")
}
