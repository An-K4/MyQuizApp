package android.kma.myquizzapp.core.common.validator

import android.util.Patterns

/**
 * Validator cho email address.
 * Kiểm tra blank và format hợp lệ.
 */
object EmailValidator {
    /**
     * Validate email address.
     * 
     * Rules:
     * - Không được để trống
     * - Phải match Android email pattern
     * 
     * @param email Email cần validate
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validate(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Error("Email không được để trống")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult.Error("Email không hợp lệ")
        }

        return ValidationResult.Success
    }
}
