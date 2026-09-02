package android.kma.myquizzapp.core.common.validator

/**
 * Validator cho quiz description.
 * Kiểm tra length constraints (optional field).
 */
object QuizDescriptionValidator {
    private const val MAX_LENGTH = 500

    /**
     * Validate quiz description.
     * 
     * Rules:
     * - Được phép để trống (optional)
     * - Nếu có giá trị, không được vượt quá 500 ký tự
     * 
     * @param description Quiz description cần validate
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validate(description: String): ValidationResult {
        // Description is optional, blank is OK
        if (description.isBlank()) {
            return ValidationResult.Success
        }

        if (description.length > MAX_LENGTH) {
            return ValidationResult.Error("Mô tả không được vượt quá $MAX_LENGTH ký tự")
        }

        return ValidationResult.Success
    }
}
