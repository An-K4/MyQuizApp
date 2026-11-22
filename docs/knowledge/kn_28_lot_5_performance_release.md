# Lô 5 — Hiệu năng, phát hành và báo cáo

> Tri thức chuẩn bị T18–T25 trong [roadmap hai môn](../roadmap/04_security_performance_plan.md); chưa có số đo before/after mới.

## Bối cảnh và bẫy

- Đo trước/sau cùng thiết bị, OS, build, dataset, mạng, thời gian, trạng thái cache và Backend; ghi mẫu thô, p50/p95, sai số và thất bại. FPS, CPU, RAM, pin, traffic và latency cần cách đo/đơn vị rõ.
- Coil, Baseline Profile và tối ưu Compose chỉ là giả thuyết đến khi chứng minh; profileinstaller đơn lẻ không tạo Baseline Profile. LeakCanary không thay đo RAM.
- R8 đã bật trong release source nhưng release minified/signed chưa được smoke-test; APK debug từ workflow không phải bản phát hành ký production.
- Khóa ký và secrets ở ngoài repo, không đưa vào log/báo cáo. Quyền mở rộng cho profile, release, profiler hoặc test device phải xin riêng khi cần.

## Khi thực hiện

Lưu phương pháp, quyết định tối ưu hoặc không tối ưu, chi phí bảo mật và hạn chế đo; số trạng thái và evidence theo task nằm tại roadmap.
