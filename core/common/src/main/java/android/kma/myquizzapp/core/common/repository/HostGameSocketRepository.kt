package android.kma.myquizzapp.core.common.repository

/**
 * Các lệnh chỉ host được gửi trên `/game`.
 *
 * Tương ứng client event của backend: `game:start`, `game:next`, `game:pause`,
 * `game:resume`, `game:end`, `lobby:config-update`. Tất cả đều không có payload
 * (trừ config-update) vì server đã biết phòng từ socketToken.
 *
 * N18 chỉ implement [joinLobby] + events(); các hàm dưới đây khai báo trước để
 * chốt contract nhưng impl còn TODO — sẽ hoàn thiện ở N20 (host console).
 */
interface HostGameSocketRepository : GameSocketRepository {

    /** `game:start` — chỉ hợp lệ khi session đang ở trạng thái lobby. */
    suspend fun startGame()

    /** `game:next` — chuyển câu tiếp theo (chỉ mode host-paced). */
    suspend fun nextQuestion()

    /** `game:pause` — tạm dừng trận đang chạy. */
    suspend fun pauseGame()

    /** `game:resume` — tiếp tục trận đang tạm dừng. */
    suspend fun resumeGame()

    /** `game:end` — kết thúc trận sớm. */
    suspend fun endGame()
}
