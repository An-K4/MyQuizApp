# N19 Knowledge — Player lobby: tra phòng, join REST, danh tính khách và cái bẫy `data.session.session`

> Hoàn thành 5/9/2026, đã build và test trên máy thật. Phạm vi: tra phòng bằng mã, join REST lấy `socketToken`, màn nhập nickname cho khách, PlayerLobby realtime, trả nợ "lý do bị buộc rời phòng" của N18. Chưa làm Bottom Navigation thật (N19.5) và gameplay (N21+).

## 1. Bug lớn nhất: `GET /games/{code}` lồng ba cấp

App crash ngay khi tra phòng, lỗi thật trên máy:

```text
Fields [id, quiz_snapshot_id, session_name, session_code, session_host,
total_players, total_questions, session_status, game_mode,
current_question_index, current_phase] are required for type with serial name
'...core.network.dto.GameSessionDto', but they were missing at path: $.data.session
```

Chuỗi suy luận từng bước:

1. `ResultCallAdapterFactory` deserialize cả `ApiEnvelope<T>` với `T` = DTO khai trong `Result<T>`, nên `$.data` chính là gốc DTO và `$.data.session` là field `session` của nó. Wiring đúng, không phải lỗi ở đây.
2. Controller đọc rất thuyết phục:

```ts
const session = await gameService.getLobby(code)
return success(res, { session })
```

3. Nhưng `getLobby` trả **`{ session, players, config }`**. Cả cụm đó bị gán vào biến *tên là* `session`, nên JSON thật trên dây là:

```json
{ "data": { "session": { "session": { ... }, "players": [ ... ], "config": { ... } } } }
```

4. Bằng chứng chọt: lỗi liệt kê thiếu **11/13** field của `GameSessionDto` nhưng **không** báo thiếu `config` — vì cấp ngoài có sẵn key `config`. Nếu object thực sự rỗng thì `config` cũng phải nằm trong danh sách thiếu.
5. Không phải lỗi naming strategy: `game.cache.ts` lưu row vẫn snake_case bằng `JSON.stringify` nguyên bản, nên đường Redis và Postgres cho ra cùng shape.

**Fix phía app** (vì app phải chạy được với server đang deploy):

```kotlin
@Serializable
data class RoomLookupResponseDto(val session: LobbySnapshotDto)

@Serializable
data class LobbySnapshotDto(
    val session: GameSessionDto,
    val players: List<LobbyPlayerDto> = emptyList()
)
```

KDoc tại chỗ phải ghi rõ: đây là bug backend, khi `getGameByCode` sửa thành `success(res, lobby)` thì chỉ cần đổi Retrofit trỏ sang `LobbySnapshotDto`. Kèm test hồi quy khóa payload thật — để khi backend sửa thì **test đỏ trước**, chứ không phải user gặp crash trước.

⚠️ Chưa đề xuất sửa backend ngay vì frontend web đang đọc `unwrap(res.data).session` và sống dựa vào shape lệch này. Sửa là breaking change cho cả hai client, phải đổi đồng thời.

## 2. Hai quy tắc rút ra từ bug trên

- **Đừng tin tên key trong `success(res, { x })` của controller** — phải đọc hàm service trả về cái gì. Bài học N12 là "đọc controller thật thay vì Swagger"; N19 đẩy thêm một tầng: đọc cả service.
- **Lỗi kotlinx.serialization là manh mối định vị, không phải chỉ stacktrace**: `at path:` cho biết chính xác điểm lệch; và field nào **không** bị báo thiếu lại là thông tin giá trị nhất — đó là field thật sự tồn tại trong JSON, đủ để đoán ra shape thật của cấp đang đứng.

## 3. Contract REST của lobby (đã xác minh bằng code)

| Endpoint | Auth | Payload thật |
|---|---|---|
| `GET /games/:code` | public | `data.session.session` (+ `players`, `config`) — lồng ba cấp, xem mục 1 |
| `POST /games/:code/join` | optionalAuth | `data.player` (full row `player_sessions`) + `data.socketToken` **phẳng** |
| `POST /games/:id/host-token` | auth (host) | `data.hostToken.socketToken` — lồng một cấp, **khác** join |

Ba endpoint cùng họ nhưng ba mức lồng khác nhau — không có quy ước chung nào để suy ra, phải đọc từng cái.

Thứ tự guard của `joinGame` (quan trọng khi hiển thị lỗi cho user — lỗi nào đến trước sẽ che lỗi sau):

1. 404 `GAME_ROOM_NOT_FOUND`
2. 409 `GAME_ALREADY_STARTED` (không ở lobby và `allowLateJoin` = false)
3. 403 `GAME_GUESTS_NOT_ALLOWED` (không có `player_id` và `allowGuests` = false)
4. 403 `GAME_HOST_CANNOT_JOIN`
5. 409 `GAME_ROOM_FULL`

**Không có endpoint refresh socket token cho player** (host thì có `POST /:id/host-token`). Do đó `GAME_TOKEN_INVALID` của player là fatal: pop về màn nhập mã và join lại từ đầu, không thể bắt chước cơ chế refresh-một-lần của HostLobby ở N18.

## 4. `total_players` không đáng tin trong lobby

Cột `total_players` trên `game_sessions` chỉ được flush khi ván kết thúc, **không tăng lúc player join**. Nếu dùng nó để kiểm tra "phòng đã đầy" thì gần như luôn sai (thường bằng 0). Payload tra phòng may mắn có sẵn `players`, nên `RoomLookup.totalPlayers` đếm `players.size`.

Quy tắc chung: **ưu tiên đếm từ danh sách thật thay vì tin cột tổng hợp**, trừ khi đã xác minh cột đó được cập nhật đồng bộ.

## 5. Danh tính khách và luồng nickname

- **Cho khách vào hay không là quyết định của host**, nằm ở `config.lobby.allowGuests` của từng phòng. Client không hardcode chính sách, chỉ đọc cấu hình để hiển đúng lý do khi bị chặn.
- **Đã đăng nhập → không có ô nhập tên**: vào thẳng lobby, body join là `{}` rỗng, server tự lấy danh tính từ cookie. Thêm ô nickname cho người đã đăng nhập vừa dư vừa gây hiểu nhầm là tên sẽ đổi.
- **Khách**: UUID sinh **lần đầu cần join** rồi lưu DataStore (`GuestIdentityStore`), không sinh lúc mở app và không sinh mới mỗi lần join — backend dùng `player_guest_id` (bắt buộc đúng định dạng UUID) để nhận ra cùng một khách khi reconnect và khi xem lịch sử qua header `x-guest-id`.

## 6. Nav result pattern (trả nợ N18)

N18 để lại `onExit(message)` chỉ `popBackStack`, mất lý do bị buộc rời phòng. Cách đã chọn:

- `GameNavGraph` có `KEY_LOBBY_EXIT_MESSAGE` + helper `popWithMessage`: ghi message vào `savedStateHandle` của **back stack entry đích** rồi mới pop.
- Màn đích (JoinRoom) đọc-và-xóa message, đẩy vào state → snackbar, kèm callback `onExitMessageShown` để không hiện lại sau recomposition/rotate.

Quy ước từ nay: mọi "kết quả trả về khi pop" đi qua `savedStateHandle` của entry đích — không nhồi vào route argument (route là đầu vào, không phải đầu ra) và không dùng ViewModel chia sẻ giữa hai màn.

## 7. Dùng lại `LobbyPlayerDto` cho cả REST và socket

Backend trả hai biến thể cho cùng khái niệm người chơi trong lobby: bản `Pick<PlayerSessionRow, 'id'|'player_name'|'player_score'|'status'>` khi đọc Postgres, và full row khi Redis còn nóng. Vì mọi field ngoài `id`/`player_name` đều có default trong DTO, một `LobbyPlayerDto` duy nhất parse được cả hai — không cần DTO riêng cho REST.

Vẫn phải decode bằng `@PreserveCaseJson` (cái bẫy từ N15/N18): `JsonNamingStrategy.SnakeCase` sẽ biến đổi cả giá trị đã khai tường minh trong `@SerialName`.

## 8. Bài học về môi trường làm việc (không liên quan code)

- **Tài liệu chỉ nằm trên nhánh `docs`**, code trên `main`. Khi đang ở `main` thì `AGENTS.md`, các file `.md` gốc và `knowledgement/` **không tồn tại trên đĩa** — phải đợi checkout sang `docs` mới cập nhật được, đừng kết luận "file bị xóa".
- `write_file` **không tự tạo thư mục cha** — phải `create_directory` từng cấp trước.
- `edit_file` là **atomic**: một `oldText` sai là toàn bộ edit bị hủy. Luôn đọc lại file trước khi sửa và dùng chuỗi **đơn dòng, duy nhất**; đừng gõ lại đoạn nhiều dòng theo trí nhớ.
- `findstr` cần **đường dẫn tuyệt đối** (tham số `cwd` không đáng tin); pipe/redirect và `more` bị chặn bởi allowlist.
- `directory_tree` chỉ gọi ở mức package, gọi ở mức module sẽ vượt giới hạn 5MB.

## 9. Nợ để lại sau N19

- `players` lấy sẵn ở bước tra phòng chưa đổ vào state đầu của PlayerLobby (socket `lobby:updated` fill ngay sau đó nên chưa cần thiết).
- Nút "Vào phòng" ở Home vẫn là `TextButton` tạm — chờ N19.5 làm Bottom Navigation thật.
- Đề xuất sửa `getGameByCode` phía backend (mục 1) — cần phụ thuộc đổi cùng frontend web.
- 2 file usecase stub từ N16.5 vẫn chờ xóa tay trong IDE.
