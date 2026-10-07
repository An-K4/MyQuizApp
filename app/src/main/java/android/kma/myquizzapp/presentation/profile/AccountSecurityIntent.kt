package android.kma.myquizzapp.presentation.profile

sealed interface AccountSecurityIntent {
    class OldPassword(val value: String) : AccountSecurityIntent { override fun toString() = "OldPassword([REDACTED])" }
    class NewPassword(val value: String) : AccountSecurityIntent { override fun toString() = "NewPassword([REDACTED])" }
    class ConfirmPassword(val value: String) : AccountSecurityIntent { override fun toString() = "ConfirmPassword([REDACTED])" }
    class DeactivationPassword(val value: String) : AccountSecurityIntent { override fun toString() = "DeactivationPassword([REDACTED])" }
    data object ChangePassword : AccountSecurityIntent
    data object RequestDeactivation : AccountSecurityIntent
    data object CancelDeactivation : AccountSecurityIntent
    data object ConfirmDeactivation : AccountSecurityIntent
    data object RetryLocalCleanup : AccountSecurityIntent
    data object Back : AccountSecurityIntent
}
