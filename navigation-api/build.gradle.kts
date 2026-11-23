plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "${libs.versions.namespace.get()}.navigation.api"
}

dependencies {
  api(project(":core"))
  api(libs.androidx.navigation3.runtime)
  api(libs.kotlinx.serialization)
  implementation(platform(libs.androidx.compose.bom))

  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.junit)
}
