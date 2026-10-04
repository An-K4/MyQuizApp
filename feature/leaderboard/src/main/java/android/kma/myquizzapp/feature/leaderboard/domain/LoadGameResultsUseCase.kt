package android.kma.myquizzapp.feature.leaderboard.domain

import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import javax.inject.Inject

/** Khôi phục kết quả từ REST khi payload socket tạm thời đã mất. */
class LoadGameResultsUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(gameId: Long) = repository.getGameResults(gameId)
}
