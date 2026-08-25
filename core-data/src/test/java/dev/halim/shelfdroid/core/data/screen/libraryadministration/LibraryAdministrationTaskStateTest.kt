package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationTaskStateTest {

  private val libraries =
    listOf(
      LibraryAdministrationLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = 0,
      )
    )

  @Test
  fun scanIsDisabledUntilConnectedSnapshotIsKnownAndIdle() {
    val unknown = LibraryAdministrationUiState(state = GenericState.Success, libraries = libraries)
    val disconnected =
      unknown.copy(
        connectionState = LibraryAdministrationConnectionState.DISCONNECTED,
        taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
      )
    val active =
      unknown.copy(
        connectionState = LibraryAdministrationConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdministrationTaskState.ACTIVE),
      )
    val idle =
      unknown.copy(
        connectionState = LibraryAdministrationConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
      )

    assertFalse(unknown.canStartScan("books"))
    assertFalse(disconnected.canStartScan("books"))
    assertFalse(active.canStartScan("books"))
    assertTrue(idle.canStartScan("books"))
  }

  @Test
  fun unknownMediaType_doesNotExposeScanAction() {
    val unknownMediaType =
      libraries.map { it.copy(mediaType = LibraryAdministrationMediaType.UNKNOWN) }
    val state =
      LibraryAdministrationUiState(
        state = GenericState.Success,
        libraries = unknownMediaType,
        connectionState = LibraryAdministrationConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
      )

    assertFalse(state.canStartScan("books"))
  }

  @Test
  fun matchIsBookOnlyAndUsesTheSameConnectionAndTaskGates() {
    val podcasts = libraries.map { it.copy(mediaType = LibraryAdministrationMediaType.PODCAST) }
    val idle =
      LibraryAdministrationUiState(
        state = GenericState.Success,
        libraries = libraries,
        connectionState = LibraryAdministrationConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
      )
    val disconnected = idle.copy(connectionState = LibraryAdministrationConnectionState.DISCONNECTED)
    val active = idle.copy(taskStates = mapOf("books" to LibraryAdministrationTaskState.ACTIVE))
    val podcastState = idle.copy(libraries = podcasts)

    assertTrue(idle.canStartMatch("books"))
    assertFalse(disconnected.canStartMatch("books"))
    assertFalse(active.canStartMatch("books"))
    assertFalse(podcastState.canStartMatch("books"))
  }
}
