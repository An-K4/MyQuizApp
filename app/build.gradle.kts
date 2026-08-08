plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // Thêm plugin Serialization

    // hilt and ksp
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
            // Khai báo Host URL cho Emulator kết nối tới Server Local (Mục 19.1 Design Doc)
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:3000\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.quizapp.com/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"https://api.quizapp.com\"")
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
        buildConfig = true // Bật BuildConfig để truyền URL từ Gradle vào Kotlin code
    }
}

dependencies {
    // Core Android Framework
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Compose Navigation & Lifecycle (Type-Safe Navigation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Serialization (Bắt buộc dùng thay cho Gson)
    implementation(libs.kotlinx.serialization.json)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Network (Retrofit + OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization) // Converter Serialization
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    // Socket.IO
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json") // Tránh xung đột
    }

    // Auth (Google One-Tap Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    // Database & Local Storage (Room + DataStore)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // UI Utilities
    implementation(libs.coil.compose)
    implementation(libs.timber)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)

    // Android Instrumentation Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}