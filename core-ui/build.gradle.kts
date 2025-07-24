import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress(
  "DSL_SCOPE_VIOLATION"
) // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.ui"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    renderScript = false
    shaders = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xcontext-receivers")
    }
  }
}

dependencies {
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreData.get()))
  implementation(project(libs.versions.media.get()))

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.kotlinx.serialization)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.hilt.navigation.compose)

  // Compose
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.icon.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.text)
  implementation(libs.androidx.compose.ui.tooling.preview)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  // Coil
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
}
