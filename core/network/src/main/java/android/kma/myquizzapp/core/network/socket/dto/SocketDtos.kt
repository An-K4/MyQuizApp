package android.kma.myquizzapp.core.network.socket.dto

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.LobbyPlayer
import android.kma.myquizzapp.core.common.model.LobbyState
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.network.dto.IgnoredGameConfigFieldDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO cho payload socket của namespace `/game`.
 *
 * QUAN TRỌNG — naming: payload socket trộn hai kiểu giống REST games API. Field
 * của player/session là snake_case (`player_name`, `session_status`) nhưng `config`
 * và `serverTime` là camelCase. Vì vậy phải decode bằng @PreserveCaseJson (KHÔNG
 * có JsonNamingStrategy.SnakeCase) và khai báo tường minh @SerialName cho các field
 * snake_case. Nếu dùng Json mặc định, namingStrategy sẽ biến đổi cả tên đã override
 * và GameConfig lồng bên trong sẽ vỡ — đúng cái bẫy đã gặp ở N16/N17.
 */
@Serializable
data class LobbyUpdatedDto(
    @SerialName("session_status") val sessionStatus: SessionStatus,
    val config: GameConfig = GameConfig(),
    val players: List<LobbyPlayerDto> = emptyList(),
    val serverTime: String? = null
) {
    fun toDomain() = LobbyState(
        sessionStatus = sessionStatus,
        config = config,
        players = players.map { it.toDomain() },
        serverTime = serverTime
    )
}

/**
 * Một dòng người chơi trong lobby (schema LobbyPlayer ở socket.doc.ts).
 *
 * N19 đã khai báo `player_avatar` và `lives` (backend gửi từ đầu): avatar để hiển
 * thị trong danh sách, `lives` cho mode survival. Cả hai nullable và có default nên
 * payload thiếu field (mode thường, hoặc guest không avatar) vẫn parse được.
 *
 * `status` giữ nguyên String đúng bản chất backend (connected / disconnected /
 * eliminated / finished) — không parse thành enum để backend thêm giá trị mới
 * không làm crash app.
 */
@Serializable
data class LobbyPlayerDto(
    val id: Long,
    @SerialName("player_name") val playerName: String,
    @SerialName("player_score") val playerScore: Int = 0,
    val status: String = "connected",
    @SerialName("player_avatar") val playerAvatar: String? = null,
    val lives: Int? = null
) {
    fun toDomain() = LobbyPlayer(
        id = id,
        playerName = playerName,
        playerScore = playerScore,
        status = status,
        playerAvatar = playerAvatar,
        lives = lives
    )
}

/**
 * Payload của event `error`.
 *
 * Backend chỉ gửi CODE, không gửi câu văn — cùng vocabulary với REST envelope
 * (shared/errors/codes.ts), nên tái dùng luôn AppError.Api(code).toUserMessage().
 *
 * Lưu ý contract: server chỉ emit `error` khi client event KHÔNG có ack callback.
 * Nếu event có ack thì code trả về trong callback dạng `{ error: { code } }`.
 */
@Serializable
data class SocketErrorDto(
    val event: String? = null,
    val code: String
)

/**
 * Payload ack thành công của `lobby:config-update`.
 *
 * Nguyên văn từ `game.socket.ts`:
 * `ack({ ok: true, changed, config: session.config, ignored })`.
 *
 * `config` là FULL config sau `normalizeConfig`, không phải các field vừa gửi — xem
 * ghi chú ở [ConfigUpdateAck]. Mọi field đều có default để backend thêm/bỏt field
 * không làm vỡ parse; `ok=false` được tầng trên coi là shape ngoài contract.
 *
 * Dùng lại [IgnoredGameConfigFieldDto] của REST create-session: cùng shape
 * `{ path, value, reason }` nên không khai báo lại, tránh hai bản lệch nhau.
 */
@Serializable
data class ConfigUpdateAckDto(
    val ok: Boolean = false,
    val changed: Boolean = false,
    val config: GameConfig = GameConfig(),
    val ignored: List<IgnoredGameConfigFieldDto> = emptyList()
) {
    fun toDomain() = ConfigUpdateAck(
        changed = changed,
        config = config,
        ignored = ignored.map { it.toDomain() }
    )
}

/**
 * Payload ack khi handler backend throw: `ack({ error: { code } })`.
 *
 * Khác với [SocketErrorDto] (event `error` cho các event KHÔNG có ack) ở chỗ code
 * nằm lồng trong `error` và không kèm tên event. `error` nullable để payload thành
 * công decode ra null thay vì ném exception — nhờ vậy phân biệt được hai nhánh
 * chỉ bằng một lần decode.
 */
@Serializable
data class SocketAckErrorDto(
    val error: SocketAckErrorBodyDto? = null
)

@Serializable
data class SocketAckErrorBodyDto(val code: String)

/**
 * Payload `game:started` broadcast cho cả room khi trận bắt đầu.
 *
 * Chú ý naming trộn: `total_questions` snake_case nhưng `serverTime` camelCase —
 * đúng lý do phải decode bằng @PreserveCaseJson với @SerialName tường minh.
 */
@Serializable
data class GameStartedDto(
    val mode: GameMode,
    val config: GameConfig = GameConfig(),
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val serverTime: String? = null
)
