# N19.6 — Session state một nguồn, gác đăng nhập, Home gộp lối vào phòng

> Xong 6/9/2026, code trên nhánh `main`, đã build và test trên máy thật.
> Bối cảnh: N19.5 vừa dựng bottom nav 5 tab thì lộ ra bug "đăng xuất rồi vào lại
> Hồ sơ vẫn thấy account cũ", và tab "Tham gia" giữa thanh nav gây phân cấp
> điều hướng lộn xộn.

---

## 1. Ba bản sao trạng thái đăng nhập là nguyên nhân gốc, không phải lỗi hiển thị

Trước N19.6 có ba nơi tự giữ "user hiện tại": `HomeUiState.currentUser`,
`ProfileUiState.user`, `CurrentUserViewModel.avatarUrl`. Mỗi nơi tự gọi
`getCurrentUser()` một lần và tự quyết khi nào nạp lại.

Bug thật: `ProfileViewModel` có `init { loadCurrentUser() }`. ViewModel này sống
theo back stack entry của tab Hồ sơ, mà entry đó được `saveState`/`restoreState`
giữ lại khi đổi tab — nên sau khi đăng xuất, quay lại tab Hồ sơ **không tạo
ViewModel mới**, `init` không chạy lại, state cũ còn nguyên.

Điều đáng ghi nhớ là hướng vá SAI mà ta đã cân nhắc: thêm
`LifecycleEventEffect(ON_RESUME)` cho mỗi màn đọc user. Nó "chữa" được triệu
chứng nhưng biến mỗi lần chạm tab thành một request `GET /users/me`, và vẫn để
lại ba nguồn sự thật — lần sau thêm màn thứ tư là lặp lại đúng bug.

→ Cách đúng: một `SessionRepository` `@Singleton` giữ `StateFlow<SessionState>`;
mọi UI **thu** từ đó. `LoginUseCase`/`LogoutUseCase` ghi vào nó, nên đăng xuất ở
bất kỳ đâu là mọi màn đổi theo cùng một khung hình. Xóa được `init` nạp dữ liệu,
xóa `ON_RESUME`, xóa callback `onCurrentUserChanged` thêm ở N19.5.

## 2. `Unknown` phải là một trạng thái riêng, khác `Guest`

`SessionState` có ba nhánh: `Unknown` (chưa hỏi xong server) / `Guest` (đã xác
nhận không đăng nhập) / `LoggedIn(user)`. Gộp `Unknown` vào `Guest` cho gọn là
sai, vì hai chỗ dùng nó theo hai cách trái ngược:

- **Gác đăng nhập** thì cho `Unknown` đi qua. Đoán sai theo hướng chặn nghĩa là
  người đã đăng nhập bị hỏi đăng nhập lại chỉ vì mạng chậm — hỏng nặng hơn là
  cho một người khách lọt vào màn Thư viện rồi nhận 401.
- **Phân luồng join phòng** thì KHÔNG được đoán, phải đợi một lần `refresh()`:
  hai nhánh dẫn tới hai nơi khác nhau (vào thẳng lobby hoặc sang màn nhập tên),
  đoán sai là hỏi tên một người đã đăng nhập rồi bỏ tên đó đi.

→ Quy tắc kèm theo: **lỗi mạng không bao giờ được dịch thành "đã đăng xuất"**.
`refresh()` thất bại thì giữ nguyên trạng thái đang có, chỉ 401 thật mới chuyển
sang `Guest`.

## 3. Gác đăng nhập đặt TRƯỚC khi điều hướng, không đặt trong màn đích

Bốn chốt: tab Thư viện (chặn ngay ở `onSelect` của bottom bar), FAB tạo quiz,
nút "Tạo phòng chơi" ở QuizDetail, và Hồ sơ (không chặn mà hiện empty state có
nút đăng nhập).

Chặn trong màn đích thì người dùng đã thấy màn trắng/hỏng trước khi bị đẩy ra,
và back stack có một entry vô nghĩa. Chặn ở nơi phát sinh hành động thì màn hình
không bao giờ được dựng.

Hai điều rút ra khi chốt danh sách:

- Chốt gác trong màn cấu hình tạo phòng bị **bỏ**, vì đã chặn ở QuizDetail rồi;
  giữ hai lớp chỉ làm người dùng thấy dialog hai lần.
- Nhưng `CreateRoomEffect.RequireAuthentication` (map từ 401) thì **giữ**. Nó
  không phải chốt gác mà là lưới an toàn cho trường hợp cookie hết hạn giữa
  luồng — client không có cách nào biết trước.

## 4. Bottom nav chỉ chứa ĐIỂM ĐẾN — tab "Tham gia" là hành động đội lốt điểm đến

Màn Join cũ chỉ có đúng một ô nhập mã rồi điều hướng đi ngay. Đặt nó thành tab
giữa, phóng to, tô màu nhấn, tạo ra hai điểm đến cùng tự nhận là "quan trọng
nhất" (Trang chủ và Tham gia) trong khi ba tab còn lại rõ ràng thấp hơn hẳn.
Thanh nav kiểu nổi bật icon giữa chỉ nên có MỘT icon nổi hơn phần còn lại.

→ Xóa màn và tab đó, ô nhập mã trở thành `JoinRoomCard` **nhúng vào Home qua
slot** `roomCodeCard: @Composable () -> Unit`. Slot do tầng navigation ở module
`app` truyền vào, không để `feature:home` gọi trực tiếp `feature:lobby` — hai
feature không nên phụ thuộc nhau chỉ vì một ô nhập.

Hệ quả phải xử lý kèm: `KEY_LOBBY_EXIT_MESSAGE` (lý do bị bật khỏi phòng chờ)
phải chuyển sang đọc ở entry của Home, vì Home mới là màn đứng sau lobby. Và lỗi
trong thẻ hiện **inline** thay vì snackbar — một `Card` không có `SnackbarHost`
riêng, đi xin host của Home thì lại buộc Home phải biết về luồng join.

## 5. Compose: mỗi trang một vùng cuộn, và placeholder lỗi thuộc về từng khối

Bản đầu ghim thẻ nhập mã cố định phía trên `LazyColumn` của feed. Sai về UX: thẻ
cao gần nửa màn hình nên chiếm chỗ suốt lúc người dùng lướt khám phá.

Ba điều kỹ thuật đi kèm khi sửa:

- **Không bọc `Column(verticalScroll)` quanh `LazyColumn`.** Hai vùng cuộn cùng
  chiều lồng nhau làm `LazyColumn` nhận ràng buộc chiều cao vô hạn. Cách đúng:
  giữ đúng MỘT `LazyColumn`, thẻ là `item` đầu tiên.
- **Không dùng `fillMaxSize()` trong `item` của `LazyColumn`** — cùng lý do trên.
  `fillParentMaxHeight()` hợp luật nhưng làm khối cao bằng cả khung nhìn, đẩy
  thẻ nhập mã trôi khỏi màn hình. Dùng `heightIn(min = 200.dp)`.
- **Trạng thái tải/lỗi/rỗng bọc trong `Card` riêng của khối Khám phá**, không
  thay thế cả trang. Thẻ nhập mã không tải gì nên không có cách nào để nó "lỗi";
  lỗi chỉ xuất hiện sau khi người dùng bấm vào phòng và hiện inline trong thẻ.
  Nhờ vậy mất mạng vẫn vào phòng được bằng mã bạn bè gửi.

Lưu ý ngược lại cũng quan trọng: **không tách placeholder lỗi cho `continue` và
cho khối khám phá thành hai**, vì cả hai về từ cùng một request `GET
/quizzes/home` — hai khối báo cùng một câu lỗi kèm hai nút "Thử lại" gọi đúng
một API trông tệ hơn một khối. Muốn lỗi độc lập thật thì phải đổi Home sang tải
từng section bằng endpoint riêng, và ta cố tình KHÔNG làm vì mất cache 5 phút
của `/home` và mất khả năng ops thêm/đổi thứ tự section bằng SQL.

## 6. "Xem thêm" xét theo `section_type`, không xét theo tiêu đề

`home_sections` là bảng dữ liệu ops sửa được (`is_active`, `position`, `title`).
Tiêu đề vì vậy có thể đổi bất cứ lúc nào ⇒ tuyệt đối không nhận dạng section
bằng `title`. Điều kiện hiện nút là
`sectionType in setOf("trending", "category", "newest")`, vì chỉ ba loại này có
endpoint phân trang khớp 100%: `trending`/`category` → `/quizzes/feed`,
`newest` → `/quizzes/search?sort=newest`. `featured` không có đích phân trang nào
(đã ghi nợ: xin backend thêm `sort`/`section` cho `/feed`).

Section không có nút vẫn hiện bình thường — trông như một danh sách được chọn
sẵn, không phải bị hỏng. Ẩn nó đi mới là cách làm sai.

`continue` thì client tự ghim lên đầu, đè `position` của backend, và không có
nút "Xem thêm" vì nó sắp theo `last_played_at` của riêng user, không endpoint
feed nào phân trang được.

## 7. `continue` là "quiz chơi dở", KHÔNG phải "phòng đang mở"

Đọc `getContinuePlaying`: lấy từ `player_sessions` các phiên `status <>
'finished'` của `ps.player_id`, trả về **danh sách QuizCard**. Nghĩa là:

- Không có `session_code` lẫn `socketToken` ⇒ bấm vào chỉ mở QuizDetail, về mặt
  kỹ thuật không thể quay lại đúng phòng cũ.
- Khóa vào `player_id` trỏ tới bảng `users`; khách nhận diện bằng `x-guest-id`
  nên backend không có dữ liệu để tính ⇒ khách phải ẩn hoàn toàn khối này.
- Tiêu đề "Tiếp tục chơi" trong DB đang hứa hẹn sai. Đã ghi nợ: đổi tiêu đề bằng
  SQL, và tính năng "quay lại phòng đang chơi" thật là một mốc riêng cần backend
  thêm endpoint trả mã phòng + token của phiên chưa kết thúc.

## 8. Mã phòng dài ĐÚNG 6 ký tự

`generateSessionCode(len = 6)` với bảng chữ `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`
— cố tình bỏ I, O, 0, 1 để đọc mã qua điện thoại không nhầm. Server **không**
validate độ dài mã ở schema nào, nên client là chỗ duy nhất kiểm.

→ Điều kiện là `length == SESSION_CODE_LENGTH`, không phải `>= 6`: mã 7 ký tự
không tồn tại nên cho bấm chỉ đổi một lượt gọi mạng thành 404. Ô nhập lọc chữ và
số (dán mã kèm gạch nối vẫn ra mã đúng) và cắt cứng ở 6. Cố tình KHÔNG lọc
I/O/0/1 dù chúng chắc chắn sai — chặn phím giữa lúc gõ khiến người dùng tưởng
bàn phím lag.

## 9. Bài học tooling: đừng gõ lại tiếng Việt vào `oldText`

`filesystem__edit_file` là atomic và so khớp từng byte. Trong phiên này có **4
lần** cả batch bị hủy chỉ vì một dấu tiếng Việt trong `oldText` bị gõ sai
(`thắng`/`thẳng`, `VẮN`/`VẪN`, `lọi`/`lỗi`, `gủi`/`gửi`) — agent viết lại đoạn
văn từ ký ức thay vì copy nguyên văn từ lần đọc.

→ Hoặc dùng `oldText` ngắn, chỉ gồm ASCII (tên hàm, ký hiệu code), hoặc copy
nguyên văn từ output đọc gần nhất. Khi cả khối cần sửa đều là tiếng Việt, ghi lại
cả file bằng `write_file` rẻ hơn nhiều lần thử `edit_file` thất bại — nhớ giữ
đúng line ending gốc (CRLF/LF) để git không báo đổi cả file.
