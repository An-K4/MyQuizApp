package android.kma.myquizzapp.feature.game_player.presentation

sealed interface GameIntent {
    data class SelectOption(val id: String) : GameIntent
    data class ToggleOption(val id: String) : GameIntent
    data class ChangeText(val value: String) : GameIntent
    data object Submit : GameIntent
    data object Retry : GameIntent
    data object Sync : GameIntent
    data object Leave : GameIntent
    data object DeadlineReached : GameIntent
    data object ErrorShown : GameIntent
}
