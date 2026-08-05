package dev.halim.shelfdroid.core.data.screen.login

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

internal const val OPEN_ID_LOGIN_CONTEXT_MAX_AGE_MILLIS = 15 * 60 * 1000L

class OpenIdCallbackCoordinator
@Inject
constructor(
  private val pendingOpenIdLoginStore: PendingOpenIdLoginStore,
  private val pendingOpenIdCallbackStore: PendingOpenIdCallbackStore,
  private val openIdLoginFailureStore: OpenIdLoginFailureStore,
) {

  suspend fun handleCallback(
    callbackUrl: String?,
    redirectUri: String,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    pendingOpenIdCallbackStore.clear()

    val callback = callbackUrl?.takeIf { it.isNotBlank() }?.let(::parseUri)
    val expectedTarget = parseUri(redirectUri)
    val pendingLogin = pendingOpenIdLoginStore.current()

    if (callback == null || expectedTarget == null || !supportsTarget(callback, expectedTarget)) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage = "OpenID login failed because the callback target is not supported.",
      )
      return
    }

    if (pendingLogin == null) {
      fail(
        pendingLogin = null,
        errorMessage = "OpenID login failed because there is no matching login in progress.",
      )
      return
    }

    if (pendingLogin.isExpired(nowMillis)) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage = "OpenID login expired before the callback returned. Please try again.",
      )
      return
    }

    val callbackQuery = parseQuery(callback.rawQuery)
    val returnedState = callbackQuery["state"]
    if (returnedState.isNullOrBlank()) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage = "OpenID login failed because the callback is missing the required state.",
      )
      return
    }

    if (returnedState != pendingLogin.state) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage =
          "OpenID login failed because the callback state does not match the current login.",
      )
      return
    }

    val providerError = callbackQuery["error"]
    if (!providerError.isNullOrBlank()) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage =
          callbackQuery["error_description"]
            ?.takeIf { it.isNotBlank() }
            ?.let { "OpenID login failed: $it" }
            ?: "OpenID login was cancelled or denied by the identity provider.",
      )
      return
    }

    val code = callbackQuery["code"]
    if (code.isNullOrBlank()) {
      fail(
        pendingLogin = pendingLogin,
        errorMessage =
          "OpenID login failed because the callback did not include an authorization code.",
      )
      return
    }

    openIdLoginFailureStore.clear()
    pendingOpenIdCallbackStore.save(
      PendingOpenIdCallback(
        normalizedServer = pendingLogin.normalizedServer,
        state = returnedState,
        code = code,
        receivedAtEpochMillis = nowMillis,
      )
    )
  }

  private suspend fun fail(
    pendingLogin: PendingOpenIdLogin?,
    errorMessage: String,
  ) {
    recordOpenIdLoginFailure(
      pendingOpenIdLoginStore = pendingOpenIdLoginStore,
      pendingOpenIdCallbackStore = pendingOpenIdCallbackStore,
      openIdLoginFailureStore = openIdLoginFailureStore,
      normalizedServer = pendingLogin?.normalizedServer,
      errorMessage = errorMessage,
    )
  }
}

internal fun PendingOpenIdLogin.isExpired(nowMillis: Long): Boolean {
  return nowMillis - createdAtEpochMillis > OPEN_ID_LOGIN_CONTEXT_MAX_AGE_MILLIS
}

private fun supportsTarget(callback: URI, expectedTarget: URI): Boolean {
  return callback.scheme?.equals(expectedTarget.scheme, ignoreCase = true) == true &&
    callback.host?.equals(expectedTarget.host, ignoreCase = true) == true &&
    normalizePath(callback.path) == normalizePath(expectedTarget.path)
}

private fun normalizePath(path: String?): String {
  return when {
    path.isNullOrEmpty() -> "/"
    path == "/" -> "/"
    else -> path.removeSuffix("/")
  }
}

private fun parseUri(value: String): URI? {
  return runCatching { URI(value) }.getOrNull()
}

private fun parseQuery(rawQuery: String?): Map<String, String> {
  if (rawQuery.isNullOrBlank()) return emptyMap()
  return rawQuery.split("&").associate { entry ->
    val (rawKey, rawValue) =
      entry.split("=", limit = 2).let { parts ->
        parts.first() to parts.getOrElse(1) { "" }
      }
    URLDecoder.decode(rawKey, StandardCharsets.UTF_8) to
      URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
  }
}
