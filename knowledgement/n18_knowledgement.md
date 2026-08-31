# N18 Knowledge — Socket layer `/game`, event mapping và reconnect thật

> Hoàn thành 30/8/2026, đã build và test trên máy thật. Phạm vi: kết nối Socket.IO namespace `/game` bằng host token, map event thành domain model, HostLobby realtime và reconnect. Chưa làm Player lobby (N19) và chưa làm gameplay (N20+).

## 1. Contract socket thật (doc thiết kế sai ở điểm này)

Đọc `game.socket.ts`, `socket.middleware.ts`, `docs/socket.channels.ts`, `docs/components/socket.doc.ts` và `shared/errors/codes.ts` trước khi code.

- Namespace `/game`; handshake qua `socket.handshake.auth.token` (JWT ký bằng `SOCKET_JWT_SECRET`, payload `{psid, gsid, code, role}`).
- **Payload lỗi thật là `{ event, code }`** — doc ghi `{event, message}` kèm prefix `UNAUTHORIZED:`/`FORBIDDEN:`/`CONFLICT:`/`GONE:`. Sai. Chỉ có code.
- Lỗi đi qua event `error` **chỉ khi** client event không có ack. Event có ack (`question:answer`) trả lỗi trong ack: `{ error: { code } }`.
- `LobbyPlayer` qua socket dùng snake_case (`player_name`, `player_score`, `player_avatar`, `lives`, `status`), trong khi `config` lồng bên trong lại camelCase — cùng một payload trộn hai kiểu naming.
- 4 code fatal: `GAME_TOKEN_INVALID`, `GAME_TOKEN_WRONG_ROOM`, `GAME_ROOM_NOT_FOUND`, `GAME_PLAYER_NOT_FOUND`.

## 2. Quyết định: không thêm `AppError.Socket`

Kế hoạch gốc yêu cầu bỏ sung `AppError.Socket(event, message)`. Đã bác bỏ: từ N16.5, `AppError.Api(code)` đã map ~60 code sang tiếng Việt. Vì socket cũng chỉ trả code từ cùng bảng `shared/errors/codes.ts`, thêm nhánh mới chỉ tạo hai đường dịch song song rồi lệch nhau.

Quy tắc rút ra: **trước khi thêm nhánh error mới, kiểm tra xem transport mới có thật sự mang loại lỗi mới không, hay chỉ là cùng loại lỗi đi đường khác.**

## 3. Ranh giới module

```text
Socket.IO JSON (snake + camel trộn)
→ SocketDtos (@SerialName, @PreserveCaseJson) — core:network
→ GameEventMapper — core:network
→ GameEvent / LobbyState (domain thuần) — core:common
→ HostLobbyUiState — feature:lobby
```

`JSONObject`, raw JSON và tên event dạng chuỗi không được vượt qua biên network — cùng luật đã áp cho config ở N17.

`core:common` phải khai `api(libs.kotlinx.coroutines.android)`, không phải `implementation`, vì `Flow<GameEvent>` nằm trong signature công khai của interface.

## 4. Ba interface socket, tách theo vai trò

```text
GameSocketRepository         — events / joinLobby / disconnect
├─ HostGameSocketRepository   — startGame / nextQuestion / pauseGame / resumeGame / endGame
└─ PlayerGameSocketRepository — leaveLobby / submitAnswer / requestNextQuestion / sync
```

Interface ở `core:common`, impl ở `core:network` — khác với `project_structure.md` cũ (dự kiến đặt cả interface lẫn impl trong feature module). Đặt ở core vì `feature:lobby`, `feature:game-host` và `feature:game-player` dùng chung một connection, mà feature module thì không được phụ thuộc nhau.

Lợi ích thấy ngay: player không thể gọi `startGame` — sai vai trò thành lỗi biên dịch, không phải lỗi runtime từ server.

## 5. `callbackFlow` là khuôn đúng cho socket

```text
events(socketToken) = callbackFlow {
  tạo socket → đăng ký listener cho 19 server event + connect/disconnect/connect_error
  awaitClose { socket.off(); socket.disconnect() }
}
```

Không có `awaitClose` dọn listener thì mỗi lần reconnect sẽ để lại một socket cũ cùng bắn event vào UI — danh sách người chơi nháy qua lại giữa hai snapshot.

Mapper **không bao giờ được throw**: parse bằng `runCatching`, lỗi → `GameEvent.Failed(event, "CLIENT_PARSE_ERROR")`. Một exception trong `callbackFlow` giết cả flow, mất luôn mọi event sau đó.

## 6. Reconnect — bài học đắt nhất của N18

**socket.io tự reconnect nhưng KHÔNG tự join lại room.** Phải gọi `lobby:join` sau MỌI lần nhận `Connected`, không phải một lần trong `init`. Nếu không: mạng trở lại, socket báo "connected", nhưng không bao giờ nhận `lobby:updated` nữa. Bug im lặng, không lộ ra khi test ở mạng tốt — phải bật chế độ máy bay ~10 giây mới thấy.

Ba lý do disconnect phải xử lý khác nhau:

| Reason string | Nghĩa | Hành động |
| --- | --- | --- |
| `io server disconnect` | server đá | socket.io KHÔNG retry → thoát màn |
| `io client disconnect` | chính ta gọi | im lặng |
| còn lại (transport) | mất mạng | giữ dữ liệu cũ, chờ tự thử lại |

Token socket có TTL riêng, ngắn hơn cookie đăng nhập — app treo lâu ở background rồi quay lại sẽ nhận `GAME_TOKEN_INVALID` dù vẫn đăng nhập. Xử lý: `RefreshHostTokenUseCase` gọi lại endpoint host-token (idempotent, không tạo phòng mới) đúng MỘT lần, có cờ `tokenRefreshAttempted` chống vòng lặp refresh.

## 7. UiState phải phân biệt "chưa có dữ liệu" và "dữ liệu rỗng"

`players.isEmpty()` có hai nghĩa hoàn toàn khác: chưa nhận snapshot nào, hay đã nhận và phòng thật sự không có ai. Dùng `hasLobbySnapshot` để tách, nếu không sẽ hiện "chưa có ai vào phòng" ngay lúc đang kết nối.

Cùng lý do đó, khi mất kết nối thì **giữ nguyên danh sách cũ** và chỉ đổi banner trạng thái, không xóa về rỗng.

## 8. Test tối thiểu cần giữ

`GameEventMapperTest` dùng chính `NetworkModule.providePreserveCaseJson()` của production — tự tạo `Json` trong test sẽ cho test xanh trong khi app thật vẫn vỡ vì khác cấu hình naming.

- `lobby:updated` trộn snake_case + camelCase lồng nhau decode đúng (`maxPlayers` không bị biến thành `max_players` rồi rơi về default).
- Thiếu `config` → dùng `GameConfig()` mặc định, không crash.
- `error` giữ nguyên code backend.
- Payload rác và payload null → `CLIENT_PARSE_ERROR`, không throw.
- Event gameplay chưa dùng → `Unhandled`, không im lặng biến mất.

## 9. Nợ kỹ thuật để lại

- `onExit(message)` ở nav graph chỉ `popBackStack`, chưa hiển thị lý do bị buộc rời phòng → N19.
- `submitAnswer` chưa xử lý ack, cần đổi sang `suspendCancellableCoroutine` → N21+.
- `player_avatar` và `lives` chưa vào DTO/domain → N19.
- `lobby:config-update` (host sửa config trong lobby) chưa làm → N20.
