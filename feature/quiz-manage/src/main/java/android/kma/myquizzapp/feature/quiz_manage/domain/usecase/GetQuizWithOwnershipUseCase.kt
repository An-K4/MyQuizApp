package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.feature.quiz_manage.domain.model.QuizWithOwnership
import javax.inject.Inject

/**
 * UseCase lấy quiz detail kèm ownership status.
 *
 * Orchestrates:
 * 1. Fetch quiz từ QuizRepository
 * 2. Check ownership bằng cách so user.id với quiz.quizOwner
 * 3. Return QuizWithOwnership object
 *
 * Benefits:
 * - Presentation layer không cần inject AuthRepository trực tiếp
 * - Ownership check logic tập trung ở Domain layer
 * - Reusable cho các contexts khác
 *
 * Note: Guest user (401 error) → isOwner = false, vẫn xem được quiz public.
 */
class GetQuizWithOwnershipUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val authRepository: AuthRepository
) {
    /**
     * @param quizId ID của quiz cần lấy
     * @return Result<QuizWithOwnership> chứa quiz và isOwner flag
     */
    suspend operator fun invoke(quizId: Long): Result<QuizWithOwnership> {
        // 1. Fetch quiz - early return nếu lỗi
        val quiz = when (val result = quizRepository.getQuizDetail(quizId)) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error(result.error)
        }

        // 2. Check ownership - fallback false nếu getCurrentUser lỗi (guest/401)
        val isOwner = when (val userResult = authRepository.getCurrentUser()) {
            is Result.Success -> userResult.data.id == quiz.quizOwner
            is Result.Error -> false
        }

        // 3. Return combined result
        return Result.Success(QuizWithOwnership(quiz, isOwner))
    }
}
