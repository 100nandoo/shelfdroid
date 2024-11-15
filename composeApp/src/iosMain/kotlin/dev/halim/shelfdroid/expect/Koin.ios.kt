package dev.halim.shelfdroid.expect

import org.koin.dsl.module

actual val targetModule = module {
    single<PlatformContext> { PlatformContext.INSTANCE }
    single<PlatformPlayer> { PlatformPlayer.INSTANCE }
}