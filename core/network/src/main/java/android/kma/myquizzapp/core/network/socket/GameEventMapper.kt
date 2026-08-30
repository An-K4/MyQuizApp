package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.network.di.PreserveCaseJson
import android.kma.myquizzapp.core.network.socket.dto.LobbyUpdatedDto
import android.kma.myquizzapp.core.network.socket.dto.SocketErrorDto
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Biến payload thô của socket.io thành [GameEvent] typed.
 *
 * Payload đến dưới dạng org.json.JSONObject (socket.io-client dùng org.json của
 * Android — xem exclude org.json trong build.gradle.kts). Ta không đọc field bằng
 * tay từ JSONObject mà toString() rồi để kotlinx.serialization decode, để việc
 * validate và giá trị mặc định tập trung ở DTO.
 *
 * Class này cố tình KHÔNG throw: một payload lạ không được phép làm chết cả
 * Flow đang giữ lobby. Lỗi parse trả về GameEvent.Failed để UI có cái hiển thị.
 */
class GameEventMapper @Inject constructor(
    @PreserveCaseJson private val json: Json
) {

    fun map(event: String, payload: Any?): GameEvent = when (event) {
        GameSocketEvents.LOBBY_UPDATED -> mapLobbyUpdated(payload)
        GameSocketEvents.ERROR -> mapError(payload)
        else -> GameEvent.Unhandled(event)
    }

    private fun mapLobbyUpdated(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.LOBBY_UPDATED) {
            json.decodeFromString(LobbyUpdatedDto.serializer(), it)
        }
        return dto?.let { GameEvent.LobbyUpdated(it.toDomain()) }
            ?: GameEvent.Failed(GameSocketEvents.LOBBY_UPDATED, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapError(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.ERROR) {
            json.decodeFromString(SocketErrorDto.serializer(), it)
        }
        return dto?.let { GameEvent.Failed(it.event, it.code) }
            ?: GameEvent.Failed(GameSocketEvents.ERROR, CODE_CLIENT_PARSE_ERROR)
    }

    private fun <T> decode(payload: Any?, event: String, block: (String) -> T): T? {
        val raw = payload?.toString()
        if (raw.isNullOrBlank()) {
            Timber.w("Socket event %s không có payload", event)
            return null
        }
        return runCatching { block(raw) }
            .onFailure { Timber.e(it, "Parse socket event %s thất bại: %s", event, raw) }
            .getOrNull()
    }

    companion object {
        /**
         * Code do CLIENT tự sinh khi không parse được payload — không thuộc
         * vocabulary của backend. Rơi vào nhánh fallback của apiCodeToMessage nên
         * người dùng thấy thông báo chung, còn lỗi thật nằm ở Timber.
         */
        const val CODE_CLIENT_PARSE_ERROR = "CLIENT_PARSE_ERROR"

        /** Handshake bị từ chối nhưng không đọc được code cụ thể từ server. */
        const val CODE_CONNECT_FAILED = "CLIENT_CONNECT_FAILED"
    }
}
