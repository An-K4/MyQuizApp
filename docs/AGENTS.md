# AGENTS.md — Trung tâm Điều hướng & Quy ước cho Agent tiếp nhận

> **Dành cho Agent tiếp nhận:** Đây là điểm vào (Single Entry Point) duy nhất bạn cần đọc đầu tiên khi bắt đầu phiên làm việc trên dự án **MyQuizApp**.
> Tài liệu này cung cấp các nguyên tắc hoạt động bất di bất dịch, bản đồ tra cứu toàn bộ kho tài liệu chuyên sâu trong thư mục `docs/`, và trạng thái checkpoint hiện tại của dự án.

---

## 1. BA NGUYÊN TẮC CỐT LÕI (GOLDEN RULES)

1. **Code thật là sự thật duy nhất (Reality Over Docs):**
   - Tuyệt đối không suy diễn từ các tài liệu thiết kế lý thuyết cũ (như `01_design_document_v2.md`). Luôn đối chiếu trực tiếp với mã nguồn thật:
     * Android Client: tại thời điểm lập roadmap, `main` `31f8fbf` (checkpoint tính năng cũ `346230b`); kiểm tra lại HEAD trước khi làm việc.
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
| **Kiến trúc & Cấu trúc** | [`docs/architecture/01_design_document_v2.md`](architecture/01_design_document_v2.md)<br>[`docs/architecture/02_project_structure.md`](architecture/02_project_structure.md)<br>[Quy ước lập trình](architecture/03_coding_conventions.md) | Thiết kế hệ thống, 14 module Android, DTO contracts, CookieJar, Authenticator, Socket flow và quy ước code. |
| **Lộ trình & Tiến độ** | [Roadmap bảo mật và tối ưu hai môn](roadmap/04_security_performance_plan.md)<br>[Roadmap 50 ngày lịch sử](roadmap/01_50_day_roadmap.md)<br>[N46 lịch sử](roadmap/02_n46_ui_polish_progress.md)<br>[N47–N50 lịch sử](roadmap/03_n47_n50_release_gates.md) | Chỉ theo dõi tiến độ mới ở roadmap hai môn; ba file cũ là lịch sử môn Lập trình Android nâng cao, không suy task cũ đã PASS. |
| **Audit Backend & Bugs** | [`docs/backend-audit/01_backend_17_bugs_status.md`](backend-audit/01_backend_17_bugs_status.md) | Báo cáo chi tiết 17 bug backend; trạng thái cần đối chiếu mã nguồn và bằng chứng nghiệm thu. BUG-13 vẫn chờ E2E. |
| **Bài tập tối ưu** | [`optimization-report/01_optimization_plan_4_sprints.md`](optimization-report/01_optimization_plan_4_sprints.md)<br>[Trade-off](optimization-report/02_performance_security_tradeoffs.md)<br>[Kịch bản đo](optimization-report/03_benchmark_metrics_scenarios.md) | Kế hoạch Android-only, cân bằng hiệu năng/bảo mật và mẫu evidence. |
| **Onboarding & Code Walk** | [`docs/walkthrough/code_walkthrough_12_entrypoints.md`](walkthrough/code_walkthrough_12_entrypoints.md) | Hướng dẫn chi tiết 12 điểm vào (Entry Points) quan trọng nhất của ứng dụng Android. |
| **Kho tri thức & Bài học** | [`docs/knowledge/INDEX.md`](knowledge/INDEX.md)<br>[`docs/knowledge/`](knowledge/) | 23 bài cũ, hướng dẫn cài đặt và 5 file tri thức theo lô hai môn; đối chiếu source hiện tại. |
| **Hồ sơ lịch sử** | [`docs/archive/`](archive/)<br>[AGENTS trước refactor](archive/agents_pre_restructure.md) | Checklist lịch sử và bản gốc AGENTS; không coi các checkpoint, quy tắc cũ là trạng thái hiện hành. |

---

## 3. CHECKPOINT DỰ ÁN & HƯỚNG DẪN BẮT ĐẦU NHANH

### 3.1. Trạng thái tại thời điểm lập kế hoạch
- **Android `main`:** `31f8fbf` sau merge PR tài liệu #5; kiểm tra HEAD khi bắt đầu. Các kết quả test lịch sử không tự chứng minh test mới PASS.
- **Backend local `main`:** `37038f6` khi audit; deployment và các kết quả runtime phải xác minh riêng. Không tự sửa Backend; thông báo người dùng để phối hợp nhóm Backend khi cần.
- **Roadmap mới:** [Kế hoạch chung bảo mật và tối ưu](roadmap/04_security_performance_plan.md) đã được người dùng duyệt. T01–T04 đã merge vào `main` qua PR #7–#10; các kết quả còn thiếu và nghiệm thu xem roadmap. Không track hoặc push `my_notes.md`.

### 3.2. Công việc tiếp theo
1. Đối chiếu trạng thái roadmap và các điểm phụ thuộc Backend trước khi triển khai lô tiếp theo; không suy merge là đã vượt mọi gate.
2. Kiểm tra ruleset và `build` CI thực tế trước PR; không suy cấu hình đã lưu chỉ từ ảnh giao diện.
3. Triển khai theo task/nhánh và nghiệm thu như roadmap; cập nhật tiến độ chỉ ở roadmap mới, ghi bài học vào knowledge của từng lô.
