# N7-N8 Knowledge: Login/Register + Stateful/Stateless Composable Pattern

**Milestone:** M2 — Auth E2E (hoàn thành 12/8/2026)

---

## 📚 TỔNG QUAN

N7-8 build `feature:auth` — Login + Register. Song song với business logic, đây cũng là lúc thiết lập pattern UI chuẩn cho **toàn dự án**: Stateful/Stateless Composable — pattern sẽ được lặp lại ở mọi feature sau này.

---

## 1. Stateful vs Stateless Composable

```kotlin
// Stateless — pure UI, preview-friendly, KHÔNG warning ViewModel trong @Preview
@Composable
fun LoginScreen(
    state: LoginState,
    onLoginClick: (email: String, password: String) -> Unit
) {
    // chỉ render UI theo state, gọi callback khi có sự kiện
}

// Stateful — wrapper khởi tạo ViewModel, nối state thật vào UI thuần
@Composable
fun LoginScreenStateful(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onLoginClick = { email, password ->
            viewModel.handleIntent(LoginIntent.Login(email, password))
        }
    )
}
```

**Vì sao dùng pattern này:**
- `@Preview` cho `LoginScreen` (stateless) chạy được ngay, không cần mock `ViewModel`/Hilt.
- Tách rõ "UI thuần" khỏi "quản lý state" → dễ test, dễ đổi nguồn state (ví dụ preview với state giả) mà không sửa UI.

---

## 2. Form Validation khớp schema backend

Validate email/password ở client theo đúng rule backend đã định nghĩa (zod/joi) — tránh trường hợp client cho pass nhưng backend trả lỗi 400 khó hiểu với user.

---

## 3. ⚠️ Nuance cookie auth: Register KHÔNG set cookie

- `POST /auth/register` chỉ trả `{ user }` — **không** set `accessToken`/`refreshToken` cookie như `login`/`one-tap`.
- **Hệ quả code:** sau khi register thành công, KHÔNG thể coi user đã đăng nhập. Phải tự động gọi `login` tiếp theo, hoặc điều hướng user về màn Login để họ đăng nhập lại.

---

## 4. Hệ thống UI nền tảng dựng cùng lúc (13/8)

| Thành phần | Vị trí | Mục đích |
|---|---|---|
| `Color.kt` / `Theme.kt` / `Type.kt` | `core:ui/theme/` | Material3 light/dark, màu brand `#7B61FF` |
| `AppTextStyles.kt` | `core:ui/style/` | Typography tập trung — titleLarge, bodyMedium, buttonText, linkText, caption |
| TextField chuẩn hoá | dùng chung mọi form | `OutlinedTextFieldDefaults.colors()` — config nhất quán (focusedTextColor, cursorColor, focusedBorderColor...) |
| Material Icons Extended | convention plugin `myquizzapp.android.compose` | Mọi module Compose tự động có icon set đầy đủ — DRY qua **build config**, không phải qua code chia sẻ |

---

## 5. ⚠️ Gỡ thói quen cũ (từ Clover Chatty)

Dự án cũ dùng JWT Bearer + `AuthInterceptor` gắn header `Authorization`. Backend MyQuizz **KHÔNG đọc** header này — mọi auth đi qua cookie HttpOnly. Đừng viết lại `AuthInterceptor` kiểu cũ theo phản xạ.

---

**Ngày hoàn thành:** 12/8/2026 · **Trạng thái:** Complete
