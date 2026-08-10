package dev.halim.shelfdroid.core.data.catalog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.database.MyDatabase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookLocalDataSourceTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun storesRetrievesAndDeletesBookMediaByLibraryItemId() {
    AndroidSqliteDriver(MyDatabase.Schema, context).use { driver ->
      val database =
        DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter())
      val repository = BookLocalDataSource(database, Json)
      val book = Book(audioFiles = listOf(AudioFile(ino = "ino-1")))

      repository.insert("book-1", book)

      val stored = repository.byId("book-1")
      assertEquals("ino-1", stored?.audioFiles?.single()?.ino)

      repository.deleteById("book-1")

      assertNull(repository.byId("book-1"))
    }
  }
}
