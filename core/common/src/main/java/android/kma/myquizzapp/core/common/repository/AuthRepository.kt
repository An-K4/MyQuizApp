package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.result.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, fullname: String, phone: String?): Result<User>
    suspend fun loginWithGoogle(idToken: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout(): Result<Unit>
}