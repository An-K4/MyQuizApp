package android.kma.myquizzapp.feature.leaderboard.presentation.historydetail

import android.kma.myquizzapp.core.common.model.GameConfig
import android.kma.myquizzapp.core.common.model.GameHistoryQuiz
import android.kma.myquizzapp.core.common.model.GameHistorySummary
import android.kma.myquizzapp.core.common.model.GameHistoryViewer
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameReview
import android.kma.myquizzapp.core.common.model.GameReviewItem
import android.kma.myquizzapp.core.common.model.LeaderboardRow
import android.kma.myquizzapp.core.common.model.PublicAnswerOption
import android.kma.myquizzapp.core.common.model.QuestionStat
import android.kma.myquizzapp.core.common.model.SessionStatus

/** Sample data for Content/component previews only; no ViewModel or network dependency. */
internal object HistoryDetailPreviewData {
    private val standings = listOf(
        LeaderboardRow(rank = 1, id = 8, playerName = "Minh Anh", playerScore = 9500, correctAnswersCount = 14, streak = 5),
        LeaderboardRow(rank = 2, id = 7, playerName = "Kiro", playerScore = 8500, correctAnswersCount = 12, streak = 3),
        LeaderboardRow(rank = 3, id = 9, playerName = "Hoàng Long", playerScore = 7600, correctAnswersCount = 11)
    )
    val items = listOf(
        GameReviewItem(
            questionIndex = 0, questionId = 101,
            questionText = "Chiến thắng Điện Biên Phủ diễn ra vào năm nào?",
            questionImage = "https://preview.invalid/history-question.png",
            answerOptions = listOf(PublicAnswerOption("0", "1954"), PublicAnswerOption("1", "1945")),
            answered = true, yourAnswers = listOf("0"), correctAnswers = listOf("0"),
            isCorrect = true, isLate = false, scoreEarned = 1000, timeTakenSeconds = 3.42
        ),
        GameReviewItem(
            questionIndex = 1, questionId = 102, questionText = "Chọn các số nguyên tố.",
            answerOptions = listOf(PublicAnswerOption("2", "2"), PublicAnswerOption("3", "3"), PublicAnswerOption("4", "4")),
            answered = true, yourAnswers = listOf("2", "4"), correctAnswers = listOf("2", "3"),
            isCorrect = false, isLate = false, scoreEarned = 0,
            explanation = "2 và 3 chỉ có hai ước dương là 1 và chính nó."
        ),
        GameReviewItem(
            questionIndex = 2, questionId = 103, questionText = "Thủ đô của Việt Nam là gì?",
            answered = true, yourAnswers = listOf("Hà Nội"), correctAnswers = listOf("Hà Nội"),
            isCorrect = true, isLate = true, scoreEarned = 900, timeTakenSeconds = 32.5
        ),
        GameReviewItem(
            questionIndex = 3, questionId = 104, questionText = "Giải thích ngắn gọn vì sao cần bảo vệ môi trường.",
            answered = true, yourAnswers = listOf("Giữ cảnh quan sạch đẹp.\nĐây là phần trả lời còn thiếu ý."),
            correctAnswers = listOf("Bảo vệ môi trường giúp giữ gìn hệ sinh thái, sức khỏe con người và nguồn tài nguyên cho các thế hệ sau."),
            isCorrect = false, isLate = false, scoreEarned = 0,
            explanation = "Cần đề cập cả sức khỏe, hệ sinh thái và tính bền vững."
        ),
        GameReviewItem(
            questionIndex = 4, questionId = 105, questionText = "Câu hỏi chưa được trả lời",
            answered = false, correctAnswers = listOf("Một đáp án tham khảo"),
            isCorrect = false, isLate = false, scoreEarned = 0
        )
    )

    fun summary(host: Boolean = false) = GameHistorySummary(
        sessionId = 91, sessionName = "Phòng lịch sử tối thứ sáu", sessionCode = "AB7CDE",
        gameMode = GameMode.CLASSIC, sessionStatus = SessionStatus.FINISHED, config = GameConfig(),
        totalPlayers = 15, totalQuestions = 15, finishedAt = "2026-10-10T07:30:00Z", hostName = "Minh Anh",
        quiz = GameHistoryQuiz(
            id = 5, name = "Đố vui Lịch sử Việt Nam", description = "Cùng ôn lại những dấu mốc lịch sử đáng nhớ.",
            image = "https://preview.invalid/history-cover.png"
        ),
        leaderboard = standings,
        perQuestion = if (host) listOf(QuestionStat(101, 0, 15, 12), QuestionStat(102, 1, 15, 9), QuestionStat(103, 2, 0, 0)) else emptyList(),
        viewer = GameHistoryViewer(isHost = host, playerId = if (host) null else 7),
        viewerResult = if (host) null else standings[1]
    )

    fun state(host: Boolean = false) = GameHistoryDetailUiState(
        gameId = 91, summary = summary(host), isSummaryLoading = false,
        review = if (host) null else GameReview(8500, 12, 15, 15, items)
    )
}
