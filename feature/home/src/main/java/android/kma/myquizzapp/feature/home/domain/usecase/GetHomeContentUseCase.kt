package android.kma.myquizzapp.feature.home.domain.usecase

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case to fetch home content (sections of quiz cards).
 * 
 * Returns different sections based on authentication:
 * - Guest: "Phổ biến", "Mới nhất"
 * - Authenticated: "Đề xuất cho bạn", "Phổ biến", "Mới nhất", "Bạn đã tham gia"
 * 
 * Backend automatically determines sections based on session cookie.
 */
class GetHomeContentUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    /**
     * Fetch home sections.
     * 
     * @return Success with list of HomeSection, or Error with AppError
     */
    suspend operator fun invoke(): Result<List<HomeSection>> {
        return quizRepository.getHomeContent()
    }
}
