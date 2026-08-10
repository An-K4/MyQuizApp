package com.example.myquizzapp.core.network.cookie

import com.example.myquizzapp.core.common.cookie.CookieStore
import com.example.myquizzapp.core.common.result.Result
import com.example.myquizzapp.core.network.api.AuthApiService
import dagger.Lazy                          // ← GỐC LỖI: phải là dagger.Lazy, KHÔNG phải kotlin.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val authApi: Lazy<AuthApiService>,   // Lazy phá vòng lặp DI: Retrofit → OkHttp → Authenticator → Retrofit
    private val cookieStore: CookieStore
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 401 từ chính auth endpoints (login/refresh...) = credential sai → để UI xử lý, không retry
        if (response.request.url.encodedPath.contains("/auth/")) return null
        // Đã retry 1 lần mà vẫn 401 → bỏ, tránh vòng lặp vô hạn
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            // Gán ra biến trước khi when — giúp compiler infer kiểu chắc chắn
            val refreshResult: Result<Unit> = runBlocking { authApi.get().refresh() }
            return when (refreshResult) {
                is Result.Success -> response.request  // cookie mới đã vào jar → retry, jar tự gắn
                is Result.Error -> {
                    runBlocking { cookieStore.clear() } // session chết hẳn → UI nhận 401 → về Login
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}