package dev.halim.shelfdroid.core.ui.screen.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

internal class AndroidOpenIdLoginLauncher(private val context: Context) {

  fun launch(authorizationUrl: String) {
    launchOpenIdAuthorizationUrl(
      authorizationUrl = authorizationUrl,
      resolveCustomTabsPackage = { CustomTabsClient.getPackageName(context, null) },
      launchCustomTab = { packageName, url ->
        CustomTabsIntent.Builder()
          .build()
          .apply {
            if (packageName != null) intent.setPackage(packageName)
          }
          .launchUrl(context, url.toUri())
      },
      launchBrowser = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
      },
    )
  }
}

internal fun launchOpenIdAuthorizationUrl(
  authorizationUrl: String,
  resolveCustomTabsPackage: () -> String?,
  launchCustomTab: (packageName: String?, authorizationUrl: String) -> Unit,
  launchBrowser: (authorizationUrl: String) -> Unit,
) {
  val packageName = resolveCustomTabsPackage()
  try {
    launchCustomTab(packageName, authorizationUrl)
    return
  } catch (_: ActivityNotFoundException) {
    // Fall back if there is no compatible activity or the provider disappears before launch.
  }

  launchBrowser(authorizationUrl)
}

internal fun handleLoginUiEvent(
  event: LoginUiEvent,
  launchOpenIdLogin: (String) -> Unit,
  requestLocalNetworkPermission: () -> Unit,
) {
  when (event) {
    is LoginUiEvent.LaunchOpenIdLogin -> launchOpenIdLogin(event.authorizationUrl)
    LoginUiEvent.RequestLocalNetworkPermission -> requestLocalNetworkPermission()
  }
}
