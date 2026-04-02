import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.test)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.test.navigation"
  compileSdk = libs.versions.targetSdk.get().toInt()
  targetProjectPath = ":app"

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "dev.halim.shelfdroid.core.testing.HiltTestRunner"
  }

  buildFeatures {
    buildConfig = false
    shaders = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
}

dependencies {
  implementation(project(libs.versions.app.get()))
  implementation(project(libs.versions.coreData.get()))
  implementation(project(libs.versions.coreTesting.get()))

  // Testing
  implementation(libs.androidx.test.core)

  // Hilt and instrumented tests.
  implementation(libs.hilt.android.testing)
  ksp(libs.hilt.android.compiler)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui.test.junit4)
}
