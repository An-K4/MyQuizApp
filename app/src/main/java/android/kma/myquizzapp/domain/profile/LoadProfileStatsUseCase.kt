package android.kma.myquizzapp.domain.profile

import android.kma.myquizzapp.core.common.repository.ProfileStatsRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ProfileStats(
    val participatedSessionCount: Int,
    val createdQuizCount: Int
)

/** Loads exact profile totals concurrently; presentation never derives them from a partial page. */
class LoadProfileStatsUseCase @Inject constructor(
    private val profileStats: ProfileStatsRepository
) {
    suspend operator fun invoke(): Result<ProfileStats> = coroutineScope {
        val sessions = async { profileStats.getParticipatedSessionCount() }
        val quizzes = async { profileStats.getCreatedQuizCount() }

        val sessionCount = when (val result = sessions.await()) {
            is Result.Success -> result.data
            is Result.Error -> return@coroutineScope result
        }
        val quizCount = when (val result = quizzes.await()) {
            is Result.Success -> result.data
            is Result.Error -> return@coroutineScope result
        }
        Result.Success(ProfileStats(sessionCount, quizCount))
    }
}
