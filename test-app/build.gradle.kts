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
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreData.get()))
  implementation(project(libs.versions.coreDatabase.get()))
  implementation(project(libs.versions.coreDatastore.get()))
  implementation(project(libs.versions.coreNetwork.get()))
  implementation(project(libs.versions.coreTesting.get()))
  implementation(project(libs.versions.coreUi.get()))
  implementation(project(libs.versions.download.get()))
  implementation(project(libs.versions.helper.get()))
  implementation(project(libs.versions.media.get()))
  implementation(project(libs.versions.socketIO.get()))
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)
  implementation(libs.kotlinx.serialization)
  implementation(libs.process.phoenix)
  implementation(libs.retrofit)
  implementation(libs.sqldelight.driver)

  // Testing
  implementation(libs.androidx.test.core)

  // Hilt and instrumented tests.
  implementation(libs.hilt.android.testing)
  ksp(libs.hilt.android.compiler)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation("androidx.compose.ui:ui-test-android")
  implementation(libs.androidx.compose.ui.test.junit4)
}
