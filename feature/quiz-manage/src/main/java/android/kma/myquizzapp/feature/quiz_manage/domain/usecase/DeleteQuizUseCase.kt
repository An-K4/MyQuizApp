package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case xóa quiz (N16) — HARD DELETE, KHÔNG có undo: backend xóa hẳn row và
 * cascade xóa questions, quiz_snapshots, game_sessions, player_sessions
 * (quiz.repository.ts) — tức mất luôn lịch sử mọi phòng đã chơi quiz này.
 * UI phải xác nhận rõ với người dùng trước khi gọi (xem dialog ở QuizDetailScreen).
 *
 * Endpoint: DELETE /v1/quizzes/id/:quizId (authRequired, chỉ owner).
 * QuizRepositoryImpl tự xóa quiz khỏi Room cache sau khi xóa thành công.
 */
class DeleteQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(quizId: Long): Result<Unit> =
        quizRepository.deleteQuiz(quizId)
}
