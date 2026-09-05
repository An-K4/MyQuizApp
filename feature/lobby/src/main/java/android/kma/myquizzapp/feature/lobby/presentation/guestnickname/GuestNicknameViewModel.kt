package android.kma.myquizzapp.feature.lobby.presentation.guestnickname

import android.kma.myquizzapp.core.common.error.AppError
import android.kma.myquizzapp.core.common.error.toUserMessage
import android.kma.myquizzapp.core.common.result.Result
import android.kma.myquizzapp.core.common.validator.NicknameValidator
import android.kma.myquizzapp.core.common.validator.ValidationResult
import android.kma.myquizzapp.feature.lobby.domain.usecase.JoinGameUseCase
import android.kma.myquizzapp.feature.lobby.domain.usecase.LookupRoomUseCase
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

/**
 * ViewModel màn nhập tên của khách.
 *
 * Tra phòng một lần nữa ngay trước khi join vì hai lý do:
 * 1. Response của `POST /join` chỉ có `{ player, socketToken }`, KHÔNG có id phiên
 *    chơi — mà màn lobby lại cần `gameId` để điều hướng.
 * 2. Người dùng có thể ngồi gõ tên cả phút; trong lúc đó host có thể tắt chế độ
 *    cho khách, phòng đầy, hoặc trận đã bắt đầu.
 *
 * Validate tên bằng [NicknameValidator] (1–50 ký tự) để khớp chính xác giới hạn
 * của backend, thay vì để server trả VALIDATION_ERROR chung chung.
 */
@HiltViewModel
class GuestNicknameViewModel @Inject constructor(
    private val joinGame: JoinGameUseCase,
    private val lookupRoom: LookupRoomUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionCode: String = checkNotNull(savedStateHandle["sessionCode"])

    private val _uiState = MutableStateFlow(GuestNicknameUiState(sessionCode = sessionCode))
    val uiState: StateFlow<GuestNicknameUiState> = _uiState.asStateFlow()

    private val _effect = Channel<GuestNicknameEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: GuestNicknameIntent) {
        when (intent) {
            is GuestNicknameIntent.NicknameChanged -> _uiState.update {
                it.copy(nickname = intent.value, nicknameError = null)
            }

            GuestNicknameIntent.Submit -> submit()

            GuestNicknameIntent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val validation = NicknameValidator.validate(state.nickname)
        if (validation is ValidationResult.Error) {
            _uiState.update { it.copy(nicknameError = validation.message) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            when (val lookup = lookupRoom(sessionCode)) {
                is Result.Error -> fail(lookup.error)

                is Result.Success -> {
                    val room = lookup.data
                    when {
                        !room.allowGuests ->
                            exit("Phòng này vừa tắt chế độ cho khách tham gia")
                        !room.isOpenForJoin -> exit("Phòng không còn nhận người chơi")
                        room.isFull -> exit("Phòng đã đủ ${room.maxPlayers} người")

                        else -> when (
                            val result = joinGame(
                                sessionCode = sessionCode,
                                nickname = state.nickname
                            )
                        ) {
                            is Result.Success -> _effect.send(
                                GuestNicknameEffect.NavigateToPlayerLobby(
                                    gameId = room.gameId,
                                    playerId = result.data.player.id,
                                    socketToken = result.data.socketToken
                                )
                            )

                            is Result.Error -> fail(result.error)
                        }
                    }
                }
            }
        }
    }

    private fun fail(error: AppError) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = error.toUserMessage()) }
    }

    private suspend fun exit(message: String) {
        _uiState.update { it.copy(isSubmitting = false) }
        _effect.send(GuestNicknameEffect.ExitWithMessage(message))
    }
}
