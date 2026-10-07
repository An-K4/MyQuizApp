package android.kma.myquizzapp.presentation.profile

import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.common.model.SessionUserToken
import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.userOrNull
import android.kma.myquizzapp.domain.profile.AvatarUpdateResult
import android.kma.myquizzapp.domain.profile.ProfileDraft

data class ProfileUiState(
    val session: SessionState = SessionState.Unknown,
    val sessionToken: SessionUserToken? = null,
    val draft: ProfileDraft? = null,
    val isSavingProfile: Boolean = false,
    val profileError: String? = null,
    val phoneServerError: String? = null,
    val showDiscardConfirmation: Boolean = false,
    val selectedAvatarUri: String? = null,
    val isUpdatingAvatar: Boolean = false,
    val avatarError: String? = null,
    val pendingAvatar: AvatarUpdateResult.NeedsVerification? = null,
    val isLoggingOut: Boolean = false
) {
    val user: User? get() = session.userOrNull
    val isLoading: Boolean get() = session is SessionState.Unknown
    val isConfirmedGuest: Boolean get() = session is SessionState.Guest
    val isBusy: Boolean get() = isSavingProfile || isUpdatingAvatar || isLoggingOut
    val canEdit: Boolean get() = user != null && !isBusy && pendingAvatar == null
}

sealed interface ProfileIntent {
    data object EditProfile : ProfileIntent
    data class ChangeFullname(val value: String) : ProfileIntent
    data class ChangePhone(val value: String) : ProfileIntent
    data class ChangeDescription(val value: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object RequestCloseEdit : ProfileIntent
    data object KeepEditing : ProfileIntent
    data object DiscardEdits : ProfileIntent
    // This is a local lifetime marker, NOT an authentication credential.
    data class AvatarPicked(val uri: String, val token: SessionUserToken?) : ProfileIntent
    data object ConfirmAvatar : ProfileIntent
    data object CancelAvatar : ProfileIntent
    data object VerifyAvatar : ProfileIntent
    data object Logout : ProfileIntent
}
