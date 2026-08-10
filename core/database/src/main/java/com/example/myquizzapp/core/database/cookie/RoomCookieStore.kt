package com.example.myquizzapp.core.database.cookie

import com.example.myquizzapp.core.common.cookie.CookieStore
import com.example.myquizzapp.core.common.cookie.StoredCookie
import com.example.myquizzapp.core.database.dao.CookieDao
import com.example.myquizzapp.core.database.entity.CookieEntity
import javax.inject.Inject

// cookie/RoomCookieStore.kt — file DUY NHẤT toàn app được biết cả Room lẫn CookieStore
class RoomCookieStore @Inject constructor(
    private val cookieDao: CookieDao
) : CookieStore {

    override suspend fun loadForHost(host: String) =
        cookieDao.findByHost(host, System.currentTimeMillis()).map { it.toStoredCookie() }

    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) {
        val now = System.currentTimeMillis()
        cookies.filter { it.expiresAt <= now }              // cookie hết hạn = server clear (logout)
            .forEach { cookieDao.deleteByKey("$host|${it.name}") }
        cookieDao.upsertAll(cookies.filter { it.expiresAt > now }.map { it.toEntity(host) })
    }

    override suspend fun clear() = cookieDao.clear()
}

private fun StoredCookie.toEntity(host: String) =
    CookieEntity("$host|$name", value, domain, path, expiresAt, secure, httpOnly)

private fun CookieEntity.toStoredCookie() =
    StoredCookie(key.substringAfterLast("|"), value, domain, path, expiresAt, secure, httpOnly)