# N46 — UI polish theo từng mốc nhỏ (WIP)

> Baseline source Android: `main` tại `ee8701c` (Register). Đối chiếu 7 commit mới sau `cd018bb`: 6 commit UI và 1 commit ignore. Đây là baseline source đã commit, không phải SHA APK đã kiểm thử.
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
| N46.next | Màn tiếp theo do user chọn + ảnh mẫu | Chưa có | Chưa triển khai; không tự chuyển màn hoặc đóng N46 |

Tên mốc là checkpoint nội bộ của N46, không thay đổi kế hoạch N1–N50 hoặc tạo release gate mới.

## 3. Font, palette và theme

- Inter v4.1: `inter_regular.ttf` (400), `inter_medium.ttf` (500), `inter_semibold.ttf` (600), `inter_bold.ttf` (700), tại `core/ui/src/main/res/font/`; license tại `core/ui/licenses/Inter-LICENSE.txt`.
- `Type.kt` gắn `InterFontFamily` cho đủ 15 typography role, giữ các metric gốc trừ style màn đang được duyệt. `MainActivity` bọc root `AppNavGraph` trong `MyQuizAppTheme`.
- Chỉ subtitle được user chỉ rõ từ Figma dùng Inter Regular 14sp, line-height 145% (20.3sp), letter-spacing 0; không áp metric này cho mọi role.
- `FrontendColors` giữ palette light và gradient result tách khỏi Material roles. `ComponentColors` là resolver nội bộ core:ui với dark fallback; không gọi palette light là thiết kế dark đã duyệt.

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
| `QuizSummaryCard.kt` | Card ngang theo mẫu; primitive props, metadata/rating caller cung cấp; rating null không tự bịa default |
| `LeaderboardPlayerCard.kt` | Hạng 1/2/3 hiển thị 🥇/🥈/🥉; hạng khác giữ label. Không tính rank/score hoặc sắp xếp; score/delta optional; có semantics hạng |
| `ComponentGalleryPreview.kt` | Bản tổng hợp Light/Dark và narrow/large-text; không phải màn app |

- `QuizCardItem` cũ dạng dọc và `HomeSectionRow` giữ API hiện hữu với model core:common; chưa mass-migrate các màn cũ sang card ngang.
- Đã bổ sung/chuẩn hóa Preview cho `AuthRequiredDialog`, `Avatar`, `HomeSectionRow`, `QuestionImage`, `QuizCardItem`, `RemoteImage`, `SettingSwitchRow`.
- `Avatar`/`RemoteImage` dùng `LocalInspectionMode` để Preview không cần fetch mạng; runtime vẫn dùng Coil. Không xem placeholder Preview là test tải ảnh thật.
- `SixCharacterCodeTest.kt`: 10 JUnit case cho OTP/room, paste/truncate, Unicode, separators và xóa ký tự. Source đã commit, kết quả chạy chưa được agent xác minh.
- Component mới chưa tự động thay thế UI ở gameplay/join/OTP/leaderboard cũ. Integrate từng màn sau khi user duyệt, giữ nguyên contract và policy server.

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

## 6. Checklist evidence và việc còn mở

- [x] Đối chiếu source/commit các mốc N46.0–N46.4; giữ bản docs tách khỏi main.
- [x] User xác nhận UI Login ổn; Register oke và đã commit.
- [ ] Thu thập build/lint/unit-test/CI log hoặc run id cho baseline N46 mới; không tái dùng XML N43/N44 làm kết quả N46.
- [ ] Kiểm thử auth regression: validation, visibility, loading/error, guest, forgot/register/login navigation, Google và session; không đánh Google/cookie fixed nếu happy case pass.
- [ ] Xem Preview và máy thật: light/dark, field/icon/spacing, keyboard/IME, font scale, narrow screen, TalkBack/touch targets/contrast.
- [ ] Chốt mẫu dark và tích hợp shared components vào các màn tiếp theo theo user chọn.
- [ ] Các màn khác, animation, sheet dark-mode và accessibility toàn app còn mở; N46 chưa hoàn tất.
- [ ] N44-OFFLINE-BOOT, Practice pause/resume, N20.6/N25/N28.5/N29/N30, privacy/session và build/deployment gates giữ trong hồ sơ N44, không waived bởi UI polish.

## 7. Quy trình cho các mốc tiếp theo

1. User chọn một màn và cung cấp ảnh local; đọc ảnh trước khi edit, không dùng ảnh màn khác để đoán.
2. Đọc source mới nhất trên main; kiểm tra callback/UiState/Intent/Effect, tái sử dụng component đã duyệt; không đổi nghiệp vụ khi chỉ polish.
3. Implement đúng phạm vi một màn; Preview gọi Content stateless với fake data/no-op; báo giới hạn kiểm chứng.
4. User duyệt/hiệu chỉnh Preview, kiểm thử và commit source riêng từng mốc. Không tự commit/push/chuyển nhánh.
5. Sau user checkout docs, đối chiếu hash mới và cập nhật plan, AGENTS, structure/design doc và sổ mốc này bằng targeted edit; không merge/cherry-pick toàn bộ main vào docs.

`design/` là ảnh local, không phải nguồn bắt buộc có trong clone. Main có `/design/` trong `.gitignore`; `.git/info/exclude` local bỏ qua cùng thư mục trên mọi nhánh, không được commit/push. Nếu ảnh thiếu ở máy khác, yêu cầu user cung cấp, không báo đã xem ảnh.
