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
