# DOCS_GUIDELINES.md — Bộ Luật Quy Chuẩn và Quản Trị Hệ Thống Tài Liệu

> **Mục đích:** Thiết lập các quy định bắt buộc, quy ước đặt tên và quy trình cập nhật tài liệu cho toàn bộ các Agent và Developer tham gia phát triển dự án **MyQuizApp**.
> Tuyệt đối không tự ý tạo file tài liệu bừa bãi ở thư mục gốc hoặc ghi chép thiếu kiểm soát.

---

## 1. QUY ƯỚC ĐẶT TÊN & BỐ TRÍ THƯ MỤC (NAMING & DIRECTORY CONVENTIONS)

### 1.1. Vị trí lưu trữ duy nhất
* **Toàn bộ tài liệu dự án BẮT BUỘC phải nằm trong thư mục `docs/`.**
* Tuyệt đối **KHÔNG** tạo thêm bất kỳ file Markdown (`.md`) hay text tài liệu nào ở thư mục gốc của repository (trừ file `readme.md` công khai của dự án).

### 1.2. Quy ước đặt tên thư mục (Directory Naming)
* Tên thư mục con trong `docs/` sử dụng chữ thường, phân cách bằng dấu gạch ngang (`kebab-case`).
* Danh mục 7 thư mục chức năng chuẩn hóa:
  1. `architecture/`: Tài liệu kiến trúc, thiết kế kỹ thuật, hợp đồng dữ liệu.
  2. `roadmap/`: Kế hoạch tổng thể, tiến độ các mốc phát triển, tiêu chí nghiệm thu.
  3. `backend-audit/`: Báo cáo audit backend, hiện trạng các bug và baseline hiệu năng.
  4. `walkthrough/`: Tài liệu hướng dẫn đọc hiểu mã nguồn và onboarding.
  5. `knowledge/`: Kho lưu trữ bài học kỹ thuật và tri thức tích lũy qua từng sprint.
  6. `archive/`: Lưu trữ hồ sơ, checklist kiểm thử lịch sử (bất biến).
  7. `optimization-report/`: Kế hoạch tối ưu Android, phương pháp đo và phân tích trade-off.

### 1.3. Quy ước đặt tên file (File Naming)
* **File tài liệu đọc theo trình tự logic:** Sử dụng tiền tố số thứ tự 2 chữ số: `01_...`, `02_...`.
  * *Ví dụ:* `01_system_overview.md`, `02_project_structure.md`.
* **File tri thức kỹ thuật (`knowledge/`):** Sử dụng tiền tố `kn_` kèm số thứ tự: `kn_01_...`, `kn_02_...`.
  * *Ví dụ:* `kn_01_n1_n5.md`, `kn_16_n19_6.md`.
* **File lưu trữ kiểm thử / checklist lịch sử (`archive/`):** Sử dụng tiền tố `chk_`.
  * *Ví dụ:* `chk_n25_e2e_classic_checklist.md`, `chk_n44_e2e_report.md`.
* **File quy chuẩn cấp cao tại root `docs/`:** Sử dụng chữ in hoa: `AGENTS.md`, `DOCS_GUIDELINES.md`, `README.md`.

---

## 2. NGUYÊN TẮC QUẢN LÝ TÀI LIỆU (GOVERNANCE PRINCIPLES)

### 2.1. Phân định rõ ràng: "Living Truth" vs "Archive"
1. **Tài liệu trạng thái sống (Living Truth Docs):**
   * Bao gồm các file trong `architecture/`, `roadmap/`, `backend-audit/`.
   * **Quy tắc:** Các khẳng định hiện trạng phải khớp mã nguồn đã đối chiếu, ghi rõ commit và phạm vi kiểm tra; đánh dấu nội dung chưa xác minh, thiết kế mục tiêu và ghi chép lịch sử. Không suy diễn kiểm thử đã đạt từ việc có mã. Khi code thay đổi, cập nhật tài liệu liên quan trong cùng PR.
2. **Tài liệu lưu trữ lịch sử (Archive Docs):**
   * Bao gồm toàn bộ file trong `archive/`.
   * **Quy tắc:** Đây là **bằng chứng lịch sử bất biến (Immutable Evidence)**. Tuyệt đối KHÔNG sửa đổi nội dung, kết quả test hay số liệu ghi nhận trong quá khứ của các file này, ngay cả khi sau này tính năng đó đã được sửa hoặc thay đổi.

### 2.2. Nguyên tắc "Code thật là sự thật duy nhất" (Reality First)
* Mọi mô tả trong tài liệu phải được kiểm chứng bằng việc đọc trực tiếp mã nguồn hoặc chạy lệnh thực tế.
* Nghiêm cấm việc suy diễn từ các file thiết kế cũ mà không đối chiếu với code thật.
* Khi phát hiện tài liệu lệch so với code thật: **Code thật luôn luôn đúng**, phải sửa lại tài liệu theo code thật (hoặc báo cáo bug nếu code chạy sai nghiệp vụ).

### 2.3. Quy định về Tri thức trong `knowledge/`
* Toàn bộ bài học xương máu, bẫy kỹ thuật (gotchas), phân tích nguyên nhân gốc rễ (root cause) trong thư mục `knowledge/` **phải được bảo tồn nguyên vẹn 100% nội dung**.
* Được phép cải thiện cách diễn đạt, chuẩn hóa markdown, bổ sung ví dụ minh họa, nhưng **tuyệt đối không được xóa bỏ hay tóm tắt làm mất chi tiết kỹ thuật**.

---

## 3. QUY CHUẨN ĐỊNH DẠNG MARKDOWN (MARKDOWN STANDARDS)

1. **Tiêu đề & Mục lục:**
   * Mỗi file phải có đúng một tiêu đề cấp 1 (`#`), tiếp theo là blockquote (`>`) mô tả ngắn gọn mục đích và phạm vi của tài liệu.
   * Sử dụng tiêu đề phân cấp hợp lý (`##`, `###`, `####`).
2. **Đường dẫn liên kết (Relative Links):**
   * Liên kết tới tài liệu và mã nguồn trong repository này dùng đường dẫn tương đối. Với repository backend riêng, dùng permalink gắn commit đã đối chiếu; không giả định `server/myquizz` tồn tại trong repository Android.
   * *Đúng từ file này:* `[Cấu trúc module](architecture/02_project_structure.md)`
   * *Sai:* Không dùng đường dẫn tuyệt đối kiểu `E:/Adr_Projects/...` hay `C:\Users\...`.
3. **Minh họa bằng Diagram & Bảng biểu:**
   * Khuyến khích sử dụng cú pháp Mermaid (`flowchart`, `sequenceDiagram`) để mô tả luồng logic hoặc kiến trúc.
   * Sử dụng bảng Markdown cho các danh sách so sánh, đối chiếu bug, ma trận dữ liệu.

---

## 4. QUY TRÌNH CẬP NHẬT TÀI LIỆU KHI LÀM VIỆC (WORKFLOW)

Mỗi khi một Agent hoặc Developer hoàn thành một tính năng hoặc sửa lỗi:
1. **Kiểm tra tác động:** Xem thay đổi của mình ảnh hưởng đến file Living Truth nào (Kiến trúc, DTO, Lộ trình hay Danh sách bug).
2. **Cập nhật tài liệu Living Truth:** Chỉnh sửa file tương ứng trong `docs/architecture/` hoặc `docs/roadmap/`.
3. **Ghi nhận bài học (nếu có):** Nếu gặp bẫy kỹ thuật mới hoặc kinh nghiệm quý, ghi chép vào `docs/knowledge/` theo đúng quy ước đặt tên.
4. **Không commit file nháp hoặc dữ liệu riêng:** Kiểm tra danh sách stage theo từng đường dẫn; không xóa, sửa hay commit các tệp cá nhân/untracked của người khác nếu chưa được yêu cầu. Đặc biệt không commit hoặc push `my_notes.md`.
5. **Kiểm thử trung thực:** Checklist thủ công nêu fixture, thao tác, dấu hiệu đạt; ghi NOT RUN nếu chưa chạy. Compile hoặc UI PASS không thay thế E2E, backend hay privacy gate. Giữ kết quả cũ bất biến và không ghi log token, cookie hoặc Authorization.
6. **Giữ ngữ cảnh lịch sử:** Bản [AGENTS trước khi rút gọn](archive/agents_pre_restructure.md) được bảo tồn nguyên văn; quy tắc và checkpoint trong đó là lịch sử, cần đối chiếu tài liệu hiện hành và source. Bài học theo mốc nằm trong [mục lục tri thức](knowledge/INDEX.md). Không ghi đè tài liệu canonical bằng snapshot cũ.
