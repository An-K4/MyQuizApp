package android.kma.myquizzapp.domain.usecase

import android.kma.myquizzapp.core.common.model.AuthState
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Determines the current authentication state by checking:
 * 1. If user has a valid authenticated session (cookies + backend verification)
 * 2. If guest mode was previously enabled (stored in DataStore)
 * 3. Otherwise, first launch
 *
 * Used by SplashViewModel to decide the initial navigation destination.
 *
 * This use case lives in `app`, not `feature:auth`, because deciding the app's
 * initial route on cold start is app-level composition logic (it only needs
 * `core:common`'s AuthRepository and `core:datastore`'s SettingsDataStore, both
 * already core-level dependencies of `app`), not a feature:auth business rule.
 * Keeping it here means `app` no longer needs to depend on feature:auth's
 * internal domain package just for splash routing.
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
