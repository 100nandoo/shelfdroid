@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components.miniplayer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

enum class MiniPlayerState {
  Hidden,
  TempHidden,
  Expanded,
  PartiallyExpanded,
}

@Composable
fun MiniPlayerHandler(
  navController: NavHostController,
  content: @Composable (PaddingValues, onShowMiniPlayer: (String) -> Unit) -> Unit,
) {
  val bottomSheetState =
    rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
  val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
  val coroutineScope = rememberCoroutineScope()

  var currentId by remember { mutableStateOf("") }
  val miniPlayerState = remember { mutableStateOf(MiniPlayerState.Hidden) }

  LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
      val route = destination.route
      val isVisibleRoute =
        route?.contains("Home") == true ||
          route?.contains("Book") == true ||
          route?.contains("Podcast") == true

      when {
        !isVisibleRoute &&
          miniPlayerState.value in
            setOf(MiniPlayerState.Expanded, MiniPlayerState.PartiallyExpanded) ->
          miniPlayerState.value = MiniPlayerState.TempHidden

        isVisibleRoute && miniPlayerState.value == MiniPlayerState.TempHidden ->
          miniPlayerState.value = MiniPlayerState.PartiallyExpanded
      }
    }
  }

  LaunchedEffect(bottomSheetState) {
    snapshotFlow { bottomSheetState.currentValue }
      .distinctUntilChanged()
      .collect { sheetValue ->
        miniPlayerState.value =
          when (sheetValue) {
            SheetValue.Hidden ->
              if (miniPlayerState.value == MiniPlayerState.TempHidden) MiniPlayerState.TempHidden
              else MiniPlayerState.Hidden

            SheetValue.PartiallyExpanded -> MiniPlayerState.PartiallyExpanded
            SheetValue.Expanded -> MiniPlayerState.Expanded
          }
      }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { miniPlayerState.value }
      .distinctUntilChanged()
      .collect { state ->
        coroutineScope.launch {
          when (state) {
            MiniPlayerState.Hidden,
            MiniPlayerState.TempHidden -> scaffoldState.bottomSheetState.hide()
            MiniPlayerState.PartiallyExpanded -> scaffoldState.bottomSheetState.partialExpand()
            MiniPlayerState.Expanded -> scaffoldState.bottomSheetState.expand()
          }
        }
      }
  }

  val onShowMiniPlayer: (String) -> Unit = { id ->
    currentId = id
    miniPlayerState.value = MiniPlayerState.PartiallyExpanded
  }

  MiniPlayer(scaffoldState, currentId, {}) { padding -> content(padding, onShowMiniPlayer) }
}
