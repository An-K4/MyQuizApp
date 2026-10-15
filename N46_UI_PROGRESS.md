# N46 — UI polish theo từng mốc nhỏ (WIP)

> Baseline source Android: `main` tại `da0cbf3`, đã đối chiếu cả `origin/main`. Sau docs `f098826` (source `7178588`), đã thêm Hồ sơ, Bảo mật tài khoản, Hoạt động, Chi tiết trận và các fix test. Đây là baseline source đã commit, không phải SHA APK/backend đã kiểm thử.
> N46 được triển khai từng phần theo user duyệt, không làm một mạch toàn app. N44 vẫn PARTIAL, N45/M6 và release gates chưa đóng; xem `N44_E2E_REPORT.md`.

## 1. Cách đọc trạng thái

- “Đã triển khai/commit” chỉ xác nhận source có trên main.
- “User duyệt UI” không thay thế build, lint, unit test, Compose UI test hoặc E2E.
- Agent chưa chạy Gradle/render Compose. Evidence mới: 8 XML local timestamp 15/10/2026, 56 PASS ở history/lobby/HostGame; 7 XML app 11/10/2026 có 41 PASS nhưng cũ, riêng security dialog chưa khớp source. Giữ 10 XML practice/editor 88 PASS của lượt trước như evidence lịch sử riêng (mục 6), không cộng thành một run/full-suite PASS. Full assemble/lint/CI/E2E mới chưa xác minh.
- Dark hiện là fallback theo Material theme, chưa có mẫu dark được duyệt toàn app.

## 2. Sổ mốc triển khai

| Mốc | Phạm vi | Source commit | Trạng thái / evidence |
| --- | --- | --- | --- |
| N46.0 | Inter + palette + root theme | `4b354b6` | Đã triển khai/commit; 4 font và license đã có; chưa xác minh build mới |
| N46.1 | Bộ component dùng chung + inline text action | `c0fedf3` | Đã triển khai/commit; user chọn `AnswerOptionItem.kt` làm mẫu pattern và yêu cầu top 3 có huy chương; test normalization đã viết, chưa đối chiếu kết quả chạy |
| N46.1a | Preview cho component cũ | `f94d8f5` | Đã triển khai/commit; Preview Light/Dark cục bộ trong file, ảnh dùng placeholder khi inspection |
| N46.2 | Splash — thêm tên app đậm dưới logo | `1585073` | Đã triển khai/commit; giữ layout/logic gốc; không có evidence kiểm thử riêng được agent xác minh |
| N46.3 | Login theo ảnh 02 | `0ba1244` | User xác nhận “màn đăng nhập ổn”; đã commit; không đồng nghĩa regression auth/Google PASS |
| N46.4 | Register theo ảnh 03, icon điện thoại ống nghe | `ee8701c` | User xác nhận oke, đã commit; chưa có log build/test mới được agent xác minh |
| Cấu hình local | Bỏ track ảnh tham chiếu bằng ignore | `423bb4d` | Main `.gitignore` dùng `/design/`; nhánh docs không có `.gitignore`, local `.git/info/exclude` đã thêm `/design/` |
| N46.5a | Forgot/OTP/Reset: validator và request guards | `a1327bb` | Đã commit; 8 test validation được viết, chưa đối chiếu kết quả chạy |
| N46.5b | UI Forgot/OTP/Reset theo quyết định user, không bê nguyên ảnh 04–06 | `3d92af1` | Đã commit; user xác nhận Forgot/Reset ổn và yêu cầu căn giữa dòng OTP gộp; chưa xác minh E2E reset |
| N46.6a | `DiscoveryQuizCard`, `RoomCodeEntryCard`, đổi tên card ngang thành `QuizListCard` | `9417aad` | Đã commit; core component nhận primitive UI props, stateless + Preview |
| N46.6b | Home + room code card theo ảnh 07 đã điều chỉnh | `88a1307` | User xác nhận ổn; đã commit; không thêm progress/resume/rating giả |
| N46.6c | Bottom navigation presentation + Preview Home đầy đủ | `b68fbff` | Đã commit; giữ 4 route Trang chủ/Thư viện/Hoạt động/Hồ sơ |
| N46.7 | Search theo phạm vi ảnh 08, card ngang + append retry | `496dc36` | Đã commit; thêm 5 state test (tổng 9), chưa đối chiếu kết quả chạy |
| N46.8 | Discover grid 2 cột, category/sort, bỏ card cũ + snapshot fix | `d5571d6` | Đã commit; có bản sửa lỗi internal màu và crash index Paging; chưa có xác nhận retest sau sửa |
| N46.9a | Tách SearchQuizCard/LibraryQuizCard + AnswerOptionItem readOnly | `19c6425` | Đã commit; card theo đúng vai trò, default đáp án tương tác không đổi |
| N46.9b | AppConfirmDialog normal/danger dùng chung | `77b4c1a` | Đã commit; AuthRequiredDialog và ConfirmDeleteQuizDialog dùng recipe; user xác nhận ổn |
| N46.10 | Thư viện, menu sửa/xóa, không thanh tab | `25901b2` | User xác nhận ổn; 9 ViewModel test XML local PASS |
| N46.11 | Chi tiết quiz + QuestionPreviewCard, mặc định mở hết | `2308cc6` | User xác nhận ổn; 10 ViewModel + 10 presentation test XML local PASS |
| N46.12a | Multi-select AnswerOptionItem + info palette | `141d81c` | Đã commit; opt-in checkbox, single/readOnly mặc định không đổi |
| N46.12b | Chơi thử theo ảnh 12, feedback/chuyển câu thủ công | `2c1543c` | User duyệt; top bar Chơi thử, critical timer 30%; 28 test XML local PASS |
| N46.13 | Form Tạo/Sửa dùng chung theo ảnh 13/14 đã điều chỉnh | `7178588` | User duyệt; validator/mapping/guards + 25 Preview; 31 editor test XML local PASS, E2E lưu/mở lại chưa xác minh |
| N46.14a | Thống kê Hồ sơ từ total server | `e7576f6` | LoadProfileStatsUseCase + ProfileStatsRepository; 2 chỉ số thật, loading/error/retry và session guard |
| N46.14b | Hồ sơ theo ảnh 26 đã điều chỉnh | `2c67ec5` | User duyệt; avatar entry trong edit, email disabled/form cuộn, guest button không full width |
| N46.14c | Sửa assertion test thống kê | `1affa01` | Sửa assertion Result.Success; app XML cũ đã đọc có 2 stats case PASS, không suy ra full CI |
| N46.15 | Bảo mật tài khoản theo ảnh 27 | `cc9088a` | User duyệt; 3 mắt password, Google-only card, password xác nhận trong dialog; Hủy/Xác nhận ngang hàng. XML VM hiện còn cũ |
| N46.16 | Hoạt động theo ảnh 24 + total riêng mỗi role | `798aabf` | User duyệt; Played/Hosted card riêng, giữ cursor/session/nav; app XML là evidence cũ |
| N46.17a | Mapping mã phòng lịch sử | `be01ee7` | sessionCode optional, sessionId vẫn identity; 5 DTO test XML local PASS (3 cũ + 2 mới) |
| N46.17b | Chi tiết trận theo ảnh 25 đã điều chỉnh | `43ed870` | User review tinh gọn tổng quan; history-only cards, ảnh câu bo góc, short/long answer + Host horizontal stats; 17 presentation test XML PASS |
| Regression interface | Đồng bộ fake với includeTotal | `93c29e3`, `da0cbf3` | HostGame + FinalResult + HostLobby; XML local tương ứng PASS; không sửa production/giảm assertion |
| N46.next | Màn tiếp theo do user chọn + ảnh mẫu | Chưa chốt | Giữ backlog retest Search/Discover/Paging và regression; không tự chuyển màn hoặc đóng N46 |

Tên mốc là checkpoint nội bộ của N46, không thay đổi kế hoạch N1–N50 hoặc tạo release gate mới.

## 3. Font, palette và theme

- Inter v4.1: `inter_regular.ttf` (400), `inter_medium.ttf` (500), `inter_semibold.ttf` (600), `inter_bold.ttf` (700), tại `core/ui/src/main/res/font/`; license tại `core/ui/licenses/Inter-LICENSE.txt`.
- `Type.kt` gắn `InterFontFamily` cho đủ 15 typography role, giữ các metric gốc trừ style màn đang được duyệt. `MainActivity` bọc root `AppNavGraph` trong `MyQuizAppTheme`.
- Chỉ subtitle được user chỉ rõ từ Figma dùng Inter Regular 14sp, line-height 145% (20.3sp), letter-spacing 0; không áp metric này cho mọi role.
- `FrontendColors` (public, `Color.kt`) giữ palette light và gradient result tách khỏi Material roles. `ComponentColors` (`ComponentColors.kt`) là resolver **internal của module core:ui** với dark fallback; feature không được import. Search/Discover dùng FrontendColors cho light, MaterialTheme.colorScheme cho dark như Login/Register. Không mở public resolver chỉ để dập lỗi compile và không gọi palette light là thiết kế dark đã duyệt.

| Token light | Hex |
| --- | --- |
| Foreground | `#0F0E17` |
| PageBackground | `#FFFFFF` |
| Border | `#E8E5F0` |
| MutedForeground | `#71717A` |
| Primary (brand) | `#7B61FF` |
| Success | `#10B981` |
| SuccessForeground | `#087857` |
| Danger | `#EF4444` |
| BrandTint | `#F3F0FF` |
| SoftBackground | `#F8F7FD` |
| Warning | `#A66C00` |
| CorrectAnswerBackground | `#E9FAF3` |
| Info | `#3B82F6` |
| InfoBackground | `#EDF5FF` |
| EditorAnswerAmber | `#F59E0B` |

Gradient result từ CSS: `#21113E` tại 0%, `#4C2B8D` tại 48%, `#25144F` tại 100%. Chỉ có token, chưa coi Final Result đã được polish. Contrast chữ trắng trên tím brand cần quyết định/kiểm tra accessibility trước khi đóng N46; không tự đổi màu user đã duyệt.

## 4. Component đã có — core:ui

| File / API | Ownership và lưu ý |
| --- | --- |
| `QuizButtons.kt` | `QuizPrimaryButton`, `QuizSecondaryButton`, `QuizDangerButton`; stateless; loading/disabled, min-height 52dp, radius 16dp. Secondary có leading/trailing icon slot và contentColor tùy chọn cho Google |
| `CustomClickableText.kt` | Stateless; chỉ đoạn action là link; `LinkAnnotation.Clickable` + `TextLinkStyles`; không dùng `ClickableText` deprecated, không nối chuỗi null thành chữ “null” |
| `SixCharacterCode.kt` | Normalize thuần: OTP 6 ASCII digits; room 6 ASCII alphanumeric uppercase; không pad/auto-submit |
| `SixCharacterCodeField.kt` | Wrapper chỉ giữ focus; text/selection nằm ngoài. `SixCharacterCodeFieldContent` là lõi stateless và là đích Preview |
| `AnswerOptionItem.kt` | Stateless; selected khác correctness. readOnly bỏ tương tác/feedback; selectionMode Single mặc định, Multiple opt-in checkbox ở Chơi thử; không tự đổi multiplayer policy |
| `SearchQuizCard.kt` | Card ngang Search; tên cũ QuizListCard (trước đó QuizSummaryCard); rename `19c6425`, primitive props/rating optional |
| `LibraryQuizCard.kt` | Card quản lý quiz riêng: ảnh vuông, visibility badge, overflow sửa/xóa; menu state/callback hoist |
| `AppConfirmDialog.kt` | Wrapper Dialog/scroll + Content stateless; Normal/Danger, nút chia đều, loading/error; AuthRequiredDialog/ConfirmDeleteQuizDialog là recipe |
| `DiscoveryQuizCard.kt` | Card dọc Home/Discover; `uniformGrid=true` dùng ảnh vuông + dành đủ dòng title/metadata/badge để grid đều; default Home giữ kích thước cũ |
| `RoomCodeEntryCard.kt` | Presentation thuần cho card nhập mã phòng: 2 text không icon, field 6 ô, nút Vào phòng; text/selection/focus/callback do wrapper cung cấp |
| `LeaderboardPlayerCard.kt` | Hạng 1/2/3 hiển thị 🥇/🥈/🥉; hạng khác giữ label. Không tính rank/score hoặc sắp xếp; score/delta optional; có semantics hạng |
| `ComponentGalleryPreview.kt` | Bản tổng hợp Light/Dark và narrow/large-text; không phải màn app |

- `QuizCardItem.kt` đã bị xóa khỏi source trong `d5571d6` sau khi các consumer Search/Discover chuyển sang đúng loại card; không dùng tên này cho code mới. `HomeSectionRow` giữ model core:common ở API adapter hiện hữu, render DiscoveryQuizCard; core card mới chỉ nhận primitive props.
- Đã bổ sung/chuẩn hóa Preview cho `AuthRequiredDialog`, `Avatar`, `HomeSectionRow`, `QuestionImage`, `RemoteImage`, `SettingSwitchRow`. QuizCardItem từng có Preview ở N46.1a nhưng file đã được loại bỏ ở N46.8; không còn trong inventory hiện tại.
- `Avatar`/`RemoteImage` dùng `LocalInspectionMode` để Preview không cần fetch mạng; runtime vẫn dùng Coil. Không xem placeholder Preview là test tải ảnh thật.
- `SixCharacterCodeTest.kt`: 10 JUnit case cho OTP/room, paste/truncate, Unicode, separators và xóa ký tự. Source đã commit, kết quả chạy chưa được agent xác minh.
- OTP và room-entry đã tích hợp code field dùng chung ở N46.5/N46.6. Gameplay/answer/leaderboard và các màn chưa được duyệt vẫn còn mở; không coi có component là đã migrate toàn app. Giữ nguyên contract và policy server.

## 5. Màn đã chỉnh

### Splash — N46.2

- Giữ logo 200dp, nền, indicator, ViewModel/effect/navigation.
- Thêm tên từ `R.string.app_name` bằng Inter Bold 24sp dưới logo.
- Screen/Content và Light/Dark Preview giữ nguyên; không viết lại native/system splash.

### Login — N46.3

- Mẫu local: `design/screens/02 · Xác thực · Đăng nhập.png`.
- Tiêu đề “Chào mừng trở lại! 👋”, mô tả, ô email/password với icon và placeholder, toggle visibility, forgot action.
- `QuizPrimaryButton` Đăng nhập → ngay dưới là `QuizSecondaryButton` Khách → divider → `QuizSecondaryButton` Google với `R.drawable.ic_google` → `CustomClickableText` Đăng ký ngay.
- Credential Manager, nonce, ViewModel/Intent/Effect và navigation giữ nguyên. Không coi sửa UI là fix Google/cookie backlog.
- ScrollState nằm ở wrapper; Content không remember nguồn state. Có IME Next/Done, tránh bàn phím, khóa action khi loading và error message từ UiState.
- 5 Preview: Light/Dark, Loading, Validation Errors, Narrow Large Text.

### Register — N46.4

- Mẫu local: `design/screens/03 · Xác thực · Tạo tài khoản.png`; chỉ chủ đích đổi icon số điện thoại thành `Icons.Outlined.Phone` hình ống nghe.
- Tiêu đề “Bắt đầu hành trình 🚀”; mô tả theo ảnh. Thứ tự Họ và tên → Email → Mật khẩu → Số điện thoại (tùy chọn); nhãn ở trên field, placeholder bên trong, rounded 16dp.
- Private `RegisterFormField` stateless; visibility và scroll state hoist lên wrapper. Nút Đăng ký dùng `QuizPrimaryButton`; dòng Đăng nhập dùng `CustomClickableText`.
- Không sửa validator, backend, ViewModel, Intent/Effect hay navigation. IME Next/Done và trạng thái loading/error được giữ theo source UI hiện tại.
- 5 Preview cùng nhóm như Login. File duy nhất của commit Register là `feature/auth/.../register/RegisterScreen.kt`.

### Forgot / OTP / Reset — N46.5a–N46.5b

- Theo ảnh 04–06 nhưng ưu tiên quyết định user: cả 3 màn giữ top app bar cũ, email/nhãn căn trái; không icon khiên/ổ khóa lớn, không checklist hoặc quy tắc độ phức tạp từ ảnh.
- Forgot chỉ có title trên app bar, mô tả dài, field email đã đăng ký và Primary Gửi mã OTP. `PasswordResetFormField.kt` là field presentation dùng chung, stateless.
- OTP dùng 6 ô ASCII digits đã chuẩn bị, thêm text nhỏ/email theo mẫu, không thêm headline lớn. Dòng gộp **“Gửi lại mã sau 00:xx. Gửi lại mã OTP”** dùng CustomClickableText, căn giữa bên dưới nút xác nhận; chỉ action span nhấn được khi hết cooldown/không loading.
- Reset giữ text Đặt lại mật khẩu; 2 field Mật khẩu/Xác nhận mật khẩu, không checklist. Email đi từ OTP; ticket-peek vẫn giữ xác minh server và hỗ trợ deep link.
- `AuthValidator.resetPasswordError` tái sử dụng rule register hiện hành (tối thiểu 8 ký tự); `confirmPasswordError` kiểm tra rỗng và khớp chính xác, **không trim mật khẩu**. ViewModel chỉ gọi validator, cập nhật lỗi và orchestration Use case.
- Guards cho submit/resend/loading/checking-ticket và đặt loading đồng bộ trước launch nhằm chặn request trùng. Luồng ticket đã có từ N16.5 vẫn là nền tảng; không thêm endpoint hay coi polish lần này là lần đầu viết reset flow. Email chỉ để hiển thị, ticket mới authorize reset. Forgot trim email đầu vào; OTP chỉ ASCII digits; các log Forgot/OTP không còn in địa chỉ email.
- `PasswordResetValidationTest.kt`: 8 case đã viết, chưa có kết quả chạy mới được agent đối chiếu. Cooldown, OTP sai/hết hạn, ticket/deep link/network/success navigation còn cần regression trên máy thật.

### Home / room-entry / bottom navigation — N46.6a–N46.6c

- Ảnh 07 + Discovery quiz component mẫu: brand MyQuizz màu tím, tìm kiếm và pill đăng ký/đăng nhập; room card gồm 2 text không icon, mã **6 ô** thay field ABCXYZ của ảnh, Primary Vào phòng.
- Home giữ **một LazyColumn**; room card là item cuộn được, loading/error của feed không che card join. `JoinRoomCard` trong feature:lobby là wrapper state/focus/selection, render RoomCodeEntryCard qua slot từ MainNavGraph; feature:home không phụ thuộc feature:lobby.
- Room code dùng ASCII alphanumeric uppercase, đúng 6 ký tự; ViewModel normalize và guard submitting đồng bộ. Không đổi contract lookup/join/nickname/auth.
- `HomeSectionRow` dùng DiscoveryQuizCard, kể cả section continue. Dữ liệu continue hiện chỉ là quiz summary, không dựng fake 8/10, progress bar hoặc luồng resume chưa có contract.
- Bottom nav giữ **Trang chủ / Thư viện / Hoạt động / Hồ sơ**; icon outlined + chấm tím tab được chọn, avatar ở Hồ sơ. Không thêm tab Khám phá theo ảnh hoặc đổi navigation/auth gate.
- `MainBottomBarContent` stateless và Preview Home đầy đủ trong MainScaffold; không đi qua Hilt/NavController trong Preview.

### Search — N46.7

- Mẫu 08, user xác nhận card ngang chuẩn bị trước DiscoveryQuizCard: tên khi implement là QuizListCard, **hiện là SearchQuizCard** sau rename `19c6425`. Field rounded tím + xóa; giữ submit/IME Search và navigation detail thật.
- Bỏ recent searches, chủ đề hot, heading/count kết quả và filter icon chưa có chức năng theo phạm vi user duyệt. Không thêm storage/history/filter giả để giống ảnh.
- Metadata lấy questionCount/playCount/owner thật; playCount là **lượt chơi**, không phải số người duy nhất. Không truyền rating khi QuizCard domain chưa có dữ liệu rating.
- `canLoadMore`/`canRetryLoadMore`: chỉ append sau trang Success đúng submittedQuery, có results/hasMore/cursor và không loading. Lỗi append dừng auto-load, Retry dùng đúng cursor và giữ list; submit mới reset loading-more/cursor/results, cancellation guard không cho request đã hủy ghi state.
- Preview Results light/dark/narrow, initial hint/loading/empty/error và append error; thêm 5 test trong SearchUiStateTest (tổng 9), chưa xác minh run mới.

### Discover — N46.8

- Mẫu 09: category chip row cuộn ngang + sort menu riêng (giữ đủ 6 kiểu sắp xếp hiện có); subtitle public quizzes; chọn danh mục giữ sort hiện tại.
- `LazyVerticalGrid(GridCells.Fixed(2))` + DiscoveryQuizCard uniformGrid (ảnh vuông, body dành cùng số dòng), không masonry. Grid/filter scroll state hoist ở wrapper; Content nhận count/accessor/key/load state/callback, không collect Paging/Hilt/Flow.
- Refresh/append loading/error/empty/retry được giữ; footer append span toàn hàng. Không bịa “48 kết quả” vì Paging source chưa expose total. Discover vẫn là màn con, không tự thêm bottom nav/tab Khám phá.
- User đã gặp compile error do import ComponentColors internal từ feature: đã đổi sang FrontendColors + dark theme fallback. User cũng cung cấp runtime crash `ItemSnapshotList index 3 / size 3` tại `itemKey` khi grid nhận count cũ nhưng key đọc live Paging list mới.
- **Bản sửa đã có trong `d5571d6`:** count/key/render data đọc cùng itemSnapshotList captured; prefetch vẫn dùng live Paging access có guard index/ID, không đổi sang peek-only làm mất load-more. Không try/catch nuốt lỗi hoặc bỏ stable keys để dập crash.
- Có Preview light/dark/narrow/large-text/loading/empty/initial-error/append-error. **Chưa có xác nhận retest sau snapshot fix**; commit không chứng minh lỗi hết. Cần thử cuộn tải thêm, đổi filter/sort liên tục từ list dài sang ngắn/rỗng và back/reopen.

### Shared cards và dialog — N46.9a/N46.9b

- `19c6425`: đổi `QuizListCard` thành **SearchQuizCard**, chỉ làm kết quả Search; tạo **LibraryQuizCard** riêng cho quản lý quiz (ảnh vuông, badge Công khai/Riêng tư, menu sửa/xóa). **DiscoveryQuizCard** vẫn dùng Home/Discover. Không ép một card khổng lồ phục vụ mọi vai trò.
- `AnswerOptionItem(readOnly=true)` dùng trong xem câu hỏi: bỏ tương tác/selectable semantics và không hiển thị selected/correct/incorrect. Default `false` giữ behavior tương tác cũ; không tự đổi policy gameplay.
- `77b4c1a`: **AppConfirmDialog** tại core:ui dùng toàn app, không tạo dependency feature → app. Wrapper giữ Dialog/scroll; **AppConfirmDialogContent** stateless, Preview trực tiếp Content để thấy dialog trong ảnh tĩnh.
- Normal: Secondary + Primary; Danger: Secondary chữ xám nhạt + QuizDangerButton. Hàng nút cùng chiều cao, mỗi nút chiếm nửa chiều rộng hữu dụng; loading khóa action và dismiss, lỗi giữ trong dialog để retry. Dùng token hiện hữu, không cần thêm màu vào Color.kt hoặc sửa QuizButtons.kt.
- AuthRequiredDialog là recipe Normal, giữ callback/auth gate; ConfirmDeleteQuizDialog là recipe Danger, giữ nguyên cảnh báo xóa vĩnh viễn quiz/câu hỏi/kết quả phòng, không hoàn tác. Thư viện và Detail dùng chung recipe này; không tuyên bố đã migrate mọi dialog của app.

### Thư viện — N46.10

- `25901b2`, mẫu 10/11 với phạm vi user chốt: tiêu đề “Thư viện của tôi”; giữ search field và bo góc 16dp, giữ bộ lọc ALL/PUBLIC/PRIVATE và 4 sort RECENTLY_UPDATED/NEWEST/OLDEST/NAME_ASC. Dòng filter cuộn ngang, sort menu riêng.
- **Không thanh tab** vì chỉ có quiz đã tạo; quiz đã lưu chưa implement, không thêm tab/count giả. List dùng LibraryQuizCard với số câu/lượt chơi thật, badge visibility và menu ba chấm. FAB tím dạng pill “Tạo Quiz mới”.
- Card mở Detail; menu Sửa nối editor qua auth gate, quay về refresh qua lifecycle. Xóa qua DeleteQuizUseCase; claim isDeleting đồng bộ trước launch, khóa dismiss/retarget khi đang xóa. Lỗi giữ target/dialog để retry; thành công đóng dialog, phát snackbar và tăng paging generation nhưng giữ filter/sort. Resource missing refresh và báo đúng trạng thái.
- Count/key/render dùng cùng captured Paging snapshot; live access guard index/ID giữ prefetch. Giữ refresh/append loading/error/retry và empty state theo filter; padding đáy tránh FAB che card cuối.
- Screen wrapper sở hữu Flow/effect/list/filter/menu state; Content nhận presentation adapters/callback, Preview có data fake và no-op. 9 ViewModel test local PASS; user duyệt UI và yêu cầu bỏ thanh tab đã được áp dụng.

### Chi tiết quiz — N46.11

- `2308cc6`: top bar back + title rút gọn, owner có 2 action Sửa/Xóa; không icon chia sẻ/lưu chưa có chức năng. Cover 16:9 bo góc, author/avatar/ngày và category/language hiện hữu; không thêm badge tác giả uy tín giả.
- Stats dùng count thật (fallback số câu đã tải), thời gian chung **chỉ khi danh sách không rỗng và mọi câu cùng thời gian**; khác nhau/rỗng thì bỏ ô này. playCount là “Lượt chơi”, thiếu thì bỏ, không giả unique participant count.
- **QuestionPreviewCard** ở feature:quiz-manage/presentation/components, khác card soạn QuestionEditorCard. Header có nội dung/loại/thời gian/chevron; body có ảnh optional, AnswerOptionItem readOnly cho lựa chọn, vùng đáp án chỉ đọc cho short/long answer. QuestionTypeLabels dùng cho nhãn loại; editor N46.13 dùng nhãn chip ngắn riêng.
- Quyết định cuối của user: **mặc định expand tất cả câu hỏi**, chỉ thu khi user bấm. Wrapper hoist rememberSaveable collapsedQuestionIds; nhiều card có thể cùng mở, câu mới mặc định mở. Content stateless; không ghi nhầm trạng thái mặc định thu gọn của phương án ban đầu.
- Correct answers ẩn với cả owner; muốn xem/sửa đáp án dùng editor. QuestionPreviewUiModel không truyền correctAnswer/hint/explanation xuống card. Backend GET detail vẫn trả answer key: đây là giới hạn transport/privacy còn mở, readonly UI không phải fix API/privacy.
- Bottom actions cố định: **“Chơi thử” / “Tạo phòng”**, Secondary/Primary cùng kích thước; giữ navigation/auth gate. Giữ disabled behavior quiz rỗng.
- ViewModel claim loading/deleting trước launch, chặn duplicate loads; xóa chỉ khi loaded/owner/valid target/confirmation, khóa reload/dismiss khi mutation. Reload bỏ stale confirmation trước kiểm tra owner; resource missing dọn quiz/owner/dialog, transient error giữ dialog retry; mutation đi qua Use case.
- Preview Content đủ owner/visitor, mixed time, question mở/thu, loading/error/empty, Light/Dark/narrow. User xác nhận UI ổn. 10 ViewModel + 10 presentation test local PASS.

### Chơi thử local — N46.12a/N46.12b

- `141d81c`: AnswerOptionItem thêm selection mode nhiều lựa chọn (checkbox); mặc định single và readOnly giữ nguyên. Color.kt thêm Info `#3B82F6`/InfoBackground `#EDF5FF` cho gợi ý/giải thích.
- `2c1543c`, mẫu 12 theo quyết định user: top bar **“Chơi thử (Câu x/y)”**, back là thoát lượt có AppConfirmDialog, nút **X bên phải là bỏ qua câu**, không phải đóng màn.
- Nhãn luyện tập/không tính xếp hạng và nhãn điểm ước tính riêng. Một lựa chọn chấm ngay khi chọn; nhiều lựa chọn/trả lời ngắn/trả lời dài dùng **Gửi đáp án** → feedback → **Câu tiếp theo** (câu cuối **Xem tổng kết**). Không tự chuyển sau 2/4 giây nữa; skip cũng có feedback rồi chuyển thủ công. Không quay lại câu trước.
- Đáp án đúng luôn xanh, kể cả không được user chọn; chỉ lựa chọn sai mà user đã chọn mới đỏ với icon X. Multiple-select chấm exact set, không biến một lựa chọn đúng thành đỏ chỉ vì thiếu đáp án khác; answer key thiếu cho trạng thái không thể chấm.
- Trước trả lời: Gợi ý nếu có; sau trả lời/skip: Giải thích nếu có, giữ icon bóng đèn. Timer progress theo thời gian, **đỏ khi còn ≤30%**, sau user yêu cầu tăng từ 10% lên gấp ba; dùng milliseconds cho ngưỡng ngắn. Time limit ≤0 ẩn timer, không tự thay bằng 30s. Đây là phòng thủ khi đọc dữ liệu, **không cho editor gửi time_limit=0** vì API tạo/sửa yêu cầu 5–600s.
- Hết thời gian không tự gửi/chuyển câu; có thể trả lời muộn theo rule cũ (đúng muộn 900, không bonus). Countdown 3 giây/tổng kết/chơi lại/edit owner-only giữ nguyên. Không room/socket/history/play-count, không đổi multiplayer scoring hoặc sửa Practice pause/resume blocker N29.
- LoadQuizPreviewUseCase sở hữu đọc quiz + kiểm tra owner qua Repository interfaces; ViewModel chỉ inject Use case/SavedStateHandle. Screen giữ effects/navigation/focus/scroll/dialog, Content stateless; thêm QuizPreviewPresentation.kt và Preview đại diện. User đã duyệt UI. XML local: 8 scoring + 8 presentation + 12 ViewModel = 28 PASS; không coi đó là E2E.

### Tạo và Sửa quiz dùng chung — N46.13

- `7178588`, ảnh 13/14 được user duyệt theo phương án **gộp một màn**. Không step badge/Next/nút lưu trên top bar; ảnh bìa đầu form, field nhãn ngoài/bo 16dp, ngôn ngữ và chủ đề vẫn nhập tự do (ngôn ngữ mặc định `vi`, không tự map “Tiếng Việt” thành `vi`).
- Phân biệt **QuestionDraft là model UI**, **QuestionEditorCard là card soạn**, **QuestionPreviewCard là card xem ở Detail**. Card soạn tách file riêng; state/scroll/expansion/callback hoist từ Screen, form dùng chung QuizEditorContent. Không đưa feature model vào core UI.
- Giữ 4 loại câu hỏi bằng chip cuộn ngang. Câu lựa chọn mới có **2 đáp án**, thêm/xóa trong khoảng **2–4**; nhập trực tiếp trong hàng A/B/C/D màu đỏ/xanh dương/cam/xanh lá, radio một lựa chọn hoặc checkbox nhiều lựa chọn. **Không viền con cả lúc focus/gõ**. Chữ tối màu giữ contrast trên nền sáng; EditorAnswerAmber `#F59E0B` thêm ở Color.kt. Không thêm dialog bút sửa, loại Đúng/Sai mới hoặc giới hạn 6 như ảnh.
- Preset **10s/20s/30s/60s + Khác**, mặc định 30s. Chọn Khác hiện ô số **5–600 giây**, không nhận 0/rỗng; usesCustomTime là trạng thái riêng để ô không biến mất khi đang gõ một giá trị trùng preset. Edit prefill duration ngoài preset ở chế độ Khác.
- **Thông tin bổ sung** (gợi ý/giải thích) mặc định thu gọn. Xóa câu hỏi ngay, không dialog, nhưng luôn giữ tối thiểu một câu. **Không reorder/tay nắm kéo**, duplicate hoặc autosave draft trong scope này.
- Nút lưu cố định dưới màn: Create công khai **Lưu & xuất bản**, riêng tư **Lưu quiz**; Edit **Lưu thay đổi**. API lưu ngay với is_public, **không có bước draft/publish riêng**. Chọn ảnh vẫn chỉ local, upload qua Use case khi Submit; giữ protocol presign/ảnh cũ và giới hạn PATCH hiện hữu (không claim đã sửa clear-cover metadata).
- Create thêm dirty guard; Create/Edit dùng AppConfirmDialog Danger khi rời màn có thay đổi. Screen giữ Photo Picker (target String saveable), UI expansion/list/scroll/dialog/effects; Content thuần. Khi đang lưu, UI và ViewModel khóa thay đổi dữ liệu, Submit lặp, reload và back.
- QuizEditorValidator ở **core:common** nhận input thuần QuestionValidationInput, không Android Uri/feature model; dùng chung Create/Edit, trim nhất quán với payload. Kiểm tra name 3–100, description ≤500, language không rỗng, category ≤50, question text 1–200, options 2–4 mỗi option 1–100, hint/explanation ≤255, timer 5–600, correct index hợp lệ và single đúng một đáp án.
- **Không filter bỏ option trống rồi giữ index cũ**: validator yêu cầu mọi hàng hiện có được điền; mapper trim nhưng giữ thứ tự/chỉ số. Xóa option dịch correctIndexes đúng; đổi lại cùng loại không reset answer key. Claim submitting/load đồng bộ trước launch; lỗi giữ draft cho retry. Validation hiển thị theo field/card và cuộn tới lỗi đầu, validationAttempt cho phép bấm lưu lại cùng lỗi vẫn cuộn.
- 25 Preview tại Create/Edit/Card/Image-Dialog component: đủ 4 types, 2/4 options, custom time, supplementary, ảnh placeholder, lỗi/saving, light/dark và discard **Content** trực tiếp. Agent chưa render Android Preview; user đã xác nhận editor khá ổn sau bỏ viền con. Cần retest máy thật Tạo → mở lại → Sửa → lưu → mở lại, ảnh/IME/back/loading/error.
- 31 test editor đã đối chiếu XML PASS: 16 validator + 6 Create VM + 7 Edit VM + 2 mapping. Quiz-manage còn 29 regression List/Detail PASS trong lượt XML mới; không suy ra full assemble/lint/CI hoặc E2E.

### Hồ sơ — N46.14a–N46.14c

- Ảnh 26 được chỉnh theo user: top bar **Hồ sơ** đậm; avatar/tên/email, không camera overlay/nhãn “đã xác thực”; bỏ Âm thanh & Hiệu ứng và Điều khoản & Hỗ trợ chưa có chức năng. Chỉ giữ **Chỉnh sửa thông tin cá nhân / Bảo mật tài khoản & Đổi mật khẩu** và logout viền đỏ; không thêm Top 1 giả.
- Stats chỉ **Lượt tham gia / Quiz đã tạo**: `LoadProfileStatsUseCase` tại app/domain/profile gọi song song `ProfileStatsRepository` (core:common); impl core:network đọc history role=played và my-quizzes ALL, limit=1/includeTotal=true, lấy `meta.total`. Thiếu total trả lỗi, không lấy độ dài page hoặc biến thành 0; có loading/“—”/retry. ProfileViewModel quản lý job/token guard, hủy khi đổi phiên/logout.
- Lối vào đổi ảnh chuyển vào **dialog Chỉnh sửa thông tin cá nhân**, không có item/avatar action riêng trên màn chính. Vẫn dùng Photo Picker, preview/lưu avatar riêng N42; đây không phải save atomically cùng các field text. Email là **OutlinedTextField disabled**, bỏ dòng “email không thể thay đổi”; form giới hạn chiều cao và cuộn được. Giữ PATCH delta/fullname/phone/description, guards và discard flow hiện hữu.
- Khách dùng `QuizPrimaryButton` “Đăng ký/Đăng nhập”, **không full width**. Có Preview logged-in/guest/stats loading/error/edit; fixture User có createdAt/updatedAt đầy đủ. User đã duyệt hình thức.
- `1affa01` sửa assertion test stats từng fail (`41 tests, 1 failed` trong log user); không sửa tính toán để xanh. App XML 11/10/2026 có 2 stats + 7 Profile VM + 8 Profile use case PASS, nhưng không ràng buộc SHA hoặc toàn bộ baseline mới.

### Bảo mật tài khoản — N46.15

- `cc9088a`, ảnh 27: back icon + title đậm, section Đổi mật khẩu với **mắt ẩn/hiện ở cả 3 field**, shared Primary “Cập nhật mật khẩu”. Giữ dòng cảnh báo đổi mật khẩu không tự đăng xuất thiết bị khác; không tự thêm chính sách password từ ảnh.
- Google-only chỉ có card thông báo dùng màu card chung/light token + dark Material fallback; không dựng form/action backend chưa hỗ trợ.
- Card nguy hiểm giữ nội dung thật: **vô hiệu hóa không xóa toàn bộ dữ liệu**, chưa có tự khôi phục. Không dùng lời hứa tạm khóa/xóa vĩnh viễn toàn bộ quiz trong ảnh.
- Nút Vô hiệu hóa mở dialog; **password xác nhận nhập và validate trong dialog**, lỗi server giữ dialog/error để retry. Hủy xóa password/error, không gửi DELETE; busy khóa action/dismiss, giữ cleanup-only retry và session-end navigation N43/N44. Password chỉ RAM.
- Review cuối của user: hàng **Hủy / Xác nhận** ngang nhau, chia đều như AppConfirmDialog; nhãn chỉ **Xác nhận**, không “Xác nhận vô hiệu hóa”. Implementation hiện là AlertDialog với row shared buttons, **không phải đã migrate sang AppConfirmDialog**.
- Source AccountSecurityViewModelTest có 7 case; XML còn 5 case cũ, thiếu 3 tên test mới cho validate/cancel/rejected dialog và chứa 1 tên cancel cũ. Chưa có evidence mới cho regression này; không nâng N43/N44/Google-cookie gate.

### Hoạt động — N46.16

- `798aabf`, ảnh 24: title **Hoạt động** đậm; giữ 2 role/tab của user và guest chỉ played. Count tab lấy `totalCount` server từng role; thiếu total thì bỏ số, không dùng số item đang tải hay con số trong mockup.
- `ActivityHistoryCard.kt`: tách **PlayedActivityCard** (rank/totalPlayers, score, correct/total + progress khi có dữ liệu) và **HostedActivityCard** (số người chơi/số câu hỏi, không rank/score cá nhân). Dùng chung frame/title/cover optional/mode/time; Played có host/avatar nếu có, Hosted không lặp tên chính mình. Card bo góc/nền dịu, icon mở detail; không bịa accuracy khi thiếu mẫu số/correct count.
- Toàn card mở `sessionId` như trước; giữ initial/append loading-error-retry/empty và cursor/page độc lập, session isolation/lifecycle refresh. Không đổi endpoint/auth/navigation.
- `getGameHistory(..., includeTotal: Boolean = false)` xuyên Use case → Repository → API; request total khi cursor=null, append giữ total cũ nếu response không có. Không fetch hết lịch sử để đếm; không cần backend role=all.
- Content/Preview fake data/no-op giữ pattern; user duyệt UI. ActivityViewModelTest XML có 8 PASS nhưng timestamp 11/10, không có SHA/run id; cần run mới khi chốt regression baseline.

### Chi tiết trận đấu — N46.17a–N46.17b

- `be01ee7` map optional `session_code` vào GameHistorySummary.sessionCode; giữ default null cho payload cũ. **Mã phòng chỉ để hiển thị**, route/identity vẫn sessionId. Có mã dùng mã, thiếu/rỗng dùng tên phòng; không show cả hai hoặc chế mã từ ID.
- `43ed870`, ảnh 25: title **Chi tiết trận đấu** đậm + back icon, **bỏ Quay lại bên phải**. Gộp card tổng quan/thành tích; giữ cover, mô tả snapshot, thời điểm/mode; chỉ badge **Đã hủy**, không Đã kết thúc. Review cuối đã **bỏ dòng Host và dòng “x người chơi • y câu hỏi”** để ngắn card; vẫn giữ rank/totalPlayers, score, correct/totalQuestions và streak khi có. Host chỉ “Bạn đã tổ chức trận này”, không giả thành tích riêng.
- Player/Guest không render full leaderboard; summary → **Xem lại câu trả lời**. Host giữ full bảng bằng `LeaderboardPlayerCard`, sau đó **Thống kê từng câu**. Giữ summary/answers loading-error-retry độc lập và Use case/VM/session/privacy guards N39/N44.
- History-only components: **HistorySummaryCard / HistoryReviewQuestionCard / HistoryQuestionStatCard**; `HistoryDetailPresentation.kt` projection thuần, `HistoryDetailColors.kt` dùng public palette + dark fallback, `HistoryDetailPreviewData.kt` chỉ fixture Preview. **Không sửa ReviewItemCard/QuestionStatCard hay polish FinalResultScreen**.
- Review card nền trung tính; ảnh câu hỏi optional bo góc **ngay dưới nội dung**, rồi trạng thái/điểm, đáp án và explanation/time/late. Single/multiple chỉ hiện correct keys xanh ✓ và wrong selected đỏ ✕; ẩn distractor chưa chọn. Lựa chọn đúng đã chọn vẫn xanh nếu cả bộ multi-select chưa đúng; giữ thứ tự/nhãn option snapshot, không lộ raw ID khi thiếu nội dung.
- Short/long dùng cùng renderer do review payload không có question_type: server `answered/isCorrect` quyết định trạng thái, **không so text để chấm lại**. Đúng chỉ block câu trả lời xanh; sai block đã trả lời đỏ + correct key xanh; bỏ qua trạng thái trung tính, không tạo câu trả lời sai giả. Key/nội dung thiếu có notice phù hợp.
- Host stat: **badge số N bên trái + column “x/y trả lời đúng – a%” và thanh tiến độ ngang**. N là questionIndex+1 server, y là answerCount (không totalPlayers), 0 lượt trả lời ghi rõ không có dữ liệu, không bịa 0%; invalid counts fallback chưa khả dụng. Không circular progress.
- Contract đã audit ở backend `7c103c87b4c78d817e8a7acf50fd0424edd16c79` (game.service.ts/game.repository.ts): summary có gs.* gồm session_code; perQuestion chỉ questionId/index/answerCount/correctCount, **không nội dung/ảnh/key/explanation**. Không fetch quiz hiện tại để giả snapshot lịch sử. BUG-17 min(question_index)/shuffle vẫn mở; chưa xác minh backend deployment mới.
- Screen sở hữu LazyListState; Content + card Preview bằng sample/no-op, có Player/Host, dark/narrow/large text, loading/error/disabled/empty, lựa chọn và short/long/skipped/key thiếu. Agent chưa render Android Preview. 17 presentation + 3 Detail VM + 9 identity + 7 FinalResult regression có XML PASS trong lượt mới; không suy ra E2E/privacy PASS.

### Regression fake sau includeTotal

- `93c29e3`: HostGameViewModelTest fake thêm tham số includeTotal. `da0cbf3`: FinalResultViewModelTest và HostLobbyViewModelTest fake cùng sửa; không đổi production, không bỏ test/assertion. User đã commit cả hai, main/origin/main hiện `da0cbf3`.
- Hai log trước là **compileDebugUnitTestKotlin FAILED**, chưa tới assertion. `--tests` vẫn compile toàn bộ test source của module; filter history không tránh fake lỗi trong FinalResult. Rà batch 57 file test khi search_content trả rỗng sai; 4 override trực tiếp (Activity/HostGame/FinalResult/HostLobby) đã khớp interface trong phạm vi đã đọc.
- 11 HostGame + 7 FinalResult + 2 HostLobby XML mới PASS (nằm trong tổng 56 dưới đây), không cộng lại thành run riêng.

## 6. Checklist evidence và việc còn mở

- [x] Đối chiếu source/commit N46.0–N46.17 tại main/origin/main `da0cbf3`; docs cập nhật sau `f098826`, không merge main vào docs.
- [x] User duyệt/hiệu chỉnh UI các mốc trước và Hồ sơ/Bảo mật/Hoạt động/Chi tiết trận; card tổng quan đã bỏ 2 dòng thừa. Không suy ra full visual/E2E PASS.
- [x] Evidence lịch sử của lượt docs `f098826`: 10 XML local practice/editor, 88 PASS, 0 failures/errors/skipped; giữ riêng bảng dưới, không coi là run mới.
- [ ] Thu thập full assemble/lint/CI và các suite còn lại cho baseline mới; 56 test XML mới và 88 test lịch sử không chứng minh toàn app/APK cùng SHA.
- [x] Đọc thêm 8 XML history/lobby/HostGame timestamp 15/10/2026: 56 PASS; 7 XML app cũ 11/10 có 41 PASS nhưng security VM không khớp source (chi tiết dưới).
- [ ] Chạy lại app regression cho source hiện tại, đặc biệt 7 AccountSecurityViewModelTest (3 tên case dialog mới chưa có trong XML); không dùng XML cũ 5 case để đóng checkpoint.
- [ ] Architecture debt cục bộ: SecretField ở AccountSecurityScreen hiện giữ visibility bằng remember nhưng thiếu stateless Content overload sau polish; cần rà/tách lõi theo AGENTS khi duyệt scope source, không sửa code trên docs.
- [ ] Kiểm thử auth regression: validation, visibility, loading/error, guest, forgot/register/login navigation, Google và session; không đánh Google/cookie fixed nếu happy case pass.
- [ ] Xem Preview và máy thật: light/dark, field/icon/spacing, keyboard/IME, font scale, narrow screen, TalkBack/touch targets/contrast.
- [ ] Retest lỗi Paging index trên Discover sau `d5571d6`: cuộn append, retry, đổi danh mục/sort nhanh từ list nhiều sang ít/rỗng, back/reopen; chưa có xác nhận sau sửa.
- [ ] Regression Search: submit/clear/query change/cancel, empty result, append error/retry đúng cursor, không tự retry-loop hoặc mất results.
- [ ] Debt editor hiện hữu: domain QuizDraft/Create–Update assets use cases còn dùng presentation QuestionDraft/toNewQuestion; chưa chuyển model/mapper qua boundary trong đợt UI này.
- [ ] Đối chiếu schema API với DDL/runtime backend khi retest lưu: audit `7c103c8` có API description ≤500, language không enum; DDL gốc 002_quizzes.sql khai description varchar(255), language varchar(50). Chưa xác minh DB triển khai/migration tương ứng; không coi API cho phép chuỗi tự do là độ dài vô hạn hoặc tuyên bố runtime bug đã tái hiện.
- [ ] E2E Chơi thử 4 types/skip/late/key thiếu/manual Next; Create/Edit lưu → mở lại, prefill ảnh/đáp án/custom duration, lỗi upload/retry, IME/back/rotation trong Photo Picker.
- [ ] Chốt mẫu dark và tích hợp shared components vào các màn tiếp theo theo user chọn; màn tiếp theo chưa chốt, không tự suy đoán thứ tự.
- [ ] Các màn khác, animation, sheet dark-mode và accessibility toàn app còn mở; N46 chưa hoàn tất.
- [ ] N44-OFFLINE-BOOT, Practice pause/resume, N20.6/N25/N28.5/N29/N30, privacy/session và build/deployment gates giữ trong hồ sơ N44, không waived bởi UI polish.

### Evidence lịch sử — Chơi thử và editor (lượt docs f098826)

Đã đọc 10 XML trong `feature/quiz-preview/build/test-results/testDebugUnitTest/`, `feature/quiz-manage/build/test-results/testDebugUnitTest/` và báo cáo **QuizEditorValidatorTest** tại `core/common/build/test-results/testDebugUnitTest/`. Không coi đây là toàn bộ suite core:common/toàn app.

| Suite | Tests | Failures / errors / skipped | Timestamp XML (UTC) |
| --- | --- | --- | --- |
| `QuizEditorValidatorTest` | 16 | 0 / 0 / 0 | `2026-10-11T14:22:18.947Z` |
| `CreateQuizViewModelTest` | 6 | 0 / 0 / 0 | `2026-10-11T14:22:28.995Z` |
| `EditQuizViewModelTest` | 7 | 0 / 0 / 0 | `2026-10-11T14:22:31.031Z` |
| `QuestionDraftMappingTest` | 2 | 0 / 0 / 0 | `2026-10-11T14:22:31.136Z` |
| `QuizDetailViewModelTest` | 10 | 0 / 0 / 0 | `2026-10-11T14:22:31.199Z` |
| `QuizDetailPresentationTest` | 10 | 0 / 0 / 0 | `2026-10-11T14:22:31.143Z` |
| `QuizManageListViewModelTest` | 9 | 0 / 0 / 0 | `2026-10-11T14:22:31.652Z` |
| `PreviewScoringTest` | 8 | 0 / 0 / 0 | `2026-10-11T13:22:14.901Z` |
| `QuizPreviewPresentationTest` | 8 | 0 / 0 / 0 | `2026-10-11T13:22:14.969Z` |
| `QuizPreviewViewModelTest` | 12 | 0 / 0 / 0 | `2026-10-11T13:22:14.991Z` |

**Tổng 88/88 PASS local:** 28 quiz-preview + 44 quiz-manage (15 editor + 29 List/Detail regression) + 16 core editor-validator; 0 failures/errors/skipped. Timestamps là 11/10/2026: Preview khoảng 13:22 UTC, editor/List/Detail khoảng 14:22 UTC. XML không mang git SHA/run id; thời gian gần các commit không chứng minh APK/backend cùng revision hoặc full assemble/lint/CI/E2E. Agent đọc report, không chạy Gradle.

Lượt docs trước ghi 29/29 List/Detail PASS tại 04:36 UTC ngày 11/10/2026; giữ là evidence lịch sử, lượt mới trên bổ sung editor và practice. Hai fixture Detail từng fail đã được sửa bằng tạo ViewModel trong runTest sau mock setup, không bỏ assertions/production guards. 10 normalization, 8 password-reset và 9 Search state test vẫn chưa được đối chiếu run mới trong lượt này.

### Evidence mới — history/lobby/HostGame và giới hạn app XML

Đọc 8 XML từ `feature/leaderboard`, `feature/lobby`, `feature/game-host` và riêng suite `core/network/.../GameHistoryDtosTest` trong `build/test-results/testDebugUnitTest/`. Các file dưới timestamp **15/10/2026 UTC**, không phải timestamp lượt 11/10 trước đó; chưa ràng buộc được với git SHA/run id hoặc APK/backend revision.

| Suite | Tests | Failures / errors / skipped | Timestamp XML (UTC) |
| --- | --- | --- | --- |
| `GameHistoryIdentityTest` | 9 | 0 / 0 / 0 | `2026-10-15T14:20:36.132Z` |
| `FinalResultViewModelTest` | 7 | 0 / 0 / 0 | `2026-10-15T14:20:38.986Z` |
| `GameHistoryDetailPresentationTest` | 17 | 0 / 0 / 0 | `2026-10-15T14:20:39.028Z` |
| `GameHistoryDetailViewModelTest` | 3 | 0 / 0 / 0 | `2026-10-15T14:20:39.088Z` |
| `HostLobbyViewModelTest` | 2 | 0 / 0 / 0 | `2026-10-15T14:20:40.997Z` |
| `PlayerLobbyViewModelTest` | 2 | 0 / 0 / 0 | `2026-10-15T14:20:41.265Z` |
| `HostGameViewModelTest` | 11 | 0 / 0 / 0 | `2026-10-15T14:14:08.533Z` |
| `GameHistoryDtosTest` | 5 | 0 / 0 / 0 | `2026-10-15T14:11:26.614Z` |

**Tổng mới: 56/56 PASS trong 8 XML** = 36 leaderboard + 4 lobby + 11 HostGame + 5 history DTO. Đối chiếu tên testcase với source trong phạm vi này khớp; không thay thế SHA/run binding/full CI hoặc E2E. Không phải toàn core:network; các suite khác/cookie-race không được đánh PASS theo đó.

Đọc thêm **7 XML app timestamp 11/10/2026**, tổng 41 PASS: AccountSecurityUseCasesTest 10, AccountSecurityViewModelTest 5, ActivityViewModelTest 8, ExampleUnitTest 1, LoadProfileStatsUseCaseTest 2, ProfileUseCasesTest 8, ProfileViewModelTest 7. Đây là **evidence cũ**, không gộp vào 56 thành một run 97 PASS hiện tại. Riêng security VM source có 7 case: 3 tên mới về validate/cancel/rejected dialog chưa có trong report, 1 tên cancel cũ vẫn có trong XML; cần run lại. Các suite app khác tên testcase khớp nhưng XML không có SHA nên cũng chưa chứng minh regression baseline mới.

Tổng 88 PASS practice/editor ở bảng trên tiếp tục là lượt lịch sử riêng. Agent chỉ đọc report/source, không chạy Gradle, không xác minh full assemble/lint/CI, runtime tải ảnh, E2E hoặc backend deployment trong lượt docs này.

## 7. Quy trình cho các mốc tiếp theo

1. User chọn một màn và cung cấp ảnh local; đọc ảnh trước khi edit, không dùng ảnh màn khác để đoán.
2. Đọc source mới nhất trên main; kiểm tra callback/UiState/Intent/Effect, tái sử dụng component đã duyệt; không đổi nghiệp vụ khi chỉ polish.
3. Implement đúng phạm vi một màn; Preview gọi Content stateless với fake data/no-op; báo giới hạn kiểm chứng.
4. User duyệt/hiệu chỉnh Preview, kiểm thử và commit source riêng từng mốc. Không tự commit/push/chuyển nhánh.
5. Sau user checkout docs, đối chiếu hash mới và cập nhật plan, AGENTS, structure/design doc và sổ mốc này bằng targeted edit; không merge/cherry-pick toàn bộ main vào docs.

`design/` là ảnh local, không phải nguồn bắt buộc có trong clone. Main có `/design/` trong `.gitignore`; `.git/info/exclude` local bỏ qua cùng thư mục trên mọi nhánh, không được commit/push. Nếu ảnh thiếu ở máy khác, yêu cầu user cung cấp, không báo đã xem ảnh.
