package android.kma.myquizzapp.feature.game_player.domain

import android.kma.myquizzapp.core.common.model.PlayerAnswer
import android.kma.myquizzapp.core.common.repository.PlayerGameSocketRepository
import javax.inject.Inject

/** Boundary duy nhất giữa GameViewModel và socket repository. */
class PlayerGameSessionUseCase @Inject constructor(
    private val repository: PlayerGameSocketRepository
) {
    fun events(socketToken: String) = repository.events(socketToken)
    suspend fun joinAndSync() {
        repository.joinLobby()
        repository.sync()
    }
    suspend fun submit(answer: PlayerAnswer) = repository.submitAnswer(answer)
    suspend fun sync() = repository.sync()
    suspend fun disconnect() = repository.disconnect()
}
