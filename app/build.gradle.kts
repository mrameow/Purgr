plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.purgr.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.purgr.app"
        minSdk = 30 // MediaStore trash requests (restorable deletes) need Android 11+
        targetSdk = 35
        // CI passes the build number so every release installs over the previous one.
        versionCode = (project.findProperty("purgrVersionCode") as String?)?.toInt() ?: 1
        versionName = "1.0." + ((project.findProperty("purgrVersionCode") as String?) ?: "0")
    }

    signingConfigs {
        // A fixed key so sideloaded updates install over each other. It is committed to the
        // repo, so it is NOT secret: use your own key (via the env vars) for a store release.
        create("release") {
            storeFile = file(System.getenv("PURGR_KEYSTORE") ?: "../keystore/purgr-release.jks")
            storePassword = System.getenv("PURGR_KEYSTORE_PASSWORD") ?: "purgr-sideload"
            keyAlias = System.getenv("PURGR_KEY_ALIAS") ?: "purgr"
            keyPassword = System.getenv("PURGR_KEY_PASSWORD") ?: "purgr-sideload"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
}
