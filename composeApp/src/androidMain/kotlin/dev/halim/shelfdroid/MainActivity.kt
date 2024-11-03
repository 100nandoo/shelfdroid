package dev.halim.shelfdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextUtils.context = this
        enableEdgeToEdge()
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        setContent {
            val isDarkMode = SharedObject.isDarkMode.collectAsState()
            controller.isAppearanceLightStatusBars = isDarkMode.value.not()
            controller.isAppearanceLightNavigationBars = isDarkMode.value.not()
            App()
        }
    }
}
