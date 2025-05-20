package dev.halim.shelfdroid.test.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.data.di.fakeAudiobooks
import dev.halim.shelfdroid.ui.MainActivity
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun test1() {
    // TODO: Add navigation tests
    composeTestRule.onNodeWithText(fakeAudiobooks.first(), substring = true).assertExists()
  }
}
