plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.stickeria.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.stickeria.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 13
        versionName = "1.3"
    }
    signingConfigs {
        create("release") {
            val storePath = System.getenv("STICKERIA_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("STICKERIA_STORE_PASSWORD")
                keyAlias = System.getenv("STICKERIA_KEY_ALIAS") ?: "stickeria"
                keyPassword = System.getenv("STICKERIA_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
