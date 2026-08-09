package dev.halim.shelfdroid.core.data.response

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.EbookFile
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.database.MyDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookMediaRepoTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun storesRetrievesAndDeletesBookMediaByLibraryItemId() {
    AndroidSqliteDriver(MyDatabase.Schema, context).use { driver ->
      val database =
        DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter())
      val repository = BookMediaRepo(database)
      val book =
        Book(
          coverPath = "cover",
          metadata =
            BookMetadata(
              title = "Book",
              subtitle = "Subtitle",
              publisher = "Publisher",
            ),
          ebookFile = EbookFile(ino = "ebook-1", metadata = FileMetadata(filename = "book.epub")),
          duration = 60.0,
        )

      repository.insert("book-1", book)

      val stored = repository.byId("book-1")
      assertEquals("Book", stored?.metadata?.title)
      assertEquals("Subtitle", stored?.metadata?.subtitle)
      assertEquals("Publisher", stored?.metadata?.publisher)
      assertEquals("book.epub", stored?.ebookFile?.metadata?.filename)
      assertEquals(60.0, stored?.duration)

      repository.deleteById("book-1")

      assertNull(repository.byId("book-1"))
    }
  }
}
