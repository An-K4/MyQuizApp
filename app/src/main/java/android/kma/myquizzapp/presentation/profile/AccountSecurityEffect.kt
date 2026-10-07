package android.kma.myquizzapp.presentation.profile

sealed interface AccountSecurityEffect {
    data object Back : AccountSecurityEffect
    data class Message(val text: String) : AccountSecurityEffect
    data class SessionEnded(val text: String) : AccountSecurityEffect
}
