# N44 — Checklist kiểm thử tích hợp để người test tự đánh dấu

> Đã có kết quả user ghi ngày 08/10/2026 22:00 và báo cáo `N44_E2E_REPORT.md`. Giữ nguyên ghi chú/tick gốc để truy vết; kết luận PARTIAL, không phải mọi case đều PASS. N44/N45/M6 và release gates chưa được chốt.
> Tài liệu thuộc nhánh `docs`. Chạy app từ bản build của `main`, không build nhánh docs. Không bật lại instrumentation N44Session đã gỡ. Google guest/cookie-race investigation đang tạm hoãn theo user, không nằm trong lượt stress test bắt buộc này.

## 1. N25 dùng lại được phần nào?

Dùng lại **kịch bản**, không dùng lại dấu tick hoặc kết quả ngày 26/9:
- `N25_E2E_CLASSIC_CHECKLIST.md` mục 2–5: fixture 4 loại câu, lobby/gameplay/final, reveal/leaderboard và pause/resume. Đã chuyển thành các case B dưới đây, không cần ghi kết quả ở hai file cùng lúc.
- Mục 6–10: reconnect, mất ACK, Host disconnect và fatal. Đã tách thành nhóm C có điều kiện rõ; không bắt người test tự tạo event nội bộ không kiểm soát được.
- Giữ nguyên N25 cũ làm hồ sơ FAIL/tạm gác. Một lượt N44 smoke PASS không đóng các gate reconnect/ACK/fatal N25 chưa được kiểm chứng.
- N44 bổ sung history, ảnh câu hỏi, preview, profile/security, đổi account và result recovery. Kỳ vọng theo N35–N43 hiện tại, không kế thừa fallback cũ của N24 khi REST recovery đã có.

### Bài học đã áp dụng

1. Chỉ có `[ ]` / `[x]`; không dùng `[o]`. Tick nghĩa là **PASS toàn case**, không phải đã thử thao tác.
2. Không dùng “có lẽ đúng”, “chưa hiểu nhưng ổn” làm PASS. Nếu bước trigger chưa xảy ra, ghi NOT RUN + “đã thử nhưng chưa tái hiện trigger”.
3. “Socket nối lại/câu hỏi hiện đúng” chưa chứng minh roster Host, input và điểm đều đúng; kiểm từng dấu hiệu riêng.
4. Tắt mạng sau submit không tự chứng minh mất ACK. Rotate/“Don't keep activities” không tự chứng minh process death. End game bình thường không tự chứng minh fatal/server disconnect.
5. Không yêu cầu xem ba máy cùng một thời điểm. Mỗi lượt tập trung một Player; để Player còn lại chờ, rồi quan sát Host sau. Có thể quay một video ngắn nếu tiện, không bắt buộc Logcat toàn bộ.
6. UI ẩn leaderboard không chứng minh API không lộ dữ liệu. Backend blocker và khả năng quan sát phải được ghi riêng.
7. Không thêm delay/double-join/forced state hay đổi kiến trúc để làm test xanh. Khi sửa code vẫn ViewModel → Use case → Repository.

## 2. Cách ghi kết quả — chỉ cần sửa dòng ngay dưới case

- `[x]` + `PASS`: đã thực hiện đúng bước, có điều kiện/fixture cần thiết và nhìn thấy mọi dấu hiệu đạt.
- `[ ]` + `FAIL`: đã chạy, thực tế trái kỳ vọng; ghi một mô tả ngắn. Chưa biết lỗi Android/backend vẫn được ghi FAIL; chưa tự gán nguyên nhân.
- `[ ]` + `BLOCKED`: thiếu dữ liệu/quyền/thiết bị hoặc thiếu backend fix/hook; nêu lý do.
- `[ ]` + `NOT RUN`: chưa chạy, hoặc chưa tạo/quan sát được trigger cần kiểm. Không đổi thành PASS chỉ vì chưa gặp lỗi.
- `DEFERRED`: chủ động tạm hoãn theo quyết định user; không phải PASS hay miễn trừ release blocker.

Ví dụ: `Kết quả: FAIL — A login B nhưng Profile còn avatar A.` Chỉ tick khi dòng kết quả đã là PASS. Case có nhiều nhánh: ghi từng nhánh; còn nhánh NOT RUN/BLOCKED thì giữ checkbox trống, không biến phần đã PASS thành PASS toàn case.

**Tối thiểu khi gặp lỗi:** ID case + 2–3 thao tác + thực tế nhìn thấy. Baseline ở mục 3 dùng chung, không phải nhập lại cho mỗi lỗi. Ảnh/video/thời điểm là bổ sung nếu có. Agent chịu trách nhiệm xin thêm log an toàn khi cần diagnose; không bắt người test đồng thời bắt event/ACK/server state trên nhiều máy.

## 3. Phiếu lượt test và chuẩn bị

Điền trước; không dùng SHA/thiết bị của N25 cũ làm baseline mới:

| Trường | Người test điền |
| --- | --- |
| Run ID / ngày / giờ / timezone | 8/10/2026 10:00PM |
| Người test | USER AN |
| Android source SHA + APK/build version thực tế | BẢN MỚI NHẤT SAU KHI VỪA COMMIT DOCS |
| Debug/release; cài mới hay nâng cấp | DEBUG |
| Build/unit/lint/CI run id hoặc log đã có | BUILD XANH KHÔNG LỖI |
| Backend môi trường + deployment revision | UNKNOWN + AI XEM LẠI MÔI TRƯỜNG |
| Máy H (Host) / OS | SAMSUNG GALAXY A21S |
| Máy P1 (Player user) / OS | VSMART LIVE 4 |
| Máy P2 (Player guest) / OS; real device hay emulator | EMULATOR HỎNG, DÙNG FRONTEND THAY THẾ DO KHÔNG KIẾM ĐỦ 3 MÁY |
| Mạng; battery restriction có can thiệp không | UNKNOWN |
| Alias account A/B/H/P1, không ghi email/password/token | CHƯA HIỂU TRƯỜNG NÀY |
| Quiz ID hoặc alias + fixture đủ/thiếu gì | |

- [x] Các client nhiều máy dùng cùng APK/source SHA; có ít nhất H + P1 + P2 cho nhóm B. Nếu có emulator thì ghi rõ, không gọi lượt đó là retest toàn bộ gate “máy Android thật” N25. CHỈ CÓ 2 MÁY THẬT, 1 FRONTEND WEB
- [x] Account A và B có dữ liệu riêng dễ phân biệt; có account **disposable local/password** riêng nếu chạy vô hiệu hóa. Không dùng account chính.
- [x] Guest giữ nguyên dữ liệu app/UUID trong lượt test; không clear data hoặc reinstall giữa hai lần kiểm lịch sử guest.
- [x] Fixture quiz: 4 loại câu (`multiple_choice`, `multiple_select`, `short_answer`, `long_answer`); câu có ảnh + không ảnh; thời gian đủ thao tác và có câu để hết hạn. Không yêu cầu sửa ID đáp án bằng API; ID 0 nếu fixture sẵn có thì kiểm thêm.
- [x] History user có played và hosted dễ phân biệt; pagination cần **hơn một trang**. Thiếu fixture nào chỉ BLOCKED case đó, vẫn chạy các case độc lập.
- [x] Trước B02 reveal-off, kiểm tra config cuối mà server đã nhận: tắt Review cùng reveal nếu normalize đòi vậy. Không test chỉ từ toggle chưa lưu. Marathon không dùng làm fixture reveal-off vì mode có rule riêng. KHÔNG THỂ KIỂM TRA TRỰC TIẾP BÊN SERVER VÌ THUỘC THẨM QUYỀN TEAM BACKEND

**Chia lượt cho dễ làm:** A = một máy (session/history/feature); B = ba client (Classic + luồng sau trận); C = case có điều kiện/hỗ trợ; D = smoke mode còn lại. Có thể nghỉ giữa các nhóm; nhóm chạy lại sau thay build/backend phải có Run ID mới.

## 4. Nhóm A — một máy, integration N38–N43

- [x] **A01 — Logout không mở lại màn riêng tư qua Back/tab restore.**
  - Làm: login A → mở Thư viện, Hoạt động, Profile; tại Profile logout → bấm Back và lần lượt mở các tab.
  - Đạt: thông tin A/draft riêng tư không trở lại; gate yêu cầu login ở thao tác user-only. Home và hành vi guest vẫn dùng được, không buộc mọi tab guest đều bị khóa.
  - Kết quả: PASS — ghi chú: tab hoạt động sau khi đăng xuất fallback về hoạt động của guest đã từng hoạt động trên máy

- [x] **A02 — Đổi A → guest → B, dữ liệu theo B.**
  - Làm: sau A01 login B; mở Profile, Thư viện, Hoạt động và thử Back. B có fixture riêng ở mục 3.
  - Đạt: Profile là B; dữ liệu riêng của A không hiện như dữ liệu B. Public quiz chung giữa A/B không được tính nhầm là rò dữ liệu riêng. Không cần restart/relogin để đổi phiên.
  - Kết quả: PASS — ghi chú: 

- [x] **A03 — Offline không tự biến user thành guest.**
  - Làm: đã login B và load màn → tắt mạng → thử tải/refresh tài nguyên → bật mạng có Internet dùng được rồi thử lại.
  - Đạt: lỗi mạng/loading/retry rõ; không tự hiện account guest hoặc nút login thay Profile chỉ do offline. Không yêu cầu mọi nội dung từ server hoạt động offline; dữ liệu hiển thị phải đúng policy cache.
  - Kết quả: PASS — ghi chú: 

- [x] **A04 — Played/hosted và refresh không trộn danh sách.**
  - Làm: account có cả hai role; mở Hoạt động played → hosted → refresh từng role rồi đổi qua lại.
  - Đạt: items đúng vai trò, không đưa played vào hosted/copy lỗi của role này sang role khác; loading/error kết thúc và có retry nếu cần.
  - Kết quả: PASS — ghi chú:

- [x] **A05 — Pagination riêng từng role.**
  - Điều kiện: mỗi role cần hơn một trang; thiếu role nào ghi BLOCKED role đó.
  - Làm: load more played → đổi hosted/load more → quay played → refresh played.
  - Đạt: không lặp/trộn items; hosted không mất cursor vì load played; refresh không để spinner load-more treo, lỗi không khóa retry mãi.
  - Kết quả: PASS — ghi chú: không thấy quay loading, khả năng tốc độ load rất nhanh

- [x] **A06 — Guest history giữ identity cũ, không có hosted.**
  - Làm: guest P2 đã kết thúc một game thật ở nhóm B; mở Hoạt động → đóng/mở lại app, vẫn guest và không clear data.
  - Đạt: played history của guest còn theo cùng identity; không tự sinh guest mới chỉ vì mở tab; không cung cấp hosted history cho guest. Chưa có game guest thì BLOCKED fixture, không PASS danh sách rỗng.
  - Kết quả: PASS — ghi chú: nhìn thấy dữ liệu cũ khi thực hiện case A01

- [x] **A07 — History detail đúng game và đúng người xem.**
  - Làm: mở detail một item played rồi một item hosted; nếu có answers được phép xem thì mở answer sheet.
  - Đạt: game/role đúng; Player chỉ thấy answer sheet của chính mình, không thấy bài Player khác; không crash ở item thiếu dữ liệu và không hiện kết quả bịa.
  - Kết quả: PASS — ghi chú:

- [ ] **A08 — Review disabled không lộ đáp án qua history.**
  - Điều kiện: game đã kết thúc có **effective config** review/reveal disabled, theo B02.
  - Làm: mở history detail/answers bằng user và guest tương ứng.
  - Đạt: chặn/ẩn answer review không được phép; không giữ đáp án từ game trước. PASS này chỉ là UI/luồng history, không xác nhận API privacy đã sửa.
  - Kết quả: NOT RUN — ghi chú: cơ chế ẩn bảng xếp hạng đã hoạt động, chưa test luồng chơi chặn/ẩn answer review

- [ ] **A09 — History đang tải rồi đổi phiên.**
  - Điều kiện: mạng chậm ổn định hoặc fixture khiến summary/answers còn loading đủ để thao tác; không thêm delay vào code.
  - Làm: mở detail/answers A đang loading → Back/tab Profile → logout → login B; mở Hoạt động/detail B. Lặp cho summary và answers nếu tạo được cả hai tình huống.
  - Đạt: response/error cũ không xuất hiện dưới B, không treo loading vì request của A; nếu chưa tạo được loading in-flight, NOT RUN nhánh đó. Unit tests đã có không thay thế kết quả E2E này.
  - Kết quả: NOT RUN — summary: / answers: không tạo được điều kiện mạng

- [x] **A10 — Ảnh câu hỏi và lỗi ảnh an toàn.**
  - Làm: xem quiz có ảnh/không ảnh ở Quiz Detail và Solo Preview; ở nhóm B kiểm lại câu ảnh trên Host/P1/P2. Với fixture ảnh hỏng có sẵn, kiểm thêm lỗi tải ảnh.
  - Đạt: nội dung/ảnh đúng câu, không giữ ảnh câu trước, không crash/che nút trả lời; ảnh hỏng có placeholder an toàn. Không có fixture ảnh hỏng thì ghi NOT RUN phần đó, không bịa URL/đáp án để test. Ảnh lựa chọn không thuộc scope v1 này.
  - Kết quả: PASS — Detail/Preview: / Host/Player: / ảnh hỏng:

- [x] **A11 — Solo Preview 4 types, skip, hết giờ, chơi lại.**
  - Làm: ghi nhận history/play count hiện có nếu UI cung cấp → “Tự chơi thử” quiz 4 types; trả lời, skip một câu, để một câu hết giờ → tổng kết → chơi lại.
  - Đạt: state chuyển đúng, không submit hai lần, chơi lại reset lượt; điểm ghi là ước tính local, không coi là điểm trận server. Không tạo room hay thêm game history. Play count chỉ PASS nếu có số liệu before/after quan sát được; không nhìn được thì NOT RUN phần counter, không bịa kiểm chứng.
  - Kết quả: PASS — chức năng: PASS / history: PASS / play count: NOT RUN

- [x] **A12 — Preview owner edit và Back.**
  - Làm: preview quiz mình sở hữu → vào sửa quiz; preview quiz public của người khác → thử các lối vào tương tự; Back khỏi preview.
  - Đạt: edit chỉ cho owner; không có đường editor giả cho non-owner; Back không vào lobby/gameplay và không tạo game thật.
  - Kết quả: PASS — ghi chú:

- [x] **A13 — Profile delta/save/cancel và avatar.**
  - Làm: sửa một field (ví dụ giới thiệu), giữ phone cũ → Save; sửa tiếp rồi Cancel; chọn ảnh khác → preview → Cancel, rồi chọn/confirm ảnh test.
  - Đạt: save field thay đổi được, Cancel không lưu; avatar preview chưa confirm không thay avatar chính; sau confirm thành công Profile và bottom nav nhất quán, không mất field vừa lưu. Không suy ra API delta đúng chỉ từ UI; contract test là bằng chứng riêng.
  - Kết quả: PASS — field save/cancel: PASS / avatar preview/save: PASS

- [x] **A14 — Đổi password local: validation, sai cũ, thành công giữ phiên.**
  - Điều kiện: account local disposable, biết password cũ; không ghi password vào file.
  - Làm: thử new dưới 8 ký tự/confirm sai/new trùng old → thử password cũ sai → đổi đúng → logout → login bằng password mới. Nếu kiểm password cũ không dùng được, chỉ thử một lần để tránh rate limit.
  - Đạt: invalid không submit thành công; sai cũ báo dễ hiểu và còn phiên; đổi đúng xóa input, giữ login; password mới dùng được. Rời màn/recreate không phục hồi ô password. Không tuyên bố logout mọi thiết bị vì contract không làm vậy.
  - Kết quả: PASS — ghi chú:

- [x] **A15 — Google-only và local liên kết Google không bị nhầm provider.**
  - Làm: mở Account Security với Google-only; nếu có fixture local đã link Google thì mở lại bằng account đó.
  - Đạt: Google-only giải thích không hỗ trợ mutation cần password; local-linked vẫn có chức năng local. Không có fixture linked thì BLOCKED nhánh đó. Đây là provider UI test, không mở lại stress race Google relogin đang DEFERRED.
  - Kết quả: PASS — Google-only: PASS / local-linked: NOT RUN - CHƯA HIỂU RÕ LUỒNG TEST, LOCAL-LINKED LÀ GÌ?

- [ ] **A16 — Deactivate: hủy/sai password, rồi success cleanup.**
  - Điều kiện bắt buộc: account **disposable**, user đồng ý thật sự vô hiệu hóa; nếu không, BLOCKED, không dùng account chính.
  - Làm: mở confirmation rồi hủy; thử sai password; cuối cùng xác nhận đúng trên account disposable → Back/tab/mở lại app → thử login account đã deactivate.
  - Đạt: hủy không vô hiệu hóa, sai password không logout; sau success về guest/Home, không restore dữ liệu account cũ; backend chặn account đã deactivate. Đây là soft deactivate, không kiểm việc xóa toàn bộ DB.
  - Kết quả: NOT RUN — ghi chú: BỎ KHÔNG TEST, KHÔNG MUỐN VÔ HIỆU HÓA TÀI KHOẢN

NOTE: PHÁT HIỆN CASE BIÊN CỦA A03, NẾU VÀO APP KHI KHÔNG CÓ MẠNG NGAY TỪ ĐẦU, CHỈ 2 TAB HOME VÀ THƯ VIỆN CÓ CƠ CHẾ RETRY, TAB HOẠT ĐỘNG VÀ HỒ SƠ SẼ TREO LOADING VĨNH VIỄN CHO ĐẾN LẦN KHỞI ĐỘNG APP CÓ KHI CÓ MẠNG TIẾP THEO

## 5. Nhóm B — một ván Classic với H + P1 user + P2 guest

Dùng fixture 4 types. Để dễ quan sát: ở câu 1 P1 trả lời trước, P2 chờ; nhìn P1 rồi Host, sau đó cho P2 trả lời. Câu 2 đổi vai. Câu 3 pause/resume. Câu 4 kiểm kết thúc. Nếu không đủ thời gian thao tác, tạo phòng mới với time limit phù hợp, không đánh FAIL do chính fixture quá ngắn.

- [ ] **B01 — Join → lobby → 4 types → final.**
  - Làm: H tạo Classic; P1 user join; P2 guest nhập nickname/join; kiểm roster rồi Start. Chơi đủ bốn câu; bấm gửi nhanh hai lần ở một câu.
  - Đạt: mỗi Player một entry, không nhân đôi; cả hai vào game; H progress phản ánh Player trả lời/đang active, ghi riêng số đếm bất thường như 0/0 khi đủ hai Player (không tự coi là đúng); P1/P2 gửi được 4 types và input khóa sau submit; chỉ hiện đúng/sai khi Results, không lộ ngay từ ACK host-paced; final đúng một lần, Back không trở lại gameplay đã kết thúc. Guest/user đều có history sau trận, dùng A06/A07 kiểm tiếp.
  - Kết quả: ? — lobby: PASS / 4 types: PASS / submit: ? - PLAYER RIÊNG LẺ TRẢ LỜI KHÔNG CẬP NHẬT RANK TRONG HOST, CHỈ CẬP NHẬT KHI TẤT CẢ ĐÃ TRẢ LỜI / final & Back: PASS

- [x] **B02 — Reveal OFF + Review OFF (effective config).**
  - Làm: tạo phòng ngắn khác, tắt review và reveal → lưu/đọc lại config cuối; chơi câu, xem Results, Final và history/review.
  - Đạt: Player không thấy answer key/distribution làm lộ đáp án hoặc stale reveal từ game trước; review bị chặn đúng. Nếu config cuối vẫn reveal ON thì chưa đủ điều kiện test này; ghi actual config và NOT RUN phần reveal-off, không tick “hoạt động ổn” mơ hồ.
  - Kết quả: PASS — effective config: / thực tế: chưa rõ lắm reveal, tôi hiểu là tiết lộ đáp án hoặc xem lại câu hỏi với đáp án đúng

- [x] **B03 — Leaderboard between_questions / end_only / never.**
  - Làm: ba phòng ngắn, mỗi phòng một config đã lưu; lần lượt quan sát khi trả lời, Results, câu mới và Final. Nếu thiếu thời gian, ghi kết quả từng cấu hình riêng.
  - Đạt: between_questions chỉ hiện live board trong Results; end_only không hiện rank/score/board live nhưng Final theo config; never không hiện leaderboard Player qua cả live/Final, không giữ bảng phòng trước. Host board phục vụ điều phối là quyền khác, không dùng Host thấy bảng để kết luận Player leak.
  - Kết quả: PASS — between_questions: PASS / end_only: PASS / never: PASS
  - Ghi nhớ: UI PASS không đóng POLICY-RESULT/BUG-16; API public vẫn là backend gate theo audit đã ghi.

- [x] **B04 — Pause/resume: một Player đã submit, một Player chưa.**
  - Làm: P1 submit xong, P2 chưa submit → H pause → P2 thử chọn/gửi → H resume khi câu còn active → P2 trả lời.
  - Đạt: pause khóa cả hai; resume P1 vẫn khóa, không gửi lại; P2 chỉ mở input nếu phase còn cho phép. Deadline theo server sau resume. “Câu còn active” không đủ để mở lại P1 đã submit.
  - Kết quả: PASS — ghi chú:

- [x] **B05 — Timer và manual-next: không suy diễn từ một nhịp hiển thị.**
  - Làm: quan sát một câu tới hết giờ; ở phòng ngắn riêng thử autoAdvance=true rồi false; thao tác next chỉ khi nút được phép.
  - Đạt: hết giờ input khóa; phase chuyển theo server; autoAdvance=true không có thao tác next thủ công trái config; manual-next không double-transition. Có countdown sau chốt/next có thể là phase server hợp lệ, không mặc định là bug. Chênh 1 giây nhìn rời rạc chưa đủ FAIL; nếu lệch kéo dài/cho gửi sau khóa, ghi thời điểm và video nếu tiện để diagnose offset.
  - Kết quả: PASS — timer: PASS / auto: PASS / manual: PASS

- [x] **B06 — Final Result nhất quán và xem Review khi được phép.**
  - Làm: sau B01, so P1/P2 theo alias/điểm/rank với bảng tổng kết mà H thực sự có; dấu “Bạn” chấp nhận thay highlight màu. Mở Review của P1 và P2 ở game review enabled.
  - Đạt: dữ liệu cùng game, không trộn Player; bài làm là của chính viewer; đáp án đúng theo effective config. Không đòi Host có màn Final riêng nếu UI chỉ có board/tổng kết hiện hữu. Không tự tính điểm local thay kết quả server.
  - Kết quả: PASS — final: PASS / Review: PASS

- [x] **B07 — Background/foreground không mất phiên gameplay.**
  - Làm: khi câu active, đưa P1 xuống nền rồi mở lại; P2 ở foreground. Quan sát P1 trước, sau đó H.
  - Đạt: P1 không reset về lobby hoặc treo Connecting; timer theo deadline hiện tại; nếu đã submit thì input vẫn khóa. Roster/presence và chuyển câu sau reconnect còn kiểm riêng C01, không suy ra tất cả đúng chỉ vì màn P1 hiện câu.
  - Kết quả: PASS — ghi chú:

## 6. Nhóm C — chỉ chạy khi có điều kiện; chưa trigger thì KHÔNG tick

Không cần chặn cả lượt A/B chỉ vì nhóm này chưa chạy. Nhưng các gate chưa kiểm vẫn phải giữ mở, không được tuyên bố N25/M4/N45 hoàn tất.

- [ ] **C01 — Player reconnect và backend presence (N25 retest).**
  - Điều kiện: có xác nhận backend presence fix/deployment revision để chốt retest. Chưa có thì BLOCKED theo hồ sơ; nếu chỉ smoke hiện trạng, ghi rõ không phải retest đóng gate.
  - Làm: trong câu còn đủ thời gian, P1 chưa trả lời → tắt/bật mạng → đợi đồng bộ; H phải thấy P1 trở lại active. Cho P2 submit khi P1 vẫn chưa submit; quan sát không chuyển câu sớm chỉ do thiếu P1. Chạy lại với P1 đã submit trước mất mạng; lặp 5–10 lần nếu đủ điều kiện retest.
  - Đạt: câu/snapshot phục hồi; H roster/progress không thiếu P1 đang online; không auto-advance sớm; input đã submit không mở lại; kết quả không cộng điểm lần hai. Ghi số vòng đã chạy, không gọi một vòng xanh là pass race.
  - Kết quả: BLOCKED — backend revision: / số vòng: / trước submit: / sau submit:

- [ ] **C02 — ACK uncertainty, tách hai nhánh server đã ghi/chưa ghi.**
  - Điều kiện: cần nhìn thấy trạng thái đang xác nhận hoặc có fixture/hỗ trợ kỹ thuật thật sự tạo ACK mất/timeout. “Bấm gửi rồi tắt mạng” chỉ là thử trigger, không chứng minh ACK đã mất; máy yếu thao tác không kịp thì NOT RUN.
  - Làm: khi trigger có thật, đợi sync, không tự bấm retry answer liên tục. Nhánh đã ghi: vẫn Submitted/khóa. Nhánh chưa ghi: chỉ mở khi snapshot xác nhận chưa ghi và câu còn active.
  - Đạt: không tự replay answer, không double-score, không treo vô hạn. Phải có bằng chứng trigger và cả hai nhánh để đóng gate; agent/backend team hỗ trợ fixture nếu manual không tạo được, không bắt user săn race mãi.
  - Kết quả: NOT RUN — đã ghi: / chưa ghi: / xác nhận trigger bằng:

- [ ] **C03 — Cold-start/retry exhaustion và hồi phục mạng (N36).**
  - Điều kiện: phải thật sự thấy UI hết retry/có nút Kết nối lại; chỉ tắt mạng vài giây chưa đủ kiểm exhaustion. Giữ phòng live, không dùng token hết hạn/room đã end làm fixture mạng.
  - Làm: đưa một màn H/P1 lobby hoặc H gameplay vào mất kết nối; khi có terminal retry UI, bật lại mạng có Internet usable. Nếu đã tự kết nối thì ghi nhánh auto; thử manual Retry trong vòng riêng khi tự hồi phục không xảy ra và backend/mạng đã sẵn sàng.
  - Đạt: không spinner vô hạn; giữ snapshot phù hợp; có khả năng phục hồi, không nhân đôi roster/listener/answer. Không FAIL auto chỉ vì user bấm Retry quá sớm; không PASS manual nếu chưa bấm được trong trạng thái hợp lệ.
  - Kết quả: PASS — màn đã kiểm: H/P1 / exhaustion: PASS / auto: PASS / manual: PASS

- [x] **C04 — Host reconnect: answer key khác answer progress.**
  - Làm: H đã mở “Xem đáp án”, mất/bật mạng trong câu; sau đồng bộ kiểm key/progress, sang câu mới.
  - Đạt: không bịa answer key khi cache bị mất và snapshot không cung cấp key; có thể dùng key hợp lệ vẫn còn cache. Key = đáp án chuẩn của quiz, KHÔNG phải replay event Player đã trả lời; không đánh FAIL vì hai thứ bị nhầm. Sang câu mới mặc định ẩn key; presence/progress stale phụ thuộc backend được ghi riêng C01.
  - Kết quả: PASS — ghi chú:

- [ ] **C05 — Fatal/server disconnect, không dùng End game giả trigger.**
  - Điều kiện: backend/test harness có thể phát server disconnect/fatal xác định được trên môi trường test, hoặc lỗi tự xuất hiện và có bằng chứng an toàn. Không sửa token/DB của user, không gây lỗi production để săn case.
  - Làm: tạo trigger với người hỗ trợ; quan sát màn/lý do thoát. Transport disconnect thường chỉ kiểm reconnect, không tính là fatal. GAME_TOKEN_INVALID của Host có renewal N36 nên không mặc định phải thoát ngay; Player renewal vẫn là gate N30.
  - Đạt: xử lý đúng loại lỗi, không reconnect vô hạn; Host renewal nếu áp dụng phải bounded. Không có trigger thì NOT RUN/BLOCKED, không tick chỉ vì End game chạy được. Case Host token hết hạn cần fixture riêng, không yêu cầu tự săn thủ công.
  - Kết quả: NOT RUN — trigger/code đã xác nhận: / vai trò: / thực tế:

- [x] **C06 — Process death tại Final Result (N35), không phải xoay màn.**
  - Điều kiện: đã kết thúc game, còn task Final Result trong Recents và backend result fixture đọc được. Có thể cần adb hỗ trợ; không force-stop/clear data.
  - Làm: đưa app nền → nếu dùng adb, `adb shell am kill android.kma.myquizzapp` → mở lại task từ Recents. Xác nhận process đã tạo lại; không lấy rotate/Don't keep activities thay cho bước này. Chưa xác nhận process death thì NOT RUN.
  - Đạt: mất transient result thì REST recovery load kết quả, có loading/error/retry rõ, không bịa rank/score. Có result sẵn qua socket thì không đòi GET thừa. Review socket token transient đã mất có thể báo credential không còn; đó không phải lỗi recovery tổng kết hay lý do logout tài khoản.
  - Kết quả: PASS — process death được xác nhận bằng: ADB / result recovery: PASS / Review: NOT RUN
  NOTE: CASE NÀY ĐÃ TEST TRƯỚC NÊN TICK PASS, KHÔNG PHẢI TEST TẠI THỜI ĐIỂM ĐÁNH PASS

- [ ] **C07 — Mutation kết quả không chắc chắn/cleanup retry.**
  - Điều kiện: môi trường test + fixture timeout sau server write hoặc lỗi cleanup được kiểm soát; không chỉ tắt mạng trước request rồi gọi đó là unknown outcome. Account disposable nếu liên quan deactivate.
  - Làm: với avatar/password/deactivate gặp trạng thái cần xác minh, làm đúng nút verify/cleanup mà UI thực sự cung cấp; không tự lặp mutation. Test từng mutation riêng, không áp chung một giao diện recovery giả.
  - Đạt: không success giả và không tự replay write/DELETE. Avatar verify chỉ đọc; deactivate cleanup retry chỉ dọn local, không DELETE lại. Không tạo được trigger thì NOT RUN, dùng unit/contract tests làm evidence riêng.
  - Kết quả: NOT RUN — avatar: / password: / deactivate: / trigger:

## 7. Nhóm D — smoke các mode, không thay full UAT N49

Dùng config thật của từng mode, có thể chạy từng phòng ngắn; thêm thời gian/fixture riêng để thấy nhánh timeout/elimination. Host self-paced không có một “câu hiện tại chung của phòng”.

- [ ] **D01 — Practice:** create/join → chơi → pause/resume nếu control được mode cho phép → end/result/review. Không đòi tất cả Player có cùng index.
  - Đạt: luồng bình thường không crash/lối vào chết; pause/resume riêng Player chỉ PASS nếu snapshot backend đúng. Theo hồ sơ N29, phần này vẫn backend-blocked nếu chưa có fix/deployment xác minh.
  - Kết quả: BLOCKED - CHƯA FIX, TEST LẠI VỚI PAUSE/RESUME GẶP LỖI CŨ BỊ NHẢY VỀ CÂU ĐẦU — happy path: / pause-resume:
- [x] **D02 — Solo:** feedback riêng từng Player, manual next nếu effective config autoAdvance=false; double tap không nhảy hai câu.
  - Đạt: tiến độ theo Player, người xong trước không ép người còn lại xong; final/history đúng sau game end. Không nhầm mode Solo có room với Solo Preview local ở A11.
  - Kết quả: PASS — ghi chú:
- [x] **D03 — Survival:** trả lời để mất life/hết life; quan sát Player elimination và Host dashboard riêng.
  - Đạt: Player terminal/input đúng; Host live lives/streak/elimination chỉ chốt khi backend event đủ. Happy path Player PASS không đổi Host delta/presence blocker N28.5 thành PASS.
  - Kết quả: PASS - BLOCKER VẪN CÒN, HAPPY PATH CHẠY ỔN TRÊN LUỒNG TỔNG THỂ KHÔNG TÍNH LIVE/STREAK — Player: PASS / Host delta: BLOCKED
- [ ] **D04 — Marathon:** timer trận + timer câu, timeout/finish và kết thúc; nếu có fixture nhiều vòng question bank thì kiểm thêm rollover.
  - Đạt: timer không bị nhầm; Player/Host terminal đúng dữ liệu server. Sync/shuffle/order/rollover chưa có fix thì ghi BLOCKED nhánh tương ứng, không giảm kỳ vọng để pass. Thiếu fixture rollover thì NOT RUN phần đó.
  - Kết quả: NOT RUN — happy path: / timeout/Host: / rollover:

## 8. Gate và phần chủ động không săn trong lượt này

Đây là **hồ sơ từ audit trước**, không khẳng định deployment hiện tại đã/chưa sửa. Chỉ đổi trạng thái khi có revision + retest evidence thật:

| Gate | Hồ sơ/điều kiện hiện có | Cách ghi trong lượt mới |
| --- | --- | --- |
| N25/BUG-04 presence | Socket cũ onLeave có thể ghi disconnected sau new join, Host thiếu Player/auto-advance sớm | C01: BLOCKED đóng gate khi chưa xác minh backend fix; smoke không được ghi là retest PASS |
| N28.5 Host delta/presence | Full board có lives/streak nhưng delta/timeout/presence có khoảng trống | D03/D04 và Host quan sát riêng; không ghi nhầm “mọi payload đều không có lives” |
| N29 pause/resume self-paced | Snapshot riêng từng Player chưa đúng | D01 nhánh pause/resume theo fix/revision; không copy kỳ vọng Classic |
| N30 renewal/sync/order | Player renewal và sync progress/shuffled order chưa đủ | Không tự ép reconnect bằng join mới; ghi gate riêng, không thêm delay/workaround |
| N20.6 active-game resume | Thiếu contract active game | Không bắt tìm room đang chơi sau process death như feature đã có; khác C06 tổng kết trận đã end |
| POLICY-RESULT/BUG-16 | REST public results có thể serialize board dù never | B03 UI PASS vẫn không đóng API privacy; cần backend integration test, không gửi raw auth/response có dữ liệu nhạy cảm |
| N44-GOOGLE-SESSION | User từng gặp guest sau Google success; capture gần nhất không tái hiện | DEFERRED theo user; nếu tự gặp khi chạy A02 ghi FAIL mới, không stress 5–10 vòng riêng để săn lúc này |
| N44-COOKIE | 2/2 reproducer opt-in FAIL đúng invariant, chưa fix | DEFERRED theo user; không bật env diagnostic trong normal gate run, không coi skip là fix |
| N44-TOKEN/N44-ARCH | Token route/SavedStateHandle và Repository trực tiếp ở VM ngoài phạm vi sửa | Architecture/security evidence riêng; không suy ra toàn app clean từ các case UI PASS |

## 9. Tóm tắt để gửi lại agent

Không cần đếm mọi case nếu chưa xong; chỉ liệt kê ID. Không lấy kết quả N25 cũ hay unit XML làm tick của lượt này.

- Run ID / source SHA:
- Nhóm đã chạy A/B/C/D: ĐÃ QUÉT QUA HẾT CÁC CASE
- PASS IDs: AI TỰ TỔNG HỢP
- FAIL IDs + mô tả ngắn: AI TỰ TỔNG HỢP
- BLOCKED IDs + thiếu điều kiện gì: AI TỰ TỔNG HỢP
- NOT RUN IDs + chưa chạy hay chưa tái hiện trigger: AI TỰ TỔNG HỢP
- DEFERRED: N44-GOOGLE-SESSION / N44-COOKIE (trừ khi user đổi quyết định): DEFFERRED
- Ảnh/video/log đã redact nếu có: KHÔNG CÓ
- Việc cần agent/backend hỗ trợ: TỔNG HỢP LẠI VÀ BÁO CÁO
- Kết luận lượt này: PARTIAL / có FAIL / BLOCKED / đã chạy hết phạm vi được duyệt: ĐÃ QUÉT HẾT CẢ FILE TEST, KẾT QUẢ AI TỰ TỔNG HỢP

**Gate N45/M6:** cần kết quả luồng chính N38–N43, build/lint/regression, matrix đủ evidence và xử lý/quyết định release rõ cho mọi blocker/rủi ro. “Không tái hiện”, “không có crash trong một ván” hoặc chỉ UI hide không phải bằng chứng đóng race/privacy gate. Checklist không tự quyết định Go/No-Go.

## Agent review sau lượt user test

Xem `N44_E2E_REPORT.md` cho matrix phân loại và backlog. Giữ nguyên toàn bộ tick/ghi chú gốc ở trên; PASS một nhánh không được hiểu là PASS toàn case.

- Run user ghi: 08/10/2026 22:00; Android APK SHA chưa rõ, backend deployment UNKNOWN; 2 Android máy thật + guest web.
- PASS theo nội dung user ghi: A01–A05, A07, A12–A14, B03–B07, C03–C04, D02. A03 chỉ warm-session offline; A05 có giới hạn evidence pagination.
- PARTIAL: A06 (chưa ghi restart), A10 (chưa rõ nhánh ảnh/bề mặt), A11 (counter chưa chạy), A15 (linked chưa chạy), D03 (Player PASS, Host BLOCKED).
- Cần làm rõ/evidence: B01 progress khác rank; B02 effective config/reveal/review, không thay A08.
- BLOCKED: C01; D01 pause/resume còn quan sát lỗi nhảy về câu đầu (gate N29).
- NOT RUN: A08, A09, C02, C05, C07, D04.
- Chủ động không chạy: A16 deactivate theo lựa chọn user (DEFERRED); không yêu cầu vô hiệu hóa account chính.
- Evidence lịch sử: C06 result recovery đã test trước bằng ADB, Review chưa chạy; không tính là retest lượt mới.
- Lỗi mới ngoài case warm-session: N44-OFFLINE-BOOT — mở app offline từ đầu, Hoạt động/Hồ sơ treo loading, theo user chỉ hồi phục khi khởi động lại có mạng.
- Google/cookie race vẫn DEFERRED; privacy/session/realtime/backend gates chưa đóng.
- Quyết định user: lưu báo cáo N44 và chuyển nghiên cứu UI N46; chưa chốt N45/M6 hoặc release.

## 10. Nguồn và kinh nghiệm đã dùng để viết

- `N25_E2E_CLASSIC_CHECKLIST.md`: giữ nguyên hồ sơ ngày 26/9, gồm các ghi chú user chưa hiểu/chưa tạo được trigger/khó thu bằng chứng.
- `knowledgement/n25_knowledgement.md`: DNS/validated network, retry exhaustion, ACK unknown outcome và presence old/new socket.
- `knowledgement/n22_n23_knowledgement.md`, `knowledgement/n24_knowledgement.md`: submit/phase/pause/reveal/config normalize; đối chiếu lại với mốc mới, không lấy giới hạn N24 làm hành vi N35 hiện tại.
- `myquizz-review-backend-ke-hoach-50-ngay.md`: Implementation Details N25, N35–N37 và N38–N44; N36 có Host renewal và auto-reconnect khi validated network trở lại.
- `BACKEND_BUG_REPORT.md`, `AGENTS.md`, `N44_INTEGRATION.md`: blocker/API privacy, Clean Architecture, trạng thái DEFERRED và scope integration.

Không re-audit backend/deployment trong lượt viết checklist này. Không sửa code, thêm log hoặc triển khai test harness.
