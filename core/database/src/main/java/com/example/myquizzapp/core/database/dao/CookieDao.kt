package com.example.myquizzapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.myquizzapp.core.database.entity.CookieEntity

@Dao
interface CookieDao {
    @Query("SELECT * FROM cookie_store WHERE domain = :host AND expiresAt > :now")
    suspend fun findByHost(host: String, now: Long): List<CookieEntity>

    @Upsert
    suspend fun upsertAll(cookies: List<CookieEntity>)

    @Query("DELETE FROM cookie_store WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM cookie_store")
    suspend fun clear()
}