package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.CreateGameRequestDto
import android.kma.myquizzapp.core.network.dto.CreateGameResponseDto
import android.kma.myquizzapp.core.network.dto.GameModesResponseDto
import android.kma.myquizzapp.core.network.dto.HostTokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Games REST API dùng PreserveCaseRetrofit vì payload trộn snake_case và camelCase. */
interface GameApiService {
    @GET("games/game-modes")
    suspend fun getGameModes(): Result<GameModesResponseDto>

    @POST("games")
    suspend fun createGame(@Body body: CreateGameRequestDto): Result<CreateGameResponseDto>

    @POST("games/{id}/host-token")
    suspend fun getHostToken(@Path("id") gameId: Long): Result<HostTokenResponseDto>
}
