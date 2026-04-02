import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ksp)
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
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreDatabase.get()))
  implementation(project(libs.versions.coreDatastore.get()))
  implementation(project(libs.versions.coreNetwork.get()))
  implementation(project(libs.versions.download.get()))
  implementation(project(libs.versions.helper.get()))

  implementation(libs.kotlinx.serialization)

  // Hilt Components
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.datetime)

  // datastore
  implementation(libs.androidx.datastore)

  // exoplayer
  implementation(libs.androidx.media3.exoplayer)

  // process phoenix
  implementation(libs.process.phoenix)

  // sqldelight
  implementation(libs.sqldelight.driver)
  implementation(libs.sqldelight.coroutines)

  implementation(libs.retrofit)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
