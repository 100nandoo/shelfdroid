@Suppress("DSL_SCOPE_VIOLATION") // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "${libs.versions.namespace.get()}.test.navigation"
    compileSdk = libs.versions.targetSdk.get().toInt()
    targetProjectPath = ":app"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

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
    implementation(project(libs.versions.app.get()))
    implementation(project(libs.versions.coreData.get()))
    implementation(project(libs.versions.coreTesting.get()))

    // Testing
    implementation(libs.androidx.test.core)

    // Hilt and instrumented tests.
    implementation(libs.hilt.android.testing)
    kapt(libs.hilt.android.compiler)

    // Compose
    implementation(libs.androidx.compose.ui.test.junit4)
}
