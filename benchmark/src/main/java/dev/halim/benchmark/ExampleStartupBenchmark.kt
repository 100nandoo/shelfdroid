package dev.halim.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>`
 *    tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces for
 * investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleStartupBenchmark {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  // ExampleStartupBenchmark_startup
  // timeToFullDisplayMs min 705.8, median 729.5, max 778.3
  // timeToInitialDisplayMs min 232.8, median 254.0, max 289.1
  // Traces: Iteration 0 1 2 3 4
  @Test
  fun startup() {
    benchmarkLogin(isStartup = true, setupBlock = { login() }) {
      pressHome()
      startActivityAndWait()
      device.wait(Until.findObject(By.text("Main")), 5000)
    }
  }

  // ExampleStartupBenchmark_frameTiming
  // frameCount           min 38.0,   median 76.0,   max 82.0
  // frameDurationCpuMs   P50   3.6,   P90  12.5,   P95  19.1,   P99  67.8
  // frameOverrunMs       P50  -7.4,   P90   5.2,   P95  30.2,   P99  57.0
  // Traces: Iteration 0 1 2 3 4
  @Test
  fun frameTiming() {
    benchmarkLogin(isFrameTiming = true, setupBlock = { login() }) {
      pressHome()
      startActivityAndWait()
      device.wait(Until.findObject(By.text("Main")), 5000)
    }
  }

  private fun MacrobenchmarkScope.login() {
    device.findObject(By.res("server"))?.text = ""
    device.findObject(By.res("username"))?.text = ""
    device.findObject(By.res("password"))?.text = ""
    device.findObject(By.res("login"))?.click()
    device.wait(Until.findObject(By.text("Main")), 5000)
  }

  private fun benchmarkLogin(
    isStartup: Boolean = false,
    isFrameTiming: Boolean = false,
    setupBlock: MacrobenchmarkScope.() -> Unit = {},
    measureBlock: MacrobenchmarkScope.() -> Unit = {},
  ) {
    val metrics = mutableListOf<Metric>()

    if (isStartup) {
      metrics.add(StartupTimingMetric())
    }
    if (isFrameTiming) {
      metrics.add(FrameTimingMetric())
    }
    benchmarkRule.measureRepeated(
      packageName = "dev.halim.shelfdroid",
      metrics = metrics,
      iterations = 5,
      startupMode = StartupMode.COLD,
      setupBlock = setupBlock,
      measureBlock = measureBlock,
    )
  }
}
