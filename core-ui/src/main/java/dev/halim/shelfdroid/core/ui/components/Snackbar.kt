package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

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

suspend fun SnackbarHostState.showSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.GENERIC))
}

suspend fun SnackbarHostState.showErrorSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.ERROR))
}

suspend fun SnackbarHostState.showSuccessSnackbar(message: String) {
  showSnackbar(AppSnackbarVisuals(message, SnackbarType.SUCCESS))
}

@Composable
fun BoxScope.MySnackbarHost(snackbarHostState: SnackbarHostState) {
  SnackbarHost(
    hostState = snackbarHostState,
    modifier = Modifier.align(Alignment.TopCenter).imePadding().zIndex(1f),
  ) { data ->
    val type = (data.visuals as? AppSnackbarVisuals)?.type

    val containerColor =
      when (type) {
        SnackbarType.SUCCESS -> MaterialTheme.colorScheme.tertiary
        SnackbarType.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.inverseSurface
      }

    val contentColor =
      when (type) {
        SnackbarType.SUCCESS -> MaterialTheme.colorScheme.onTertiary
        SnackbarType.ERROR -> MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.inverseOnSurface
      }

    Snackbar(snackbarData = data, containerColor = containerColor, contentColor = contentColor)
  }
}
