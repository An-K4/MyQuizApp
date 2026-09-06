package android.kma.myquizzapp.navigation

import android.content.res.Configuration
import android.kma.myquizzapp.R
import android.kma.myquizzapp.core.ui.components.Avatar
import android.kma.myquizzapp.core.ui.theme.MyQuizAppTheme
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

/**
 * 5 tab cấp cao nhất của app (N19.5). Thứ tự khai báo = thứ tự trên bottom bar.
 *
 * Nguyên tắc: bottom nav chỉ chứa ĐIỂM ĐẾN, không chứa HÀNH ĐỘNG. Vì vậy KHÔNG
 * có tab "Tạo quiz" — tạo quiz là FAB trong tab Thư viện, mở thẳng editor.
 *
 * LIBRARY trỏ tới [Route.MyQuizzes] (màn thật, có Paging + xoá + refresh).
 * Route.Library cũ chỉ là placeholder trùng lặp, đã bị xoá ở N19.5.
 *
 * Mọi tab đều cùng cỡ icon [TAB_ICON_SIZE_DP] — tab giữa KHÔNG được phóng to hay
 * bọc nền tròn, chỉ khác ở chỗ dùng logo app.
 *
 * Khi thêm/đổi [label], nhớ rằng cỡ chữ nhãn được tính từ nhãn DÀI NHẤT (xem
 * [rememberTabLabelFontSize]) — thêm một nhãn dài sẽ làm nhỏ chữ cả thanh nav.
 *
 * @param icon icon vector mặc định của tab.
 * @param iconRes drawable dùng thay cho [icon] khi cần ảnh thật (ví dụ logo app ở
 *   tab Tham gia). Được vẽ nguyên màu, KHÔNG nhuộm theo trạng thái chọn — xem
 *   ghi chú ở [MainBottomBar].
 * @param usesAvatar tab Hồ sơ hiển thị avatar user; [icon] là fallback khi chưa
 *   đăng nhập.
 */
enum class TopLevelTab(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    @DrawableRes val iconRes: Int? = null,
    val usesAvatar: Boolean = false
) {
    HOME(Route.Home, "Trang chủ", Icons.Filled.Home),
    LIBRARY(Route.MyQuizzes, "Thư viện", Icons.Filled.List),
    JOIN(Route.JoinRoom, "Tham gia", Icons.Filled.PlayArrow, iconRes = R.drawable.app_logo_nav_icon),
    ACTIVITY(Route.Activity, "Hoạt động", Icons.Filled.History),
    PROFILE(Route.Profile, "Hồ sơ", Icons.Filled.Person, usesAvatar = true)
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
 * Logo app và avatar được vẽ bằng [Image] (nguyên màu) thay vì [Icon] (bị nhuộm
 * theo màu trạng thái). Để bù lại phần định hướng bị mất, trạng thái đang chọn
 * của 2 tab này vẫn thể hiện qua indicator pill và màu nhãn của Material.
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
    val labelFontSize = rememberTabLabelFontSize()

    NavigationBar(modifier = modifier) {
        TopLevelTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    when {
                        tab.iconRes != null -> Image(
                            painter = painterResource(tab.iconRes),
                            contentDescription = tab.label,
                            modifier = Modifier.size(TAB_ICON_SIZE_DP.dp)
                        )
                        tab.usesAvatar && !avatarUrl.isNullOrBlank() -> Avatar(
                            avatarUrl = avatarUrl,
                            contentDescription = tab.label,
                            size = TAB_ICON_SIZE_DP.dp
                        )
                        else -> Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        // softWrap = false: thà co chữ chứ không để nhãn tràn xuống
                        // dòng 2 (làm lệch chiều cao cả thanh nav).
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = labelFontSize,
                            lineHeight = labelFontSize * 1.25f,
                            // labelSmall mặc định có letterSpacing 0.5sp — bỏ đi để lấy
                            // thêm chỗ cho nhãn 9 ký tự ("Trang chủ", "Hoạt động").
                            letterSpacing = 0.sp
                        ),
                        // Ô nhãn của Material rộng bằng cả item; fillMaxWidth + center để
                        // chữ căn giữa theo icon thay vì dồn về bên trái.
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
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
 * NavigationBarItem — logo app và avatar phải bám theo con số này để 5 ô trông
 * ngang bằng nhau.
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
        MainBottomBar(selected = Route.Home, onSelect = {}, avatarUrl = null)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainBottomBarJoinSelectedPreview() {
    MyQuizAppTheme {
        MainBottomBar(selected = Route.JoinRoom, onSelect = {}, avatarUrl = null)
    }
}

/** Máy hẹp: kiểm tra nhãn 9 ký tự vẫn nằm 1 dòng sau khi co cỡ chữ. */
@Preview(showBackground = true, widthDp = 320)
@Composable
private fun MainBottomBarNarrowScreenPreview() {
    MyQuizAppTheme {
        MainBottomBar(selected = Route.Profile, onSelect = {}, avatarUrl = null)
    }
}
