package android.kma.myquizzapp.feature.quiz_preview.domain.usecase

import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/** Local preview loading only: no room, submission, result mutation or edit navigation. */
class LoadQuizPreviewUseCase @Inject constructor(
    private val quizRepository: QuizRepository,
) {
    internal suspend operator fun invoke(quizId: Long): Result<Quiz> =
        quizRepository.getQuizDetail(quizId)
}
