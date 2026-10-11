package android.kma.myquizzapp.core.common.repository

import android.kma.myquizzapp.core.common.result.Result

/** Exact, server-provided summary counts for the signed-in user's profile. */
interface ProfileStatsRepository {
    /** Closed sessions in which the user participated (finished or cancelled). */
    suspend fun getParticipatedSessionCount(): Result<Int>

    /** Non-deleted quizzes owned by the user, across all visibility states. */
    suspend fun getCreatedQuizCount(): Result<Int>
}
