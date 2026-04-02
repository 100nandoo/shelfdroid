import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.sqldelight)
}

android {
  namespace = "${libs.versions.namespace.get()}.core.database"
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

sqldelight {
  databases {
    create("MyDatabase") { packageName.set("${libs.versions.namespace.get()}.core.database") }
  }
}

dependencies {
  implementation(project(libs.versions.core.get()))

  // kotlin
  implementation(libs.kotlinx.serialization)

  // sqldelight
  implementation(libs.sqldelight.driver)
  implementation(libs.sqldelight.coroutines)
  implementation(libs.sqldelight.primitives)
}
