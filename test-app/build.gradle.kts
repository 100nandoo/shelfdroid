plugins {
  alias(libs.plugins.android.test)
  id("shelfdroid.android-compile")
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.test.navigation"
  targetProjectPath = ":app"

  defaultConfig {
    testInstrumentationRunner = "dev.halim.shelfdroid.core.testing.HiltTestRunner"
  }
}

dependencies {
  implementation(project(":app"))
  implementation(project(":core"))
  implementation(project(":core-data"))
  implementation(project(":core-database"))
  implementation(project(":core-datastore"))
  implementation(project(":core-network"))
  implementation(project(":core-testing"))
  implementation(project(":core-ui"))
  implementation(project(":download"))
  implementation(project(":helper"))
  implementation(project(":media"))
  implementation(project(":navigation-api"))
  implementation(project(":socketio"))
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization)
  implementation(libs.retrofit)
  implementation(libs.sqldelight.driver)

  // Testing
  implementation(libs.androidx.test.core)
  implementation(libs.androidx.test.rules)

  // Hilt and instrumented tests.
  implementation(libs.hilt.android.testing)
  ksp(libs.hilt.android.compiler)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation("androidx.compose.ui:ui-test-android")
  implementation(libs.androidx.compose.ui.test.junit4)
}
