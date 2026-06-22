package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@ShelfDroidPreview
@Composable
fun FontPreview() {
  PreviewWrapper(dynamicColor = false) {
    FontPreviewPage(
      title = "Typography Preview",
      description = "Preview the bundled local fonts used by ShelfDroid.",
    ) {
      PreviewGroup(title = "Lora Display", description = "displayLarge to displaySmall.") {
        LoraDisplayContent()
      }
      PreviewGroup(
        title = "Lora Headline + Title",
        description = "headlineLarge to titleSmall.",
      ) {
        LoraHeadlineAndTitleContent()
      }
      PreviewGroup(
        title = "Body + Label",
        description = "Inter body roles and JetBrains Mono label roles.",
      ) {
        BodyAndLabelContent()
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun LoraDisplayPreview() {
  PreviewWrapper(dynamicColor = false) {
    FontPreviewPage(title = "Lora Display", description = "displayLarge to displaySmall.") {
      LoraDisplayContent()
    }
  }
}

@ShelfDroidPreview
@Composable
fun LoraHeadlineAndTitlePreview() {
  PreviewWrapper(dynamicColor = false) {
    FontPreviewPage(
      title = "Lora Headline + Title",
      description = "headlineLarge to titleSmall.",
    ) {
      LoraHeadlineAndTitleContent()
    }
  }
}

@ShelfDroidPreview
@Composable
fun BodyAndLabelPreview() {
  PreviewWrapper(dynamicColor = false) {
    FontPreviewPage(
      title = "Body + Label",
      description = "Inter body roles and JetBrains Mono label roles.",
    ) {
      BodyAndLabelContent()
    }
  }
}

@Composable
private fun FontPreviewPage(
  title: String,
  description: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    PreviewHeader(title = title, description = description)
    content()
  }
}

@Composable
private fun PreviewHeader(title: String, description: String) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun PreviewGroup(
  title: String,
  description: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    content()
  }
}

@Composable
private fun LoraDisplayContent() {
  LoraDisplaySection()
}

@Composable
private fun LoraHeadlineAndTitleContent() {
  LoraHeadlineSection()
  LoraTitleSection()
}

@Composable
private fun BodyAndLabelContent() {
  InterBodySection()
  JetBrainsMonoLabelSection()
}

@Composable
private fun LoraDisplaySection() {
  TypographyRoleSection(
    title = "Display",
    sample = "The Fellowship of the Ring",
    styles =
      listOf(
        "displayLarge" to MaterialTheme.typography.displayLarge,
        "displayMedium" to MaterialTheme.typography.displayMedium,
        "displaySmall" to MaterialTheme.typography.displaySmall,
      ),
  )
}

@Composable
private fun LoraHeadlineSection() {
  TypographyRoleSection(
    title = "Headline",
    sample = "The Fellowship of the Ring",
    styles =
      listOf(
        "headlineLarge" to MaterialTheme.typography.headlineLarge,
        "headlineMedium" to MaterialTheme.typography.headlineMedium,
        "headlineSmall" to MaterialTheme.typography.headlineSmall,
      ),
  )
}

@Composable
private fun LoraTitleSection() {
  TypographyRoleSection(
    title = "Title",
    sample = "The Fellowship of the Ring",
    styles =
      listOf(
        "titleLarge" to MaterialTheme.typography.titleLarge,
        "titleMedium" to MaterialTheme.typography.titleMedium,
        "titleSmall" to MaterialTheme.typography.titleSmall,
      ),
  )
}

@Composable
private fun InterBodySection() {
  TypographyRoleSection(
    title = "Body",
    sample =
      "In a hole in the ground there lived a hobbit. Not a nasty, dirty, wet hole, but a hobbit-hole.",
    styles =
      listOf(
        "bodyLarge" to MaterialTheme.typography.bodyLarge,
        "bodyMedium" to MaterialTheme.typography.bodyMedium,
        "bodySmall" to MaterialTheme.typography.bodySmall,
      ),
  )
}

@Composable
private fun JetBrainsMonoLabelSection() {
  TypographyRoleSection(
    title = "Label",
    sample = "DOWNLOAD COMPLETE 12/12",
    styles =
      listOf(
        "labelLarge" to MaterialTheme.typography.labelLarge,
        "labelMedium" to MaterialTheme.typography.labelMedium,
        "labelSmall" to MaterialTheme.typography.labelSmall,
      ),
  )
}

@Composable
private fun TypographyRoleSection(
  title: String,
  sample: String,
  styles: List<Pair<String, TextStyle>>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )

    styles.forEach { (name, style) ->
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = name,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
        Text(text = sample, style = style, color = MaterialTheme.colorScheme.onSurface)
      }
    }
  }
}
