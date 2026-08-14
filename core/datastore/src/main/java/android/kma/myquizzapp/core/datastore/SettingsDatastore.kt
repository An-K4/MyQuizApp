package android.kma.myquizzapp.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val themeMode: Flow<String> = context.dataStore.data
        .map { it[KEY_THEME] ?: "system" }

    val onboardingSeen: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING] ?: false }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME] = mode }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = seen }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_seen")
    }
}