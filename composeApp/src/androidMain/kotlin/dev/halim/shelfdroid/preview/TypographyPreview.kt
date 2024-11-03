package dev.halim.shelfdroid.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.theme.ShelfDroidTheme

@Preview(showBackground = true, heightDp = 1280)
@Composable
fun TypographyPreview() {
    ShelfDroidTheme(false) {
        TypographyScreen()
    }
}

@Preview(showBackground = true, heightDp = 1280)
@Composable
fun DarkTypographyPreview() {
    ShelfDroidTheme(true) {
        TypographyScreen(isDarkMode = true)
    }
}

@Composable
fun TypographyScreen(
    isDarkMode: Boolean = false
) {
    ShelfDroidTheme(isDarkMode) {
        Scaffold { paddingValues ->
            val typography = MaterialTheme.typography
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Typography Preview",
                        style = typography.headlineMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                val typographyPairs = listOf(
                    "displayLarge" to typography.displayLarge,
                    "displayMedium" to typography.displayMedium,
                    "displaySmall" to typography.displaySmall,
                    "headlineLarge" to typography.headlineLarge,
                    "headlineMedium" to typography.headlineMedium,
                    "headlineSmall" to typography.headlineSmall,
                    "titleLarge" to typography.titleLarge,
                    "titleMedium" to typography.titleMedium,
                    "titleSmall" to typography.titleSmall,
                    "bodyLarge" to typography.bodyLarge,
                    "bodyMedium" to typography.bodyMedium,
                    "bodySmall" to typography.bodySmall,
                    "labelLarge" to typography.labelLarge,
                    "labelMedium" to typography.labelMedium,
                    "labelSmall" to typography.labelSmall
                )

                items(typographyPairs) { (name, style) ->
                    TypographyItem(
                        name = name,
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypographyItem(
    name: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            style = style,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}