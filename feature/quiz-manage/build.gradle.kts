plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "android.kma.myquizzapp.feature.quiz_manage"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 1. Nhúng các Core Modules cần thiết
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":core:datastore"))

    // 2. Compose Navigation & Lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    // 2a. Activity Result API (N15 - Android Photo Picker để chọn ảnh quiz/câu hỏi)
    implementation(libs.androidx.activity)

    // 2b. Paging 3 (N13-14 - danh sách "Quiz của tôi", cursor pagination)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 3. Coil (Hiển thị preview ảnh khi upload/chỉnh sửa quiz)
    implementation(libs.coil.compose)

    // 4. Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}