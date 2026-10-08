package android.kma.myquizzapp

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.profile.ObserveProfileSessionUseCase
import android.kma.myquizzapp.domain.profile.SaveProfileUseCase
import android.kma.myquizzapp.domain.profile.UpdateAvatarUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.LogoutUseCase
import android.kma.myquizzapp.presentation.profile.AvatarImagePreparer
import android.kma.myquizzapp.presentation.profile.ProfileIntent
import android.kma.myquizzapp.presentation.profile.ProfileViewModel
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `session refresh changes displayed user but does not overwrite editing draft`() = runTest(dispatcher) {
        val fixture = fixture()
        runCurrent()
        fixture.vm.onIntent(ProfileIntent.EditProfile)
        fixture.vm.onIntent(ProfileIntent.ChangeDescription("Unsaved bio"))
        fixture.session.applyUserUpdate(checkNotNull(fixture.session.captureUserSession()), profileUser().copy(fullname = "Server Name"))
        runCurrent()
        assertEquals("Server Name", fixture.vm.uiState.value.user?.fullname)
        assertEquals("Unsaved bio", fixture.vm.uiState.value.draft?.description)
        fixture.vm.onIntent(ProfileIntent.RequestCloseEdit)
        assertTrue(fixture.vm.uiState.value.showDiscardConfirmation)
        fixture.vm.onIntent(ProfileIntent.DiscardEdits)
        assertNull(fixture.vm.uiState.value.draft)
    }

    @Test fun `failed save retains draft and duplicate submit is blocked`() = runTest(dispatcher) {
        val fixture = fixture()
        val response = CompletableDeferred<Result<android.kma.myquizzapp.core.common.model.User>>()
        fixture.users.profileHandler = { response.await() }
        runCurrent()
        fixture.vm.onIntent(ProfileIntent.EditProfile)
        fixture.vm.onIntent(ProfileIntent.ChangeFullname("New Name"))
        fixture.vm.onIntent(ProfileIntent.SaveProfile)
        fixture.vm.onIntent(ProfileIntent.SaveProfile)
        runCurrent()
        assertEquals(1, fixture.users.patches.size)
        assertTrue(fixture.vm.uiState.value.isSavingProfile)
        response.complete(Result.Error(AppError.Network))
        runCurrent()
        assertFalse(fixture.vm.uiState.value.isSavingProfile)
        assertEquals("New Name", fixture.vm.uiState.value.draft?.fullname)
        assertNotNull(fixture.vm.uiState.value.profileError)
        assertEquals("User Name", fixture.vm.uiState.value.user?.fullname)
    }

    @Test fun `avatar selection only previews then confirm uploads and verification never replays`() = runTest(dispatcher) {
        val fixture = fixture()
        fixture.users.avatarResult = Result.Error(AppError.Network)
        fixture.users.currentResult = Result.Error(AppError.Network)
        runCurrent()
        fixture.vm.onIntent(ProfileIntent.AvatarPicked("content://photo/1", fixture.session.captureUserSession()))
        assertEquals(0, fixture.storage.putCalls)
        fixture.vm.onIntent(ProfileIntent.ConfirmAvatar)
        fixture.vm.onIntent(ProfileIntent.ConfirmAvatar)
        runCurrent()
        assertEquals(1, fixture.users.avatarCalls)
        assertNotNull(fixture.vm.uiState.value.pendingAvatar)
        assertFalse(fixture.vm.uiState.value.canEdit)
        fixture.users.currentResult = Result.Success(profileUser().copy(avatar = PROFILE_AVATAR_URL))
        fixture.vm.onIntent(ProfileIntent.VerifyAvatar)
        runCurrent()
        assertEquals(1, fixture.users.avatarCalls)
        assertEquals(PROFILE_AVATAR_URL, fixture.vm.uiState.value.user?.avatar)
        assertNull(fixture.vm.uiState.value.selectedAvatarUri)
    }

    @Test fun `logout clears draft and stale picker result is ignored even for same account`() = runTest(dispatcher) {
        val fixture = fixture()
        runCurrent()
        val old = fixture.session.captureUserSession()
        fixture.vm.onIntent(ProfileIntent.EditProfile)
        fixture.vm.onIntent(ProfileIntent.ChangeFullname("Unsaved"))
        fixture.session.onSignedOut()
        runCurrent()
        assertEquals(SessionState.Guest, fixture.vm.uiState.value.session)
        assertNull(fixture.vm.uiState.value.draft)
        fixture.session.onAuthenticated(profileUser())
        runCurrent()
        fixture.vm.onIntent(ProfileIntent.AvatarPicked("content://old", old))
        assertNull(fixture.vm.uiState.value.selectedAvatarUri)
    }

    @Test fun `draft is restored for the same user after recreation`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val first = fixture(handle)
        runCurrent()
        first.vm.onIntent(ProfileIntent.EditProfile)
        first.vm.onIntent(ProfileIntent.ChangeDescription("Restored bio"))
        val second = fixture(handle)
        runCurrent()
        assertEquals("Restored bio", second.vm.uiState.value.draft?.description)
    }

    @Test fun `duplicate phone error is attached to field`() = runTest(dispatcher) {
        val fixture = fixture()
        fixture.users.profileHandler = { Result.Error(AppError.Api("AUTH_PHONE_TAKEN")) }
        runCurrent()
        fixture.vm.onIntent(ProfileIntent.EditProfile)
        fixture.vm.onIntent(ProfileIntent.ChangePhone("+84999999999"))
        fixture.vm.onIntent(ProfileIntent.SaveProfile)
        runCurrent()
        assertNotNull(fixture.vm.uiState.value.phoneServerError)
        assertNull(fixture.vm.uiState.value.profileError)
        assertNotNull(fixture.vm.uiState.value.draft)
    }

    private fun fixture(handle: SavedStateHandle = SavedStateHandle()): Fixture {
        val session = ProfileTestSession()
        val users = ProfileTestUsers()
        val storage = ProfileTestStorage()
        val preparer = mockk<AvatarImagePreparer>()
        coEvery { preparer.prepare(any()) } returns Result.Success(byteArrayOf(1, 2))
        val logout = mockk<LogoutUseCase>()
        coEvery { logout.invoke() } coAnswers { session.onSignedOut(); Result.Success(Unit) }
        return Fixture(ProfileViewModel(ObserveProfileSessionUseCase(session), logout, SaveProfileUseCase(users, session),
            UpdateAvatarUseCase(storage, users, session), preparer, handle), session, users, storage)
    }

    private data class Fixture(
        val vm: ProfileViewModel, val session: ProfileTestSession,
        val users: ProfileTestUsers, val storage: ProfileTestStorage
    )
}
