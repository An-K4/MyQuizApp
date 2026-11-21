# N15 Knowledge: Upload ảnh presign S3 2 bước — và bài học lớn về kotlinx.serialization

**Milestone:** M3 — Quiz CRUD (đang chạy) · N15 hoàn thành 24/8/2026 · Có bug thật phát sinh + fix ngay trong ngày, nên tài liệu này chi tiết hơn các bản trước.

---

## 📚 TỔNG QUAN

N15 hoàn thiện việc upload ảnh cover cho quiz theo đúng cơ chế backend đã thiết kế: **S3 presign 2 bước** — app xin backend 1 URL PUT tạm thời (presigned URL) cho S3/bên lưu trữ thứ 3, rồi tự PUT thẳng bytes ảnh lên URL đó, **không đi qua backend**. Đây là lần đầu dự án cần 2 luồng network khác bản chất trong cùng 1 tính năng: (1) gọi backend thật (có cookie, JSON envelope, `ResultCallAdapter`) và (2) gọi thẳng 1 URL bên thứ 3 (không cookie, không envelope, chỉ PUT bytes thô).

Sau khi implement xong và tưởng đã hoàn tất, **test trên máy thật phát hiện bug 400 VALIDATION_ERROR** — hóa ra không phải bug backend mà là bug client sâu trong tầng serialization, dẫn tới bài học quan trọng nhất của N15 (mục 4).

---

## 1. Domain & Data layer

- **Domain** (`core:common`): `PresignResult` (uploadUrl, publicUrl, key) + interface `StorageRepository`:
  - `presignUpload(contentType, folder, fileSize): Result<PresignResult>` — hỏi backend xin URL PUT tạm.
  - `uploadBytes(uploadUrl, contentType, bytes): Result<Unit>` — tự PUT bytes lên URL đó.
- **Data** (`core:network`):
  - `StorageApiService` — chỉ 1 endpoint `POST /storage/presign`, đi qua Retrofit + `ResultCallAdapter` như mọi API khác của backend.
  - `StorageDtos.kt` — `PresignUploadRequestDto(contentType, folder, fileSize)`, `PresignResponseDto(presignedUrl: PresignResultDto)`, `PresignResultDto(uploadUrl, publicUrl, key)` + `toDomain()`.
  - `StorageRepositoryImpl` — gọi `StorageApiService.presignUpload(...)` để lấy `PresignResult`, sau đó **tự tay dùng `OkHttpClient.newCall(Request).execute()`** để PUT bytes ảnh lên `uploadUrl` — cố tình **không** dùng Retrofit cho bước này vì URL đích là S3/bên thứ 3, không phải backend, và không cần JSON envelope.
- **Backend contract đã xác nhận** (đọc `storage.schema.ts`/`storage.controller.ts` thật, không suy đoán từ design doc): `POST /v1/storage/presign` nhận `{ contentType, folder, fileSize }` (camelCase), trả `{ presignedUrl: { uploadUrl, publicUrl, key } }`; `ALLOWED_FOLDERS = ['avatars', 'quizzes', 'questions', 'uploads']`; `MAX_FILE_SIZE = 2 * 1024 * 1024` (2MB).

---

## 2. DI: vì sao cần `OkHttpClient` riêng cho bước PUT ảnh

`@RawUploadOkHttpClient` — một `OkHttpClient` mới, **không** có `cookieJar`/`authenticator` như client chuẩn của app. Lý do: client chuẩn tự động gắn cookie phiên đăng nhập (`PersistentCookieJar`) vào **mọi** request nó thực hiện — nếu dùng client đó để PUT lên URL S3 presigned, app sẽ vô tình gửi cookie nội bộ của `api.myquizz.dpdns.org` sang domain S3, sai về bảo mật và không cần thiết (URL presigned tự nó đã mang quyền truy cập qua query signature). `UploadImageUseCase` (tầng feature) điều phối toàn bộ luồng: nén ảnh → `presignUpload` → PUT bytes qua client riêng này → trả `publicUrl` để gán vào `NewQuiz`.

📌 **Quy tắc chung rút ra**: bất cứ khi nào app gọi thẳng một URL không phải backend của mình (presigned URL, CDN, webhook bên thứ 3...), phải dùng `OkHttpClient` riêng không mang theo cookie/token nội bộ — không tái dùng client đã cấu hình cho backend.

---

## 3. Nén ảnh trước khi upload

`ImageCompressor.kt` — resize + nén JPEG ảnh cover trước khi gửi lên S3, giảm dung lượng truyền tải và tránh vượt `MAX_FILE_SIZE` 2MB của backend. Quyết định phạm vi đã khảo sát người dùng trước khi code: **nén = có** (làm ở N15), **avatar upload = chưa** (để dịp khác — dùng lại được cùng cơ chế presign này khi cần).

`CreateQuizViewModel.kt` được viết lại hoàn toàn ở N15 (bản trước bị lỗi cắt cụt file khi ghi), và `CreateQuizScreen.kt` thêm UI chọn ảnh cover qua `rememberLauncherForActivityResult` (cần thêm dependency `activity-compose` — `libs.androidx.activity` 1.13.0 — vào `feature/quiz-manage/build.gradle.kts`, module này trước đó chưa cần tới Activity APIs).

---

## 4. 🔴 BÀI HỌC LỚN: `JsonNamingStrategy` vẫn đổi tên field dù đã có `@SerialName` tường minh

### 4.1. Hiện tượng

Sau khi code xong và tưởng đã xong N15, test luồng "Tạo quiz có ảnh cover" trên máy thật thì luôn nhận **400 VALIDATION_ERROR** từ `POST /storage/presign`. Logcat (OkHttp interceptor BODY level) cho thấy body thực tế gửi đi là:

```json
{"content_type":"image/jpeg","folder":"quizzes","file_size":62542}
```

— toàn bộ **snake_case** — trong khi `PresignUploadRequestDto` đã khai rõ:

```kotlin
data class PresignUploadRequestDto(
    @SerialName("contentType") val contentType: String,
    val folder: String,
    @SerialName("fileSize") val fileSize: Long,
)
```

### 4.2. Vì sao dễ nghĩ đây là lỗi backend

- Response trả về đúng format lỗi chuẩn của backend (`{success:false, data:null, error:{code:"VALIDATION_ERROR"}, meta:{...}}`), trông rất giống 1 lỗi nghiệp vụ phía server.
- Trước đó, dự án đã có tiền lệ thật là **backend sai/khác doc** (đối chiếu mục backend ở file kế hoạch chính — envelope lồng `{quiz:...}`, `{session:...}`), nên phản xạ đầu tiên hợp lý là nghi backend tiếp.
- Nhưng lần này khi đối chiếu kỹ `storage.schema.ts`/`storage.controller.ts` thật của backend, xác nhận schema là camelCase — khớp đúng những gì `frontend/src/api/storage.api.js` (web) đang gửi thành công. Vậy vấn đề chắc chắn ở phía Android.

### 4.3. Nguyên nhân thật (root cause)

Dự án cấu hình 1 `Json` dùng chung toàn app trong `NetworkModule.provideJson()`:

```kotlin
fun provideJson(): Json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
```

Giả định ban đầu (ghi từ N1, mục "Đã chốt" của file kế hoạch): dùng `namingStrategy = SnakeCase` một lần cho toàn app để tự động map `quizName` (Kotlin) ↔ `quiz_name` (JSON backend thật — hầu hết field backend dùng snake_case), và nghĩ rằng khi cần field nào khác quy tắc (như 3 field của storage — backend camelCase) thì chỉ cần khai `@SerialName("...")` tường minh để "thoát" quy tắc chung.

**Giả định đó SAI.** Theo cách `kotlinx.serialization` hoạt động: `JsonNamingStrategy` được áp dụng lên **tên serial đã resolve** của property — và tên serial đã resolve **chính là giá trị trong `@SerialName`** khi có khai báo đó (thay cho tên property Kotlin). Nghĩa là `JsonNamingStrategy.SnakeCase` không biết và không quan tâm việc tên đó đến từ đâu (tên property gốc hay từ `@SerialName`) — nó cứ áp transform snake_case lên bất kỳ chuỗi tên serial nào nó nhận được. Vì vậy `@SerialName("contentType")` vẫn bị biến tiếp thành `content_type` trên wire.

**Bằng chứng thực nghiệm rất rõ** trong log: field `folder` (1 từ, không có ranh giới camelCase) giữ nguyên `"folder"`, còn `contentType`/`fileSize` (có ranh giới từ) bị đổi thành `content_type`/`file_size` — đúng chính xác hành vi biến đổi của `SnakeCase` strategy, không phải lỗi map nhầm tên hay lỗi ngẫu nhiên nào khác.

### 4.4. Fix đã áp dụng

Không thể chỉ dựa vào `@SerialName` để một DTO "thoát" `namingStrategy` chung khi dùng chung 1 `Json`. Cách đúng: **tách hẳn 1 cặp `Json`/`Retrofit` riêng, không set `namingStrategy`**, chỉ dùng riêng cho `StorageApiService` (vì 3 field của schema storage đã đúng camelCase 100%, không cần transform gì cả):

```kotlin
// Qualifiers.kt
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class StorageJson
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class StorageRetrofit

// NetworkModule.kt
@StorageJson @Provides @Singleton
fun provideStorageJson(): Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    // KHÔNG set namingStrategy — đây là điểm khác biệt duy nhất và mấu chốt
}

@StorageRetrofit @Provides @Singleton
fun provideStorageRetrofit(@StorageJson json: Json, okHttpClient: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)                    // dùng lại OkHttpClient chuẩn (có cookie) — vẫn gọi backend thật
        .addCallAdapterFactory(ResultCallAdapterFactory(json))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

@Provides @Singleton
fun provideStorageApiService(@StorageRetrofit retrofit: Retrofit): StorageApiService = retrofit.create()
```

Lưu ý: `ResultCallAdapterFactory` sẵn đã nhận `Json` qua constructor, và `ApiEnvelope`/`ApiErrorBody`/`Meta` đều generic, không phụ thuộc `namingStrategy` — nên việc thêm 1 cặp `Json`/`Retrofit` thứ hai chỉ là thêm mới (additive), không phải sửa lại toàn bộ tầng network, không ảnh hưởng các API service khác (`AuthApiService`, `UserApiService`, `QuizApiService` vẫn dùng `Retrofit`/`Json` mặc định như cũ).

Sau fix, request thực tế gửi đúng `{"contentType":"image/jpeg","folder":"quizzes","fileSize":62542}` — khớp `presignUploadSchema` của backend, test lại trên máy thật chạy đúng luồng end-to-end (presign → PUT ảnh → tạo quiz với `coverImageUrl`).

### 4.5. Bài học tổng quát (áp dụng cho các endpoint mới sau này)

1. **`@SerialName` không phải "vé thoát" khỏi `JsonNamingStrategy` dùng chung.** Nếu một endpoint có field không tuân theo quy tắc naming chung của app (ví dụ backend cố tình dùng camelCase cho 1 module riêng), giải pháp đúng là **`Json`/`Retrofit` riêng không set `namingStrategy`** cho service đó — không phải rải `@SerialName` lên từng field.
2. **Luôn xác minh byte thật trên wire, không chỉ đọc code DTO.** Bug này chỉ lộ ra rõ ràng khi nhìn log OkHttp ở mức `BODY` — nếu chỉ đọc lại `PresignUploadRequestDto` sẽ thấy "đúng rồi, đã có `@SerialName`" và bỏ qua được vấn đề thật.
3. **400 VALIDATION_ERROR không mặc định nghĩa là bug backend** — nhưng cũng không mặc định nghĩa là bug client. Phải đối chiếu **cả 2 phía**: đọc schema backend thật (không suy đoán) VÀ đọc chính xác byte request client gửi, rồi so khớp field-by-field.
4. Khi thêm 1 `ApiService` mới gọi 1 nhóm endpoint có naming convention khác với phần còn lại của backend, cân nhắc tách `Json`/`Retrofit` riêng **ngay từ đầu** thay vì chỉ dùng `@SerialName` — tránh lặp lại đúng bug này ở module tiếp theo.

---

## 5. Kiến trúc: luồng upload ảnh hoàn chỉnh

```
CreateQuizViewModel (chọn ảnh từ picker)
  → ImageCompressor.compress(uri) → ByteArray đã nén
  → UploadImageUseCase(contentType, folder="quizzes", fileSize, bytes)
       → StorageRepository.presignUpload(contentType, folder, fileSize)
            → StorageApiService (Retrofit riêng, @StorageJson không namingStrategy)
            → backend POST /v1/storage/presign → { presignedUrl: { uploadUrl, publicUrl, key } }
       → StorageRepository.uploadBytes(uploadUrl, contentType, bytes)
            → OkHttpClient riêng (@RawUploadOkHttpClient, không cookie) → PUT thẳng lên S3
       → trả publicUrl
  → CreateQuizViewModel gán publicUrl vào NewQuiz.coverImageUrl → CreateQuizUseCase (như N13-14)
```

---

## ✅ CHECKLIST HOÀN THÀNH

- [x] Domain: `PresignResult`, `StorageRepository`
- [x] Data: `StorageApiService`, `StorageDtos`, `StorageRepositoryImpl`
- [x] DI: `@RawUploadOkHttpClient` (PUT ảnh không cookie), `@StorageJson`/`@StorageRetrofit` (fix bug naming strategy)
- [x] `ImageCompressor` (resize + nén JPEG)
- [x] `UploadImageUseCase` điều phối presign → PUT → publicUrl
- [x] `CreateQuizViewModel`/`CreateQuizScreen` — UI chọn ảnh cover, viết lại sau bug file bị cắt cụt
- [x] Bug 400 VALIDATION_ERROR (client gửi snake_case) — root-caused và fix bằng Json/Retrofit riêng cho Storage
- [x] Test lại trên máy thật — luồng tạo quiz có ảnh cover chạy đúng end-to-end
- [ ] Avatar upload (để dịp khác — dùng lại được `UploadImageUseCase`/`StorageRepository` sẵn có, chỉ đổi `folder="avatars"`)

---

**Ngày hoàn thành:** 24/8/2026 · **Trạng thái:** N15 Complete, sẵn sàng N16 (sửa/xóa quiz — chốt M3)
