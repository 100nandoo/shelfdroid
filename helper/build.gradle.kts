plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.helper"
}

dependencies {
  implementation(project(":core"))
  implementation(project(":core-datastore"))

  // Kotlinx
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
