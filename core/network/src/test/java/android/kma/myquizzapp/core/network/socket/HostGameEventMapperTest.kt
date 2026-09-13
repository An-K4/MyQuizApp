package android.kma.myquizzapp.core.network.socket

import android.kma.myquizzapp.core.common.model.GameEvent
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.SessionStatus
import android.kma.myquizzapp.core.network.di.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test các nhánh gameplay của HOST trong [GameEventMapper] (N21).
 *
 * Fixture copy theo schema thật ở `docs/socket.channels.ts` và
 * `docs/components/socket.doc.ts`, không phải theo trí nhớ: naming trộn
 * snake_case/camelCase chính là chỗ đã gây lỗi ở N16/N17.
 *
 * Dùng ĐÚNG Json của production (NetworkModule.providePreserveCaseJson) — tự tạo
 * Json trong test sẽ cho test xanh trong khi app thật vỡ.
 */
class HostGameEventMapperTest {

    private val mapper = GameEventMapper(NetworkModule.providePreserveCaseJson())

    @Test
    fun `host question doc duoc dap an va giu thu tu lua chon`() {
        val payload = """
            {
              "question": {
                "index": 2,
                "total": 10,
                "id": 77,
                "question_type": "multiple_choice",
                "question_text": "Thủ đô của Việt Nam?",
                "question_image": null,
                "question_hint": null,
                "answer_options": [
                  { "id": 0, "text": "Hà Nội" },
                  { "id": 1, "text": "Huế" },
                  { "id": 2, "text": "Đà Nẵng" }
                ]
              },
              "correct_answer": [0],
              "time_limit": 20,
              "endsAt": "2026-09-10T14:00:20.000Z",
              "total_questions": 10,
              "serverTime": "2026-09-10T14:00:00.000Z"
            }
        """.trimIndent()

        val event = mapper.map(GameSocketEvents.HOST_QUESTION, payload)

        assertTrue(event is GameEvent.HostQuestionReceived)
        val hostQuestion = (event as GameEvent.HostQuestionReceived).hostQuestion
        assertEquals(2, hostQuestion.question.index)
        assertEquals(10, hostQuestion.totalQuestions)
        assertEquals(20, hostQuestion.timeLimitSeconds)
        assertEquals("2026-09-10T14:00:20.000Z", hostQuestion.endsAt)
        // id lựa chọn có thể là 0: quy về chuỗi "0", không được coi là rỗng.
        assertEquals(listOf("0"), hostQuestion.correctAnswers)
        assertEquals(listOf("0", "1", "2"), hostQuestion.question.answerOptions.map { it.id })
        assertEquals("Hà Nội", hostQuestion.question.answerOptions.first().text)
    }

    @Test
    fun `host question chap nhan dap an dang chuoi tu luan`() {
        val payload = """
            {
              "question": {
                "index": 0, "total": 1, "id": 5,
                "question_type": "short_answer",
                "question_text": "2 + 2 = ?"
              },
              "correct_answer": "4",
              "total_questions": 1
            }
        """.trimIndent()

        val hostQuestion =
            (mapper.map(GameSocketEvents.HOST_QUESTION, payload) as GameEvent.HostQuestionReceived)
                .hostQuestion

        assertEquals(listOf("4"), hostQuestion.correctAnswers)
        // Không có answer_options vẫn phải parse được, không được ném.
        assertTrue(hostQuestion.question.answerOptions.isEmpty())
        assertNull(hostQuestion.timeLimitSeconds)
    }

    @Test
    fun `question results tach total khoi phan bo theo option`() {
        val payload = """
            {
              "index": 3,
              "question_id": 88,
              "correct_answer": [1],
              "stats": { "total": 5, "0": 2, "1": 3 },
              "nextQuestionAt": "2026-09-10T14:01:00.000Z",
              "serverTime": "2026-09-10T14:00:58.000Z"
            }
        """.trimIndent()

        val results =
            (mapper.map(GameSocketEvents.QUESTION_RESULTS, payload) as GameEvent.QuestionResultsReceived)
                .results

        assertEquals(3, results.index)
        assertEquals(88L, results.questionId)
        assertEquals(listOf("1"), results.correctAnswers)
        assertEquals(5, results.stats.total)
        assertEquals(mapOf("0" to 2, "1" to 3), results.stats.distribution)
        assertEquals("2026-09-10T14:01:00.000Z", results.nextQuestionAt)
    }

    /**
     * Khi `flow.showCorrectAnswer = false`, backend bóp cả đáp án và phân bố — kể cả
     * với host. Phân bố rỗng KHÔNG được hiểu là "không ai trả lời".
     */
    @Test
    fun `question results khong tiet lo dap an van parse duoc`() {
        val payload = """
            {
              "index": 0,
              "question_id": null,
              "correct_answer": null,
              "stats": { "total": 4 },
              "nextQuestionAt": null
            }
        """.trimIndent()

        val results =
            (mapper.map(GameSocketEvents.QUESTION_RESULTS, payload) as GameEvent.QuestionResultsReceived)
                .results

        assertTrue(results.correctAnswers.isEmpty())
        assertTrue(results.stats.distribution.isEmpty())
        assertEquals(4, results.stats.total)
        assertNull(results.questionId)
        assertNull(results.nextQuestionAt)
    }

    @Test
    fun `question locked doc dung ly do dong cau`() {
        val timeUp = mapper.map(
            GameSocketEvents.QUESTION_LOCKED,
            """{"index":1,"reason":"time_up"}"""
        ) as GameEvent.QuestionLocked
        val allAnswered = mapper.map(
            GameSocketEvents.QUESTION_LOCKED,
            """{"index":1,"reason":"all_answered"}"""
        ) as GameEvent.QuestionLocked
        val future = mapper.map(
            GameSocketEvents.QUESTION_LOCKED,
            """{"index":1,"reason":"ly_do_moi_cua_backend"}"""
        ) as GameEvent.QuestionLocked

        assertEquals(QuestionLockReason.TIME_UP, timeUp.reason)
        assertEquals(QuestionLockReason.ALL_ANSWERED, allAnswered.reason)
        // Lý do lạ không được làm sập luồng đang giữ trận.
        assertEquals(QuestionLockReason.UNKNOWN, future.reason)
    }

    @Test
    fun `host answer received mang ten nguoi choi va dung sai`() {
        val payload = """
            {
              "index": 2,
              "answered": 4,
              "activePlayers": 7,
              "player": { "id": 31, "player_name": "Kiro" },
              "is_correct": true,
              "serverTime": "2026-09-10T14:00:10.000Z"
            }
        """.trimIndent()

        val answer =
            (mapper.map(GameSocketEvents.HOST_ANSWER_RECEIVED, payload) as GameEvent.HostAnswerReceivedEvent)
                .answer

        assertEquals(2, answer.index)
        assertEquals(4, answer.answered)
        assertEquals(7, answer.activePlayers)
        assertEquals(31L, answer.playerId)
        assertEquals("Kiro", answer.playerName)
        assertTrue(answer.isCorrect)
    }

    @Test
    fun `leaderboard host doc du bo dem tung nguoi choi`() {
        val payload = """
            {
              "leaderboard": [
                {
                  "rank": 1, "id": 31, "player_name": "Kiro", "player_score": 2400,
                  "answered_count": 3, "correct_count": 3, "wrong_count": 0,
                  "unanswered_count": 7, "total_questions": 10,
                  "current_question_index": 3, "streak": 3, "lives": null,
                  "status": "connected"
                }
              ],
              "total_questions": 10,
              "answered_total": 12,
              "serverTime": "2026-09-10T14:00:30.000Z"
            }
        """.trimIndent()

        val board =
            (mapper.map(GameSocketEvents.LEADERBOARD_HOST, payload) as GameEvent.HostLeaderboardUpdated)
                .leaderboard

        assertEquals(10, board.totalQuestions)
        assertEquals(12, board.answeredTotal)
        assertEquals(1, board.rows.size)
        val row = board.rows.first()
        assertEquals("Kiro", row.playerName)
        assertEquals(2400, row.playerScore)
        assertEquals(3, row.correctCount)
        assertEquals(7, row.unansweredCount)
        assertEquals(3, row.streak)
        assertNull(row.lives)
    }

    /**
     * Snapshot dùng bản `publicQuestion`: KHÔNG có `correct_answer`. Đây chính là bẫy
     * làm host mất khoá đáp án khi reconnect giữa một câu đang mở.
     */
    @Test
    fun `game state dung lai duoc man hinh sau reconnect`() {
        val payload = """
            {
              "session_status": "active",
              "current_phase": "question_active",
              "mode": "classic",
              "config": { "version": 1, "timing": { "autoAdvance": false } },
              "index": 4,
              "total_questions": 10,
              "question": {
                "index": 4, "total": 10, "id": 91,
                "question_type": "multiple_choice",
                "question_text": "Câu đang mở"
              },
              "countdown": null,
              "endsAt": "2026-09-10T14:02:00.000Z",
              "remainingSeconds": 12,
              "leaderboard": [
                { "rank": 1, "id": 31, "player_name": "Kiro", "player_score": 900 }
              ],
              "serverTime": "2026-09-10T14:01:48.000Z"
            }
        """.trimIndent()

        val snapshot =
            (mapper.map(GameSocketEvents.GAME_STATE, payload) as GameEvent.StateSnapshot).snapshot

        assertEquals(SessionStatus.ACTIVE, snapshot.sessionStatus)
        assertEquals(GamePhase.QUESTION_ACTIVE, snapshot.phase)
        assertEquals(4, snapshot.index)
        assertEquals(12, snapshot.remainingSeconds)
        assertEquals("Câu đang mở", snapshot.question?.questionText)
        assertEquals(false, snapshot.config?.timing?.autoAdvance)
        assertEquals(1, snapshot.leaderboard.size)
        assertNull(snapshot.countdownStartsAt)
    }

    @Test
    fun `game countdown giu moc tuyet doi`() {
        val event = mapper.map(
            GameSocketEvents.GAME_COUNTDOWN,
            """{"seconds":3,"startsAt":"2026-09-10T14:00:03.000Z","serverTime":"2026-09-10T14:00:00.000Z"}"""
        ) as GameEvent.Countdown

        assertEquals(3, event.countdown.seconds)
        assertEquals("2026-09-10T14:00:03.000Z", event.countdown.startsAt)
        assertEquals("2026-09-10T14:00:00.000Z", event.serverTime)
    }

    @Test
    fun `game ended mang san bang xep hang va thong ke tung cau`() {
        val payload = """
            {
              "leaderboard": [
                { "rank": 1, "id": 31, "player_name": "Kiro", "player_score": 3200 },
                { "rank": 2, "id": 32, "player_name": "An", "player_score": 2100 }
              ],
              "perQuestion": [
                { "question_id": 91, "question_index": 0, "answer_count": 2, "correct_count": 1 }
              ],
              "review_enabled": true,
              "serverTime": "2026-09-10T14:10:00.000Z"
            }
        """.trimIndent()

        val ended = (mapper.map(GameSocketEvents.GAME_ENDED, payload) as GameEvent.GameEndedEvent).ended

        assertEquals(2, ended.leaderboard.size)
        assertEquals("Kiro", ended.leaderboard.first().playerName)
        assertEquals(1, ended.perQuestion.size)
        assertEquals(91L, ended.perQuestion.first().questionId)
        assertTrue(ended.reviewEnabled)
    }

    /**
     * Host NẰM TRONG room chung nên vẫn nhận `question:started` (bản đã cắt đáp án).
     * Mapper phải để nó rơi vào Unhandled, nếu không bản không có đáp án sẽ ghi đè
     * bản `host:question` đến trước đó và host mất khoá đáp án.
     */
    @Test
    fun `question started khong duoc map thanh event rieng`() {
        val event = mapper.map(
            GameSocketEvents.QUESTION_STARTED,
            """{"question":{"index":0,"total":1,"id":1,"question_type":"multiple_choice","question_text":"X"}}"""
        )

        assertEquals(GameEvent.Unhandled(GameSocketEvents.QUESTION_STARTED), event)
    }

    @Test
    fun `payload rac thanh client parse error chu khong throw`() {
        assertEquals(
            GameEvent.Failed(
                GameSocketEvents.HOST_QUESTION,
                GameEventMapper.CODE_CLIENT_PARSE_ERROR
            ),
            mapper.map(GameSocketEvents.HOST_QUESTION, "{ question: thiếu ngoặc }")
        )
    }

    @Test
    fun `payload rong thanh client parse error`() {
        assertEquals(
            GameEvent.Failed(
                GameSocketEvents.LEADERBOARD_HOST,
                GameEventMapper.CODE_CLIENT_PARSE_ERROR
            ),
            mapper.map(GameSocketEvents.LEADERBOARD_HOST, null)
        )
    }
}
