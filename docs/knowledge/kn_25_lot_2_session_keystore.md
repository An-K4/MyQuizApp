# Lô 2 — Cookie, phiên và Keystore

> Tri thức chuẩn bị T05–T09; [roadmap hai môn](../roadmap/04_security_performance_plan.md) là nguồn trạng thái. Mọi bằng chứng kỹ thuật phải gắn commit và test thực tế.

## Bối cảnh và bẫy

- Cookie có domain, host-only, path, Secure, expiry và nhiều cookie cùng tên; chỉ lọc host/name không tương đương `Cookie.matches`.
- Test chẩn đoán `N44CookieRaceReproductionTest` mô tả refresh cũ và Set-Cookie đến muộn; chưa coi là kết quả test hiện tại.
- Mã hóa giá trị cookie cần migration dữ liệu rõ, xử lý mất khóa, backup/restore và logout. Không đồng nhất UUID guest với bearer guestToken; Guest token cần bảo vệ và tuân thủ contract Backend.
- Logout khi offline có thể xóa local mà không chứng minh server thu hồi; Keystore không bảo vệ khỏi mọi thiết bị đã bị chiếm quyền.

## Migration cookie v1 → v2 (T05)

- v1 chỉ lưu khóa `responseHost|name`, không lưu cờ host-only và không phân biệt nhiều path cùng tên. Không thể khôi phục chính xác phạm vi ban đầu từ dữ liệu cũ.
- v2 lưu danh tính `(name, domain, path)` và host-only; cookie v1 được giữ lại với phạm vi an toàn là đúng response host, không mở rộng sang miền con. Cookie cũ có thể bị thu hẹp so với Domain ban đầu; xác minh phiên thật sau nâng cấp là bắt buộc.
- Bản ghi cũ được đánh dấu legacy để Set-Cookie mới hoặc xóa từ server thay thế/xóa bản cũ cùng tên và path; cookie mới dùng scope server cung cấp.
- Kiểm thử Room trên Android 11 bằng database/cookie giả: 2 test PASS (job `60b92150`), gồm migration giữ cookie và dữ liệu cache, xử lý xóa và lưu nhiều path. Sau cài đè bản T05 trên Live 4 Android 11 (job `73c0a30e`), user xác nhận phiên đang đăng nhập vẫn còn; đây là smoke test, không thay cho kiểm thử mọi vòng đời.

## Guard vòng đời credential (T06)

- CookieJar mặc định không biết request thuộc phiên nào: chỉ guard `SessionState` không ngăn `Set-Cookie` cũ ghi trở lại hoặc terminal refresh cũ xóa cookie mới. Trên nhánh T06 dựa vào `c8d9463`, request mang epoch; network interceptor kiểm tra epoch khi đọc/ghi cookie, coordinator đồng bộ thao tác lưu/xóa cục bộ và không giữ lock khi chờ network.
- Login mới bắt đầu epoch và dọn cookie cũ; khi còn pending, request không thuộc auth không được lấy cookie hay tự refresh. Logout và kết quả terminal chỉ retire epoch đã bắt. Refresh đồng thời dùng version để tái sử dụng kết quả refresh thay vì liên tiếp refresh token đã xoay.
- Nếu xóa kho thất bại, tiến trình hiện tại chặn đọc/ghi cookie đến khi xóa thành công. Cờ chặn chỉ nằm trong RAM: **không chứng minh an toàn qua process restart nếu storage lỗi**; không được gọi đây là bảo đảm fail-closed bền vững. Logout offline không chứng minh backend đã revoke.
- `testDebugUnitTest` PASS trên bản nhánh chưa commit (job `2698e259-2720-4930-a7a7-f9f2d6336edb`), gồm race giả lập, refresh đồng thời và lỗi storage. Không thay thế login/logout, upgrade và network E2E trên thiết bị thật.

## Khi thực hiện

Bổ sung quyết định, sơ đồ dữ liệu, kết quả kiểm chứng và bẫy mới sau khi nghiệm thu; không lưu bí mật hoặc lặp lại tiến độ task trong file này.
