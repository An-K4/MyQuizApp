package android.kma.myquizzapp.core.common.validator

/**
 * Validator cho room settings (room name, player limit, time per question).
 * Kiểm tra constraints cho việc tạo game room.
 */
object RoomSettingsValidator {
    private const val MIN_ROOM_NAME_LENGTH = 2
    private const val MAX_ROOM_NAME_LENGTH = 100
    private const val MIN_PLAYERS = 1
    private const val MAX_PLAYERS = 100
    private const val MIN_TIME_PER_QUESTION = 5 // seconds
    private const val MAX_TIME_PER_QUESTION = 300 // seconds (5 minutes)

    /**
     * Validate room name.
     * 
     * Rules:
     * - Không được để trống
     * - Tối thiểu 2 ký tự
     * - Tối đa 100 ký tự
     * 
     * @param roomName Room name cần validate
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validateRoomName(roomName: String): ValidationResult {
        if (roomName.isBlank()) {
            return ValidationResult.Error("Tên phòng không được để trống")
        }

        if (roomName.trim().length < MIN_ROOM_NAME_LENGTH) {
            return ValidationResult.Error("Tên phòng phải có ít nhất $MIN_ROOM_NAME_LENGTH ký tự")
        }

        if (roomName.trim().length > MAX_ROOM_NAME_LENGTH) {
            return ValidationResult.Error("Tên phòng không được vượt quá $MAX_ROOM_NAME_LENGTH ký tự")
        }

        return ValidationResult.Success
    }

    /**
     * Validate max players.
     * 
     * Rules:
     * - Tối thiểu 1 người chơi
     * - Tối đa 100 người chơi
     * 
     * @param maxPlayers Số lượng người chơi tối đa
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validateMaxPlayers(maxPlayers: Int): ValidationResult {
        if (maxPlayers < MIN_PLAYERS) {
            return ValidationResult.Error("Số người chơi phải ít nhất $MIN_PLAYERS")
        }

        if (maxPlayers > MAX_PLAYERS) {
            return ValidationResult.Error("Số người chơi không được vượt quá $MAX_PLAYERS")
        }

        return ValidationResult.Success
    }

    /**
     * Validate time per question.
     * 
     * Rules:
     * - Tối thiểu 5 giây
     * - Tối đa 300 giây (5 phút)
     * 
     * @param timePerQuestion Thời gian cho mỗi câu hỏi (giây)
     * @return ValidationResult.Success nếu hợp lệ, ValidationResult.Error nếu không
     */
    fun validateTimePerQuestion(timePerQuestion: Int): ValidationResult {
        if (timePerQuestion < MIN_TIME_PER_QUESTION) {
            return ValidationResult.Error("Thời gian phải ít nhất $MIN_TIME_PER_QUESTION giây")
        }

        if (timePerQuestion > MAX_TIME_PER_QUESTION) {
            return ValidationResult.Error("Thời gian không được vượt quá $MAX_TIME_PER_QUESTION giây")
        }

        return ValidationResult.Success
    }
}
