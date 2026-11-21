# N19.5 — Bottom Navigation thật (6/9/2026)

> Bổ sung ngoài kế hoạch, trả nợ từ N13.5. Code trên nhánh `main`, đã test trên máy thật.
> Chi tiết thi hành: xem `### 11.6` trong design doc; trạng thái: xem mục N19.5 trong file kế hoạch.

---

## 1. UI cấp app nằm ngoài `NavHost` thì không có back stack entry

**Tình huống.** Bottom bar phải hiện ở 5 tab và ẩn ở mọi màn khác. Có 2 cách: đặt `Scaffold` trong từng destination, hoặc bọc `Scaffold` ngoài `NavHost` rồi suy ra tab đang chọn từ destination hiện tại. Chọn cách 2:

```kotlin
val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
    currentDestination?.hasRoute(tab.route::class) == true
}
// null -> không phải tab cấp cao nhất -> không render bar
```

Lợi: màn con và toàn bộ màn game **tự động** ẩn bar, không cần cờ `showBottomBar` rải khắp các graph, thêm màn mới không phải nhớ gì cả.

**Cái giá phải trả (điểm dễ vấp).** Bar không thuộc `NavBackStackEntry` nào, nên:

- **Không dùng được `hiltViewModel()` scope theo destination.** Muốn bar có dữ liệu (avatar user) thì phải dùng ViewModel scope theo Activity — `CurrentUserViewModel` đặt ngay trong `navigation/`.
- **Bar không biết chuyện xảy ra bên trong `NavHost`.** User đăng xuất ở tab Hồ sơ, bar vẫn giữ avatar cũ. Phải cho màn bên trong báo ngược ra bằng callback: `mainGraph(navController, onCurrentUserChanged = currentUserViewModel::refresh)`.

**Rút ra:** trước khi đặt UI ra ngoài `NavHost`, hãy hỏi "UI này lấy dữ liệu ở đâu và ai báo cho nó biết dữ liệu đã đổi?". Nếu không trả lời được thì sẽ dính state cũ dai dẳng.

---

## 2. Phải biết repository có cache hay không TRƯỚC khi nạp dữ liệu cho UI luôn hiển thị

Ý định ban đầu: cho bar tự nạp avatar mỗi khi cần. Đọc code trước khi viết thì thấy:

```kotlin
// AuthRepositoryImpl
override suspend fun getCurrentUser(): Result<User> =
    userApi.getMe().map { it.user.toDomain() }   // KHÔNG cache, KHÔNG memo
```

Bar là UI **luôn hiển thị** và recompose mỗi lần đổi tab. Nếu nạp theo kiểu "mỗi lần bar cần avatar" thì mỗi cú chạm tab = 1 request `GET /users/me`. Người dùng chuyển tab 20 lần trong 1 phút là 20 request cho dữ liệu gần như không bao giờ đổi.

**Cách làm đã chốt** — refresh theo **mốc sự kiện**, không theo recompose:

| Mốc | Vì sao |
| --- | --- |
| Dựng bar lần đầu | cần dữ liệu ban đầu |
| `ON_RESUME` (`LifecycleEventEffect`) | user có thể đã đổi avatar bên web |
| Vừa **RỜI** `AuthGraph` | đăng nhập/đăng ký xong → danh tính đổi |
| Đăng xuất | phải xóa avatar người vừa thoát |

Hai chi tiết khiến nó chạy đúng:

- `LaunchedEffect` key theo `inAuthGraph` (tính bằng `currentDestination?.hierarchy?.any { it.hasRoute(Route.AuthGraph::class) }`) — effect chỉ chạy ở **lằn ranh** auth, không chạy theo mỗi lần đổi tab. Nếu key theo `currentDestination` thì lại rơi đúng vào cái bẫy trên.
- `refresh()` tự dedupe: `if (inFlight?.isActive == true) return`. Lúc khởi động 2–3 mốc chồng nhau nhưng chỉ tốn 1 request.

**Nợ còn lại (đã ghi vào AGENTS.md §6):** Home cũng gọi `getCurrentUser()` riêng qua `HomeIntent.CheckAuthState`. Vậy vào app là **2 request không cache cho cùng một `User`**. Đúng chỗ để sửa là thêm cache ở repository (in-memory + invalidate khi login/logout), không phải khâu vá ở tầng UI — sửa ở repo thì cả 2 chỗ gọi đều được lợi mà không phải biết về nhau.

---

## 3. `Icon` nhuộm màu, `Image` thì không

`Icon` của Material tô nội dung theo `LocalContentColor` — hợp với icon đơn sắc, nhưng sẽ **bôi phẳng** logo brand và avatar thành một khối đơn sắc. Cả hai phải vẽ bằng `Image`.

Đánh đổi phải nhận: 2 tab đó **mất tín hiệu chọn/không chọn bằng màu icon**, chỉ còn indicator pill + màu nhãn. Đây là quyết định có ý thức, không phải bug — ghi lại để sau này không có ai "sửa" ngược thành `Icon`.

Liên quan: `TopLevelTab` giữ `icon: ImageVector` là **non-null** và thêm `iconRes: Int?` ưu tiên cao hơn, thay vì cho cả hai nullable. Nhờ vậy phần render không phát sinh nhánh `else` không-bao-giờ-chạy, và tab Hồ sơ vẫn có vector fallback cho guest.

---

## 4. Nhãn bottom nav tiếng Việt rất dễ tràn 2 dòng

Nhãn 9 ký tự ("Trang chủ", "Hoạt động") chia 5 ô trên máy 360dp là tràn xuống dòng 2 → **lệch chiều cao cả thanh nav**, không chỉ xấu một ô.

Cách tính (`rememberTabLabelFontSize`): bề rộng 1 ô ≈ `screenWidthDp / số tab`, bề rộng 1 ký tự ≈ `0.55 × fontSize`, đảo lại để suy ra fontSize lớn nhất còn vừa, rồi kẹp 9–12sp (chặn trên để tablet không phóng chữ lệch với icon, chặn dưới để còn đọc được). Kèm `maxLines = 1` + `softWrap = false` + `textAlign = Center` + `fillMaxWidth`, và bỏ `letterSpacing` 0.5sp mặc định của `labelSmall` để lấy thêm chỗ. Vẫn giữ `TextOverflow.Ellipsis` làm lưới an toàn cho trường hợp font scale hệ thống rất lớn.

**Hệ quả phải nhớ khi bảo trì:** cỡ chữ tính theo nhãn **dài nhất**, nên thêm một tab có nhãn dài sẽ làm nhỏ chữ của tất cả tab. Có preview `widthDp = 320` để kiểm tra trước khi build.

---

## 5. Quyết định sản phẩm đã chốt trong lúc thảo luận

- **Bottom nav chỉ chứa ĐIỂM ĐẾN, không chứa HÀNH ĐỘNG** → bỏ ý tưởng tab "Tạo quiz" trong bản thiết kế; tạo quiz là FAB trong tab Thư viện, bấm vào mở thẳng editor (khi nào có thêm luồng import mới cần màn chọn).
- **Không xây lại màn đã có.** Tab Thư viện trỏ thẳng `Route.MyQuizzes` — màn thật đã làm từ N13, chỉ đổi đường vào (trước đây vào từ Profile) và cho `onNavigateBack` thành nullable để bỏ nút back khi nó đóng vai tab gốc. `Route.Library` placeholder bị xóa, chấm dứt trùng lặp treo từ N13.5.
- **Fact-check trước khi thiết kế:** đã định làm sub-tab "Đã lưu" trong Thư viện, nhưng audit backend cho thấy chỉ có `GET /v1/quizzes/me`, không có bảng saved/bookmark nào → bỏ. Web cũng chỉ có một `/library`.
- **Avatar chỉ hiện ở một nơi.** Bỏ avatar ở TopBar Home, chuyển xuống icon tab Hồ sơ.
- **Tab Hoạt động để placeholder có chủ đích.** Backend chỉ có `role=played|hosted`, chưa có `role=all` với cursor thống nhất. Gộp 2 danh sách ở client sẽ sai phân trang (2 con trỏ khác nhau), nên chờ backend thay vì làm tạm.
