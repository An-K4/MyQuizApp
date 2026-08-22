package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.HomeContentDto
import android.kma.myquizzapp.core.network.dto.QuizCardDto
import android.kma.myquizzapp.core.network.dto.QuizDetailDto
import android.kma.myquizzapp.core.network.dto.QuizListDto
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
     * 
     * Backend bọc response trong { quizzes: [...] } (listing.controller.ts →
     * success(res, { quizzes: page.items })), nên kiểu trả về là QuizListDto
     * chứ không phải List<QuizCardDto> trực tiếp.
     */
    @GET("quizzes/search")
    suspend fun searchQuizzes(
        @Query("keyword") keyword: String
    ): Result<QuizListDto>
    
    /**
     * GET /v1/quizzes/me
     * 
     * Auth required: cookie authentication.
     * Trả danh sách quiz của user hiện tại.
     * 
     * TODO: N13 - add pagination params.
     * 
     * Backend bọc response trong { quizzes: [...] } (listing.controller.ts →
     * success(res, { quizzes: page.items })), nên kiểu trả về là QuizListDto
     * chứ không phải List<QuizCardDto> trực tiếp.
     */
    @GET("quizzes/me")
    suspend fun getMyQuizzes(): Result<QuizListDto>
    
    /**
     * GET /v1/quizzes/id/:quizId
     * 
     * Optional auth: public quiz có thể xem không cần login.
     * Trả full quiz detail với questions array.
     * 
     * Backend bọc response trong { quiz: {...} } (quiz.controller.ts → success(res, { quiz })),
     * nên kiểu trả về là QuizDetailDto chứ không phải Quiz trực tiếp.
     */
    @GET("quizzes/id/{quizId}")
    suspend fun getQuizDetail(
        @Path("quizId") quizId: Long
    ): Result<QuizDetailDto>
    
    // TODO: Phase 3 (N13-14) - CRUD endpoints
    // @POST("quizzes")
    // suspend fun createQuiz(@Body request: CreateQuizRequest): Quiz
    
    // @PATCH("quizzes/id/{quizId}")
    // suspend fun updateQuiz(@Path("quizId") quizId: Long, @Body request: UpdateQuizRequest): Quiz
    
    // @DELETE("quizzes/id/{quizId}")
    // suspend fun deleteQuiz(@Path("quizId") quizId: Long)
}
