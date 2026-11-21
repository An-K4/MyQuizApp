# AGENTS.md — Trung tâm Điều hướng & Quy ước cho Agent tiếp nhận

> **Dành cho Agent tiếp nhận:** Đây là điểm vào (Single Entry Point) duy nhất bạn cần đọc đầu tiên khi bắt đầu phiên làm việc trên dự án **MyQuizApp**. 
> Tài liệu này cung cấp các nguyên tắc hoạt động bất di bất dịch, bản đồ tra cứu toàn bộ kho tài liệu chuyên sâu trong thư mục `docs/`, và trạng thái checkpoint hiện tại của dự án.

---

## 1. BA NGUYÊN TẮC CỐT LÕI (GOLDEN RULES)

1. **Code thật là sự thật duy nhất (Reality Over Docs):**
   - Tuyệt đối không suy diễn từ các tài liệu thiết kế lý thuyết cũ (như `01_design_document_v2.md`). Luôn đối chiếu trực tiếp với mã nguồn thật:
     * Android Client: Nhánh `main` (HEAD: `35d6bfd`, checkpoint tính năng: `346230b`).
     * Backend Server: Thư mục `server/myquizz` (HEAD: `37038f6`).
2. **100% KHÔNG sửa đổi Backend:**
   - User không can thiệp trực tiếp vào mã nguồn backend. Mọi tính năng, tối ưu hóa và sửa lỗi đều thực hiện **thuần túy trên Android Client**.
   - Kế thừa và tận dụng trực tiếp các thành quả từ 5 đợt audit của backend (như kiểm tra Magic Bytes S3, Redis Lua script, Fail-Closed, Invalidation trigger, Realtime recovery checkpoint) để làm minh chứng kỹ thuật.
   - Các bug backend chưa fix (như BUG-01 trên Practice mode) được ghi nhận vào mục *"Hạn chế của hệ thống / Hướng phát triển"*, không cố gắng sửa code backend.
3. **Tuân thủ Clean Architecture & Quy ước Phân tầng:**
   - Luồng bắt buộc: `UI / Screen` ➔ `ViewModel` ➔ `UseCase` ➔ `Repository`.
   - Ưu tiên ranh giới domain/data và hạn chế phụ thuộc chéo. Thực tế `35d6bfd`: `feature:home` khai báo `feature:auth`, và Splash trong `app` dùng `RefreshSessionUseCase` từ `feature:auth`; phân biệt kiến trúc mục tiêu với code hiện hữu.

---

## 2. BẢN ĐỒ TRA CỨU TÀI LIỆU (DOCS SITEMAP)

Toàn bộ tài liệu kỹ thuật đã được quy hoạch gọn gàng trong thư mục `docs/`. Khi cần làm việc ở mảng nào, hãy mở trực tiếp tài liệu chuyên môn tương ứng:

| Nhóm tài liệu | Đường dẫn | Mục đích & Nội dung chính |
| :--- | :--- | :--- |
| **Quy chuẩn ghi chép** | [`docs/DOCS_GUIDELINES.md`](DOCS_GUIDELINES.md) | **Bắt buộc đọc:** Bộ luật cập nhật tài liệu, quy ước đặt tên, phân loại Living Truth vs Archive. |
| **Kiến trúc & Cấu trúc** | [`docs/architecture/01_design_document_v2.md`](architecture/01_design_document_v2.md)<br>[`docs/architecture/02_project_structure.md`](architecture/02_project_structure.md) | Thiết kế hệ thống, phân chia trách nhiệm 14 module Android, DTO contracts, CookieJar, Authenticator và Socket flow. |
| **Lộ trình & Tiến độ** | [`roadmap/01_50_day_roadmap.md`](roadmap/01_50_day_roadmap.md)<br>[`roadmap/02_n46_ui_polish_progress.md`](roadmap/02_n46_ui_polish_progress.md)<br>[`roadmap/03_n47_n50_release_gates.md`](roadmap/03_n47_n50_release_gates.md) | Lộ trình, evidence N46 và release gates chưa đóng. |
| **Audit Backend & Bugs** | [`docs/backend-audit/01_backend_17_bugs_status.md`](backend-audit/01_backend_17_bugs_status.md) | Báo cáo chi tiết 17 bug backend; trạng thái cần đối chiếu mã nguồn và bằng chứng nghiệm thu. BUG-13 vẫn chờ E2E. |
| **Bài tập tối ưu** | [`optimization-report/01_optimization_plan_4_sprints.md`](optimization-report/01_optimization_plan_4_sprints.md)<br>[Trade-off](optimization-report/02_performance_security_tradeoffs.md)<br>[Kịch bản đo](optimization-report/03_benchmark_metrics_scenarios.md) | Kế hoạch Android-only, cân bằng hiệu năng/bảo mật và mẫu evidence. |
| **Onboarding & Code Walk** | [`docs/walkthrough/code_walkthrough_12_entrypoints.md`](walkthrough/code_walkthrough_12_entrypoints.md) | Hướng dẫn chi tiết 12 điểm vào (Entry Points) quan trọng nhất của ứng dụng Android. |
| **Kho tri thức & Bài học** | [`docs/knowledge/INDEX.md`](knowledge/INDEX.md)<br>[`docs/knowledge/`](knowledge/) | 23 bài tri thức và một hướng dẫn cài đặt; các bài ghi nhận bối cảnh lịch sử, cần đối chiếu source hiện tại. |
| **Hồ sơ kiểm thử lịch sử** | [`docs/archive/`](archive/) | Lưu trữ bất biến các checklist E2E lịch sử (N24.5, N25, N43, N44, Refactor). |

---

## 3. CHECKPOINT DỰ ÁN & HƯỚNG DẪN BẮT ĐẦU NHANH

### 3.1. Trạng thái hiện tại của Codebase
- **Android `main`:** Tại `35d6bfd` (checkpoint gameplay `346230b`), N46 đã có các thay đổi UI được commit; tiến độ và bằng chứng nghiệm thu được theo dõi tại [N46 UI polish](roadmap/02_n46_ui_polish_progress.md). Không suy ra N46, CI hay E2E đã hoàn tất chỉ từ commit.
- **Android `docs`:** Đang tái cấu trúc tài liệu vào `docs/`; chưa coi việc đối chiếu Living Truth và chuẩn bị merge là hoàn tất.
- **Backend `server/myquizz`:** Đã cập nhật commit mới nhất `37038f6`, hoàn tất 5 đợt audit bảo mật và hiệu năng.

### 3.2. Công việc tiếp theo
1. **Hoàn tất merge PR nhánh `docs` vào `main`:** Giữ sạch thư mục gốc của repository Android.
2. **Kích hoạt GitHub Ruleset:** Sau khi merge PR, bật `Require status checks to pass` trong Ruleset `Protect Main Branch` trên repo GitHub.
3. **Triển khai mốc N47 (Regression & Tests) hoặc Kế hoạch Tối ưu môn học:** Thực thi 4 Sprint tối ưu hiệu năng (Baseline Profiles, Coil Custom ImageLoader, LeakCanary, Mã hóa Cookie, Network Security Config) hoàn toàn trên Android Client.
