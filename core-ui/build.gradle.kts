plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.compose.screenshot)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.ui"

  experimentalProperties["android.experimental.enableScreenshotTest"] = true

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":core"))
  implementation(project(":core-data"))
  implementation(project(":download"))
  implementation(project(":helper"))
  implementation(project(":media"))
  implementation(project(":navigation-api"))
  implementation(project(":socketio"))

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  screenshotTestImplementation(composeBom)

  implementation(libs.kotlinx.serialization)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.palette)

  // Arch Components
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)

  // Compose
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
  screenshotTestImplementation(libs.screenshot.validation.api)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  /// 3rd party

  // Accompanist
  implementation(libs.accompanist.permissions)

  // Coil
  implementation(libs.coil)
  implementation(libs.coil.okhttp)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito)
  testImplementation(project(":core-datastore"))
  testImplementation(libs.androidx.datastore)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
