package android.kma.myquizzapp.domain.security

import android.kma.myquizzapp.core.common.validator.PasswordValidator
import android.kma.myquizzapp.core.common.validator.errorMessage

data class PasswordErrors(val current: String? = null, val new: String? = null, val confirm: String? = null) {
    val hasErrors get() = current != null || new != null || confirm != null
}

object SecurityValidation {
    // Passwords are opaque: never trim or normalize them.
    fun change(old: String, new: String, confirm: String) = PasswordErrors(
        PasswordValidator.validate(old, 8).errorMessage,
        PasswordValidator.validate(new, 8).errorMessage ?: if (old == new) "Mật khẩu mới phải khác mật khẩu hiện tại" else null,
        PasswordValidator.validateConfirm(new, confirm).errorMessage
    )
    fun deactivate(password: String): String? = PasswordValidator.validate(password, 8).errorMessage
}
