package dev.halim.shelfdroid.core.ui.screen.userinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoUiState
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.Defaults.USER_INFO_MEDIA_PROGRESS
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MediaProgressItem(mediaProgress: UserInfoUiState.MediaProgress) {
  val background =
    if (mediaProgress.isFinished) Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    else Modifier
  Row(
    modifier =
      Modifier.fillMaxWidth().then(background).padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CoverNoAnimation(
      Modifier.height(64.dp).padding(end = 16.dp),
      coverUrl = mediaProgress.cover,
      shape = RoundedCornerShape(4.dp),
    )
    Column(Modifier.fillMaxWidth().weight(1f)) {
      Text(
        text = mediaProgress.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface,
      )
      TextLabelSmall(
        text = mediaProgress.startAt,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      TextLabelSmall(
        text = mediaProgress.lastUpdate,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    TextTitleMedium(text = mediaProgress.progress)
  }
}

@ShelfDroidPreview
@Composable
private fun MediaProgressItemPreview() {
  PreviewWrapper(dynamicColor = false) {
    LazyColumn() { item { MediaProgressItem(mediaProgress = USER_INFO_MEDIA_PROGRESS) } }
  }
}
