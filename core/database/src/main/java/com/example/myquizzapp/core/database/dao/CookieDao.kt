package com.example.myquizzapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myquizzapp.core.database.entity.CookieEntity

@Dao
interface CookieDao {
    @Query("SELECT * FROM cookies WHERE expiresAt > :now")
    suspend fun getValid(now: Long): List<CookieEntity>

    @Upsert   // Room 2.6+ — insert hoặc update nếu trùng primary key
    suspend fun upsertAll(cookies: List<CookieEntity>)

    @Query("DELETE FROM cookies WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM cookies")
    suspend fun clear()   // logout
}