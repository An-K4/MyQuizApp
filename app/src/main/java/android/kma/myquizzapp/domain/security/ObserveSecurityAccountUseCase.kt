package android.kma.myquizzapp.domain.security

import android.kma.myquizzapp.core.common.model.AuthProvider
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SecurityAccount(val token: SessionUserToken?, val loading: Boolean, val googleOnly: Boolean)

/** Read-only application boundary. Presentation never accesses SessionRepository directly. */
class ObserveSecurityAccountUseCase @Inject constructor(private val session: SessionRepository) {
    operator fun invoke(): Flow<SecurityAccount> = session.state.map { current() }
    fun current(): SecurityAccount = SecurityAccount(
        token = session.captureUserSession(),
        loading = session.state.value == SessionState.Unknown,
        googleOnly = session.state.value.userOrNull?.authProvider == AuthProvider.GOOGLE
    )
}
