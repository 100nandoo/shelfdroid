package dev.halim.shelfdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import dev.halim.shelfdroid.datastore.DataStoreManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataStoreManager: DataStoreManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val isDarkMode = dataStoreManager.isDarkMode.collectAsState(true)
            controller.isAppearanceLightStatusBars = isDarkMode.value.not()
            controller.isAppearanceLightNavigationBars = isDarkMode.value.not()
            App()
        }
    }
}