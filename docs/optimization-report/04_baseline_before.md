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

## Warm relaunch — phép đo pilot

- Sau lần đo Home, người dùng đưa app ra nền bằng `input keyevent 3`, chờ 2 giây rồi gọi `am start -W` cùng MainActivity, chờ 5 giây và lặp 5 lần. Cả 5 lần `Status: ok`, `LaunchState: WARM`; pin/nhiệt độ chính xác lúc đo chưa được lấy lại.

| Lần | TotalTime (ms) | WaitTime (ms) |
|---:|---:|---:|
| 1 | 325 | 334 |
| 2 | 293 | 305 |
| 3 | 294 | 304 |
| 4 | 292 | 304 |
| 5 | 277 | 285 |

`TotalTime` trung vị **293 ms** (N=5), khoảng **277–325 ms**. Đây là kết quả relaunch Activity do Android phân loại warm, không phải thời gian Home sẵn sàng tương tác; không suy p95 ổn định từ 5 mẫu và không so trực tiếp với cold như hiệu quả tối ưu.

## RAM, CPU và đồ họa — phép đo pilot

- Người dùng xác nhận phép đo dùng máy Live 4 và app baseline như phiên trước. Kết quả do người dùng chạy ADB trong VS Code. Trước phép đo `dumpsys battery`: mức pin 3%, USB powered, status 2, nhiệt độ pin 31,2°C. Mức pin rất thấp và trạng thái sạc là yếu tố gây nhiễu; không suy ra mức tiêu hao pin từ số này. Người dùng xác nhận dùng Wi-Fi; dataset chưa được xác nhận. Một lần đọc `dumpsys display` sau đó cho mode 1080×2340, 60,000004 Hz; không chứng minh mode luôn giữ nguyên trong mọi phép đo.
- Trình tự yêu cầu: Home đã tải xong, `dumpsys meminfo`, `dumpsys gfxinfo ... reset`, thao tác cuộn khoảng 30 giây rồi `dumpsys gfxinfo`. Người dùng xác nhận đã cuộn Home khoảng 30 giây. Khoảng uptime giữa output reset và output sau là khoảng 33,6 giây, không đồng nghĩa thời gian cuộn chính xác.
- `meminfo` là một mẫu tức thời trước thao tác, không phải peak hoặc trung bình: TOTAL PSS **196504 KB**, TOTAL RSS trong App Summary **270408 KB**, TOTAL SWAP PSS **149 KB**. Các giá trị là số Android báo; không suy ra mức tiêu thụ riêng của thao tác cuộn.
- Sau phép đo Home, `dumpsys cpuinfo` lọc tiến trình app báo **0%** (0% user + 0% kernel); thiếu cửa sổ lấy mẫu vì output đã lọc, chỉ là một lần đọc, không đại diện CPU khi cuộn hay trung bình cả phiên.
- `gfxinfo` sau reset: **1334 frames**, **167 janky frames (12,52%)**; frame duration p50 **10 ms**, p90 **19 ms**, p95 **38 ms**, p99 **113 ms**; missed vsync 23, frame deadline missed 57. GPU p50/p90/p95/p99 lần lượt **13/19/20/22 ms**. Số liệu là histogram tổng hợp của một lần chạy, không phải FPS hay mẫu thống kê giữa các lần chạy.
- Output của lệnh `gfxinfo ... reset` còn hiển thị thống kê cũ (464 frames, 92 janky); không dùng nó làm kết quả sau reset hoặc so sánh với lần sau. Các giá trị chi tiết/histogram đầy đủ hiện nằm trong output người dùng gửi trong chat; bản ghi trên chỉ trích xuất các số chính, không phải bản lưu toàn bộ raw dump.

## Cold start offline — tái hiện trước T04

- Kịch bản yêu cầu tắt Wi-Fi và dữ liệu di động; người dùng chạy `am force-stop` rồi `am start -W` bản baseline và báo giao diện mất kết nối. Android báo `Status: ok`, `LaunchState: COLD`, `TotalTime: 7600 ms`, `WaitTime: 7604 ms`; đây là một mẫu offline, không dùng so sánh hiệu năng với loạt startup online.
- Người dùng quan sát cả Hoạt động và Hồ sơ quay loading liên tục; Home và Thư viện hiển thị badge mất kết nối. Sau bật lại Wi-Fi, người dùng xác nhận hai màn hình không tự phục hồi. Chưa xác nhận tình trạng phiên hay kết quả sau khởi động lại app.
- Đối chiếu tĩnh với mã: `SessionRepositoryImpl` giữ nguyên trạng thái khi `getCurrentUser` trả lỗi không thuộc nhóm kết thúc phiên; Activity hiển thị loading khi `Unknown` và Profile xác định `isLoading` theo `Unknown`. `AppNavGraph` làm mới phiên khi vào graph hoặc app trở lại foreground; `NetworkStatusViewModel` chỉ phơi bày trạng thái mạng, không gọi refresh khi mạng phục hồi. Điều này phù hợp hiện tượng, nhưng chưa xác minh riêng loại lỗi thực tế tại thiết bị.

## Các số liệu còn thiếu

| Nội dung | Trạng thái | Lý do / bước tiếp |
|---|---|---|
| Startup warm, thời gian Home tương tác | PARTIAL | Có 5 mẫu warm Activity; thời gian Home tương tác chưa đo. |
| RAM/PSS và CPU | PARTIAL | Có một mẫu PSS/RSS và một dòng cpuinfo; peak và CPU theo kịch bản chưa đo. |
| FPS, frame time/jank Home/Discover | PARTIAL | Có một mẫu gfxinfo Home và mode 60 Hz ở lần đọc sau; chưa đo FPS. |
| REST/socket latency và lưu lượng | NOT RUN | Cần kịch bản test, cấu hình backend/mạng và công cụ đo đã che dữ liệu. |
| Pin theo phiên cố định | NOT RUN | Chưa có phép đo; thiết bị đang sạc ở mức pin thấp. |
| After | NOT RUN | Chưa thử bản cải tiến và chưa đủ điều kiện so sánh. |

Không coi T01 là PASS hoặc cho T04 vượt gate chỉ từ các mẫu pilot này. Phương pháp chung và kịch bản xem [03](03_benchmark_metrics_scenarios.md); chỉ cập nhật tiến độ trong [roadmap](../roadmap/04_security_performance_plan.md).
