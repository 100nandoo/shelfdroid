package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState

data class LibraryAdministrationUiState(
  val state: GenericState = GenericState.Loading,
  val libraries: List<LibraryAdministrationLibrary> = emptyList(),
  val isRefreshing: Boolean = true,
)
