package dev.halim.shelfdroid.core.ui.screen.login

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
    val loginUiState = LoginUiState(server = "audiobookshelf.org", username = "admin", password = "123456")
    PreviewWrapper(dynamicColor = false) {
        LoginScreenContent(loginUiState)
    }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
    val loginUiState  = LoginUiState(loginState = LoginState.Failure("Wrong credentials"))
    PreviewWrapper(dynamicColor = true) {
        LoginScreenContent(loginUiState)
    }
}