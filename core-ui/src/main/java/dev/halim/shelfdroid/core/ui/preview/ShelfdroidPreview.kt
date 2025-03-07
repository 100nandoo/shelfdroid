package dev.halim.shelfdroid.core.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme

@ShelfDroidPreview
@Composable
fun ShelfDroidPreview(
    content: @Composable (paddingValues: PaddingValues) -> Unit = {}
) {
    ShelfDroidTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content(PaddingValues())
        }
    }
}
@Preview(
    name = "Dark",
    group = "Dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES,
    fontScale = 1f
)
@Preview(
    name = "Light",
    group = "Light",
    showBackground = true,
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_NO,
    fontScale = 1f
)
annotation class ShelfDroidPreview
