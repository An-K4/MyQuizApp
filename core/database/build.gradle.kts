plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.hilt)
}

android {
    namespace = "android.kma.myquizzapp.core.database"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Gọi Module Common
    implementation(project(":core:common"))

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}