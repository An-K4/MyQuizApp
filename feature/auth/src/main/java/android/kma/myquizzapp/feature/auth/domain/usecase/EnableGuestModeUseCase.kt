package android.kma.myquizzapp.feature.auth.domain.usecase

import android.kma.myquizzapp.core.datastore.SettingsDataStore
import javax.inject.Inject

/**
 * Enables guest mode by persisting the preference to DataStore.
 * Called when user clicks "Play as Guest" button in LoginScreen.
 *
 * Once enabled, user will bypass auth screen on subsequent app launches
 * and navigate directly to guest features (PlayerGraph/HomeGraph).
 */
class EnableGuestModeUseCase @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke() {
        settingsDataStore.setGuestMode(enabled = true)
    }
}
