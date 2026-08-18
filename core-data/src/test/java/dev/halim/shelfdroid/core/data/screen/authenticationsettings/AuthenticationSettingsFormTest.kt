package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.response.authenticationsettings.AuthenticationSettingsResponse
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AuthenticationSettingsFormTest {

  @Test
  fun map_preservesComplexHtmlAndReducesSecret() {
    val settings =
      AuthenticationSettingsMapper.map(
        AuthenticationSettingsResponse(
          authLoginCustomMessage = "<p><strong>Welcome</strong> <a href=\"/help\">help</a></p>",
          authOpenIDClientSecret = "never-store-this-value",
        )
      )

    assertTrue(settings.customMessageEnabled)
    assertEquals(
      "<p><strong>Welcome</strong> <a href=\"/help\">help</a></p>",
      settings.customMessage,
    )
    assertFalse(settings.toString().contains("never-store-this-value"))
  }

  @Test
  fun toUpdateRequest_noChangesOmitsPatch() {
    val saved = form(message = "<p>Welcome</p>", methods = listOf(LoginMethod.Local))

    assertNull(AuthenticationSettingsMapper.toUpdateRequest(saved, saved))
  }

  @Test
  fun toUpdateRequest_blankMessageSendsExplicitClear() {
    val saved = form(message = "<p>Welcome</p>", methods = listOf(LoginMethod.Local))
    val draft = saved.copy(customMessage = "", customMessageEnabled = false)

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)

    assertEquals("", request?.authLoginCustomMessage)
    assertNull(request?.authActiveAuthMethods)
  }

  @Test
  fun toUpdateRequest_changesOnlyLoginMethods() {
    val saved = form(message = "<p>Welcome</p>", methods = listOf(LoginMethod.Local))
    val draft = saved.copy(activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId))

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)

    assertNull(request?.authLoginCustomMessage)
    assertEquals(listOf("local", "openid"), request?.authActiveAuthMethods)
  }

  @Test
  fun toUpdateRequest_changesOnlyEditedOpenIdFields() {
    val saved = form(message = "", methods = listOf(LoginMethod.Local))
    val draft =
      saved.copy(
        openId = saved.openId.copy(
          issuerUrl = "https://new-issuer.example",
          clientId = "new-client",
        )
      )

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)!!

    assertEquals("https://new-issuer.example", request.authOpenIDIssuerURL)
    assertEquals("new-client", request.authOpenIDClientID)
    assertNull(request.authOpenIDAuthorizationURL)
    assertNull(request.authOpenIDTokenURL)
    assertNull(request.authOpenIDTokenSigningAlgorithm)
    assertEquals(
      "{\"authOpenIDIssuerURL\":\"https://new-issuer.example\",\"authOpenIDClientID\":\"new-client\"}",
      Json { explicitNulls = false }.encodeToString(request),
    )
  }

  @Test
  fun toUpdateRequest_ignoresNonProviderOpenIdFields() {
    val saved = form(message = "", methods = listOf(LoginMethod.Local))
    val draft =
      saved.copy(
        openId =
          saved.openId.copy(
            clientSecretConfigured = !saved.openId.clientSecretConfigured,
            mobileRedirectUris = listOf("shelfdroid://oauth"),
            buttonText = "Edited button",
          )
      )

    assertNull(AuthenticationSettingsMapper.toUpdateRequest(saved, draft))
    assertFalse(AuthenticationSettingsMapper.hasOpenIdChanges(saved, draft))
  }

  @Test
  fun requestSerialization_omitsUnchangedFields() {
    val request =
      AuthenticationSettingsMapper.toUpdateRequest(
        form(message = "<p>Welcome</p>", methods = listOf(LoginMethod.Local)),
        form(message = "<p>Changed</p>", methods = listOf(LoginMethod.Local)),
      )!!

    assertEquals(
      "{\"authLoginCustomMessage\":\"<p>Changed</p>\"}",
      Json { explicitNulls = false }.encodeToString(request),
    )
  }

  @Test
  fun validation_allowsIncompleteOpenIdWhenDisabled() {
    val form = form(message = "", methods = listOf(LoginMethod.Local))

    assertTrue(form.validation().isValid)
  }

  @Test
  fun validation_rejectsNoLoginMethodAndIncompleteEnabledOpenId() {
    val noMethods = form(message = "", methods = emptyList())
    val incompleteOpenId =
      form(message = "", methods = listOf(LoginMethod.OpenId)).copy(openId = OpenIdSettingsSummary())

    assertTrue(
      AuthenticationSettingsValidationError.NoLoginMethod in noMethods.validation().errors
    )
    assertTrue(
      AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete in
        incompleteOpenId.validation().errors
    )
  }

  @Test
  fun mergeDiscovery_updatesProviderFieldsAndKeepsUnrelatedDraftValues() {
    val start = form(message = "", methods = listOf(LoginMethod.Local))
    val draft =
      start.copy(
        openId =
          start.openId.copy(
            clientId = "edited-client",
            mobileRedirectUris = listOf("audiobookshelf://oauth"),
            buttonText = "Use company login",
          )
      )

    val merged =
      AuthenticationSettingsMapper.mergeDiscovery(
        current = draft,
        operationStart = start,
        discovery =
          OpenIdDiscoveryResult(
            issuerUrl = "https://issuer.example/normalized",
            authorizationUrl = "https://issuer.example/authorize-new",
            tokenUrl = "https://issuer.example/token-new",
            userInfoUrl = "https://issuer.example/userinfo-new",
            jwksUrl = "https://issuer.example/jwks-new",
            logoutUrl = "https://issuer.example/logout-new",
            signingAlgorithms = listOf("RS256", "ES256"),
          ),
      )

    assertEquals("https://issuer.example/normalized", merged.openId.issuerUrl)
    assertEquals("https://issuer.example/authorize-new", merged.openId.authorizationUrl)
    assertEquals("edited-client", merged.openId.clientId)
    assertEquals(listOf("audiobookshelf://oauth"), merged.openId.mobileRedirectUris)
    assertEquals("Use company login", merged.openId.buttonText)
    assertEquals("RS256", merged.openId.tokenSigningAlgorithm)
  }

  private fun form(
    message: String,
    methods: List<LoginMethod>,
  ): AuthenticationSettingsForm =
    AuthenticationSettingsSummary(
      customMessageEnabled = message.isNotEmpty(),
      customMessage = message,
      activeLoginMethods = methods,
      openId =
        OpenIdSettingsSummary(
          issuerUrl = "https://issuer.example.com",
          authorizationUrl = "https://issuer.example.com/authorize",
          tokenUrl = "https://issuer.example.com/token",
          userInfoUrl = "https://issuer.example.com/userinfo",
          jwksUrl = "https://issuer.example.com/jwks",
          clientId = "shelfdroid",
          tokenSigningAlgorithm = "RS256",
        ),
    )
}
