# N16.5 — Vá 3 điểm lệch khẩn (25/8/2026)

Bài học/phát hiện từ N16.5 (sửa luồng Quên mật khẩu sai endpoint + envelope lỗi thật + search cursor), bổ sung cho block "N16.5 Implementation Details" trong file kế hoạch chính.

## 1. Module user của backend dùng camelCase THẬT — không phải snake_case

Backend không nhất quán casing giữa các module: module quiz dùng snake_case (`quiz_name`, `question_count`...), nhưng module user trả/nhận camelCase thật trên wire (`resetTime`, `expiresAt`, `newPassword`, `oldPassword` — xem user.schema.ts/user.controller.ts). Json chung của app có `JsonNamingStrategy.SnakeCase` nên DTO camelCase sẽ bị đổi tên sai hướng, mà `@SerialName` KHÔNG thoát được (bẫy N15, mục 3.1 AGENTS.md).

Fix: tách hẳn `PasswordResetApiService` với cặp Json/Retrofit riêng không namingStrategy (`@PasswordResetJson`/`@PasswordResetRetrofit` trong Qualifiers.kt + NetworkModule.kt) — tái dùng y nguyên pattern `@StorageJson` của N15. **Quy tắc rút ra: khi đọc backend endpoint mới, kiểm tra casing của payload THẬT trong controller/schema trước khi chọn Json nào cho DTO.**

## 2. Luồng reset password 3 bước kiểu ticket (và các con số TTL thật)

Backend (user.route.ts): xin mã → chứng minh đọc được email → đổi pass. Chỉ bước cuối đụng password và nó không hề thấy OTP/token:

1. `POST /users/forgot-password {email}` → `{resetTime, expiresAt}`. OTP/link sống **2 phút** (RESET_TTL — app từng ghi nhầm "5 phút" trên UI).
2. `POST /users/password-reset/verify` — body **strict union**: `{email, otp}` XOR `{token}`; gửi cả hai bị 400. Sai OTP 5 lần → OTP chết (RESET_MAX_ATTEMPTS → RESET_OTP_ATTEMPTS). Trả `{ticket, expiresAt, email}`.
3. `GET /users/password-reset/ticket?ticket=...` (peek) → `{email, expiresAt}`.
4. `POST /users/password-reset/complete {ticket, newPassword}` → ticket chết sau dùng.

**Ticket** = "giấy thông hành" chứng minh user đã verify email; sống **10 phút** (RESET_TICKET_TTL). 2 lần gửi OTP phải cách nhau **≥60s** (RESET_RESEND_TTL), vượt → RATE_LIMITED.

Quyết định UX đi kèm (đã chốt với user):
- **Verify OTP ngay tại màn OTP** (bản cũ chỉ navigate, OTP sai chỉ lộ ra lúc submit password — và endpoint lúc đó còn không tồn tại).
- **Nút gửi lại đếm ngược 60s kể từ khi VÀO màn OTP**, không phải sau lần bấm đầu — vì lần gửi đầu đã xảy ra ở màn Forgot; enable sớm chỉ tổ ăn RATE_LIMITED.
- **Peek ticket lúc mở màn Reset**: hiện email từ server (user biết đang đổi pass cho đúng tài khoản) + bắt sớm ticket hết hạn — đặc biệt nhánh deep link email rất hay mở link cũ đã hết hạn. Lỗi mạng khi peek thì bỏ qua (ticket vẫn thử submit được), chỉ lỗi nghiệp vụ mới chặn form.
- Màn Reset bỏ hẳn dual-flow token/OTP + ToggleFlow của bản cũ — chỉ còn ticket.

## 3. Envelope lỗi chỉ có {code} — client sở hữu toàn bộ wording

`fail()` trong response.ts cố ý chỉ trả `error: {code}` (comment gốc: prose sẽ là tiếng Anh, sẽ lên màn hình phải nói ngôn ngữ khác, và leak internals). Hệ quả client:

- `AppError.Api(code)` chỉ mang code; `AppErrorExt.toUserMessage()` map ~60 code trong `shared/errors/codes.ts` → tiếng Việt, có fallback chung cho code lạ.
- **Khi backend thêm code mới mà client chưa map, user thấy fallback** — nên khi review backend diff có code mới, map ngay vào AppErrorExt.
- Codes.ts có quy tắc "never remove or rename a shipped code" → map của client không bao giờ bị gãy ngược, chỉ cần lo hướng thêm mới.

## 4. Search: 2 bug sót từ N11–12 (lộ ra khi test N16.5)

**Bug A — không có đường kích hoạt search**: SearchScreen có đủ Intent/ViewModel/repo nhưng ô nhập chỉ bắn `QueryChanged`; `SubmitSearch` không được gọi từ bất cứ đâu (không nút, không keyboardActions). Triệu chứng: gõ từ khóa → "Không tìm thấy kết quả" với mọi input. Đây là kiểu bug **compile sạch nhưng luồng chết** — bài học: khi review màn, trace ngược từ handler về UI xem mỗi intent có chỗ gọi thật không; và khi user report "không hoạt động", kiểm tra action có được trigger không TRƯỚC khi nghi backend.

Fix: submit thủ công qua icon kính lúp (`leadingIcon` + IconButton) song song `ImeAction.Search` (phòng dòng máy không nhận phím Search ảo). User quyết **không real-time/debounce** — dạng app này không cần.

**Bug B — load more lặp lại trang 1**: backend không đọc tham số `page`; ViewModel cũ tăng `page = 2, 3...` nhưng request gửi đi y hệt trang 1. Fix N16.5: cursor thật qua `meta.pagination` → `Result.Success.page`; `nextCursor`/`hasMore` đọc từ server thay vì đoán `size >= 20`.

**Lưu ý cursor**: cursor là opaque base64url gắn fingerprint của filter+sort (listing.cursor.ts) — đổi keyword giữa chừng mà giữ cursor cũ → 400 QUIZ_CURSOR_INVALID. Mọi chỗ đổi query/filter/sort phải reset cursor về null.

## 5. Dọn rác sau refactor

MCP filesystem không có quyền delete file (`del` cũng bị allowlist chặn) — 2 usecase cũ của luồng reset (`ResetPasswordUseCase.kt`, `ResetPasswordWithOtpUseCase.kt`) chỉ có thể biến thành stub vô hại (package + comment). Nhớ xóa tay trong IDE; viết TODO trong chính file stub để không ai hiểu nhầm còn dùng.
