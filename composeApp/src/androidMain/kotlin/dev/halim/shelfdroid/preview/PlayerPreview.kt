package dev.halim.shelfdroid.preview

import ShelfDroidPreview
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.ui.screens.player.PlayerScreenContent

@ShelfDroidPreview
@Composable
fun PlayerContentPreview() {
    ShelfDroidPreview { paddingValues ->
        PlayerScreenContent(paddingValues = paddingValues)
    }
}

