plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.myquizzapp.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}