package dev.halim.shelfdroid.core.data.screen.libraryadmin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminCreateValidationTest {

  private val bookProviders = listOf(LibraryAdminProvider("audible", "Audible"))

  @Test
  fun validDraft_trimsNameAndAllowsFolderThatDoesNotExistYet() {
    val validation =
      validateLibraryAdminDraft(
        LibraryAdminDraft(
          name = "  Audiobooks  ",
          folders = listOf("/server/new-library"),
          bookProvider = "audible",
        ),
        bookProviders,
      )

    assertTrue(validation.isValid)
    assertEquals("Audiobooks", LibraryAdminDraft(name = " Audiobooks ").name.trim())
  }

  @Test
  fun validationRejectsBlankNameMissingFoldersAndProviderFailure() {
    val validation =
      validateLibraryAdminDraft(
        LibraryAdminDraft(name = " "),
        null,
      )

    assertFalse(validation.isValid)
    assertEquals(
      listOf(LibraryAdminCreateError.NAME_REQUIRED),
      validation.errors[LibraryAdminCreateField.NAME],
    )
    assertEquals(
      listOf(LibraryAdminCreateError.PROVIDER_UNAVAILABLE),
      validation.errors[LibraryAdminCreateField.PROVIDER],
    )
    assertEquals(
      listOf(LibraryAdminCreateError.FOLDERS_REQUIRED),
      validation.errors[LibraryAdminCreateField.FOLDERS],
    )
  }

  @Test
  fun validationRejectsDuplicateAndParentChildFolders() {
    val validation =
      validateLibraryAdminDraft(
        LibraryAdminDraft(
          name = "Books",
          folders = listOf("/media/books", "/media/books/", "/media/books/children"),
          bookProvider = "audible",
        ),
        bookProviders,
      )

    assertEquals(
      listOf(
        LibraryAdminCreateError.DUPLICATE_FOLDER,
        LibraryAdminCreateError.OVERLAPPING_FOLDER,
      ),
      validation.errors[LibraryAdminCreateField.FOLDERS],
    )
  }

  @Test
  fun validationTreatsWindowsPathsCaseInsensitivelyAndNormalizesSeparators() {
    val validation =
      validateLibraryAdminDraft(
        LibraryAdminDraft(
          name = "Podcasts",
          folders = listOf("C:\\Media\\Podcasts", "c:/media/podcasts/Shows"),
          podcastProvider = "itunes",
          mediaType = LibraryAdminMediaType.PODCAST,
        ),
        listOf(LibraryAdminProvider("itunes", "iTunes")),
      )

    assertEquals(
      listOf(LibraryAdminCreateError.OVERLAPPING_FOLDER),
      validation.errors[LibraryAdminCreateField.FOLDERS],
    )
    assertEquals("C:/Media/Podcasts", normalizeLibraryFolderPath("C:\\Media\\Podcasts\\"))
  }

  @Test
  fun validationRejectsInvalidFinishThresholdAndDisabledScanner() {
    val draft =
      LibraryAdminDraft(
        name = "Books",
        folders = listOf("/books"),
        bookProvider = "audible",
        bookSettings = LibraryAdminBookSettings(markAsFinishedPercentComplete = 101),
        metadataSources = LibraryAdminDraft().metadataSources.map { it.copy(enabled = false) },
      )

    val validation = validateLibraryAdminDraft(draft, bookProviders)

    assertEquals(
      listOf(LibraryAdminCreateError.INVALID_FINISH_THRESHOLD),
      validation.errors[LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD],
    )
    assertEquals(
      listOf(LibraryAdminCreateError.SCANNER_PRECEDENCE_REQUIRED),
      validation.errors[LibraryAdminCreateField.SCANNER_PRECEDENCE],
    )
  }
}
