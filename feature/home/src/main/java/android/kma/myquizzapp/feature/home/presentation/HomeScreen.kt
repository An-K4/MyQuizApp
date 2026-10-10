package android.kma.myquizzapp.feature.home.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.ui.components.HomeSectionRow
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import android.kma.myquizzapp.core.ui.components.CustomClickableText
import android.kma.myquizzapp.core.ui.components.QuizSecondaryButton
import android.kma.myquizzapp.core.ui.components.RoomCodeEntryCard
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Trang chủ.
 *
 * @param roomCodeCard thẻ nhập mã phòng, chèn ở đầu trang (N19.6).
 * @param onNavigateToDiscover mở màn Khám phá. `sectionKey` != null là "Xem thêm"
 *   của một section cụ thể (màn đích mở sẵn section đó), null là "Khám phá
 *   tất cả" ở cuối trang.
 *
 * Vì sao là slot chứ không phải Home tự gọi: thẻ đó thuộc `feature:lobby` (nó tra
 * cứu phòng rồi điều hướng vào luồng game). Nếu `feature:home` gọi trực tiếp
 * thì hai feature phải phụ thuộc nhau chỉ vì một ô nhập; tầng navigation ở
 * module `app` đã biết cả hai nên nó là chỗ đúng để nối.
 */
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onNavigateToDiscover: (sectionKey: String?, sectionType: String?, title: String?, topic: String?) -> Unit,
    noticeMessage: String? = null,
    onNoticeShown: () -> Unit = {},
    roomCodeCard: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(noticeMessage) {
        noticeMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onNoticeShown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToSearch -> onNavigateToSearch()
                is HomeEffect.NavigateToQuizDetail -> onNavigateToQuizDetail(effect.quizId)
            }
        }
    }

    // N19.6: không còn LifecycleEventEffect(ON_RESUME) để hỏi lại trạng thái đăng
    // nhập — uiState đã collect thẳng nguồn chung nên tự đổi khi đăng nhập/đăng
    // xuất ở bất kỳ đâu.
    HomeScreenContent(
        uiState = uiState,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToAuth = onNavigateToAuth,
        onNavigateToQuizDetail = onNavigateToQuizDetail,
        onNavigateToDiscover = onNavigateToDiscover,
        onRetry = { viewModel.onIntent(HomeIntent.Retry) },
        snackbarHostState = snackbarHostState,
        listState = listState,
        roomCodeCard = roomCodeCard,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNavigateToSearch: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToQuizDetail: (Long) -> Unit,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    onNavigateToDiscover: (sectionKey: String?, sectionType: String?, title: String?, topic: String?) -> Unit = { _, _, _, _ -> },
    roomCodeCard: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeHeader(uiState.showSignInAction, onNavigateToSearch, onNavigateToAuth)
        },
        modifier = modifier
    ) { paddingValues ->
        HomeFeed(
            uiState = uiState,
            onQuizClick = onNavigateToQuizDetail,
            onRetry = onRetry,
            onNavigateToDiscover = onNavigateToDiscover,
            roomCodeCard = roomCodeCard,
            listState = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues).imePadding()
        )
    }
}

/**
 * Góc phải top bar: CHỈ còn lối đăng nhập cho khách.
 *
 * Khi đã đăng nhập thì không hiện gì — avatar đã chuyển xuống tab "Hồ sơ" ở
 * bottom nav (N19.5), để ở cả hai chỗ là dư thừa và làm top bar chật.
 *
 * N19.6 — nhận `Boolean` thay vì `User?`: ở đây chỉ cần biết "có hiện nút hay
 * không", và câu trả lời đó KHÁC với `user == null` — lúc chưa xác định được
 * phiên thì cũng không có user, nhưng không được hiện nút (sẽ nháy rồi biến
 * mất với người đã đăng nhập).
 */
@Composable
private fun HomeHeader(showSignIn: Boolean, onSearch: () -> Unit, onSignIn: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "MyQuizz", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Tìm kiếm", Modifier.size(22.dp)) }
        if (showSignIn) {
            // The visible pill is compact; its clickable outer target remains at least 48dp tall.
            Box(Modifier.heightIn(min = 48.dp).clickable(onClick = onSignIn), contentAlignment = Alignment.Center) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else FrontendColors.BrandTint,
                ) {
                    Text(
                        "Đăng ký/Đăng nhập", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

/**
 * Toàn bộ nội dung cuộn của Trang chủ.
 *
 * Cả trang chỉ có MỘT vùng cuộn, và thẻ nhập mã là `item` đầu tiên của nó nên
 * thẻ CUỘN ĐI khi người dùng lướt xuống. Trước đó thẻ bị ghim cố định phía
 * trên: nó cao gần nửa màn hình nên chiếm chỗ suốt lúc đang xem khu Khám phá.
 *
 * KHÔNG bọc `Column(verticalScroll)` quanh `LazyColumn` để làm việc này: hai
 * vùng cuộn cùng chiều lồng nhau sẽ vỡ vì `LazyColumn` nhận chiều cao vô hạn.
 *
 * Thẻ nhập mã vẫn nằm NGOÀI mọi nhánh trạng thái của `/home`. Bản thân nó
 * không tải gì nên không có cách nào để nó "lỗi" — lỗi chỉ xuất hiện sau khi
 * người dùng bấm vào phòng, và lỗi đó tự hiện inline trong thẻ. Vì vậy khi
 * feed lỗi thì chỉ khối Khám phá đổi thành placeholder, lối vào phòng của
 * người vừa được bạn bè gửi mã không bị chặn.
 */
@Composable
private fun HomeFeed(
    uiState: HomeUiState,
    onQuizClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onNavigateToDiscover: (sectionKey: String?, sectionType: String?, title: String?, topic: String?) -> Unit,
    roomCodeCard: @Composable () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val homeError = uiState.homeError
    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        item(key = KEY_ROOM_CODE) {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                roomCodeCard()
            }
        }

        when {
            uiState.isLoadingHome -> item(key = KEY_DISCOVER_STATUS) {
                DiscoverStatusCard { CircularProgressIndicator() }
            }

            homeError != null -> item(key = KEY_DISCOVER_STATUS) {
                DiscoverStatusCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Không thể tải nội dung", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            homeError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        QuizSecondaryButton("Thử lại", onRetry)
                    }
                }
            }

            uiState.homeSections.isEmpty() -> item(key = KEY_DISCOVER_STATUS) {
                DiscoverStatusCard {
                    Text("Không có nội dung", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                // 1) "Tiếp tục chơi" ghim trên đầu (xem HomeUiState.continueSection).
                //    KHÔNG có "Xem thêm": danh sách này sắp theo `last_played_at`
                //    của riêng user, không endpoint feed nào phân trang được nó.
                uiState.continueSection?.let { section ->
                    item(key = section.sectionKey) {
                        HomeSectionRow(section = section, onQuizClick = onQuizClick)
                    }
                }

                // 2) Các section gợi ý, đúng thứ tự backend sắp.
                items(uiState.discoverySections, key = { it.sectionKey }) { section ->
                    HomeSectionRow(
                        section = section,
                        onQuizClick = onQuizClick,
                        eyebrowText = if (section.sectionKey == uiState.discoverySections.firstOrNull()?.sectionKey) "QUIZ NỔI BẬT" else null,
                        onSeeMore = if (section.hasSeeMore) {
                            {
                                onNavigateToDiscover(
                                    section.sectionKey,
                                    section.sectionType,
                                    section.title,
                                    section.discoverTopic
                                )
                            }
                        } else {
                            // Section không có nút vẫn hiển thị bình thường — trông
                            // như một danh sách được chọn sẵn, không phải bị hỏng.
                            null
                        }
                    )
                }

                // 3) Lối vào màn Khám phá đầy đủ (nhiều section hơn Trang chủ).
                item(key = KEY_DISCOVER_ALL) {
                    CustomClickableText(
                        "Khám phá tất cả", { onNavigateToDiscover(null, null, null, null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textAlign = TextAlign.Center,
                        textSize = 14.sp,
                    )
                }
            }
        }
    }
}

/**
 * Khung chứa trạng thái tải / lỗi / rỗng của RIÊNG khối Khám phá.
 *
 * Có nền riêng để người dùng đọc được "phần này chưa có nội dung" thay vì
 * tưởng cả trang hỏng — ngay trên nó thẻ nhập mã vẫn dùng được.
 *
 * Chiều cao tối thiểu là số cứng, KHÔNG dùng `fillMaxSize()`: trong `item` của
 * `LazyColumn`, ràng buộc chiều cao tối đa là vô hạn nên `fillMaxSize()` sẽ
 * lỗi. `fillParentMaxHeight()` thì hợp luật nhưng lại đẩy khối này cao bằng cả
 * khung nhìn, làm thẻ nhập mã phía trên bị trôi khỏi màn hình.
 */
@Composable
private fun DiscoverStatusCard(content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DISCOVER_STATUS_MIN_HEIGHT.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

// Khóa `item` để LazyColumn giữ đúng vị trí cuộn khi feed đổi trạng thái.
private const val KEY_ROOM_CODE = "room-code"
private const val KEY_DISCOVER_STATUS = "discover-status"
private const val KEY_DISCOVER_ALL = "discover-all"

/** Đủ cao để khối trạng thái trông như một khu vực nội dung, không phải một dòng lỗi. */
private const val DISCOVER_STATUS_MIN_HEIGHT = 200

@Preview(name = "Home Guest Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Guest Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentPreview() = HomeContentPreview(homePreviewState())

@Preview(name = "Home Continue Section", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeContinuePreview() {
    val state = homePreviewState()
    HomeContentPreview(state.copy(homeSections = listOf(HomeSection("continue-preview", "Tiếp tục chơi", "continue", state.homeSections.first().items)) + state.homeSections))
}

@Preview(name = "Home Session Unknown", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeUnknownSessionPreview() = HomeContentPreview(homePreviewState().copy(session = android.kma.myquizzapp.core.common.model.SessionState.Unknown))

@Preview(name = "Home Loading", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeLoadingPreview() = HomeContentPreview(homePreviewState().copy(isLoadingHome = true))

@Preview(name = "Home Feed Error", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeErrorPreview() = HomeContentPreview(homePreviewState().copy(homeError = "Kiểm tra kết nối rồi thử lại."))

@Preview(name = "Home Empty", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeEmptyPreview() = HomeContentPreview(homePreviewState().copy(homeSections = emptyList()))

@Preview(name = "Home Narrow Large Text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun HomeNarrowPreview() = HomeContentPreview(homePreviewState())

@Composable
private fun HomeContentPreview(state: HomeUiState) {
    MyQuizAppTheme {
        HomeScreenContent(
            uiState = state, onNavigateToSearch = {}, onNavigateToAuth = {}, onNavigateToQuizDetail = {}, onRetry = {},
            snackbarHostState = remember { SnackbarHostState() }, listState = rememberLazyListState(),
            roomCodeCard = {
                RoomCodeEntryCard(TextFieldValue("AB23", TextRange(4)), {}, true, {}, {}, false)
            },
        )
    }
}

private fun homePreviewState(): HomeUiState {
    val quiz = android.kma.myquizzapp.core.common.model.QuizCard(
        id = 1L, quizName = "Đố vui vũ trụ", quizCategory = "Khoa học", quizLanguage = "vi", quizOwnerId = 1L,
        questionCount = 12, playCount = 350, completionRate = 0.85, createdAt = "2026-10-10T00:00:00Z",
    )
    return HomeUiState(
        session = android.kma.myquizzapp.core.common.model.SessionState.Guest,
        homeSections = listOf(
            HomeSection("trending-preview", "Thịnh hành tuần này 🔥", "trending", listOf(quiz, quiz.copy(id = 2L, quizName = "Khám phá thế giới", quizCategory = "Địa lý"))),
            HomeSection("newest-preview", "Quiz mới nhất", "newest", listOf(quiz.copy(id = 3L))),
        ),
    )
}
