package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.result.Result

/** Shared REST contract dùng bởi room setup, lobby và lịch sử trận. */
interface GameSessionRepository {
    suspend fun getGameModes(): Result<List<GameModeDescriptor>>
    suspend fun createGameSession(params: CreateGameSessionParams): Result<CreateGameSessionResult>
    suspend fun getHostToken(gameId: Long): Result<String>
}
