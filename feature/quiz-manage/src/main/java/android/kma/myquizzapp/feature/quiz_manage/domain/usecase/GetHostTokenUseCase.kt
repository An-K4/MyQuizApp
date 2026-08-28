package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

class GetHostTokenUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(gameId: Long): Result<String> = repository.getHostToken(gameId)
}
