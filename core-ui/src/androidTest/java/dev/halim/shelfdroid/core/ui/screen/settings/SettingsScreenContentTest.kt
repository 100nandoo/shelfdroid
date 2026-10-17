package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenContentTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun widgetSettingOpensWidgetSettings() {
    var widgetSettingClicked = false
    composeRule.setContent {
      SettingsScreenContent(onWidgetClicked = { widgetSettingClicked = true })
    }

    composeRule.onNodeWithText("Widget").performScrollTo().performClick()

    assertTrue(widgetSettingClicked)
  }
}
