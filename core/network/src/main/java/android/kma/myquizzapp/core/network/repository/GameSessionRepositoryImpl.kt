package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.CreateGameSessionResult
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.JoinRoomResult
import android.kma.myquizzapp.core.common.model.RoomLookup
import android.kma.myquizzapp.core.common.repository.GameSessionRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.GameApiService
import android.kma.myquizzapp.core.network.dto.JoinGameRequestDto
import android.kma.myquizzapp.core.network.dto.toRequestDto
import javax.inject.Inject

class GameSessionRepositoryImpl @Inject constructor(
    private val gameApi: GameApiService
) : GameSessionRepository {

    override suspend fun getGameModes(): Result<List<GameModeDescriptor>> =
        gameApi.getGameModes().map { response -> response.gameModes.map { it.toDomain() } }

    override suspend fun createGameSession(
        params: CreateGameSessionParams
    ): Result<CreateGameSessionResult> =
        gameApi.createGame(params.toRequestDto()).map { it.toDomain() }

    override suspend fun getHostToken(gameId: Long): Result<String> =
        gameApi.getHostToken(gameId).map { it.hostToken.socketToken }

    override suspend fun lookupRoom(sessionCode: String): Result<RoomLookup> =
        gameApi.lookupRoom(sessionCode.trim().uppercase()).map { it.toDomain() }

    /**
     * Người đã đăng nhập: cả hai tham số null → body `{}` (explicitNulls = false),
     * server tự điền danh tính từ cookie. Guest: tên + uuid thiết bị.
     */
    override suspend fun joinRoom(
        sessionCode: String,
        playerName: String?,
        guestId: String?
    ): Result<JoinRoomResult> =
        gameApi.joinRoom(
            sessionCode = sessionCode.trim().uppercase(),
            body = JoinGameRequestDto(
                playerName = playerName?.trim(),
                playerGuestId = guestId
            )
        ).map { it.toDomain() }
}
