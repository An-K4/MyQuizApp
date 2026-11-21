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
   - Domain interface đặt tại `core:common`. Implementation đặt tại `core:network` (API/Socket) hoặc `core:database` (Room Cache). Tầng trên không bao giờ import ngược dependency của tầng dưới.

---

## 2. BẢN ĐỒ TRA CỨU TÀI LIỆU (DOCS SITEMAP)

Toàn bộ tài liệu kỹ thuật đã được quy hoạch gọn gàng trong thư mục `docs/`. Khi cần làm việc ở mảng nào, hãy mở trực tiếp tài liệu chuyên môn tương ứng:

| Nhóm tài liệu | Đường dẫn | Mục đích & Nội dung chính |
| :--- | :--- | :--- |
| **Quy chuẩn ghi chép** | [`docs/DOCS_GUIDELINES.md`](DOCS_GUIDELINES.md) | **Bắt buộc đọc:** Bộ luật cập nhật tài liệu, quy ước đặt tên, phân loại Living Truth vs Archive. |
| **Kiến trúc & Cấu trúc** | [`docs/architecture/01_design_document_v2.md`](architecture/01_design_document_v2.md)<br>[`docs/architecture/02_project_structure.md`](architecture/02_project_structure.md) | Thiết kế hệ thống, phân chia trách nhiệm 14 module Android, DTO contracts, CookieJar, Authenticator và Socket flow. |
| **Lộ trình & Tiến độ** | [`docs/roadmap/01_50_day_roadmap.md`](roadmap/01_50_day_roadmap.md)<br>[`docs/roadmap/02_n46_ui_polish_progress.md`](roadmap/02_n46_ui_polish_progress.md) | Lộ trình tổng thể 50 ngày (N1–N50), tiến độ chi tiết mốc N46 UI Polish và các tiêu chí phát hành (Release Gates N47–N50). |
| **Audit Backend & Bugs** | [`docs/backend-audit/01_backend_17_bugs_status.md`](backend-audit/01_backend_17_bugs_status.md) | Báo cáo chi tiết 17 bug backend: đối chiếu 7 bug đã fix (BUG-04, 05, 06, 08, 09, 13, 15, 16) và 9 bug còn mở. |
| **Onboarding & Code Walk** | [`docs/walkthrough/code_walkthrough_12_entrypoints.md`](walkthrough/code_walkthrough_12_entrypoints.md) | Hướng dẫn chi tiết 12 điểm vào (Entry Points) quan trọng nhất của ứng dụng Android. |
| **Kho tri thức & Bài học** | [`docs/knowledge/INDEX.md`](knowledge/INDEX.md)<br>[`docs/knowledge/`](knowledge/) | Thư mục 24 bài học xương máu (từ Tuần 1 đến Tuần 6, N1–N25, quy tắc stateless Compose, hướng dẫn cài đặt). |
| **Hồ sơ kiểm thử lịch sử** | [`docs/archive/`](archive/) | Lưu trữ bất biến các checklist E2E lịch sử (N24.5, N25, N43, N44, Refactor). |

---

## 3. CHECKPOINT DỰ ÁN & HƯỚNG DẪN BẮT ĐẦU NHANH

### 3.1. Trạng thái hiện tại của Codebase
- **Android `main`:** Đã hoàn thành mốc **N46 (UI Polish)** tại commit `35d6bfd` (checkpoint `346230b`). Toàn bộ 4 chế độ chơi (Classic, Solo, Survival, Marathon, Practice) đã được tinh chỉnh giao diện, hỗ trợ single-choice chạm-là-gửi, fallback logo thương hiệu và tối ưu thẻ bài review.
- **Android `docs`:** Đã hoàn thành đợt tái cấu trúc cực lớn, toàn bộ tài liệu được gom vào thư mục con `docs/`, chuẩn bị merge PR vào `main`.
- **Backend `server/myquizz`:** Đã cập nhật commit mới nhất `37038f6`, hoàn tất 5 đợt audit bảo mật và hiệu năng.

### 3.2. Công việc tiếp theo
1. **Hoàn tất merge PR nhánh `docs` vào `main`:** Giữ sạch thư mục gốc của repository Android.
2. **Kích hoạt GitHub Ruleset:** Sau khi merge PR, bật `Require status checks to pass` trong Ruleset `Protect Main Branch` trên repo GitHub.
3. **Triển khai mốc N47 (Regression & Tests) hoặc Kế hoạch Tối ưu môn học:** Thực thi 4 Sprint tối ưu hiệu năng (Baseline Profiles, Coil Custom ImageLoader, LeakCanary, Mã hóa Cookie, Network Security Config) hoàn toàn trên Android Client.
