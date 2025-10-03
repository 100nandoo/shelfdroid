@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.player.bigplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.extensions.toSleepTimerText
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

@Composable
fun SleepTimer(sleepTimeLeft: Duration, onEvent: (PlayerEvent) -> Unit) {
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(stringResource(R.string.sleep_timer))

    Spacer(modifier = Modifier.height(4.dp))

    Box(
      modifier =
        Modifier.clip(CircleShape).clickable { scope.launch { sheetState.show() } }.size(48.dp)
    ) {
      if (sleepTimeLeft.inWholeSeconds > 0) {
        Text(
          sleepTimeLeft.toSleepTimerText(),
          modifier = Modifier.align(Alignment.Center),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.Bold,
        )
      } else {
        Icon(
          modifier = Modifier.size(36.dp).align(Alignment.Center),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          imageVector = Icons.Default.Timer,
          contentDescription = stringResource(R.string.timer),
        )
      }
    }
    SleepTimerBottomSheet(sheetState, onEvent)
  }
}

@Composable
fun SleepTimerBottomSheet(sheetState: SheetState, onEvent: (PlayerEvent) -> Unit) {
  val scope = rememberCoroutineScope()

  val sleepTimerOptions =
    listOf(
      stringResource(R.string.timer_minute, 60) to 60,
      stringResource(R.string.timer_minute, 45) to 45,
      stringResource(R.string.timer_minute, 30) to 30,
      stringResource(R.string.timer_minute, 15) to 15,
      stringResource(R.string.timer_minute, 10) to 10,
      stringResource(R.string.timer_minute, 5) to 5,
      stringResource(R.string.timer_minute, 1) to 1,
      stringResource(R.string.clear) to 0,
    )
  if (sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      Row(Modifier.padding(bottom = 32.dp, start = 64.dp, end = 64.dp)) {
        val firstColumnOptions = sleepTimerOptions.take((sleepTimerOptions.size + 1) / 2)
        val secondColumnOptions = sleepTimerOptions.drop((sleepTimerOptions.size + 1) / 2)
        Column(modifier = Modifier.weight(1f)) {
          firstColumnOptions.forEach { (label, duration) ->
            TextButton(
              shape = RectangleShape,
              modifier = Modifier.fillMaxWidth(),
              onClick = {
                onEvent(PlayerEvent.SleepTimer(duration.minutes))
                scope.launch { sheetState.hide() }
              },
            ) {
              Text(label)
            }
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          secondColumnOptions.forEach { (label, duration) ->
            TextButton(
              shape = RectangleShape,
              modifier = Modifier.fillMaxWidth(),
              onClick = {
                onEvent(PlayerEvent.SleepTimer(duration.minutes))
                scope.launch { sheetState.hide() }
              },
            ) {
              Text(label)
            }
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewSleepTimerBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val sleepTimerSheetState = sheetState(density)

    SleepTimerBottomSheet(sleepTimerSheetState, onEvent = {})
    LaunchedEffect(Unit) { sleepTimerSheetState.show() }
  }
}
