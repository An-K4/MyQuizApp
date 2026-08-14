package android.kma.myquizzapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_quizzes")
data class CachedQuizEntity(
    @PrimaryKey val quizId: Long,
    val quizJson: String,   // Quiz serialize bằng kotlinx.serialization
    val cachedAt: Long
)