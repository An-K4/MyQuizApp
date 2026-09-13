# N22–N23 — Gameplay Player host-paced/classic: bài học

> Phiên 13/9/2026. Hoàn thiện happy case màn chơi player cho classic: hand-off từ lobby, state machine realtime, input 4 loại câu, typed answer ACK, timer offset và reconnect/resync. Build compile đã qua sau một hotfix; chủ dự án đã review happy case và chủ động để test toàn diện về sau.

---

## 1. Implementation mới là contract, tài liệu AsyncAPI chỉ để tham khảo

Audit trước code phát hiện `socket.channels.ts` mô tả ACK `question:answer` như luôn có `is_correct`/`score_earned`, nhưng `game.socket.ts` host-paced thật chỉ trả:

- `accepted`
- `isLate`
- `lives`
- `eliminated`
- `serverTime`

`isCorrect`, `scoreEarned`, `totalScore`, `streak`, `correct_answer` chỉ xuất hiện khi self-paced và cấu hình cho phép reveal. Vì vậy N22–N23 không hiển thị hay suy luận đúng/sai từ ACK host-paced.

**Quy tắc:** trước khi đóng DTO socket, đọc handler thật trong `game.socket.ts`, đường ghi dữ liệu và frontend đang chạy. File docs chỉ xác nhận ý nghĩa/tên event, không được ưu tiên hơn implementation.

## 2. ACK timeout không đồng nghĩa request thất bại

Socket có thể đã gửi request và server đã ghi đáp án, nhưng callback ACK bị mất hoặc về sau timeout. Nếu client mở lại input hay tự retry thì sẽ tạo duplicate và làm UX sai.

State machine an toàn:

1. Khóa input trước khi emit.
2. ACK success → giữ submitted.
3. Timeout/not-connected/duplicate/locked/too-late → vẫn giữ khóa, chuyển sang “Đang xác nhận câu trả lời”.
4. Emit `player:sync`.
5. Đọc `game:state.player.answered_questions`.
6. Chỉ mở lại khi snapshot xác nhận câu hiện tại chưa được ghi và phase còn `question_active`.

Đây là mẫu chung cho mọi command có side effect nhưng ACK không chắc chắn: **reconcile từ server truth, không retry mù**.

## 3. Submitted của một player khác QuestionLocked của cả phòng

Sau khi một người gửi đáp án, phòng vẫn có thể ở `question_active` để chờ người khác. Nếu dùng chung một phase “locked” thì client dễ nhầm trạng thái cá nhân với trạng thái room.

N22–N23 tách:

- `Question`: input còn mở cho player này.
- `Submitted`: player này đã gửi/đang xác nhận, input khóa.
- `Locked`: server đã khóa câu cho cả phòng.
- `Results`: server công bố kết quả câu.

Tách hai trục giúp reconnect phục hồi đúng: snapshot room vẫn `question_active` nhưng answered snapshot tồn tại thì UI phải vào `Submitted`, không vào `Question`.

## 4. Snapshot player là chìa khóa của reconnect

`game:state` không chỉ có phase/question/deadline. Backend còn trả `player.answered_questions` với `question_id`, `question_index`, `answer`, `is_late`, `answered_at`.

Nếu bỏ phần player như DTO cũ, client không thể biết ACK timeout trước đó đã được server ghi hay chưa. N22–N23 mở rộng snapshot domain/network và dùng `answerFor(snapshot.index)` để dựng lại submitted cùng đáp án đã chọn.

Hotfix compile sau review: `GameSnapshot` dùng field `index`, không có `currentQuestionIndex`; `currentQuestionIndex` thuộc `PlayerStateSnapshot`. Bài học là phải phân biệt index room snapshot với index nằm trong player payload, không đoán tên property khi nối hai model.

## 5. One-shot effect phải phát trước khi tự hủy collector

Trong `PlayerLobbyViewModel`, `game:started` được xử lý ngay bên trong coroutine đang collect socket. Nếu gọi `eventJob.cancel()` rồi mới `_effect.send(...)`, coroutine hiện tại đã bị cancel và `send()` có thể ném `CancellationException`; navigation biến mất dù event đã tới.

Thứ tự đúng:

1. phát `NavigateToGame`/`Exit` effect;
2. disconnect socket;
3. hủy collector.

Quy tắc này áp dụng cho mọi ViewModel tự kết thúc chính stream đang gọi nó.

## 6. Boundary typed giữ JSON khỏi rò lên presentation

Contract cũ `submitAnswer(rawAnswerJson: String)` buộc UI/domain tự biết wire format. Contract mới dùng:

- `PlayerAnswer.SingleChoice(optionId)`
- `PlayerAnswer.MultipleSelect(optionIds)`
- `PlayerAnswer.Text(value)`
- `Result<AnswerAck>`

Chỉ `PlayerGameSocketRepositoryImpl` chuyển chúng thành `{answer: ...}`. Nhờ vậy compiler kiểm soát loại đáp án, test boundary dễ hơn và self-paced N26 có thể dùng lại ACK typed.

## 7. Timer hiển thị không được điều khiển phase server

Deadline dùng ISO tuyệt đối của server và offset `serverTime - localNow`. `endsAt == null` nghĩa là không giới hạn, phải ẩn timer. Khi UI đếm về 0 chỉ khóa input; không tự chuyển sang results vì server có thể xử lý phase trễ hơn do network/timer scheduling.

Server broadcast `question:locked`/`question:results` mới là tín hiệu chuyển phase authoritative.

## 8. Giới hạn đã biết và phạm vi tiếp theo

- Reconnect vào `showing_results` không thể replay đầy đủ đúng/sai/stats vì snapshot không chứa toàn bộ `question:results`; UI chỉ được hiện “Đang chờ câu tiếp theo”, không tự suy luận.
- `game:ended` hiện mới đưa Gameplay về finished; navigation và `FinalResultScreen` thuộc N24.
- `answer:received`, player leaderboard và kết quả giữa câu đầy đủ thuộc N24.
- Self-paced/lives/marathon/practice thuộc N26–N30.
- Ảnh câu hỏi vẫn là nợ polish; ảnh lựa chọn đã bỏ hẳn theo quyết định N21.
- Test mapper/ACK/validation cơ bản đã thêm; ViewModel/Compose/E2E toàn diện được chủ dự án chủ động hoãn.
