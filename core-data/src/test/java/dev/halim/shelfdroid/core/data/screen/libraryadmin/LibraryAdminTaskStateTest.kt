package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.GenericState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminTaskStateTest {

  private val libraries =
    listOf(
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = MediaType.BOOK,
        displayOrder = 0,
      )
    )

  @Test
  fun scanIsDisabledUntilConnectedSnapshotIsKnownAndIdle() {
    val unknown = LibraryAdminUiState(state = GenericState.Success, libraries = libraries)
    val disconnected =
      unknown.copy(
        connectionState = LibraryAdminConnectionState.DISCONNECTED,
        taskStates = mapOf("books" to LibraryAdminTaskState.IDLE),
      )
    val active =
      unknown.copy(
        connectionState = LibraryAdminConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdminTaskState.ACTIVE),
      )
    val idle =
      unknown.copy(
        connectionState = LibraryAdminConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdminTaskState.IDLE),
      )

    assertFalse(unknown.canStartScan("books"))
    assertFalse(disconnected.canStartScan("books"))
    assertFalse(active.canStartScan("books"))
    assertTrue(idle.canStartScan("books"))
  }

  @Test
  fun unknownMediaType_doesNotExposeScanAction() {
    val unknownMediaType = libraries.map { it.copy(mediaType = MediaType.UNKNOWN) }
    val state =
      LibraryAdminUiState(
        state = GenericState.Success,
        libraries = unknownMediaType,
        connectionState = LibraryAdminConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdminTaskState.IDLE),
      )

    assertFalse(state.canStartScan("books"))
  }

  @Test
  fun matchIsBookOnlyAndUsesTheSameConnectionAndTaskGates() {
    val podcasts = libraries.map { it.copy(mediaType = MediaType.PODCAST) }
    val idle =
      LibraryAdminUiState(
        state = GenericState.Success,
        libraries = libraries,
        connectionState = LibraryAdminConnectionState.CONNECTED,
        taskStates = mapOf("books" to LibraryAdminTaskState.IDLE),
      )
    val disconnected = idle.copy(connectionState = LibraryAdminConnectionState.DISCONNECTED)
    val active = idle.copy(taskStates = mapOf("books" to LibraryAdminTaskState.ACTIVE))
    val podcastState = idle.copy(libraries = podcasts)

    assertTrue(idle.canStartMatch("books"))
    assertFalse(disconnected.canStartMatch("books"))
    assertFalse(active.canStartMatch("books"))
    assertFalse(podcastState.canStartMatch("books"))
  }
}
