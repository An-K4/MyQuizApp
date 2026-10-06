package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.CreateGameRequestDto
import android.kma.myquizzapp.core.network.dto.CreateGameResponseDto
import android.kma.myquizzapp.core.network.dto.GameHistoryResponseDto
import android.kma.myquizzapp.core.network.dto.GameHistorySummaryResponseDto
import android.kma.myquizzapp.core.network.dto.GameModesResponseDto
import android.kma.myquizzapp.core.network.dto.GameReviewResponseDto
import android.kma.myquizzapp.core.network.dto.GameResultsResponseDto
import android.kma.myquizzapp.core.network.dto.HostTokenResponseDto
import android.kma.myquizzapp.core.network.dto.JoinGameRequestDto
import android.kma.myquizzapp.core.network.dto.JoinGameResponseDto
import android.kma.myquizzapp.core.network.dto.RoomLookupResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

/** Games REST API dùng PreserveCaseRetrofit vì payload trộn snake_case và camelCase. */
interface GameApiService {
    @GET("games/game-modes")
    suspend fun getGameModes(): Result<GameModesResponseDto>

    @POST("games")
    suspend fun createGame(@Body body: CreateGameRequestDto): Result<CreateGameResponseDto>

    @POST("games/{id}/host-token")
    suspend fun getHostToken(@Path("id") gameId: Long): Result<HostTokenResponseDto>

    /**
     * Tra phòng theo mã. Endpoint PUBLIC — guest chưa đăng nhập vẫn gọi được.
     *
     * Path là `{code}` (mã 6 ký tự), KHÔNG phải id số — khác với các endpoint
     * `games/{id}/...` bên trên.
     */
    @GET("games/{code}")
    suspend fun lookupRoom(@Path("code") sessionCode: String): Result<RoomLookupResponseDto>

    /**
     * Vào phòng. optionalAuth: có cookie thì server dùng danh tính phiên đăng nhập
     * và bỏ qua body; không có cookie thì bắt buộc có player_name + player_guest_id.
     * Trả về 201 kèm socketToken phẳng.
     */
    @POST("games/{code}/join")
    suspend fun joinRoom(
        @Path("code") sessionCode: String,
        @Body body: JoinGameRequestDto
    ): Result<JoinGameResponseDto>

    /** Optional-auth history: client có cookie cho user, header UUID cho guest. */
    @GET("games/history")
    suspend fun getGameHistory(
        @Query("role") role: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("include_total") includeTotal: Boolean = false,
        @Header("x-guest-id") guestId: String? = null
    ): Result<GameHistoryResponseDto>

    /** Tổng quan một trận cũ; cookie user thắng header guest nếu cả hai cùng có. */
    @GET("games/{id}/summary")
    suspend fun getGameHistorySummary(
        @Path("id") gameId: Long,
        @Header("x-guest-id") guestId: String? = null
    ): Result<GameHistorySummaryResponseDto>

    /** Answer sheet lịch sử dùng cookie/guest id thay cho socket token đã mất. */
    @GET("games/{id}/my-answers")
    suspend fun getGameHistoryAnswers(
        @Path("id") gameId: Long,
        @Header("x-guest-id") guestId: String? = null
    ): Result<GameReviewResponseDto>

    /** Kết quả công khai; chỉ dùng làm fallback khi payload `game:ended` không còn trong RAM. */
    @GET("games/{id}/results")
    suspend fun getGameResults(@Path("id") gameId: Long): Result<GameResultsResponseDto>

    /** Answer sheet của chính Player; backend chỉ cho đọc sau khi trận kết thúc. */
    @GET("games/{id}/review")
    suspend fun getGameReview(
        @Path("id") gameId: Long,
        @Header("x-socket-token") socketToken: String
    ): Result<GameReviewResponseDto>
}
