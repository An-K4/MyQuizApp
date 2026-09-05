> 🎯 **Tóm tắt**: Backend myquizz (Express + TypeScript + Socket.IO + PostgreSQL + Redis) chất lượng khá tốt, đúng chuẩn server-authoritative. Design doc v2 khớp ~90% code thật, có 11 điểm lệch cần vá (mục 3) — 3 điểm mới phát hiện 24/8 khi đối chiếu lại trực tiếp qua GitHub API thay vì ghi chú cũ: REST error thật chỉ có `{code}` (không `message`/`details`), luồng Quên mật khẩu Android gọi 2 endpoint không tồn tại, và quiz listing đã hỗ trợ pagination thật (cursor). Kế hoạch 50 ngày / 10 tuần đã hiệu chỉnh theo trình độ thực tế. **Trạng thái: Tuần 3 (N11–N15) hoàn tất 24/8 — Home/Search + Quiz Detail (cache Room) + Quiz-manage danh sách/tạo quiz (Paging 3) + Upload ảnh presign S3 2 bước xong; `feature:auth` đã refactor UiState/Intent/Effect tách file riêng (22/8); bổ sung ngoài kế hoạch: Home auth header + màn Profile + component `Avatar` chung ở `core:ui` (22/8). Bài học lớn N15: kotlinx.serialization `JsonNamingStrategy` vẫn đổi tên field dù đã có `@SerialName` tường minh — đã tách `Json`/`Retrofit` riêng cho `StorageApiService`. N16 (25/8) hoàn tất — sửa/xóa quiz: màn editquiz pre-fill cả đáp án (PATCH replace-all questions, dirty guard), xóa hard delete + dialog xác nhận, nút owner-only ở QuizDetail, cache Room cập nhật/xóa theo; **M3 chốt**. Phát hiện thêm trong N16: điểm lệch #10 (DELETE là hard delete, doc ghi nhầm "xóa mềm") + bug reload-on-resume do flag `remember` bị reset khi Navigation dispose composition (fix: `rememberSaveable`). N16.5 (xong ngay 25/8) — 3 vá khẩn hoàn tất: luồng Quên mật khẩu viết lại theo 3 bước ticket thật (verify OTP ngay tại màn OTP, nút gửi lại đếm ngược 60s, màn Reset nhận ticket + peek), envelope lỗi đổi sang `{code}` + map ~60 code → tiếng Việt, search dùng cursor thật; kèm 2 hotfix khi test: SearchScreen không có trigger submit nào (bug sót từ N11–12) + click card chưa điều hướng. N17 hoàn tất 28/8 — CreateRoomScreen tạo phòng thật theo contract động của `/games/game-modes`, lấy host token riêng và hand-off sang HostLobby placeholder; config đã refactor typed, JSON/dotted path chỉ còn ở network. Refactor kiến trúc UI trước N18 cũng hoàn tất 28/8: audit đủ 14 Screen, chuẩn hóa Stateful `XxxScreen` + stateless `XxxScreenContent`, hoist platform/lifecycle/effect state và dùng chung `QuizEditorContent`. N18 hoàn tất 30/8 — socket layer thật, đã build và test trên máy thật: `core:common` có `GameEvent`/`LobbyState` + 3 interface socket tách theo vai trò (base/host/player), `core:network` có `GameSocketClient` (callbackFlow + `awaitClose`, handshake `auth.token`) và `GameEventMapper` (payload rác → `Failed(CLIENT_PARSE_ERROR)` chứ không throw làm chết flow), `feature:lobby` có HostLobby thật thay `HostLobbyPlaceholder` (đã xóa). Ba bài học N18: (1) socket.io tự reconnect nhưng KHÔNG tự join lại room, nên phải gọi `lobby:join` sau MỌI lần `Connected` — nếu không, socket vẫn "connected" mà im lặng không nhận `lobby:updated` nữa; (2) `io server disconnect` không được socket.io retry nên phải thoát màn, khác hoàn toàn với mất mạng (transport); (3) contract lỗi socket thật là `{event, code}` — doc cũ ghi sai thành `{event, message}` kèm prefix, và vì `AppError.Api(code)` đã map sẵn ~60 code nên KHÔNG thêm `AppError.Socket`. N19 hoàn tất 5/9 — Player lobby thật, đã build và test trên máy thật: tra phòng bằng mã (`GET /games/{code}`) → join REST (`POST /games/{code}/join`) lấy `socketToken` → vào PlayerLobby realtime dùng lại socket layer N18. Khách có `GuestIdentityStore` (UUID sinh lần đầu cần join, lưu DataStore) + màn nhập nickname riêng; người đã đăng nhập vào thẳng lobby không qua màn nhập tên (server tự lấy danh tính từ cookie, body join rỗng `{}`); quyền cho khách vào hay không do host cấu hình (`config.lobby.allowGuests`), client chỉ tôn trọng chứ không tự quyết. Đã trả luôn nợ N18: lý do bị buộc rời phòng giờ hiện thật qua `savedStateHandle` + snackbar. **Bài học lớn N19 — điểm lệch #11**: `GET /games/{code}` trả `data.session.session` (lồng ba cấp) vì `getGameByCode` đặt tên biến `session` cho cả cụm `{session, players, config}` mà `getLobby` trả — app crash `MissingFieldException` khi test thật; dấu hiệu nhận biết là danh sách field thiếu tại `$.data.session` nhưng `config` KHÔNG nằm trong danh sách đó (vì cấp ngoài có sẵn key `config`). Rút ra: đọc `*.service.ts` để biết shape thật, đừng tin tên key trong `success(res, { ... })` của controller. Bước tiếp theo: N19.5 — Bottom Navigation thật (chờ ảnh thiết kế), rồi N20 — HostLobby + `lobby:config-update` + QR.**

File này tự chứa đủ ngữ cảnh để bắt đầu phiên làm việc mới (đã gộp & tinh gọn log các phiên 9–11/8).

*Nguồn review: repo [github.com/Ntd1411/myquizz](https://github.com/Ntd1411/myquizz) (backend, nhánh main) + repo Android [github.com/An-K4/MyQuizApp](https://github.com/An-K4/MyQuizApp). Trình độ dev đánh giá qua 2 dự án trước: My Schedule v3.2 (Kotlin, MVVM, Room, custom Canvas, AlarmManager) và Clover Chatty (Kotlin + Compose, Clean Architecture + MVVM, Hilt, Socket.IO, Retrofit, Room, DataStore, FCM).*

---

## 1. Bức tranh tổng thể

| Thành phần | Công nghệ | Trạng thái |
|---|---|---|
| `backend/` | Express 5 + TS (ESM), Socket.IO 4.8, PostgreSQL 16 (pg thuần), Redis 7 (ioredis), S3 presign, JWT qua cookie HttpOnly | Hoàn thiện cao: auth đầy đủ, game engine 5 mode, rate limit, Swagger `/v1/api-docs`, migration + seed. Đã deploy: `api.myquizz.dpdns.org` |
| `frontend/` | Vue 3 + Vite + Pinia + vue-query + axios + Tailwind + GSAP | Cổng quản lý web (auth, discover, quiz CRUD, import xlsx, join). **Không có UI gameplay realtime → gameplay thuộc app Android** |
| DevOps | Dockerfile, docker-compose (PG + Redis + BE + FE), PM2, GitHub Actions | Chạy được; CI backend chưa có bước test (vì chưa có test) |

## 2. Review backend

### 2.1. Điểm mạnh — giữ nguyên

- **Server-authoritative tuyệt đối**: chấm điểm/thời gian/đáp án đúng đều ở server; `correct_answer` bị cắt khỏi mọi payload gửi player.
- **Anti-cheat chu đáo**: shuffle có seed (reconnect giữ nguyên thứ tự câu); reconnect không reset đồng hồ; câu hết hạn lúc offline bị đóng luôn; chống double-submit (`allowChange=false`); identity chỉ lấy từ socket token.
- **Socket token flow**: JWT ngắn hạn ký bằng `SOCKET_JWT_SECRET` riêng, payload `{psid, gsid, code, role}`; kick player = token cũ tự vô hiệu.
- **Config engine chắc** (`engine/config.rule.ts`): editable/locked theo mode; `sanitizeConfigPatch` không bao giờ throw (drop + report `ignored`).
- **Vòng đời game đầy đủ** (`game.socket.ts`, ~1460 dòng): countdown, pause/resume dời clock từng player, marathon timer phía server, flush Redis → Postgres cuối câu/cuối game.
- **Hạ tầng tốt**: cache-aside Redis, rate limiter có rollback + fail-open, refresh token rotation + blacklist + device tracking, OAuth state cookie chống CSRF, quiz snapshot khi tạo phòng.

### 2.2. Vấn đề cần xử lý (phía backend)

| Mức | Vấn đề | Đề xuất |
|---|---|---|
| 🔴 | `session_code` không UNIQUE; tạo code bằng check-then-insert → race trùng mã phòng | Partial unique index `WHERE deleted_at IS NULL AND session_status IN ('lobby','active','paused')` • `INSERT ... ON CONFLICT` retry |
| 🔴 | Không có bất kỳ test nào; engine timer/scoring phức tạp | Unit test trước cho `scoring.ts`, `config.rule.ts`, `grade`/`normalize` (hàm thuần, dễ test) |
| 🔴 | `GameSocket` god class ~1460 dòng; timers/questions là Map in-memory theo process | Tách PhaseManager/ClockService; khi cần scale: Socket.IO Redis adapter + externalize timers |
| 🟡 | Redis chết → self-paced mất clock → `timeTaken ≈ 0` → ăn speed bonus tối đa | Fail-closed cho `question:answer`, hoặc chấp nhận degraded + cảnh báo vận hành |
| 🟡 | `POST /v1/games` trả `data.data.session` (lồng kép); nặng hơn: `GET /v1/games/:code` trả `data.session.session` vì `getGameByCode` viết `success(res, { session })` trong khi `session` thực chất là cả cụm `{session, players, config}` của `getLobby` — đây là **bug đặt tên biến**, không phải quy ước | Client map DTO đúng (Android đã có `LobbySnapshotDto` tại N19); đề xuất backend sửa `getGameByCode` thành `success(res, lobby)` — nhưng phải đổi đồng thời cả frontend web (`unwrap(res.data).session`) và app, không deploy một mình |
| 🟡 | `/v1/api-docs` public mọi môi trường | Gate theo `NODE_ENV !== 'production'` |
| 🟡 | Song song joi + zod; enum `game_mode` chứa `'team'` chưa implement | Chuẩn hóa zod; xóa `'team'` hoặc bổ sung engine |
| 🟡 | Cookie auth chỉ dựa `SameSite=Lax`, không CSRF token | Giữ lax; cân nhắc CSRF token khi mở rộng surface |
| 🟢 | Ternary thừa trong cookie config; root README rỗng; compose publish port 5432/6379; tạo phòng không chặn quiz 0 câu | Dọn dẹp; validate `total_questions > 0` |

### 2.3. Frontend web (nhanh)

Vue 3 thuần JS + Pinia + vue-query + Tailwind/GSAP; có import quiz từ xlsx (~17KB, đáng port sang Android phase 2) và `mock.api.js` (phát triển UI không cần backend — pattern nên học theo). Web = quản lý + join; **gameplay nằm ở Android** (khớp design doc).

## 3. Đối chiếu Design Doc v2 ↔ code thật

**Khớp ~90%**: cookie HttpOnly auth (không Bearer); REST trước → `socketToken` → Socket.IO handshake `auth.token`; 5 mode + pacing host/self; bảng event 5.2–5.3; `AnswerAck`; 4 `question_type`; envelope `{success, data, error}` (đúng outer shape, xem #7 cho inner error); upload ảnh presign S3 2 bước; CookieStore DI qua `core:common`.

**11 điểm lệch — vá doc trước khi code phần liên quan (#7–#9 phát hiện 24/8 khi đối chiếu trực tiếp qua GitHub API, #10 phát hiện ở N16, #11 phát hiện ở N19 khi app crash lúc test thật):**

| # | Doc đang ghi | Code thật | Hành động Android |
|---|---|---|---|
| 1 | Route không prefix | Mọi REST dưới `/v1`; game router ở `/v1/games` | `BASE_URL = https://api.myquizz.dpdns.org/v1/` |
| 2 | Socket base URL mặc định | Server dùng namespace `/game` | `IO.socket(SOCKET_URL + "/game", options)` |
| 3 | `question:started` = `{question, time_limit, endsAt, serverTime}` | Self-paced thêm `matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `lives` | Bổ sung model `SelfQuestionStarted` |
| 4 | Không có REST update config | Có `PATCH /v1/games/:id/config` (host, lobby-only) | App ưu tiên kênh socket `lobby:config-update` (có ACK + broadcast) |
| 5 | `CreateGameSession` trả phẳng | Thực tế lồng `data.data.session` (kèm `ignored`) | DTO map đúng |
| 6 | `game:state` mô tả chung chung | Snapshot gồm `question`, `countdown`, `endsAt/matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `player` (gated reveal), `leaderboard` (rỗng khi câu đang mở) | Nguồn dữ liệu chính của Host screen + resync |
| 7 | REST error = `{message, details}` (đã cài đặt sai theo giả định này ở N1–5, `core:network`) | `fail()` trong `response.ts` chỉ trả `{code}` — cố ý không có message/details (tránh leak nội bộ + đa ngôn ngữ) | Sửa `ApiError` còn 1 field `code: String`; `AppError.Http` đổi field `message` → `code`; thêm hàm map `code` → `UiText` tiếng Việt — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 8 | Quên mật khẩu: 3 endpoint giả định `/users/forgot-password` + `/users/reset-password-token` + `/users/reset-password` (đã code xong ở N15, 15/8) | Flow ticket 3 bước thật: `POST /users/forgot-password` (đúng tên, response khác) → `POST /users/password-reset/verify` (`{email,otp}` hoặc `{token}` → `{ticket,expiresAt,email}`) → `GET /users/password-reset/ticket` (peek) → `POST /users/password-reset/complete` (`{ticket,newPassword}`) | Viết lại DTO/API service/ViewModel reset theo 4 endpoint thật — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 9 | Ghi chú kỹ thuật nợ ở N11: "backend chưa có pagination" | `/quizzes/search`, `/quizzes/me`, `/quizzes/users/id/:ownerId` đã hỗ trợ `cursor`/`limit` (1–24)/`include_total` | Refactor Paging 3 dùng cursor thật thay vì `page` không dùng — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 10 | DELETE /quizzes ghi "xóa mềm" | **Hard delete**: `DELETE FROM` + cascade mất questions, quiz_snapshots, game_sessions, player_sessions (quiz.repository.ts) | Dialog xác nhận cảnh báo mất lịch sử chơi + xóa Room cache sau khi xóa — đã làm ở N16 |
| 11 | Tra phòng `GET /games/{code}` trả `data.session` = một `GameSession` phẳng | Lồng ba cấp `data.session.session`: `getLobby` trả `{session, players, config}` nhưng `getGameByCode` gán cả cụm vào biến tên `session` rồi `success(res, { session })` — bug backend (xem mục 2.2), không phải quy ước bao resource như #5 | `RoomLookupResponseDto(session: LobbySnapshotDto)` + `LobbySnapshotDto(session, players)`; `totalPlayers` đếm từ `players` vì `total_players` trên row KHÔNG tăng khi player join (chỉ flush cuối ván) — đã làm ở N19 (5/9), có test hồi quy khóa shape |

Lưu ý thêm: swagger ghi `/auth/refresh` trả tokens nhưng thực tế chỉ trả `{message}`.

## 4. Hồ sơ kỹ năng

**Đã có sẵn** (từ My Schedule + Clover Chatty): Kotlin + Coroutines/Flow • Compose M3 • Clean Architecture 3 tầng + UseCase • Hilt • Socket.IO client • Retrofit/OkHttp • Room + DataStore • MVVM • Custom Canvas, AlarmManager, Notification • FCM • offline-first / safeApiCall / UiText.

**Cần học** (đã chèn vào tuần tương ứng):

| # | Chủ đề | Tuần |
|---|---|---|
| 1 | Gradle multi-module (version catalog, convention plugins — spike nowinandroid) | 1 ✅ |
| 2 | Cookie auth: OkHttp CookieJar + Authenticator (gỡ thói quen Bearer) | 1–2 ✅ |
| 3 | Credential Manager + Google One Tap | 2 ✅ |
| 4 | kotlinx.serialization (bỏ Gson) | 3 ✅ |
| 5 | Paging 3 (có thể thay phân trang tay) + S3 presign 2 bước | 3 ✅ |
| 6 | Socket.IO nâng cao: namespace `/game`, ACK, auth handshake; bắt đầu MVI | 4–5 |
| 7 | Testing: JUnit + MockK + Turbine + MockWebServer + Compose UI Test — **khoảng trống lớn nhất, bắt đầu từ Tuần 5** | 5–9 |
| 8 | Release hardening: R8/ProGuard, network security config, LeakCanary | 8–9 |

## 5. Lộ trình 50 ngày

**Giả định**: 1 dev full-time, 5 ngày/tuần; backend dùng nguyên trạng, vá nhỏ chạy song song không block app; chuẩn bị từ Tuần 1: Google OAuth client, S3 bucket + quyền presign, backend chạy local bằng docker-compose.

**Milestones**

| Mốc | Ngày | Tiêu chí chốt |
|---|---|---|
| M1 — Foundation | N5 | ✅ **CHỐT 9/8** — 13 module build xanh, CI lint + test + assemble |
| M2 — Auth E2E | N10 | ✅ **CHỐT 13/8** — Login/Register/Google One Tap với backend thật; 401 → refresh → retry tự động + UI polish (theme, Stateful/Stateless pattern, AppTextStyles) |
| M3 — Quiz CRUD | N20 | Tạo/sửa quiz + upload ảnh S3 + tạo phòng config động |
| M4 — Classic playable | N25 | Chơi trọn ván classic: host + ≥2 app Android thật |
| M5 — Đủ 5 mode + Host console | N35 | 4 mode self-paced + màn điều khiển host realtime |
| M6 — Release candidate | N45 | Test pass, release build R8, LeakCanary sạch |
| M7 — Ship | N50 | Internal release Play Console + tài liệu bàn giao |

## 6. Trạng thái hiện tại (chốt 11/8)

### 6.1. ✅ Tuần 1 (N1–N5) đã xong — M1 chốt 9/8

- **Repo + build**: 13 module (`:app` + 5 core + 7 feature) • version catalog + wrapper • build-logic (3 plugin `myquizzapp.android.*`, build file module ~65 → ~17 dòng) • CI GitHub Actions (lint + `testDebugUnitTest` + `assembleDebug`) xanh, gồm cả lần phá build thử → đỏ → revert → xanh.
- **`core:common`**: Result + AppError + 7 domain model (User, Quiz, Question, GameConfig, GameSession, Player, QuizSummary) đọc từ backend thật. `Question` domain **cố ý không có `correct_answer`** (anti-cheat).
- **`core:database`**: Room (`CookieEntity` key `host|name`, `CachedQuizEntity`, `GameHistoryEntity` + DAO + Hilt module). **`core:datastore`**: SettingsDataStore (theme/onboarding — **không lưu token**).
- **`core:network`**: Retrofit + `ApiEnvelope` khớp `response.ts` thật (`AppError.Api` = `message` + `details`, **không có `code`**) • `ResultCallAdapter` unwrap → `Result` • `JsonNamingStrategy.SnakeCase` 1 lần trong NetworkModule • build variants `BASE_URL`/`SOCKET_URL` đặt **trong `core:network`** (BuildConfig sinh theo module).
- **Cookie persistence + refresh**: 2 test MockWebServer xanh local + CI (401 → refresh → retry; refresh chết → clear cookie).

### 6.2. 🔑 Quyết định đã chốt (đừng đổi trừ khi có lý do)

- **Naming đồng bộ `myquizzapp`**: applicationId/namespace `android.kma.myquizzapp` ✅ **Đã đổi 14/8** (Play Console khóa applicationId vĩnh viễn sau publish đầu); build-logic package `com.myquizzapp.buildlogic`; plugin ID `myquizzapp.android.library` / `.compose` / `.hilt`; catalog alias `myquizzapp-android-*`.
- **URLs**: release `BASE_URL = https://api.myquizz.dpdns.org/v1/` (backend đã deploy domain thật; **Retrofit bắt buộc `/` cuối**), `SOCKET_URL = https://api.myquizz.dpdns.org` (không path — lib tự nối `/socket.io/`). Chạy backend local: `http://10.0.2.2:3000/v1/` + `http://10.0.2.2:3000`.
- **DIP cho cookie** (đúng design doc 3.2 + 12.1): `core:network` **không** phụ thuộc `core:database`. Interface `CookieStore` + `StoredCookie` ở `core:common/cookie`; `RoomCookieStore` + `DatabaseBindingModule` (`@Binds`) ở `core:database`; `PersistentCookieJar` chỉ biết interface; `TokenAuthenticator` dùng `Lazy<AuthApiService>` (là **`dagger.Lazy`**, không phải `kotlin.Lazy`) phá vòng lặp DI.
- **Sai khác có chủ đích so với doc**: (1) thêm `CookieStore.clear()` cho logout; (2) `runBlocking` trong `saveFromResponse` (code mẫu doc không compile); (3) authenticator kèm `CookieStore` để clear khi refresh chết; (4) guard path `/auth/` tránh refresh vô ích khi sai password.
- **Dọn dẹp đã làm**: theme XML đổi parent platform theme, `MainActivity` chuyển Compose, xóa `res/layout` + `libs.material`; catalog đã thêm Compose BOM + `activity-compose` + `ui`/`material3`.

### 6.3. 🧠 Bài học N5 (giữ cho retrospective + tránh tái phạm)

- **Room là nguồn sự thật duy nhất cho cookie — không cache RAM song song** (cache lai suýt thành bug prod: Splash gọi `/users/me` trước khi cache warm → user bị hất ra Login).
- Unit test **không hardcode `localhost`** — máy có Docker Desktop resolve host lạ (`kubernetes.docker.internal`); luôn dùng `server.hostName`.
- File test phải nằm `src/test` (đặt nhầm `src/main` → `testImplementation` không có trên classpath).

### 6.4. ✅ Tuần 2 (N6–N10) — Auth & Navigation — **HOÀN THÀNH M2 (13/8)**

- [x] **N6**: Navigation type-safe 3 graph (Auth/Player/Host — xem design doc mục 11) • Splash gọi `GET /v1/users/me` xác định trạng thái login (401 → Login/Guest) — đây là test thật đầu tiên cho cookie jar + authenticator. ✅ **Hoàn thành 12/8**
- [x] **N7–8**: `feature:auth` — Login + Register (UI + ViewModel + UseCase), validate form theo schema backend. ⚠️ `register` **không set cookie** (chỉ trả `{user}`) → sau register auto-login hoặc về Login; response login/register/one-tap đều là `data = { user }`. ✅ **Hoàn thành 12/8**
- [x] **N9**: Google One Tap (Credential Manager) → `POST /v1/auth/google/one-tap` • logout • xử lý 403 deactivated. ✅ **Hoàn thành 12/8** (⚠️ Cần config Web Client ID từ Firebase Console trước khi test)
- [x] **N10**: Integration test auth E2E với backend local (docker-compose: debug `BASE_URL=http://10.0.2.2:3000/v1/`, `SOCKET_URL=http://10.0.2.2:3000`), fix mismatch → **Chốt M2**. ✅ **Hoàn thành 12/8**

**🎨 UI Polish & Architecture Patterns (13/8) — Công việc bổ sung ngoài kế hoạch:**

- [x] **Theme System**: Tạo `core:ui/theme/` với Color.kt (Primary #7B61FF từ logo), Theme.kt (Material3 light/dark), Type.kt (typography definitions). ✅
- [x] **AppTextStyles System**: Tạo `core:ui/style/AppTextStyles.kt` — centralized typography cho toàn dự án với bold buttons/links làm điểm nhấn (titleLarge, bodyMedium, buttonText, linkText, caption). ✅
- [x] **Stateful/Stateless Pattern**: Refactor LoginScreen và RegisterScreen theo pattern: wrapper stateful quản lý ViewModel/effects + composable stateless thuần UI (Preview-friendly, không warning ViewModel). ✅
- [x] **TextField Standardization**: Pattern nhất quán cho tất cả TextField — embedded labels + `OutlinedTextFieldDefaults.colors()` config (focusedTextColor, unfocusedTextColor, focusedBorderColor, cursorColor). ✅
- [x] **Material Icons Extended**: Thêm vào convention plugin `myquizzapp.android.compose` → tất cả module Compose tự động có Icons Extended. ✅
- [x] **Dynamic Image Sizing**: Pattern `with(density) { textStyle.fontSize.toDp() }` để icon scale theo text size. ✅

**🔧 Infrastructure & Bug Fixes (14/8):**

- [x] **Timber Setup**: Added Timber 5.0.1 to `AndroidLibraryConventionPlugin` → all 13 modules automatically get Timber for consistent logging (DRY principle). ✅
- [x] **Google One Tap Fix**: SHA-1 fingerprint (`43:9E:38:4F:D6:7C:F6:A7:EE:04:A0:DF:37:9B:1B:5A:E5:62:64:1D`) + SHA-256 (`79:5B:96:EA:F8:95:68:E7:A0:D1:E6:7C:D0:2A:73:F5:33:6D:E3:11:82:14:77:F6:DE:D8:79:FD:71:33:19:03`) added to Google Cloud Console Android OAuth Client ID by backend team → Google One Tap working. ✅

**🎯 Architecture Enhancements (15/8):**

- [x] **Guest Mode Implementation**: Clean Architecture với AuthState enum (Authenticated/Guest/Unauthenticated), CheckAuthStateUseCase, EnableGuestModeUseCase, SettingsDataStore.isGuestMode flag → Guest users can browse/join without auth, persistent across app restarts (mirrors backend's optionalAuthMiddleware pattern). ✅
- [x] **Navigation Refactor**: Đổi từ 3 graphs riêng (AuthGraph/PlayerGraph/HostGraph) sang unified MainGraph pattern với bottom nav (Home/Discover/Join/Library/Profile) → browse-first UX, soft auth gates, khớp với web frontend và backend optionalAuthMiddleware. Design doc Section 11 đã cập nhật đầy đủ. ✅
- [x] **Forgot Password Feature (Week 2 Final Task)**: ✅ **Hoàn thành 15/8**
  - Data Layer: 3 DTOs (ForgotPasswordRequest, ResetPasswordRequest, ResetPasswordWithOtpRequest) + 3 API endpoints (POST `/users/forgot-password`, `/users/reset-password-token`, `/users/reset-password`) + Repository implementation
  - Domain Layer: 3 UseCases (ForgotPasswordUseCase, ResetPasswordUseCase, ResetPasswordWithOtpUseCase)
  - Presentation Layer: 2 ViewModels (ForgotPasswordViewModel 92 lines, ResetPasswordViewModel 188 lines dual-flow support) + 2 Screens (ForgotPasswordScreen 118 lines, ResetPasswordScreen 172 lines)
  - Navigation: Routes added, composables wired in AuthGraph, LoginScreen "Quên mật khẩu?" link connected
  - Deep Linking: Android App Links implemented với intent-filter trong AndroidManifest.xml (android:autoVerify="true" cho https://myquizz.dpdns.org/reset-password), MainActivity handles onCreate + onNewIntent, AppNavGraph navigates với token từ email link. Backend team added assetlinks.json với SHA-256.
  - Flows: Token flow (primary - click email link) + OTP flow (fallback - manual 6-digit entry)
- [x] **OTP Verification Enhancement (15/8)**: ✅ **Hoàn thành cùng ngày**
  - **Phase 1 - Snackbar Repositioning**: Di chuyển tất cả snackbar lên top (dưới status bar) cho 4 màn auth (LoginScreen, RegisterScreen, ForgotPasswordScreen, ResetPasswordScreen) → không bị che bởi bàn phím
  - **Phase 2 - OTP Verification Screen**: Tạo màn OTP verification trung gian với 6 ô nhập riêng biệt
    - Files mới: `OtpVerificationViewModel.kt` (127 lines) + `OtpVerificationScreen.kt` (220 lines)
    - 6-box OTP input với auto-focus, backspace handling, paste support
    - Nút "Xác nhận" + link "Gửi lại mã"
    - Navigation flow: ForgotPassword → OtpVerification(email) → ResetPassword(email, otp)
  - Files updated: Routes.kt, AppNavGraph.kt (+ import), ForgotPasswordViewModel/Screen, ResetPasswordViewModel (extract email + OTP from navigation)
  - Preview functions: Fix RegisterScreen (2 previews thiếu tham số) + thêm ResetPasswordScreen preview
  - Compilation: ✅ PASS - không có errors

**📊 Week 2 Summary (N6-N10):**
- Milestone M2 đạt 13/8: Auth E2E working (Login/Register/Google One Tap + Cookie auth + Token refresh)
- Bonus achievements: UI polish (theme/typography/patterns), infrastructure (Timber), bug fixes (SHA-1), architectural improvements (guest mode + unified navigation), forgot password feature complete với deep linking + OTP verification screen
- **Status: Week 2 HOÀN THÀNH 15/8. Ready for Week 3 (N11-N20) — Quiz CRUD.**

**✅ Navigation Architecture — Quyết định chốt: Option B — Browse-First Flow (20/8):**

⚠️ **Bối cảnh**: Trong Tuần 2, để test flow sau login, đã tạo **host placeholder screen** làm điểm đích tạm thời sau khi authentication thành công — workaround cho việc màn Home chưa được implement. Sau khi N11 (Home) hoàn thành 17/8, đã đánh giá UX thực tế và **chốt Option B** (Browse-First), bỏ Option A (Auth-First).

**Quyết định & refactor đã thực hiện (20/8):**
- Splash → điều hướng thẳng **Home** (`MainGraph`) trong mọi trường hợp — không còn rẽ nhánh Auth/Guest/Host tại Splash.
- `AuthState` rút gọn từ mô hình 3 trạng thái (kiểu "first launch" riêng) xuống **2 giá trị**: `GUEST`, `AUTHENTICATED`. `CheckAuthStateUseCase` chỉ hỏi backend có đang authenticated không; nếu không → gọi `SettingsDataStore.setGuestMode(true)` và trả `GUEST` (không còn khái niệm "lần đầu mở app" riêng biệt).
- `SplashViewModel`/`SplashScreen` gộp `UiState` về `Loading`/`Ready`, chỉ còn 1 callback `onNavigateToHome`.
- `AppNavGraph.kt`: Splash composable chỉ gọi `onNavigateToHome` → `navigate(Route.MainGraph) { popUpTo<Route.Splash> { inclusive = true } }`.
- Soft auth gates (`RequireAuth`) tại từng route bảo vệ (Library, Profile, CreateQuiz, EditQuiz, CreateRoom...) giữ nguyên như thiết kế — chỉ luồng Splash thay đổi.
- Refresh token / verify-session lúc khởi động: **chưa implement** — để lại `TODO` trong `SplashViewModel` (dự kiến làm ở N12+ khi cần giữ trạng thái đăng nhập bền hơn qua `GET /users/me` hoặc refresh cookie).

**Action items**:
- [x] Xóa host placeholder screen (đã xóa từ N11, thay bằng HomeScreen thật)
- [x] Cập nhật navigation graph theo Option B (20/8)
- [x] Đảm bảo guest mode integration mượt mà với Option B — `CheckAuthStateUseCase` tự set guest mode khi chưa authenticated
- [x] Cập nhật design doc mục 11 (Navigation Architecture) cho khớp luồng Splash mới
- [ ] (N12+) Thêm refresh-token/verify-session use case thật cho Splash khi cần

---

**📚 Học trong tuần**:

- CookieJar trong OkHttp (`saveFromResponse`/`loadForRequest`)
- **Authenticator ≠ Interceptor** cho 401 → refresh → retry (tránh retry vô hạn — đã có guard `responseCount >= 2`)
- HttpOnly cookie trên native khác web thế nào
- Credential Manager lấy Google ID token
- **Stateful/Stateless Composable Pattern**: Cách tách state management khỏi pure UI để hỗ trợ Preview mà không gặp warning ViewModel — pattern chuẩn Android
- **Convention Plugins**: Hiểu sâu hơn cách dùng convention plugin để DRY dependencies (Material Icons Extended example)

**⚠️ Gỡ thói quen cũ**: Clover Chatty dùng JWT + `AuthInterceptor` gắn Bearer — backend này **không đọc** Authorization header, mọi auth đi qua cookie.

**⚠️ Phụ thuộc đã sẵn sàng**: Compose BOM + `activity-compose` + `ui`/`material3` đã có trong catalog (9/8) — N7–8 không bị vỡ tại `setContent {}`.

**🎯 Kết quả đạt được**:

- ✅ M2 milestone hoàn thành đầy đủ theo kế hoạch
- ✅ Bonus: UI/UX chất lượng cao với theme system + typography consistency
- ✅ Bonus: Architecture pattern Stateful/Stateless học được và áp dụng thành công
- ✅ Code quality: Preview-friendly, maintainable, scalable patterns
- ✅ Sẵn sàng cho Tuần 3: Home & Quiz module

### Tuần 3 (N11–15) — Home & Quiz đọc

- [x] **N11**: `feature:home` — search quiz công khai (paging), tab Khám phá/Của tôi. ✅ **Hoàn thành 17/8**
- [x] **N12**: Quiz detail + cache Room. ✅ **Hoàn thành 21/8**
- [x] **N13–14**: `feature:quiz-manage` — danh sách + tạo quiz; editor 4 loại câu hỏi (`multiple_choice`, `multiple_select`, `short_answer`, `long_answer`). ✅ **Hoàn thành 22/8**
- [x] **N15**: Upload ảnh presign S3 2 bước (`UploadImageUseCase`, PUT trực tiếp, không cookie) + Coil. ✅ **Hoàn thành 24/8**
- 📚 kotlinx.serialization (`@Serializable`, `JsonElement` cho `correct_answer` đa kiểu), Paging 3, S3 presign.

**📝 N11 Implementation Details (17/8):**

**Phase 1-4 Complete (4 Phases):**
- ✅ **Phase 1 - Domain Models** (4 files): QuizOwner.kt, QuizCard.kt (lightweight listing model - NO questions array), Quiz.kt (updated with owner/deletedAt), HomeSection.kt
- ✅ **Phase 2 - Data Layer** (6 files): QuizRepository interface, QuizApiService, HomeContentDto + mappers (snake_case → domain), QuizRepositoryImpl, DI NetworkBindingModule
- ✅ **Phase 3 - Feature Module** (8 files): Home/Search architecture split - HomeViewModel (sections only), SearchViewModel (separate screen with pagination logic), 2 UseCases (GetHomeContentUseCase, SearchQuizzesUseCase), Intent/UiState cho cả 2 screens
- ✅ **Phase 4 - UI Layer + Navigation** (6 files): HomeScreen.kt (TopBar + tabs + sections scroll), SearchScreen.kt (dedicated screen with auto-focus + infinite scroll), QuizCardItem.kt, HomeSectionRow.kt, Routes.kt (added Route.Search), AppNavGraph.kt (wiring)

**Bug Fixes & Refactoring (17/8):**
- ✅ **Result Pattern Fix**: Initial mistake dùng `Result<T, AppError>` (2 type params) → Fixed sang `Result<T>` (1 param) khớp project pattern (6 files: QuizRepository, QuizRepositoryImpl, 2 UseCases, QuizApiService, HomeViewModel import)
- ✅ **API Pattern Fix**: QuizApiService methods trả về DTO trực tiếp → Fixed sang `Result<T>` như AuthApiService; QuizRepositoryImpl dùng `.map { }` thay vì try-catch (theo AuthRepository pattern)
- ✅ **HomeViewModel Corruption Fix**: File bị corrupt với duplicate code → Viết lại clean version
- ✅ **Shared Components Refactoring**: Move QuizCardItem.kt + HomeSectionRow.kt từ feature:home → core:ui/components/ (added Coil import, benefits: reusable cho quiz-manage/leaderboard, Coil đã có sẵn trong core:ui)

**Key Architectural Decisions:**
- ✅ Search architecture: Chọn **separate SearchScreen** (Option B) thay vì same-screen search → cleaner separation of concerns, better UX với auto-focus
- ✅ Backend exploration: Hiểu 2 distinct payload types - **QuizCard** (lightweight, no questions, cho listing) vs **Quiz** (full detail với questions array)
- ⚠️ Pagination: Backend comment ghi rõ "Version 1: Simple list, không có pagination" → UseCase có params page/limit nhưng backend chưa support (để lại cho Paging 3 refactor sau)
- ✅ Host placeholder: Đã remove khỏi AppNavGraph, replaced với HomeScreen thật

**Files Affected Summary:**
- **New files**: 18 files (4 domain models, 3 API/DTO, 2 repositories, 2 UseCases, 4 ViewModels/Intent/UiState, 2 UI screens, 2 shared components moved to core:ui)
- **Modified files**: Quiz.kt (domain), Routes.kt, AppNavGraph.kt, HomeScreen.kt (refactored), SearchScreen.kt (imports updated)
- **Compilation**: ✅ All green, no errors

**📚 Học trong N11:**
- Backend API exploration: Đọc source code `/server/backend` để hiểu response structure (HomeContentDto với sections array, QuizCardDto vs Quiz distinction)
- Result pattern trong project: `Result<T>` với AppError cố định trong Error case, KHÔNG phải generic `Result<T, E>`
- API service pattern: Tất cả Retrofit methods phải trả về `Result<T>` (CallAdapter tự động wrap), repository dùng `.map { }` thay vì try-catch
- Clean Architecture: Shared UI components thuộc core:ui, không nằm trong feature modules
- Search UX patterns: Separate screen vs same-screen search tradeoffs

**⚠️ Technical Debt & Future Work:**
- [ ] Old components trong `feature:home/presentation/components/` có thể xóa (đã move sang core:ui)
- [ ] SearchQuizzesUseCase có params `page/limit` không dùng (backend chưa có pagination) → refactor khi implement Paging 3
- [ ] Backend endpoint `/v1/quizzes/search` chưa có pagination → N13 sẽ học Paging 3 để refactor
- [ ] Auth button UI trong HomeScreen TopBar (TODO comment line 40-42) → discuss implementation sau khi test flow

**🎯 N11 Status: COMPLETE ✅** (17/8 evening) - Tested và chạy ổn, ready cho N12 (Quiz detail + Room cache)

**📝 N12 Implementation Details (21/8):**

- ✅ **Quiz Detail feature**: `QuizDetailScreen` + `QuizDetailViewModel`/`UiState`/`Intent` đặt hẳn ở `feature:quiz-manage/presentation/quizdetail` (không giữ ở `feature:home`) — `GetQuizDetailUseCase` cache-aside: gọi API trước, cache Room khi thành công, fallback đọc cache khi lỗi mạng.
- ✅ **Sửa vi phạm Clean Architecture (DIP) phát hiện giữa chừng**: bản đầu `core:network` import trực tiếp `core:database` để cache quiz detail — vi phạm bảng dependency (project_structure.md mục 2.3). Đã sửa theo đúng pattern `CookieStore`: thêm interface `QuizCacheStore` ở `core:common`, impl `RoomQuizCacheStore` ở `core:database` (`@Binds` trong `DatabaseBindingModule`), gỡ dependency Gradle `core:network → core:database`.
- ✅ **Bug tìm thấy qua đối chiếu backend thật**: `GET /v1/quizzes/:id` trả envelope lồng `{ quiz: {...} }` (giống case `CreateGameSession` đã ghi ở mục 3 #5) nhưng Android map thẳng — gây `MissingFieldException` runtime. Fix: thêm `QuizDetailDto(val quiz: QuizDto)` ở `core:network/dto`, unwrap ở `QuizRepositoryImpl.getQuizDetail`.
- ✅ **Audit edge-to-edge/insets**: rà lại toàn bộ 9 Composable Screen + `MainActivity` — phát hiện `QuizDetailScreen` bottomBar Button thiếu `.navigationBarsPadding()` (bị 3 nút điều hướng che) → đã fix; 8 screen còn lại đã đúng.
- ⚠️ **Kỹ thuật nợ (chưa làm, note lại)**: `searchQuizzes`/`getMyQuizzes` bị lỗi wrapper tương tự (`data: { quizzes: [...] }` chưa unwrap đúng ở `QuizApiService`/`QuizRepositoryImpl`) — cần `QuizListDto(val quizzes: List<QuizCardDto>)` giống cách đã fix quiz detail. **Chưa impl màn dùng đến (list quiz của tôi) nên để làm sau, không block N12.**

**🧠 Bài học N12 (đối chiếu backend + kiến trúc):**

- Backend hay bọc response trong 1 field lồng theo tên resource (`{ quiz: ... }`, `{ session: ... }` — xem mục 3 #5) dù đã có envelope `{success, data, error}` chung — **luôn đọc `*.controller.ts`/`*.type.ts` thật trước khi viết DTO**, đừng suy đoán từ Swagger hoặc naming REST thông thường.
- **Quy ước UiState/Intent/Effect tách file riêng** (`<Feature>UiState.kt`, `<Feature>Intent.kt`, `<Feature>Effect.kt`) được chốt làm chuẩn chung sau khi phát hiện `feature:auth` (5 ViewModel Login/Register/Forgot/Otp/Reset) đi lệch pattern so với `feature:home`/`feature:home/search`/`feature:quiz-manage` (định nghĩa nested trong ViewModel) — đã refactor lại `feature:auth` cho đồng bộ (22/8). **Mọi feature mới đặt UiState/Intent/Effect ở file riêng, không nested trong ViewModel.**
- **Quiz cache (Room) là cơ chế fallback, không phải tính năng offline**: chỉ hữu ích khi mở lại **đúng quiz đã cache trước đó** mà request mạng thất bại (Home/Search không cache danh sách nên không thể chọn quiz mới lúc offline). Quyết định giữ nguyên scope fallback này (22/8), không mở rộng.
- `NetworkModule` (`core:network`) chỉ *nhận* `CookieStore`/`QuizCacheStore` qua constructor injection, không tự `@Provides` — Hilt gộp graph đúng ở `:app` dù 2 module Gradle không biết nhau (xem project_structure.md mục 12.1 design doc).

**📝 N13–14 Implementation Details (22/8):**

- ✅ **Domain/Data layer**: mở rộng `QuizRepository`/`QuizRepositoryImpl` với `getMyQuizzes` (Paging 3, `MyQuizzesPagingSource`) và `createQuiz`; thêm `MyQuizzesParams`, `NewQuiz` (domain), `QuizManageDtos.kt` (data) — **nhân dịp này vá luôn nợ kỹ thuật đã ghi ở N12** (`getMyQuizzes` bọc `data: { quizzes: [...] }` chưa unwrap đúng) bằng DTO wrapper giống cách đã fix quiz detail.
- ✅ **UseCase**: `GetMyQuizzesUseCase` (trả `Flow<PagingData<QuizCard>>`), `CreateQuizUseCase`.
- ✅ **Presentation**: package `presentation/quizmanagelist/` (UiState/Intent/ViewModel/Screen — danh sách quiz của tôi, Paging 3, page size 3 theo quyết định người dùng) + package `presentation/createquiz/` (UiState/Intent/Effect/ViewModel/Screen + editor đủ 4 loại câu hỏi).
- ✅ **Navigation**: `Routes.kt` + `AppNavGraph.kt` thêm route danh sách/tạo quiz (`Route.MyQuizzes`, tạo quiz).
- ⚠️ **Quyết định phạm vi (theo khảo sát người dùng)**: upload ảnh quiz/câu hỏi **chưa làm ở bản này** — để lại N15 (S3 presign 2 bước); nếu Paging 3 phát sinh lỗi khó xử lý có phương án fallback tải danh sách thủ công (chưa cần dùng tới, Paging 3 chạy ổn ở bản đầu).

**🎯 N13.5 — Home auth header & màn Profile (bổ sung ngoài kế hoạch, 22/8):**

> Không có trong lộ trình gốc — phát sinh khi làm UI/UX cạnh nút tìm kiếm ở Home mà N11 để lại TODO (xem mục Technical Debt của N11).

- ✅ Bỏ hẳn `TabRow` ("Khám phá"/"Của tôi") khỏi `HomeScreen` — Home giờ chỉ còn nội dung khám phá cuộn dọc.
- ✅ Thêm component auth-aware cạnh nút tìm kiếm trong `TopAppBar` của Home: chưa đăng nhập → nút "Đăng ký/Đăng nhập"; đã đăng nhập → avatar tròn (bấm vào → `Route.Profile`). Có `LifecycleEventEffect(ON_RESUME)` để re-check trạng thái đăng nhập mỗi khi quay lại Home.
- ✅ Màn `ProfileScreen` mới (đặt ở module `app`, không phải `feature:*`, vì gắn `Route.Profile` cấp app) — header avatar + tên + email, item "Quiz của tôi" điều hướng vào đúng màn `quizmanagelist` vừa làm ở N13–14, item "Đăng xuất".
- ✅ **Component `Avatar` chung mới** ở `core:ui/components/Avatar.kt` — quy ước mới cho toàn dự án: nơi nào cần hiển thị avatar user thì dùng component này (qua dependency `core:ui` đã có sẵn ở hầu hết module), **không** tự thêm `coil.compose` riêng — Coil là chi tiết nội bộ của `core:ui`. Chốt sau khi cân nhắc 2 hướng (thêm Coil trực tiếp vào module gọi vs. bọc trong `core:ui`), chọn hướng dùng lại vì avatar sẽ còn xuất hiện ở nhiều màn khác (lobby, leaderboard...).
- ⚠️ **Chưa có Bottom Navigation** (mục 11.4 design doc mô tả 5 tab Home/Discover/Join/Library/Profile) — hiện tại điều hướng Profile/MyQuizzes đi qua `NavController` thông thường từ Home, chưa có bottom nav bar. Xem design doc mục 11.5 (mới thêm) để biết chi tiết sai khác.

**📝 N15 Implementation Details (24/8):**

- ✅ **Domain/Data layer**: `PresignResult` (domain, `core:common`) + interface `StorageRepository` (`presignUpload(contentType, folder, fileSize)`, `uploadBytes(uploadUrl, contentType, bytes)`); impl ở `core:network` — `StorageApiService` (chỉ `POST /storage/presign`, qua Retrofit/`ResultCallAdapter` như API khác) + `StorageRepositoryImpl` (dùng `StorageApiService` lấy presigned URL, rồi tự PUT thẳng bytes ảnh lên `uploadUrl` bằng OkHttp thuần — **không** qua Retrofit, không cookie/auth header, vì đây là URL S3 presigned bên thứ 3, không phải backend của mình).
- ✅ **DI**: `@RawUploadOkHttpClient` — `OkHttpClient` riêng không có `cookieJar`/`authenticator` dùng cho bước PUT ảnh (client chuẩn có 2 thứ đó sẽ vô tình gắn cookie nội bộ vào request PUT S3, sai domain); `UploadImageUseCase` (feature layer) điều phối: nén ảnh → `presignUpload` → PUT bytes → trả `publicUrl` để gán vào `NewQuiz`.
- ✅ **Nén ảnh**: `ImageCompressor.kt` (resize + nén JPEG trước khi upload) — theo khảo sát người dùng trước khi code (nén = có, avatar = để sau, không làm ở N15).
- ✅ **UI**: viết lại `CreateQuizViewModel.kt` (sửa bug file bị cắt cụt ở bản trước) + `CreateQuizScreen.kt` thêm UI chọn ảnh cover (image picker); thêm `activity-compose` (`libs.androidx.activity` 1.13.0) vào `feature/quiz-manage/build.gradle.kts` cho `rememberLauncherForActivityResult`.
- 🔴 **Bug phát sinh khi test thật trên máy (24/8), đã fix cùng ngày**: Logcat cho thấy request `POST /storage/presign` gửi **snake_case** (`content_type`, `file_size`) dù `PresignUploadRequestDto` đã khai `@SerialName("contentType")`/`@SerialName("fileSize")` tường minh → backend trả `400 VALIDATION_ERROR` (đúng, vì schema thật của backend là camelCase — backend không có lỗi). **Nguyên nhân**: `Json { namingStrategy = JsonNamingStrategy.SnakeCase }` dùng chung toàn app (`NetworkModule.provideJson()`) vẫn biến đổi tên **sau khi đã resolve**, kể cả khi property đã có `@SerialName` tường minh — giả định trước đó rằng `@SerialName` sẽ "thoát" được `namingStrategy` chung là **sai**. **Fix**: tách hẳn `Json`/`Retrofit` riêng chỉ cho `StorageApiService` — 2 qualifier mới `@StorageJson` (Json **không** set `namingStrategy`) + `@StorageRetrofit` trong `Qualifiers.kt`, thêm `provideStorageJson()`/`provideStorageRetrofit()` trong `NetworkModule.kt`, đổi `provideStorageApiService` sang dùng `@StorageRetrofit`. Đã test lại trên máy thật, chạy đúng. Chi tiết đầy đủ ở `knowledgement/n15_knowledgement.md`.
- ⚠️ **Phạm vi đã chốt (giữ nguyên)**: avatar upload **chưa làm** ở N15 (để dịp khác, cùng cơ chế presign này dùng lại được); upload ảnh cover quiz hoạt động đúng luồng presign 2 bước.

### Tuần 4 (N16–20) — Socket layer & Lobby

- **N16**: Sửa/xóa quiz, hoàn thiện quiz-manage → **Chốt M3**. ✅ **XONG 25/8**

**📝 N16 Implementation Details (25/8):**

- ✅ **Domain/Data**: `QuizPatch` (core:common, mọi field optional khớp `updateQuizSchema = createQuizSchema.partial()`); `QuizRepository` + `updateQuiz`/`deleteQuiz`; `QuizApiService` thêm `PATCH`/`DELETE quizzes/id/{quizId}`; `UpdateQuizRequestDto` — field null bị omit khỏi body nhờ `explicitNulls=false` (NetworkModule), đúng semantics "field vắng = giữ nguyên" của backend.
- ✅ **`Question` domain thêm `correctAnswer: JsonElement?`** — backend `GET /quizzes/id/:quizId` trả `correct_answer` trong questions (quiz.repository.ts select cả field này); chỉ dùng để pre-fill màn Sửa quiz của chủ quiz, gameplay không đụng model này.
- ✅ **Cache**: `QuizCacheStore.removeQuiz` + `QuizCacheDao.deleteById` mới; update xong ghi đè cache, delete xong xóa cache — quiz đã xóa không "hồi sinh" từ Room khi offline.
- ✅ **Shared editor components** (`presentation/components`, KHÔNG đưa lên core:ui vì chỉ dùng trong quiz-manage): `QuestionDraft` chuyển từ createquiz (thêm `existingImageUrl` + extension `toNewQuestion` dùng chung); `QuestionEditorCard`/`ImagePickerSection`/`QuestionTypeMenuButton` nhận callback riêng lẻ thay vì Intent của từng màn — pattern `QuizEditor.vue` của web (editor dùng chung, màn chỉ khác load/save).
- ✅ **Màn Sửa quiz** (`presentation/editquiz`, 5 file): pre-fill từ quiz detail kể cả đáp án đúng (parse union `number[] | string`); submit gửi metadata + THAY THẾ toàn bộ questions (backend `replaceQuizQuestions` — không có patch từng câu); chỉ upload ảnh mới chọn lúc bấm Lưu (ảnh cũ giữ nguyên URL); dirty flag + dialog "Bỏ thay đổi?" khi thoát (BackHandler + nút back).
- ✅ **QuizDetail**: nút Chỉnh sửa/Xóa chỉ hiện với owner (`AuthRepository.getCurrentUser` so `quizOwner`; guest xem quiz public → 401 → false); dialog xóa cảnh báo hard delete mất cả lịch sử phòng chơi, giữ dialog mở kèm lỗi khi fail (pattern web); `QuizDeleted` effect → popBackStack; reload khi ON_RESUME (bỏ lần resume đầu).
- ✅ **QuizManageList refresh khi ON_RESUME** qua intent `Refresh` → `_refreshGeneration + 1` → `combine` → `flatMapLatest` tạo Pager mới → luôn load lại từ mạng (không dựa vào `LazyPagingItems.refresh()` vốn không đáng tin khi flow đã `cachedIn`).
- 🐛 **Bug thật user phát hiện khi test (đã fix cùng ngày)**: reload không bao giờ chạy ở cả 2 màn vì flag skip-first nằm trong `remember` — Navigation dispose composition khi rời màn nên flag bị reset mỗi lần quay lại → nhánh "bỏ qua" chạy mãi. Fix: `rememberSaveable` (state ghi vào SavedState của NavBackStackEntry). Chi tiết: `knowledgement/n16_knowledgement.md` mục 1.
- ✅ **Bổ sung sau test cùng ngày**: field "Thời gian (giây)" cho editor câu hỏi (`timeLimit` đã có sẵn draft → intent → DTO, chỉ thiếu UI; draft giữ String thô — trống = mặc định 30s, nhập thì validate 5–600s khớp quiz.schema.ts); `fallback`/`error` placeholder cho ảnh cover ở `QuizCardItem` (core:ui) + QuizDetail + MyQuizzes (TODO polish: ảnh mặc định trong res/).
- ⚠️ **Phát hiện khi đọc backend/frontend**: điểm lệch #10 (đã vá ở mục 3) — DELETE là hard delete; PATCH **không thể clear** `quiz_image`/`quiz_description` về null (field vắng = giữ cũ) — nút "xóa cover" của frontend web thực chất là vẽ lại default cover rồi upload (`ensureCover` + `defaultCover.js`), Android giữ hành vi "xóa cover = giữ cover cũ"; ảnh **câu hỏi** thì xóa được thật (questions bị replace toàn bộ). Cần theo dõi: `question_count` do trigger DB (migration 005) duy trì — nếu sau khi sửa câu hỏi mà count sai lệch thì là bug trigger backend, báo chủ backend.

**⚠️ Backlog "Editor UX gaps" (học từ frontend web, xử lý ở N38 polish hoặc phase 2):** duplicate question, move question lên/xuống, autosave draft local + banner khôi phục (`useQuizDraft`), default cover từ `res/`, crop ảnh (cover 1600×1000, câu hỏi 16:9), validation inline theo field + đếm ký tự `x/max`, "Try it yourself" (preview — map mode `solo` sau khi có gameplay), import xlsx (phase 2).
- **N16.5 (bổ sung khẩn, phát hiện 24/8 khi đối chiếu backend qua GitHub API)** — làm trước khi sang N17, vì ảnh hưởng `core:network`/`feature:auth` đã code ở Tuần 1–2. ✅ **XONG 25/8**
  1. **Sửa luồng Quên mật khẩu sai endpoint**: `core:network`/`feature:auth` đang gọi `/users/reset-password-token` và `/users/reset-password` — không tồn tại trên backend thật. Đổi sang 3 bước ticket thật: `POST /users/forgot-password` (giữ, đúng tên) → `POST /users/password-reset/verify` (body `{email,otp}` hoặc `{token}`, trả `{ticket, expiresAt, email}`) → `GET /users/password-reset/ticket?ticket=...` (peek, tuỳ chọn) → `POST /users/password-reset/complete` (`{ticket, newPassword}`). Ảnh hưởng: 3 DTO request cũ, `AuthApiService`/`UserApiService`, `ForgotPasswordViewModel`, `OtpVerificationViewModel`, `ResetPasswordViewModel` (bỏ nhánh dual-flow token/OTP song song, chuyển thành 3 bước tuyến tính).
  2. **Sửa `ApiEnvelope`/`ApiError` theo envelope lỗi thật**: lỗi REST chỉ có `{code}` (string), không có `message`/`details` như đã cài đặt ở N1–5. Sửa `ApiError` còn 1 field `code: String`; `AppError.Http` đổi `message` → `code`; thêm hàm map `code` → `UiText` tiếng Việt hiển thị cho user (server không trả message).
  3. **Áp dụng pagination cursor thật cho danh sách quiz**: `/v1/quizzes/search`, `/v1/quizzes/me`, `/v1/quizzes/users/id/:ownerId` đã hỗ trợ `cursor`/`limit` (1–24)/`include_total` — khác ghi chú kỹ thuật nợ ở N11/N13 ("backend chưa có pagination"). Refactor `SearchQuizzesUseCase`/`GetMyQuizzesUseCase`/Paging 3 `PagingSource` dùng `cursor` thật thay vì tham số `page` không dùng tới.

**📝 N16.5 Implementation Details (25/8):**

- ✅ **Envelope lỗi thật (điểm lệch #7)**: `ApiErrorBody` chỉ còn `code`; `AppError.Api(message, details)` → `AppError.Api(code)`; `ResultCall` đọc `error.code` từ body; `AppErrorExt.toUserMessage()` viết lại thành map ~60 code trong `shared/errors/codes.ts` → tiếng Việt (fallback chung cho code lạ). Vá cả `StorageRepositoryImpl` (lỗi S3 thô → `AppError.Server`).
- ✅ **Luồng Quên mật khẩu 3 bước (điểm lệch #8)**: service mới `PasswordResetApiService` (forgot-password → verify `{email,otp}` XOR `{token}` → GET ticket peek → complete `{ticket,newPassword}`) với Json/Retrofit RIÊNG (`@PasswordResetJson`/`@PasswordResetRetrofit`) vì module user backend dùng **camelCase thật** trên wire — tái dùng đúng pattern N15. Màn OTP **verify thật ngay khi bấm** (trước chỉ navigate, OTP sai chỉ lộ lúc submit pass); nút gửi lại **đếm ngược 60s kể từ khi vào màn** (RESET_RESEND_TTL tính cả lần gửi ở màn Forgot); sửa text TTL sai "5 phút" → 2 phút (RESET_TTL). Màn Reset bỏ dual-flow + ToggleFlow, chỉ nhận `ticket`, **peek lúc mở màn** để hiện email + chặn sớm ticket hết hạn (10 phút); deep link `?token=` tự verify đổi lấy ticket. Route: `ResetPassword(ticket, token, email)`. 2 usecase cũ (`ResetPasswordUseCase`/`ResetPasswordWithOtpUseCase`) thành stub chờ xóa tay trong IDE.
- ✅ **Search cursor thật (điểm lệch #9)**: `searchQuizzes(keyword, cursor, limit)` xuyên suốt ApiService → Repo → UseCase → ViewModel; `nextCursor`/`hasMore` lấy từ `meta.pagination` thật thay vì đoán `size >= 20`; reset cursor khi đổi query (tránh `QUIZ_CURSOR_INVALID`). `/quizzes/me` vốn đã chuẩn cursor từ N13–14 → không đụng.
- 🐛 **2 hotfix khi user test (bug sót từ N11–12, không phải do N16.5)**: (1) `SearchScreen` **không có đường nào gọi `SubmitSearch`** — không nút tìm, không keyboardActions → gõ từ khóa mà không request nào được gửi ("không có kết quả với mọi từ khóa"); fix bằng icon kính lúp (`leadingIcon`) + `ImeAction.Search` — user quyết KHÔNG real-time/debounce, chỉ submit thủ công. (2) Click quiz card trong search trỏ vào hàm TODO rỗng → giờ navigate thẳng QuizDetail; dọn intent `QuizCardClicked` chết. Kèm sửa luôn bug load-more-lặp-trang-1 (backend không đọc `page`). Chi tiết: `knowledgement/n16_5_knowledgement.md`.
- [x] **N17**: `CreateRoomScreen` tạo phòng từ `GET /v1/games/game-modes`, gọi `POST /v1/games`, lấy token qua `POST /v1/games/:id/host-token`; UI chia editor rõ theo 5 mode nhưng default/constraint vẫn lấy từ backend. ✅ **XONG 28/8**

**📝 N17 Implementation Details (28/8):**

- ✅ **Audit contract thật trước khi code**: modes nằm ở `data.gameModes`; create request dùng `quiz_id`, `session_name`, `mode`, `config`; create response lồng `data.data.session` kèm `ignored`; host token phải gọi endpoint riêng và đọc `data.hostToken.socketToken`.
- ✅ **Networking/DI**: mở rộng `GameApi`, thêm `GameSessionRepository`/`GameSessionRepositoryImpl` + 3 use case; Games dùng shared `@PreserveCaseRetrofit` vì payload trộn outer snake_case với nested `GameConfig` camelCase. Refactor qualifier cũ của Storage/Password Reset thành cặp PreserveCase dùng chung; giữ Retrofit SnakeCase mặc định và `RawUploadOkHttpClient` riêng.
- ✅ **Typed boundary**: `JsonElement`, `JsonNull`, dotted path và logic dựng nested JSON chỉ tồn tại trong DTO/mapper của `core:network`. Domain dùng `GameConfigKey`, `GameConfigValue`, `GameConfigConstraint`; presentation dùng `RoomConfigForm` typed, không cast JSON trong UiState/Intent/Composable.
- ✅ **UI theo mode, không render map mù**: `GameModeConfigEditor` dispatch bằng `when` sang `ClassicModeEditor`, `SoloModeEditor`, `SurvivalModeEditor`, `MarathonModeEditor`, `PracticeModeEditor`. Layout/nhãn thuộc từng editor; default/min/max/nullable/options/editable vẫn lấy từ descriptor backend. Pattern text + switch tách thành `SettingSwitchRow` dùng chung ở `core:ui`.
- ✅ **Validation + patch**: tên phòng 2–100 ký tự; number field validate theo constraint backend; đổi mode reset đúng baseline; chỉ gửi field khác default; typed patch chỉ đổi sang dotted-path JSON tại network boundary. Quy tắc xung đột descriptor: `locked` thắng `editable` (backend hiện báo `flow.allowAnswerLate` trong cả hai collection).
- ✅ **MVI/partial success**: intent typed (`ToggleChanged`/`NumberChanged`/`ChoiceChanged`), guard double-submit. Nếu create thành công nhưng host-token lỗi thì giữ `pendingSession`; retry chỉ gọi host-token, không tạo phòng thứ hai. Unauthorized đi Auth flow.
- ✅ **Navigation**: thành công đi `Route.HostLobby(gameId, socketToken, sessionCode)` và pop CreateRoom; HostLobby thật thuộc N20 nên hiện dùng `HostLobbyPlaceholder` để nhìn rõ mã phòng/Game ID và xác nhận hand-off N17.
- ✅ **Regression tests**: PreserveCase wire JSON, typed key → nested camelCase patch, mode descriptor/locked precedence, `data.data.session`, host-token unwrap và baseline diff. Build/test máy thật đã xanh và code đã push.

**🧠 Bài học N17:**

- Contract động từ backend không đồng nghĩa UI phải render mù bằng `Map<String, JsonElement>`; cách cân bằng tốt là backend sở hữu value/constraint, còn app sở hữu layout/label theo mode.
- Transport representation (`JsonElement`, snake_case, dotted path) phải dừng ở network boundary; để chúng đi vào UiState/Intent vẫn giữ được one-way MVI nhưng làm yếu Clean Architecture và type-safety.
- Chuỗi create session → issue host token là hai request, phải thiết kế như partial transaction để retry idempotent phía client; tuyệt đối không create lại khi request token lỗi.
- Khi backend trả một key trong cả `editable` và `locked`, client phải áp quy tắc deterministic `locked wins` để không vẽ control chỉnh được nhưng server lại âm thầm bỏ qua.

**📝 N18 Implementation Details (30/8):**

- **`core:common` (4 file domain mới)**: `GameEvent` (`Connected` / `Disconnected(DisconnectReason)` / `LobbyUpdated(LobbyState)` / `Failed(event, code)` / `Unhandled`), `LobbyState(sessionStatus, config, players, serverTime)`; `GameSocketRepository` (base: `events(socketToken)`, `joinLobby`, `disconnect`) + `HostGameSocketRepository` (`startGame/nextQuestion/pauseGame/resumeGame/endGame`) + `PlayerGameSocketRepository` (`leaveLobby/submitAnswer/requestNextQuestion/sync`). Tách interface theo vai trò để gửi sai lệnh thành lỗi biên dịch, không phải lỗi runtime từ server.
- **`core:network` (7 file mới)**: `GameSocketClient` (`callbackFlow` + `awaitClose` dọn listener và socket, handshake `auth.token`, dịch lý do disconnect thành `DisconnectReason`), `GameEventMapper` (parse bằng `runCatching`, lỗi → `Failed(event, "CLIENT_PARSE_ERROR")`), `GameSocketEvents` (hằng tên 19 server event + các client event), `SocketDtos` (snake_case bằng `@SerialName` + `@PreserveCaseJson` — vẫn là cái bẫy của N15), `HostGameSocketRepositoryImpl`, `PlayerGameSocketRepositoryImpl`, `SocketBindingModule`.
- **`core/common/build.gradle.kts`**: thêm `api(libs.kotlinx.coroutines.android)` — phải là `api` chứ không `implementation`, vì `Flow` nằm trong signature công khai của interface repository.
- **`feature:lobby` (6 file mới)**: `HostLobbyUiState`/`HostLobbyIntent`/`HostLobbyEffect`/`HostLobbyViewModel`/`HostLobbyScreen` theo baseline Stateful + `HostLobbyScreenContent` stateless, cùng `RefreshHostTokenUseCase`. Chưa thêm Intent `StartGame` vì N18 không có UI trigger thật cho nó (luật "mọi Intent phải có đường kích hoạt thật") — để N20.
- **Reconnect (spike đã thành hàng thật)**: `lobby:join` gọi lại sau mọi `Connected`; mất mạng → trạng thái `RECONNECTING`, giữ nguyên danh sách người chơi cũ; `io server disconnect` → thoát hẳn; `GAME_TOKEN_INVALID` → refresh token qua REST đúng một lần rồi kết nối lại, vẫn fail thì thoát.
- **Test**: `GameEventMapperTest` 6 case (mixed snake/camel của `lobby:updated`, thiếu `config`, `error` giữ nguyên code, payload rác, payload null, event gameplay → `Unhandled`), fixture copy từ `socket.doc.ts`, dùng chính `NetworkModule.providePreserveCaseJson()` của production thay vì tự tạo `Json` trong test.
- **Nợ nhỏ để lại**: `onExit(message)` ở nav graph chỉ `popBackStack`, chưa hiển thị lý do bị buộc rời phòng (N19 — ✅ đã trả 5/9: `KEY_LOBBY_EXIT_MESSAGE` qua `savedStateHandle` + snackbar ở JoinRoom); `submitAnswer` chưa xử lý ack (N21+); `player_avatar`/`lives` chưa vào DTO (N19 — ✅ đã thêm 5/9).

**🧹 Refactor kiến trúc UI trước N18 (28/8, ngoài kế hoạch ngày):**

- ✅ Audit đủ 14 file `*Screen.kt`; `LoginScreen`, `RegisterScreen`, `SplashScreen` đã đúng từ trước, 11 màn còn lại được chuẩn hóa thành Stateful `XxxScreen` + stateless `XxxScreenContent`.
- ✅ Stateful boundary sở hữu ViewModel/Hilt, Flow collection, lifecycle effect, navigation/effect, Photo Picker, `BackHandler`, focus request, pagination observer và transient UI state cần hoist.
- ✅ Stateless content chỉ nhận state/value/callback; Preview gọi content trực tiếp bằng fake state/no-op callback, ưu tiên Light/Dark.
- ✅ Tạo `QuizEditorContent` dùng chung cho Create/Edit Quiz; component có state cục bộ như question type menu và choice field giữ convenience wrapper nhưng bổ sung stateless content API lõi.
- ✅ Không đổi backend contract, ViewModel, repository hay domain behavior; đây là refactor kiến trúc, không đánh số N17.5 và không dịch lịch N18.
- ✅ Build/test máy thật đã xanh và code đã push. Chi tiết: `knowledgement/ui_stateful_stateless_refactor.md`.

- ✅ **N18** (xong 30/8): Socket layer — `GameSocketClient` (namespace `/game`, thay cho ý tưởng `SocketFactory` trong doc), `GameEventMapper`, connect → `lobby:join`, spike reconnect. ⚠️ **Đã vá điểm lệch của doc tại đây**: contract lỗi socket thật là `{ event, code }` (schema `SocketError` trong `backend/src/docs/components/socket.doc.ts`) — KHÔNG có `message`, KHÔNG có prefix `UNAUTHORIZED:`/`FORBIDDEN:`/`CONFLICT:`/`GONE:`. Vì vậy **không thêm `AppError.Socket`** như doc yêu cầu: `AppError.Api(code)` đã map sẵn ~60 code sang tiếng Việt từ N16.5, thêm nhánh mới chỉ là trùng lặp. 4 code fatal (`GAME_TOKEN_INVALID`, `GAME_TOKEN_WRONG_ROOM`, `GAME_ROOM_NOT_FOUND`, `GAME_PLAYER_NOT_FOUND`) → thoát màn thay vì retry; riêng `GAME_TOKEN_INVALID` được thử refresh token đúng một lần trước khi thoát.

**📝 N18.5: Polish Architecture (31/8 - 2/9/2026):**

**Mục tiêu:** Chuẩn hóa Clean Architecture + MVI pattern toàn dự án sau khi hoàn thành N18.

**Thành tựu:**
- ✅ Sửa 5 vi phạm nghiêm trọng architecture: navigation logic in ViewModel (Home, Profile), god methods (CreateQuiz/EditQuiz submit ~50-65 lines), layer violations (ViewModels gọi trực tiếp multiple repositories thay vì qua UseCase)
- ✅ Chuẩn hóa naming conventions: `handleIntent` → `onIntent` (26 occurrences, 16 files), `GameApi` → `GameApiService` (9 occurrences, 3 files)
- ✅ Refactor navigation: tách `AppNavGraph.kt` (~267 lines) thành 4 feature graphs — `AuthNavGraph` (5 routes), `MainNavGraph` (6 routes), `QuizManageNavGraph` (5 routes), `GameNavGraph` (5 routes). AppNavGraph còn 60 lines (giảm 78%), chỉ giữ Splash + gọi 4 sub-graphs.
- ✅ Thống nhất validation pattern: tạo 6 validators tái sử dụng ở `:core:common/validator` (ValidationResult, EmailValidator, PasswordValidator, QuizNameValidator, QuizDescriptionValidator, RoomSettingsValidator) thay vì inline validation rải rác; refactor 5 ViewModels (Login, Register, CreateQuiz, EditQuiz, CreateRoom) sang Pattern C.
- ✅ Extract orchestration UseCases: `CreateQuizWithAssetsUseCase`, `UpdateQuizWithAssetsUseCase` (thay CreateQuiz/UpdateQuiz/UploadImage riêng lẻ), `GetQuizWithOwnershipUseCase` (thay GetQuizDetail) — ViewModels giảm từ 50-65 lines submit logic xuống ~30 lines.

**Files affected:** 30+ files across 8 modules (app, core:common, core:network, feature:auth, feature:home, feature:quiz-manage).

**Lessons learned:**
- MVI Effect pattern phải áp dụng đồng nhất từ đầu cho mọi ViewModel — navigation/one-shot events KHÔNG BAO GIỜ ở ViewModel state, luôn qua Effect channel.
- God methods (>40-50 lines) trong ViewModel.submit() là dấu hiệu thiếu UseCase orchestration layer — phải extract business logic ra UseCase, ViewModel chỉ validate + build DTO + delegate.
- Validation logic dùng lại (email, password, quiz name) phải centralized ở `:core:common` thay vì duplicate ở mỗi ViewModel — Pattern C (validator objects) scale tốt hơn inline checks.
- Navigation modular theo feature (4 NavGraphs) dễ maintain và parallel development hơn monolithic AppNavGraph — mỗi feature module có thể contribute routes riêng.

**Chi tiết kỹ thuật:** `REFACTOR_CHECKLIST.md` (628 lines, 14 tasks, 4 sprints). Build/test thành công trên máy thật, code đã push 2/9.

- [x] **N19**: `feature:lobby` Player — lookup room, join REST → `socketToken` → connect; PlayerLobbyScreen. ✅ **XONG 5/9**
- **N19.5** (bổ sung ngoài kế hoạch, chờ ảnh thiết kế từ user): Bottom Navigation thật cho `MainGraph` — trả nợ từ N13.5 (design doc 11.4/11.5), chuyển nút "Vào phòng" ra khỏi TopBar Home (hiện là `TextButton` tạm), chốt `Route.Library` vs `Route.MyQuizzes`.
- **N20**: HostLobbyScreen + `lobby:config-update` (ACK `{ok, changed, config, ignored}`); chia sẻ mã phòng/QR.
- 📚 Socket.IO Android nâng cao (`IO.Options.auth`, namespace, ACK `emit` + `Ack {}`, `EVENT_CONNECT`/`EVENT_DISCONNECT`), `callbackFlow` + `awaitClose`; làm quen MVI (Intent sealed → 1 StateFlow).

**📝 N19 Implementation Details (3–5/9):**

- ✅ **Audit backend trước khi code** (đọc `game.route.ts`, `game.schema.ts`, `game.controller.ts`, `game.service.ts`, `socket.channels.ts`): `GET /games/:code` là **public**, `POST /games/:code/join` dùng **optionalAuth** (có cookie → player thật, không có → khách); `joinGameSchema` nhận `player_name` 1–50, `player_guest_id` **phải là UUID**; thứ tự guard của server: 404 `GAME_ROOM_NOT_FOUND` → 409 `GAME_ALREADY_STARTED` → 403 `GAME_GUESTS_NOT_ALLOWED` → 403 `GAME_HOST_CANNOT_JOIN` → 409 `GAME_ROOM_FULL`. Join trả `data.player` + `data.socketToken` **phẳng** (khác host token lồng một cấp `data.hostToken.socketToken`). Không có endpoint refresh token cho player — khác host, nên `GAME_TOKEN_INVALID` của player là fatal, join lại từ đầu.
- ✅ **`core:common`**: `RoomLookup` (+ `isOpenForJoin`, `isFull`), `JoinRoomResult`/`JoinedPlayer`, `LobbyPlayer` thêm `playerAvatar`/`lives` (trả nợ N18), `NicknameValidator` (1–50 ký tự, khớp schema backend), `GameSessionRepository` thêm `lookupRoom`/`joinRoom`.
- ✅ **`core:network` + `core:datastore`**: `GameApiService` thêm 2 endpoint; `GameDtos` thêm `RoomLookupResponseDto`/`LobbySnapshotDto`/`JoinGameRequestDto`/`JoinGameResponseDto`/`JoinedPlayerDto`; `LobbyPlayerDto` (socket dto) **dùng lại cho cả REST** — backend trả bản `Pick` khi đọc Postgres và full row khi Redis còn nóng, DTO có default nên parse được cả hai. `GuestIdentityStore` mới (DataStore): UUID sinh **lần đầu cần join** rồi giữ mãi — để backend nhận ra cùng một khách khi reconnect/xem lịch sử (`x-guest-id`).
- ✅ **UseCase**: `LookupRoomUseCase` (normalize mã về uppercase + trim trước khi gọi), `JoinGameUseCase` (tự cấp guest UUID khi chưa đăng nhập).
- ✅ **Presentation (3 package mới, đúng baseline Stateful/Stateless + UiState/Intent/Effect tách file)**: `joinroom` (nhập mã phòng), `guestnickname` (chỉ khách thấy màn này), `playerlobby` (realtime qua `PlayerGameSocketRepository`, hiện danh sách người chơi + trạng thái kết nối, host thoát/huỷ phòng → pop kèm lý do).
- ✅ **Navigation**: `Route.GuestNickname(sessionCode)` mới; `GameNavGraph` viết lại với `KEY_LOBBY_EXIT_MESSAGE` + helper `popWithMessage`; `MainNavGraph` đọc message từ `savedStateHandle` của entry đích rồi hiện snackbar ở JoinRoom — **pattern chuẩn cho mọi "kết quả trả về khi pop"** từ nay.
- 🔴 **Bug thật khi test trên máy (đã fix cùng ngày)**: crash `MissingFieldException` ở bước tra phòng — nguyên nhân là điểm lệch #11 (`data.session.session`). Fix bằng `LobbySnapshotDto` + KDoc ghi rõ đây là bug backend và cách dọn khi backend sửa. Rà lại toàn bộ mapping REST/socket của game trong cùng phiên: 5 endpoint + `lobby:updated` còn lại đều đúng, không phải sửa thêm.
- ✅ **Test**: `GameDtosTest` thêm 3 case (payload lồng ba cấp → `RoomLookup` + đếm player, join trả `socketToken` phẳng + bỏ qua cột thừa của `player_sessions`, body join của người đã đăng nhập encode ra `{}`); `GameEventMapperTest` thêm 2 case cho `player_avatar`/`lives`.
- ⚠️ **Nợ nhỏ để lại**: danh sách `players` lấy sẵn ở bước tra phòng chưa được đổ vào state đầu của PlayerLobby (socket `lobby:updated` fill ngay sau đó nên chưa cần); nút "Vào phòng" ở Home vẫn là `TextButton` tạm chờ N19.5; 2 file usecase stub từ N16.5 vẫn chờ xóa tay trong IDE.

**🧠 Bài học N19:**

- **Không tin tên key trong controller**: `success(res, { session })` đọc rất thuyết phục nhưng biến `session` ấy do `getLobby` trả và chứa cả `{session, players, config}`. Phải truy tiếp vào `*.service.ts` mới biết shape thật — mở rộng bài học N12 ("đọc controller thật") thêm một tầng.
- **Đọc lỗi kotlinx như một manh mối, không chỉ là stacktrace**: danh sách "fields ... were missing" kèm `at path:` đủ để định vị lỗi. Field nào **không** bị báo thiếu chính là field thật sự có trong JSON — ở đây `config` vắng mặt trong danh sách đã tỏ ra cấp ngoài là cụm lobby chứ không phải object rỗng.
- **Client phải map theo payload đang chạy, không theo payload đúng** — nhưng phải ghi KDoc nói rõ đây là bug backend + cách dọn, kèm test hồi quy để khi backend sửa thì test đỏ ngay thay vì user gặp crash.
- **Đếm tại chỗ thay vì tin cột tổng hợp**: `total_players` chỉ được flush cuối ván nên luôn lệch trong lobby — kiểm tra "phòng đã đầy" phải đếm `players.size`.
- **Quyền do server cấu hình, client chỉ phản ánh**: cho khách vào hay không nằm ở `config.lobby.allowGuests` của từng phòng; client không được hardcode chính sách, chỉ đọc cấu hình và hiển thị đúng lý do khi bị chặn.
- **Nav result pattern**: muốn trả dữ liệu về màn trước lúc pop thì ghi vào `savedStateHandle` của back stack entry đích rồi đọc-và-xóa ở đó; đừng nhồi vào route argument hay ViewModel dùng chung.

### Tuần 5 (N21–25) — Gameplay Player: host-paced (classic)

- **N21**: `GameViewModel` + `GamePhaseUi` state machine; countdown overlay từ `game:countdown`.
- **N22**: `question:started` → `question:locked` → `question:results`; `AnswerInput` dispatcher 4 loại câu.
- **N23**: Timer sync theo offset `serverTime`; khóa input ngay sau submit.
- **N24**: `leaderboard:updated` + `answer:received` (không lộ đúng/sai); kết quả giữa câu.
- **N25**: E2E classic nhiều máy thật → **Chốt M4**.
- 📚 MVI thực chiến (sealed phases, one-shot events), timer offset. **Bắt đầu unit test ViewModel bằng Turbine từ tuần này** — không dồn Tuần 9.

### Tuần 6 (N26–30) — Gameplay Player: self-paced (solo/survival/marathon/practice)

- **N26**: `question:started` cá nhân; đọc kết quả ngay từ ACK (`isCorrect`, `scoreEarned`, `streak`, `correct_answer` khi được reveal).
- **N27**: `question:next` (autoAdvance=false) + `question:awaiting_next` khi reconnect.
- **N28**: Survival (lives UI, `player:eliminated`) + Marathon (`matchEndsAt`, `question:timeout`).
- **N29**: Practice (điểm = 0, luôn reveal) + review mode (`game:review` + explanation).
- **N30**: `player:sync` khi resume; token hết hạn → gọi lại REST lấy token mới.
- 📚 Đọc `sendSelfQuestion`/`onAnswer` trong `game.socket.ts` trước khi code — domain khó nhất app; player clock phía server.

### Tuần 7 (N31–35) — Host console & Leaderboard

- **N31**: `HostGameScreen` + `HostGameViewModel` riêng; `game:state` làm nguồn chính.
- **N32**: `host:question` (có đáp án), `host:answer-received` (có `is_correct`), `host:player-progress` (self-paced).
- **N33**: Điều khiển start/pause/resume/next/end theo `autoAdvance`.
- **N34**: `leaderboard:host` full table.
- **N35**: `feature:leaderboard` — FinalResultScreen + REST fallback `/v1/games/:id/results` + thống kê từng câu → **Chốt M5**.
- 📚 Không chủ đề mới — củng cố MVI + socket; map đúng payload host room.

### Tuần 8 (N36–40) — Hardening & polish

- **N36**: Reconnect hoàn chỉnh (re-emit `lobby:join`; **không** tự reconnect khi "io server disconnect").
- **N37**: Error envelope → `AppError`; `GONE` → về Home; banner offline.
- **N38**: UI polish, animation, dark mode, accessibility.
- **N39**: `GameHistory` (Room) + màn lịch sử.
- **N40**: Buffer bugfix tuần 5–8.
- 📚 LeakCanary (Socket factory tạo instance mới mỗi phiên — dễ leak nếu quên `disconnect`), R8/ProGuard keep-rules (kotlinx.serialization + socket.io-client), `network_security_config.xml`, Crashlytics.

### Tuần 9 (N41–45) — Testing & release build

- **N41–42**: Unit test ViewModel (Turbine + fake socket repo) **cả 2 nhánh** host-paced/self-paced.
- **N43**: MockWebServer (cookie/401/refresh) + Room in-memory.
- **N44**: Compose UI test: GamePlay (2 nhánh), HostGame, Lobby.
- **N45**: LeakCanary, ProGuard/R8, network security config, release build → **Chốt M6**.
- 📚 JUnit4 + `runTest`, MockK, Turbine (`state.test { awaitItem() }`), MockWebServer enqueue 401 → 200, Room in-memory, Compose UI Test (semantics).

### Tuần 10 (N46–50) — UAT & ship

- **N46–47**: E2E đủ 5 mode với người thật; load test nhẹ 20–50 player/phòng.
- **N48**: Fix bug UAT; tích hợp Crashlytics/analytics.
- **N49**: Internal release Play Console; tài liệu sử dụng.
- **N50**: Go/No-Go review, bàn giao, retrospective + backlog phase 2 (import xlsx, mode team…) → **Chốt M7**.
- 📚 Play Console internal track, app signing, đọc crash report.

## 7. Việc backend song song (không block app)

- **N1–3**: Vá `session_code` unique (partial index + ON CONFLICT retry).
- **N1–5**: Vá design doc 6 điểm lệch ở mục 3.
- **N10–15**: Unit test `scoring.ts` + `config.rule.ts`.
- **Backlog**: gate `/v1/api-docs` theo env, chuẩn hóa zod, xóa enum `team`, bỏ publish port DB/Redis ở prod, validate quiz rỗng khi tạo phòng.

## 8. Rủi ro & giảm thiểu

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Chưa từng chia Gradle multi-module | Cao | ✅ Đã qua (M1 chốt 9/8); phương án B gộp 6 module không cần dùng nữa |
| Chưa có thói quen viết test | TB | Viết test ViewModel từ Tuần 5, không dồn Tuần 9 |
| Quen pattern JWT Interceptor từ dự án cũ | TB | Tuần 2 tập trung gỡ — backend chỉ đọc cookie |
| Socket.IO Android + namespace/cookie có quirks | TB | Spike N16–18 trên máy thật |
| Timer skew giữa các máy | TB | Offset `serverTime` tính lại mỗi event; test mạng yếu |
| Google OAuth / S3 chưa có tài khoản | TB | Đăng ký ngay Tuần 1–2 (One Tap cần ở N9) |
| Bug backend phát sinh khi tích hợp | TB | Log toàn bộ socket event ở debug build; kênh fix nhanh với chủ backend |
| Scope creep (import xlsx, history, animation) | Cao | Cắt sang phase 2 nếu trễ milestone |

## 9. Definition of Done

- Pass toàn bộ checklist kiến trúc/auth/realtime/testing ở mục 20 của design doc v2 (bản đã vá).
- 5 mode chơi E2E trên ≥3 thiết bị; reconnect giữa trận không mất state; `GONE` điều hướng đúng.
- Release build bật R8 + network security config; LeakCanary không báo leak socket; Crashlytics hoạt động.