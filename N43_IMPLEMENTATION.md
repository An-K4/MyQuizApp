# N43 — Bảo mật tài khoản

**Trạng thái: source đã triển khai trên main, CHƯA chốt N43.** Chờ build/unit test và kiểm thử nhanh máy thật. Không có commit/push tự động, không sửa backend, không gọi API đổi mật khẩu/vô hiệu hóa thật.

## Kiến trúc

`AccountSecurityScreen → AccountSecurityViewModel → ObserveSecurityAccountUseCase / ChangePasswordUseCase / DeactivateAccountUseCase → interface SessionRepository/UserRepository (core:common) → implementation (core:network)`.

- ViewModel mới chỉ inject Use case, không Repository, CookieStore, Retrofit hay API service.
- UiState, Intent, Effect và Screen tách file; Screen/Content tách stateful/stateless; Preview Light/Dark và Google-only.
- Domain validation tái dùng PasswordValidator; không trim password.
- Không thêm module Gradle; Profile chỉ chuyển callback điều hướng, không điều phối nghiệp vụ bảo mật.

## Contract đã audit

Backend commit `7c103c87b4c78d817e8a7acf50fd0424edd16c79`, Android baseline `0da538f3e2dc99e56f37c3e186ac2d1487d63f7e`.

- PATCH `/v1/users/me/password`: camelCase `oldPassword/newPassword`, mỗi field ít nhất 8 ký tự. Mật khẩu mới khác cũ; sai cũ `USER_PASSWORD_INCORRECT`, trùng `RESET_PASSWORD_REUSED`.
- DELETE `/v1/users/me`: JSON body `password`, Retrofit @HTTP(hasBody=true). Soft delete bằng `deleted_at`, không tuyên bố xóa toàn bộ dữ liệu hoặc tự khôi phục.
- Hai mutation không tự revoke mọi session và không clear cookie; đổi password giữ phiên hiện tại. Khác với luồng reset-password vốn revoke refresh sessions.
- Google-only không có password: backend `AUTH_GOOGLE_ONLY`, UI giải thích không hỗ trợ. Tài khoản local liên kết Google vẫn dùng password local.
- Error map theo code, không dùng raw server message/exception.

## Hành vi và hardening

- Đổi password thành công xóa trường mật khẩu, snackbar, giữ phiên.
- Vô hiệu hóa cần password + dialog xác nhận; thành công gọi cleanup local có generation guard, Guest và reset MainGraph/Home. Saved back stacks các tab bị dọn để tránh khôi phục UI của account cũ.
- Timeout/lỗi mạng/5xx sau gửi mutation được xem là kết quả không chắc chắn, không auto replay. Các trường secret bị xóa và submit bị khóa ở màn hiện tại; người dùng kiểm tra bằng đăng nhập trước khi thử tiếp.
- Lỗi cleanup sau mutation được xác nhận cho nút chỉ dọn local, không DELETE lại. Terminal session không bị thông báo nhầm là vô hiệu hóa thành công.
- Mutation giữ client PreserveCase + retryOnConnectionFailure(false). 401 do authMiddleware từ chối trước mutation vẫn theo refresh hiện có.
- Refresh lỗi tạm thời giữ cookie và chuyển thành lỗi transport, không propagate 401 gốc làm user bị đăng xuất. Refresh terminal mới clear cookie.
- SafeHttpLogger chỉ log method/encodedPath/status ở debug; không body/header/query/redirect/exception-message. DTO/state/intent toString được redact; password chỉ ở RAM, không SavedStateHandle/route/Room/DataStore.
- Generation/revision guard phủ response cũ và cleanup đối với phiên login đã publish; vẫn cần kiểm thử concurrency/transport thật, không coi static review là chứng minh mọi race đã được loại trừ.

## Kiểm chứng

- Bổ sung **25 test case**: 10 Use case, 5 ViewModel, 4 contract API, 3 refresh, 1 logging, 2 session cleanup. Cập nhật fake repository của test Profile để giữ compatibility.
- `git diff --check` đã pass trước lượt rà cuối, cần chạy lại sau mọi sửa cuối.
- Gradle bị MCP allowlist chặn; KHÔNG có bằng chứng compile/test đã xanh. Không có CI run mới vì chưa commit/push.
- Không đánh dấu N43 hoàn tất hoặc bắt đầu N44 trước khi build/test và luồng máy thật được xác nhận.

Chạy từ repo trên Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :core:network:testDebugUnitTest :app:assembleDebug
```

## Checklist test nhanh

1. Hồ sơ → Bảo mật tài khoản: màn con, bottom bar ẩn, Back đúng, mật khẩu không được khôi phục sau rời màn/recreation.
2. Validation: dưới 8 ký tự, confirm sai, new trùng old; không request khi invalid.
3. Sai password hiện tại: lỗi tiếng Việt, còn session, sửa và gửi lại được.
4. Đổi password đúng: snackbar, ô input rỗng, không bị logout; logout chủ động rồi kiểm tra old không còn đăng nhập được/new đăng nhập được.
5. Google-only: hiện giải thích, không có thao tác giả; local liên kết Google vẫn sử dụng chức năng local.
6. **Dùng tài khoản test dùng một lần**: hủy xác nhận vô hiệu hóa không gửi DELETE; sai password không logout; đúng password → Guest/Home, Back/đổi tab/mở app không khôi phục account cũ; đăng nhập lại bị backend chặn.
7. Mất mạng/response không rõ: không tự gửi lại mutation, không báo thành công giả. Refresh bị lỗi mạng/5xx không tự clear session.
8. Logcat không chứa password/cookie/token/ticket. Không gửi log có secret.

## Tài liệu và việc tiếp theo

Các tài liệu lộ trình/design/structure gốc nằm trên nhánh `docs`, không có trong working tree main. Không checkout qua nhánh khác khi source đang dirty; không push/commit thay người dùng. Báo cáo này ghi trạng thái N43 tạm thời; sau khi test pass mới cập nhật các tài liệu canonical trên docs và chốt N43.

N44 integration và N45 feature gate tiếp theo; N20.6/N25/N28.5/N29/N30 vẫn backend-blocked. Không workaround server-authoritative, không lấn sang polish N46.
