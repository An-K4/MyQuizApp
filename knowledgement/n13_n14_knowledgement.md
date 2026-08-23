# N13-14 Knowledge: Quiz Manage (List + Create) & N13.5 Home Auth Header

**Milestone:** M3 — Quiz CRUD (đang chạy) · N13-14 hoàn thành 22/8/2026 · N13.5 (bonus) hoàn thành cùng ngày

---

## 📚 TỔNG QUAN

N13-14 tiếp nối N11 (Home/Search) và N12 (Quiz detail) — hoàn thiện nhánh "đọc + tạo" của quiz trước khi sang N15 (upload ảnh) và N16 (sửa/xóa quiz, chốt M3). Đây cũng là lần đầu dự án dùng **Paging 3** thật (N11 chỉ có tham số `page/limit` chưa dùng vì backend chưa hỗ trợ).

Cùng ngày, phát sinh thêm việc ngoài kế hoạch (**N13.5**): thiết kế lại khu vực cạnh nút tìm kiếm ở Home (TODO để lại từ N11) thành component auth-aware, kéo theo việc tạo màn Profile thật và một quyết định kiến trúc nhỏ về nơi đặt logic hiển thị avatar (Coil).

---

## 1. Vá nợ kỹ thuật N12: wrapper `getMyQuizzes`

N12 đã ghi lại nợ kỹ thuật: `searchQuizzes`/`getMyQuizzes` không unwrap đúng envelope lồng `data: { quizzes: [...] }` (giống pattern `{ quiz: ... }` đã gặp ở quiz detail). N13-14 tiện tay vá luôn phần `getMyQuizzes` bằng DTO wrapper riêng ở `QuizManageDtos.kt`, cùng cách đã làm với `QuizDetailDto` ở N12.

📌 **Bài học lặp lại**: backend cứ bọc response theo tên resource dù đã có envelope `{success, data, error}` chung — luôn kiểm tra `*.controller.ts` thật, đừng suy đoán theo REST convention thông thường.

---

## 2. Paging 3 lần đầu dùng thật

- `MyQuizzesPagingSource` + `GetMyQuizzesUseCase` trả `Flow<PagingData<QuizCard>>`.
- Page size chọn **3** theo quyết định người dùng (khảo sát trước khi code), có phương án dự phòng: nếu Paging 3 phát sinh lỗi khó debug, sẵn sàng fallback sang tải danh sách thủ công (load more bằng tay). Thực tế bản đầu chạy ổn, chưa cần dùng fallback.

---

## 3. Quyết định phạm vi: bỏ upload ảnh khỏi v1 của CreateQuiz

Theo khảo sát người dùng trước khi code: **không** làm upload ảnh (S3 presign) ở bản CreateQuiz đầu tiên — để dành hẳn cho N15 (`UploadImageUseCase`, S3 presign 2 bước). CreateQuiz v1 chỉ có editor đủ 4 loại câu hỏi (`multiple_choice`, `multiple_select`, `short_answer`, `long_answer`), không có trường ảnh.

---

## 4. N13.5 (bonus) — Home auth header & Profile

### 4.1. Quyết định UX: "Của tôi" rời khỏi tab Home, vào Profile

N11 làm Home với `TabRow` 2 tab ("Khám phá"/"Của tôi"). Sau khi có auth check use case thật, nhận ra luồng tab không còn hợp lý cho nội dung cần đăng nhập — quyết định:

- Bỏ hẳn `TabRow` khỏi `HomeScreen`, chỉ còn nội dung khám phá cuộn dọc.
- Thêm component auth-aware cạnh nút tìm kiếm ở `TopAppBar`: chưa đăng nhập → nút "Đăng ký/Đăng nhập"; đã đăng nhập → avatar (bấm vào → Profile).
- Tạo màn `ProfileScreen` thật (module `app`, gắn `Route.Profile` cấp app — không phải `feature:*`) chứa item "Quiz của tôi" (nav vào đúng màn `quizmanagelist` của N13-14) và "Đăng xuất".

📌 **Bài học**: 1 UI TODO nhỏ để lại từ feature trước (N11) có thể kéo theo quyết định UX lớn hơn (bỏ tab, thêm màn mới) khi feature phụ thuộc (auth) đã sẵn sàng — nên định kỳ quay lại các TODO cũ khi có đủ điều kiện, đừng để tồn đọng vô thời hạn.

### 4.2. Quyết định kiến trúc: Avatar dùng chung ở `core:ui`, không tự thêm Coil mỗi module

`ProfileScreen` (module `app`) và `HomeScreen` (module `feature:home`) đều cần `AsyncImage` cho avatar, nhưng module `app` chưa có dependency `coil.compose` — phát sinh câu hỏi: thêm Coil trực tiếp vào `app`, hay tạo component chung?

**2 hướng cân nhắc:**

| | Hướng A: Coil trực tiếp mỗi module | Hướng B: `Avatar` chung ở `core:ui` (đã chọn) |
|---|---|---|
| Ưu điểm | Đơn giản, khớp precedent `feature:quiz-manage` đã làm cho ảnh cover quiz | Đúng DIP, tránh lặp code fallback avatar, tái dùng cho nhiều màn sau (lobby, leaderboard) |
| Nhược điểm | Lặp dependency + code fallback ở nhiều module | Cần sửa thêm `HomeScreen`/`ProfileScreen` để gọi component chung |

**Quyết định**: Hướng B — vì avatar (khác với ảnh cover quiz, mỗi nơi hiển thị khác nhau) là UI element sẽ tái dùng ở nhiều màn. Tạo `Avatar(avatarUrl, contentDescription, size, modifier)` ở `core:ui/components/Avatar.kt`, Coil vẫn ở scope `implementation` trong `core:ui/build.gradle.kts` (không cần nâng lên `api`) — các module gọi (`app`, `feature:home`) chỉ cần dependency `core:ui` đã có sẵn, không tự khai `coil.compose` riêng.

📌 **Quy ước mới cho các feature sau**: mọi nơi cần hiển thị avatar user (không phải ảnh quiz/câu hỏi) dùng `Avatar` của `core:ui`, không tự thêm Coil.

### 4.3. Chưa có Bottom Navigation

Design doc v2 mục 11.4 mô tả sẵn `BottomNavigationBar` 5 tab (Home/Discover/Join/Library/Profile) nhưng **thực tế chưa triển khai** — điều hướng Profile/MyQuizzes hiện đi qua `NavController` thông thường từ Home, không qua bottom nav. `Route.Library` vẫn tồn tại trong sealed Route nhưng chưa có điểm truy cập; "Quiz của tôi" hiện đi qua `Route.Profile` → `Route.MyQuizzes`. Cần quyết định lại ở N16+: có cần bottom nav thật không, và `Route.Library` có hợp nhất với `Route.MyQuizzes` hay giữ vai trò khác.

---

## ✅ CHECKLIST HOÀN THÀNH

**N13-14:**
- [x] Domain/Data: `getMyQuizzes` (Paging 3) + `createQuiz`, vá nợ kỹ thuật wrapper N12
- [x] UseCase: `GetMyQuizzesUseCase`, `CreateQuizUseCase`
- [x] Presentation: `quizmanagelist/` + `createquiz/` (editor 4 loại câu hỏi)
- [x] Navigation: routes + `AppNavGraph.kt`
- [ ] Upload ảnh (để N15)

**N13.5 (bonus):**
- [x] Bỏ `TabRow` khỏi Home
- [x] Component auth-aware cạnh nút tìm kiếm
- [x] `ProfileScreen` thật (module `app`) + wiring `Route.Profile`
- [x] Component `Avatar` chung ở `core:ui`
- [ ] Bottom Navigation thật (chưa làm, note lại cho N16+)

---

**Ngày hoàn thành:** 22/8/2026 · **Trạng thái:** N13-14 + N13.5 Complete, sẵn sàng N15 (upload ảnh S3 presign)
