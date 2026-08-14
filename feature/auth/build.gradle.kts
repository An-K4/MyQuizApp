plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myquizzapp.feature.auth"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 1. Nhúng các Core Modules cần thiết
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:datastore"))

    // 2. Google Sign-In Credential Manager (Chỉ riêng Feature Auth dùng)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    // 3. Compose Navigation & Lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    // 4. Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}