# MyQuiz Backend — Bug Report

> Báo cáo này mô tả lỗi theo góc nhìn người dùng: **bấm gì, màn hình nào, mong đợi thấy gì và thực tế thấy gì**.
>
> File này là tài liệu độc lập: team backend không cần mở thêm file kế hoạch, source Android hoặc tài liệu khác. Các mục “đã tái hiện” ghi kết quả quan sát trên thiết bị; các mục “xác định qua audit” ghi đầy đủ kết quả mà backend hiện tại sẽ tạo ra và chuỗi thao tác để team backend tự xác nhận.
>
> Thứ tự ưu tiên: lỗi đã tái hiện trên thiết bị thật → lỗi nghiêm trọng cần backend kiểm tra thêm → phần còn thiếu tính năng.

---

# I. Lỗi đã tái hiện trên thiết bị thật

## BUG-01 — Practice: Host Pause/Resume làm Player quay về câu đầu nhưng câu trả lời lại được chấm cho câu đang chơi dở

- **Severity:** `BLOCKER`
- **Mode:** Practice

### Điều kiện ban đầu

- Quiz có ít nhất 5 câu.
- Phòng đang chạy bình thường.
- Player đã trả lời 3 câu và đang nhìn thấy câu 4.

### Performed actions — Chuỗi hành động thực hiện

1. Trên máy Host, bấm **Tạm dừng**.
2. Chờ màn Player chuyển sang trạng thái tạm dừng.
3. Trên máy Host, bấm **Tiếp tục**.
4. Quan sát câu hỏi xuất hiện trên máy Player.
5. Trên máy Player, chọn đáp án của câu đang hiển thị và bấm **Trả lời**.

### Expected output

- Sau khi Host bấm **Tiếp tục**, Player vẫn nhìn thấy câu 4.
- Nội dung câu hỏi và các lựa chọn phải giống hệt trước khi tạm dừng.
- Câu trả lời phải được chấm cho câu 4.
- Sau đó Player chuyển sang câu 5.

### Actual output

- Sau khi Host bấm **Tiếp tục**, Player bị đưa về giao diện câu 1.
- Player chọn đáp án cho câu 1 đang hiển thị, nhưng backend lại chấm đáp án đó cho câu 4.
- Sau khi trả lời, Player nhảy thẳng sang câu 5.
- Nếu phòng bật xáo trộn câu hỏi, nội dung câu hiện lại còn có thể không khớp thứ tự riêng của Player.

### Ghi chú kỹ thuật cho backend

Trạng thái gửi sau Pause/Resume đang dùng vị trí câu chung của phòng thay vì vị trí câu riêng của từng Player. Với self-paced, mỗi Player cần nhận lại đúng câu và đúng thứ tự shuffle của chính mình.

---

## BUG-02 — Host không thấy bất kỳ thay đổi nào khi Player bị timeout

- **Severity:** `HIGH`
- **Mode:** Survival, Marathon và các mode self-paced có giới hạn thời gian câu.

### Điều kiện ban đầu

- Tạo phòng có thời gian trả lời ngắn, ví dụ 5–10 giây.
- Host đang mở màn theo dõi tiến độ.
- Player đang ở một câu chưa trả lời.

### Performed actions — Chuỗi hành động thực hiện

1. Không thao tác gì trên máy Player.
2. Chờ đồng hồ câu hỏi về `0`.
3. Chờ Player tự chuyển sang trạng thái timeout/câu tiếp theo.
4. Quan sát dòng của Player trên màn Host.

### Expected output

Ngay sau timeout, Host phải thấy dữ liệu của Player thay đổi:

- số câu đã làm tăng lên;
- số câu sai tăng lên;
- vị trí câu hiện tại thay đổi;
- lives giảm nếu mode có lives;
- trạng thái chuyển sang bị loại nếu lives về `0`.

### Actual output

- Máy Player hiển thị timeout và tiếp tục luồng chơi.
- Màn Host không thay đổi.
- Số câu đã làm, số câu sai, vị trí câu, lives và trạng thái đều giữ nguyên dữ liệu cũ.
- Host chỉ có thể thấy dữ liệu mới sau một event khác hoặc khi game kết thúc.

---

## BUG-03 — Survival: Player mất mạng nhưng số mạng trên Host không thay đổi

- **Severity:** `HIGH`
- **Mode:** Survival

### Điều kiện ban đầu

- Tạo phòng Survival với 3 mạng.
- Player và Host đều đang ở trong trận.
- Host đang nhìn thấy Player có 3 mạng.

### Performed actions — Chuỗi hành động thực hiện

1. Trên máy Player, chọn một đáp án sai.
2. Bấm **Trả lời**.
3. Chờ máy Player hiện kết quả và số mạng mới.
4. Quan sát số mạng của cùng Player trên màn Host.
5. Lặp lại với một câu sai khác.

### Expected output

- Sau lần sai đầu tiên, Player và Host cùng hiển thị 2 mạng.
- Sau lần sai tiếp theo, Player và Host cùng hiển thị 1 mạng.
- Thay đổi phải xuất hiện trên Host ngay sau khi backend chấm câu.

### Actual output

- Player hiển thị đúng số mạng mới.
- Host vẫn giữ số mạng cũ lấy từ lúc bắt đầu/reconnect.
- Điểm và tiến độ có thể cập nhật, nhưng lives trên Host không cập nhật theo.

---

## BUG-04 — Reconnect có thể làm Host coi Player đang online thành mất kết nối và chuyển câu sớm

- **Severity:** `BLOCKER`
- **Mode:** Classic/host-paced

### Điều kiện ban đầu

- Có 1 Host và ít nhất 2 Player trong trận.
- Một câu hỏi đang mở và chưa phải tất cả Player đều trả lời.

### Performed actions — Chuỗi hành động thực hiện

1. Tắt Wi-Fi/dữ liệu di động trên một máy Player.
2. Chờ máy Player báo mất kết nối.
3. Bật mạng lại và chờ Player tự kết nối lại vào đúng câu đang chơi.
4. Không trả lời ngay trên Player vừa reconnect.
5. Quan sát danh sách Player và thời điểm chuyển câu trên Host.
6. Lặp lại chuỗi mất mạng/kết nối lại vài lần.

### Expected output

- Player reconnect phải trở lại trạng thái đang kết nối.
- Host phải tiếp tục tính Player đó là người đang chơi.
- Câu chỉ được đóng/chuyển khi Player đó đã trả lời hoặc hết thời gian.

### Actual output

- Player reconnect và nhìn thấy đúng câu hiện tại.
- Host vẫn có thể coi Player là mất kết nối.
- Backend không chờ câu trả lời của Player vừa reconnect và có thể chuyển câu sớm.

---

## BUG-05 — Tra phòng từng trả payload lồng sai làm ứng dụng không mở được phòng

- **Severity:** `MEDIUM`
- **Phạm vi:** màn nhập mã phòng

### Điều kiện ban đầu

- Có một phòng hợp lệ đang ở lobby.
- Người chơi có mã phòng đúng.

### Performed actions — Chuỗi hành động thực hiện

1. Mở ứng dụng ở máy Player.
2. Nhập mã phòng vào ô **Nhập mã phòng**.
3. Bấm **Tham gia**.

### Expected output

- Ứng dụng hiển thị thông tin phòng.
- Người chơi có thể tiếp tục nhập tên hoặc vào lobby.

### Actual output

- Response tra phòng có cấu trúc lồng thêm một cấp `session` ngoài dự kiến.
- Client theo contract thông thường không đọc được dữ liệu phòng và từng crash khi tra mã hợp lệ.
- Client hiện đã phải giữ mapping riêng cho response này.

### Đề nghị

Chuẩn hóa response tra phòng thành một lobby object nhất quán. Nếu sửa shape, cần báo trước để các client bỏ mapping tương thích cũ.

---

## BUG-06 — Lựa chọn trả lời trong tài liệu socket không khớp dữ liệu thực tế

- **Severity:** `LOW`
- **Phạm vi:** câu trắc nghiệm

### Điều kiện ban đầu

- Quiz có câu multiple choice với lựa chọn dạng chữ.
- Host bắt đầu trận.

### Performed actions — Chuỗi hành động thực hiện

1. Trên Host, bấm **Bắt đầu**.
2. Chờ câu trắc nghiệm xuất hiện.
3. Quan sát nội dung các lựa chọn trên màn Host.

### Expected output

- Mỗi lựa chọn hiển thị đúng nội dung chữ đã nhập khi tạo quiz.
- Dữ liệu thực tế phải khớp schema được mô tả trong tài liệu socket.

### Actual output

- Tài liệu mô tả một tên field, nhưng dữ liệu thực tế dùng tên khác và có trường hợp dùng chuỗi thuần.
- Client triển khai đúng theo tài liệu từng hiển thị mọi lựa chọn thành “(ảnh)” hoặc không đọc được cả câu hỏi.

### Đề nghị

Chuẩn hóa một shape duy nhất cho lựa chọn và cập nhật đồng thời runtime payload, type và tài liệu socket.

---

# II. Lỗi nghiêm trọng cần backend xác nhận bằng test

## BUG-07 — `player:sync`/Reconnect có thể trả sai câu hoặc không có câu cho Player self-paced

- **Severity:** `BLOCKER`
- **Mode:** Practice, Solo, Survival, Marathon khi bật shuffle.

### Điều kiện ban đầu

- Tạo phòng self-paced và bật **Xáo trộn câu hỏi**.
- Player đã trả lời vài câu.

### Performed actions — Chuỗi hành động thực hiện

1. Ghi lại nội dung câu Player đang nhìn thấy.
2. Tắt mạng trên máy Player.
3. Bật mạng lại và chờ Player reconnect.
4. So sánh câu sau reconnect với câu trước khi mất mạng.
5. Trả lời câu đang hiển thị và quan sát câu kế tiếp.

### Expected output

- Player trở lại đúng câu trước khi mất mạng.
- Nội dung và thứ tự câu không thay đổi sau reconnect.
- Backend chấm đúng câu đang hiển thị.

### Actual output — kết quả backend hiện tại xác định qua audit

- `player:sync` đã lấy đúng `player.current_question_index`, nhưng `snapshot()` đọc `questions[index]` từ question bank gốc thay vì dùng cùng `orderedQuestionsFor()` như luồng gửi/chấm câu self-paced.
- Khi bật shuffle, Player có thể nhận sai nội dung câu hoặc sai thứ tự lựa chọn dù index nhìn có vẻ đúng; câu đang hiển thị có thể không phải câu backend sẽ chấm.
- Với Marathon đã đi qua hơn một vòng question bank, index tiến độ có thể lớn hơn tổng số câu. Snapshot chưa modulo index nên có thể trả `question=null` dù trận vẫn đang active.
- Snapshot phải dùng đúng shuffled order/option seed của từng Player; Marathon chỉ modulo khi truy cập question bank nhưng vẫn giữ progress index thật để tính tiến độ.

---

## BUG-08 — Marathon có thể kết thúc sau một vòng câu hỏi dù đồng hồ tổng vẫn còn

- **Severity:** `BLOCKER`
- **Mode:** Marathon

### Điều kiện ban đầu

- Quiz có ít câu, ví dụ 3 câu.
- Marathon có tổng thời gian đủ dài để chơi nhiều hơn 3 câu.

### Performed actions — Chuỗi hành động thực hiện

1. Bắt đầu Marathon.
2. Trả lời hoặc chờ timeout lần lượt hết 3 câu.
3. Quan sát màn Player khi kết thúc câu cuối của question bank.
4. Kiểm tra đồng hồ tổng còn thời gian hay không.

### Expected output

- Nếu đồng hồ tổng vẫn còn, Player phải quay vòng lại question bank và tiếp tục chơi.
- Chỉ kết thúc khi hết điều kiện thời gian/kết thúc của Marathon.

### Actual output — kết quả backend hiện tại xác định qua audit

- Nhánh timeout có thể coi việc đi hết question bank là đã hoàn thành.
- Player có thể kết thúc Marathon sớm dù đồng hồ tổng vẫn còn.

---

## BUG-09 — Reconnect ở màn kết quả manual-next có thể làm mất nút “Câu tiếp theo”

- **Severity:** `HIGH`
- **Mode:** self-paced với tự động chuyển câu tắt.

### Điều kiện ban đầu

- Tạo phòng self-paced với **Tự động chuyển câu** tắt.
- Player vừa trả lời xong và đang nhìn thấy màn kết quả có nút **Câu tiếp theo**.

### Performed actions — Chuỗi hành động thực hiện

1. Không bấm **Câu tiếp theo**.
2. Tắt mạng trên máy Player.
3. Bật mạng lại và chờ reconnect.
4. Quan sát màn Player sau khi đồng bộ xong.

### Expected output

- Player vẫn ở màn kết quả của câu vừa trả lời.
- Nút **Câu tiếp theo** vẫn xuất hiện và bấm được.

### Actual output — kết quả backend hiện tại xác định qua audit

- Trạng thái đồng bộ có thể ghi đè màn kết quả.
- Player có thể mất nút **Câu tiếp theo** hoặc bị đưa sang trạng thái câu khác.

---

## BUG-10 — Survival: timeout làm hết mạng nhưng Host không nhận trạng thái bị loại

- **Severity:** `HIGH`
- **Mode:** Survival

### Điều kiện ban đầu

- Player chỉ còn 1 mạng.
- Câu hiện tại có giới hạn thời gian.

### Performed actions — Chuỗi hành động thực hiện

1. Không trả lời trên máy Player.
2. Chờ đồng hồ về `0`.
3. Quan sát màn bị loại trên Player.
4. Quan sát status/lives của Player trên Host.

### Expected output

- Player hiển thị đã bị loại.
- Host đồng thời hiển thị Player **Đã bị loại** và `0` mạng.

### Actual output — kết quả backend hiện tại xác định qua audit

- Backend có thể giảm lives về `0` nhưng không thông báo trạng thái bị loại cho Host.
- Host tiếp tục giữ status/lives cũ.

---

## BUG-11 — Player vào muộn hoặc reconnect không xuất hiện ngay trên Host

- **Severity:** `HIGH`
- **Mode:** self-paced

### Điều kiện ban đầu

- Phòng đang active và Host đang mở dashboard.
- Phòng cho phép Player vào muộn, hoặc một Player cũ đang reconnect.

### Performed actions — Chuỗi hành động thực hiện

1. Cho Player mới nhập mã và vào phòng đang chơi; hoặc cho Player cũ reconnect.
2. Không trả lời câu nào trên Player đó.
3. Quan sát danh sách Player trên Host.
4. Với trường hợp disconnect, tắt mạng Player và quan sát status trên Host.

### Expected output

- Player mới/reconnect xuất hiện ngay trên Host.
- Khi mất mạng, status chuyển sang **Mất kết nối**.
- Khi kết nối lại, status chuyển về **Đang chơi**.

### Actual output — kết quả backend hiện tại xác định qua audit

- Host không nhận bản cập nhật ngay khi presence thay đổi.
- Player thường chỉ xuất hiện sau lần trả lời đầu tiên.
- Nếu không có câu trả lời mới, dashboard Host có thể giữ dữ liệu cũ vô thời hạn.

---

## BUG-12 — Host reconnect giữa câu không còn xem được đáp án đúng

- **Severity:** `HIGH`
- **Mode:** Classic/host-paced

### Điều kiện ban đầu

- Một câu đang mở.
- Trước khi mất mạng, Host có thể bấm **Xem đáp án**.

### Performed actions — Chuỗi hành động thực hiện

1. Tắt mạng trên máy Host trong lúc câu đang mở.
2. Bật mạng lại và chờ Host reconnect.
3. Bấm **Xem đáp án** cho câu hiện tại.

### Expected output

- Host vẫn xem được đáp án đúng của câu đang mở sau reconnect.

### Actual output — kết quả backend hiện tại xác định qua audit

- Host khôi phục được câu hỏi nhưng không nhận lại đáp án đúng.
- Chức năng xem đáp án chỉ hoạt động lại từ câu tiếp theo.

---

## BUG-13 — Player reconnect ở màn kết quả không khôi phục được kết quả vừa trả lời

- **Severity:** `HIGH`
- **Mode:** Classic/host-paced

### Điều kiện ban đầu

- Player vừa trả lời xong.
- Màn đang hiển thị kết quả đúng/sai trước khi sang câu mới.

### Performed actions — Chuỗi hành động thực hiện

1. Tắt mạng trên máy Player khi màn kết quả đang hiển thị.
2. Bật mạng lại trước khi câu tiếp theo bắt đầu.
3. Chờ đồng bộ hoàn tất.

### Expected output

- Player nhìn lại đúng kết quả của câu vừa trả lời.
- Điểm câu, đúng/sai và đáp án được phép reveal phải giống trước khi mất mạng.

### Actual output — kết quả backend hiện tại xác định qua audit

- Dữ liệu khôi phục không đủ để dựng lại màn kết quả.
- Player chỉ có thể thấy trạng thái chờ câu tiếp theo.

---

## BUG-14 — Có thể tạo hoặc bắt đầu phòng từ quiz không có câu hỏi

- **Severity:** `MEDIUM`

### Điều kiện ban đầu

- Có một quiz không chứa câu hỏi.

### Performed actions — Chuỗi hành động thực hiện

1. Mở quiz rỗng.
2. Bấm **Tạo phòng chơi**.
3. Hoàn tất màn cấu hình phòng.
4. Trên Host Lobby, bấm **Bắt đầu**.

### Expected output

- Backend từ chối tạo hoặc bắt đầu phòng.
- Host nhận thông báo rõ ràng rằng quiz chưa có câu hỏi.

### Actual output — kết quả backend hiện tại xác định qua audit

- Backend chưa chặn chắc chắn quiz có `0` câu.
- Game có thể đi vào trạng thái không có câu để hiển thị.

---

## BUG-15 — Player mất kết nối đúng lúc game kết thúc có thể không vào được Final Result

- **Severity:** `HIGH`
- **Mode:** tất cả mode

### Điều kiện ban đầu

- Player đang ở trong trận.
- Kết nối Player bị mất ngay trước hoặc đúng lúc backend kết thúc game.

### Performed actions — Chuỗi hành động thực hiện

1. Tắt mạng Player ngay trước khi Host kết thúc game hoặc trước khi Player cuối cùng hoàn thành.
2. Chờ backend chuyển session sang `finished`.
3. Bật mạng và để Player reconnect.
4. Quan sát điều hướng trên Player.

### Expected output

- Player nhận được trạng thái terminal hoặc có REST fallback để mở Final Result.
- Nếu mode cho review, token hợp lệ vẫn cho phép tải answer sheet sau game.

### Actual output — kết quả backend hiện tại xác định qua audit

- Socket middleware từ chối handshake khi session đã `finished`/`cancelled` và trả `GAME_ROOM_NOT_FOUND`.
- Player đã bỏ lỡ `game:ended` không còn đường socket để lấy lại terminal state; Android hiện phải thoát Gameplay thay vì điều hướng sang Final Result.
- Cần chốt contract terminal recovery: cho phép một snapshot terminal có kiểm soát hoặc cung cấp REST endpoint lấy kết quả của chính Player sau reconnect.

---

## BUG-16 — API kết quả public làm lộ leaderboard dù Host cấu hình không cho xem

- **Severity:** `HIGH` — lộ dữ liệu bị cấu hình ẩn.
- **Mode:** tất cả mode có `flow.showLeaderboard=never`.
- **Phát hiện:** audit N35; Android đã phòng thủ UI nhưng không thể bảo vệ dữ liệu ở tầng API.

### Điều kiện ban đầu

- Tạo phòng với `config.flow.showLeaderboard = "never"`.
- Chơi và kết thúc trận để database có bảng điểm cuối.
- Không cần cookie, socket token hoặc quyền Host.

### Performed actions — Chuỗi hành động thực hiện

1. Gọi public endpoint `GET /v1/games/{id}/results` bằng `gameId` của trận.
2. Quan sát `data.results.session.config.flow.showLeaderboard`.
3. Quan sát `data.results.leaderboard`.

### Expected output

- Backend tự enforce visibility theo config và viewer/role.
- Khi `showLeaderboard=never`, response Player/public không chứa full leaderboard; hoặc endpoint yêu cầu credential có quyền rõ ràng.
- Không dựa vào client để bỏ dữ liệu nhạy cảm sau khi đã gửi xuống mạng.

### Actual output — kết quả backend hiện tại xác định qua audit

- Route `/games/:id/results` hiện public.
- `getResults()` luôn gọi `repo.getLeaderboard(gameId)` và trả toàn bộ bảng điểm, không kiểm tra `showLeaderboard` hoặc danh tính người gọi.
- Chỉ cần đoán/biết `gameId` là có thể đọc rank, tên và điểm dù Host đã chọn ẩn bảng.
- Android N35 đã đọc `session.config.flow.showLeaderboard` và loại leaderboard trước khi state tới UI; đây chỉ là defense-in-depth, dữ liệu vẫn xuất hiện trong HTTP response.

### Đề nghị

- Ưu tiên enforce visibility tại backend trước khi serialize response.
- Nếu Host vẫn cần full result, tách contract theo role/credential thay vì một endpoint public trả chung.
- Bổ sung integration test: `showLeaderboard=never` + unauthenticated/Player request không nhận full leaderboard; Host có quyền nhận dữ liệu theo policy sản phẩm.

---

## BUG-17 — Thống kê từng câu có thể hiển thị nhiều thẻ cùng nhãn khi Player được xáo trộn thứ tự riêng

- **Severity:** `MEDIUM` — số đếm theo câu vẫn được gom, nhưng nhãn vị trí gây hiểu sai báo cáo Host.
- **Mode:** self-paced có `shuffleQuestions=true` (Solo, Practice, Survival, Marathon tùy config).
- **Phát hiện:** tái hiện trên màn chi tiết lịch sử N39 và xác nhận qua audit `getQuestionStats()`.

### Điều kiện ban đầu

- Quiz có ít nhất 2 câu.
- Phòng self-paced bật xáo trộn câu hỏi.
- Có từ 2 Player trở lên để mỗi Player có thể nhận thứ tự câu riêng.

### Performed actions — Chuỗi hành động thực hiện

1. Cho các Player chơi và trả lời cùng một trận.
2. Kết thúc trận.
3. Host mở **Hoạt động → Đã tổ chức → Chi tiết trận**.
4. Quan sát phần **Thống kê từng câu**.

### Expected output

- Mỗi câu hỏi thật xuất hiện đúng một thẻ.
- Nhãn “Câu N” phải dựa trên một thứ tự ổn định của quiz snapshot, không phụ thuộc Player nào nhìn thấy câu đó ở vị trí nào.
- Số lượt trả lời và số lượt đúng được tổng hợp cho đúng `question_id`.

### Actual output — đã tái hiện và xác nhận qua audit

- Có thể xuất hiện nhiều thẻ cùng nhãn, ví dụ hai thẻ đều ghi **Câu 2**, dù chúng là hai `question_id` khác nhau.
- `getQuestionStats()` đang `GROUP BY question_id` nên không nhân thẻ theo từng lượt trả lời; phần đếm vẫn được gom đúng theo câu hỏi thật.
- Tuy nhiên query gắn nhãn bằng `min(question_index)`. Với self-paced, `questionsAsPlayed()` shuffle tiếp bằng seed riêng `gameId + playerId`, nên hai câu khác nhau có thể cùng nằm ở index 1 của hai Player khác nhau.
- Android không thể gộp theo `question_index`: làm vậy sẽ trộn số liệu của hai câu hỏi khác nhau.

### Đề nghị

- Giữ aggregate theo `question_id`.
- Map `question_id` về vị trí ổn định từ `quiz_snapshots.snapshot_data.questions` hoặc trả thêm một field nhãn/thứ tự canonical riêng.
- Không suy ra vị trí canonical bằng `min(question_index)` từ answer rows của Player.
- Bổ sung integration test với ít nhất hai Player self-paced có shuffled order khác nhau; response phải có `question_id` duy nhất và nhãn/order canonical không trùng.

---

# III. Thiếu tính năng/contract — không gọi là bug

## FEATURE-01 — Host chưa xem được thời gian còn lại riêng của từng Player Marathon

### Performed actions — Chuỗi hành động thực hiện

1. Tạo phòng Marathon cho phép vào muộn.
2. Cho Player A vào từ đầu.
3. Cho Player B vào muộn hơn.
4. Quan sát dashboard Host.

### Expected output nếu sản phẩm cần tính năng này

Host thấy thời gian còn lại riêng của A và B.

### Actual output

Host chỉ thấy tiến độ/điểm; không có deadline riêng của từng Player nên không thể hiển thị countdown chính xác.

---

## FEATURE-02 — Chưa có luồng “quay lại phòng đang chơi” sau khi mở lại ứng dụng

### Performed actions — Chuỗi hành động thực hiện

1. User đang ở trong một game chưa kết thúc.
2. Đóng ứng dụng.
3. Mở lại ứng dụng.
4. Tìm phòng đang chơi trên Home/Hoạt động.

### Expected output nếu sản phẩm cần tính năng này

Có nút **Tiếp tục phòng đang chơi** và user quay lại đúng vai trò Host/Player.

### Actual output

Không có đủ API để ứng dụng tìm active session và xin lại thông tin kết nối cần thiết.

---

## FEATURE-07 — Chưa có endpoint cấp lại socket token cho Player đang ở trong trận

- **Mức độ ảnh hưởng:** `BLOCKER` cho N30 resume/token.
- **Phạm vi:** Player đã join, token hết hạn hoặc bị mất sau reconnect/process recreation.

### Performed actions — Chuỗi hành động thực hiện

1. Player join phòng và nhận socket token.
2. Để token hết TTL hoặc mô phỏng `GAME_TOKEN_INVALID` ở lần handshake tiếp theo.
3. Khi game vẫn đang active, thử reconnect vào đúng PlayerSession.
4. Thử dùng lại `POST /games/{code}/join` để xin token mới.

### Expected output nếu sản phẩm cần resume an toàn

- Có endpoint renewal riêng cho Player đã tồn tại.
- Endpoint xác thực đúng user/guest và đúng PlayerSession rồi cấp token mới cho cùng session.
- Renewal hoạt động khi game đang active, không bị chặn bởi `allowLateJoin` hoặc `maxPlayers` vì đây không phải Player mới.
- Response phân biệt rõ session/player không còn tồn tại, game đã kết thúc và credential không hợp lệ.

### Actual output — kết quả backend hiện tại xác định qua audit

- Backend chỉ có `POST /games/{id}/host-token` cho Host; Player token chỉ được cấp từ `POST /games/{code}/join`.
- `joinGame()` kiểm tra game đã bắt đầu/`allowLateJoin` và phòng đầy **trước khi** tìm PlayerSession cũ. Vì vậy Player hợp lệ vẫn có thể nhận `GAME_ALREADY_STARTED` hoặc `GAME_ROOM_FULL` khi chỉ muốn xin lại token.
- Android hiện buộc coi `GAME_TOKEN_INVALID` là fatal và thoát khỏi phòng; không thể refresh đúng chỉ bằng thay đổi client.
- `SOCKET_TOKEN_TTL` là cấu hình môi trường (`.env.example` hiện là `6h`), nên lỗi thường xuất hiện khi reconnect hoặc process recreation sau thời gian dài, không phải khi socket đang kết nối liên tục.

### Contract backend cần chốt

- Thêm endpoint player-token/renew-token hoặc điều chỉnh contract tương đương, tách hoàn toàn khỏi semantics late join.
- Hỗ trợ cả user đăng nhập và guest theo cơ chế định danh an toàn đã thống nhất.
- Quy định rõ có cho renew khi session `finished` để tải Final Result/Review hay không.
- Không trả token trong URL/query và không yêu cầu client log hoặc lưu token lâu dài.

---

## FEATURE-03 — Chưa có danh sách Hoạt động chung cho cả phòng đã host và đã chơi

### Performed actions — Chuỗi hành động thực hiện

1. User từng host một số phòng và tham gia một số phòng khác.
2. Mở tab **Hoạt động**.
3. Thử xem một danh sách chung theo thời gian.

### Expected output nếu sản phẩm cần tính năng này

Một danh sách phân trang thống nhất gồm cả hai vai trò.

### Actual output

Backend chỉ cung cấp các danh sách theo vai trò riêng; không có một cursor/order chung để tạo feed chính xác.

---

## FEATURE-04 — Section Featured chưa có nút “Xem thêm” đúng dữ liệu

### Performed actions — Chuỗi hành động thực hiện

1. Mở Home có section Featured.
2. Tìm cách mở danh sách đầy đủ các quiz Featured.

### Expected output nếu sản phẩm cần tính năng này

Bấm **Xem thêm** mở danh sách Featured có phân trang.

### Actual output

Chưa có filter/endpoint public để tải đúng tập Featured với cursor ổn định.

---

## FEATURE-05 — Chưa có danh sách chủ đề quiz do backend quản lý

### Performed actions — Chuỗi hành động thực hiện

1. Mở màn Khám phá.
2. Mở bộ lọc chủ đề.
3. So sánh các chủ đề hiển thị với dữ liệu quiz thực tế.

### Expected output nếu sản phẩm cần tính năng này

Danh sách chủ đề lấy từ backend và luôn khớp dữ liệu production.

### Actual output

Client phải dùng danh sách chủ đề cố định và chỉ biết chủ đề mới khi tình cờ gặp trong dữ liệu.

---

## FEATURE-06 — Chưa thể xóa cover hoặc description của quiz về trạng thái trống

### Performed actions — Chuỗi hành động thực hiện

1. Mở một quiz đã có cover/description.
2. Bấm **Chỉnh sửa**.
3. Xóa cover hoặc xóa toàn bộ description.
4. Bấm **Lưu** và mở lại quiz.

### Expected output nếu sản phẩm cần tính năng này

Field đã xóa trở về trạng thái trống.

### Actual output

Contract hiện tại không phân biệt rõ “không sửa field” với “xóa field”, nên dữ liệu cũ có thể được giữ lại.

---

# IV. Thứ tự xử lý đề xuất

1. `BUG-01` — Pause/Resume làm Player self-paced hiển thị và trả lời sai câu.
2. `BUG-04` — Reconnect presence race và chuyển câu sớm.
3. `BUG-16` — Endpoint kết quả public làm lộ leaderboard đã bị Host cấu hình ẩn.
4. `BUG-17` — Thống kê self-paced shuffle gắn nhãn câu hỏi không ổn định.
5. `FEATURE-07` — Contract cấp lại Player socket token để N30 có thể triển khai an toàn.
6. `BUG-07`, `BUG-08` — Snapshot self-paced/shuffle/Marathon không nhất quán.
7. `BUG-02`, `BUG-03`, `BUG-10` — Đồng bộ progress/lives/elimination cho timeout và answer.
8. `BUG-09` — Reconnect làm mất trạng thái chờ Next.
9. `BUG-11` — Dashboard Host không refresh theo presence.
10. `BUG-12`, `BUG-13`, `BUG-15` — Reconnect không phục hồi đầy đủ Host/Player/terminal state.
11. Các lỗi mức Medium/Low và nhóm thiếu tính năng còn lại.
