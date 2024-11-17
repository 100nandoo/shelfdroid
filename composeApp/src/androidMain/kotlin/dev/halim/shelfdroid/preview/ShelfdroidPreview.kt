
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.halim.shelfdroid.theme.ShelfDroidTheme

@ShelfDroidPreview
@Composable
fun ShelfDroidPreview(
    content: @Composable (paddingValues: PaddingValues) -> Unit = {}
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    ShelfDroidTheme(isSystemInDarkTheme) {
        Scaffold { paddingValues ->
            content(paddingValues)
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
