# N46 — UI polish theo từng mốc nhỏ (WIP)

> Baseline source Android: `main` tại `d5571d6` (Search/Discover). Lần cập nhật docs trước `8bf89d0` ghi đến Register `ee8701c`; lần này đối chiếu thêm 7 commit `ee8701c..d5571d6` (14 commit tổng cộng sau `cd018bb`). Đây là baseline source đã commit, không phải SHA APK đã kiểm thử.
> N46 được triển khai từng phần theo user duyệt, không làm một mạch toàn app. N44 vẫn PARTIAL, N45/M6 và release gates chưa đóng; xem `N44_E2E_REPORT.md`.

## 1. Cách đọc trạng thái

- “Đã triển khai/commit” chỉ xác nhận source có trên main.
- “User duyệt UI” không thay thế build, lint, unit test, Compose UI test hoặc E2E.
- Agent chưa chạy Gradle/render Compose và chưa đối chiếu log/run id build/CI mới cho đợt N46 này. Không suy ra PASS từ việc commit hoặc có `@Preview`.
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
| N46.next | Màn tiếp theo do user chọn + ảnh mẫu | Chưa chốt | Kiểm tra Search/Discover và lỗi Paging sau sửa trước; không tự chuyển màn hoặc đóng N46 |

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

Gradient result từ CSS: `#21113E` tại 0%, `#4C2B8D` tại 48%, `#25144F` tại 100%. Chỉ có token, chưa coi Final Result đã được polish. Contrast chữ trắng trên tím brand cần quyết định/kiểm tra accessibility trước khi đóng N46; không tự đổi màu user đã duyệt.

## 4. Component đã có — core:ui

| File / API | Ownership và lưu ý |
| --- | --- |
| `QuizButtons.kt` | `QuizPrimaryButton`, `QuizSecondaryButton`, `QuizDangerButton`; stateless; loading/disabled, min-height 52dp, radius 16dp. Secondary có leading/trailing icon slot và contentColor tùy chọn cho Google |
| `CustomClickableText.kt` | Stateless; chỉ đoạn action là link; `LinkAnnotation.Clickable` + `TextLinkStyles`; không dùng `ClickableText` deprecated, không nối chuỗi null thành chữ “null” |
| `SixCharacterCode.kt` | Normalize thuần: OTP 6 ASCII digits; room 6 ASCII alphanumeric uppercase; không pad/auto-submit |
| `SixCharacterCodeField.kt` | Wrapper chỉ giữ focus; text/selection nằm ngoài. `SixCharacterCodeFieldContent` là lõi stateless và là đích Preview |
| `AnswerOptionItem.kt` | Single-choice row stateless; selected tách khỏi Correct/Incorrect. Caller quyết định policy reveal; không suy luận đáp án đúng từ selection |
| `QuizListCard.kt` | Card ngang (đổi tên từ QuizSummaryCard ở `9417aad`); Search dùng card này, dùng được cho Library list; primitive props, rating optional, không bịa default |
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

- Mẫu 08, nhưng user xác nhận dùng **QuizListCard** (card ngang chuẩn bị trước DiscoveryQuizCard). Field tìm kiếm rounded tím + nút xóa; giữ nút submit/IME Search và navigation detail thật.
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

## 6. Checklist evidence và việc còn mở

- [x] Đối chiếu source/commit các mốc N46.0–N46.8 tại main `d5571d6`; lần này bổ sung 7 commit sau Register, giữ bản docs tách khỏi main.
- [x] User xác nhận UI Login/Register/Home ổn; các mốc đã commit. Search/Discover đã commit theo user, không suy ra visual/E2E PASS.
- [ ] Thu thập build/lint/unit-test/CI log hoặc run id cho baseline N46 mới; không tái dùng XML N43/N44 làm kết quả N46. 10 normalization + 8 password-reset validation + 9 Search state case là inventory source, không phải báo cáo PASS.
- [ ] Kiểm thử auth regression: validation, visibility, loading/error, guest, forgot/register/login navigation, Google và session; không đánh Google/cookie fixed nếu happy case pass.
- [ ] Xem Preview và máy thật: light/dark, field/icon/spacing, keyboard/IME, font scale, narrow screen, TalkBack/touch targets/contrast.
- [ ] Retest lỗi Paging index trên Discover sau `d5571d6`: cuộn append, retry, đổi danh mục/sort nhanh từ list nhiều sang ít/rỗng, back/reopen; chưa có xác nhận sau sửa.
- [ ] Regression Search: submit/clear/query change/cancel, empty result, append error/retry đúng cursor, không tự retry-loop hoặc mất results.
- [ ] Chốt mẫu dark và tích hợp shared components vào các màn tiếp theo theo user chọn; màn tiếp theo chưa chốt, không tự suy đoán thứ tự.
- [ ] Các màn khác, animation, sheet dark-mode và accessibility toàn app còn mở; N46 chưa hoàn tất.
- [ ] N44-OFFLINE-BOOT, Practice pause/resume, N20.6/N25/N28.5/N29/N30, privacy/session và build/deployment gates giữ trong hồ sơ N44, không waived bởi UI polish.

## 7. Quy trình cho các mốc tiếp theo

1. User chọn một màn và cung cấp ảnh local; đọc ảnh trước khi edit, không dùng ảnh màn khác để đoán.
2. Đọc source mới nhất trên main; kiểm tra callback/UiState/Intent/Effect, tái sử dụng component đã duyệt; không đổi nghiệp vụ khi chỉ polish.
3. Implement đúng phạm vi một màn; Preview gọi Content stateless với fake data/no-op; báo giới hạn kiểm chứng.
4. User duyệt/hiệu chỉnh Preview, kiểm thử và commit source riêng từng mốc. Không tự commit/push/chuyển nhánh.
5. Sau user checkout docs, đối chiếu hash mới và cập nhật plan, AGENTS, structure/design doc và sổ mốc này bằng targeted edit; không merge/cherry-pick toàn bộ main vào docs.

`design/` là ảnh local, không phải nguồn bắt buộc có trong clone. Main có `/design/` trong `.gitignore`; `.git/info/exclude` local bỏ qua cùng thư mục trên mọi nhánh, không được commit/push. Nếu ảnh thiếu ở máy khác, yêu cầu user cung cấp, không báo đã xem ảnh.
