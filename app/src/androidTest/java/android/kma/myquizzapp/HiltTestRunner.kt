package android.kma.myquizzapp

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for Hilt instrumented tests.
 * 
 * This runner replaces the default Application with HiltTestApplication,
 * allowing Hilt to inject dependencies in instrumented tests.
 * 
 * Usage: Set this in app/build.gradle.kts:
 *   testInstrumentationRunner = "android.kma.myquizzapp.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
