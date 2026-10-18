package android.kma.myquizzapp.feature.lobby.presentation.guestnickname

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import android.kma.myquizzapp.core.common.validator.NicknameValidator
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.Primary
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful boundary: collects navigation effects and owns scrolling/Snackbar state.
 *
 * @param onExitWithMessage phòng không còn vào được — quay về Trang chủ kèm lý do.
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
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onIntent(GuestNicknameIntent.ErrorShown)
    }

    GuestNicknameScreenContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        scrollState = scrollState,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    )
}

/** Stateless content: previews supply sample state and scrolling, with no ViewModel or effects. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestNicknameScreenContent(
    uiState: GuestNicknameUiState,
    onIntent: (GuestNicknameIntent) -> Unit,
    onBack: () -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = GuestNicknameColors.page,
        contentColor = GuestNicknameColors.foreground,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = { Text("Gia nhập phòng chơi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GuestNicknameColors.page,
                    titleContentColor = GuestNicknameColors.foreground,
                    navigationIconContentColor = GuestNicknameColors.foreground,
                ),
            )
        },
    ) { innerPadding ->
        // Fill the viewport to center a short card; grow and scroll when content is taller.
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GuestNicknameCard(
                state = uiState,
                onNicknameChange = { onIntent(GuestNicknameIntent.NicknameChanged(it)) },
                onSubmit = { onIntent(GuestNicknameIntent.Submit) },
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GuestNicknameCard(
    state: GuestNicknameUiState,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = GuestNicknameColors.surface,
        contentColor = GuestNicknameColors.foreground,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.sessionCode.isNotBlank()) {
                Surface(shape = RoundedCornerShape(50), color = GuestNicknameColors.tint) {
                    Text(
                        text = "Mã phòng: ${state.sessionCode}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            GuestNicknameField(
                nickname = state.nickname,
                error = state.nicknameError,
                enabled = !state.isSubmitting,
                canSubmit = state.canSubmit,
                onNicknameChange = onNicknameChange,
                onSubmit = onSubmit,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GuestNicknameColors.tint,
            ) {
                Text(
                    text = "Bạn đang tham gia với tư cách Khách. Tên này được dùng để hiển thị bạn trong phòng chơi.",
                    modifier = Modifier.padding(12.dp),
                    color = GuestNicknameColors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            QuizPrimaryButton(
                text = "Vào phòng chơi 🚀",
                onClick = onSubmit,
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GuestNicknameField(
    nickname: String,
    error: String?,
    enabled: Boolean,
    canSubmit: Boolean,
    onNicknameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nhập biệt danh của bạn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            enabled = enabled,
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Biệt danh của bạn" },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            placeholder = {
                Text(
                    "VD: Siêu Nhân Trí Tuệ",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (canSubmit) onSubmit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = GuestNicknameColors.foreground,
                unfocusedTextColor = GuestNicknameColors.foreground,
                disabledTextColor = GuestNicknameColors.muted,
                focusedContainerColor = GuestNicknameColors.surface,
                unfocusedContainerColor = GuestNicknameColors.surface,
                disabledContainerColor = GuestNicknameColors.page,
                unfocusedBorderColor = GuestNicknameColors.border,
                disabledBorderColor = GuestNicknameColors.border,
                focusedPlaceholderColor = GuestNicknameColors.muted,
                unfocusedPlaceholderColor = GuestNicknameColors.muted,
            ),
        )
        // The validator and request use trim(); don't truncate the raw editable input.
        val length = nickname.trim().length
        Text(
            text = "Tối đa ${NicknameValidator.MAX_LENGTH} ký tự • $length/${NicknameValidator.MAX_LENGTH}",
            style = MaterialTheme.typography.bodySmall,
            color = GuestNicknameColors.muted,
            textAlign = TextAlign.Center,
        )
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Approved light palette; dark is a provisional Material fallback. */
private object GuestNicknameColors {
    private val dark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val page: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.background else FrontendColors.SoftBackground
    val surface: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.surface else FrontendColors.PageBackground
    val foreground: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.outline else FrontendColors.Border
    val tint: Color
        @Composable get() = if (dark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
}

/** Local UI samples only; no lookup, join request or real ViewModel. */
class GuestNicknamePreviewProvider : PreviewParameterProvider<GuestNicknameUiState> {
    override val values: Sequence<GuestNicknameUiState>
        get() {
            val empty = GuestNicknameUiState(sessionCode = "AB23CD")
            val filled = empty.copy(nickname = "Khách vui tính")
            return sequenceOf(
                empty,
                filled,
                empty.copy(nickname = "A".repeat(NicknameValidator.MAX_LENGTH)),
                empty.copy(
                    nickname = "A".repeat(NicknameValidator.MAX_LENGTH + 1),
                    nicknameError = "Tên hiển thị không được vượt quá ${NicknameValidator.MAX_LENGTH} ký tự",
                ),
                empty.copy(nickname = "   ", nicknameError = "Tên hiển thị không được để trống"),
                filled.copy(isSubmitting = true),
                filled.copy(errorMessage = "Không thể kết nối máy chủ. Kiểm tra kết nối rồi thử lại."),
                filled.copy(nickname = "   Khách vui tính   "),
            )
        }
}

@Preview(name = "Guest nickname Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Guest nickname Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GuestNicknameScreenContentPreview(
    @PreviewParameter(GuestNicknamePreviewProvider::class) state: GuestNicknameUiState,
) {
    MyQuizAppTheme {
        GuestNicknameScreenContent(
            uiState = state,
            onIntent = {},
            onBack = {},
            scrollState = rememberScrollState(),
            // Render an error sample directly; Preview must not run production Snackbar effects.
            snackbarHost = {
                state.errorMessage?.let { message -> Snackbar { Text(message) } }
            },
        )
    }
}

@Preview(name = "Guest nickname Small", showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Preview(name = "Guest nickname Short viewport", showBackground = true, widthDp = 390, heightDp = 360)
@Composable
private fun GuestNicknameSmallPreview() {
    MyQuizAppTheme {
        GuestNicknameScreenContent(
            uiState = GuestNicknameUiState(sessionCode = "AB23CD", nickname = "Khách vui tính"),
            onIntent = {},
            onBack = {},
            scrollState = rememberScrollState(),
        )
    }
}
