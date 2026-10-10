# MyQuizApp - Project Structure Documentation

> **Tài liệu cấu trúc dự án chi tiết**  
> Mô tả vai trò, trách nhiệm và mối quan hệ giữa các module trong kiến trúc Multi-module Gradle  
> **Version:** 2.23 | **Last Updated:** N46 WIP đến Search/Discover (source main `d5571d6`; N44 vẫn PARTIAL)

---

## 📋 Mục lục

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Core Modules](#2-core-modules)
   - 2.1 [`:app` - Application Entry Point](#21-app---application-entry-point)
   - 2.2 [`:core:network` - Network Infrastructure](#22-corenetwork---network-infrastructure)
   - 2.3 [`:core:database` - Room Persistence](#23-coredatabase---room-persistence)
   - 2.4 [`:core:datastore` - Preferences Storage](#24-coredatastore---preferences-storage)
   - 2.5 [`:core:ui` - Design System](#25-coreui---design-system)
   - 2.6 [`:core:common` - Shared Foundations](#26-corecommon---shared-foundations)
3. [Feature Modules](#3-feature-modules)
4. [Dependency Rules](#4-dependency-rules)
5. [Repository Pattern](#5-repository-pattern)
6. [Clean Architecture Layers](#6-clean-architecture-layers)
7. [Best Practices](#7-best-practices)

---

## 1. Tổng quan dự án

MyQuizApp được xây dựng theo **Multi-module Gradle Architecture** với **Clean Architecture + MVI pattern**. Dự án chia thành **14 modules** chính:

### Core Modules (6 modules)
- `:app` - Entry point, navigation host, DI assembly
- `:core:network` - Retrofit, OkHttp, Socket.IO client, cookie auth
- `:core:database` - Room database, DAOs, entities
- `:core:datastore` - DataStore Preferences
- `:core:ui` - Material 3 theme, shared composables; N40 thêm `QuestionImage` dùng chung trên `RemoteImage`
- `:core:common` - Domain models, Result wrapper, Repository interfaces

### Feature Modules (8 modules)
- `:feature:auth` - Login, Register, Google One Tap
- `:feature:home` - Quiz discovery, browse public quizzes
- `:feature:lobby` - Waiting room (Host & Player)
- `:feature:game-player` - **Gameplay Player host-paced + self-paced qua N22–N28, ảnh câu hỏi qua N40**: Classic, Solo manual-next, Survival lives/elimination, Marathon timer tổng/timeout/finish; typed ACK/reconnect, Final Result và hardening N25
- `:feature:game-host` - Host control console — **Classic/host-paced và dashboard self-paced đã hoàn thành phía Android**. Có `HostGameScreen` + `UiState`/`Intent`/`Effect`/`ViewModel`, điều khiển socket host room, merge progress riêng từng Player và render ảnh câu hỏi host-paced qua N40
- `:feature:leaderboard` - **Final Result + Game History Detail hoàn chỉnh qua N39**: socket-first/REST recovery cho kết quả; chi tiết lịch sử và answer sheet theo quyền host/player/guest; visibility defense
- `:feature:quiz-manage` - CRUD quizzes (Host only); N40 bổ sung ảnh trong card preview câu hỏi Quiz Detail; N41 thêm callback mở Tự chơi thử
- `:feature:quiz-preview` - **N41 Solo Preview local**: đọc quiz/answer key, countdown/timer/4 types/feedback/tổng kết/chơi lại; không room/socket/history

### Nguyên tắc thiết kế cốt lõi
- **Unidirectional Data Flow**: Intent → ViewModel → State → UI
- **Single Source of Truth**: State tập trung tại ViewModel
- **Separation of Concerns**: UI không biết business logic
- **Dependency Inversion**: Modules phụ thuộc interface (abstraction), không phụ thuộc implementation cụ thể
- **Server-Authoritative**: Mọi logic trận multiplayer (scoring, timing, validation) ở server, client chỉ render + send intent. Ngoại lệ tách biệt N41: Preview local dùng điểm ước tính, không tạo trận hoặc lưu kết quả server.
- **Role-Aware Architecture**: Tách rõ Host vs Player (khác payload, khác state machine)

---

## 2. Core Modules

### 2.1 `:app` - Application Entry Point

**📦 Module Type:** `com.android.application`

#### Mục đích
Module duy nhất có `applicationId`, đóng vai trò entry point của ứng dụng. Tập hợp toàn bộ modules con và khởi tạo Dagger/Hilt dependency graph.

#### Trách nhiệm
- ✅ Khởi tạo `Application` class với `@HiltAndroidApp`
- ✅ Host `MainActivity` chứa `NavHost` gốc
- ✅ Ghép các `NavGraph` con từ feature modules
- ✅ Cấu hình build variants (debug/release)
- ⚠️ Chỉ chứa orchestration mỏng và UI của top-level route do `:app` sở hữu; N38 đặt `LoadGameHistoryUseCase` tại đây để ghép luồng Hoạt động cấp ứng dụng; N42 đặt use case profile tại `domain/profile` cho top-level route app sở hữu
- ❌ **KHÔNG** giữ business logic/data model có thể tái sử dụng hoặc use case cross-cutting mà nhiều feature cần dùng lại (xem mục 8 - đưa xuống `core:*` thay vì để trong `:app`)

#### Cấu trúc thư mục
```
app/
├── src/main/
│   ├── java/.../
│   │   ├── QuizApp.kt              # Application class - @HiltAndroidApp
│   │   ├── MainActivity.kt         # N46.0 - root MyQuizAppTheme bọc AppNavGraph
│   │   ├── presentation/splash/
│   │   │   ├── SplashScreen.kt     # N46.2 - giữ UI gốc, thêm tên app Inter Bold dưới logo
│   │   │   └── SplashViewModel.kt  # Dùng CheckAuthStateUseCase từ core:datastore
│   │   ├── domain/activity/
│   │   │   └── LoadGameHistoryUseCase.kt # 🆕 N38 - orchestration mỏng cho lịch sử theo vai trò
│   │   ├── domain/profile/         # N42 - orchestration cho Profile do app sở hữu
│   │   │   ├── ProfileDraft.kt
│   │   │   ├── SaveProfileUseCase.kt
│   │   │   └── UpdateAvatarUseCase.kt
│   │   ├── presentation/profile/   # N42 - sửa hồ sơ, avatar preview/lưu riêng, snackbar
│   │   │   ├── ProfileScreen.kt
│   │   │   ├── ProfileViewModel.kt
│   │   │   ├── ProfileUiState.kt    # Bao gồm typed ProfileIntent
│   │   │   ├── ProfileEffect.kt
│   │   │   └── AvatarImagePreparer.kt # Android/IO/EXIF adapter, không dùng quiz compressor
│   │   ├── presentation/activity/  # N38 - lịch sử server-backed cho user/guest
│   │   │   ├── ActivityContract.kt
│   │   │   ├── ActivityViewModel.kt
│   │   │   └── ActivityScreen.kt
│   │   └── navigation/
│   │       ├── AppNavGraph.kt      # 🆕 N18.5 - Root NavHost orchestrator; N19.5: Scaffold + bottom bar bọc NGOÀI NavHost
│   │       ├── MainScaffold.kt     # N19.6: 4 tab; N46.6: MainBottomBarContent stateless, outlined/chấm tím + full Home Preview
│   │       ├── CurrentUserViewModel.kt # 🆕 N19.5 - avatar cho tab Hồ sơ; scope Activity vì bar nằm ngoài NavHost
│   │       ├── AuthNavGraph.kt     # 🆕 N18.5 - Auth routes: Login/Register/ForgotPassword/OtpVerification/ResetPassword
│   │       ├── MainNavGraph.kt     # Main routes; N39 thêm GameHistoryDetail từ card Hoạt động
│   │       ├── QuizManageNavGraph.kt  # 🆕 N18.5 - Quiz management routes: MyQuizzes/CreateQuiz/EditQuiz/QuizDetail/CreateRoom
│   │       └── GameNavGraph.kt     # 🆕 N18.5 - Game routes: PlayerLobby/HostLobby/GamePlay/HostGame/FinalResult
│   ├── AndroidManifest.xml
│   └── res/
└── build.gradle.kts                # Phụ thuộc TẤT CẢ modules
```

#### Phụ thuộc
```kotlin
dependencies {
    // Core modules
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    
    // Feature modules
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:lobby"))
    implementation(project(":feature:game-player"))
    implementation(project(":feature:game-host"))
    implementation(project(":feature:leaderboard"))
    implementation(project(":feature:quiz-manage"))
    implementation(project(":feature:quiz-preview"))
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
}
```

#### Lưu ý quan trọng
⚠️ **Chỉ `:app` mới được phụ thuộc tất cả modules khác**. Các module con **KHÔNG BAO GIỜ** biết `:app` tồn tại.

#### N42 — ranh giới Profile và data layer
- `core:common/model/UserProfilePatch.kt`: PATCH delta và `SessionUserToken` (userId/generation, không credential); `repository/UserRepository.kt` và guarded API của `SessionRepository`.
- `core:network/api/UserMutationApiService.kt`, `dto/UserProfileDtos.kt`, `repository/UserRepositoryImpl.kt`: profile/avatar mutation; GET đối soát dùng `UserApiService` hiện có. DI binding trong `NetworkBindingModule`, PreserveCase mutation Retrofit + retry disabled trong `NetworkModule`.
- `SessionRepositoryImpl`: generation/revision guard chống response refresh/mutation cũ ghi đè phiên mới/logout; avatar merge vào current user. `UserDto` bảo toàn role/authProvider.
- `app/domain/profile`: trim/validate/delta, publish SSOT và avatar upload/reconciliation; `app/presentation/profile`: modal edit/draft/discard, Photo Picker + preview thuần và adapter JPEG/EXIF/2 MiB chạy IO. Không crop tương tác, không thêm feature module/dependency feature → feature; pipeline ảnh quiz giữ nguyên.
- Lỗi avatar PATCH không chắc chắn chỉ GET xác minh, không replay cùng URL do backend chưa idempotent. Profile và bottom nav cùng đọc session SSOT, snackbar mapping tập trung.
- Test mới: app `ProfileUseCasesTest` (8), `ProfileViewModelTest` (6); core/network `UserProfileApiTest` (3), `SessionRepositoryTest` (6). User xác nhận kiểm thử nhanh pass; agent không tự chạy Gradle qua MCP.


🔑 **`:app` không phụ thuộc domain của 1 feature cụ thể**: Trước đây `SplashViewModel` (trong `:app`) từng cần logic của `feature:auth` để kiểm tra trạng thái đăng nhập. Vì đây là logic cross-cutting (không chỉ Splash cần), `CheckAuthStateUseCase` được đưa xuống `core:datastore` thay vì đặt trong `:app` hay giữ phụ thuộc vào `feature:auth`. Xem mục 2.4 và mục 8.

---

#### N43 — ranh giới Account Security (không thêm module)
- `app/domain/security`: `ObserveSecurityAccountUseCase`, `ChangePasswordUseCase`, `DeactivateAccountUseCase`, `SecurityValidation` và `SecurityMutationResult`. Quan sát/guard session, validation và orchestration qua Repository interface, không data implementation.
- `app/presentation/profile`: `AccountSecurityViewModel` chỉ inject 3 Use case; `AccountSecurityUiState`, `AccountSecurityIntent`, `AccountSecurityEffect`, `AccountSecurityScreen` tách riêng. Screen/Content stateful/stateless, preview Light/Dark + Google-only; password chỉ RAM.
- `app/navigation`: `Route.AccountSecurity`, callback từ Profile và reset MainGraph/Home + dọn saved tab stacks khi session kết thúc. Không đặt mutation/cleanup nghiệp vụ trong NavGraph.
- `core:common`: mở rộng `UserRepository` cho password/deactivation và `SessionRepository.clearSession(token)` cho cleanup guarded.
- `core:network`: mở rộng `UserMutationApiService`/`UserProfileDtos`/`UserRepositoryImpl` (PreserveCase; DELETE có JSON body; mutation retry disabled); `SessionRepositoryImpl` dọn cookie và Guest dưới generation guard; `TokenAuthenticator` phân terminal/transient; `di/SafeHttpLogger` không ghi body/header/query/credential.
- 25 regression test bổ sung N43 đã đối chiếu XML local PASS; user kiểm thử nhanh ổn. Không suy ra full build/lint/CI từ unit XML.

#### N44 — session isolation và navigation cleanup (không thêm module)
- `core:common/model/SessionSnapshot.kt`: `SessionSnapshot`/`SessionIdentity`; `SessionRepository.snapshot()` đọc state + lifetime generation, implementation production đọc atomically dưới state lock ở core:network.
- `app/domain/activity/ObserveActivitySessionUseCase`, `app/domain/profile/ObserveProfileSessionUseCase` và `feature:leaderboard` domain `ObserveHistorySessionUseCase`: quan sát/current snapshot/guard theo phạm vi; ViewModel không inject/gọi Repository trực tiếp.
- Activity/History Detail: job cancellation, identity/epoch và request-id guards; reset ngay khi đổi lifetime, không để in-flight request cũ chặn phiên mới. Profile giữ mutation use cases hiện hữu.
- `app/navigation/SessionNavigation.kt`: helper reset MainGraph + clear saved tab stacks, dùng cho logout thường và Account Security session-end; chỉ navigation, không business cleanup/API.
- 16 regression bổ sung có XML local PASS. Hai race tests opt-in trong core:network đã tái hiện FAIL trên transport chưa sửa; không thêm production credential epoch/adapter trong scope đã hoàn thành.
- N44Session logger/network interceptor/request marker và test riêng logger đã gỡ khỏi source; không liệt kê chúng trong cấu trúc production. Google/session + transport fix tạm hoãn theo user; chặng 4 E2E/matrix còn mở.
- Tài liệu và AGENTS thuộc nhánh docs, main chỉ source/config/tests. Không thêm Gradle module; vẫn 14 module.

### 2.2 `:core:network` - Network Infrastructure

**📦 Module Type:** `com.android.library`

#### Mục đích
Cung cấp hạ tầng networking dùng chung cho toàn app: Retrofit với Cookie-based auth, Socket.IO client, và **implementation** của các Repository dùng REST API.

#### Trách nhiệm
- ✅ Configure `OkHttpClient` với `CookieJar` + `Authenticator` (401 auto-refresh)
- ✅ N29: HTTP client public preserve-case riêng cho `GET /games/{id}/review`, không cookie/Authenticator; redact `x-socket-token`, Cookie, Set-Cookie và Authorization khỏi log
- ✅ Configure `Retrofit` với base URL theo build variant
- ✅ Định nghĩa các `ApiService` interfaces (Auth, User, Quiz, Game, Storage)
- ✅ **Implement** `AuthRepository`, `QuizRepository`, `GameSessionRepository` (interface ở `core:common`)
- ✅ Cung cấp `GameSocketClient` tạo Socket.IO client namespace `/game` với socket token (N18 — doc cũ dự kiến tên `SocketFactory`); N25 thêm `ConnectivityManager`, gate `INTERNET + VALIDATED`, tự nối lại khi mạng false→true, terminal `CLIENT_RECONNECT_EXHAUSTED` và log cause chain đã loại bí mật
- ✅ Parse Socket events thành domain models qua `GameEventMapper` (N22: `question:started` + player snapshot; N24: `answer:received`, player `leaderboard:updated` và full final rows; payload rác → `GameEvent.Failed(event, "CLIENT_PARSE_ERROR")`, không bao giờ throw)
- ✅ `SocketAckMapper` parse riêng ACK command: host config và player answer; ACK error thật là `{error:{code}}`, timeout/not-connected dùng client code riêng
- ✅ **Implement** `HostGameSocketRepository`, `PlayerGameSocketRepository` (interface ở `core:common`, N18)
- ❌ **KHÔNG** import `core:database` (dependency inversion qua `CookieStore` interface)

#### Cấu trúc thư mục
```
core/network/
├── src/main/java/.../network/
│   ├── di/
│   │   ├── NetworkModule.kt              # Provide OkHttpClient, Retrofit, Json (+ @PreserveCaseJson)
│   │   ├── NetworkBindingModule.kt       # @Binds AuthRepositoryImpl → AuthRepository
│   │   └── SocketBindingModule.kt        # 🆕 N18: @Binds 2 socket repository impl
│   ├── cookie/
│   │   ├── PersistentCookieJar.kt        # implements CookieJar, dùng CookieStore interface
│   │   └── TokenAuthenticator.kt         # 401 → /auth/refresh → retry
│   ├── api/                              # Retrofit ApiService interfaces
│   │   ├── AuthApiService.kt             # POST /auth/login, /auth/register, etc.
│   │   ├── UserApiService.kt             # GET /users/me, PATCH /users/me
│   │   ├── QuizApiService.kt             # GET /quizzes/search, POST /quizzes
│   │   ├── GameApiService.kt             # Game REST; N38 history, N39 summary + my-answers
│   │   └── StorageApiService.kt          # GET /storage/presign
│   ├── repository/                       # 🟩 Implementation của interfaces từ core:common
│   │   ├── AuthRepositoryImpl.kt         # implements AuthRepository
│   │   ├── QuizRepositoryImpl.kt         # implements QuizRepository
│   │   ├── GameSessionRepositoryImpl.kt  # implements GameSessionRepository; N39 history detail/answers
│   │   └── InMemoryGameResultRepository.kt # 🆕 N24 - transient game:ended hand-off
│   ├── socket/                            # 🆕 N18 (30/8) — 6 file thật
│   │   ├── GameSocketClient.kt           # (socketToken) → callbackFlow<GameEvent> + awaitClose
│   │   ├── GameSocketEvents.kt           # internal object: hằng 19 server event + client event
│   │   ├── GameEventMapper.kt            # JSONObject → GameEvent sealed class (runCatching, không throw)
│   │   ├── HostGameSocketRepositoryImpl.kt    # 🟩 implements HostGameSocketRepository
│   │   ├── PlayerGameSocketRepositoryImpl.kt  # 🟩 implements PlayerGameSocketRepository
│   │   └── dto/
│   │       ├── SocketDtos.kt             # lobby/game-started + ACK DTO (config/player answer)
│   │       └── HostSocketDtos.kt         # gameplay DTO dùng chung host/player; N22 thêm player snapshot
│   └── dto/
│       ├── ApiEnvelope.kt                # {success, data, error, meta}
│       └── ApiError.kt                   # Error response model
└── build.gradle.kts
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))  // CookieStore interface, Repository interfaces
    // KHÔNG được phụ thuộc :core:database
    
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.socket.io)
    implementation(libs.kotlinx.serialization.json)
}
```

#### Lưu ý quan trọng
🔑 **Dependency Inversion Pattern**: `PersistentCookieJar` chỉ biết `CookieStore` interface (từ `core:common`), không biết `RoomCookieStore` implementation (từ `core:database`). Hilt tự wire implementation đúng khi build `:app`.

🔒 **Cookie-based Auth**: Khác JWT Bearer token - cookie tự động gửi kèm request, HttpOnly secure, refresh token rotation.

⚡ **Socket Token Flow**: REST call trước (`POST /games/:code/join`) → lấy `socketToken` (JWT ngắn hạn) → connect Socket.IO với `auth: {token}`.

⚠️ **ACK answer là trạng thái có thể không chắc chắn**: timeout chỉ nói callback không về, không chứng minh server chưa ghi đáp án. Gameplay phải khóa input, gọi `player:sync`, rồi đọc `game:state.player.answered_questions`; tuyệt đối không tự retry.

---

### 2.3 `:core:database` - Room Persistence

**📦 Module Type:** `com.android.library`

#### Mục đích
Quản lý persistent storage với Room database. Lưu trữ cookies (cho auth), cached quizzes (offline), và game history.

#### Trách nhiệm
- ✅ Define Room `@Database` với version management
- ✅ Define `@Entity` classes (CookieEntity, CachedQuizEntity, GameHistoryEntity)
- ✅ Define `@Dao` interfaces với suspend functions
- ✅ **Implement** `CookieStore` interface (từ `core:common`) qua `RoomCookieStore`
- ✅ **Implement** `QuizCacheStore` interface (từ `core:common`) qua `RoomQuizCacheStore` — cache-aside fallback cho quiz detail khi mất mạng (N12, 21/8)
- ✅ Provide database instance qua Hilt
- ❌ **KHÔNG** expose entity classes ra ngoài module - chỉ expose DAOs

#### Cấu trúc thư mục
```
core/database/
├── src/main/java/.../database/
│   ├── AppDatabase.kt                    # @Database, singleton Room instance
│   ├── dao/
│   │   ├── CookieDao.kt                  # CRUD cookies
│   │   ├── QuizCacheDao.kt               # Cache quizzes for offline
│   │   └── GameHistoryDao.kt             # Save completed games
│   ├── entity/
│   │   ├── CookieEntity.kt               # @Entity: cookies table
│   │   ├── CachedQuizEntity.kt           # @Entity: quiz_cache table
│   │   └── GameHistoryEntity.kt          # @Entity: game_history table
│   ├── cookie/
│   │   └── RoomCookieStore.kt            # 🟩 implements CookieStore interface
│   ├── cache/
│   │   └── RoomQuizCacheStore.kt         # 🟩 implements QuizCacheStore interface (N12, 21/8)
│   └── di/
│       └── DatabaseBindingModule.kt      # @Binds RoomCookieStore → CookieStore, RoomQuizCacheStore → QuizCacheStore
└── build.gradle.kts
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))  // CookieStore interface, StoredCookie model
    
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
}
```

#### Lưu ý quan trọng
🔒 **CookieEntity Schema**:
```kotlin
@Entity(
    tableName = "cookie_store",
    primaryKeys = ["host", "name"]  // Composite key
)
data class CookieEntity(
    val host: String,
    val name: String,
    val value: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
)
```

🔄 **Mapper Pattern**: Entity ↔ Domain model mapping trong `RoomCookieStore`:
```kotlin
fun CookieEntity.toStoredCookie() = StoredCookie(...)
fun StoredCookie.toEntity(host: String) = CookieEntity(...)
```

---

### 2.4 `:core:datastore` - Preferences Storage

**📦 Module Type:** `com.android.library`

#### Mục đích
Lưu trữ preferences nhẹ (user settings, UI state) dùng DataStore Preferences API. **KHÔNG lưu auth tokens** (tokens ở cookies trong Room). Ngoài ra là nơi đặt các use case cross-cutting liên quan tới trạng thái auth/guest mà nhiều module cần dùng lại.

#### Trách nhiệm
- ✅ Define `PreferenceKeys` object với typed keys
- ✅ Provide `DataStore<Preferences>` instance qua Hilt
- ✅ Wrapper class `UserPreferences`/`SettingsDataStore` với Flow-based API
- ✅ Chứa use case cross-cutting dùng `AuthRepository` + `SettingsDataStore` (vd. `CheckAuthStateUseCase`)
- ❌ **KHÔNG** lưu sensitive data (tokens, passwords)

#### Cấu trúc thư mục
```
core/datastore/
├── src/main/java/.../datastore/
│   ├── PreferenceKeys.kt                 # Centralized preference keys
│   ├── UserPreferences.kt                # DataStore wrapper với helper methods
│   ├── GuestIdentityStore.kt             # 🆕 N19: UUID của khách (player_guest_id), sinh lần đầu cần join rồi giữ mãi
│   ├── usecase/
│   │   └── CheckAuthStateUseCase.kt      # 🟩 Cross-cutting: trả về AuthState (FIRST_LAUNCH/GUEST_MODE/AUTHENTICATED)
│   └── di/
│       └── DataStoreModule.kt            # Provide DataStore instance
└── build.gradle.kts
```

#### Preference Keys
```kotlin
object PreferenceKeys {
    val NICKNAME = stringPreferencesKey("nickname")
    val LAST_ROOM_CODE = stringPreferencesKey("last_room_code")
    val GUEST_ID = stringPreferencesKey("guest_id")            // UUID for guest players
    val THEME_MODE = stringPreferencesKey("theme_mode")        // "light" | "dark" | "system"
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))  // AuthRepository interface, Extensions
    
    implementation(libs.datastore.preferences)
}
```

#### Lưu ý quan trọng
⚠️ **Auth state KHÔNG lưu ở đây**. Auth state được xác định bằng cách gọi `GET /users/me` lúc app start - nếu cookie valid → authenticated, nếu 401 → unauthenticated.

🔑 **`CheckAuthStateUseCase` đặt ở đây, KHÔNG ở `:app` hay `:feature:auth`**: Đây là logic cross-cutting - không chỉ Splash cần biết trạng thái auth, mà bất kỳ feature nào cũng có thể cần (vd. hiện dialog yêu cầu đăng nhập khi guest bấm vào tính năng cần tài khoản). Nó chỉ phụ thuộc `AuthRepository` (từ `core:common`) và `SettingsDataStore` (module này) nên không cần đặt trong 1 feature cụ thể hay trong `:app`. Đây là ví dụ điển hình của quy tắc: *nếu ≥2 nơi cần dùng chung 1 logic → đưa xuống `core:*` tương ứng, tạo use case trung gian thay vì để `:app` phụ thuộc domain của 1 feature*.

---

### 2.5 `:core:ui` - Design System

**📦 Module Type:** `com.android.library`

#### Mục đích
Design system dùng chung cho toàn app. Centralize Material 3 theme, typography, colors, và shared composable components.

#### Trách nhiệm
- ✅ Define Material 3 theme (Color scheme, Typography, Shapes)
- ✅ Shared reusable composables (buttons, text fields, cards, etc.)
- ✅ Component thật: buttons/code input/answer row/quiz summary/leaderboard/avatar/remote image/inline link; không liệt kê tên dự kiến như file đã tồn tại
- ℹ️ `UiText` bên dưới là pattern minh họa, không phải file đã tồn tại trong tree main được đối chiếu ở N46
- ❌ **KHÔNG** chứa business logic hay data models
- ❌ **KHÔNG** phụ thuộc `:core:network` hay `:core:database`

#### Cấu trúc thư mục
```
core/ui/
├── src/main/java/.../core/ui/
│   ├── theme/
│   │   ├── Color.kt                    # Brand + FrontendColors light palette/gradient
│   │   ├── ComponentColors.kt          # N46 - internal light/dark resolver
│   │   ├── Type.kt                     # N46 - InterFontFamily, đủ 15 typography role
│   │   └── Theme.kt                    # MyQuizAppTheme
│   ├── components/
│   │   ├── QuizButtons.kt              # Primary/Secondary/Danger; leading icon slot
│   │   ├── CustomClickableText.kt      # Chỉ action span là link; stateless
│   │   ├── SixCharacterCode.kt         # OTP/Room normalize thuần
│   │   ├── SixCharacterCodeField.kt    # Wrapper focus + stateless Content
│   │   ├── AnswerOptionItem.kt         # Selection khác correctness; single-choice
│   │   ├── QuizListCard.kt             # Card ngang Search/Library; đổi tên từ QuizSummaryCard ở N46.6
│   │   ├── DiscoveryQuizCard.kt        # Card dọc Home/Discover; uniformGrid cho lưới đều
│   │   ├── RoomCodeEntryCard.kt        # Card room 6 ô, stateless primitive props
│   │   ├── LeaderboardPlayerCard.kt    # Top 3 medals; caller cung cấp rank/score
│   │   ├── ComponentGalleryPreview.kt  # Tổng hợp Light/Dark/narrow-large-text
│   │   ├── HomeSectionRow.kt
│   │   ├── AuthRequiredDialog.kt
│   │   ├── Avatar.kt
│   │   ├── RemoteImage.kt              # Preview inspection placeholder; runtime Coil
│   │   ├── QuestionImage.kt            # N40 - 16:9, URL rỗng không chiếm chỗ
│   │   └── SettingSwitchRow.kt
│   ├── gameconfig/                     # Editor cấu hình dùng chung từ N20
│   └── style/                          # AppTextStyles hiện hữu
├── src/main/res/font/
│   ├── inter_regular.ttf              # 400
│   ├── inter_medium.ttf               # 500
│   ├── inter_semibold.ttf             # 600
│   └── inter_bold.ttf                 # 700
├── src/test/java/.../components/
│   └── SixCharacterCodeTest.kt         # 10 case đã viết; chưa xác minh run mới
├── licenses/Inter-LICENSE.txt
└── build.gradle.kts
```

Tree phần N46 trên đối chiếu source main tại `d5571d6`, không lấy source vắng khi checkout docs làm bằng chứng file chưa có. QuizSummaryCard đã đổi tên thành QuizListCard (`9417aad`); QuizCardItem đã xóa khỏi source (`d5571d6`). ComponentColors là internal chỉ dùng trong core:ui; feature dùng FrontendColors public + MaterialTheme dark fallback. Preview không thay thế test/build evidence; chi tiết checkpoint ở `N46_UI_PROGRESS.md`.

#### UiText Pattern (minh họa, không phải inventory source)
```kotlin
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(@StringRes val resId: Int) : UiText()
    data class StringResourceWithArgs(@StringRes val resId: Int, val args: List<Any>) : UiText()
}

@Composable
fun UiText.asString(): String = when (this) {
    is DynamicString -> value
    is StringResource -> stringResource(resId)
    is StringResourceWithArgs -> stringResource(resId, *args.toTypedArray())
}
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))  // Extensions only
    
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
}
```

#### Lưu ý quan trọng
🎨 **Theme Values**: Colors được định nghĩa từ logo MyQuizApp:
- Primary: `#7B61FF` (tím brand)
- Material Secondary hiện hữu: `#625B71`; không nhầm với `FrontendColors.Danger = #EF4444`.
- Palette Figma light đầy đủ và dark fallback ghi ở `N46_UI_PROGRESS.md`; chưa chốt dark design toàn app.
- Root `MainActivity` áp `MyQuizAppTheme`; typography dùng Inter local, không download font lúc chạy.

📱 **Stateful vs Stateless**: Components trong `:core:ui` nên **stateless** - nhận data qua parameters, emit events qua callbacks.

---

### 2.6 `:core:common` - Shared Foundations

**📦 Module Type:** `com.android.library`

#### Mục đích
Module nền tảng chứa code dùng chung bởi **TẤT CẢ modules khác**: domain models, Result wrapper, error handling, Repository interfaces, extensions.

#### Trách nhiệm
- ✅ Define domain models thuần Kotlin (User, Quiz, GameSession, Player, etc.)
- ✅ Define `Result<T>` sealed class cho error handling
- ✅ Define `AppError` sealed class — **không có nhánh `Socket`** (lỗi socket cũng chỉ mang code nên dùng chung `Api(code)`, xem 5.4); N37 bổ sung helper phân loại authentication/terminal session/missing resource/transient để mọi tầng dùng nhất quán
- ✅ Define **TẤT CẢ Repository interfaces** dùng bởi ≥2 features
- ✅ Define `CookieStore` interface (DIP cho cookie persistence)
- ✅ Extension functions (Flow, Instant, String extensions)
- ❌ **KHÔNG** phụ thuộc bất kỳ module nào khác trong project
- ❌ **KHÔNG** chứa implementation - chỉ interfaces và models

#### Cấu trúc thư mục
```
core/common/
├── src/main/java/.../common/
│   ├── result/
│   │   ├── Result.kt                     # sealed class Result<T>
│   │   └── AppError.kt                   # sealed class AppError
│   ├── cookie/
│   │   ├── CookieStore.kt                # 🟦 interface - implemented by core:database
│   │   └── StoredCookie.kt               # data class thuần
│   ├── cache/
│   │   └── QuizCacheStore.kt             # 🟦 interface - implemented by core:database (RoomQuizCacheStore, N12 21/8)
│   ├── validator/                        # 🆕 N18.5 (2/9) - Centralized validation (Pattern C)
│   │   ├── ValidationResult.kt           # sealed interface Success/Error + extension helpers
│   │   ├── EmailValidator.kt             # validate(email) using Android Patterns.EMAIL_ADDRESS
│   │   ├── PasswordValidator.kt          # validate(password, minLength), validateForRegister(), validateConfirm()
│   │   ├── QuizNameValidator.kt          # validate(name) - min 3, max 100 chars
│   │   ├── QuizDescriptionValidator.kt   # validate(description) - max 500 chars, optional
│   │   └── RoomSettingsValidator.kt      # validateRoomName/MaxPlayers/TimePerQuestion
│   ├── model/                            # Domain models thuần Kotlin
│   │   ├── User.kt
│   │   ├── Quiz.kt
│   │   ├── Question.kt                   # KHÔNG có correct_answer field (anti-cheat)
│   │   ├── GameSession.kt
│   │   ├── GameConfig.kt
│   │   ├── GameModeSpec.kt
│   │   ├── Player.kt
│   │   ├── PlayerScore.kt
│   │   ├── GameResults.kt
│   │   ├── GameHistoryDetail.kt          # N39 - summary snapshot + viewer/visibility state
│   │   ├── AuthState.kt                  # enum: FIRST_LAUNCH/GUEST_MODE/AUTHENTICATED
│   │   └── GameEvent.kt                  # sealed class: all socket events
│   ├── repository/                       # 🟦 Repository interfaces dùng chung
│   │   ├── AuthRepository.kt             # Login, register, logout
│   │   ├── QuizRepository.kt             # CRUD quizzes, search
│   │   ├── GameSessionRepository.kt      # Create/join game, host token, leaderboard
│   │   ├── GameSocketRepository.kt       # 🆕 N18 base: events(socketToken)/joinLobby/disconnect
│   │   ├── HostGameSocketRepository.kt   # 🆕 N18: startGame/nextQuestion/pause/resume/end
│   │   ├── PlayerGameSocketRepository.kt # 🆕 N18: leaveLobby/submitAnswer/requestNext/sync
│   │   └── GameResultRepository.kt       # 🆕 N24: transient final result boundary
│   └── ext/                              # Extensions
│       ├── FlowExt.kt
│       ├── InstantExt.kt
│       └── StringExt.kt
└── build.gradle.kts
```

#### Result Pattern
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T, val page: PageInfo? = null) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
}

sealed class AppError { /* các nhánh thật: Network, Unauthorized, Forbidden, NotFound,
   Gone, Server(httpCode), Api(code), Unknown(cause) — KHÔNG có nhánh Socket, xem 5.4 */ }
```

#### Phụ thuộc
```kotlin
dependencies {
    // KHÔNG phụ thuộc bất kỳ module nào khác
    // Chỉ dependencies bên ngoài (Kotlin stdlib, kotlinx.serialization)
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}
```

#### Lưu ý quan trọng
🔑 **Repository Interface Placement Rules**:
1. Interface dùng bởi **≥2 features** → đặt ở `core:common/repository`
2. Interface dùng bởi **chỉ 1 feature** → đặt trong `feature:<đó>/domain/repository`

📦 **Quy tắc bảng Repository** (xem design doc mục 2.4):
- `CookieStore`: interface ở `core:common`, impl ở `core:database`
- `QuizCacheStore`: interface ở `core:common`, impl ở `core:database` (`RoomQuizCacheStore`, N12 21/8) — cùng pattern DIP với `CookieStore`; sửa từ vi phạm ban đầu (`core:network` từng import trực tiếp `core:database` để cache quiz detail)
- `AuthRepository`, `QuizRepository`, `GameSessionRepository`: interface ở `core:common`, impl ở `core:network`
- `GameSocketRepository` (base) + `HostGameSocketRepository` + `PlayerGameSocketRepository`: interface ở `core:common`, impl ở `core:network/socket` — ⚠️ **đổi so với v2.2** (trước dự kiến đặt cả interface lẫn impl trong feature module); xem 5.3

🚫 **Anti-cheat**: Domain model `Question` **KHÔNG bao giờ** chứa field `correct_answer` - server không bao giờ gửi đáp án đúng xuống client trong gameplay.

---

## 3. Feature Modules

Feature modules chứa UI (Compose screens), presentation logic (ViewModels với MVI), domain logic (UseCases), và đôi khi data layer (Repository implementations) nếu chỉ feature đó dùng.

### Nguyên tắc Feature Module
- ✅ Mỗi feature có thể có tối đa 3 tầng: `presentation/`, `domain/`, `data/`
- ✅ UseCases inject Repository interfaces (từ `core:common` hoặc `domain/repository` cục bộ)
- ✅ ViewModels theo MVI pattern: `Intent → State → UI`
- ✅ Package Kotlin PHẢI khớp namespace Gradle: `feature:<tên>` → package `android.kma.myquizzapp.feature.<tên>.*` (không rút gọn bỏ `feature.`)
- ❌ **KHÔNG** phụ thuộc feature khác - nếu cần chia sẻ logic → đưa xuống `core:*`

---

### 3.1 `:feature:auth` - Authentication

**📦 Module Type:** `com.android.library`

#### Mục đích
Xử lý authentication flow: Login, Register, Google One Tap, Forgot Password, OTP verification, Reset Password.

#### Trách nhiệm
- ✅ LoginScreen + LoginViewModel (email/password, Play as Guest)
- ✅ RegisterScreen + RegisterViewModel
- 🚧 N46.3/N46.4 đã polish Login (`0ba1244`) và Register (`ee8701c`) theo ảnh local; Screen/Content stateless, ScrollState/visibility hoist lên wrapper, shared buttons/inline link và 5 Preview mỗi màn. Register phone dùng icon ống nghe. Không đổi validator/Use case/Intent/Effect/API; chưa xác minh regression/build mới, xem `N46_UI_PROGRESS.md`.
- ✅ Google One Tap integration (Credential Manager API)
- ✅ ForgotPasswordScreen → OtpVerificationScreen → ResetPasswordScreen: luồng reset ticket thật từ N16.5, hỗ trợ deep-link token verify → ticket; N46.5 polish 3 màn (`3d92af1`), không viết thêm endpoint. OTP 6 ô + countdown/resend inline gộp căn giữa dưới nút; giữ top app bar, không icon lớn/checklist.
- ✅ `AuthValidator` thuần Kotlin validate email/password/fullname/phone; N46.5 thêm resetPasswordError (rule register min 8) và confirmPasswordError (rỗng/khớp chính xác, không trim). ViewModel gọi validator + Use case và claim loading đồng bộ; `a1327bb`. 8 PasswordResetValidationTest case đã viết, chưa xác minh run mới.
- ✅ Deep linking cho reset password token

#### Cấu trúc thư mục
```
feature/auth/
├── src/main/java/.../feature/auth/
│   ├── presentation/
│   │   ├── PasswordResetFormField.kt  # N46.5 - field stateless dùng chung Forgot/Reset
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │   │   ├── LoginViewModel.kt
│   │   │   ├── LoginUiState.kt
│   │   │   ├── LoginIntent.kt
│   │   │   └── LoginEffect.kt            # UiState/Intent/Effect tách file riêng (chốt 22/8, khớp home/search/quiz-manage)
│   │   ├── register/
│   │   │   ├── RegisterScreen.kt
│   │   │   ├── RegisterViewModel.kt
│   │   │   ├── RegisterUiState.kt
│   │   │   ├── RegisterIntent.kt
│   │   │   └── RegisterEffect.kt
│   │   ├── forgot/
│   │   │   ├── ForgotPasswordScreen.kt
│   │   │   ├── ForgotPasswordViewModel.kt
│   │   │   ├── ForgotPasswordUiState.kt
│   │   │   ├── ForgotPasswordIntent.kt
│   │   │   └── ForgotPasswordEffect.kt
│   │   ├── otp/
│   │   │   ├── OtpVerificationScreen.kt
│   │   │   ├── OtpVerificationViewModel.kt
│   │   │   ├── OtpVerificationUiState.kt
│   │   │   ├── OtpVerificationIntent.kt
│   │   │   └── OtpVerificationEffect.kt
│   │   ├── reset/
│   │   │   ├── ResetPasswordScreen.kt
│   │   │   ├── ResetPasswordViewModel.kt
│   │   │   ├── ResetPasswordUiState.kt
│   │   │   ├── ResetPasswordIntent.kt
│   │   │   └── ResetPasswordEffect.kt
│   │   └── validation/
│   │       └── AuthValidator.kt          # object thuần Kotlin, dùng kotlin.Regex (chạy được trong local unit test)
│   └── domain/
│       └── usecase/
│           ├── AuthUseCases.kt           # LoginUseCase, RegisterUseCase, GetCurrentUserUseCase, LoginWithGoogleUseCase, LogoutUseCase
│           ├── EnableGuestModeUseCase.kt # Persist guest mode khi bấm "Play as Guest"
│           ├── ForgotPasswordUseCase.kt       # Request reset OTP/email
│           ├── VerifyResetOtpUseCase.kt       # Email + OTP → ticket
│           ├── VerifyResetTokenUseCase.kt     # Deep-link token → ticket
│           ├── GetResetTicketUseCase.kt       # Peek ticket metadata
│           └── CompleteResetUseCase.kt        # Ticket + password → complete reset
└── build.gradle.kts
```

⚠️ **Lưu ý namespace**: Toàn bộ package Kotlin trong module này dùng `android.kma.myquizzapp.feature.auth.*` (không phải `android.kma.myquizzapp.auth.*`), khớp với `namespace` khai báo trong `build.gradle.kts` - đồng nhất với `feature:home` và convention `feature.<tên>` chung của dự án.

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // AuthRepository interface
    implementation(project(":core:datastore")) // EnableGuestModeUseCase dùng SettingsDataStore
    implementation(project(":core:ui"))        // Theme, components
    
    implementation(libs.credentials)           // Credential Manager for Google One Tap
}
```

#### Lưu ý quan trọng
🔐 **Cookie Auth Flow**: Login success → server set HttpOnly cookies (`accessToken`, `refreshToken`) → không có token string trong response body.

✅ **Validation dùng chung**: `AuthValidator` được cả 5 ViewModel (Login/Register/Forgot/Otp/Reset) dùng chung - tránh lặp lại logic validate ở mỗi form. Password rule khác nhau giữa Login (chỉ check không rỗng, tương thích tài khoản cũ) và Register (tối thiểu 8 ký tự, khớp `registerSchema` backend).

📐 **Quy ước UiState/Intent/Effect (chốt 22/8)**: Cả 5 ViewModel auth ban đầu định nghĩa `UiState`/`Intent`/`Effect` **nested trong file ViewModel** — lệch với `feature:home`, `feature:home/search`, `feature:quiz-manage` (đã dùng file top-level riêng từ đầu). Sau khi phát hiện, đã refactor `feature:auth` sang tách file riêng cho đồng bộ. **Chuẩn dùng cho mọi feature mới**: `<Feature>UiState.kt`, `<Feature>Intent.kt`, `<Feature>Effect.kt` — không nested trong ViewModel.

---

### 3.2 `:feature:home` - Quiz Discovery

**📦 Module Type:** `com.android.library`

#### Mục đích
Browse public quizzes, search, view "My Quizzes" (nếu logged in), entry point để tạo game room.

#### Trách nhiệm
- ✅ HomeScreen — feed trong một LazyColumn; không còn tab Của tôi (Library là route riêng). N46.6 header brand/pill auth, room 6 ô qua slot + DiscoveryQuizCard (`88a1307`). Không fake continue progress/resume.
- ✅ SearchScreen **riêng** — auto-focus, manual submit/IME và cursor append. N46.7 dùng QuizListCard ngang (`496dc36`), có append error/retry đúng cursor giữ results; SearchUiStateTest tổng 9 case, chưa xác minh run mới.
- ✅ N20.5: `presentation/discover` có Screen/Content và UiState/Intent/Effect riêng; domain có resolver/PagingSource/Observe use case, dùng public-only client. N46.8 (`d5571d6`): grid 2 cột DiscoveryQuizCard uniform, category chips/sort; wrapper hoist grid/filter state và captured snapshot cho count/key/data, live access guard index/ID để giữ prefetch. Có source fix Paging index crash, chưa có xác nhận retest sau sửa; không thêm tab hay total giả.
- ✅ Search quizzes công khai (không cần login - `optionalAuthMiddleware`)
- ⚠️ Quiz detail (preview + entry chơi) đã **chuyển sang `:feature:quiz-manage/presentation/quizdetail`** (N12, 21/8) — xem mục 3.7, không còn ở `feature:home`
- ✅ Navigate to CreateRoomScreen (trong `:feature:quiz-manage`)

#### Cấu trúc thư mục
```
feature/home/
├── src/main/java/.../feature/home/
│   ├── domain/
│   │   └── discover/
│   │       ├── DiscoverSource.kt         # Filter/sort/query + route resolver
│   │       ├── DiscoverPagingSource.kt   # Cursor Paging; feed/search theo query
│   │       └── ObserveDiscoverQuizzesUseCase.kt
│   ├── presentation/
│   │   ├── HomeScreen.kt                 # N46.6: header + room slot + một LazyColumn feed
│   │   ├── HomeViewModel.kt
│   │   ├── discover/
│   │   │   ├── DiscoverScreen.kt
│   │   │   ├── DiscoverViewModel.kt
│   │   │   ├── DiscoverUiState.kt
│   │   │   ├── DiscoverIntent.kt
│   │   │   └── DiscoverEffect.kt
│   │   ├── HomeUiState.kt
│   │   ├── HomeIntent.kt
│   │   └── search/
│   │       ├── SearchScreen.kt           # Màn search riêng (Option B, 17/8)
│   │       ├── SearchViewModel.kt
│   │       ├── SearchUiState.kt
│   │       └── SearchIntent.kt
│   └── domain/
│       └── usecase/
│           ├── GetHomeContentUseCase.kt  # inject QuizRepository
│           └── SearchQuizzesUseCase.kt
└── build.gradle.kts
```

> ℹ️ `HomeSectionRow.kt` ở `core:ui/components/`, nay render DiscoveryQuizCard. QuizListCard là card ngang Search/Library, DiscoveryQuizCard là card dọc Home/Discover. QuizCardItem đã xóa ở N46.8; không còn là API để dùng lại.

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // QuizRepository interface, Quiz model
    implementation(project(":core:ui"))
    
    implementation(libs.paging.compose)        // Paging 3
    implementation(libs.coil.compose)          // Image loading
}
```

#### Lưu ý quan trọng
🔍 **Optional Auth**: Endpoint `/quizzes/search` không yêu cầu login - guest users có thể browse quizzes.

📄 **Pagination**: Discover dùng Paging 3 với cursor/limit thật; Search giữ UiState + nextCursor/hasMore và submit thủ công. Không suy ra cursor/hasMore từ itemCount hoặc gọi page số thay cursor. Khi tách stateless Content, count/key/data phải cùng snapshot; xem bài học N46 trong AGENTS.md.

---

### 3.3 `:feature:lobby` - Waiting Room

**📦 Module Type:** `com.android.library`

#### Mục đích
Phòng chờ trước khi game bắt đầu. Có 2 perspectives: **Host** (điều khiển config, start game) và **Player** (xem danh sách người chơi, chờ host start).

#### Trách nhiệm
- ✅ HostLobbyScreen + HostLobbyViewModel (N18: danh sách người chơi realtime + reconnect; update config để N20)
- ✅ PlayerLobbyScreen + PlayerLobbyViewModel (N19, 5/9 — join REST → `socketToken` → socket realtime, đã test trên máy thật)
- ✅ `JoinRoomCard` (ô nhập mã phòng, N19.6 nhúng vào Home qua slot `roomCodeCard`; trước đó là `JoinRoomScreen` riêng ở N19) + GuestNicknameScreen (chỉ khách thấy; người đã đăng nhập vào thẳng lobby)
- ✅ Real-time player list qua socket event **`lobby:updated`** — server bắn 1 snapshot đầy đủ, KHÔNG có `lobby:player-joined`/`lobby:player-left` như doc cũ ghi
- ✅ Host có thể kick player, update config
- ✅ Hiển thị room code + QR code để share

#### Cấu trúc thư mục
```
feature/lobby/
├── src/main/java/.../feature/lobby/
│   ├── presentation/
│   │   ├── hostlobby/                     # 🆕 N18 (30/8) — hàng thật, đã test trên máy thật
│   │   │   ├── HostLobbyScreen.kt        # Stateful + HostLobbyScreenContent stateless
│   │   │   ├── HostLobbyViewModel.kt     # re-join sau MỌI Connected, refresh token đúng 1 lần
│   │   │   ├── HostLobbyUiState.kt       # + hasLobbySnapshot, ConnectionStatus
│   │   │   ├── HostLobbyIntent.kt        # Retry / LeaveRoom / ErrorShown
│   │   │   └── HostLobbyEffect.kt        # ExitLobby(message)
│   │   ├── joinroom/                       # N19 (5/9) → N19.6 (6/9): card nhập mã nhúng ở Home
│   │   │   ├── JoinRoomCard.kt             # 🆕 N19.6 — Card + Content stateless; lỗi & lý do bị rời phòng hiện inline
│   │   │   ├── JoinRoomViewModel.kt       # lookup → rẽ nhánh: đã login → join luôn / khách → GuestNickname
│   │   │   └── JoinRoomContract.kt         # 🆕 N19.6 — UiState + Intent + Effect + `SESSION_CODE_LENGTH = 6`
│   │   ├── guestnickname/                  # 🆕 N19 (5/9) — chỉ dành cho khách
│   │   │   ├── GuestNicknameScreen.kt
│   │   │   ├── GuestNicknameViewModel.kt  # NicknameValidator + GuestIdentityStore (UUID)
│   │   │   ├── GuestNicknameUiState.kt
│   │   │   ├── GuestNicknameIntent.kt
│   │   │   └── GuestNicknameEffect.kt
│   │   └── playerlobby/                    # 🆕 N19 (5/9) — hàng thật, dùng lại socket layer N18
│   │       ├── PlayerLobbyScreen.kt       # Stateful + Content stateless
│   │       ├── PlayerLobbyViewModel.kt    # re-join sau MỌI Connected; token invalid = fatal (không refresh được)
│   │       ├── PlayerLobbyUiState.kt
│   │       ├── PlayerLobbyIntent.kt
│   │       └── PlayerLobbyEffect.kt       # ExitLobby(message)
│   └── domain/
│       └── usecase/
│           └── RefreshHostTokenUseCase.kt # 🆕 N18: POST /games/:id/host-token (idempotent)
```

> ✅ **Đã chốt ở N19 (5/9)**: use case thật của `feature:lobby` là `RefreshHostTokenUseCase` (N18), `LookupRoomUseCase` và `JoinGameUseCase` (N19). Tên dự kiến trong v2.2 không dùng: `JoinGameAsPlayerUseCase` → `JoinGameUseCase`; `GetHostSocketTokenUseCase` → `RefreshHostTokenUseCase`; `ConnectLobbySocketUseCase` **bỏ hẳn** vì connect nằm trong `GameSocketRepository.events()` ở `core:network`.

```text
└── build.gradle.kts
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // GameSessionRepository, GameEvent
    implementation(project(":core:network"))   // N18: impl socket ở core:network; feature chỉ dùng interface từ core:common
    implementation(project(":core:ui"))
    
    implementation(libs.zxing)                 // QR code generation
}
```

#### Lưu ý quan trọng
🔄 **Two-step Join**: 
1. REST call `POST /games/:code/join` → nhận `socketToken`
2. Connect Socket.IO với `auth: {token: socketToken}`

🎮 **Role-aware**: Host và Player nhận events khác nhau - Host vào `hostRoom`, Player vào `room`.

---

### 3.4 `:feature:game-player` - Gameplay (Player Perspective)

**📦 Module Type:** `com.android.library`

#### Mục đích
Màn hình chơi game phía Player. Hỗ trợ cả **host-paced** (classic - tất cả cùng câu hỏi) và **self-paced** (solo/survival/marathon/practice - mỗi người tiến độ riêng).

#### Trách nhiệm
- ✅ GamePlayScreen - hiển thị câu hỏi, ảnh câu hỏi N40, timer, điểm số
- ✅ GameViewModel với `GamePhaseUi` state machine (Lobby → Countdown → Question → Results → Finished)
- ✅ AnswerInput dispatcher theo 4 `question_type`: multiple_choice, multiple_select, short_answer, long_answer
- ✅ Submit answer qua socket với ACK response
- ✅ Reconnect handling - restore state từ `game:state` event
- ✅ Timer synchronization với server offset
- ✅ N24: `answer:received` progress, `question:results`, leaderboard giữa câu theo config
- ✅ N24: pause/resume từ `game:state.sessionStatus`, khóa ở cả UI và ViewModel
- ✅ N24: `game:ended` → lưu transient result → `NavigateToFinalResult`
- ✅ N25 Android hardening: Submitted không bị mở lại sau pause/resume; snapshot cũ không kéo Results lùi; `GameConnection.RECONNECT_FAILED` + Retry tạo connection mới; Host phân biệt transport với `SERVER_DISCONNECT`
- ✅ N25 regression tests: `GameViewModelTest` phủ stale snapshot, pause/resume submitted và reconnect exhaustion → Retry → join + sync; `HostGameViewModelTest` có 6 case cho state machine/disconnect
- ✅ N26–N27: self-paced ACK feedback, soft deadline, manual `question:next` và restore `question:awaiting_next`
- ✅ N28: Survival lives/elimination; Marathon giữ timer câu và timer tổng riêng; typed `question:timeout`/`player:finished`; phase cá nhân `Eliminated`/`PlayerFinished` chờ `game:ended`
- ⚠️ N25 E2E chưa pass: backend presence race vẫn có thể loại Player đã reconnect khỏi `activePlayers`; chi tiết ở `N25_E2E_CLASSIC_CHECKLIST.md` và `knowledgement/n25_knowledgement.md`

#### Cấu trúc thư mục
```
feature/game-player/
├── src/main/java/.../feature/game_player/
│   ├── presentation/
│   │   ├── GamePlayScreen.kt             # Main game UI + progress/results/live leaderboard
│   │   ├── GameViewModel.kt              # MVI reducer; race/config/pause gating
│   │   ├── GameUiState.kt                # 🆕 N24 - state + derived visibility/input rules
│   │   ├── GameIntent.kt                 # 🆕 N24 - tách theo chuẩn AGENTS
│   │   ├── GameEffect.kt                 # 🆕 N24 - NavigateToFinalResult
│   │   ├── GameContract.kt               # pure answer/result feedback helpers
│   │   └── components/
│   │       ├── AnswerInput.kt            # Dispatcher theo question_type
│   │       ├── SingleChoiceGrid.kt       # Radio buttons cho multiple_choice
│   │       ├── MultiSelectGrid.kt        # Checkboxes cho multiple_select
│   │       ├── ShortTextField.kt         # Text input cho short_answer
│   │       ├── LongTextField.kt          # Multiline input cho long_answer
│   │       ├── CountdownTimer.kt         # Circular timer với server sync
│   │       └── ScoreDisplay.kt
│   ├── domain/
│   │   ├── repository/
│   │   │   └── (⚠️ N18: PlayerGameSocketRepository đã chuyển sang core:common/repository)
│   │   └── usecase/
│   │       ├── SubmitAnswerUseCase.kt
│   │       ├── RequestNextQuestionUseCase.kt  # Self-paced only
│   │       └── SyncGameStateUseCase.kt        # Reconnect recovery
│   └── data/
│       └── repository/
│           └── (⚠️ N18: impl đã chuyển sang core:network/socket)
└── build.gradle.kts
```

#### GamePhaseUi State Machine
```kotlin
sealed class GamePhaseUi {
    object Lobby : GamePhaseUi()
    data class Countdown(val secondsLeft: Int) : GamePhaseUi()
    data class Question(
        val question: Question,
        val timeLimit: Int?,
        val endsAt: Instant?,
        val allowAnswerLate: Boolean,
        val currentAnswer: Answer?
    ) : GamePhaseUi()
    data class QuestionResults(
        val question: Question,
        val correctAnswer: Any?,        // Chỉ có khi server reveal
        val playerAnswer: Answer,
        val isCorrect: Boolean,
        val scoreEarned: Int
    ) : GamePhaseUi()
    data class Finished(val finalScore: Int, val rank: Int?) : GamePhaseUi()
}
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // GameEvent, Question, Player
    implementation(project(":core:network"))   // SocketFactory, GameEventMapper
    implementation(project(":core:ui"))
}
```

#### Lưu ý quan trọng
⏱️ **Timer Sync**: Mỗi event `question:started` có `serverTime` → tính offset → local timer chính xác.

🔒 **Answer Locking**: Sau khi submit → disable input ngay lập tức, không đợi ACK (prevent double-submit).

🔄 **Reconnect**: Khi mất kết nối → reconnect → server gửi `game:state` snapshot → restore phase.

---

### 3.5 `:feature:game-host` - Host Control Console

**📦 Module Type:** `com.android.library`

#### Mục đích
Màn hình điều khiển game phía Host. Hỗ trợ cả Classic/host-paced và dashboard self-paced cho Solo/Survival/Marathon; phần realtime chỉ hiển thị dữ liệu server-authoritative thực sự nhận được.

#### Trách nhiệm
- ✅ HostGameScreen - dashboard theo dõi game
- ✅ HostGameViewModel với `HostGameUiState` riêng (KHÔNG dùng chung với Player)
- ✅ Emit host actions: `host:start`, `host:pause`, `host:resume`, `host:next`, `host:end`
- ✅ Classic: nhận `host:question`, `host:answer-received`, `leaderboard:host`; hiển thị câu/ảnh câu hỏi N40/đáp án, submissions và leaderboard realtime
- ✅ Config update qua `lobby:config-update` (lobby phase only)
- ✅ Self-paced: xử lý `host:player-progress`, `player:finished`, `player:eliminated`; merge với baseline `leaderboard:host`; render tiến độ/điểm/lives/streak/status theo từng Player; không giả định câu hỏi/timer chung và không hiện `game:next`
- ⚠️ Backend gate: `host:player-progress` còn thiếu lives/streak; timeout không phát progress/elimination; late-join/reconnect/disconnect active không refresh Host. Client giữ baseline cũ thay vì polling hoặc tự suy dữ liệu

#### Cấu trúc thư mục
```
feature/game-host/
├── src/main/java/.../feature/game_host/
│   ├── presentation/
│   │   ├── HostGameScreen.kt             # Dashboard với controls
│   │   ├── HostGameViewModel.kt          # HostGameUiState - khác GamePhaseUi
│   │   └── components/
│   │       ├── HostControls.kt           # Start/Pause/Resume/Next/End buttons
│   │       ├── PlayerProgressList.kt     # Real-time progress (self-paced)
│   │       ├── AnswerSubmissionFeed.kt   # Live feed của answers
│   │       └── QuestionPreview.kt        # Host xem câu hỏi + đáp án đúng
│   ├── domain/
│   │   ├── repository/
│   │   │   └── (⚠️ N18: HostGameSocketRepository đã chuyển sang core:common/repository)
│   │   └── usecase/
│   │       ├── HostControlUseCase.kt           # Emit host actions
│   │       └── UpdateRoomConfigUseCase.kt      # Lobby phase only
│   └── data/
│       └── repository/
│           └── (⚠️ N18: impl đã chuyển sang core:network/socket)
└── build.gradle.kts
```

#### HostGameUiState
```kotlin
data class HostGameUiState(
    val phase: HostPhase,                    // Lobby/Countdown/Playing/Paused/Finished
    val currentQuestion: QuestionWithAnswer?, // Host thấy đáp án đúng
    val playerCount: Int,
    val submissions: List<PlayerSubmission>,  // Real-time answer feed
    val leaderboard: List<PlayerScore>,
    val playerProgress: Map<String, Int>      // playerId → questionIndex (self-paced)
)
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
}
```

#### Lưu ý quan trọng
👑 **Host Privileges**: Host nhận payload khác hẳn Player - `host:question` có `correct_answer`, `host:answer-received` có `is_correct`.

🔀 **Separate Repository**: `HostGameSocketRepository` KHÔNG dùng chung với `PlayerGameSocketRepository` dù cùng phòng - vì hợp đồng khác nhau theo vai trò.

---

### 3.6 `:feature:leaderboard` - Rankings & Results

**📦 Module Type:** `com.android.library`

#### Mục đích
Sở hữu màn kết quả cuối game và màn chi tiết lịch sử. Leaderboard giữa câu vẫn nằm trong Gameplay Player; N24 truyền kết quả cuối từ `game:ended` qua repository boundary dùng chung, N29 bổ sung breakdown post-game từ REST review, N35 bổ sung REST recovery sau process death, và N39 mở lại snapshot/answer sheet bằng danh tính bền vững thay vì socket token cũ.

#### Trách nhiệm
- ✅ N24: `FinalResultScreen` + `FinalResultViewModel` + contract riêng
- ✅ Đọc `StoredGameResult` transient do Gameplay lưu từ payload `game:ended`
- ✅ N29: `StoredGameResult` mang thêm `mode` + `socketToken` chỉ trong memory; token không đi qua route/Bundle/DataStore
- ✅ Hiển thị bảng cuối, rank/score và highlight Player hiện tại
- ✅ N29: lazy-load `GET /games/{id}/review` sau khi kết thúc; loading/error/retry; ưu tiên câu bỏ qua/sai; render ảnh, đáp án Player/đúng, explanation, time và score
- ✅ N35: nếu transient result còn thì không gọi REST; nếu mất sau process death thì `LoadGameResultsUseCase` gọi `GET /games/{id}/results` với loading/error/retry riêng
- ✅ N35: render `perQuestion` đã sort, số lượt đúng/tổng và tỷ lệ đúng an toàn khi mẫu số bằng 0
- ✅ N35: enforce `showLeaderboard=never` trước presentation state; endpoint backend public vẫn trả full table đã ghi `BUG-16`
- ✅ Fallback an toàn khi leaderboard rỗng, Review lỗi hoặc process recreation làm mất transient result/token
- ✅ N39: `GameHistoryDetailScreen/ViewModel/Contract` mở từ card Hoạt động qua `Route.GameHistoryDetail(sessionId)`
- ✅ N39: `LoadGameHistorySummaryUseCase` và `LoadGameHistoryAnswersUseCase`; user dùng cookie, guest dùng UUID đã tồn tại; host không gọi answers
- ✅ N39: summary/answers có loading-error-retry độc lập; render snapshot quiz, thành tích riêng, leaderboard, host per-question và answer sheet
- ✅ N39: player giữ `viewerResult` nhưng full leaderboard bị loại khi `showLeaderboard=never`; host vẫn xem đủ
- ⚠️ Backend `BUG-17`: per-question group theo `question_id` nhưng dùng `min(question_index)`, nên self-paced shuffle có thể tạo nhiều thẻ cùng nhãn “Câu N”; Android không gộp nhầm theo index
- ⏳ Share result ngoài phạm vi N39

#### Cấu trúc thư mục
```
feature/leaderboard/
├── src/main/java/.../feature/leaderboard/
│   ├── domain/
│   │   ├── LoadGameReviewUseCase.kt          # N29 - post-game REST review
│   │   ├── LoadGameResultsUseCase.kt         # N35 - REST recovery khi mất transient result
│   │   ├── LoadGameHistorySummaryUseCase.kt  # N39 - summary theo cookie/guest identity
│   │   └── LoadGameHistoryAnswersUseCase.kt  # N39 - answer sheet của participant
│   └── presentation/
│       ├── FinalResultScreen.kt              # bảng cuối + shared stat/review cards
│       ├── FinalResultViewModel.kt
│       ├── FinalResultContract.kt
│       ├── FinalResultIntent.kt
│       └── historydetail/
│           ├── GameHistoryDetailContract.kt
│           ├── GameHistoryDetailViewModel.kt
│           └── GameHistoryDetailScreen.kt
└── build.gradle.kts
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // GameSessionRepository + GameReview models
    implementation(project(":core:ui"))
}
```

---

### 3.7 `:feature:quiz-manage` - Quiz CRUD

**📦 Module Type:** `com.android.library`

#### Mục đích
Tạo, chỉnh sửa, xóa quiz (chỉ dành cho Host đã login). Editor hỗ trợ 4 loại câu hỏi với upload ảnh S3 presign.

#### Trách nhiệm
- ✅ QuizManageScreen - danh sách quiz của tôi
- ✅ QuizEditorScreen - editor với drag-drop reorder questions
- ✅ CreateRoomScreen - tạo game room với config động từ `GET /games/game-modes`
- ✅ Upload ảnh 2 bước: (1) GET presigned URL, (2) PUT file trực tiếp lên S3
- ✅ Question editor hỗ trợ 4 types: multiple_choice, multiple_select, short_answer, long_answer
- ✅ N40: card câu hỏi trong Quiz Detail render `questionImage` bằng component dùng chung
- ✅ N41: Quiz Detail thêm nút Tự chơi thử, disabled nếu quiz rỗng; callback điều hướng do app sở hữu, không phụ thuộc trực tiếp `feature:quiz-preview`.

#### Cấu trúc thư mục
```
feature/quiz-manage/
├── src/main/java/.../feature/quiz_manage/
│   ├── presentation/
│   │   ├── quizdetail/                   # 🆕 N12 (21/8) — chuyển từ feature:home
│   │   │   ├── QuizDetailScreen.kt       # Preview quiz + entry điểm chơi
│   │   │   ├── QuizDetailViewModel.kt
│   │   │   ├── QuizDetailUiState.kt
│   │   │   └── QuizDetailIntent.kt
│   │   ├── QuizManageScreen.kt           # List của quiz của tôi + FAB create
│   │   ├── QuizEditorScreen.kt           # Edit quiz metadata + questions
│   │   ├── CreateRoomScreen.kt           # Tạo game session với config
│   │   └── components/
│   │       ├── QuestionEditor.kt         # Dispatcher theo question_type
│   │       ├── MultipleChoiceEditor.kt
│   │       ├── MultipleSelectEditor.kt
│   │       ├── ShortAnswerEditor.kt
│   │       ├── LongAnswerEditor.kt
│   │       └── ImageUploadField.kt       # S3 presign upload
│   └── domain/
│       └── usecase/
│           ├── GetQuizWithOwnershipUseCase.kt  # 🆕 N18.5 - Orchestrates quiz detail + ownership check (replaces GetQuizDetailUseCase)
│           ├── CreateQuizWithAssetsUseCase.kt  # 🆕 N18.5 - Orchestrates quiz creation + image uploads (replaces CreateQuizUseCase + UploadImageUseCase)
│           ├── UpdateQuizWithAssetsUseCase.kt  # 🆕 N18.5 - Orchestrates quiz update + image uploads (replaces UpdateQuizUseCase + UploadImageUseCase)
│           ├── DeleteQuizUseCase.kt
│           ├── CreateGameSessionUseCase.kt     # inject GameSessionRepository
│           └── GetGameModesUseCase.kt          # GET /games/game-modes
└── build.gradle.kts
```

#### S3 Presign Upload Flow
```kotlin
// Step 1: Get presigned URL from backend
val presignResponse = storageApi.getPresignUrl(filename, contentType)

// Step 2: PUT file directly to S3 (không gửi cookie, không authentication)
val uploadRequest = Request.Builder()
    .url(presignResponse.uploadUrl)
    .put(file.asRequestBody())
    .build()

// Step 3: Use public URL returned from step 1
quiz.imageUrl = presignResponse.publicUrl
```

#### Phụ thuộc
```kotlin
dependencies {
    implementation(project(":core:common"))    // QuizRepository, GameSessionRepository
    implementation(project(":core:ui"))
    
    implementation(libs.coil.compose)          // Image loading
}
```

#### Lưu ý quan trọng
🎨 **Dynamic Config UI**: CreateRoomScreen đọc `GET /games/game-modes` để render form động - không hardcode 5 modes. Backend có thể thêm mode mới mà app không cần update.

📸 **Image Upload**: Presigned URL cho phép client upload trực tiếp S3 mà không cần proxy qua backend.

🗄️ **Quiz cache = fallback, KHÔNG phải offline-first** (chốt 22/8, harden N37): `QuizCacheStore` chỉ cache quiz detail đã tải thành công; repository chỉ fallback cho **cùng quizId** khi lỗi mạng/server tạm thời. Resource missing như `QUIZ_NOT_FOUND` phải xóa cache stale, không được trả dữ liệu cũ làm quiz đã xóa “sống lại”. `feature:home` không cache danh sách → user không chọn được quiz mới lúc offline. Cache chỉ có tác dụng khi mở lại đúng quiz đã cache trước đó (retry, quay lại từ list in-memory, khôi phục sau process-death). Quyết định giữ nguyên scope fallback, không mở rộng thành offline-first đầy đủ.

⚠️ **Kỹ thuật nợ đã biết (chưa fix)**: `searchQuizzes`/`getMyQuizzes` ở `core:network/QuizApiService` + `QuizRepositoryImpl` có cùng loại lỗi wrapper envelope như quiz detail (`{ quizzes: [...] }` chưa unwrap đúng) — cần `QuizListDto(val quizzes: List<QuizCardDto>)` tương tự `QuizDetailDto`. Chưa impl màn dùng tới (list quiz của tôi) nên để làm ở N13–14.

---

### 3.8 `:feature:quiz-preview` - Solo Preview local (N41)

**Status:** Hoàn thành tại commit `648bf37`; user xác nhận kiểm thử nhanh pass.

- Dependency module: chỉ `:core:common` + `:core:ui`, không network/database hoặc feature khác. `:app` wiring `QuizPreviewNavGraph.kt`, `Route.QuizPreview(quizId)` và callback từ Quiz Detail.
- Namespace/package: `android.kma.myquizzapp.feature.quiz_preview`. File thực tế nằm trực tiếp trong package (không dựng thêm cây presentation/domain/data giả).
- `PreviewContract.kt`: `PreviewPhase`, `QuizPreviewUiState`, intent/effect, `PreviewQuestionResult`.
- `PreviewScoring.kt`: evaluator thuần Kotlin cho 4 types; multiple-select exact set, text trim/case-insensitive, key thiếu không crash; điểm ước tính Classic mặc định, đúng muộn chỉ 900 không bonus.
- `QuizPreviewViewModel.kt`: inject `QuizRepository`, `SessionRepository`, `SavedStateHandle`; đọc quiz và owner từ session SSOT, countdown 3 giây, timer monotonic, local grade, skip/late, feedback auto-next 2/4 giây, restart/exit/edit.
- `QuizPreviewScreen.kt`: stateful wrapper + stateless content; loading/error/empty, ảnh `QuestionImage`, hint, inputs, feedback, tổng kết/review. Edit owner-only pop Preview trước khi mở editor.
- Tests: `PreviewScoringTest.kt`, `QuizPreviewViewModelTest.kt` (scoring/types/key/late, load/countdown/local grade và số lần gọi quiz repository).
- Không room/host/socket/history/play-count; không dùng nguyên `GamePlayScreen`. Không thay đổi server-authoritative của multiplayer; điểm preview không phải kết quả trận thật.

---

## 4. Dependency Rules

### Bảng phụ thuộc giữa các modules

| Module Source | CÓ THỂ phụ thuộc | KHÔNG ĐƯỢC phụ thuộc |
|---|---|---|
| `:app` | Tất cả modules khác | — |
| `:feature:*` | `:core:network`, `:core:database`, `:core:datastore`, `:core:ui`, `:core:common` | Các `:feature:*` khác |
| `:core:network` | `:core:common` | `:core:database`, `:core:ui`, bất kỳ `:feature:*` |
| `:core:database` | `:core:common` | `:core:network`, `:core:ui`, bất kỳ `:feature:*` |
| `:core:datastore` | `:core:common` | Mọi `:core:*` khác, bất kỳ `:feature:*` |
| `:core:ui` | `:core:common` | `:core:network`, `:core:database`, `:core:datastore`, bất kỳ `:feature:*` |
| `:core:common` | Không phụ thuộc module nào | Mọi module khác trong project |

### Nguyên tắc quan trọng

✅ **DOs:**
- Feature modules chỉ phụ thuộc core modules
- Core modules chỉ phụ thuộc `:core:common` (ngoại trừ `:core:common` không phụ thuộc gì)
- Nếu 2 features cần chia sẻ logic → đưa xuống `:core:*`
- Dùng interfaces (abstraction) thay vì concrete classes khi cross-module
- Nếu `:app` cần logic mà nhiều feature/module có thể cần lại → tạo use case trung gian ở `:core:*` phù hợp thay vì để `:app` phụ thuộc domain của 1 feature cụ thể

❌ **DON'Ts:**
- Feature KHÔNG ĐƯỢC phụ thuộc feature khác
- Core module KHÔNG ĐƯỢC phụ thuộc feature module
- `:core:network` và `:core:database` KHÔNG ĐƯỢC phụ thuộc lẫn nhau
- `:core:common` KHÔNG ĐƯỢC phụ thuộc bất kỳ module nào
- `:app` KHÔNG ĐƯỢC phụ thuộc trực tiếp vào `domain/` của 1 feature cụ thể cho logic cross-cutting

### Dependency Inversion Example

**Problem**: `:core:network` cần persist cookies, nhưng KHÔNG ĐƯỢC phụ thuộc `:core:database`.

**Solution**:
1. Define `CookieStore` **interface** ở `:core:common` (abstraction)
2. `:core:network` dùng interface đó qua constructor injection
3. `:core:database` implement `RoomCookieStore` và tự `@Binds` sang interface
4. Hilt tự wire implementation khi build `:app`

```kotlin
// core:common (abstraction)
interface CookieStore {
    suspend fun loadForHost(host: String): List<StoredCookie>
}

// core:network (depends on abstraction, not implementation)
class PersistentCookieJar @Inject constructor(
    private val cookieStore: CookieStore  // Interface only!
)

// core:database (provides implementation)
class RoomCookieStore @Inject constructor(...) : CookieStore {
    override suspend fun loadForHost(host: String) = ...
}

@Module @InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {
    @Binds abstract fun bindCookieStore(impl: RoomCookieStore): CookieStore
}
```

---

## 5. Repository Pattern

### 5.1. Placement Rules

**Quy tắc đặt Repository interface và implementation:**

1. **Repository dùng bởi ≥2 features** → interface ở `:core:common/repository`
2. **Repository dùng bởi chỉ 1 feature** → interface ở `feature:<đó>/domain/repository`
3. **Implementation** đặt ở module sở hữu công nghệ cần thiết:
   - Retrofit/OkHttp → `:core:network`
   - Room → `:core:database`
   - Socket.IO → `:core:network` hoặc trong feature nếu hợp đồng riêng

### 5.2. Repository Mapping Table

| Repository | Interface Location | Implementation Location | Used By |
|---|---|---|---|
| `CookieStore` | `core:common/cookie` | `core:database` (RoomCookieStore) | `core:network` only |
| `AuthRepository` | `core:common/repository` | `core:network` (AuthRepositoryImpl) | `feature:auth`, `core:datastore` (CheckAuthStateUseCase) |
| `QuizRepository` | `core:common/repository` | `core:network` (QuizRepositoryImpl) | `feature:home`, `feature:quiz-manage` |
| `GameSessionRepository` | `core:common/repository` | `core:network` (GameSessionRepositoryImpl) | `feature:lobby`, `feature:quiz-manage`, `feature:leaderboard` (N29 review, N35 results recovery, N39 history detail/answers), `app` (N38 history list) |
| `GameSocketRepository` (base) | `core:common/repository` | `core:network/socket` (GameSocketClient) | `feature:lobby` (host ở N18, player ở N19), `feature:game-*` (N21+) |
| `PlayerGameSocketRepository` | `core:common/repository` | `core:network/socket` (PlayerGameSocketRepositoryImpl) | `feature:lobby` (player), `feature:game-player` |
| `HostGameSocketRepository` | `core:common/repository` | `core:network/socket` (HostGameSocketRepositoryImpl) | `feature:lobby` (host), `feature:game-host` |
| `GameResultRepository` | `core:common/repository` | `core:network/repository` (InMemoryGameResultRepository) | `feature:game-player`, `feature:leaderboard` |

### 5.3. Why Separate Socket Repositories?

`PlayerGameSocketRepository` và `HostGameSocketRepository` chỉ share **base** `GameSocketRepository` (`events`/`joinLobby`/`disconnect`), không share phần còn lại, vì:
- Host join `hostRoom`, Player join `room` - khác namespace
- Host nhận `host:question` (có `correct_answer`), Player nhận `question:started` (không có)
- Host emit `host:start/pause/resume`, Player emit `question:answer`
- Sai vai trò trở thành lỗi biên dịch: player không thể gọi `startGame`, không phải đợi server trả lỗi runtime

⚠️ **Đổi so với v2.2 (N18, 30/8)**: cả 3 interface đặt ở `core:common/repository`, impl ở `core:network/socket` — không đặt trong feature module nữa. Lý do: `feature:lobby`, `feature:game-host`, `feature:game-player` dùng chung một connection `/game`, mà feature module thì không được phụ thuộc nhau → đặt ở feature sẽ phải nhân bản code socket 3 lần.

### 5.4. Không có `AppError.Socket` (quyết định N18)

Contract lỗi socket thật là `{ event, code }` — chỉ có code, cùng bảng code với REST (`shared/errors/codes.ts`). Vì `AppError.Api(code)` từ N16.5 đã map ~60 code sang tiếng Việt, thêm nhánh `Socket` chỉ tạo hai đường dịch song song rồi lệch nhau. `GameEvent.Failed(event, code)` mang thêm tên event để phân loại fatal/không fatal, phần hiển thị vẫn qua `Api(code)`.

---

## 6. Clean Architecture Layers

### 6.1. Layer Responsibilities

Mỗi feature module có tối đa **3 layers**: Presentation, Domain, Data.

#### **Presentation Layer** (`presentation/`)
- ✅ Composable functions (UI)
- ✅ ViewModels (MVI pattern: Intent → State)
- ✅ UI state classes (`UiState`, `UiIntent`, `UiEvent`)
- ✅ Navigation logic
- ❌ KHÔNG gọi Retrofit/Room trực tiếp
- ❌ KHÔNG chứa business logic phức tạp

#### **Domain Layer** (`domain/`)
- ✅ UseCases (business logic thuần Kotlin)
- ✅ Repository interfaces (nếu chỉ feature này dùng)
- ✅ Domain models (nếu khác với models ở `core:common`)
- ✅ Business rules, validation logic
- ❌ KHÔNG import Android framework classes (Context, Bundle, etc.)
- ❌ KHÔNG import Compose/UI libraries
- ❌ KHÔNG biết Retrofit/Room/Socket.IO tồn tại

#### **Data Layer** (`data/`)
- ✅ Repository implementations
- ✅ Data sources (RemoteDataSource, LocalDataSource)
- ✅ DTO ↔ Domain model mapping
- ✅ API calls, database queries, socket events
- ❌ KHÔNG chứa business logic
- ❌ KHÔNG expose DTOs ra ngoài - chỉ expose domain models

### 6.2. Dependency Flow (Never Violated)

```
Presentation → Domain → Data
    ↓           ↓         ↓
   UI      UseCases   Repository
           (logic)    (I/O)
```

**Rules:**
- Presentation depends on Domain (call UseCases)
- Domain depends on Data interfaces (không depend on implementations)
- Data implements Domain interfaces
- Mũi tên chỉ chiều xuống - KHÔNG BAO GIỜ đi ngược lên

### 6.3. MVI Pattern trong ViewModels

**Bắt buộc:** ViewModel chỉ gọi Use case; không inject/gọi Repository trực tiếp, kể cả observe session/capture token. Use case phối hợp Repository interface, data implementation giữ API/DTO/Room. Mẫu N43: `AccountSecurityViewModel` → 3 Use case trong `app/domain/security`. Legacy debt được audit/report trước khi sửa, không dùng làm tiền lệ.

```kotlin
// Intent - User actions
sealed class GameIntent {
    object LoadGame : GameIntent()
    data class SubmitAnswer(val answer: Answer) : GameIntent()
    object RequestNextQuestion : GameIntent()
}

// State - Single source of truth
data class GameUiState(
    val phase: GamePhaseUi = GamePhaseUi.Lobby,
    val score: Int = 0,
    val isLoading: Boolean = false,
    val error: UiText? = null
)

// ViewModel
class GameViewModel @Inject constructor(
    private val submitAnswerUseCase: SubmitAnswerUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    
    fun handleIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.SubmitAnswer -> submitAnswer(intent.answer)
            // ...
        }
    }
}
```

---

## 7. Best Practices & Patterns

### 7.1. Naming Conventions

| Component | Pattern | Examples |
|---|---|---|
| Module | `:<category>:<feature>` | `:core:network`, `:feature:auth` |
| Package | lowercase, kebab-case | `feature.game_player`, `core.common` |
| Composable | PascalCase + Screen/Dialog | `LoginScreen`, `ConfirmDialog` |
| ViewModel | PascalCase + ViewModel | `LoginViewModel`, `GameViewModel` |
| UseCase | PascalCase + UseCase | `LoginUseCase`, `SubmitAnswerUseCase` |
| Repository Interface | PascalCase + Repository | `AuthRepository`, `QuizRepository` |
| Repository Impl | PascalCase + RepositoryImpl | `AuthRepositoryImpl` |

### 7.2. Code Organization Patterns

#### **Stateful/Stateless Composable Pattern**
```kotlin
// Stateful integration boundary: ViewModel, Flow, effect, navigation/platform API.
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Collect one-off effect ở đây.
    LoginScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent
    )
}

// Stateless UI: chỉ nhận state/value/callback, Preview và test trực tiếp.
@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    onIntent: (LoginIntent) -> Unit
) {
    // Pure UI
}
```

Quy ước đã được audit và áp dụng cho toàn bộ 14 Screen hiện có ngày 28/8/2026. Activity Result launcher, `BackHandler`, lifecycle effect, focus/pagination observer và navigation effect thuộc Stateful boundary. Reusable component stateless mặc định; nếu có convenience wrapper giữ state cục bộ thì phải cung cấp stateless content API lõi. Create/Edit Quiz dùng chung `feature:quiz-manage/presentation/components/QuizEditorContent.kt`.

#### **UseCase Pattern**
```kotlin
class SubmitAnswerUseCase @Inject constructor(
    private val repository: PlayerGameSocketRepository
) {
    suspend operator fun invoke(
        questionId: String,
        answer: Answer
    ): Result<AnswerAck> {
        return repository.submitAnswer(questionId, answer)
    }
}
```

#### **Result Wrapper Pattern**
```kotlin
// N22–N23: ViewModel đi qua PlayerGameSessionUseCase và giữ server-authoritative.
viewModelScope.launch {
    _state.update { it.copy(isSubmitting = true) } // khóa trước khi emit

    playerGameSession.submitAnswer(answer)
        .onSuccess { ack ->
            // Host-paced ACK chỉ xác nhận acceptance, không tự suy luận đúng/sai.
            _state.update { it.copy(isSubmitting = false, isSubmitted = ack.accepted) }
        }
        .onFailure { error ->
            // Timeout/not-connected là unknown outcome: giữ khóa và resync, không retry.
            playerGameSession.sync()
            _state.update { it.copy(isSubmitting = false, error = error.toUiText()) }
        }
}
```

### 7.3. Hilt Injection Patterns

#### **Constructor Injection (Preferred)**
```kotlin
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val getUserUseCase: GetCurrentUserUseCase
) : ViewModel()
```

#### **@Binds for Interface → Implementation**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository
}
```

#### **@Provides for Complex Objects**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: CookieJar,
        authenticator: Authenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .authenticator(authenticator)
        .build()
}
```

### 7.4. Testing Strategy

| Layer | Test Type | Tools | What to Test |
|---|---|---|---|
| Domain (UseCases) | Unit Test | JUnit, MockK | Business logic, edge cases |
| Data (Repository) | Unit Test | MockWebServer, Room in-memory | API contracts, data mapping |
| Presentation (ViewModel) | Unit Test | Turbine, Fake repositories | State transitions, intent handling |
| UI (Composables) | UI Test | Compose Test, semantics | User interactions, UI states |

---

## 8. Common Pitfalls to Avoid

### ❌ Wrong: Feature phụ thuộc Feature
```kotlin
// feature:home/build.gradle.kts
dependencies {
    implementation(project(":feature:auth"))  // WRONG!
}
```
✅ **Fix**: Nếu cần logic của `:feature:auth` → di chuyển logic đó xuống `:core:common`.

### ❌ Wrong: ViewModel gọi Repository trực tiếp
```kotlin
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository  // WRONG! Skip UseCase
)
```
✅ **Fix**: Luôn đi qua UseCase - tách business logic khỏi presentation.

### ❌ Wrong: Exposing DTOs ra ngoài Data Layer
```kotlin
interface QuizRepository {
    suspend fun getQuizzes(): List<QuizDto>  // WRONG! DTO leak
}
```
✅ **Fix**: Map DTO → Domain model trong Repository implementation.

### ❌ Wrong: Hardcode config values
```kotlin
val GAME_MODES = listOf("classic", "solo", "survival")  // WRONG! May change
```
✅ **Fix**: Gọi `GET /games/game-modes` để lấy config động từ backend.

### ❌ Wrong: Domain layer import Android classes
```kotlin
class LoginUseCase @Inject constructor(
    private val context: Context  // WRONG! Android framework in domain
)
```
✅ **Fix**: Domain phải thuần Kotlin - nếu cần context → xử lý ở presentation layer.

### ❌ Wrong: `:app` giữ logic cross-cutting mà nhiều feature có thể cần lại
```kotlin
// app/.../usecase/CheckAuthStateUseCase.kt - chỉ Splash gọi được vì nằm trong :app
class CheckAuthStateUseCase @Inject constructor(...)
```
✅ **Fix**: Nếu logic cross-cutting (vd. kiểm tra trạng thái đăng nhập) có thể cần dùng lại ở nhiều feature trong tương lai (không chỉ 1 nơi gọi ban đầu như Splash) → đưa xuống `core:*` module sở hữu dependency cần thiết (ở đây là `core:datastore` vì cần `SettingsDataStore`) và tạo use case trung gian ở đó, thay vì để `:app` phụ thuộc domain của 1 feature cụ thể hoặc tự giữ logic riêng trong `:app`. Đây chính là refactor đã áp dụng cho `CheckAuthStateUseCase` (xem mục 2.4, 2.1).

### ❌ Wrong: Package Kotlin không khớp namespace Gradle của feature
```kotlin
// feature/auth/build.gradle.kts khai báo namespace "android.kma.myquizzapp.feature.auth"
// nhưng code lại nằm ở package thiếu "feature.":
package android.kma.myquizzapp.auth.domain.usecase  // WRONG!
```
✅ **Fix**: Package Kotlin của mọi file trong `feature:<tên>` phải bắt đầu bằng `android.kma.myquizzapp.feature.<tên>.*`, khớp namespace Gradle và đồng nhất với các feature khác (vd. `feature:home`).

---

## 9. Quick Reference Guide

### 9.1. Module Dependency Cheat Sheet

```
app → ALL modules

feature:auth → core:{network,database,datastore,ui,common}
feature:home → core:{network,database,datastore,ui,common}
feature:lobby → core:{network,ui,common}
feature:game-player → core:{network,ui,common}
feature:game-host → core:{network,ui,common}
feature:leaderboard → core:{network,ui,common}
feature:quiz-manage → core:{network,ui,common}
feature:quiz-preview → core:{ui,common}

core:network → core:common
core:database → core:common
core:datastore → core:common
core:ui → core:common
core:common → (nothing)
```

### 9.2. Repository Quick Lookup

| Need to... | Use Repository | Interface at | Implementation at |
|---|---|---|---|
| Login/Register | `AuthRepository` | `core:common` | `core:network` |
| CRUD Quizzes | `QuizRepository` | `core:common` | `core:network` |
| Create/Join Game | `GameSessionRepository` | `core:common` | `core:network` |
| Socket chung (connect/lobby) | `GameSocketRepository` | `core:common` | `core:network` |
| Player gameplay | `PlayerGameSocketRepository` | `core:common` | `core:network` |
| Host control | `HostGameSocketRepository` | `core:common` | `core:network` |
| Final result hand-off | `GameResultRepository` | `core:common` | `core:network` (in-memory) |
| Persist cookies | `CookieStore` | `core:common` | `core:database` |

> ℹ️ **UseCase cross-cutting**: `CheckAuthStateUseCase` (kiểm tra AuthState app-wide: FIRST_LAUNCH/GUEST_MODE/AUTHENTICATED) sống ở `core:datastore/usecase`. Không phải Repository nhưng theo cùng nguyên tắc: dùng bởi ≥2 nơi (Splash + tiềm năng các feature khác) → đưa xuống `core:*` thay vì đặt trong `:app` hoặc 1 feature cụ thể.

### 9.3. When to Create New Module?

**Create NEW feature module when:**
- ✅ New distinct user-facing feature (new screen/flow)
- ✅ Feature có thể bật/tắt độc lập
- ✅ Feature có thể được develop song song bởi team khác

**Add to EXISTING module when:**
- ❌ Chỉ là một biến thể UI của feature hiện có
- ❌ Tightly coupled với feature đã có
- ❌ Chia sẻ hầu hết ViewModels/UseCases

---

## 10. Code Review Checklist

Khi review code, kiểm tra các điểm sau:

### ✅ Architecture Compliance
- [ ] Feature modules KHÔNG phụ thuộc feature khác
- [ ] Domain layer KHÔNG import Android framework classes
- [ ] ViewModels gọi UseCases, KHÔNG gọi Repository trực tiếp
- [ ] Repository implementations ở đúng module (network/database/feature)
- [ ] Interface ở `core:common` nếu dùng bởi ≥2 features
- [ ] `:app` KHÔNG chứa use case cross-cutting mà nên đưa xuống `core:*`
- [ ] Package Kotlin của mỗi file trong `feature:<tên>` khớp namespace `android.kma.myquizzapp.feature.<tên>.*`

### ✅ Clean Code
- [ ] Screen có Stateful `XxxScreen` và stateless `XxxScreenContent`
- [ ] Content không lấy ViewModel/Hilt, collect Flow, sở hữu NavController hoặc thực hiện business I/O
- [ ] Platform/lifecycle/one-off effect nằm ở Stateful boundary
- [ ] Reusable component stateless mặc định; wrapper có state phải có stateless API lõi
- [ ] Preview gọi trực tiếp stateless content bằng fake state/no-op callbacks
- [ ] ViewModels follow MVI pattern (Intent → State)
- [ ] UseCases có single responsibility
- [ ] Error handling dùng `Result<T>` wrapper
- [ ] Strings dùng `UiText` sealed class (không hardcode)

### ✅ Performance
- [ ] Không recomposition không cần thiết (remember, derivedStateOf)
- [ ] Heavy operations chạy trong coroutine, không block main thread
- [ ] Images load với Coil (caching)
- [ ] Paging cho lists lớn

### ✅ Testing
- [ ] UseCases có unit tests
- [ ] ViewModels có unit tests với Turbine
- [ ] Key composables có UI tests

### ✅ Security
- [ ] Không log sensitive data (tokens, passwords)
- [ ] Cookie auth qua HTTPS only
- [ ] `Question` model KHÔNG chứa `correct_answer` field
- [ ] Input validation trước khi gửi server

---

## 11. Summary

MyQuizApp được xây dựng với **14 modules** theo **Clean Architecture + MVI**:

- **6 Core modules** cung cấp hạ tầng dùng chung (network, database, UI, common models)
- **8 Feature modules** implement các tính năng user-facing (auth, home, gameplay, quiz management, local preview)

### Key Architecture Decisions

1. **Multi-module Gradle**: Tách rõ concerns, parallel builds, reusability
2. **Cookie-based Auth**: Khác JWT Bearer - HttpOnly cookies auto-sent với mỗi request
3. **Dependency Inversion**: `CookieStore` interface ở `core:common`, impl ở `core:database`
4. **Role-aware Design**: Host và Player có repositories riêng vì payload Socket.IO khác nhau
5. **Server-authoritative**: Multiplayer chỉ render + send intent; N41 Preview là luồng local độc lập với điểm ước tính, không tạo/lưu trận server
6. **Cross-cutting use case ở core, không ở app**: `CheckAuthStateUseCase` sống ở `core:datastore` để mọi feature (không chỉ Splash) đều có thể dùng, tránh `:app` phụ thuộc domain của 1 feature
7. **Compose Stateful/Stateless baseline**: `XxxScreen` là integration boundary; `XxxScreenContent` là UI thuần; reusable component stateless mặc định

### Files to Read Next

- `quiz-app-android-design-document-v2.md` - Chi tiết API contracts, Socket events, game modes
- `myquizz-review-backend-ke-hoach-50-ngay.md` - Development roadmap, milestones, current status

---

**Document Version:** 2.23  
**Last Updated:** N46 WIP đến Search/Discover (source Android `d5571d6`)
**Status:** Living document — cập nhật thêm 7 commit sau Register: password recovery/validator/guards, component/Home/room/bottom nav, Search và Discover. Source snapshot fix Paging và internal-color đã commit; chưa có xác nhận retest sau sửa hoặc build/lint/test/CI mới được agent đối chiếu. N46 chưa hoàn tất, N44 vẫn PARTIAL, Google/cookie DEFERRED và backend/privacy/session/deployment gates giữ nguyên; chưa chốt N45/M6/release. Vẫn 14 module. Source/tests ở main, tài liệu ở docs; checkpoint/evidence trong N46_UI_PROGRESS.md, báo cáo N44 trong N44_E2E_REPORT.md.

---

*Tài liệu này là living document - cập nhật khi có thay đổi kiến trúc hoặc thêm modules mới.*
