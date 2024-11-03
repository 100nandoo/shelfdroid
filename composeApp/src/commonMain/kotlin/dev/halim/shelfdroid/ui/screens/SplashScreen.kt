package dev.halim.shelfdroid.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding()
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Loading...", style = MaterialTheme.typography.headlineLarge)
    }
}