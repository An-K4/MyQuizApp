# Quy ước lập trình và bàn giao MyQuizApp

> Quy ước làm việc rút từ AGENTS trước refactor và các tài liệu tri thức. Đây là hướng dẫn thiết kế, không phải khẳng định mọi file ở Android `35d6bfd` đều đã tuân thủ. Đối chiếu [cấu trúc thực tế](02_project_structure.md), [thiết kế](01_design_document_v2.md) và source trước khi thay đổi.

## Ranh giới kiến trúc

- Ưu tiên luồng Screen → ViewModel → use case → repository; tách chính sách nghiệp vụ khỏi Compose và DTO khỏi domain model. Đưa abstraction dùng chung vào module phù hợp, không tự tạo phụ thuộc vòng.
- Phân biệt **mục tiêu** với đồ thị hiện hữu: tại `35d6bfd`, `feature:home` khai báo `feature:auth`; Splash trong `app` dùng `RefreshSessionUseCase` từ `feature:auth`. Không mô tả source như thể đã loại bỏ các phụ thuộc này.
- Trước khi thêm use case hoặc module, xác định trách nhiệm, số consumer thực sự và ranh giới dependency; không chuyển logic chỉ vì một ví dụ cũ.

## Compose và trạng thái

- `XxxScreen` là ranh giới tích hợp ViewModel, navigation và effects; ưu tiên `XxxScreenContent` thuần state/callback để preview và test. Component tái sử dụng mặc định stateless, state được hoist đến nơi sở hữu.
- Mọi action phải có đường kích hoạt thật từ UI; kiểm tra loading, empty, error và retry ở đúng vùng nội dung. Tránh tạo nhiều vùng scroll lồng nhau không cần thiết.
- Không giả định `remember` sống qua navigation/dispose; kiểm tra lifecycle, back stack và state restoration cho từng flow. Chi tiết: [Stateful/Stateless](../knowledge/kn_23_ui_stateful_stateless_refactor.md), [N16](../knowledge/kn_10_n16.md) và [N19.6](../knowledge/kn_16_n19_6.md).

## API, session và lỗi

- Đọc route, controller **và service backend thật** tại commit đang dùng trước khi thiết kế DTO; không suy payload từ tên controller hay tài liệu cũ. Kiểm tra envelope, lớp lồng, casing và pagination bằng fixture/test.
- Ánh xạ lỗi có cấu trúc vào kết quả/domain error phù hợp; UI có retry và thông báo theo ngữ cảnh. Không biến timeout hoặc lỗi mạng mơ hồ thành kết luận đăng xuất thành công.
- Cookie, token và dữ liệu cá nhân không xuất hiện trong log, báo cáo, screenshot hoặc fixture thật. Khi audit C04, kiểm tra contract `x-guest-token` cho cả history, summary và my-answers; không nhầm `x-guest-id` với token đã ký.
- Client upload presigned URL cần tuân thủ contract storage và không gửi cookie/auth không liên quan. Xem [N15](../knowledge/kn_09_n15.md) và [báo cáo backend](../backend-audit/01_backend_17_bugs_status.md).

## Socket và phục hồi

- Xác định từng event có ACK hay không; timeout ACK không chứng minh server chưa xử lý và không tự replay mutation không an toàn. Parse error theo contract thật.
- Kết nối lại transport không tự chứng minh đã join room/sync state. Kiểm thử chuỗi reconnect, retry, join và sync; phân biệt snapshot room với trạng thái self-paced từng player. Xem [N18](../knowledge/kn_13_n18.md), [N20](../knowledge/kn_17_n20.md) và [walkthrough](../walkthrough/code_walkthrough_12_entrypoints.md).
- UI ẩn leaderboard không thay thế kiểm tra quyền phía server; kiểm tra payload `/results` và `/summary`, ghi hạn chế backend thay vì khẳng định đã bảo mật.

## Bằng chứng và bàn giao

- Audit source/contract trước khi chốt phương án; với failure mơ hồ, thu thập timeline và log đã xóa bí mật trước khi quyết định sửa.
- Unit/integration test cho nhánh lỗi, bất biến và regression; checklist thủ công nêu fixture, thao tác, kết quả mong đợi và trạng thái PASS/FAIL/NOT RUN. Build sạch không thay thế E2E, thiết bị thật hoặc privacy gate.
- Khi sửa code, cập nhật tài liệu Living Truth tương ứng và giữ nguyên kết quả lịch sử. Thông lệ chi tiết, bài học và các quyết định theo thời điểm được lưu nguyên văn trong [AGENTS lịch sử](../archive/agents_pre_restructure.md); không áp dụng checkpoint hay hướng dẫn cũ mà chưa đối chiếu.
