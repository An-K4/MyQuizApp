plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "android.kma.myquizzapp.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    // N18: GameSocketRepository expose Flow<GameEvent> trong chữ ký public nên
    // coroutines phải là `api`, không phải `implementation` — nếu để
    // implementation thì core:network và feature:lobby không thấy được kiểu Flow
    // khi implement/collect interface này.
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}