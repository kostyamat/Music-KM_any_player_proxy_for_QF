import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.qf.musicplayer.ui"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.qf.musicplayer"
        minSdk = 29


        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["sharedUserId"] = "android.uid.system" // Додаємо sharedUserId тут
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            // Увімкни ці два параметри
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8 // Corrected line
        targetCompatibility = JavaVersion.VERSION_1_8 // Corrected line
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")

}