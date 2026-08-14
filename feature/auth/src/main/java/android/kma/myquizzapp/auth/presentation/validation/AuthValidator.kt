package android.kma.myquizzapp.auth.presentation.validation

object AuthValidator {
    // Dùng kotlin.Regex thay android.util.Patterns để object này chạy được trong
    // LOCAL unit test (android.util.* không tồn tại trên JVM test — bài học "test
    // phải chạy được trên máy lạ" của N5).
    private val EMAIL = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE = Regex("^\\+?[0-9]{7,15}$") // copy y nguyên từ auth.schema.ts

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email không được để trống"
        !EMAIL.matches(email.trim()) -> "Email không hợp lệ"
        else -> null
    }

    /** LOGIN: backend chỉ yêu cầu min(1) — đừng chặn user bằng rule min(8) ở màn login,
     *  vì tài khoản cũ có thể không theo rule mới. */
    fun loginPasswordError(password: String): String? =
        if (password.isEmpty()) "Mật khẩu không được để trống" else null

    /** REGISTER: password min(8) — khớp registerSchema. */
    fun registerPasswordError(password: String): String? = when {
        password.isEmpty() -> "Mật khẩu không được để trống"
        password.length < 8 -> "Mật khẩu tối thiểu 8 ký tự"
        else -> null
    }

    fun fullnameError(name: String): String? = when {
        name.isBlank() -> "Họ tên không được để trống"
        name.trim().length < 2 -> "Họ tên tối thiểu 2 ký tự"
        name.length > 100 -> "Họ tên tối đa 100 ký tự"
        else -> null
    }

    fun phoneError(phone: String): String? = when {
        phone.isBlank() -> null // optional
        !PHONE.matches(phone) -> "Số điện thoại phải gồm 7–15 chữ số"
        else -> null
    }
}