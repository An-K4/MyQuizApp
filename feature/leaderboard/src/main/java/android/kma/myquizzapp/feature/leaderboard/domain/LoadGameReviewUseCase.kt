package android.kma.myquizzapp.feature.leaderboard.domain

import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import javax.inject.Inject

/** Tải answer sheet của chính Player sau khi trận đã kết thúc. */
class LoadGameReviewUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(gameId: Long, socketToken: String) =
        repository.getGameReview(gameId, socketToken)
}
