package android.kma.myquizzapp.core.network.api

import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.network.dto.CreateQuizRequestDto
import android.kma.myquizzapp.core.network.dto.HomeContentDto
import android.kma.myquizzapp.core.network.dto.QuizCardDto
import android.kma.myquizzapp.core.network.dto.QuizDetailDto
import android.kma.myquizzapp.core.network.dto.QuizListDto
import android.kma.myquizzapp.core.network.dto.QuizSummaryListDto
import android.kma.myquizzapp.core.network.dto.UpdateQuizRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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
     * GET /v1/quizzes/me?visibility=...&keyword=...&sort=...&cursor=...&limit=...
     * 
     * Auth required: cookie authentication.
     * Trả 1 trang danh sách quiz của user hiện tại (cursor pagination) — khớp
     * myQuizzesQuerySchema (quiz.schema.ts). cursor null = trang đầu.
     * 
     * Backend bọc response trong { quizzes: [...] } (listing.controller.ts →
     * success(res, { quizzes: page.items })). Dùng QuizSummaryDto (không phải
     * QuizCardDto) vì /quizzes/me trả thêm is_public/updated_at (chỉ owner mới
     * xem được). Cursor/hasMore của trang này nằm trong meta.pagination, được
     * ResultCall tự gắn vào Result.Success.page — xem ApiCallResult.kt.
     */
    @GET("quizzes/me")
    suspend fun getMyQuizzes(
        @Query("visibility") visibility: String,
        @Query("keyword") keyword: String?,
        @Query("sort") sort: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int
    ): Result<QuizSummaryListDto>
    
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
    
    /**
     * POST /v1/quizzes
     * 
     * Auth required. Tạo quiz mới cùng toàn bộ câu hỏi trong 1 request — khớp
     * createQuizSchema (quiz.schema.ts). Backend insert quiz + questions rồi
     * đọc lại full quiz (giống GET detail), nên response cũng bọc trong
     * { quiz: {...} } → dùng lại QuizDetailDto.
     */
    @POST("quizzes")
    suspend fun createQuiz(
        @Body body: CreateQuizRequestDto
    ): Result<QuizDetailDto>
    
    /**
     * PATCH /v1/quizzes/id/:quizId (N16)
     *
     * Auth required, chỉ owner. updateQuizSchema = createQuizSchema.partial() — field
     * nào vắng mặt thì giữ nguyên; `questions` nếu gửi thì THAY THẾ toàn bộ danh sách
     * câu hỏi (replaceQuizQuestions). Json chung có explicitNulls=false nên field null
     * trong UpdateQuizRequestDto sẽ bị omit khỏi body — đúng semantics "không đụng tới".
     * Response bọc { quiz: {...} } → dùng lại QuizDetailDto.
     */
    @PATCH("quizzes/id/{quizId}")
    suspend fun updateQuiz(
        @Path("quizId") quizId: Long,
        @Body body: UpdateQuizRequestDto
    ): Result<QuizDetailDto>

    /**
     * DELETE /v1/quizzes/id/:quizId (N16)
     *
     * Auth required, chỉ owner. HARD DELETE — xóa hẳn row, cascade xóa questions,
     * quiz_snapshots, game_sessions, player_sessions (quiz.repository.ts). Response
     * trả lại row vừa xóa bọc trong { quiz: {...} } — client chỉ cần biết thành công.
     */
    @DELETE("quizzes/id/{quizId}")
    suspend fun deleteQuiz(
        @Path("quizId") quizId: Long
    ): Result<QuizDetailDto>
}
