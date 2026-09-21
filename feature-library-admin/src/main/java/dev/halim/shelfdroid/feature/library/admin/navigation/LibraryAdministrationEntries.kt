package dev.halim.shelfdroid.feature.library.admin.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.result.LocalResultEventBus
import dev.halim.shelfdroid.core.navigation.LibraryChangedNavResult
import dev.halim.shelfdroid.core.ui.navigation.CreateLibrary
import dev.halim.shelfdroid.core.ui.navigation.EditLibrary
import dev.halim.shelfdroid.core.ui.navigation.Libraries
import dev.halim.shelfdroid.core.ui.navigation.Nav3ScreenWrapper
import dev.halim.shelfdroid.core.ui.navigation.ShelfNavKey
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.LibraryAdminScreen
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateScreen

fun EntryProviderScope<ShelfNavKey>.libraryAdministrationEntries(
  sharedTransitionScope: SharedTransitionScope,
  navigate: (ShelfNavKey) -> Unit,
  pop: () -> Unit,
) {
  entry<Libraries> {
    Nav3ScreenWrapper(sharedTransitionScope) {
      LibraryAdminScreen(
        collectNavResultEvent = true,
        onCreateLibraryClicked = { navigate(CreateLibrary) },
        onEditLibraryClicked = { navigate(EditLibrary(it)) },
      )
    }
  }
  entry<CreateLibrary> {
    val resultBus = LocalResultEventBus.current
    Nav3ScreenWrapper(sharedTransitionScope) {
      LibraryAdminCreateScreen(
        onNavigateBack = pop,
        onSaved = { id ->
          resultBus.sendResult(LibraryChangedNavResult(id))
          pop()
        },
      )
    }
  }
  entry<EditLibrary> { key ->
    val resultBus = LocalResultEventBus.current
    Nav3ScreenWrapper(sharedTransitionScope) {
      LibraryAdminCreateScreen(
        libraryId = key.libraryId,
        onNavigateBack = pop,
        onSaved = { id ->
          resultBus.sendResult(LibraryChangedNavResult(id))
          pop()
        },
      )
    }
  }
}
