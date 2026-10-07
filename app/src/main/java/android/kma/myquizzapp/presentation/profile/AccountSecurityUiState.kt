package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.domain.security.PasswordErrors
import android.kma.myquizzapp.domain.security.SecurityAccount

enum class SecurityAction { CHANGE_PASSWORD, DEACTIVATE }

data class AccountSecurityUiState(
    val account: SecurityAccount = SecurityAccount(null, true, false),
    val oldPassword: String = "", val newPassword: String = "", val confirmPassword: String = "",
    val deactivationPassword: String = "",
    val passwordErrors: PasswordErrors = PasswordErrors(), val deactivationError: String? = null,
    val busy: SecurityAction? = null, val showConfirmation: Boolean = false,
    val uncertain: SecurityAction? = null, val cleanupPending: Boolean = false, val message: String? = null
) {
    val canEdit get() = account.token != null && !account.googleOnly && busy == null && uncertain == null && !cleanupPending
    override fun toString() = "AccountSecurityUiState(passwords=[REDACTED], busy=$busy, uncertain=$uncertain)"
}
