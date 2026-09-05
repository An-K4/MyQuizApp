package android.kma.myquizzapp.feature.lobby.presentation.guestnickname

import android.kma.myquizzapp.core.common.validator.NicknameValidator
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Màn nhập tên hiển thị cho khách (stateful).
 *
 * @param onExitWithMessage phòng không còn vào được — quay về màn nhập mã kèm lý do.
 */
@Composable
fun GuestNicknameScreen(
    onNavigateToPlayerLobby: (gameId: Long, playerId: Long, socketToken: String) -> Unit,
    onExitWithMessage: (message: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GuestNicknameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GuestNicknameEffect.NavigateToPlayerLobby -> onNavigateToPlayerLobby(
                    effect.gameId,
                    effect.playerId,
                    effect.socketToken
                )

                is GuestNicknameEffect.ExitWithMessage -> onExitWithMessage(effect.message)
            }
        }
    }

    GuestNicknameScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestNicknameScreenContent(
    uiState: GuestNicknameUiState,
    onIntent: (GuestNicknameIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onIntent(GuestNicknameIntent.ErrorShown)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tên hiển thị") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Phòng ${uiState.sessionCode} — mọi người sẽ thấy bạn với tên này.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = { onIntent(GuestNicknameIntent.NicknameChanged(it)) },
                label = { Text("Tên hiển thị") },
                singleLine = true,
                isError = uiState.nicknameError != null,
                supportingText = {
                    Text(
                        uiState.nicknameError
                            ?: "${uiState.nickname.length}/${NicknameValidator.MAX_LENGTH}"
                    )
                },
                enabled = !uiState.isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { if (uiState.canSubmit) onIntent(GuestNicknameIntent.Submit) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onIntent(GuestNicknameIntent.Submit) },
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Vào phòng")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuestNicknameScreenContentPreview() {
    MyQuizAppTheme {
        GuestNicknameScreenContent(
            uiState = GuestNicknameUiState(sessionCode = "4829AB", nickname = "Khách vui tính"),
            onIntent = {},
            onBack = {}
        )
    }
}
