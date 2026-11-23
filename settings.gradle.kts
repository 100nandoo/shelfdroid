pluginManagement {
  includeBuild("build-logic")

  plugins { id("com.android.settings") version "9.5.2" apply false }

  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("com.android.settings")
}

android {
  compileSdk {
    version = release(37) { minorApiLevel = 1 }
  }
  minSdk {
    version = release(29)
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Shelfdroid"

include(":app")

include(":core")

include(":core-data")

include(":core-database")

include(":core-datastore")

include(":core-network")

include(":core-testing")

include(":core-ui")

include(":download")

include(":feature-library-admin")

include(":helper")

include(":media")

include(":navigation-api")

include(":socketio")

include(":test-app")

include(":widget")

include(":benchmark")
