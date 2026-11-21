# N24 — Kết quả Player, leaderboard và Final Result: bài học

> Phiên 14–15/9/2026. Hoàn thiện phần kết quả của gameplay host-paced/classic: progress trả lời, kết quả giữa câu, leaderboard theo config, pause/resume Player, `game:ended` → Final Result. Unit test/build đã được chủ dự án chạy xanh; commit `3271eda` đã push lên `main`. E2E nhiều máy và các case mất mạng/ACK uncertainty chuyển sang N25.

---

## 1. Thứ tự event là một phần của contract

Backend host-paced phát theo thứ tự:

1. `question:locked`
2. `question:results`
3. flush PostgreSQL
4. `leaderboard:updated` nếu config cho phép
5. chuyển phase server sang `showing_results`
6. câu tiếp theo hoặc `game:ended`

Snapshot `game:state` có thể đến trễ hơn event realtime. Vì vậy reducer không được cho snapshot `question_locked` kéo UI từ Results lùi lại Locked. Với realtime state machine, contract không chỉ là shape payload mà còn là ordering và rule chống regression.

## 2. Visibility phải được chặn trước UI

Chỉ đặt `if (showCorrectAnswer)` quanh một Composable là chưa đủ. Payload stale, snapshot hoặc state giữ từ event trước vẫn có thể bị render ở vị trí khác.

N24 áp rule ngay khi tạo UiState:

- `showCorrectAnswer != true` → xóa `correctAnswers`, xóa distribution, outcome = `HIDDEN`.
- `showLeaderboard != BETWEEN_QUESTIONS` → xóa leaderboard live, rank và score live.
- Với `BETWEEN_QUESTIONS`, bảng chỉ render trong phase Results; sang câu mới phải ẩn.

**Quy tắc:** quyền nhìn dữ liệu là invariant của reducer/domain presentation, không phải chi tiết layout.

## 3. `answer:received` là progress snapshot, không phải đáp án

Payload thật chỉ có `index`, `answered`, `activePlayers`, `serverTime`. Nó không chứa player nào trả lời, không có đúng-sai và progress có thể giảm nếu player disconnect.

Do đó client phải thay snapshot mới nhất theo index, không tự cộng delta và không suy luận kết quả.

## 4. Pause Player đi qua `game:state`

Backend xử lý `game:pause`/`game:resume` ở Host rồi broadcast `game:state`; không có `game:paused`/`game:resumed` riêng cho Player.

Player phải đọc `sessionStatus`:

- paused → khóa card/radio/checkbox/text field/submit và chặn cả Intent trong ViewModel;
- active → chỉ mở lại nếu phase vẫn Question và Player chưa submit;
- Submitted/Locked/Results không được mở input chỉ vì vừa resume.

Khóa UI mà không khóa ViewModel vẫn để automation/double tap gửi intent; khóa ViewModel mà không disable control lại tạo UX giả. Phải có cả hai lớp.

## 5. `reviewMode` và `showCorrectAnswer` không độc lập

Backend `normalizeConfig` có rule:

```text
reviewMode = true → showCorrectAnswer = true
```

Bug test thật: host tắt “Hiện đáp án đúng”, lưu thành công, nhưng mở lại thấy bật vì `reviewMode` mặc định vẫn true và backend normalize bật reveal trở lại.

Fix ở form dùng chung:

- tắt show correct → tắt luôn review;
- bật review → bật luôn show correct;
- Marathon luôn reveal nên show correct read-only.

Bài học mở rộng từ N20: dựng lại form từ ACK là cần thiết để thấy server truth, nhưng UX tốt hơn còn phải phản chiếu các dependency normalize ngay lúc chỉnh để người dùng hiểu patch thực sự sẽ thành gì.

## 6. Final Result dùng transient hand-off có giới hạn rõ

`game:ended` đã mang leaderboard đầy đủ cho happy-case nên N24 không gọi REST lần nữa. `GameResultRepository` interface ở `core:common`, implementation in-memory ở `core:network`; Gameplay lưu trước khi phát navigation effect.

Ưu điểm: không request thừa, Final Result mở ngay. Giới hạn: process recreation làm mất dữ liệu. Khi đó UI phải hiện fallback an toàn, không tự dựng rank/score giả. Nếu sau này cần phục hồi đầy đủ thì thay implementation bằng persistence hoặc REST mà không đổi feature nhờ DIP.

## 7. Phạm vi còn lại

- N25: E2E classic với Host + ít nhất 2 Player Android thật.
- Test mất mạng, disconnect/reconnect, ACK timeout và reconcile bằng `player:sync`.
- Xác minh ba config leaderboard và `showCorrectAnswer=false` trên nhiều thiết bị.
- Chốt M4 sau khi N25 xanh.
- Self-paced/lives/marathon/practice gameplay vẫn thuộc N26–N30.
- Review detail, Activity/history thật và ảnh câu hỏi vẫn ngoài phạm vi N24.
