package dev.halim.shelfdroid.core.ui.screen.login

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenIdLoginLauncherTest {

  @Test
  fun launchOpenIdAuthorizationUrl_whenCustomTabsProviderExists_usesCustomTabs() {
    val launchedCustomTabs = mutableListOf<Pair<String?, String>>()
    val launchedBrowserUrls = mutableListOf<String>()

    launchOpenIdAuthorizationUrl(
      authorizationUrl = "https://example.com/auth/openid",
      resolveCustomTabsPackage = { "com.android.chrome" },
      launchCustomTab = { packageName, authorizationUrl ->
        launchedCustomTabs += packageName to authorizationUrl
      },
      launchBrowser = { launchedBrowserUrls += it },
    )

    assertEquals(
      listOf("com.android.chrome" to "https://example.com/auth/openid"),
      launchedCustomTabs,
    )
    assertEquals(emptyList<String>(), launchedBrowserUrls)
  }

  @Test
  fun launchOpenIdAuthorizationUrl_whenNoCustomTabsProviderExists_stillUsesCustomTabsIntent() {
    val launchedCustomTabs = mutableListOf<Pair<String?, String>>()
    val launchedBrowserUrls = mutableListOf<String>()

    launchOpenIdAuthorizationUrl(
      authorizationUrl = "https://example.com/auth/openid",
      resolveCustomTabsPackage = { null },
      launchCustomTab = { packageName, authorizationUrl ->
        launchedCustomTabs += packageName to authorizationUrl
      },
      launchBrowser = { launchedBrowserUrls += it },
    )

    assertEquals(listOf(null to "https://example.com/auth/openid"), launchedCustomTabs)
    assertEquals(emptyList<String>(), launchedBrowserUrls)
  }

  @Test
  fun launchOpenIdAuthorizationUrl_whenCustomTabsLaunchFails_fallsBackToBrowser() {
    val launchedBrowserUrls = mutableListOf<String>()

    launchOpenIdAuthorizationUrl(
      authorizationUrl = "https://example.com/auth/openid",
      resolveCustomTabsPackage = { "com.android.chrome" },
      launchCustomTab = { _, _ -> throw ActivityNotFoundException("provider disappeared") },
      launchBrowser = { launchedBrowserUrls += it },
    )

    assertEquals(listOf("https://example.com/auth/openid"), launchedBrowserUrls)
  }

  @Test
  fun launchOpenIdAuthorizationUrl_whenNoProviderAndCustomTabsLaunchFails_fallsBackToBrowser() {
    val launchedBrowserUrls = mutableListOf<String>()

    launchOpenIdAuthorizationUrl(
      authorizationUrl = "https://example.com/auth/openid",
      resolveCustomTabsPackage = { null },
      launchCustomTab = { _, _ -> throw ActivityNotFoundException("no supporting activity") },
      launchBrowser = { launchedBrowserUrls += it },
    )

    assertEquals(listOf("https://example.com/auth/openid"), launchedBrowserUrls)
  }

  @Test
  fun handleLoginUiEvent_whenLaunchOpenIdLoginEvent_delegatesToLauncher() {
    val launchedUrls = mutableListOf<String>()
    var localNetworkPermissionRequested = false

    handleLoginUiEvent(
      event = LoginUiEvent.LaunchOpenIdLogin("https://example.com/auth/openid"),
      launchOpenIdLogin = { launchedUrls += it },
      requestLocalNetworkPermission = { localNetworkPermissionRequested = true },
    )

    assertEquals(listOf("https://example.com/auth/openid"), launchedUrls)
    assertEquals(false, localNetworkPermissionRequested)
  }

  @Test
  fun handleLoginUiEvent_whenRequestLocalNetworkPermissionEvent_delegatesToPermissionRequester() {
    val launchedUrls = mutableListOf<String>()
    var localNetworkPermissionRequested = false

    handleLoginUiEvent(
      event = LoginUiEvent.RequestLocalNetworkPermission,
      launchOpenIdLogin = { launchedUrls += it },
      requestLocalNetworkPermission = { localNetworkPermissionRequested = true },
    )

    assertEquals(emptyList<String>(), launchedUrls)
    assertEquals(true, localNetworkPermissionRequested)
  }
}
