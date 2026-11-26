// CookieStore.kt
package android.kma.myquizzapp.core.common.cookie

interface CookieStore {
    suspend fun loadForHost(host: String): List<StoredCookie>
    suspend fun saveAll(host: String, cookies: List<StoredCookie>)
    suspend fun clear()   // logout + refresh-thất-bại cần xóa sạch
}

/** Local credential cleanup failed; callers must not report successful logout. */
class CredentialStorageException(cause: Throwable) : java.io.IOException("Credential storage unavailable", cause)

// StoredCookie.kt
data class StoredCookie(
    val name: String, val value: String, val domain: String,
    val path: String, val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean,
    val hostOnly: Boolean = true
)