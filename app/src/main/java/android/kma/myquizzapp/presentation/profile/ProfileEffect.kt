package android.kma.myquizzapp.presentation.profile

sealed interface ProfileEffect {
    data object NavigateBack : ProfileEffect
    data class ShowError(val message: String) : ProfileEffect
    data class ShowMessage(val message: String) : ProfileEffect
}
