package android.kma.myquizzapp.core.common.validator

/**
 * Validator cho password.
 * Kiểm tra blank và minimum length (configurable).
 */
object PasswordValidator {
    /**
     * Validate password với minimum length tùy chỉnh.
     * 
     * Rules:
     * - Không được để trống
     * - Phải đạt minimum length
     * 
     * @param password Password cần validate
     * @param minLength Độ dài tối thiểu (default = 1 - chỉ cần không rỗng)
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validate(password: String, minLength: Int = 1): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult.Error("Mật khẩu không được để trống")
        }

        if (password.length < minLength) {
            return ValidationResult.Error("Mật khẩu phải có ít nhất $minLength ký tự")
        }

        return ValidationResult.Success
    }

    /**
     * Validate password cho Register (min 8 chars - khớp backend registerSchema).
     * 
     * @param password Password cần validate
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validateForRegister(password: String): ValidationResult {
        return validate(password, minLength = 8)
    }

    /**
     * Validate confirm password khớp với password.
     * 
     * @param password Password gốc
     * @param confirmPassword Password xác nhận
     * @return ValidationResult.Success nếu khớp, ValidationResult.Error nếu không
     */
    fun validateConfirm(password: String, confirmPassword: String): ValidationResult {
        if (confirmPassword.isBlank()) {
            return ValidationResult.Error("Xác nhận mật khẩu không được để trống")
        }

        if (password != confirmPassword) {
            return ValidationResult.Error("Mật khẩu xác nhận không khớp")
        }

        return ValidationResult.Success
    }
}
