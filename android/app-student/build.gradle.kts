import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.realcraftguy.classview.student"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.realcraftguy.classview.student"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
        // Vorbelegte Server-Adresse, z. B. per GitHub-Variable CLASSVIEW_SERVER_URL; sonst in der App eintragen.
        val serverUrl = (project.findProperty("classviewServerUrl") as String?) ?: System.getenv("CLASSVIEW_SERVER_URL") ?: ""
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$serverUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Fester Debug-Schlüssel im Repository, damit neue Versionen (auch aus GitHub Actions) sich über die
    // alte installieren lassen, ohne dass App-Daten verloren gehen. Kein Geheimnis – nur für Debug-Builds.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        // Komprimiert packen, damit die APK klein bleibt.
        dex { useLegacyPackaging = true }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


dependencies {
    implementation(project(":android:core-ui"))
    implementation(project(":backend:protocol"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    // Integrationstest gegen den echten Server (läuft im Test-Prozess)
    testImplementation(project(":backend:server"))
}

tasks.withType<Test>().configureEach {
    systemProperty("mockupDir", rootProject.file("docs/mockups/android/student").absolutePath)
}
