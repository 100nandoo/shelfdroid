@Suppress("DSL_SCOPE_VIOLATION") // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "dev.halim.shelfdroid.test.navigation"
    compileSdk = 35
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 21
        targetSdk = 35

        testInstrumentationRunner = "dev.halim.shelfdroid.core.testing.HiltTestRunner"
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core-data"))
    implementation(project(":core-testing"))
    implementation(project(":feature-audiobook"))

    // Testing
    implementation(libs.androidx.test.core)

    // Hilt and instrumented tests.
    implementation(libs.hilt.android.testing)
    kapt(libs.hilt.android.compiler)

    // Compose
    implementation(libs.androidx.compose.ui.test.junit4)
}
