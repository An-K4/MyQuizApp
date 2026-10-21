package android.kma.myquizzapp.feature.game_host.presentation.hostgame

import android.kma.myquizzapp.core.common.model.AnswerStats
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.ShowLeaderboard
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GamePhase
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionLockReason
import android.kma.myquizzapp.core.common.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostClassicQuestionPresentationTest {
    private fun state() = HostGameUiState(
        mode = GameMode.CLASSIC, pacing = Pacing.HOST, connection = HostGameConnection.CONNECTED,
        sessionStatus = SessionStatus.ACTIVE, phase = GamePhase.QUESTION_ACTIVE,
        autoAdvance = false, index = 3, totalQuestions = 15, isStateSynchronized = true,
        isQuestionClockKnown = true, deadlineEpochMs = 30_000,
        answeredCount = 18, activePlayers = 20, hasAnswerProgress = true,
        question = PublicQuestion(3, 15, 4, "multiple_choice", "Q", answerOptions = listOf(
            PublicAnswerOption("0", "Zero"), PublicAnswerOption("ru", "Russia"))),
    )

    private fun resultsState() = state().copy(phase = GamePhase.SHOWING_RESULTS,
        showCorrectAnswer = true, showLeaderboard = ShowLeaderboard.BETWEEN_QUESTIONS,
        hasAnswerKey = true, isAnswerRevealed = true, correctAnswers = listOf("ru"),
        results = QuestionResults(3, 4, listOf("ru"), AnswerStats(20, mapOf("ru" to 15, "0" to 2))))

    @Test
    fun `round shell includes results and missing question but excludes unrelated phases`() {
        for (phase in listOf(GamePhase.QUESTION_ACTIVE, GamePhase.QUESTION_LOCKED, GamePhase.SHOWING_RESULTS)) {
            assertTrue(state().copy(phase = phase).usesClassicRoundLayout)
            assertTrue(state().copy(phase = phase, question = null).usesClassicRoundLayout)
        }
        for (phase in listOf(GamePhase.COUNTDOWN, GamePhase.FINISHED, GamePhase.UNKNOWN))
            assertFalse(state().copy(phase = phase).usesClassicRoundLayout)
        assertFalse(resultsState().copy(pacing = Pacing.SELF).usesClassicRoundLayout)
        assertFalse(resultsState().copy(sessionStatus = SessionStatus.FINISHED).usesClassicRoundLayout)
    }

    @Test
    fun `distribution uses actual option ids and server option order not map order`() {
        val columns = hostResultColumns(resultsState())!!
        assertEquals(listOf("0", "ru"), columns.map { it.optionId })
        assertEquals(listOf("A", "B"), columns.map { it.label })
        assertEquals(listOf(2, 15), columns.map { it.selections })
        assertEquals(listOf(false, true), columns.map { it.isCorrect })
    }

    @Test
    fun `missing individual option counts are zero but missing full distribution is not fabricated`() {
        val state = resultsState()
        assertEquals(listOf(0, 15), hostResultColumns(state.copy(
            results = QuestionResults(3, 4, stats = AnswerStats(20, mapOf("ru" to 15)))))!!.map { it.selections })
        assertNull(hostResultColumns(state.copy(results = QuestionResults(3, 4, stats = AnswerStats(20)))))
        assertNull(hostResultColumns(state.copy(results = null)))
        assertNull(hostResultColumns(state.copy(showCorrectAnswer = false)))
        assertNull(hostResultColumns(state.copy(showCorrectAnswer = null)))
        assertEquals(listOf(0, 0), hostResultColumns(state.copy(
            results = QuestionResults(3, 4, stats = AnswerStats(0))))!!.map { it.selections })
    }

    @Test
    fun `invalid distribution and stale question results cannot produce columns`() {
        val state = resultsState()
        for (stats in listOf(AnswerStats(-1), AnswerStats(2, mapOf("unknown" to 1)),
            AnswerStats(2, mapOf("ru" to -1)), AnswerStats(2, mapOf("ru" to 3))))
            assertNull(hostResultColumns(state.copy(results = QuestionResults(3, 4, stats = stats))))
        assertNull(hostResultColumns(state.copy(results = QuestionResults(2, 4))))
        assertNull(hostResultColumns(state.copy(results = QuestionResults(3, 999))))
        assertNull(hostResultColumns(state.copy(question = null)))
        assertNull(hostResultColumns(state.copy(phase = GamePhase.QUESTION_ACTIVE)))
    }

    @Test
    fun `multi select counts may overlap without forcing their sum to the number of submissions`() {
        val state = resultsState().let { it.copy(question = it.question?.copy(questionType = "multiple_select"),
            results = QuestionResults(3, 4, stats = AnswerStats(2, mapOf("0" to 2, "ru" to 2)))) }
        assertEquals(listOf(2, 2), hostResultColumns(state)!!.map { it.selections })
    }

    @Test
    fun `backend grouped multi select answers become per option counts in displayed order`() {
        val state = resultsState().let { it.copy(
            question = it.question?.copy(questionType = "multiple_select", answerOptions = listOf(
                PublicAnswerOption("2", "Two"), PublicAnswerOption("0", "Zero"), PublicAnswerOption("1", "One"))),
            results = QuestionResults(3, 4, stats = AnswerStats(5,
                mapOf("0,2" to 2, "2,1" to 1, "1" to 1, "null" to 1)))) }
        assertEquals(listOf("2", "0", "1"), hostResultColumns(state)!!.map { it.optionId })
        assertEquals(listOf(3, 2, 2), hostResultColumns(state)!!.map { it.selections })
    }

    @Test
    fun `timeout buckets never become options and all timeout is a genuine zero distribution`() {
        val state = resultsState().copy(results = QuestionResults(3, 4,
            stats = AnswerStats(2, mapOf("null" to 2))))
        assertEquals(listOf(0, 0), hostResultColumns(state)!!.map { it.selections })
        val multiple = state.let { it.copy(question = it.question?.copy(questionType = "multiple_select"),
            results = QuestionResults(3, 4, stats = AnswerStats(2, mapOf("" to 1, "null" to 1)))) }
        assertEquals(listOf(0, 0), hostResultColumns(multiple)!!.map { it.selections })
    }

    @Test
    fun `duplicate ids within one multi select answer contribute once not twice`() {
        val state = resultsState().let { it.copy(question = it.question?.copy(questionType = "multiple_select"),
            results = QuestionResults(3, 4, stats = AnswerStats(1, mapOf("0,0,ru" to 1)))) }
        assertEquals(listOf(1, 1), hostResultColumns(state)!!.map { it.selections })
    }

    @Test
    fun `unknown ambiguous or overcounted multi select groups remain unavailable`() {
        val state = resultsState().let { it.copy(question = it.question?.copy(questionType = "multiple_select")) }
        for (buckets in listOf(mapOf("0,unknown" to 1), mapOf("0,ru" to 2, "ru" to 1)))
            assertNull(hostResultColumns(state.copy(results = QuestionResults(3, 4, stats = AnswerStats(2, buckets)))))
        val ambiguous = state.let { it.copy(question = it.question?.copy(answerOptions = listOf(
            PublicAnswerOption("0,ru", "Ambiguous"), PublicAnswerOption("0", "Zero"), PublicAnswerOption("ru", "Russia")))) }
        assertNull(hostResultColumns(ambiguous.copy(results = QuestionResults(3, 4,
            stats = AnswerStats(1, mapOf("0,ru" to 1))))))
        // Never split an arbitrary string into several options for a single-choice question.
        assertNull(hostResultColumns(resultsState().copy(results = QuestionResults(3, 4,
            stats = AnswerStats(1, mapOf("0,ru" to 1))))))
    }

    @Test
    fun `short and long answers never get an option distribution even if options are present`() {
        for (type in listOf("short_answer", "long_answer")) {
            val state = resultsState().let { it.copy(question = it.question?.copy(questionType = type)) }
            assertFalse(hostIsChoiceQuestion(state))
            assertNull(hostResultColumns(state))
        }
    }

    @Test
    fun `choice feedback marks only revealed known correct ids and leaves other options neutral`() {
        val state = resultsState()
        assertTrue(hostOptionIsCorrect(state, "ru"))
        assertFalse(hostOptionIsCorrect(state, "0"))
        assertFalse(hostOptionIsCorrect(state.copy(correctAnswers = listOf("unknown")), "unknown"))
        assertFalse(hostOptionIsCorrect(state.copy(isAnswerRevealed = false), "ru"))
        assertFalse(hostOptionIsCorrect(state.copy(hasAnswerKey = false), "ru"))
        // Host-only manual reveal remains possible even when public reveal is disabled.
        assertTrue(hostOptionIsCorrect(state.copy(showCorrectAnswer = false), "ru"))
    }

    @Test
    fun `results leaderboard is enabled only between questions and never for active monitoring`() {
        for (setting in ShowLeaderboard.entries) assertEquals(setting == ShowLeaderboard.BETWEEN_QUESTIONS,
            hostShowsResultsLeaderboard(resultsState().copy(showLeaderboard = setting)))
        assertFalse(hostShowsResultsLeaderboard(resultsState().copy(showLeaderboard = null)))
        assertFalse(hostShowsResultsLeaderboard(resultsState().copy(phase = GamePhase.QUESTION_ACTIVE)))
        assertFalse(hostShowsResultsLeaderboard(resultsState().copy(pacing = Pacing.SELF)))
    }

    @Test
    fun `auto result clock uses authoritative deadline and offset while zero only waits for server`() {
        val state = resultsState().copy(autoAdvance = true, deadlineEpochMs = 30_000, serverOffsetMs = 2_000)
        assertEquals(9, hostQuestionTime(state, 19_999).seconds)
        assertEquals(0, hostQuestionTime(state, 30_001).seconds)
        assertEquals(GamePhase.SHOWING_RESULTS, state.phase)
        assertEquals("Đang chờ máy chủ chuyển câu…", hostQuestionTime(state, 30_001).label)
        assertNull(hostQuestionTime(state.copy(deadlineEpochMs = null), 0).seconds)
        assertNull(hostQuestionTime(state.copy(autoAdvance = false), 0).seconds)
        assertNull(hostQuestionTime(state.copy(sessionStatus = SessionStatus.PAUSED), 0).seconds)
        assertNull(hostQuestionTime(state.copy(connection = HostGameConnection.RECONNECTING), 0).seconds)
    }

    @Test
    fun `manual result next is disabled outside a synchronized manual result phase`() {
        assertTrue(resultsState().canAdvanceFromResults)
        for (state in listOf(resultsState().copy(autoAdvance = true), resultsState().copy(isSendingCommand = true),
            resultsState().copy(sessionStatus = SessionStatus.PAUSED), resultsState().copy(isStateSynchronized = false),
            resultsState().copy(connection = HostGameConnection.RECONNECTING),
            resultsState().copy(phase = GamePhase.QUESTION_ACTIVE))) assertFalse(state.canAdvanceFromResults)
    }

    @Test
    fun `live progress derives pending fraction and percentage from current question pair`() {
        val progress = hostQuestionProgress(state())!!
        assertEquals(2, progress.pending)
        assertEquals(0.9f, progress.fraction, 0.0001f)
        assertEquals(90, progress.percent)
    }

    @Test
    fun `unknown unsynchronized and disconnected progress stays unknown`() {
        assertNull(hostQuestionProgress(state().copy(hasAnswerProgress = false)))
        assertNull(hostQuestionProgress(state().copy(isStateSynchronized = false)))
        assertNull(hostQuestionProgress(state().copy(connection = HostGameConnection.RECONNECTING)))
    }

    @Test
    fun `empty or invalid totals cannot render zero over zero or invalid percentages`() {
        for (pair in listOf(0 to 0, 1 to 0, -1 to 20, 21 to 20, 0 to -1)) {
            assertNull(hostQuestionProgress(state().copy(answeredCount = pair.first, activePlayers = pair.second)))
        }
        assertEquals(0f, hostQuestionProgress(state().copy(answeredCount = 0))!!.fraction, 0f)
        assertEquals(1f, hostQuestionProgress(state().copy(answeredCount = 20))!!.fraction, 0f)
    }

    @Test
    fun `clock uses server offset and upward rounding without changing phase`() {
        val state = state().copy(serverOffsetMs = 2_000)
        val time = hostQuestionTime(state, 19_999)
        assertEquals(9, time.seconds)
        assertEquals("Thời gian còn lại", time.label)
        assertEquals(GamePhase.QUESTION_ACTIVE, state.phase)
    }

    @Test
    fun `zero clock waits for server rather than inventing results`() {
        val time = hostQuestionTime(state(), 30_001)
        assertEquals(0, time.seconds)
        assertEquals("Đang chờ máy chủ đóng câu…", time.label)
    }

    @Test
    fun `paused clock hides stale deadline and never invents remaining seconds`() {
        val time = hostQuestionTime(state().copy(sessionStatus = SessionStatus.PAUSED), 60_000)
        assertNull(time.seconds)
        assertEquals("Thời gian đang tạm dừng", time.label)
    }

    @Test
    fun `untimed clock differs from unknown or disconnected clock`() {
        val untimed = state().copy(deadlineEpochMs = null)
        assertEquals("Không giới hạn thời gian", hostQuestionTime(untimed, 0).label)
        assertEquals("Chờ đồng bộ thời gian", hostQuestionTime(untimed.copy(isQuestionClockKnown = false), 0).label)
        assertEquals("Chờ đồng bộ thời gian", hostQuestionTime(untimed.copy(isStateSynchronized = false), 0).label)
        assertEquals("Chờ đồng bộ thời gian", hostQuestionTime(untimed.copy(connection = HostGameConnection.RECONNECTING), 0).label)
    }

    @Test
    fun `new layout never captures countdown results final or self paced modes`() {
        assertTrue(state().usesClassicQuestionLayout)
        assertTrue(state().copy(phase = GamePhase.QUESTION_LOCKED).usesClassicQuestionLayout)
        for (phase in listOf(GamePhase.COUNTDOWN, GamePhase.SHOWING_RESULTS, GamePhase.FINISHED, GamePhase.UNKNOWN))
            assertFalse(state().copy(phase = phase).usesClassicQuestionLayout)
        assertFalse(state().copy(mode = GameMode.SOLO).usesClassicQuestionLayout)
        assertFalse(state().copy(pacing = Pacing.SELF).usesClassicQuestionLayout)
        assertFalse(state().copy(question = null).usesClassicQuestionLayout)
        assertFalse(state().copy(index = 4).usesClassicQuestionLayout)
        assertFalse(state().copy(sessionStatus = SessionStatus.FINISHED).usesClassicQuestionLayout)
    }

    @Test
    fun `manual close requires exact active synchronized controllable phase`() {
        assertTrue(state().canCloseClassicQuestion)
        for (blocked in listOf(
            state().copy(autoAdvance = true), state().copy(isSendingCommand = true),
            state().copy(sessionStatus = SessionStatus.PAUSED), state().copy(sessionStatus = SessionStatus.LOBBY),
            state().copy(isStateSynchronized = false), state().copy(connection = HostGameConnection.RECONNECTING),
            state().copy(phase = GamePhase.QUESTION_LOCKED), state().copy(phase = GamePhase.SHOWING_RESULTS),
        )) assertFalse(blocked.canCloseClassicQuestion)
    }

    @Test
    fun `private answer labels follow actual server ids including zero`() {
        val open = state().copy(hasAnswerKey = true, isAnswerRevealed = true, correctAnswers = listOf("ru", "0"))
        assertEquals(listOf("B. Russia", "A. Zero"), hostAnswerKeyLines(open))
        assertTrue(hostAnswerKeyLines(open.copy(isAnswerRevealed = false)).isEmpty())
        assertTrue(hostAnswerKeyLines(open.copy(hasAnswerKey = false)).isEmpty())
        assertEquals("A", hostOptionLetter(0))
        assertEquals("Z", hostOptionLetter(25))
        assertEquals("27", hostOptionLetter(26))
    }

    @Test
    fun `private text answer is displayed literally only after explicit reveal`() {
        val open = state().copy(hasAnswerKey = true, isAnswerRevealed = true,
            question = state().question?.copy(questionType = "long_answer", answerOptions = emptyList()),
            correctAnswers = listOf("Liên bang Nga"))
        assertEquals(listOf("Liên bang Nga"), hostAnswerKeyLines(open))
        assertTrue(hostAnswerKeyLines(open.copy(isAnswerRevealed = false)).isEmpty())
    }

    @Test
    fun `manual close backend reason cannot be mislabeled as everyone answered`() {
        val locked = state().copy(phase = GamePhase.QUESTION_LOCKED, lockReason = QuestionLockReason.ALL_ANSWERED)
        assertEquals("Câu hỏi đã đóng. Đang chờ kết quả từ máy chủ…", hostQuestionStatus(locked))
        assertEquals("Đã hết giờ. Đang chờ kết quả từ máy chủ…",
            hostQuestionStatus(locked.copy(lockReason = QuestionLockReason.TIME_UP)))
    }

    @Test
    fun `status distinguishes auto manual pause pending and offline`() {
        assertTrue(hostQuestionStatus(state()).contains("chốt sớm"))
        assertFalse(hostQuestionStatus(state().copy(autoAdvance = true)).contains("chốt sớm"))
        assertTrue(hostQuestionStatus(state().copy(sessionStatus = SessionStatus.PAUSED)).contains("tạm dừng"))
        assertTrue(hostQuestionStatus(state().copy(isSendingCommand = true)).contains("chờ máy chủ xác nhận"))
        assertTrue(hostQuestionStatus(state().copy(connection = HostGameConnection.RECONNECT_FAILED)).contains("đang khóa"))
    }
}
