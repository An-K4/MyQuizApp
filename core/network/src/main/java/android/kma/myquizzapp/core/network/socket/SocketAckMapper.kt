package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.ConfigUpdateAck
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.di.PreserveCaseJson
import android.kma.myquizzapp.core.network.socket.dto.ConfigUpdateAckDto
import android.kma.myquizzapp.core.network.socket.dto.SocketAckErrorDto
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

/**
 * Biến payload ack thô của socket thành [Result] typed.
 *
 * Vì sao cần class riêng thay vì nhét vào [GameEventMapper]: ack KHÁC event. Event
 * đi một chiều và luôn phải ra một GameEvent (không được throw, không được mất),
 * còn ack là kết quả của một lệnh do người dùng bấm nên phải ra Result để
 * ViewModel biết nút vừa bấm thành hay bại.
 *
 * Contract lỗi (đúng mục 3.7 AGENTS.md): backend chỉ gửi CODE.
 * - Event không ack → `socket.emit('error', { event, code })`.
 * - Event có ack → `ack({ error: { code } })`, KHÔNG emit `error`.
 * Nên ở đây ta phải thử đọc `error.code` TRƯỚC khi đọc ack thành công.
 */
class SocketAckMapper @Inject constructor(
    @PreserveCaseJson private val json: Json
) {

    fun toConfigUpdateAck(result: SocketAckResult): Result<ConfigUpdateAck> = when (result) {
        SocketAckResult.NotConnected -> {
            Timber.w("Không gửi được lobby:config-update: chưa có socket")
            Result.Error(AppError.Api(CODE_NOT_CONNECTED))
        }

        SocketAckResult.Timeout -> Result.Error(AppError.Api(CODE_ACK_TIMEOUT))

        is SocketAckResult.Payload -> parseConfigUpdateAck(result.raw)
    }

    private fun parseConfigUpdateAck(raw: String): Result<ConfigUpdateAck> {
        if (raw.isBlank()) {
            Timber.e("Ack lobby:config-update rỗng")
            return parseError()
        }

        // Nhánh lỗi trước: payload thành công không có khoá `error` nên decode ra null,
        // còn payload lỗi thiếu toàn bộ field của ack thành công.
        val errorCode = runCatching {
            json.decodeFromString(SocketAckErrorDto.serializer(), raw)
        }.getOrNull()?.error?.code
        if (errorCode != null) return Result.Error(AppError.Api(errorCode))

        val dto = runCatching {
            json.decodeFromString(ConfigUpdateAckDto.serializer(), raw)
        }.onFailure {
            Timber.e(it, "Parse ack lobby:config-update thất bại: %s", raw)
        }.getOrNull() ?: return parseError()

        // `ok` luôn true khi handler backend không throw. ok=false mà không có `error`
        // là shape ngoài contract — không đoán ý server, báo lỗi chung.
        if (!dto.ok) {
            Timber.e("Ack lobby:config-update trả ok=false mà không có error: %s", raw)
            return parseError()
        }

        return Result.Success(dto.toDomain())
    }

    private fun parseError(): Result<ConfigUpdateAck> =
        Result.Error(AppError.Api(GameEventMapper.CODE_CLIENT_PARSE_ERROR))

    companion object {
        /**
         * Code do CLIENT sinh khi ack không về kịp. Không thuộc vocabulary backend
         * nên để ở tiền tố CLIENT_ cho dễ tách khi đọc log.
         */
        const val CODE_ACK_TIMEOUT = "CLIENT_ACK_TIMEOUT"

        /** Bấm nút trong lúc socket đang rụng / đang reconnect. */
        const val CODE_NOT_CONNECTED = "CLIENT_NOT_CONNECTED"
    }
}
