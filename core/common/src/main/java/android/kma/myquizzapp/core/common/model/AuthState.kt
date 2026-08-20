package android.kma.myquizzapp.core.common.model

/**
 * Represents the authentication state of the user.
 *
 * Option B (Browse-First): Splash always navigates straight to Home
 * regardless of this state. This state is used later by individual
 * features to decide whether to prompt login when a guest tries to use an
 * account-gated feature (e.g. Create Quiz, Create Room).
 */
enum class AuthState {
    /**
     * No valid authenticated session. Covers both "first launch" and
     * "previously chose guest mode" - they are treated identically.
     */
    GUEST,

    /**
     * Authenticated - user has valid login session (verified with backend).
     */
    AUTHENTICATED
}
