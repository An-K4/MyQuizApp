# N20 — Host Lobby thật: sửa cấu hình phòng, chia sẻ mã, bắt đầu trận

> Hoàn thành 10/9/2026. Phạm vi: `HostLobbyScreen` thật (thay phần lobby còn thiếu của N18), `lobby:config-update` có ACK, hai nút copy mã/link chia sẻ, nút "Bắt đầu" neo vào `game:started`, hand-off sang `HostGamePlaceholder` của N21.

---

## 1. Đọc backend trước khi code cứu được 4 lần phải viết lại

Trước khi viết dòng nào, tôi đối chiếu `config.rule.ts`, `game.socket.ts`, `game.service.ts` với giả định trong kế hoạch. Bốn điểm lệch tìm được — nếu code trước rồi mới đọc thì cả bốn đều là bug phải test thủ công mới lộ:

1. **`normalizeConfig` ghi đè giá trị host gửi lên.** Server không chỉ nhận patch mà còn tự sửa: `perQuestionSeconds === 0` ⇒ tắt `speedBonus`; self-paced + `showLeaderboard = between_questions` ⇒ hạ xuống `end_only`; `reviewMode = true` ⇒ bật cưỡng bức `showCorrectAnswer`. Hệ quả kiến trúc: **không được coi state cục bộ là sự thật sau khi lưu**. Form phải được dựng lại từ `ack.config` mà server trả về, không phải từ giá trị người dùng vừa gõ.
2. **`game:start` không có ACK.** Chỉ `lobby:config-update` và `question:answer` có ACK; các lệnh điều khiển trận là fire-and-forget. Nên "đã bắt đầu thành công" phải neo vào event broadcast `game:started`, kèm timeout tự thoát trạng thái chờ (chọn 5s, bằng `ACK_TIMEOUT_MS`).
3. **`changed` của server là so sánh toàn bộ object**, không phải "có field nào được áp": `JSON.stringify(config) !== JSON.stringify(session.config)`. Vì `normalizeConfig` có thể trả về đúng config cũ, `changed = false` **không** đồng nghĩa lệnh bị từ chối — nó nghĩa là kết quả cuối trùng trạng thái cũ.
4. **Chặn sửa config sau khi trận bắt đầu nằm ở `writeConfig`**, ném 409 `GAME_LOBBY_ONLY`, chứ không nằm ở handler socket `onConfigUpdate`. Ban đầu tôi ghi nhận sai là "không có chốt nào" và đã phải tự đính chính giữa phiên. Bài học: **đọc tới hàm thật sự ghi dữ liệu**, đừng dừng ở handler nhận event — cùng họ với bài học N19 ("đọc `*.service.ts`, đừng tin tên key ở controller").

## 2. Baseline của patch là config của phòng, không phải default của mode

Ở N17 (màn tạo phòng), `buildGameConfigPatch` so giá trị form với **default của mode** để chỉ gửi field khác mặc định — đúng, vì lúc đó phòng chưa tồn tại.

Ở N20 baseline đó sai hoàn toàn. Nếu host bật "Hiện gợi ý" lúc tạo phòng (default là tắt) rồi vào lobby tắt lại, thì giá trị mới **trùng default** ⇒ builder lọc mất field ⇒ patch rỗng ⇒ không có gì được gửi, mà UI vẫn hồn nhiên báo "đã lưu". Đây là loại bug im lặng tệ nhất: không crash, không log, chỉ đơn giản là thao tác của người dùng bốc hơi.

Cách sửa: `buildGameConfigPatch(descriptor, values, baseline)` với `baseline` là tham số có default. Màn tạo phòng truyền `descriptor.defaultBaseline()`, host lobby truyền `GameConfig.baselineFor(descriptor)` — tức là **config thật đang chạy của phòng**. Rút ra: hàm "chỉ gửi cái đã thay đổi" phải nhận điểm so sánh từ bên gọi; hard-code baseline vào trong hàm là ép một ngữ cảnh sử dụng lên mọi ngữ cảnh khác.

## 3. Thêm một nhánh vào sealed class ở `core:common` làm vỡ `when` ở module khác

Thêm `GameEvent.GameStarted` xong thì build fail hai chỗ hoàn toàn không liên quan tới việc tôi đang làm:

```
e: HostLobbyViewModel.kt:102:9 'when' expression must be exhaustive. Add the 'is GameStarted' branch or an 'else' branch.
e: PlayerLobbyViewModel.kt:87:9 ...
```

Đây thực ra là **tính năng, không phải bug** — compiler đang làm đúng việc: bắt tôi trả lời câu hỏi "player lobby phản ứng thế nào khi trận bắt đầu?" thay vì để nó âm thầm rơi vào `else`. Quy trình rút ra: sau khi thêm nhánh vào sealed class dùng chung, **rà `when` ở TẤT CẢ module tiêu thụ trước khi build**, và tuyệt đối không dập lửa bằng cách thêm `else -> Unit`.

## 4. Editor cấu hình đặt sai module từ N17 — trả nợ ở đây

N17 để `RoomConfigForm`, `GameModeConfigEditors` và `GameConfigPatchBuilder` trong `feature:quiz-manage` vì lúc đó chỉ màn tạo phòng dùng. N20 cần đúng bộ đó ở `feature:lobby`, và **feature không được phụ thuộc feature**.

Đã chuyển lên `core:ui/gameconfig` và tách hạt mịn hơn thay vì bê nguyên khối: `RoomConfigForm` (state), `BooleanSettingRow`, `NumberSettingField`, `ChoiceSettingField` (3 loại control), `GameModeConfigEditor` (dispatch theo mode), `GameConfigPatchBuilder` (diff). Ràng buộc kỹ thuật đáng ghi: `core:ui` **không có Hilt, không có kotlinx.serialization** — nên mọi thứ chuyển lên phải là hàm thuần + composable stateless, không được lỡ mang theo DI hay DTO. Chính ràng buộc này lại là bộ lọc tốt: cái gì chuyển lên `core:ui` được thì thường đúng là UI thật.

Bài học đặt module: khi một component *có thể* dùng chung nhưng hiện chỉ một chỗ dùng, để lại ở feature là hợp lý — nhưng phải chấp nhận **chi phí di chuyển sau này**, và chi phí đó rẻ hơn hẳn nếu component đã tách nhỏ theo trách nhiệm từ đầu.

## 5. UX của bottom sheet: đóng khi nào là quyết định thiết kế, không phải chi tiết cài đặt

Bản đầu tôi giữ sheet mở sau mọi kết quả để người dùng "thấy trạng thái mới". Test thật cho thấy nó sai: lưu xong mà sheet còn đó thì người dùng không biết đã xong hay chưa, phải bấm "Đóng" thêm một lần vô nghĩa.

Quy tắc chốt lại, đủ đơn giản để phát biểu thành một câu: **sheet chỉ ở lại khi lỗi phát sinh trước lúc gọi server** (field sai định dạng — vì lỗi nằm ngay trong form, đóng đi là phá dữ liệu đang gõ). Mọi kết quả khác — lưu thành công, patch rỗng, lỗi mạng, mất socket, phòng đã rời lobby — đều **đóng sheet và báo bằng snackbar**. Tiêu chí phân loại không phải "thành công/thất bại" mà là **"còn việc cho người dùng làm trong sheet hay không"**.

Kèm theo: nút Lưu disable ngay khi bấm (`isSavingConfig`), nút Đóng cũng disable trong lúc chờ ACK, và ViewModel có guard `if (isSavingConfig) return` để double-tap trong cùng một frame không lọt được hai request.

## 6. Field không sửa được thì ẩn hẳn — và điều đó khiến một nhánh code không thể test từ UI

Editor chỉ render field nằm trong `descriptor.editable`; field `locked` không vẽ ra (không phải làm mờ). Lý do: field locked của một mode thường vô nghĩa với người dùng — "Số mạng" ở Classic chẳng bao giờ dùng tới, hiện ra chỉ gây nhiễu.

Hệ quả mà tôi chỉ nhận ra khi liệt kê kịch bản test: **client không còn đường nào gửi field bị locked**, nên nhánh xử lý `ignored` (server báo "đã bỏ qua N thiết lập") **không thể kích hoạt được từ UI**. Nó không phải code chết — nó là lớp phòng thủ cho trường hợp backend đổi `MODE_CONFIG_SPEC` mà app chưa cập nhật. Nhưng phải ghi rõ điều đó vào KDoc, nếu không người sau (kể cả chính tôi) sẽ mất thời gian tìm cách tái hiện một tình huống mà UI đã chặn từ gốc.

Cùng loại vấn đề: nút "Bắt đầu" disable khi `connection != CONNECTED`, nên **không tài nào bấm được lúc mất mạng** — nhánh timeout 5s chỉ tái hiện bằng cách bấm rồi bật chế độ máy bay ngay sau đó. Rút ra chung: **UI chặn sớm tốt cho người dùng nhưng tạo ra vùng code không test được thủ công**; những nhánh đó thuộc về unit test, và phải nói thẳng trong danh sách kịch bản là "không áp dụng", đừng để chúng nằm đó như case fail.

## 7. Cache descriptor làm mất nhánh lỗi ở lần mở thứ hai

`openConfigSheet()` chỉ gọi mạng khi `descriptor == null`; lần sau dựng lại form từ cache. Đúng về hiệu năng, nhưng khi test "mở sheet lúc mất mạng" thì không thấy lỗi đâu cả — vì lần mở đầu (còn mạng) đã cache xong. Muốn chạm nhánh lỗi phải để **lần mở đầu tiên** rơi vào lúc offline. Bài học nhỏ nhưng lặp lại nhiều: khi viết kịch bản test cho UI có cache, phải ghi rõ **thứ tự thao tác**, không chỉ ghi trạng thái mạng.

## 8. Hai request song song vì backend không có endpoint gộp

Sheet cần ba thứ: `config` (đã có từ `lobby:updated`), `mode` và `descriptor` (editable/locked/constraint). Không endpoint nào trả cả ba: `GET /:code` cho session nhưng không có config spec, `GET /game-modes` cho spec nhưng không biết phòng nào. Chọn cách chạy hai request **song song** bằng hai `async` trong một `coroutineScope`, tải lười vào lần mở sheet đầu tiên — thay vì tải sẵn lúc vào lobby (đa số host không sửa config) hoặc chạy tuần tự (chậm gấp đôi vô ích vì hai request độc lập).

## 9. Chia sẻ phòng: chốt phạm vi trước khi code

Kế hoạch ghi "mã phòng/QR". Sau khi cân nhắc, bỏ QR ở lượt này (cần thêm thư viện, và người nhận vẫn phải có app) và làm hai nút copy: **mã phòng** (`ABC123`) cho người đã có app, **link** `https://myquizz.dpdns.org/join?code=ABC123` cho người mở web. Origin để `const val WEB_ORIGIN` trong `HostLobbyUiState` thay vì `BuildConfig` — sai lệch có chủ ý, ghi lại để khi cần đổi theo môi trường thì biết chỗ.

## 10. Bài học công cụ / quy trình

- **Tài liệu `.md` nằm ở nhánh `docs`, code Kotlin ở `main`.** Không checkout đúng nhánh thì ghi file vào cây làm việc sai — phải kiểm tra `git status` trước mỗi lượt tài liệu.
- **`write_file` của MCP filesystem không tự tạo thư mục cha** — `ENOENT` bốn lần liên tiếp cho tới khi gọi `create_directory` trước. Và MCP này **không có tool xóa file**, nên file cũ phải `move_file` sang thư mục rác rồi nhờ người dùng `git rm` + xóa tay.
- **Tool search của MCP báo false negative** (`search_files`, `search_content` trả rỗng cho file chắc chắn tồn tại) ⇒ dùng `directory_tree` + `list_directory` + `read_multiple_files` để định vị file.
- **`git push` fail vì SSH port 22 bị chặn chập chờn** (`Connection timed out`), lần thử thứ hai lại thành công. Cách chữa dứt điểm: `Host github.com` / `HostName ssh.github.com` / `Port 443` trong `~/.ssh/config`.
- **`292 actionable tasks: 292 up-to-date` nghĩa là test KHÔNG chạy lại.** Muốn chắc thì `--rerun-tasks`.
- **Line ending trộn trong repo** (một số file LF, một số CRLF, vài file có BOM) — `edit_file` khớp từng byte nên phải kiểm tra line ending của đúng file đó trước khi soạn patch.

---

## Nợ để lại sau N20

- `HostGamePlaceholder` (định nghĩa private trong `GameNavGraph.kt`) sẽ bị thay bằng màn host thật ở N21 — `feature:game-host` vẫn đang rỗng.
- KDoc mục (2) của `HostGameSocketRepository` còn ghi sai rằng server không kiểm `session_status` khi cập nhật config; cần sửa lại theo đúng phát hiện ở mục 1.4.
- Dark mode của sheet cấu hình chưa rà (hoãn theo quyết định của user, thuộc N38 polish).
- Một số typo tiếng Việt trong KDoc/chuỗi (`SocketDtos.kt`, `HostLobbyUiState.kt`, `HostLobbyIntent.kt`) chưa dọn.
- Nhánh `ignored` và nhánh timeout của `game:start` chưa có unit test — hiện chỉ có `SocketAckMapperTest` (7 case) và `GameStartedMapperTest` (3 case) phủ tầng mapper.
