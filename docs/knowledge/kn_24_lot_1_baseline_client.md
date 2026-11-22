# Lô 1 — Baseline, log, dữ liệu đầu vào và offline

> Tri thức chuẩn bị cho T01–T04 trong [roadmap hai môn](../roadmap/04_security_performance_plan.md); đây là giả thuyết và hướng kiểm thử, không phải kết quả đã đạt. Tiến độ chỉ cập nhật ở roadmap.

## Bối cảnh và bẫy

- Đo trước khi sửa trên cùng thiết bị, APK và Backend được ghi phiên bản; phân biệt startup lạnh/ấm và debug/release. Không đoán phần trăm tiết kiệm.
- `SafeHttpLogger` tránh query/header/body nhưng các log email, exception và ACK vẫn cần rà; không ghi token, cookie, email thật hoặc ảnh chứa PII làm evidence.
- Chuẩn hóa theo từng trường; không tự ý trim mật khẩu. Với session `Unknown`, mất mạng không đồng nghĩa đã logout; tái hiện offline boot trước khi thiết kế.

## Khi thực hiện

Ghi tại đây kiến thức, nguyên nhân và giới hạn mới sau khi đã kiểm chứng, kèm task/commit và tham chiếu test đã che dữ liệu. Đặt trạng thái task và kết quả nghiệm thu trong roadmap, không tạo sổ tiến độ thứ hai.
