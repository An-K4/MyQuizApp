plugins {
    alias(libs.plugins.android.application)   // GIỮ — không dùng myquizzapp.android.library
    alias(libs.plugins.kotlin.android)        // GIỮ
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "android.kma.myquizzapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "android.kma.myquizzapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.kma.myquizzapp.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // BASE_URL / SOCKET_URL: chỉ khai báo ở core:network (single source of truth).
            // Module app không có dependency Retrofit/OkHttp và không gọi thẳng server,
            // nên không cần khai trùng BuildConfig field ở đây.
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

    // 2. CHỈ GIữ LẠI THƯ VIỆN CẦN THIẾT CHO MAINACTIVITY & NAVIGATION
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    
    // Timber logging
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)

    // Hilt Entry Point
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}