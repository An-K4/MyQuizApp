package android.kma.myquizzapp.feature.quiz_manage.domain.usecase

import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

class GetGameModesUseCase @Inject constructor(
    private val repository: GameSessionRepository
) {
    suspend operator fun invoke(): Result<List<GameModeDescriptor>> = repository.getGameModes()
}
