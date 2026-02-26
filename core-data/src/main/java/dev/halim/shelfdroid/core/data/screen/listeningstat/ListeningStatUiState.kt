package dev.halim.shelfdroid.core.data.screen.listeningstat

import dev.halim.shelfdroid.core.data.GenericState

data class ListeningStatUiState(
  val state: GenericState = GenericState.Loading,
  val totalTime: String = "",
  val today: String = "",
  val days: Map<String, Int> = emptyMap(),
  val dayOfWeek: Map<String, Int> = emptyMap(),
)
