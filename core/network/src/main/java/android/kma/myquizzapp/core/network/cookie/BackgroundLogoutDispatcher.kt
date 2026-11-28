package android.kma.myquizzapp.core.network.cookie

import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/** One-shot best-effort logout. Never writes response cookies to the active session. */
internal class BackgroundLogoutDispatcher(
    private val lifecycle: CredentialLifecycle,
    client: OkHttpClient,
    private val logoutUrl: HttpUrl,
) {
    // Do not log credentials, follow redirects, retry, refresh, or use the shared cookie jar.
    private val isolatedClient = client.newBuilder().apply {
        cookieJar(CookieJar.NO_COOKIES)
        authenticator(Authenticator.NONE)
        followRedirects(false)
        followSslRedirects(false)
        retryOnConnectionFailure(false)
        interceptors().clear()
        networkInterceptors().clear()
    }.build()

    fun dispatch(epoch: CredentialEpoch) {
        lifecycle.ifCurrent(epoch) {
            val cookies = lifecycle.load(epoch, logoutUrl.host, allowPending = false)
                ?.map { it.toCookie() }
                ?.filter {
                    it.name in setOf("accessToken", "refreshToken") &&
                        it.expiresAt > System.currentTimeMillis() && it.matches(logoutUrl)
                }
                ?.sortedByDescending { it.path.length }
                ?: return@ifCurrent
            // The local backend logout requires both tokens. Never enqueue an anonymous logout.
            if (cookies.none { it.name == "accessToken" } || cookies.none { it.name == "refreshToken" }) return@ifCurrent
            val request = Request.Builder()
                .url(logoutUrl)
                .header("Cookie", cookies.joinToString("; ") { "${it.name}=${it.value}" })
                .post(ByteArray(0).toRequestBody())
                .build()
            isolatedClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = Unit
                override fun onResponse(call: Call, response: Response) = response.close()
            })
        }
    }
}
