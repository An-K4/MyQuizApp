package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.model.QuizPatch
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.common.result.Result

/**
 * Repository interface cho Quiz domain.
 * 
 * Interface đặt tại core:common vì được dùng bởi nhiều feature modules:
 * - feature:home (search/browse)
 * - feature:quiz-manage (CRUD)
 * 
 * Implementation: QuizRepositoryImpl trong core:network
 */
interface QuizRepository {
    
    /**
     * Lấy home content với sections (featured, trending, newest, continue...).
     * 
     * Endpoint: GET /v1/quizzes/home (optionalAuth)
     * - Guest: trả featured, trending, newest
     * - Authenticated: thêm "continue" section
     */
    suspend fun getHomeContent(): Result<List<HomeSection>>
    
    /**
     * Search quiz công khai theo keyword.
     * 
     * Endpoint: GET /v1/quizzes/search?keyword=...
     * Version 1: Simple list, không có pagination (để học Paging 3 sau)
     */
    suspend fun searchQuizzes(keyword: String): Result<List<QuizCard>>
    
    /**
     * Lấy 1 trang danh sách quiz của user hiện tại (N13-14).
     *
     * Endpoint: GET /v1/quizzes/me (authRequired), cursor-paginated.
     * QuizSummary là superset của QuizCard (thêm is_public/updated_at — cần thiết
     * để hiển thị badge công khai/riêng tư và thời gian sửa cuối trên danh sách này).
     * Cursor cho trang kế tiếp trả về qua Result.Success.page (PageInfo), không phải
     * trong data — dùng cho MyQuizzesPagingSource (feature:quiz-manage, Paging 3).
     */
    suspend fun getMyQuizzes(params: MyQuizzesParams): Result<List<QuizSummary>>
    
    /**
     * Lấy full quiz detail với questions array.
     * 
     * Endpoint: GET /v1/quizzes/id/:quizId (optionalAuth)
     */
    suspend fun getQuizDetail(quizId: Long): Result<Quiz>
    
    /**
     * Tạo quiz mới cùng toàn bộ câu hỏi trong 1 lần gọi (N13-14).
     *
     * Endpoint: POST /v1/quizzes (authRequired). Quiz cần ít nhất 1 câu hỏi —
     * backend từ chối với QUIZ_NO_QUESTIONS nếu không (quiz.service.ts).
     */
    suspend fun createQuiz(newQuiz: NewQuiz): Result<Quiz>
    
    /**
     * Cập nhật quiz (N16). Metadata patch từng field; `patch.questions` nếu != null
     * thì THAY THẾ toàn bộ danh sách câu hỏi (backend replaceQuizQuestions).
     *
     * Endpoint: PATCH /v1/quizzes/id/:quizId (authRequired, chỉ owner —
     * không phải owner nhận 404 QUIZ_NOT_FOUND, backend cố ý không lộ 403).
     */
    suspend fun updateQuiz(quizId: Long, patch: QuizPatch): Result<Quiz>

    /**
     * Xóa quiz (N16) — HARD DELETE: xóa hẳn row, cascade mất luôn lịch sử các
     * phòng đã chơi (quiz.repository.ts). Không có undo.
     *
     * Endpoint: DELETE /v1/quizzes/id/:quizId (authRequired, chỉ owner).
     */
    suspend fun deleteQuiz(quizId: Long): Result<Unit>
}
