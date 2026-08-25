// QuizCacheStore.kt
package android.kma.myquizzapp.core.common.cache

import android.kma.myquizzapp.core.common.model.Quiz

/**
 * Hợp đồng cache offline cho quiz detail — thuần Kotlin, không biết đến Room.
 *
 * impl thật: core:database (RoomQuizCacheStore), tự @Binds ngay trong module đó.
 * core:network chỉ nhận interface này qua constructor injection — không được phép
 * phụ thuộc core:database (giống pattern CookieStore, xem design doc mục 3.2/12.1).
 */
interface QuizCacheStore {
    suspend fun getCachedQuiz(quizId: Long): Quiz?
    suspend fun cacheQuiz(quizId: Long, quiz: Quiz)

    /**
     * Xóa quiz khỏi cache (N16) — gọi sau khi deleteQuiz thành công để màn chi tiết
     * không "hồi sinh" quiz đã xóa từ Room khi offline.
     */
    suspend fun removeQuiz(quizId: Long)
}
