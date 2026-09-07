package android.kma.myquizzapp.core.network.socket

/**
 * Kết quả thô của một lần emit CÓ ack trên socket.
 *
 * Tách riêng khỏi việc parse để [SocketAckMapper] trở thành hàm thuần: test được
 * cả 4 nhánh (thành công / lỗi có code / hết hạn chờ / chưa có kết nối) mà không
 * cần dựng socket thật.
 *
 * [Payload] giữ nguyên JSON dạng String vì payload ack đến từ org.json của
 * socket.io-client; việc decode do kotlinx.serialization đảm nhiệm, giống
 * [GameEventMapper].
 */
sealed interface SocketAckResult {

    /** Server đã ack. [raw] là JSON thô, có thể là ack thành công hoặc `{ error: { code } }`. */
    data class Payload(val raw: String) : SocketAckResult

    /** Không có ack nào về trong thời gian chờ — coi như thất bại, không treo UI vô hạn. */
    data object Timeout : SocketAckResult

    /** Chưa/không còn socket (mạng vừa rụng, người dùng bấm khi đang reconnect). */
    data object NotConnected : SocketAckResult
}
