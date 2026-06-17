package dev.halim.shelfdroid.test.app.testdi

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.core.datastore.di.DatastoreModule
import java.io.File
import java.util.UUID
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatastoreModule::class])
object FakeDatastoreModule {

  @Provides
  @Singleton
  fun provideDatastore(@ApplicationContext appContext: Context): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
      produceFile = {
        File(appContext.filesDir, "datastore/settings-test-${UUID.randomUUID()}.preferences_pb")
          .apply { parentFile?.mkdirs() }
      }
    )
  }
}
