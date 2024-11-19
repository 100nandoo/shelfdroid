package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.IOSDatabaseDriverFactory
import org.koin.dsl.module

actual val targetModule = module {
    single<PlatformContext> { PlatformContext.INSTANCE }
    single<PlatformPlayer> { PlatformPlayer.INSTANCE }
    single<Database> { Database(IOSDatabaseDriverFactory())}
}