# Kịch bản đo lường Before / After

> Template thực nghiệm; xem [pilot baseline chưa hoàn chỉnh](04_baseline_before.md). Không chạy thử tấn công, tải hay thu thập dữ liệu thật ngoài môi trường được phép.

## Thiết lập chung

- Ghi commit Android/backend, loại build và cấu hình profile/R8, thiết bị/OS/tần số màn hình, kết nối mạng, nhiệt độ/pin, dữ liệu thử và quy trình reset cache.
- Giữ cùng điều kiện Before/After, chạy pilot, định nghĩa warmup và cỡ mẫu trước khi đo; lưu từng mẫu, ngoại lệ và cách tính p50/p95/p99. Không so cold start với warm start hoặc debug với release như hai mẫu tương đương.
- Dùng tài khoản/quiz thử; xóa hoặc che cookie, token, thông tin cá nhân và định danh nhạy cảm trong log, trace và ảnh chụp.

## Ma trận đo

| Kịch bản | Công cụ gợi ý | Số liệu và cách kiểm soát |
|---|---|---|
| Startup | Macrobenchmark / Perfetto; bổ sung `adb shell am start -W` | Cold/warm riêng, nhiều lần, thống kê p50/p95; nói rõ TTID/TTFD hay `TotalTime`, không gọi mọi chỉ số là thời gian Home sẵn sàng. |
| Cuộn Home/Discover | Macrobenchmark / Perfetto / `adb shell dumpsys gfxinfo` | Cùng thao tác, refresh rate và dữ liệu; frame time và janky frames, reset counters trước mỗi run. |
| RAM / CPU | Android Studio Profiler / `adb shell dumpsys meminfo` | Idle, cuộn và trận cùng độ dài; ghi peak, thời gian lấy mẫu, PSS và điều kiện tải. LeakCanary là công cụ phát hiện, không thay cho đo RAM. |
| Network / latency | Network Inspector hoặc instrumentation đã redaction | Payload, số request, hit/miss cache và REST/Socket ACK p50/p95; cùng mạng và backend; phân biệt cache ấm/lạnh. |
| Pin | Android Studio Power Profiler / `dumpsys batterystats` khi phù hợp | Phiên cố định, màn hình/mạng/pin/nhiệt độ như nhau; ghi phương pháp, thời gian và giới hạn độ phân giải. |

## Bốn kịch bản bảo mật có kiểm soát

1. Upload: file hợp lệ, sai magic bytes và loại không hỗ trợ trong môi trường thử; xác minh response backend, không suy ra chặn mọi mã độc.
2. Refresh replay: xoay refresh token rồi thử token cũ trong tài khoản test; ghi hành vi phiên khác và log đã che bí mật.
3. Guest token: hợp lệ, sai chữ ký, hết hạn, thiếu token và cookie user ưu tiên trên history/summary/my-answers. Không ghi token vào kết quả.
4. Dữ liệu cục bộ: kiểm tra bản cài đặt test trước/sau migration, backup và key mất; xác minh cookie/token không hiện plaintext theo threat model đã chọn. Không hứa chống được root hoặc thiết bị bị chiếm quyền.

Thêm case privacy `showLeaderboard=never`: kiểm tra trực tiếp payload `/results` **và** `/summary` của host/player, không chỉ UI. Nếu backend chưa đáp ứng, ghi hạn chế thay vì sửa backend trong phạm vi môn học.

## Mẫu ghi kết quả

| Chỉ số | Before | After | Đơn vị / percentile | N / điều kiện | Bằng chứng |
|---|---|---|---|---|---|
| Chưa đo | — | — | — | — | — |
