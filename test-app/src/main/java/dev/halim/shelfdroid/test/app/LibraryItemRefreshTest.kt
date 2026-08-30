package dev.halim.shelfdroid.test.app

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.test.app.testdi.FakeApiService
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class LibraryItemRefreshTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var repository: LibraryItemRepository
  @Inject lateinit var database: MyDatabase
  @Inject lateinit var fakeApiService: FakeApiService

  @Before
  fun setUp() {
    hiltRule.inject()
    clearDatabase()
    fakeApiService.reset()
  }

  @After
  fun tearDown() {
    clearDatabase()
  }

  @Test
  fun refreshLibraryItems_removesItemsForLibrariesRemovedWhileDisconnected() {
    database.libraryEntityQueries.insert(
      LibraryEntity(
        id = FakeApiService.BOOK_LIBRARY_ID,
        name = "Books",
        folders = Json.encodeToString(emptyList<String>()),
        isBookLibrary = 1L,
        displayOrder = 0L,
        icon = "books-2",
      )
    )
    database.libraryItemEntityQueries.insert(item("orphaned-item", "removed-library"))
    database.libraryItemEntityQueries.insert(
      item(FakeApiService.BOOK_ITEM_ID, FakeApiService.BOOK_LIBRARY_ID)
    )

    val result = runBlocking { repository.refreshLibraryItems() }

    assertTrue(result.isSuccess)
    assertNull(database.libraryItemEntityQueries.byId("orphaned-item").executeAsOneOrNull())
    assertNotNull(
      database.libraryItemEntityQueries.byId(FakeApiService.BOOK_ITEM_ID).executeAsOneOrNull()
    )
  }

  private fun item(id: String, libraryId: String): LibraryItemEntity =
    LibraryItemEntity(
      id = id,
      libraryId = libraryId,
      author = "Author",
      title = id,
      description = "",
      cover = "",
      updatedAt = 0L,
      rssFeed = null,
      isBook = if (libraryId == FakeApiService.BOOK_LIBRARY_ID) 1L else 0L,
      inoId = "",
      duration = "",
      addedAt = 0L,
    )

  private fun clearDatabase() {
    database.libraryItemEntityQueries.deleteAll()
    database.libraryEntityQueries.deleteAll()
    database.bookEntityQueries.deleteAll()
    database.podcastEntityQueries.deleteAll()
    database.podcastEpisodeEntityQueries.deleteAll()
    database.progressEntityQueries.deleteAll()
  }
}
