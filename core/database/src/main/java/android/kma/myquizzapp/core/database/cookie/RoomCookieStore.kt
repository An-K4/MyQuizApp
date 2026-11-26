package android.kma.myquizzapp.core.database.cookie

import android.kma.myquizzapp.core.common.cookie.CookieStore
import android.kma.myquizzapp.core.common.cookie.StoredCookie
import android.kma.myquizzapp.core.database.dao.CookieDao
import android.kma.myquizzapp.core.database.entity.CookieEntity
import javax.inject.Inject

class RoomCookieStore @Inject constructor(private val cookieDao: CookieDao) : CookieStore {
    override suspend fun loadForHost(host: String): List<StoredCookie> =
        cookieDao.findByHost(host, System.currentTimeMillis()).map { it.toStoredCookie() }

    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        cookieDao.applyCookies(host, cookies.map { it.toEntity() }, System.currentTimeMillis())
    }

    override suspend fun clear() = cookieDao.clear()
}

private fun StoredCookie.toEntity() = CookieEntity(name, domain, path, value, expiresAt, secure, httpOnly, hostOnly, legacy = false)
private fun CookieEntity.toStoredCookie() = StoredCookie(name, value, domain, path, expiresAt, secure, httpOnly, hostOnly)
