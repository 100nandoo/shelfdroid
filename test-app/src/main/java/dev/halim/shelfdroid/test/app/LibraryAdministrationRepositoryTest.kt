package dev.halim.shelfdroid.test.app

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMutationResult
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationRepository
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.test.app.testdi.FakeApiService
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@HiltAndroidTest
class LibraryAdministrationRepositoryTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var repository: LibraryAdministrationRepository
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
    assertTrue(outcome is LibraryAdministrationMutationResult.AcceptedButNotSynchronized)
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
    assertTrue(result.getOrThrow() is LibraryAdministrationMutationResult.Accepted)
  }

  private fun order() =
    listOf(
      LibraryAdministrationLibrary(
        id = "podcasts",
        name = "Podcasts",
        mediaType = LibraryAdministrationMediaType.PODCAST,
        displayOrder = 1,
      ),
      LibraryAdministrationLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = 2,
      ),
    )

  private fun acceptedOrder(
    result: LibraryAdministrationMutationResult<List<LibraryAdministrationLibrary>>
  ): List<String> =
    when (result) {
      is LibraryAdministrationMutationResult.Accepted -> result.value.map { it.id }
      is LibraryAdministrationMutationResult.AcceptedButNotSynchronized ->
        result.value.map { it.id }
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
