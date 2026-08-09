plugins {
    `kotlin-dsl`
}

group = "com.myquizzapp.buildlogic"

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "myquizzapp.android.library"
            implementationClass = "com.myquizzapp.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "myquizzapp.android.compose"
            implementationClass = "com.myquizzapp.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "myquizzapp.android.hilt"
            implementationClass = "com.myquizzapp.buildlogic.AndroidHiltConventionPlugin"
        }
    }
}

dependencies {
    // compileOnly vì AGP/KGP sẽ có sẵn ở build chính, build-logic chỉ cần "nhìn thấy" class để compile
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}