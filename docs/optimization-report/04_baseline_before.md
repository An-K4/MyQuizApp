# T01 — Baseline trước cải tiến (chưa hoàn chỉnh)

## Startup — phép đo pilot

- Android source: `76d849f2c3ef8a4218b87a0c64b29993836fd11d` (`main`), APK debug được cài đè bằng `:app:installDebug`; người dùng xác nhận tài khoản demo vẫn đăng nhập sau cài đặt. Không đối chiếu hash APK.
- Thiết bị: Live 4, Android 11. Người dùng đo bằng VS Code/ADB; phiên bản backend **đang chạy chưa xác minh** (mã backend dùng đối chiếu là `37038f6`). Loại mạng, dataset, trạng thái màn hình và cache filesystem chưa ghi nhận đầy đủ.
- Trước đo: pin 12%, USB đang cấp nguồn, nhiệt độ pin 35,2°C theo `dumpsys battery`. Không xem đây là kết quả đo tiêu hao pin; điều kiện pin thấp/sạc có thể ảnh hưởng khả năng so sánh.
- Quy trình mỗi lần: `am force-stop`, chờ 2 giây, `am start -W -n android.kma.myquizzapp/.MainActivity`, chờ 10 giây. Không xóa dữ liệu hoặc cache. Cả 5 lần `Status: ok`, `LaunchState: COLD`. Đây là cold start tiến trình với cache filesystem không kiểm soát, không phải cold storage cache.

| Lần | TotalTime (ms) | WaitTime (ms) |
|---:|---:|---:|
| 1 | 2641 | 2648 |
| 2 | 2641 | 2644 |
| 3 | 2678 | 2681 |
| 4 | 2665 | 2668 |
| 5 | 2678 | 2681 |

`TotalTime` trung vị 2665 ms (N=5); khoảng 2641–2678 ms. N quá nhỏ để dùng p95 như ước lượng ổn định. Chỉ đo thời gian khởi chạy Activity, không suy ra thời gian Home sẵn sàng tương tác; không so với release hay các điều kiện khác.

## Các số liệu còn thiếu

| Nội dung | Trạng thái | Lý do / bước tiếp |
|---|---|---|
| Startup warm, thời gian Home tương tác | NOT RUN | Cần định nghĩa và đo trên thiết bị. |
| RAM/PSS và CPU | NOT RUN | Cần thu theo kịch bản và thời gian cố định. |
| FPS, frame time/jank Home/Discover | NOT RUN | Cần thao tác cuộn cùng dữ liệu, thu gfxinfo/trace; xác nhận refresh rate. |
| REST/socket latency và lưu lượng | NOT RUN | Cần kịch bản test, cấu hình backend/mạng và công cụ đo đã che dữ liệu. |
| Pin theo phiên cố định | NOT RUN | Chưa có phép đo; thiết bị đang sạc ở mức pin thấp. |
| After | NOT RUN | Chưa thử bản cải tiến và chưa đủ điều kiện so sánh. |

Không coi T01 là PASS hoặc cho T04 vượt gate chỉ từ các mẫu pilot này. Phương pháp chung và kịch bản xem [03](03_benchmark_metrics_scenarios.md); chỉ cập nhật tiến độ trong [roadmap](../roadmap/04_security_performance_plan.md).
