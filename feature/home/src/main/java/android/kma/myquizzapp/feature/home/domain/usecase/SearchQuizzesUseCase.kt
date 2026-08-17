package android.kma.myquizzapp.feature.home.domain.usecase

import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case to search public quizzes with pagination.
 *
 * Searches in public quizzes only (is_public = true).
 * Applies keyword search on quiz title.
 *
 * @param query Search keyword (applied to quiz title)
 * @param page Page number (1-indexed)
 * @param limit Items per page (default 20)
 * 
 * @return Success with list of QuizCard (may be empty if no results),
 *         or Error with AppError
 */
class SearchQuizzesUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<QuizCard>> {
        // Trim query to avoid unnecessary API calls
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            // Return empty list for blank query
            return Result.Success(emptyList())
        }
        
        return quizRepository.searchQuizzes(trimmedQuery)
    }
}
