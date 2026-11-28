package android.kma.myquizzapp.core.network.cookie

import android.kma.myquizzapp.core.common.cookie.StoredCookie
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/** Stamps the original call once; retry and redirect requests retain the tag. */
class CredentialStampInterceptor @Inject constructor(
    private val lifecycle: CredentialLifecycle
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val stamp = request.tag(CredentialEpoch::class.java) ?: lifecycle.current()
        val version = request.tag(RefreshVersion::class.java) ?: lifecycle.refreshVersion(stamp)
            ?: throw IOException("Credential lifetime retired")
        return chain.proceed(request.newBuilder().tag(CredentialEpoch::class.java, stamp)
            .tag(RefreshVersion::class.java, version).build())
    }
}

/**
 * Request-aware cookie I/O for an authenticated client using NO_COOKIES.
 * Must not be installed on public or raw-upload clients.
 */
class CredentialCookieInterceptor @Inject constructor(
    private val lifecycle: CredentialLifecycle
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val epoch = original.tag(CredentialEpoch::class.java)
            ?: throw IOException("Missing credential lifetime")
        // Unrelated requests cannot refresh an empty/incomplete login lifetime.
        val path = original.url.encodedPath
        val authOperation = path.endsWith("/auth/login") || path.endsWith("/auth/register") ||
            path.endsWith("/auth/google/one-tap") || path.endsWith("/auth/logout")
        val stored = lifecycle.load(epoch, original.url.host, allowPending = authOperation)
            ?: throw IOException("Credential lifetime unavailable")
        val cookies = stored.map { it.toCookie() }
            .filter { it.expiresAt > System.currentTimeMillis() && it.matches(original.url) }
            .sortedByDescending { it.path.length }
        val request = original.newBuilder().removeHeader("Cookie").apply {
            if (cookies.isNotEmpty()) header("Cookie", cookies.joinToString("; ") { "${it.name}=${it.value}" })
        }.build()
        val response = chain.proceed(request)
        val received = Cookie.parseAll(request.url, response.headers).map { it.toStoredCookie() }
        if (!lifecycle.save(epoch, request.url.host, received)) {
            response.close()
            throw IOException("Credential lifetime retired")
        }
        return response
    }
}

private fun Cookie.toStoredCookie() =
    StoredCookie(name, value, domain, path, expiresAt, secure, httpOnly, hostOnly)

internal fun StoredCookie.toCookie(): Cookie = Cookie.Builder()
    .name(name).value(value).path(path).expiresAt(expiresAt)
    .apply {
        if (hostOnly) hostOnlyDomain(domain) else domain(domain)
        if (secure) secure()
        if (httpOnly) httpOnly()
    }.build()
