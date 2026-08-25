package dev.halim.shelfdroid.core.data.screen.libraryadministration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationCreateValidationTest {

  private val bookProviders = listOf(LibraryAdministrationProvider("audible", "Audible"))

  @Test
  fun validDraft_trimsNameAndAllowsFolderThatDoesNotExistYet() {
    val validation =
      validateLibraryAdministrationDraft(
        LibraryAdministrationDraft(
          name = "  Audiobooks  ",
          folders = listOf("/server/new-library"),
          bookProvider = "audible",
        ),
        bookProviders,
      )

    assertTrue(validation.isValid)
    assertEquals("Audiobooks", LibraryAdministrationDraft(name = " Audiobooks ").name.trim())
  }

  @Test
  fun validationRejectsBlankNameMissingFoldersAndProviderFailure() {
    val validation =
      validateLibraryAdministrationDraft(
        LibraryAdministrationDraft(name = " "),
        null,
      )

    assertFalse(validation.isValid)
    assertEquals(
      listOf(LibraryAdministrationCreateError.NAME_REQUIRED),
      validation.errors[LibraryAdministrationCreateField.NAME],
    )
    assertEquals(
      listOf(LibraryAdministrationCreateError.PROVIDER_UNAVAILABLE),
      validation.errors[LibraryAdministrationCreateField.PROVIDER],
    )
    assertEquals(
      listOf(LibraryAdministrationCreateError.FOLDERS_REQUIRED),
      validation.errors[LibraryAdministrationCreateField.FOLDERS],
    )
  }

  @Test
  fun validationRejectsDuplicateAndParentChildFolders() {
    val validation =
      validateLibraryAdministrationDraft(
        LibraryAdministrationDraft(
          name = "Books",
          folders = listOf("/media/books", "/media/books/", "/media/books/children"),
          bookProvider = "audible",
        ),
        bookProviders,
      )

    assertEquals(
      listOf(
        LibraryAdministrationCreateError.DUPLICATE_FOLDER,
        LibraryAdministrationCreateError.OVERLAPPING_FOLDER,
      ),
      validation.errors[LibraryAdministrationCreateField.FOLDERS],
    )
  }

  @Test
  fun validationTreatsWindowsPathsCaseInsensitivelyAndNormalizesSeparators() {
    val validation =
      validateLibraryAdministrationDraft(
        LibraryAdministrationDraft(
          name = "Podcasts",
          folders = listOf("C:\\Media\\Podcasts", "c:/media/podcasts/Shows"),
          podcastProvider = "itunes",
          mediaType = LibraryAdministrationMediaType.PODCAST,
        ),
        listOf(LibraryAdministrationProvider("itunes", "iTunes")),
      )

    assertEquals(
      listOf(LibraryAdministrationCreateError.OVERLAPPING_FOLDER),
      validation.errors[LibraryAdministrationCreateField.FOLDERS],
    )
    assertEquals("C:/Media/Podcasts", normalizeLibraryFolderPath("C:\\Media\\Podcasts\\"))
  }

  @Test
  fun validationRejectsInvalidFinishThresholdAndDisabledScanner() {
    val draft =
      LibraryAdministrationDraft(
        name = "Books",
        folders = listOf("/books"),
        bookProvider = "audible",
        bookSettings = LibraryAdministrationBookSettings(markAsFinishedPercentComplete = 101),
        metadataSources = LibraryAdministrationDraft().metadataSources.map { it.copy(enabled = false) },
      )

    val validation = validateLibraryAdministrationDraft(draft, bookProviders)

    assertEquals(
      listOf(LibraryAdministrationCreateError.INVALID_FINISH_THRESHOLD),
      validation.errors[LibraryAdministrationCreateField.SETTINGS_FINISH_THRESHOLD],
    )
    assertEquals(
      listOf(LibraryAdministrationCreateError.SCANNER_PRECEDENCE_REQUIRED),
      validation.errors[LibraryAdministrationCreateField.SCANNER_PRECEDENCE],
    )
  }
}
