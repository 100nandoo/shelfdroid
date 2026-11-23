plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  id("shelfdroid.android-test-runner")
}

android {
  namespace = "${libs.versions.namespace.get()}.core.testing"
}

dependencies {
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)

  implementation(libs.androidx.test.runner)
  implementation(libs.hilt.android.testing)
}
