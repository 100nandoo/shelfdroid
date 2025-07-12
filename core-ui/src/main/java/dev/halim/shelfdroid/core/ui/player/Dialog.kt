package dev.halim.shelfdroid.core.ui.player

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog

@Composable
fun DeleteDialog(showDialog: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  MyAlertDialog(
    title = "Delete Bookmark",
    text = "Are you sure you want to delete this bookmark?",
    showDialog = showDialog,
    confirmText = "Delete",
    dismissText = "Cancel",
    onConfirm = { onConfirm() },
    onDismiss = { onDismiss() },
  )
}
