# N44 — Integration gate và session isolation

## Trạng thái

**PARTIAL — đã có báo cáo manual E2E, backlog/gates chưa đóng.** N44 đã có báo cáo manual E2E PARTIAL cho lượt user ghi 08/10/2026 22:00; xem `N44_E2E_REPORT.md`. Lỗi mới N44-OFFLINE-BOOT: cold-start offline làm Hoạt động/Hồ sơ treo loading; Practice pause/resume vẫn nhảy về câu đầu. Một số case PARTIAL/NOT RUN/BLOCKED hoặc dùng evidence lịch sử; APK SHA/backend deployment/full CI chưa xác nhận. Theo quyết định user, lưu backlog và chuyển sang nghiên cứu giao diện N46; đây là ngoại lệ thứ tự research, không chốt N45/M6, không miễn trừ release blockers. Google/cookie race vẫn DEFERRED, chưa fix; 16 regression N44 và 25 test N43 XML PASS là evidence trước đó.

- Android source đã commit: `3ea792acda25d5ebf4a7ec2ea23aceb60e7326a9` trên main (`fix: isolate history by session and reset navigation on logout`). Baseline N43: `ab6621bccdd4c6f452cdadacf30ba2ecf1e837a2`. Ghi chú audit/diagnostic cập nhật sau commit; chưa có transport fix.
- Backend source audit: `7c103c87b4c78d817e8a7acf50fd0424edd16c79`; chưa xác nhận deployment revision.
- N43: đã đọc XML local, 23 test trong 5 suite security và 8 SessionRepositoryTest đều không failures/errors/skips. Hai test retirement mới nằm trong suite 8, tổng 25 test bổ sung N43 có báo cáo PASS. Không phải test N44, không phải agent vừa chạy lại, không thay thế CI/lint/assemble/E2E.
- Báo cáo này cùng AGENTS/roadmap/design/structure đã merge trên nhánh docs. Main chỉ source/config/tests; không dùng ghi chú lịch sử để thay thế trạng thái WIP hiện tại.

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

## Test bổ sung — regression PASS; cookie-race diagnostic FAIL đã tái hiện

16 regression test bổ sung đã có XML local PASS sau sửa generic (Activity 8/8, GameHistoryIdentity 9/9, SessionRepository 10/10; không failures/errors/skips). Các suite cũ Profile 6/6 và GameHistoryDetail 3/3 cũng PASS. Báo cáo có timestamp 2026-10-08; đây là output user chạy, không phải agent tự chạy. Chưa có log đầy đủ để chốt assembleDebug/lint/CI.

Phạm vi 16 test bổ sung:
- ActivityViewModelTest: +5 (late played success, late hosted error sang guest, same-user generation, stale append sau refresh, refresh lỗi cũng retire loading-more).
- GameHistoryIdentityTest: 9 (summary success/error cũ, answers success/error cũ, dedupe retry, same-user generation, profile update không reload, Unknown, guest UUID).
- SessionRepositoryTest: +2 (snapshot guest/same-account lifetime; profile publish giữ identity).
- ProfileViewModelTest, 3 test Activity cũ và 3 test GameHistoryDetail cũ đã đổi constructor qua Use case; phải chạy lại toàn bộ suite cũ.

2 opt-in diagnostic test trong `N44CookieRaceReproductionTest`:
- Desired invariant: terminal refresh cũ không clear cookie của login mới.
- Desired invariant: Set-Cookie từ refresh cũ không phục hồi cookie sau logout local.
- **Mặc định SKIPPED**, chỉ chạy khi env `N44_COOKIE_RACE_DIAGNOSTICS=1`.
- **Đã FAIL 2/2 đúng assertion trên transport chưa sửa**: XML timestamp `2026-10-08T08:18:08.030Z`, tests=2, failures=2, errors=0, skipped=0. Đây là bằng chứng tái hiện, không phải feature gate PASS; không phải compile error hoặc timeout.
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
| UNIT-01 | 16 test N44 và các suite cũ đã đối chiếu | Các test trong phạm vi đã đọc xanh | PASS (XML local) | Activity 8, GameHistoryIdentity 9, SessionRepository 10, Profile 6, GameHistoryDetail 3: không failures/errors/skips; chưa phải full CI |
| BUILD-01 | Debug build + DI | Build thành công | Lỗi compile test trước đã vượt qua; assemble chưa đối chiếu log | Sau sửa generic có XML PASS mới ở app/leaderboard/network; user báo “có vẻ ổn”; chưa có full BUILD SUCCESSFUL/lint/CI để chốt |
| CI-01 | Lint/full CI | Không regression toàn project | Chưa có evidence được đối chiếu | Commit source đã có; cần CI run id/log |
| SESS-01 | A → logout → B | Back không về state A; Library/Profile/Activity không giữ dữ liệu A | NOT RUN | Máy thật |
| SESS-02 | A logout offline → B login | Cleanup local và back stack vẫn đúng | NOT RUN | Máy thật + mạng |
| SESS-03 | History summary/answers đang tải → đổi identity | Không hiện result/error A, B không bị treo loading | NOT RUN | Regression + máy thật |
| SESS-04 | Same-account relogin / rotate | Không nhận response lifetime cũ | NOT RUN | Regression + máy thật |
| SESS-05 | Logout → guest → Google login | Các tab nhận LoggedIn, không cần restart/relogin | DEFERRED theo user; latest capture không tái hiện | 5/5 Google login success; không terminal clear/Guest publish sau login trước logout kế tiếp. Lỗi user từng gặp chưa được fix/chốt nguyên nhân |
| COOKIE-01 | Refresh cũ lỗi sau login mới | Cookie mới còn nguyên | FAIL (diagnostic đã tái hiện); fix DEFERRED theo user | Assertion line 63: expected new-login-fixture, actual null |
| COOKIE-02 | Refresh Set-Cookie đến sau logout | Cookie store không bị phục hồi | FAIL (diagnostic đã tái hiện); fix DEFERRED theo user | Assertion line 97: retired cookie store không còn rỗng sau response muộn |
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
| N44-HIST | P1 stale history cross-identity | Source fix + unit XML PASS; pending E2E | Chưa được đóng |
| N44-NAV | P1 saved private stacks sau logout | Source fix, pending máy thật | Chưa được đóng |
| N44-GOOGLE-SESSION | P1 Google relogin không đồng bộ trạng thái phiên | DEFERRED theo user; runtime mới không tái hiện; chưa fix | Không tự đóng session gate |
| N44-COOKIE | P1 transport cookie races đã tái hiện | DEFERRED theo user; 2/2 reproducer FAIL, chưa fix | Rủi ro vẫn mở, chưa chứng minh nguyên nhân runtime Google guest |
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

### Kết quả rà source lần đầu (chưa tái hiện runtime)

- Luồng chuẩn: LoginViewModel chỉ phát NavigateToHostHome sau Result.Success của backend; LoginWithGoogleUseCase gọi session.onAuthenticated trước khi trả kết quả. Backend Google One Tap ở revision đã audit có set cùng access/refresh cookie options với local login, không phải endpoint chỉ trả User mà quên set cookie.
- DI bind SessionRepository và CookieStore trong SingletonComponent với @Singleton. Profile/Activity collect session qua Use case; auth gate Library đọc StateFlow hiện tại. Chưa thấy bằng chứng tạo session riêng cho mỗi tab hoặc UI nhớ cứng guest.
- Điểm nghi ngờ chính: callback logout gọi CurrentUserViewModel.refresh; AppNavGraph cũng refresh khi rời auth và ON_RESUME. Refresh thời guest vẫn gọi /users/me và TokenAuthenticator có thể gọi /auth/refresh khi 401 dù local không còn cookie.
- Guard revision của SessionRepositoryImpl bảo vệ publish StateFlow, KHÔNG bảo vệ side effect cookie của request cũ. TokenAuthenticator clear CookieStore trên refresh terminal error mà không kiểm tra lifetime request; PersistentCookieJar/RoomCookieStore áp Set-Cookie/expired-cookie theo host/name không có request-generation guard.
- Timeline khả dĩ (giả thuyết, chưa xác nhận lần lỗi user): refresh khi guest bắt đầu → Google login lưu cookie/publish LoggedIn → refresh cũ trả terminal và clear cookie mới → refresh sau thoát auth bị từ chối → session về Guest → các tab cùng hiện guest. Response Set-Cookie cũ cũng là đường có thể ghi đè/xóa cookie theo tên.
- Việc restart đôi khi khôi phục chưa được giải thích hoàn toàn bởi kịch bản “cookie đã bị xóa”: nếu cookie thực sự mất và không có response muộn, restart một mình không thể phục hồi. Phải có Logcat/timeline để phân biệt cookie bị phá với state bị invalidated khi cookie còn hợp lệ; không chốt root cause chỉ từ triệu chứng.
- N44CookieRaceReproductionTest đã chạy opt-in: XML 2/2 FAIL, 0 skipped/errors. Terminal refresh cũ xóa cookie login mới (expected fixture mới, actual null); Set-Cookie muộn phục hồi store sau logout. Reproducer dùng TokenAuthenticator/PersistentCookieJar thật với fake CookieStore/MockWebServer; không phải Room instrumentation hoặc Google E2E. Bước kế tiếp: duyệt fix transport và thu HTTP method/path/status quanh lần Google guest; không cung cấp token, cookie, Authorization header hoặc body credential.
- Điểm phụ cần trace nếu Google picker đóng nhưng không có POST one-tap: LoginScreen chỉ nhận instance GoogleIdTokenCredential; nếu có Invalid credential type thì chưa tới backend success. Không gộp lỗi credential picker vào race sau backend success.
- Các lượt ghi nhận/audit/đối chiếu diagnostic chỉ cập nhật tài liệu; chưa sửa production transport. Không thêm delay, polling, forced guest/login state hoặc auto-relogin workaround.

## Kế hoạch fix transport — lưu lại, tạm hoãn theo user

1. Thiết kế credential lifetime/epoch ở tầng data/network, retire khi chuyển phiên login/logout/deactivate. Stamp epoch trên request gốc, truyền rõ sang refresh/retry; không dựa riêng vào generation publish của SessionRepository vì cookie login được lưu trước onAuthenticated.
2. Cookie read/write/clear phải kiểm tra epoch và mutate atomically; response/terminal refresh của epoch cũ không được ghi/xóa credential mới. Không giữ lock suốt network I/O. CookieJar.saveFromResponse không nhận Request, nên cần request-aware cookie adapter/interceptor có context tường minh; không chỉ thêm check rời rạc trong jar hoặc suy đoán bằng thread-local.
3. Giữ ViewModel → Use case → Repository; coordination transport ở data/network, không sửa từng tab để che race. Refresh hợp lệ trong cùng lifetime vẫn hoạt động, transient failure không clear phiên; bảo vệ cả expired Set-Cookie và local logout/deactivate.
4. Giữ nguyên hai assertion đã đỏ; sau fix bỏ opt-in để thành regression thường. Bổ sung stale expired-cookie sau login, guest refresh đua login, refresh hiện hành thành công, transient failure, cùng account đăng nhập lại và retirement deactivate. User chạy unit/build rồi test máy thật logout → Google → ba tab; đối chiếu runtime trước đóng N44-GOOGLE-SESSION.

## Kết luận runtime và dọn instrumentation

- Đã đọc n44-session-log.txt, window 15:46:16.674–15:47:36.758 ngày 2026-10-08 theo timestamp log; một process. File do MCP đọc UTF-16LE thành chuỗi có NUL, đã chuẩn hóa chỉ để phân tích diagnostic metadata; không lấy nhãn beginning-of-crash làm bằng chứng app crash.
- 5/5 google_login_result success=true. Trong từng khoảng sau login tới logout kế tiếp/kết thúc capture: không có terminal_refresh clear và không có publish Guest. Các refresh sau login thành công; fetch cũ bị discard do revision_changed.
- Các terminal-refresh clear trong capture đều ở guest sau logout và hoàn tất trước Google login kế tiếp. Có overlap refresh/login, nhưng refresh trả success, không phá phiên trong các vòng này. Không thấy dấu hiệu response cũ phục hồi cookie sau logout trước login mới.
- Chỉ kết luận KHÔNG TÁI HIỆN trong capture này; không khẳng định bug tự hết, transport đã được sửa hay source không có race. Hai reproducer đã đỏ vẫn là bằng chứng riêng, không bị thay thế bởi E2E không gặp lỗi.
- Theo user: gác tạm điều tra/fix. Đã gỡ toàn bộ N44Session log, marker/request tag, network logger và 3 test riêng của logger; giữ SafeHttpLogger có sẵn, history isolation/navigation fix và 2 race test opt-in. Source app/core đã trở lại nội dung commit source 3ea792a theo git diff trước khi checkout docs; chưa chạy lại Gradle trong lượt merge.
- Quy ước nhánh: main chỉ source/config/test; toàn bộ tài liệu dự án ở docs. Handoff đã được merge vào docs bằng cập nhật targeted; không tạo Markdown trên main và không overwrite AGENTS canonical bằng snapshot cũ.

## Kế hoạch chặng 4 trước lượt manual — giữ tham chiếu, trạng thái mới ở báo cáo

Lượt manual đã được tổng hợp trong `N44_E2E_REPORT.md`; kế hoạch dưới đây không có nghĩa mọi bước đã thực hiện. Theo user, chuyển sang research UI N46, giữ thiếu evidence/blocker mở.

**Checklist người test điền:** `N44_E2E_INTEGRATION_CHECKLIST.md` trên docs. Kế thừa kịch bản Classic N25 nhưng mọi kết quả lượt mới để trống; tách UI test một máy, Classic nhiều client, case cần hỗ trợ và backend/deferred gates. Không copy dấu tick lịch sử từ N25.

1. Ghi source SHA/build/CI run id, thiết bị/OS, loại account và backend deployment revision. Đối chiếu full assemble/lint/regression, không suy ra CI từ unit XML chọn lọc.
2. Máy thật: account A → logout/guest → B, Back/tab restore/process recreation; history played/hosted/cursor → detail/own answers/visibility; ảnh câu hỏi → Solo Preview → replay/owner edit, không tạo room/history/play count.
3. Profile/avatar → password/deactivate/cleanup retry bằng account disposable. Không gọi mutation hoặc xóa account thật nếu chưa duyệt rõ; không tự replay request uncertain.
4. Host + Player user + guest chạy create/join/lobby/Classic/end/result/review/history. Self-paced/reconnect/pause/resume ghi rõ case Android chạy được vs backend-blocked; retest blocker chỉ khi có fix/deployment revision xác minh.
5. Cập nhật matrix PASS/FAIL/BLOCKED/NOT RUN + bằng chứng và bug ledger theo severity. N44-GOOGLE-SESSION/N44-COOKIE vẫn DEFERRED/chưa fix; tạm hoãn không miễn trừ release gate. N45 feature freeze/M6 chỉ sau đánh giá blocker/quyết định release rõ ràng.

Không thêm feature phase 2 hoặc workaround backend. Quyết định mới của user cho phép nghiên cứu UI N46 trước khi gate đóng; không coi research là release approval hoặc duyệt code toàn bộ polish. Luôn giữ ViewModel → Use case → Repository.

## Test nhanh ưu tiên

1. Account A mở Library (đổi keyword/sort), Activity và Profile; logout. Back không về màn/state riêng tư cũ.
2. Login B, mở lại các tab; không thấy dữ liệu/draft/filter cũ của A. Guest vẫn chỉ có played history.
3. Load history detail/answers trên mạng chậm rồi đổi phiên/đăng nhập lại; không treo loading hoặc lóe dữ liệu cũ.
4. Profile edit/save và avatar preview/confirm/verify chạy như N42; security change password giữ phiên; chỉ dùng account disposable để test deactivate.
5. Host + Player user + guest chạy Classic xuyên suốt. Các mode/reconnect có blocker phải ghi timeline, không tự kết luận PASS từ happy path.

**N44 chỉ được chốt khi evidence/blocker có quyết định rõ. User đã yêu cầu chuyển sang research N46; N45/M6 và release vẫn chưa chốt. Xem `N44_E2E_REPORT.md`.**
