package dev.halim.shelfdroid.core.data.screen.login

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenIdCallbackCoordinatorTest {

  @Test
  fun handleCallback_whenProviderReturnsError_clearsPendingLoginAndStoresFailure() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      val coordinator = coordinator(dataStoreManager)
      val pendingOpenIdLoginStore = PendingOpenIdLoginStore(dataStoreManager)
      pendingOpenIdLoginStore.save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com/audiobookshelf",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid.debug://oauth?error=access_denied&state=expected-state",
          redirectUri = "dev.halim.shelfdroid.debug://oauth",
          nowMillis = 2_000L,
        )

      assertTrue(result is OpenIdCallbackHandlingResult.Failed)
      assertNull(pendingOpenIdLoginStore.current())
      val failure = coordinator.consumeFailure()
      assertNotNull(failure)
      requireNotNull(failure)
      assertEquals("https://example.com/audiobookshelf", failure.normalizedServer)
      assertEquals(
        "OpenID login was cancelled or denied by the identity provider.",
        failure.errorMessage,
      )
      assertNull(coordinator.consumeFailure())
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun handleCallback_whenStateIsMissing_failsClosed() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      PendingOpenIdLoginStore(dataStoreManager).save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )
      val coordinator = coordinator(dataStoreManager)

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid://oauth?code=abc123",
          redirectUri = "dev.halim.shelfdroid://oauth",
          nowMillis = 2_000L,
        )

      assertTrue(result is OpenIdCallbackHandlingResult.Failed)
      assertNotNull(PendingOpenIdLoginStore(dataStoreManager).current())
      assertEquals(
        "OpenID login failed because the callback is missing the required state.",
        coordinator.consumeFailure()?.errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun handleCallback_whenStateDoesNotMatch_failsClosed() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      PendingOpenIdLoginStore(dataStoreManager).save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )
      val coordinator = coordinator(dataStoreManager)

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid://oauth?code=abc123&state=other-state",
          redirectUri = "dev.halim.shelfdroid://oauth",
          nowMillis = 2_000L,
        )

      assertTrue(result is OpenIdCallbackHandlingResult.Failed)
      assertNotNull(PendingOpenIdLoginStore(dataStoreManager).current())
      assertEquals(
        "OpenID login failed because the callback state does not match the current login.",
        coordinator.consumeFailure()?.errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun handleCallback_whenPendingLoginExpired_failsClosed() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      PendingOpenIdLoginStore(dataStoreManager).save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )
      val coordinator = coordinator(dataStoreManager)

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid://oauth?code=abc123&state=expected-state",
          redirectUri = "dev.halim.shelfdroid://oauth",
          nowMillis = 1_000L + OPEN_ID_LOGIN_CONTEXT_MAX_AGE_MILLIS + 1L,
        )

      assertTrue(result is OpenIdCallbackHandlingResult.Failed)
      assertNull(PendingOpenIdLoginStore(dataStoreManager).current())
      assertEquals(
        "OpenID login expired before the callback returned. Please try again.",
        coordinator.consumeFailure()?.errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun handleCallback_whenTargetIsUnsupported_failsClosed() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      PendingOpenIdLoginStore(dataStoreManager).save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )
      val coordinator = coordinator(dataStoreManager)

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid://unexpected?code=abc123&state=expected-state",
          redirectUri = "dev.halim.shelfdroid://oauth",
          nowMillis = 2_000L,
        )

      assertTrue(result is OpenIdCallbackHandlingResult.Failed)
      assertNotNull(PendingOpenIdLoginStore(dataStoreManager).current())
      assertEquals(
        "OpenID login failed because the callback target is not supported.",
        coordinator.consumeFailure()?.errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun handleCallback_whenCallbackIsValid_persistsPendingCallbackForContinuation() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      PendingOpenIdLoginStore(dataStoreManager).save(
        PendingOpenIdLogin(
          normalizedServer = "https://example.com/audiobookshelf",
          state = "expected-state",
          codeVerifier = "verifier",
          createdAtEpochMillis = 1_000L,
        )
      )
      val callbackStore = PendingOpenIdCallbackStore(dataStoreManager)
      val coordinator = coordinator(dataStoreManager)

      val result =
        coordinator.handleCallback(
          callbackUrl = "dev.halim.shelfdroid.debug://oauth?code=abc123&state=expected-state",
          redirectUri = "dev.halim.shelfdroid.debug://oauth",
          nowMillis = 2_000L,
        )

      assertEquals(OpenIdCallbackHandlingResult.Continue, result)
      assertNotNull(PendingOpenIdLoginStore(dataStoreManager).current())
      assertNull(coordinator.consumeFailure())
      assertEquals(
        PendingOpenIdCallback(
          normalizedServer = "https://example.com/audiobookshelf",
          state = "expected-state",
          code = "abc123",
          receivedAtEpochMillis = 2_000L,
        ),
        callbackStore.current(),
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  private fun coordinator(scope: CoroutineScope): OpenIdCallbackCoordinator {
    return coordinator(dataStoreManager(scope))
  }

  private fun coordinator(dataStoreManager: DataStoreManager): OpenIdCallbackCoordinator {
    return OpenIdCallbackCoordinator(
      pendingOpenIdLoginStore = PendingOpenIdLoginStore(dataStoreManager),
      pendingOpenIdCallbackStore = PendingOpenIdCallbackStore(dataStoreManager),
      openIdLoginFailureStore = OpenIdLoginFailureStore(dataStoreManager),
    )
  }

  private fun dataStoreManager(scope: CoroutineScope): DataStoreManager {
    val file =
      Files.createTempFile("openid-callback", ".preferences_pb").toFile().apply { deleteOnExit() }
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    return DataStoreManager(dataStore)
  }

  private fun dataStoreScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }
}
