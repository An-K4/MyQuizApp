package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case lấy chi tiết 1 quiz theo id.
 * 
 * Endpoint: GET /v1/quizzes/id/:quizId (optionalAuth).
 * Cache-aside qua Room được xử lý ở QuizRepositoryImpl (core:network).
 */
class GetQuizDetailUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    /**
     * @return Success với Quiz detail, hoặc Error với AppError
     */
    suspend operator fun invoke(quizId: Long): Result<Quiz> {
        return quizRepository.getQuizDetail(quizId)
    }
}
