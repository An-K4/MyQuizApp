# AGENTS.md — Hướng dẫn cho agent làm việc tiếp trên dự án MyQuizApp

> File này không thay thế `myquizz-review-backend-ke-hoach-50-ngay.md` (kế hoạch + trạng thái chi tiết từng ngày) — đây là tập hợp **quy tắc làm việc + kinh nghiệm + lưu ý** rút ra sau nhiều phiên, giúp agent mới khởi đầu nhanh hơn và không lặp lại sai lầm cũ. Đọc file này **trước**, rồi đọc file kế hoạch để biết đang ở đâu.
>
> Cập nhật lần cuối: 28/8/2026, sau khi hoàn thành N17 và migration Stateful/Stateless trên toàn bộ UI hiện có.

---

## 1. Luật số 1 (quan trọng nhất): KHÔNG tin tuyệt đối file thiết kế

User đã nói rõ: **đừng tin tuyệt đối `quiz-app-android-design-document-v2.md`**, vì backend (`github.com/Ntd1411/myquizz`) có thể có commit/cập nhật mới bất cứ lúc nào. Design doc rất có thể đã lệc thời và chỉ mang tính tham khảo, **không phải tài liệu triển khai tuyệt đối**.

→ **Trước khi implement bất kỳ tính năng nào đụng chạm backend (endpoint, schema request/response, socket event...), luôn đọc source backend thật trước** (controller/route/schema/service thật trong repo backend), rồi mới code client theo đúng cái đọc được — không suy diễn từ design doc.

Đã có nhiều lần design doc khớp ~90% nhưng không hoàn toàn (xem mục 4 file kế hoạch chính — "6 điểm lệch"), và N15 cho thấy ngay cả khi backend đúng chuẩn, **client vẫn có thể tữ sinh bug** nếu tin nhầm giả định của chính mình (xem mục 3 dưới).

---

## 2. Kiến trúc & quy ước đã chốt (áp dụng nhất quán, không bửa lại)

- **Clean Architecture + DIP**: interface domain đặt ở `core:common`, implementation đặt ở `core:network` (API thật) hoặc `core:database` (Room cache). Tầng trên (feature/ViewModel) chỉ phụ thuệc interface, không bao giờ import trực tiếp `core:network`/`core:database`.
- **UiState/Intent/Effect tách file riêng** cho mọi ViewModel (quy ước từ sau refactor `feature:auth` 22/8) — không gộp chung 1 file ViewModel khổng lồ.
- **`Result<T>`** là wrapper chuẩn xuyên suốt domain/data (không throw exception qua boundary layer), pattern generic 1 tham số.
- **`ResultCallAdapterFactory`** (Retrofit call adapter tự viết) xử lý `ApiEnvelope` chuẩn của backend ở tầng network, trả thẳng `Result<T>` lên trên — không phải mọi Repository tự unwrap envelope.
- **Component dùng chung ở `core:ui`**: dùng `Avatar` cho avatar và `SettingSwitchRow` cho pattern text/description + switch; component core chỉ nhận primitive UI props, không import model/intent của feature.
- **Paging 3** page-size = 3 là quy ước chung cho các list quiz (Home search, quiz-manage list...).
- **Auth dùng cookie-based session**, không phải Bearer token — `PersistentCookieJar` gắn sẵn vào `OkHttpClient` chuẩn của app cho mọi request tới backend thật.
- **Room cache là fallback/cache, không phải tính năng offline-first** — đừng thiết kế luồng đọc giả định app phải hoạt động đậy đủ khi offline.
- **`ProfileScreen` đặt ở module `app`**, không phải `feature:*`, vì gắn `Route.Profile` cấp app.
- **Chưa có Bottom Navigation thật** (design doc nói 5 tab Home/Discover/Join/Library/Profile) — hiện điều hướng qua `NavController` thông thường. Còn tồn tại trùng lặp `Route.Library` vs `Route.MyQuizzes` chưa giải quyết.

### 2.1. Quy ước Stateful/Stateless cho mọi Screen và UI Component

Lấy `feature:auth/.../login/LoginScreen.kt` làm mẫu chuẩn. Mỗi màn hình phải tách rõ hai lớp:

- **Stateful/Route composable** — thường mang tên `XxxScreen`: là integration boundary; được phép lấy `hiltViewModel()`, `collectAsStateWithLifecycle()`, collect `Effect` trong `LaunchedEffect`, giữ UI state cục bộ bằng `remember`/`rememberSaveable`, dùng `Context` hoặc platform API, và chuyển effect thành navigation/snackbar. Hàm này không dựng UI chi tiết; sau khi wiring xong phải gọi composable stateless.
- **Stateless content composable** — thường mang tên `XxxScreenContent`: chỉ nhận `UiState`/primitive value và callback/intent từ ngoài, không tự lấy ViewModel, không gọi Hilt, không collect Flow, không sở hữu `NavController`, không tự điều hướng và không thực hiện business logic/I/O. State phải được hoist để hàm dễ Preview và test.
- **Reusable component**: stateless by default; nhận value + callback thay vì giữ source of truth bên trong. Nếu thật sự cần stateful convenience wrapper cho state UI cục bộ, phải giữ một stateless overload/content làm API lõi và đặt tên hai hàm rõ ràng.
- **Preview** gọi trực tiếp composable stateless với fake state/no-op callbacks; tối thiểu có trạng thái đại diện, ưu tiên cả Light/Dark như Login. Không Preview thông qua ViewModel/Hilt/navigation.
- **Event flow**: UI event đi lên qua callback hoặc typed `Intent`; state đi xuống qua parameter. One-off effect chỉ được xử lý ở stateful boundary.
- Có thể giữ state thuần hiển thị như password visibility ở stateful layer; state ảnh hưởng nghiệp vụ, validation hoặc cần sống qua process/navigation phải thuộc `UiState`/ViewModel. Dùng `rememberSaveable` khi state cần sống qua recreation/back-stack disposal.

Khi tạo hoặc review screen/component mới, nếu không có lý do kỹ thuật được ghi rõ thì sai pattern trên được coi là architecture debt và phải sửa trước khi merge.

**Baseline đã hoàn thành ngày 28/8/2026:** đã quét 14 `*Screen.kt` hiện có. `LoginScreen`, `RegisterScreen`, `SplashScreen` vốn đúng pattern; 11 màn còn lại đã được chuẩn hóa về `XxxScreen` + `XxxScreenContent`. Các reusable component giữ state cục bộ như menu/dropdown phải có stateless content lõi. `CreateQuizScreen` và `EditQuizScreen` dùng chung `QuizEditorContent` trong `feature:quiz-manage/presentation/components`.

**Ownership cho các trường hợp đặc biệt:**
- Photo Picker/Activity Result launcher, `BackHandler`, lifecycle effect, focus request và quan sát pagination bằng `snapshotFlow` nằm ở stateful boundary.
- Stateless content được phép nhận UI object do wrapper sở hữu như `LazyListState`, `LazyPagingItems` hoặc `SnackbarHostState`; content không được tự lấy ViewModel, collect Flow, điều hướng hay thực hiện business I/O.
- State hiển thị tạm thời như password visibility, menu expanded, dialog expanded có thể ở wrapper; state ảnh hưởng nghiệp vụ vẫn phải ở `UiState`/ViewModel.
- Preview gọi trực tiếp content với fake state/no-op callback; với Paging có thể dùng `PagingData.empty()`.

Chi tiết audit, inventory và checklist ở `knowledgement/ui_stateful_stateless_refactor.md`.

---

## 3. Bẫy (traps) đã gặp thật — đừng lặp lại

### 3.1. `JsonNamingStrategy.SnakeCase` đè lên `@SerialName` (N15, xem chi tiết ở `knowledgement/n15_knowledgement.md`)

App dùng 1 `Json` chung với `namingStrategy = JsonNamingStrategy.SnakeCase` cho đa số endpoint backend (vì backend chủ yếu dùng snake_case). **Sai lầm tưởng rằng khai `@SerialName("contentType")` tường minh sẽ "thoát" được namingStrategy chung** — KHÔNG đúng, vì `namingStrategy` áp transform lên **tên serial đã resolve** (tức chính là giá trị `@SerialName` khi có khai báo), không phân biệt đó là tên gốc hay tên đã override.

→ **Khi endpoint có naming convention khác phần lớn backend, dùng Retrofit không naming strategy** — đừng rải `@SerialName` rồi tin là đủ. Sau refactor trước N17, Storage, Password Reset và Games dùng chung `@PreserveCaseJson`/`@PreserveCaseRetrofit`; chỉ tạo qualifier/client riêng khi behavior thực sự khác (ví dụ `@RawUploadOkHttpClient` không cookie/authenticator), không tạo một cặp Retrofit cho từng feature.

→ Khi debug lỗi request/response không khớp kỳ vọng, luôn **xem Logcat OkHttp level BODY** để thấy byte thật trên wire, đừng chỉ đọc lại code DTO rồi kết luận "nhìn code thì đúng rồi".

### 3.2. Backend hay trả response bọc lồng thêm 1 tầng resource key

Nhiều endpoint không chỉ trả `ApiEnvelope<T>` chuẩn mà còn lồng thêm 1 key resource bên trong `data`, ví dụ: `{quiz: {...}}`, `{session: {...}}`, `{quizzes: [...]}`, `{presignedUrl: {...}}` (xem N15). → Khi viết DTO cho response mới, **luôn kiểm tra kỹ backend thật trả field gọc hay bọc trong 1 key con** — không giả định flat theo design doc.

### 3.3. Gọi thẳng URL bên thứ 3 (không phải backend của mình)

Khi cần PUT/GET trực tiếp lên 1 presigned URL (S3...) hay bất kỳ domain bên thứ 3 nào, **không dùng lại `OkHttpClient` chuẩn có cookieJar/authenticator của app** — tạo `OkHttpClient` riêng không mang theo thông tin đăng nhập nội bộ (xem `@RawUploadOkHttpClient`).

### 3.4. Flag "skip first resume" trong `remember` bị reset khi Navigation dispose composition (N16)

Màn cần "reload khi quay lại" (ON_RESUME) nhưng bỏ qua lần mở đầu → đừng giữ flag skip bằng `remember`: Navigation dispose composition của màn cũ khi điều hướng đi, nên flag reset mỗi lần quay lại và reload không bao giờ chạy. Fix: `rememberSaveable` (state ghi vào SavedState của NavBackStackEntry) hoặc giữ flag trong ViewModel. Với list Paging 3, đừng dựa vào `LazyPagingItems.refresh()` (không đáng tin khi flow đã `cachedIn`) — cho refresh đi qua ViewModel bằng intent và tạo Pager mới (pattern generation). Chi tiết: `knowledgement/n16_knowledgement.md` mục 1.

### 3.5. Mọi user action phải có ít nhất 1 đường kích hoạt THẬT từ UI (bug sót N11–12, lộ ra khi test N16.5)

SearchScreen gõ từ khóa nhưng "không có kết quả với mọi từ khóa": intent `SubmitSearch` tồn tại đầy đủ trong ViewModel nhưng **không có nút/handler nào trong UI gọi nó** (không nút tìm, không `keyboardActions`) — compile sạch, logic đúng, luồng chết vì thiếu mắt xích cuối. Tương tự click quiz card trỏ vào hàm TODO rỗng. → Khi viết/review màn mới, **trace ngược từ handler về UI**: mỗi intent phải có ít nhất 1 chỗ gọi thật. Đừng tin "nhìn code thì chắc chạy" — test luồng thật trên máy. Chi tiết: `knowledgement/n16_5_knowledgement.md` mục 4.

### 3.6. Dynamic backend contract không đồng nghĩa render UI bằng raw map (N17)

Bản đầu Create Room đưa `Map<String, JsonElement>` và dotted path xuyên từ DTO tới UiState/Intent/Composable, rồi dùng `configLabel(path)` để vá readability. Luồng vẫn one-way MVI nhưng vi phạm boundary sạch, mất type-safety và khó đọc cấu hình từng mode.

→ JSON/snake_case/dotted path chỉ tồn tại trong `core:network`; domain dùng `GameConfigKey`/`GameConfigValue`/typed constraint; presentation dùng typed form. UI dispatch theo mode (`ClassicModeEditor`, `SoloModeEditor`...) để sở hữu layout/label, trong khi backend descriptor vẫn sở hữu default/min/max/nullable/options. Với conflict descriptor, **`locked` luôn thắng `editable`**.

→ Flow tạo phòng gồm 2 REST request (`create game` rồi `host-token`): nếu request 1 thành công và request 2 lỗi, giữ `pendingSession`; retry chỉ request token, không tạo room lần hai. Chi tiết: `knowledgement/n17_knowledgement.md`.

---

### 3.7. Socket: contract lỗi thật là `{event, code}`, và reconnect KHÔNG tự join lại room (N18, xem `knowledgement/n18_knowledgement.md`)

- Doc thiết kế ghi payload lỗi socket là `{event, message}` với prefix `UNAUTHORIZED:`/`FORBIDDEN:`/`CONFLICT:`/`GONE:`. **Sai.** Schema `SocketError` thật (`backend/src/docs/components/socket.doc.ts`) là `{ event: string, code: string }` — chỉ có code, không có message. Đừng parse prefix từ message.
- Lỗi chỉ bắn qua event `error` khi client event KHÔNG có ack. Nếu event có ack (ví dụ `question:answer`) thì lỗi trả trong ack dưới dạng `{ error: { code } }`. Bắt một đường sẽ bỏ sót đường kia.
- Không tạo nhánh `AppError` mới cho socket: `AppError.Api(code)` đã map sẵn ~60 code sang tiếng Việt từ N16.5.
- **socket.io tự reconnect nhưng KHÔNG tự join lại room.** Phải gọi `lobby:join` sau MỌI lần nhận `Connected`, không phải một lần trong `init`. Nếu không, sau khi mạng trở lại socket vẫn "connected" mà không bao giờ nhận `lobby:updated` nữa — bug im lặng, không lộ ra khi test ở mạng tốt.
- Phân biệt 3 lý do disconnect: `io server disconnect` (server đá — socket.io KHÔNG retry, phải thoát màn), `io client disconnect` (do chính mình gọi — im lặng), còn lại là transport/mất mạng (giữ dữ liệu cũ, chờ socket.io tự thử lại).
- Mapper event không bao giờ được throw: payload rác → trả `GameEvent.Failed(event, "CLIENT_PARSE_ERROR")`. Throw trong `callbackFlow` sẽ giết cả flow và mất mọi event sau đó.
- Token socket có TTL riêng, ngắn hơn cookie đăng nhập. Gặp `GAME_TOKEN_INVALID` → refresh qua REST đúng MỘT lần (có guard chống vòng lặp refresh) rồi thoát nếu vẫn fail.
- UiState realtime phải phân biệt "chưa có snapshot" với "snapshot rỗng" (cờ `hasLobbySnapshot`), và khi mất kết nối thì giữ nguyên dữ liệu cũ thay vì xóa về rỗng.

## 4. Quy tắc quy trình làm việc với user

- User thường test trên **máy thật** sau khi agent báo "xong" — luồng có thể vẫn có bug trên máy thật dù build/compile sạch (ví dụ N15). Đừng coi "compile thành công" là bằng chứng đầy đủ feature hoạt động đúng — khi user gửi Logcat báo lỗi, đọc log thật (không suy đoán) để xác định đúng rõ lỗi backend hay client trước khi sửa.
- User ghi chép tiến độ chi tiết trong `myquizz-review-backend-ke-hoach-50-ngay.md` theo từng N (ngày/milestone) — sau mỗi N hoàn thành: (1) tích `[x]` checklist, (2) thêm block "📝 N.. Implementation Details" ngay sau block N đó, (3) cập nhật blockquote tóm tắt ở đầu file, (4) nếu N có nhiều để học/có bug thật, viết thêm file `knowledgement/nXX_knowledgement.md` chi tiết hơn (không phải N nào cũng cần file riêng — N12 không có file vì gọn gàng đủ trong plan).
- User thích **khảo sát/hỏi ý kiến trước khi code** cho các quyết định có nhiều hướng hợp lý (ví dụ N15: hỏi trước về upload deferred/avatar/compression) — dùng survey nhiều lựa chọn thay vì tự quyết.
- Giao tiếp với user bằng tiếng Việt (user viết tiếng Việt, các file `.md` của dự án cũng viết tiếng Việt).
- Đây là **local filesystem qua MCP** (git repo trên đĩa, không phải Notion) — quy tắc `edit-diffs.md`/`editDescriptionVariableName` của Notion không áp dụng cho các thay đổi file `.kt`/`.md` này.

---

## 5. Tooling quirks trong môi trường MCP này (đã gặp thật, không phải lý thuyết)

- **`search__find_path` hay trả false negative** ("nothing matched") cho tên file thật sự tồn tại trong repo — đừng tin ngay kết quả không tìm thấy, thử `filesystem__list_directory`/`directory_tree` theo đường dẫn cấp trên trước.
- **`shell__run_cmd` chỉ cho phép một phần lệnh git**: hiện `git status`/`log`/`diff`/`show` chạy được, nhưng `git branch`, `git grep`, command chaining/redirection hoặc Gradle wrapper có thể bị allowlist chặn. Dùng lệnh được hỗ trợ trước; nếu cần build thì nhờ user chạy local và gửi log thật.
- **`filesystem__*` tools có thể flaky tạm thời với đường dẫn sâu** (`core/*/src/main/java/...`, `feature/*/src/main/java/...`) — từng gặp lỗi "Parent directory does not exist" cho chính path đã đọc thành công ngay trước đó trong cùng turn, kể cả `list_directory` cấp `core/network` chỉ trả về `[DIR] build` (thiếu `src`). Path gần root (ví dụ file `.md` ở root) thì ổn định hơn. Nếu gặp lại: thử lại sau, hoặc dựa vào nội dung file đã xác nhận được từ trước đó trong phiên làm việc thay vì block cả task chờ tool phục hồi.
- `filesystem__edit_file` dùng `{path, edits:[{oldText,newText}], dryRun?}` — `oldText` phải khớp chính xác từng byte (kể cả line ending) với nội dung file thật đã đọc, không phải bản "viết lại cho đẹp" của agent.

---

- `filesystem__create_directory` KHÔNG tạo thư mục lồng nhau dù tên gợi ý ngược lại: phải tạo từng cấp một, tuần tự, nếu không sẽ gặp `Parent directory does not exist`. `write_file` cũng fail cùng lý do khi thư mục cha chưa tồn tại.
- `search__search_content` cho ÂM TÍNH GIẢ: trả "no lines matched" cho chuỗi chắc chắn có trong file vừa đọc. Không bao giờ kết luận "không tồn tại" từ kết quả rỗng của nó — xác minh lại bằng đọc file, `search__find_path` (tool này đáng tin), hoặc `findstr` qua shell.
- Tool đọc/ghi file có thể bị khóa giữa phiên với lỗi "changed its operation type since the last admin approval" (gặp 30/8: `read_text_file`, `read_multiple_files`, `write_file`, `edit_file` và cả GitHub `get_file_contents`/`create_branch`, trong khi user không đổi quyền gì; reconnect server không xóa được, phải đợi phía Notion xử lý — hôm sau tự hết). Khi đó `shell__run_cmd` thường vẫn sống: dùng `findstr /n "^" <file>` để đọc file kèm số dòng và `findstr /n /c:"chuỗi" <file>` để định vị dòng cần sửa. Lưu ý findstr trong môi trường này không mở được wildcard kiểu `thư_mục\*.kt`. **Bài học: khi tool ghi chết giữa việc, trích nguyên văn các đoạn `oldText` cần sửa bằng findstr rồi giữ sẵn, để khi tool sống lại là ghi được ngay không phải đọc lại.**
- Shell bị allowlist chặt: `git status --short` và `findstr` chạy được, nhưng `git rm`, `git ls-files`, `ls`, `del` thì không; không chain/redirect và không chạy được Gradle. **Xóa file và build/test phải nhờ user.**
- Nếu MCP filesystem "mù" toàn bộ source (`ENOENT` trên thư mục chắc chắn tồn tại, `list_directory` chỉ thấy `build/`), rất có thể user chưa checkout nhánh làm việc — hỏi trước khi kết luận cấu trúc dự án.

## 6. Trạng thái hiện tại (tính đến 30/8/2026) — xem chi tiết ở file kế hoạch chính

- Tuần 1–3 (N1–15), N16 + N16.5, **N17 và N18 đã hoàn thành**. Create Room gọi đủ modes → create game → host-token, config typed. Refactor kiến trúc UI trước N18 đã xong: 14 Screen được audit, toàn bộ màn tuân theo Stateful/Stateless baseline. N18 (xong 30/8, đã build và test trên máy thật): socket layer namespace `/game` — `GameEvent` + 3 interface socket tách theo vai trò ở `core:common`, `GameSocketClient` + `GameEventMapper` + 2 impl ở `core:network`, HostLobby thật ở `feature:lobby` thay `HostLobbyPlaceholder` (file placeholder đã xóa). Việc tiếp theo: **N19** (Player lobby: join bằng mã phòng + nickname, bổ sung `player_avatar`/`lives` vào DTO).
- Các việc bị defer còn treo: avatar upload (dùng lại cơ chế presign của N15), Bottom Navigation thật, refresh-token use case, trùng lặp `Route.Library`/`Route.MyQuizzes`, review lại padding `SplashScreen` (80dp), backlog "Editor UX gaps" (duplicate/move câu hỏi, autosave draft, default cover từ `res/`, crop ảnh, validation inline — xem block N16 trong file kế hoạch), 2 file usecase stub của luồng reset cũ chờ xóa tay trong IDE.
- Bài học gần nhất: flag `remember` bị reset khi Navigation dispose composition → `rememberSaveable`; DELETE quiz là hard delete; PreserveCase Retrofit dùng chung cho endpoint camel/mixed case; mọi intent phải có UI trigger thật; dynamic backend schema không được kéo raw JSON/dotted path vào presentation; create game + host-token phải xử lý partial success idempotent; platform/lifecycle/effect ownership phải dừng ở Stateful Screen boundary, còn `XxxScreenContent` là UI thuần để Preview/test.
- File kế hoạch chính: `myquizz-review-backend-ke-hoach-50-ngay.md` (root). Thư mục `knowledgement/` chứa bài học chi tiết từng giai đoạn — đọc file `nXX_knowledgement.md` tương ứng khi cần hiểu sâu lại quyết định của một giai đoạn cụ thể.

---

*Agent mới: hãy đọc file này + blockquote tóm tắt đầu `myquizz-review-backend-ke-hoach-50-ngay.md` trước khi hỏi user "ta đang ở đâu". Chúc may mắn!*
