package android.kma.myquizzapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sessionName: String,
    val gameMode: String,
    val playerName: String,
    val finalScore: Int,
    val correctCount: Int,
    val totalQuestions: Int,
    val rank: Int?,
    val playedAt: Long
)