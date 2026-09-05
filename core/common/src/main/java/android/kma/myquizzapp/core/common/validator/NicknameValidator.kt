package android.kma.myquizzapp.core.common.validator

/**
 * Validator cho nickname của người chơi khách (guest) khi vào phòng.
 *
 * Giới hạn 1–50 ký tự copy nguyên từ `joinGameSchema.player_name` ở backend
 * (game.schema.ts): validate ở client chỉ để báo lỗi sớm, server vẫn kiểm lại.
 * Nếu backend đổi giới hạn thì sửa ở đây cho khớp, đừng nới rộng phía client.
 *
 * Người dùng ĐÃ ĐĂNG NHẬP không đi qua validator này: server tự lấy fullname từ
 * phiên đăng nhập và bỏ qua body, nên app cũng không hỏi tên.
 */
object NicknameValidator {
    private const val MIN_LENGTH = 1
    const val MAX_LENGTH = 50

    /**
     * Validate nickname.
     *
     * Rules:
     * - Không được để trống (khoảng trắng cũng tính là trống)
     * - Tối đa 50 ký tự (tính trên chuỗi đã trim, đúng như phần sẽ gửi lên server)
     */
    fun validate(nickname: String): ValidationResult {
        val trimmed = nickname.trim()

        if (trimmed.length < MIN_LENGTH) {
            return ValidationResult.Error("Tên hiển thị không được để trống")
        }

        if (trimmed.length > MAX_LENGTH) {
            return ValidationResult.Error("Tên hiển thị không được vượt quá $MAX_LENGTH ký tự")
        }

        return ValidationResult.Success
    }
}
