package dev.halim.shelfdroid.expect

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.halim.shelfdroid.datastore.createDataStore
import org.koin.dsl.module

actual val targetModule = module {
    single<PlatformContext> { PlatformContext.INSTANCE }
    single<DataStore<Preferences>> { createDataStore(get()) }
    single<PlatformPlayer> { PlatformPlayer.INSTANCE }
}