package android.kma.myquizzapp.core.database.cache

import android.kma.myquizzapp.core.common.cache.QuizCacheStore
import android.kma.myquizzapp.core.common.model.Quiz
import android.kma.myquizzapp.core.database.dao.QuizCacheDao
import android.kma.myquizzapp.core.database.entity.CachedQuizEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// cache/RoomQuizCacheStore.kt — file DUY NHẤT toàn app được biết cả Room lẫn QuizCacheStore
class RoomQuizCacheStore @Inject constructor(
    private val quizCacheDao: QuizCacheDao
) : QuizCacheStore {

    override suspend fun getCachedQuiz(quizId: Long): Quiz? {
        val cached = quizCacheDao.getById(quizId) ?: return null
        return try {
            json.decodeFromString<Quiz>(cached.quizJson)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cacheQuiz(quizId: Long, quiz: Quiz) {
        try {
            quizCacheDao.upsert(
                CachedQuizEntity(
                    quizId = quizId,
                    quizJson = json.encodeToString(quiz),
                    cachedAt = System.currentTimeMillis()
                )
            )
            quizCacheDao.deleteOlderThan(System.currentTimeMillis() - CACHE_TTL_MS)
        } catch (e: Exception) {
            // Cache là best-effort, không throw để tránh làm hỏng response chính
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        // Dọn cache cũ hơn 7 ngày mỗi lần ghi mới, tránh Room phình to vô hạn
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
