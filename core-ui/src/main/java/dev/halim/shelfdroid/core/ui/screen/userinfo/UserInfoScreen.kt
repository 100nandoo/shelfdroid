@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.userinfo

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.userinfo.UserInfoUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextHeadlineMedium
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.extensions.appendBold
import dev.halim.shelfdroid.core.ui.extensions.appendTwoLine
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_STAT_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun UserInfoScreen(viewModel: UserInfoViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ListeningStatContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun ListeningStatContent(
  uiState: UserInfoUiState = UserInfoUiState(),
  onEvent: (ListeningStatEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    ListeningStatSection(uiState = uiState)
    SavedMediaProgressSection()
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun SavedMediaProgressSection() {
  TextHeadlineSmall(
    text = stringResource(R.string.saved_media_progress),
    modifier = Modifier.padding(bottom = 8.dp),
  )
  LazyColumn() {}
}

@Composable
private fun ListeningStatSection(uiState: UserInfoUiState = UserInfoUiState()) {
  TextHeadlineSmall(
    text = stringResource(R.string.listening_stats),
    modifier = Modifier.padding(bottom = 8.dp),
  )
  TotalSection(uiState.total)
  uiState.thisWeek?.let { ThisWeekSection(it) }
  HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun TotalSection(total: UserInfoUiState.Total) {
  TextTitleMedium(text = stringResource(R.string.all_time))
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
private fun ThisWeekSection(thisWeek: UserInfoUiState.ThisWeek) {
  ThisWeekHeader()
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
private fun ThisWeekHeader() {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp)) {
    TextTitleMedium(text = stringResource(R.string.this_week))
    val state = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    Spacer(modifier = Modifier.width(8.dp))

    TooltipBox(
      positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
      state = state,
      tooltip = {
        RichTooltip {
          Text(
            buildAnnotatedString {
              appendBold(stringResource(R.string.server_label))
              append(stringResource(R.string.server_this_week_desc))
              appendTwoLine()

              appendBold(stringResource(R.string.app_label))
              append(stringResource(R.string.app_this_week_desc))
              appendTwoLine()

              append(stringResource(R.string.difference_desc))
            }
          )
        }
      },
    ) {
      Icon(
        modifier = Modifier.clickable { scope.launch { state.show() } },
        painter = painterResource(id = R.drawable.info),
        contentDescription = stringResource(R.string.info),
      )
    }
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
