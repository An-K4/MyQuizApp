# Stateful/Stateless Compose Refactor — 28/8/2026

## 1. Bối cảnh

Sau N17, toàn bộ Compose UI được audit trước khi bắt đầu N18. Mục tiêu là lấy `LoginScreen` làm mẫu chung: tách integration boundary khỏi UI thuần để Screen dễ đọc, Preview được không cần Hilt và thuận lợi cho Compose UI test.

Đây là refactor kiến trúc, không thay đổi backend contract, ViewModel, repository, domain behavior hoặc UX chủ đích; vì vậy không đánh số N17.5.

## 2. Baseline đã chốt

### Stateful `XxxScreen`

Được phép và chịu trách nhiệm:

- Lấy `hiltViewModel()` và collect state bằng `collectAsStateWithLifecycle()`.
- Collect one-off Effect và chuyển thành navigation/snackbar.
- Sở hữu Activity Result launcher/Photo Picker, `BackHandler`, lifecycle effect và platform API.
- Sở hữu UI state cần hoist như password visibility, menu expanded hoặc discard dialog.
- Tạo/quan sát focus, list state và pagination bằng `snapshotFlow` khi đó là integration behavior.
- Wiring xong phải gọi `XxxScreenContent`; không dựng phần lớn UI chi tiết ở wrapper.

### Stateless `XxxScreenContent`

- Chỉ nhận `UiState`, primitive/UI value và callback hoặc typed Intent.
- Không lấy ViewModel/Hilt, không collect Flow, không sở hữu `NavController`.
- Không thực hiện business logic, repository/network/database I/O hoặc tự điều hướng.
- Có thể nhận UI object do wrapper sở hữu như `LazyListState`, `LazyPagingItems` hoặc `SnackbarHostState` nếu cần render.
- Preview gọi trực tiếp content với fake state/no-op callback; ưu tiên Light/Dark.

### Reusable component

- Stateless mặc định: value xuống, callback đi lên.
- Nếu cần convenience wrapper giữ local UI state thì phải có stateless content API lõi và tên rõ ràng, ví dụ `QuestionTypeMenuButton`/`QuestionTypeMenuButtonContent`.
- Component chỉ dùng trong một feature giữ ở `feature/.../presentation/components`; chỉ đưa lên `core:ui` khi thực sự dùng chung giữa nhiều module và API không phụ thuộc model/intent của feature.

## 3. Inventory 14 Screen

Đã đúng pattern trước audit:

1. `LoginScreen`
2. `RegisterScreen`
3. `SplashScreen`

Được chuẩn hóa trong đợt refactor:

4. `ProfileScreen`
5. `ForgotPasswordScreen`
6. `OtpVerificationScreen`
7. `ResetPasswordScreen`
8. `HomeScreen`
9. `SearchScreen`
10. `CreateQuizScreen`
11. `CreateRoomScreen`
12. `EditQuizScreen`
13. `QuizDetailScreen`
14. `QuizManageListScreen`

`HostLobbyPlaceholder` đã là UI stateless thuần nên không cần stateful wrapper.

## 4. Những điểm refactor đáng chú ý

- `SearchScreen`: wrapper sở hữu `FocusRequester`, `LazyListState`, autofocus và pagination observer; content chỉ render và phát callback.
- `CreateQuizScreen`/`EditQuizScreen`: Photo Picker ở wrapper; editor UI dùng chung `QuizEditorContent`.
- `EditQuizScreen`: `BackHandler` và discard dialog state ở wrapper.
- `QuizDetailScreen`/`QuizManageListScreen`: lifecycle refresh và `rememberSaveable` skip-first-resume ở wrapper.
- `QuizManageListScreen`: wrapper collect `PagingData`, giữ sort-menu expansion; content nhận `LazyPagingItems` để render.
- `ResetPasswordScreen`: password visibility được hoist khỏi content.
- `QuestionTypeMenuButton` và game-config choice field: giữ convenience wrapper nhưng bổ sung stateless content API lõi.
- Các component `Avatar`, `HomeSectionRow`, `QuizCardItem`, `SettingSwitchRow`, `QuestionEditorCard`, `ImagePickerSection` và `HostLobbyPlaceholder` vốn đã stateless.

## 5. Shared quiz editor

Tạo `feature/quiz-manage/.../presentation/components/QuizEditorContent.kt` để gom UI editor dùng chung giữa Create/Edit Quiz:

- Metadata quiz, cover picker và danh sách câu hỏi dùng một implementation.
- Màn Create/Edit chỉ khác state nguồn, submit/effect và dirty/back behavior.
- Component dùng model/callback của quiz-manage nên giữ trong feature, không đẩy lên `core:ui`.

## 6. Checklist cho Screen/component mới

- [ ] `XxxScreen` chỉ làm integration/wiring và gọi `XxxScreenContent`.
- [ ] `XxxScreenContent` không tham chiếu ViewModel/Hilt/Flow/NavController.
- [ ] State đi xuống; event/Intent đi lên.
- [ ] One-off Effect chỉ được xử lý ở wrapper.
- [ ] Platform launcher, lifecycle hook và `BackHandler` ở wrapper.
- [ ] Business state nằm trong `UiState`/ViewModel; transient display state được hoist hợp lý.
- [ ] Reusable component stateless mặc định.
- [ ] Preview gọi content trực tiếp và không cần dependency runtime.
- [ ] Trace từng Intent/callback về một UI trigger thật.
- [ ] Chạy `git diff --check`, compile và test luồng trên máy thật trước khi merge.

## 7. Kết quả

- Audit đủ 14 Screen hiện có.
- Chuẩn hóa 11 Screen; 3 Screen đã đúng được giữ nguyên.
- Tạo một shared `QuizEditorContent`, giảm duplication giữa Create/Edit Quiz.
- Build/test máy thật đã xanh và thay đổi đã được push trước khi cập nhật tài liệu.
- Baseline này là điều kiện review cho mọi UI mới từ N18 trở đi.
