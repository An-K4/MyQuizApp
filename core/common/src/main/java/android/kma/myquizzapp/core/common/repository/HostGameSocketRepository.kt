package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.result.Result

/**
 * Các lệnh chỉ host được gửi trên `/game`.
 *
 * Tương ứng client event của backend: `game:start`, `game:next`, `game:pause`,
 * `game:resume`, `game:end`, `lobby:config-update`.
 *
 * Chú ý sự bất đối xứng về kiểu trả về — không phải ngẫu nhiên mà bám theo
 * backend: [updateConfig] trả [Result] vì `lobby:config-update` CÓ ack; các lệnh
 * điều khiển trận trả Unit vì chúng fire-and-forget, kết quả về sau qua event.
 */
interface HostGameSocketRepository : GameSocketRepository {

    /**
     * `lobby:config-update` — sửa cấu hình phòng khi đang ở lobby.
     *
     * [patch] là các field host vừa đổi (partial), không cần gửi cả config.
     *
     * Hai cạm bẫy đã kiểm chứng bằng code backend:
     *
     * 1. Ack trả [ConfigUpdateAck.config] là config ĐẦY ĐỦ sau normalize, có thể
     *    khác cả ở field không nằm trong [patch]. Phải render lại form từ ack.
     *
     * 2. `onConfigUpdate` KHÔNG kiểm tra `session_status`, nghĩa là gọi khi trận
     *    đã chạy vẫn "thành công" và sửa config giữa trận. Client phải tự chặn:
     *    chỉ cho sửa khi trạng thái còn là lobby.
     *
     * Trả lỗi (không throw) khi host không còn quyền (`GAME_NOT_HOST`), phòng biến
     * mất (`GAME_ROOM_NOT_FOUND`), mất kết nối, hay ack không về kịp.
     */
    suspend fun updateConfig(
        patch: Map<GameConfigKey, GameConfigValue>
    ): Result<ConfigUpdateAck>

    /**
     * `game:start` — chỉ hợp lệ khi session đang ở trạng thái lobby.
     *
     * KHÔNG có ack: hàm trả về ngay sau khi đẩy event đi, hoàn toàn chưa biết
     * trận có bắt đầu hay không. Kết quả về sau theo một trong hai đường:
     * - Thành công: broadcast `game:started` → `GameEvent.GameStarted`.
     * - Thất bại: event `error` → `GameEvent.Failed(event = "game:start", code)`,
     *   thường là `GAME_ALREADY_STARTED` hoặc `GAME_NOT_HOST`.
     *
     * Do đó UI phải tự đặt hạn chờ, không được coi "hàm trả về" là thành công.
     */
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
