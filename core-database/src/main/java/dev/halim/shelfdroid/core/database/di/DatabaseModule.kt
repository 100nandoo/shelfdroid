package dev.halim.shelfdroid.core.database.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Singleton

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
  fun provideSqlDelightAppDatabase(driver: SqlDriver): MyDatabase {
    return MyDatabase(driver)
  }
}
