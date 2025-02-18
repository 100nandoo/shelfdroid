

package dev.halim.shelfdroid.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.database.AppDatabase
import dev.halim.shelfdroid.core.database.AudiobookDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideAudiobookDao(appDatabase: AppDatabase): AudiobookDao {
        return appDatabase.audiobookDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "Audiobook"
        ).build()
    }
}
