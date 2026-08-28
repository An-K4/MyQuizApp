package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode

sealed interface CreateRoomIntent {
    data object RetryLoadModes : CreateRoomIntent
    data class SessionNameChanged(val value: String) : CreateRoomIntent
    data class ModeSelected(val mode: GameMode) : CreateRoomIntent
    data class ToggleChanged(val key: GameConfigKey, val checked: Boolean) : CreateRoomIntent
    data class NumberChanged(val key: GameConfigKey, val value: String) : CreateRoomIntent
    data class ChoiceChanged(val key: GameConfigKey, val value: String) : CreateRoomIntent
    data object Submit : CreateRoomIntent
    data object RetryHostToken : CreateRoomIntent
    data object ErrorShown : CreateRoomIntent
}
