package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

class CreateGameSessionUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(params: CreateGameSessionParams): Result<CreateGameSessionResult> =
        repository.createGameSession(params)
}
