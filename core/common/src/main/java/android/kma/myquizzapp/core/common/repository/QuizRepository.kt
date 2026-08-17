package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizCard
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
     * Lấy danh sách quiz của user hiện tại.
     * 
     * Endpoint: GET /v1/quizzes/me (authRequired)
     * Version 1: Simple list, không có pagination
     */
    suspend fun getMyQuizzes(): Result<List<QuizCard>>
    
    /**
     * Lấy full quiz detail với questions array.
     * 
     * Endpoint: GET /v1/quizzes/id/:quizId (optionalAuth)
     */
    suspend fun getQuizDetail(quizId: Long): Result<Quiz>
    
    // TODO: Phase 3 (N13-14) - CRUD methods
    // suspend fun createQuiz(request: CreateQuizRequest): Result<Quiz, AppError>
    // suspend fun updateQuiz(quizId: Long, request: UpdateQuizRequest): Result<Quiz, AppError>
    // suspend fun deleteQuiz(quizId: Long): Result<Unit, AppError>
}
