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
- Trước khi xóa cookie, RoomCookieStore cố ghi đồng bộ dấu dọn dẹp đang chờ; nếu dấu này đã lưu thành công, lần đọc/ghi sau khi tạo lại store phải thử xóa lại trước. Lỗi lưu trữ được báo cho luồng auth thay vì báo xóa thành công. Nếu không ghi được dấu, dấu bị mất/hỏng hoặc dữ liệu backup/restore không nhất quán, chưa có bảo đảm cookie cũ không trở lại sau restart; việc tiến trình chết trước khi ghi dấu cũng chưa được chứng minh an toàn. Không coi đây là bảo đảm fail-closed tuyệt đối. Logout offline không chứng minh backend đã revoke.
- Khi không ghi được dấu dọn dẹp, store vẫn thử xóa cookie trong Room và báo lỗi thay vì khẳng định đã xóa an toàn. Nếu cả ghi dấu và xóa đều thất bại, hoặc tiến trình chết trước khi xóa, không có bảo đảm an toàn sau restart. Với bản sửa này, unit test PASS (job `9ecf72a0-2d0e-483c-8c14-4f1ba61ce72c`), bao gồm mô phỏng lỗi ghi dấu và lỗi đồng thời; lint PASS (job `5e78396e-34b1-49d0-ab8f-314d19a7aaa3`), assembleDebug PASS (job `cef97330-97e8-4ca3-badc-cc61e1c86ee0`).
- Người dùng xác nhận trên Live 4: sau cài đè code `9a034d5` (job `6e866502-be30-4f06-94a1-3ed7694dc0b2`), Home tải và phiên còn đăng nhập; đăng xuất rồi mở lại vẫn đăng xuất; đăng nhập lại rồi mở lại hoạt động bình thường. Đây là smoke test thủ công, không kiểm chứng race, lỗi storage hay backend revoke.
- `testDebugUnitTest` PASS (job `b6d846b6-6094-4b44-9f6f-f1f1ce0a4116`) trên nội dung code `9a034d5`, gồm race giả lập, refresh đồng thời, lỗi storage và tạo lại store với journal giả. Android test cách ly trên Live 4 / Android 11: 3 test PASS (job `ace43c29-c52e-4f38-a485-aef548ec5692`), dùng database/cookie giả, tạo lại store với dấu đã lưu trong SharedPreferences; không giả lập process death hay lỗi ghi dấu. Không thay thế login/logout, upgrade và network E2E trên thiết bị thật.

## Khi thực hiện

Bổ sung quyết định, sơ đồ dữ liệu, kết quả kiểm chứng và bẫy mới sau khi nghiệm thu; không lưu bí mật hoặc lặp lại tiến độ task trong file này.
