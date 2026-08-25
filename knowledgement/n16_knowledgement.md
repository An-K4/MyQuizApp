# N16 — Sửa/Xóa quiz (25/8/2026)

Những bài học/phát hiện đáng nhớ từ N16, bổ sung cho block "N16 Implementation Details" trong file kế hoạch chính.

## 1. Bug reload-on-resume: 2 lớp vấn đề (bug thật, user phát hiện khi test)

Triệu chứng: xóa quiz ở QuizDetail → back về "Quiz của tôi" → quiz vẫn hiện; thêm câu hỏi ở EditQuiz → back về QuizDetail → số câu vẫn cũ. Cả 2 màn đều đã có `LifecycleEventEffect(ON_RESUME)` + flag `skipFirstResume` để bỏ qua lần mở đầu.

**Root cause (lớp 1 — lý do reload không bao giờ chạy):** flag `skipFirstResume` nằm trong `remember { mutableStateOf(true) }`. Navigation dispose composition của màn cũ khi điều hướng đi (dù back stack entry còn sống) → quay lại = composition mới → flag reset về `true` → nhánh "bỏ qua lần đầu" chạy lặp mãi → reload không bao giờ xảy ra.

Fix: `rememberSaveable { mutableStateOf(true) }` — state được ghi vào SavedState của NavBackStackEntry nên sống qua việc dispose. (Cách tương đương: giữ flag trong ViewModel — ViewModel sống trong ViewModelStore của entry.)

**Lớp 2 (phòng thủ cho list Paging):** bản đầu màn list gọi `LazyPagingItems.refresh()` — API này đi qua `PagingDataDiffer` và có lịch sử không đáng tin khi flow đã `cachedIn` (nhiều issue về refresh + cachedIn trong Paging 3). Đã đổi sang pattern generation: Screen bắn intent `Refresh` → ViewModel tăng `_refreshGeneration` → `combine` + `flatMapLatest` tạo Pager mới → luôn load lại từ mạng. Vừa chắc chắn hơn, vừa đúng MVI (mọi thay đổi đi qua ViewModel).

```kotlin
private val _refreshGeneration = MutableStateFlow(0)

val quizzes = _uiState
    .map { ... params ... }.distinctUntilChanged()
    .combine(_refreshGeneration) { params, gen -> params to gen }
    .distinctUntilChanged()
    .flatMapLatest { (params, _) -> getMyQuizzesUseCase(params) }  // Pager MỚI mỗi generation
    .cachedIn(viewModelScope)
```

Bài học kiểm chứng chéo: backend `/quizzes/me` KHÔNG cache (comment trong listing.service.ts: "must never serve a stale view") — nên khi list hiện dữ liệu cũ, hãy nghi ngờ client trước, đừng đổ cho server cache.

## 2. DELETE quiz là HARD DELETE (điểm lệch #10 — doc/design doc ghi nhầm "xóa mềm")

`quiz.repository.ts`: `DELETE FROM quizzes ... RETURNING *` + ON DELETE CASCADE → mất questions, quiz_snapshots, game_sessions, player_sessions = mất lịch sử mọi phòng đã chơi. Comment trong `frontend/src/api/quizzes.api.js` cũng ghi nhầm "soft delete". Bảng `quizzes` vẫn có cột `deleted_at` (tàn dư bản cũ) nên nhìn schema dễ hiểu nhầm — phải đọc code repository.

Hệ quả Android: dialog xóa phải cảnh báo rõ "mất vĩnh viễn, kèm lịch sử chơi"; xóa xong phải xóa Room cache (`QuizCacheStore.removeQuiz`) để quiz không "hồi sinh" khi offline.

## 3. PATCH không thể clear field về null — và cách frontend web "né"

Backend `updateQuizMetadata` chỉ update field `!== undefined`; zod `.optional()` từ chối null → không có cách xóa `quiz_image`/`quiz_description` đang có giá trị. Nút "xóa cover" của web (`QuizEditor.vue`) thực chất: set `quiz_image=''` → lúc save `ensureCover()` vẽ lại cover mặc định từ tên+category (`defaultCover.js`, canvas 1600×1000) rồi upload — tức "xóa" = "thay bằng ảnh tự sinh". Android N16 giữ hành vi "xóa cover = giữ cover cũ" + TODO polish dùng ảnh mặc định từ `res/`.

Ngược lại, ảnh **câu hỏi** xóa được thật vì `questions` bị replace toàn bộ (`replaceQuizQuestions`: soft-delete câu cũ + insert câu mới trong 1 transaction) — câu mới không có `question_image` là xong.

## 4. Pre-fill editor cần `correct_answer` từ GET quiz detail

Domain `Question` ban đầu cố ý không có `correct_answer` (anti-cheat). Nhưng backend `getQuizById` select cả `correct_answer` (ai xem detail cũng nhận được — web còn có toggle "Show correct answers"), và màn Sửa cần nó để pre-fill. Giải pháp N16: thêm `correctAnswer: JsonElement?` vào `Question` (wire là union `number[] | string` — JsonElement tránh cần 2 DTO), gameplay vẫn không đụng model này (câu hỏi lúc chơi đi qua socket đã cắt đáp án).

## 5. Shared editor components: callback riêng lẻ, không share Intent

`QuestionEditorCard` dùng chung cho 2 màn Tạo/Sửa nhận ~13 callback riêng lẻ thay vì sealed Intent của 1 màn — vì (a) 2 màn có Intent type riêng theo quy ước MVI của dự án, (b) sealed interface không thể kế thừa chéo package (Kotlin yêu cầu cùng package). Verbose hơn nhưng zero coupling — đúng pattern `QuizEditor.vue` (editor dùng chung, page chỉ khác load/save).

## 6. Field số trong form: giữ String thô ở UI state, parse ở boundary

`QuestionDraft.timeLimit` là `String` (mặc định `"30"`), không phải Int — để người dùng xóa trắng ô thoải mái (Int không biểu diễn được trạng thái rỗng, bản đầu dùng Int khiến ô không xóa được). Rỗng lúc submit = mặc định 30s (`trim().toIntOrNull() ?: 30`); nhập thì validate 5–600s khớp `TIME_LIMIT_MIN/MAX` (quiz.schema.ts). Quy tắc chung rút ra: field số trong form nên giữ String ở tầng UI state, parse khi submit.
