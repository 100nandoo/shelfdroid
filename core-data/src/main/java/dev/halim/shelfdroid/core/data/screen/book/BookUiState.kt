package dev.halim.shelfdroid.core.data.screen.book

import dev.halim.shelfdroid.core.data.GenericState

data class BookUiState(
  val state: GenericState = GenericState.Loading,
  val author: String = "",
  val narrator: String = "",
  val title: String = "",
  val subtitle: String = "",
  val duration: String = "",
  val remaining: String = "",
  val cover: String = "",
  val description: String = "",
  val publishYear: String = "",
  val publisher: String = "",
  val genres: String = "",
  val language: String = "",
  val progress: String = "",
)
