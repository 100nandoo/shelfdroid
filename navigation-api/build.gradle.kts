import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "${libs.versions.namespace.get()}.navigation.api"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

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
  api(project(libs.versions.core.get()))
  api(libs.androidx.navigation3.runtime)
  api(libs.kotlinx.serialization)
  implementation(platform(libs.androidx.compose.bom))

  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.junit)
}
