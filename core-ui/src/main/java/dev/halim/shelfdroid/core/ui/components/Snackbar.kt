package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

enum class SnackbarType {
  SUCCESS,
  ERROR,
  GENERIC,
}

class AppSnackbarVisuals(
  override val message: String,
  val type: SnackbarType = SnackbarType.GENERIC,
) : SnackbarVisuals {
  override val actionLabel: String? = null
  override val withDismissAction: Boolean = false
  override val duration: SnackbarDuration = SnackbarDuration.Short
}

suspend fun SnackbarHostState.showPlainSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.GENERIC))
}

suspend fun SnackbarHostState.showErrorSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.ERROR))
}

suspend fun SnackbarHostState.showSuccessSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.SUCCESS))
}

@Composable
private fun AppSnackbar(data: SnackbarData, type: SnackbarType?) {
  Snackbar(
    snackbarData = data,
    containerColor = snackbarContainerColor(type),
    contentColor = snackbarContentColor(type),
  )
}

@Composable
private fun PreviewSnackbar(message: String, type: SnackbarType) {
  Snackbar(
    containerColor = snackbarContainerColor(type),
    contentColor = snackbarContentColor(type),
  ) {
    Text(message)
  }
}

@Composable
private fun snackbarContainerColor(type: SnackbarType?): androidx.compose.ui.graphics.Color =
  when (type) {
    SnackbarType.SUCCESS -> MaterialTheme.colorScheme.tertiary
    SnackbarType.ERROR -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.inverseSurface
  }

@Composable
private fun snackbarContentColor(type: SnackbarType?): androidx.compose.ui.graphics.Color =
  when (type) {
    SnackbarType.SUCCESS -> MaterialTheme.colorScheme.onTertiary
    SnackbarType.ERROR -> MaterialTheme.colorScheme.onError
    else -> MaterialTheme.colorScheme.inverseOnSurface
  }

@Composable
fun BoxScope.MySnackbarHost(snackbarHostState: SnackbarHostState) {
  SnackbarHost(
    hostState = snackbarHostState,
    modifier = Modifier.align(Alignment.TopCenter).imePadding().zIndex(1f),
  ) { data ->
    val type = (data.visuals as? AppSnackbarVisuals)?.type
    AppSnackbar(data = data, type = type)
  }
}

@ShelfDroidPreview
@Composable
private fun MySnackbarHostPreview() {
  PreviewWrapper {
    Column {
      PreviewSnackbar(message = "Generic message", type = SnackbarType.GENERIC)
      PreviewSnackbar(message = "Success message", type = SnackbarType.SUCCESS)
      PreviewSnackbar(message = "Error message", type = SnackbarType.ERROR)
    }
  }
}

@Composable
private fun SnackbarPreview(type: SnackbarType, message: String) {
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(message, type) { snackbarHostState.showSnackbar(AppSnackbarVisuals(message, type)) }

  PreviewWrapper(dynamicColor = false) {
    Box(modifier = Modifier.fillMaxSize()) { MySnackbarHost(snackbarHostState = snackbarHostState) }
  }
}

@ShelfDroidPreview
@Composable
private fun ErrorSnackbarPreview() {
  SnackbarPreview(type = SnackbarType.ERROR, message = "Connection to the server failed")
}

@ShelfDroidPreview
@Composable
private fun SuccessSnackbarPreview() {
  SnackbarPreview(type = SnackbarType.SUCCESS, message = "Backup restored successfully")
}
