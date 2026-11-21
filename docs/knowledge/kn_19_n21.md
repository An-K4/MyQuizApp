# N21 — Host game console (classic): bài học

> Phiên 11–13/9. Màn điều khiển của host cho chế độ classic. **Phạm vi đổi có chủ đích**: kế hoạch gốc để N21 là màn chơi của player, nhưng N20 kết thúc bằng `HostGamePlaceholder` nên làm màn host trước mới liền mạch ⇒ kéo N31–N33 của Tuần 7 lên đây.

---

## 1. Tài liệu backend KHÔNG phải nguồn sự thật — bài học đắt nhất phiên này

`docs/components/socket.doc.ts` khai lựa chọn trả lời là `{ id, text, image }`. Tôi tin nó và đọc `option.text`. Kết quả khi test thật: **mọi lựa chọn chữ đều hiện "(ảnh)"**, vì `text` luôn null nên rơi vào nhánh fallback.

Sự thật nằm ở ba chỗ khác nhau, tất cả đều đồng thuận:

| Nguồn | Nói gì |
| --- | --- |
| `game.type.ts` | `answer_options: Array<{ id: number; option_text: string }> \| null` |
| `quiz.repository.ts` | `options.map((option, index) => ({ id: index, option_text: option }))` |
| `backend/README.md` | "`answer_options` **has exactly one stored shape**" |
| `game.socket.ts` | forward **nguyên xi** `q.answer_options`, không map lại |

Và frontend web né được bug này vì `QuestionStage.vue` đọc `option.option_text`.

**Quy trình từ nay** — muốn biết shape thật của một payload socket, đọc theo đúng thứ tự này, KHÔNG đọc file docs:

1. Chỗ **lưu** vào CSDL (`*.repository.ts`) — shape gốc.
2. Chỗ **emit** (`game.socket.ts`) — xem có map lại hay forward thẳng.
3. Chỗ **frontend web đọc** — client đang chạy thật, là bản kiểm chứng chéo tốt nhất.
4. `*.doc.ts` chỉ để tham khảo tên event, **không** để tin tên field.

**Điểm tự phê**: tôi đã ghi nhận "web đọc `option_text`" ngay từ lúc audit N20, nhưng vẫn chọn theo `text` của docs khi viết DTO. Manh mối đã có trong tay mà không dùng — đây là lỗi quy trình, không phải lỗi thiếu thông tin.

## 2. Payload đa hình thì KHÔNG được dùng `@Serializable data class`

`game.doc.ts` viết lựa chọn có thể là "either `[{ id, option_text }]` rows **or plain strings**" (quiz đời cũ). DTO cũ của tôi là `data class` ⇒ gặp mảng chuỗi thuần sẽ **ném lỗi parse**, `GameEventMapper` biến thành `Failed(CLIENT_PARSE_ERROR)` và **rơi trọn sự kiện `host:question`**.

Hậu quả nếu không phát hiện: màn host đứng im, không có câu hỏi, **không crash, không log gì đáng chú ý** — loại bug tốn cả buổi để truy.

Quy tắc: field nào tài liệu/code nói "có thể là A hoặc B" thì nhận vào `JsonElement` rồi parse tay. Cụ thể đã làm:

- `JsonObject` → `id` đọc `"id"`, thiếu/null thì **fallback vị trí**; text đọc `option_text` rồi mới tới `text`.
- Không phải object → dùng **vị trí làm id**, khớp đúng cách backend sinh id (`{ id: index }`).
- Chuỗi rỗng quy về null để UI không phải phân biệt hai thứ như nhau.

## 3. Package phẳng: tra tên trước khi thêm model

Thêm `AnswerOption` vào `core.common.model` gây `Redeclaration` với class cùng tên trong `Quiz.kt`, **và** làm hỏng luôn serializer của `Question` (`Serializer has not been found for type ...AnswerOption`) — một lỗi sinh ra ba dòng báo lỗi ở hai file, dễ hiểu nhầm là lỗi serialization.

Đổi thành `PublicAnswerOption` và ghi KDoc phân biệt rõ: bản của `Quiz.kt` là **hàng trong CSDL** (`id: Long`), bản mới là **payload realtime** (`id: String` vì backend khai id kiểu tự do). Gộp hai model làm một sẽ bắt một phía ép kiểu sai lệch.

## 4. UI dựa trên danh sách lựa chọn phải có nhánh cho câu tự luận

Đáp án đúng được vẽ bằng cách in đậm lựa chọn đúng. Câu `SHORT_ANSWER`/`LONG_ANSWER` **không có lựa chọn nào** ⇒ bấm "Xem đáp án" đổi nhãn nút nhưng dưới đó không có gì thay đổi, nhìn y hệt nút hỏng. Dữ liệu vẫn về đủ, chỉ là không có chỗ hiển thị.

Bài học rộng hơn: mỗi khi render dữ liệu qua một **collection**, hỏi "collection này rỗng thì màn hình còn lại gì?". Rỗng hợp lệ (câu tự luận) khác rỗng do lỗi — phải phân biệt và nói rõ, ở đây là "không kèm đáp án mẫu".

## 5. Lệnh không có ack thì xác nhận bằng broadcast

Chỉ `lobby:config-update` và `question:answer` có ack. `game:next/pause/resume/end` là fire-and-forget. Nên mọi nút điều khiển không thể chờ "lệnh thành công" — chúng chỉ khóa tạm (`COMMAND_GUARD_MS = 800`) rồi đợi broadcast kế tiếp đổi state. Cùng triết lý với `game:start` ở N20.

## 6. Nút không nên tồn tại khi server sẽ từ chối

Classic mặc định `autoAdvance=true`, và `game:next` khi đó trả 409 `GAME_ADVANCE_NOT_ALLOWED`. Thay vì hiện nút rồi báo lỗi, **ẩn hẳn nút** (`isManualAdvanceVisible`). Cùng nguyên tắc đã chốt ở N20 cho field config bị locked: điều server không cho thì UI đừng mời người dùng thử.

## 7. Reconnect giữa câu mất đáp án — hạn chế của server, phải nói ra

`game:state` không kèm `correct_answer`. Reconnect giữa lúc câu đang mở ⇒ host mất đáp án tới hết câu đó. Không vá được ở client (cache theo index chỉ giúp khi đã nhận được một lần). Cách xử lý: hiện thông báo rõ "chưa có đáp án cho câu này — sẽ có lại từ câu tiếp theo" thay vì để nút im lặng không phản hồi. **Giới hạn đã biết phải được nói ra, không được che.**

## 8. Chốt phạm vi ảnh (13/9)

| Hạng mục | Quyết định | Căn cứ |
| --- | --- | --- |
| **Ảnh cho lựa chọn trả lời** | **Bỏ hẳn** | Schema Zod + INSERT của backend cứng ở `{ id, option_text }` ⇒ chưa từng tồn tại ở bất kỳ tầng nào. Field `image` đã gỡ khỏi model/parser/UI để không thành nợ chết. Quiz chỉ dùng lựa chọn chữ. |
| **Ảnh câu hỏi** | **Ghi nợ tới đợt polish** | `question_image` backend **có đủ**, chảy qua cả REST và socket, đã tới `PublicQuestion.questionImage`. Chỉ thiếu phần render ở Android. Nợ tồn tại từ **M2**. |

Khi làm phần ảnh câu hỏi, phạm vi thật là **mọi màn có câu hỏi** (chơi, host, review, preview) chứ không riêng màn host, và cần kiểm tra Coil đã có trong version catalog chưa. Cũng chưa có cơ chế nhận biết "câu hỏi này chỉ có ảnh, không có chữ" — phải chốt quy ước hiển thị cho trường hợp đó.

## 9. Nợ để lại sau N21

- Chưa có test cho parser lựa chọn mới — nên thêm 3 case: `{id, option_text}`, chuỗi thuần, và `id = 0` (bẫy truthiness mà backend cảnh báo).
- Màn kết thúc trận còn là bảng gọn, màn kết quả thật thuộc **N24**.
- Self-paced (`host:player-progress`, `question:awaiting_next`) ngoài phạm vi, thuộc Tuần 6.
- Loạt typo tiếng Việt trong comment nhiều file, chờ dọn một lượt.
- Ca test N20 còn tồn: A4, B1, D6.
