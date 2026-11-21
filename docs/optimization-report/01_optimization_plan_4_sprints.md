# Kế hoạch 4 Sprint — Tối ưu phần mềm ứng dụng MyQuizApp

> Kế hoạch đề xuất cho môn học, không phải tính năng đã triển khai. Baseline Android `main` `35d6bfd`, backend `main` `37038f6`; mọi thay đổi code thực hiện trên Android. Không sửa backend; các vấn đề backend là hạn chế hoặc yêu cầu chuyển team backend.

## Nguyên tắc

- Đo baseline trước thay đổi và đo lại cùng thiết bị, build, dữ liệu, kịch bản; không bịa phần trăm cải thiện. Tách kết quả đo được, mục tiêu và giả thuyết.
- Không dùng UI ẩn dữ liệu để khẳng định server đã bảo vệ dữ liệu. Không log token, cookie hoặc dữ liệu người dùng trong evidence.
- Không coi Classic/Solo là không có lỗi; chọn cấu hình đo ổn định sau pilot và công bố các giới hạn. Test bảo mật backend chỉ trong môi trường được phép, không sửa code backend.

## Sprint 1 — Tương thích và an toàn client

1. **C04:** nhận `guestToken` từ join, thiết kế lưu token nhạy cảm, truyền `x-guest-token` cho history, summary và my-answers; giữ quy tắc cookie user ưu tiên. Xử lý token thiếu/hết hạn, nâng cấp từ cài đặt cũ và quy trình lấy token mới theo contract backend. Test guest/user/chuyển tài khoản, dữ liệu cũ và ba API.
2. **N44-OFFLINE-BOOT:** tái hiện và tìm nguyên nhân toàn luồng session `Unknown` → UI Activity/Profile, không giả định chỉ cần bắt `IOException`. Thiết kế lỗi/offline/retry có giới hạn; test cold start offline, mạng chậm và phục hồi mà không logout nhầm.
3. **Kênh truyền:** kiểm tra cấu hình TLS/cleartext thực tế trên các API Android hỗ trợ; thiết kế network security config theo từng build variant. CA debug chỉ cho debug. Pinning chỉ cân nhắc sau khi có chính sách backup pin, rotation và khôi phục, không mặc định áp dụng.
4. **Lưu trữ:** đánh giá `RoomCookieStore`, `CookieEntity`, guest identity và backup; chọn mã hóa dùng Keystore, migration/rollback, mất key, logout và khôi phục app. Không tuyên bố `allowBackup=false` ngăn được mọi hình thức trích xuất hay thay thế mã hóa.

**Gate:** test đơn vị/integration cho contract, offline, migration và session isolation; không đóng vấn đề backend từ thay đổi UI.

## Sprint 2 — Hiệu năng và vòng đời

1. Đo trước, sau đó đánh giá cấu hình Coil 2.7 (`ImageLoader`, memory/disk cache, kích thước ảnh) theo API thư viện thực tế; không hứa bitmap pool hay mức tiết kiệm trước khi đo.
2. Xây dựng Baseline Profile với kịch bản và benchmark phù hợp; so sánh build profileable/release tương đương, không mặc định chỉ thêm `profileinstaller` là đã có profile.
3. Đánh giá socket foreground/background theo yêu cầu trận đấu, deadline, presence và reconnect. Chỉ ngắt khi có chính sách rõ ràng; đo pin và regression trước/sau, không tự giả định có thể dừng ping mà không ảnh hưởng trận.
4. LeakCanary debug; release minified smoke test, kiểm tra R8 rules và mapping, thêm rule tối thiểu theo lỗi thực tế.

**Gate:** test hành vi sau mỗi thay đổi và ghi log + commit. Không cam kết tỷ lệ giảm thời gian, RAM hoặc APK khi chưa đo.

## Sprint 3 — Thực nghiệm

- Theo [kịch bản đo lường](03_benchmark_metrics_scenarios.md): startup, frame/jank, CPU/RAM, mạng/latency, pin và 4 kịch bản bảo mật.
- Lưu dữ liệu thô, cỡ mẫu, điều kiện mạng, thiết bị, phiên bản OS/build/backend, warmup, bất thường và cách tính percentile. Không đưa credential vào báo cáo.
- Chọn Classic hoặc Solo không shuffle sau khi pilot; ghi sự cố và không ngoại suy kết quả sang các mode chưa test.

## Sprint 4 — Báo cáo

1. Hiện trạng và kiến trúc, phạm vi Android, baseline và giới hạn.
2. Mỗi giải pháp: vấn đề, cách triển khai, kiểm thử, chi phí và trade-off.
3. So sánh before/after theo dữ liệu thô; nêu trường hợp không cải thiện và giới hạn phép đo.
4. Dẫn chứng backend đã có chỉ khi kiểm tra được nguồn/bằng chứng; phân biệt thành quả backend với công việc Android của môn học.
5. Kết luận và các bug backend chưa xử lý, gồm [báo cáo 17 bug](../backend-audit/01_backend_17_bugs_status.md) và rủi ro privacy summary.

Xem thêm [các trade-off](02_performance_security_tradeoffs.md) và [release gates](../roadmap/03_n47_n50_release_gates.md).
