@Suppress(
  "DSL_SCOPE_VIOLATION"
) // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.data"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "dev.halim.shelfdroid.core.testing.HiltTestRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    aidl = false
    buildConfig = false
    renderScript = false
    shaders = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation(project(libs.versions.coreDatabase.get()))
  implementation(project(libs.versions.coreDatastore.get()))
  implementation(project(libs.versions.coreNetwork.get()))

  implementation(libs.kotlinx.serialization)

  // Arch Components
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)

  // datastore
  implementation(libs.androidx.datastore)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
