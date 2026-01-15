pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
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

include(":helper")

include(":media")

include(":socketio")

include(":test-app")

include(":benchmark")
