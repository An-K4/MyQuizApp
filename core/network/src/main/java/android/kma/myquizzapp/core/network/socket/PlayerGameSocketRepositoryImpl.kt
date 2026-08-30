package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hiện thực kênh socket cho vai PLAYER.
 *
 * Giống host, giữ [GameSocketClient] riêng nên hai vai không dùng chung kết nối.
 *
 * Phạm vi N18 chỉ cần connect + join + leave (màn player lobby làm ở N19), nhưng
 * các emit không payload đã hiện thực luôn vì đúng contract và vô hại.
 */
@Singleton
class PlayerGameSocketRepositoryImpl @Inject constructor(
    private val client: GameSocketClient
) : PlayerGameSocketRepository {

    override fun events(socketToken: String): Flow<GameEvent> = client.events(socketToken)

    override suspend fun joinLobby() = client.emit(GameSocketEvents.LOBBY_JOIN)

    override suspend fun disconnect() = client.disconnect()

    override suspend fun leaveLobby() = client.emit(GameSocketEvents.LOBBY_LEAVE)

    override suspend fun requestNextQuestion() = client.emit(GameSocketEvents.QUESTION_NEXT)

    override suspend fun sync() = client.emit(GameSocketEvents.PLAYER_SYNC)

    /**
     * Gửi đáp án.
     *
     * N18 gửi đi nhưng CHƯA đọc ack. Theo socket.doc.ts, `question:answer` là event
     * duy nhất có ack callback trả về `{ error: { code } }` hoặc kết quả tính điểm,
     * nên khi làm màn chơi (N21+) phải đổi sang dạng suspend có ack (Ack callback
     * của socket.io bọc bằng suspendCancellableCoroutine) để biết đáp án có được
     * nhận hay bị từ chối vì hết thời gian. Đừng dùng hàm này cho gameplay trước
     * khi đã xử lý ack.
     *
     * @param rawAnswerJson JSON đã serialize sẵn ở tầng trên (hình dạng payload phụ
     *   thuộc loại câu hỏi nên không áp một DTO chung ở đây).
     */
    override suspend fun submitAnswer(rawAnswerJson: String) {
        client.emit(GameSocketEvents.QUESTION_ANSWER, JSONObject(rawAnswerJson))
    }
}
