# INDEX.md — Mục Lục Tra Cứu Kho Tri Thức & Bài Học Kỹ Thuật

> **Giới thiệu:** Thư mục này lưu trữ các bài học, phân tích bẫy kỹ thuật (gotchas) và giải pháp kiến trúc được ghi lại qua các mốc phát triển dự án **MyQuizApp**.
> Bảng mục lục dưới đây giúp bạn tra cứu nhanh chóng bài học theo từng mốc phát triển và chủ đề kỹ thuật.

---

## BẢNG DANH MỤC 24 TÀI LIỆU TRI THỨC

| STT | File tài liệu | Mốc | Chủ đề kỹ thuật & Bài học cốt lõi |
| :---: | :--- | :---: | :--- |
| 00 | [`setup_guide_vn.md`](setup_guide_vn.md) | Setup | Hướng dẫn Docker dựng backend và frontend, các dịch vụ PostgreSQL và Redis. |
| 01 | [`kn_01_n1_n5.md`](kn_01_n1_n5.md) | N1–N5 | Khởi tạo dự án, thiết kế Clean Architecture đa module, nguyên tắc Dependency Inversion (DIP), Result wrapper. |
| 02 | [`kn_02_n6.md`](kn_02_n6.md) | N6 | Type-safe Navigation Compose và kiểm tra phiên qua API tại Splash (bối cảnh lịch sử N6). |
| 03 | [`kn_03_n7_n8.md`](kn_03_n7_n8.md) | N7–N8 | Xây dựng luồng Đăng ký & Đăng nhập, form validation trong Jetpack Compose MVI, xử lý bàn phím IME. |
| 04 | [`kn_04_n9.md`](kn_04_n9.md) | N9 | Google One Tap với Credential Manager, nonce và cấu hình Web Client ID. |
| 05 | [`kn_05_n10.md`](kn_05_n10.md) | N10 | Integration test Android với Hilt, kiểm thử auth và cấu hình test runner. |
| 06 | [`kn_06_tuan2_bonus.md`](kn_06_tuan2_bonus.md) | Tuần 2 | Guest mode, unified navigation, quên mật khẩu và giao diện OTP (quyết định tại thời điểm Tuần 2). |
| 07 | [`kn_07_n11.md`](kn_07_n11.md) | N11 | Home & Search: QuizCard và Quiz, Result wrapper, API service và shared UI. |
| 08 | [`kn_08_n13_n14.md`](kn_08_n13_n14.md) | N13–N14 | Quản lý quiz: danh sách và tạo quiz, Paging 3, Home auth header/Profile (N13.5). |
| 09 | [`kn_09_n15.md`](kn_09_n15.md) | N15 | Upload ảnh presigned S3 hai bước, client PUT riêng, nén ảnh và bẫy `JsonNamingStrategy`. |
| 10 | [`kn_10_n16.md`](kn_10_n16.md) | N16 | Quản lý bộ câu hỏi (`feature:quiz-manage`), tạo và sửa Quiz, validation danh sách câu hỏi. |
| 11 | [`kn_11_n16_5.md`](kn_11_n16_5.md) | N16.5 | Vá contract: reset password ba bước bằng ticket, lỗi `{code}`, search và naming camelCase. |
| 12 | [`kn_12_n17.md`](kn_12_n17.md) | N17 | Tạo phòng chơi (Create Room), cấu hình phòng (GameMode, Timing, Flow), sinh mã 6 ký tự. |
| 13 | [`kn_13_n18.md`](kn_13_n18.md) | N18 | **Tầng Socket.IO:** Đóng gói Socket thành Cold Flow qua `callbackFlow`, dọn dẹp listener trong `awaitClose`, bỏ polling upgrade. |
| 14 | [`kn_14_n19.md`](kn_14_n19.md) | N19 | Player Lobby: Nhận diện người chơi khách (`GuestIdentityStore`), tra phòng và join REST API. |
| 15 | [`kn_15_n19_5.md`](kn_15_n19_5.md) | N19.5 | Tái cấu trúc MainScaffold, Bottom Navigation bar, quản lý trạng thái chọn tab và lưu giữ back stack. |
| 16 | [`kn_16_n19_6.md`](kn_16_n19_6.md) | N19.6 | Hợp nhất quản lý phiên (`SessionRepository`), Login Guard bảo vệ các hành động đòi hỏi đăng nhập. |
| 17 | [`kn_17_n20.md`](kn_17_n20.md) | N20 | Host Lobby: Cập nhật cấu hình phòng theo thời gian thực qua socket, xử lý ACK và phân biệt host token. |
| 18 | [`kn_18_n20_5.md`](kn_18_n20_5.md) | N20.5 | Màn hình Khám phá (Discover), tách riêng `PublicApiRetrofit` không mang cookie, phân trang hỗn hợp. |
| 19 | [`kn_19_n21.md`](kn_19_n21.md) | N21 | Host Control Console: Điều khiển trận đấu, đếm ngược bắt đầu game, đồng bộ trạng thái phase. |
| 20 | [`kn_20_n22_n23.md`](kn_20_n22_n23.md) | N22–N23 | Gameplay Player chế độ Classic: Hiển thị câu hỏi, đếm ngược đồng hồ, lựa chọn và gửi đáp án. |
| 21 | [`kn_21_n24.md`](kn_21_n24.md) | N24 | Kết quả Player, leaderboard, thứ tự event, visibility và pause qua snapshot. |
| 22 | [`kn_22_n25.md`](kn_22_n25.md) | N25 | Kiểm thử tích hợp E2E Classic, cơ chế khôi phục kết nối (Reconnect Hardening) với `ConnectivityManager`. |
| 23 | [`kn_23_ui_stateful_stateless_refactor.md`](kn_23_ui_stateful_stateless_refactor.md) | Chuyên đề | **Quy chuẩn UI Compose:** Phân tách triệt để Stateful và Stateless components, hoist state, tối ưu recomposition. |
