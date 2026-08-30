package android.kma.myquizzapp.core.common.repository

/**
 * Các lệnh chỉ player được gửi trên `/game`.
 *
 * Tương ứng client event của backend: `question:answer` (có ack), `question:next`
 * (self-paced), `player:sync`, `lobby:leave`.
 *
 * N18 chưa dùng interface này (player lobby là N19); khai báo sẵn để chốt ranh giới
 * vai trò ngay từ đầu, tránh sau này nhồi lệnh player vào repository của host.
 */
interface PlayerGameSocketRepository : GameSocketRepository {

    /** `lobby:leave` — rời phòng khi còn ở lobby. */
    suspend fun leaveLobby()

    /**
     * `question:answer` — gửi đáp án, server trả kết quả qua ack callback.
     *
     * Đáp án đa kiểu (id, danh sách id, hoặc text tự luận) nên nhận String đã
     * serialize ở tầng data; domain không phụ thuộc JsonElement.
     */
    suspend fun submitAnswer(rawAnswerJson: String)

    /** `question:next` — player tự chuyển câu (mode self-paced). */
    suspend fun requestNextQuestion()

    /** `player:sync` — xin lại `game:state` sau khi reconnect. */
    suspend fun sync()
}
