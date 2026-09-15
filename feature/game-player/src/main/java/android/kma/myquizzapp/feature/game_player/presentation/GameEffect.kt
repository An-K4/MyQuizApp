package android.kma.myquizzapp.feature.game_player.presentation

sealed interface GameEffect {
    data class Exit(val message: String?) : GameEffect
    data class NavigateToFinalResult(val gameId: Long, val playerId: Long) : GameEffect
}
