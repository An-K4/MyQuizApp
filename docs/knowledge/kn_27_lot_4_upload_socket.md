# Lô 4 — Upload, chống gian lận và socket

> Tri thức chuẩn bị T14–T17 trong [roadmap hai môn](../roadmap/04_security_performance_plan.md); nội dung sau là nhận định theo source, không phải kết quả kiểm thử mới.

## Bối cảnh và bẫy

- `ImageCompressor` của quiz có thể trả kết quả lớn hơn ngưỡng khi chạm chất lượng tối thiểu. Kiểm tra byte cuối, MIME, chữ ký và lỗi decode trước presign/PUT; thử file giả và ranh giới.
- Theo Backend `37038f6`, kiểm tra sở hữu/metadata và một số magic bytes khi gắn file sau upload; không kết luận file chưa bao giờ vào storage hoặc quét malware.
- Server quyết định quyền, deadline và grading; client không thay thế server. ACK timeout là kết quả chưa chắc chắn: không tự gửi lại gây duplicate.
- Socket ở nền có thể ảnh hưởng presence/reconnect/trận; chỉ tối ưu sau khi đo và có chính sách, không ngắt mặc định.

## Khi thực hiện

Ghi contract được xác minh với Backend, fixture an toàn và kết quả kỹ thuật có thể tái hiện; không lưu payload nhạy cảm hoặc biến bài học thành sổ tiến độ.
