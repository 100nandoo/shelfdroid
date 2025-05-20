package dev.halim.shelfdroid.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Audiobook::class, ProgressEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
  abstract fun audiobookDao(): AudiobookDao

  abstract fun progressDao(): ProgressDao
}
