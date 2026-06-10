import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "${libs.versions.namespace.get()}.widget"
  compileSdk = libs.versions.targetSdk.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdk.get().toInt()
    consumerProguardFiles("consumer-rules.pro")
  }

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
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
}
