package android.kma.myquizzapp.feature.home.presentation

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.HomeSectionRow
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    onNavigateToDiscover: (sectionKey: String?) -> Unit,
    roomCodeCard: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    onNavigateToDiscover: (sectionKey: String?) -> Unit = {},
    roomCodeCard: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyQuizz") },
                actions = {
                    // N19.6: lối vào phòng không còn ở top bar lẫn bottom nav —
                    // ô nhập mã nằm ngay trong nội dung trang (xem roomCodeCard).
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                    AuthHeaderAction(
                        showSignIn = uiState.showSignInAction,
                        onNavigateToAuth = onNavigateToAuth
                    )
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        HomeFeed(
            uiState = uiState,
            onQuizClick = onNavigateToQuizDetail,
            onRetry = onRetry,
            onNavigateToDiscover = onNavigateToDiscover,
            roomCodeCard = roomCodeCard,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
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
private fun AuthHeaderAction(
    showSignIn: Boolean,
    onNavigateToAuth: () -> Unit
) {
    if (showSignIn) {
        TextButton(onClick = onNavigateToAuth) { Text("Đăng ký/Đăng nhập") }
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
    onNavigateToDiscover: (sectionKey: String?) -> Unit,
    roomCodeCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val homeError = uiState.homeError
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item(key = KEY_ROOM_CODE) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                        Button(onClick = onRetry) { Text("Thử lại") }
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
                        onSeeMore = if (section.hasSeeMore) {
                            { onNavigateToDiscover(section.sectionKey) }
                        } else {
                            // Section không có nút vẫn hiển thị bình thường — trông
                            // như một danh sách được chọn sẵn, không phải bị hỏng.
                            null
                        }
                    )
                }

                // 3) Lối vào màn Khám phá đầy đủ (nhiều section hơn Trang chủ).
                item(key = KEY_DISCOVER_ALL) {
                    TextButton(
                        onClick = { onNavigateToDiscover(null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) { Text("Khám phá tất cả") }
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
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenContentPreview() {
    MyQuizAppTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onNavigateToSearch = {},
            onNavigateToAuth = {},
            onNavigateToQuizDetail = {},
            onRetry = {}
        )
    }
}
