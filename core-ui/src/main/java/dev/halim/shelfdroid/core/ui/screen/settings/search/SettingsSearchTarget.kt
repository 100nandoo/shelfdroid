package dev.halim.shelfdroid.core.ui.screen.settings.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import kotlinx.coroutines.delay

@Composable
fun SettingsSearchTarget(
  settingId: String,
  label: String,
  targetSettingId: String?,
  scrollConsumed: MutableState<Boolean>? = null,
  content: @Composable () -> Unit,
) {
  val isTarget = settingId == targetSettingId
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  var highlighted by remember(settingId) { mutableStateOf(false) }
  val accessibilityLabel = stringResource(R.string.settings_search_target_found, label)
  val announceTarget = isTarget && (scrollConsumed?.value != true || highlighted)

  LaunchedEffect(isTarget, bringIntoViewRequester, scrollConsumed) {
    if (!isTarget || scrollConsumed?.value == true) {
      highlighted = false
      return@LaunchedEffect
    }
    withFrameNanos {}
    if (scrollConsumed?.value == true) return@LaunchedEffect
    if (scrollConsumed != null) scrollConsumed.value = true
    highlighted = true
    bringIntoViewRequester.bringIntoView()
    delay(1_600)
    highlighted = false
  }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .bringIntoViewRequester(bringIntoViewRequester)
        .padding(horizontal = 8.dp, vertical = 2.dp)
        .background(
          color =
            if (highlighted) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(12.dp),
        )
        .semantics {
          if (announceTarget) {
            liveRegion = LiveRegionMode.Polite
            stateDescription = accessibilityLabel
          }
        }
  ) {
    content()
  }
}
