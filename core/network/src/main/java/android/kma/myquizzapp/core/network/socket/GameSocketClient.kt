package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.DisconnectReason
import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.network.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Bọc socket.io-client cho namespace `/game` thành một Flow.
 *
 * KHÔNG đánh @Singleton: mỗi chỗ inject nhận một instance riêng, nhờ vậy
 * HostGameSocketRepositoryImpl và PlayerGameSocketRepositoryImpl không dùng chung
 * một kết nối. Điều này quan trọng vì token socket của host và của player khác
 * nhau (role khác nhau trong JWT) và một thiết bị hoàn toàn có thể vừa host vừa
 * chơi khi test.
 *
 * Vòng đời: socket được tạo khi [events] bắt đầu được collect và bị hủy trong
 * awaitClose khi collector dừng (ViewModel clear / screen rời khỏi backstack). Không
 * có socket nào tồn tại ngoài vòng đời của Flow.
 */
class GameSocketClient @Inject constructor(
    private val mapper: GameEventMapper
) {

    /**
     * Socket đang sống của lần collect hiện tại, dùng cho [emit].
     *
     * Dùng AtomicReference vì callback của socket.io chạy trên thread riêng của nó,
     * còn [emit] được gọi từ coroutine của ViewModel.
     */
    private val socketRef = AtomicReference<Socket?>(null)

    /**
     * Mở kết nối và phát event. Cold flow — mỗi lần collect là một kết nối mới.
     *
     * Lưu ý: flow này KHÔNG tự emit `lobby:join`. Việc join là quyết định của tầng
     * trên (sau khi nhận [GameEvent.Connected]), vì khi reconnect ta cần join lại
     * nhưng có thể với token đã được làm mới.
     *
     * @param socketToken JWT lấy từ REST (host: GET /games/{id}/host-token). Gửi qua
     *   `auth.token` của handshake — đúng chỗ socket.middleware.ts đọc.
     */
    fun events(socketToken: String): Flow<GameEvent> = callbackFlow {
        val options = IO.Options().apply {
            // Chỉ WebSocket: bỏ qua bước polling upgrade cho nhanh và tránh rắc rối
            // sticky-session nếu sau này backend chạy nhiều instance.
            transports = arrayOf(WebSocket.NAME)
            auth = mapOf(AUTH_TOKEN_KEY to socketToken)
            // forceNew: không dùng lại Manager đang cache theo URL, nếu không thì host
            // và player trên cùng máy sẽ vô tình dùng chung kết nối và token đầu tiên.
            forceNew = true
            reconnection = true
            reconnectionAttempts = RECONNECT_ATTEMPTS
            reconnectionDelay = RECONNECT_DELAY_MS
        }

        val socket = IO.socket(BuildConfig.SOCKET_URL + GameSocketEvents.NAMESPACE, options)
        socketRef.set(socket)

        socket.on(Socket.EVENT_CONNECT) {
            Timber.d("Socket /game connected")
            trySend(GameEvent.Connected)
        }

        socket.on(Socket.EVENT_DISCONNECT) { args ->
            val raw = args.firstOrNull() as? String
            Timber.w("Socket /game disconnected: %s", raw)
            trySend(GameEvent.Disconnected(raw.toDisconnectReason()))
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val code = args.firstOrNull().toHandshakeErrorCode()
            Timber.e("Socket /game connect_error: %s", code)
            trySend(GameEvent.Failed(event = Socket.EVENT_CONNECT_ERROR, code = code))
        }

        GameSocketEvents.SERVER_EVENTS.forEach { name ->
            socket.on(name) { args ->
                trySend(mapper.map(name, args.firstOrNull()))
            }
        }

        socket.connect()

        awaitClose {
            Timber.d("Dóng socket /game")
            socket.off()
            socket.disconnect()
            socketRef.compareAndSet(socket, null)
        }
    }

    /**
     * Gửi một event lên server. Bỏ qua có log nếu chưa có kết nối — không throw, vì
     * nhấn nút khi mạng vừa rụng là chuyện bình thường, không phải lỗi lập trình.
     */
    fun emit(event: String, payload: JSONObject? = null) {
        val socket = socketRef.get()
        if (socket == null) {
            Timber.w("Bỏ qua emit %s: chưa có socket", event)
            return
        }
        if (payload == null) socket.emit(event) else socket.emit(event, payload)
    }

    /** Ngắt kết nối chủ động (rời lobby) mà không cần hủy Flow. */
    fun disconnect() {
        socketRef.get()?.disconnect()
    }

    private fun String?.toDisconnectReason(): DisconnectReason = when (this) {
        // Server chủ động đá (token sai, phòng đóng...): socket.io sẽ KHÔNG tự
        // reconnect. Tầng trên phải điều hướng ra ngoài thay vì chờ vô vọng.
        REASON_SERVER_DISCONNECT -> DisconnectReason.SERVER_DISCONNECT
        REASON_CLIENT_DISCONNECT -> DisconnectReason.CLIENT
        else -> DisconnectReason.TRANSPORT
    }

    /**
     * Lấy error code từ lần handshake thất bại.
     *
     * socket.middleware.ts gọi `next(new Error(CODE))` nên thông điệp thường chính
     * là error code (VD GAME_TOKEN_INVALID). Nhưng client Java có thể đưa về
     * exception của tầng transport (mất mạng, DNS...) — khi đó chuỗi không phải
     * code, ta trả code riêng của client để không giả mạo vocabulary backend.
     */
    private fun Any?.toHandshakeErrorCode(): String {
        val text = when (this) {
            null -> null
            is String -> this
            is Throwable -> message
            else -> toString()
        }?.trim()
        return if (!text.isNullOrEmpty() && text.matches(ERROR_CODE_PATTERN)) {
            text
        } else {
            GameEventMapper.CODE_CONNECT_FAILED
        }
    }

    private companion object {
        const val AUTH_TOKEN_KEY = "token"
        const val RECONNECT_ATTEMPTS = 5
        const val RECONNECT_DELAY_MS = 1_000L

        /** Chuỗi reason do socket.io đặt ra, không phải do backend. */
        const val REASON_SERVER_DISCONNECT = "io server disconnect"
        const val REASON_CLIENT_DISCONNECT = "io client disconnect"

        /** Error code của dự án luôn là SCREAMING_SNAKE_CASE. */
        val ERROR_CODE_PATTERN = Regex("^[A-Z][A-Z0-9_]*$")
    }
}
