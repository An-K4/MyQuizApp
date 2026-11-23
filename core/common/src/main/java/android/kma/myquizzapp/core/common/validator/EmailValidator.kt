package android.kma.myquizzapp.core.common.validator

/** Lightweight UX check, not a replacement for the backend's z.email validation.
 * Callers trim the address before both validation and submission. Keep this check
 * permissive so an address accepted by the server is not blocked on the client.
 */
object EmailValidator {
    private val basicFormat = Regex("^[^@\\s]+@[^@\\s]+$")

    fun validate(email: String): ValidationResult = when {
        email.isBlank() -> ValidationResult.Error("Email không được để trống")
        !basicFormat.matches(email) -> ValidationResult.Error("Email không hợp lệ")
        else -> ValidationResult.Success
    }
}
