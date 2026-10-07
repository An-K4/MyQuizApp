package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.PresignResult
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.UserProfilePatch
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.core.common.repository.SessionRepository
import android.kma.myquizzapp.core.common.repository.StorageRepository
import android.kma.myquizzapp.core.common.repository.UserRepository
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.profile.AvatarUpdateResult
import android.kma.myquizzapp.domain.profile.ProfileDraft
import android.kma.myquizzapp.domain.profile.SaveProfileUseCase
import android.kma.myquizzapp.domain.profile.UpdateAvatarUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileUseCasesTest {
    @Test fun `delta does not send unchanged phone and blank explicitly clears fields`() {
        val draft = ProfileDraft.from(profileUser())
        assertTrue(draft.patch().isEmpty)
        assertNull(draft.copy(fullname = "New Name").patch().phone)
        val clear = draft.copy(phone = "  ", description = "  ").patch()
        assertEquals("", clear.phone)
        assertEquals("", clear.description)
        assertNull(clear.fullname)
    }

    @Test fun `validation follows backend bounds and only validates changed legacy fields`() {
        val draft = ProfileDraft.from(profileUser())
        assertNotNull(draft.copy(fullname = "a").errors().fullname)
        assertNotNull(draft.copy(fullname = "a".repeat(101)).errors().fullname)
        assertNull(draft.copy(phone = "+1234567").errors().phone)
        assertNull(draft.copy(phone = "").errors().phone)
        assertNotNull(draft.copy(phone = "123456").errors().phone)
        assertNotNull(draft.copy(phone = "1234567890123456").errors().phone)
        assertNotNull(draft.copy(description = "a".repeat(201)).errors().description)
        assertNull(draft.copy(description = "a".repeat(200)).errors().description)
        val legacy = ProfileDraft.from(profileUser().copy(phone = "legacy-number"))
        assertFalse(legacy.copy(fullname = "New Name").errors().hasErrors)
    }

    @Test fun `profile save publishes server user without an extra get me`() = runTest {
        val users = ProfileTestUsers()
        val session = ProfileTestSession()
        val result = SaveProfileUseCase(users, session)(
            ProfileDraft.from(profileUser()).copy(fullname = "  New Name  "), checkNotNull(session.captureUserSession())
        )
        assertTrue(result is Result.Success)
        assertEquals(UserProfilePatch(fullname = "New Name"), users.patches.single())
        assertEquals("New Name", session.state.value.userOrNull?.fullname)
        assertEquals(0, users.getCalls)
    }

    @Test fun `profile failure keeps server user and stale success cannot restore logout`() = runTest {
        val users = ProfileTestUsers()
        val session = ProfileTestSession()
        users.profileHandler = { Result.Error(AppError.Api("AUTH_PHONE_TAKEN")) }
        val useCase = SaveProfileUseCase(users, session)
        val draft = ProfileDraft.from(profileUser()).copy(fullname = "New Name")
        assertTrue(useCase(draft, checkNotNull(session.captureUserSession())) is Result.Error)
        assertEquals(profileUser(), session.state.value.userOrNull)
        val response = CompletableDeferred<Result<User>>()
        users.profileHandler = { response.await() }
        val token = checkNotNull(session.captureUserSession())
        val request = launch { useCase(draft, token) }
        runCurrent()
        session.onSignedOut()
        response.complete(Result.Success(profileUser().copy(fullname = "New Name")))
        request.join()
        assertEquals(SessionState.Guest, session.state.value)
    }

    @Test fun `avatar uses presign put patch order and only publishes after patch`() = runTest {
        val events = mutableListOf<String>()
        val storage = ProfileTestStorage(events)
        val users = ProfileTestUsers(events)
        val session = ProfileTestSession()
        val result = UpdateAvatarUseCase(storage, users, session)(byteArrayOf(1, 2), checkNotNull(session.captureUserSession()))
        assertEquals(listOf("presign:avatars:image/jpeg:2", "put", "avatar"), events)
        assertEquals(AvatarUpdateResult.Saved, result)
        assertEquals(PROFILE_AVATAR_URL, session.state.value.userOrNull?.avatar)
        assertEquals(0, users.getCalls)
    }

    @Test fun `presign and put failures never send avatar patch`() = runTest {
        val users = ProfileTestUsers()
        val storage = ProfileTestStorage()
        val session = ProfileTestSession()
        val useCase = UpdateAvatarUseCase(storage, users, session)
        val token = checkNotNull(session.captureUserSession())
        storage.presignResult = Result.Error(AppError.Network)
        assertTrue(useCase(byteArrayOf(1), token) is AvatarUpdateResult.Failed)
        assertEquals(0, storage.putCalls)
        storage.presignResult = Result.Success(PresignResult("https://upload.test", PROFILE_AVATAR_URL, "avatars/7/new"))
        storage.putResult = Result.Error(AppError.Server(403))
        assertTrue(useCase(byteArrayOf(1), token) is AvatarUpdateResult.Failed)
        assertEquals(0, users.avatarCalls)
        assertEquals(profileUser().avatar, session.state.value.userOrNull?.avatar)
    }

    @Test fun `ambiguous avatar failure verifies result without replaying patch`() = runTest {
        val users = ProfileTestUsers()
        val session = ProfileTestSession()
        val useCase = UpdateAvatarUseCase(ProfileTestStorage(), users, session)
        users.avatarResult = Result.Error(AppError.Network)
        users.currentResult = Result.Error(AppError.Network)
        val pending = useCase(byteArrayOf(1), checkNotNull(session.captureUserSession())) as AvatarUpdateResult.NeedsVerification
        assertEquals(1, users.avatarCalls)
        users.currentResult = Result.Success(profileUser().copy(avatar = PROFILE_AVATAR_URL))
        assertEquals(AvatarUpdateResult.Saved, useCase.verify(pending.publicUrl, pending.token))
        assertEquals(1, users.avatarCalls)
        assertEquals(PROFILE_AVATAR_URL, session.state.value.userOrNull?.avatar)
    }

    @Test fun `old avatar operation cannot modify another login`() = runTest {
        val users = ProfileTestUsers()
        val session = ProfileTestSession()
        val old = checkNotNull(session.captureUserSession())
        session.onSignedOut(); session.onAuthenticated(profileUser())
        assertEquals(AvatarUpdateResult.SessionChanged, UpdateAvatarUseCase(ProfileTestStorage(), users, session)(byteArrayOf(1), old))
        assertEquals(0, users.avatarCalls)
    }
}

internal const val PROFILE_AVATAR_URL = "https://images.test/new-avatar"
internal fun profileUser() = User(
    id = 7, fullname = "User Name", email = "user@example.com", phone = "+84123456789",
    description = "Old bio", avatar = "https://images.test/old-avatar", createdAt = "2026-10-01", updatedAt = "2026-10-06"
)

internal class ProfileTestSession : SessionRepository {
    private var generation = 1L
    override val state = MutableStateFlow<SessionState>(SessionState.LoggedIn(profileUser()))
    override suspend fun refresh() = Unit
    override fun onAuthenticated(user: User) { generation++; state.value = SessionState.LoggedIn(user) }
    override fun onSignedOut() { generation++; state.value = SessionState.Guest }
    override fun captureUserSession() = state.value.userOrNull?.let { SessionUserToken(it.id, generation) }
    override fun applyUserUpdate(token: SessionUserToken, user: User): Boolean {
        if (captureUserSession() != token || user.id != token.userId) return false
        state.value = SessionState.LoggedIn(user); return true
    }
    override fun applyAvatarUpdate(token: SessionUserToken, avatarUrl: String): Boolean {
        val current = state.value.userOrNull ?: return false
        return applyUserUpdate(token, current.copy(avatar = avatarUrl))
    }
    override suspend fun clearSession(token: SessionUserToken): Boolean = invalidateSession(token)
    override fun invalidateSession(token: SessionUserToken): Boolean {
        if (captureUserSession() != token) return false
        onSignedOut(); return true
    }
}

internal class ProfileTestUsers(private val events: MutableList<String> = mutableListOf()) : UserRepository {
    val patches = mutableListOf<UserProfilePatch>()
    var profileHandler: suspend (UserProfilePatch) -> Result<User> = { patch ->
        Result.Success(profileUser().copy(
            fullname = patch.fullname ?: profileUser().fullname,
            phone = if (patch.phone == null) profileUser().phone else patch.phone?.takeIf { it.isNotEmpty() },
            description = if (patch.description == null) profileUser().description else patch.description?.takeIf { it.isNotEmpty() }
        ))
    }
    var avatarResult: Result<String> = Result.Success(PROFILE_AVATAR_URL)
    var currentResult: Result<User> = Result.Success(profileUser())
    var avatarCalls = 0
    var getCalls = 0
    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = Result.Error(AppError.Api("VALIDATION_ERROR"))
    override suspend fun deactivateAccount(password: String): Result<Unit> = Result.Error(AppError.Api("VALIDATION_ERROR"))
    override suspend fun updateProfile(patch: UserProfilePatch): Result<User> { patches += patch; return profileHandler(patch) }
    override suspend fun updateAvatar(fileUrl: String): Result<String> { events += "avatar"; avatarCalls++; return avatarResult }
    override suspend fun getCurrentUser(): Result<User> { getCalls++; return currentResult }
}

internal class ProfileTestStorage(private val events: MutableList<String> = mutableListOf()) : StorageRepository {
    var presignResult: Result<PresignResult> = Result.Success(PresignResult("https://upload.test", PROFILE_AVATAR_URL, "avatars/7/new"))
    var putResult: Result<Unit> = Result.Success(Unit)
    var putCalls = 0
    override suspend fun presignUpload(contentType: String, folder: String, fileSize: Long): Result<PresignResult> {
        events += "presign:$folder:$contentType:$fileSize"; return presignResult
    }
    override suspend fun uploadBytes(uploadUrl: String, contentType: String, bytes: ByteArray): Result<Unit> {
        events += "put"; putCalls++; return putResult
    }
}
