package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameConfigValue
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.repository.HostGameSocketRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.toWireJsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hiện thực kênh socket cho vai HOST.
 *
 * @Singleton nhưng giữ [GameSocketClient] riêng (client không phải singleton) nên
 * kết nối của host độc lập với của player.
 *
 * Các lệnh điều khiển trận (start/next/pause/resume/end) là emit không payload,
 * không ack — kết quả quay về qua broadcast (`game:started`, `question:started`...)
 * hoặc qua event `error`.
 *
 * Riêng `lobby:config-update` (N20) có ack nên đi qua [GameSocketClient.emitWithAck]
 * và được [SocketAckMapper] quy về [Result].
 */
@Singleton
class HostGameSocketRepositoryImpl @Inject constructor(
    private val client: GameSocketClient,
    private val ackMapper: SocketAckMapper
) : HostGameSocketRepository {

    override fun events(socketToken: String): Flow<GameEvent> = client.events(socketToken)

    override suspend fun joinLobby() = client.emit(GameSocketEvents.LOBBY_JOIN)

    override suspend fun disconnect() = client.disconnect()

    override suspend fun updateConfig(
        patch: Map<GameConfigKey, GameConfigValue>
    ): Result<ConfigUpdateAck> {
        // Tái dùng đúng bảng ánh xạ dotted-path của REST create-session
        // (`toWireJsonObject`), không viết lại ở đây — hai đường phải luôn đồng bộ.
        val body = JsonObject(mapOf(KEY_CONFIG to patch.toWireJsonObject()))

        // socket.io-client Java chỉ nhận org.json, còn ta dỡng payload bằng
        // kotlinx.serialization → bắt buộc đi qua chuỗi JSON trung gian.
        val ack = client.emitWithAck(
            event = GameSocketEvents.LOBBY_CONFIG_UPDATE,
            payload = JSONObject(body.toString())
        )
        return ackMapper.toConfigUpdateAck(ack)
    }

    override suspend fun startGame() = client.emit(GameSocketEvents.GAME_START)

    override suspend fun nextQuestion() = client.emit(GameSocketEvents.GAME_NEXT)

    override suspend fun pauseGame() = client.emit(GameSocketEvents.GAME_PAUSE)

    override suspend fun resumeGame() = client.emit(GameSocketEvents.GAME_RESUME)

    override suspend fun endGame() = client.emit(GameSocketEvents.GAME_END)

    private companion object {
        /** Backend nhận `raw.config ?? raw`; gửi dạng bọc cho tường minh. */
        const val KEY_CONFIG = "config"
    }
}
