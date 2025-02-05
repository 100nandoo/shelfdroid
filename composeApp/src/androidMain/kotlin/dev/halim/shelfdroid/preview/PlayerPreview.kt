package dev.halim.shelfdroid.preview

import ShelfDroidPreview
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.di.allModules
import dev.halim.shelfdroid.ui.screens.player.BookPlayerUiState
import dev.halim.shelfdroid.ui.screens.player.PlayerScreenContent
import org.koin.compose.KoinApplication

@ShelfDroidPreview
@Composable
fun PlayerContentPreview() {
    KoinApplication(application = {
        modules(allModules)
    }) {
        ShelfDroidPreview { paddingValues ->
            PlayerScreenContent(paddingValues = paddingValues, BookPlayerUiState("1", "Sun Ra", "The Art of Peace"))
        }
    }
}

