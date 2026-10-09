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
        versionCode = 5
        versionName = "0.5"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
