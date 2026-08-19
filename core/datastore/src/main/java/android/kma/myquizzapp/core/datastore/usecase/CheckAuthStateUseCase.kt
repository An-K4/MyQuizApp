package android.kma.myquizzapp.core.datastore.usecase

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
 * Lives in `core:datastore` (not `app` or `feature:auth`) because it is a
 * cross-cutting concern needed by more than just the splash screen: any
 * feature can depend on `core:datastore` to check auth state, e.g. to show a
 * login prompt when a guest user tries to use a feature that requires an
 * account. It only needs `core:common`'s AuthRepository and this module's own
 * SettingsDataStore, so it does not need to live inside a specific feature
 * module or in `app`.
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
