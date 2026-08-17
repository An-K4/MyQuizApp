package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.HomeContentDto
import android.kma.myquizzapp.core.network.dto.QuizCardDto
import android.kma.myquizzapp.core.common.model.Quiz
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service cho Quiz endpoints.
 * 
 * Base URL: /v1/quizzes
 */
interface QuizApiService {
    
    /**
     * GET /v1/quizzes/home
     * 
     * Optional auth: cookie tự động gửi kèm nếu có.
     * Response khác nhau tùy auth state (guest vs authenticated).
     */
    @GET("quizzes/home")
    suspend fun getHomeContent(): Result<HomeContentDto>
    
    /**
     * GET /v1/quizzes/search?keyword=...
     * 
     * Optional auth: public search, không cần login.
     * 
     * Note: Backend có pagination nhưng v1 này chưa dùng.
     * TODO: N13 - refactor sang Paging 3 với page/limit params.
     */
    @GET("quizzes/search")
    suspend fun searchQuizzes(
        @Query("keyword") keyword: String
    ): Result<List<QuizCardDto>>
    
    /**
     * GET /v1/quizzes/me
     * 
     * Auth required: cookie authentication.
     * Trả danh sách quiz của user hiện tại.
     * 
     * TODO: N13 - add pagination params.
     */
    @GET("quizzes/me")
    suspend fun getMyQuizzes(): Result<List<QuizCardDto>>
    
    /**
     * GET /v1/quizzes/id/:quizId
     * 
     * Optional auth: public quiz có thể xem không cần login.
     * Trả full quiz detail với questions array.
     */
    @GET("quizzes/id/{quizId}")
    suspend fun getQuizDetail(
        @Path("quizId") quizId: Long
    ): Result<Quiz>
    
    // TODO: Phase 3 (N13-14) - CRUD endpoints
    // @POST("quizzes")
    // suspend fun createQuiz(@Body request: CreateQuizRequest): Quiz
    
    // @PATCH("quizzes/id/{quizId}")
    // suspend fun updateQuiz(@Path("quizId") quizId: Long, @Body request: UpdateQuizRequest): Quiz
    
    // @DELETE("quizzes/id/{quizId}")
    // suspend fun deleteQuiz(@Path("quizId") quizId: Long)
}
