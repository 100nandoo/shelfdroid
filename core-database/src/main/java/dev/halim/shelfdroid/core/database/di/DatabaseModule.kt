package dev.halim.shelfdroid.core.database.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.database.LibraryEntityQueries
import dev.halim.shelfdroid.core.database.LibraryItemEntityQueries
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.ProgressEntityQueries
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
  @Provides
  @Singleton
  fun provideSqlDriver(@ApplicationContext appContext: Context): SqlDriver {
    return AndroidSqliteDriver(MyDatabase.Schema, appContext, "shelfdroid.db")
  }

  @Provides
  @Singleton
  fun provideSqlDelightAppDatabase(driver: SqlDriver): MyDatabase {
    return MyDatabase(driver)
  }

  @Provides
  @Singleton
  fun provideProgressEntityQueries(myDatabase: MyDatabase): ProgressEntityQueries {
    return myDatabase.progressEntityQueries
  }

  @Provides
  @Singleton
  fun provideLibraryItemEntityQueries(myDatabase: MyDatabase): LibraryItemEntityQueries {
    return myDatabase.libraryItemEntityQueries
  }

  @Provides
  @Singleton
  fun provideLibraryEntityQueries(myDatabase: MyDatabase): LibraryEntityQueries {
    return myDatabase.libraryEntityQueries
  }
}
