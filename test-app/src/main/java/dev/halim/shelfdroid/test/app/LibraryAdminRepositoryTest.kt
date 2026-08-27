package dev.halim.shelfdroid.test.app

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMutationResult
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminRepository
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.test.app.testdi.FakeApiService
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@HiltAndroidTest
class LibraryAdminRepositoryTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var repository: LibraryAdminRepository
  @Inject lateinit var database: MyDatabase
  @Inject lateinit var fakeApiService: FakeApiService

  @Before
  fun setUp() {
    hiltRule.inject()
    clearDatabase()
    fakeApiService.reset()
  }

  @Test
  fun reorder_serverRejectionRemainsFailureInsteadOfPartialSuccess() {
    fakeApiService.failNextLibraryReorder(IllegalStateException("server rejected"))

    val result = runBlocking { repository.reorderLibraries(order()) }

    assertTrue(result.isFailure)
  }

  @Test
  fun reorder_acceptedButSynchronizationFailureReturnsAcceptedOrderAsPartialSuccess() {
    fakeApiService.failLibraryDataSynchronization(IllegalStateException("catalog unavailable"))

    val result = runBlocking { repository.reorderLibraries(order()) }

    assertTrue(result.isSuccess)
    val outcome = result.getOrThrow()
    assertTrue(outcome is LibraryAdminMutationResult.AcceptedButNotSynchronized)
    assertEquals(listOf("podcasts", "books"), acceptedOrder(outcome))
    assertEquals(
      listOf("podcasts", "books"),
      database.libraryEntityQueries.all().executeAsList().map { it.id },
    )
  }

  @Test
  fun reorder_acceptedAndSynchronizedReturnsAccepted() {
    val result = runBlocking { repository.reorderLibraries(order()) }

    assertTrue(result.isSuccess)
    assertTrue(result.getOrThrow() is LibraryAdminMutationResult.Accepted)
  }

  @Test
  fun delete_serverRejectionRemainsFailureInsteadOfPartialSuccess() {
    fakeApiService.failNextLibraryDelete(IllegalStateException("server rejected"))

    val result = runBlocking { repository.deleteLibrary(FakeApiService.BOOK_LIBRARY_ID) }

    assertTrue(result.isFailure)
  }

  @Test
  fun delete_acceptedButSynchronizationFailureReturnsPartialSuccessAndRemovesCatalogLibrary() {
    seedLibraries()
    fakeApiService.failLibraryDataSynchronization(IllegalStateException("catalog unavailable"))

    val result = runBlocking { repository.deleteLibrary(FakeApiService.BOOK_LIBRARY_ID) }

    assertTrue(result.isSuccess)
    val outcome = result.getOrThrow()
    assertTrue(outcome is LibraryAdminMutationResult.AcceptedButNotSynchronized)
    assertEquals(
      listOf(FakeApiService.PODCAST_LIBRARY_ID),
      database.libraryEntityQueries.all().executeAsList().map { it.id },
    )
  }

  @Test
  fun delete_acceptedAndSynchronizedReturnsAccepted() {
    seedLibraries()

    val result = runBlocking { repository.deleteLibrary(FakeApiService.BOOK_LIBRARY_ID) }

    assertTrue(result.isSuccess)
    assertTrue(result.getOrThrow() is LibraryAdminMutationResult.Accepted)
  }

  @Test
  fun delete_partialSuccessCanRetrySynchronizationWithoutRepeatingDelete() {
    seedLibraries()
    fakeApiService.failNextLibraryDataSynchronization(IllegalStateException("catalog unavailable"))

    val result = runBlocking { repository.deleteLibrary(FakeApiService.BOOK_LIBRARY_ID) }

    assertTrue(result.isSuccess)
    assertTrue(
      result.getOrThrow() is LibraryAdminMutationResult.AcceptedButNotSynchronized
    )
    val retry = runBlocking { repository.synchronizeLibraries() }
    assertTrue(retry.isSuccess)
    assertEquals(
      listOf(FakeApiService.PODCAST_LIBRARY_ID),
      database.libraryEntityQueries.all().executeAsList().map { it.id },
    )
  }

  private fun order() =
    listOf(
      LibraryAdminLibrary(
        id = "podcasts",
        name = "Podcasts",
        mediaType = LibraryAdminMediaType.PODCAST,
        displayOrder = 1,
      ),
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdminMediaType.BOOK,
        displayOrder = 2,
      ),
    )

  private fun acceptedOrder(
    result: LibraryAdminMutationResult<List<LibraryAdminLibrary>>
  ): List<String> =
    when (result) {
      is LibraryAdminMutationResult.Accepted -> result.value.map { it.id }
      is LibraryAdminMutationResult.AcceptedButNotSynchronized ->
        result.value.map { it.id }
    }

  private fun seedLibraries() {
    database.libraryEntityQueries.insert(
      LibraryEntity(
        id = FakeApiService.BOOK_LIBRARY_ID,
        name = "Books",
        folders = Json.encodeToString(emptyList<String>()),
        isBookLibrary = 1L,
        displayOrder = 1L,
      )
    )
    database.libraryEntityQueries.insert(
      LibraryEntity(
        id = FakeApiService.PODCAST_LIBRARY_ID,
        name = "Podcasts",
        folders = Json.encodeToString(emptyList<String>()),
        isBookLibrary = 0L,
        displayOrder = 2L,
      )
    )
  }

  private fun clearDatabase() {
    database.libraryItemEntityQueries.deleteAll()
    database.libraryEntityQueries.deleteAll()
    database.bookEntityQueries.deleteAll()
    database.podcastEntityQueries.deleteAll()
    database.podcastEpisodeEntityQueries.deleteAll()
    database.progressEntityQueries.deleteAll()
  }
}
