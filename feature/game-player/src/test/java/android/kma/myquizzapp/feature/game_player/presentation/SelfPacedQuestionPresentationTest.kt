package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.QuestionResults
import android.kma.myquizzapp.core.common.model.SessionStatus
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SelfPacedQuestionPresentationTest {
    private val now = Instant.parse("2026-10-20T04:00:00Z").toEpochMilli()
    private fun state(mode: GameMode = GameMode.SOLO) = GameUiState(
        mode = mode, pacing = Pacing.SELF, connection = GameConnection.CONNECTED,
        sessionStatus = SessionStatus.ACTIVE, phase = GamePhaseUi.Question, isInputLocked = false,
        showCorrectAnswer = true, question = PublicQuestion(0, 3, 1, "multiple_choice", "Câu hỏi",
            answerOptions = listOf(PublicAnswerOption("a", "Một"), PublicAnswerOption("b", "Hai"))),
    )
    private fun feedback(mode: GameMode = GameMode.SOLO) = state(mode).copy(
        phase = GamePhaseUi.Results(), isInputLocked = true, outcome = QuestionOutcome.CORRECT,
        results = QuestionResults(0, correctAnswers = listOf("a")), selectedOptionId = "a",
        acceptedAnswerKeys = listOf("a"), scoreEarned = 950, totalScore = 1000, streak = 3,
    )

    @Test fun `all four modes use self paced layout but Classic and unknown do not`() {
        listOf(GameMode.SOLO, GameMode.SURVIVAL, GameMode.MARATHON, GameMode.PRACTICE).forEach {
            assertTrue(state(it).usesSelfPacedLayout)
        }
        assertFalse(state(GameMode.CLASSIC).usesSelfPacedLayout)
        assertFalse(state().copy(mode = null).usesSelfPacedLayout)
        assertFalse(state().copy(pacing = Pacing.HOST).usesSelfPacedLayout)
    }
    @Test fun `single choice instructs tap to send and has no manual submit button`() {
        assertEquals("Chạm một đáp án để gửi ngay.", selfPacedFooterText(state()))
        assertFalse(state().selfPacedShowsSubmitAction)
        assertFalse(state().copy(phase = GamePhaseUi.Submitted).selfPacedShowsSubmitAction)
    }
    @Test fun `only multi select and text retain a submit action before feedback`() {
        for (type in listOf("multiple_select", "short_answer", "long_answer")) {
            val state = state().let { it.copy(question = it.question!!.copy(questionType = type)) }
            assertTrue(state.selfPacedShowsSubmitAction)
            assertTrue(state.copy(phase = GamePhaseUi.Submitted).selfPacedShowsSubmitAction)
            assertFalse(state.copy(phase = GamePhaseUi.Results()).selfPacedShowsSubmitAction)
        }
    }
    @Test fun `results show accepted choice never an unsent draft`() {
        val pending = feedback().copy(acceptedAnswerKeys = null, selectedOptionId = "b")
        assertTrue(selfPacedSelectedKeys(pending).isEmpty())
        assertEquals(listOf("a"), selfPacedSelectedKeys(pending.copy(acceptedAnswerKeys = listOf("a"))))
        assertEquals(listOf("b"), selfPacedSelectedKeys(state().copy(selectedOptionId = "b")))
    }
    @Test fun `visible matching feedback shows score and positive streak`() {
        assertEquals("+950 điểm", selfPacedScoreDeltaLabel(feedback()))
        assertEquals("Chuỗi 3 câu đúng liên tiếp!", selfPacedStreakLabel(feedback()))
        assertEquals("Chính xác!", selfPacedOutcomeTitle(feedback()))
    }
    @Test fun `hidden feedback hides grading score and streak even if fields are populated`() {
        listOf(false, null).forEach { reveal ->
            val hidden = feedback().copy(showCorrectAnswer = reveal)
            assertFalse(hidden.selfPacedRevealsResult)
            assertNull(selfPacedScoreDeltaLabel(hidden))
            assertNull(selfPacedStreakLabel(hidden))
            assertEquals("Đã ghi nhận kết quả", selfPacedOutcomeTitle(hidden))
        }
    }
    @Test fun `stale index and wrong question identity do not reveal feedback`() {
        assertFalse(feedback().copy(results = QuestionResults(1, correctAnswers = listOf("a"))).selfPacedRevealsResult)
        assertFalse(feedback().copy(results = QuestionResults(0, questionId = 99, correctAnswers = listOf("a"))).selfPacedRevealsResult)
        assertFalse(feedback().copy(question = null).selfPacedRevealsResult)
    }
    @Test fun `restored result without details remains neutral but still allows server approved next`() {
        val restored = feedback().copy(phase = GamePhaseUi.Results(true), autoAdvance = false, canRequestNext = true)
        assertFalse(restored.selfPacedRevealsResult)
        assertNull(selfPacedScoreDeltaLabel(restored))
        assertNull(selfPacedStreakLabel(restored))
        assertTrue(restored.selfPacedCanRequestNext)
    }
    @Test fun `Practice hides score but retains authorized learning streak`() {
        val practice = feedback(GameMode.PRACTICE).copy(scoreEarned = 0, totalScore = 0)
        assertNull(selfPacedScoreDeltaLabel(practice))
        assertEquals("Chuỗi 3 câu đúng liên tiếp!", selfPacedStreakLabel(practice))
        assertEquals("Đã hoàn thành luyện tập", selfPacedCompletionTitle(practice.copy(phase = GamePhaseUi.PlayerFinished)))
    }
    @Test fun `zero and negative score are not prefixed by a plus sign`() {
        assertEquals("0 điểm", selfPacedScoreDeltaLabel(feedback().copy(scoreEarned = 0)))
        assertEquals("-100 điểm", selfPacedScoreDeltaLabel(feedback().copy(scoreEarned = -100)))
    }
    @Test fun `incorrect timeout and zero streak do not celebrate`() {
        assertNull(selfPacedStreakLabel(feedback().copy(streak = 0)))
        val wrong = feedback().copy(outcome = QuestionOutcome.INCORRECT, timedOut = true, streak = 9)
        assertNull(selfPacedStreakLabel(wrong))
        assertEquals("Đã hết thời gian câu này", selfPacedOutcomeTitle(wrong))
    }
    @Test fun `manual next needs server eligibility and an active synchronized connection`() {
        val manual = feedback().copy(autoAdvance = false, canRequestNext = true)
        assertTrue(manual.selfPacedCanRequestNext)
        assertFalse(manual.copy(autoAdvance = true).selfPacedCanRequestNext)
        assertFalse(manual.copy(canRequestNext = false).selfPacedCanRequestNext)
        assertFalse(manual.copy(isRequestingNext = true).selfPacedCanRequestNext)
        assertFalse(manual.copy(connection = GameConnection.RECONNECTING).selfPacedCanRequestNext)
        assertFalse(manual.copy(isSynchronizing = true).selfPacedCanRequestNext)
        assertFalse(manual.copy(sessionStatus = SessionStatus.PAUSED).selfPacedCanRequestNext)
        assertFalse(manual.copy(matchTimeExpired = true).selfPacedCanRequestNext)
    }
    @Test fun `answer actions are disabled on stale paused locked and submitting states`() {
        assertTrue(state().selfPacedCanAnswer)
        assertFalse(state().copy(connection = GameConnection.RECONNECT_FAILED).selfPacedCanAnswer)
        assertFalse(state().copy(isSynchronizing = true).selfPacedCanAnswer)
        assertFalse(state().copy(sessionStatus = SessionStatus.PAUSED).selfPacedCanAnswer)
        assertFalse(state().copy(isInputLocked = true).selfPacedCanAnswer)
        assertFalse(state().copy(isSubmitting = true).selfPacedCanAnswer)
        assertFalse(feedback().selfPacedCanAnswer)
    }
    @Test fun `Marathon counts completed attempts instead of a finite bank percentage`() {
        val marathon = state(GameMode.MARATHON).let { it.copy(question = it.question?.copy(index = 12)) }
        assertEquals("Đã làm 12 câu", selfPacedProgressLabel(marathon))
        assertEquals("Đã làm 13 câu", selfPacedProgressLabel(marathon.copy(acceptedAnswerKeys = listOf("a"))))
        assertEquals("Câu 1/3", selfPacedProgressLabel(state()))
    }
    @Test fun `missing deadlines never produce a countdown or duration`() {
        assertEquals(SelfPacedTime(), selfPacedTime(state(), now))
        assertEquals(SelfPacedTime(), selfPacedTime(state().copy(endsAt = "bad", matchEndsAt = "bad"), now))
    }
    @Test fun `question and personal match clocks are independent and use server offset`() {
        val clocks = state(GameMode.MARATHON).copy(
            endsAt = Instant.ofEpochMilli(now + 10_001).toString(), timeLimitSeconds = 30,
            matchEndsAt = Instant.ofEpochMilli(now + 65_001).toString(), serverOffsetMs = 2_000)
        val time = selfPacedTime(clocks, now)
        assertEquals(9, time.question.seconds)
        assertEquals(64, time.matchSeconds)
        assertFalse(time.matchExpired)
        assertEquals("1:04", selfPacedClockLabel(time.matchSeconds!!))
    }
    @Test fun `match clock continues through feedback but question clock is gone`() {
        val result = feedback(GameMode.MARATHON).copy(endsAt = Instant.ofEpochMilli(now + 10_000).toString(),
            matchEndsAt = Instant.ofEpochMilli(now + 60_000).toString())
        val time = selfPacedTime(result, now)
        assertNull(time.question.seconds)
        assertEquals(60, time.matchSeconds)
    }
    @Test fun `paused disconnected and terminal clocks never fire local expiration`() {
        val live = state(GameMode.MARATHON).copy(endsAt = Instant.ofEpochMilli(now - 1).toString(),
            matchEndsAt = Instant.ofEpochMilli(now - 1).toString())
        assertTrue(selfPacedTime(live, now).matchExpired)
        assertEquals(SelfPacedTime(), selfPacedTime(live.copy(sessionStatus = SessionStatus.PAUSED), now))
        assertEquals(SelfPacedTime(), selfPacedTime(live.copy(connection = GameConnection.RECONNECTING), now))
        assertEquals(SelfPacedTime(), selfPacedTime(live.copy(phase = GamePhaseUi.PlayerFinished), now))
    }
    @Test fun `match budget alone does not imply time expired in completion copy`() {
        val finished = state(GameMode.MARATHON).copy(phase = GamePhaseUi.PlayerFinished,
            matchEndsAt = Instant.ofEpochMilli(now + 60_000).toString())
        assertEquals("Bạn đã hoàn thành lượt chơi", selfPacedCompletionTitle(finished))
        assertEquals("Đã hết thời gian", selfPacedCompletionTitle(finished.copy(matchTimeExpired = true)))
        assertEquals("Bạn đã hết mạng", selfPacedCompletionTitle(finished.copy(phase = GamePhaseUi.Eliminated)))
    }
}
