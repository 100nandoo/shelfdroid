package dev.halim.shelfdroid.core.ui.screen.listeningstat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.listeningstat.ListeningStatUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextHeadlineMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_STAT_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlin.math.roundToInt

@Composable
fun ListeningStatScreen(viewModel: ListeningStatViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ListeningStatContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun ListeningStatContent(
  uiState: ListeningStatUiState = ListeningStatUiState(),
  onEvent: (ListeningStatEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    TotalSection(uiState.total)
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    ThisWeekSection(uiState.thisWeek)

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun ThisWeekSection(thisWeek: ListeningStatUiState.ThisWeek) {
  TextHeadlineMedium(text = stringResource(R.string.this_week))
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(vertical = 12.dp),
  ) {
    item { StatCard(stringResource(R.string.listening_days), thisWeek.days, thisWeek.daysDelta) }
    item {
      StatCard(stringResource(R.string.minutes_listened), thisWeek.minutes, thisWeek.minutesDelta)
    }
    item { StatCard(stringResource(R.string.most_minutes_in_a_day), thisWeek.mostMinutes) }
    item {
      StatCard(stringResource(R.string.current_streak), thisWeek.streak, thisWeek.streakDelta)
    }
    item {
      StatCard(
        stringResource(R.string.daily_average_minutes),
        thisWeek.dailyAverage,
        thisWeek.dailyAverageDelta,
      )
    }
  }
}

@Composable
private fun TotalSection(total: ListeningStatUiState.Total) {
  TextHeadlineMedium(text = stringResource(R.string.all_time))
  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(vertical = 12.dp),
  ) {
    item { StatCard(stringResource(R.string.listening_days), total.days) }
    item { StatCard(stringResource(R.string.minutes_listened), total.minutes) }
  }
}

@Composable
private fun StatCard(label: String, value: String, delta: Number? = null) {
  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.padding(start = 16.dp, top = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextHeadlineMedium(text = value)
      Spacer(modifier = Modifier.width(4.dp))
      if (delta != null && delta.toFloat() != 0f) {
        TextDeltaStat(delta = delta)
      }
    }
    TextLabelSmall(
      modifier = Modifier.padding(start = 16.dp, bottom = 16.dp),
      text = label,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun TextDeltaStat(delta: Number) {
  val isPositive = delta.toFloat() > 0
  val sign = if (isPositive) "+" else ""
  val color =
    if (isPositive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.error
  val postfix = if (delta is Float) "%" else ""

  val deltaFloat = delta.toFloat()
  val deltaString = deltaFloat.roundToInt().toString()

  Row(verticalAlignment = Alignment.CenterVertically) {
    TextLabelSmall(text = sign + deltaString + postfix, color = color)
  }
}

@ShelfDroidPreview
@Composable
fun ListeningStatScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { ListeningStatContent(LISTENING_STAT_UI_STATE) }
}
