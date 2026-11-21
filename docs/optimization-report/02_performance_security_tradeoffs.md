# Các trade-off hiệu năng, bảo mật và độ tin cậy

> Khung phân tích cho báo cáo; các giải pháp và kết quả phải gắn với source, cấu hình và phép đo cụ thể. Không coi giả thuyết là kết quả đã đạt.

| Trường hợp | Lợi ích / rủi ro | Cần chứng minh |
|---|---|---|
| Kiểm tra chữ ký ảnh ở backend | Kiểm tra byte đầu có thể hạn chế giả mạo định dạng với chi phí truyền thấp; không chứng minh ảnh an toàn tuyệt đối hay quét mã độc. | Đối chiếu implementation, loại file hỗ trợ, test hợp lệ/giả mạo và latency có môi trường được phép. |
| Redis xử lý realtime và checkpoint PostgreSQL | Tối ưu thao tác nóng, checkpoint giúp recovery; mất Redis hoặc crash có chi phí/độ trễ và cửa sổ dữ liệu cần kiểm chứng. | Test failure/recovery, đo latency và tính nhất quán; không khẳng định thời gian hay tỷ lệ thành công chưa đo. |
| Cache và thu hồi khi dữ liệu đổi | Cache giảm truy vấn nhưng có thể trả dữ liệu cũ sau đổi privacy hoặc xóa. | Test invalidation, TTL, quyền truy cập và cache miss; báo cáo trường hợp stale/failure. |
| Mã hóa cục bộ có chọn lọc | Bảo vệ token/cookie khi dữ liệu lưu trữ bị lộ; khởi tạo key, migration và khôi phục có thể tăng chi phí. | Threat model, test key mất/hỏng, migration, backup và thời gian startup before/after. Keystore không bảo vệ khỏi mọi thiết bị đã bị chiếm quyền. |
| Socket khi app ở nền | Giảm hoạt động nền có thể tiết kiệm pin nhưng ngắt kết nối có thể ảnh hưởng trận và reconnect. | Đo năng lượng theo kịch bản, thời gian vắng mặt, recovery và server presence; tôn trọng hành vi game. |

**Giới hạn hiện tại:** `getResults` có kiểm tra ẩn leaderboard, nhưng `getHistorySummary` còn trả leaderboard cho participant trong source `37038f6`. Android ẩn UI không phải sửa bảo mật server. Xem [báo cáo backend](../backend-audit/01_backend_17_bugs_status.md).
