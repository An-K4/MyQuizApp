package android.kma.myquizzapp.domain.security

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.hasApiCode
import android.kma.myquizzapp.core.common.error.isSessionTerminal

sealed interface SecurityMutationResult {
    data object Completed : SecurityMutationResult
    data object SessionChanged : SecurityMutationResult
    data object SessionEnded : SecurityMutationResult
    data object Uncertain : SecurityMutationResult
    data object CleanupPending : SecurityMutationResult
    data class Rejected(val error: AppError) : SecurityMutationResult
}
internal val AppError.securitySessionTerminal: Boolean
    get() = isSessionTerminal || hasApiCode("USER_NOT_FOUND")
internal val AppError.mutationUncertain: Boolean
    get() = this is AppError.Network || this is AppError.Server || this is AppError.Unknown || hasApiCode("SERVER_ERROR")
