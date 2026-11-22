# N47–N50 — Kế hoạch kiểm thử và release gates

> **LỊCH SỬ — môn Lập trình Android nâng cao đã báo cáo:** Các task và gate dưới đây là hồ sơ cũ kể cả khi chưa thực hiện; không suy đã PASS. Theo dõi công việc hai môn mới tại [roadmap bảo mật và tối ưu](04_security_performance_plan.md).

> Kế hoạch, chưa phải kết quả kiểm thử. Baseline Android `35d6bfd`, backend `37038f6`; N44 PARTIAL và N46 WIP theo hồ sơ. Bằng chứng cần gắn commit, thiết bị, ngày chạy và kết quả; không suy PASS từ việc code đã được commit.

## N47 — Regression và test tự động

- Chạy CI tương đương `./gradlew lint --continue`, `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`; lưu log/report và commit. Trên Windows dùng `gradlew.bat`.
- Regression auth/session, logout/đổi user, offline boot, history/summary/my-answers, upload ảnh; host/player Classic và bốn mode self-paced, reconnect, pause/resume, timeout và privacy.
- Với BUG-13, xác nhận kết quả cá nhân sau reconnect ở màn kết quả; với C04 thử guest mới, token hết hạn/sai, user có cookie và ba API history. Ghi rõ test nào bị backend block.
- **Gate:** kết quả và lỗi còn mở được ghi nhận; không gộp các XML lịch sử thành một run mới.

## N48 — Release hardening

- Thử build release có R8, smoke test APK minified, kiểm tra rules thực sự cần; không mặc định file rules rỗng đồng nghĩa R8 không hoạt động.
- Đánh giá leak với LeakCanary trên debug; kiểm tra cookie/guest token lưu cục bộ, backup, TLS/cleartext và an toàn log; test nâng cấp dữ liệu và rollback theo thay đổi được duyệt.
- **Gate:** lưu build artifact/hash, mapping, test và quyết định cho mỗi rủi ro; không ghi «0 leaks» nếu chưa có kết quả kiểm thử.

## N49 — UAT / internal testing

- Chạy có kiểm soát trên thiết bị/mạng đại diện: join, host/player, các game mode, recovery, đổi tài khoản, lỗi mạng. Ghi môi trường backend và cấu hình phòng.
- Nếu kiểm thử tải 20–50 player, chuẩn bị môi trường và quyền trước; ghi số lượng thực tế, kịch bản, sự cố và tài nguyên. Đây là mục tiêu thử nghiệm, không phải kết quả đã đạt.
- **Gate:** tổng hợp pass/fail/deferred, mức ảnh hưởng và quyết định chấp nhận rủi ro; kiểm tra privacy ở payload server, không chỉ UI.

## N50 — Go / No-Go và bàn giao

- Chỉ quyết định phát hành sau khi kiểm tra N44/N45 và N47–N49, build/CI, các blocker và rủi ro backend còn mở. Nếu gate chưa đạt, ghi **No-Go** hoặc quyết định ngoại lệ có người chịu trách nhiệm.
- Bàn giao commit, artifact, report, known issues, hướng rollback và người phê duyệt. Không tự phát hành, merge hoặc bật ruleset thay người có quyền.

## Tham chiếu

- [Lộ trình 50 ngày](01_50_day_roadmap.md) · [N46 progress](02_n46_ui_polish_progress.md) · [N44 E2E lịch sử](../archive/chk_n44_e2e_report.md)
- [Các bug backend và giới hạn](../backend-audit/01_backend_17_bugs_status.md) · [Kế hoạch tối ưu Android](../optimization-report/01_optimization_plan_4_sprints.md)
