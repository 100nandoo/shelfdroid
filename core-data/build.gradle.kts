plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.data"

  defaultConfig {
    testInstrumentationRunner = "dev.halim.shelfdroid.core.testing.HiltTestRunner"
  }
}

dependencies {
  implementation(project(":core"))
  implementation(project(":core-database"))
  implementation(project(":core-datastore"))
  implementation(project(":core-network"))
  implementation(project(":socketio"))
  implementation(project(":download"))
  implementation(project(":helper"))

  implementation(libs.kotlinx.serialization)

  // Hilt Components
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)

  // datastore
  implementation(libs.androidx.datastore)

  // exoplayer
  implementation(libs.androidx.media3.exoplayer)

  // sqldelight
  implementation(libs.sqldelight.driver)
  implementation(libs.sqldelight.coroutines)

  implementation(libs.retrofit)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.androidx.datastore)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.retrofit.kotlinx.serialization)
  testImplementation(libs.retrofit.result)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.sqldelight.driver)
  androidTestImplementation(project(":core-testing"))
}
