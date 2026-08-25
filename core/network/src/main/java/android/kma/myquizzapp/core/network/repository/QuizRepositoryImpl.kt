package android.kma.myquizzapp.core.network.repository

import android.kma.myquizzapp.core.common.cache.QuizCacheStore
import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.MyQuizzesParams
import android.kma.myquizzapp.core.common.model.NewQuiz
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.model.QuizPatch
import android.kma.myquizzapp.core.common.model.QuizSummary
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.result.map
import android.kma.myquizzapp.core.network.api.QuizApiService
import android.kma.myquizzapp.core.network.dto.toDomain
import android.kma.myquizzapp.core.network.dto.toRequestDto
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation của QuizRepository interface.
 * 
 * Dùng QuizApiService (Retrofit) để gọi backend API và map DTO sang domain models.
 * 
 * getQuizDetail dùng cache-aside qua QuizCacheStore (interface ở core:common, impl thật
 * là RoomQuizCacheStore ở core:database — core:network KHÔNG được phép biết Room,
 * giống pattern CookieStore, xem design doc mục 3.2/12.1):
 * - Network-first: gọi API trước.
 * - Thành công: lưu quiz vào cache.
 * - Lỗi (mất mạng, server down...): fallback đọc quiz đã cache theo quizId, nếu có.
 */
class QuizRepositoryImpl @Inject constructor(
    private val quizApi: QuizApiService,
    private val quizCacheStore: QuizCacheStore
) : QuizRepository {
    
    override suspend fun getHomeContent(): Result<List<HomeSection>> =
        quizApi.getHomeContent().map { it.toDomain() }
    
    override suspend fun searchQuizzes(keyword: String): Result<List<QuizCard>> =
        // Backend bọc response trong { quizzes: [...] } → unwrap QuizListDto.quizzes.
        quizApi.searchQuizzes(keyword).map { dto -> 
            dto.quizzes.map { it.toDomain() }
        }
    
    override suspend fun getMyQuizzes(params: MyQuizzesParams): Result<List<QuizSummary>> =
        // Backend bọc response trong { quizzes: [...] } → unwrap QuizSummaryListDto.quizzes.
        // page cursor (meta.pagination) được .map giữ lại nguyên vẹn qua Result.page.
        quizApi.getMyQuizzes(
            visibility = params.visibility.apiValue,
            keyword = params.keyword?.takeIf { it.isNotBlank() },
            sort = params.sort.apiValue,
            cursor = params.cursor,
            limit = params.limit
        ).map { dto ->
            dto.quizzes.map { it.toDomain() }
        }
    
    override suspend fun createQuiz(newQuiz: NewQuiz): Result<Quiz> =
        // Backend bọc response trong { quiz: {...} } → unwrap QuizDetailDto.quiz.
        quizApi.createQuiz(newQuiz.toRequestDto()).map { it.quiz }
    
    override suspend fun getQuizDetail(quizId: Long): Result<Quiz> {
        // Backend bọc response trong { quiz: {...} } → unwrap QuizDetailDto.quiz thành domain model.
        val networkResult = quizApi.getQuizDetail(quizId).map { it.quiz }

        return when (networkResult) {
            is Result.Success -> {
                quizCacheStore.cacheQuiz(quizId, networkResult.data)
                networkResult
            }
            is Result.Error -> {
                // Network lỗi (mất mạng, server down...) - fallback đọc cache offline
                quizCacheStore.getCachedQuiz(quizId)?.let { Result.Success(it) } ?: networkResult
            }
        }
    }

    override suspend fun updateQuiz(quizId: Long, patch: QuizPatch): Result<Quiz> {
        // Backend bọc response trong { quiz: {...} } → unwrap QuizDetailDto.quiz.
        val result = quizApi.updateQuiz(quizId, patch.toRequestDto()).map { it.quiz }
        // Ghi đè cache để màn Chi tiết/Sửa mở lại thấy bản mới ngay, kể cả khi
        // lần mở sau rơi vào nhánh offline-fallback.
        if (result is Result.Success) quizCacheStore.cacheQuiz(quizId, result.data)
        return result
    }

    override suspend fun deleteQuiz(quizId: Long): Result<Unit> =
        when (val result = quizApi.deleteQuiz(quizId)) {
            is Result.Success -> {
                // Xóa cache để quiz đã xóa không "hồi sinh" từ Room khi offline.
                quizCacheStore.removeQuiz(quizId)
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
}
