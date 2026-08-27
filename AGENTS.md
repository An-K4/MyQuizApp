# AGENTS.md — Hướng dẫn cho agent làm việc tiếp trên dự án MyQuizApp

> File này không thay thế `myquizz-review-backend-ke-hoach-50-ngay.md` (kế hoạch + trạng thái chi tiết từng ngày) — đây là tập hợp **quy tắc làm việc + kinh nghiệm + lưu ý** rút ra sau nhiều phiên, giúp agent mới khởi đầu nhanh hơn và không lặp lại sai lầm cũ. Đọc file này **trước**, rồi đọc file kế hoạch để biết đang ở đâu.
>
> Cập nhật lần cuối: 25/8/2026, sau khi hoàn thành N16 + N16.5 (chốt M3 + vá 3 điểm lệch khẩn).

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
- **Component `Avatar` chung** ở `core:ui/components/Avatar.kt` — bất kỳ đâu cần hiển thị avatar user thì dùng lại component này (qua dependency `core:ui`), **không** tự thêm `coil.compose` riêng ở module gọi — Coil là chi tiết nội bộ của `core:ui`.
- **Paging 3** page-size = 3 là quy ước chung cho các list quiz (Home search, quiz-manage list...).
- **Auth dùng cookie-based session**, không phải Bearer token — `PersistentCookieJar` gắn sẵn vào `OkHttpClient` chuẩn của app cho mọi request tới backend thật.
- **Room cache là fallback/cache, không phải tính năng offline-first** — đừng thiết kế luồng đọc giả định app phải hoạt động đậy đủ khi offline.
- **`ProfileScreen` đặt ở module `app`**, không phải `feature:*`, vì gắn `Route.Profile` cấp app.
- **Chưa có Bottom Navigation thật** (design doc nói 5 tab Home/Discover/Join/Library/Profile) — hiện điều hướng qua `NavController` thông thường. Còn tồn tại trùng lặp `Route.Library` vs `Route.MyQuizzes` chưa giải quyết.

---

## 3. Bẫy (traps) đã gặp thật — đừng lặp lại

### 3.1. `JsonNamingStrategy.SnakeCase` đè lên `@SerialName` (N15, xem chi tiết ở `knowledgement/n15_knowledgement.md`)

App dùng 1 `Json` chung với `namingStrategy = JsonNamingStrategy.SnakeCase` cho đa số endpoint backend (vì backend chủ yếu dùng snake_case). **Sai lầm tưởng rằng khai `@SerialName("contentType")` tường minh sẽ "thoát" được namingStrategy chung** — KHÔNG đúng, vì `namingStrategy` áp transform lên **tên serial đã resolve** (tức chính là giá trị `@SerialName` khi có khai báo), không phân biệt đó là tên gốc hay tên đã override.

→ **Khi 1 nhóm endpoint có naming convention khác với phần còn lại của backend (ví dụ module storage dùng camelCase thuần), hãy tạo cặp `Json`/`Retrofit` riêng không set `namingStrategy` cho `ApiService` đó ngay từ đầu** — đừng rải `@SerialName` từng field rồi tin là đủ. Pattern mẫu: `@StorageJson`/`@StorageRetrofit` trong `Qualifiers.kt` + `NetworkModule.kt`. N16.5 gặp lại đúng bẫy này ở **module user** (camelCase thật: `resetTime`/`expiresAt`/`newPassword`) → đã tái dùng pattern với `@PasswordResetJson`/`@PasswordResetRetrofit` + `PasswordResetApiService`.

→ Khi debug lỗi request/response không khớp kỳ vọng, luôn **xem Logcat OkHttp level BODY** để thấy byte thật trên wire, đừng chỉ đọc lại code DTO rồi kết luận "nhìn code thì đúng rồi".

### 3.2. Backend hay trả response bọc lồng thêm 1 tầng resource key

Nhiều endpoint không chỉ trả `ApiEnvelope<T>` chuẩn mà còn lồng thêm 1 key resource bên trong `data`, ví dụ: `{quiz: {...}}`, `{session: {...}}`, `{quizzes: [...]}`, `{presignedUrl: {...}}` (xem N15). → Khi viết DTO cho response mới, **luôn kiểm tra kỹ backend thật trả field gọc hay bọc trong 1 key con** — không giả định flat theo design doc.

### 3.3. Gọi thẳng URL bên thứ 3 (không phải backend của mình)

Khi cần PUT/GET trực tiếp lên 1 presigned URL (S3...) hay bất kỳ domain bên thứ 3 nào, **không dùng lại `OkHttpClient` chuẩn có cookieJar/authenticator của app** — tạo `OkHttpClient` riêng không mang theo thông tin đăng nhập nội bộ (xem `@RawUploadOkHttpClient`).

### 3.4. Flag "skip first resume" trong `remember` bị reset khi Navigation dispose composition (N16)

Màn cần "reload khi quay lại" (ON_RESUME) nhưng bỏ qua lần mở đầu → đừng giữ flag skip bằng `remember`: Navigation dispose composition của màn cũ khi điều hướng đi, nên flag reset mỗi lần quay lại và reload không bao giờ chạy. Fix: `rememberSaveable` (state ghi vào SavedState của NavBackStackEntry) hoặc giữ flag trong ViewModel. Với list Paging 3, đừng dựa vào `LazyPagingItems.refresh()` (không đáng tin khi flow đã `cachedIn`) — cho refresh đi qua ViewModel bằng intent và tạo Pager mới (pattern generation). Chi tiết: `knowledgement/n16_knowledgement.md` mục 1.

### 3.5. Mọi user action phải có ít nhất 1 đường kích hoạt THẬT từ UI (bug sót N11–12, lộ ra khi test N16.5)

SearchScreen gõ từ khóa nhưng "không có kết quả với mọi từ khóa": intent `SubmitSearch` tồn tại đầy đủ trong ViewModel nhưng **không có nút/handler nào trong UI gọi nó** (không nút tìm, không `keyboardActions`) — compile sạch, logic đúng, luồng chết vì thiếu mắt xích cuối. Tương tự click quiz card trỏ vào hàm TODO rỗng. → Khi viết/review màn mới, **trace ngược từ handler về UI**: mỗi intent phải có ít nhất 1 chỗ gọi thật. Đừng tin "nhìn code thì chắc chạy" — test luồng thật trên máy. Chi tiết: `knowledgement/n16_5_knowledgement.md` mục 4.

---

## 4. Quy tắc quy trình làm việc với user

- User thường test trên **máy thật** sau khi agent báo "xong" — luồng có thể vẫn có bug trên máy thật dù build/compile sạch (ví dụ N15). Đừng coi "compile thành công" là bằng chứng đầy đủ feature hoạt động đúng — khi user gửi Logcat báo lỗi, đọc log thật (không suy đoán) để xác định đúng rõ lỗi backend hay client trước khi sửa.
- User ghi chép tiến độ chi tiết trong `myquizz-review-backend-ke-hoach-50-ngay.md` theo từng N (ngày/milestone) — sau mỗi N hoàn thành: (1) tích `[x]` checklist, (2) thêm block "📝 N.. Implementation Details" ngay sau block N đó, (3) cập nhật blockquote tóm tắt ở đầu file, (4) nếu N có nhiều để học/có bug thật, viết thêm file `knowledgement/nXX_knowledgement.md` chi tiết hơn (không phải N nào cũng cần file riêng — N12 không có file vì gọn gàng đủ trong plan).
- User thích **khảo sát/hỏi ý kiến trước khi code** cho các quyết định có nhiều hướng hợp lý (ví dụ N15: hỏi trước về upload deferred/avatar/compression) — dùng survey nhiều lựa chọn thay vì tự quyết.
- Giao tiếp với user bằng tiếng Việt (user viết tiếng Việt, các file `.md` của dự án cũng viết tiếng Việt).
- Đây là **local filesystem qua MCP** (git repo trên đĩa, không phải Notion) — quy tắc `edit-diffs.md`/`editDescriptionVariableName` của Notion không áp dụng cho các thay đổi file `.kt`/`.md` này.

---

## 5. Tooling quirks trong môi trường MCP này (đã gặp thật, không phải lý thuyết)

- **`search__find_path` hay trả false negative** ("nothing matched") cho tên file thật sự tồn tại trong repo — đừng tin ngay kết quả không tìm thấy, thử `filesystem__list_directory`/`directory_tree` theo đường dẫn cấp trên trước.
- **`shell__run_cmd` chạy git bị chặn bởi allowlist** (ví dụ `git branch` bị từ chối "not in the allowlist") — không dùng git command qua shell để kiểm tra branch/status, hỏi user trực tiếp nếu cần biết branch hiện tại.
- **`filesystem__*` tools có thể flaky tạm thời với đường dẫn sâu** (`core/*/src/main/java/...`, `feature/*/src/main/java/...`) — từng gặp lỗi "Parent directory does not exist" cho chính path đã đọc thành công ngay trước đó trong cùng turn, kể cả `list_directory` cấp `core/network` chỉ trả về `[DIR] build` (thiếu `src`). Path gần root (ví dụ file `.md` ở root) thì ổn định hơn. Nếu gặp lại: thử lại sau, hoặc dựa vào nội dung file đã xác nhận được từ trước đó trong phiên làm việc thay vì block cả task chờ tool phục hồi.
- `filesystem__edit_file` dùng `{path, edits:[{oldText,newText}], dryRun?}` — `oldText` phải khớp chính xác từng byte (kể cả line ending) với nội dung file thật đã đọc, không phải bản "viết lại cho đẹp" của agent.

---

## 6. Trạng thái hiện tại (tính đến 25/8/2026) — xem chi tiết ở file kế hoạch chính

- Tuần 1–2 (N1–10) + Tuần 3 (N11–15) hoàn thành; **N16 + N16.5 xong 25/8 → M3 (Quiz CRUD) chốt và 3 điểm lệch khẩn #7/#8/#9 đã vá hết**. Việc tiếp theo: **N17** (CreateRoomScreen động từ `/games/game-modes`).
- Các việc bị defer còn treo: avatar upload (dùng lại cơ chế presign của N15), Bottom Navigation thật, refresh-token use case, trùng lặp `Route.Library`/`Route.MyQuizzes`, review lại padding `SplashScreen` (80dp), backlog "Editor UX gaps" (duplicate/move câu hỏi, autosave draft, default cover từ `res/`, crop ảnh, validation inline — xem block N16 trong file kế hoạch), 2 file usecase stub của luồng reset cũ chờ xóa tay trong IDE.
- Bài học N16/N16.5 đáng nhớ: flag `remember` bị reset khi Navigation dispose composition → `rememberSaveable` (mục 3.4); DELETE quiz là **hard delete** (điểm lệch #10); PATCH không clear được field về null; module user backend dùng **camelCase thật** → tách Json/Retrofit theo pattern N15 (mục 3.1); mọi intent phải có đường kích hoạt thật từ UI (mục 3.5).
- File kế hoạch chính: `myquizz-review-backend-ke-hoach-50-ngay.md` (root). Thư mục `knowledgement/` chứa bài học chi tiết từng giai đoạn — đọc file `nXX_knowledgement.md` tương ứng khi cần hiểu sâu lại quyết định của một giai đoạn cụ thể.

---

*Agent mới: hãy đọc file này + blockquote tóm tắt đầu `myquizz-review-backend-ke-hoach-50-ngay.md` trước khi hỏi user "ta đang ở đâu". Chúc may mắn!*
