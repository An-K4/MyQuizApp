package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case tạo quiz mới cùng toàn bộ câu hỏi.
 *
 * Endpoint: POST /v1/quizzes (authRequired). Validate phía client ở
 * CreateQuizViewModel; backend vẫn validate lại lần nữa (createQuizSchema).
 */
class CreateQuizUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(newQuiz: NewQuiz): Result<Quiz> =
        quizRepository.createQuiz(newQuiz)
}
