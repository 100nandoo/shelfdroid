import java.util.Properties
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress(
  "DSL_SCOPE_VIOLATION"
) // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.hilt.gradle)
  alias(libs.plugins.kotlin.kapt)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = libs.versions.namespace.get()
  compileSdk = libs.versions.targetSdk.get().toInt()

  signingConfigs {
    val keystorePropertiesFile =
      if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        file("..\\..\\signing\\keystore.properties")
      } else {
        file("../../signing/keystore.properties")
      }

    if (keystorePropertiesFile.exists()) {
      val properties = Properties().apply { load(keystorePropertiesFile.inputStream()) }

      create("release") {
        storePassword = properties.getProperty("storePassword")
        keyPassword = properties.getProperty("keyPassword")
        keyAlias = properties.getProperty("keyAlias")
        storeFile = rootProject.file(properties.getProperty("storeFile"))
      }
    }
  }

  defaultConfig {
    applicationId = libs.versions.namespace.get()
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()

    vectorDrawables { useSupportLibrary = true }
    buildConfigField("String", "MEDIA3_VERSION", "\"${libs.versions.androidxMedia3.get()}\"")
  }

  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      isDebuggable = true
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (signingConfigs.names.contains("release")) {
        signingConfig = signingConfigs.getByName("release")
      }
      ndk { debugSymbolLevel = "full" }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    renderScript = false
    shaders = false
  }

  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

  lint { disable += listOf("Instantiatable") }
}

dependencies {
  implementation(project(libs.versions.core.get()))
  implementation(project(libs.versions.coreUi.get()))
  implementation(project(libs.versions.coreData.get()))

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Hilt Dependency Injection
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.icon.extended)
  implementation(libs.androidx.compose.material3)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Coil
  implementation(libs.coil)
  implementation(libs.coil.okhttp)
}
