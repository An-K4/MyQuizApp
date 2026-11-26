package android.kma.myquizzapp.core.database

import android.content.Context
import android.kma.myquizzapp.core.database.cookie.RoomCookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated synthetic database; never opens the app's myquizz.db. */
@RunWith(AndroidJUnit4::class)
class CookieMigrationTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "cookie-migration-test.db"
    private var db: MyQuizzDatabase? = null

    @After fun cleanup() { db?.close(); context.deleteDatabase(name) }

    @Test fun storePreservesPathsAndDeletesOnlyMatchingScope() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MyQuizzDatabase::class.java).build()
        db = database
        val store = RoomCookieStore(database.cookieDao())
        val expiry = 4102444800000L
        store.saveAll("api.example.test", listOf(
            StoredCookie("sid", "root", "example.test", "/", expiry, false, true, false),
            StoredCookie("sid", "nested", "example.test", "/v1", expiry, true, true, false),
        ))
        assertEquals(2, store.loadForHost("child.example.test").size)
        assertTrue(store.loadForHost("evil-example.test").isEmpty())
        store.saveAll("api.example.test", listOf(
            StoredCookie("sid", "", "example.test", "/v1", 0, true, true, false),
        ))
        assertEquals(listOf("/"), store.loadForHost("api.example.test").map { it.path })
        store.clear()
        assertTrue(store.loadForHost("api.example.test").isEmpty())
    }

    @Test fun upgradePreservesLegacyCookieAndAllowsCorrectDeletion() = runBlocking {
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        old.execSQL("CREATE TABLE IF NOT EXISTS cookie_store (`key` TEXT NOT NULL, `value` TEXT NOT NULL, `domain` TEXT NOT NULL, `path` TEXT NOT NULL, `expiresAt` INTEGER NOT NULL, `secure` INTEGER NOT NULL, `httpOnly` INTEGER NOT NULL, PRIMARY KEY(`key`))")
        old.execSQL("CREATE TABLE IF NOT EXISTS cached_quizzes (`quizId` INTEGER NOT NULL, `quizJson` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`quizId`))")
        old.execSQL("CREATE TABLE IF NOT EXISTS game_history (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, `sessionName` TEXT NOT NULL, `gameMode` TEXT NOT NULL, `playerName` TEXT NOT NULL, `finalScore` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `rank` INTEGER, `playedAt` INTEGER NOT NULL)")
        old.execSQL("INSERT INTO cookie_store VALUES ('api.example.test|session', 'synthetic', 'example.test', '/', 4102444800000, 1, 1)")
        old.execSQL("INSERT INTO cached_quizzes VALUES (1, '{}', 123)")
        old.version = 1
        old.close()

        val migrated = Room.databaseBuilder(context, MyQuizzDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2).build()
        db = migrated
        val store = RoomCookieStore(migrated.cookieDao())
        val legacy = store.loadForHost("api.example.test").single()
        assertEquals("synthetic", legacy.value)
        assertEquals("api.example.test", legacy.domain)
        assertTrue(legacy.hostOnly)
        assertEquals(0, store.loadForHost("example.test").size)
        val cursor = migrated.openHelper.readableDatabase.query("SELECT count(*) FROM cached_quizzes")
        cursor.use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }

        // Server clears a Domain cookie: the narrowed legacy copy must not survive.
        store.saveAll("api.example.test", listOf(StoredCookie("session", "", "example.test", "/", 0, true, true, false)))
        assertTrue(store.loadForHost("api.example.test").isEmpty())
    }
}
