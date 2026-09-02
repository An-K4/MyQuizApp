package android.kma.myquizzapp.core.common.validator

/**
 * Kết quả validation.
 * Dùng sealed interface để type-safe handling.
 */
sealed interface ValidationResult {
    /**
     * Validation thành công.
     */
    data object Success : ValidationResult

    /**
     * Validation thất bại với message lỗi.
     */
    data class Error(val message: String) : ValidationResult
}

/**
 * Extension để check nhanh validation có thành công không.
 */
val ValidationResult.isSuccess: Boolean
    get() = this is ValidationResult.Success

/**
 * Extension để check nhanh validation có lỗi không.
 */
val ValidationResult.isError: Boolean
    get() = this is ValidationResult.Error

/**
 * Extension để lấy error message (null nếu Success).
 */
val ValidationResult.errorMessage: String?
    get() = (this as? ValidationResult.Error)?.message
