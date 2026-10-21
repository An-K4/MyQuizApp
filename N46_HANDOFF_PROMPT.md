# Prompt bàn giao — N46 sau countdown và kế hoạch logic N24.5

> Sao chép phần từ “Bạn tiếp nhận…” bên dưới vào phiên mới. Baseline: local main `b005df3`, docs cập nhật sau `7b6fd82`; backend local `0c5908f` (#21). Kiểm tra lại git, không coi ref local là SHA APK/deployment/test. N24.5 mới là kế hoạch, chưa tích hợp client.

---

Bạn tiếp nhận công việc **N46 — polish UI Android của dự án MyQuizApp** từ agent trước.

Dự án local:
`E:\Adr_Projects\MyQuizApp`

Kết nối:
- **Aki MCP Server from local Shell & FileSystem**: đọc/ghi dự án trên máy tôi.
- **GitHub**: đọc backend thật tại `Ntd1411/myquizz` khi cần xác minh contract. Nếu connection báo cần reauth thì báo tôi, không retry liên tục. Local mirror `server/myquizz/backend` nếu có phải kiểm tra revision; không gọi đó là upstream/deployment mới nhất.
- Ảnh thiết kế nằm trong **design/screens** và **design/components**, chỉ local và đã ignore. Không lấy ảnh đính kèm phiên cũ thay cho việc mở đúng ảnh màn mới.

## 1. Trước khi bắt đầu

- Kiểm tra **git status, branch và lịch sử commit** để xác nhận baseline, thay đổi chưa commit/untracked. Không sửa đè công việc của tôi.
- `main` dành cho source/config/tests; `docs` dành cho tài liệu.
- **Không tự commit, push, checkout, merge, cherry-pick hoặc rewrite history.** Nếu đang ở docs và cần sửa source, báo tôi checkout main; nếu chỉ đọc source có thể dùng `git show main:<file>`.
- Nếu đang ở main, đọc tài liệu bằng `git show docs:<tên-file>`; không tạo bản sao tài liệu trên main. Tại thời điểm bàn giao, user đang ở docs; phải kiểm tra lại, không giả định đã về main.

Đọc theo thứ tự:
1. **AGENTS.md** — quy ước, phối hợp và bài học.
2. **N46_UI_PROGRESS.md** — nguồn bàn giao chính; đặc biệt checkpoint mới mục **5b** (5a giữ lịch sử) và evidence/backlog mục 6.
3. **N24_5_CLASSIC_PERSONAL_RESULTS_PLAN.md** — kế hoạch logic tiếp theo, chưa triển khai; rồi **myquizz-review-backend-ke-hoach-50-ngay.md** — tóm tắt hiện tại, N44–N46 và backlog liên quan.
4. **project_structure.md** — cấu trúc/vị trí code hiện tại.
5. **quiz-app-android-design-document-v2.md** — tham khảo, không phải contract tuyệt đối.

Nếu tài liệu mới nhất chưa commit/không đọc được, báo tôi. Không âm thầm suy diễn từ bản cũ. Không sửa docs khi vẫn ở main.

## 2. Điểm bàn giao

Baseline source cuối phiên: **local main `b005df3`**; docs trước `7b6fd82` dừng ở `992b8c6`. Không fetch/xác nhận remote hoặc deployment từ ref local.

Các commit đã thêm:
- `463248a`: Player Classic question/4 types/IME.
- `2250b4c`: Chơi thử sửa footer/IME câu trả lời dài, không đổi logic local.
- `d72fcf6`: Host Classic question → results, stats chart/full board, bỏ active tracker và results question card.
- `9b8e0a6`: Player feedback 2s → summary, default results 5s theo backend cũ (explicit config không đổi), smart-cast fix.
- `b005df3`: Countdown chung + initial preparing shell chống flash UI cũ.

Các quyết định UI trước đó vẫn giữ theo N46_UI_PROGRESS; user duyệt mắt thường không thay thế test toàn diện. N44 PARTIAL/N46 WIP, N45/M6/release chưa chốt; privacy/presence/session/Practice và Google/cookie DEFERRED giữ nguyên.

**Việc tiếp theo đã lập kế hoạch:** backend local `0c5908f` (#21) có private Classic results/streak + safe snapshots. Xếp **N24.5** nối tiếp N22–N24, không đổi N47–N50. Android b005df3 **chưa có listener/model/mapper/reducer mới**. Đọc kế hoạch chi tiết và xin user review/checkout main trước source work; không bắt đầu trên docs. Sau N24.5 mới quay lại audit kết quả cuối Host/Player → review/Home; chưa duyệt scope triển khai UI cuối trận.

## 3. Những quyết định cuối cần giữ

### Tạo phòng / Biệt danh khách
- Tạo phòng giữ 5 mode từ descriptor/server bằng chip + pacing hint, không ép 2 mode theo ảnh 15. Shared field/switch/number/choice bo góc; dropdown bảng xếp hạng **full width bằng field**; footer tạo cố định, form cuộn. Giữ validation và create→host-token/retry.
- Biệt danh khách theo ảnh 16 nhưng **không avatar/chọn ảnh**; giữ badge mã phòng đã truyền. Validation đúng hiện tại là **1–50 ký tự sau trim**, không 20 theo ảnh. Counter theo NicknameValidator, không truncate raw input.
- Preview GuestNickname nằm cuối screen, không file riêng; đã bỏ BoxWithConstraints không dùng scope.

### Hai phòng chờ
- Shared helper **feature/lobby/.../presentation/components/LobbyComponents.kt**: palette/avatars/initials/status badges. Không import ComponentColors internal core:ui sang feature.
- Host: cả card mã phòng/header và roster cuộn cùng nhau, Start cố định. Copy mã/Copy link thật bằng clipboard; không QR/native share/kick UI giả. Sửa config trong sheet dùng editor shared. Back có confirm, rời màn **không hủy phòng**; empty-room warning không thêm rule tối thiểu 2 player.
- Player: không chữ Rời phòng góc phải, không dots; back/system back confirm qua LeaveRoom cũ. Hero/gamepad + thẻ danh tính + roster cuộn; footer connection/retry cố định.
- Asset runtime đã track: **feature/lobby/src/main/res/drawable/gamepad_lobby.webp**; đây không phải ảnh design bị ignore.
- **Thẻ dài “Bạn là”**: tên đậm cùng foreground, không `(Bạn)`, không highlight tím.
- **Card trong lưới**: bố cục dọc như Host; own item hiện **“Tên (Bạn)” đậm cùng màu foreground**. Không bỏ hậu tố này hoặc tô tím riêng. Tên các player khác cũng đậm.
- Player grid tối đa **2 cột theo width/font scale, không theo số người**. Ngưỡng hiện tại `2 × 140dp × max(fontScale, 1) + 50dp` = 330dp ở scale 1; màn hẹp/font lớn vẫn về 1 cột. Host dùng Adaptive min 104dp × max(fontScale,1), không ép 2 màn cùng grid policy.
- Identity theo **playerId**, không tên/account; roster giữ cả offline/unknown, online chỉ exact connected; trước snapshot không bịa count, reconnect nói rõ snapshot cũ. Lives chỉ hiện non-null, kể cả 0.
- Không thêm ready/start cho Player, không keep-screen-awake flag mới. Giữ socket/retry/start/nav; leave không ACK không bảo đảm server đã xóa player.
- Host và Player mỗi màn **6 Preview cố định**; tránh tổ hợp Preview nhân hàng chục case.

### Bài học build vừa sửa
- Pin ui-graphics **1.12.1** đã kéo AAR yêu cầu SDK37/AGP9.1, xung đột compileSdk36/AGP8.13.2.
- `cc3b1c4` bỏ pin runtime 1.11.4/ui-graphics 1.12.1, aliases theo **Compose BOM 2025.06.01**. Không tự nâng SDK/AGP/Kotlin/BOM để sửa Preview.
- Lỗi Preview `NoSuchMethodError getCurrentCompositeKeyHashCode` cần kiểm tra binary/runtime/tooling mismatch, không kết luận do số Preview.
- Shared LobbyComponents từng untracked sau merge: khi đề xuất commit phải rà cả untracked, không chỉ modified files.

### Classic flow và countdown mới
- Host đúng 2 layout active/results: progress trước card câu có ảnh (ảnh có thể là nội dung câu), nút reveal fill gộp vào card; results bỏ card câu và tracker, chart lựa chọn 4 màu/stats thật + full leaderboard theo config, text không chart, board không collapse.
- Player feedback inline 2 giây rồi summary gọn trong cùng Gameplay, không chart/full board/route mới. Server phase/deadline preempt; pause/offline/duplicate/late ACK có guard. Không fake điểm câu/streak của Classic ở baseline cũ.
- Countdown chung core:ui có stateless Content; Screen giữ clock/offset. UNKNOWN/Connecting dùng preparing shell mới. 0 chờ server; Preview vẫn local 3 giây. Host menu và Player leave confirm giữ gating.
- N24.5: private result authoritative, so index + question_id; restore cá nhân không đồng nghĩa replay key. reveal=false không chờ rank vô hạn. Không local tăng streak/advance hoặc thay toolchain. Chi tiết implementation/tests/evidence trong plan riêng.

## 4. Cách phối hợp với tôi

Tôi giám sát từng phần, **không làm một mạch toàn app**. Với gameplay, audit cả chuỗi phase/route/initial/recovery trước khi chốt từng màn; tránh lặp lỗi chỉ polish active frame rồi rơi về UI cũ ở Results.

Với màn tiếp theo:
1. Đọc đúng ảnh mẫu và audit source mới nhất trước.
2. Báo những gì đã có, khác thiết kế, chưa có chức năng; lập bảng so sánh.
3. Đưa kế hoạch sơ bộ và những quyết định cần tôi chốt. Với nhiều hướng hợp lý, tạo khảo sát.
4. **Chỉ triển khai sau khi tôi duyệt phạm vi.**
5. Xong thì dừng để tôi xem Preview/hiệu chỉnh và chạy build/test trên máy.
6. Sau khi ổn, đọc git status/diff rồi đề xuất commit, **liệt kê đúng tên file từng đợt**. Không tự commit/push.
7. Khi tôi checkout docs, cập nhật targeted các tài liệu canonical; không merge toàn bộ main sang docs.

**Ưu tiên tiếp theo là review kế hoạch N24.5, không hỏi lại như chưa có hướng sau Player Lobby.** Xác nhận status/refs và user checkout main trước source work; nếu phạm vi/contract thay đổi phải hỏi lại. Final-result polish sau N24.5 mới audit/chốt riêng, không tự triển khai một mạch.

## 5. Nguyên tắc triển khai

- Giữ stateful Screen / stateless Content và component, mẫu AnswerOptionItem.kt. State xuống/callback lên; Screen xử lý Hilt/Flow/effects/navigation/platform/UI state cần thiết.
- ViewModel → UseCase → Repository; không dependency feature → feature.
- Reuse Inter/palette/QuizButtons/AppConfirmDialog và component đã duyệt, không dựng lại style riêng mỗi màn.
- SearchQuizCard/LibraryQuizCard/DiscoveryQuizCard đúng vai trò; QuestionEditorCard là soạn, QuestionPreviewCard là xem. History-review card riêng, **FinalResultScreen chưa polish**, không tự xem đã hoàn tất.
- Preview dữ liệu mẫu/no-op, không mạng/VM thật; số case gọn, trạng thái đại diện, cân nhắc narrow/font scale. Không mặc định tách Preview ra file riêng nếu không có lý do.
- Không bịa rating/lượt chơi/progress/status/contract/chức năng từ ảnh. UI-only giữ nghiệp vụ hiện có.
- Khi đụng API/socket, đọc backend thật trước, kiểm tra revision; design doc không phải contract. Không workaround client che backend presence/privacy/session bug.
- Không đưa design/ vào commit. Đổi interface phải rà implementation/fake tất cả consumer; `--tests` vẫn compile toàn test source của module.
- Nếu tool trả output dài, lưu/đọc đoạn cần thiết, không yêu cầu tôi copy output khổng lồ. Không vượt allowlist MCP bằng đường vòng để chạy lệnh bị chặn.

## 6. Kiểm chứng: không tự nâng trạng thái

- Agent **không chạy Gradle/render Compose**. User báo scoped `:core:network:testDebugUnitTest :feature:game-host:testDebugUnitTest :app:assembleDebug` xanh sau Host d72fcf6; không có run SHA/log. Player command từng FAIL smart cast, đã sửa rồi user duyệt UI nhưng chưa xác nhận toàn command PASS; countdown b005df3 chỉ được duyệt mắt thường, 9 test mới chưa đối chiếu run. Chưa có full run/log/XML gắn SHA cuối.
- 88 PASS practice/editor (11/10) và 56 PASS history/lobby/HostGame/DTO (15/10) là evidence lịch sử **khác lượt/phạm vi**, không cộng thành full run. 41 PASS app XML cũ không đóng regression security dialog 7 case.
- Lobby XML cũ chỉ 2 VM case/màn, có trước polish. Source đã thêm HostLobbyUiStateTest, PlayerLobbyUiStateTest (7 case) và 2 leave cases vào PlayerLobbyViewModelTest (tổng 4); **chưa xác minh run mới**.
- XML không có SHA/run id, không chứng minh APK/backend cùng revision hoặc full assemble/lint/CI/E2E. Preview/compile sạch không phải E2E PASS.
- Nếu cần bổ sung evidence, đề xuất user chạy trên **main** rồi đọc report, không tự đánh PASS:

```powershell
.\gradlew.bat :core:common:testDebugUnitTest :core:network:testDebugUnitTest :feature:game-player:testDebugUnitTest :feature:game-host:testDebugUnitTest :feature:lobby:testDebugUnitTest :app:assembleDebug
```

- Giữ backlog regression shared config/CreateRoom/Host sheet, reconnect/retry/leave/game-start/clipboard/roster/avatars/narrow/IME, security dialog, Discover Paging snapshot và toàn bộ release gates. Không tự mở rộng sửa tất cả backlog khi đang nhận một màn UI.
