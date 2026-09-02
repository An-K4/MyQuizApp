package android.kma.myquizzapp.feature.quiz_manage.presentation.createroom

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.model.CreateGameSessionParams
import android.kma.myquizzapp.core.common.model.GameConfigKey
import android.kma.myquizzapp.core.common.model.GameMode
import android.kma.myquizzapp.core.common.model.GameSession
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.validator.RoomSettingsValidator
import android.kma.myquizzapp.core.common.validator.ValidationResult
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.CreateGameSessionUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetGameModesUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.usecase.GetHostTokenUseCase
import android.kma.myquizzapp.feature.quiz_manage.domain.util.buildGameConfigPatch
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRoomViewModel @Inject constructor(
    private val getGameModes: GetGameModesUseCase,
    private val createGameSession: CreateGameSessionUseCase,
    private val getHostToken: GetHostTokenUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = checkNotNull(savedStateHandle["quizId"])
    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    private val _effect = Channel<CreateRoomEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadModes()
    }

    fun onIntent(intent: CreateRoomIntent) {
        when (intent) {
            CreateRoomIntent.RetryLoadModes -> loadModes()
            is CreateRoomIntent.SessionNameChanged -> updateSessionName(intent.value)
            is CreateRoomIntent.ModeSelected -> selectMode(intent.mode)
            is CreateRoomIntent.ToggleChanged -> updateBoolean(intent.key, intent.checked)
            is CreateRoomIntent.NumberChanged -> updateNumber(intent.key, intent.value)
            is CreateRoomIntent.ChoiceChanged -> updateChoice(intent.key, intent.value)
            CreateRoomIntent.Submit -> submit()
            CreateRoomIntent.RetryHostToken -> retryHostToken()
            CreateRoomIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadModes() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModes = true, errorMessage = null) }
            when (val result = getGameModes()) {
                is Result.Success -> {
                    val modes = result.data
                    val selected = modes.firstOrNull { it.mode == GameMode.CLASSIC } ?: modes.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoadingModes = false,
                            modes = modes,
                            selectedMode = selected?.mode,
                            modeConfig = selected?.let(RoomConfigForm::fromDescriptor),
                            invalidConfigKeys = emptySet(),
                            errorMessage = if (selected == null) {
                                "Máy chủ chưa cung cấp chế độ chơi nào."
                            } else null
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoadingModes = false, errorMessage = result.error.toUserMessage())
                }
            }
        }
    }

    private fun updateSessionName(value: String) {
        if (!canEditConfig()) return
        _uiState.update {
            it.copy(sessionName = value.take(100), validationErrors = emptyList())
        }
    }

    private fun selectMode(mode: GameMode) {
        if (!canEditConfig()) return
        val descriptor = _uiState.value.modes.firstOrNull { it.mode == mode } ?: return
        _uiState.update {
            it.copy(
                selectedMode = mode,
                modeConfig = RoomConfigForm.fromDescriptor(descriptor),
                invalidConfigKeys = emptySet(),
                validationErrors = emptyList(),
                ignoredFields = emptyList(),
                errorMessage = null
            )
        }
    }

    private fun updateBoolean(key: GameConfigKey, value: Boolean) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                modeConfig = it.modeConfig?.updateBoolean(key, value),
                invalidConfigKeys = it.invalidConfigKeys - key,
                validationErrors = emptyList()
            )
        }
    }

    private fun updateNumber(key: GameConfigKey, value: String) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                modeConfig = it.modeConfig?.updateNumber(key, value.filter { char -> char.isDigit() }.take(4)),
                invalidConfigKeys = it.invalidConfigKeys - key,
                validationErrors = emptyList()
            )
        }
    }

    private fun updateChoice(key: GameConfigKey, value: String) {
        if (!canEdit(key)) return
        _uiState.update {
            it.copy(
                modeConfig = it.modeConfig?.updateChoice(key, value),
                invalidConfigKeys = it.invalidConfigKeys - key,
                validationErrors = emptyList()
            )
        }
    }

    private fun canEditConfig(): Boolean {
        val state = _uiState.value
        return state.pendingSession == null && !state.isSubmitting
    }

    private fun canEdit(key: GameConfigKey): Boolean =
        canEditConfig() && key in _uiState.value.selectedDescriptor?.editable.orEmpty()

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        state.pendingSession?.let {
            requestHostToken(it)
            return
        }

        val descriptor = state.selectedDescriptor ?: return
        val form = state.modeConfig ?: return
        val invalidKeys = form.invalidKeys(descriptor)
        val errors = buildList {
            // Validate room name using RoomSettingsValidator (Pattern C)
            val nameResult = RoomSettingsValidator.validateRoomName(state.sessionName)
            if (nameResult is ValidationResult.Error) {
                add(nameResult.message)
            }
            if (invalidKeys.isNotEmpty()) {
                add("Hãy kiểm tra lại các cấu hình đang được đánh dấu.")
            }
        }
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(validationErrors = errors, invalidConfigKeys = invalidKeys)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, errorMessage = null, validationErrors = emptyList())
            }
            val params = CreateGameSessionParams(
                quizId = quizId,
                sessionName = state.sessionName.trim(),
                mode = descriptor.mode,
                configPatch = buildGameConfigPatch(descriptor, form.values())
            )
            when (val result = createGameSession(params)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            pendingSession = result.data.session,
                            ignoredFields = result.data.ignored,
                            isSubmitting = false
                        )
                    }
                    requestHostToken(result.data.session)
                }
                is Result.Error -> handleError(result.error)
            }
        }
    }

    private fun retryHostToken() {
        val session = _uiState.value.pendingSession ?: return
        if (!_uiState.value.isSubmitting) requestHostToken(session)
    }

    private fun requestHostToken(session: GameSession) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = getHostToken(session.id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.send(
                        CreateRoomEffect.NavigateToHostLobby(
                            gameId = session.id,
                            socketToken = result.data,
                            sessionCode = session.sessionCode
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = "Phòng ${session.sessionCode} đã được tạo, nhưng chưa lấy được quyền host. " +
                                "Hãy thử lại; ứng dụng sẽ không tạo phòng thứ hai."
                        )
                    }
                    if (result.error is AppError.Unauthorized) {
                        _effect.send(CreateRoomEffect.RequireAuthentication)
                    }
                }
            }
        }
    }

    private suspend fun handleError(error: AppError) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = error.toUserMessage()) }
        if (error is AppError.Unauthorized) {
            _effect.send(CreateRoomEffect.RequireAuthentication)
        }
    }
}
