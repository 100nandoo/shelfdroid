import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.ksp)
}

android {
  namespace = "${libs.versions.namespace.get()}.widget"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    buildConfig = false
    shaders = false
  }

  testOptions { unitTests.isIncludeAndroidResources = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
}

dependencies {
  implementation(project(libs.versions.coreUi.get()))
  implementation(project(libs.versions.coreData.get()))

  implementation(libs.androidx.glance)
  implementation(libs.androidx.glance.appwidget)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.material3)
  implementation(libs.kotlinx.coroutines.android)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(libs.androidx.glance.appwidget.testing)
  testImplementation(libs.androidx.glance.testing)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
}
