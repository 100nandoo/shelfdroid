plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.download"
}

dependencies {
  implementation(project(":core"))
  implementation(project(":core-network"))
  implementation(project(":helper"))

  // Kotlinx
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.core.ktx)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Media3
  implementation(libs.androidx.media3.exoplayer)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
