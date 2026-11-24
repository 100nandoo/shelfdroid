plugins {
  alias(libs.plugins.android.library)
  id("shelfdroid.android-compile")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.widget"

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        // Robolectric's FileDescriptor interceptor accesses SharedSecrets on modern JDKs.
        it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
      }
    }
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  // Project modules
  implementation(project(":core"))
  implementation(project(":core-datastore"))
  implementation(project(":core-ui"))
  implementation(project(":media"))
  implementation(project(":helper"))

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)

  // Compose and Glance
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.glance)
  implementation(libs.androidx.glance.appwidget)
  debugImplementation(libs.androidx.glance.preview)
  debugImplementation(libs.androidx.glance.appwidget.preview)
  debugImplementation(libs.androidx.compose.ui.tooling.preview)

  // Coil
  implementation(libs.coil)

  // Unit testing
  testImplementation(libs.androidx.datastore)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.glance.testing)
  testImplementation(libs.androidx.glance.appwidget.testing)
  testImplementation(libs.robolectric)
}
