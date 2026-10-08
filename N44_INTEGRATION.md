# N44 — Integration gate và session isolation

## Trạng thái

**WIP — chưa chốt N44, chưa feature freeze N45.** Source chặng 1–2 đã triển khai; chờ build/unit test/máy thật. Chặng 3 đã có diagnostic test nhưng CHƯA chạy, chưa sửa cookie transport. Chặng 4 mới có matrix, chưa thực hiện E2E.

- Android baseline: `ab6621bccdd4c6f452cdadacf30ba2ecf1e837a2` trên main; patch N44 hiện chưa commit.
- Backend source audit: `7c103c87b4c78d817e8a7acf50fd0424edd16c79`; chưa xác nhận deployment revision.
- N43: đã đọc XML local, 23 test trong 5 suite security và 8 SessionRepositoryTest đều không failures/errors/skips. Hai test retirement mới nằm trong suite 8, tổng 25 test bổ sung N43 có báo cáo PASS. Không phải test N44, không phải agent vừa chạy lại, không thay thế CI/lint/assemble/E2E.
- Canonical roadmap/design/structure ở nhánh docs. File này là handoff source trên main; không dùng các ghi chú lịch sử trong AGENTS để thay thế trạng thái WIP này.

## Chặng 1 — History/session isolation

- `SessionSnapshot`/`SessionIdentity`: state và generation đọc cùng critical section ở SessionRepositoryImpl; identity không đổi chỉ vì sửa tên/avatar.
- Activity và History Detail đọc session qua Use case; không inject/gọi Repository trong ViewModel.
- Đổi identity: retire epoch, hủy job cũ, reset state và tải ngay cho identity mới. Không để cờ in-flight cũ chặn account mới.
- Mỗi response kiểm tra active job, epoch và identity hiện hành trước khi publish; Activity thêm request id theo role để chặn trang cũ trong cùng phiên.
- Cùng user đăng nhập lại là lifetime mới. StateFlow có thể conflate User bằng nhau; intent và result vẫn đọc snapshot hiện tại để bắt generation mới.
- Guest dùng UUID đã có qua use case lịch sử; không sinh UUID chỉ vì mở history; không dùng socket token.

## Chặng 2 — Logout cleanup

- Profile session read/capture đi qua ObserveProfileSessionUseCase; giữ nguyên profile/avatar mutation use cases.
- `resetMainGraphAfterSessionEnd` dùng chung cho logout thường và AccountSecurity session-ended: xóa saved tab stacks Home/MyQuizzes/Activity/Profile, bỏ MainGraph cũ không save state, dựng MainGraph mới không restore.
- Helper navigation chỉ điều hướng, không gọi repository/API/cleanup nghiệp vụ.
- Không tự thay đổi cơ chế transport logout hoặc các ViewModel ngoài scope. TokenAuthenticator/PersistentCookieJar và backend không sửa.

## Test bổ sung — CHƯA CHẠY

16 regression test:
- ActivityViewModelTest: +5 (late played success, late hosted error sang guest, same-user generation, stale append sau refresh, refresh lỗi cũng retire loading-more).
- GameHistoryIdentityTest: 9 (summary success/error cũ, answers success/error cũ, dedupe retry, same-user generation, profile update không reload, Unknown, guest UUID).
- SessionRepositoryTest: +2 (snapshot guest/same-account lifetime; profile publish giữ identity).
- ProfileViewModelTest, 3 test Activity cũ và 3 test GameHistoryDetail cũ đã đổi constructor qua Use case; phải chạy lại toàn bộ suite cũ.

2 opt-in diagnostic test trong `N44CookieRaceReproductionTest`:
- Desired invariant: terminal refresh cũ không clear cookie của login mới.
- Desired invariant: Set-Cookie từ refresh cũ không phục hồi cookie sau logout local.
- **Mặc định SKIPPED**, chỉ chạy khi env `N44_COOKIE_RACE_DIAGNOSTICS=1`.
- **Dự kiến FAIL trên transport chưa sửa**. FAIL assertion đúng chỗ là bằng chứng tái hiện cần phân tích, không phải feature gate PASS. Timeout/compile error không phải bằng chứng xác nhận cookie race.
- Sau khi có failure xác nhận, mới thiết kế/fix transport dưới domain/data boundary; sau fix chuyển thành regression test chạy thường. Không đổi assertion để "cho xanh".
- Dùng fixture giả và MockWebServer cục bộ; không gọi backend thật hay dùng credential thật.

## Lệnh kiểm chứng trên máy Windows

PowerShell tại repo (MCP shell chưa cho agent chạy Gradle):

```powershell
.\gradlew.bat :app:testDebugUnitTest :feature:leaderboard:testDebugUnitTest :core:network:testDebugUnitTest assembleDebug
```

Diagnostic riêng (sẽ có khả năng BUILD FAILED do invariant chưa được bảo vệ):

```powershell
$env:N44_COOKIE_RACE_DIAGNOSTICS = "1"
try {
    .\gradlew.bat :core:network:testDebugUnitTest --tests "*N44CookieRaceReproductionTest" --rerun-tasks
} finally {
    Remove-Item Env:N44_COOKIE_RACE_DIAGNOSTICS -ErrorAction SilentlyContinue
}
```

Không commit/push/chốt milestone từ source inspection. Lưu revision/build id, XML/log và thiết bị/OS/backend revision cho mỗi lần test. Chạy lại test thường sau diagnostic để report gate không bị trộn với expected-red diagnostics. CI lint/full regression vẫn cần kiểm tra riêng.

## Matrix tích hợp

| ID | Vai trò / trường hợp | Kỳ vọng | Trạng thái | Bằng chứng |
| --- | --- | --- | --- | --- |
| ARCH-01 | Activity/Profile/History Detail | Không Repository ở ViewModel, qua Use case | PASS (source review, chưa build) | Source patch N44 |
| UNIT-01 | 16 test N44 và suite cũ | Tất cả regression xanh | NOT RUN | Chờ XML mới |
| BUILD-01 | Debug build + DI | Build thành công | FAIL ở lần chạy đầu; chờ chạy lại | :app:compileDebugUnitTestKotlin lỗi suy luận Continuation<Result.Success> trong ActivityViewModelTest; đã khai báo generic Result tường minh ở Activity và History identity tests, chưa xác nhận build lại |
| CI-01 | Lint/full CI | Không regression toàn project | NOT RUN | Chờ run/commit |
| SESS-01 | A → logout → B | Back không về state A; Library/Profile/Activity không giữ dữ liệu A | NOT RUN | Máy thật |
| SESS-02 | A logout offline → B login | Cleanup local và back stack vẫn đúng | NOT RUN | Máy thật + mạng |
| SESS-03 | History summary/answers đang tải → đổi identity | Không hiện result/error A, B không bị treo loading | NOT RUN | Regression + máy thật |
| SESS-04 | Same-account relogin / rotate | Không nhận response lifetime cũ | NOT RUN | Regression + máy thật |
| SESS-05 | Logout → guest → Google login | Tất cả tab nhận phiên LoggedIn ngay, không cần restart/relogin | FAIL (user báo lỗi ngắt quãng); OPEN | Thư viện/Hoạt động/Profile đôi khi vẫn guest sau Google login báo thành công; chưa có timeline/log để chốt nguyên nhân |
| COOKIE-01 | Refresh cũ lỗi sau login mới | Cookie mới còn nguyên | NOT RUN | Opt-in diagnostic |
| COOKIE-02 | Refresh Set-Cookie đến sau logout | Cookie store không bị phục hồi | NOT RUN | Opt-in diagnostic |
| HIST-01 | User played/hosted | Cursor độc lập, load more/refresh đúng | NOT RUN | Máy thật |
| HIST-02 | Guest played | UUID cũ, không hosted, không tạo UUID vì mở tab | NOT RUN | Máy thật |
| HIST-03 | Host summary / Player own answers | Đúng viewer, review disabled không lộ đáp án | NOT RUN | Máy thật |
| MEDIA-01 | Câu có/không có ảnh, ảnh lỗi | Detail/preview/Host/Player nhất quán, không crash | NOT RUN | Máy thật |
| PREVIEW-01 | 4 loại câu, skip/late/restart/owner edit | Không tạo room/history/play count | NOT RUN | Máy thật |
| PROFILE-01 | Profile delta/avatar | Save, preview, verify không replay, giữ draft hợp lệ | NOT RUN | Máy thật |
| SEC-01 | Local account đổi password | Validation, sai password, success giữ phiên | NOT RUN | Account test riêng |
| SEC-02 | Google-only / local linked Google | Khả năng thao tác đúng provider | NOT RUN | Account test riêng |
| SEC-03 | Deactivate cancel/confirm/uncertain/cleanup retry | Không replay DELETE, graph sạch | NOT RUN | Chỉ account dùng để test xóa |
| GAME-01 | Host + user Player + guest, Classic | Create/join/lobby/game/end/result/review/history xuyên suốt | NOT RUN | 2–3 client thật |
| GAME-02 | Self-paced/Solo/Survival/Marathon | Progress/timer/scoring server-authoritative | NOT RUN; có backend gates dưới | Chưa E2E |
| NET-01 | Disconnect/retry/reconnect | Rejoin/sync, terminal UI, không duplicate answer | NOT RUN; có N25/N30 gates | Máy thật + timeline redacted |
| REST-01 | Public results với showLeaderboard=never | Backend không serialize board cho Player | BLOCKED (source policy gap) | game.service.ts |

## Bug ledger / gate N45

| ID | Mức / phạm vi | Trạng thái | Gate |
| --- | --- | --- | --- |
| N44-HIST | P1 stale history cross-identity | Source fix, pending regression/E2E | Chưa được đóng |
| N44-NAV | P1 saved private stacks sau logout | Source fix, pending máy thật | Chưa được đóng |
| N44-GOOGLE-SESSION | P1 Google relogin không đồng bộ trạng thái phiên | OPEN, user đã gặp ngắt quãng; audit trước fix | Không chốt session gate; chưa kết luận cookie race hay UI/DI |
| N44-COOKIE | P1 candidate transport cookie races | Chờ opt-in reproducer; chưa fix | Chưa được đóng/chưa xác nhận runtime |
| N44-TOKEN | Credential lifecycle debt: token trong route/SavedStateHandle | Ngoài scope đã duyệt | Cần duyệt riêng và quyết định gate |
| N44-ARCH | Repository trực tiếp ở các VM khác | Ngoài scope đã duyệt | Không đánh dấu toàn app clean |
| N20.6 | Không endpoint active game | BLOCKED backend | Resume active game chưa đạt |
| N25 | Old socket disconnect/presence race | BLOCKED backend | Reconnect gate chưa đạt |
| N28.5 | Host self-paced delta lives/streak + timeout/presence | BLOCKED backend | Host monitoring gate chưa đạt |
| N29 | Pause/resume shared snapshot, thiếu Player-specific state/order | BLOCKED backend | Self-paced pause/resume chưa đạt |
| N30 | Player renewal + shuffled snapshot sync | BLOCKED backend | Recovery Player chưa đạt |
| POLICY-RESULT | Public REST results serialize leaderboard bất kể never | BLOCKED backend, privacy policy | Không dùng UI hide làm bằng chứng fix |

Backend gate được xác nhận từ source main, chưa xác nhận trên deployment. `player:sync` có đọc index riêng nhưng `snapshot` vẫn chọn questions[index] không áp shuffled order riêng; không được ghi nhầm thành "hoàn toàn không có Player index". Full host board có lives/streak; thiếu ở delta và các refresh paths, không phải thiếu mọi payload.

## Bug mới — Google relogin vẫn hiển thị guest (N44-GOOGLE-SESSION)

- User báo sau bản sửa generic test: “có vẻ ổn”, nhưng logout về guest → đăng nhập lại Google báo thành công → đôi khi Thư viện/Hoạt động/Profile vẫn guest và hiện nút đăng nhập.
- Thoát app vào lại hoặc đăng nhập lần nữa có thể khôi phục. Tần suất/chế độ mạng/account/device chưa được ghi nhận.
- Đây là lỗi chức năng đã được user quan sát; agent chưa tái hiện hoặc có Logcat/network timeline. Không đồng nhất thành công của Google credential picker với thành công backend cho tới khi đối chiếu log.
- Audit cần trace credential → API login → lưu cookie → onAuthenticated → /users/me refresh → SessionState → các tab và DI singleton. Không thêm delay, force-refresh hay fix riêng từng tab để che nguyên nhân.
- Commit phần history isolation/navigation cleanup hiện tại không đóng bug này. Không sửa production thêm trong lượt ghi nhận/audit.

## Test nhanh ưu tiên

1. Account A mở Library (đổi keyword/sort), Activity và Profile; logout. Back không về màn/state riêng tư cũ.
2. Login B, mở lại các tab; không thấy dữ liệu/draft/filter cũ của A. Guest vẫn chỉ có played history.
3. Load history detail/answers trên mạng chậm rồi đổi phiên/đăng nhập lại; không treo loading hoặc lóe dữ liệu cũ.
4. Profile edit/save và avatar preview/confirm/verify chạy như N42; security change password giữ phiên; chỉ dùng account disposable để test deactivate.
5. Host + Player user + guest chạy Classic xuyên suốt. Các mode/reconnect có blocker phải ghi timeline, không tự kết luận PASS từ happy path.

**N44 chỉ được chốt khi có test evidence mới và quyết định rõ cho các blocker; không tự chuyển sang N45/N46.**
