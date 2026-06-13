import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ksp)
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
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreData.get()))
  implementation(project(libs.versions.media.get()))

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
  implementation(libs.androidx.media3.session)

  implementation(libs.coil)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.guava)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
