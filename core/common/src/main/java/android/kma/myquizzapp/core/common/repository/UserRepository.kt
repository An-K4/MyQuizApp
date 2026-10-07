package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.UserProfilePatch
import android.kma.myquizzapp.core.common.result.Result

interface UserRepository {
    suspend fun updateProfile(patch: UserProfilePatch): Result<User>
    suspend fun updateAvatar(fileUrl: String): Result<String>
    /** Authoritative reconciliation only; presentation continues to read session SSOT. */
    suspend fun getCurrentUser(): Result<User>
}
