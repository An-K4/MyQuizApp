package android.kma.myquizzapp.core.common.repository

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
    
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resetPasswordWithToken(token: String, newPassword: String): Result<Unit>
    suspend fun resetPasswordWithOtp(email: String, otp: String, newPassword: String): Result<Unit>
}