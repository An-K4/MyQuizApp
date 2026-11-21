# Lô 2 — Cookie, phiên và Keystore

> Tri thức chuẩn bị T05–T09; [roadmap hai môn](../roadmap/04_security_performance_plan.md) là nguồn trạng thái. Mọi bằng chứng kỹ thuật phải gắn commit và test thực tế.

## Bối cảnh và bẫy

- Cookie có domain, host-only, path, Secure, expiry và nhiều cookie cùng tên; chỉ lọc host/name không tương đương `Cookie.matches`.
- Test chẩn đoán `N44CookieRaceReproductionTest` mô tả refresh cũ và Set-Cookie đến muộn; chưa coi là kết quả test hiện tại.
- Mã hóa giá trị cookie cần migration dữ liệu rõ, xử lý mất khóa, backup/restore và logout. Không đồng nhất UUID guest với bearer guestToken; Guest token cần bảo vệ và tuân thủ contract Backend.
- Logout khi offline có thể xóa local mà không chứng minh server thu hồi; Keystore không bảo vệ khỏi mọi thiết bị đã bị chiếm quyền.

## Khi thực hiện

Bổ sung quyết định, sơ đồ dữ liệu, kết quả kiểm chứng và bẫy mới sau khi nghiệm thu; không lưu bí mật hoặc lặp lại tiến độ task trong file này.
