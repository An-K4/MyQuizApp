> Đối chiếu với backend thực tế tại `github.com/Ntd1411/myquizz` (Express + TypeScript + [Socket.IO](http://Socket.IO) + PostgreSQL + Redis).
> Đây là bản viết lại của design doc v1.0, sửa toàn bộ phần hợp đồng API/Socket cho khớp với code backend thật (không còn là template quiz-app chung chung).
**Kotlin • Jetpack Compose • MVI + Clean Architecture • **[**Socket.IO**](http://Socket.IO)** • Retrofit + Cookie Auth**
---
## Mục lục
1. Tổng quan kiến trúc
2. Module Structure (Multi-module Gradle)
	- 2.1. Danh sách module
	- 2.2. Dependency Flow
	- 2.3. Cây thư mục chi tiết & vai trò từng folder
	- 2.4. Bảng tra cứu: Repository nào — interface ở đâu — impl ở đâu
3. Xác thực (Auth) — Cookie-based, không phải Bearer token
4. REST API Layer — đối chiếu route thật
5. [Socket.IO](http://Socket.IO) — luồng kết nối & bảng mapping event thật
6. Game Modes & State Machine
7. Domain Layer — Model, UseCase, Repository
8. Presentation Layer — MVI theo vai trò Host/Player
9. UI Layer — Compose theo loại câu hỏi
10. Local Storage — DataStore & Room
11. Navigation Architecture
12. Dependency Injection — Hilt Modules
13. Timer Synchronization & Reconnection
14. Error Handling — theo đúng envelope backend
15. Upload ảnh — Presigned S3
16. Testing Strategy
17. Dependencies — build.gradle.kts
18. Bảo mật & Anti-Cheat phía Client
19. Observability & Debug Tools
20. Checklist tổng kết
---
## 1. Tổng quan kiến trúc
Ứng dụng Android xây dựng theo **Clean Architecture kết hợp MVI**, đảm bảo Unidirectional Data Flow và khả năng test cao. Toàn bộ UI dùng **Jetpack Compose + Material 3**.
### 1.1. Nguyên tắc thiết kế cốt lõi
- **Unidirectional Data Flow**: Intent → State → UI, không có side-effect ngầm.
- **Single Source of Truth**: State quản lý tập trung tại ViewModel.
- **Separation of Concerns**: UI không biết logic nghiệp vụ.
- **Reactive**: UI subscribe `StateFlow`, tự re-render khi state đổi.
- **Server-authoritative**: mọi điểm số, thời gian, đáp án đúng đều do server quyết định — client chỉ hiển thị và gửi intent. Điều này bắt buộc vì backend tính điểm/thời gian hoàn toàn phía server (xem mục 6, 13).
- **Role-aware**: kiến trúc phải tách rõ **Host** và **Player**, vì backend dùng 2 room [Socket.IO](http://Socket.IO) khác nhau (`room` và `hostRoom`) với payload khác nhau — không dùng chung 1 `GameUiState` cho cả hai vai trò như bản v1.0.
### 1.2. Vì sao viết lại so với v1.0
Bản v1.0 giả định một backend "chat-app-style" tổng quát: JWT trả trong body, 1 luồng câu hỏi tuyến tính, event `question:start/locked/result`. Backend thật có 3 khác biệt lớn:
<table header-row="true">
<tr>
<td>Chủ đề</td>
<td>Doc v1.0 giả định</td>
<td>Thực tế backend</td>
<td></td>
</tr>
<tr>
<td>Auth</td>
<td>JWT trả trong response body, lưu DataStore, gắn header Bearer</td>
<td>**HttpOnly cookie** `accessToken`/`refreshToken`, không có token string trong body</td>
<td></td>
</tr>
<tr>
<td>Join room</td>
<td>`socket.emit('game:join', {roomCode, nickname})` trực tiếp</td>
<td>Phải gọi **REST trước** (`POST /games/:code/join`) để lấy `socketToken`, rồi mới connect [Socket.IO](http://Socket.IO) với `auth: {token}`</td>
<td></td>
</tr>
<tr>
<td>Luồng câu hỏi</td>
<td>1 state machine tuyến tính chung cho mọi người chơi</td>
<td>**5 game mode** (`classic`, `solo`, `survival`, `marathon`, `practice`) với \`pacing: host \\</td>
<td>self\` — self-paced *không bao giờ* có event câu hỏi chung phòng</td>
</tr>
<tr>
<td>UX Navigation</td>
<td>Tách AuthGraph/PlayerGraph/HostGraph riêng biệt theo trạng thái login</td>
<td>**Unified MainGraph** với bottom nav (Home/Discover/Join/Library/Profile) — browse-first pattern, optionalAuth gate tại composable level, align với `/quizzes/home` backend</td>
<td></td>
</tr>
</table>
---
## 2. Module Structure (Multi-module Gradle)
### 2.1. Danh sách module
```javascript
:app                  → entry point, DI graph, Navigation host
:core:network         → Retrofit, OkHttp (CookieJar), Socket.IO client
:core:database         → Room DB, DAOs, entities
:core:datastore        → DataStore preferences (không lưu token, xem mục 3)
:core:ui               → Design system, shared Composables, theme
:core:common            → Extensions, utils, base classes, Result/AppError

:feature:auth           → Login / Register / Google Sign-In
:feature:home            → Danh sách quiz, tạo phòng
:feature:lobby           → Phòng chờ (Host & Player)
:feature:game-player     → Gameplay phía Player (host-paced & self-paced)
:feature:game-host       → Màn hình điều khiển của Host (monitor + control)
:feature:leaderboard      → Bảng xếp hạng real-time + cuối game
:feature:quiz-manage      → CRUD quiz (chỉ Host, có login)
```
> 💡 So với v1.0: tách `:feature:game` thành `:feature:game-player` và `:feature:game-host` vì hai màn hình nhận payload Socket khác nhau hoàn toàn (host nhận `host:question`, `host:answer-received`, `host:player-progress`; player không bao giờ thấy các event này).
### 2.2. Dependency Flow
```javascript
Composable (UI Layer)
 ↓ sends Intent
ViewModel (Presentation) ←── StateFlow / UiState
 ↓ calls
UseCase (Domain Layer)
 ↓ calls
Repository interface
 ↓ implemented by
RepositoryImpl (Data Layer) ←── RemoteDataSource / SocketDataSource / LocalDataSource
 ↓
Retrofit API (cookie auth) / Socket.IO (JWT socketToken) / Room DAO
```
### 2.3. Cây thư mục chi tiết & vai trò từng folder
Cây thư mục dưới đây triển khai cụ thể 12 module ở mục 2 thành cấu trúc Gradle thật. Nguyên tắc đặt tên: mỗi `feature` module có tối đa 3 tầng con `presentation / domain / data`, mirror đúng Dependency Flow ở trên — giúp khi mở bất kỳ module nào cũng biết ngay code nào thuộc layer nào mà không cần đọc hết file.
> 🔑 **Ký hiệu dùng trong cây bên dưới**: `🟦 interface` = Repository **interface** (thuộc domain layer, thuần Kotlin, không import lib cụ thể) · `🟩 impl` = Repository **implementation** (thuộc data layer, được `@Binds` sang interface tương ứng qua Hilt).
```javascript
myquizz-android/
├── app/                                    # Entry point — KHÔNG chứa business logic
│   ├── src/main/java/.../
│   │   ├── MainActivity.kt                 # Compose host, gắn NavHost gốc
│   │   ├── QuizApp.kt                      # @HiltAndroidApp
│   │   └── navigation/
│   │       └── AppNavGraph.kt              # Ghép các NavGraph con từ từng :feature
│   └── build.gradle.kts                    # Chỉ module có applicationId, phụ thuộc TẤT CẢ module khác
│
├── core/
│   ├── network/                            # Hạ tầng gọi mạng dùng chung toàn app — nơi đặt IMPL của các repo dùng chung
│   │   └── src/main/java/.../network/
│   │       ├── di/
│   │       │   ├── NetworkModule.kt        # Provide OkHttpClient, Retrofit (mục 12) — inject CookieStore qua interface
│   │       │   └── NetworkBindingModule.kt # @Binds AuthRepositoryImpl/QuizRepositoryImpl/GameSessionRepositoryImpl (mục 12.2)
│   │       ├── cookie/
│   │       │   ├── PersistentCookieJar.kt  # implements okhttp3.CookieJar, chỉ gọi CookieStore (interface, mục 3, 10)
│   │       │   └── TokenAuthenticator.kt   # Xử lý 401 → /auth/refresh → retry (mục 3.2)
│   │       ├── api/
│   │       │   ├── AuthApiService.kt       # /auth/*
│   │       │   ├── UserApiService.kt       # /users/*
│   │       │   ├── QuizApiService.kt       # /quizzes/*
│   │       │   ├── GameApiService.kt       # /games/*
│   │       │   └── StorageApiService.kt    # /storage/presign
│   │       ├── repository/                 # 🟩 IMPL — hiện thực các interface khai báo ở core:common/repository
│   │       │   ├── AuthRepositoryImpl.kt       # 🟩 implements AuthRepository — dùng AuthApiService, UserApiService
│   │       │   ├── QuizRepositoryImpl.kt       # 🟩 implements QuizRepository — dùng QuizApiService
│   │       │   └── GameSessionRepositoryImpl.kt # 🟩 implements GameSessionRepository — dùng GameApiService
│   │       ├── socket/
│   │       │   ├── SocketFactory.kt        # (socketToken) -> Socket (mục 5.1, 12)
│   │       │   └── GameEventMapper.kt      # parse JSONObject của socket.on() → GameEvent (model ở core:common)
│   │       └── dto/
│   │           ├── ApiEnvelope.kt          # {success, data, error, meta} — mục 4.6
│   │           └── ApiError.kt
│   │       # ⚠️ core:network KHÔNG import core:database. PersistentCookieJar chỉ biết
│   │       #    interface CookieStore (định nghĩa ở core:common) — không biết Room tồn tại.
│   │       # ✅ core:network TỰ bind interface → impl cho Auth/Quiz/GameSession trong chính module này
│   │       #    (interface và impl khác thư mục con nhưng CÙNG module Gradle → không vi phạm bảng dependency).
│   │
│   ├── database/                           # Room — cache, lịch sử offline & lưu trữ bền cho CookieStore
│   │   └── src/main/java/.../database/
│   │       ├── AppDatabase.kt
│   │       ├── dao/
│   │       │   ├── QuizCacheDao.kt
│   │       │   ├── GameHistoryDao.kt
│   │       │   └── CookieDao.kt            # bảng lưu cookie, chỉ dùng nội bộ module này
│   │       ├── entity/
│   │       │   ├── CachedQuizEntity.kt
│   │       │   ├── GameHistoryEntity.kt
│   │       │   └── CookieEntity.kt         # mục 10.2
│   │       ├── cookie/
│   │       │   └── RoomCookieStore.kt      # 🟩 implements CookieStore (interface ở core:common, mục 3.2)
│   │       └── di/
│   │           └── DatabaseBindingModule.kt # @Binds RoomCookieStore -> CookieStore (Hilt, xem mục 12.1)
│   │       # core:database phụ thuộc core:common để "thấy" interface CookieStore rồi tự implement + tự bind.
│   │       # core:network KHÔNG cần biết RoomCookieStore tồn tại — chỉ nhận CookieStore qua constructor injection.
│   │       # → CookieStore là repo interface DUY NHẤT có interface và impl nằm ở 2 module KHÁC NHAU
│   │       #   (vì impl bắt buộc cần Room). Mọi repo khác trong dự án này: interface ở core:common,
│   │       #   impl ở CÙNG module với nơi hiện thực (core:network, hoặc trong chính feature nếu impl chỉ 1 feature dùng).
│   │
│   ├── datastore/                          # DataStore Preferences — dữ liệu nhẹ, không phải token
│   │   └── src/main/java/.../datastore/
│   │       ├── PreferenceKeys.kt           # NICKNAME, LAST_ROOM_CODE, GUEST_ID... (mục 3.4, 10.1)
│   │       └── UserPreferences.kt
│   │
│   ├── ui/                                 # Design system dùng chung mọi feature
│   │   └── src/main/java/.../ui/
│   │       ├── theme/                      # Color.kt, Typography.kt, Theme.kt (Material 3)
│   │       ├── components/                 # QuizButton, QuizTextField, CountdownTimer...
│   │       └── UiText.kt                   # sealed class localize message (mục 8)
│   │
│   └── common/                             # Extensions, model thuần & TOÀN BỘ Repository INTERFACE dùng liên feature
│       └── src/main/java/.../common/
│           ├── result/
│           │   ├── Result.kt               # sealed class Success/Error dùng xuyên toàn app
│           │   └── AppError.kt             # Http / Socket / NetworkError (mục 14)
│           ├── cookie/
│           │   ├── CookieStore.kt          # 🟦 interface — hợp đồng lưu trữ, không biết Room/OkHttp cụ thể
│           │   └── StoredCookie.kt         # data class thuần: host, name, value, expiresAt, secure, httpOnly
│           ├── model/                      # domain model thuần Kotlin, dùng chung bởi mọi repo interface bên dưới
│           │   ├── User.kt, Quiz.kt, GameSession.kt, GameModeSpec.kt
│           │   ├── Player.kt, PlayerScore.kt, GameResults.kt
│           │   └── GameEvent.kt            # sealed class toàn bộ event server→client (mục 5.3), AnswerAck (mục 5.4)
│           ├── repository/                 # 🟦 TOÀN BỘ Repository INTERFACE của app nằm ở đây — xem bảng bên dưới
│           │   ├── AuthRepository.kt       # 🟦 — impl: core:network (dùng bởi feature:auth, và :app cho Splash)
│           │   ├── QuizRepository.kt       # 🟦 — impl: core:network (dùng bởi feature:home + feature:quiz-manage)
│           │   └── GameSessionRepository.kt # 🟦 — impl: core:network (dùng bởi feature:lobby + feature:quiz-manage + feature:leaderboard)
│           └── ext/                        # Instant, Flow extension dùng chung
│           # core:network implement PersistentCookieJar dựa trên CookieStore này (adapter sang okhttp3.Cookie).
│           # core:database implement RoomCookieStore dựa trên CookieStore này (adapter sang CookieEntity).
│           # → CookieStore: 2 module impl không biết nhau, chỉ cùng phụ thuộc 1 interface chung ở core:common.
│           # → 3 repository còn lại: interface đặt đây VÌ được ≥ 2 feature module dùng chung (feature không được
│           #   phụ thuộc feature khác — xem bảng dependency), nên phải "nâng" interface lên core:common để mọi
│           #   feature cùng thấy được, mà không cần thấy nhau.
│
├── feature/
│   ├── auth/                               # Login / Register / Google Sign-In (mục 3.3)
│   │   └── src/main/java/.../feature/auth/
│   │       ├── presentation/
│   │       │   ├── login/LoginScreen.kt, LoginViewModel.kt
│   │       │   └── register/RegisterScreen.kt, RegisterViewModel.kt
│   │       └── domain/
│   │           └── usecase/ LoginUseCase.kt, LoginWithGoogleUseCase.kt, GetCurrentUserUseCase.kt
│   │       # KHÔNG có domain/repository hay data/repository ở đây nữa — UseCase inject thẳng
│   │       # AuthRepository (🟦 interface từ core:common), Hilt tự nối sang AuthRepositoryImpl (🟩 core:network).
│   │
│   ├── home/                               # Danh sách quiz công khai + quiz của tôi, entry tạo phòng
│   │   └── .../feature/home/
│   │       ├── presentation/HomeScreen.kt, HomeViewModel.kt
│   │       └── domain/usecase/ SearchQuizzesUseCase.kt, GetMyQuizzesUseCase.kt
│   │       # UseCase inject QuizRepository (🟦 core:common) — KHÔNG tự định nghĩa hay implement lại.
│   │
│   ├── lobby/                              # Phòng chờ — dùng chung UI cho cả Host & Player, khác ViewModel
│   │   └── .../feature/lobby/
│   │       ├── presentation/
│   │       │   ├── player/PlayerLobbyScreen.kt, PlayerLobbyViewModel.kt
│   │       │   └── host/HostLobbyScreen.kt, HostLobbyViewModel.kt   # có UpdateConfig intent
│   │       └── domain/usecase/ JoinGameAsPlayerUseCase.kt, GetHostSocketTokenUseCase.kt, LookupRoomUseCase.kt
│   │       # 3 UseCase trên đều inject GameSessionRepository (🟦 core:common).
│   │
│   ├── game-player/                        # Gameplay phía Player — cả host-paced & self-paced (mục 6, 9)
│   │   └── .../feature/game_player/
│   │       ├── presentation/
│   │       │   ├── GamePlayScreen.kt
│   │       │   ├── GameViewModel.kt        # GamePhaseUi state machine (mục 6.2)
│   │       │   └── components/
│   │       │       ├── AnswerInput.kt      # dispatcher theo question_type (mục 9)
│   │       │       ├── SingleChoiceGrid.kt, MultiSelectGrid.kt
│   │       │       ├── ShortTextField.kt, LongTextField.kt
│   │       │       └── CountdownTimer.kt
│   │       ├── domain/
│   │       │   ├── repository/
│   │       │   │   └── PlayerGameSocketRepository.kt  # 🟦 interface RIÊNG của feature này — connect, submitAnswer, requestNext...
│   │       │   └── usecase/ SubmitAnswerUseCase.kt, RequestNextQuestionUseCase.kt
│   │       └── data/
│   │           └── repository/
│   │               └── PlayerGameSocketRepositoryImpl.kt # 🟩 impl — dùng SocketFactory + GameEventMapper (core:network)
│   │       # Đây là repo KHÔNG nâng lên core:common vì chỉ 1 feature dùng — giữ đúng nguyên tắc domain/data cục bộ.
│   │
│   ├── game-host/                          # Màn điều khiển của Host — payload khác hẳn Player (mục 5.3, 6.3)
│   │   └── .../feature/game_host/
│   │       ├── presentation/
│   │       │   ├── HostGameScreen.kt
│   │       │   └── HostGameViewModel.kt    # HostGameUiState riêng, không share với GameViewModel
│   │       ├── domain/
│   │       │   ├── repository/
│   │       │   │   └── HostGameSocketRepository.kt    # 🟦 interface RIÊNG — hostAction, updateConfig, nhận host:*-event
│   │       │   └── usecase/ HostControlUseCase.kt, UpdateRoomConfigUseCase.kt
│   │       └── data/
│   │           └── repository/
│   │               └── HostGameSocketRepositoryImpl.kt # 🟩 impl — cũng dùng chung SocketFactory + GameEventMapper (core:network)
│   │       # game-player và game-host KHÔNG dùng chung 1 repo interface (dù cùng nói chuyện 1 phòng Socket)
│   │       # vì 2 feature không được phụ thuộc nhau — mỗi bên tự định nghĩa hợp đồng theo đúng nhu cầu vai trò.
│   │
│   ├── leaderboard/                        # Bảng xếp hạng live + màn kết quả cuối
│   │   └── .../feature/leaderboard/
│   │       ├── presentation/
│   │       │   ├── FinalResultScreen.kt
│   │       │   └── LeaderboardViewModel.kt
│   │       └── domain/usecase/ GetLeaderboardUseCase.kt, GetResultsUseCase.kt
│   │       # inject GameSessionRepository (🟦 core:common) — REST fallback, không cần Socket ở màn này.
│   │
│   └── quiz-manage/                        # CRUD quiz — chỉ Host, cần login
│       └── .../feature/quiz_manage/
│           ├── presentation/
│           │   ├── QuizManageScreen.kt, QuizEditorScreen.kt
│           │   └── CreateRoomScreen.kt     # đọc GetGameModesUseCase để build UI động (mục 4.4, 6.1)
│           └── domain/usecase/ CreateQuizUseCase.kt, UpdateQuizUseCase.kt, UploadImageUseCase.kt (mục 15)
│           # CreateQuizUseCase/UpdateQuizUseCase inject QuizRepository, CreateRoomScreen's UseCase inject
│           # GameSessionRepository — CẢ HAI đều là 🟦 interface từ core:common, CÙNG instance impl với feature:home/lobby.
│
└── build-logic/                            # (tuỳ chọn) convention plugins dùng chung version catalog
    └── convention/  AndroidFeatureConventionPlugin.kt, HiltConventionPlugin.kt
```
### 2.4. Bảng tra cứu nhanh: Repository nào — interface ở đâu — impl ở đâu
<table header-row="true">
<tr>
<td>Repository</td>
<td>🟦 Interface đặt tại</td>
<td>🟩 Impl đặt tại</td>
<td>Feature nào dùng</td>
</tr>
<tr>
<td>`CookieStore`</td>
<td>`core:common/cookie`</td>
<td>`core:database/cookie` (`RoomCookieStore`)</td>
<td>Không feature nào dùng trực tiếp — chỉ `core:network` (qua `PersistentCookieJar`)</td>
</tr>
<tr>
<td>`AuthRepository`</td>
<td>`core:common/repository`</td>
<td>`core:network/repository` (`AuthRepositoryImpl`)</td>
<td>`feature:auth`; `:app` (check login lúc Splash)</td>
</tr>
<tr>
<td>`QuizRepository`</td>
<td>`core:common/repository`</td>
<td>`core:network/repository` (`QuizRepositoryImpl`)</td>
<td>`feature:home` (search/browse), `feature:quiz-manage` (CRUD)</td>
</tr>
<tr>
<td>`GameSessionRepository`</td>
<td>`core:common/repository`</td>
<td>`core:network/repository` (`GameSessionRepositoryImpl`)</td>
<td>`feature:lobby` (join/host-token), `feature:quiz-manage` (createSession/getGameModes), `feature:leaderboard` (leaderboard/results REST)</td>
</tr>
<tr>
<td>`PlayerGameSocketRepository`</td>
<td>`feature:game-player/domain/repository` (cục bộ)</td>
<td>`feature:game-player/data/repository`</td>
<td>Chỉ `feature:game-player`</td>
</tr>
<tr>
<td>`HostGameSocketRepository`</td>
<td>`feature:game-host/domain/repository` (cục bộ)</td>
<td>`feature:game-host/data/repository`</td>
<td>Chỉ `feature:game-host`</td>
</tr>
</table>
**Quy tắc chọn nơi đặt interface** (áp dụng khi thêm repository mới về sau):
1. Repository chỉ được **1 feature module** dùng → đặt interface ở `feature:<đó>/domain/repository`, impl ở `feature:<đó>/data/repository` (mẫu `PlayerGameSocketRepository`).
2. Repository được **≥ 2 feature module** dùng → bắt buộc "nâng" interface lên `core:common/repository` (vì feature không được phụ thuộc feature khác), impl đặt ở module `core:*` nào sở hữu hạ tầng cần thiết — thường là `core:network` (mẫu `QuizRepository`), đôi khi là `core:database` nếu cần Room (mẫu `CookieStore`).
3. Interface **luôn ở module có ít quyền phụ thuộc nhất có thể thấy được bởi mọi nơi cần dùng nó**; impl **luôn ở module sở hữu công nghệ cụ thể** (Retrofit → `core:network`, Room → `core:database`, [Socket.IO](http://Socket.IO) → `core:network` hoặc riêng từng feature nếu hợp đồng khác nhau theo vai trò).
**Nguyên tắc phụ thuộc giữa các thư mục** (áp cho toàn bộ cây trên, khớp Dependency Flow ở 2.1 — không đổi so với bản trước):
<table header-row="true">
<tr>
<td>Thư mục</td>
<td>Được phép phụ thuộc</td>
<td>Không được phụ thuộc</td>
</tr>
<tr>
<td>`app`</td>
<td>mọi `:core:*` và `:feature:*`</td>
<td>—</td>
</tr>
<tr>
<td>`feature:*`</td>
<td>`core:network`, `core:database`, `core:datastore`, `core:ui`, `core:common`</td>
<td>các `feature:*` khác (nếu 2 feature cần chia sẻ, đưa phần chung xuống `core`)</td>
</tr>
<tr>
<td>`core:network`, `core:database`, `core:datastore`</td>
<td>`core:common`</td>
<td>`core:ui`, bất kỳ `feature:*` nào</td>
</tr>
<tr>
<td>`core:ui`</td>
<td>`core:common`</td>
<td>`core:network`, `core:database` (không được biết tới tầng data)</td>
</tr>
<tr>
<td>`core:common`</td>
<td>không phụ thuộc module nào khác trong dự án</td>
<td>mọi module còn lại</td>
</tr>
</table>
> 💡 Vì `feature:game-player` và `feature:game-host` cùng nói chuyện với chung 1 phòng Socket, nhưng **không được phép phụ thuộc lẫn nhau** — phần dùng chung (parse `GameEvent`, `GameConfig`, decode payload) đặt ở `core:network/socket` + model ở `core:common/model` dưới dạng thuần dữ liệu, mỗi feature tự định nghĩa Repository interface riêng (`PlayerGameSocketRepository` / `HostGameSocketRepository`) rồi build `UiState` riêng theo vai trò (đúng tinh thần mục 6.3 và bảng 2.4).
>
> ⚠️ **Trường hợp cần lưu ý nhất trong bảng trên**: `core:network` cần lưu cookie bền (`PersistentCookieJar`) và nơi lưu hợp lý nhất là Room — nhưng bảng cấm `core:network` phụ thuộc `core:database`. Cách giải quyết đúng là **Dependency Inversion**: interface `CookieStore` đặt ở `core:common` (cả hai module đều được phép phụ thuộc `core:common`), `core:network` chỉ dùng interface đó, còn `core:database` mới là nơi viết `RoomCookieStore` implement nó và tự `@Binds` ngay trong module của mình. Nhờ vậy bảng dependency ở trên **không cần sửa** — chi tiết implement xem mục 3.2 và 12.1.
---
## 3. Xác thực (Auth) — Cookie-based, không phải Bearer token
### 3.1. Thực tế backend
`auth.middleware.ts` chỉ đọc `req.cookies.accessToken`, không chấp nhận `Authorization` header:
```typescript
const token = req.cookies.accessToken as string | undefined
if (!token) throw new AppError(401, 'Access token missing')
```
`POST /auth/login`, `/auth/register`, `/auth/refresh` **set cookie HttpOnly** `accessToken` + `refreshToken`, body response chỉ trả `{ user }`. `POST /auth/logout` revoke + clear cookie. `POST /auth/google/one-tap` nhận `{ credential }` (ID token từ Google) và cũng set cookie.
### 3.2. Hệ quả thiết kế cho Android
`AuthInterceptor` tự đính Bearer token **không dùng được**. Thay vào đó:
- Dùng **`OkHttpClient.cookieJar`** với một `CookieJar` có khả năng **persist** (không có sẵn trong OkHttp — phải tự implement).
- HttpOnly **chỉ chặn JavaScript đọc cookie**, không ảnh hưởng OkHttp (native HTTP client), nên cookie vẫn hoạt động bình thường trên Android.
- Cookie được lưu bền trong **Room** (bảng `cookie_store`, mục 10.2), không phải DataStore — vì cookie có nhiều field cấu trúc (`domain`, `path`, `expiresAt`, `secure`, `httpOnly`) và cần query theo host, hợp với DAO hơn là key-value đơn của DataStore.
- **`core:network`**** không được phép phụ thuộc ****`core:database`** (theo bảng dependency ở mục 2.3) nên `PersistentCookieJar` **không** biết Room tồn tại — nó chỉ nói chuyện qua interface `CookieStore` (định nghĩa ở `core:common`). Implementation thật `RoomCookieStore` nằm ở `core:database`, tự `@Binds` sang `CookieStore` ngay trong module đó (xem mục 12.1) — không cần một module trung gian nào "thấy cả hai".
- Khi 401 trả về (access token hết hạn) → gọi `POST /auth/refresh` (cookie refresh token tự động gửi kèm) → cookie mới được set → **retry request gốc**. Cài đặt qua `Authenticator` của OkHttp, không phải Interceptor.
```kotlin
// core:common — hợp đồng thuần Kotlin, không import okhttp3 lẫn Room
interface CookieStore {
    suspend fun loadForHost(host: String): List<StoredCookie>
    suspend fun saveAll(host: String, cookies: List<StoredCookie>)
}

data class StoredCookie(
    val name: String, val value: String, val domain: String,
    val path: String, val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean
)
```
```kotlin
// core:network — chỉ biết interface, KHÔNG import core:database
class PersistentCookieJar @Inject constructor(
    private val cookieStore: CookieStore // interface, impl thật đến từ đâu network không quan tâm
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.saveAll(url.host, cookies.map { it.toStoredCookie() }) // map sang StoredCookie
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        runBlocking { cookieStore.loadForHost(url.host).map { it.toOkHttpCookie(url.host) } }
}

class TokenAuthenticator @Inject constructor(
    private val authApi: Lazy<AuthApiService> // Lazy để tránh vòng lặp DI
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null // tránh retry vô hạn
        val refreshed = runBlocking { authApi.get().refresh() } // cookie tự gửi kèm
        return if (refreshed.isSuccessful) response.request else null
    }
}
```
```kotlin
// core:database — implement interface của core:common, KHÔNG được core:network biết tới
class RoomCookieStore @Inject constructor(
    private val cookieDao: CookieDao
) : CookieStore {
    override suspend fun loadForHost(host: String) =
        cookieDao.findByHost(host).map { it.toStoredCookie() }
    override suspend fun saveAll(host: String, cookies: List<StoredCookie>) =
        cookieDao.upsertAll(cookies.map { it.toEntity(host) })
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {
    @Binds
    @Singleton
    abstract fun bindCookieStore(impl: RoomCookieStore): CookieStore
}
```
> 💡 Vì Hilt gộp toàn bộ Dagger component ở tầng `:app` (nơi cả `core:network` lẫn `core:database` đều có mặt trên classpath), `@Binds` khai báo ngay trong `core:database` vẫn được Hilt nhìn thấy khi build `:app` — **không cần** tạo thêm một module trung gian nào để "thấy cả hai module" như cách 2 ở trên gợi ý. Đây chính là ứng dụng Dependency Inversion Principle: tầng thấp (`core:network`) định nghĩa "mình cần gì" qua interface, tầng cao hơn (`core:database`) mới là nơi biết cách hiện thực interface đó.
### 3.3. Google Sign-In trên Android
Backend expose 2 kiểu: `GET /auth/google` (redirect — dành cho web) và `POST /auth/google/one-tap` (nhận `credential` — **đây là kiểu Android phải dùng**).
Luồng đúng cho Android:
1. Dùng **Credential Manager API** (`androidx.credentials`) hoặc Google Identity Services để lấy Google ID Token.
2. `POST /auth/google/one-tap` với `{ credential: idToken }`.
3. Server set cookie như login thường → `CookieJar` tự lưu.
> ⚠️ Không dùng WebView redirect flow (`/auth/google`) trên Android — flow đó thiết kế cho browser redirect về `FRONTEND_URL/auth/callback`, không phù hợp app native.
### 3.4. `UserPreferences` (DataStore) — chỉ lưu thông tin hiển thị, KHÔNG lưu token
```kotlin
object PreferenceKeys {
    val NICKNAME = stringPreferencesKey("nickname")
    val LAST_ROOM_CODE = stringPreferencesKey("last_room_code")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in") // suy ra từ việc gọi GET /users/me thành công
    val GUEST_ID = stringPreferencesKey("guest_id") // uuid, dùng cho player_guest_id khi join không login
}
```
Trạng thái đăng nhập được xác thực bằng cách gọi `GET /users/me` lúc khởi động app (nếu cookie hết hạn/không có → 401 → điều hướng Login/Guest), **không suy đoán qua việc "có token trong local storage" như v1.0**.
---
## 4. REST API Layer — đối chiếu route thật
### 4.1. Auth (`/auth`)
<table header-row="true">
<tr>
<td>Method</td>
<td>Path</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>POST</td>
<td>`/auth/register`</td>
<td>body: `email, password, fullname, phone?`</td>
</tr>
<tr>
<td>POST</td>
<td>`/auth/login`</td>
<td>body: `email, password`</td>
</tr>
<tr>
<td>POST</td>
<td>`/auth/refresh`</td>
<td>dùng cookie refresh token</td>
</tr>
<tr>
<td>POST</td>
<td>`/auth/logout`</td>
<td>cần đã login</td>
</tr>
<tr>
<td>POST</td>
<td>`/auth/google/one-tap`</td>
<td>body: `{ credential }` — dùng cho Android</td>
</tr>
</table>
### 4.2. User (`/users`)
<table header-row="true">
<tr>
<td>Method</td>
<td>Path</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>GET</td>
<td>`/users/me`</td>
<td>lấy profile hiện tại, dùng để check login state</td>
</tr>
<tr>
<td>PATCH</td>
<td>`/users/me`</td>
<td>update profile</td>
</tr>
<tr>
<td>PATCH</td>
<td>`/users/me/password`</td>
<td>đổi mật khẩu</td>
</tr>
<tr>
<td>PATCH</td>
<td>`/users/me/avatar`</td>
<td>upload avatar (multipart)</td>
</tr>
<tr>
<td>DELETE</td>
<td>`/users/me`</td>
<td>deactivate account</td>
</tr>
<tr>
<td>GET</td>
<td>`/users/:userId`</td>
<td>xem profile công khai</td>
</tr>
<tr>
<td>POST</td>
<td>`/users/forgot-password`</td>
<td>Bước 1: gửi OTP + link về email. Body: `{email}`. Trả `{resetTime, expiresAt}`</td>
</tr>
<tr>
<td>POST</td>
<td>`/users/password-reset/verify`</td>
<td>Bước 2: đổi OTP hoặc token lấy ticket. Body: `{email, otp}` **hoặc** `{token}`. Trả `{ticket, expiresAt, email (masked)}`</td>
</tr>
<tr>
<td>GET</td>
<td>`/users/password-reset/ticket?ticket=...`</td>
<td>Xem trạng thái ticket (không tiêu ticket) — trả `{email (masked), expiresAt}`, dùng để quyết định render form trước khi user nhập mật khẩu mới</td>
</tr>
<tr>
<td>POST</td>
<td>`/users/password-reset/complete`</td>
<td>Bước 3, nơi duy nhất ghi mật khẩu: body `{ticket, newPassword}`. Revoke toàn bộ refresh token của user sau khi đổi thành công</td>
</tr>
</table>

> 🔴 **Sai lệch nghiêm trọng phát hiện 24/8**: Luồng "Quên mật khẩu" đã code xong ở N15 (15/8) gọi 3 endpoint không khớp backend thật: `/users/reset-password-token` và `/users/reset-password` **không tồn tại**. Backend thật dùng flow ticket 3 bước như bảng trên (`verify` → `ticket` peek → `complete`), không có khái niệm "reset bằng token trực tiếp" hay "reset kèm OTP trực tiếp" như 2 DTO `ResetPasswordRequest`/`ResetPasswordWithOtpRequest` đã viết. Cần sửa `core:network` (API service + DTO) và `feature:auth` (ViewModel/UseCase reset) theo đúng 4 endpoint trên — việc cụ thể xem kế hoạch 50 ngày, mục N16.5 (Tuần 4).
### 4.3. Quiz (`/quizzes`)
<table header-row="true">
<tr>
<td>Method</td>
<td>Path</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>POST</td>
<td>`/quizzes`</td>
<td>tạo quiz (cần login)</td>
</tr>
<tr>
<td>GET</td>
<td>`/quizzes/search`</td>
<td>tìm quiz công khai — **không cần login** (`optionalAuthMiddleware`)</td>
</tr>
<tr>
<td>GET</td>
<td>`/quizzes/users/id/:ownerId`</td>
<td>danh sách quiz theo chủ sở hữu</td>
</tr>
<tr>
<td>GET</td>
<td>`/quizzes/id/:quizId`</td>
<td>chi tiết quiz</td>
</tr>
<tr>
<td>PATCH</td>
<td>`/quizzes/id/:quizId`</td>
<td>update (cần là owner)</td>
</tr>
<tr>
<td>DELETE</td>
<td>`/quizzes/id/:quizId`</td>
<td>xóa mềm</td>
</tr>
</table>
> So với v1.0: không có 1 `GetQuizListUseCase(userId)` gộp chung — phải tách **`SearchQuizzesUseCase`** (khám phá quiz công khai, không cần login) và **`GetQuizzesByOwnerUseCase`** (quiz của chính host).
### 4.4. Game (`/games`)
<table header-row="true">
<tr>
<td>Method</td>
<td>Path</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>GET</td>
<td>`/games/game-modes`</td>
<td>danh sách mode khả dụng + config spec — **nên gọi để build màn hình CreateRoom dynamic thay vì hardcode 5 mode**</td>
</tr>
<tr>
<td>POST</td>
<td>`/games`</td>
<td>tạo phòng, cần login (Host). body: `quiz_id, session_name, mode, config?`</td>
</tr>
<tr>
<td>GET</td>
<td>`/games/:code`</td>
<td>tra cứu phòng theo mã (JoinRoomScreen dùng để validate trước khi join)</td>
</tr>
<tr>
<td>POST</td>
<td>`/games/:id/host-token`</td>
<td>Host lấy `socketToken` để connect [Socket.IO](http://Socket.IO), cần login</td>
</tr>
<tr>
<td>POST</td>
<td>`/games/:code/join`</td>
<td>Player join (guest hoặc login), trả về `{ player, socketToken }`</td>
</tr>
<tr>
<td>GET</td>
<td>`/games/:id/leaderboard`</td>
<td>leaderboard (REST — dùng khi vào lại màn hình mà chưa kịp nhận socket)</td>
</tr>
<tr>
<td>GET</td>
<td>`/games/:id/results`</td>
<td>kết quả cuối</td>
</tr>
<tr>
<td>PATCH</td>
<td>`/games/:id/config`</td>
<td>cập nhật config phòng qua REST (song song với `lobby:config-update` qua Socket — dùng REST khi Host chỉnh config *trước* khi vào lobby socket, ví dụ ngay ở CreateRoomScreen)</td>
</tr>
</table>
### 4.5. Storage (`/storage`)
<table header-row="true">
<tr>
<td>Method</td>
<td>Path</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>POST</td>
<td>`/storage/presign`</td>
<td>xin presigned URL để upload ảnh thẳng lên S3 (xem mục 15)</td>
</tr>
</table>
### 4.6. Response envelope thật (áp dụng cho toàn bộ REST)
```json
// success
{ "success": true, "data": { ... }, "error": null, "meta": { "timestamp": "..." } }

// fail — CHỈ có "code", không có "message"/"details" trên wire (response.ts: hàm fail() chỉ nhận ErrorCode)
{ "success": false, "data": null, "error": { "code": "VALIDATION_ERROR" }, "meta": { "timestamp": "..." } }
```
→ DTO wrapper phía Android:
```kotlin
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(val code: String)
```
> ⚠️ **Sửa 24/8**: bản trước giả định `error: {message, details}` — sai. Backend cố tình không trả message tiếng Anh cho client (xem comment `response.ts`: "No message and no field dump... The reason is logged instead, where developers can read it and users cannot"): mọi lỗi chỉ có `code` (string, ví dụ `VALIDATION_ERROR`, `RESET_TICKET_INVALID`, `SERVER_ERROR`, `FILE_TOO_LARGE`, `UNAUTHORIZED`...). Android phải tự map `code` → chuỗi hiển thị tiếng Việt (`UiText`/string resource theo code), không hiển thị `code` thô cho người dùng.

Retrofit nên dùng 1 `CallAdapter` custom để unwrap `envelope` thành `Result<T, AppError>` domain, tránh mỗi Repository tự parse `success/data/error`.
---
## 5. [Socket.IO](http://Socket.IO) — luồng kết nối & bảng mapping event thật
### 5.1. Luồng kết nối đúng (khác hoàn toàn v1.0)
```javascript
1. Player: POST /games/{code}/join  → { player, socketToken }
   Host:   POST /games/{id}/host-token → { socketToken }
2. Khởi tạo Socket.IO:
   IO.socket(SOCKET_URL, IO.Options().apply {
       auth = mapOf("token" to socketToken)
   })
3. socket.connect()
4. socket.emit("lobby:join")   // vào room, KHÔNG kèm roomCode/nickname — role/gameId đã nằm trong JWT token
```
Server verify `socketToken` (JWT ký bằng `SOCKET_JWT_SECRET`, payload `{ psid, gsid, code, role }`) ngay ở bước `handshake` (`socketAuth` middleware) — nếu thiếu hoặc sai sẽ bị `next(new Error('UNAUTHORIZED...'))` và không connect được. **Không hề có bước emit ****`game:join`**** kèm ****`roomCode + nickname`**** như v1.0 mô tả.**
```kotlin
class SocketGameDataSource @Inject constructor(
    private val socketFactory: (String) -> Socket // nhận socketToken
) {
    private var socket: Socket? = null

    fun connect(socketToken: String): Flow<GameEvent> = callbackFlow {
        val s = socketFactory(socketToken)
        registerListeners(s, ::trySend)
        s.connect()
        socket = s
        awaitClose { s.off(); s.disconnect() }
    }

    fun joinLobby() = socket?.emit("lobby:join")
    fun leaveLobby() = socket?.emit("lobby:leave")

    // các event có ACK — dùng callback, không phải fire-and-forget
    fun submitAnswer(answer: Any, onAck: (AnswerAck) -> Unit) {
        socket?.emit("question:answer", JSONObject(mapOf("answer" to answer)), Ack { args ->
            onAck(parseAnswerAck(args))
        })
    }
}
```
### 5.2. Bảng event **client → server** (đầy đủ, đối chiếu `game.socket.ts`)
<table header-row="true">
<tr>
<td>Event</td>
<td>Vai trò</td>
<td>Có ACK?</td>
<td>Ghi chú</td>
</tr>
<tr>
<td>`lobby:join`</td>
<td>cả 2</td>
<td>không</td>
<td>vào room sau khi connect</td>
</tr>
<tr>
<td>`lobby:leave`</td>
<td>cả 2</td>
<td>không</td>
<td></td>
</tr>
<tr>
<td>`lobby:config-update`</td>
<td>Host</td>
<td>**có** — trả `{ok, changed, config, ignored}`</td>
<td>Host chỉnh config trước khi start</td>
</tr>
<tr>
<td>`game:start`</td>
<td>Host</td>
<td>không</td>
<td></td>
</tr>
<tr>
<td>`game:next`</td>
<td>Host</td>
<td>không</td>
<td>chuyển câu hỏi thủ công (khi `autoAdvance=false`)</td>
</tr>
<tr>
<td>`game:pause` / `game:resume`</td>
<td>Host</td>
<td>không</td>
<td></td>
</tr>
<tr>
<td>`game:end`</td>
<td>Host</td>
<td>không</td>
<td>kết thúc sớm</td>
</tr>
<tr>
<td>`question:answer`</td>
<td>Player</td>
<td>**có** — trả `AnswerAck`</td>
<td>xem 5.4</td>
</tr>
<tr>
<td>`question:next`</td>
<td>Player</td>
<td>không</td>
<td>dùng cho self-paced (solo/marathon/practice) — player tự next</td>
</tr>
<tr>
<td>`player:sync`</td>
<td>Player</td>
<td>không</td>
<td>yêu cầu server gửi lại state hiện tại (dùng khi resume app)</td>
</tr>
<tr>
<td>`game:review`</td>
<td>Player</td>
<td>không</td>
<td>xem lại đáp án đã làm (practice/solo có `reviewMode`)</td>
</tr>
</table>
### 5.3. Bảng event **server → client** (đầy đủ, tách theo room)
<table header-row="true">
<tr>
<td>Event</td>
<td>Nhận bởi</td>
<td>Payload chính</td>
</tr>
<tr>
<td>`lobby:updated`</td>
<td>cả room</td>
<td>danh sách player trong lobby</td>
</tr>
<tr>
<td>`game:state`</td>
<td>Host room (mọi lần join/resync), Player khi `player:sync`</td>
<td>snapshot toàn bộ session — **màn hình host dùng cái này là nguồn chính vì self-paced không bao giờ emit ****`question:started`**** ra room**</td>
</tr>
<tr>
<td>`game:countdown`</td>
<td>cả room</td>
<td>`{seconds, startsAt, serverTime}` — đếm ngược trước câu 1</td>
</tr>
<tr>
<td>`game:started`</td>
<td>cả room</td>
<td>`{mode, config, total_questions, serverTime}`</td>
</tr>
<tr>
<td>`question:started`</td>
<td>cả room (**chỉ host-paced**)</td>
<td>`{question (đã ẩn đáp án), time_limit, endsAt, serverTime}`</td>
</tr>
<tr>
<td>`host:question`</td>
<td>**chỉ Host room**</td>
<td>như trên **cộng** `correct_answer` — đáp án đúng CHỈ gửi cho host</td>
</tr>
<tr>
<td>`question:locked`</td>
<td>cả room</td>
<td>\`\{index, reason: 'time_up'\\</td>
</tr>
<tr>
<td>`question:results`</td>
<td>cả room</td>
<td>`{index, correct_answer?, stats, nextQuestionAt, serverTime}` — `correct_answer`/`stats` chi tiết chỉ có nếu `config.flow.showCorrectAnswer=true`</td>
</tr>
<tr>
<td>`leaderboard:updated`</td>
<td>Player room (trừ host)</td>
<td>top N, phụ thuộc `showLeaderboard`</td>
</tr>
<tr>
<td>`leaderboard:host`</td>
<td>**chỉ Host room**</td>
<td>leaderboard đầy đủ, luôn gửi bất kể config</td>
</tr>
<tr>
<td>`answer:received`</td>
<td>Player room (trừ host)</td>
<td>`{index, answered, activePlayers, serverTime}` — **không tiết lộ ai đúng/sai**</td>
</tr>
<tr>
<td>`host:answer-received`</td>
<td>**chỉ Host room**</td>
<td>như trên **cộng** `player{id, player_name}` và `is_correct`</td>
</tr>
<tr>
<td>`host:player-progress`</td>
<td>**chỉ Host room**</td>
<td>self-paced only — tiến độ từng player riêng lẻ</td>
</tr>
<tr>
<td>`player:eliminated`</td>
<td>cả room</td>
<td>survival mode khi hết mạng</td>
</tr>
<tr>
<td>`question:awaiting_next`</td>
<td>Player</td>
<td>self-paced, re-emit kết quả câu trước khi client reconnect giữa chừng</td>
</tr>
<tr>
<td>`question:timeout`</td>
<td>Player</td>
<td>self-paced, hết giờ câu hiện tại</td>
</tr>
<tr>
<td>`player:finished`</td>
<td>Player + Host room</td>
<td>player hoàn thành hết câu hỏi (self-paced)</td>
</tr>
<tr>
<td>`game:review`</td>
<td>Player</td>
<td>trả lại danh sách câu đã làm + đáp án đúng (nếu `reviewMode`)</td>
</tr>
<tr>
<td>`game:ended`</td>
<td>cả room</td>
<td>kết quả cuối cùng</td>
</tr>
<tr>
<td>`error`</td>
<td>socket gây lỗi</td>
<td>`{event, message}` — mọi lỗi nghiệp vụ (`CONFLICT`, `FORBIDDEN`, `GONE`...) đi qua đây, không phải HTTP status</td>
</tr>
</table>
> ⚠️ **Nguyên tắc bảo mật quan trọng**: đáp án đúng (`correct_answer`) và ai trả lời đúng/sai (`is_correct`) **không bao giờ** được gửi cho Player room trong lúc câu hỏi còn active — chỉ Host room nhận được. Client Player **không được** cố gắng suy luận đáp án từ payload nhận được lúc `question:started`.
### 5.4. `AnswerAck` — kết quả trả lời qua ACK, không qua event riêng
```kotlin
data class AnswerAck(
    val accepted: Boolean,
    val isLate: Boolean,
    val lives: Int?,
    val eliminated: Boolean,
    val serverTime: String,
    // các field dưới CHỈ có khi (selfPaced && showCorrectAnswer) — host-paced sẽ null hết,
    // kết quả thật sự đến qua question:results/leaderboard:updated
    val isCorrect: Boolean? = null,
    val scoreEarned: Int? = null,
    val totalScore: Int? = null,
    val streak: Int? = null,
    val correctAnswer: JsonElement? = null
)
```
Điều này khác hoàn toàn thiết kế v1.0: **ViewModel không "chờ event riêng để biết đúng/sai"**, mà (a) với self-paced đọc ngay trong ACK của `question:answer`, (b) với host-paced phải chờ `question:results` sau khi `question:locked`.
### 5.5. Reconnection
```kotlin
socket.on(Socket.EVENT_CONNECT) {
    // Không cần emit lại roomCode/nickname — socketToken còn hạn (SOCKET_TOKEN_TTL)
    // và server tự khôi phục qua onLobbyJoin() khi client emit lại "lobby:join"
    socket.emit("lobby:join")
}
socket.on(Socket.EVENT_DISCONNECT) { args ->
    val reason = args.getOrNull(0)?.toString()
    if (reason == "io server disconnect") {
        // Server chủ động ngắt (VD: game đã finished/cancelled) → không tự reconnect
    }
    // các trường hợp khác: Socket.IO client tự reconnect, sau đó CONNECT handler ở trên chạy lại
}
```
Lưu `socketToken` (KHÔNG lưu vào DataStore lâu dài — nó có TTL ngắn `SOCKET_TOKEN_TTL`, chỉ giữ trong bộ nhớ/ViewModel trong suốt phiên chơi). Nếu token hết hạn khi app resume sau khi bị kill → gọi lại REST `/join` hoặc `/host-token` để lấy token mới.
---
## 6. Game Modes & State Machine
### 6.1. 5 mode thật (`game.schema.ts`, `engine/modes/*`)
<table header-row="true">
<tr>
<td>Mode</td>
<td>`pacing`</td>
<td>Đặc điểm</td>
</tr>
<tr>
<td>`classic`</td>
<td>`host`</td>
<td>Cả phòng cùng 1 câu hỏi, host điều khiển nhịp độ, leaderboard giữa các câu</td>
</tr>
<tr>
<td>`solo`</td>
<td>`self`</td>
<td>1 người chơi tự tiến độ, `allowAnswerLate=true`, có review + đáp án đúng ngay</td>
</tr>
<tr>
<td>`survival`</td>
<td>`self`</td>
<td>Có `lives` (mặc định 3), hết mạng → `player:eliminated`</td>
</tr>
<tr>
<td>`marathon`</td>
<td>`self`</td>
<td>Giới hạn tổng thời gian (`totalMatchSeconds`), câu hỏi **lặp vòng** khi hết bộ câu hỏi</td>
</tr>
<tr>
<td>`practice`</td>
<td>`self`</td>
<td>`basePoints=0` (điểm luôn = 0, có ý đồ), luôn hiện đáp án đúng, dùng để luyện tập</td>
</tr>
</table>
Toàn bộ config này (`scoring`, `timing`, `lobby`, `flow`) đều có thể bị Host **patch một phần** qua `lobby:config-update` trước khi start — vì vậy **không hardcode logic theo mode ở Android**, mà đọc `GameConfig` nhận được từ `game:started`/`game:state` để quyết định UI (ẩn/hiện timer, ẩn/hiện nút "Next", có hiện leaderboard giữa câu hay không...).
### 6.2. State Machine đề xuất (thay cho enum tuyến tính của v1.0)
```kotlin
sealed class GamePhaseUi {
    object Lobby : GamePhaseUi()
    data class Countdown(val secondsLeft: Int, val startsAt: Instant) : GamePhaseUi()

    // host-paced: dùng chung cho cả phòng
    data class RoomQuestion(
        val question: QuestionUi,
        val endsAt: Instant?,          // null nếu không giới hạn thời gian
        val isLocked: Boolean,
        val myAnswer: AnswerPayload? = null
    ) : GamePhaseUi()
    data class RoomResults(
        val index: Int,
        val correctAnswer: JsonElement?,
        val stats: QuestionStats,
        val nextQuestionAt: Instant?
    ) : GamePhaseUi()

    // self-paced: mỗi player một nhịp riêng
    data class SelfQuestion(
        val question: QuestionUi,
        val endsAt: Instant?,
        val livesLeft: Int?,
        val matchEndsAt: Instant?   // marathon
    ) : GamePhaseUi()
    data class SelfAnswered(val ack: AnswerAck) : GamePhaseUi()

    data class Eliminated(val reason: String) : GamePhaseUi()
    object Finished : GamePhaseUi()
    data class Ended(val finalLeaderboard: List<PlayerScoreUi>) : GamePhaseUi()
    data class Error(val message: String) : GamePhaseUi()
}

data class GameUiState(
    val mode: String = "classic",
    val config: GameConfigUi = GameConfigUi(),
    val phase: GamePhaseUi = GamePhaseUi.Lobby,
    val leaderboard: List<PlayerScoreUi> = emptyList(),
    val players: List<PlayerUi> = emptyList(), // lobby roster
    val myScore: Int = 0,
    val myStreak: Int = 0,
    val error: UiText? = null
)
```
`GameViewModel` chọn nhánh `RoomQuestion` hay `SelfQuestion` dựa trên `config.flow.pacing`, subscribe đúng tập event tương ứng (host-paced lắng `question:started/locked/results`; self-paced lắng `question:awaiting_next/timeout` + đọc kết quả trực tiếp từ ACK của `question:answer`).
### 6.3. Host UI cần state riêng
```kotlin
data class HostGameUiState(
    val session: GameSessionSnapshotUi,       // từ game:state
    val currentQuestionForHost: HostQuestionUi?, // từ host:question — có correct_answer
    val answerProgress: AnswerProgressUi,        // từ host:answer-received / host:player-progress
    val fullLeaderboard: List<PlayerScoreUi>,    // từ leaderboard:host — luôn đầy đủ
    val playersConnected: Int
)
```
---
## 7. Domain Layer — Model, UseCase, Repository
### 7.1. UseCase (viết lại theo route/event thật)
<table header-row="true">
<tr>
<td>UseCase</td>
<td>Input</td>
<td>Output</td>
<td>Mô tả</td>
</tr>
<tr>
<td>`LoginUseCase`</td>
<td>email, password</td>
<td>`Result<User>`</td>
<td>cookie set tự động qua CookieJar</td>
</tr>
<tr>
<td>`LoginWithGoogleUseCase`</td>
<td>idToken</td>
<td>`Result<User>`</td>
<td>gọi `google/one-tap`</td>
</tr>
<tr>
<td>`GetCurrentUserUseCase`</td>
<td>–</td>
<td>`Result<User>`</td>
<td>`GET /users/me`, dùng để xác định trạng thái đăng nhập</td>
</tr>
<tr>
<td>`SearchQuizzesUseCase`</td>
<td>keyword, page</td>
<td>`Flow<PaginatedResponse<Quiz>>`</td>
<td>không cần login</td>
</tr>
<tr>
<td>`GetMyQuizzesUseCase`</td>
<td>ownerId</td>
<td>`Flow<PaginatedResponse<Quiz>>`</td>
<td></td>
</tr>
<tr>
<td>`GetQuizDetailUseCase`</td>
<td>quizId</td>
<td>`Result<Quiz>`</td>
<td></td>
</tr>
<tr>
<td>`GetGameModesUseCase`</td>
<td>–</td>
<td>`Result<List<GameModeSpec>>`</td>
<td>build màn CreateRoom động</td>
</tr>
<tr>
<td>`CreateGameSessionUseCase`</td>
<td>quizId, name, mode, configPatch?</td>
<td>`Result<GameSession>`</td>
<td></td>
</tr>
<tr>
<td>`LookupRoomUseCase`</td>
<td>code</td>
<td>`Result<GameSession>`</td>
<td>validate mã phòng trước khi join</td>
</tr>
<tr>
<td>`JoinGameAsPlayerUseCase`</td>
<td>code, nickname, guestId?</td>
<td>`Result<PlayerSession + socketToken>`</td>
<td></td>
</tr>
<tr>
<td>`GetHostSocketTokenUseCase`</td>
<td>gameId</td>
<td>`Result<socketToken>`</td>
<td></td>
</tr>
<tr>
<td>`ConnectGameSocketUseCase`</td>
<td>socketToken</td>
<td>`Flow<GameEvent>`</td>
<td>mở kết nối, emit `lobby:join`</td>
</tr>
<tr>
<td>`SubmitAnswerUseCase`</td>
<td>answer</td>
<td>`AnswerAck` (qua ACK callback → suspend)</td>
<td></td>
</tr>
<tr>
<td>`RequestNextQuestionUseCase` (self-paced)</td>
<td>–</td>
<td>–</td>
<td>emit `question:next`</td>
</tr>
<tr>
<td>`HostControlUseCase`</td>
<td>action: start/pause/resume/next/end</td>
<td>–</td>
<td>emit event tương ứng, Host only</td>
</tr>
<tr>
<td>`UpdateRoomConfigUseCase` (Host)</td>
<td>configPatch</td>
<td>`Result<{changed, config, ignored}>`</td>
<td>emit `lobby:config-update` có ACK</td>
</tr>
<tr>
<td>`GetLeaderboardUseCase`</td>
<td>gameId</td>
<td>`Result<List<PlayerScore>>`</td>
<td>REST fallback khi chưa kịp nhận socket</td>
</tr>
<tr>
<td>`GetResultsUseCase`</td>
<td>gameId</td>
<td>`Result<GameResults>`</td>
<td></td>
</tr>
</table>
### 7.2. Repository Interfaces
> Vị trí file của từng interface — xem cây thư mục mục 2.3 và bảng tra cứu mục 2.4. Tóm tắt: 3 interface đầu đặt tại `core:common/repository` (dùng chung ≥ 2 feature), 2 interface cuối đặt cục bộ trong chính feature sở hữu (chỉ 1 feature dùng).
```kotlin
// 📁 core:common/repository/AuthRepository.kt — impl: core:network/repository/AuthRepositoryImpl.kt
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User, AppError>
    suspend fun loginWithGoogle(idToken: String): Result<User, AppError>
    suspend fun getCurrentUser(): Result<User, AppError>
    suspend fun logout(): Result<Unit, AppError>
}

// 📁 core:common/repository/QuizRepository.kt — impl: core:network/repository/QuizRepositoryImpl.kt
interface QuizRepository {
    fun searchQuizzes(keyword: String, page: Int): Flow<PagingData<Quiz>>
    fun getQuizzesByOwner(ownerId: Long): Flow<PagingData<Quiz>>
    suspend fun getQuizDetail(quizId: Long): Result<Quiz, AppError>
    suspend fun createQuiz(req: QuizRequest): Result<Quiz, AppError>
    suspend fun updateQuiz(quizId: Long, patch: QuizPatch): Result<Quiz, AppError>
    suspend fun deleteQuiz(quizId: Long): Result<Unit, AppError>
}

// 📁 core:common/repository/GameSessionRepository.kt — impl: core:network/repository/GameSessionRepositoryImpl.kt
interface GameSessionRepository { // REST phần tạo/join phòng
    suspend fun getGameModes(): Result<List<GameModeSpec>, AppError>
    suspend fun createSession(req: CreateSessionRequest): Result<GameSession, AppError>
    suspend fun lookupRoom(code: String): Result<GameSession, AppError>
    suspend fun joinAsPlayer(code: String, nickname: String, guestId: String?): Result<JoinResult, AppError>
    suspend fun getHostToken(gameId: Long): Result<String, AppError>
    suspend fun getLeaderboard(gameId: Long): Result<List<PlayerScore>, AppError>
    suspend fun getResults(gameId: Long): Result<GameResults, AppError>
}

// 📁 feature:game-player/domain/repository/PlayerGameSocketRepository.kt (CỤC BỘ — không nâng lên core:common)
// impl: feature:game-player/data/repository/PlayerGameSocketRepositoryImpl.kt
interface PlayerGameSocketRepository {
    fun connect(socketToken: String): Flow<GameEvent>
    fun disconnect()
    suspend fun submitAnswer(answer: AnswerPayload): AnswerAck
    fun requestNext()                     // question:next (self-paced)
    fun requestReview()                   // game:review
    fun requestSync()                     // player:sync
}

// 📁 feature:game-host/domain/repository/HostGameSocketRepository.kt (CỤC BỘ — không nâng lên core:common)
// impl: feature:game-host/data/repository/HostGameSocketRepositoryImpl.kt
interface HostGameSocketRepository {
    fun connect(socketToken: String): Flow<GameEvent>
    fun disconnect()
    fun hostAction(action: HostAction)    // game:start/next/pause/resume/end
    suspend fun updateConfig(patch: GameConfigPatch): ConfigUpdateAck
}
```
> 💡 Vì sao tách `GameSocketRepository` thành 2 interface thay vì dùng chung 1 cái như bản trước: nếu để chung, interface đó buộc phải đặt ở `core:common` (do 2 feature cùng dùng) — nhưng `submitAnswer`/`requestNext` chỉ có ý nghĩa với Player, còn `hostAction`/`updateConfig` chỉ Host mới gọi được (server cũng chặn ở tầng socket middleware nếu sai role). Gộp chung sẽ tạo ra 1 interface có nửa số method luôn ném lỗi tuỳ theo ai gọi — vi phạm Interface Segregation. Tách riêng theo đúng vai trò còn giúp giữ 2 interface này ở cấp `feature` (phạm vi hẹp nhất có thể), không cần "leo" lên `core:common` một cách không cần thiết.
Domain layer là **Pure Kotlin**, không import Android/framework class — dễ test JUnit thuần, giữ nguyên nguyên tắc từ v1.0.
---
## 8. Presentation Layer — MVI theo vai trò Host/Player
### 8.1. Player — `GameViewModel`
```kotlin
sealed class GameIntent {
    data class SubmitAnswer(val answer: AnswerPayload) : GameIntent()
    object NextQuestion : GameIntent()       // self-paced
    object RequestReview : GameIntent()
    object LeaveGame : GameIntent()
    object Resync : GameIntent()             // gọi khi app resume
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val connectSocket: ConnectGameSocketUseCase,
    private val submitAnswer: SubmitAnswerUseCase,
    private val requestNext: RequestNextQuestionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun handleIntent(intent: GameIntent) { /* map sang UseCase tương ứng */ }
}
```
### 8.2. Host — `HostGameViewModel` (tách riêng, không dùng chung `GameViewModel`)
```kotlin
sealed class HostGameIntent {
    object StartGame : HostGameIntent()
    object NextQuestion : HostGameIntent()   // game:next, chỉ dùng khi autoAdvance=false
    object PauseGame : HostGameIntent()
    object ResumeGame : HostGameIntent()
    object EndGame : HostGameIntent()
    data class UpdateConfig(val patch: GameConfigPatch) : HostGameIntent()
}
```
⚠️ Không gọi UseCase trực tiếp từ Composable — luôn qua `ViewModel.handleIntent()` (giữ nguyên nguyên tắc v1.0).
---
## 9. UI Layer — Compose theo loại câu hỏi
Backend hỗ trợ 4 `question_type`: `multiple_choice`, `multiple_select`, `short_answer`, `long_answer` (`quiz.type.ts`). `AnswerOptionGrid` (chỉ chọn 1 đáp án) trong v1.0 **không đủ** — cần dispatcher theo loại:
```kotlin
@Composable
fun AnswerInput(
    question: QuestionUi,
    isLocked: Boolean,
    onSubmit: (AnswerPayload) -> Unit
) {
    when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> SingleChoiceGrid(question.options, isLocked, onSubmit)
        QuestionType.MULTIPLE_SELECT -> MultiSelectGrid(question.options, isLocked, onSubmit)
        QuestionType.SHORT_ANSWER -> ShortTextField(isLocked, onSubmit)
        QuestionType.LONG_ANSWER -> LongTextField(isLocked, onSubmit)
    }
}
```
### 9.1. GamePlayScreen (Player, host-paced) — Composable Breakdown
```kotlin
@Composable
fun GamePlayScreen(viewModel: GameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold { padding ->
        Column(Modifier.padding(padding)) {
            when (val phase = state.phase) {
                is GamePhaseUi.Countdown -> CountdownOverlay(phase.secondsLeft)
                is GamePhaseUi.RoomQuestion -> {
                    phase.endsAt?.let { CountdownTimer(it) }  // null = không giới hạn thời gian
                    QuestionCard(phase.question)
                    AnswerInput(
                        question = phase.question,
                        isLocked = phase.isLocked,
                        onSubmit = { viewModel.handleIntent(GameIntent.SubmitAnswer(it)) }
                    )
                }
                is GamePhaseUi.RoomResults -> AnswerResultCard(phase)
                is GamePhaseUi.SelfQuestion -> { /* tương tự nhưng có thanh mạng/lives nếu survival */ }
                is GamePhaseUi.Eliminated -> EliminatedScreen(phase.reason)
                is GamePhaseUi.Ended -> FinalResultRedirect()
                else -> {}
            }
        }
    }
}
```
### 9.2. CountdownTimer — vẫn giữ nguyên tắc timer local từ v1.0
```kotlin
@Composable
fun CountdownTimer(endsAt: Instant) {
    // tick 100ms trong ViewModel dựa trên serverTs offset, xem mục 13
}
```
---
## 10. Local Storage — DataStore & Room
### 10.1. DataStore (khác v1.0: bỏ `auth_token`)
```kotlin
object PreferenceKeys {
    val NICKNAME = stringPreferencesKey("nickname")
    val LAST_ROOM_CODE = stringPreferencesKey("last_room_code")
    val GUEST_ID = stringPreferencesKey("guest_id")
    val IS_HOST_MODE = booleanPreferencesKey("is_host_mode")
    // KHÔNG có AUTH_TOKEN — auth state suy ra từ cookie + GET /users/me
}
```
### 10.2. Room — cache & lịch sử (giữ nguyên tinh thần v1.0, chỉnh field theo DTO thật)
```kotlin
@Entity(tableName = "cached_quiz")
data class CachedQuizEntity(
    @PrimaryKey val id: Long,
    val quizName: String,
    val quizDescription: String?,
    val questionCount: Int,
    val quizImage: String?,
    val cachedAt: Long
)

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    @PrimaryKey val gameId: Long,
    val quizName: String,
    val mode: String,
    val finalScore: Int,
    val rank: Int,
    val totalPlayers: Int,
    val playedAt: Long
)

@Entity(tableName = "cookie_store") // backing storage cho RoomCookieStore (mục 3.2) — chỉ dùng nội bộ core:database
data class CookieEntity(
    @PrimaryKey val key: String, // host|name
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
)
```
> Lưu ý: `CookieEntity` và `CookieDao` **không** được `core:network` import trực tiếp — chúng chỉ được `RoomCookieStore` (cùng nằm trong `core:database`) dùng để hiện thực interface `CookieStore` ở `core:common` (xem mục 3.2). Đây là lý do bảng dependency ở mục 2.3 vẫn đúng dù cookie thực chất được lưu bằng Room.
---
## 11. Navigation Architecture

### 11.1. Unified MainGraph — Browse-First Mobile UX

Giữ Navigation Compose + type-safe routes (Kotlin Serializable) như v1.0, nhưng **thay đổi cấu trúc graph** để phù hợp UX mobile và pattern `optionalAuthMiddleware` của backend:

> ✅ **Quyết định chốt (20/8)**: Option B — Browse-First. Splash **luôn** điều hướng thẳng Home, không rẽ nhánh Auth/Guest/Host tại Splash nữa. `AuthState` rút gọn còn 2 giá trị `GUEST`/`AUTHENTICATED` (bỏ khái niệm "first launch" riêng) — `CheckAuthStateUseCase` hỏi backend; nếu chưa authenticated thì set guest mode = true và trả `GUEST`. Auth screens (`AuthGraph`) chỉ được vào qua các `RequireAuth` soft gate ở mục 11.2, không còn là đích điều hướng của Splash. Refresh-token/verify-session lúc khởi động app: chưa implement, đang là `TODO` trong `SplashViewModel` (N12+).

```kotlin
NavHost(navController, startDestination = Route.Splash) {
    // Splash screen — luôn điều hướng thẳng Home (Option B — Browse-First, chốt 20/8)
    // CheckAuthStateUseCase quyết định AuthState (GUEST/AUTHENTICATED) nhưng KHÔNG ảnh hưởng đích điều hướng ở đây
    composable<Route.Splash> { 
        SplashScreen(
            onNavigateToHome = { 
                navController.navigate(Route.MainGraph) {
                    popUpTo<Route.Splash> { inclusive = true }
                }
            }
        )
    }

    // Auth flow — modal, không nằm trong bottom nav
    navigation<Route.AuthGraph>(startDestination = Route.Login) {
        composable<Route.Login> { LoginScreen(...) }
        composable<Route.Register> { RegisterScreen(...) }
    }

    // Main app shell — unified graph cho mọi user (guest + authenticated)
    // Bottom Navigation: Home | Discover | Join | Library | Profile
    navigation<Route.MainGraph>(startDestination = Route.Home) {
        
        // ===== PUBLIC ROUTES — accessible to everyone =====
        composable<Route.Home> { 
            // Gọi GET /quizzes/home (optionalAuthMiddleware)
            // Backend trả sections khác nhau tùy auth state:
            //   - Guest: trending, newest, most_played
            //   - Authenticated: trending, newest, most_played + "continue" section
            HomeScreen(onNavigateToAuth = { navController.navigate(Route.AuthGraph) })
        }
        
        composable<Route.Discover> { 
            // Gọi GET /quizzes/search (optionalAuthMiddleware)
            // Filters + browse — không cần login
            DiscoverScreen()
        }
        
        composable<Route.JoinRoom> { 
            // Guest có thể join bằng nickname (player_guest_id từ DataStore)
            // Authenticated join bằng userId
            JoinRoomScreen(onNavigateToAuth = { navController.navigate(Route.AuthGraph) })
        }
        
        composable<Route.QuizDetail> { 
            // View quiz detail — public, có thể preview không cần login
            QuizDetailScreen()
        }

        // ===== PROTECTED ROUTES — require authentication =====
        composable<Route.Library> { 
            RequireAuth(
                onNavigateToAuth = { navController.navigate(Route.AuthGraph) }
            ) {
                LibraryScreen() // Danh sách quiz của tôi
            }
        }
        
        composable<Route.Profile> { 
            RequireAuth(
                onNavigateToAuth = { navController.navigate(Route.AuthGraph) }
            ) {
                ProfileScreen()
            }
        }
        
        composable<Route.CreateQuiz> { 
            RequireAuth(
                onNavigateToAuth = { navController.navigate(Route.AuthGraph) }
            ) {
                CreateQuizScreen()
            }
        }
        
        composable<Route.EditQuiz> { 
            RequireAuth(
                onNavigateToAuth = { navController.navigate(Route.AuthGraph) }
            ) {
                EditQuizScreen()
            }
        }
        
        composable<Route.CreateRoom> { 
            // Host tạo phòng từ quiz — đọc /games/game-modes
            RequireAuth(
                onNavigateToAuth = { navController.navigate(Route.AuthGraph) }
            ) {
                CreateRoomScreen()
            }
        }

        // ===== GAMEPLAY ROUTES — tách rõ Host vs Player ViewModels =====
        composable<Route.PlayerLobby> { 
            // Player lobby — guest hoặc authenticated
            PlayerLobbyScreen()
        }
        
        composable<Route.HostLobby> { 
            // Host lobby — cần authenticated (đã check ở CreateRoom)
            HostLobbyScreen()
        }
        
        composable<Route.GamePlay> { 
            // Player gameplay — dùng GameViewModel
            GamePlayScreen()
        }
        
        composable<Route.HostGame> { 
            // Host monitor/control — dùng HostGameViewModel riêng
            // (nhận host:* events khác với player events)
            HostGameScreen()
        }
        
        composable<Route.FinalResult> { 
            // Kết quả cuối game — accessible cho mọi người
            FinalResultScreen()
        }
    }
}
```

### 11.2. RequireAuth Composable — Soft Gate Pattern

```kotlin
@Composable
fun RequireAuth(
    onNavigateToAuth: () -> Unit,
    content: @Composable () -> Unit
) {
    val authState = LocalAuthState.current // từ CompositionLocal
    
    if (authState.isAuthenticated) {
        content()
    } else {
        // Hiện bottom sheet "Đăng nhập để tiếp tục" thay vì navigate hẳn
        AuthRequiredBottomSheet(
            onLoginClick = onNavigateToAuth,
            onDismiss = { /* quay về màn trước */ }
        )
    }
}
```

### 11.3. Type-Safe Routes với Kotlin Serialization

```kotlin
@Serializable sealed interface Route {
    @Serializable data object Splash : Route
    
    // Auth graph
    @Serializable data object AuthGraph : Route
    @Serializable data object Login : Route
    @Serializable data object Register : Route
    
    // Main graph — bottom nav + gameplay
    @Serializable data object MainGraph : Route
    @Serializable data object Home : Route
    @Serializable data object Discover : Route
    @Serializable data object JoinRoom : Route
    @Serializable data object Library : Route
    @Serializable data object Profile : Route
    
    @Serializable data class QuizDetail(val quizId: Long) : Route
    @Serializable data object CreateQuiz : Route
    @Serializable data class EditQuiz(val quizId: Long) : Route
    @Serializable data class CreateRoom(val quizId: Long) : Route
    
    // Gameplay routes — truyền socketToken qua argument
    @Serializable data class PlayerLobby(
        val gameId: Long, 
        val playerId: Long, 
        val socketToken: String
    ) : Route
    
    @Serializable data class HostLobby(
        val gameId: Long, 
        val socketToken: String
    ) : Route
    
    @Serializable data class GamePlay(
        val gameId: Long, 
        val playerId: Long, 
        val socketToken: String
    ) : Route
    
    @Serializable data class HostGame(
        val gameId: Long, 
        val socketToken: String
    ) : Route
    
    @Serializable data class FinalResult(val gameId: Long) : Route
}
```

> 💡 **socketToken** truyền qua route argument (không lưu DataStore) vì token có TTL ngắn và chỉ hợp lệ trong phiên chơi hiện tại.

### 11.4. Bottom Navigation Setup

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = listOf(
                    BottomNavItem("Home", Icons.Default.Home, Route.Home),
                    BottomNavItem("Discover", Icons.Default.Search, Route.Discover),
                    BottomNavItem("Join", Icons.Default.AddCircle, Route.JoinRoom),
                    BottomNavItem("Library", Icons.Default.LibraryBooks, Route.Library),
                    BottomNavItem("Profile", Icons.Default.Person, Route.Profile)
                ),
                navController = navController
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.MainGraph,
            modifier = Modifier.padding(paddingValues)
        ) {
            // ... (navigation graph như trên)
        }
    }
}
```

> ⚠️ **Lưu ý về Role-Aware Architecture**: Mặc dù navigation graph được merge, **GamePlay và HostGame vẫn dùng ViewModel hoàn toàn khác nhau** (`GameViewModel` vs `HostGameViewModel`) vì backend gửi payload Socket.IO khác nhau (`room` events vs `hostRoom` events). Merge graph chỉ ảnh hưởng **initial navigation/browsing**, không ảnh hưởng **gameplay role separation** (xem mục 6.3, 8).

### 11.5. Cập nhật 22/8 — Home auth header & Profile (chưa có Bottom Navigation)

⚠️ **Sai khác so với mục 11.4**: tại thời điểm N13.5 (22/8), `BottomNavigationBar` 5 tab (Home/Discover/Join/Library/Profile) mô tả ở mục 11.4 **chưa được triển khai**. Điều hướng thực tế hiện đi qua `NavController` thông thường gọi từ `HomeScreen`, chưa có bottom nav bar. Quyết định cụ thể:

- Tab "Của tôi" trước đây nằm trong `TabRow` của `HomeScreen` (song song tab "Khám phá", từ N11) đã **bỏ hẳn** — `HomeScreen` giờ chỉ còn nội dung khám phá cuộn dọc.
- Cạnh nút tìm kiếm ở `TopAppBar` của Home, thêm 1 component auth-aware: chưa đăng nhập → nút "Đăng ký/Đăng nhập" (nav `Route.AuthGraph`); đã đăng nhập → avatar tròn (nav `Route.Profile`). Re-check trạng thái đăng nhập mỗi khi Home resume (`LifecycleEventEffect(ON_RESUME)`).
- `Route.Profile` hết placeholder, trỏ vào `ProfileScreen` thật — nhưng đặt ở module `app` (không phải `feature:*`) vì gắn trực tiếp route cấp app. Màn hiển thị avatar + tên + email, có item "Quiz của tôi" (nav `Route.MyQuizzes`, ghép vào `feature:quiz-manage` đã làm ở N13–14) và item "Đăng xuất".
- Thêm component chung `Avatar` ở `core:ui/components/Avatar.kt` (bọc Coil ở scope `implementation`, tự fallback icon khi avatar null/rỗng). **Quy ước mới**: mọi nơi cần hiển thị avatar user dùng component này qua dependency `core:ui` sẵn có, không tự thêm `coil.compose` riêng cho từng module gọi (khác với ảnh cover quiz — vẫn để mỗi feature tự khai Coil như `feature:quiz-manage` đang làm, vì hiển thị khác nhau theo từng nơi).
- `Route.Library` (mục 11.3) vẫn còn trong sealed Route nhưng **chưa có điểm truy cập** (chưa có bottom nav) — "Quiz của tôi" hiện đi qua `Route.Profile` → `Route.MyQuizzes`, không qua `Route.Library`. Cần quyết định lại ở N16+: giữ `Route.Library` cho mục đích khác (có thể là danh sách quiz đã lưu/yêu thích công khai) hay hợp nhất với `Route.MyQuizzes`.
---
## 12. Dependency Injection — Hilt Modules
<table header-row="true">
<tr>
<td>Module</td>
<td>Đặt tại module Gradle</td>
<td>Provides</td>
<td>Scope</td>
</tr>
<tr>
<td>`NetworkModule`</td>
<td>`core:network`</td>
<td>`OkHttpClient` (nhận `CookieStore` qua interface, `PersistentCookieJar`  • `TokenAuthenticator`), `Retrofit`, `AuthApiService`, `QuizApiService`, `GameApiService`</td>
<td>`@Singleton`</td>
</tr>
<tr>
<td>`SocketModule`</td>
<td>`core:network`</td>
<td>Factory `(socketToken: String) -> Socket` (không phải 1 `Socket` singleton cố định — mỗi phiên chơi cần token khác nhau)</td>
<td>`@Singleton` cho factory</td>
</tr>
<tr>
<td>`DatabaseModule`</td>
<td>`core:database`</td>
<td>Room `AppDatabase`, DAOs</td>
<td>`@Singleton`</td>
</tr>
<tr>
<td>`DatabaseBindingModule`</td>
<td>`core:database`</td>
<td>`@Binds RoomCookieStore → CookieStore` (mục 3.2) — module tự implement và tự bind interface của `core:common`, `core:network` không cần biết</td>
<td>`@Singleton`</td>
</tr>
<tr>
<td>`NetworkBindingModule`</td>
<td>`core:network`</td>
<td>`@Binds AuthRepositoryImpl → AuthRepository`, `@Binds QuizRepositoryImpl → QuizRepository`, `@Binds GameSessionRepositoryImpl → GameSessionRepository` (mục 7.2, 12.2)</td>
<td>`@Singleton`</td>
</tr>
<tr>
<td>`DataStoreModule`</td>
<td>`core:datastore`</td>
<td>`DataStore<Preferences>`</td>
<td>`@Singleton`</td>
</tr>
<tr>
<td>`GameSocketBindingModule`</td>
<td>mỗi `feature:game-player`, `feature:game-host` (2 module riêng)</td>
<td>`@Binds PlayerGameSocketRepositoryImpl → PlayerGameSocketRepository` (hoặc bản Host) — cục bộ trong từng feature vì interface không nâng lên `core:common` (mục 7.2)</td>
<td>`@Singleton`</td>
</tr>
</table>
### 12.1. Vì sao `NetworkModule` không tự tạo `CookieStore`
`NetworkModule` (nằm trong `core:network`) chỉ khai báo `provideCookieJar(store: CookieStore): CookieJar` — **nhận** `CookieStore` làm tham số chứ **không @Provides ra nó**. Instance thật của `CookieStore` đến từ `DatabaseBindingModule` ở `core:database`. Vì Hilt gộp mọi module Dagger trên classpath khi build `:app` (module duy nhất phụ thuộc cả `core:network` lẫn `core:database`), đồ thị DI vẫn ráp đúng dù 2 module Gradle này không hề biết đến nhau ở compile-time. Đây chính là cách Dependency Inversion Principle được hiện thực bằng Hilt mà không cần một "binding module trung gian" nào khác.
### 12.2. `NetworkBindingModule` — vì sao bind được ngay trong `core:network` mà không cần "module trung gian"
Khác với `CookieStore` (interface và impl nằm ở 2 module khác nhau, phải nhờ Hilt gộp graph ở `:app`), 3 repository `AuthRepository` / `QuizRepository` / `GameSessionRepository` có **interface ở ****`core:common`** nhưng **impl lại nằm ngay trong ****`core:network`** — mà `core:network` vốn đã được phép phụ thuộc `core:common` (đúng bảng dependency mục 2.3). Vì vậy `core:network` vừa thấy được interface, vừa tự viết impl, vừa tự `@Binds` — không cần chờ tới `:app` mới ráp graph như trường hợp `CookieStore`:
```kotlin
// core:network/di/NetworkBindingModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    @Binds @Singleton
    abstract fun bindGameSessionRepository(impl: GameSessionRepositoryImpl): GameSessionRepository
}
```
Tương tự, `PlayerGameSocketRepository`/`HostGameSocketRepository` có cả interface lẫn impl nằm trong **cùng một** feature module — nên `@Binds` cũng khai báo ngay tại chỗ (`feature:game-player/domain/di` hoặc gộp vào file DI hiện có của feature), không cần đụng tới `core` hay `:app`.
> Quy tắc chung rút ra: **nếu interface và impl nằm cùng 1 module Gradle → ****`@Binds`**** khai báo ngay trong module đó.** Chỉ khi interface và impl buộc phải tách sang 2 module khác nhau (như `CookieStore`, vì impl cần Room mà interface không được phép biết Room) thì mới phụ thuộc vào việc `:app` gộp graph ở bước build cuối cùng.
```kotlin
// core:network/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideCookieJar(store: CookieStore): CookieJar = PersistentCookieJar(store) // store do Hilt inject từ core:database

    @Provides @Singleton
    fun provideOkHttp(cookieJar: CookieJar, authenticator: TokenAuthenticator): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .authenticator(authenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
}

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {
    @Provides @Singleton
    fun provideSocketFactory(): (String) -> Socket = { token ->
        val options = IO.Options.builder()
            .setPath("/socket.io/")
            .setTransports(arrayOf(WebSocket.NAME))
            .setAuth(mapOf("token" to token))
            .build()
        IO.socket(BuildConfig.SOCKET_URL, options)
    }
}
```
---
## 13. Timer Synchronization & Reconnection
Giữ nguyên nguyên lý offset từ v1.0 — server luôn gửi `serverTime` kèm mọi event có deadline (`endsAt`, `nextQuestionAt`, `startsAt`):
```kotlin
val offset = Instant.parse(serverTime).toEpochMilli() - System.currentTimeMillis()
// remaining = endsAt - now - offset
```
Điểm bổ sung so với v1.0:
- Với **self-paced**, deadline nằm trong "player clock" riêng của từng người (không có `phase_ends_at` chung của session) — Android phải tính lại offset **mỗi khi nhận ****`SelfQuestion`**, không share 1 offset toàn cục với host-paced.
- `endsAt` có thể là `null` (câu hỏi không giới hạn thời gian, `time_limit` không set) → UI phải ẩn hẳn `CountdownTimer`, không mặc định 20s như v1.0 giả định.
- Khi mất kết nối và reconnect giữa chừng một câu hỏi self-paced, server gửi `question:awaiting_next` để đồng bộ lại UI — ViewModel cần một nhánh xử lý riêng cho event này, không map chung vào `SelfQuestion`.
---
## 14. Error Handling — theo đúng envelope backend
Toàn bộ lỗi REST trả JSON `{success:false, error:{code}}` — **chỉ có `code`, không có `message`/`details`** (xem mục 4.6) — với HTTP status tương ứng (`400/401/403/404/409/413/500/503` — xem `error.handler.ts`). Lỗi [Socket.IO](http://Socket.IO) đi qua **event ****`error`** dạng `{event, message}` với message có prefix loại lỗi: `UNAUTHORIZED:`, `FORBIDDEN:`, `CONFLICT:`, `GONE:` (lỗi Socket vẫn có message, chỉ REST là code thuần).
```kotlin
sealed class AppError {
    data class Http(val status: Int, val code: String) : AppError()   // code thô từ server, VD "VALIDATION_ERROR"
    data class Socket(val sourceEvent: String, val message: String) : AppError() {
        val kind: SocketErrorKind get() = when {
            message.startsWith("UNAUTHORIZED") -> SocketErrorKind.UNAUTHORIZED
            message.startsWith("FORBIDDEN") -> SocketErrorKind.FORBIDDEN
            message.startsWith("CONFLICT") -> SocketErrorKind.CONFLICT   // VD: đã trả lời rồi, game chưa start
            message.startsWith("GONE") -> SocketErrorKind.GONE           // VD: phòng đã kết thúc, bị kick
            else -> SocketErrorKind.UNKNOWN
        }
    }
    object NetworkError : AppError()
}

// Map code (REST) hoặc kind (Socket) sang chuỗi hiển thị tiếng Việt — KHÔNG hiển thị code/message thô cho user.
fun AppError.Http.toUiText(): UiText = when (code) {
    "VALIDATION_ERROR" -> UiText.StringResource(R.string.error_validation)
    "RESET_TICKET_INVALID" -> UiText.StringResource(R.string.error_reset_ticket_invalid)
    "UNAUTHORIZED" -> UiText.StringResource(R.string.error_unauthorized)
    else -> UiText.StringResource(R.string.error_generic)
}
```
`GONE` (VD: `player not in room`, `game is not active`) nên tự động điều hướng người chơi ra khỏi màn hình game về Home kèm thông báo, thay vì chỉ hiện snackbar như lỗi thông thường.
---
## 15. Upload ảnh — Presigned S3
Backend **không nhận file upload trực tiếp** qua REST body cho ảnh câu hỏi/quiz (trừ avatar dùng multipart riêng `PATCH /users/me/avatar`) — dùng luồng **presign 2 bước**:
```kotlin
interface StorageApiService {
    @POST("storage/presign")
    suspend fun presignUpload(@Body req: PresignRequest): PresignResponse
}

// 1. Xin URL
val presign = storageApi.presignUpload(PresignRequest(fileName, contentType))
// 2. PUT thẳng file lên S3 bằng URL đó (KHÔNG qua backend, không cookie)
httpClient.newCall(Request.Builder().url(presign.uploadUrl).put(fileBody).build()).execute()
// 3. Lưu presign.publicUrl / key vào field quiz_image, question_image khi tạo/sửa quiz
```
> v1.0 chỉ nhắc `coil-compose` để **hiển thị** ảnh — cần bổ sung hẳn use case `UploadImageUseCase` cho luồng tạo/sửa quiz vì host có thể chèn ảnh vào câu hỏi.
---
## 16. Testing Strategy
Giữ nguyên test pyramid từ v1.0 (JUnit + MockK + Turbine, Compose UI Test, MockWebServer) — bổ sung 2 điểm quan trọng theo backend thật:
<table header-row="true">
<tr>
<td>Loại test</td>
<td>Trọng tâm bổ sung</td>
</tr>
<tr>
<td>Unit test</td>
<td>Test riêng state machine cho **cả 2 nhánh** `host-paced` và `self-paced` — dễ bỏ sót self-paced vì không có event chung phòng</td>
</tr>
<tr>
<td>Integration test</td>
<td>Mock `PersistentCookieJar` để test luồng 401 → refresh → retry (dùng `Authenticator`, không phải Interceptor, nên cần test riêng qua MockWebServer với `enqueue` 401 rồi 200)</td>
</tr>
<tr>
<td>Socket test</td>
<td>Fake `PlayerGameSocketRepository` (và `HostGameSocketRepository` cho màn Host) phát lại đúng thứ tự event thật (`game:countdown` → `question:started`/`host:question` song song → `answer:received`/`host:answer-received` → `question:locked` → `question:results`) để bắt lỗi race condition giữa 2 luồng host/player</td>
</tr>
</table>
```kotlin
@Test
fun `self-paced answer reveals result immediately via ack`() = runTest {
    val fakeSocketRepo = FakePlayerGameSocketRepository()
    val viewModel = GameViewModel(fakeSocketRepo, ...)
    viewModel.state.test {
        awaitItem() // Lobby
        fakeSocketRepo.emitSelfQuestion(...)
        assertTrue(awaitItem().phase is GamePhaseUi.SelfQuestion)
        viewModel.handleIntent(GameIntent.SubmitAnswer(AnswerPayload.Choice(1)))
        val afterAck = awaitItem()
        assertTrue(afterAck.phase is GamePhaseUi.SelfAnswered)
    }
}
```
---
## 17. Dependencies — build.gradle.kts
### 17.1. Core & UI
<table header-row="true">
<tr>
<td>Thư viện</td>
<td>Version</td>
<td>Mục đích</td>
</tr>
<tr>
<td>[androidx.compose.bom](http://androidx.compose.bom)</td>
<td>2025.04.01+</td>
<td>BOM Compose</td>
</tr>
<tr>
<td>androidx.compose.material3</td>
<td>BOM</td>
<td>Material 3</td>
</tr>
<tr>
<td>androidx.navigation:navigation-compose</td>
<td>2.8.x</td>
<td>Type-safe routes</td>
</tr>
<tr>
<td>androidx.lifecycle:lifecycle-viewmodel-compose</td>
<td>2.8.x</td>
<td>`hiltViewModel()`, `collectAsStateWithLifecycle()`</td>
</tr>
<tr>
<td>androidx.credentials + androidx.credentials:credentials-play-services-auth</td>
<td>mới nhất</td>
<td>Google Sign-In (Credential Manager)</td>
</tr>
</table>
### 17.2. DI & Async
<table header-row="true">
<tr>
<td>Thư viện</td>
<td>Version</td>
<td>Mục đích</td>
</tr>
<tr>
<td>[com.google](http://com.google).dagger:hilt-android</td>
<td>2.51.x</td>
<td>DI</td>
</tr>
<tr>
<td>androidx.hilt:hilt-navigation-compose</td>
<td>1.2.x</td>
<td></td>
</tr>
<tr>
<td>org.jetbrains.kotlinx:kotlinx-coroutines-android</td>
<td>1.8.x</td>
<td></td>
</tr>
<tr>
<td>org.jetbrains.kotlinx:kotlinx-serialization-json</td>
<td>1.7.x</td>
<td>**dùng dứt khoát, bỏ Gson** — khớp converter Retrofit đã cấu hình</td>
</tr>
</table>
### 17.3. Network, Socket & Storage
<table header-row="true">
<tr>
<td>Thư viện</td>
<td>Version</td>
<td>Mục đích</td>
</tr>
<tr>
<td>com.squareup.retrofit2:retrofit</td>
<td>2.11.x</td>
<td>REST</td>
</tr>
<tr>
<td>com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter</td>
<td>1.0.0</td>
<td></td>
</tr>
<tr>
<td>com.squareup.okhttp3:okhttp</td>
<td>4.12.x</td>
<td>HTTP + CookieJar + Authenticator</td>
</tr>
<tr>
<td>com.squareup.okhttp3:logging-interceptor</td>
<td>4.12.x</td>
<td>debug</td>
</tr>
<tr>
<td>io.socket:[socket.io](http://socket.io)-client</td>
<td>2.1.x</td>
<td>[Socket.IO](http://Socket.IO)</td>
</tr>
<tr>
<td>[androidx.room](http://androidx.room):room-runtime / room-ktx</td>
<td>2.6.x</td>
<td></td>
</tr>
<tr>
<td>androidx.datastore:datastore-preferences</td>
<td>1.1.x</td>
<td></td>
</tr>
<tr>
<td>io.coil-kt:coil-compose</td>
<td>2.7.x</td>
<td>hiển thị ảnh (upload dùng OkHttp trực tiếp lên S3, mục 15)</td>
</tr>
</table>
### 17.4. Testing
<table header-row="true">
<tr>
<td>Thư viện</td>
<td>Version</td>
<td>Mục đích</td>
</tr>
<tr>
<td>junit:junit</td>
<td>4.13.x</td>
<td></td>
</tr>
<tr>
<td>io.mockk:mockk</td>
<td>1.13.x</td>
<td></td>
</tr>
<tr>
<td>[app.cash](http://app.cash).turbine:turbine</td>
<td>1.2.x</td>
<td>test Flow/StateFlow</td>
</tr>
<tr>
<td>org.jetbrains.kotlinx:kotlinx-coroutines-test</td>
<td>1.8.x</td>
<td></td>
</tr>
<tr>
<td>com.squareup.okhttp3:mockwebserver</td>
<td>4.12.x</td>
<td>test REST + CookieJar/Authenticator</td>
</tr>
<tr>
<td>androidx.compose.ui:ui-test-junit4</td>
<td>BOM</td>
<td></td>
</tr>
<tr>
<td>[com.google](http://com.google).dagger:hilt-android-testing</td>
<td>2.51.x</td>
<td></td>
</tr>
<tr>
<td>com.squareup.leakcanary:leakcanary-android</td>
<td>2.14</td>
<td>debugOnly</td>
</tr>
</table>
---
## 18. Bảo mật & Anti-Cheat phía Client
### 18.1. Auth & Cookie
- Cookie **HttpOnly** — app không tự đọc được giá trị token qua code Kotlin thông thường, chỉ OkHttp `CookieJar` quản lý; điều này thực ra **an toàn hơn** JWT tự lưu trong DataStore như v1.0 vì hạn chế rò rỉ token qua log/crash report.
- Không hardcode `SOCKET_JWT_SECRET` hay bất kỳ secret nào phía client — client **chỉ nhận** `socketToken` đã ký sẵn từ REST, không tự tạo.
- `socketToken` có TTL ngắn (`SOCKET_TOKEN_TTL`) — không cache dài hạn, hết hạn giữa game phải gọi lại REST `/join` hoặc `/host-token`.
### 18.2. Anti-cheat phía client (giữ nguyên tinh thần v1.0, chính xác hoá theo backend)
- Disable input ngay sau khi emit `question:answer` (trước cả khi có ACK) để tránh double-submit — dù server đã tự chặn (`recordAnswer(..., allowChange=false)` → trả lỗi `CONFLICT: answer already submitted`), UI vẫn nên khóa sớm cho trải nghiệm mượt.
- **Không bao giờ** tự tính điểm/đúng-sai ở client để hiển thị "dự đoán" trước khi có phản hồi server — mọi con số hiển thị phải đến từ `AnswerAck`, `question:results`, hoặc `leaderboard:updated/host`.
- Với host-paced, Player **không nhận** `correct_answer` cho tới khi `question:results` bắn ra sau `question:locked` — không lưu, không log payload trung gian nào chứa đáp án ở phía client trước thời điểm đó.
### 18.3. Network Security
```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.myquizz.dpdns.org</domain>
    </domain-config>
</network-security-config>
```
---
## 19. Observability & Debug Tools
<table header-row="true">
<tr>
<td>Tool</td>
<td>Mục đích</td>
</tr>
<tr>
<td>Timber</td>
<td>Logging, tắt ở release</td>
</tr>
<tr>
<td>OkHttp Logging Interceptor</td>
<td>Log request/response, **redact cookie header** ở log production build</td>
</tr>
<tr>
<td>LeakCanary</td>
<td>Phát hiện leak, đặc biệt quan trọng vì `Socket` factory tạo instance mới mỗi phiên — dễ leak nếu quên `disconnect()`</td>
</tr>
<tr>
<td>Compose Layout Inspector</td>
<td>Debug recomposition</td>
</tr>
<tr>
<td>Room Database Inspector</td>
<td>Query cache/history trực tiếp</td>
</tr>
<tr>
<td>Firebase Crashlytics (tuỳ chọn)</td>
<td>Crash reporting production</td>
</tr>
</table>
### 19.1. Build Variants
> ⚠️ Backend gắn [Socket.IO](http://Socket.IO) **cùng một ****`httpServer`****/cổng** với Express (`createServer(app)` → `new Server(httpServer)` → `httpServer.listen(port)` trong `app.ts`) — `BASE_URL` và `SOCKET_URL` thực chất trỏ **chung 1 host:port**, chỉ khác đường dẫn: REST nằm dưới prefix `/v1` (không có `/api`), [Socket.IO](http://Socket.IO) handshake ở path mặc định `/socket.io/`. Vẫn nên khai 2 `BuildConfig` field riêng cho rõ nghĩa (Retrofit cần `baseUrl` có path `/v1/`, còn `IO.socket()` cần URL gốc không kèm path), nhưng đừng nhầm là 2 server khác nhau.
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/v1/\"")
        buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:3000\"")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://api.myquizz.dpdns.org/v1/\"")   // path prefix /v1
        buildConfigField("String", "SOCKET_URL", "\"https://api.myquizz.dpdns.org\"")     // KHÔNG có /v1 — lib tự nối /socket.io/
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
}
```
> 💡 `10.0.2.2` là địa chỉ host machine từ Android Emulator (tương đương `localhost`).
---
## 20. Checklist tổng kết
### 20.1. Architecture checklist
- [ ] Clean Architecture: UI → ViewModel → UseCase → Repository → DataSource
- [ ] MVI: Intent → UiState → Composable, không two-way binding
- [ ] Hilt DI: toàn bộ dependency được inject
- [ ] Kotlin Flow cho mọi async (socket, DB, preferences)
- [ ] Type-safe Navigation (Serializable routes)
- [ ] **Tách rõ ViewModel/UiState Host và Player** — không dùng chung 1 state machine
### 20.2. Auth checklist
- [ ] `CookieJar` persist qua DataStore/Room, không lưu token dạng string đơn
- [ ] `Authenticator` xử lý 401 → `/auth/refresh` → retry, không dùng Interceptor gắn Bearer
- [ ] Google Sign-In dùng Credential Manager + `/auth/google/one-tap`, không dùng redirect flow
- [ ] Trạng thái login xác định qua `GET /users/me`, không suy đoán từ local storage
### 20.3. Real-time checklist
- [ ] Lấy `socketToken` qua REST (`/join` hoặc `/host-token`) **trước** khi connect [Socket.IO](http://Socket.IO)
- [ ] [Socket.IO](http://Socket.IO) `auth: {token}` ở handshake, không emit `roomCode/nickname` thủ công
- [ ] Bảng event client↔server khớp đúng tên với `game.socket.ts` (mục 5.2–5.3)
- [ ] Phân biệt rõ payload Host room (`host:question`, `host:answer-received`, `leaderboard:host`) và Player room
- [ ] Xử lý đúng 5 game mode qua `config.flow.pacing`, không hardcode 1 state machine tuyến tính
- [ ] `awaitClose { socket.off(); socket.disconnect() }` — cleanup khi Flow bị cancel
- [ ] Reconnection: re-emit `lobby:join`, xử lý `question:awaiting_next` cho self-paced
### 20.4. Testing checklist
- [ ] ViewModel: Turbine + Fake Repository cho **cả** nhánh host-paced và self-paced
- [ ] UseCase: pure Kotlin, JUnit + MockK
- [ ] Repository: MockWebServer test luồng cookie/401/refresh; Room in-memory cho cache
- [ ] UI: Compose UI Test cho GamePlay (host-paced), GamePlay (self-paced), HostGame, Lobby
---
*Tài liệu này đối chiếu trực tiếp với source code backend tại thời điểm phân tích (**`Ntd1411/myquizz`**, nhánh mặc định). Khi backend đổi tên event/route hoặc thêm mode mới, cần cập nhật lại mục 4–6.*
