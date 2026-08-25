package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.ResetTicket
import android.kma.myquizzapp.core.common.model.ResetTicketStatus
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.result.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, fullname: String, phone: String?): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout(): Result<Unit>
    
    /**
     * Checks if user is authenticated by verifying cookies with backend.
     * Returns true if cookies exist AND backend confirms valid session.
     */
    suspend fun isAuthenticated(): Boolean
    
    // N16.5 — luồng reset 3 bước thật (user.route.ts): xin mã → verify (OTP hoặc
    // token deep link) → đổi pass bằng ticket. 2 method resetPasswordWith* cũ gọi
    // endpoint không tồn tại nên đã bị xóa.
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun verifyResetWithOtp(email: String, otp: String): Result<ResetTicket>
    suspend fun verifyResetWithToken(token: String): Result<ResetTicket>
    suspend fun getResetTicket(ticket: String): Result<ResetTicketStatus>
    suspend fun completeReset(ticket: String, newPassword: String): Result<Unit>
}