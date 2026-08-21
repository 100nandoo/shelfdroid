package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementDialog
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomMetadataProviderManagementContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun providerHeaders_areMaskedByDefaultAndEachShowHideControlIsIndependent() {
    val storedProvider = provider(authHeaderValue = "Bearer stored-secret")
    val events = mutableListOf<CustomMetadataProviderManagementEvent>()
    var uiState by
      mutableStateOf(
        CustomMetadataProviderManagementUiState(
          state = GenericState.Success,
          providers = listOf(storedProvider),
          nameDraft = "Community",
          urlDraft = "https://provider.example",
          authHeaderDraft = "Bearer draft-secret",
        )
      )

    composeRule.setContent {
      CustomMetadataProviderManagementContent(uiState) { event ->
        events += event
        when (event) {
          is CustomMetadataProviderManagementEvent.SetAuthHeaderVisible ->
            uiState = uiState.copy(authHeaderVisible = event.visible)
          is CustomMetadataProviderManagementEvent.SetProviderVisible ->
            uiState =
              uiState.copy(
                revealedProviderIds =
                  if (event.visible) uiState.revealedProviderIds + event.providerId
                  else uiState.revealedProviderIds - event.providerId
              )
          else -> Unit
        }
      }
    }

    composeRule.onNodeWithText("Provider name").assertIsDisplayed()
    composeRule.onNodeWithText("Provider URL").assertIsDisplayed()
    composeRule.onNodeWithText("Add provider").assertIsDisplayed().assertIsEnabled()
    composeRule.onAllNodesWithText("Bearer draft-secret").assertCountEquals(0)
    composeRule.onAllNodesWithText("Bearer stored-secret").assertCountEquals(0)
    composeRule.onAllNodesWithContentDescription("Show authorization header").assertCountEquals(2)

    composeRule.onAllNodesWithContentDescription("Show authorization header")[0].performClick()
    composeRule.onNodeWithText("Bearer draft-secret").assertIsDisplayed()
    assertTrue(
      events.contains(CustomMetadataProviderManagementEvent.SetAuthHeaderVisible(visible = true))
    )

    composeRule.onNodeWithContentDescription("Hide authorization header").performClick()
    composeRule.onAllNodesWithText("Bearer draft-secret").assertCountEquals(0)

    composeRule.onAllNodesWithContentDescription("Show authorization header")[1].performClick()
    composeRule.onNodeWithText("Bearer stored-secret").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Hide authorization header").performClick()
    composeRule.onAllNodesWithText("Bearer stored-secret").assertCountEquals(0)
  }

  @Test
  fun storedProvider_hasDeleteControlAndConfirmationNamesProviderAndGoogleFallback_withoutEdit() {
    val storedProvider = provider()
    composeRule.setContent {
      CustomMetadataProviderManagementContent(
        CustomMetadataProviderManagementUiState(
          state = GenericState.Success,
          providers = listOf(storedProvider),
          dialog = CustomMetadataProviderManagementDialog.Delete(storedProvider),
        ),
        onEvent = {},
      )
    }

    composeRule.onNodeWithContentDescription("Delete Custom metadata provider").assertIsDisplayed()
    composeRule.onAllNodesWithContentDescription("Edit Custom metadata provider").assertCountEquals(0)
    composeRule.onNodeWithText("Delete Custom metadata provider").assertIsDisplayed()
    composeRule.onNodeWithText("Community", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("Google metadata source", substring = true).assertIsDisplayed()
  }

  private fun provider(authHeaderValue: String? = null) =
    CustomMetadataProvider(
      id = "provider-1",
      name = "Community",
      url = "https://provider.example",
      slug = "custom-provider-1",
      authHeaderValue = authHeaderValue,
    )
}
