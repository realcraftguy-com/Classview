pluginManagement {
    repositories {
        google()
        // Spiegel von Maven Central (Google Cloud), falls Maven Central drosselt
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Spiegel von Maven Central (Google Cloud), falls Maven Central drosselt
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "classview"

include(":backend:protocol")
include(":backend:server")
include(":android:core-ui")
include(":android:app-student")
include(":android:app-host")
