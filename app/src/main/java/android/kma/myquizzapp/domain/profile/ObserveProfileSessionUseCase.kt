package android.kma.myquizzapp.domain.profile

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.repository.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Profile presentation may read session data only through this use case. */
class ObserveProfileSessionUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke(): StateFlow<SessionState> = session.state
    fun current(): SessionState = session.snapshot().state
    fun captureToken() = session.captureUserSession()
}
