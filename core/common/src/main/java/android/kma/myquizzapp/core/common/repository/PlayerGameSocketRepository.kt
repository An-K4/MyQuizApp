package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.AnswerAck
import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.result.Result

/** Các lệnh chỉ player được gửi trên namespace `/game`. */
interface PlayerGameSocketRepository : GameSocketRepository {
    /** `lobby:leave` — chỉ dùng khi còn ở phòng chờ. */
    suspend fun leaveLobby()

    /**
     * `question:answer` có ACK. Domain truyền đáp án typed; JSON chỉ được tạo ở
     * core:network. Timeout không chứng minh server chưa ghi đáp án, vì vậy caller
     * phải giữ input khóa rồi `sync()` thay vì tự gửi lại.
     */
    suspend fun submitAnswer(answer: PlayerAnswer): Result<AnswerAck>

    /** `question:next` — self-paced, ngoài phạm vi N22–N23. */
    suspend fun requestNextQuestion()

    /** `player:sync` — xin snapshot mới sau reconnect/resume/ACK không chắc chắn. */
    suspend fun sync()
}
