package android.kma.myquizzapp.core.common.model

/**
 * Represents the authentication state of the user.
 * Used by SplashViewModel to determine initial navigation destination.
 */
enum class AuthState {
    /**
     * First app launch - user has never chosen login or guest mode.
     * Navigation: AuthGraph (show Login screen)
     */
    FIRST_LAUNCH,

    /**
     * Guest mode enabled - user chose to use app without authentication.
     * Navigation: PlayerGraph/HomeGraph (guest features)
     */
    GUEST_MODE,

    /**
     * Authenticated - user has valid login session (verified with backend).
     * Navigation: HostGraph (full features)
     */
    AUTHENTICATED
}
