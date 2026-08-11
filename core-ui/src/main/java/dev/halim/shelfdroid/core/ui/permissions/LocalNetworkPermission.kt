package dev.halim.shelfdroid.core.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.halim.shelfdroid.core.ui.extensions.findActivity

private const val ANDROID_17_API_LEVEL = 37

internal data class LocalNetworkPermissionHandler(
  val requestPermission: () -> Unit,
  val openAppSettings: () -> Unit,
)

@Composable
internal fun rememberLocalNetworkPermissionHandler(
  onPermissionResult: (granted: Boolean, permanentlyDenied: Boolean) -> Unit
): LocalNetworkPermissionHandler {
  val currentOnPermissionResult = rememberUpdatedState(onPermissionResult)
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val permanentlyDenied =
        !granted &&
          !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_LOCAL_NETWORK,
          )
      currentOnPermissionResult.value(granted, permanentlyDenied)
    }

  return remember(context, activity, launcher) {
    LocalNetworkPermissionHandler(
      requestPermission = {
        val granted =
          Build.VERSION.SDK_INT < ANDROID_17_API_LEVEL ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
              PackageManager.PERMISSION_GRANTED
        if (granted) {
          currentOnPermissionResult.value(true, false)
        } else {
          launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
      },
      openAppSettings = {
        context.startActivity(
          Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
          )
        )
      },
    )
  }
}
