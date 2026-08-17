> 🎯 **Tóm tắt**: Backend myquizz (Express + TypeScript + Socket.IO + PostgreSQL + Redis) chất lượng khá tốt, đúng chuẩn server-authoritative. Design doc v2 khớp ~90% code thật, có 6 điểm lệch cần vá (mục 4). Kế hoạch 50 ngày / 10 tuần đã hiệu chỉnh theo trình độ thực tế. **Trạng thái: Tuần 1 (N1–N5) hoàn tất 11/8 — Foundation + core:network xong. Bước tiếp theo: Tuần 2 — Auth & Navigation (mục 6.4).**

File này tự chứa đủ ngữ cảnh để bắt đầu phiên làm việc mới (đã gộp & tinh gọn log các phiên 9–11/8). Chi tiết kỹ thuật (API/socket/DI/tree module): https://app.notion.com/p/ea02b2e9982b4b72b7fa75440028621a

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
| 🟡 | `POST /v1/games` trả `data.data.session` (lồng kép) | Client map DTO đúng; đề xuất backend sửa phẳng |
| 🟡 | `/v1/api-docs` public mọi môi trường | Gate theo `NODE_ENV !== 'production'` |
| 🟡 | Song song joi + zod; enum `game_mode` chứa `'team'` chưa implement | Chuẩn hóa zod; xóa `'team'` hoặc bổ sung engine |
| 🟡 | Cookie auth chỉ dựa `SameSite=Lax`, không CSRF token | Giữ lax; cân nhắc CSRF token khi mở rộng surface |
| 🟢 | Ternary thừa trong cookie config; root README rỗng; compose publish port 5432/6379; tạo phòng không chặn quiz 0 câu | Dọn dẹp; validate `total_questions > 0` |

### 2.3. Frontend web (nhanh)

Vue 3 thuần JS + Pinia + vue-query + Tailwind/GSAP; có import quiz từ xlsx (~17KB, đáng port sang Android phase 2) và `mock.api.js` (phát triển UI không cần backend — pattern nên học theo). Web = quản lý + join; **gameplay nằm ở Android** (khớp design doc).

## 3. Đối chiếu Design Doc v2 ↔ code thật

**Khớp ~90%**: cookie HttpOnly auth (không Bearer); REST trước → `socketToken` → Socket.IO handshake `auth.token`; 5 mode + pacing host/self; bảng event 5.2–5.3; `AnswerAck`; 4 `question_type`; envelope `{success, data, error}`; upload ảnh presign S3 2 bước; CookieStore DI qua `core:common`.

**6 điểm lệch — vá doc trước khi code phần liên quan:**

| # | Doc đang ghi | Code thật | Hành động Android |
|---|---|---|---|
| 1 | Route không prefix | Mọi REST dưới `/v1`; game router ở `/v1/games` | `BASE_URL = https://api.myquizz.dpdns.org/v1/` |
| 2 | Socket base URL mặc định | Server dùng namespace `/game` | `IO.socket(SOCKET_URL + "/game", options)` |
| 3 | `question:started` = `{question, time_limit, endsAt, serverTime}` | Self-paced thêm `matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `lives` | Bổ sung model `SelfQuestionStarted` |
| 4 | Không có REST update config | Có `PATCH /v1/games/:id/config` (host, lobby-only) | App ưu tiên kênh socket `lobby:config-update` (có ACK + broadcast) |
| 5 | `CreateGameSession` trả phẳng | Thực tế lồng `data.data.session` (kèm `ignored`) | DTO map đúng |
| 6 | `game:state` mô tả chung chung | Snapshot gồm `question`, `countdown`, `endsAt/matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `player` (gated reveal), `leaderboard` (rỗng khi câu đang mở) | Nguồn dữ liệu chính của Host screen + resync |

Lưu ý thêm: swagger ghi `/auth/refresh` trả tokens nhưng thực tế chỉ trả `{message}`.

## 4. Hồ sơ kỹ năng

**Đã có sẵn** (từ My Schedule + Clover Chatty): Kotlin + Coroutines/Flow • Compose M3 • Clean Architecture 3 tầng + UseCase • Hilt • Socket.IO client • Retrofit/OkHttp • Room + DataStore • MVVM • Custom Canvas, AlarmManager, Notification • FCM • offline-first / safeApiCall / UiText.

**Cần học** (đã chèn vào tuần tương ứng):

| # | Chủ đề | Tuần |
|---|---|---|
| 1 | Gradle multi-module (version catalog, convention plugins — spike nowinandroid) | 1 ✅ |
| 2 | Cookie auth: OkHttp CookieJar + Authenticator (gỡ thói quen Bearer) | 1–2 |
| 3 | Credential Manager + Google One Tap | 2 |
| 4 | kotlinx.serialization (bỏ Gson) | 3 |
| 5 | Paging 3 (có thể thay phân trang tay) + S3 presign 2 bước | 3 |
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

**🤔 Navigation Architecture — Pending Decision (17/8):**

⚠️ **Vấn đề hiện tại**: Trong Tuần 2, để test flow sau login, đã tạo **host placeholder screen** làm điểm đích tạm thời sau khi authentication thành công. Đây là workaround cho việc màn Home chưa được implement.

**Hai phương án đang cân nhắc:**

1. **Option A — Auth-First Flow** (hiện tại):
   - Splash → check auth → Login/Register (nếu unauthenticated)
   - Login thành công → xóa host placeholder → điều hướng thẳng đến **Home**
   - Guest mode: Splash → Home trực tiếp (bypass auth)

2. **Option B — Browse-First Flow** (đề xuất):
   - Splash → **Home** trực tiếp (browse mode)
   - Auth screens chỉ hiện khi user **chủ động chọn login** từ Home để sử dụng chức năng cụ thể (tạo quiz, host game, etc.)
   - Soft auth gates: "Bạn cần đăng nhập để sử dụng tính năng này"
   - Flow này khớp với backend `optionalAuthMiddleware` pattern và web frontend

**Decision point**: Sẽ thảo luận và quyết định sau khi hoàn thành **màn Home (N11)** để đánh giá UX thực tế với bottom navigation và guest mode.

**Action items**:
- [ ] Xóa host placeholder screen sau khi xác định flow cuối cùng (N11)
- [ ] Cập nhật navigation graph theo option đã chọn
- [ ] Đảm bảo guest mode integration mượt mà với option đã chọn

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
- [ ] **N12**: Quiz detail + cache Room.
- [ ] **N13–14**: `feature:quiz-manage` — danh sách + tạo quiz; editor 4 loại câu hỏi (`multiple_choice`, `multiple_select`, `short_answer`, `long_answer`).
- [ ] **N15**: Upload ảnh presign S3 2 bước (`UploadImageUseCase`, PUT trực tiếp, không cookie) + Coil.
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

### Tuần 4 (N16–20) — Socket layer & Lobby

- **N16**: Sửa/xóa quiz, hoàn thiện quiz-manage → **Chốt M3**.
- **N17**: `CreateRoomScreen` render động từ `GET /v1/games/game-modes` (theo editable/locked spec, không hardcode 5 mode).
- **N18**: Socket layer — `SocketFactory` (namespace `/game`), `GameEventMapper`, connect → `lobby:join`; spike reconnect sớm. ⚠️ **Bổ sung `AppError.Socket` vào `core:common` ở ngày này** (doc mục 14: payload `{event, message}`, prefix `UNAUTHORIZED:`/`FORBIDDEN:`/`CONFLICT:`/`GONE:`; `GONE` → về Home).
- **N19**: `feature:lobby` Player — lookup room, join REST → `socketToken` → connect; PlayerLobbyScreen.
- **N20**: HostLobbyScreen + `lobby:config-update` (ACK `{ok, changed, config, ignored}`); chia sẻ mã phòng/QR.
- 📚 Socket.IO Android nâng cao (`IO.Options.auth`, namespace, ACK `emit` + `Ack {}`, `EVENT_CONNECT`/`EVENT_DISCONNECT`), `callbackFlow` + `awaitClose`; làm quen MVI (Intent sealed → 1 StateFlow).

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