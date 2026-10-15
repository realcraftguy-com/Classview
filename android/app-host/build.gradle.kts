import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.realcraftguy.classview.host"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.realcraftguy.classview.host"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        // Die Tailscale-Bibliothek ist nur für arm64 gebaut (Galaxy S21 FE).
        ndk { abiFilters += "arm64-v8a" }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        // Die Go-Bibliothek (~22 MB) komprimiert speichern; sonst wird die APK sehr groß.
        jniLibs { useLegacyPackaging = true }
        dex { useLegacyPackaging = true }
        resources {
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties", "META-INF/{AL2.0,LGPL2.1}")
            pickFirsts += setOf("META-INF/versions/9/previous-compilation-data.bin")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Gebaute Lehrer-Web-App (web/dist) als assets/web in die APK packen.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/webAssets"))
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

val tsbridgeAar = file("libs/tsbridge.aar")

val copyWebApp by tasks.registering(Sync::class) {
    from(rootProject.file("web/dist"))
    into(layout.buildDirectory.dir("generated/webAssets/web"))
}

val checkTsbridge by tasks.registering {
    doFirst {
        if (!tsbridgeAar.exists()) {
            throw GradleException(
                "android/app-host/libs/tsbridge.aar fehlt. Erst ./android/tailscale-bridge/build.sh ausführen (Go + Android NDK nötig).",
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(copyWebApp, checkTsbridge) }

dependencies {
    implementation(project(":android:core-ui"))
    implementation(project(":backend:server"))
    implementation(files(tsbridgeAar))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
}

tasks.withType<Test>().configureEach {
    systemProperty("mockupDir", rootProject.file("docs/mockups/android/host").absolutePath)
}
