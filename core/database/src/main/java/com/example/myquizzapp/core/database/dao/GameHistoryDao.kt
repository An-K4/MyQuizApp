package com.example.myquizzapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myquizzapp.core.database.entity.GameHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameHistoryDao {
    @Insert
    suspend fun insert(entry: GameHistoryEntity)

    @Query("SELECT * FROM game_history ORDER BY playedAt DESC")
    fun observeAll(): Flow<List<GameHistoryEntity>>  // Flow — UI tự cập nhật khi có trận mới
}