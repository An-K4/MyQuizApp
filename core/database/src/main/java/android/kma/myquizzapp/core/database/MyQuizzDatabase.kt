package android.kma.myquizzapp.core.database

import android.kma.myquizzapp.core.database.dao.CookieDao
import android.kma.myquizzapp.core.database.dao.GameHistoryDao
import android.kma.myquizzapp.core.database.dao.QuizCacheDao
import android.kma.myquizzapp.core.database.entity.CachedQuizEntity
import android.kma.myquizzapp.core.database.entity.CookieEntity
import android.kma.myquizzapp.core.database.entity.GameHistoryEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v1 did not record hostOnly; retain legacy credentials only for the exact
// response host encoded in the old key. Never widen an unknown cookie scope.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cookie_store_new` (`name` TEXT NOT NULL, `domain` TEXT NOT NULL, `path` TEXT NOT NULL, `value` TEXT NOT NULL, `expiresAt` INTEGER NOT NULL, `secure` INTEGER NOT NULL, `httpOnly` INTEGER NOT NULL, `hostOnly` INTEGER NOT NULL, `legacy` INTEGER NOT NULL, PRIMARY KEY(`name`, `domain`, `path`))")
        db.execSQL("INSERT OR REPLACE INTO `cookie_store_new` (`name`, `domain`, `path`, `value`, `expiresAt`, `secure`, `httpOnly`, `hostOnly`, `legacy`) SELECT substr(`key`, instr(`key`, '|') + 1), substr(`key`, 1, instr(`key`, '|') - 1), `path`, `value`, `expiresAt`, `secure`, `httpOnly`, 1, 1 FROM `cookie_store` WHERE instr(`key`, '|') > 1")
        db.execSQL("DROP TABLE `cookie_store`")
        db.execSQL("ALTER TABLE `cookie_store_new` RENAME TO `cookie_store`")
    }
}

@Database(
    entities = [CookieEntity::class, CachedQuizEntity::class, GameHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MyQuizzDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun quizCacheDao(): QuizCacheDao
    abstract fun gameHistoryDao(): GameHistoryDao
}