package android.kma.myquizzapp.feature.home.domain.usecase

import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case to search public quizzes — cursor pagination (N16.5).
 *
 * Backend: GET /quizzes/search — keyset cursor (opaque base64url, sort mặc định
 * "new", listing.service.ts). cursor null = trang đầu; nextCursor/hasMore đọc từ
 * Result.Success.page (PageInfo). Đổi query phải reset cursor về null — cursor gắn
 * fingerprint của filter, dùng sai → QUIZ_CURSOR_INVALID (listing.cursor.ts).
 *
 * @param query Search keyword (applied to quiz title)
 * @param cursor Cursor của trang trước (null = trang đầu)
 * @param limit Items per page (default 20, backend cho 1-24)
 */
class SearchQuizzesUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    suspend operator fun invoke(
        query: String,
        cursor: String? = null,
        limit: Int = 20
    ): Result<List<QuizCard>> {
        // Trim query to avoid unnecessary API calls
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            // Return empty list for blank query
            return Result.Success(emptyList())
        }

        return quizRepository.searchQuizzes(trimmedQuery, cursor, limit)
    }
}
