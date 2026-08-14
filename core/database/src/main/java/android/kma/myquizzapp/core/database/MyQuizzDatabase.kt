package android.kma.myquizzapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import android.kma.myquizzapp.core.database.dao.CookieDao
import android.kma.myquizzapp.core.database.dao.GameHistoryDao
import android.kma.myquizzapp.core.database.dao.QuizCacheDao
import android.kma.myquizzapp.core.database.entity.CachedQuizEntity
import android.kma.myquizzapp.core.database.entity.CookieEntity
import android.kma.myquizzapp.core.database.entity.GameHistoryEntity

@Database(
    entities = [CookieEntity::class, CachedQuizEntity::class, GameHistoryEntity::class],
    version = 1,
    exportSchema = false   // khi nào có migration thật thì bật lại + commit schema JSON để test migration
)
abstract class MyQuizzDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun quizCacheDao(): QuizCacheDao
    abstract fun gameHistoryDao(): GameHistoryDao
}