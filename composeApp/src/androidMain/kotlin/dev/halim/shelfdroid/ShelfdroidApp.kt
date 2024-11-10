package dev.halim.shelfdroid

import android.app.Application
import dev.halim.shelfdroid.expect.initializeKoin
import org.koin.android.ext.koin.androidContext

class ShelfdroidApp: Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin {
            androidContext(applicationContext)
        }
    }
}