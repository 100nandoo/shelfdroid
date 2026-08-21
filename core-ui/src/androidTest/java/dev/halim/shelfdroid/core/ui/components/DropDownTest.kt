package dev.halim.shelfdroid.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DropDownTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun chipDropdownMenu_usesOptionLabelForEmptySelection() {
    composeRule.setContent {
      ChipDropdownMenu(
        options = listOf("", "/audiobookshelf"),
        label = "Callback subfolder",
        labelPosition = LabelPosition.Top,
        initialValue = "",
        optionLabel = { value -> if (value.isEmpty()) "None" else value },
      )
    }

    composeRule.onNodeWithText("Callback subfolder").assertIsDisplayed()
    composeRule.onNodeWithText("None").assertIsDisplayed()
  }
}
