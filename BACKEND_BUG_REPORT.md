# MyQuiz Backend — Bug Report

> **Mục đích:** tập hợp các lỗi backend đã được tái hiện hoặc phát hiện khi audit source để team backend chọn lọc và xử lý.
>
> **Thứ tự:** (1) lỗi đã có luồng test thật, ưu tiên theo mức độ nghiêm trọng; (2) lỗi xác nhận qua audit source, ưu tiên theo mức độ nghiêm trọng; (3) các phần thiếu tính năng/contract nằm cuối và không được gọi là bug.
>
> **Mức độ:** `BLOCKER` = làm sai state/luật chơi hoặc không thể nghiệm thu; `HIGH` = ảnh hưởng trực tiếp gameplay/dữ liệu; `MEDIUM` = contract hoặc dữ liệu không nhất quán nhưng có thể phòng thủ ở client; `LOW` = tài liệu/schema chưa đồng nhất.

---

# I. Lỗi đã có luồng test thật

## TEST-01 — Pause/Resume từ Host làm Player Practice hiển thị sai câu và backend chấm một câu khác

- **Severity:** `BLOCKER`
- **Phạm vi:** self-paced, đã tái hiện ở Practice; cùng root cause có thể ảnh hưởng Solo/Survival/Marathon.

### Performed actions — Chuỗi hành động thực hiện

1. Tạo phòng Practice có nhiều câu hỏi.
2. Cho Player trả lời một vài câu, ví dụ `player.current_question_index` đã lên `3`.
3. Dùng Host bấm Pause.
4. Dùng Host bấm Resume.
5. Quan sát câu được hiển thị lại trên Player.
6. Player trả lời câu đang hiển thị.

### Expected output

- Sau Resume, Player vẫn ở đúng câu trước khi Pause.
- Nội dung câu, index và thứ tự shuffle phải khớp state riêng của Player.
- Backend phải chấm đúng câu đang hiển thị cho Player.

### Actual output

- Player bị đưa về giao diện câu đầu, thường là index `0`.
- `player.current_question_index` thật trên backend vẫn là `3`.
- Khi Player trả lời giao diện “câu 1”, backend thực tế chấm câu tại index `3`, tăng index thật lên `4` rồi gửi câu kế tiếp.
- Nếu bật shuffle, nội dung câu còn có thể sai dù index tình cờ đúng.

### Root cause đã xác định

Khi Host Pause/Resume, backend broadcast cùng một `game:state` cho toàn phòng bằng `snapshot(session)` không truyền Player. Với self-paced, snapshot này dùng `session.current_question_index` thay vì `player.current_question_index`, đồng thời không áp dụng lại `orderedQuestionsFor(..., sessionId + playerId)` của từng Player.

### Suggested fix

- Không broadcast một shared snapshot không có Player cho self-paced.
- Build `game:state` riêng theo từng Player, dùng `player.current_question_index` và đúng ordered question list/seed của Player.
- Host nhận Host snapshot riêng; Player nhận Player snapshot riêng.
- Thêm regression test: Player ở index `3` → Pause → Resume → snapshot vẫn index `3`, cùng question id trước Pause.

---

## TEST-02 — Reconnect presence race làm Player mới kết nối lại bị ghi thành disconnected

- **Severity:** `BLOCKER`
- **Phạm vi:** reconnect trong game đang active.

### Performed actions — Chuỗi hành động thực hiện

1. Chạy một phòng Classic với Host và nhiều Player.
2. Cho một Player mất mạng trong lúc trận đang diễn ra.
3. Bật mạng lại để Socket.IO tạo socket mới.
4. Socket mới kết nối và gửi `lobby:join`/`player:sync`.
5. Lặp lại reconnect nhiều lần và quan sát roster Host cùng thời điểm auto-advance.

### Expected output

- Socket mới join thành công phải giữ Player ở trạng thái `connected`.
- `pendingAnswers()` phải tính Player vừa reconnect.
- Host phải thấy đủ Player và không chuyển câu sớm.

### Actual output

- `onLeave()` của socket cũ có thể hoàn tất sau `lobby:join` của socket mới.
- Callback socket cũ ghi đè `player.status = disconnected`.
- Socket mới vẫn nhận đúng `game:state`, nhưng `pendingAnswers()` bỏ Player vì status không còn là `connected`.
- Host thấy thiếu người và có thể auto-advance sớm.

### Root cause đã xác định

Presence chỉ gắn với Player record, chưa gắn với socket identity/generation. Callback rời phòng của socket cũ không kiểm tra đã có socket thay thế hay chưa.

### Suggested fix

- Theo dõi active socket id/generation của từng Player.
- Chỉ đánh dấu offline và gọi `maybeAdvanceAfterLeave()` khi Player không còn socket thay thế.
- Thêm test race: socket B join trước khi `onLeave(socket A)` hoàn tất; kết quả cuối phải là `connected`.

---

## TEST-03 — Auto-timeout không cập nhật tiến độ cho Host

- **Severity:** `HIGH`
- **Phạm vi:** Host dashboard self-paced.

### Performed actions — Chuỗi hành động thực hiện

1. Tạo phòng self-paced có câu hỏi giới hạn thời gian.
2. Cho Player không trả lời cho đến khi câu timeout.
3. Theo dõi màn Player và màn Host cùng lúc.
4. So sánh progress/score/status của Player trên Host trước và sau timeout.

### Expected output

Sau timeout, Host phải nhận realtime update cho Player, tối thiểu gồm:

- current question index hoặc answered count;
- correct/wrong count;
- score;
- lives nếu mode có lives;
- status mới nếu finished/eliminated.

### Actual output

- Backend chỉ emit `question:timeout` cho chính Player.
- Không emit `host:player-progress`.
- Không emit bản `leaderboard:host` mới.
- Host giữ nguyên dữ liệu trước timeout cho đến một event khác hoặc cuối game.

### Suggested fix

Cho nhánh timeout đi qua cùng pipeline cập nhật Host như nhánh submit answer và emit một delta đầy đủ sau khi đã persist state mới.

---

## TEST-04 — Lives của Player không cập nhật realtime trên Host

- **Severity:** `HIGH`
- **Phạm vi:** Survival/Host dashboard.

### Performed actions — Chuỗi hành động thực hiện

1. Tạo phòng Survival.
2. Cho Player trả lời sai để bị trừ mạng.
3. Quan sát lives trên Player và lives của cùng Player trên Host.
4. Tiếp tục trả lời/timeout thêm để lives thay đổi nhiều lần.

### Expected output

Sau mỗi lần backend thay đổi lives, Host phải nhận số mạng mới ngay lập tức.

### Actual output

- Player nhận được lives mới.
- `host:player-progress` không chứa `lives` và `streak`.
- Host giữ số mạng từ baseline `leaderboard:host`; chỉ có thể về `0` nếu sau đó nhận được `player:eliminated`.

### Suggested fix

Bổ sung `lives` và `streak` vào `host:player-progress`, đồng thời emit event này sau answer và timeout:

```ts
{
  player: {
    id,
    player_name,
    current_question_index,
    player_score,
    correct_answers_count,
    lives,
    streak,
    status
  },
  total_questions,
  serverTime
}
```

---

## TEST-05 — `GET /games/:code` trả payload lồng thành `data.session.session`

- **Severity:** `MEDIUM`
- **Phạm vi:** REST room lookup.

### Performed actions — Chuỗi hành động thực hiện

1. Gọi `GET /v1/games/{sessionCode}` với một mã phòng hợp lệ.
2. Parse response theo contract một session object ở `data.session`.
3. Kiểm tra payload thực tế khi ứng dụng tra phòng.

### Expected output

Một lobby payload nhất quán, ví dụ:

```json
{
  "data": {
    "session": {},
    "players": [],
    "config": {}
  }
}
```

### Actual output

Payload thực tế bị lồng:

```text
data.session.session
data.session.players
data.session.config
```

Client từng crash `MissingFieldException` khi map theo contract phẳng.

### Root cause đã xác định

`getLobby()` trả `{session, players, config}`, nhưng controller gán cả cụm vào biến tên `session` rồi gọi `success(res, { session })`.

### Suggested fix

Trả trực tiếp lobby object, ví dụ `success(res, lobby)`. Cần phối hợp migration với các client đang map shape cũ.

---

## TEST-06 — Socket documentation mô tả sai shape answer option

- **Severity:** `LOW`
- **Phạm vi:** socket contract/documentation.

### Performed actions — Chuỗi hành động thực hiện

1. Tạo quiz có câu multiple choice.
2. Host bắt đầu game và nhận `host:question`.
3. Parse option theo socket documentation: `{id, text, image}`.
4. So sánh payload thực tế từ database/game socket.

### Expected output

Runtime payload phải khớp TypeScript type và socket documentation.

### Actual output

- Dữ liệu thực tế dùng `option_text`, không phải `text`.
- Một số dữ liệu có thể ở dạng chuỗi thuần.
- Client theo docs hiển thị sai hoặc có thể làm rơi toàn bộ `host:question` khi parse cứng.

### Suggested fix

Chọn một schema chuẩn duy nhất cho answer option; normalize trước khi emit và cập nhật đồng thời runtime type, docs và test contract.

---

# II. Lỗi xác nhận qua audit source

## AUDIT-01 — Snapshot self-paced không dùng cùng ordered question list với gameplay

- **Severity:** `BLOCKER`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo phòng self-paced có `shuffleQuestions=true`.
2. Cho Player nhận câu qua `sendSelfQuestion()`.
3. Gọi reconnect hoặc `player:sync` để backend tạo snapshot.
4. So sánh question id từ snapshot với question id mà Player đang thực sự làm.

### Expected output

Snapshot phải dùng cùng ordered list và cùng seed với `sendSelfQuestion()`/`onAnswer()`.

### Actual output

`sendSelfQuestion()` và `onAnswer()` dùng `orderedQuestionsFor(..., sessionId + playerId)`, trong khi `snapshot()` đọc trực tiếp `questions[index]`. Snapshot có thể trả sai nội dung câu.

### Suggested fix

Tập trung logic resolve current question vào một hàm dùng chung cho send, answer, timeout và snapshot.

---

## AUDIT-02 — Snapshot Marathon không modulo question index

- **Severity:** `BLOCKER`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo Marathon với question bank hữu hạn.
2. Cho `player.current_question_index` tăng đến hoặc vượt `questions.length`.
3. Gọi reconnect hoặc `player:sync`.
4. Kiểm tra question trong snapshot.

### Expected output

Snapshot trả đúng câu Marathon theo `index % orderedQuestions.length`.

### Actual output

Snapshot truy cập trực tiếp `questions[index]`; khi index vượt bank có thể trả `question = null`.

### Suggested fix

Áp dụng modulo sau khi resolve ordered question list của Player.

---

## AUDIT-03 — Marathon timeout kết thúc game ở cuối question bank dù vẫn còn match time

- **Severity:** `BLOCKER`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo Marathon có `totalMatchSeconds` dài hơn thời gian cần để đi hết một vòng question bank.
2. Cho Player đến câu cuối bank.
3. Để callback timeout xử lý và tăng `current_question_index` lên bằng `questions.length`.
4. Kiểm tra trạng thái Player/game.

### Expected output

Marathon quay vòng question bank và chỉ kết thúc theo điều kiện kết thúc riêng của Marathon, chủ yếu là match budget.

### Actual output

Callback timeout coi `current_question_index >= questions.length` là đã hoàn thành, kể cả Marathon, nên có thể kết thúc sớm.

### Suggested fix

Tách điều kiện terminal theo mode; Marathon modulo index thay vì dùng điều kiện hết bank của Solo/Survival.

---

## AUDIT-04 — `player:sync` có thể ghi đè `question:awaiting_next`

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo self-paced với `autoAdvance=false`.
2. Player trả lời một câu và nhận `question:awaiting_next`.
3. Trước khi bấm Next, cho client reconnect hoặc gọi `player:sync`.
4. Quan sát snapshot trả về và state cuối của Player.

### Expected output

Player vẫn ở result/awaiting-next của câu vừa trả lời và vẫn có quyền yêu cầu câu tiếp theo.

### Actual output

Snapshot có thể biểu diễn index/question kế tiếp hoặc một phase không tương đương, đến sau và ghi đè state `question:awaiting_next`.

### Suggested fix

Lưu/derive rõ `awaiting_next` trong Player snapshot và đảm bảo event cùng snapshot có cùng state semantics.

---

## AUDIT-05 — Timeout làm lives về 0 nhưng không broadcast `player:eliminated`

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo Survival với Player còn `1` mạng.
2. Để câu hiện tại timeout.
3. Backend giảm lives về `0`.
4. Theo dõi event ở Player room và Host room.

### Expected output

Backend emit `player:eliminated` sau khi persist lives/status terminal.

### Actual output

Nhánh timeout cập nhật state nhưng không broadcast `player:eliminated`.

### Suggested fix

Dùng chung hàm xử lý elimination cho answer sai và timeout; thêm contract test cho cả hai đường.

---

## AUDIT-06 — Host không được refresh khi Player late-join/reconnect/disconnect trong session active

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Bắt đầu một phòng self-paced.
2. Cho Player late-join hoặc reconnect/disconnect khi session đang active.
3. Không cho Player trả lời thêm.
4. Theo dõi `lobby:updated`, `host:player-progress` và `leaderboard:host` ở Host.

### Expected output

Host nhận ngay roster/status mới sau mọi thay đổi presence.

### Actual output

Backend không phát một snapshot/delta đầy đủ cho Host. Host thường chỉ thấy Player sau progress event tiếp theo; nếu Player không trả lời thêm, dashboard có thể stale vô thời hạn.

### Suggested fix

Emit Host delta hoặc `leaderboard:host` sau join, reconnect, disconnect, leave, finish và elimination.

---

## AUDIT-07 — Host reconnect giữa câu bị mất answer key

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Chạy một câu host-paced đang active.
2. Cho Host mất kết nối rồi reconnect.
3. Host join lại room và nhận `game:state`.
4. Kiểm tra khả năng xem đáp án đúng của câu hiện tại.

### Expected output

Host reconnect phải nhận đủ Host-only state, bao gồm answer key của câu đang mở.

### Actual output

`game:state.question` là public question, không có `correct_answer`; backend không phát lại `host:question`. Host không thể khôi phục answer key cho đến câu tiếp theo.

### Suggested fix

Sau Host join/reconnect, emit lại `host:question` hoặc trả Host snapshot riêng chứa answer key.

---

## AUDIT-08 — Snapshot ở `showing_results` không đủ dữ liệu phục hồi màn kết quả

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Cho game chuyển sang `showing_results`.
2. Ngắt kết nối Player.
3. Reconnect và gọi sync để nhận `game:state`.
4. Dựng lại result screen chỉ từ snapshot.

### Expected output

Snapshot có đủ result state gần nhất: outcome, score earned, answer key theo visibility rule và stats cần thiết.

### Actual output

Snapshot thiếu dữ liệu để phục hồi đúng màn kết quả; client chỉ có thể chờ câu tiếp theo.

### Suggested fix

Bổ sung last-question result vào Player snapshot và áp dụng đúng `showCorrectAnswer`/`showLeaderboard`.

---

## AUDIT-09 — Race condition tạo trùng `session_code`

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Gửi nhiều request tạo game đồng thời.
2. Để các request chạy qua bước sinh mã và kiểm tra mã tồn tại.
3. Cho hai request cùng chọn một mã trước khi request kia insert.
4. Kiểm tra constraint/database result.

### Expected output

Database không bao giờ có hai phòng còn hiệu lực cùng `session_code`.

### Actual output

`session_code` không có unique constraint phù hợp; flow check-then-insert có cửa sổ race tạo mã trùng.

### Suggested fix

Dùng partial unique index cho session còn hiệu lực và `INSERT ... ON CONFLICT` retry mã khác.

---

## AUDIT-10 — Redis failure có thể làm `timeTaken ≈ 0` và cấp speed bonus tối đa

- **Severity:** `HIGH`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo self-paced có speed bonus.
2. Làm Redis/player clock unavailable trước khi submit answer.
3. Submit một câu trả lời.
4. Kiểm tra `timeTaken` và điểm speed bonus.

### Expected output

Thiếu authoritative clock phải làm request fail an toàn hoặc dùng fallback không thiên vị.

### Actual output

`timeTaken` có thể rơi về gần `0`, khiến Player nhận speed bonus tối đa.

### Suggested fix

Không default thời gian về `0`; fail-closed grading khi thiếu clock hoặc dùng một timestamp fallback đã được xác minh.

---

## AUDIT-11 — `total_players` không phản ánh roster realtime

- **Severity:** `MEDIUM`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo lobby mới.
2. Cho nhiều Player join/leave.
3. Đọc session row và danh sách `players` trước khi game được flush/kết thúc.
4. So sánh `total_players` với số phần tử roster thực tế.

### Expected output

`total_players` khớp roster hiện tại nếu field được public như dữ liệu realtime.

### Actual output

Field có thể stale vì không cập nhật theo mỗi join/leave và thường chỉ được flush ở giai đoạn sau.

### Suggested fix

Cập nhật counter atomically hoặc bỏ field khỏi contract realtime và quy định dùng roster length.

---

## AUDIT-12 — Có thể tạo/bắt đầu game từ quiz không có câu hỏi

- **Severity:** `MEDIUM`

### Performed actions — Chuỗi hành động thực hiện

1. Tạo hoặc lấy một quiz có `total_questions = 0`.
2. Gọi API tạo game với quiz đó.
3. Host gọi start game.
4. Quan sát question/timer/terminal state.

### Expected output

Backend reject create hoặc start bằng error code rõ ràng.

### Actual output

Thiếu validation chắc chắn cho quiz rỗng, khiến game có thể vào state không có câu để gửi.

### Suggested fix

Validate `total_questions > 0` ở service tạo/start game và thêm database/service test.

---

## AUDIT-13 — Config descriptor có field đồng thời nằm trong `editable` và `locked`

- **Severity:** `MEDIUM`

### Performed actions — Chuỗi hành động thực hiện

1. Gọi endpoint lấy game-mode config descriptor.
2. So sánh hai tập `editable` và `locked`.
3. Kiểm tra các key như `flow.allowAnswerLate`.

### Expected output

Hai tập không giao nhau; mỗi field có một quyền chỉnh sửa rõ ràng.

### Actual output

Một field có thể xuất hiện trong cả hai tập, khiến client phải tự chọn quy tắc ưu tiên.

### Suggested fix

Validate descriptor khi khởi tạo và fail test nếu `editable ∩ locked` khác rỗng.

---

## AUDIT-14 — Enum `game_mode` có `team` nhưng engine chưa implement

- **Severity:** `MEDIUM`

### Performed actions — Chuỗi hành động thực hiện

1. Kiểm tra enum/schema/database của `game_mode`.
2. Kiểm tra mode registry và gameplay engine.
3. So sánh khả năng tạo dữ liệu với khả năng chạy game.

### Expected output

Mọi mode được schema chấp nhận phải có engine hoàn chỉnh, hoặc không được public/insert.

### Actual output

`team` tồn tại trong enum nhưng không có gameplay behavior đầy đủ.

### Suggested fix

Xóa/khóa `team` khỏi schema public cho đến khi implement xong, hoặc bổ sung engine và test trước khi cho tạo phòng.

---

# III. Thiếu tính năng hoặc contract — không phân loại là bug

## FEATURE-01 — Host không nhận `matchEndsAt` riêng của từng Player Marathon

### Performed actions — Chuỗi hành động thực hiện

1. Tạo Marathon cho phép late join.
2. Cho hai Player nhận câu đầu ở hai thời điểm khác nhau.
3. Theo dõi `leaderboard:host` và `host:player-progress`.

### Expected output

Nếu sản phẩm yêu cầu Host theo dõi thời gian, mỗi row cần có deadline/budget còn lại của Player.

### Actual output

`matchEndsAt` tồn tại theo từng Player nhưng không được gửi cho Host; Host không thể hiển thị countdown chính xác.

### Request đề xuất

Bổ sung `matchEndsAt` vào Host snapshot/progress. Việc Player late-join vẫn nhận đủ `totalMatchSeconds` riêng là contract hiện tại, không phải bug.

---

## FEATURE-02 — Chưa có endpoint lấy phiên game đang active để quay lại phòng

### Performed actions — Chuỗi hành động thực hiện

1. User đang có một game chưa kết thúc.
2. Đóng/mở lại ứng dụng hoặc rời màn game.
3. Client tìm endpoint để lấy active session và socket token mới.

### Expected output

Có endpoint như `GET /games/active` trả active session, role, session code và cách cấp lại socket token.

### Actual output

Không có endpoint đủ dữ liệu để client tìm và quay lại game đang diễn ra.

---

## FEATURE-03 — History chưa có `role=all` với cursor thống nhất

### Performed actions — Chuỗi hành động thực hiện

1. User có cả session đã host và session đã chơi.
2. Gọi history cho `role=hosted` và `role=played`.
3. Thử dựng một feed hoạt động chung có phân trang ổn định.

### Expected output

Một query `role=all` hoặc endpoint thống nhất với một cursor/order duy nhất.

### Actual output

Chỉ có các luồng riêng; merge phía client làm hỏng semantics cursor và thứ tự toàn cục.

---

## FEATURE-04 — Feed chưa có đường phân trang đầy đủ cho `featured`

### Performed actions — Chuỗi hành động thực hiện

1. Lấy Home section có type `featured`.
2. Tìm endpoint/filter để tải thêm đúng tập featured.
3. Thử phân trang bằng feed/search hiện có.

### Expected output

Có filter `section=featured` hoặc equivalent với cursor ổn định.

### Actual output

Không có endpoint/filter public tương đương để “xem thêm” đúng tập featured.

---

## FEATURE-05 — Chưa có taxonomy endpoint cho quiz topics

### Performed actions — Chuỗi hành động thực hiện

1. Client cần hiển thị danh sách topic/filter hiện hành.
2. Tìm endpoint trả taxonomy/metadata topic.
3. So sánh với topic đang xuất hiện trong quiz data.

### Expected output

Backend cung cấp danh sách topic canonical, label và trạng thái sử dụng.

### Actual output

Client phải dùng danh sách trình bày hard-code và chỉ append topic lạ khi tình cờ gặp trong dữ liệu.

---

## FEATURE-06 — PATCH quiz chưa có semantics rõ để clear cover/description

### Performed actions — Chuỗi hành động thực hiện

1. Quiz đã có `quiz_image` hoặc `quiz_description`.
2. Gửi PATCH với ý định xóa field về `null`.
3. Đọc lại quiz.

### Expected output

API có một cách rõ ràng để phân biệt “giữ nguyên field” và “xóa field”.

### Actual output

Field vắng được hiểu là giữ nguyên; contract hiện tại không cung cấp clear semantics ổn định nên client không thể xóa cover/description thật.

### Request đề xuất

Cho phép explicit `null` để clear hoặc cung cấp operation/endpoint riêng.

---

# IV. Thứ tự xử lý đề xuất

1. `TEST-01` — Pause/Resume làm Player self-paced hiển thị và trả lời sai câu.
2. `TEST-02` — Presence race khi reconnect.
3. `TEST-03` + `TEST-04` + `AUDIT-05` — Đồng bộ Host progress/lives/elimination cho answer và timeout.
4. `AUDIT-01` + `AUDIT-02` + `AUDIT-03` — Thống nhất resolver câu self-paced/Marathon.
5. `AUDIT-04` — Snapshot phải giữ `awaiting_next`.
6. `AUDIT-06` — Refresh Host khi presence thay đổi.
7. `AUDIT-07` + `AUDIT-08` — Hoàn thiện snapshot phục hồi Host/Player.
8. `AUDIT-09` + `AUDIT-10` — Data integrity và scoring integrity.
9. Các lỗi contract/data mức Medium và nhóm Feature Request.
