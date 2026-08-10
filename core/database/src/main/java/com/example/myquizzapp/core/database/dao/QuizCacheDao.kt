package com.example.myquizzapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myquizzapp.core.database.entity.CachedQuizEntity

@Dao
interface QuizCacheDao {
    @Query("SELECT * FROM cached_quizzes WHERE quizId = :quizId")
    suspend fun getById(quizId: Long): CachedQuizEntity?

    @Upsert
    suspend fun upsert(quiz: CachedQuizEntity)

    @Query("DELETE FROM cached_quizzes WHERE cachedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}