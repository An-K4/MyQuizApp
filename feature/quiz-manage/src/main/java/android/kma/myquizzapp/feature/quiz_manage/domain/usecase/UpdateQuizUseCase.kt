package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizPatch
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case cập nhật quiz (N16).
 *
 * Endpoint: PATCH /v1/quizzes/id/:quizId (authRequired, chỉ owner — backend trả
 * 404 QUIZ_NOT_FOUND cho non-owner, cố ý không lộ 403). Metadata patch từng field;
 * `patch.questions` != null sẽ THAY THẾ toàn bộ danh sách câu hỏi
 * (replaceQuizQuestions — xem quiz.service.ts). Validate phía client ở
 * EditQuizViewModel; backend vẫn validate lại lần nữa (updateQuizSchema).
 */
class UpdateQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(quizId: Long, patch: QuizPatch): Result<Quiz> =
        quizRepository.updateQuiz(quizId, patch)
}
