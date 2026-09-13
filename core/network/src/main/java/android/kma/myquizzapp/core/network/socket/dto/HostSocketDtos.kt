package android.kma.myquizzapp.core.network.socket.dto

import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.EliminatedPlayer
import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameCountdown
import android.kma.myquizzapp.core.common.model.GameEnded
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.GameSnapshot
import android.kma.myquizzapp.core.common.model.HostAnswerReceived
import android.kma.myquizzapp.core.common.model.HostLeaderboard
import android.kma.myquizzapp.core.common.model.HostLeaderboardRow
import android.kma.myquizzapp.core.common.model.HostQuestion
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.common.model.SessionStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * DTO cho payload gameplay của HOST trên namespace `/game` (N21).
 *
 * Ba điểm phải nhắt quán với [SocketDtos.kt]:
 *
 * 1. Naming trộn: field nghiệp vụ là snake_case (`question_text`, `total_questions`,
 *    `is_correct`) nhưng mốc thời gian là camelCase (`endsAt`, `nextQuestionAt`,
 *    `serverTime`). Phải decode bằng @PreserveCaseJson và khai báo @SerialName tỉ
 *    mỉ — dùng Json mặc định thì namingStrategy đổi cả tên đã override.
 *
 * 2. Mọi field đều có default. Payload thật thay đổi theo mode và theo phase (VD
 *    `question:results` không kèm `correct_answer` khi phòng ẩn đáp án), thiếu field
 *    là chuyện bình thường chứ không phải lỗi.
 *
 * 3. `correct_answer` và id lựa chọn KHÔNG có kiểu cứng ở backend: có thể là số,
 *    chuỗi, hay mảng. Nhận bằng [JsonElement] rồi quy về List<String> — khai
 *    Int hay String ở đây là tự tạo lỗi parse cho loại câu hỏi tự luận.
 */

/** Quy mọi kiểu nguyên thủy về chuỗi. `id` có thể là 0 nên không được coi 0 là rỗng. */
private fun JsonElement?.toOptionKey(): String = when {
    this == null || this is JsonNull -> ""
    this is JsonPrimitive -> content
    else -> toString()
}

/**
 * Đọc `correct_answer` ở cả ba dạng: một id, mảng id, hoặc chuỗi tự luận.
 *
 * Trả danh sách rỗng khi backend giữ kín đáp án (`null`) — trạng thái "chưa có đáp
 * án" và "đáp án là chuỗi rỗng" đều vô nghĩa với UI, nên gộp một.
 */
internal fun JsonElement?.toAnswerKeys(): List<String> = when {
    this == null || this is JsonNull -> emptyList()
    this is JsonArray -> mapNotNull { item ->
        item.toOptionKey().takeIf { it.isNotBlank() }
    }
    else -> listOfNotNull(toOptionKey().takeIf { it.isNotBlank() })
}

/**
 * Đọc `stats` của `question:results`.
 *
 * Schema là object có `total` cộng thêm các khóa động theo id lựa chọn
 * (additionalProperties), nên không thể khai báo data class cứng.
 *
 * Lưu ý nghiệp vụ: khi `flow.showCorrectAnswer = false`, backend chỉ gửi `total`
 * — kể cả cho host. Phân bố rỗng KHÔNG có nghĩa là không ai trả lời.
 */
internal fun JsonObject?.toAnswerStats(): AnswerStats {
    if (this == null) return AnswerStats()
    val total = (this[KEY_TOTAL] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
    val distribution = entries.mapNotNull { (key, value) ->
        if (key == KEY_TOTAL) return@mapNotNull null
        val count = (value as? JsonPrimitive)?.content?.toIntOrNull() ?: return@mapNotNull null
        key to count
    }.toMap()
    return AnswerStats(total = total, distribution = distribution)
}

private const val KEY_TOTAL = "total"

/**
 * Đọc MỘT lựa chọn trả lời từ payload thô.
 *
 * KHÔNG khai @Serializable data class cho việc này, vì `answer_options` không có
 * một hình dạng duy nhất:
 *
 * 1. Dạng thật sự chảy qua socket là `{ id, option_text }` — đó là shape backend
 *    lưu trong snapshot (`game.type.ts`, `quiz.repository.ts`). Tài liệu
 *    `socket.doc.ts` ghi `{ id, text, image }` nhưng `game.socket.ts` forward
 *    thẳng `q.answer_options` chứ không hề map lại, nên tài liệu đang sai.
 *    Đọc mỗi `text` sẽ ra null cho MỌI lựa chọn — đúng bug đã thấy trên máy thật.
 * 2. Quiz đời cũ còn lưu lựa chọn là CHUỖI THUẦN (`game.doc.ts` nói rõ "either
 *    [{ id, option_text }] rows or plain strings"). Một data class sẽ ném lỗi
 *    parse ở đây và làm rơi trọn sự kiện `host:question`.
 *
 * Khi lựa chọn là chuỗi thuần thì không có id: dùng VỊ TRÍ làm id, khớp cách
 * backend sinh id (`options.map((option, index) => ({ id: index, ... }))`).
 *
 * Không đọc ảnh cho lựa chọn: CSDL chỉ lưu `{ id, option_text }` nên lựa chọn
 * bằng ảnh không tồn tại ở bất kỳ tầng nào (đã chốt bỏ hẳn hướng này).
 */
private fun JsonElement.toAnswerOption(position: Int): PublicAnswerOption {
    if (this is JsonObject) {
        val id = this[KEY_ID]
        return PublicAnswerOption(
            id = if (id == null || id is JsonNull) position.toString() else id.toOptionKey(),
            text = (this[KEY_OPTION_TEXT] ?: this[KEY_TEXT]).toOptionText()
        )
    }
    return PublicAnswerOption(id = position.toString(), text = toOptionKey())
}

/** Chuỗi rỗng và null đều coi là "không có", để UI khỏi phải phân biệt hai thứ như nhau. */
private fun JsonElement?.toOptionText(): String? = when {
    this == null || this is JsonNull -> null
    this is JsonPrimitive -> content.takeIf { it.isNotBlank() }
    else -> null
}

private const val KEY_ID = "id"
private const val KEY_OPTION_TEXT = "option_text"
private const val KEY_TEXT = "text"

@Serializable
data class PublicQuestionDto(
    val index: Int = 0,
    val total: Int = 0,
    val id: Long = 0L,
    @SerialName("question_type") val questionType: String = "",
    @SerialName("question_text") val questionText: String = "",
    @SerialName("question_image") val questionImage: String? = null,
    @SerialName("question_hint") val questionHint: String? = null,
    @SerialName("answer_options") val answerOptions: List<JsonElement>? = null
) {
    fun toDomain() = PublicQuestion(
        index = index,
        total = total,
        id = id,
        questionType = questionType,
        questionText = questionText,
        questionImage = questionImage,
        questionHint = questionHint,
        // Giữ nguyên thứ tự server gửi: việc trộn lựa chọn đã làm ở server.
        answerOptions = answerOptions.orEmpty().mapIndexed { position, raw ->
            raw.toAnswerOption(position)
        }
    )
}

/** `host:question` — bản có đáp án, chỉ phát vào host room. */
@Serializable
data class HostQuestionDto(
    val question: PublicQuestionDto,
    @SerialName("correct_answer") val correctAnswer: JsonElement? = null,
    @SerialName("time_limit") val timeLimit: Int? = null,
    val endsAt: String? = null,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val serverTime: String? = null
) {
    fun toDomain() = HostQuestion(
        question = question.toDomain(),
        correctAnswers = correctAnswer.toAnswerKeys(),
        timeLimitSeconds = timeLimit,
        endsAt = endsAt,
        totalQuestions = totalQuestions
    )
}

@Serializable
data class GameCountdownDto(
    val seconds: Int = 0,
    val startsAt: String? = null,
    val serverTime: String? = null
) {
    fun toDomain() = GameCountdown(seconds = seconds, startsAt = startsAt)
}

@Serializable
data class QuestionLockedDto(
    val index: Int = 0,
    val reason: String? = null,
    val serverTime: String? = null
)

@Serializable
data class QuestionResultsDto(
    val index: Int = 0,
    @SerialName("question_id") val questionId: Long? = null,
    @SerialName("correct_answer") val correctAnswer: JsonElement? = null,
    val stats: JsonObject? = null,
    val nextQuestionAt: String? = null,
    val serverTime: String? = null
) {
    fun toDomain() = QuestionResults(
        index = index,
        questionId = questionId,
        correctAnswers = correctAnswer.toAnswerKeys(),
        stats = stats.toAnswerStats(),
        nextQuestionAt = nextQuestionAt
    )
}

@Serializable
data class AnsweringPlayerDto(
    val id: Long = 0L,
    @SerialName("player_name") val playerName: String = ""
)

/** `host:answer-received` — có tên người chơi và đúng/sai, khác `answer:received`. */
@Serializable
data class HostAnswerReceivedDto(
    val index: Int = 0,
    val answered: Int = 0,
    val activePlayers: Int = 0,
    val player: AnsweringPlayerDto = AnsweringPlayerDto(),
    @SerialName("is_correct") val isCorrect: Boolean = false,
    val serverTime: String? = null
) {
    fun toDomain() = HostAnswerReceived(
        index = index,
        answered = answered,
        activePlayers = activePlayers,
        playerId = player.id,
        playerName = player.playerName,
        isCorrect = isCorrect
    )
}

@Serializable
data class HostLeaderboardRowDto(
    val rank: Int = 0,
    val id: Long = 0L,
    @SerialName("player_name") val playerName: String = "",
    @SerialName("player_score") val playerScore: Int = 0,
    @SerialName("answered_count") val answeredCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    @SerialName("unanswered_count") val unansweredCount: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("current_question_index") val currentQuestionIndex: Int = 0,
    val streak: Int = 0,
    val lives: Int? = null,
    val status: String = "connected"
) {
    fun toDomain() = HostLeaderboardRow(
        rank = rank,
        id = id,
        playerName = playerName,
        playerScore = playerScore,
        answeredCount = answeredCount,
        correctCount = correctCount,
        wrongCount = wrongCount,
        unansweredCount = unansweredCount,
        totalQuestions = totalQuestions,
        currentQuestionIndex = currentQuestionIndex,
        streak = streak,
        lives = lives,
        status = status
    )
}

@Serializable
data class HostLeaderboardDto(
    val leaderboard: List<HostLeaderboardRowDto> = emptyList(),
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("answered_total") val answeredTotal: Int = 0,
    val serverTime: String? = null
) {
    fun toDomain() = HostLeaderboard(
        rows = leaderboard.map { it.toDomain() },
        totalQuestions = totalQuestions,
        answeredTotal = answeredTotal
    )
}

@Serializable
data class LeaderboardRowDto(
    val rank: Int = 0,
    val id: Long = 0L,
    @SerialName("player_name") val playerName: String = "",
    @SerialName("player_score") val playerScore: Int = 0
) {
    fun toDomain() = LeaderboardRow(
        rank = rank,
        id = id,
        playerName = playerName,
        playerScore = playerScore
    )
}

@Serializable
data class QuestionStatDto(
    @SerialName("question_id") val questionId: Long = 0L,
    @SerialName("question_index") val questionIndex: Int = 0,
    @SerialName("answer_count") val answerCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0
) {
    fun toDomain() = QuestionStat(
        questionId = questionId,
        questionIndex = questionIndex,
        answerCount = answerCount,
        correctCount = correctCount
    )
}

@Serializable
data class CountdownStateDto(val startsAt: String? = null)

/**
 * `game:state` — snapshot dụng để dựng lại màn hình sau reconnect.
 *
 * `question` ở đây là bản công khai: KHÔNG mang `correct_answer`, kể cả với host.
 */
@Serializable
data class GameStateDto(
    @SerialName("session_status") val sessionStatus: SessionStatus? = null,
    @SerialName("current_phase") val currentPhase: String? = null,
    val mode: GameMode? = null,
    val config: GameConfig? = null,
    val index: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val question: PublicQuestionDto? = null,
    val countdown: CountdownStateDto? = null,
    val endsAt: String? = null,
    val remainingSeconds: Int? = null,
    val leaderboard: List<LeaderboardRowDto> = emptyList(),
    val serverTime: String? = null
) {
    fun toDomain() = GameSnapshot(
        sessionStatus = sessionStatus,
        phase = GamePhase.fromRaw(currentPhase),
        mode = mode,
        config = config,
        index = index,
        totalQuestions = totalQuestions,
        question = question?.toDomain(),
        countdownStartsAt = countdown?.startsAt,
        endsAt = endsAt,
        remainingSeconds = remainingSeconds,
        leaderboard = leaderboard.map { it.toDomain() }
    )
}

@Serializable
data class GameEndedDto(
    val leaderboard: List<LeaderboardRowDto> = emptyList(),
    val perQuestion: List<QuestionStatDto> = emptyList(),
    @SerialName("review_enabled") val reviewEnabled: Boolean = false,
    val serverTime: String? = null
) {
    fun toDomain() = GameEnded(
        leaderboard = leaderboard.map { it.toDomain() },
        perQuestion = perQuestion.map { it.toDomain() },
        reviewEnabled = reviewEnabled
    )
}

@Serializable
data class PlayerEliminatedDto(
    val id: Long = 0L,
    @SerialName("player_name") val playerName: String = "",
    val serverTime: String? = null
) {
    fun toDomain() = EliminatedPlayer(id = id, playerName = playerName)
}
