package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

sealed interface CreateRoomEffect {
    data class NavigateToHostLobby(
        val gameId: Long,
        val socketToken: String,
        val sessionCode: String
    ) : CreateRoomEffect
    data object RequireAuthentication : CreateRoomEffect
}
