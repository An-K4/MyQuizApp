package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.network.di.PreserveCaseJson
import android.kma.myquizzapp.core.network.socket.dto.GameCountdownDto
import android.kma.myquizzapp.core.network.socket.dto.GameEndedDto
import android.kma.myquizzapp.core.network.socket.dto.GameStartedDto
import android.kma.myquizzapp.core.network.socket.dto.GameStateDto
import android.kma.myquizzapp.core.network.socket.dto.HostAnswerReceivedDto
import android.kma.myquizzapp.core.network.socket.dto.HostLeaderboardDto
import android.kma.myquizzapp.core.network.socket.dto.HostQuestionDto
import android.kma.myquizzapp.core.network.socket.dto.LobbyUpdatedDto
import android.kma.myquizzapp.core.network.socket.dto.PlayerEliminatedDto
import android.kma.myquizzapp.core.network.socket.dto.QuestionLockedDto
import android.kma.myquizzapp.core.network.socket.dto.QuestionResultsDto
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
 * Flow đang giữ phòng. Lỗi parse trả về GameEvent.Failed để UI có cái hiển thị.
 *
 * N21 bổ sung phần gameplay của host. Một lưu ý về phạm vi: `question:started`
 * KHÔNG được map thành event riêng. Host vẫn nhận nó (ở trong room chung) nhưng
 * đó là bản đã cắt `correct_answer`; nếu map và xử lý thì bản này sẽ ghi đè bản
 * `host:question` có đáp án. Để nó rơi vào [GameEvent.Unhandled] là có ý thức.
 */
class GameEventMapper @Inject constructor(
    @PreserveCaseJson private val json: Json
) {

    fun map(event: String, payload: Any?): GameEvent = when (event) {
        GameSocketEvents.LOBBY_UPDATED -> mapLobbyUpdated(payload)
        GameSocketEvents.ERROR -> mapError(payload)
        GameSocketEvents.GAME_STARTED -> mapGameStarted(payload)
        GameSocketEvents.GAME_COUNTDOWN -> mapCountdown(payload)
        GameSocketEvents.GAME_STATE -> mapState(payload)
        GameSocketEvents.GAME_ENDED -> mapGameEnded(payload)
        GameSocketEvents.HOST_QUESTION -> mapHostQuestion(payload)
        GameSocketEvents.QUESTION_LOCKED -> mapQuestionLocked(payload)
        GameSocketEvents.QUESTION_RESULTS -> mapQuestionResults(payload)
        GameSocketEvents.HOST_ANSWER_RECEIVED -> mapHostAnswerReceived(payload)
        GameSocketEvents.LEADERBOARD_HOST -> mapHostLeaderboard(payload)
        GameSocketEvents.PLAYER_ELIMINATED -> mapPlayerEliminated(payload)
        else -> GameEvent.Unhandled(event)
    }

    private fun mapLobbyUpdated(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.LOBBY_UPDATED) {
            json.decodeFromString(LobbyUpdatedDto.serializer(), it)
        }
        return dto?.let { GameEvent.LobbyUpdated(it.toDomain()) }
            ?: GameEvent.Failed(GameSocketEvents.LOBBY_UPDATED, CODE_CLIENT_PARSE_ERROR)
    }

    /**
     * `game:started` — xác nhận duy nhất rằng `game:start` đã thành công (event đó
     * không có ack). Host neo việc điều hướng vào đây nên parse lỗi phải thành
     * [GameEvent.Failed] để nút "Bắt đầu" thoát trạng thái chờ, không được im lặng.
     */
    private fun mapGameStarted(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.GAME_STARTED) {
            json.decodeFromString(GameStartedDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.GameStarted(
                mode = it.mode,
                config = it.config,
                totalQuestions = it.totalQuestions,
                serverTime = it.serverTime
            )
        } ?: GameEvent.Failed(GameSocketEvents.GAME_STARTED, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapCountdown(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.GAME_COUNTDOWN) {
            json.decodeFromString(GameCountdownDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.Countdown(countdown = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.GAME_COUNTDOWN, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapState(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.GAME_STATE) {
            json.decodeFromString(GameStateDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.StateSnapshot(snapshot = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.GAME_STATE, CODE_CLIENT_PARSE_ERROR)
    }

    /**
     * `host:question` — nuốt lỗi parse ở đây là tệ nhất trong cả nhóm: host sẽ ngồi
     * trước một màn hình trống trong khi người chơi đã thấy câu hỏi và đang trả lời.
     */
    private fun mapHostQuestion(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.HOST_QUESTION) {
            json.decodeFromString(HostQuestionDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.HostQuestionReceived(hostQuestion = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.HOST_QUESTION, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapQuestionLocked(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.QUESTION_LOCKED) {
            json.decodeFromString(QuestionLockedDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.QuestionLocked(
                index = it.index,
                reason = QuestionLockReason.fromRaw(it.reason),
                serverTime = it.serverTime
            )
        } ?: GameEvent.Failed(GameSocketEvents.QUESTION_LOCKED, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapQuestionResults(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.QUESTION_RESULTS) {
            json.decodeFromString(QuestionResultsDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.QuestionResultsReceived(results = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.QUESTION_RESULTS, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapHostAnswerReceived(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.HOST_ANSWER_RECEIVED) {
            json.decodeFromString(HostAnswerReceivedDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.HostAnswerReceivedEvent(answer = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.HOST_ANSWER_RECEIVED, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapHostLeaderboard(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.LEADERBOARD_HOST) {
            json.decodeFromString(HostLeaderboardDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.HostLeaderboardUpdated(leaderboard = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.LEADERBOARD_HOST, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapGameEnded(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.GAME_ENDED) {
            json.decodeFromString(GameEndedDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.GameEndedEvent(ended = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.GAME_ENDED, CODE_CLIENT_PARSE_ERROR)
    }

    private fun mapPlayerEliminated(payload: Any?): GameEvent {
        val dto = decode(payload, GameSocketEvents.PLAYER_ELIMINATED) {
            json.decodeFromString(PlayerEliminatedDto.serializer(), it)
        }
        return dto?.let {
            GameEvent.PlayerEliminated(player = it.toDomain(), serverTime = it.serverTime)
        } ?: GameEvent.Failed(GameSocketEvents.PLAYER_ELIMINATED, CODE_CLIENT_PARSE_ERROR)
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
