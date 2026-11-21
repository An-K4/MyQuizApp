# N20.5 Knowledge — Discover, public-only Retrofit và mixed-case pagination

> Hoàn thành 13/9/2026. Phạm vi Android-only; backend không thay đổi.

## 1. Optional auth không đồng nghĩa public-only

`GET /quizzes/search` dùng optional auth. Khi request mang cookie, backend cho caller thấy public quiz cộng với private/empty quiz do chính caller sở hữu. Đây là behavior đúng cho search dùng chung, nhưng sai với màn Khám phá công khai.

Giải pháp là một network boundary riêng:

- clone `OkHttpClient` chuẩn để giữ timeout/logging;
- thay `CookieJar.NO_COOKIES`;
- thay `Authenticator.NONE`;
- build `@PublicApiRetrofit` dùng lại Json, converter, `ResultCallAdapterFactory` và `BASE_URL`;
- inject `@PublicQuizApiService` chỉ cho public browse.

Không tái dùng `@PreserveCaseRetrofit`: nó vẫn dùng authenticated client. Không tái dùng `@RawUploadOkHttpClient`: client đó thuộc third-party S3 upload, timeout/logging và mục đích khác.

## 2. Mixed naming trong cùng response

Quiz DTO dùng snake_case, nhưng pagination meta do controller tự dựng lại dùng camelCase:

```json
{
  "meta": {
    "pagination": {
      "limit": 12,
      "nextCursor": "...",
      "hasMore": true
    }
  }
}
```

App dùng `JsonNamingStrategy.SnakeCase`, nên nếu DTO chỉ khai `nextCursor`/`hasMore`, parser tìm `next_cursor`/`has_more`; key thật bị bỏ qua vì `ignoreUnknownKeys=true`, rồi default trở thành `null/false`. Trang đầu vẫn parse và render bình thường nên bug biểu hiện như “chỉ có đúng page-size quiz”, không crash.

Fix phù hợp cho vài field lẻ trong envelope dùng chung:

```kotlin
@JsonNames("nextCursor") val nextCursor: String? = null
@JsonNames("hasMore") val hasMore: Boolean = false
```

Luôn test bằng chính production Json (`NetworkModule.provideJson()`), không tạo Json đơn giản riêng trong test vì sẽ che mất naming strategy.

## 3. Query và UX

- TopBar luôn là “Khám phá”; title section không điều khiển behavior.
- Route truyền `sectionType/title/topic` chỉ để resolve query đầu.
- Tất cả/Chơi nhiều nhất/Xu hướng là quick filters ngoài.
- Sort phụ và topic nằm trong menu.
- Category mặc định chạy Trending qua `/feed?topic=...` để khớp Home; sort khác chạy public-only `/search?category=...&sort=...`.
- `General` (“Tổng hợp”) là category thật; “Tất cả” là không gửi category.
- Backend chưa có taxonomy endpoint. Sáu topic mặc định là taxonomy trình bày tương thích frontend; topic lạ do Home truyền vào vẫn phải append động.

## 4. Paging

Discover dùng page-size 12, initial load 12, prefetch distance 3 và backend cap 24. Page-size 3 phù hợp list quản lý nhỏ nhưng quá ít cho màn browse; khi pagination meta hỏng, nó còn làm triệu chứng dừng trang đầu lộ rõ hơn.

## 5. Mock data không phải production data

Frontend có thể render hàng trăm quiz mock, nhưng Android đang gọi backend thật. Mock chỉ chứng minh UI frontend chịu được danh sách lớn; không chứng minh production database có cùng số row. Khi kiểm tra pagination, phân biệt rõ:

1. số item trang đầu;
2. `meta.pagination.hasMore`;
3. `nextCursor`;
4. request append có thật sự được gửi;
5. số row thực tế đủ điều kiện public/non-deleted/question_count > 0.
