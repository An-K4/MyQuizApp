package android.kma.myquizzapp.core.network.di

import android.kma.myquizzapp.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/** Log only the HTTP method and response status, never URLs or payloads. */
internal class SafeHttpLogger(
    private val enabled: Boolean = BuildConfig.DEBUG,
    private val write: (String) -> Unit = { Timber.d(it) }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (enabled) write("HTTP ${request.method}")
        val response = chain.proceed(request)
        if (enabled) write("HTTP ${request.method} -> ${response.code}")
        return response
    }
}
