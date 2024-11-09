package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.di.appModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

expect val targetModule: Module

fun initializeKoin(config: (KoinApplication.() -> Unit)? = null) {
    startKoin {
        config?.invoke(this)
        modules(targetModule)
        modules(appModule)
    }
}