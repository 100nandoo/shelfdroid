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

  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

  buildFeatures {
    compose = true
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
  // Project modules
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreDatastore.get()))
  implementation(project(libs.versions.coreUi.get()))
  implementation(project(libs.versions.media.get()))

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

  // Coil
  implementation(libs.coil)

  // Unit testing
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
