plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
  alias(libs.plugins.sqldelight)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.database"
}

sqldelight {
  databases {
    create("MyDatabase") { packageName.set("${libs.versions.namespace.get()}.core.database") }
  }
}

dependencies {
  implementation(project(":core"))

  // kotlin
  implementation(libs.kotlinx.serialization)

  // sqldelight
  implementation(libs.sqldelight.driver)
  implementation(libs.sqldelight.coroutines)
  implementation(libs.sqldelight.primitives)

  testImplementation(libs.junit)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
}
