package dev.halim.shelfdroid.core.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryItemCatalogMigrationTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val databaseName = "library-item-catalog-migration-test.db"

  @After
  fun deleteDatabase() {
    context.deleteDatabase(databaseName)
  }

  @Test
  fun catalog_readsCurrentLibraryItemFields() {
    AndroidSqliteDriver(MyDatabase.Schema, context).use { driver ->
      val queries = LibraryItemEntityQueries(driver)
      queries.insert(
        LibraryItemEntity(
          id = "podcast-1",
          libraryId = "library-1",
          author = "Author",
          title = "Podcast",
          description = "",
          cover = "cover",
          updatedAt = 0,
          rssFeed = null,
          isBook = 0,
          inoId = "",
          duration = "",
          addedAt = 1,
        )
      )

      assertEquals(
        LibraryItemCatalog(
          id = "podcast-1",
          libraryId = "library-1",
          author = "Author",
          title = "Podcast",
          cover = "cover",
          isBook = 0,
          addedAt = 1,
          episodeCount = 0,
        ),
        queries.libraryItemCatalog().executeAsOne(),
      )
    }
  }

  @Test
  fun openingLegacyDatabaseRemovesPodcastPayloadAndRetainsCompactBookCatalog() {
    context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
      database.execSQL(
        """
        CREATE TABLE LibraryItemEntity (
          id TEXT PRIMARY KEY NOT NULL,
          libraryId TEXT NOT NULL,
          author TEXT NOT NULL DEFAULT '',
          title TEXT NOT NULL DEFAULT '',
          description TEXT NOT NULL DEFAULT '',
          cover TEXT NOT NULL DEFAULT '',
          updatedAt INTEGER NOT NULL DEFAULT 0,
          media TEXT NOT NULL DEFAULT '',
          rssFeed TEXT,
          isBook INTEGER NOT NULL DEFAULT 1,
          inoId TEXT NOT NULL DEFAULT '',
          duration TEXT NOT NULL DEFAULT '',
          addedAt INTEGER NOT NULL
        )
        """
          .trimIndent()
      )
      database.execSQL(
        """
        INSERT INTO LibraryItemEntity(
          id, libraryId, author, title, description, cover, updatedAt, media, rssFeed,
          isBook, inoId, duration, addedAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>(
          "podcast-1",
          "library-1",
          "Author",
          "Podcast",
          "",
          "cover",
          0,
          "x".repeat(3_000_000),
          null,
          0,
          "",
          "",
          1,
        ),
      )
      database.execSQL(
        """
        INSERT INTO LibraryItemEntity(
          id, libraryId, author, title, description, cover, updatedAt, media, rssFeed,
          isBook, inoId, duration, addedAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>(
          "book-1",
          "library-1",
          "Author",
          "Book",
          "",
          "cover",
          0,
          "book-media",
          null,
          1,
          "",
          "",
          1,
        ),
      )
      database.version = 2
    }

    AndroidSqliteDriver(MyDatabase.Schema, context, databaseName).use { driver ->
      LibraryItemEntityQueries(driver).libraryItemCatalog().executeAsList()
    }

    context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
      assertEquals(
        listOf("book-1"),
        database.rawQuery("SELECT id FROM LibraryItemEntity", null).use { cursor ->
          buildList {
            while (cursor.moveToNext()) {
              add(cursor.getString(0))
            }
          }
        },
      )
    }
  }

  @Test
  fun openingLegacyDatabaseMovesBookMediaIntoBookEntity() {
    context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
      database.execSQL(
        """
        CREATE TABLE LibraryItemEntity (
          id TEXT PRIMARY KEY NOT NULL,
          libraryId TEXT NOT NULL,
          author TEXT NOT NULL DEFAULT '',
          title TEXT NOT NULL DEFAULT '',
          description TEXT NOT NULL DEFAULT '',
          cover TEXT NOT NULL DEFAULT '',
          updatedAt INTEGER NOT NULL DEFAULT 0,
          media TEXT NOT NULL DEFAULT '',
          rssFeed TEXT,
          isBook INTEGER NOT NULL DEFAULT 1,
          inoId TEXT NOT NULL DEFAULT '',
          duration TEXT NOT NULL DEFAULT '',
          addedAt INTEGER NOT NULL
        )
        """
          .trimIndent()
      )
      database.execSQL(
        """
        INSERT INTO LibraryItemEntity(
          id, libraryId, author, title, description, cover, updatedAt, media, rssFeed,
          isBook, inoId, duration, addedAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>(
          "book-1",
          "library-1",
          "Author",
          "Book",
          "",
          "cover",
          0,
          "book-media",
          null,
          1,
          "",
          "",
          1,
        ),
      )
      database.version = 2
    }

    AndroidSqliteDriver(MyDatabase.Schema, context, databaseName).use { driver ->
      assertEquals(
        BookEntity(libraryItemId = "book-1", media = "book-media"),
        BookEntityQueries(driver).byLibraryItemId("book-1").executeAsOne(),
      )
    }

    context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
      assertEquals(
        "book-media",
        database
          .rawQuery("SELECT media FROM BookEntity WHERE libraryItemId = 'book-1'", null)
          .use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
          },
      )
      assertEquals(
        listOf(
          "id",
          "libraryId",
          "author",
          "title",
          "description",
          "cover",
          "updatedAt",
          "rssFeed",
          "isBook",
          "inoId",
          "duration",
          "addedAt",
        ),
        database.rawQuery("PRAGMA table_info(LibraryItemEntity)", null).use { cursor ->
          buildList {
            while (cursor.moveToNext()) {
              add(cursor.getString(1))
            }
          }
        },
      )
    }
  }

  @Test
  fun openingLegacyDatabaseCreatesFinalPodcastTables() {
    context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
      database.execSQL(
        """
        CREATE TABLE LibraryItemEntity (
          id TEXT PRIMARY KEY NOT NULL,
          libraryId TEXT NOT NULL,
          author TEXT NOT NULL DEFAULT '',
          title TEXT NOT NULL DEFAULT '',
          description TEXT NOT NULL DEFAULT '',
          cover TEXT NOT NULL DEFAULT '',
          updatedAt INTEGER NOT NULL DEFAULT 0,
          media TEXT NOT NULL DEFAULT '',
          rssFeed TEXT,
          isBook INTEGER NOT NULL DEFAULT 1,
          inoId TEXT NOT NULL DEFAULT '',
          duration TEXT NOT NULL DEFAULT '',
          addedAt INTEGER NOT NULL
        )
        """
          .trimIndent()
      )
      database.execSQL(
        """
        INSERT INTO LibraryItemEntity(
          id, libraryId, author, title, description, cover, updatedAt, media, rssFeed,
          isBook, inoId, duration, addedAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
          .trimIndent(),
        arrayOf<Any?>(
          "podcast-1",
          "library-1",
          "Author",
          "Podcast",
          "Description",
          "cover",
          0,
          "x".repeat(3_000_000),
          null,
          0,
          "",
          "",
          1,
        ),
      )
      database.version = 2
    }

    AndroidSqliteDriver(MyDatabase.Schema, context, databaseName).use { driver ->
      assertEquals(
        emptyList<LibraryItemCatalog>(),
        LibraryItemEntityQueries(driver).libraryItemCatalog().executeAsList(),
      )
      assertEquals(
        null,
        PodcastEntityQueries(driver).byLibraryItemId("podcast-1").executeAsOneOrNull(),
      )
      assertEquals(
        emptyList<PodcastEpisodeEntity>(),
        PodcastEpisodeEntityQueries(driver).byLibraryItemId("podcast-1").executeAsList(),
      )
    }
  }
}
