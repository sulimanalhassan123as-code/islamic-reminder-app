plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreFile = rootProject.file("keystore.jks")

android {
    namespace = "com.neverhide.islamicreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neverhide.islamicreminder"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    if (keystoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = "neverhide2026"
                keyAlias = "reminder"
                keyPassword = "neverhide2026"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
}
