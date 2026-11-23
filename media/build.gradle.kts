plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.media"
}

dependencies {
  implementation(project(":core"))
  implementation(project(":core-network"))
  implementation(project(":core-data"))
  implementation(project(":download"))
  implementation(project(":helper"))

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.guava)

  // Media3
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.compose.ui)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)

  // Coil
  implementation(libs.coil)
  implementation(libs.coil.okhttp)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.mockito)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
