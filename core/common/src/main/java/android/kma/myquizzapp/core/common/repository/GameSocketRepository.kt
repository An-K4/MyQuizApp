package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.GameEvent
import kotlinx.coroutines.flow.Flow

/**
 * Phần socket dùng chung cho cả host và player trên namespace `/game`.
 *
 * Interface nằm ở core:common (DIP) — impl ở core:network. Hai vai trò được tách
 * thành [HostGameSocketRepository] và [PlayerGameSocketRepository] vì backend phân
 * quyền theo role trong socketToken: host gửi `game:start` sẽ bị từ chối bằng
 * GAME_NOT_HOST nếu token là player, và ngược lại là GAME_PLAYER_ONLY. Tách
 * interface khiến compiler chặn sai vai trò ngay lúc build thay vì lỗi runtime.
 */
interface GameSocketRepository {

    /**
     * Mở kết nối và trả về luồng sự kiện của phiên đó.
     *
     * - Hot behavior: mỗi lần collect là một kết nối mới; huỷ collect sẽ gỡ toàn bộ
     *   listener và disconnect (không rò socket).
     * - Flow này KHÔNG throw khi server từ chối; lỗi được phát ra dưới dạng
     *   [GameEvent.Failed] để ViewModel tự quyết định hiển thị hay điều hướng.
     *
     * @param socketToken JWT ngắn hạn lấy từ REST (`POST /games/{id}/host-token`).
     *   Token mang sẵn gsid/role nên không cần truyền thêm gameId.
     */
    fun events(socketToken: String): Flow<GameEvent>

    /** Gửi `lobby:join`. Server trả lời bằng `lobby:updated` (và `game:state` với host). */
    suspend fun joinLobby()

    /** Ngắt kết nối chủ động, dùng khi người dùng rời màn hình. */
    suspend fun disconnect()
}
