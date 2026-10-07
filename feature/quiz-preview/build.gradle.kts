plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "android.kma.myquizzapp.feature.quiz_preview"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}
