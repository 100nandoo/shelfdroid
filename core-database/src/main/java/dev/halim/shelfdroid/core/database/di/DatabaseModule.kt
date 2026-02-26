package dev.halim.shelfdroid.core.database.di

import android.content.Context
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.database.ListeningStatEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.UserEntity
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  const val DATABASE_NAME = "shelfdroid.db"

  @Provides
  @Singleton
  fun provideSqlDriver(@ApplicationContext appContext: Context): SqlDriver {
    return AndroidSqliteDriver(MyDatabase.Schema, appContext, DATABASE_NAME)
  }

  @Provides
  @Singleton
  fun provideSqlDelightAppDatabase(
    driver: SqlDriver,
    mapAdapter: ColumnAdapter<Map<String, Int>, String>,
  ): MyDatabase {
    val listOfStringsAdapter =
      object : ColumnAdapter<List<String>, String> {
        override fun decode(databaseValue: String) =
          if (databaseValue.isEmpty()) {
            listOf()
          } else {
            databaseValue.split(",")
          }

        override fun encode(value: List<String>) = value.joinToString(separator = ",")
      }

    val userTypeAdapter =
      object : ColumnAdapter<UserType, String> {
        override fun decode(databaseValue: String) = UserType.valueOf(databaseValue)

        override fun encode(value: UserType) = value.name
      }

    return MyDatabase(
      driver,
      ListeningStatEntity.Adapter(mapAdapter, mapAdapter),
      UserEntity.Adapter(userTypeAdapter, listOfStringsAdapter, listOfStringsAdapter),
    )
  }

  @Provides
  @Singleton
  fun provideMapAdapter(): ColumnAdapter<Map<String, Int>, String> {
    return object : ColumnAdapter<Map<String, Int>, String> {
      override fun decode(databaseValue: String): Map<String, Int> {
        return if (databaseValue.isEmpty()) {
          emptyMap()
        } else {
          Json.decodeFromString(databaseValue)
        }
      }

      override fun encode(value: Map<String, Int>): String {
        return Json.encodeToString(value)
      }
    }
  }
}
