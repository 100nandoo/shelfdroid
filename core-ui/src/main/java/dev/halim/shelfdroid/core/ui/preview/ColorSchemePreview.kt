package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme


@ShelfDroidPreview
@Composable
fun ColorSchemeScreen() {
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val colorScheme: ColorScheme = if (isSystemInDarkTheme) darkScheme else lightScheme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Light Color Scheme Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onBackground
                )
            }

            val colorPairs = listOf(
                "primary" to colorScheme.primary,
                "onPrimary" to colorScheme.onPrimary,
                "primaryContainer" to colorScheme.primaryContainer,
                "onPrimaryContainer" to colorScheme.onPrimaryContainer,
                "inversePrimary" to colorScheme.inversePrimary,
                "secondary" to colorScheme.secondary,
                "onSecondary" to colorScheme.onSecondary,
                "secondaryContainer" to colorScheme.secondaryContainer,
                "onSecondaryContainer" to colorScheme.onSecondaryContainer,
                "tertiary" to colorScheme.tertiary,
                "onTertiary" to colorScheme.onTertiary,
                "tertiaryContainer" to colorScheme.tertiaryContainer,
                "onTertiaryContainer" to colorScheme.onTertiaryContainer,
                "background" to colorScheme.background,
                "onBackground" to colorScheme.onBackground,
                "surface" to colorScheme.surface,
                "onSurface" to colorScheme.onSurface,
                "surfaceVariant" to colorScheme.surfaceVariant,
                "onSurfaceVariant" to colorScheme.onSurfaceVariant,
                "surfaceTint" to colorScheme.surfaceTint,
                "inverseSurface" to colorScheme.inverseSurface,
                "inverseOnSurface" to colorScheme.inverseOnSurface,
                "error" to colorScheme.error,
                "onError" to colorScheme.onError,
                "errorContainer" to colorScheme.errorContainer,
                "onErrorContainer" to colorScheme.onErrorContainer,
                "outline" to colorScheme.outline,
                "outlineVariant" to colorScheme.outlineVariant,
                "scrim" to colorScheme.scrim,
                "surfaceBright" to colorScheme.surfaceBright,
                "surfaceContainer" to colorScheme.surfaceContainer,
                "surfaceContainerHigh" to colorScheme.surfaceContainerHigh,
                "surfaceContainerHighest" to colorScheme.surfaceContainerHighest,
                "surfaceContainerLow" to colorScheme.surfaceContainerLow,
                "surfaceContainerLowest" to colorScheme.surfaceContainerLowest,
                "surfaceDim" to colorScheme.surfaceDim
            )

            items(colorPairs) { (name, color) ->
                ColorItem(
                    name = name,
                    color = color,
                    isDarkMode = isSystemInDarkTheme
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    name: String,
    color: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

