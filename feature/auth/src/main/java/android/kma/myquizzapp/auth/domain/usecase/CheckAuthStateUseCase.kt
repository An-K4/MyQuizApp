package android.kma.myquizzapp.auth.domain.usecase

import android.kma.myquizzapp.core.common.model.AuthState
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Determines the current authentication state by checking:
 * 1. If user has valid authenticated session (cookies + backend verification)
 * 2. If guest mode is enabled (stored in DataStore)
 * 3. Otherwise, first launch
 *
 * Used by SplashViewModel to decide initial navigation destination.
 */
class CheckAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsDataStore: SettingsDataStore
) {
    suspend operator fun invoke(): AuthState {
        // Check authentication first (has cookies AND backend confirms valid)
        val isAuthenticated = authRepository.isAuthenticated()
        if (isAuthenticated) return AuthState.AUTHENTICATED

        // Check if guest mode was previously enabled
        val isGuestMode = settingsDataStore.isGuestMode.first()
        if (isGuestMode) return AuthState.GUEST_MODE

        // First launch - no cookies, no guest mode preference
        return AuthState.FIRST_LAUNCH
    }
}
