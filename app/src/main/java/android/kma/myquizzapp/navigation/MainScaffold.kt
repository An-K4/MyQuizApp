package android.kma.myquizzapp.navigation

import android.content.res.Configuration
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import android.kma.myquizzapp.core.ui.theme.FrontendColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.QuizCard
import android.kma.myquizzapp.core.common.model.SessionState
import android.kma.myquizzapp.core.ui.components.RoomCodeEntryCard
import android.kma.myquizzapp.feature.home.presentation.HomeScreenContent
import android.kma.myquizzapp.feature.home.presentation.HomeUiState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

/**
 * 4 tab cấp cao nhất của app. Thứ tự khai báo = thứ tự trên bottom bar.
 *
 * Nguyên tắc: bottom nav chỉ chứa ĐIỂM ĐẾN, không chứa HÀNH ĐỘNG. Vì vậy KHÔNG
 * có tab "Tạo quiz" — tạo quiz là FAB trong tab Thư viện, mở thẳng editor.
 *
 * LIBRARY trỏ tới [Route.MyQuizzes] (màn thật, có Paging + xoá + refresh).
 * Route.Library cũ chỉ là placeholder trùng lặp, đã bị xoá ở N19.5.
 *
 * N19.6 bỏ tab "Tham gia" (5 → 4 tab) vì cùng nguyên tắc trên: nó là một HÀNH
 * ĐỘNG bị dựng thành điểm đến. Màn cũ chỉ có đúng một ô nhập mã rồi điều
 * hướng đi ngay, nên ô đó chuyển thành thế nằm trên Trang chủ (JoinRoomCard).
 * Lý do thứ hai: nó chiếm ô giữa — vị trí được thiết kế cho điểm đến nổi bật
 * nhất — trong khi Trang chủ cũng đang giữ vai đó, làm phân cấp mờ đi.
 *
 * Mọi tab đều cùng cỡ icon [TAB_ICON_SIZE_DP]; không tab nào được phóng to hay
 * bọc nền tròn riêng.
 *
 * Khi thêm/đổi [label], nhớ rằng cỡ chữ nhãn được tính từ nhãn DÀI NHẤT (xem
 * [rememberTabLabelFontSize]) — thêm một nhãn dài sẽ làm nhỏ chữ cả thanh nav.
 *
 * @param icon icon vector mặc định của tab.
 * @param usesAvatar tab Hồ sơ hiển thị avatar user; [icon] là fallback khi chưa
 *   đăng nhập.
 */
enum class TopLevelTab(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val usesAvatar: Boolean = false
) {
    HOME(Route.Home, "Trang chủ", Icons.Outlined.Home),
    LIBRARY(Route.MyQuizzes, "Thư viện", Icons.Outlined.LibraryBooks),
    ACTIVITY(Route.Activity, "Hoạt động", Icons.Outlined.History),
    PROFILE(Route.Profile, "Hồ sơ", Icons.Outlined.Person, usesAvatar = true)
}

/**
 * Chuyển tab: giữ state của tab cũ, không chồng thêm bản sao vào back stack.
 *
 * popUpTo<Route.Home> vì Home là start destination của MainGraph — nhờ vậy bấm
 * back ở tab bất kỳ sẽ về Trang chủ, chứ không bật ngược lần lượt từng tab đã đi.
 */
fun NavHostController.navigateToTab(route: Route) {
    navigate(route) {
        popUpTo<Route.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Bottom bar thuần UI — nhận tab đang chọn + avatar user, phát ra tab được bấm.
 *
 * Avatar ở tab Hồ sơ được vẽ bằng [Avatar] (ảnh nguyên màu) thay vì [Icon] (bị
 * nhuộm theo màu trạng thái). Để bù lại phần định hướng bị mất, trạng thái đang
 * chọn của tab đó vẫn thể hiện qua chấm tím và màu nhãn của tab.
 *
 * @param avatarUrl avatar của user đang đăng nhập cho tab "Hồ sơ"; null = guest
 *   hoặc chưa tải xong, khi đó dùng icon [TopLevelTab.icon].
 */
@Composable
fun MainBottomBar(
    selected: Route?,
    onSelect: (Route) -> Unit,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    MainBottomBarContent(selected, onSelect, avatarUrl, rememberTabLabelFontSize(), modifier)
}

/** Pure visual content; every destination and selected state comes from the app navigation boundary. */
@Composable
fun MainBottomBarContent(
    selected: Route?,
    onSelect: (Route) -> Unit,
    avatarUrl: String?,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val muted = if (isDark) scheme.onSurfaceVariant else FrontendColors.MutedForeground
    val border = if (isDark) scheme.outlineVariant else FrontendColors.Border
    Surface(modifier = modifier.fillMaxWidth(), color = scheme.background) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(border))
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                TopLevelTab.entries.forEach { tab ->
                    val active = selected == tab.route
                    val tint = if (active) scheme.primary else muted
                    Column(
                        Modifier.weight(1f).heightIn(min = 64.dp)
                            .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab.route) })
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (tab.usesAvatar && !avatarUrl.isNullOrBlank()) {
                            Avatar(avatarUrl = avatarUrl, contentDescription = tab.label, size = TAB_ICON_SIZE_DP.dp)
                        } else {
                            Icon(tab.icon, null, Modifier.size(TAB_ICON_SIZE_DP.dp), tint = tint)
                        }
                        Text(
                            tab.label, color = tint, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = labelFontSize, lineHeight = labelFontSize * 1.25f, letterSpacing = 0.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                        // Fixed space keeps labels aligned; the active tab has the small purple dot from the sample.
                        Box(Modifier.size(3.dp).background(if (active) scheme.primary else Color.Transparent, CircleShape))
                    }
                }
            }
        }
    }
}

/**
 * Cỡ chữ nhãn tab, tính theo bề rộng màn hình để nhãn dài nhất vẫn vừa 1 dòng.
 *
 * Bề rộng mỗi item ≈ screenWidth / số tab. Bề rộng thực của 1 ký tự ở font mặc
 * định xấp xỉ [AVG_CHAR_WIDTH_RATIO] × fontSize, nên đảo lại công thức để suy ra
 * fontSize lớn nhất còn vừa, rồi kẹp trong [MIN_TAB_LABEL_SP]..[MAX_TAB_LABEL_SP]:
 * chặn trên để tablet không phóng chữ to lệch với icon, chặn dưới để không nhỏ
 * tới mức không đọc được.
 *
 * Vẫn giữ ellipsis ở nơi gọi làm lưới an toàn: nếu người dùng đặt cỡ chữ hệ
 * thống rất lớn thì sp đã kẹp vẫn có thể tràn.
 */
@Composable
private fun rememberTabLabelFontSize(): TextUnit {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        val itemWidthDp = screenWidthDp.toFloat() / TopLevelTab.entries.size - TAB_LABEL_PADDING_DP
        val longestLabelLength = TopLevelTab.entries.maxOf { it.label.length }
        (itemWidthDp / (longestLabelLength * AVG_CHAR_WIDTH_RATIO))
            .coerceIn(MIN_TAB_LABEL_SP, MAX_TAB_LABEL_SP)
            .sp
    }
}

/**
 * Cỡ icon chung cho mọi tab, bằng cỡ icon mặc định của Material trong
 * NavigationBarItem — avatar phải bám theo con số này để các ô trông ngang bằng
 * nhau.
 */
private const val TAB_ICON_SIZE_DP = 24

/** Padding ngang Material tự chừa trong mỗi item (trái + phải). */
private const val TAB_LABEL_PADDING_DP = 8f
private const val AVG_CHAR_WIDTH_RATIO = 0.55f
private const val MIN_TAB_LABEL_SP = 9f
private const val MAX_TAB_LABEL_SP = 12f

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MainBottomBarHomeSelectedPreview() {
    MyQuizAppTheme {
        MainBottomBarContent(selected = Route.Home, onSelect = {}, avatarUrl = null, labelFontSize = 11.sp)
    }
}

/** Máy hẹp: kiểm tra nhãn 9 ký tự vẫn nằm 1 dòng sau khi co cỡ chữ. */
@Preview(showBackground = true, widthDp = 320)
@Composable
private fun MainBottomBarNarrowScreenPreview() {
    MyQuizAppTheme {
        MainBottomBarContent(selected = Route.Profile, onSelect = {}, avatarUrl = null, labelFontSize = 10.sp)
    }
}

/** Full Home + bottom navigation fixture. No ViewModel, Hilt, network, or NavController. */
@Preview(name = "Home Full Light", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844)
@Preview(name = "Home Full Dark", showBackground = true, showSystemUi = true, widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Home Full Narrow Large Text", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun MainHomeFullPreview() {
    val quiz = QuizCard(
        id = 1L, quizName = "Đố vui vũ trụ", quizCategory = "Khoa học", quizLanguage = "vi", quizOwnerId = 1L,
        questionCount = 12, playCount = 350, completionRate = 0.85, createdAt = "2026-10-10T00:00:00Z",
    )
    val state = HomeUiState(
        session = SessionState.Guest,
        homeSections = listOf(HomeSection("trending-preview", "Thịnh hành tuần này 🔥", "trending", listOf(quiz, quiz.copy(id = 2L, quizName = "Khám phá thế giới", quizCategory = "Địa lý")))),
    )
    MyQuizAppTheme {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { MainBottomBarContent(Route.Home, {}, null, 11.sp) },
        ) { padding ->
            HomeScreenContent(
                uiState = state, onNavigateToSearch = {}, onNavigateToAuth = {}, onNavigateToQuizDetail = {}, onRetry = {},
                snackbarHostState = remember { SnackbarHostState() }, listState = rememberLazyListState(),
                roomCodeCard = { RoomCodeEntryCard(TextFieldValue("AB23", TextRange(4)), {}, true, {}, {}, false) },
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            )
        }
    }
}
