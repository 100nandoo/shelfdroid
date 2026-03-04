@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.userinfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import dev.halim.shelfdroid.core.ui.preview.Defaults.USER_INFO_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun UserInfoScreen(viewModel: UserInfoViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  UserInfoContent(uiState = uiState)
}

@Composable
private fun UserInfoContent(uiState: UserInfoUiState = UserInfoUiState()) {
  val startPadding = Modifier.padding(start = 16.dp)
  val endPadding = Modifier.padding(end = 16.dp)
  LazyVerticalGrid(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Fixed(2),
    verticalArrangement = Arrangement.Bottom,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      TextHeadlineSmall(
        text = stringResource(R.string.listening_stats),
        modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
      )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      TextTitleMedium(
        text = stringResource(R.string.all_time),
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
    item {
      StatCard(modifier = startPadding, stringResource(R.string.listening_days), uiState.total.days)
    }
    item {
      StatCard(
        modifier = endPadding,
        stringResource(R.string.minutes_listened),
        uiState.total.minutes,
      )
    }

    uiState.thisWeek?.let { thisWeek ->
      item(span = { GridItemSpan(maxLineSpan) }) { ThisWeekHeader() }

      item {
        StatCard(
          modifier = startPadding,
          stringResource(R.string.listening_days),
          thisWeek.days,
          thisWeek.daysDelta,
        )
      }
      item {
        StatCard(
          modifier = endPadding,
          stringResource(R.string.minutes_listened),
          thisWeek.minutes,
          thisWeek.minutesDelta,
        )
      }
      item {
        StatCard(
          modifier = startPadding,
          stringResource(R.string.most_minutes_in_a_day),
          thisWeek.mostMinutes,
        )
      }
      item {
        StatCard(
          modifier = endPadding,
          stringResource(R.string.current_streak),
          thisWeek.streak,
          thisWeek.streakDelta,
        )
      }
      item {
        StatCard(
          modifier = startPadding,
          stringResource(R.string.daily_average_minutes),
          thisWeek.dailyAverage,
          thisWeek.dailyAverageDelta,
        )
      }
    }

    if (uiState.mediaProgress.isNotEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(16.dp)) }
      item(span = { GridItemSpan(maxLineSpan) }) {
        TextHeadlineSmall(
          text = stringResource(R.string.saved_media_progress),
          modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
        )
      }
      items(items = uiState.mediaProgress, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) {
        item ->
        MediaProgressItem(item)
        HorizontalDivider()
      }
    }

    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun ThisWeekHeader() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 12.dp).height(24.dp),
  ) {
    TextTitleMedium(
      text = stringResource(R.string.this_week),
      modifier = Modifier.padding(start = 16.dp),
    )
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
private fun StatCard(modifier: Modifier, label: String, value: String, delta: Number? = null) {
  ElevatedCard(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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
fun UserInfoScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { UserInfoContent(USER_INFO_UI_STATE) }
}
