import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreFile = rootProject.file("keystore.jks")

// SECURITY (2026-11-06): signing passwords used to be hardcoded here AND in the
// CI workflow, while the keystore itself was committed to this public repo.
// The key has been rotated. Credentials now come from local.properties for
// local builds, or from CI secrets exposed as environment variables.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingSecret(key: String, env: String): String =
    localProps.getProperty(key) ?: System.getenv(env) ?: ""

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

    if (keystoreFile.exists() && signingSecret("keystore.storePassword", "KEYSTORE_STORE_PASSWORD").isNotEmpty()) {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = signingSecret("keystore.storePassword", "KEYSTORE_STORE_PASSWORD")
                keyAlias = "reminder"
                keyPassword = signingSecret("keystore.keyPassword", "KEYSTORE_KEY_PASSWORD")
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
