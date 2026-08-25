package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState

data class LibraryAdministrationUiState(
  val state: GenericState = GenericState.Loading,
  val libraries: List<LibraryAdministrationLibrary> = emptyList(),
  val isRefreshing: Boolean = true,
  val connectionState: LibraryAdministrationConnectionState =
    LibraryAdministrationConnectionState.UNKNOWN,
  val taskStates: Map<String, LibraryAdministrationTaskState> = emptyMap(),
  val reorderError: String? = null,
  val isReordering: Boolean = false,
)

fun LibraryAdministrationUiState.canReorder(libraryId: String): Boolean =
  connectionState == LibraryAdministrationConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    // Moving one row also changes every row it crosses. Require a known idle snapshot for the
    // whole ordered set so an active/unknown library cannot be shifted indirectly.
    libraries.all { taskStates[it.id] == LibraryAdministrationTaskState.IDLE }
