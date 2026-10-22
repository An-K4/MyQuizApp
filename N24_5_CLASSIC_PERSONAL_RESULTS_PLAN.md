# N24.5 — Classic private personal results và safe reconnect (#21)

> **Trạng thái: IMPLEMENTED — còn mở verification/gates.** Android local `main`: `14fb62f`; baseline trước triển khai `b005df3`. Backend local đã audit `server/myquizz` `0c5908f` (#21), không phải bằng chứng deployment. User báo test xanh và happy case bước đầu ổn; evidence/giới hạn hiện tại ở mục 8.
> Mục 1–7 giữ contract, phạm vi và ma trận đã duyệt; không xem mọi case trong kế hoạch là đã chạy. Lượt cập nhật này chỉ sửa docs trên nhánh `docs`, không chạy Gradle hoặc commit/push/checkout.

## 1. Phân loại trong kế hoạch N1–N50

- **N24.5** là bổ sung contract/state machine cho **N22–N24 (Player Classic/host-paced)**. Không đổi tên hoặc mở lại toàn bộ các mốc lịch sử đã hoàn thành; không thêm N51 hay dồn thay đổi nghiệp vụ vào một checkpoint UI-only.
- N46.22–N46.25 đã polish câu hỏi/feedback/tổng kết giữa câu và countdown. N24.5 tận dụng layout này để nhận dữ liệu thật, không thiết kế lại toàn luồng.
- Liên quan **N21** (Host), **N26–N29** (self-paced), **N25/N44/N47** (regression/E2E). Sự liên quan không có nghĩa mọi blocker của các N này đã được sửa.
- Tiến trình: đã triển khai/commit N24.5 `14fb62f`, user báo test xanh và happy case bước đầu ổn. Còn verify ma trận ngoài happy case; sau đó audit **câu cuối → game ended → kết quả cuối Host/Player → entry xem lại/Home**, xin duyệt scope UI riêng. lỗi max streak (N24.5 mục 9) được ghi backlog, không tự gộp fix vào N24.5.

## 2. Baseline và bằng chứng

| Nguồn | Đã xác minh / giới hạn |
| --- | --- |
| Android `b005df3` → `14fb62f` | Baseline cũ chưa có private result; `14fb62f` đã tích hợp model/event/snapshot/mapper/reducer và binding summary Classic, 15 file source/test. |
| Backend `0c5908f` | Đã audit source socket, AsyncAPI và test. Diff có 8 file, không thấy migration DB/đổi REST schema; không đổi mặc định 5 giây từ bản trước. |
| Thư backend | Team báo build, lint, 20 test và CI đạt. Đây là **báo cáo của team**, agent không chạy lại hoặc xác minh deployment. Thư viết PR chờ review/merge; local hiện có commit #21, không dùng thư để suy trạng thái upstream/deploy hiện tại. |
| Tài liệu contract | `backend/CLASSIC_PLAYER_RESULTS.md` được nhắc trong thư nhưng **không tồn tại tại HEAD local đã kiểm tra**. Kế hoạch dựa trên implementation + AsyncAPI + tests, không báo đã đọc file này. |
| Kiểm thử Android | Các lượt cũ giữ nguyên evidence lịch sử. Lượt N24.5: user báo test xanh sau fix fixture và happy case bước đầu ổn; agent chưa đối chiếu log/XML lượt xanh, chi tiết mục 8. |

Tham chiếu backend: [commit #21](https://github.com/Ntd1411/myquizz/commit/0c5908f), [PR #21](https://github.com/Ntd1411/myquizz/pull/21). Kiểm tra lại ref trước khi triển khai; không giả định local mirror luôn mới nhất.

## 3. Contract đã audit: khác bản cũ ở đâu?

### 3.1. Kết quả riêng cho Player

Event mới **`question:personal-results`** chỉ gửi tới các kết nối được xác thực của đúng Player, sau khi câu đã đóng và kết quả được chốt, trong Classic `showing_results` và khi **`showCorrectAnswer=true`**.

```json
{
  "index": 0,
  "question_id": 123,
  "answered": true,
  "isCorrect": true,
  "scoreEarned": 950,
  "totalScore": 3420,
  "streak": 3,
  "nextQuestionAt": "2026-10-20T04:00:05.000Z",
  "serverTime": "2026-10-20T04:00:00.000Z"
}
```

Ví dụ minh họa shape, không phải dữ liệu production. Giữ nguyên mixed-case field names; `index` phải khớp index server, không tự đổi cơ số khi so khớp.

- `answered=false` → `isCorrect=null`, `scoreEarned=0` khi không có đáp án được chấp nhận lúc đóng câu.
- `streak` là **chuỗi đúng hiện tại**, không phải chuỗi dài nhất. Đúng tăng 1; sai hoặc không trả lời được chấp nhận (kể cả hết giờ/mất kết nối) reset 0 **do server tính**.
- `nextQuestionAt` nullable cho nhánh thủ công/tạm dừng. Không tự gán deadline khi null.
- ACK `question:answer` của Classic vẫn chỉ xác nhận tiếp nhận; **không công bố chấm điểm khi câu mở**. Không sửa ACK thành nguồn grading.
- Common `question:results` vẫn cung cấp key/stats/timing được phép, không phải nguồn `scoreEarned`/streak cá nhân.

### 3.2. Snapshot/reconnect

- `player:sync` → `game:state.personal_result` cùng contract ở `showing_results` (kể cả đang pause), ngoài phase này null; vẫn cần reveal bật.
- Snapshot broadcast dùng chung khi pause/resume không chứa private result. **Null/thiếu từ broadcast không mặc nhiên có nghĩa xóa kết quả riêng đã nhận cho cùng câu.**
- Snapshot public question vẫn không có `correct_answer`; private result cũng không có key hoặc lựa chọn đã chọn. Reconnect có thể khôi phục **kết quả cá nhân/điểm/streak**, nhưng **không đảm bảo replay tô màu đáp án inline** nếu chưa từng nhận key/accepted keys.
- So khớp cả `index` và `question_id`. Thời gian/phase từ snapshot server mới nhất có quyền ưu tiên; không để event cũ phục hồi deadline đã bị pause hoặc kéo Results lùi về Question.

### 3.3. Privacy và side effects ngoài event mới

| Trước #21 | Sau #21 / ảnh hưởng cần kiểm tra |
| --- | --- |
| Classic thiếu grading/điểm/streak cá nhân giữa trận | Có event riêng và snapshot restore có điều kiện; Android phải chủ động tích hợp. |
| Snapshot Player có thể lộ grading/score/streak lúc câu mở, kể cả pause | Snapshot che các field này theo phase; không xem field vắng là điểm 0 thật. |
| Player leaderboard giữa trận dựa config hiển thị | Nhánh emit chung và snapshot còn ràng buộc reveal; `showCorrectAnswer=false` không được treo UI “đang chờ hạng” vô hạn. |
| Shared leaderboard dùng nhiều mode | Thay đổi hàm chung có thể ảnh hưởng **self-paced**, không được chỉ test Classic. Host giữ dữ liệu giám sát riêng/full board. |
| Answer và close có thể giao nhau | Backend serialize nhận đáp án/đóng câu; client vẫn cần idempotency và ACK uncertainty, không tự cộng điểm/chuỗi. |
| Lobby/snapshot có thể mang player_score | Có nhánh bỏ score trong active/paused. DTO default 0 parse được không đồng nghĩa 0 là giá trị đã xác minh. |

Không có bằng chứng #21 sửa presence race (BUG-04/N25), Practice pause/resume/order (BUG-01/N29), token renewal (N30) hoặc public REST visibility (BUG-16). Không đóng các gate này.

## 4. Phạm vi Android phải đổi

### 4.1. Model và socket — core:common / core:network

1. Thêm domain model private result và `GameEvent` tương ứng; bổ sung nullable `personalResult` vào domain snapshot, giữ tương thích server cũ không có field.
2. Thêm event name/listener trong `GameSocketEvents`/`SERVER_EVENTS`; bổ sung DTO/mapper trong lớp socket, bao gồm `GameStateDto` hiện ở `HostSocketDtos.kt`. Dùng chung một mapper cho event riêng và `personal_result` để không lệch quy tắc.
3. Validate payload: identity bắt buộc, nullable đúng contract, phân biệt **thiếu field / malformed** với kết quả hợp lệ `answered=false`, điểm/streak 0. Không default field chấm điểm thiếu thành thất bại thật.
4. Rà tất cả consumer exhaustive `when(GameEvent)`, cập nhật `serverTime`, repository/fake/tests, gồm Host/Lobby nếu compile phụ thuộc sealed event; Host không render kết quả riêng của Player.
5. Thêm fixtures đúng mixed-case và snapshot có/không/null private result. Không thay ACK Classic hay tự suy private result từ leaderboard.

### 4.2. Reconcile trạng thái — feature:game-player

1. Tạo một đường reconcile dùng cho realtime + sync; state giữ private result gắn cặp **index + questionId**, không chỉ một số streak toàn trận.
2. Kết quả riêng hợp lệ là nguồn ưu tiên cho answered/isCorrect/scoreEarned/totalScore/streak, kể cả ACK trước đó mất. Fallback cũ chỉ dùng cho phần đủ dữ liệu và được phép, không chấm editable draft hoặc giả điểm/streak.
3. Bỏ qua sai câu/cũ/đã Finished; new question xóa dữ liệu câu trước. Duplicate cập nhật idempotent, không cộng streak, không nhân điểm, không restart feedback 2 giây.
4. Xử lý common/private/ACK đến khác thứ tự. Late ACK không kéo phase lùi hoặc ghi đè grading đã chốt; event riêng không tự mở khóa input. Test cả private-before-common như tình huống phòng thủ, không tuyên bố backend hứa thứ tự đó.
5. Snapshot có private result hợp lệ khôi phục summary ngay cả khi common result không replay. Snapshot dùng chung thiếu/null không xóa cache hợp lệ cùng câu; nhưng đổi câu/config cấm reveal/terminal phải xóa hoặc chặn render dữ liệu cũ.
6. Pause/offline đóng băng thao tác và feedback theo flow hiện tại; resume nhận deadline mới từ state. Không để `nextQuestionAt` event riêng cũ ghi đè trạng thái pause/null hoặc timing mới hơn.
7. Giữ sync tại connected/recovery, không thêm delay/double-join/polling hoặc sync mỗi broadcast để che thiếu contract. Nếu phát hiện cần một sync bổ sung, chứng minh trigger/guard và review riêng.
8. `game:ended`/câu mới preempt mọi feedback/summary; FINISHED snapshot chỉ navigate một lần, giữ REST fallback hiện tại. Không gửi `question:next` cho Classic.

### 4.3. Presentation — tận dụng UI N46.24, không thêm route

- Vẫn **feedback inline tối đa 2 giây → tổng kết gọn** trong Gameplay; tổng kết dùng thời gian còn lại từ `nextQuestionAt`/clock offset. Không cộng 2 giây vào deadline server hoặc ép đủ thời gian khi server đã chuyển câu.
- Reconnect muộn vào summary trực tiếp thay vì replay đủ 2 giây. Deadline đã qua thì chờ event/state server, không tự advance.
- Chỉ tô key/đáp án khi có đủ dữ liệu được phép từ common result/cache; private result không chứa key nên không được dựng lựa chọn/đáp án giả.
- Summary thêm **điểm câu, tổng điểm và streak hiện tại** từ private result hợp lệ, độc lập việc có rank. 0 hợp lệ khác thiếu dữ liệu. Không thêm explanation, maxStreak hoặc số mẫu từ ảnh.
- Hạng chỉ hiện khi cấu hình cho phép và leaderboard đúng câu/còn mới. Không dùng leaderboard làm nguồn duy nhất của tổng điểm khi đã có personal result.
- `showCorrectAnswer=false`: không render private grading/streak/điểm giữa trận, không hiện loading hạng không có nguồn; giữ chờ trung tính/skip summary theo gating. Config chưa biết thì không suy quyền reveal.
- `showLeaderboard=end_only/never` không cấm summary cá nhân khi reveal=true; `between_questions` vẫn cần dữ liệu thật và quyền reveal theo contract #21.
- Không thêm full board/chart cho Player, không đưa question/image/options vào summary lần nữa. Host giữ 2 layout N46.23, countdown N46.25 và Solo Preview không đổi nghiệp vụ.

## 5. Ma trận regression bắt buộc

| Nhóm | Case tối thiểu / expected |
| --- | --- |
| Contract | Event riêng + snapshot cùng payload; server cũ/missing/null; mixed-case; unanswered/null; malformed không biến thành thua/0 thật. |
| Grading | Đúng/sai/không trả lời/ACK mất/timeout/disconnected: chỉ hiển thị kết quả server, không tự tăng/reset streak. |
| Race/idempotency | Common↔private khác thứ tự; duplicate; ACK đến trễ; sai index hoặc question_id; event câu trước; không restart feedback/cộng điểm. |
| Restore | Offline trong Question/feedback/summary; reconnect sớm/muộn; cùng câu/đã sang câu mới; snapshot có private nhưng thiếu key; shared snapshot null giữ cache hợp lệ. |
| Pause/timing | Pause ở feedback/Results; null deadline; resume deadline mới; event cũ không unpause; explicit showResultsSeconds 0/2/5 và manual advance. |
| Privacy/config | reveal true/false/unknown × board between_questions/end_only/never; không lộ grading/score/streak trước close/khi pause câu mở; đổi config không lộ cache. |
| Terminal | Câu mới/game ended preempt; FINISHED sync nav once + REST fallback; Classic không phát question:next; lỗi token/terminal giữ hành vi hiện tại. |
| Host | Live count, chart lựa chọn, multi-select distribution, text không chart; full board theo config, pause/resume/end/manual next; private event không xuất hiện sai vai trò. |
| Self-paced | Solo/Survival/Marathon/Practice: ACK feedback, next/auto/manual, lives/elimination/timeout và leaderboard sau thay đổi hàm chung backend. Ghi rõ blocker cũ, không nhận vơ đã sửa. |
| Lobby/UI | Bỏ player_score không parse fail/hiện 0 như thật; 4 question types, option id "0", accepted keys, ảnh, IME text dài, initial preparing shell/countdown không flash UI cũ. |

Bổ sung mapper/VM/presentation tests trước hoặc cùng production; không xóa assertion/nới guard để xanh. Rà tất cả fake khi thêm domain/event fields. Không cần thay shared core:ui nếu layout hiện tại đáp ứng; nếu có thay đổi shared thì mở rộng suite tương ứng.

## 6. Các đợt triển khai và điều kiện hoàn tất

1. **Recheck trước code:** user checkout main; kiểm tra status/refs, backend revision và server thực sự đang test đã deploy #21 chưa. Đọc source mới nhất; nếu contract khác, dừng cập nhật kế hoạch trước.
2. **Contract + tests:** domain/event/snapshot/mapper/listener, compatibility và exhaustive consumers. Chưa dùng dữ liệu giả để demo streak.
3. **Reducer + tests:** authority/identity/order/cache/privacy/timing/reconnect; giữ flow self-paced cũ tách biệt.
4. **UI nhỏ + tests:** render personal summary thật, rank optional, hidden state không loading vô hạn; không redesign kết quả cuối trong commit logic này.
5. **User chạy trên main** (đề xuất, agent không chạy):

```powershell
.\gradlew.bat :core:common:testDebugUnitTest :core:network:testDebugUnitTest :feature:game-player:testDebugUnitTest :feature:game-host:testDebugUnitTest :feature:lobby:testDebugUnitTest :app:assembleDebug
```

Nếu chạm shared countdown/preview/core:ui thì thêm `:core:ui:testDebugUnitTest` và `:feature:quiz-preview:testDebugUnitTest`. Lint/CI và release gates vẫn cần evidence riêng.

6. **Manual ít nhất 1 Host + 2 Player:** ghi source/APK/backend deployed revision, config, thời gian, thứ tự thao tác và log an toàn (không token/secret). Phủ privacy trước close, đúng/sai/unanswered, lost ACK, pause/resume, reconnect muộn, board gating, câu cuối và smoke self-paced. Backend chưa deploy thì ghi BLOCKED, không gọi PASS.
7. **Review/commit riêng:** chỉ đề xuất sau khi kiểm tra diff/untracked và user duyệt. Tên dự kiến `feat(game-player): consume private Classic question results (N24.5)`. Có thể chia contract/reducer nếu diff lớn nhưng không để bản trung gian hỏng compile.
8. **Docs/evidence:** cập nhật trạng thái N24.5 bằng kết quả thật, không tự đóng N25/N44/N45/M6. BUG-13 chỉ xem xét đóng trong phạm vi đã test; key replay không có trong payload phải ghi rõ giới hạn.

## 7. Ngoài phạm vi và điều kiện phải hỏi lại

- Không sửa backend, DB/REST, thuật toán điểm/streak; không local increment/reset streak, không thay countdown/default 5 giây lần nữa.
- Không làm token renewal/presence workaround, Practice snapshot fix hoặc public REST privacy trong đợt client #21.
- Không bật reveal để lấy kết quả; không phát thông tin riêng sang Host/Player khác; không persist private result/token vào kho lâu dài.
- Không tự nâng Compose/Kotlin/JDK/AGP để chữa Preview; lỗi compile smart cast đã xử lý bằng biến local ổn định, không phải lý do nâng toolchain.
- Nếu muốn replay key khi reconnect, đổi quy tắc reveal=false, đưa maxStreak vào cuối trận hoặc sửa backend leaderboard self-paced khác contract hiện có: cần xác nhận contract/phạm vi mới trước.
- Kết quả cuối Host/Player và màn review là hướng N46 tiếp theo **sau N24.5**, phải audit rộng cả route/initial/loading/final recovery, không chỉ sửa một frame.

## 8. Checkpoint triển khai thực tế — Android `14fb62f`

### Đã triển khai, không UI polish

- Commit `14fb62f` — `feat(game-player): support Classic private personal results (N24.5)`, đã kiểm tra local main; 15 file source/test. Không suy local commit thành APK/backend deployed revision.
- Model `PersonalQuestionResult`, private event và optional snapshot dùng chung parser; validate field bắt buộc/mixed-case/zero/unanswered, malformed private field không làm mất snapshot bao ngoài. Không log raw private payload.
- Reducer dùng server grading/điểm/current streak, identity index + questionId, idempotent khi duplicate/đảo thứ tự common-private/late ACK; restore giữ cache cùng câu khi shared snapshot thiếu/null. Config/reveal, câu mới và terminal chặn hoặc xóa dữ liệu cũ.
- Snapshot timing/pause/resume ưu tiên; không tự advance, tính điểm hoặc tăng streak; không thêm polling/double join. Reconnect thiếu key chỉ khôi phục personal summary, không tự tạo đáp án để highlight.
- Summary hiện có nhận điểm câu/tổng điểm/current streak độc lập rank; reveal=false/unknown không lộ grading và không chờ hạng vô hạn. Không redesign/final-result polish, không sửa backend/shared countdown/Preview/toolchain.
- Đã rà consumer Host/Lobby và đường repository/use case; không cần sửa source Host/Lobby, nhưng regression thiết bị vẫn cần verify.

### File source/test đã thay đổi

- `core/common/.../model/`: `GameEvent.kt`, `HostGameModels.kt`, **mới** `PersonalQuestionResult.kt`.
- `core/network/.../socket/`: `GameSocketEvents.kt`, `GameEventMapper.kt`, `dto/HostSocketDtos.kt`, **mới** `dto/PersonalQuestionResultDto.kt`; **mới** test `ClassicPersonalResultMapperTest.kt`.
- `feature/game-player/.../presentation/`: `GameUiState.kt`, `GameViewModel.kt`, `ClassicResultsPresentation.kt`, `ClassicResultsContent.kt`, **mới** `ClassicPersonalResultsReducer.kt`; test `GameViewModelTest.kt` và **mới** `ClassicPersonalResultsReducerTest.kt`.
- Tổng cộng **10 file sửa + 5 file mới**. Danh sách đầy đủ/path chính xác có trong diff commit `14fb62f`; các dấu `...` ở trên chỉ rút gọn package Android.

### Evidence kiểm thử — tách rõ báo cáo và xác minh

- Đã bổ sung mapper/reducer/ViewModel regression tests cho contract, identity/order, authority, cache restore, privacy/config, deadline/pause, terminal và self-paced isolation. Có test không đồng nghĩa mọi case thiết bị đã PASS.
- Lượt command common/network/player/host/lobby/assemble đầu tiên FAIL tại Player: **99 tests, 1 failure**, test `classic text answers require submit and never send blank values`. XML đã đối chiếu: fixture lặp short/long trên cùng ViewModel và cùng identity sau Submitted, bị duplicate-start guard chặn đúng. Đã tách fixture thành hai test độc lập, giữ assertion và thêm regression duplicate `question:started`; không nới guard production.
- Sau fix, user xác nhận **“test đã xanh”** và happy case thủ công **“có vẻ ổn”**. Ghi nhận là **user-reported**, chưa có log/XML rerun xanh hoặc CI/run/APK SHA để agent kiểm tra; không tự khẳng định số test PASS sau rerun hoặc full lint/CI.
- Happy-case checklist đã giao: 1 Host + 2 Player Classic, reveal/leaderboard bật, 3 câu; kiểm tra đúng/sai, điểm/tổng điểm/current streak, chuyển câu, leaderboard và kết thúc. User xác nhận tổng quát, chưa có biên bản tick từng mục/config/deployment revision.
- `git diff --check` source sau fix sạch. Agent không chạy Gradle. Warning JDK/SDK không được dùng làm lý do nâng toolchain.

### Còn mở / bàn giao verify rồi UI polish

- Chưa có evidence manual riêng cho unanswered, ACK mất/trễ, duplicate event, reconnect sớm/muộn/đổi câu, pause/resume, reveal × leaderboard/privacy, terminal recovery và smoke Host/Lobby/self-paced. Ma trận mục 5 vẫn là checklist phải verify, không tự tick PASS.
- Backend deployment/APK revision, full lint/CI/E2E/release chưa xác minh. Không đóng BUG-13, N25/N44/N45/M6 hay presence/Practice/public REST privacy/token gates.
- **Lỗi phát hiện thêm:** tổng kết/lịch sử hiển thị sai chuỗi đúng cao nhất; ghi riêng ở mục 9, chưa xác định nguyên nhân hoặc tầng gây lỗi, không kết luận là lỗi backend.
- Phiên sau đọc checkpoint này, verify đúng source hiện tại và ghi evidence thực; nếu làm UI polish/final-result hoặc lỗi max streak (N24.5 mục 9) phải audit, báo khoảng trống và xin duyệt scope trước. Không coi việc có bug này là cho phép mở rộng commit logic hiện tại.

## 9. Lỗi phát hiện thêm — chuỗi đúng cao nhất ở tổng kết/lịch sử hiển thị 0

- **Trạng thái:** `OPEN` — user báo khi kiểm thử happy case Classic sau N24.5; agent chưa tái hiện độc lập hoặc audit nguyên nhân.
- **Phân loại:** sai thống kê hiển thị, **chưa xác định frontend hay backend**. Không đưa vào danh sách lỗi backend khi chưa có bằng chứng.
- **Scope:** ngoài phần current-streak/private personal results sau mỗi câu của N24.5; ghi nhận để audit/fix ở lượt được duyệt tiếp theo. Không kèm UI polish và không tự mở rộng scope hiện tại.

### Cách tái hiện / quan sát do user báo

1. Chơi một lượt Classic, đạt chuỗi đúng cao nhất **3** trong game.
2. Hoàn tất game, xem chỉ số chuỗi đúng ở màn tổng kết cuối.
3. Mở lịch sử chơi của cùng lượt và đối chiếu chỉ số này.

**Mong đợi:** cả hai màn hiển thị chuỗi đúng cao nhất **3** của cả lượt, kể cả khi chuỗi hiện tại ở câu cuối thấp hơn.

**Thực tế:** cả hai màn hiển thị **0** dù trong game đạt chuỗi cao nhất **3**.

### Bằng chứng còn thiếu và hướng kiểm tra lần tới

- Chưa đối chiếu payload/log, tính toán/lưu trữ, response, DTO/domain/mapper hoặc binding UI; chưa quy trách nhiệm cho tầng nào. Chưa có bằng chứng ảnh hưởng điểm hoặc xếp hạng, chưa kiểm chứng các mode khác.
- Khi được duyệt audit, kiểm tra end-to-end source/contract thật: giá trị chuỗi cao nhất của cả lượt → dữ liệu kết quả/lịch sử → mapper → UI hai màn; chỉ phân loại nguyên nhân sau khi có bằng chứng.
- Không dùng `streak` hiện tại của câu cuối trong private result để thay cho chuỗi cao nhất, không hard-code hoặc tự dựng số liệu UI che lỗi.
- Tiêu chí verify sau fix: cùng lượt đạt cao nhất 3 thì cả tổng kết/lịch sử đều là 3; thêm regression khi chuỗi cuối thấp hơn chuỗi cao nhất.
- Test xanh và happy case N24.5 do user xác nhận không đóng phát hiện này hoặc các gate deployment/privacy/E2E/release. Lượt cập nhật này chỉ chuyển/sửa tài liệu, không sửa code hoặc chạy test.
