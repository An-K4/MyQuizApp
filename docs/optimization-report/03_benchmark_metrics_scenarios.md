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

## Kịch bản demo báo cáo gần

> Kế hoạch trình diễn, **chưa thực hiện**. Tên màn hình và khả năng công cụ tùy phiên bản Android Studio, Android và build; thử trước trên thiết bị, ghi NOT RUN nếu thiếu quyền/hỗ trợ. Phân lô và trạng thái chính thức ở [roadmap](../roadmap/04_security_performance_plan.md#phân-lô-cân-bằng-và-mốc-báo-cáo-gần).

### Chuẩn bị chung

1. Khóa APK/commit before (main trước thay đổi) và after, ghi hash APK khi khả dụng; cùng loại build, máy/OS, backend, tài khoản và dataset test, mạng, độ sáng, pin/nhiệt, refresh rate và quy tắc cache. Không so debug với release. Không xóa dữ liệu app, đăng xuất hoặc reset cache trừ khi kịch bản có chấp thuận và được ghi rõ.
2. Đo before/after theo cùng thao tác, tối thiểu 5 lượt cho startup/latency nếu khả thi; lưu từng mẫu, N, trung vị và phạm vi; p95 chỉ trình bày khi cỡ mẫu đủ và nêu cách tính. Cố định thời lượng cuộn, điều kiện cache và warmup. Ghi raw đã che ở nơi được phép, log build/test và ảnh công cụ; không commit credential, PII hay payload.
3. Người dùng chuẩn bị thiết bị Android Studio thấy được, mở profiler/inspector và thao tác cuộn khi cần. Agent chuẩn bị checklist, build/test trong quyền; lệnh `adb shell`, profiler, trace hoặc cài app vượt allowlist thì nhờ user chạy hoặc cấp đúng quyền, không tự vượt giới hạn. Thiếu backend hay tool: chụp bằng chứng lỗi không nhạy cảm và đánh dấu BLOCKED/NOT RUN.

### Trình tự trình diễn đề xuất (5–8 phút)

| Bước | Công cụ / thao tác | Bằng chứng và giới hạn cần nói |
|---|---|---|
| 1. Bối cảnh | Mở bảng before/after đã lưu, mô tả máy, build, kịch bản và N. | Phân biệt pilot cũ với dữ liệu mới; chỉ kết luận cải thiện khi hai lượt so sánh được. |
| 2. Startup | Android Studio Profiler/system trace hoặc Macrobenchmark nếu hỗ trợ; dự phòng `adb shell am start -W` với các lượt cold/warm riêng. | Hiển thị từng TotalTime và trung vị; TotalTime không phải thời gian Home sẵn sàng. Baseline Profile chưa tạo/áp dụng thì ghi NOT RUN. |
| 3. Mượt Home | T20: cùng dataset và thao tác cuộn; Profiler/system trace hoặc `dumpsys gfxinfo` reset counter trước từng lượt. | Frame time/janky frames, N, refresh rate; trình bày hotspot và diff chỉ khi thực sự tìm/sửa. Jank không tự tương đương FPS. |
| 4. RAM/CPU | Profiler Memory/CPU và `dumpsys meminfo` nếu có quyền; cùng thời lượng idle/cuộn. | Biểu đồ và mẫu PSS, nói rõ PSS không phải Java heap hoặc peak; đo profiling có overhead, so cùng cấu hình. Leak chưa tái hiện không chứng minh không có leak. |
| 5. API/network | App Inspection → Network Inspector nếu phiên bản/build hỗ trợ, thực hiện cùng thao tác API test; che URL nhạy cảm, headers, cookie, token, body. | Số request và duration/bytes mà công cụ thực sự cho thấy; không gọi response size là tổng traffic. Nếu không thấy traffic thì dùng instrumentation đã redaction nếu sẵn có, nếu không ghi NOT RUN. |
| 6. Socket.IO | Vào phòng test, thử kết nối và reconnect; minh họa trạng thái qua UI và event metadata an toàn nếu có. Sau khi T17 sửa BUG-ANDROID-01 và contract đủ điều kiện, thử lưu cấu hình và đo ACK. | Chỉ lưu loại sự kiện, kết quả, thời gian/đơn vị và ID thử ẩn danh; không lưu token/room ID/packet. Inspector không mặc định hỗ trợ Socket.IO/WebSocket frames; nếu không quan sát được hoặc bị lỗi DTO thì ghi BLOCKED/NOT RUN, không tạo số giả. |
| 7. Pin | Với Power Profiler/energy trace hoặc `batterystats` nếu thiết bị hỗ trợ, chạy phiên cố định theo cùng điều kiện; demo có thể chiếu kết quả đã thu. | Nêu tool, khoảng đo, nguồn sạc, nhiệt/độ sáng; số đo toàn máy hoặc ước lượng không tự là năng lượng riêng app. Không suy mức tiết kiệm từ một lần chạy. |
| 8. Bảo mật | T02: chạy test redaction với fixture giả và chiếu kết quả; T06: minh họa UI logout/local và kết quả unit test race, không trình chiếu cookie. | Tách test giả lập khỏi thực tế; logout local không chứng minh backend revoke. T07 migration, T12/T13 hay case khác chỉ demo là đã làm khi có test/phiên bản thực tế. |
| 9. Kết luận | Bảng PASS/FAIL/NOT RUN/BLOCKED, link commit/test và ảnh đã che. | Nêu giới hạn, trade-off và phần còn thiếu; không tuyên bố toàn bộ 7 tiêu chí đã tối ưu. |

### Các thao tác chuẩn bị có thể lặp lại

- Các lệnh dưới đây dành cho người dùng chạy trên máy có Android SDK và đúng một thiết bị test. Kiểm tra `adb devices` trước, chọn đúng APK và lưu commit/build; nếu nhiều thiết bị, chọn serial bằng `adb -s <serial>`. Các lệnh không xóa dữ liệu app; `force-stop` dừng tiến trình và công việc đang chạy, chỉ thực hiện sau khi các thao tác test cần giữ đã kết thúc.
- Đo cold start tiến trình: lặp ít nhất 5 lần; mỗi lần force-stop, đợi 2 giây, khởi chạy, ghi `Status`, `LaunchState`, `TotalTime`, rồi đợi 10 giây trước lượt tiếp theo. Chỉ gộp các lần cùng trạng thái; đây không phải cold filesystem cache hay thời gian Home sẵn sàng.

```bash
adb devices
adb shell am force-stop android.kma.myquizzapp
adb shell am start -W -n android.kma.myquizzapp/.MainActivity
```

- Đo cuộn Home: mở cùng dataset, đợi màn hình ổn định, reset thống kê; cuộn theo cùng hành trình và thời lượng 30 giây, sau đó lấy kết quả. Lặp ít nhất 5 lượt và giữ nguyên refresh rate, cache, thiết bị và thao tác. Đọc janky frames/frame time nếu thiết bị hỗ trợ, ghi rõ nguồn và không quy đổi tùy tiện thành FPS.

```bash
adb shell dumpsys gfxinfo android.kma.myquizzapp reset
adb shell dumpsys gfxinfo android.kma.myquizzapp
adb shell dumpsys meminfo android.kma.myquizzapp
```

- Trên Android Studio, chọn thiết bị và tiến trình app trong Profiler; ghi CPU/Memory hoặc system trace nếu máy hỗ trợ. Mở App Inspection → Network Inspector và thực hiện cùng một thao tác test; nếu không có dữ liệu hoặc không hỗ trợ, ghi NOT RUN. Chuẩn bị ảnh/trace đã che trước khi chiếu: không chiếu live các tab có thể lộ URL nhạy cảm, token, cookie, headers hoặc body.
- Trước buổi báo cáo, thu đủ lượt và tính trung vị/phạm vi; phần trình diễn 5–8 phút dùng bảng và ảnh/trace đã chuẩn bị, có thể chạy lại một lượt cuộn 30 giây để minh họa. Một lượt live không thay thế bộ mẫu before/after.

### Phương án tối thiểu nếu thời gian gấp

- Bắt buộc: kịch bản cuộn Home trước/sau có thể tái hiện, bằng chứng test bảo mật đã chạy, và bảng trạng thái toàn bộ tiêu chí. Nếu T20 chưa tìm thấy điểm nghẽn, trình bày phép đo và kết luận chưa tối ưu, không bịa chênh lệch.
- Nếu công cụ live không sẵn: dùng ảnh/log/trace đã ghi trước và ghi môi trường; nếu chưa có thì hiển thị NOT RUN. Pin và Socket ACK không được lấy pilot không tương đương để dựng kết quả after.
- Bộ tài liệu bàn giao: bảng mẫu ở dưới + commit/APK + hướng dẫn thao tác + ảnh/trace đã redaction + nhật ký bất thường. Các phụ thuộc release, backend, tài khoản và quyền công cụ được liệt kê công khai không chứa secret.

## Mẫu ghi kết quả

| Chỉ số | Before | After | Đơn vị / percentile | N / điều kiện | Bằng chứng |
|---|---|---|---|---|---|
| Chưa đo | — | — | — | — | — |
