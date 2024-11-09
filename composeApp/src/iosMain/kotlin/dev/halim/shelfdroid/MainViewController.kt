package dev.halim.shelfdroid

import androidx.compose.ui.window.ComposeUIViewController
import dev.halim.shelfdroid.expect.initializeKoin

fun MainViewController() = ComposeUIViewController(configure = { initializeKoin() }) { App() }