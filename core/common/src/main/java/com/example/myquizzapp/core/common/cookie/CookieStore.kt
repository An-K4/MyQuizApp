// CookieStore.kt
package com.example.myquizzapp.core.common.cookie

interface CookieStore {
    suspend fun loadForHost(host: String): List<StoredCookie>
    suspend fun saveAll(host: String, cookies: List<StoredCookie>)
    suspend fun clear()   // logout + refresh-thất-bại cần xóa sạch
}

// StoredCookie.kt
data class StoredCookie(
    val name: String, val value: String, val domain: String,
    val path: String, val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean
)