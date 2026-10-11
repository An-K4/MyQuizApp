package android.kma.myquizzapp.feature.quiz_preview.domain.usecase

import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

internal data class LoadedQuizPreview(val quiz: Quiz, val isOwner: Boolean)

/** Local preview orchestration only: no room, submission or result mutation. */
class LoadQuizPreviewUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
    private val sessionRepository: SessionRepository,
) {
    internal suspend operator fun invoke(quizId: Long): Result<LoadedQuizPreview> =
        when (val result = quizRepository.getQuizDetail(quizId)) {
            is Result.Success -> Result.Success(LoadedQuizPreview(result.data, isOwner(result.data)))
            is Result.Error -> Result.Error(result.error)
        }

    internal fun isOwner(quiz: Quiz): Boolean =
        sessionRepository.state.value.userOrNull?.id == quiz.quizOwner
}
