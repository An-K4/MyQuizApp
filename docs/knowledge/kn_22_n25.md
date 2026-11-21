# N25 — E2E Classic, reconnect và presence race: bài học

> Phiên 15–26/9/2026. Audit Host/Player gameplay, bổ sung unit test, chạy E2E Classic với 1 Host + 2 Player Android thật và sửa các lỗi Android xác nhận được. Kết quả cuối: **FAIL — tạm gác**. Android reconnect đã phục hồi đúng question state; M4 chưa chốt vì backend còn presence race và một số gate E2E chưa tái hiện đủ.

---

## 1. Kết quả N25

Đã xác minh trên thiết bị thật:

- Happy path Classic với 4 loại câu.
- Progress, reveal và leaderboard theo config.
- Pause/resume không mở lại input của Player đã submit.
- Final Result và kết thúc trận.
- Mất DNS/mạng rồi reconnect có thể phục hồi đúng câu hiện tại.

Android đã harden:

- Snapshot `QUESTION_ACTIVE` cũ không kéo Results lùi.
- Trạng thái answer đã accepted/pending sống qua snapshot pause/resume thiếu `answered_questions`.
- Transport disconnect giữ UI ở reconnecting; `io server disconnect` thoát với lý do rõ.
- `GameSocketClient` dùng `ConnectivityManager`, chỉ reconnect khi network có `INTERNET + VALIDATED`.
- Cạn số lần retry phát `CLIENT_RECONNECT_EXHAUSTED`; Player map sang `GameConnection.RECONNECT_FAILED` và hiển thị nút **Kết nối lại**.
- Retry đóng connection cũ, tạo connection mới, rồi thực hiện lại `Connected → lobby:join → player:sync`.
- Đã có regression tests cho Player và Host.

N25 chưa pass vì:

- Backend presence race có thể khiến Player đã reconnect vẫn bị tính là `disconnected`.
- ACK uncertainty chưa tái hiện thủ công đủ cả hai nhánh server đã ghi/chưa ghi.
- Fatal/server-disconnect chưa tái hiện đầy đủ trên E2E.

## 2. Diagnose trước, fix sau

Lỗi ban đầu chỉ lộ ra ngoài bằng `CLIENT_CONNECT_FAILED`, dễ dẫn tới kết luận sai rằng token hết hạn hoặc phòng đã kết thúc. Log cause chain an toàn cho thấy nguyên nhân thật là DNS:

```text
EngineIOException: websocket error
└─ UnknownHostException
   └─ GaiException: EAI_NODATA
```

Bài học:

1. Thu timeline dài từ lúc transport đứt đến hết retry.
2. Log exception type/message/cause chain trước khi map về mã generic.
3. Không suy đoán handshake/token khi chưa thấy HTTP/socket rejection thật.
4. Trình bày chẩn đoán và hướng sửa với user trước khi áp speculative fix.
5. Không log hoặc chép lại `socketToken`, cookie, Authorization header hay auth payload.

## 3. Transport readiness khác socket state

`Socket.connected == false` không cho biết thiết bị đã có Internet dùng được. Gọi `connect()` liên tục khi DNS/network chưa sẵn sàng chỉ tiêu hết finite retry và để UI treo ở trạng thái mơ hồ.

Thiết kế N25:

- Theo dõi default network bằng `ConnectivityManager.NetworkCallback`.
- Chỉ coi network sẵn sàng khi có `NET_CAPABILITY_INTERNET` và `NET_CAPABILITY_VALIDATED`.
- Khi validated network chuyển false → true, khởi động lại connect nếu socket chưa connected.
- Sau 5 connect error liên tiếp, phát terminal event thay vì ở `RECONNECTING` vô hạn.
- UI có Retry thủ công để mở một connection cycle mới.
- Hủy callback trong `awaitClose` để tránh leak.

## 4. ACK timeout là unknown outcome

Không tự gửi lại đáp án chỉ vì ACK timeout. Server có thể đã ghi answer nhưng ACK bị mất; retry có thể double-submit hoặc double-score.

Luồng đúng vẫn là:

1. Khóa input trước emit.
2. Nếu ACK không chắc chắn, giữ khóa và yêu cầu `player:sync`.
3. Dùng snapshot authoritative để xác định answer đã được ghi hay chưa.
4. Chỉ mở lại nếu server nói chưa ghi và phase vẫn cho phép trả lời.

Gate này phải được retest sau khi backend presence ổn định.

## 5. Snapshot đúng không chứng minh presence đúng

Sau reconnect, Player đã nhận đúng question snapshot nhưng Host đôi lúc vẫn thấy chỉ còn một Player active. Đây không phải lỗi render Android.

Race backend đã audit:

1. Socket cũ disconnect và bắt đầu `onLeave()` bất đồng bộ.
2. Socket mới kết nối rồi gửi `lobby:join`.
3. Join mới có thể thấy status cũ vẫn `connected`, hoặc vừa sửa thành `connected`.
4. `onLeave()` cũ hoàn tất muộn hơn và ghi status về `disconnected`.
5. Socket mới vẫn nhận `game:state`, nhưng `pendingAnswers()`/answer progress chỉ tính Player có status `connected`.
6. `activePlayers` bị thiếu và Classic có thể auto-advance ngay khi Player còn lại trả lời.

Ngoài ra, active-game `onLobbyJoin()` gọi `onSync()` nhưng không broadcast roster refresh cho Host; `onSync()` chỉ emit snapshot và không sửa presence.

## 6. Hướng fix đúng thuộc backend

Backend cần gắn presence với socket identity/generation hoặc active socket count:

- Lưu socket ID/generation mới nhất theo `playerSessionId`.
- `onLeave(oldSocket)` chỉ đánh dấu offline nếu socket đó vẫn là generation hiện hành hoặc không còn socket active thay thế.
- Chỉ gọi `maybeAdvanceAfterLeave()` sau khi xác nhận Player thực sự offline.
- Reconnect trong active game phải refresh roster/presence cho Host.
- Nên bổ sung integration test cho thứ tự `old disconnect → new join → old onLeave completes`.

Android giữ contract:

```text
Connected → lobby:join → player:sync
```

Nếu backend sửa nội bộ mà không đổi protocol, Android không cần đổi chức năng. Nếu backend thêm ACK cho `lobby:join`, Android có thể đợi ACK rồi mới `player:sync`. Không thêm delay hoặc double-join để che race server-authoritative.

## 7. Test và bằng chứng

- `GameViewModelTest`: stale snapshot, submitted qua pause/resume, reconnect exhaustion → Retry → Connected → join + sync và các reducer/ACK case liên quan.
- `HostGameViewModelTest`: 6 case cho host state machine, transport disconnect và server disconnect.
- Checklist chuẩn: `N25_E2E_CLASSIC_CHECKLIST.md`.
- Checklist cuối giữ `FAIL — tạm gác`; không đánh dấu N25/M4 hoàn thành.

## 8. Việc tiếp theo

- Chuyển critical path sang N26–N30 gameplay self-paced.
- Khi backend presence fix sẵn sàng, quay lại N25 và chạy lặp reconnect 5–10 lần với 1 Host + 2 Player.
- Retest ACK uncertainty, fatal/server disconnect, roster Host, all-answered/auto-advance và không double-score.
- N20.6 vẫn blocked bởi endpoint `GET /games/active`.
