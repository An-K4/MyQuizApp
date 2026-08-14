package android.kma.myquizzapp.auth.domain.usecase

import android.kma.myquizzapp.core.common.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String) = repo.login(email, password)
}

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    // auto-login đã nằm TRONG AuthRepositoryImpl.register — UseCase không cần biết
    suspend operator fun invoke(email: String, password: String, fullname: String, phone: String?) =
        repo.register(email, password, fullname, phone)
}

class GetCurrentUserUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.getCurrentUser()
}

class LoginWithGoogleUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(idToken: String) = repo.loginWithGoogle(idToken)
}

class LogoutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.logout()
}