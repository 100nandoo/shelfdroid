package dev.halim.shelfdroid.test.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class AppTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<dev.halim.shelfdroid.core.ui.screen.MainActivity>()

  @Test
  fun mainActivityLaunches() {
    assertNotNull(composeTestRule.activity)
  }
}
