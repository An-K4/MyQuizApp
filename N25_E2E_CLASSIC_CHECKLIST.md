# N25 — Checklist E2E Classic nhiều thiết bị

> Mục tiêu: chốt M4 bằng một ván Classic hoàn chỉnh với **1 Host + ít nhất 2 Player Android thật**.  
> Trạng thái: test tự động và audit đã chuẩn bị; **không đánh dấu N25 hoàn thành** cho tới khi toàn bộ mục bắt buộc bên dưới đạt.
>
> **Tạm gác N25 — 26/09/2026:** Android đã sửa treo reconnect, bổ sung reconnect theo mạng đã `VALIDATED`, trạng thái thất bại và nút thử lại. Tuy nhiên E2E phát hiện race phía backend: `onLeave` của socket cũ có thể ghi đè trạng thái sau khi socket mới `lobby:join`, khiến Player đã reconnect vẫn bị tính là `disconnected`; Host báo thiếu người và có thể chuyển câu sớm. Android giữ contract `Connected → lobby:join → player:sync`, không thêm delay/double-join workaround. Chờ backend sửa presence theo socket identity/generation rồi retest trước khi chốt N25/M4.

## 1. Chuẩn bị

- [x] Cả ba thiết bị cài cùng một build từ cùng commit.
- [x] Backend thật hoạt động; ghi lại môi trường và thời điểm test.
- [x] Bật Logcat cho các tag socket/network/game nhưng bảo đảm không log cookie, socket token hoặc mật khẩu.
- [x] Chuẩn bị một tài khoản Host.
- [x] Player A đăng nhập bằng tài khoản thật.
- [x] Player B vào bằng guest để phủ cả hai đường join.
- [x] Tắt chế độ tiết kiệm pin cho app trên thiết bị dùng kiểm tra reconnect nếu hệ điều hành can thiệp socket.

## 2. Quiz chuẩn N25

Quiz dùng test cần có tối thiểu bốn câu, theo đúng thứ tự sau để dễ đối chiếu:

- [x] Câu 1: `multiple_choice`, có lựa chọn id hợp lệ kể cả `0` nếu dữ liệu cho phép.
- [x] Câu 2: `multiple_select`, có ít nhất hai đáp án đúng.
- [x] Câu 3: `short_answer`, có đáp án phân biệt được trim/case.
- [x] Câu 4: `long_answer`.
- [x] Có câu dùng `time_limit` để kiểm tra deadline.
- [x] Ảnh lựa chọn không nằm trong phạm vi; ảnh câu hỏi chỉ ghi nhận hiện trạng, không block M4.

Ghi lại:

- Quiz ID/tên: `Test E2E`
- Commit/app version: `820e142`
- Backend/environment: `https://api.myquizz.dpdns.org/v1/`
- Host device: `Vsmart Live 4`
- Player A device: `Samsung Galaxy A21s`
- Player B device: `Medium Phone API 35 (mục 3) - Pixel 6A API 35 (từ mục 4 đến hết)`

## 3. Happy path bắt buộc

### Lobby

- [x] Host tạo phòng Classic thành công.
- [x] Mã phòng dài đúng 6 ký tự và copy được.
- [x] Player A tra mã và vào thẳng lobby khi đã đăng nhập.
- [x] Player B nhập nickname guest và vào lobby.
- [x] Cả ba máy thấy roster thống nhất, không trùng Player.
- [x] Host bắt đầu một lần; các máy cùng chuyển qua countdown/gameplay.
- [x] Không còn lobby sai trên back stack sau khi game bắt đầu.

### Gameplay bốn loại câu

Với từng câu:

- [x] Nội dung, số thứ tự và lựa chọn giống nhau giữa hai Player.
- [x] Host thấy đúng câu hỏi; đáp án Host mặc định bị ẩn sau nút “Xem đáp án”.
- [ ] Đồng hồ các máy không lệch bất thường; UI về 0 chỉ khóa input, server quyết định phase. (Host chậm hơn 1s so với player - vd player còn 20s host hiển thị còn 21s)
- [x] Input Player khóa ngay khi bấm gửi.
- [x] Bấm gửi nhiều lần không tạo double-submit.
- [x] Host nhận progress `đã trả lời/đang hoạt động` đúng. (Khi mới vào phòng, chưa ai trả lời thì hiện 0/0, còn lại hoạt động đúng)
- [x] Player host-paced không hiện đúng/sai từ ACK; chỉ đổi sau `question:results`. (có lẽ đúng, người chơi đầu gửi đáp án phải đợi khi mọi người chơi đã chọn hết đáp án hoặc hết thời gian)
- [o] Event của câu cũ đến muộn không ghi đè progress/kết quả câu mới. (chưa có cơ hội test)

Kiểm tra riêng:

- [x] Multiple choice gửi đúng một id.
- [x] Multiple select giữ thứ tự server và gửi đủ tập id đã chọn.
- [x] Short answer trim khoảng trắng trước khi gửi.
- [x] Long answer nhập/gửi được và bị khóa đúng sau submit.

### Kết thúc

- [x] `game:ended` đưa cả hai Player sang Final Result đúng một lần.
- [x] Gameplay đã kết thúc không còn trên back stack.
- [x] Player hiện tại được highlight đúng. (không highlight, ghi chữ (Bạn) thay thế)
- [x] Điểm/thứ hạng giữa Host, Player A và Player B nhất quán.
- [o] Không crash nếu kết quả transient mất; fallback nói rõ không có dữ liệu, không bịa bảng điểm. (chưa có cơ hội test)

## 4. Ma trận reveal và leaderboard

Chạy tối thiểu ba phòng ngắn (có thể dùng quiz 1–2 câu):

### A. Reveal bật + leaderboard giữa câu

- [x] `showCorrectAnswer=true`.
- [x] `showLeaderboard=between_questions`.
- [x] Đáp án/phân bố chỉ xuất hiện sau Results.
- [x] Leaderboard live chỉ xuất hiện trong Results.
- [x] Sang câu mới không còn dữ liệu reveal/rank stale của câu trước.

### B. Reveal tắt

- [x] `showCorrectAnswer=false` kéo theo trạng thái review phù hợp normalize backend. (chưa hiểu rõ lắm, nhưng hoạt động ổn ở client)
- [x] Player không thấy đáp án đúng.
- [x] Player không thấy distribution có thể làm lộ đáp án.
- [x] Kết quả dùng wording trung tính “đã ghi nhận”.
- [x] Không có dữ liệu nhạy cảm trong Logcat/state dump dùng làm bằng chứng.

### C. Leaderboard cuối trận

- [x] `showLeaderboard=end_only` không hiện rank/score/leaderboard live.
- [x] Final Result vẫn hiển thị theo payload cuối đúng cấu hình backend.

### D. Leaderboard never (khuyến nghị)

- [x] `showLeaderboard=never` không hiện leaderboard live.
- [x] Final Result dùng fallback/visibility đúng, không giữ bảng từ phòng trước.

## 5. Pause/resume

- [x] Host pause khi câu đang active.
- [x] Hai Player nhận trạng thái pause qua `game:state.sessionStatus`.
- [x] Input bị khóa ở cả UI và ViewModel; thao tác chọn/gửi không có tác dụng.
- [x] Host resume.
- [x] Player chưa trả lời và câu còn active được mở lại input.
- [x] Player đã trả lời vẫn khóa sau resume. (đã sửa và xác nhận lại trên thiết bị; snapshot thiếu `answered_questions` không mở lại input khi ACK trước đó đã thành công)
- [x] Deadline sau resume bám mốc mới của server, không tự cộng/trừ ở client.

## 6. Reconnect Player

### Trước khi trả lời

- [x] Tắt mạng Player A khi câu đang active và chưa trả lời.
- [x] UI chuyển sang “đang đồng bộ lại”, giữ câu cũ nhưng khóa input.
- [x] Bật mạng lại.
- [x] Sau `Connected`, client re-emit `lobby:join` rồi `player:sync`.
- [x] Snapshot phục hồi đúng câu/phase/deadline.
- [x] Nếu server xác nhận chưa trả lời và câu còn active, input mở lại.

### Sau khi trả lời

- [x] Player B gửi đáp án thành công rồi tắt mạng.
- [x] Sau reconnect, snapshot phục hồi đúng question state và không còn treo vô hạn ở Android. (còn blocker backend presence race: socket đã sống nhưng Player có thể vẫn mang status `disconnected`)
- [ ] Input không mở lại và không gửi trùng.
- [ ] Điểm cuối không bị cộng hai lần.

### Background/foreground

- [x] Đưa Player xuống nền trong câu rồi mở lại.
- [x] Timer được tính lại theo deadline + server offset.
- [x] State không reset về lobby/connecting vô hạn.

## 7. ACK uncertainty — bắt buộc
NOTE: MÁY YẾU KHÔNG KỊP TÁI HIỆN
Thực hiện vài lần để cố tạo trường hợp mất ACK: bấm gửi rồi tắt mạng ngay lập tức.

- [ ] Input khóa trước khi emit.
- [ ] Khi ACK timeout/not-connected, app không tự retry.
- [ ] UI nói đang xác nhận, không khẳng định “gửi thất bại”.
- [ ] Client gọi `player:sync`.
- [ ] Nếu server đã ghi: snapshot giữ Submitted và khóa input.
- [ ] Nếu server chưa ghi: chỉ mở lại khi snapshot nói chưa có đáp án **và** phase vẫn `question_active`.
- [ ] Không double-submit, không cộng điểm hai lần, không loading vô hạn.

Nếu không tái hiện được trường hợp server chưa ghi, ghi rõ “chưa tái hiện” thay vì đánh Pass.

## 8. Reconnect và disconnect Host

- [x] Tắt mạng Host giữa câu: UI giữ snapshot cũ và chuyển Reconnecting.
- [x] Bật mạng: Host re-emit `lobby:join` và nhận `game:state`. (phải tự ấn kết nối lại)
- [ ] Reconnect trong câu không bịa đáp án đúng; nếu cache không còn, UI nói rõ chưa có answer key. (chưa hiểu lắm, reconnect sẽ miss các socket của player đã trả lời, chỉ khi nào có đáp án mới cập nhật)
- [x] Câu mới tự ẩn đáp án dù câu trước Host đã bấm xem.
- [x] Progress của câu cũ đến muộn bị bỏ. (có lẽ là hành vi của checklist số 3 mục này)
- [x] `io server disconnect` làm Host thoát với thông báo; không nằm vô hạn ở Reconnecting. (chưa hiểu lắm, cần giải thích thêm, có lẽ chưa tái hiện được case này)

## 9. Lệnh Host fire-and-forget

- [x] Pause/resume/end không được coi là thành công chỉ vì hàm emit trả về.
- [x] Bấm nhanh hai lần không gửi hai lệnh trong command guard.
- [x] Nút mở lại sau broadcast xác nhận hoặc sau guard timeout.
- [x] Classic `autoAdvance=true` không hiện nút chuyển câu thủ công.
- [x] Nếu test cấu hình `autoAdvance=false`, chỉ cho chuyển ở phase hợp lệ; countdown không cho bấm. (ấn chốt câu vẫn hiện countdown đếm ngược, điều này có bình thường không?)

## 10. Server disconnect và lỗi fatal
NOTE: KHÔNG TÁI HỆN ĐƯỢC CASE NÀY
- [ ] Đóng/hủy phòng hoặc tạo tình huống server chủ động ngắt.
- [ ] Player nhận effect thoát thay vì reconnect vô hạn.
- [ ] Host nhận effect thoát thay vì reconnect vô hạn.
- [ ] Các code `GAME_TOKEN_INVALID`, `GAME_TOKEN_WRONG_ROOM`, `GAME_ROOM_NOT_FOUND`, `GAME_PLAYER_NOT_FOUND` thoát đúng và có thông báo.
- [ ] Transport disconnect thông thường không bị hiểu nhầm thành fatal.

## 11. Bằng chứng cho mỗi lỗi

NOTE: TÔI KHÔNG QUAN SÁT ĐƯỢC CÙNG LÚC BẰNG ĐÓ THÔNG TIN TẠI 1 THỜI ĐIỂM NÊN CHỈ CÓ THỂ MÔ TẢ LỖI

Khi một mục Fail, ghi đủ:

- Thiết bị/vai trò:
- Thời điểm:
- Commit/app version:
- Cấu hình phòng:
- Câu hỏi/phase:
- Các bước tái hiện tối thiểu:
- Kỳ vọng:
- Thực tế:
- Logcat từ trước lỗi 5–10 giây đến sau lỗi 5–10 giây:
- Ảnh/video nếu lỗi UI hoặc lệch nhiều máy:
- Có tái hiện lần hai không:

Không gửi hoặc commit cookie, socket token, OTP, mật khẩu.

## 12. Gate chốt M4

Chỉ chốt N25/M4 khi tất cả điều kiện sau đạt:

- [x] Một ván Classic hoàn chỉnh chạy với 1 Host + ít nhất 2 Player Android thật.
- [x] Cả bốn loại câu hỏi hoạt động.
- [x] Reconnect trước/sau submit không làm mất state hoặc double-submit.
- [ ] ACK uncertainty không tự retry và được giải quyết bằng sync.
- [x] Pause/resume đúng trên cả hai Player.
- [x] Reveal/leaderboard tuân thủ config, không rò đáp án.
- [x] Final Result nhất quán giữa các máy.
- [ ] Không crash, không treo vô hạn, không lỗi fatal bị retry vô hạn. (treo reconnect Android đã sửa; fatal/server-disconnect chưa tái hiện đầy đủ và backend presence race vẫn còn)
- [x] Unit/regression tests và build chạy xanh.
- [ ] Lỗi phát hiện trong E2E đã có regression test khi có thể mô phỏng ổn định.

Kết quả cuối: `FAIL` — tạm gác; chờ backend sửa presence race khi reconnect và retest các gate còn lại.
Người test: `An`  
Ngày: `26/9/2026`  
Commit đã test: `820e142`
