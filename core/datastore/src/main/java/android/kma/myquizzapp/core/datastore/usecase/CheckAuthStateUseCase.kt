package android.kma.myquizzapp.core.datastore.usecase

import android.kma.myquizzapp.core.common.model.AuthState
import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.datastore.SettingsDataStore
import javax.inject.Inject

/**
 * Determines the current authentication state by checking:
 * 1. If user has a valid authenticated session (cookies + backend verification)
 * 2. Otherwise, guest - covers both first launch and previously-chosen guest mode
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

        // Not authenticated - treat as guest (Option B: Splash always
        // navigates straight to Home). Persist the guest flag so it is
        // explicit going forward; no-op if already set. First launch and
        // previously-chosen guest mode are intentionally treated the same.
        settingsDataStore.setGuestMode(true)
        return AuthState.GUEST
    }
}
