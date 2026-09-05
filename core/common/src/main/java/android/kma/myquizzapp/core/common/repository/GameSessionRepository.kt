package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.result.Result

/** Shared REST contract dùng bởi room setup, lobby và lịch sử trận. */
interface GameSessionRepository {
    suspend fun getGameModes(): Result<List<GameModeDescriptor>>
    suspend fun createGameSession(params: CreateGameSessionParams): Result<CreateGameSessionResult>
    suspend fun getHostToken(gameId: Long): Result<String>

    /**
     * Tra thông tin phòng theo mã (`GET /games/{code}`) — endpoint PUBLIC.
     *
     * Dùng ở màn nhập mã: biết phòng có tồn tại, còn nhận người, và có cho guest
     * vào hay không TRƯỚC khi tạo bản ghi người chơi. Tra cứu không tạo gì cả nên
     * gọi lại bao nhiêu lần cũng an toàn.
     */
    suspend fun lookupRoom(sessionCode: String): Result<RoomLookup>

    /**
     * Vào phòng (`POST /games/{code}/join`, optionalAuth) và lấy socket token.
     *
     * @param playerName chỉ truyền cho GUEST. Người đã đăng nhập phải truyền null:
     *   server tự lấy fullname + avatar từ phiên đăng nhập và Bỏ QUA body, nên gửi
     *   tên lên là vô nghĩa và dễ gây hiểu sai khi đọc log.
     * @param guestId UUID thiết bị, BẮT BUỘC với guest (server validate đúng định
     *   dạng uuid). Giữ cố định theo máy để sau này tra được lịch sử trận của khách.
     */
    suspend fun joinRoom(
        sessionCode: String,
        playerName: String? = null,
        guestId: String? = null
    ): Result<JoinRoomResult>
}
