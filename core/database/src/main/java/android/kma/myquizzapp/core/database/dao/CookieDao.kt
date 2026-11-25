package android.kma.myquizzapp.core.database.dao

import android.kma.myquizzapp.core.database.entity.CookieEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface CookieDao {
    // Return candidates only; OkHttp still enforces host-only, path and Secure.
    @Query("SELECT * FROM cookie_store WHERE expiresAt > :now AND (:host = domain OR substr(:host, -(length(domain) + 1)) = '.' || domain)")
    suspend fun findByHost(host: String, now: Long): List<CookieEntity>

    @Upsert
    suspend fun upsertAll(cookies: List<CookieEntity>)

    @Query("DELETE FROM cookie_store WHERE name = :name AND domain = :domain AND path = :path")
    suspend fun deleteByScope(name: String, domain: String, path: String)

    @Query("DELETE FROM cookie_store WHERE legacy = 1 AND domain = :host AND name = :name AND path = :path")
    suspend fun deleteLegacy(host: String, name: String, path: String)

    @Transaction
    suspend fun applyCookies(host: String, cookies: List<CookieEntity>, now: Long) {
        for (cookie in cookies) {
            deleteLegacy(host, cookie.name, cookie.path)
            if (cookie.expiresAt <= now) deleteByScope(cookie.name, cookie.domain, cookie.path)
            else upsertAll(listOf(cookie))
        }
    }

    @Query("DELETE FROM cookie_store")
    suspend fun clear()
}
