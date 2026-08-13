package com.myquizzapp.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // GUARD: :app đã là application → không được kéo library plugin vào
        if (!pluginManager.hasPlugin("com.android.application")) {
            pluginManager.apply("myquizzapp.android.library")
        }
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val applicationExt = extensions.findByType<ApplicationExtension>()
        val libraryExt = extensions.findByType<LibraryExtension>()
        check(applicationExt != null || libraryExt != null) {
            "myquizzapp.android.compose cần android application/library apply trước"
        }
        applicationExt?.buildFeatures?.compose = true
        libraryExt?.buildFeatures?.compose = true

        dependencies {
            "implementation"(platform(libs.findLibrary("androidx.compose.bom").get()))
            "implementation"(libs.findLibrary("androidx.compose.ui").get())
            "implementation"(libs.findLibrary("androidx.compose.ui.tooling.preview").get())
            "implementation"(libs.findLibrary("androidx.material3").get())
            "implementation"(libs.findLibrary("androidx.material.icons.extended").get())
            "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
            "debugImplementation"(libs.findLibrary("androidx.compose.ui.tooling").get())
        }
    }
}