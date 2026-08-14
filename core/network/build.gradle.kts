plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "android.kma.myquizzapp.core.network"
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://api.myquizz.dpdns.org/v1/\"")
        buildConfigField("String", "SOCKET_URL", "\"https://api.myquizz.dpdns.org\"")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":core:common"))

    // Network & Socket
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    // DI & Log
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.timber)
}