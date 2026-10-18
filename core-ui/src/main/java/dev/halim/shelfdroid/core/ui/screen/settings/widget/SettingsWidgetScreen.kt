package dev.halim.shelfdroid.core.ui.screen.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SettingsWidgetScreen(
  snackbarHostState: SnackbarHostState,
  onRequestPin: suspend () -> Boolean,
) {
  val coroutineScope = rememberCoroutineScope()
  val pinRequestFailedMessage = stringResource(R.string.widget_pin_request_failed)

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    Text(text = stringResource(R.string.widget_settings_description))
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      onClick = {
        coroutineScope.launch {
          val requestSent =
            try {
              onRequestPin()
            } catch (exception: CancellationException) {
              throw exception
            } catch (exception: Exception) {
              false
            }
          if (!requestSent) snackbarHostState.showSnackbar(pinRequestFailedMessage)
        }
      },
    ) {
      Text(text = stringResource(R.string.add_widget))
    }
  }
}

@ShelfDroidPreview
@Composable
private fun SettingsWidgetScreenPreview() {
  PreviewWrapper {
    SettingsWidgetScreen(
      snackbarHostState = remember { SnackbarHostState() },
      onRequestPin = { false },
    )
  }
}
