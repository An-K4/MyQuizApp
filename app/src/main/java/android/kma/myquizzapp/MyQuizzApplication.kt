package android.kma.myquizzapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyQuizzApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Plant Timber debug tree for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialized - Debug logging enabled")
        }
    }
}
