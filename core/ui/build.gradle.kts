plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.compose)
}

android {
    namespace = "com.example.myquizzapp.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Gọi Module Common
    implementation(project(":core:common"))

    // Hiển thị ảnh Coil (dùng cho Avatar, Quiz Image)
    implementation(libs.coil.compose)
}