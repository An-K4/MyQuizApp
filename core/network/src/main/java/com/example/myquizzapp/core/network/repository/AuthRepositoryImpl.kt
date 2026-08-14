package com.example.myquizzapp.core.network.repository

import com.example.myquizzapp.core.common.cookie.CookieStore
import com.example.myquizzapp.core.common.model.User
import com.example.myquizzapp.core.common.repository.AuthRepository
import com.example.myquizzapp.core.common.result.Result
import com.example.myquizzapp.core.common.result.map
import com.example.myquizzapp.core.network.api.AuthApiService
import com.example.myquizzapp.core.network.api.UserApiService
import com.example.myquizzapp.core.network.dto.GoogleOneTapRequest
import com.example.myquizzapp.core.network.dto.LoginRequest
import com.example.myquizzapp.core.network.dto.RegisterRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val userApi: UserApiService,
    private val cookieStore: CookieStore, // interface ở core:common — dùng cho logout
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        // Cookie đã được PersistentCookieJar.saveFromResponse() lưu vào Room
        // TRƯỚC khi dòng này chạy — repository không cần động vào token.
        authApi.login(LoginRequest(email, password)).map { it.user.toDomain() }

    override suspend fun register(
        email: String, password: String, fullname: String, phone: String?,
    ): Result<User> {
        // ⚠️ HỢP ĐỒNG QUAN TRỌNG: POST /auth/register KHÔNG set cookie (xác minh trong
        // auth.controller.ts — chỉ trả 201 + { user }). Đăng ký xong phải login ngay
        // để CookieJar nhận cookie, nếu không user sẽ bị hất ra màn Login.
        val reg = authApi.register(
            RegisterRequest(
                email,
                password,
                fullname,
                phone?.takeIf { it.isNotBlank() })
        )
        if (reg is Result.Error) return reg
        return login(email, password)
    }

    override suspend fun loginWithGoogle(idToken: String): Result<User> =
        authApi.loginWithGoogle(GoogleOneTapRequest(idToken)).map { it.user.toDomain() }

    override suspend fun getCurrentUser(): Result<User> =
        userApi.getMe().map { it.user.toDomain() }

    override suspend fun logout(): Result<Unit> {
        val result = authApi.logout() // server revoke refresh token + clearCookie
        // Dù call server thất bại (mất mạng...) vẫn dọn local — trạng thái "đã logout"
        // phía client là quan trọng nhất. CookieStore.clear() bạn đã thêm ở Tuần 1.
        cookieStore.clear()
        return result
    }
}