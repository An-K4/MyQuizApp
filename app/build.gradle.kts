plugins {
    alias(libs.plugins.android.application)   // GIỮ — không dùng myquizzapp.android.library
    alias(libs.plugins.kotlin.android)        // GIỮ
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myquizzapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myquizzapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api.myquizz.dpdns.org/v1/\"")
            buildConfigField("String", "SOCKET_URL", "\"https://api.myquizz.dpdns.org\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BASE_URL", "\"https://api.myquizz.dpdns.org/v1/\"")
            buildConfigField("String", "SOCKET_URL", "\"https://api.myquizz.dpdns.org\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // 1. NHÚNG TẤT CẢ MODULE CON
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:lobby"))
    implementation(project(":feature:game-player"))
    implementation(project(":feature:game-host"))
    implementation(project(":feature:leaderboard"))
    implementation(project(":feature:quiz-manage"))

    // 2. CHỈ GIỮ LẠI THƯ VIỆN CẦN THIẾT CHO MAINACTIVITY & NAVIGATION
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt Entry Point
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}