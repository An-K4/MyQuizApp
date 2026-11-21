# N11 Knowledge: Home & Search (Quiz Discovery)

**Milestone:** M3 — Quiz CRUD (đang chạy) · N11 hoàn thành 17/8/2026

---

## 📚 TỔNG QUAN

N11 mở đầu Tuần 3 — Quiz module. Đây là lần đầu team triển khai 1 feature đầy đủ 4 phase (Domain → Data → Feature/Presentation → UI+Navigation) sau khi các pattern nền tảng đã ổn định từ Tuần 1–2. Cũng là lần đầu phát sinh vài lỗi nhỏ vì lệch convention có sẵn — rất đáng học để không lặp lại ở các feature sau (quiz-manage, lobby...).

---

## 1. `QuizCard` vs `Quiz` — 2 payload khác nhau cho 2 mục đích khác nhau

| | `QuizCard` | `Quiz` |
|---|---|---|
| Dùng cho | Danh sách/search (listing) | Chi tiết/chơi |
| Có mảng `questions`? | ❌ Không | ✅ Có |
| Kích thước | Nhẹ | Nặng |

**Lý do tách:** tránh tải hàng trăm quiz kèm full câu hỏi chỉ để hiển thị 1 danh sách card — lãng phí băng thông và bộ nhớ không cần thiết.

---

## 2. ⚠️ Lỗi thật đã gặp: sai convention `Result<T>`

**Sai lúc đầu** (do quen tay từ codebase khác): dùng `Result<T, AppError>` — generic 2 type param.

**Đúng theo convention project** (đã định nghĩa từ Tuần 1 ở `core:common`): `Result<T>` — `AppError` luôn cố định bên trong case `Error`, KHÔNG phải generic.

Phải sửa lại **6 file**: `QuizRepository`, `QuizRepositoryImpl`, 2 UseCase, `QuizApiService`, import ở `HomeViewModel`.

📌 **Bài học:** trước khi viết Repository/UseCase mới cho 1 feature mới, luôn mở lại `core:common/result/Result.kt` xem đúng chữ ký chuẩn, đừng gõ theo trí nhớ hoặc theo thói quen từ dự án khác.

---

## 3. API Service Pattern — luôn trả `Result<T>`, không trả DTO trần

**Sai lúc đầu:** `QuizApiService` methods trả DTO trực tiếp (kiểu Retrofit thô, không qua adapter).

**Đúng:** mọi method Retrofit phải trả `Result<T>` (nhờ `ResultCallAdapter` tự wrap, giống `AuthApiService` đã làm từ Tuần 1); `QuizRepositoryImpl` dùng `.map { }` để chuyển DTO → domain, KHÔNG dùng try-catch thủ công.

---

## 4. Shared UI Components thuộc `core:ui`, không nằm trong feature

`QuizCardItem.kt` và `HomeSectionRow.kt` lúc đầu đặt trong `feature:home/presentation/components/` — sau move sang `core:ui/components/` vì các feature khác (`quiz-manage`, `leaderboard`) cũng cần dùng lại.

**Nguyên tắc rút ra:** nếu 1 component UI có khả năng dùng lại ở ≥2 feature → đặt ở `core:ui` ngay từ đầu, đừng đợi tới lúc feature thứ 2 cần rồi mới move.

---

## 5. Kiến trúc Home/Search: tách 2 ViewModel riêng

- `HomeViewModel`: chỉ load sections (tab Khám phá/Của tôi).
- `SearchViewModel`: màn **riêng**, có logic pagination + auto-focus khi mở màn.

**Lý do chọn "Search riêng màn" (Option B) thay vì tìm kiếm ngay trên Home (Option A):** tách rõ concerns, UX tốt hơn (auto-focus ô search ngay khi mở, infinite scroll riêng biệt), tránh làm phình logic của `HomeViewModel`.

---

## 6. Nợ kỹ thuật đã biết trước (ghi lại để không quên)

- `SearchQuizzesUseCase` có tham số `page`/`limit` nhưng backend **chưa hỗ trợ pagination thật** (`/v1/quizzes/search` hiện là simple list) → cần refactor khi học Paging 3 ở N13.
- Component cũ trong `feature:home/presentation/components/` (bản trước khi move sang `core:ui`) có thể xoá — dọn dẹp sau.
- Nút auth ở `HomeScreen` TopBar còn TODO — cần thảo luận cách hiển thị sau khi test flow thật.

---

## ✅ CHECKLIST HOÀN THÀNH

- [x] Domain models: `QuizOwner`, `QuizCard`, `Quiz` (cập nhật), `HomeSection`
- [x] Data layer: Repository interface + impl + DTO mapper (snake_case → domain)
- [x] Feature layer: 2 ViewModel, 2 UseCase, Intent/UiState cho cả 2 màn
- [x] UI + Navigation: HomeScreen, SearchScreen, shared components, routes
- [x] Compile: PASS, không lỗi

---

**Ngày hoàn thành:** 17/8/2026 (evening) · **Trạng thái:** N11 Complete, sẵn sàng N12 (Quiz detail + Room cache)
