import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
}

android {
  namespace = "${libs.versions.namespace.get()}.media"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
}

dependencies {
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreNetwork.get()))
  implementation(project(libs.versions.coreData.get()))
  implementation(project(libs.versions.download.get()))
  implementation(project(libs.versions.helper.get()))

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.guava)

  // Media3
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.compose.ui)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)

  // Coil
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
}
