plugins {
  alias(libs.plugins.android.test)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
}

android {
  namespace = "dev.halim.benchmark"

  defaultConfig {
    targetSdk = libs.versions.targetSdk.get().toInt()
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It"s signed with a debug key
    // for easy local/CI testing.
    create("benchmark") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":app"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.ext.junit)
  implementation(libs.androidx.test.espresso)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents { beforeVariants(selector().all()) { it.enable = it.buildType == "benchmark" } }
