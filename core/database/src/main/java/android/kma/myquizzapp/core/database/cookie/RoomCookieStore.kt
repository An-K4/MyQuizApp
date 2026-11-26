package android.kma.myquizzapp.core.database.cookie

import android.content.Context
import android.content.SharedPreferences
import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.database.dao.CookieDao
import android.kma.myquizzapp.core.database.entity.CookieEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject

class RoomCookieStore internal constructor(cookieDao: CookieDao, journal: ClearJournal) : CookieStore {
    @Inject constructor(cookieDao: CookieDao, @ApplicationContext context: Context) : this(
        cookieDao,
        PreferenceClearJournal(context.getSharedPreferences("credential_cleanup_v1", Context.MODE_PRIVATE))
    )

    private val guarded = JournaledCookieStore(RoomDelegate(cookieDao), journal)
    override suspend fun loadForHost(host: String) = guarded.loadForHost(host)
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) = guarded.saveAll(host, cookies)
    override suspend fun clear() = guarded.clear()
}

internal class PreferenceClearJournal(private val preferences: SharedPreferences) : ClearJournal {
    override fun isPending(): Boolean = preferences.getBoolean("pending", false)
    override fun setPending(value: Boolean) {
        if (!preferences.edit().putBoolean("pending", value).commit()) {
            throw IOException("Unable to persist credential cleanup state")
        }
    }
}

private class RoomDelegate(private val cookieDao: CookieDao) : CookieStore {
    override suspend fun loadForHost(host: String): List<StoredCookie> =
        cookieDao.findByHost(host, System.currentTimeMillis()).map { it.toStoredCookie() }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        cookieDao.applyCookies(host, cookies.map { it.toEntity() }, System.currentTimeMillis())
    }
    override suspend fun clear() = cookieDao.clear()
}

private fun StoredCookie.toEntity() = CookieEntity(name, domain, path, value, expiresAt, secure, httpOnly, hostOnly, legacy = false)
private fun CookieEntity.toStoredCookie() = StoredCookie(name, value, domain, path, expiresAt, secure, httpOnly, hostOnly)
