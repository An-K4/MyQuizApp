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
- Theo lựa chọn của người dùng, bỏ journal/dấu dọn dẹp và xóa cookie Room trực tiếp; epoch guard vẫn ngăn phản hồi cũ làm thay đổi phiên mới. Lỗi xóa chặn dùng cookie trong tiến trình hiện tại và được báo thay vì khẳng định đăng xuất an toàn. Lần mở sau kiểm tra phiên với backend; nếu backend xác nhận phiên không hợp lệ thì về guest và thử xóa lại. Mất mạng không được coi là phiên hết hạn.
- Logout chủ động: snapshot cookie hợp lệ theo scope và enqueue một request riêng trước khi xóa local; không đợi backend. Chỉ điều hướng khi xóa local thành công. Request dùng client không ghi cookie response, không redirect hay tự retry; không có hàng đợi bền vững, nên process chết/mất mạng có thể khiến server không thu hồi. Không gọi kết quả local là xác nhận revoke server.
- Backend trong repo yêu cầu cả accessToken và refreshToken cho logout; `logoutService` có nhánh revoke mọi phiên người dùng nếu refresh token không còn tồn tại. Request cũ đến trễ hoặc refresh token bị xoay có thể ảnh hưởng phiên mới/thiết bị khác. Guard epoch ở Android không bảo vệ khỏi tác động phía server; deployment thật chưa xác minh. Không hứa chắc an toàn race phía backend.
- Giới hạn: nếu server chưa nhận logout và xóa local thất bại, không có dấu lưu ý định đăng xuất sau khi tiến trình chết; backend có thể vẫn chấp nhận phiên. Cảnh báo người dùng rằng chưa xác nhận đăng xuất, không tuyên bố fail-closed. Logout offline không chứng minh backend thu hồi token; cần kiểm chứng contract và deployment server. Không mặc định coi mọi token đã bị thu hồi.
- Người dùng xác nhận trên Live 4: sau cài đè code `9a034d5` (job `6e866502-be30-4f06-94a1-3ed7694dc0b2`), Home tải và phiên còn đăng nhập; đăng xuất rồi mở lại vẫn đăng xuất; đăng nhập lại rồi mở lại hoạt động bình thường. Đây là smoke test thủ công, không kiểm chứng race, lỗi storage hay backend revoke.
- Unit test trên bản không journal `b6bac07` PASS (job `7c15073e-50b8-4754-8df4-dff0f4ed0d49`), lint PASS (`6c7e5ced-90b1-408b-9ecc-6e4d7bb864c7`), assembleDebug PASS (`7781461b-081f-42c3-9d4e-adda78e045aa`). Test gồm server giả trả 401 sau khi tạo lại coordinator và retry xóa sau lỗi; đây là mô phỏng, không chứng minh backend thật revoke. Chức năng retry dọn local của màn bảo mật dùng state trong RAM gắn với epoch/revision để tránh xóa phiên mới; không phải dấu lưu qua restart.
- Bằng chứng lịch sử cho code `9a034d5` có journal (không phải code hiện tại): unit test PASS (job `b6d846b6-6094-4b44-9f6f-f1f1ce0a4116`), Android test cách ly 3 test PASS (job `ace43c29-c52e-4f38-a485-aef548ec5692`). Smoke test thủ công phía trên cũng là trên code cũ; cần kiểm tra lại bản không journal.

## Khi thực hiện

Bổ sung quyết định, sơ đồ dữ liệu, kết quả kiểm chứng và bẫy mới sau khi nghiệm thu; không lưu bí mật hoặc lặp lại tiến độ task trong file này.
