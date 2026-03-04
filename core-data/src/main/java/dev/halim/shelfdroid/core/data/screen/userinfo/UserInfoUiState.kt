package dev.halim.shelfdroid.core.data.screen.userinfo

import dev.halim.shelfdroid.core.data.GenericState

data class UserInfoUiState(
  val state: GenericState = GenericState.Loading,
  val totalTime: String = "",
  val today: String = "",
  val days: Map<String, Int> = emptyMap(),
  val dayOfWeek: Map<String, Int> = emptyMap(),
  val total: Total = Total(),
  val thisWeek: ThisWeek? = null,
  val mediaProgress: List<MediaProgress> = emptyList(),
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

  data class MediaProgress(
    val id: String = "",
    val title: String = "",
    val cover: String = "",
    val isFinished: Boolean = false,
    val progress: String = "",
    val startAt: String = "",
    val lastUpdate: String = "",
  )
}
