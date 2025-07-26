package dev.halim.shelfdroid.core.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import dev.halim.shelfdroid.core.ui.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationPermissionHandler(
  snackbarHostState: SnackbarHostState,
  onPermissionGranted: () -> Unit,
): () -> Unit {
  val isTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
  val state =
    if (isTiramisu) {
      rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
      if (wasGranted) {
        onPermissionGranted()
      }
    }

  return {
    when {
      state == null || state.status == PermissionStatus.Granted -> onPermissionGranted()
      state.status.shouldShowRationale -> {
        scope.launch {
          val result =
            snackbarHostState.showSnackbar(
              message = context.getString(R.string.permission_required),
              actionLabel = context.getString(R.string.go_to_settings),
            )
          if (result == SnackbarResult.ActionPerformed) {
            val intent =
              Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
              )
            context.startActivity(intent)
          }
        }
      }
      else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}
