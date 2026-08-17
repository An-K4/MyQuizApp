# N1-N5 Knowledge: Gradle Multi-module + Clean Architecture Foundation

**Milestone:** M1 — Foundation (chốt 09/8/2026)

---

## 📚 TỔNG QUAN

Tuần 1 (N1–N5) là nền tảng của toàn bộ app: dựng 13 module Gradle, build-logic convention plugins, domain models thuần Kotlin, Room persistence, network layer Retrofit, và pattern Dependency Inversion cho cookie. Mọi milestone sau đều dựa trên các quyết định ở đây — nắm chắc phần này giúp đọc hiểu toàn bộ codebase nhanh hơn.

---

## 1. Gradle Multi-module + Version Catalog

- **13 module**: `:app` + 6 core (`network`, `database`, `datastore`, `ui`, `common`) + 7 feature.
- **Version catalog** (`gradle/libs.versions.toml`): tập trung version của mọi dependency ở 1 chỗ, tránh version lệch giữa module.
- **Convention plugins** (`build-logic/`): định nghĩa 3 plugin ID dùng chung — `myquizzapp.android.library`, `myquizzapp.android.compose`, `myquizzapp.android.hilt`. Mỗi module chỉ cần `apply` plugin thay vì khai báo lại toàn bộ config Android/Kotlin/Hilt.

**Hiệu quả thực tế:** file `build.gradle.kts` của 1 module giảm từ ~65 dòng xuống ~17 dòng.

```kotlin
// feature/auth/build.gradle.kts — SAU khi có convention plugin
plugins {
    alias(libs.plugins.myquizzapp.android.library)
    alias(libs.plugins.myquizzapp.android.compose)
    alias(libs.plugins.myquizzapp.android.hilt)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(libs.credentials)
}
```

---

## 2. Vì sao multi-module?

- Parallel build (Gradle build module độc lập song song).
- **Ép buộc chiều phụ thuộc đúng** — module con không compile được nếu cố ý phụ thuộc sai chiều (feature → feature bị chặn ngay ở Gradle, không phải review code mới phát hiện).
- Reuse code rõ ràng qua ranh giới module.

**Bảng phụ thuộc cốt lõi** (giữ nguyên xuyên suốt dự án):

| Module | CÓ THỂ phụ thuộc |
|---|---|
| `:app` | Tất cả |
| `:feature:*` | `core:network/database/datastore/ui/common` — KHÔNG BAO GIỜ feature khác |
| `:core:network`, `:core:database`, `:core:datastore`, `:core:ui` | `core:common` chỉ |
| `:core:common` | Không phụ thuộc gì |

---

## 3. `core:common` — Result + AppError + Domain Models

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
}

sealed class AppError {
    data class Http(val code: Int, val message: String, val details: String? = null) : AppError()
    data class Socket(val event: String, val message: String) : AppError()
    data class Network(val exception: Throwable) : AppError()
    object Unknown : AppError()
}
```

⚠️ **Ghi nhớ quan trọng**: `Result<T>` chỉ có **1 type param** — `AppError` luôn cố định bên trong `Error`. KHÔNG phải `Result<T, E>` generic 2 param như một số codebase khác hay dùng (đây là lỗi thật đã lặp lại ở N11, xem file `n11_knowledgement.md`).

**7 domain models** đọc từ backend thật: `User`, `Quiz`, `Question`, `GameConfig`, `GameSession`, `Player`, `QuizSummary`.

🚫 **Anti-cheat có chủ đích**: `Question` domain model KHÔNG có field `correct_answer` — server không bao giờ gửi đáp án đúng xuống client trong lúc chơi.

---

## 4. Dependency Inversion Pattern — CookieStore

**Vấn đề:** `core:network` cần lưu cookie, nhưng theo bảng phụ thuộc thì KHÔNG được phép phụ thuộc `core:database` (nơi có Room).

**Giải pháp (DIP):**

```kotlin
// core:common/cookie/CookieStore.kt — interface (abstraction)
interface CookieStore {
    suspend fun loadForHost(host: String): List<StoredCookie>
    suspend fun save(host: String, cookies: List<StoredCookie>)
    suspend fun clear()
}

// core:network/cookie/PersistentCookieJar.kt — chỉ biết interface
class PersistentCookieJar @Inject constructor(
    private val cookieStore: CookieStore
) : CookieJar { /* ... */ }

// core:database/cookie/RoomCookieStore.kt — implementation thật
class RoomCookieStore @Inject constructor(
    private val cookieDao: CookieDao
) : CookieStore { /* ... */ }

// core:database/di/DatabaseBindingModule.kt
@Module @InstallIn(SingletonComponent::class)
abstract class DatabaseBindingModule {
    @Binds abstract fun bindCookieStore(impl: RoomCookieStore): CookieStore
}
```

Hilt tự wire `RoomCookieStore` vào `CookieStore` khi build `:app` — `core:network` không cần biết Room tồn tại.

---

## 5. Cookie-based Auth: CookieJar + Authenticator

- **CookieJar** (`saveFromResponse`/`loadForRequest`): tự động lưu/gửi cookie theo từng request, khác hẳn thói quen JWT Bearer thủ công.
- **Authenticator ≠ Interceptor**: Authenticator được OkHttp gọi riêng khi nhận 401, dùng để refresh token rồi retry request — Interceptor không có cơ chế retry an toàn tương đương.
- **Guard chống retry vô hạn**: `responseCount >= 2` → không refresh nữa, trả lỗi luôn (tránh loop 401 → refresh → 401 → refresh...).
- **`dagger.Lazy<AuthApiService>`** (không phải `kotlin.Lazy`) trong `TokenAuthenticator` — phá vòng lặp DI vì `AuthApiService` cũng nằm trong `core:network`, cùng module với Authenticator cần nó.
- **Path guard**: bỏ qua refresh cho request tới `/auth/` — tránh gọi refresh vô ích khi lỗi 401 là do sai mật khẩu, không phải do token hết hạn.

---

## 6. Network Layer — ApiEnvelope + ResultCallAdapter

- `ApiEnvelope` khớp đúng response thật của backend: `{success, data, error, meta}`.
- `ResultCallAdapter`: tự động unwrap Retrofit call → `Result<T>` — ViewModel/UseCase không bao giờ thấy raw `HttpException`.
- `JsonNamingStrategy.SnakeCase` cấu hình **1 lần duy nhất** trong `NetworkModule` (backend trả JSON snake_case, Kotlin field camelCase — khỏi phải `@SerialName` từng field).
- `BASE_URL`/`SOCKET_URL` khai báo trong `core:network` (không phải `:app`) — BuildConfig sinh theo từng module cần, tránh rò rỉ config ra module không liên quan.

---

## 7. Bài học N5 (tránh lặp lại)

- **Room là nguồn sự thật duy nhất cho cookie** — không cache RAM song song. Một bug suýt lên prod: Splash gọi `/users/me` trước khi cache RAM warm xong → user bị hất về Login sai.
- **Không hardcode `"localhost"`** trong unit test — máy có Docker Desktop resolve ra host lạ (`kubernetes.docker.internal`). Luôn dùng `server.hostName` từ MockWebServer.
- **File test phải nằm đúng `src/test`** — đặt nhầm `src/main` thì `testImplementation` không nằm trên classpath, test không compile được mà lỗi rất khó hiểu.

---

## ✅ CHECKLIST HOÀN THÀNH

- [x] 13 module build xanh
- [x] CI (lint + `testDebugUnitTest` + `assembleDebug`) xanh — kể cả sau 1 lần phá build thử để test pipeline (đỏ → revert → xanh)
- [x] Cookie persistence + refresh: 2 test MockWebServer xanh local + CI

---

**Ngày hoàn thành:** 09/8/2026 · **Trạng thái:** M1 Complete
