package dev.halim.shelfdroid.test.app

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.test.app.testdi.FakeApiService
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AppTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createEmptyComposeRule()

  @Inject lateinit var fakeApiService: FakeApiService
  @Inject lateinit var dataStoreManager: DataStoreManager

  private var scenario: ActivityScenario<MainActivity>? = null

  @Before
  fun setUp() {
    hiltRule.inject()
    resetAppState()
  }

  @After
  fun tearDown() {
    scenario?.close()
    scenario = null
    resetAppState()
  }

  @Test
  fun launch_gates_to_login_when_logged_out() {
    launchMainActivity()

    composeTestRule.onNodeWithText("Login").assertExists()
    composeTestRule.onNodeWithText("Server address").assertExists()
  }

  @Test
  fun home_supports_media_drill_down_and_relogin() {
    launchMainActivity()
    login()

    composeTestRule.onNodeWithText("Daily Bytes").assertExists()
    composeTestRule.onRoot().performTouchInput { swipeRight() }
    composeTestRule.onNodeWithText("Systems Book").performClick()
    composeTestRule.onNodeWithText("Systems Book").assertExists()

    composeTestRule.activity?.onBackPressedDispatcher?.onBackPressed()
    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithText("Settings").performClick()
    composeTestRule.onNodeWithText("Re-login").performClick()
    composeTestRule.onNodeWithText("OK").performClick()
    composeTestRule.onNodeWithText("Login").performClick()

    composeTestRule.onNodeWithText("Daily Bytes").assertExists()
  }

  @Test
  fun api_key_screen_renders_seeded_list() {
    launchMainActivity()
    login()

    composeTestRule.onRoot().performTouchInput { swipeLeft() }
    composeTestRule.onNodeWithText("API Keys").performClick()
    waitForText("Primary Key")
    composeTestRule.onNodeWithText("Primary Key").assertExists()
  }

  private fun launchMainActivity() {
    scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
    waitForText("Login", "Daily Bytes")
  }

  private fun login() {
    composeTestRule.onNodeWithTag("server").performTextInput("example.com")
    composeTestRule.onNodeWithTag("Username").performTextInput("root")
    composeTestRule.onNodeWithTag("Password").performTextInput("root")
    composeTestRule.onNodeWithTag("Login").performClick()
    waitForText("Daily Bytes")
  }

  private fun resetAppState() {
    scenario?.close()
    scenario = null
    fakeApiService.reset()
    runBlocking { dataStoreManager.clear() }
    val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
    targetContext.deleteDatabase(DatabaseModule.DATABASE_NAME)
  }

  private fun waitForText(vararg values: String) {
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      values.any { value -> composeTestRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
  }

  private val androidx.compose.ui.test.junit4.ComposeTestRule.activity: MainActivity?
    get() = scenario?.let { current ->
      var activity: MainActivity? = null
      current.onActivity { activity = it }
      activity
    }
}
