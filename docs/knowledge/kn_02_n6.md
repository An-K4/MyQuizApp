# N6 Knowledge: Type-safe Navigation + Splash Auth Check

**Milestone:** M2 — Auth & Navigation (hoàn thành 12/8/2026)

---

## 📚 TỔNG QUAN

N6 giải quyết 2 việc: (1) thiết lập Navigation Compose type-safe với nhiều graph theo vai trò, (2) dùng Splash screen làm bước xác thực trạng thái đăng nhập đầu tiên. Đây cũng là lần đầu toàn bộ pipeline cookie jar + authenticator dựng ở Tuần 1 được chạy thật trên thiết bị, không còn chỉ là unit test.

---

## 1. Navigation Compose Type-safe

Dùng route dạng object/data class (`@Serializable`) thay cho String route thô — compiler bắt lỗi ngay nếu thiếu/sai kiểu argument, không phải crash runtime.

```kotlin
sealed interface Route {
    @Serializable object Splash : Route
    @Serializable object Login : Route
    @Serializable data class ResetPassword(val email: String, val token: String?) : Route
}

NavHost(navController, startDestination = Route.Splash) {
    composable<Route.Splash> { SplashScreen(...) }
    composable<Route.Login> { LoginScreenStateful(...) }
}
```

**3 graph ban đầu** (theo vai trò — role-aware): `AuthGraph` / `PlayerGraph` / `HostGraph`.

> ⚠️ Lưu ý: cấu trúc 3-graph này đã được refactor lại thành 1 `MainGraph` thống nhất ở giai đoạn sau (15/8) — xem `tuan2_bonus_knowledgement.md` để hiểu lý do và flow mới.

---

## 2. Splash Screen — xác thực qua API thật, không qua flag local

**Vì sao không lưu `isLoggedIn: Boolean` trong DataStore?**
- Cookie có thể hết hạn hoặc bị xoá ngoài ý muốn (user xoá app data, cookie bị revoke phía server...).
- Chỉ có server mới biết chắc session còn hợp lệ hay không — flag local dễ bị lệch với thực tế ("stale state").

**Flow chuẩn:**

```
Splash → GET /v1/users/me
    200 OK → Authenticated → điều hướng Home/Player
    401    → Unauthenticated → điều hướng Login (hoặc Guest nếu bật guest mode)
```

Đây là request đầu tiên chạy qua toàn bộ chain: `CookieJar.loadForRequest` → Retrofit → (nếu 401) `TokenAuthenticator` → `ResultCallAdapter` → `Result<User>`.

---

## 3. Vì sao đây là "bài test thật" đầu tiên

Trước N6, cookie jar + authenticator chỉ được xác nhận qua **MockWebServer trong unit test**. N6 là lần đầu chạy trên emulator/thiết bị thật, với UI thật gọi network thật — nơi phát hiện được các vấn đề mà mock không mô phỏng được (timing, cold start, cấu hình build variant debug/release).

---

## ⚠️ Common pitfalls

- Quên loading state trong lúc chờ `/users/me` → user thấy màn hình trắng vài trăm ms.
- Không tách case lỗi mạng (mất kết nối) khỏi case 401 thật — lỗi mạng không có nghĩa là chưa đăng nhập, không nên điều hướng thẳng về Login.

---

**Ngày hoàn thành:** 12/8/2026 · **Trạng thái:** Complete
