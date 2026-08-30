package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Folder
import dev.halim.core.network.response.Library
import dev.halim.core.network.response.LibrarySettings
import dev.halim.core.network.response.NetworkMediaType
import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminEditMappingTest {

  @Test
  fun serverLibraryMapsAllEditableFieldsAndExistingFolderIds() {
    val snapshot = serverLibrary().toEditSnapshot()

    assertEquals("Books", snapshot.draft.name)
    assertEquals("audible", snapshot.draft.bookProvider)
    assertEquals(listOf("/books"), snapshot.draft.folders)
    assertEquals(mapOf("/books" to "folder-1"), snapshot.folderIdsByPath)
    assertTrue(snapshot.draft.bookSettings.audiobooksOnly)
    assertEquals(75, snapshot.draft.bookSettings.markAsFinishedPercentComplete)
    assertNull(snapshot.draft.bookSettings.markAsFinishedTimeRemaining)
    assertEquals(
      LibraryAdminScheduleInterval.Every30Minutes,
      snapshot.draft.schedule.simple.interval,
    )
    assertEquals(
      listOf("folderStructure", "audioMetatags"),
      snapshot.draft.metadataPrecedence,
    )
  }

  @Test
  fun changedFolderPayloadPreservesIdsAndDisabledScheduleSendsExplicitNull() {
    val original = serverLibrary().toEditSnapshot()
    val updated =
      original.draft.copy(
        name = "My Books",
        folders = listOf("/books", "/new-books"),
        schedule = original.draft.schedule.copy(enabled = false),
      )

    val request = buildUpdateLibraryRequest(original, updated)

    assertEquals("My Books", request.name)
    assertEquals("folder-1", request.folders!![0].id)
    assertNull(request.folders!![1].id)
    assertEquals(JsonNull, request.settings!!["autoScanCronExpression"])
  }

  private fun serverLibrary() =
    Library(
      id = "books",
      name = "Books",
      folders = listOf(Folder(id = "folder-1", fullPath = "/books")),
      icon = "books-2",
      mediaType = NetworkMediaType.BOOK,
      provider = "audible",
      settings =
        LibrarySettings(
          coverAspectRatio = 1,
          audiobooksOnly = true,
          metadataPrecedence = listOf("folderStructure", "audioMetatags"),
          markAsFinishedPercentComplete = 75,
          markAsFinishedTimeRemaining = null,
          autoScanCronExpression = "*/30 * * * *",
        ),
    )
}
