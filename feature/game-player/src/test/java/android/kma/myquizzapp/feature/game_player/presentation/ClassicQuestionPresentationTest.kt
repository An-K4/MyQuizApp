package android.kma.myquizzapp.feature.game_player.presentation

import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.Pacing
import android.kma.myquizzapp.core.common.model.PublicQuestion
import android.kma.myquizzapp.core.common.model.SessionStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicQuestionPresentationTest {
    private val deadline = Instant.parse("2026-10-20T04:00:30Z").toEpochMilli()
    private fun questionState(type: String = "multiple_choice") = GameUiState(
        connection = GameConnection.CONNECTED, sessionStatus = SessionStatus.ACTIVE,
        mode = GameMode.CLASSIC, pacing = Pacing.HOST, phase = GamePhaseUi.Question,
        isInputLocked = false, timeLimitSeconds = 30,
        endsAt = Instant.ofEpochMilli(deadline).toString(),
        question = PublicQuestion(0, 10, 1, type, "Câu hỏi kiểm thử"),
    )

    @Test
    fun `clock uses server offset and rounds remaining seconds upward`() {
        val time = classicQuestionTime(questionState().copy(serverOffsetMs = 2_000), deadline - 10_001)
        assertEquals(9, time.seconds)
        assertEquals(8_001f / 30_000f, time.fraction!!, 0.0001f)
        assertFalse(time.expired)
    }

    @Test
    fun `critical threshold uses original duration not reconnect time left`() {
        assertFalse(classicQuestionTime(questionState(), deadline - 9_001).critical)
        assertTrue(classicQuestionTime(questionState(), deadline - 9_000).critical)
        assertEquals(0.3f, classicQuestionTime(questionState(), deadline - 9_000).fraction!!, 0.0001f)
    }

    @Test
    fun `expired clock is clamped to zero`() {
        val time = classicQuestionTime(questionState(), deadline + 1)
        assertEquals(0, time.seconds)
        assertEquals(0f, time.fraction!!, 0f)
        assertTrue(time.critical)
        assertTrue(time.expired)
    }

    @Test
    fun `clock fraction cannot exceed full duration`() {
        val time = classicQuestionTime(questionState(), deadline - 60_000)
        assertEquals(60, time.seconds)
        assertEquals(1f, time.fraction!!, 0f)
        assertFalse(time.critical)
    }

    @Test
    fun `untimed question has neither seconds nor progress`() {
        assertEquals(ClassicQuestionTime(), classicQuestionTime(questionState().copy(endsAt = null), deadline))
    }

    @Test
    fun `reconnect without original duration shows seconds only`() {
        val time = classicQuestionTime(questionState().copy(timeLimitSeconds = null, remainingSeconds = 8), deadline - 8_000)
        assertEquals(8, time.seconds)
        assertNull(time.fraction)
        assertFalse(time.critical)
        assertFalse(time.expired)
    }

    @Test
    fun `nonpositive duration cannot produce an invented progress bar`() {
        for (limit in listOf(0, -1)) {
            val time = classicQuestionTime(questionState().copy(timeLimitSeconds = limit), deadline - 8_000)
            assertEquals(8, time.seconds)
            assertNull(time.fraction)
            assertFalse(time.critical)
        }
    }

    @Test
    fun `paused clock freezes at authoritative remaining seconds`() {
        val state = questionState().copy(sessionStatus = SessionStatus.PAUSED, remainingSeconds = 12)
        val time = classicQuestionTime(state, deadline + 60_000)
        assertEquals(12, time.seconds)
        assertEquals(0.4f, time.fraction!!, 0.0001f)
        assertFalse(time.expired)
        assertEquals(time, classicQuestionTime(state, deadline + 120_000))
    }

    @Test
    fun `pause without authoritative remaining time does not use stale deadline`() {
        val state = questionState().copy(sessionStatus = SessionStatus.PAUSED, remainingSeconds = null)
        assertEquals(ClassicQuestionTime(), classicQuestionTime(state, deadline + 60_000))
    }

    @Test
    fun `invalid deadline does not guess a clock from remaining seconds`() {
        val state = questionState().copy(endsAt = "not-an-instant", remainingSeconds = 8)
        assertEquals(ClassicQuestionTime(), classicQuestionTime(state, deadline))
    }

    @Test
    fun `layout is restricted to classic question submitted and locked phases`() {
        for (phase in listOf(GamePhaseUi.Question, GamePhaseUi.Submitted, GamePhaseUi.Locked)) {
            assertTrue(questionState().copy(phase = phase).usesClassicQuestionLayout)
        }
        for (phase in listOf(GamePhaseUi.Connecting, GamePhaseUi.Countdown(null), GamePhaseUi.Results(),
            GamePhaseUi.Eliminated, GamePhaseUi.PlayerFinished, GamePhaseUi.Finished)) {
            assertFalse(questionState().copy(phase = phase).usesClassicQuestionLayout)
        }
        assertFalse(questionState().copy(mode = GameMode.SOLO).usesClassicQuestionLayout)
        assertFalse(questionState().copy(pacing = Pacing.SELF).usesClassicQuestionLayout)
        assertFalse(questionState().copy(question = null).usesClassicQuestionLayout)
    }

    @Test
    fun `footer explains each answer interaction without grading`() {
        assertEquals("Chạm một đáp án để gửi ngay.", classicQuestionStatus(questionState()))
        assertEquals("Chọn các đáp án rồi bấm Gửi đáp án.", classicQuestionStatus(questionState("multiple_select")))
        for (type in listOf("short_answer", "long_answer")) {
            assertEquals("Nhập câu trả lời rồi bấm Gửi đáp án.", classicQuestionStatus(questionState(type)))
        }
        assertEquals("Đã gửi đáp án. Đang chờ kết quả…", classicQuestionStatus(questionState().copy(
            phase = GamePhaseUi.Submitted, isInputLocked = true)))
        assertEquals("Đang xác nhận câu trả lời với máy chủ…", classicQuestionStatus(questionState().copy(
            phase = GamePhaseUi.Submitted, isInputLocked = true, isConfirming = true)))
        assertEquals("Câu hỏi đã đóng. Đang chờ kết quả…", classicQuestionStatus(questionState().copy(
            phase = GamePhaseUi.Locked, isInputLocked = true)))
    }

    @Test
    fun `connection and pause statuses take precedence over submission status`() {
        val submitted = questionState().copy(phase = GamePhaseUi.Submitted, isInputLocked = true)
        assertEquals("Trận đang tạm dừng — chờ Host tiếp tục.", classicQuestionStatus(submitted.copy(
            sessionStatus = SessionStatus.PAUSED)))
        assertEquals("Đang kết nối và đồng bộ câu hỏi…", classicQuestionStatus(submitted.copy(
            sessionStatus = SessionStatus.PAUSED, connection = GameConnection.RECONNECTING)))
        assertEquals("Chưa kết nối lại được. Đáp án đang khóa.", classicQuestionStatus(submitted.copy(
            connection = GameConnection.RECONNECT_FAILED)))
    }
}
