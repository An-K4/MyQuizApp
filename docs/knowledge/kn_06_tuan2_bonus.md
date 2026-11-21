# Tuần 2 Bonus Knowledge: Guest Mode + Unified Navigation + Forgot Password + OTP

**Phạm vi:** Công việc bổ sung ngoài kế hoạch, thực hiện 13–15/8/2026 sau khi chốt M2 core (N6–N10)

---

## 📚 TỔNG QUAN

Sau khi hoàn thành M2 core, dự án làm thêm một loạt cải tiến kiến trúc quan trọng ảnh hưởng lâu dài đến cấu trúc navigation toàn app: Guest Mode, refactor Navigation sang unified graph, và Forgot Password đầy đủ với deep linking + OTP.

---

## 1. Guest Mode — mirror backend `optionalAuthMiddleware`

```kotlin
enum class AuthState { Authenticated, Guest, Unauthenticated }
```

- `CheckAuthStateUseCase`, `EnableGuestModeUseCase`.
- `SettingsDataStore.isGuestMode` — flag persistent qua app restart.
- **Vì sao cần**: backend có `optionalAuthMiddleware` cho phép nhiều endpoint hoạt động không cần login (browse quiz, join phòng). App nên phản chiếu đúng model đó ở client, tránh gate sai chỗ (chặn user xem nội dung công khai chỉ vì chưa đăng nhập).

---

## 2. Navigation Refactor: 3 graph riêng → unified MainGraph

| | Trước (N6) | Sau (15/8) |
|---|---|---|
| Cấu trúc | `AuthGraph` / `PlayerGraph` / `HostGraph` riêng | 1 `MainGraph` với bottom nav (Home/Discover/Join/Library/Profile) |
| UX | Auth-first (phải login mới vào được app) | Browse-first — vào Home ngay, chỉ gate khi cần |

**Lý do đổi:** khớp UX của web frontend + đúng tinh thần `optionalAuthMiddleware` phía backend. User nên xem được nội dung public trước, chỉ gặp "soft auth gate" (dạng thông báo "Bạn cần đăng nhập để dùng tính năng này") khi thao tác thật sự cần tài khoản.

⚠️ **Còn tồn action item** (ghi trong plan doc ngày 17/8, cần theo dõi):
- [ ] Xoá host placeholder screen (workaround tạm thời cho lúc Home chưa xong) sau khi flow cuối được chốt.
- [ ] Đảm bảo guest mode tích hợp mượt với navigation mới.

---

## 3. Forgot Password — Dual Flow (Token + OTP)

**Kiến trúc 3 lớp đầy đủ:**
- Data: 3 DTO (`ForgotPasswordRequest`, `ResetPasswordRequest`, `ResetPasswordWithOtpRequest`) + 3 endpoint + Repository.
- Domain: 3 UseCase tương ứng.
- Presentation: 2 ViewModel + 2 Screen (Forgot, Reset — Reset hỗ trợ dual-flow).

**2 luồng song song:**

1. **Token flow (chính)** — user click link trong email → **Android App Link** (`intent-filter android:autoVerify="true"` cho `https://myquizz.dpdns.org/reset-password`) → `MainActivity` xử lý cả `onCreate` và `onNewIntent` → điều hướng thẳng `ResetPassword(email, token)`.
2. **OTP flow (dự phòng)** — user nhập mã 6 số thủ công khi không bấm được link (ví dụ mở email trên máy khác).

⚠️ **Điều kiện để deep link hoạt động**: backend phải có `assetlinks.json` với đúng SHA-256 fingerprint của app — nếu thiếu, Android không verify được domain và link sẽ mở browser thay vì app.

---

## 4. OTP 6-box Input Pattern

`OtpVerificationViewModel` + `OtpVerificationScreen`: 6 ô nhập riêng biệt, auto-focus ô tiếp theo khi nhập, xử lý backspace lùi về ô trước, hỗ trợ paste toàn bộ mã vào 1 ô đầu.

**Navigation flow:** `ForgotPassword → OtpVerification(email) → ResetPassword(email, otp)`.

---

## 5. Snackbar Repositioning

Toàn bộ snackbar trong 4 màn auth (Login/Register/ForgotPassword/ResetPassword) chuyển lên **top, dưới status bar** — tránh bị bàn phím che khi form đang mở (vấn đề UX rất dễ gặp với form nhiều field + bàn phím luôn mở).

---

## ⚠️ Common pitfalls đã gặp thật

- **File corruption khi refactor lớn**: `HomeViewModel`/tương tự có thể bị duplicate code nếu edit tool áp patch sai vị trí — luôn đọc lại file sau khi refactor lớn để chắc chắn không có đoạn code lặp.
- Quên xoá placeholder screen sau khi feature thật ra đời → routes rác tồn đọng trong navigation graph.

---

**Ngày hoàn thành:** 15/8/2026 · **Trạng thái:** Complete (còn 2 action item nhỏ theo dõi ở mục 2)
