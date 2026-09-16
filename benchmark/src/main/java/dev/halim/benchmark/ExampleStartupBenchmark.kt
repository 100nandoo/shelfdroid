package dev.halim.benchmark

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleStartupBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun startup() {
    benchmarkAuthenticatedColdLaunch(metric = StartupTimingMetric())
  }

  @Test
  fun frameTiming() {
    benchmarkAuthenticatedColdLaunch(metric = FrameTimingMetric())
  }

  private val scenario by lazy {
    BenchmarkScenario.from(InstrumentationRegistry.getArguments())
  }

  private fun benchmarkAuthenticatedColdLaunch(metric: Metric) {
    benchmarkRule.measureRepeated(
      packageName = TARGET_PACKAGE,
      metrics = listOf(metric),
      iterations = 5,
      startupMode = StartupMode.COLD,
      setupBlock = {
        device.executeShellCommand("pm clear $TARGET_PACKAGE")
        grantAudioPermission()
        startActivityAndWait()
        login()
      },
    ) {
      pressHome()
      startActivityAndWait()
      openCatalogPage()
    }
  }

  private fun MacrobenchmarkScope.login() {
    requireObject(By.res("server").pkg(TARGET_PACKAGE), "server field").text = scenario.server
    requireObject(By.res("Username").pkg(TARGET_PACKAGE), "username field").text = scenario.username
    requireObject(By.res("Password").pkg(TARGET_PACKAGE), "password field").text = scenario.password
    requireObject(By.res("Login").pkg(TARGET_PACKAGE), "login button").click()
    openCatalogPage()
  }

  private fun MacrobenchmarkScope.grantAudioPermission() {
    val permission =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
      } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
      }
    device.executeShellCommand("pm grant $TARGET_PACKAGE $permission")
  }

  private fun MacrobenchmarkScope.waitForDestination() {
    requireObject(
      By.res("catalog-library")
        .pkg(TARGET_PACKAGE)
        .hasDescendant(By.text(scenario.destinationText).pkg(TARGET_PACKAGE)),
      "catalog destination '${scenario.destinationText}'",
    )
  }

  private fun MacrobenchmarkScope.openCatalogPage() {
    val pager = requireObject(By.res("catalog").pkg(TARGET_PACKAGE), "Catalog pager")
    requireObject(By.res("catalog-library").pkg(TARGET_PACKAGE), "Catalog library page")
    pager.swipe(Direction.RIGHT, SWIPE_PERCENT)
    waitForDestination()
  }

  private fun MacrobenchmarkScope.requireObject(
    selector: BySelector,
    description: String,
  ): UiObject2 =
    checkNotNull(device.wait(Until.findObject(selector), WAIT_TIMEOUT_MS)) {
      "Timed out after ${WAIT_TIMEOUT_MS} ms waiting for $description"
    }

  private data class BenchmarkScenario(
    val server: String,
    val username: String,
    val password: String,
    val destinationText: String,
  ) {
    companion object {
      fun from(arguments: Bundle): BenchmarkScenario =
        BenchmarkScenario(
          server = arguments.requiredValue(SERVER_ARGUMENT),
          username = arguments.requiredValue(USERNAME_ARGUMENT),
          password = arguments.requiredValue(PASSWORD_ARGUMENT),
          destinationText = arguments.requiredValue(DESTINATION_ARGUMENT),
        )
    }
  }

  private companion object {
    const val TARGET_PACKAGE = "dev.halim.shelfdroid"
    const val WAIT_TIMEOUT_MS = 10_000L
    const val SWIPE_PERCENT = 0.9f
    const val SERVER_ARGUMENT = "benchmark.server"
    const val USERNAME_ARGUMENT = "benchmark.username"
    const val PASSWORD_ARGUMENT = "benchmark.password"
    const val DESTINATION_ARGUMENT = "benchmark.destination"

    fun Bundle.requiredValue(name: String): String =
      getString(name)?.takeIf(String::isNotBlank)
        ?: error("Missing non-blank instrumentation argument '$name'")
  }
}
