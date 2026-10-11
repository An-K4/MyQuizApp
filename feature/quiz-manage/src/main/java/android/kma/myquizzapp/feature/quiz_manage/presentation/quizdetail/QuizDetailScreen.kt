package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.*
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.components.QuizPrimaryButton
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.components.RemoteImage
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.ConfirmDeleteQuizDialog
import android.kma.myquizzapp.feature.quiz_manage.presentation.components.QuestionPreviewCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect

/** Stateful integration only: lifecycle, effects, scrolling and accordion state. */
@Composable
fun QuizDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    onResourceMissing: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var skipFirstResume by rememberSaveable { mutableStateOf(true) }
    // Only deliberate collapses are stored; loaded/new questions start expanded.
    var collapsedQuestionIds by rememberSaveable(
        stateSaver = listSaver<Set<Long>, Long>(save = { it.toList() }, restore = { it.toSet() }),
    ) { mutableStateOf(emptySet<Long>()) }
    val currentOnBack by rememberUpdatedState(onNavigateBack)
    val currentOnResourceMissing by rememberUpdatedState(onResourceMissing)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                QuizDetailEffect.QuizDeleted -> currentOnBack()
                is QuizDetailEffect.ResourceMissing -> currentOnResourceMissing(effect.message)
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipFirstResume) skipFirstResume = false
        else viewModel.onIntent(QuizDetailIntent.LoadQuizDetail)
    }
    LaunchedEffect(uiState.quiz?.questions?.map { it.id }) {
        uiState.quiz?.let { quiz ->
            collapsedQuestionIds = collapsedQuestionIds.intersect(quiz.questions.map { it.id }.toSet())
        }
    }

    QuizDetailScreenContent(
        uiState = uiState,
        listState = listState,
        expandedQuestionIds = uiState.quiz?.questions.orEmpty().map { it.id }.toSet() - collapsedQuestionIds,
        onQuestionExpandedChange = { id, expanded ->
            collapsedQuestionIds = if (expanded) collapsedQuestionIds - id else collapsedQuestionIds + id
        },
        onNavigateBack = onNavigateBack,
        onNavigateToCreateRoom = onNavigateToCreateRoom,
        onNavigateToPreview = onNavigateToPreview,
        onNavigateToEditQuiz = onNavigateToEditQuiz,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreenContent(
    uiState: QuizDetailUiState,
    listState: LazyListState,
    expandedQuestionIds: Set<Long>,
    onQuestionExpandedChange: (Long, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCreateRoom: (Long) -> Unit,
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToEditQuiz: (Long) -> Unit,
    onIntent: (QuizDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground = if (isDark) MaterialTheme.colorScheme.onBackground else FrontendColors.Foreground
    val danger = if (isDark) MaterialTheme.colorScheme.error else FrontendColors.Danger
    val border = if (isDark) MaterialTheme.colorScheme.outlineVariant else FrontendColors.Border
    val quiz = uiState.quiz
    // Do not expose actions on a stale quiz while reloading or after a load error.
    val readyQuiz = quiz?.takeIf { !uiState.isLoading && uiState.error == null }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(quiz?.quizName ?: "Chi tiết quiz", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = foreground, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !uiState.isDeleting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (uiState.isOwner && readyQuiz != null) {
                        IconButton(onClick = { onNavigateToEditQuiz(readyQuiz.id) }, enabled = !uiState.isDeleting) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Chỉnh sửa quiz", tint = foreground)
                        }
                        IconButton(onClick = { onIntent(QuizDetailIntent.DeleteQuizClicked) }, enabled = !uiState.isDeleting) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Xóa quiz", tint = danger)
                        }
                    }
                },
            )
        },
        bottomBar = {
            readyQuiz?.let { currentQuiz ->
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.navigationBarsPadding()) {
                        HorizontalDivider(color = border)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            QuizSecondaryButton(
                                text = "Chơi thử",
                                onClick = { onNavigateToPreview(currentQuiz.id) },
                                enabled = currentQuiz.questions.isNotEmpty() && !uiState.isDeleting,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            QuizPrimaryButton(
                                text = "Tạo phòng",
                                onClick = { onNavigateToCreateRoom(currentQuiz.id) },
                                enabled = !uiState.isDeleting,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> CenterContent(Modifier.padding(innerPadding)) { CircularProgressIndicator() }
            uiState.error != null -> CenterContent(Modifier.padding(innerPadding)) {
                Text(uiState.error, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                QuizSecondaryButton("Thử lại", { onIntent(QuizDetailIntent.Retry) })
            }
            readyQuiz != null -> QuizDetailContent(
                quiz = readyQuiz,
                listState = listState,
                expandedQuestionIds = expandedQuestionIds,
                onQuestionExpandedChange = onQuestionExpandedChange,
                modifier = Modifier.padding(innerPadding),
            )
            else -> CenterContent(Modifier.padding(innerPadding)) { Text("Không có dữ liệu quiz.") }
        }
    }
    if (uiState.isConfirmingDelete && quiz != null) {
        ConfirmDeleteQuizDialog(
            quizName = quiz.quizName,
            deleteError = uiState.deleteError,
            isDeleting = uiState.isDeleting,
            onConfirm = { onIntent(QuizDetailIntent.DeleteQuizConfirmed) },
            onDismiss = { onIntent(QuizDetailIntent.DeleteQuizDismissed) },
        )
    }
}

@Composable
private fun CenterContent(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizDetailContent(
    quiz: Quiz,
    listState: LazyListState,
    expandedQuestionIds: Set<Long>,
    onQuestionExpandedChange: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val foreground = if (isDark) MaterialTheme.colorScheme.onSurface else FrontendColors.Foreground
    val muted = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else FrontendColors.MutedForeground
    val soft = if (isDark) MaterialTheme.colorScheme.surfaceVariant else FrontendColors.SoftBackground
    val tint = if (isDark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint
    val questions = quiz.questions.map { it.toQuestionPreviewUiModel() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "cover") {
            RemoteImage(
                imageUrl = quiz.quizImage, contentDescription = "Ảnh bìa ${quiz.quizName}",
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
            )
        }
        item(key = "title") {
            Text(quiz.quizName, color = foreground,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp))
        }
        item(key = "author") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tint, contentColor = MaterialTheme.colorScheme.primary) {
                    Avatar(quiz.owner?.avatar, contentDescription = null, size = 44.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tạo bởi ${quiz.displayAuthorName()}", color = foreground,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    formatQuizCreatedDate(quiz.createdAt)?.let { date ->
                        Text("Ngày tạo: $date", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item(key = "metadata") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quiz.quizCategory?.takeIf { it.isNotBlank() }?.let { category -> QuizMetadataPill(category) }
                quiz.quizLanguage.takeIf { it.isNotBlank() }?.let { language -> QuizMetadataPill("Ngôn ngữ: $language") }
            }
        }
        item(key = "stats") {
            val cells = buildList {
                add(quiz.displayQuestionCount().toString() to "Câu hỏi")
                quiz.sharedQuestionTimeSeconds()?.let { add("${it}s" to "Mỗi câu hỏi") }
                quiz.playCount?.let { add(it.toString() to "Lượt chơi") }
            }
            Surface(shape = RoundedCornerShape(20.dp), color = soft) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cells.forEach { (value, label) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(value, color = foreground, textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text(label, color = muted, textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        quiz.quizDescription?.takeIf { it.isNotBlank() }?.let { description ->
            item(key = "description") { Text(description, color = muted, style = MaterialTheme.typography.bodyMedium) }
        }
        item(key = "question-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danh sách câu hỏi (${questions.size} câu)", color = foreground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp))
                    Text("Đáp án đúng đã được ẩn", color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (questions.isEmpty()) item(key = "question-empty") {
            Text("Quiz chưa có câu hỏi.", color = muted)
        }
        items(questions, key = { it.id }) { question ->
            QuestionPreviewCard(
                questionText = question.text,
                questionType = question.type,
                timeLimitSeconds = question.timeLimitSeconds,
                imageUrl = question.imageUrl,
                optionTexts = question.optionTexts,
                expanded = question.id in expandedQuestionIds,
                onExpandedChange = { onQuestionExpandedChange(question.id, it) },
            )
        }
    }
}

@Composable
private fun QuizMetadataPill(text: String) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isDark) MaterialTheme.colorScheme.primaryContainer else FrontendColors.BrandTint,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

private fun quizDetailPreviewQuiz(mixedTimes: Boolean = false): Quiz {
    val questions = QuestionType.entries.mapIndexed { index, type ->
        Question(
            id = index + 1L, quizId = 1L, questionType = type,
            questionText = when (type) {
                QuestionType.MULTIPLE_CHOICE -> "Kim tự tháp Giza nằm ở quốc gia nào?"
                QuestionType.MULTIPLE_SELECT -> "Những địa danh nào nằm ở châu Á?"
                QuestionType.SHORT_ANSWER -> "Thủ đô của Nhật Bản là gì?"
                QuestionType.LONG_ANSWER -> "Kể về một nét văn hóa mà bạn yêu thích."
            },
            timeLimit = if (mixedTimes) 30 + index * 15 else 30,
            questionImage = if (type == QuestionType.MULTIPLE_SELECT) "https://preview.invalid/question.png" else null,
            answerOptions = when (type) {
                QuestionType.MULTIPLE_CHOICE -> listOf("Ai Cập", "Nhật Bản", "Pháp", "Ấn Độ")
                QuestionType.MULTIPLE_SELECT -> listOf("Núi Phú Sĩ", "Tháp Eiffel", "Vịnh Hạ Long", "Kim tự tháp Giza")
                else -> null
            }?.mapIndexed { optionIndex, text -> AnswerOption(optionIndex + 1L, text) },
        )
    }
    return Quiz(
        id = 1L, quizOwner = 1L, owner = QuizOwner(1L, "Hoàng Long"),
        quizName = "Khám phá Văn hóa Thế giới", quizLanguage = "vi", quizCategory = "Văn hóa",
        isPublic = true, questionCount = questions.size, playCount = 2400,
        quizDescription = "Bộ câu hỏi tổng hợp những nét đặc sắc về phong tục, ẩm thực và danh lam thắng cảnh trên toàn cầu.",
        createdAt = "2026-10-08T00:00:00Z", updatedAt = "2026-10-08T00:00:00Z", questions = questions,
    )
}

@Composable
private fun QuizDetailPreviewContent(
    uiState: QuizDetailUiState = QuizDetailUiState(quiz = quizDetailPreviewQuiz(), isOwner = true),
    expandedQuestionIds: Set<Long> = uiState.quiz?.questions.orEmpty().map { it.id }.toSet(),
    initialListIndex: Int = 0,
) {
    MyQuizAppTheme {
        QuizDetailScreenContent(
            uiState = uiState,
            listState = rememberLazyListState(initialFirstVisibleItemIndex = initialListIndex),
            expandedQuestionIds = expandedQuestionIds,
            onQuestionExpandedChange = { _, _ -> },
            onNavigateBack = {}, onNavigateToCreateRoom = {}, onNavigateToPreview = {},
            onNavigateToEditQuiz = {}, onIntent = {},
        )
    }
}

@Preview(name = "Quiz Detail Owner Light", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Quiz Detail Owner Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Quiz Detail Large Text", showBackground = true, widthDp = 320, heightDp = 844, fontScale = 1.5f)
@Composable
private fun QuizDetailScreenContentPreview() = QuizDetailPreviewContent()

@Preview(name = "Quiz Detail Visitor", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailVisitorPreview() = QuizDetailPreviewContent(QuizDetailUiState(quiz = quizDetailPreviewQuiz()))

@Preview(name = "Quiz Detail Mixed Times", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailMixedTimesPreview() = QuizDetailPreviewContent(QuizDetailUiState(quiz = quizDetailPreviewQuiz(mixedTimes = true), isOwner = true))

@Preview(name = "Quiz Detail Questions Collapsed", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailQuestionsCollapsedPreview() = QuizDetailPreviewContent(expandedQuestionIds = emptySet(), initialListIndex = 6)

@Preview(name = "Quiz Detail Questions Expanded", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Quiz Detail Questions Expanded Dark", showBackground = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizDetailQuestionsExpandedPreview() = QuizDetailPreviewContent(initialListIndex = 6)

@Preview(name = "Quiz Detail Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailLoadingPreview() = QuizDetailPreviewContent(QuizDetailUiState(isLoading = true))

@Preview(name = "Quiz Detail Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailErrorPreview() = QuizDetailPreviewContent(QuizDetailUiState(error = "Không có kết nối mạng. Vui lòng thử lại."))

@Preview(name = "Quiz Detail Empty Questions", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun QuizDetailEmptyQuestionsPreview() = QuizDetailPreviewContent(QuizDetailUiState(
    quiz = quizDetailPreviewQuiz().copy(questions = emptyList(), questionCount = 0, playCount = null), isOwner = true,
))
