# N17 Knowledge — Create Room REST, typed config boundary và mode-specific UI

> Hoàn thành 28/8/2026. Phạm vi: lấy game modes, tạo session, lấy host token và hand-off sang HostLobby placeholder. Chưa triển khai Socket.IO hoặc lobby realtime.

## 1. Contract backend thật

Luôn đọc `game.controller.ts`, `game.service.ts`, `game.schema.ts`, `game.type.ts` và `engine/config.rule.ts` trước khi code.

- `GET /v1/games/game-modes` → outer envelope `data.gameModes`.
- `POST /v1/games` body: `quiz_id`, `session_name`, `mode`, `config`.
- Create response bị lồng: outer envelope → `data.data.session`; `ignored` nằm cạnh inner `data`.
- `POST /v1/games/:id/host-token` → `data.hostToken.socketToken`.
- Nested `GameConfig` dùng camelCase (`totalMatchSeconds`, `maxPlayers`...), trong khi outer create request dùng snake_case.

## 2. Quyết định serialization

Không tạo `GameJson`/`GameRetrofit` riêng. Storage, Password Reset và Games cùng dùng shared `@PreserveCaseJson`/`@PreserveCaseRetrofit` vì đều cần giữ nguyên tên property. Retrofit SnakeCase mặc định vẫn phục vụ phần backend còn lại; `@RawUploadOkHttpClient` vẫn tách riêng vì khác behavior bảo mật.

Test wire format phải xác nhận đồng thời:

- Có `quiz_id` và `session_name` nhờ `@SerialName` explicit.
- Nested config vẫn là `totalMatchSeconds`, không biến thành `total_match_seconds`.

## 3. Boundary sạch cho config động

Thiết kế đầu tiên đưa `Map<String, JsonElement>` từ DTO lên tận UiState/Intent/Composable. Cách này vẫn là one-way MVI nhưng làm presentation phụ thuộc representation của wire protocol.

Thiết kế cuối:

```text
HTTP JSON/dotted path
→ Game DTO + mapper (core:network)
→ GameConfigKey/GameConfigValue/GameConfigConstraint (core:common)
→ RoomConfigForm typed (feature presentation)
→ typed Intent
→ typed baseline diff
→ network mapper dựng nested JSON khi gửi request
```

Quy tắc: `JsonElement`, `JsonNull`, snake_case và dotted path không được vượt qua network boundary.

## 4. Dynamic contract và UI rõ nghĩa

Backend vẫn là nguồn sự thật cho:

- default value;
- editable/locked;
- min/max/nullable;
- enum options.

App là nguồn sự thật cho layout và wording. `GameModeConfigEditor` dùng `when(mode)` rồi gọi editor riêng cho Classic/Solo/Survival/Marathon/Practice. Các section chung được tái sử dụng, và pattern text + switch nằm ở `core:ui/components/SettingSwitchRow.kt`.

Không dùng `configLabel(path)` hoặc vòng lặp raw map để tự vẽ UI. Nếu backend thêm field mới, app cũ bỏ qua an toàn cho tới khi có layout/label được thiết kế rõ.

## 5. Conflict editable/locked

Backend hiện có thể trả `flow.allowAnswerLate` trong cả `editable` và `locked`. Sanitizer kiểm tra locked trước, nên client cũng phải áp dụng cùng precedence:

```text
locked wins editable
```

Nếu không, app sẽ hiển thị control chỉnh được nhưng server âm thầm đưa field vào `ignored`.

## 6. Partial success của create flow

Tạo phòng và cấp host token là hai request độc lập:

```text
create session thành công
→ lưu pendingSession/gameId/sessionCode
→ request host token
```

Nếu host-token lỗi:

- không gọi create session lần nữa;
- giữ pending session;
- nút retry chỉ gọi host-token;
- double-submit bị guard.

Đây là pattern cần tái sử dụng cho mọi flow nhiều bước mà bước đầu tạo resource có side effect.

## 7. Navigation tạm thời

Sau khi có token, app điều hướng `Route.HostLobby(gameId, socketToken, sessionCode)` và pop CreateRoom. N20 mới làm HostLobby thật; N17 dùng `HostLobbyPlaceholder` để xác nhận session code/Game ID và hand-off thành công mà không phụ thuộc socket.

## 8. Test tối thiểu cần giữ

- PreserveCase mixed snake/camel request.
- Typed key → nested JSON.
- Decode modes descriptor.
- `locked wins editable`.
- Decode `data.data.session`.
- Decode `hostToken.socketToken`.
- Baseline diff chỉ gửi field đã đổi.
- Mode switch reset form theo default của mode mới.
- Retry token không create room lần hai.
