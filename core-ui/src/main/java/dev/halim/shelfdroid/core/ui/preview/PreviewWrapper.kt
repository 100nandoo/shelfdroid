package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme


@Composable
fun PreviewWrapper(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    ShelfDroidTheme(dynamicColor = dynamicColor) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) { content() }
    }
}