package android.kma.myquizzapp.core.common.validator

/**
 * Validator cho quiz name.
 * Kiểm tra blank và length constraints.
 */
object QuizNameValidator {
    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 100

    /**
     * Validate quiz name.
     * 
     * Rules:
     * - Không được để trống
     * - Tối thiểu 3 ký tự
     * - Tối đa 100 ký tự
     * 
     * @param name Quiz name cần validate
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validate(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult.Error("Tên quiz không được để trống")
        }

        if (name.length < MIN_LENGTH) {
            return ValidationResult.Error("Tên quiz phải có ít nhất $MIN_LENGTH ký tự")
        }

        if (name.length > MAX_LENGTH) {
            return ValidationResult.Error("Tên quiz không được vượt quá $MAX_LENGTH ký tự")
        }

        return ValidationResult.Success
    }
}
