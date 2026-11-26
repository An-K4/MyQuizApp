# Lô 1 — Baseline, log, dữ liệu đầu vào và offline

> Tri thức và hướng kiểm thử cho T01–T04 trong [roadmap hai môn](../roadmap/04_security_performance_plan.md). Nội dung được ghi theo phạm vi đã đối chiếu; không tự suy kiểm thử hay nghiệm thu đã đạt. Tiến độ chỉ cập nhật ở roadmap.

## Bối cảnh và bẫy

- Đo trước khi sửa trên cùng thiết bị, APK và Backend được ghi phiên bản; phân biệt startup lạnh/ấm và debug/release. Không đoán phần trăm tiết kiệm.
- T04: `Unknown` không chứng minh là guest. Khi mạng trở lại, thử làm mới phiên có giới hạn; lỗi mạng/timeout phải có trạng thái và retry, không chủ động xóa cookie vì mất mạng. Phân biệt lỗi phiên kết thúc với lỗi mạng. Kiểm thử race với request offline còn chạy và phiên mới/đăng xuất; test máy thật vẫn cần thiết. Đối chiếu `SessionRecoveryController` và `SessionRepositoryImpl` trên nhánh T04; các unit test controller bao phủ retry, timeout và phiên thay đổi trong lúc refresh, không thay thế kiểm thử tích hợp.
- T02 `8133c7d` đã rà và giảm log nhạy cảm ở auth/session/socket; `SafeHttpLogger` không ghi path/query/header/body. Kiểm tra log thiết bị và build release vẫn cần bằng chứng riêng; không ghi token, cookie, email thật hoặc ảnh chứa PII làm evidence.
- Chuẩn hóa theo từng trường; không tự ý trim mật khẩu. Với session `Unknown`, mất mạng không đồng nghĩa đã logout; tái hiện offline boot trước khi thiết kế.

## T03 — Hợp đồng input đối chiếu mã

Đối chiếu schema backend nguồn `37038f6`: `backend/src/modules/auth/auth.schema.ts`, `user/user.schema.ts`, `quiz/quiz.schema.ts`. Độ dài là số đơn vị UTF-16, không phải số byte hay số ký tự hiển thị. Backend luôn là nơi xác thực cuối; ứng dụng không thay thế kiểm tra sở hữu/quyền ở server.

| Trường / luồng | Schema backend | Xử lý Android / lưu ý |
|---|---|---|
| Email login/register/forgot | `z.email()` | Trim trước validate và gửi. Bộ lọc chung ở client chỉ chặn dạng rõ ràng sai; không đồng nhất hoàn toàn với `z.email()`, server có thể từ chối thêm. |
| Mật khẩu login | Tối thiểu 1 đơn vị UTF-16 | Giữ nguyên khoảng trắng và nội dung; không áp min 8 cho login cũ. |
| Mật khẩu register/reset/đổi/vô hiệu hóa | Tối thiểu 8 đơn vị UTF-16 (kể cả mật khẩu cũ trong đổi mật khẩu) | Giữ nguyên; confirmation so sánh chính xác. Android yêu cầu mật khẩu mới khác cũ khi đổi. |
| Họ tên đăng ký/profile | 2–100 đơn vị UTF-16 | Trim trước kiểm tra và gửi. Profile chỉ kiểm tra trường thực sự thay đổi. |
| Điện thoại | `+` tùy chọn, 7–15 chữ số; register optional; profile cho phép chuỗi rỗng | Trim trước kiểm tra/gửi; register bỏ qua khi trống, profile dùng rỗng để xóa. |
| Giới thiệu profile | Tối đa 200, optional | Trim trước kiểm tra/gửi; rỗng có thể xóa. |
| Tên/mô tả/category quiz | 3–100 / tối đa 500 / tối đa 50 | Kiểm tra sau trim; mô tả/category tùy chọn. |
| Câu hỏi và lựa chọn | Text 1–200; 2–4 lựa chọn, mỗi lựa chọn 1–100; hint/explanation tối đa 255; thời gian 5–600 giây | Editor kiểm tra nội dung sau trim và index đáp án; server kiểm tra lại. |

Không ghi giá trị input thật vào evidence. Việc kiểm tra lỗi hiển thị trên thiết bị và đối chiếu endpoint thật là nghiệm thu riêng, không suy ra từ unit test.

## Khi thực hiện

Ghi tại đây kiến thức, nguyên nhân và giới hạn mới sau khi đã kiểm chứng, kèm task/commit và tham chiếu test đã che dữ liệu. Đặt trạng thái task và kết quả nghiệm thu trong roadmap, không tạo sổ tiến độ thứ hai.
