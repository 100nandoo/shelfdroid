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
  fun toUpdateRequest_changedCallbacksIncludeExplicitEmptySubfolder() {
    val saved = form(message = "", methods = listOf(LoginMethod.Local)).copy(
      openId = form(message = "", methods = listOf(LoginMethod.Local)).openId.copy(
        mobileRedirectUris = listOf("audiobookshelf://oauth"),
        subfolderForRedirectUrls = "/shelf",
      ),
    )
    val draft = saved.copy(
      openId = saved.openId.copy(
        mobileRedirectUris = listOf("audiobookshelf://oauth", "sampleapp://oauth/callback"),
        subfolderForRedirectUrls = "",
      ),
    )

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)!!

    assertEquals(
      listOf("audiobookshelf://oauth", "sampleapp://oauth/callback"),
      request.authOpenIDMobileRedirectURIs,
    )
    assertEquals("", request.authOpenIDSubfolderForRedirectURLs)
    assertEquals(
      "{\"authOpenIDMobileRedirectURIs\":[\"audiobookshelf://oauth\",\"sampleapp://oauth/callback\"],\"authOpenIDSubfolderForRedirectURLs\":\"\"}",
      Json { explicitNulls = false }.encodeToString(request),
    )
  }

  @Test
  fun validation_acceptsServerRedirectUrisAndSoleWildcard() {
    val settings = form(message = "", methods = listOf(LoginMethod.Local)).copy(
      openId = form(message = "", methods = listOf(LoginMethod.Local)).openId.copy(
        mobileRedirectUris = listOf("audiobookshelf://oauth", "sampleapp://oauth/callback"),
      ),
    )
    assertTrue(settings.validation().isValid)
    assertTrue(
      settings.copy(openId = settings.openId.copy(mobileRedirectUris = listOf("*")))
        .validation()
        .isValid
    )
  }

  @Test
  fun validation_rejectsInvalidRedirectUriAndWildcardCombination() {
    val base = form(message = "", methods = listOf(LoginMethod.Local))
    val invalid = base.copy(openId = base.openId.copy(mobileRedirectUris = listOf("https://")))
    assertTrue(
      AuthenticationSettingsValidationError.InvalidMobileRedirectUri in invalid.validation().errors
    )

    val wildcard = base.copy(
      openId = base.openId.copy(mobileRedirectUris = listOf("*", "audiobookshelf://oauth")),
    )
    assertTrue(
      AuthenticationSettingsValidationError.WildcardMobileRedirectUriMustBeSoleEntry in
        wildcard.validation().errors
    )
  }

  @Test
  fun validation_matchesAudiobookshelfRedirectUriBoundaries() {
    val base = form(message = "", methods = listOf(LoginMethod.Local))
    val accepted =
      listOf(
        "audiobookshelf://oauth",
        "sampleapp://host.example/path_1/file-name",
        // The server contract uses JavaScript `\\w+` for the scheme, so these remain accepted.
        "1scheme://host",
        "_scheme://host/path",
      )
    accepted.forEach { uri ->
      assertTrue(
        "Audiobookshelf accepts $uri",
        base.copy(openId = base.openId.copy(mobileRedirectUris = listOf(uri))).validation().isValid,
      )
    }

    val rejected =
      listOf(
        "scheme://",
        "scheme://host?query",
        "scheme://host#fragment",
        "scheme+plus://host",
        "scheme://host/path with space",
        "scheme://host/path?query",
      )
    rejected.forEach { uri ->
      assertFalse(
        "Audiobookshelf rejects $uri",
        base.copy(openId = base.openId.copy(mobileRedirectUris = listOf(uri))).validation().isValid,
      )
    }
  }

  @Test
  fun validation_rejectsCallbackSubfolderOutsideServerChoices() {
    val settings = form(message = "", methods = listOf(LoginMethod.Local)).copy(
      openId = form(message = "", methods = listOf(LoginMethod.Local)).openId.copy(
        subfolderForRedirectUrls = "/invented",
      ),
    )

    assertTrue(
      AuthenticationSettingsValidationError.InvalidCallbackSubfolder in
        settings.validation(callbackSubfolderOptions = listOf("", "/shelf")).errors
    )
  }

  @Test
  fun callbackUrls_preserveRootAndSubpathInstallations() {
    val settings = OpenIdSettingsSummary(subfolderForRedirectUrls = "")
    assertEquals(
      "https://example.com/auth/openid/callback",
      settings.callbackUrls("https://example.com").web,
    )

    val subpath = settings.copy(subfolderForRedirectUrls = "/audiobookshelf")
    val callbacks = subpath.callbackUrls("https://example.com/audiobookshelf/")
    assertEquals("https://example.com/audiobookshelf/auth/openid/callback", callbacks.web)
    assertEquals("https://example.com/audiobookshelf/auth/openid/mobile-redirect", callbacks.mobile)
  }

  @Test
  fun toUpdateRequest_includesChangedCallbackFields() {
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

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)!!

    assertEquals(listOf("shelfdroid://oauth"), request.authOpenIDMobileRedirectURIs)
    assertNull(request.authOpenIDSubfolderForRedirectURLs)
    assertTrue(AuthenticationSettingsMapper.hasOpenIdChanges(saved, draft))
  }

  @Test
  fun toUpdateRequest_untouchedSecretIsOmitted() {
    val saved = form(message = "", methods = listOf(LoginMethod.Local))
    val draft = saved.copy(customMessage = "<p>Changed</p>", customMessageEnabled = true)

    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)!!

    assertNull(request.authOpenIDClientSecret)
    assertFalse(Json { explicitNulls = false }.encodeToString(request).contains("ClientSecret"))
  }

  @Test
  fun toUpdateRequest_replacementSendsEnteredSecretExactly() {
    val saved = form(message = "", methods = listOf(LoginMethod.Local))

    val request =
      AuthenticationSettingsMapper.toUpdateRequest(
        saved,
        saved,
        AuthenticationSettingsSecretUpdate.Replace("replacement-secret"),
      )!!

    assertEquals("replacement-secret", request.authOpenIDClientSecret)
    assertEquals(
      "{\"authOpenIDClientSecret\":\"replacement-secret\"}",
      Json { explicitNulls = false }.encodeToString(request),
    )
  }

  @Test
  fun toUpdateRequest_clearSendsExplicitEmptyString() {
    val saved =
      form(message = "", methods = listOf(LoginMethod.Local, LoginMethod.OpenId)).copy(
        openId = form(message = "", methods = listOf(LoginMethod.Local)).openId.copy(
          clientSecretConfigured = true,
        ),
      )

    val request =
      AuthenticationSettingsMapper.toUpdateRequest(
        saved,
        saved,
        AuthenticationSettingsSecretUpdate.Clear,
      )!!

    assertEquals("", request.authOpenIDClientSecret)
    assertEquals(
      "{\"authOpenIDClientSecret\":\"\"}",
      Json { explicitNulls = false }.encodeToString(request),
    )
  }

  @Test
  fun validation_rejectsClearingConfiguredSecretWhileOpenIdIsEnabled() {
    val settings =
      form(message = "", methods = listOf(LoginMethod.Local, LoginMethod.OpenId)).copy(
        openId = form(message = "", methods = listOf(LoginMethod.Local)).openId.copy(
          clientSecretConfigured = true,
        ),
      )

    assertFalse(settings.validation(AuthenticationSettingsSecretUpdate.Clear).isValid)
    assertTrue(
      AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete in
        settings.validation(AuthenticationSettingsSecretUpdate.Clear).errors
    )
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
