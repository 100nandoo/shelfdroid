@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components.player.small

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

enum class SmallPlayerState {
  Hidden,
  TempHidden,
  Expanded,
  PartiallyExpanded,
}

@Composable
fun PlayerHandler(
  navController: NavHostController,
  content: @Composable (PaddingValues, onShowSmallPlayer: (String) -> Unit) -> Unit,
) {
  val bottomSheetState =
    rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
  val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
  val coroutineScope = rememberCoroutineScope()

  var currentId by remember { mutableStateOf("") }
  val smallPlayerState = remember { mutableStateOf(SmallPlayerState.Hidden) }

  LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
      val route = destination.route
      val isVisibleRoute =
        route?.contains("Home") == true ||
          route?.contains("Book") == true ||
          route?.contains("Podcast") == true

      when {
        !isVisibleRoute &&
          smallPlayerState.value in
            setOf(SmallPlayerState.Expanded, SmallPlayerState.PartiallyExpanded) ->
          smallPlayerState.value = SmallPlayerState.TempHidden

        isVisibleRoute && smallPlayerState.value == SmallPlayerState.TempHidden ->
          smallPlayerState.value = SmallPlayerState.PartiallyExpanded
      }
    }
  }

  LaunchedEffect(bottomSheetState) {
    snapshotFlow { bottomSheetState.currentValue }
      .distinctUntilChanged()
      .collect { sheetValue ->
        smallPlayerState.value =
          when (sheetValue) {
            SheetValue.Hidden ->
              if (smallPlayerState.value == SmallPlayerState.TempHidden) SmallPlayerState.TempHidden
              else SmallPlayerState.Hidden

            SheetValue.PartiallyExpanded -> SmallPlayerState.PartiallyExpanded
            SheetValue.Expanded -> SmallPlayerState.Expanded
          }
      }
  }

  LaunchedEffect(Unit) {
    snapshotFlow { smallPlayerState.value }
      .distinctUntilChanged()
      .collect { state ->
        coroutineScope.launch {
          when (state) {
            SmallPlayerState.Hidden,
            SmallPlayerState.TempHidden -> scaffoldState.bottomSheetState.hide()
            SmallPlayerState.PartiallyExpanded -> scaffoldState.bottomSheetState.partialExpand()
            SmallPlayerState.Expanded -> scaffoldState.bottomSheetState.expand()
          }
        }
      }
  }

  val onShowSmallPlayer: (String) -> Unit = { id ->
    currentId = id
    smallPlayerState.value = SmallPlayerState.PartiallyExpanded
  }

  SmallPlayer(scaffoldState, currentId, { smallPlayerState.value = SmallPlayerState.Expanded }) {
    padding ->
    content(padding, onShowSmallPlayer)
  }
}
