package android.kma.myquizzapp.domain.activity

import android.kma.myquizzapp.core.common.model.SessionIdentity
import android.kma.myquizzapp.core.common.model.SessionSnapshot
import android.kma.myquizzapp.core.common.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Session reads stay behind the domain boundary, never in presentation. */
class ObserveActivitySessionUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke(): Flow<SessionSnapshot> = session.state.map { current() }
    fun current(): SessionSnapshot = session.snapshot()
    fun isCurrent(identity: SessionIdentity): Boolean = current().identity == identity
}
