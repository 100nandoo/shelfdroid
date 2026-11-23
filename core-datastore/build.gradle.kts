plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.datastore"
}

dependencies {
  implementation(project(":core"))

  // datastore
  implementation(libs.androidx.datastore)

  implementation(libs.kotlinx.serialization)

  // hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
