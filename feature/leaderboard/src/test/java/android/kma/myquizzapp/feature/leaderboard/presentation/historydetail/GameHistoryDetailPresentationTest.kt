package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameHistoryViewer
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameHistoryDetailPresentationTest {
    private val options = listOf(
        PublicAnswerOption("0", "Alpha"), PublicAnswerOption("1", "Beta"), PublicAnswerOption("2", "Gamma")
    )
    private fun item() = GameReviewItem(
        questionIndex = 0, answered = true, isCorrect = false, isLate = false, scoreEarned = 0
    )

    @Test
    fun `single correct shows only the relevant answer including zero id`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("0"), correctAnswers = listOf("0"), isCorrect = true).toHistoryReviewUi()
        assertEquals(HistoryAnswerTone.Correct, ui.tone)
        assertEquals(1, ui.answers.size)
        assertEquals("A", ui.answers.single().optionLabel)
        assertEquals(HistoryAnswerTone.Correct, ui.answers.single().tone)
    }

    @Test
    fun `single wrong keeps snapshot labels and excludes unselected distractors`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("1"), correctAnswers = listOf("2")).toHistoryReviewUi()
        assertEquals(listOf("B", "C"), ui.answers.map { it.optionLabel })
        assertEquals(listOf(HistoryAnswerTone.Incorrect, HistoryAnswerTone.Correct), ui.answers.map { it.tone })
    }

    @Test
    fun `incomplete multiple set does not recolor a correct selected option`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("0"), correctAnswers = listOf("0", "1")).toHistoryReviewUi()
        assertEquals(HistoryAnswerTone.Incorrect, ui.tone)
        assertEquals(listOf("A", "B"), ui.answers.map { it.optionLabel })
        assertTrue(ui.answers.all { it.tone == HistoryAnswerTone.Correct })
        assertEquals("Bạn đã chọn", ui.answers[0].caption)
        assertEquals("Đáp án đúng", ui.answers[1].caption)
    }

    @Test
    fun `multiple choice colors only chosen wrong options red`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("0", "2"), correctAnswers = listOf("0", "1")).toHistoryReviewUi()
        assertEquals(listOf(HistoryAnswerTone.Correct, HistoryAnswerTone.Correct, HistoryAnswerTone.Incorrect), ui.answers.map { it.tone })
    }

    @Test
    fun `accepted text trusts server result instead of comparing text locally`() {
        val ui = item().copy(yourAnswers = listOf("  HÀ NỘI  "), correctAnswers = listOf("Hanoi"), isCorrect = true, scoreEarned = 900).toHistoryReviewUi()
        assertEquals(HistoryAnswerTone.Correct, ui.tone)
        assertEquals(1, ui.answers.size)
        assertEquals("  HÀ NỘI  ", ui.answers.single().text)
        assertEquals(HistoryAnswerTone.Correct, ui.answers.single().tone)
        assertEquals("Bạn trả lời đúng (+900đ)", ui.statusText)
    }

    @Test
    fun `wrong long text has separate submitted and correct blocks without truncation`() {
        val answer = "Dòng thứ nhất\nDòng thứ hai"
        val key = "Nội dung đáp án dài\nGiữ nguyên xuống dòng"
        val ui = item().copy(yourAnswers = listOf(answer), correctAnswers = listOf(key)).toHistoryReviewUi()
        assertEquals(listOf(answer, key), ui.answers.map { it.text })
        assertEquals(listOf(HistoryAnswerTone.Incorrect, HistoryAnswerTone.Correct), ui.answers.map { it.tone })
    }

    @Test
    fun `unanswered text has no fake red submitted block`() {
        val ui = item().copy(answered = false, yourAnswers = listOf("stale"), correctAnswers = listOf("Key")).toHistoryReviewUi()
        assertEquals(HistoryAnswerTone.Neutral, ui.tone)
        assertEquals(1, ui.answers.size)
        assertEquals("Key", ui.answers.single().text)
        assertEquals(HistoryAnswerTone.Correct, ui.answers.single().tone)
    }

    @Test
    fun `unanswered choices ignore stale selected keys`() {
        val ui = item().copy(answered = false, answerOptions = options, yourAnswers = listOf("1"), correctAnswers = listOf("0")).toHistoryReviewUi()
        assertEquals(listOf("A"), ui.answers.map { it.optionLabel })
        assertEquals("Đáp án đúng", ui.answers.single().caption)
    }

    @Test
    fun `missing answer key does not guess option correctness`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("1")).toHistoryReviewUi()
        assertEquals(HistoryAnswerTone.Neutral, ui.answers.single().tone)
        assertTrue(ui.notice.orEmpty().isNotBlank())
    }

    @Test
    fun `unknown option ids do not become invented answer text`() {
        val ui = item().copy(answerOptions = options, yourAnswers = listOf("missing-id"), correctAnswers = listOf("0")).toHistoryReviewUi()
        assertEquals(1, ui.answers.size)
        assertFalse(ui.answers.any { it.text.contains("missing-id") })
        assertTrue(ui.notice.orEmpty().isNotBlank())
    }

    @Test
    fun `missing accepted text has a notice rather than a fabricated answer`() {
        val ui = item().copy(isCorrect = true).toHistoryReviewUi()
        assertTrue(ui.answers.isEmpty())
        assertTrue(ui.notice.orEmpty().isNotBlank())
    }

    @Test
    fun `host accuracy uses submitted answers as denominator`() {
        val ui = historyAccuracy(59, 100)
        assertEquals("59/100 trả lời đúng – 59%", ui.text)
        assertEquals(0.59f, ui.progress!!, 0.0001f)
        assertEquals("1/3 trả lời đúng – 33%", historyAccuracy(1, 3).text)
    }

    @Test
    fun `no answers is not displayed as zero percent incorrect`() {
        val ui = historyAccuracy(0, 0)
        assertEquals("Chưa có lượt trả lời", ui.text)
        assertNull(ui.progress)
    }

    @Test
    fun `invalid aggregate counts are not drawn as valid progress`() {
        for ((correct, answers) in listOf(1 to 0, -1 to 2, 3 to 2, 0 to -1)) {
            assertNull(historyAccuracy(correct, answers).progress)
        }
        assertEquals(0f, historyAccuracy(0, 5).progress!!, 0f)
        assertEquals(1f, historyAccuracy(5, 5).progress!!, 0f)
    }

    @Test
    fun `room code replaces room name with nullable and blank fallback`() {
        val summary = GameHistorySummary(
            sessionId = 91, sessionName = "Friday room", sessionCode = " AB7CDE ",
            gameMode = GameMode.CLASSIC, sessionStatus = SessionStatus.FINISHED,
            config = GameConfig(), totalPlayers = 2, totalQuestions = 10, viewer = GameHistoryViewer(true)
        )
        assertEquals("Mã phòng: AB7CDE", summary.historyRoomLabel())
        assertEquals("Phòng: Friday room", summary.copy(sessionCode = null).historyRoomLabel())
        assertEquals("Phòng: Friday room", summary.copy(sessionCode = " ").historyRoomLabel())
        assertNull(summary.copy(sessionCode = null, sessionName = " ").historyRoomLabel())
        assertEquals(91L, summary.sessionId)
    }

    @Test
    fun `only cancelled sessions display a status badge`() {
        for (status in SessionStatus.entries) {
            if (status == SessionStatus.CANCELLED) assertEquals("Đã hủy", status.historyStatusLabel())
            else assertNull(status.historyStatusLabel())
        }
    }

    @Test
    fun `negative points keep their sign and missing date is omitted`() {
        val ui = item().copy(scoreEarned = -250).toHistoryReviewUi()
        assertEquals("Bạn trả lời sai (-250đ)", ui.statusText)
        assertEquals("8,500", formatHistoryNumber(8500))
        assertNull(formatHistoryTime(null))
        assertNull(formatHistoryTime(" "))
        assertEquals("unparsed", formatHistoryTime("unparsed"))
    }
}
