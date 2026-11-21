# N43 — Bảo mật tài khoản

**Trạng thái: Android đã triển khai ở commit `ab6621b`; user kiểm thử nhanh ổn; XML local xác nhận 25 test bổ sung PASS.** Báo cáo đã chuyển vào nhánh docs. Chưa có bằng chứng đầy đủ cho full assemble/lint/CI/E2E; agent không tự chạy Gradle qua MCP, không sửa backend hoặc gọi mutation thật trong lượt merge tài liệu.

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
- Đã đọc XML local PASS cho 25 test bổ sung N43: 23 test ở các suite security và 2 retirement cases trong SessionRepository suite. Đây là kết quả user chạy, không phải agent chạy; chưa có full build/lint/CI evidence để chốt toàn bộ gate.
- N44 đã bắt đầu và có source/test evidence riêng; xem N44_INTEGRATION.md. Không nhầm unit XML PASS + user smoke với full CI/E2E pass.

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

Toàn bộ tài liệu dự án, kể cả báo cáo này và AGENTS.md, nằm trên nhánh docs; main chỉ code/config/tests. Không đổi nhánh khi source dirty và không push/commit thay người dùng. Báo cáo này đã merge vào canonical docs, không còn là handoff tạm trên main.

N44 integration và N45 feature gate tiếp theo; N20.6/N25/N28.5/N29/N30 vẫn backend-blocked. Không workaround server-authoritative, không lấn sang polish N46.
