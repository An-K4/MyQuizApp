# Lô 3 — Kênh truyền và thành phần Android

> Tri thức chuẩn bị T10–T13; xem [roadmap hai môn](../roadmap/04_security_performance_plan.md) để theo dõi trạng thái. Chưa có chứng cứ runtime mới.

## Bối cảnh và bẫy

- API, public, presigned upload và Socket.IO có thể dùng client/host khác nhau; không suy pin một client bảo vệ tất cả. Không tự bịa pin hoặc yêu cầu private key.
- Pin SPKI cần chính sách backup/rotation; debug CA chỉ ở debug; kiểm tra cả sai CA, sai pin và WSS. Pin sai có thể khóa ứng dụng khỏi Backend.
- App Link `autoVerify` không chứng minh domain đã xác minh. Tài liệu cũ ghi từng có `assetlinks.json`; phản hồi Backend mới nói hiện thiếu, chưa kiểm tra được deployment. Người dùng xác nhận từng tự mở thành công link email thật vào app; chưa có thông tin build/ngày hoặc trạng thái xác minh domain. Android xử lý link reset và đổi token thành ticket; Backend local tạo đường dẫn `/reset-password/link`, vì vậy không siết path làm hỏng luồng. Kiểm tra merged manifest, explicit Intent, action/path/token và assetlinks với fingerprint đúng.
- Quyền URI cần giới hạn thời gian/phạm vi; không suy không có exported component chỉ từ manifest nguồn.

## Khi thực hiện

Bổ sung kết luận được kiểm chứng, phạm vi client và hướng phục hồi khi chứng chỉ đổi. Không lưu certificate private key, token hay thông tin cá nhân.
