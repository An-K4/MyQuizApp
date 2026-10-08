package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.core.common.error.hasApiCode
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.domain.profile.ObserveProfileSessionUseCase
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.domain.profile.AvatarUpdateResult
import android.kma.myquizzapp.domain.profile.ProfileDraft
import android.kma.myquizzapp.domain.profile.SaveProfileUseCase
import android.kma.myquizzapp.domain.profile.UpdateAvatarUseCase
import android.kma.myquizzapp.feature.auth.domain.usecase.LogoutUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeSession: ObserveProfileSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val updateAvatar: UpdateAvatarUseCase,
    private val imagePreparer: AvatarImagePreparer,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var actionJob: Job? = null
    private var ownerToken: SessionUserToken? = null
    private var restored = false

    init {
        viewModelScope.launch {
            observeSession().collect { reconcileSession() }
        }
    }

    private fun reconcileSession() {
        val current = observeSession.current()
        val token = observeSession.captureToken()
        if (token != ownerToken) {
            actionJob?.cancel()
            val draft = if (!restored && token != null) restoreDraft(current.userOrNull) else null
            if (token != null) restored = true
            if (draft == null) clearDraft()
            ownerToken = token
            _uiState.value = ProfileUiState(session = current, sessionToken = token, draft = draft)
        } else _uiState.update { it.copy(session = current) }
    }

    fun onIntent(intent: ProfileIntent) {
        reconcileSession() // Read SSOT now, not the last Compose frame.
        when (intent) {
            ProfileIntent.EditProfile -> if (_uiState.value.canEdit) {
                val user = _uiState.value.user ?: return
                setDraft(ProfileDraft.from(user))
            }
            is ProfileIntent.ChangeFullname -> changeDraft { copy(fullname = intent.value) }
            is ProfileIntent.ChangePhone -> changeDraft { copy(phone = intent.value) }
            is ProfileIntent.ChangeDescription -> changeDraft { copy(description = intent.value) }
            ProfileIntent.SaveProfile -> save()
            ProfileIntent.RequestCloseEdit -> if (!_uiState.value.isBusy) {
                if (_uiState.value.draft?.isDirty == true) _uiState.update { it.copy(showDiscardConfirmation = true) }
                else discardDraft()
            }
            ProfileIntent.KeepEditing -> _uiState.update { it.copy(showDiscardConfirmation = false) }
            ProfileIntent.DiscardEdits -> if (!_uiState.value.isBusy) discardDraft()
            is ProfileIntent.AvatarPicked -> if (_uiState.value.canEdit && intent.token == ownerToken && ownerToken != null) {
                _uiState.update { it.copy(selectedAvatarUri = intent.uri, avatarError = null) }
            }
            ProfileIntent.ConfirmAvatar -> uploadAvatar()
            ProfileIntent.CancelAvatar -> if (!_uiState.value.isBusy && _uiState.value.pendingAvatar == null) {
                _uiState.update { it.copy(selectedAvatarUri = null, avatarError = null) }
            }
            ProfileIntent.VerifyAvatar -> verifyAvatar()
            ProfileIntent.Logout -> logout()
        }
    }

    private fun changeDraft(change: ProfileDraft.() -> ProfileDraft) {
        if (_uiState.value.isBusy) return
        val draft = _uiState.value.draft ?: return
        setDraft(draft.change())
    }

    private fun setDraft(draft: ProfileDraft) {
        _uiState.update { it.copy(draft = draft, profileError = null, phoneServerError = null) }
        savedState["profileDraftOwner"] = ownerToken?.userId
        savedState["profileDraft"] = arrayListOf(
            draft.originalFullname, draft.originalPhone, draft.originalDescription,
            draft.fullname, draft.phone, draft.description
        )
    }

    private fun restoreDraft(user: User?): ProfileDraft? {
        if (user == null || savedState.get<Long>("profileDraftOwner") != user.id) return null
        val values = savedState.get<ArrayList<String>>("profileDraft") ?: return null
        if (values.size != 6) return null
        return ProfileDraft(values[0], values[1], values[2], values[3], values[4], values[5])
    }

    private fun clearDraft() {
        savedState.remove<Long>("profileDraftOwner")
        savedState.remove<ArrayList<String>>("profileDraft")
    }

    private fun discardDraft() {
        clearDraft()
        _uiState.update { it.copy(draft = null, profileError = null, phoneServerError = null, showDiscardConfirmation = false) }
    }

    private fun save() {
        val state = _uiState.value
        val draft = state.draft ?: return
        val token = ownerToken ?: return
        if (state.isBusy || !draft.isDirty || draft.errors().hasErrors) return
        _uiState.update { it.copy(isSavingProfile = true, profileError = null, phoneServerError = null) }
        actionJob = viewModelScope.launch {
            val result = saveProfile(draft, token)
            if (observeSession.captureToken() != token) return@launch
            when (result) {
                is Result.Success -> {
                    discardDraft()
                    _effect.send(ProfileEffect.ShowMessage("Đã cập nhật hồ sơ"))
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        profileError = if (result.error.hasApiCode("AUTH_PHONE_TAKEN")) null else result.error.toUserMessage(),
                        phoneServerError = if (result.error.hasApiCode("AUTH_PHONE_TAKEN")) "Số điện thoại này đã được sử dụng" else null
                    )
                }
            }
            _uiState.update { it.copy(isSavingProfile = false) }
        }
    }

    private fun uploadAvatar() {
        val state = _uiState.value
        val uri = state.selectedAvatarUri ?: return
        val token = ownerToken ?: return
        if (state.isBusy || state.pendingAvatar != null) return
        _uiState.update { it.copy(isUpdatingAvatar = true, avatarError = null) }
        actionJob = viewModelScope.launch {
            val outcome = when (val prepared = imagePreparer.prepare(uri)) {
                is Result.Success -> updateAvatar(prepared.data, token)
                is Result.Error -> AvatarUpdateResult.Failed(prepared.error)
            }
            handleAvatarResult(outcome, token)
        }
    }

    private fun verifyAvatar() {
        val pending = _uiState.value.pendingAvatar ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isUpdatingAvatar = true, avatarError = null) }
        actionJob = viewModelScope.launch {
            handleAvatarResult(updateAvatar.verify(pending.publicUrl, pending.token), pending.token)
        }
    }

    private suspend fun handleAvatarResult(result: AvatarUpdateResult, token: SessionUserToken) {
        if (observeSession.captureToken() != token) return
        when (result) {
            AvatarUpdateResult.Saved -> {
                _uiState.update { it.copy(selectedAvatarUri = null, pendingAvatar = null, isUpdatingAvatar = false, avatarError = null) }
                _effect.send(ProfileEffect.ShowMessage("Đã cập nhật ảnh đại diện"))
            }
            is AvatarUpdateResult.NeedsVerification -> _uiState.update {
                it.copy(isUpdatingAvatar = false, pendingAvatar = result,
                    avatarError = "Chưa xác định được kết quả lưu ảnh. Hãy kiểm tra lại; ứng dụng không gửi lại lệnh lưu.")
            }
            is AvatarUpdateResult.Failed -> _uiState.update {
                it.copy(isUpdatingAvatar = false, pendingAvatar = null, avatarError = result.error.toUserMessage())
            }
            AvatarUpdateResult.SessionChanged -> reconcileSession()
        }
    }

    fun logout() {
        reconcileSession()
        if (_uiState.value.isBusy) return
        // Explicit logout is available even while an avatar result awaits reconciliation.
        actionJob?.cancel()
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            logoutUseCase()
            _effect.send(ProfileEffect.NavigateBack)
        }
    }
}
