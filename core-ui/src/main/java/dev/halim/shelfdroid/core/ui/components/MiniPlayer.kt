package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.extensions.dropShadow
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.player.BasicPlayerControl

@Composable
fun MiniPlayer() {
    val insets = WindowInsets.systemGestures.asPaddingValues()
    val gestureNavBottomInset = insets.calculateBottomPadding()

    BasicPlayerControl(
        Modifier
            .dropShadow(RectangleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(bottom = gestureNavBottomInset)
    )
}

@ShelfDroidPreview
@Composable
fun MiniPlayerPreview() {
    PreviewWrapper(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.Bottom){
            Box(Modifier.weight(1f))
            Box(Modifier.padding(16.dp)){
                MiniPlayer()
            }
        }
    }
}

