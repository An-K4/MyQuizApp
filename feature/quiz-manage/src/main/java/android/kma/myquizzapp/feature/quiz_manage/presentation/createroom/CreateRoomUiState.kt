package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameModeDescriptor
import android.kma.myquizzapp.core.common.model.GameSession
import android.kma.myquizzapp.core.common.model.IgnoredGameConfigField

data class CreateRoomUiState(
    val isLoadingModes: Boolean = true,
    val modes: List<GameModeDescriptor> = emptyList(),
    val selectedMode: GameMode? = null,
    val sessionName: String = "Phòng chơi của tôi",
    val modeConfig: RoomConfigForm? = null,
    val invalidConfigKeys: Set<GameConfigKey> = emptySet(),
    val isSubmitting: Boolean = false,
    val pendingSession: GameSession? = null,
    val ignoredFields: List<IgnoredGameConfigField> = emptyList(),
    val validationErrors: List<String> = emptyList(),
    val errorMessage: String? = null
) {
    val selectedDescriptor: GameModeDescriptor?
        get() = modes.firstOrNull { it.mode == selectedMode }

    val isWaitingForHostToken: Boolean
        get() = pendingSession != null
}
