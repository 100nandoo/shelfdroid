package dev.halim.shelfdroid.test.app

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.core.network.response.login.LoginResponse
import dev.halim.core.network.response.login.User
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginSuccessHandler
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.prefs.ServerAccessMode
import dev.halim.shelfdroid.test.app.testdi.FakeApiService
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
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
  @Inject lateinit var authStore: DataStoreManager
  @Inject lateinit var login: LoginSuccessHandler
  private val originalBaseUrl = DataStoreManager.BASE_URL

  @Before
  fun setUp() {
    hiltRule.inject()
    clearDatabase()
    fakeApiService.reset()
  }

  @After
  fun tearDown() {
    clearDatabase()
    runBlocking { authStore.clear() }
    DataStoreManager.BASE_URL = originalBaseUrl
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

  @Test
  fun refreshLibraryItems_clearsAnEmptyLibrary() = runBlocking {
    val libraryId = FakeApiService.BOOK_LIBRARY_ID
    database.libraryEntityQueries.insert(LibraryEntity(libraryId, "Books", "[]", 1, 0, "books-2"))
    database.libraryItemEntityQueries.insert(item(FakeApiService.BOOK_ITEM_ID, libraryId))
    fakeApiService.deleteItem(FakeApiService.BOOK_ITEM_ID, hard = 1).getOrThrow()

    assertTrue(repository.refreshLibraryItems().isSuccess)
    assertEquals(
      emptyList<String>(),
      database.libraryItemEntityQueries.idsByLibraryId(libraryId).executeAsList(),
    )
  }

  @Test
  fun reLoginPreservesTheCatalogForTheSameUserAndClearsItForADifferentUser() = runBlocking {
    val server = "https://audiobooks.example"
    val first = LoginResponse(user = User(id = "first", accessToken = "first-token"))
    login.onLoginSuccess(server, ServerAccessMode.Internet, first)
    authStore.updateCatalogSyncTime(123)
    database.libraryItemEntityQueries.insert(item("private-book", FakeApiService.BOOK_LIBRARY_ID))

    authStore.beginForcedReLogin(AuthPromptReason.ManualReLogin)
    login.onLoginSuccess(server, ServerAccessMode.Internet, first)
    assertNotNull(database.libraryItemEntityQueries.byId("private-book").executeAsOneOrNull())
    assertEquals(123L, authStore.catalogSyncTime.first())

    authStore.beginForcedReLogin(AuthPromptReason.ManualReLogin)
    val second = LoginResponse(user = User(id = "second", accessToken = "second-token"))
    login.onLoginSuccess(server, ServerAccessMode.Internet, second)
    assertNull(database.libraryItemEntityQueries.byId("private-book").executeAsOneOrNull())
    assertEquals(0L, authStore.catalogSyncTime.first())
    assertEquals("second", authStore.userPrefs.first().id)
  }

  private fun item(id: String, libraryId: String): LibraryItemEntity =
    LibraryItemEntity(
      id = id,
      libraryId = libraryId,
      author = "Author",
      authorFirstLast = "Author",
      authorLastFirst = "Author",
      title = id,
      description = "",
      cover = "",
      updatedAt = 0L,
      rssFeed = null,
      isBook = if (libraryId == FakeApiService.BOOK_LIBRARY_ID) 1L else 0L,
      inoId = "",
      duration = 0.0,
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
