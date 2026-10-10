> 🎯 **Tóm tắt**: Backend myquizz (Express + TypeScript + Socket.IO + PostgreSQL + Redis) chất lượng khá tốt, đúng chuẩn server-authoritative. Design doc v2 khớp ~90% code thật, có 11 điểm lệch cần vá (mục 3) — 3 điểm mới phát hiện 24/8 khi đối chiếu lại trực tiếp qua GitHub API thay vì ghi chú cũ: REST error thật chỉ có `{code}` (không `message`/`details`), luồng Quên mật khẩu Android gọi 2 endpoint không tồn tại, và quiz listing đã hỗ trợ pagination thật (cursor). Kế hoạch 50 ngày / 10 tuần đã hiệu chỉnh theo trình độ thực tế. **Trạng thái: Tuần 3 (N11–N15) hoàn tất 24/8 — Home/Search + Quiz Detail (cache Room) + Quiz-manage danh sách/tạo quiz (Paging 3) + Upload ảnh presign S3 2 bước xong; `feature:auth` đã refactor UiState/Intent/Effect tách file riêng (22/8); bổ sung ngoài kế hoạch: Home auth header + màn Profile + component `Avatar` chung ở `core:ui` (22/8). Bài học lớn N15: kotlinx.serialization `JsonNamingStrategy` vẫn đổi tên field dù đã có `@SerialName` tường minh — đã tách `Json`/`Retrofit` riêng cho `StorageApiService`. N16 (25/8) hoàn tất — sửa/xóa quiz: màn editquiz pre-fill cả đáp án (PATCH replace-all questions, dirty guard), xóa hard delete + dialog xác nhận, nút owner-only ở QuizDetail, cache Room cập nhật/xóa theo; **M3 chốt**. Phát hiện thêm trong N16: điểm lệch #10 (DELETE là hard delete, doc ghi nhầm "xóa mềm") + bug reload-on-resume do flag `remember` bị reset khi Navigation dispose composition (fix: `rememberSaveable`). N16.5 (xong ngay 25/8) — 3 vá khẩn hoàn tất: luồng Quên mật khẩu viết lại theo 3 bước ticket thật (verify OTP ngay tại màn OTP, nút gửi lại đếm ngược 60s, màn Reset nhận ticket + peek), envelope lỗi đổi sang `{code}` + map ~60 code → tiếng Việt, search dùng cursor thật; kèm 2 hotfix khi test: SearchScreen không có trigger submit nào (bug sót từ N11–12) + click card chưa điều hướng. N17 hoàn tất 28/8 — CreateRoomScreen tạo phòng thật theo contract động của `/games/game-modes`, lấy host token riêng và hand-off sang HostLobby placeholder; config đã refactor typed, JSON/dotted path chỉ còn ở network. Refactor kiến trúc UI trước N18 cũng hoàn tất 28/8: audit đủ 14 Screen, chuẩn hóa Stateful `XxxScreen` + stateless `XxxScreenContent`, hoist platform/lifecycle/effect state và dùng chung `QuizEditorContent`. N18 hoàn tất 30/8 — socket layer thật, đã build và test trên máy thật: `core:common` có `GameEvent`/`LobbyState` + 3 interface socket tách theo vai trò (base/host/player), `core:network` có `GameSocketClient` (callbackFlow + `awaitClose`, handshake `auth.token`) và `GameEventMapper` (payload rác → `Failed(CLIENT_PARSE_ERROR)` chứ không throw làm chết flow), `feature:lobby` có HostLobby thật thay `HostLobbyPlaceholder` (đã xóa). Ba bài học N18: (1) socket.io tự reconnect nhưng KHÔNG tự join lại room, nên phải gọi `lobby:join` sau MỌI lần `Connected` — nếu không, socket vẫn "connected" mà im lặng không nhận `lobby:updated` nữa; (2) `io server disconnect` không được socket.io retry nên phải thoát màn, khác hoàn toàn với mất mạng (transport); (3) contract lỗi socket thật là `{event, code}` — doc cũ ghi sai thành `{event, message}` kèm prefix, và vì `AppError.Api(code)` đã map sẵn ~60 code nên KHÔNG thêm `AppError.Socket`. N19 hoàn tất 5/9 — Player lobby thật, đã build và test trên máy thật: tra phòng bằng mã (`GET /games/{code}`) → join REST (`POST /games/{code}/join`) lấy `socketToken` → vào PlayerLobby realtime dùng lại socket layer N18. Khách có `GuestIdentityStore` (UUID sinh lần đầu cần join, lưu DataStore) + màn nhập nickname riêng; người đã đăng nhập vào thẳng lobby không qua màn nhập tên (server tự lấy danh tính từ cookie, body join rỗng `{}`); quyền cho khách vào hay không do host cấu hình (`config.lobby.allowGuests`), client chỉ tôn trọng chứ không tự quyết. Đã trả luôn nợ N18: lý do bị buộc rời phòng giờ hiện thật qua `savedStateHandle` + snackbar. **Bài học lớn N19 — điểm lệch #11**: `GET /games/{code}` trả `data.session.session` (lồng ba cấp) vì `getGameByCode` đặt tên biến `session` cho cả cụm `{session, players, config}` mà `getLobby` trả — app crash `MissingFieldException` khi test thật; dấu hiệu nhận biết là danh sách field thiếu tại `$.data.session` nhưng `config` KHÔNG nằm trong danh sách đó (vì cấp ngoài có sẵn key `config`). Rút ra: đọc `*.service.ts` để biết shape thật, đừng tin tên key trong `success(res, { ... })` của controller. N19.5 hoàn tất 6/9 — Bottom Navigation thật 5 tab (Trang chủ/Thư viện/Tham gia/Hoạt động/Hồ sơ) với `Scaffold` bọc ngoài `NavHost`, tab Thư viện trỏ thẳng `Route.MyQuizzes` (xóa `Route.Library` trùng lặp, không xây lại màn), Profile rút về thông tin + cài đặt, avatar user chuyển xuống icon tab Hồ sơ; tab Hoạt động còn là placeholder vì backend chưa có `role=all`. Bài học N19.5: UI nằm ngoài `NavHost` không có back stack entry nên không dùng được ViewModel theo destination, và phải biết repository có cache hay không trước khi nạp dữ liệu cho UI luôn hiển thị (`/users/me` không cache). Bước tiếp theo: N19.6 (chốt kế hoạch 6/9) — session state một nguồn (`SessionRepository` + `StateFlow`, sửa gốc bug đăng xuất vẫn thấy account cũ), `AuthRequiredDialog` ở `core:ui` + gác đăng nhập ở Thư viện/FAB tạo quiz/nút tạo phòng ở QuizDetail/Hồ sơ, xóa hẳn màn Join và tab Tham gia (bottom nav còn 4 tab, ô nhập mã chuyển vào Home qua slot), nút "Xem thêm" cho 3 section khớp 100% (`trending`/`category` → `/feed`, `newest` → `/search?sort=newest`). N19.6 hoàn tất 6/9 — đã build và test trên máy thật: session state một nguồn (`SessionRepository` + `StateFlow<SessionState>` với 3 nhánh `Unknown`/`Guest`/`LoggedIn`), 4 chốt gác đăng nhập chặn trước khi điều hướng, bottom nav rút về 4 tab và ô nhập mã phòng chuyển thành `JoinRoomCard` nhúng vào Home qua slot, Home có nút "Xem thêm" theo `section_type` + `Route.Discover(sectionKey)`, mã phòng chốt đúng 6 ký tự. Ba bài học N19.6: (1) bug "đăng xuất vẫn thấy account cũ" không phải lỗi hiển thị mà do ba bản sao trạng thái + `init{}` của ViewModel không chạy lại vì back stack entry của tab được `saveState`/`restoreState`; (2) `Unknown` phải là trạng thái riêng — gác đăng nhập cho nó đi qua, còn phân luồng join phòng thì bắt buộc đợi `refresh()`, và lỗi mạng không bao giờ được dịch thành "đã đăng xuất"; (3) một trang chỉ được có MỘT vùng cuộn — không lồng `verticalScroll` quanh `LazyColumn`, không `fillMaxSize()` trong lazy item, và placeholder lỗi thuộc về từng khối chứ không thay cả trang. N20 hoàn tất 10/9 — Host lobby thật, đã build và test trên máy thật: host sửa được cấu hình phòng ngay trong lobby qua `lobby:config-update` (có ACK `{changed, config, ignored}`), chia sẻ phòng bằng 2 nút copy (mã phòng + link `https://myquizz.dpdns.org/join?code=...`, bỏ QR khỏi phạm vi), nút "Bắt đầu" neo vào broadcast `game:started` kèm timeout 5s vì `game:start` KHÔNG có ack, và hand-off sang `HostGamePlaceholder` của N21. Editor cấu hình của N17 được chuyển từ `feature:quiz-manage` lên `core:ui/gameconfig` và tách thành 6 file (form state, 3 loại control, dispatcher theo mode, patch builder) vì feature không được phụ thuộc feature. Bốn bài học N20: (1) `normalizeConfig` của server ghi đè giá trị host gửi lên (perQuestionSeconds=0 → tắt speedBonus; self-paced + between_questions → end_only; reviewMode → bật showCorrectAnswer) nên form phải dựng lại từ `ack.config`, tuyệt đối không coi state cục bộ là sự thật sau khi lưu; (2) baseline của patch trong lobby là **config thật của phòng**, không phải default của mode — dùng sai baseline thì thao tác "tắt lại một field vốn khác default" bốc hơi im lặng, không crash không log; (3) thêm một nhánh vào sealed class ở `core:common` làm vỡ `when` ở module khác (`GameStarted` làm fail cả HostLobby lẫn PlayerLobby) — phải rà mọi module tiêu thụ trước khi build, và không được dập lửa bằng `else`; (4) chốt ngăn sửa config sau khi trận bắt đầu nằm ở `writeConfig` (409 `GAME_LOBBY_ONLY`) chứ không ở handler socket — mở rộng bài học N19, phải đọc tới hàm thật sự ghi dữ liệu. Quy ước UX chốt ở đây: bottom sheet **chỉ ở lại khi lỗi phát sinh trước lúc gọi server** (field sai định dạng), mọi kết quả khác đều đóng sheet + snackbar. N21 hoàn tất 13/9 — **màn điều khiển của host (host console) cho classic**, đã build và test trên máy thật. **Lưu ý đổi phạm vi có chủ đích**: kế hoạch gốc để N21 là màn CHƠI của player, nhưng vì N20 đã hand-off sang `HostGamePlaceholder` nên làm màn host trước mới liền mạch ⇒ đã **kéo N31–N33 của Tuần 7 lên N21**, và màn chơi của player dời xuống N22–N23. `feature:game-host` có `HostGameScreen` thật (đếm ngược → câu hỏi → thống kê → bảng theo dõi → kết thúc), câu hỏi luôn hiện còn đáp án ẩn sau nút "Xem đáp án", điều khiển pause/resume/kết thúc sần (`game:next` **không** hiện ở classic vì `autoAdvance=true` bị server từ chối 409), đồng hồ bù lệch theo `serverTime`. **Bốn bài học N21**: (1) `docs/components/socket.doc.ts` **ghi sai** shape lựa chọn trả lời (`{id, text, image}`) trong khi CSDL chỉ lưu `{id, option_text}` và `game.socket.ts` forward nguyên xi ⇒ mọi lựa chọn hiện "(ảnh)" khi test thật; tài liệu backend không phải nguồn sự thật, phải đọc code emit + shape lưu; (2) payload có shape đa hình (object hoặc chuỗi thuần) thì KHÔNG được dùng `@Serializable data class` — một lựa chọn lạ kiểu làm ném lỗi parse và **rơi trọn sự kiện `host:question`**, màn host đứng im không crash không log; (3) `core.common.model` là package phẳng nên thêm `AnswerOption` trùng tên class của `Quiz.kt` ⇒ `Redeclaration` + hỏng luôn serializer của `Question`; phải tra tên trước khi thêm model (đã đổi thành `PublicAnswerOption`); (4) UI dựa trên danh sách lựa chọn phải có nhánh riêng cho câu tự luận — câu `SHORT_ANSWER`/`LONG_ANSWER` không có option nào để in đậm nên nút "Xem đáp án" trông y hệt nút hỏng. Chốt phạm vi ảnh (13/9): **ảnh cho lựa chọn trả lời bỏ hẳn** (schema backend cứng `{id, option_text}`, quiz chỉ dùng lựa chọn chữ) còn **ảnh câu hỏi đã re-baseline thành N40 feature correctness** (`question_image` backend đã có đủ và đã tới Android, chỉ thiếu phần render — nợ tồn tại từ M2). N20.5 hoàn tất 13/9 — màn Khám phá thật với Paging 3 cursor, filter/sort/category, public-only client và fix mixed-case pagination meta. N22–N23 hoàn tất 13/9 — **Gameplay Player host-paced/classic** đã chạy ổn theo happy case: PlayerLobby hand-off sang `Route.GamePlay`, state machine countdown/question/submitted/locked/results/finished, input đủ 4 loại câu, typed `question:answer` ACK, timer theo clock offset và reconnect/resync bằng `game:state.player.answered_questions`. Bài học chính: ACK timeout là trạng thái **không chắc chắn**, phải giữ input khóa + `player:sync`, không tự retry; implementation `game.socket.ts` là nguồn sự thật vì ACK host-paced chỉ xác nhận acceptance, không trả đúng/sai/điểm. Test toàn diện để sau. **N24 hoàn tất 15/9/2026**: Player nhận `answer:received`, `question:results`, `leaderboard:updated`, điều hướng từ `game:ended` sang `Route.FinalResult`, lưu kết quả tạm bằng `GameResultRepository`, render bảng giữa câu đúng theo `showLeaderboard`, khóa input khi pause và phòng thủ không lộ đáp án khi `showCorrectAnswer=false`. Hotfix cấu hình phòng: backend ép `reviewMode=true → showCorrectAnswer=true`, nên form giờ đồng bộ hai công tắc và Marathon khóa reveal ở trạng thái bật. **N25 đã audit + E2E nhiều máy ngày 26/9**: happy path, 4 loại câu, reveal/leaderboard, Final Result và pause/resume đạt; Android đã vá stale snapshot, giữ Submitted qua pause/resume, phân biệt transport/server disconnect, thêm reconnect theo mạng `VALIDATED`, trạng thái cạn retry và nút thử lại. N25 vẫn **FAIL và tạm gác** vì ACK uncertainty/fatal disconnect chưa tái hiện đầy đủ và backend có presence race: `onLeave` của socket cũ có thể ghi đè `connected` của socket mới, làm Host tính thiếu Player và chuyển câu sớm. Không thêm delay/double-join workaround ở Android; chờ backend sửa theo socket identity/generation rồi retest. N26 hoàn tất 26/9 — nền gameplay self-paced/Solo: câu hỏi cá nhân, feedback tức thời từ ACK, score/streak/đáp án đúng theo reveal, soft deadline và phòng thủ không lộ grading fields; đã test máy thật với 1–2 Player, `autoAdvance=true` chạy hết luồng. **N27 hoàn tất 27/9** — manual progression cho `autoAdvance=false`: Android map `question:awaiting_next`, hiện nút **Câu tiếp theo**, phát `question:next`, chống double tap và mở lại thao tác sau timeout 5 giây; `autoAdvance=true` không đổi. **N28 hoàn tất 27/9** — Player Survival/Marathon: lives, elimination, timer tổng theo `matchEndsAt`, `question:timeout`, `player:finished` và trạng thái chờ `game:ended`; commit `e68a3af`. **N28.5 hoàn tất phía Android ngày 27/9** — Host Console self-paced theo dõi tiến độ/điểm/status từng Player; phần lives/streak realtime, timeout và presence refresh đang chờ backend. **N29 hoàn tất phía Android ngày 3/10** (commit `499297c`) — Practice happy case đã pass, post-game review dùng REST `GET /games/{id}/review` với `x-socket-token`; gate pause/resume còn chờ backend sửa snapshot self-paced. **N30 đã audit và tạm block** vì backend chưa có contract cấp lại Player socket token, đồng thời `player:sync` còn sai shuffled order/Marathon index. N31–N33 đã hấp thụ; N34 đã có đủ trong N21/N28.5. **N35 hoàn tất ngày 3/10** (commit `37bc61a`) — Final Result ưu tiên `game:ended`, chỉ fallback `GET /games/{id}/results` khi mất transient result, có loading/error/retry, thống kê từng câu và chặn lộ leaderboard khi `showLeaderboard=never`. **N36 hoàn thành ngày 3/10** (commit `f232a75`) — harden reconnect cho Host/Player Lobby và Host Game, thêm trạng thái cạn retry, Host token refresh và test ViewModel; 3 luồng E2E chính đã pass. **N37 hoàn thành ngày 3/10** — chuẩn hóa error code-first, phân loại lỗi tập trung, giữ session khi lỗi mạng/server, banner offline toàn app (trừ màn realtime), chặn cache làm sống lại quiz đã xóa và sửa race gác đăng nhập sau logout. Audit feature completeness đã chốt: phần Android còn thiếu gồm Hoạt động/Lịch sử, render ảnh câu hỏi trong gameplay, Solo Preview và account settings; backend hiện đã có đủ history list/summary/my-answers nên không còn chờ `role=all`. **N38 hoàn thành 6/10** (commit `ba0ff49`) — tab Hoạt động dùng history server thật, user có Đã chơi/Đã tổ chức, guest có Đã chơi bằng UUID thiết bị, cursor riêng từng role và tự refresh khi quay lại màn; manual guest flow sau trận đã pass. **N39 hoàn thành 6/10** (commit `ecce5cd`) — row lịch sử mở được chi tiết snapshot trận cho host/player/guest; summary và answer sheet tải độc lập bằng danh tính cookie hoặc guest UUID, không phụ thuộc socket token cũ; host có leaderboard/per-question, player giữ thành tích riêng và Android phòng thủ `showLeaderboard=never`. **N40 hoàn thành 6/10** (commit `9b3cdf0`) — ảnh câu hỏi đã render thống nhất ở Host Game, Player Game và card preview Quiz Detail qua component `QuestionImage`; mapper test bảo vệ cả event realtime và snapshot, kiểm thử nhanh + build/test đã pass. **N41 hoàn thành** (commit `648bf37`) — module `:feature:quiz-preview` chơi local từ Quiz Detail, 4 loại câu hỏi, countdown/timer, skip/late answer, feedback, tổng kết/chơi lại/sửa owner-only; không room/socket/history/play-count. User xác nhận kiểm thử nhanh pass; đã thêm unit test scoring + ViewModel. **N42 hoàn thành** (commit `0da538f`) — sửa fullname/phone/description bằng PATCH delta, avatar preview và lưu riêng, SSOT generation/revision guard và snackbar thật; bổ sung 23 test case, user xác nhận kiểm thử nhanh pass. **N43 hoàn thành phần Android** (commit `ab6621b`) — đổi mật khẩu/vô hiệu hóa, Use case boundaries, session/navigation hardening và logging an toàn; user xác nhận kiểm thử nhanh ổn, bổ sung 25 test case (chưa có log build/unit test/CI được agent xác minh). Bước tiếp theo: **N44 — audit integration + backend-blocker retest gate**. Polish được dời sang N46, sau feature-complete gate N45. N25/M4 vẫn chờ backend sửa presence race và N20.6 vẫn chờ `GET /games/active`.**

> **Cập nhật source N46 mới nhất:** N44 vẫn PARTIAL (source `3ea792a`, báo cáo `N44_E2E_REPORT.md`); N46 đã triển khai đến Search/Discover, source main `d5571d6`. Sau mốc Register `ee8701c` có thêm 7 commit: Forgot/OTP/Reset + validator/guards, card/room-entry/Home/bottom nav, Search và Discover; xem `N46_UI_PROGRESS.md`. Login/Register/Home đã được user xác nhận UI ổn; Search/Discover đã commit, bản sửa lỗi màu internal và crash Paging đã có trong source nhưng chưa có xác nhận retest sau sửa hoặc log build/lint/test/CI mới được agent đối chiếu. N46 WIP, không chốt N45/M6/release; Google/cookie DEFERRED và các blocker N44/backend giữ nguyên.

> **Báo cáo N44 giữ nguyên:** N44 đã có báo cáo manual E2E PARTIAL cho lượt user ghi 08/10/2026 22:00; xem `N44_E2E_REPORT.md`. Lỗi mới N44-OFFLINE-BOOT: cold-start offline làm Hoạt động/Hồ sơ treo loading; Practice pause/resume vẫn nhảy về câu đầu. Một số case PARTIAL/NOT RUN/BLOCKED hoặc dùng evidence lịch sử; APK SHA/backend deployment/full CI chưa xác nhận. Theo quyết định user, lưu backlog và chuyển sang nghiên cứu giao diện N46; đây là ngoại lệ thứ tự research, không chốt N45/M6, không miễn trừ release blockers. Google/cookie race vẫn DEFERRED, chưa fix; 16 regression N44 và 25 test N43 XML PASS là evidence trước đó.

File này tự chứa đủ ngữ cảnh để bắt đầu phiên làm việc mới (đã gộp & tinh gọn log các phiên 9–11/8).

*Nguồn review: repo [github.com/Ntd1411/myquizz](https://github.com/Ntd1411/myquizz) (backend, nhánh main) + repo Android [github.com/An-K4/MyQuizApp](https://github.com/An-K4/MyQuizApp). Trình độ dev đánh giá qua 2 dự án trước: My Schedule v3.2 (Kotlin, MVVM, Room, custom Canvas, AlarmManager) và Clover Chatty (Kotlin + Compose, Clean Architecture + MVVM, Hilt, Socket.IO, Retrofit, Room, DataStore, FCM).*

---

## 1. Bức tranh tổng thể

| Thành phần | Công nghệ | Trạng thái |
|---|---|---|
| `backend/` | Express 5 + TS (ESM), Socket.IO 4.8, PostgreSQL 16 (pg thuần), Redis 7 (ioredis), S3 presign, JWT qua cookie HttpOnly | Hoàn thiện cao: auth đầy đủ, game engine 5 mode, rate limit, Swagger `/v1/api-docs`, migration + seed. Đã deploy: `api.myquizz.dpdns.org` |
| `frontend/` | Vue 3 + Vite + Pinia + vue-query + axios + Tailwind + GSAP | Cổng quản lý web (auth, discover, quiz CRUD, import xlsx, join). **Không có UI gameplay realtime → gameplay thuộc app Android** |
| DevOps | Dockerfile, docker-compose (PG + Redis + BE + FE), PM2, GitHub Actions | Chạy được; CI backend chưa có bước test (vì chưa có test) |

## 2. Review backend

### 2.1. Điểm mạnh — giữ nguyên

- **Server-authoritative tuyệt đối**: chấm điểm/thời gian/đáp án đúng đều ở server; `correct_answer` bị cắt khỏi mọi payload gửi player.
- **Anti-cheat chu đáo**: shuffle có seed (reconnect giữ nguyên thứ tự câu); reconnect không reset đồng hồ; câu hết hạn lúc offline bị đóng luôn; chống double-submit (`allowChange=false`); identity chỉ lấy từ socket token.
- **Socket token flow**: JWT ngắn hạn ký bằng `SOCKET_JWT_SECRET` riêng, payload `{psid, gsid, code, role}`; kick player = token cũ tự vô hiệu.
- **Config engine chắc** (`engine/config.rule.ts`): editable/locked theo mode; `sanitizeConfigPatch` không bao giờ throw (drop + report `ignored`).
- **Vòng đời game đầy đủ** (`game.socket.ts`, ~1460 dòng): countdown, pause/resume dời clock từng player, marathon timer phía server, flush Redis → Postgres cuối câu/cuối game.
- **Hạ tầng tốt**: cache-aside Redis, rate limiter có rollback + fail-open, refresh token rotation + blacklist + device tracking, OAuth state cookie chống CSRF, quiz snapshot khi tạo phòng.

### 2.2. Vấn đề cần xử lý (phía backend)

| Mức | Vấn đề | Đề xuất |
|---|---|---|
| 🔴 | `session_code` không UNIQUE; tạo code bằng check-then-insert → race trùng mã phòng | Partial unique index `WHERE deleted_at IS NULL AND session_status IN ('lobby','active','paused')` • `INSERT ... ON CONFLICT` retry |
| 🔴 | Không có bất kỳ test nào; engine timer/scoring phức tạp | Unit test trước cho `scoring.ts`, `config.rule.ts`, `grade`/`normalize` (hàm thuần, dễ test) |
| 🔴 | `GameSocket` god class ~1460 dòng; timers/questions là Map in-memory theo process | Tách PhaseManager/ClockService; khi cần scale: Socket.IO Redis adapter + externalize timers |
| 🟡 | Redis chết → self-paced mất clock → `timeTaken ≈ 0` → ăn speed bonus tối đa | Fail-closed cho `question:answer`, hoặc chấp nhận degraded + cảnh báo vận hành |
| 🟡 | `POST /v1/games` trả `data.data.session` (lồng kép); nặng hơn: `GET /v1/games/:code` trả `data.session.session` vì `getGameByCode` viết `success(res, { session })` trong khi `session` thực chất là cả cụm `{session, players, config}` của `getLobby` — đây là **bug đặt tên biến**, không phải quy ước | Client map DTO đúng (Android đã có `LobbySnapshotDto` tại N19); đề xuất backend sửa `getGameByCode` thành `success(res, lobby)` — nhưng phải đổi đồng thời cả frontend web (`unwrap(res.data).session`) và app, không deploy một mình |
| 🟡 | `/v1/api-docs` public mọi môi trường | Gate theo `NODE_ENV !== 'production'` |
| 🟡 | Song song joi + zod; enum `game_mode` chứa `'team'` chưa implement | Chuẩn hóa zod; xóa `'team'` hoặc bổ sung engine |
| 🟡 | Cookie auth chỉ dựa `SameSite=Lax`, không CSRF token | Giữ lax; cân nhắc CSRF token khi mở rộng surface |
| 🟢 | Ternary thừa trong cookie config; root README rỗng; compose publish port 5432/6379; tạo phòng không chặn quiz 0 câu | Dọn dẹp; validate `total_questions > 0` |

### 2.3. Frontend web (nhanh)

Vue 3 thuần JS + Pinia + vue-query + Tailwind/GSAP; có import quiz từ xlsx (~17KB, đáng port sang Android phase 2) và `mock.api.js` (phát triển UI không cần backend — pattern nên học theo). Web = quản lý + join; **gameplay nằm ở Android** (khớp design doc).

## 3. Đối chiếu Design Doc v2 ↔ code thật

**Khớp ~90%**: cookie HttpOnly auth (không Bearer); REST trước → `socketToken` → Socket.IO handshake `auth.token`; 5 mode + pacing host/self; bảng event 5.2–5.3; `AnswerAck`; 4 `question_type`; envelope `{success, data, error}` (đúng outer shape, xem #7 cho inner error); upload ảnh presign S3 2 bước; CookieStore DI qua `core:common`.

**11 điểm lệch — vá doc trước khi code phần liên quan (#7–#9 phát hiện 24/8 khi đối chiếu trực tiếp qua GitHub API, #10 phát hiện ở N16, #11 phát hiện ở N19 khi app crash lúc test thật):**

| # | Doc đang ghi | Code thật | Hành động Android |
|---|---|---|---|
| 1 | Route không prefix | Mọi REST dưới `/v1`; game router ở `/v1/games` | `BASE_URL = https://api.myquizz.dpdns.org/v1/` |
| 2 | Socket base URL mặc định | Server dùng namespace `/game` | `IO.socket(SOCKET_URL + "/game", options)` |
| 3 | `question:started` = `{question, time_limit, endsAt, serverTime}` | Self-paced thêm `matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `lives` | Bổ sung model `SelfQuestionStarted` |
| 4 | Không có REST update config | Có `PATCH /v1/games/:id/config` (host, lobby-only) | App ưu tiên kênh socket `lobby:config-update` (có ACK + broadcast) |
| 5 | `CreateGameSession` trả phẳng | Thực tế lồng `data.data.session` (kèm `ignored`) | DTO map đúng |
| 6 | `game:state` mô tả chung chung | Snapshot gồm `question`, `countdown`, `endsAt/matchEndsAt`, `allow_answer_late`, `remainingSeconds`, `player` (gated reveal), `leaderboard` (rỗng khi câu đang mở) | Nguồn dữ liệu chính của Host screen + resync |
| 7 | REST error = `{message, details}` (đã cài đặt sai theo giả định này ở N1–5, `core:network`) | `fail()` trong `response.ts` chỉ trả `{code}` — cố ý không có message/details (tránh leak nội bộ + đa ngôn ngữ) | Sửa `ApiError` còn 1 field `code: String`; `AppError.Http` đổi field `message` → `code`; thêm hàm map `code` → `UiText` tiếng Việt — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 8 | Quên mật khẩu: 3 endpoint giả định `/users/forgot-password` + `/users/reset-password-token` + `/users/reset-password` (đã code xong ở N15, 15/8) | Flow ticket 3 bước thật: `POST /users/forgot-password` (đúng tên, response khác) → `POST /users/password-reset/verify` (`{email,otp}` hoặc `{token}` → `{ticket,expiresAt,email}`) → `GET /users/password-reset/ticket` (peek) → `POST /users/password-reset/complete` (`{ticket,newPassword}`) | Viết lại DTO/API service/ViewModel reset theo 4 endpoint thật — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 9 | Ghi chú kỹ thuật nợ ở N11: "backend chưa có pagination" | `/quizzes/search`, `/quizzes/me`, `/quizzes/users/id/:ownerId` đã hỗ trợ `cursor`/`limit` (1–24)/`include_total` | Refactor Paging 3 dùng cursor thật thay vì `page` không dùng — ✅ ĐÃ VÁ ở N16.5 (25/8) |
| 10 | DELETE /quizzes ghi "xóa mềm" | **Hard delete**: `DELETE FROM` + cascade mất questions, quiz_snapshots, game_sessions, player_sessions (quiz.repository.ts) | Dialog xác nhận cảnh báo mất lịch sử chơi + xóa Room cache sau khi xóa — đã làm ở N16 |
| 11 | Tra phòng `GET /games/{code}` trả `data.session` = một `GameSession` phẳng | Lồng ba cấp `data.session.session`: `getLobby` trả `{session, players, config}` nhưng `getGameByCode` gán cả cụm vào biến tên `session` rồi `success(res, { session })` — bug backend (xem mục 2.2), không phải quy ước bao resource như #5 | `RoomLookupResponseDto(session: LobbySnapshotDto)` + `LobbySnapshotDto(session, players)`; `totalPlayers` đếm từ `players` vì `total_players` trên row KHÔNG tăng khi player join (chỉ flush cuối ván) — đã làm ở N19 (5/9), có test hồi quy khóa shape |

Lưu ý thêm: swagger ghi `/auth/refresh` trả tokens nhưng thực tế chỉ trả `{message}`.

## 4. Hồ sơ kỹ năng

**Đã có sẵn** (từ My Schedule + Clover Chatty): Kotlin + Coroutines/Flow • Compose M3 • Clean Architecture 3 tầng + UseCase • Hilt • Socket.IO client • Retrofit/OkHttp • Room + DataStore • MVVM • Custom Canvas, AlarmManager, Notification • FCM • offline-first / safeApiCall / UiText.

**Cần học** (đã chèn vào tuần tương ứng):

| # | Chủ đề | Tuần |
|---|---|---|
| 1 | Gradle multi-module (version catalog, convention plugins — spike nowinandroid) | 1 ✅ |
| 2 | Cookie auth: OkHttp CookieJar + Authenticator (gỡ thói quen Bearer) | 1–2 ✅ |
| 3 | Credential Manager + Google One Tap | 2 ✅ |
| 4 | kotlinx.serialization (bỏ Gson) | 3 ✅ |
| 5 | Paging 3 (có thể thay phân trang tay) + S3 presign 2 bước | 3 ✅ |
| 6 | Socket.IO nâng cao: namespace `/game`, ACK, auth handshake; bắt đầu MVI | 4–5 |
| 7 | Testing: JUnit + MockK + Turbine + MockWebServer + Compose UI Test — **khoảng trống lớn nhất, bắt đầu từ Tuần 5** | 5–9 |
| 8 | Release hardening: R8/ProGuard, network security config, LeakCanary | 8–9 |

## 5. Lộ trình 50 ngày

**Giả định**: 1 dev full-time, 5 ngày/tuần; backend dùng nguyên trạng, vá nhỏ chạy song song không block app; chuẩn bị từ Tuần 1: Google OAuth client, S3 bucket + quyền presign, backend chạy local bằng docker-compose.

**Milestones**

| Mốc | Ngày | Tiêu chí chốt |
|---|---|---|
| M1 — Foundation | N5 | ✅ **CHỐT 9/8** — 13 module build xanh, CI lint + test + assemble |
| M2 — Auth E2E | N10 | ✅ **CHỐT 13/8** — Login/Register/Google One Tap với backend thật; 401 → refresh → retry tự động + UI polish (theme, Stateful/Stateless pattern, AppTextStyles) |
| M3 — Quiz CRUD | N20 | Tạo/sửa quiz + upload ảnh S3 + tạo phòng config động |
| M4 — Classic playable | N25 | ⚠️ Chưa chốt — ván Classic đã chạy đủ thiết bị, nhưng N25 còn backend presence race và các gate E2E chưa tái hiện |
| M5 — Đủ 5 mode + Host console | N35 | ⚠️ **Android hoàn thành 3/10**; E2E đầy đủ còn chờ các blocker realtime backend của N25/N28.5/N29/N30 |
| M6 — Feature complete | N45 | N38–N43 xong; blocker backend đã được retest hoặc ghi nhận rõ là release blocker |
| M7 — Release candidate | N48 | Regression pass, release build R8, LeakCanary sạch |
| M8 — Ship | N50 | Internal release Play Console + tài liệu bàn giao |

## 6. Trạng thái hiện tại và lộ trình chuẩn (N44 WIP, source Android `3ea792a`)

> **Cách đọc:** mã N là định danh lịch sử ổn định, không đổi số sau khi đã commit. Thứ tự triển khai chuẩn lấy theo bảng dưới; các block Implementation Details phía sau là nhật ký kỹ thuật, không dùng để suy ra việc tiếp theo.

| Mốc | Phạm vi hiện tại | Trạng thái | Thứ tự xử lý |
|---|---|---|---|
| N1–N20 | Nền tảng → Host Lobby | ✅ Hoàn thành | Lịch sử |
| N20.5 | Màn Khám phá thật | ✅ Hoàn thành 13/9 | Lịch sử |
| N20.6 | Quay lại phòng đang chơi | ⛔ Blocked bởi `GET /games/active` | Bỏ qua cho tới khi backend sẵn sàng |
| N21 | Host Console classic; hấp thụ phần chính N31–N33 | ✅ Hoàn thành 13/9 | Lịch sử |
| N22–N23 | Gameplay player host-paced | ✅ Hoàn thành 13/9; happy case đã review, test toàn diện để sau | Lịch sử |
| N24 | Kết quả player + điều hướng cuối trận | ✅ Hoàn thành 15/9; unit test/build đã chạy xanh | Lịch sử |
| **N25** | E2E classic nhiều máy | ⏸️ **Tạm gác 26/9 — kết quả FAIL** | Chờ backend sửa presence race rồi retest để chốt M4 |
| **N26** | Nền gameplay self-paced + Solo ACK feedback | ✅ Hoàn thành 26/9; test 1–2 Player | Lịch sử |
| **N27** | Manual next + `question:awaiting_next` | ✅ Hoàn thành 27/9; happy case đã xác nhận | Lịch sử |
| **N28** | Player Survival + Marathon | ✅ Hoàn thành 27/9; happy case đã xác nhận | Lịch sử |
| **N28.5** | Host Console self-paced | ✅ **Android xong 27/9; chờ backend** | Dashboard đã hỗ trợ tiến độ/điểm/status; lives/timeout/presence realtime phụ thuộc payload backend |
| **N29** | Practice + post-game review | ✅ **Android xong 3/10; happy case pass; chờ backend pause/resume** | Commit `499297c`; review là REST, token chỉ giữ transient |
| **N30** | Resume + làm mới socket token | ⛔ **Blocked sau audit 3/10** | Chờ backend có Player token renewal và sửa `player:sync` theo progress/order riêng |
| N31–N33 | Host Console theo kế hoạch gốc | ✅ Đã hấp thụ vào N21/N28.5 | Không triển khai lại |
| **N34** | `leaderboard:host` full table | ✅ Android đã hoàn thành trong N21/N28.5 | E2E dữ liệu lives/timeout/presence chờ backend |
| **N35** | Final Result REST recovery + `perQuestion` | ✅ **Hoàn thành 3/10** | Commit `37bc61a`; happy path ưu tiên socket, REST chỉ recovery |
| **N36** | Reconnect hardening | ✅ **Hoàn thành 3/10** | Commit `f232a75`; 3 case E2E chính pass, host-token refresh có unit test |
| **N37** | Error handling + `GONE` + offline banner | ✅ **Hoàn thành 3/10** | Code-first error mapping; session/cache/nav hardening; offline banner; manual test chức năng chính pass |
| **Pre-N38** | Audit feature completeness toàn app | ✅ **Hoàn thành 3/10** | Đã tách phần Android làm ngay, blocker backend và phase 2 |
| **N38** | Hoạt động: history data layer + danh sách | ✅ **Hoàn thành 6/10** | Commit `ba0ff49`; user/guest, cursor riêng từng role, refresh khi quay lại màn; manual guest flow pass |
| **N39** | Chi tiết lịch sử + answer sheet | ✅ **Hoàn thành 6/10** | Commit `ecce5cd`; host/player/guest, summary + answers độc lập, visibility defense |
| **N40** | Render ảnh câu hỏi trong gameplay + Quiz Detail | ✅ **Hoàn thành 6/10** | Commit `9b3cdf0`; component `QuestionImage`, Host/Player/Quiz Detail, mapper test + manual pass |
| **N41** | Solo Preview không phòng/host/socket | ✅ **Hoàn thành** | Commit `648bf37`; chơi thử local từ Quiz Detail, 4 types, feedback/tổng kết/chơi lại; kiểm thử nhanh pass |
| **N42** | Hồ sơ + avatar | ✅ **Hoàn thành** | Commit `0da538f`; PATCH delta fullname/phone/description, avatar preview/lưu riêng, session guard; kiểm thử nhanh pass |
| **N43** | Bảo mật tài khoản | ✅ Android xong; user kiểm thử nhanh ổn, XML local PASS | Commit `ab6621b`; 25 test bổ sung đã đối chiếu; full build/lint/CI không suy ra từ unit XML |
| **N44** | Integration + session isolation + E2E/backend gate | 🚧 Manual E2E đã có báo cáo PARTIAL; backlog/gates mở | Xem `N44_E2E_REPORT.md`; cold-start offline và Practice pause/resume còn lỗi; Google/cookie DEFERRED; chưa chốt N45 |
| **N45** | Feature freeze + chốt M6 | ⏳ Chưa làm | Chỉ sau integration pass và mọi blocker có kết quả hoặc quyết định release rõ ràng |
| **N46** | UI polish + animation + dark mode + accessibility | 🚧 WIP theo mốc; đã commit đến Search/Discover `d5571d6` | Thêm Forgot/OTP/Reset, card/room/Home/bottom nav, Search/Discover; xem `N46_UI_PROGRESS.md`. Paging fix chưa có xác nhận retest; build/regression/gate mới chưa chốt |
| **N47–N50** | Test, release hardening, UAT, ship | ⏳ Chưa làm | N48 RC, N50 ship |

**Trọng tâm hiện tại theo quyết định user:** triển khai UI N46 từng màn với user giám sát; đã commit đến Search/Discover tại main `d5571d6`, thêm 7 commit sau Register. Ưu tiên xác nhận retest snapshot fix của Discover và regression Search/password recovery; chưa chốt màn UI kế tiếp, cần user chọn/ảnh/duyệt. N44 PARTIAL với cold-start offline, Practice pause/resume và các gate coverage/backend/privacy/session vẫn mở; N45/M6/release chưa chốt.

### 6.1. ✅ Tuần 1 (N1–N5) đã xong — M1 chốt 9/8

- **Repo + build**: 13 module (`:app` + 5 core + 7 feature) • version catalog + wrapper • build-logic (3 plugin `myquizzapp.android.*`, build file module ~65 → ~17 dòng) • CI GitHub Actions (lint + `testDebugUnitTest` + `assembleDebug`) xanh, gồm cả lần phá build thử → đỏ → revert → xanh.
- **`core:common`**: Result + AppError + 7 domain model (User, Quiz, Question, GameConfig, GameSession, Player, QuizSummary) đọc từ backend thật. `Question` domain **cố ý không có `correct_answer`** (anti-cheat).
- **`core:database`**: Room (`CookieEntity` key `host|name`, `CachedQuizEntity`, `GameHistoryEntity` + DAO + Hilt module). **`core:datastore`**: SettingsDataStore (theme/onboarding — **không lưu token**).
- **`core:network`**: Retrofit + `ApiEnvelope` khớp `response.ts` thật (`AppError.Api` = `message` + `details`, **không có `code`**) • `ResultCallAdapter` unwrap → `Result` • `JsonNamingStrategy.SnakeCase` 1 lần trong NetworkModule • build variants `BASE_URL`/`SOCKET_URL` đặt **trong `core:network`** (BuildConfig sinh theo module).
- **Cookie persistence + refresh**: 2 test MockWebServer xanh local + CI (401 → refresh → retry; refresh chết → clear cookie).

### 6.2. 🔑 Quyết định đã chốt (đừng đổi trừ khi có lý do)

- **Naming đồng bộ `myquizzapp`**: applicationId/namespace `android.kma.myquizzapp` ✅ **Đã đổi 14/8** (Play Console khóa applicationId vĩnh viễn sau publish đầu); build-logic package `com.myquizzapp.buildlogic`; plugin ID `myquizzapp.android.library` / `.compose` / `.hilt`; catalog alias `myquizzapp-android-*`.
- **URLs**: release `BASE_URL = https://api.myquizz.dpdns.org/v1/` (backend đã deploy domain thật; **Retrofit bắt buộc `/` cuối**), `SOCKET_URL = https://api.myquizz.dpdns.org` (không path — lib tự nối `/socket.io/`). Chạy backend local: `http://10.0.2.2:3000/v1/` + `http://10.0.2.2:3000`.
- **DIP cho cookie** (đúng design doc 3.2 + 12.1): `core:network` **không** phụ thuộc `core:database`. Interface `CookieStore` + `StoredCookie` ở `core:common/cookie`; `RoomCookieStore` + `DatabaseBindingModule` (`@Binds`) ở `core:database`; `PersistentCookieJar` chỉ biết interface; `TokenAuthenticator` dùng `Lazy<AuthApiService>` (là **`dagger.Lazy`**, không phải `kotlin.Lazy`) phá vòng lặp DI.
- **Sai khác có chủ đích so với doc**: (1) thêm `CookieStore.clear()` cho logout; (2) `runBlocking` trong `saveFromResponse` (code mẫu doc không compile); (3) authenticator kèm `CookieStore` để clear khi refresh chết; (4) guard path `/auth/` tránh refresh vô ích khi sai password.
- **Dọn dẹp đã làm**: theme XML đổi parent platform theme, `MainActivity` chuyển Compose, xóa `res/layout` + `libs.material`; catalog đã thêm Compose BOM + `activity-compose` + `ui`/`material3`.

### 6.3. 🧠 Bài học N5 (giữ cho retrospective + tránh tái phạm)

- **Room là nguồn sự thật duy nhất cho cookie — không cache RAM song song** (cache lai suýt thành bug prod: Splash gọi `/users/me` trước khi cache warm → user bị hất ra Login).
- Unit test **không hardcode `localhost`** — máy có Docker Desktop resolve host lạ (`kubernetes.docker.internal`); luôn dùng `server.hostName`.
- File test phải nằm `src/test` (đặt nhầm `src/main` → `testImplementation` không có trên classpath).

### 6.4. ✅ Tuần 2 (N6–N10) — Auth & Navigation — **HOÀN THÀNH M2 (13/8)**

- [x] **N6**: Navigation type-safe 3 graph (Auth/Player/Host — xem design doc mục 11) • Splash gọi `GET /v1/users/me` xác định trạng thái login (401 → Login/Guest) — đây là test thật đầu tiên cho cookie jar + authenticator. ✅ **Hoàn thành 12/8**
- [x] **N7–8**: `feature:auth` — Login + Register (UI + ViewModel + UseCase), validate form theo schema backend. ⚠️ `register` **không set cookie** (chỉ trả `{user}`) → sau register auto-login hoặc về Login; response login/register/one-tap đều là `data = { user }`. ✅ **Hoàn thành 12/8**
- [x] **N9**: Google One Tap (Credential Manager) → `POST /v1/auth/google/one-tap` • logout • xử lý 403 deactivated. ✅ **Hoàn thành 12/8** (⚠️ Cần config Web Client ID từ Firebase Console trước khi test)
- [x] **N10**: Integration test auth E2E với backend local (docker-compose: debug `BASE_URL=http://10.0.2.2:3000/v1/`, `SOCKET_URL=http://10.0.2.2:3000`), fix mismatch → **Chốt M2**. ✅ **Hoàn thành 12/8**

**🎨 UI Polish & Architecture Patterns (13/8) — Công việc bổ sung ngoài kế hoạch:**

- [x] **Theme System**: Tạo `core:ui/theme/` với Color.kt (Primary #7B61FF từ logo), Theme.kt (Material3 light/dark), Type.kt (typography definitions). ✅
- [x] **AppTextStyles System**: Tạo `core:ui/style/AppTextStyles.kt` — centralized typography cho toàn dự án với bold buttons/links làm điểm nhấn (titleLarge, bodyMedium, buttonText, linkText, caption). ✅
- [x] **Stateful/Stateless Pattern**: Refactor LoginScreen và RegisterScreen theo pattern: wrapper stateful quản lý ViewModel/effects + composable stateless thuần UI (Preview-friendly, không warning ViewModel). ✅
- [x] **TextField Standardization**: Pattern nhất quán cho tất cả TextField — embedded labels + `OutlinedTextFieldDefaults.colors()` config (focusedTextColor, unfocusedTextColor, focusedBorderColor, cursorColor). ✅
- [x] **Material Icons Extended**: Thêm vào convention plugin `myquizzapp.android.compose` → tất cả module Compose tự động có Icons Extended. ✅
- [x] **Dynamic Image Sizing**: Pattern `with(density) { textStyle.fontSize.toDp() }` để icon scale theo text size. ✅

**🔧 Infrastructure & Bug Fixes (14/8):**

- [x] **Timber Setup**: Added Timber 5.0.1 to `AndroidLibraryConventionPlugin` → all 13 modules automatically get Timber for consistent logging (DRY principle). ✅
- [x] **Google One Tap Fix**: SHA-1 fingerprint (`43:9E:38:4F:D6:7C:F6:A7:EE:04:A0:DF:37:9B:1B:5A:E5:62:64:1D`) + SHA-256 (`79:5B:96:EA:F8:95:68:E7:A0:D1:E6:7C:D0:2A:73:F5:33:6D:E3:11:82:14:77:F6:DE:D8:79:FD:71:33:19:03`) added to Google Cloud Console Android OAuth Client ID by backend team → Google One Tap working. ✅

**🎯 Architecture Enhancements (15/8):**

- [x] **Guest Mode Implementation**: Clean Architecture với AuthState enum (Authenticated/Guest/Unauthenticated), CheckAuthStateUseCase, EnableGuestModeUseCase, SettingsDataStore.isGuestMode flag → Guest users can browse/join without auth, persistent across app restarts (mirrors backend's optionalAuthMiddleware pattern). ✅
- [x] **Navigation Refactor**: Đổi từ 3 graphs riêng (AuthGraph/PlayerGraph/HostGraph) sang unified MainGraph pattern với bottom nav (Home/Discover/Join/Library/Profile) → browse-first UX, soft auth gates, khớp với web frontend và backend optionalAuthMiddleware. Design doc Section 11 đã cập nhật đầy đủ. ✅
- [x] **Forgot Password Feature (Week 2 Final Task)**: ✅ **Hoàn thành 15/8**
  - Data Layer: 3 DTOs (ForgotPasswordRequest, ResetPasswordRequest, ResetPasswordWithOtpRequest) + 3 API endpoints (POST `/users/forgot-password`, `/users/reset-password-token`, `/users/reset-password`) + Repository implementation
  - Domain Layer: 3 UseCases (ForgotPasswordUseCase, ResetPasswordUseCase, ResetPasswordWithOtpUseCase)
  - Presentation Layer: 2 ViewModels (ForgotPasswordViewModel 92 lines, ResetPasswordViewModel 188 lines dual-flow support) + 2 Screens (ForgotPasswordScreen 118 lines, ResetPasswordScreen 172 lines)
  - Navigation: Routes added, composables wired in AuthGraph, LoginScreen "Quên mật khẩu?" link connected
  - Deep Linking: Android App Links implemented với intent-filter trong AndroidManifest.xml (android:autoVerify="true" cho https://myquizz.dpdns.org/reset-password), MainActivity handles onCreate + onNewIntent, AppNavGraph navigates với token từ email link. Backend team added assetlinks.json với SHA-256.
  - Flows: Token flow (primary - click email link) + OTP flow (fallback - manual 6-digit entry)
- [x] **OTP Verification Enhancement (15/8)**: ✅ **Hoàn thành cùng ngày**
  - **Phase 1 - Snackbar Repositioning**: Di chuyển tất cả snackbar lên top (dưới status bar) cho 4 màn auth (LoginScreen, RegisterScreen, ForgotPasswordScreen, ResetPasswordScreen) → không bị che bởi bàn phím
  - **Phase 2 - OTP Verification Screen**: Tạo màn OTP verification trung gian với 6 ô nhập riêng biệt
    - Files mới: `OtpVerificationViewModel.kt` (127 lines) + `OtpVerificationScreen.kt` (220 lines)
    - 6-box OTP input với auto-focus, backspace handling, paste support
    - Nút "Xác nhận" + link "Gửi lại mã"
    - Navigation flow: ForgotPassword → OtpVerification(email) → ResetPassword(email, otp)
  - Files updated: Routes.kt, AppNavGraph.kt (+ import), ForgotPasswordViewModel/Screen, ResetPasswordViewModel (extract email + OTP from navigation)
  - Preview functions: Fix RegisterScreen (2 previews thiếu tham số) + thêm ResetPasswordScreen preview
  - Compilation: ✅ PASS - không có errors

**📊 Week 2 Summary (N6-N10):**
- Milestone M2 đạt 13/8: Auth E2E working (Login/Register/Google One Tap + Cookie auth + Token refresh)
- Bonus achievements: UI polish (theme/typography/patterns), infrastructure (Timber), bug fixes (SHA-1), architectural improvements (guest mode + unified navigation), forgot password feature complete với deep linking + OTP verification screen
- **Status: Week 2 HOÀN THÀNH 15/8. Ready for Week 3 (N11-N20) — Quiz CRUD.**

**✅ Navigation Architecture — Quyết định chốt: Option B — Browse-First Flow (20/8):**

⚠️ **Bối cảnh**: Trong Tuần 2, để test flow sau login, đã tạo **host placeholder screen** làm điểm đích tạm thời sau khi authentication thành công — workaround cho việc màn Home chưa được implement. Sau khi N11 (Home) hoàn thành 17/8, đã đánh giá UX thực tế và **chốt Option B** (Browse-First), bỏ Option A (Auth-First).

**Quyết định & refactor đã thực hiện (20/8):**
- Splash → điều hướng thẳng **Home** (`MainGraph`) trong mọi trường hợp — không còn rẽ nhánh Auth/Guest/Host tại Splash.
- `AuthState` rút gọn từ mô hình 3 trạng thái (kiểu "first launch" riêng) xuống **2 giá trị**: `GUEST`, `AUTHENTICATED`. `CheckAuthStateUseCase` chỉ hỏi backend có đang authenticated không; nếu không → gọi `SettingsDataStore.setGuestMode(true)` và trả `GUEST` (không còn khái niệm "lần đầu mở app" riêng biệt).
- `SplashViewModel`/`SplashScreen` gộp `UiState` về `Loading`/`Ready`, chỉ còn 1 callback `onNavigateToHome`.
- `AppNavGraph.kt`: Splash composable chỉ gọi `onNavigateToHome` → `navigate(Route.MainGraph) { popUpTo<Route.Splash> { inclusive = true } }`.
- Soft auth gates (`RequireAuth`) tại từng route bảo vệ (Library, Profile, CreateQuiz, EditQuiz, CreateRoom...) giữ nguyên như thiết kế — chỉ luồng Splash thay đổi.
- Refresh token / verify-session lúc khởi động: **chưa implement** — để lại `TODO` trong `SplashViewModel` (dự kiến làm ở N12+ khi cần giữ trạng thái đăng nhập bền hơn qua `GET /users/me` hoặc refresh cookie).

**Action items**:
- [x] Xóa host placeholder screen (đã xóa từ N11, thay bằng HomeScreen thật)
- [x] Cập nhật navigation graph theo Option B (20/8)
- [x] Đảm bảo guest mode integration mượt mà với Option B — `CheckAuthStateUseCase` tự set guest mode khi chưa authenticated
- [x] Cập nhật design doc mục 11 (Navigation Architecture) cho khớp luồng Splash mới
- [ ] (N12+) Thêm refresh-token/verify-session use case thật cho Splash khi cần

---

**📚 Học trong tuần**:

- CookieJar trong OkHttp (`saveFromResponse`/`loadForRequest`)
- **Authenticator ≠ Interceptor** cho 401 → refresh → retry (tránh retry vô hạn — đã có guard `responseCount >= 2`)
- HttpOnly cookie trên native khác web thế nào
- Credential Manager lấy Google ID token
- **Stateful/Stateless Composable Pattern**: Cách tách state management khỏi pure UI để hỗ trợ Preview mà không gặp warning ViewModel — pattern chuẩn Android
- **Convention Plugins**: Hiểu sâu hơn cách dùng convention plugin để DRY dependencies (Material Icons Extended example)

**⚠️ Gỡ thói quen cũ**: Clover Chatty dùng JWT + `AuthInterceptor` gắn Bearer — backend này **không đọc** Authorization header, mọi auth đi qua cookie.

**⚠️ Phụ thuộc đã sẵn sàng**: Compose BOM + `activity-compose` + `ui`/`material3` đã có trong catalog (9/8) — N7–8 không bị vỡ tại `setContent {}`.

**🎯 Kết quả đạt được**:

- ✅ M2 milestone hoàn thành đầy đủ theo kế hoạch
- ✅ Bonus: UI/UX chất lượng cao với theme system + typography consistency
- ✅ Bonus: Architecture pattern Stateful/Stateless học được và áp dụng thành công
- ✅ Code quality: Preview-friendly, maintainable, scalable patterns
- ✅ Sẵn sàng cho Tuần 3: Home & Quiz module

### Tuần 3 (N11–15) — Home & Quiz đọc

- [x] **N11**: `feature:home` — search quiz công khai (paging), tab Khám phá/Của tôi. ✅ **Hoàn thành 17/8**

**📝 N11 Implementation Details (17/8):**

**Phase 1-4 Complete (4 Phases):**
- ✅ **Phase 1 - Domain Models** (4 files): QuizOwner.kt, QuizCard.kt (lightweight listing model - NO questions array), Quiz.kt (updated with owner/deletedAt), HomeSection.kt
- ✅ **Phase 2 - Data Layer** (6 files): QuizRepository interface, QuizApiService, HomeContentDto + mappers (snake_case → domain), QuizRepositoryImpl, DI NetworkBindingModule
- ✅ **Phase 3 - Feature Module** (8 files): Home/Search architecture split - HomeViewModel (sections only), SearchViewModel (separate screen with pagination logic), 2 UseCases (GetHomeContentUseCase, SearchQuizzesUseCase), Intent/UiState cho cả 2 screens
- ✅ **Phase 4 - UI Layer + Navigation** (6 files): HomeScreen.kt (TopBar + tabs + sections scroll), SearchScreen.kt (dedicated screen with auto-focus + infinite scroll), QuizCardItem.kt, HomeSectionRow.kt, Routes.kt (added Route.Search), AppNavGraph.kt (wiring)

**Bug Fixes & Refactoring (17/8):**
- ✅ **Result Pattern Fix**: Initial mistake dùng `Result<T, AppError>` (2 type params) → Fixed sang `Result<T>` (1 param) khớp project pattern (6 files: QuizRepository, QuizRepositoryImpl, 2 UseCases, QuizApiService, HomeViewModel import)
- ✅ **API Pattern Fix**: QuizApiService methods trả về DTO trực tiếp → Fixed sang `Result<T>` như AuthApiService; QuizRepositoryImpl dùng `.map { }` thay vì try-catch (theo AuthRepository pattern)
- ✅ **HomeViewModel Corruption Fix**: File bị corrupt với duplicate code → Viết lại clean version
- ✅ **Shared Components Refactoring**: Move QuizCardItem.kt + HomeSectionRow.kt từ feature:home → core:ui/components/ (added Coil import, benefits: reusable cho quiz-manage/leaderboard, Coil đã có sẵn trong core:ui)

**Key Architectural Decisions:**
- ✅ Search architecture: Chọn **separate SearchScreen** (Option B) thay vì same-screen search → cleaner separation of concerns, better UX với auto-focus
- ✅ Backend exploration: Hiểu 2 distinct payload types - **QuizCard** (lightweight, no questions, cho listing) vs **Quiz** (full detail với questions array)
- ⚠️ Pagination: Backend comment ghi rõ "Version 1: Simple list, không có pagination" → UseCase có params page/limit nhưng backend chưa support (để lại cho Paging 3 refactor sau)
- ✅ Host placeholder: Đã remove khỏi AppNavGraph, replaced với HomeScreen thật

**Files Affected Summary:**
- **New files**: 18 files (4 domain models, 3 API/DTO, 2 repositories, 2 UseCases, 4 ViewModels/Intent/UiState, 2 UI screens, 2 shared components moved to core:ui)
- **Modified files**: Quiz.kt (domain), Routes.kt, AppNavGraph.kt, HomeScreen.kt (refactored), SearchScreen.kt (imports updated)
- **Compilation**: ✅ All green, no errors

**📚 Học trong N11:**
- Backend API exploration: Đọc source code `/server/backend` để hiểu response structure (HomeContentDto với sections array, QuizCardDto vs Quiz distinction)
- Result pattern trong project: `Result<T>` với AppError cố định trong Error case, KHÔNG phải generic `Result<T, E>`
- API service pattern: Tất cả Retrofit methods phải trả về `Result<T>` (CallAdapter tự động wrap), repository dùng `.map { }` thay vì try-catch
- Clean Architecture: Shared UI components thuộc core:ui, không nằm trong feature modules
- Search UX patterns: Separate screen vs same-screen search tradeoffs

**⚠️ Technical Debt & Future Work:**
- [ ] Old components trong `feature:home/presentation/components/` có thể xóa (đã move sang core:ui)
- [ ] SearchQuizzesUseCase có params `page/limit` không dùng (backend chưa có pagination) → refactor khi implement Paging 3
- [ ] Backend endpoint `/v1/quizzes/search` chưa có pagination → N13 sẽ học Paging 3 để refactor
- [ ] Auth button UI trong HomeScreen TopBar (TODO comment line 40-42) → discuss implementation sau khi test flow

**🎯 N11 Status: COMPLETE ✅** (17/8 evening) - Tested và chạy ổn, ready cho N12 (Quiz detail + Room cache)

- [x] **N12**: Quiz detail + cache Room. ✅ **Hoàn thành 21/8**

**📝 N12 Implementation Details (21/8):**

- ✅ **Quiz Detail feature**: `QuizDetailScreen` + `QuizDetailViewModel`/`UiState`/`Intent` đặt hẳn ở `feature:quiz-manage/presentation/quizdetail` (không giữ ở `feature:home`) — `GetQuizDetailUseCase` cache-aside: gọi API trước, cache Room khi thành công, fallback đọc cache khi lỗi mạng.
- ✅ **Sửa vi phạm Clean Architecture (DIP) phát hiện giữa chừng**: bản đầu `core:network` import trực tiếp `core:database` để cache quiz detail — vi phạm bảng dependency (project_structure.md mục 2.3). Đã sửa theo đúng pattern `CookieStore`: thêm interface `QuizCacheStore` ở `core:common`, impl `RoomQuizCacheStore` ở `core:database` (`@Binds` trong `DatabaseBindingModule`), gỡ dependency Gradle `core:network → core:database`.
- ✅ **Bug tìm thấy qua đối chiếu backend thật**: `GET /v1/quizzes/:id` trả envelope lồng `{ quiz: {...} }` (giống case `CreateGameSession` đã ghi ở mục 3 #5) nhưng Android map thẳng — gây `MissingFieldException` runtime. Fix: thêm `QuizDetailDto(val quiz: QuizDto)` ở `core:network/dto`, unwrap ở `QuizRepositoryImpl.getQuizDetail`.
- ✅ **Audit edge-to-edge/insets**: rà lại toàn bộ 9 Composable Screen + `MainActivity` — phát hiện `QuizDetailScreen` bottomBar Button thiếu `.navigationBarsPadding()` (bị 3 nút điều hướng che) → đã fix; 8 screen còn lại đã đúng.
- ⚠️ **Kỹ thuật nợ (chưa làm, note lại)**: `searchQuizzes`/`getMyQuizzes` bị lỗi wrapper tương tự (`data: { quizzes: [...] }` chưa unwrap đúng ở `QuizApiService`/`QuizRepositoryImpl`) — cần `QuizListDto(val quizzes: List<QuizCardDto>)` giống cách đã fix quiz detail. **Chưa impl màn dùng đến (list quiz của tôi) nên để làm sau, không block N12.**

**🧠 Bài học N12 (đối chiếu backend + kiến trúc):**

- Backend hay bọc response trong 1 field lồng theo tên resource (`{ quiz: ... }`, `{ session: ... }` — xem mục 3 #5) dù đã có envelope `{success, data, error}` chung — **luôn đọc `*.controller.ts`/`*.type.ts` thật trước khi viết DTO**, đừng suy đoán từ Swagger hoặc naming REST thông thường.
- **Quy ước UiState/Intent/Effect tách file riêng** (`<Feature>UiState.kt`, `<Feature>Intent.kt`, `<Feature>Effect.kt`) được chốt làm chuẩn chung sau khi phát hiện `feature:auth` (5 ViewModel Login/Register/Forgot/Otp/Reset) đi lệch pattern so với `feature:home`/`feature:home/search`/`feature:quiz-manage` (định nghĩa nested trong ViewModel) — đã refactor lại `feature:auth` cho đồng bộ (22/8). **Mọi feature mới đặt UiState/Intent/Effect ở file riêng, không nested trong ViewModel.**
- **Quiz cache (Room) là cơ chế fallback, không phải tính năng offline**: chỉ hữu ích khi mở lại **đúng quiz đã cache trước đó** mà request mạng thất bại (Home/Search không cache danh sách nên không thể chọn quiz mới lúc offline). Quyết định giữ nguyên scope fallback này (22/8), không mở rộng.
- `NetworkModule` (`core:network`) chỉ *nhận* `CookieStore`/`QuizCacheStore` qua constructor injection, không tự `@Provides` — Hilt gộp graph đúng ở `:app` dù 2 module Gradle không biết nhau (xem project_structure.md mục 12.1 design doc).

- [x] **N13–14**: `feature:quiz-manage` — danh sách + tạo quiz; editor 4 loại câu hỏi (`multiple_choice`, `multiple_select`, `short_answer`, `long_answer`). ✅ **Hoàn thành 22/8**

**📝 N13–14 Implementation Details (22/8):**

- ✅ **Domain/Data layer**: mở rộng `QuizRepository`/`QuizRepositoryImpl` với `getMyQuizzes` (Paging 3, `MyQuizzesPagingSource`) và `createQuiz`; thêm `MyQuizzesParams`, `NewQuiz` (domain), `QuizManageDtos.kt` (data) — **nhân dịp này vá luôn nợ kỹ thuật đã ghi ở N12** (`getMyQuizzes` bọc `data: { quizzes: [...] }` chưa unwrap đúng) bằng DTO wrapper giống cách đã fix quiz detail.
- ✅ **UseCase**: `GetMyQuizzesUseCase` (trả `Flow<PagingData<QuizCard>>`), `CreateQuizUseCase`.
- ✅ **Presentation**: package `presentation/quizmanagelist/` (UiState/Intent/ViewModel/Screen — danh sách quiz của tôi, Paging 3, page size 3 theo quyết định người dùng) + package `presentation/createquiz/` (UiState/Intent/Effect/ViewModel/Screen + editor đủ 4 loại câu hỏi).
- ✅ **Navigation**: `Routes.kt` + `AppNavGraph.kt` thêm route danh sách/tạo quiz (`Route.MyQuizzes`, tạo quiz).
- ⚠️ **Quyết định phạm vi (theo khảo sát người dùng)**: upload ảnh quiz/câu hỏi **chưa làm ở bản này** — để lại N15 (S3 presign 2 bước); nếu Paging 3 phát sinh lỗi khó xử lý có phương án fallback tải danh sách thủ công (chưa cần dùng tới, Paging 3 chạy ổn ở bản đầu).

**🎯 N13.5 — Home auth header & màn Profile (bổ sung ngoài kế hoạch, 22/8):**

> Không có trong lộ trình gốc — phát sinh khi làm UI/UX cạnh nút tìm kiếm ở Home mà N11 để lại TODO (xem mục Technical Debt của N11).

- ✅ Bỏ hẳn `TabRow` ("Khám phá"/"Của tôi") khỏi `HomeScreen` — Home giờ chỉ còn nội dung khám phá cuộn dọc.
- ✅ Thêm component auth-aware cạnh nút tìm kiếm trong `TopAppBar` của Home: chưa đăng nhập → nút "Đăng ký/Đăng nhập"; đã đăng nhập → avatar tròn (bấm vào → `Route.Profile`). Có `LifecycleEventEffect(ON_RESUME)` để re-check trạng thái đăng nhập mỗi khi quay lại Home.
- ✅ Màn `ProfileScreen` mới (đặt ở module `app`, không phải `feature:*`, vì gắn `Route.Profile` cấp app) — header avatar + tên + email, item "Quiz của tôi" điều hướng vào đúng màn `quizmanagelist` vừa làm ở N13–14, item "Đăng xuất".
- ✅ **Component `Avatar` chung mới** ở `core:ui/components/Avatar.kt` — quy ước mới cho toàn dự án: nơi nào cần hiển thị avatar user thì dùng component này (qua dependency `core:ui` đã có sẵn ở hầu hết module), **không** tự thêm `coil.compose` riêng — Coil là chi tiết nội bộ của `core:ui`. Chốt sau khi cân nhắc 2 hướng (thêm Coil trực tiếp vào module gọi vs. bọc trong `core:ui`), chọn hướng dùng lại vì avatar sẽ còn xuất hiện ở nhiều màn khác (lobby, leaderboard...).
- ⚠️ **Chưa có Bottom Navigation** (mục 11.4 design doc mô tả 5 tab Home/Discover/Join/Library/Profile) — hiện tại điều hướng Profile/MyQuizzes đi qua `NavController` thông thường từ Home, chưa có bottom nav bar. Xem design doc mục 11.5 (mới thêm) để biết chi tiết sai khác.

- [x] **N15**: Upload ảnh presign S3 2 bước (`UploadImageUseCase`, PUT trực tiếp, không cookie) + Coil. ✅ **Hoàn thành 24/8**

**📝 N15 Implementation Details (24/8):**

- ✅ **Domain/Data layer**: `PresignResult` (domain, `core:common`) + interface `StorageRepository` (`presignUpload(contentType, folder, fileSize)`, `uploadBytes(uploadUrl, contentType, bytes)`); impl ở `core:network` — `StorageApiService` (chỉ `POST /storage/presign`, qua Retrofit/`ResultCallAdapter` như API khác) + `StorageRepositoryImpl` (dùng `StorageApiService` lấy presigned URL, rồi tự PUT thẳng bytes ảnh lên `uploadUrl` bằng OkHttp thuần — **không** qua Retrofit, không cookie/auth header, vì đây là URL S3 presigned bên thứ 3, không phải backend của mình).
- ✅ **DI**: `@RawUploadOkHttpClient` — `OkHttpClient` riêng không có `cookieJar`/`authenticator` dùng cho bước PUT ảnh (client chuẩn có 2 thứ đó sẽ vô tình gắn cookie nội bộ vào request PUT S3, sai domain); `UploadImageUseCase` (feature layer) điều phối: nén ảnh → `presignUpload` → PUT bytes → trả `publicUrl` để gán vào `NewQuiz`.
- ✅ **Nén ảnh**: `ImageCompressor.kt` (resize + nén JPEG trước khi upload) — theo khảo sát người dùng trước khi code (nén = có, avatar = để sau, không làm ở N15).
- ✅ **UI**: viết lại `CreateQuizViewModel.kt` (sửa bug file bị cắt cụt ở bản trước) + `CreateQuizScreen.kt` thêm UI chọn ảnh cover (image picker); thêm `activity-compose` (`libs.androidx.activity` 1.13.0) vào `feature/quiz-manage/build.gradle.kts` cho `rememberLauncherForActivityResult`.
- 🔴 **Bug phát sinh khi test thật trên máy (24/8), đã fix cùng ngày**: Logcat cho thấy request `POST /storage/presign` gửi **snake_case** (`content_type`, `file_size`) dù `PresignUploadRequestDto` đã khai `@SerialName("contentType")`/`@SerialName("fileSize")` tường minh → backend trả `400 VALIDATION_ERROR` (đúng, vì schema thật của backend là camelCase — backend không có lỗi). **Nguyên nhân**: `Json { namingStrategy = JsonNamingStrategy.SnakeCase }` dùng chung toàn app (`NetworkModule.provideJson()`) vẫn biến đổi tên **sau khi đã resolve**, kể cả khi property đã có `@SerialName` tường minh — giả định trước đó rằng `@SerialName` sẽ "thoát" được `namingStrategy` chung là **sai**. **Fix**: tách hẳn `Json`/`Retrofit` riêng chỉ cho `StorageApiService` — 2 qualifier mới `@StorageJson` (Json **không** set `namingStrategy`) + `@StorageRetrofit` trong `Qualifiers.kt`, thêm `provideStorageJson()`/`provideStorageRetrofit()` trong `NetworkModule.kt`, đổi `provideStorageApiService` sang dùng `@StorageRetrofit`. Đã test lại trên máy thật, chạy đúng. Chi tiết đầy đủ ở `knowledgement/n15_knowledgement.md`.
- ⚠️ **Phạm vi đã chốt (giữ nguyên)**: avatar upload **chưa làm** ở N15 (để dịp khác, cùng cơ chế presign này dùng lại được); upload ảnh cover quiz hoạt động đúng luồng presign 2 bước.

- 📚 kotlinx.serialization (`@Serializable`, `JsonElement` cho `correct_answer` đa kiểu), Paging 3, S3 presign.

### Tuần 4 (N16–20) — Socket layer & Lobby

- **N16**: Sửa/xóa quiz, hoàn thiện quiz-manage → **Chốt M3**. ✅ **XONG 25/8**

**📝 N16 Implementation Details (25/8):**

- ✅ **Domain/Data**: `QuizPatch` (core:common, mọi field optional khớp `updateQuizSchema = createQuizSchema.partial()`); `QuizRepository` + `updateQuiz`/`deleteQuiz`; `QuizApiService` thêm `PATCH`/`DELETE quizzes/id/{quizId}`; `UpdateQuizRequestDto` — field null bị omit khỏi body nhờ `explicitNulls=false` (NetworkModule), đúng semantics "field vắng = giữ nguyên" của backend.
- ✅ **`Question` domain thêm `correctAnswer: JsonElement?`** — backend `GET /quizzes/id/:quizId` trả `correct_answer` trong questions (quiz.repository.ts select cả field này); chỉ dùng để pre-fill màn Sửa quiz của chủ quiz, gameplay không đụng model này.
- ✅ **Cache**: `QuizCacheStore.removeQuiz` + `QuizCacheDao.deleteById` mới; update xong ghi đè cache, delete xong xóa cache — quiz đã xóa không "hồi sinh" từ Room khi offline.
- ✅ **Shared editor components** (`presentation/components`, KHÔNG đưa lên core:ui vì chỉ dùng trong quiz-manage): `QuestionDraft` chuyển từ createquiz (thêm `existingImageUrl` + extension `toNewQuestion` dùng chung); `QuestionEditorCard`/`ImagePickerSection`/`QuestionTypeMenuButton` nhận callback riêng lẻ thay vì Intent của từng màn — pattern `QuizEditor.vue` của web (editor dùng chung, màn chỉ khác load/save).
- ✅ **Màn Sửa quiz** (`presentation/editquiz`, 5 file): pre-fill từ quiz detail kể cả đáp án đúng (parse union `number[] | string`); submit gửi metadata + THAY THẾ toàn bộ questions (backend `replaceQuizQuestions` — không có patch từng câu); chỉ upload ảnh mới chọn lúc bấm Lưu (ảnh cũ giữ nguyên URL); dirty flag + dialog "Bỏ thay đổi?" khi thoát (BackHandler + nút back).
- ✅ **QuizDetail**: nút Chỉnh sửa/Xóa chỉ hiện với owner (`AuthRepository.getCurrentUser` so `quizOwner`; guest xem quiz public → 401 → false); dialog xóa cảnh báo hard delete mất cả lịch sử phòng chơi, giữ dialog mở kèm lỗi khi fail (pattern web); `QuizDeleted` effect → popBackStack; reload khi ON_RESUME (bỏ lần resume đầu).
- ✅ **QuizManageList refresh khi ON_RESUME** qua intent `Refresh` → `_refreshGeneration + 1` → `combine` → `flatMapLatest` tạo Pager mới → luôn load lại từ mạng (không dựa vào `LazyPagingItems.refresh()` vốn không đáng tin khi flow đã `cachedIn`).
- 🐛 **Bug thật user phát hiện khi test (đã fix cùng ngày)**: reload không bao giờ chạy ở cả 2 màn vì flag skip-first nằm trong `remember` — Navigation dispose composition khi rời màn nên flag bị reset mỗi lần quay lại → nhánh "bỏ qua" chạy mãi. Fix: `rememberSaveable` (state ghi vào SavedState của NavBackStackEntry). Chi tiết: `knowledgement/n16_knowledgement.md` mục 1.
- ✅ **Bổ sung sau test cùng ngày**: field "Thời gian (giây)" cho editor câu hỏi (`timeLimit` đã có sẵn draft → intent → DTO, chỉ thiếu UI; draft giữ String thô — trống = mặc định 30s, nhập thì validate 5–600s khớp quiz.schema.ts); `fallback`/`error` placeholder cho ảnh cover ở `QuizCardItem` (core:ui) + QuizDetail + MyQuizzes (TODO polish: ảnh mặc định trong res/).
- ⚠️ **Phát hiện khi đọc backend/frontend**: điểm lệch #10 (đã vá ở mục 3) — DELETE là hard delete; PATCH **không thể clear** `quiz_image`/`quiz_description` về null (field vắng = giữ cũ) — nút "xóa cover" của frontend web thực chất là vẽ lại default cover rồi upload (`ensureCover` + `defaultCover.js`), Android giữ hành vi "xóa cover = giữ cover cũ"; ảnh **câu hỏi** thì xóa được thật (questions bị replace toàn bộ). Cần theo dõi: `question_count` do trigger DB (migration 005) duy trì — nếu sau khi sửa câu hỏi mà count sai lệch thì là bug trigger backend, báo chủ backend.

**⚠️ Backlog "Editor UX gaps" (phase 2 sau feature freeze):** duplicate question, move question lên/xuống, autosave draft local + banner khôi phục (`useQuizDraft`), default cover từ `res/`, crop ảnh (cover 1600×1000, câu hỏi 16:9), validation inline theo field + đếm ký tự `x/max`, import xlsx. “Try it yourself” đã được kéo ra khỏi backlog và chốt thành **N41 Solo Preview**.
- **N16.5 (bổ sung khẩn, phát hiện 24/8 khi đối chiếu backend qua GitHub API)** — làm trước khi sang N17, vì ảnh hưởng `core:network`/`feature:auth` đã code ở Tuần 1–2. ✅ **XONG 25/8**
  1. **Sửa luồng Quên mật khẩu sai endpoint**: `core:network`/`feature:auth` đang gọi `/users/reset-password-token` và `/users/reset-password` — không tồn tại trên backend thật. Đổi sang 3 bước ticket thật: `POST /users/forgot-password` (giữ, đúng tên) → `POST /users/password-reset/verify` (body `{email,otp}` hoặc `{token}`, trả `{ticket, expiresAt, email}`) → `GET /users/password-reset/ticket?ticket=...` (peek, tuỳ chọn) → `POST /users/password-reset/complete` (`{ticket, newPassword}`). Ảnh hưởng: 3 DTO request cũ, `AuthApiService`/`UserApiService`, `ForgotPasswordViewModel`, `OtpVerificationViewModel`, `ResetPasswordViewModel` (bỏ nhánh dual-flow token/OTP song song, chuyển thành 3 bước tuyến tính).
  2. **Sửa `ApiEnvelope`/`ApiError` theo envelope lỗi thật**: lỗi REST chỉ có `{code}` (string), không có `message`/`details` như đã cài đặt ở N1–5. Sửa `ApiError` còn 1 field `code: String`; `AppError.Http` đổi `message` → `code`; thêm hàm map `code` → `UiText` tiếng Việt hiển thị cho user (server không trả message).
  3. **Áp dụng pagination cursor thật cho danh sách quiz**: `/v1/quizzes/search`, `/v1/quizzes/me`, `/v1/quizzes/users/id/:ownerId` đã hỗ trợ `cursor`/`limit` (1–24)/`include_total` — khác ghi chú kỹ thuật nợ ở N11/N13 ("backend chưa có pagination"). Refactor `SearchQuizzesUseCase`/`GetMyQuizzesUseCase`/Paging 3 `PagingSource` dùng `cursor` thật thay vì tham số `page` không dùng tới.

**📝 N16.5 Implementation Details (25/8):**

- ✅ **Envelope lỗi thật (điểm lệch #7)**: `ApiErrorBody` chỉ còn `code`; `AppError.Api(message, details)` → `AppError.Api(code)`; `ResultCall` đọc `error.code` từ body; `AppErrorExt.toUserMessage()` viết lại thành map ~60 code trong `shared/errors/codes.ts` → tiếng Việt (fallback chung cho code lạ). Vá cả `StorageRepositoryImpl` (lỗi S3 thô → `AppError.Server`).
- ✅ **Luồng Quên mật khẩu 3 bước (điểm lệch #8)**: service mới `PasswordResetApiService` (forgot-password → verify `{email,otp}` XOR `{token}` → GET ticket peek → complete `{ticket,newPassword}`) với Json/Retrofit RIÊNG (`@PasswordResetJson`/`@PasswordResetRetrofit`) vì module user backend dùng **camelCase thật** trên wire — tái dùng đúng pattern N15. Màn OTP **verify thật ngay khi bấm** (trước chỉ navigate, OTP sai chỉ lộ lúc submit pass); nút gửi lại **đếm ngược 60s kể từ khi vào màn** (RESET_RESEND_TTL tính cả lần gửi ở màn Forgot); sửa text TTL sai "5 phút" → 2 phút (RESET_TTL). Màn Reset bỏ dual-flow + ToggleFlow, chỉ nhận `ticket`, **peek lúc mở màn** để hiện email + chặn sớm ticket hết hạn (10 phút); deep link `?token=` tự verify đổi lấy ticket. Route: `ResetPassword(ticket, token, email)`. 2 usecase cũ (`ResetPasswordUseCase`/`ResetPasswordWithOtpUseCase`) thành stub chờ xóa tay trong IDE.
- ✅ **Search cursor thật (điểm lệch #9)**: `searchQuizzes(keyword, cursor, limit)` xuyên suốt ApiService → Repo → UseCase → ViewModel; `nextCursor`/`hasMore` lấy từ `meta.pagination` thật thay vì đoán `size >= 20`; reset cursor khi đổi query (tránh `QUIZ_CURSOR_INVALID`). `/quizzes/me` vốn đã chuẩn cursor từ N13–14 → không đụng.
- 🐛 **2 hotfix khi user test (bug sót từ N11–12, không phải do N16.5)**: (1) `SearchScreen` **không có đường nào gọi `SubmitSearch`** — không nút tìm, không keyboardActions → gõ từ khóa mà không request nào được gửi ("không có kết quả với mọi từ khóa"); fix bằng icon kính lúp (`leadingIcon`) + `ImeAction.Search` — user quyết KHÔNG real-time/debounce, chỉ submit thủ công. (2) Click quiz card trong search trỏ vào hàm TODO rỗng → giờ navigate thẳng QuizDetail; dọn intent `QuizCardClicked` chết. Kèm sửa luôn bug load-more-lặp-trang-1 (backend không đọc `page`). Chi tiết: `knowledgement/n16_5_knowledgement.md`.
- [x] **N17**: `CreateRoomScreen` tạo phòng từ `GET /v1/games/game-modes`, gọi `POST /v1/games`, lấy token qua `POST /v1/games/:id/host-token`; UI chia editor rõ theo 5 mode nhưng default/constraint vẫn lấy từ backend. ✅ **XONG 28/8**

**📝 N17 Implementation Details (28/8):**

- ✅ **Audit contract thật trước khi code**: modes nằm ở `data.gameModes`; create request dùng `quiz_id`, `session_name`, `mode`, `config`; create response lồng `data.data.session` kèm `ignored`; host token phải gọi endpoint riêng và đọc `data.hostToken.socketToken`.
- ✅ **Networking/DI**: mở rộng `GameApi`, thêm `GameSessionRepository`/`GameSessionRepositoryImpl` + 3 use case; Games dùng shared `@PreserveCaseRetrofit` vì payload trộn outer snake_case với nested `GameConfig` camelCase. Refactor qualifier cũ của Storage/Password Reset thành cặp PreserveCase dùng chung; giữ Retrofit SnakeCase mặc định và `RawUploadOkHttpClient` riêng.
- ✅ **Typed boundary**: `JsonElement`, `JsonNull`, dotted path và logic dựng nested JSON chỉ tồn tại trong DTO/mapper của `core:network`. Domain dùng `GameConfigKey`, `GameConfigValue`, `GameConfigConstraint`; presentation dùng `RoomConfigForm` typed, không cast JSON trong UiState/Intent/Composable.
- ✅ **UI theo mode, không render map mù**: `GameModeConfigEditor` dispatch bằng `when` sang `ClassicModeEditor`, `SoloModeEditor`, `SurvivalModeEditor`, `MarathonModeEditor`, `PracticeModeEditor`. Layout/nhãn thuộc từng editor; default/min/max/nullable/options/editable vẫn lấy từ descriptor backend. Pattern text + switch tách thành `SettingSwitchRow` dùng chung ở `core:ui`.
- ✅ **Validation + patch**: tên phòng 2–100 ký tự; number field validate theo constraint backend; đổi mode reset đúng baseline; chỉ gửi field khác default; typed patch chỉ đổi sang dotted-path JSON tại network boundary. Quy tắc xung đột descriptor: `locked` thắng `editable` (backend hiện báo `flow.allowAnswerLate` trong cả hai collection).
- ✅ **MVI/partial success**: intent typed (`ToggleChanged`/`NumberChanged`/`ChoiceChanged`), guard double-submit. Nếu create thành công nhưng host-token lỗi thì giữ `pendingSession`; retry chỉ gọi host-token, không tạo phòng thứ hai. Unauthorized đi Auth flow.
- ✅ **Navigation**: thành công đi `Route.HostLobby(gameId, socketToken, sessionCode)` và pop CreateRoom; HostLobby thật thuộc N20 nên hiện dùng `HostLobbyPlaceholder` để nhìn rõ mã phòng/Game ID và xác nhận hand-off N17.
- ✅ **Regression tests**: PreserveCase wire JSON, typed key → nested camelCase patch, mode descriptor/locked precedence, `data.data.session`, host-token unwrap và baseline diff. Build/test máy thật đã xanh và code đã push.

**🧠 Bài học N17:**

- Contract động từ backend không đồng nghĩa UI phải render mù bằng `Map<String, JsonElement>`; cách cân bằng tốt là backend sở hữu value/constraint, còn app sở hữu layout/label theo mode.
- Transport representation (`JsonElement`, snake_case, dotted path) phải dừng ở network boundary; để chúng đi vào UiState/Intent vẫn giữ được one-way MVI nhưng làm yếu Clean Architecture và type-safety.
- Chuỗi create session → issue host token là hai request, phải thiết kế như partial transaction để retry idempotent phía client; tuyệt đối không create lại khi request token lỗi.
- Khi backend trả một key trong cả `editable` và `locked`, client phải áp quy tắc deterministic `locked wins` để không vẽ control chỉnh được nhưng server lại âm thầm bỏ qua.

**🧹 Refactor kiến trúc UI trước N18 (28/8, ngoài kế hoạch ngày):**

- ✅ Audit đủ 14 file `*Screen.kt`; `LoginScreen`, `RegisterScreen`, `SplashScreen` đã đúng từ trước, 11 màn còn lại được chuẩn hóa thành Stateful `XxxScreen` + stateless `XxxScreenContent`.
- ✅ Stateful boundary sở hữu ViewModel/Hilt, Flow collection, lifecycle effect, navigation/effect, Photo Picker, `BackHandler`, focus request, pagination observer và transient UI state cần hoist.
- ✅ Stateless content chỉ nhận state/value/callback; Preview gọi content trực tiếp bằng fake state/no-op callback, ưu tiên Light/Dark.
- ✅ Tạo `QuizEditorContent` dùng chung cho Create/Edit Quiz; component có state cục bộ như question type menu và choice field giữ convenience wrapper nhưng bổ sung stateless content API lõi.
- ✅ Không đổi backend contract, ViewModel, repository hay domain behavior; đây là refactor kiến trúc, không đánh số N17.5 và không dịch lịch N18.
- ✅ Build/test máy thật đã xanh và code đã push. Chi tiết: `knowledgement/ui_stateful_stateless_refactor.md`.

- ✅ **N18** (xong 30/8): Socket layer — `GameSocketClient` (namespace `/game`, thay cho ý tưởng `SocketFactory` trong doc), `GameEventMapper`, connect → `lobby:join`, spike reconnect. ⚠️ **Đã vá điểm lệch của doc tại đây**: contract lỗi socket thật là `{ event, code }` (schema `SocketError` trong `backend/src/docs/components/socket.doc.ts`) — KHÔNG có `message`, KHÔNG có prefix `UNAUTHORIZED:`/`FORBIDDEN:`/`CONFLICT:`/`GONE:`. Vì vậy **không thêm `AppError.Socket`** như doc yêu cầu: `AppError.Api(code)` đã map sẵn ~60 code sang tiếng Việt từ N16.5, thêm nhánh mới chỉ là trùng lặp. 4 code fatal (`GAME_TOKEN_INVALID`, `GAME_TOKEN_WRONG_ROOM`, `GAME_ROOM_NOT_FOUND`, `GAME_PLAYER_NOT_FOUND`) → thoát màn thay vì retry; riêng `GAME_TOKEN_INVALID` được thử refresh token đúng một lần trước khi thoát.

**📝 N18 Implementation Details (30/8):**

- **`core:common` (4 file domain mới)**: `GameEvent` (`Connected` / `Disconnected(DisconnectReason)` / `LobbyUpdated(LobbyState)` / `Failed(event, code)` / `Unhandled`), `LobbyState(sessionStatus, config, players, serverTime)`; `GameSocketRepository` (base: `events(socketToken)`, `joinLobby`, `disconnect`) + `HostGameSocketRepository` (`startGame/nextQuestion/pauseGame/resumeGame/endGame`) + `PlayerGameSocketRepository` (`leaveLobby/submitAnswer/requestNextQuestion/sync`). Tách interface theo vai trò để gửi sai lệnh thành lỗi biên dịch, không phải lỗi runtime từ server.
- **`core:network` (7 file mới)**: `GameSocketClient` (`callbackFlow` + `awaitClose` dọn listener và socket, handshake `auth.token`, dịch lý do disconnect thành `DisconnectReason`), `GameEventMapper` (parse bằng `runCatching`, lỗi → `Failed(event, "CLIENT_PARSE_ERROR")`), `GameSocketEvents` (hằng tên 19 server event + các client event), `SocketDtos` (snake_case bằng `@SerialName` + `@PreserveCaseJson` — vẫn là cái bẫy của N15), `HostGameSocketRepositoryImpl`, `PlayerGameSocketRepositoryImpl`, `SocketBindingModule`.
- **`core/common/build.gradle.kts`**: thêm `api(libs.kotlinx.coroutines.android)` — phải là `api` chứ không `implementation`, vì `Flow` nằm trong signature công khai của interface repository.
- **`feature:lobby` (6 file mới)**: `HostLobbyUiState`/`HostLobbyIntent`/`HostLobbyEffect`/`HostLobbyViewModel`/`HostLobbyScreen` theo baseline Stateful + `HostLobbyScreenContent` stateless, cùng `RefreshHostTokenUseCase`. Chưa thêm Intent `StartGame` vì N18 không có UI trigger thật cho nó (luật "mọi Intent phải có đường kích hoạt thật") — để N20.
- **Reconnect (spike đã thành hàng thật)**: `lobby:join` gọi lại sau mọi `Connected`; mất mạng → trạng thái `RECONNECTING`, giữ nguyên danh sách người chơi cũ; `io server disconnect` → thoát hẳn; `GAME_TOKEN_INVALID` → refresh token qua REST đúng một lần rồi kết nối lại, vẫn fail thì thoát.
- **Test**: `GameEventMapperTest` 6 case (mixed snake/camel của `lobby:updated`, thiếu `config`, `error` giữ nguyên code, payload rác, payload null, event gameplay → `Unhandled`), fixture copy từ `socket.doc.ts`, dùng chính `NetworkModule.providePreserveCaseJson()` của production thay vì tự tạo `Json` trong test.
- **Nợ nhỏ để lại**: `onExit(message)` ở nav graph chỉ `popBackStack`, chưa hiển thị lý do bị buộc rời phòng (N19 — ✅ đã trả 5/9: `KEY_LOBBY_EXIT_MESSAGE` qua `savedStateHandle` + snackbar ở JoinRoom); `submitAnswer` chưa xử lý ack (N21+); `player_avatar`/`lives` chưa vào DTO (N19 — ✅ đã thêm 5/9).

**📝 N18.5: Polish Architecture (31/8 - 2/9/2026):**

**Mục tiêu:** Chuẩn hóa Clean Architecture + MVI pattern toàn dự án sau khi hoàn thành N18.

**Thành tựu:**
- ✅ Sửa 5 vi phạm nghiêm trọng architecture: navigation logic in ViewModel (Home, Profile), god methods (CreateQuiz/EditQuiz submit ~50-65 lines), layer violations (ViewModels gọi trực tiếp multiple repositories thay vì qua UseCase)
- ✅ Chuẩn hóa naming conventions: `handleIntent` → `onIntent` (26 occurrences, 16 files), `GameApi` → `GameApiService` (9 occurrences, 3 files)
- ✅ Refactor navigation: tách `AppNavGraph.kt` (~267 lines) thành 4 feature graphs — `AuthNavGraph` (5 routes), `MainNavGraph` (6 routes), `QuizManageNavGraph` (5 routes), `GameNavGraph` (5 routes). AppNavGraph còn 60 lines (giảm 78%), chỉ giữ Splash + gọi 4 sub-graphs.
- ✅ Thống nhất validation pattern: tạo 6 validators tái sử dụng ở `:core:common/validator` (ValidationResult, EmailValidator, PasswordValidator, QuizNameValidator, QuizDescriptionValidator, RoomSettingsValidator) thay vì inline validation rải rác; refactor 5 ViewModels (Login, Register, CreateQuiz, EditQuiz, CreateRoom) sang Pattern C.
- ✅ Extract orchestration UseCases: `CreateQuizWithAssetsUseCase`, `UpdateQuizWithAssetsUseCase` (thay CreateQuiz/UpdateQuiz/UploadImage riêng lẻ), `GetQuizWithOwnershipUseCase` (thay GetQuizDetail) — ViewModels giảm từ 50-65 lines submit logic xuống ~30 lines.

**Files affected:** 30+ files across 8 modules (app, core:common, core:network, feature:auth, feature:home, feature:quiz-manage).

**Lessons learned:**
- MVI Effect pattern phải áp dụng đồng nhất từ đầu cho mọi ViewModel — navigation/one-shot events KHÔNG BAO GIỜ ở ViewModel state, luôn qua Effect channel.
- God methods (>40-50 lines) trong ViewModel.submit() là dấu hiệu thiếu UseCase orchestration layer — phải extract business logic ra UseCase, ViewModel chỉ validate + build DTO + delegate.
- Validation logic dùng lại (email, password, quiz name) phải centralized ở `:core:common` thay vì duplicate ở mỗi ViewModel — Pattern C (validator objects) scale tốt hơn inline checks.
- Navigation modular theo feature (4 NavGraphs) dễ maintain và parallel development hơn monolithic AppNavGraph — mỗi feature module có thể contribute routes riêng.

**Chi tiết kỹ thuật:** `REFACTOR_CHECKLIST.md` (628 lines, 14 tasks, 4 sprints). Build/test thành công trên máy thật, code đã push 2/9.

### 6.8. N19–N20.6 — Lobby và Discover

- [x] **N19**: `feature:lobby` Player — lookup room, join REST → `socketToken` → connect; PlayerLobbyScreen. ✅ **XONG 5/9**

**📝 N19 Implementation Details (3–5/9):**

- ✅ **Audit backend trước khi code** (đọc `game.route.ts`, `game.schema.ts`, `game.controller.ts`, `game.service.ts`, `socket.channels.ts`): `GET /games/:code` là **public**, `POST /games/:code/join` dùng **optionalAuth** (có cookie → player thật, không có → khách); `joinGameSchema` nhận `player_name` 1–50, `player_guest_id` **phải là UUID**; thứ tự guard của server: 404 `GAME_ROOM_NOT_FOUND` → 409 `GAME_ALREADY_STARTED` → 403 `GAME_GUESTS_NOT_ALLOWED` → 403 `GAME_HOST_CANNOT_JOIN` → 409 `GAME_ROOM_FULL`. Join trả `data.player` + `data.socketToken` **phẳng** (khác host token lồng một cấp `data.hostToken.socketToken`). Không có endpoint refresh token cho player — khác host, nên `GAME_TOKEN_INVALID` của player là fatal, join lại từ đầu.
- ✅ **`core:common`**: `RoomLookup` (+ `isOpenForJoin`, `isFull`), `JoinRoomResult`/`JoinedPlayer`, `LobbyPlayer` thêm `playerAvatar`/`lives` (trả nợ N18), `NicknameValidator` (1–50 ký tự, khớp schema backend), `GameSessionRepository` thêm `lookupRoom`/`joinRoom`.
- ✅ **`core:network` + `core:datastore`**: `GameApiService` thêm 2 endpoint; `GameDtos` thêm `RoomLookupResponseDto`/`LobbySnapshotDto`/`JoinGameRequestDto`/`JoinGameResponseDto`/`JoinedPlayerDto`; `LobbyPlayerDto` (socket dto) **dùng lại cho cả REST** — backend trả bản `Pick` khi đọc Postgres và full row khi Redis còn nóng, DTO có default nên parse được cả hai. `GuestIdentityStore` mới (DataStore): UUID sinh **lần đầu cần join** rồi giữ mãi — để backend nhận ra cùng một khách khi reconnect/xem lịch sử (`x-guest-id`).
- ✅ **UseCase**: `LookupRoomUseCase` (normalize mã về uppercase + trim trước khi gọi), `JoinGameUseCase` (tự cấp guest UUID khi chưa đăng nhập).
- ✅ **Presentation (3 package mới, đúng baseline Stateful/Stateless + UiState/Intent/Effect tách file)**: `joinroom` (nhập mã phòng), `guestnickname` (chỉ khách thấy màn này), `playerlobby` (realtime qua `PlayerGameSocketRepository`, hiện danh sách người chơi + trạng thái kết nối, host thoát/huỷ phòng → pop kèm lý do).
- ✅ **Navigation**: `Route.GuestNickname(sessionCode)` mới; `GameNavGraph` viết lại với `KEY_LOBBY_EXIT_MESSAGE` + helper `popWithMessage`; `MainNavGraph` đọc message từ `savedStateHandle` của entry đích rồi hiện snackbar ở JoinRoom — **pattern chuẩn cho mọi "kết quả trả về khi pop"** từ nay.
- 🔴 **Bug thật khi test trên máy (đã fix cùng ngày)**: crash `MissingFieldException` ở bước tra phòng — nguyên nhân là điểm lệch #11 (`data.session.session`). Fix bằng `LobbySnapshotDto` + KDoc ghi rõ đây là bug backend và cách dọn khi backend sửa. Rà lại toàn bộ mapping REST/socket của game trong cùng phiên: 5 endpoint + `lobby:updated` còn lại đều đúng, không phải sửa thêm.
- ✅ **Test**: `GameDtosTest` thêm 3 case (payload lồng ba cấp → `RoomLookup` + đếm player, join trả `socketToken` phẳng + bỏ qua cột thừa của `player_sessions`, body join của người đã đăng nhập encode ra `{}`); `GameEventMapperTest` thêm 2 case cho `player_avatar`/`lives`.
- ⚠️ **Nợ nhỏ để lại**: danh sách `players` lấy sẵn ở bước tra phòng chưa được đổ vào state đầu của PlayerLobby (socket `lobby:updated` fill ngay sau đó nên chưa cần); nút "Vào phòng" tạm ở Home đã bỏ ở N19.5 (thay bằng tab Tham gia); 2 file usecase stub từ N16.5 vẫn chờ xóa tay trong IDE.

**🧠 Bài học N19:**

- **Không tin tên key trong controller**: `success(res, { session })` đọc rất thuyết phục nhưng biến `session` ấy do `getLobby` trả và chứa cả `{session, players, config}`. Phải truy tiếp vào `*.service.ts` mới biết shape thật — mở rộng bài học N12 ("đọc controller thật") thêm một tầng.
- **Đọc lỗi kotlinx như một manh mối, không chỉ là stacktrace**: danh sách "fields ... were missing" kèm `at path:` đủ để định vị lỗi. Field nào **không** bị báo thiếu chính là field thật sự có trong JSON — ở đây `config` vắng mặt trong danh sách đã tỏ ra cấp ngoài là cụm lobby chứ không phải object rỗng.
- **Client phải map theo payload đang chạy, không theo payload đúng** — nhưng phải ghi KDoc nói rõ đây là bug backend + cách dọn, kèm test hồi quy để khi backend sửa thì test đỏ ngay thay vì user gặp crash.
- **Đếm tại chỗ thay vì tin cột tổng hợp**: `total_players` chỉ được flush cuối ván nên luôn lệch trong lobby — kiểm tra "phòng đã đầy" phải đếm `players.size`.
- **Quyền do server cấu hình, client chỉ phản ánh**: cho khách vào hay không nằm ở `config.lobby.allowGuests` của từng phòng; client không được hardcode chính sách, chỉ đọc cấu hình và hiển thị đúng lý do khi bị chặn.
- **Nav result pattern**: muốn trả dữ liệu về màn trước lúc pop thì ghi vào `savedStateHandle` của back stack entry đích rồi đọc-và-xóa ở đó; đừng nhồi vào route argument hay ViewModel dùng chung.
- [x] **N19.5** (bổ sung ngoài kế hoạch): Bottom Navigation thật cho `MainGraph` — trả nợ từ N13.5 (design doc 11.4/11.5/11.6). ✅ **XONG 6/9**

**📝 N19.5 Implementation Details (6/9):**

- **Pass 1 — khung điều hướng**: `navigation/MainScaffold.kt` mới (`TopLevelTab` 5 tab, `MainBottomBar`, `navigateToTab` với `popUpTo<Route.Home>` + `saveState`/`restoreState`); `AppNavGraph` bọc `Scaffold` **ngoài** `NavHost`, bar chỉ hiện khi destination `hasRoute` 1 trong 5 tab nên màn con/màn game tự ẩn, `consumeWindowInsets` để không cộng dồn padding; thêm `presentation/activity/ActivityScreen.kt` placeholder.
- **Chốt trùng lặp Library/MyQuizzes**: xóa `Route.Library`, tab Thư viện trỏ thẳng `Route.MyQuizzes` — màn thật đã có từ N13, KHÔNG xây lại; `QuizManageListScreen.onNavigateBack` đổi thành nullable để bỏ nút back khi màn đóng vai tab gốc.
- **Profile rút gọn**: bỏ item "Quiz của tôi" + back arrow → Profile chỉ còn thông tin và (về sau) cài đặt.
- **Pass 2 — avatar & nhãn**: `navigation/CurrentUserViewModel.kt` mới (scope Activity, `avatarUrl`, `refresh()` tự dedupe) cấp avatar cho tab Hồ sơ, refresh đúng 4 mốc (dựng bar, `ON_RESUME`, rời `AuthGraph`, đăng xuất); bỏ avatar ở TopBar Home; cỡ chữ nhãn tính theo bề rộng màn hình để luôn 1 dòng và căn giữa.
- **Pass 3 — polish**: bỏ nút Tham gia phóng to + nền tròn, mọi tab cùng cỡ icon 24dp; `TopLevelTab.iconRes` cho phép truyền drawable, tab Tham gia dùng `R.drawable.app_logo` vẽ bằng `Image` để không bị `Icon` nhuộm màu.
- ⚠️ **Nợ để lại**: tab Hoạt động vẫn là placeholder — màn thật cần backend có `role=all` với cursor thống nhất (hiện chỉ `role=played|hosted`, tuyệt đối không merge phía client); `getCurrentUser()` chưa cache nên Home (`CheckAuthState`) và bottom bar gọi `/users/me` 2 lần riêng; `app_logo.png` là bitmap, nếu ở 24dp trông rối thì làm vector đơn sắc `ic_logo_mono.xml` rồi thay vào `iconRes` (không phải sửa code).

**🧠 Bài học N19.5:** xem `knowledgement/n19_5_knowledgement.md`.

- [x] **N19.6** (bổ sung ngoài kế hoạch): Session state một nguồn + gác đăng nhập + xóa màn Join + nút "Xem thêm" ở Home. ✅ **XONG 6/9**

**📝 N19.6 — Kế hoạch chi tiết (chốt 6/9, làm trước N20):**

Bốn lượt, **lượt 1 phải xong trước** vì cả gác đăng nhập lẫn bug avatar đều đọc từ nó.

- **Lượt 1 — `SessionRepository` + `StateFlow` (sửa gốc)**: interface ở `core:common`, impl `@Singleton` ở `core:network`; `StateFlow<AuthState>` = `Unknown` / `Guest` / `LoggedIn(user)`; `refresh()` tự chống gọi trùng; `markLoggedOut()`. `AuthRepositoryImpl.getCurrentUser()` đi qua nó ⇒ hết cảnh gọi `GET /users/me` 2–3 lần một phiên. Ba bản sao trạng thái hiện tại (`HomeUiState.currentUser`, `ProfileUiState.user`, `CurrentUserViewModel.avatarUrl`) trở thành bên **thu** ⇒ bug "đăng xuất rồi vào lại Hồ sơ vẫn thấy account cũ" tự hết, **không cần** vá bằng `ON_RESUME`. Bỏ được callback `onCurrentUserChanged` thêm ở N19.5; xóa `CurrentUserViewModel` nếu bottom bar đọc trực tiếp được session state.
- **Lượt 2 — `AuthRequiredDialog` ở `core:ui`**: stateless (`title`, `message`, `onLogin`, `onRegister`, `onDismiss`), **không** biết `Route` cũng không biết auth state. Gắn vào các chốt gác ở mục dưới, dùng lại luôn cho `GuestBlockedDialog` (khách bị host chặn) và empty state của Hồ sơ.
- **Lượt 3 — xóa màn Join**: bốn việc màn này đang gánh phải chuyển đi hết, nếu không sẽ mất thầm lặng.
  1. Ô nhập mã + validate + `JoinRoomViewModel` (3 đường ra) → tách thành composable công khai `JoinCodeCard` ở `:feature:lobby`, `HomeScreen` nhận qua **slot** `joinSlot: @Composable () -> Unit` do `:app` nối dây (tránh `:feature:home` phải phụ thuộc `:feature:lobby`).
  2. `GuestBlockedDialog` → thành khách hàng đầu tiên của `AuthRequiredDialog`.
  3. **Snackbar lý do bị buộc rời phòng** (`KEY_LOBBY_EXIT_MESSAGE`, nợ đã trả ở N18): Join từng là "màn duy nhất còn sống sau khi lobby đóng" ⇒ **Home phải nhận việc này** và `GameNavGraph.popWithMessage` phải trỏ lại về Home, nếu không thông báo "host đã đóng phòng" mất im lặng.
  4. Deep link mã phòng → rà lại `AppNavGraph(initialDeepLinkToken)` sau khi `Route.JoinRoom` bị xóa.
  Kèm theo: xóa `TopLevelTab.JOIN` ⇒ bottom nav còn **4 tab**, `rememberTabLabelFontSize()` tự tính lại nên **không phải sửa gì** phần cỡ chữ nhãn.
- **Lượt 4 — nút "Xem thêm" ở Home**: căn phải cạnh tiêu đề section, **không tạo section cha "Khám phá"** (backend `home_sections` là danh sách phẳng theo `position`, bọc cha sẽ biến cấu hình server-driven thành taxonomy hardcode ở client). Bố cục Home: ô nhập mã → **"Tiếp tục chơi" pin cứng ở đây** (client cố ý ghi đè `position` của riêng `continue`, vì nó là type user-specific duy nhất) → các section còn lại theo `position` → nút full-width "Khám phá tất cả" ở cuối trang.

**Đích của nút "Xem thêm" theo `section_type`** (`HomeSectionDto` đã mang sẵn `section_key` + `section_type`, không phải thêm field):

| Section | Đích | Khớp |
| --- | --- | --- |
| `trending` | `GET /quizzes/feed` (không `topic`) | 100% — cùng `ORDER BY hot_score DESC, id DESC` |
| `category` | `GET /quizzes/feed?topic=<category_name>` | 100% |
| `newest` | `GET /quizzes/search?sort=newest` (keyword optional) | 100% — khác endpoint, khác định dạng cursor |
| `featured` | **không có nút** | không endpoint nào lọc `is_featured` |
| `continue` | **không có nút** | dữ liệu cá nhân, sắp theo `last_played_at` |

Section không có nút **không phải lỗi** — nó trông "được tuyển chọn có hạn", đúng bản chất staff picks. Định danh section phải dùng `section_key`/`section_type`, **không dùng `title`** (title là text hiển thị, sửa được trong DB).

**Danh sách chốt gác đăng nhập:**

| Chỗ | Gác thế nào | Căn cứ |
| --- | --- | --- |
| Tab **Thư viện** | chặn ngay trong `onSelect`, hiện dialog | `listing.controller.ts::getMyQuizzes` ném `AppError(401, …, 'QUIZ_AUTH_REQUIRED')` |
| **FAB Tạo quiz** | kiểm tra lại (rẻ, chỉ đọc StateFlow) | chốt của user — hơi thừa vì FAB chỉ nằm trong Thư viện |
| Nút **"Tạo phòng chơi"** ở QuizDetail | chặn khi bấm | `QuizDetailScreen` bottomBar hiện **không có** kiểm tra auth nào |
| **Hồ sơ** | empty state + CTA đăng nhập/đăng ký (không phải dialog) | sau này thêm section cài đặt/giới thiệu |
| Tab **Hoạt động** | **không gác** | history nhận khách qua header `x-guest-id` |

Ở màn cấu hình phòng: **giữ** `CreateRoomEffect.RequireAuthentication` nhưng đổi vai thành **lưới an toàn cho lỗi 401** (phiên có thể hết hạn trong lúc user điền form), thay vì xóa đi để lại ngõ cụt lỗi đỏ. Hành vi "bấm là gọi API luôn" vốn đã có: `submit()` → `createGameSession(params)` → `requestHostToken(session)`.

**Mốc N20.5 đã hoàn thành 13/9**: `Route.Discover(sectionKey, sectionType, title, topic)` trỏ vào màn Khám phá thật, preselect đúng query từ Home và cho đổi filter/sort ngay trong màn. `/feed` dùng cho Xu hướng/category mặc định; public-only `/search` dùng cho các sort còn lại; cả hai phân trang cursor bằng Paging 3.

**Xin backend (chưa gấp)**: thêm tham số `sort`/`section` cho `GET /quizzes/feed` để `featured` cũng phân trang được.

**📝 N19.6 Implementation Details (6/9):**

- **Session state một nguồn**: `SessionState` (`Unknown`/`Guest`/`LoggedIn(user)`) + `SessionRepository` (`StateFlow`, `refresh()`, `onAuthenticated()`, `onSignedOut()`) ở `core:common`, impl `@Singleton` ở `core:network`; `ObserveSessionUseCase`/`RefreshSessionUseCase` cho presentation. `HomeUiState`/`ProfileUiState`/`CurrentUserViewModel` chỉ còn **thu**. Bug "đăng xuất rồi vào lại Hồ sơ vẫn thấy account cũ" hết từ gốc — nguyên nhân thật: `ProfileViewModel.init{}` không chạy lại vì back stack entry của tab được `saveState`/`restoreState` giữ nguyên khi đổi tab. Đã xóa `AuthState`, `CheckAuthStateUseCase`, `GetCurrentUserUseCase`, `HomeIntent.CheckAuthState` và callback `onCurrentUserChanged` thêm ở N19.5 ⇒ trả luôn nợ "gọi `GET /users/me` 2 lần một phiên".
- **Gác đăng nhập**: `AuthRequiredDialog` ở `core:ui`, `requireAuth` do `AppNavGraph` sở hữu và chặn **trước** khi điều hướng. 4 chốt: tab Thư viện (chặn ngay ở `onSelect` của bottom bar), FAB tạo quiz, nút "Tạo phòng chơi" ở QuizDetail, Profile (empty state có nút đăng nhập). Chỉ chặn khi `isConfirmedGuest`, `Unknown` đi qua. Bỏ gác trong màn cấu hình tạo phòng (đã gác ở ngoài) nhưng **giữ** `CreateRoomEffect.RequireAuthentication` làm lưới an toàn cho 401 giữa luồng.
- **Xóa màn Join + tab Tham gia**: bottom nav còn **4 tab** (Trang chủ / Thư viện / Hoạt động / Hồ sơ), `Route.JoinRoom` và `TopLevelTab.iconRes` đã xóa. Ô nhập mã trở thành `JoinRoomCard` nhúng vào Home qua slot `roomCodeCard: @Composable () -> Unit` do `MainNavGraph` truyền vào (giữ `feature:home` độc lập với `feature:lobby`); `KEY_LOBBY_EXIT_MESSAGE` chuyển sang entry của Home và lỗi hiện **inline** trong card thay vì snackbar.
- **Home**: đúng MỘT `LazyColumn` — card nhập mã là `item` đầu tiên và **cuộn đi được** (bản ghim đầu tiên bị bỏ vì che nửa màn hình khi lướt) → `continue` ghim ngay dưới (client đè `position` của backend, không có nút) → các section còn lại theo `position` trong `home_sections`, nút "Xem thêm" chỉ hiện khi `sectionType in {trending, category, newest}` (xét theo `section_type`, KHÔNG theo `title` vì ops sửa được title bằng SQL) → nút "Khám phá tất cả". Loading/lỗi/rỗng = MỘT `DiscoverStatusCard` (`heightIn(min = 200.dp)`) thay chỗ khối Khám phá, card nhập mã không bị ảnh hưởng. `Route.Discover(sectionKey: String? = null)`.
- **Mã phòng dài đúng 6**: server `generateSessionCode(len = 6)`, bảng chữ `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (bỏ I/O/0/1), và backend **không** validate độ dài ở schema nào ⇒ client là chốt duy nhất. `SESSION_CODE_LENGTH = 6`, nút bật khi `length == 6` (không phải `>= 6`), ô nhập lọc chữ+số rồi cắt cứng ở 6 ký tự; cố tình không lọc I/O/0/1 lúc đang gõ.
- **Nợ ghi lại**: đổi `title` của section `continue` trong `home_sections` bằng SQL ("Tiếp tục chơi" hứa hẹn sai — thực chất là *quiz chơi dở*, không phải phòng đang mở); tab Hoạt động vẫn placeholder; `featured` vẫn chưa có đích phân trang.
- 🧠 Bài học chi tiết: `knowledgement/n19_6_knowledgement.md`.

- [x] **N20**: HostLobbyScreen + `lobby:config-update` (ACK `{ok, changed, config, ignored}`); chia sẻ mã phòng (QR cắt khỏi phạm vi, thay bằng 2 nút copy mã/link). ✅ **XONG 10/9**

**📝 N20 Implementation Details (10/9):**

- ✅ **Audit backend trước khi code** (`config.rule.ts`, `game.socket.ts`, `game.service.ts`) — 4 điểm lệch so với giả định của kế hoạch: (1) `normalizeConfig` **ghi đè** giá trị host gửi lên (`perQuestionSeconds === 0` → `speedBonus=false`; self-paced + `between_questions` → `end_only`; `reviewMode` → cưỡng bức `showCorrectAnswer=true`); (2) `game:start` **không có ack**, chỉ `lobby:config-update` và `question:answer` mới có; (3) `changed` là so sánh `JSON.stringify` toàn bộ object nên `changed=false` KHÔNG có nghĩa lệnh bị từ chối; (4) chốt lobby-only nằm ở `writeConfig` (409 `GAME_LOBBY_ONLY`), không nằm ở handler `onConfigUpdate`.
- ✅ **Socket ack layer (`core:network`)**: `GameSocketClient.emitWithAck(event, payload, timeoutMs = 5_000)` dùng `suspendCancellableCoroutine` + `withTimeoutOrNull` — bắt buộc có timeout vì socket.io không gọi ack và cũng không báo lỗi khi mạng rụng đúng lúc emit, không timeout thì nút "đang lưu" treo vĩnh viễn. `SocketAckResult` (`Payload`/`Timeout`/`NotConnected`) + `SocketAckMapper` (`CLIENT_ACK_TIMEOUT`, `CLIENT_NOT_CONNECTED`) + `ConfigUpdateAck(changed, config, ignored)`. `GameEvent.GameStarted(mode, config, totalQuestions, serverTime)` mới ở `core:common`.
- ✅ **Editor cấu hình chuyển lên `core:ui/gameconfig`** (trả nợ N17 đặt sai module — `feature:lobby` không được phụ thuộc `feature:quiz-manage`): 6 file `RoomConfigForm` (state) / `BooleanSettingRow` / `NumberSettingField` / `ChoiceSettingField` / `GameModeConfigEditor` (dispatch theo mode) / `GameConfigPatchBuilder` (diff). Ràng buộc `core:ui` không có Hilt và không có kotlinx.serialization hóa ra là bộ lọc tốt: chỉ hàm thuần + composable stateless mới lên được. `ChoiceSettingSegment` đổi thành dropdown (`ChoiceSettingField`) vì 3 lựa chọn leaderboard có nhãn dài.
- ✅ **Baseline của patch là tham số, không hard-code**: `buildGameConfigPatch(descriptor, values, baseline = descriptor.defaultBaseline())` — màn tạo phòng giữ baseline default, host lobby truyền `GameConfig.baselineFor(descriptor)` (config thật của phòng). Không sửa chỗ này thì "tắt lại một field vốn khác default" cho ra patch rỗng và UI vẫn báo đã lưu.
- ✅ **`feature:lobby`**: `GetGameModesUseCase` + `UpdateRoomConfigUseCase` (`require(patch.isNotEmpty())`), `HostLobbyUiState` mở rộng (`config`/`mode`/`descriptor`/`configForm`/`invalidConfigKeys`/`isConfigSheetOpen`/`isLoadingSpec`/`isSavingConfig`/`configNotice`/`isStarting` + derived `canEditConfig`/`isConfigFormEnabled`/`canStartGame`/`shareLink`). Spec tải **lazy lúc mở sheet lần đầu** bằng 2 `async` song song (không endpoint nào trả cả session + config spec), cache lại cho các lần mở sau.
- ✅ **UX bottom sheet (chốt sau khi user test)**: sheet **chỉ ở lại khi lỗi validate cục bộ** (field sai định dạng — lỗi nằm trong form nên đóng đi là phá dữ liệu đang gõ). Mọi kết quả khác đóng sheet + snackbar: lưu thành công, patch rỗng ("Chưa có thay đổi nào để lưu."), lỗi mạng/timeout/mất socket, phòng đã rời LOBBY. Tiêu chí phân loại không phải "thành công/thất bại" mà là "còn việc cho người dùng làm trong sheet hay không". Nút Lưu disable ngay khi bấm, nút Đóng disable trong lúc chờ ack, ViewModel có guard `if (isSavingConfig) return`.
- ✅ **Bắt đầu trận**: `game:start` fire-and-forget → thành công chỉ được xác nhận bởi `game:started`; `startTimeoutJob` 5s tự hồi nút + báo "Máy chủ chưa xác nhận trận bắt đầu, hãy thử lại." Dialog cảnh báo khi phòng chưa có ai (`rememberSaveable` theo bài học N16). Điều hướng `Route.HostGame` + `popUpTo<Route.HostLobby>{inclusive=true}`; `HostGamePlaceholder` private trong `GameNavGraph.kt` chờ N21 (`feature:game-host` vẫn rỗng).
- ✅ **Chia sẻ phòng**: bỏ QR khỏi phạm vi (cần thêm thư viện, mà người nhận vẫn phải có app), thay bằng `SessionCodeCard` với 2 `OutlinedButton` "Copy mã" / "Copy link" dùng `LocalClipboardManager`. `WEB_ORIGIN` để `const val` trong `HostLobbyUiState` thay vì `BuildConfig` — sai lệch có chủ ý, ghi lại để biết chỗ đổi khi cần theo môi trường.
- ✅ **Test**: `SocketAckMapperTest` (7 case) + `GameStartedMapperTest` (3 case) ở `core:network`, `GameConfigPatchBuilderTest` (6 case, 4 case mới cho baseline) ở `core:ui`.
- 🐛 **Build fail thật khi thêm `GameEvent.GameStarted`**: `'when' expression must be exhaustive` ở CẢ `HostLobbyViewModel` và `PlayerLobbyViewModel` — compiler làm đúng việc, buộc trả lời "player lobby phản ứng thế nào khi trận bắt đầu" thay vì để rơi vào `else`. Quy trình từ nay: thêm nhánh vào sealed class dùng chung ⇒ rà `when` ở TẤT CẢ module tiêu thụ trước khi build.
- ⚠️ **Nợ để lại**: KDoc mục (2) của `HostGameSocketRepository` còn ghi sai rằng server không kiểm `session_status` khi update config; nhánh `ignored` **không thể kích hoạt từ UI** (field locked bị ẩn hẳn nên client không bao giờ gửi field mode không nhận — nó là lớp phòng thủ cho trường hợp backend đổi `MODE_CONFIG_SPEC`); nhánh timeout của `game:start` cũng khó test thủ công vì nút disable ngay khi mất mạng; dark mode của sheet hoãn sang N46 sau feature-complete gate.
- 🧠 Bài học chi tiết: `knowledgement/n20_knowledgement.md`.
- [x] **N20.5** (hoàn thành 13/9): màn **Khám phá** thật — Paging 3 cursor; nhận `sectionType/title/topic` từ Home để preselect filter; chip Tất cả/Chơi nhiều nhất/Xu hướng; sort + topic trong menu; top bar cố định “Khám phá”; public-only `/search` và `/feed` theo query. ✅ **XONG 13/9**

**📝 N20.5 Implementation Details (13/9):**

- ✅ **Contract Android-only, không sửa backend**: `trending` và category mặc định dùng `/quizzes/feed` (`topic` nullable); Tất cả/Mới nhất/Cũ nhất/Chơi nhiều/Tên A-Z/Z-A dùng `/quizzes/search`. Category từ Home lấy từ `QuizCard.quizCategory`, không suy diễn từ title; category section không có một topic nhất quán thì ẩn “Xem thêm”.
- ✅ **Public-only boundary**: tạo `@PublicApiOkHttpClient` từ client chuẩn nhưng thay bằng `CookieJar.NO_COOKIES` + `Authenticator.NONE`, rồi `@PublicApiRetrofit`/`@PublicQuizApiService`. Lý do: `/search` là optional-auth, gửi cookie sẽ lẫn private/empty quiz của chính owner vào màn công khai. Vẫn tái dùng Json, converter, ResultCallAdapter, BASE_URL và interceptor từ client chuẩn.
- ✅ **Paging + MVI**: `DiscoverQuery` (`filter`, `sort`) → `ObserveDiscoverQuizzesUseCase` → `DiscoverPagingSource`; page-size 12, prefetch 3, max request 24. `DiscoverUiState`/`Intent`/`Effect` tách file; `DiscoverScreen` stateful + `DiscoverScreenContent` stateless; đổi query tạo Pager generation mới và reset cursor.
- ✅ **UX filter**: thanh ngoài chỉ giữ Tất cả/Chơi nhiều nhất/Xu hướng; menu chứa Mới nhất/Cũ nhất/Tên A-Z/Tên Z-A và các topic đã Việt hóa (General vẫn gửi wire value `General` nhưng hiển thị “Tổng hợp”). Đi từ Home category sẽ tự chọn đúng topic; top bar luôn “Khám phá”.
- 🐛 **Bug thật sau test — luôn dừng đúng 3 quiz**: payload quiz dùng snake_case nhưng `meta.pagination` của backend dùng camelCase `nextCursor`/`hasMore`; Json SnakeCase bỏ qua hai key này nên chúng rơi về null/false, Paging tưởng hết trang. Fix bằng `@JsonNames` trên `PaginationMetaDto` + regression test dùng production Json; đồng thời trả Discover về initial/page-size 12.
- ⚠️ **Dữ liệu**: gần 200 quiz của frontend là mock dataset, không phải production rows. Android chỉ render dữ liệu thật backend trả về. Backend không có taxonomy endpoint; sáu topic mặc định là taxonomy trình bày theo frontend, topic lạ từ Home vẫn được append động.
- ✅ CI/workflow xanh sau commit trên `main`.
- 📚 Socket.IO Android nâng cao (`IO.Options.auth`, namespace, ACK `emit` + `Ack {}`, `EVENT_CONNECT`/`EVENT_DISCONNECT`), `callbackFlow` + `awaitClose`; làm quen MVI (Intent sealed → 1 StateFlow).

- [ ] **N20.6 — BLOCKED, ngoài critical path**: quay lại phòng đang chơi; cần backend thêm endpoint kiểu `GET /games/active` trả `session_code` + socket token của phiên chưa kết thúc. Không giữ N22–N25 lại để chờ mốc này. Section `continue` hiện tại KHÔNG làm được việc này (xem mục 7 của `knowledgement/n19_6_knowledgement.md`).

### Tuần 5 (N21–25) — Gameplay Player: host-paced (classic)

- [x] **N21 — hoàn tất 13/9, đổi phạm vi có chủ đích**: làm **host console** và hấp thụ phần chính của N31–N33 thay vì màn chơi player. `HostGameScreen` + `HostGameViewModel` + 16 DTO host + 12 test mapper.

**📝 N21 Implementation Details (11–13/9):**

- ⚠️ **Đổi phạm vi so với kế hoạch gốc**: N21 làm **host console** (việc của N31–N33) thay vì màn chơi player. Lý do: N20 kết thúc bằng hand-off sang `HostGamePlaceholder`, để placeholder nằm đó suốt Tuần 5–6 thì host không chơi được ván nào. Hệ quả: N31–N33 coi như đã trả (trừ `host:player-progress` của self-paced), N22–N23 nhận phần màn chơi player, N24 vẫn là màn kết quả cuối trận thật.
- ✅ **Audit backend + frontend web trước khi code** (`game.socket.ts`, `socket.channels.ts`, `socket.doc.ts`, `classic.mode.ts`, `scoring.ts`, `game.store.js`, `HostGameConsole.vue`, `QuestionStage.vue`) — 6 bẫy: (1) `game:next` trả 409 `GAME_ADVANCE_NOT_ALLOWED` khi `autoAdvance===true` nên classic **không được hiện nút chuyển câu**; (2) `game:next` ở `question_active` là chốt sớm, ở `showing_results` là sang câu, ở `countdown` là 409; (3) reconnect giữa câu **mất đáp án** vì `game:state` không kèm `correct_answer` ⇒ phải cache theo index và nói rõ với host; (4) pause chỉ nằm trong RAM server; (5) `stats` chỉ có phân bố khi `showCorrectAnswer=true`, còn lại chỉ có `total`; (6) `offset = serverTime − localNow` tính lại mọi message. Thêm: `game:next/pause/resume/end` đều **không có ack** — chỉ `lobby:config-update` và `question:answer` mới có, nên mọi lệnh điều khiển được xác nhận bằng broadcast kế tiếp chứ không phải bằng ack.
- ✅ **`core:common`**: `HostGameModels.kt` mới — `GamePhase` (có `UNKNOWN` để backend thêm phase không làm sập màn), `PublicAnswerOption`, `PublicQuestion`, `HostQuestion`, `GameCountdown`, `QuestionLockReason`, `AnswerStats`, `QuestionResults`, `HostAnswerReceived`, `HostLeaderboard(Row)`, `LeaderboardRow`, `QuestionStat`, `GameSnapshot`, `GameEnded`, `EliminatedPlayer`. Hai quy ước: mọi mốc thời gian **giữ nguyên chuỗi ISO của server** (client tự bù lệch, không tin đồng hồ máy), và id lựa chọn + đáp án đúng đều quy về `String` (backend khai id là kiểu tự do "can be 0", `correct_answer` có thể là id / mảng id / chuỗi tự luận) ⇒ so khớp chỉ có một luật, giống web (`trim` + `lowercase`). `GameEvent` thêm 9 nhánh.
- ✅ **`core:network`**: `HostSocketDtos.kt` (16 DTO) + `GameSocketEvents`/`GameEventMapper` viết lại cho 19 sự kiện server. Host **bỏ qua `question:started`** (host cũng nằm trong room chung, bản đó đã bị cắt đáp án nên sẽ ghi đè bản `host:question` có đáp án).
- 🔴 **Bug thật khi test: mọi lựa chọn hiện "(ảnh)"** — nguyên nhân là đọc field theo `socket.doc.ts` (`text`/`image`) trong khi dữ liệu thật là `option_text`. Đã thay `AnswerOptionDto` bằng parser `JsonElement` thủ công: object → `id` (fallback vị trí khi thiếu/null) + text đọc `option_text` rồi mới `text`; chuỗi thuần → dùng **vị trí làm id** (khớp `options.map((option, index) => ({ id: index, ... }))` của `quiz.repository.ts`). Nhánh chuỗi thuần là từ `game.doc.ts` ("either [{ id, option_text }] rows or plain strings") — với DTO cũ nó sẽ làm rơi cả sự kiện `host:question`.
- 🔴 **Bug thật khi test: câu tự luận bấm "Xem đáp án" không hiện gì** — đáp án được vẽ bằng cách in đậm lựa chọn đúng, mà câu tự luận không có lựa chọn nào. Đã thêm nhánh in đáp án ra chữ (nhiều đáp án thì liệt kê hết để host chấm tay), và nói rõ "không kèm đáp án mẫu" khi rỗng thay vì im lặng.
- ✅ **`feature:game-host`**: `HostGameUiState`/`Intent`/`Effect`/`ViewModel`/`Screen` đúng baseline. State gồm `hasAnswerKey` + `isAnswerRevealed` tách đôi ("có đáp án hay không" khác "host đang muốn xem hay không"), đáp án **tự ẩn lại** mọi khi sang câu; `COMMAND_GUARD_MS = 800` chống bấm dồn; 4 code fatal (`GAME_TOKEN_INVALID`, `GAME_TOKEN_WRONG_ROOM`, `GAME_ROOM_NOT_FOUND`, `GAME_PLAYER_NOT_FOUND`) ⇒ thoát màn kèm lý do; một `LazyColumn` duy nhất theo bài học N19.6.
- ✅ **Navigation**: `HostGamePlaceholder` **đã xóa hẳn** khỏi `GameNavGraph.kt`, route `Route.HostGame` trỏ thật sang `HostGameScreen`; `:app` đã có sẵn `:feature:game-host` từ M1 nên không phải sửa Gradle.
- ✅ **Test**: `HostGameEventMapperTest` (12 case) ở `core:network`.
- ⚠️ **Nợ để lại**: **ảnh câu hỏi chưa render** (nợ từ M2, `question_image` đã có sẵn trong `PublicQuestion` — đợt polish phải làm ở cả màn chơi/host/review/preview, cần kiểm tra Coil đã có trong version catalog chưa); chưa có test cho parser lựa chọn mới (3 case nên thêm: `{id, option_text}`, chuỗi thuần, `id = 0`); màn kết thúc trận còn là bảng gọn chờ N24; self-paced (`host:player-progress`, `question:awaiting_next`) ngoài phạm vi; loạt typo tiếng Việt trong comment chờ dọn một lượt.
- 🧠 Bài học chi tiết: `knowledgement/n21_knowledgement.md`.
- [x] **N22 — hoàn thành 13/9** (nhận phần state machine của N21 cũ): `GameViewModel` + `GamePhaseUi`; countdown từ `game:countdown`; `question:started` → submitted/locked → `question:results`; input dispatcher đủ 4 loại câu. ✅
- [x] **N23 — hoàn thành 13/9** (nhận phần timer/input của N21 cũ): timer sync theo offset `serverTime`; khóa input trước khi gửi; ACK timeout/not-connected → giữ khóa + `player:sync`; reconnect dựng lại submitted từ `game:state.player.answered_questions`. ✅

**📝 N22–N23 Implementation Details (13/9):**

- ✅ **Contract/domain typed**: thêm `PlayerAnswer` (`SingleChoice`/`MultipleSelect`/`Text`), `AnswerAck`, `PlayerQuestionStarted`, `AnsweredQuestionSnapshot`, `PlayerStateSnapshot`; `PlayerGameSocketRepository.submitAnswer()` đổi từ JSON string + `Unit` sang typed answer + `Result<AnswerAck>`. JSON chỉ tồn tại ở `core:network`.
- ✅ **ACK theo implementation thật**: `PlayerGameSocketRepositoryImpl` dùng `emitWithAck`; host-paced parse acceptance-only (`accepted`, `isLate`, `lives`, `eliminated`, `serverTime`), vẫn tương thích field mở rộng self-paced; lỗi đọc đúng shape `{error:{code}}`. Không hiển thị đúng/sai/điểm từ ACK host-paced.
- ✅ **Event/snapshot**: `GameEventMapper` map `question:started`; `game:state` map thêm `matchEndsAt`, `allow_answer_late`, `player.answered_questions`. Host vẫn bỏ qua `QuestionStarted` có chủ đích để bản public không ghi đè `host:question`.
- ✅ **State machine + UI**: `feature:game-player` có `GameUiState`/`GameIntent`/`GameEffect`/`GamePhaseUi`, `PlayerGameSessionUseCase`, `GameViewModel`, `GamePlayScreen` stateful + `GamePlayScreenContent` stateless. Hỗ trợ `multiple_choice`, `multiple_select`, `short_answer`, `long_answer`; không gửi lựa chọn rỗng/text blank; giữ option order; id `"0"` hợp lệ.
- ✅ **Timer/reconnect**: tính clock offset lại từ `serverTime`; dùng deadline tuyệt đối, `endsAt=null` thì ẩn timer; UI về 0 chỉ khóa input, server quyết định phase. Sau mọi `Connected`: `lobby:join` + `player:sync`.
- ✅ **ACK uncertainty**: input khóa trước request. Timeout/not-connected/duplicate/locked/too-late không tự retry; giữ trạng thái đang xác nhận rồi sync. Snapshot xác nhận chưa ghi và phase còn active mới mở lại.
- ✅ **Navigation**: `PlayerLobbyEffect.NavigateToGame`; truyền `gameId/playerId/socketToken`; pop PlayerLobby khỏi back stack. Sửa thứ tự phát effect trước khi hủy collector để tránh `CancellationException` làm mất navigation.
- ✅ **Test contract/mapper đã thêm**: ACK host-paced/self-paced/error/timeout; `question:started`; player answered snapshot; option object/plain string/id `0`; validation input rỗng. Test toàn diện ViewModel/E2E được chủ dự án chủ động để sau.
- ⚠️ **Giới hạn đã biết**: reconnect ở `showing_results` không có đủ payload để replay đúng/sai/stats nên chỉ hiện “Đang chờ câu tiếp theo”; kết thúc trận chưa nav sang Final Result — cả hai phần kết quả đầy đủ thuộc N24. Ảnh câu hỏi vẫn là nợ polish; self-paced thuộc N26–N30.
- 🧠 Bài học chi tiết: `knowledgement/n22_n23_knowledgement.md`.
- [x] **N24**: `leaderboard:updated` + `answer:received`; kết quả giữa câu; pause/config gating; xử lý `game:ended` và điều hướng sang `Route.FinalResult`/màn kết quả cuối trận. ✅ **Hoàn thành 15/9/2026**

**📝 N24 Implementation Details (15/9/2026):**

- ✅ Mở rộng contract socket Player với `AnswerProgressUpdated` và `PlayerLeaderboardUpdated`; mapper hỗ trợ leaderboard lean giữa câu và full leaderboard cuối trận.
- ✅ `GameViewModel` nhận `question:results` theo thứ tự server-authoritative, chống snapshot `question_locked` đến trễ kéo UI lùi khỏi Results; normalise đáp án player để so sánh an toàn nhưng không tự tính điểm.
- ✅ `showCorrectAnswer=false` được phòng thủ ở client: xóa answer key + distribution và dùng outcome trung tính, kể cả payload stale vô tình chứa dữ liệu reveal.
- ✅ `showLeaderboard=between_questions` chỉ render bảng đầy đủ trong phase Results; `end_only`/`never` xóa rank, score và leaderboard live. Final leaderboard vẫn lấy từ `game:ended` theo đúng config.
- ✅ Pause/resume Player đọc `sessionStatus` từ `game:state`: pause khóa cả UI control lẫn Intent/ViewModel; resume chỉ mở lại nếu phase còn Question và player chưa submit.
- ✅ `game:ended` được lưu vào `GameResultRepository` in-memory rồi phát `NavigateToFinalResult`; `Route.FinalResult(gameId, playerId)` pop Gameplay khỏi back stack. Process recreation không bịa kết quả, chỉ hiện fallback an toàn.
- ✅ `feature:leaderboard` có `FinalResultScreen`/`FinalResultViewModel`/contract thật; highlight Player hiện tại và có fallback khi leaderboard bị ẩn.
- ✅ Hotfix config sau test thật: `normalizeConfig` backend ép `reviewMode=true` bật lại `showCorrectAnswer`; `RoomConfigForm` giờ tắt reveal thì tắt luôn review, bật review thì bật reveal, còn Marathon giữ reveal bật và read-only.
- ✅ Bổ sung unit test mapper, result visibility, leaderboard gating, pause gating và quan hệ config; chủ dự án đã chạy test/build xanh và push commit `3271eda` lên `main`.
- ⚠️ Giới hạn còn lại chuyển sang N25: E2E ≥2 máy, mất mạng/disconnect/reconnect và ACK uncertainty trên thiết bị thật. Self-paced vẫn thuộc N26–N30.

Chi tiết bài học: `knowledgement/n24_knowledgement.md`.

- [ ] **N25 — TẠM GÁC, E2E ngày 26/9 cho kết quả FAIL**: đã chạy Classic với 1 Host + 2 Player Android, phủ 4 loại câu, reveal/leaderboard, pause/resume, kết thúc và reconnect. Android đã bổ sung regression test cho Host/Player; giữ trạng thái Submitted qua pause/resume; chặn stale snapshot kéo Results lùi; transport disconnect giữ snapshot và khóa input; `io server disconnect` thoát rõ ràng; reconnect chờ mạng `VALIDATED`, tự thử lại, chuyển `RECONNECT_FAILED` sau khi cạn lượt và hiện nút **Kết nối lại**. Log thật xác định lỗi ban đầu là DNS `UnknownHostException/EAI_NODATA`, không phải token hay reducer.
  - **Blocker backend đã báo team backend**: `onLeave(socket cũ)` chạy bất đồng bộ có thể ghi `player.status=disconnected` sau khi socket mới đã `lobby:join`; socket mới vẫn nhận đúng `game:state` nhưng `pendingAnswers()` chỉ đếm status `connected`, khiến Host báo thiếu người và auto-advance sớm. Fix đúng nằm ở backend: presence theo socket identity/generation, chỉ đánh dấu offline và gọi `maybeAdvanceAfterLeave()` khi không còn socket thay thế; reconnect trong trận active cần refresh roster Host. Android giữ contract `Connected → lobby:join → player:sync`, không thêm delay/double-join che lỗi.
  - **Còn phải retest trước khi chốt M4**: backend presence fix lặp 5–10 lần, ACK uncertainty hai nhánh server đã/chưa ghi, fatal/server disconnect, input/double-score sau reconnect và regression/build của đúng commit. Checklist bằng chứng: `N25_E2E_CLASSIC_CHECKLIST.md`.
- [x] **N26 — HOÀN THÀNH 26/9**: nền gameplay self-paced/Solo đã chạy happy case với 1–2 Player; commit `091ab72` trên `main`.
- [x] **N27 — HOÀN THÀNH 27/9**: manual progression cho Solo/self-paced khi `autoAdvance=false`, gồm `question:next`, `question:awaiting_next`, chống double tap và timeout phục hồi thao tác; commit `6a5f02f` trên `main`.
- [x] **N28 — HOÀN THÀNH 27/9**: Player Survival/Marathon với lives, elimination, timer tổng, timeout câu và trạng thái hoàn thành cá nhân; commit `e68a3af` trên `main`.
- [x] **N28.5 — ANDROID HOÀN THÀNH 27/9, CHỜ BACKEND**: Host Console self-paced cho Solo/Survival/Marathon đã theo dõi tiến độ, điểm và status từng Player; không giả định cả phòng cùng một câu. Lives/streak realtime, timeout progress và presence refresh chưa thể hoàn tất E2E vì payload backend còn thiếu.
- [x] **N29 — ANDROID HOÀN THÀNH 3/10, HAPPY CASE PASS; CHỜ BACKEND PAUSE/RESUME**: Practice + post-game REST review đã hoàn thành ở commit `499297c`; chưa chốt gate pause/resume vì snapshot self-paced phía backend sai progress.
- [ ] **N30 — TIẾP THEO**: `player:sync` khi resume và làm mới socket token; không đánh dấu M4 hoàn thành cho tới khi quay lại chốt các gate N25.
- 📚 MVI thực chiến (sealed phases, one-shot events), timer offset. **Bắt đầu unit test ViewModel bằng Turbine từ tuần này** — không dồn Tuần 9.

### Tuần 6 (N26–30) — Gameplay Player: self-paced (solo/survival/marathon/practice)

- [x] **N26 — hoàn thành 26/9/2026**: `question:started` cá nhân; đọc kết quả ngay từ ACK (`isCorrect`, `scoreEarned`, `totalScore`, `streak`, `correct_answer` khi được reveal). ✅

**📝 N26 Implementation Details (26/9/2026):**

- ✅ **Audit backend thật trước khi code**: đọc `game.socket.ts` (`sendSelfQuestion`, `onAnswer`, `snapshot`), `config.rule.ts` và bốn mode self-paced. Contract chốt: self-paced vẫn dùng `question:started` nhưng emit riêng từng Player; server tự lấy `player.current_question_index`; ACK luôn có acceptance/timing/lives, còn đúng-sai/điểm/streak/đáp án chỉ xuất hiện khi `pacing=self && showCorrectAnswer=true`. `autoAdvance=true` để server tự gửi câu mới; `autoAdvance=false` bắt buộc từng Player phát `question:next`, Host không thể next thay.
- ✅ **Reducer/UI Android**: `GameUiState` giữ `pacing`, `allowAnswerLate`, `matchEndsAt`; ACK self-paced đưa phase thẳng sang Results và hiển thị outcome, đáp án đúng, điểm câu, tổng điểm, streak, trạng thái late và lives. `showCorrectAnswer=false` được enforce tại reducer: xóa grading fields kể cả payload bất thường chứa dữ liệu reveal. Classic giữ nguyên acceptance-only.
- ✅ **Timer**: deadline mềm (`allowAnswerLate=true`) không khóa input khi về 0; test soft deadline với `autoAdvance=false` dừng ở kết quả là đúng vì nút Next thuộc N27.
- ✅ **Test máy thật**: Solo `autoAdvance=true` chạy đúng với 1–2 Player; câu mới tự đến sau result window, feedback ACK đúng và các luồng còn lại ổn. `autoAdvance=false` cố ý dừng ở kết quả cho tới khi N27 thêm `question:next`.
- ✅ **Regression tests**: mở rộng mapper `question:started`, full self-paced ACK, feedback tức thời, visibility khi ẩn đáp án và soft deadline. Commit Android: `091ab72` (`feat(game-player): support self-paced answer feedback`).
- ⚠️ **Backend note — không workaround ở Android**: `snapshot()` self-paced đang lấy `questions[index]` thay vì cùng `orderedQuestionsFor(..., sessionId + playerId)` như `sendSelfQuestion()`/`onAnswer()`; reconnect có thể trả sai câu khi shuffle, và Marathon thiếu modulo nên `index >= total` có thể trả `question=null`. Ngoài ra trạng thái đã trả lời/chờ Next có thể nhận `question:awaiting_next` rồi bị `player:sync` trả snapshot của index kế tiếp ghi đè. Hai điểm này không đổi happy case N26 nhưng phải gửi team backend và chốt trước N27 reconnect/N30 resume.

- [x] **N27 — hoàn thành 27/9/2026**: `question:next` (`autoAdvance=false`) + `question:awaiting_next`; happy case đã xác nhận. ✅

**📝 N27 Implementation Details (27/9/2026):**

- ✅ **Contract + mapper**: thêm typed model/DTO cho `question:awaiting_next`, đọc `previous_result`, `player_score`, `lives`, `serverTime`; mapper có regression test payload thật.
- ✅ **Manual progression**: chỉ với `pacing=self && autoAdvance=false`, màn Results hiện nút **Câu tiếp theo** nếu còn câu; bấm nút phát `question:next`. `autoAdvance=true` giữ nguyên hành vi server tự chuyển.
- ✅ **Chống thao tác lặp**: khóa nút ngay lần bấm đầu; `question:started` xác nhận chuyển câu và reset state. Nếu sau 5 giây chưa có câu mới, Android mở lại nút và báo thử lại; lỗi socket cho `question:next` cũng mở lại thao tác.
- ✅ **Reconnect state**: `question:awaiting_next` khôi phục màn kết quả và quyền bấm Next; reducer vẫn enforce `showCorrectAnswer=false`, không tin payload bất thường để lộ grading fields.
- ✅ **Tests**: bổ sung unit test cho mapper, manual-next chỉ emit một lần, awaiting-next restore, visibility guard và timeout. Commit Android: `6a5f02f` (`feat(game-player): support manual self-paced progression`).
- ⚠️ **Backend blockers giữ nguyên**: chưa coi reconnect sau khi trả lời là gate pass cho N27 cho tới khi backend thống nhất `snapshot()` với `orderedQuestionsFor()`/Marathon modulo và tránh `player:sync` snapshot ghi đè `question:awaiting_next`. Android không thêm workaround.

- [x] **N28 — hoàn thành 27/9/2026**: Player Survival (lives + elimination) và Marathon (`matchEndsAt`, `question:timeout`, `player:finished`). ✅

**📝 N28 Implementation Details (27/9/2026):**

- ✅ Map typed `question:timeout` và `player:finished`; reducer xử lý ACK/timeout/elimination theo server-authoritative state.
- ✅ Survival hiển thị lives xuyên suốt, có phase/màn bị loại riêng và vẫn giữ socket để chờ `game:ended`.
- ✅ Marathon hiển thị đồng hồ tổng từ `matchEndsAt`, khóa input khi hết ngân sách nhưng chờ server chốt; timeout câu có feedback riêng và không lộ đáp án khi `showCorrectAnswer=false`.
- ✅ Player hoàn thành sớm chuyển sang trạng thái chờ, giữ leaderboard cá nhân và chỉ điều hướng Final Result khi có `game:ended`.
- ✅ Regression tests cho mapper, ACK elimination, timeout/reveal guard, `player:finished` và match deadline. Commit Android: `e68a3af` (`feat(game-player): support survival and marathon gameplay`).
- ℹ️ **Marathon late join là contract hiện tại**: `matchEndsAt` tạo riêng khi từng Player nhận câu đầu, nên người vào muộn vẫn có đủ `totalMatchSeconds`; đây là budget theo Player, không phải deadline chung của phòng.
- ⚠️ **Backend notes mới**: callback timeout đang coi `current_question_index >= questions.length` là hết game kể cả Marathon nên có thể kết thúc sớm ở cuối bank thay vì modulo; nhánh timeout làm lives về 0 không broadcast `player:eliminated`. Các lỗi snapshot/order/modulo và `player:sync` ghi đè awaiting-next từ N26–N27 vẫn giữ nguyên; Android không workaround.

- [x] **N28.5 — Android hoàn thành 27/9/2026, chờ backend để chốt E2E**: Host Console self-paced dùng `game:state`, `host:player-progress`, `player:finished`, `player:eliminated`, `leaderboard:host`; bỏ giả định câu hỏi/phase chung và không hiển thị `game:next` cho self-paced.

**🔎 N28.5 Backend Audit (27/9/2026):**

- ✅ Host join/reconnect nhận `game:state` rồi `leaderboard:host`; bảng host có đủ progress, score, streak, lives và status để dựng baseline. Pause/resume/end dùng chung được; server freeze/resume clock riêng từng Player.
- ⚠️ `game:state` của Host self-paced gọi `snapshot(session)` không có Player nên vẫn trả `index/current question` cấp session (thường câu 0). Đây không phải câu chung thật; Android phải bỏ qua `snapshot.question/index/endsAt` khi `pacing=self`.
- ⚠️ `host:player-progress` mới có id/name/current index/score/correct count/status, **không có lives/streak** và hiện Android chưa map event này. Sau câu trả lời thường, Host có thể merge tiến độ nhưng không cập nhật chính xác lives trung gian.
- ⚠️ Auto-timeout cập nhật Player rồi chỉ emit `question:timeout` cho Player; không emit `host:player-progress`, và khi lives về 0 cũng không broadcast `player:eliminated`. Dashboard Host sẽ stale sau timeout cho tới `player:finished`/`game:ended`.
- ⚠️ Player late-join/reconnect trong session active không làm server emit lại lobby/`leaderboard:host`; Host chỉ thấy người đó sau event tiến độ đầu tiên. `matchEndsAt` cũng là clock riêng và không được gửi cho Host, nên Host không thể hiển thị countdown chính xác từng Player.
- **Kết luận gate:** Android có thể làm đúng layout/routing và merge các event hiện có, nhưng N28.5 chỉ pass đầy đủ sau backend bổ sung một nguồn snapshot/update self-paced nhất quán (ưu tiên mở rộng `host:player-progress` + emit cho answer/timeout/join/disconnect, hoặc phát `leaderboard:host` sau mọi thay đổi liên quan). Không polling và không tự suy lives/rank ở Android.

**📝 N28.5 Implementation Details (27/9/2026):**

- ✅ **Contract/mapping**: thêm `HostPlayerProgress`, `GameEvent.HostPlayerProgressUpdated`, DTO + mapper cho `host:player-progress`; `player:finished` giữ thêm `player_name` để Host có thể dựng/cập nhật row terminal.
- ✅ **Reducer Host self-paced**: lấy `pacing` từ lobby/start/snapshot; khi `pacing=self` bỏ `snapshot.question/index/endsAt` cấp session vì không đại diện cho từng Player. Merge progress gia tăng vào baseline `leaderboard:host`, giữ lại lives/streak mà payload delta không gửi, cập nhật score/answered/correct/wrong/status và sắp xếp lại theo điểm.
- ✅ **UI Host**: Solo/Survival/Marathon dùng dashboard tiến độ riêng; hiện số Player đang chơi/mất kết nối/hoàn thành/bị loại và row điểm, tiến độ, đúng/sai, lives, streak, status. Self-paced không hiện câu hỏi/timer chung, không hiện `game:next` hoặc đáp án; vẫn giữ Pause/Resume/End. Marathon nói rõ mỗi Player có clock riêng.
- ✅ **Terminal events**: `player:finished` cập nhật/chen row hoàn thành; `player:eliminated` chuyển status và lives về 0 khi backend có broadcast.
- ✅ **Regression**: mapper test cho progress/finished; ViewModel test snapshot self-paced, merge delta giữ lives/streak và terminal events; Player reducer bỏ qua event chỉ dành cho Host. Commit Android: `2be419b` (`feat(game-host): support self-paced host dashboard`).
- ✅ **Test máy thật phần contract hiện có**: layout/routing và progress/score cập nhật theo answer bình thường; điều khiển Host dùng được. Test đồng thời xác nhận lives và timeout bị stale do backend không phát đủ dữ liệu; Classic giữ nguyên nhánh xử lý cũ.
- ⚠️ **E2E backend gate đã tái hiện**: lives không đổi realtime vì `host:player-progress` thiếu lives/streak; auto-timeout không phát progress cho Host và không phát elimination khi lives về 0; late-join/reconnect/disconnect active không refresh ngay dashboard Host. Đây là dữ liệu server-authoritative nên Android không suy đoán hoặc polling để che lỗi.
- **Trạng thái:** phần Android hoàn tất theo contract hiện có. Khi backend đổi payload, chỉ mở rộng DTO/merge nếu cần rồi retest Survival/Marathon; roadmap Android chuyển sang N29–N30.

- [x] **N29 — hoàn thành phía Android 3/10/2026**: Practice (điểm = 0, reveal theo config) + post-game review qua REST `GET /games/{id}/review`.

**📝 N29 Implementation Details (3/10/2026):**

- ✅ **Audit contract trước khi code**: Practice là self-paced, hỗ trợ nhiều Player nhưng mỗi Player có tiến độ riêng; mặc định `basePoints=0`, không speed bonus, không giới hạn thời gian/countdown, auto-advance, luôn reveal và không hiện leaderboard. Streak vẫn theo chuỗi trả lời đúng dù score luôn bằng 0.
- ✅ **Review đúng là REST, không phải socket event**: `GET /games/{id}/review`, gửi `x-socket-token`; chỉ gọi sau khi session kết thúc và `reviewMode=true`. Response gồm thống kê Player và toàn bộ câu hỏi với đáp án đã chọn, đáp án đúng, explanation, thời gian và điểm từng câu; Marathon có thể có thêm item theo vòng lặp.
- ✅ **Network/security**: thêm model/DTO/use case typed; parser chịu được mixed naming của payload thật. Review dùng Retrofit preserve-case riêng không gắn cookie/`TokenAuthenticator`, tránh `401 GAME_TOKEN_INVALID` kích hoạt refresh cookie hoặc logout. Redact `x-socket-token`, `Cookie`, `Set-Cookie`, `Authorization` khỏi HTTP log.
- ✅ **Handoff/UI**: `StoredGameResult` giữ `mode` + `socketToken` trong memory, không đưa token vào route/Bundle/DataStore. `FinalResultViewModel` lazy-load review, có loading/error/retry; UI ưu tiên câu bỏ qua/sai, hiển thị ảnh, lựa chọn của Player, đáp án đúng, explanation, thời gian và điểm.
- ✅ **Kiểm thử**: bổ sung `GameReviewDtoTest`, `FinalResultViewModelTest`, cập nhật regression `GameViewModelTest`; `git diff --check` sạch. User đã xác nhận happy case Practice + review hoạt động; commit Android `499297c` (`feat(game-player): support practice review`).
- ⚠️ **Backend blocker — pause/resume self-paced**: Host pause broadcast shared `game:state` được dựng từ `session.current_question_index` (thường 0) thay vì `player.current_question_index`; UI nhảy về câu 1 nhưng server vẫn giữ tiến độ thật, nên trả lời xong có thể nhảy từ câu 1 hiển thị sang câu 4. Snapshot này còn có nguy cơ dùng raw question order thay vì order đã shuffle theo Player. Android không workaround vì server mới là nguồn sự thật.
- 🔁 **Checklist retest để chốt N29 sau backend fix**: vào Practice nhiều Player; đi tới câu 3+; Host pause/resume; từng Player phải giữ đúng câu và option order; answer sau resume phải được chấm đúng question id/index; auto-advance/manual-next đúng câu kế; reconnect + `player:sync` không lùi tiến độ; kết thúc game tải review đủ item/đáp án/explanation; token sai/hết hạn chỉ báo lỗi review, không logout tài khoản.

- ⛔ **N30 — tạm block sau audit 3/10**: chưa có endpoint cấp lại Player socket token; `player:sync` dùng question bank gốc thay vì shuffled order riêng và chưa modulo Marathon index. Đã ghi `BUG-07`, `BUG-15`, `FEATURE-07` trong `BACKEND_BUG_REPORT.md`; không workaround phía Android.

### Tuần 7 (N31–35) — Phạm vi cũ Host Console + Leaderboard

> N31–N33 được giữ làm mã tham chiếu lịch sử, nhưng không còn là ba mốc triển khai độc lập sau khi N21 hấp thụ Host Console. Không làm lại phần đã hoàn thành.

- [x] **N31 — CLOSED, absorbed by N21** (13/9): `HostGameScreen` + `HostGameViewModel`; `game:state` làm nguồn chính.
- [x] **N32 — CLOSED, absorbed by N21/N28.5**: `host:question`, `host:answer-received`, `host:player-progress` và dashboard self-paced đã có; không mở lại toàn bộ N32.
- [x] **N33 — CLOSED, absorbed by N21/N27/N28.5**: pause/resume/end, manual-next đúng pacing và rule ẩn next khi `autoAdvance=true` đã xong.
- [x] **N34 — Android hoàn thành, absorbed by N21/N28.5**: `leaderboard:host` full table + merge progress/terminal events; E2E lives/timeout/presence chờ backend.
- [x] **N35 — hoàn thành 3/10/2026**: Final Result ưu tiên socket, REST fallback `/v1/games/:id/results` sau process death, loading/error/retry, thống kê từng câu và visibility defense → **phần Android của M5 hoàn tất**.

**📝 N35 Implementation Details (3/10/2026):**

- ✅ **Socket-first, REST recovery-only**: còn `StoredGameResult` thì render ngay và tuyệt đối không gọi `/results`; chỉ khi process recreation làm mất transient result mới chạy `LoadGameResultsUseCase`.
- ✅ **Contract typed**: thêm `GameResults`, DTO envelope `data.results`, mapper `session`/`leaderboard`/`perQuestion`, `GameApiService.getGameResults()` và `GameSessionRepository.getGameResults()` qua public preserve-case Retrofit.
- ✅ **State/UI**: `FinalResultViewModel` có loading/error/retry độc lập với Review; chặn request đồng thời; `FinalResultScreen` phục hồi kết quả và render section **Thống kê từng câu** đã sort theo `questionIndex`, tỷ lệ đúng an toàn khi `answerCount=0`.
- ✅ **Không regression N29**: Review vẫn lazy-load bằng socket token transient; sau process death vẫn xem được tổng kết, còn mở Review sẽ báo credential không còn thay vì logout hoặc tự gọi sai endpoint.
- ✅ **Visibility defense**: `/results` hiện public và trả full leaderboard kể cả `showLeaderboard=never`; Android đọc `session.config.flow.showLeaderboard` rồi loại bảng trước khi state tới UI. Đã ghi thành `BUG-16` trong `BACKEND_BUG_REPORT.md` vì dữ liệu vẫn bị lộ ở tầng API.
- ✅ **Kiểm thử/commit**: thêm `GameResultsDtoTest`, mở rộng `FinalResultViewModelTest` cho no-call happy path, REST success/error/retry và hidden leaderboard; `git diff --check` sạch. Chủ dự án xác nhận ổn và commit `37bc61a` (`feat: add REST fallback api to show data after come back to death process`).
- ⚠️ **Ngoài phạm vi**: Final Result riêng cho Host, share result, lịch sử/Hoạt động, resume active game và refresh Player socket token.

**N36 đã hoàn thành 3/10/2026** — audit trước rồi chỉ harden khoảng trống Android, không làm lại finite retry N25 và không workaround Player token renewal N30.

### Tuần 8 (N36–40) — Hardening & feature completion I

- [x] **N36 — hoàn thành 3/10/2026**: Reconnect hoàn chỉnh; commit `f232a75`; 3 luồng E2E chính pass.

**📝 N36 Implementation Details (3/10/2026):**

- ✅ **Audit-first, không viết lại socket layer**: N18/N25 đã có finite retry, phân biệt transport với `io server disconnect`, `lobby:join` sau mọi `Connected`, Player Gameplay `player:sync`, cleanup `callbackFlow/awaitClose` và gating input/control. N36 chỉ vá phần còn thiếu.
- ✅ **Không còn spinner vô hạn**: thêm `RECONNECT_FAILED` cho Host Lobby, Player Lobby và Host Game. `RECONNECTING` chỉ báo Socket.IO đang tự thử; khi nhận `CLIENT_RECONNECT_EXHAUSTED` mới hiện nút **Kết nối lại**, đồng thời giữ snapshot cũ.
- ✅ **Tự phục hồi khi mạng trở lại**: dù Socket.IO đã cạn retry, `ConnectivityManager` vẫn theo dõi mạng `VALIDATED`; khi mạng trở lại, client reset bộ đếm và tự `connect()`. Nút retry là fallback khi backend hồi phục nhưng trạng thái mạng không đổi hoặc callback hệ thống không tới.
- ✅ **Host token renewal trong gameplay**: Host Game gọi `GameSessionRepository.getHostToken(gameId)` khi gặp `GAME_TOKEN_INVALID`, lưu token mới vào `SavedStateHandle` rồi tạo connection flow mới. Host Lobby reset `tokenRefreshAttempted` sau mỗi lần kết nối thành công để xử lý được lần hết hạn sau mà vẫn chặn vòng lặp token hỏng.
- ✅ **UI/control an toàn**: Host không gửi next/pause/resume/end khi chưa `CONNECTED`; Player không gửi answer khi offline; retry hủy collector/socket cũ trước khi tạo flow mới, không nhân đôi listener.
- ✅ **Kiểm thử**: bổ sung unit test cho cold-start exhaustion, manual retry tạo flow mới, join lại phòng và Host token refresh; chủ dự án đã xác nhận E2E Host Lobby, Player Lobby và Host Game đều pass. Case hết hạn Host token chưa tái hiện thủ công, được giữ bằng unit test.
- ⚠️ **Không workaround backend**: Player token renewal, self-paced sync/order, old/new socket presence race và finished-session reconnect tiếp tục thuộc N30/BUG backend.

- [x] **N37 — hoàn thành 3/10**: Error handling + resource missing + offline banner.
  - `ResultCallAdapterFactory` đọc `error.code` trước HTTP status; chỉ fallback status khi envelope thiếu/hỏng, không đưa raw exception/HTTP text ra UI.
  - Phân loại tập trung cho authentication, terminal session, missing resource và transient failure; chỉ terminal session mới chuyển `SessionState` sang Guest, còn mạng/server giữ nguyên đăng nhập.
  - `QuizRepository` chỉ fallback cache với lỗi mạng/server tạm thời; `QUIZ_NOT_FOUND`/resource missing xóa cache stale nên quiz đã xóa không bị “hồi sinh”.
  - Banner offline cấp app dùng network `VALIDATED`, loại trừ Host/Player Lobby và Host/Player Game vì các màn realtime đã có UI reconnect riêng.
  - Quiz Detail/Edit điều hướng Home khi resource biến mất; nếu quiz bị xóa khi A đang ở Detail thì A vẫn có thể vào màn config, nhưng backend chặn ở lần nhấn Tạo phòng cuối. Hành vi test thật: snackbar lỗi tại config; Back về stale Detail sẽ được điều hướng Home. Chưa có push invalidation chủ động cho màn đang mở, chấp nhận trong scope N37.
  - Gác đăng nhập đọc `SessionRepository.state.value` đúng thời điểm người dùng bấm, tránh dùng Compose snapshot cũ ngay sau logout.
  - Manual test chức năng chính đã pass: mapping lỗi, giữ session khi offline/server lỗi, banner REST + loại trừ realtime, dialog Thư viện sau logout, và quiz bị xóa không còn dùng cache để mở/tạo phòng thành công.
- [x] **Pre-N38 — audit feature completeness hoàn thành 3/10**:
  - Android làm ngay: Hoạt động/Lịch sử, ảnh câu hỏi trong gameplay, Solo Preview và account settings.
  - Backend block: active-game resume, Player token renewal, `player:sync` order/index, presence race, Practice pause snapshot và realtime Host self-paced.
  - Phase 2: import xlsx, duplicate/reorder/autosave draft, crop/default cover, public creator profile và admin mobile.
- [x] **N38 — Hoạt động: history data layer + danh sách — hoàn thành 6/10, commit `ba0ff49`**:
  - Thêm DTO/domain/API/repository cho `GET /games/history` với cursor, `limit`, `include_total` và role `played|hosted`.
  - Người đăng nhập có hai tab **Đã chơi / Đã tổ chức**; guest chỉ có **Đã chơi** và gửi `x-guest-id` hiện có.
  - Paging cursor riêng theo từng role; không tạo tab “Tất cả”, không merge hai cursor.
  - Thay `ActivityScreen` placeholder bằng stateful/stateless screen có loading/error/empty/retry/list/load-more; row dùng `sessionId`, không dùng room code làm định danh.
  - Server history là nguồn sự thật. `GameHistoryDao/Entity` đang unused: không nối vào UI; chỉ xóa/refactor khi có quyết định riêng, không tạo hai nguồn dữ liệu.
  - Test DTO/wire shape, repository query/header, ViewModel paging/reset role và manual happy path user + guest.

**📝 N38 Implementation Details (6/10/2026):**

- ✅ **Audit trước khi code**: đối chiếu `game.route.ts`, controller/service/schema/type/repository và cursor codec thật. Endpoint optional-auth, cookie thắng guest header; query `role=played|hosted`, `cursor`, `limit` 1..50, `include_total`; response `data.sessions` snake_case nhưng `meta.pagination` camelCase.
- ✅ **Domain/data**: thêm `GameHistoryRole`/`GameHistoryItem`, mở rộng `GameSessionRepository`, DTO preserve-case với `@SerialName`, `GameApiService.getGameHistory()` và mapping trong `GameSessionRepositoryImpl`. History đi qua authenticated `gameApi` để giữ cookie; không dùng public client. `include_total=false` vì UI chưa cần count.
- ✅ **Danh tính guest**: thêm `GuestIdentityStore.getGuestIdOrNull()`; guest chưa từng join nhận empty state cục bộ, không tự sinh UUID. User không gửi `x-guest-id`; backend cookie là danh tính duy nhất. Không merge lịch sử guest cũ vào tài khoản.
- ✅ **UI + cursor**: thay placeholder bằng Stateful/Stateless `ActivityScreen`, ViewModel giữ page/cursor/loading/error độc lập cho Đã chơi và Đã tổ chức, chống request/item trùng theo `sessionId`, có initial/append retry và load-more. Server history là nguồn sự thật; `GameHistoryDao/Entity` tiếp tục unused.
- ✅ **Freshness khi quay lại tab**: bottom navigation dùng `saveState/restoreState` nên ViewModel và empty state cũ được giữ sau khi guest chơi xong. `LifecycleEventEffect(ON_RESUME)` gửi `Refresh` tải lại role hiện tại từ cursor null; ViewModel bỏ qua nếu initial load đang chạy. Manual flow guest chơi xong → quay lại Hoạt động đã pass.
- ✅ **UI dùng chung + test**: thêm `RemoteImage` ở `core:ui`; thêm DTO mixed-case test, MockWebServer query/header/pagination test, ViewModel test cho cursor độc lập, guest chưa có UUID và refresh khi quay lại. `git diff --check` sạch; commit Android `ba0ff49` (`feat: add server backed activity history for users and guests`).

- [x] **N39 — Chi tiết lịch sử + answer sheet — hoàn thành 6/10/2026, commit `ecce5cd`**:
  - ✅ Audit trước khi lập kế hoạch: `/games/{id}/summary` và `/games/{id}/my-answers` dùng optional auth; cookie thắng `x-guest-id`; chỉ host hoặc participant mở được summary; answers chỉ dành cho participant và trả `GAME_REVIEW_DISABLED` khi phòng tắt review.
  - ✅ Thêm `GameHistorySummary`/quiz/viewer domain, API/repository và hai use case. User không gửi guest header; guest chỉ dùng UUID đã tồn tại, không tự tạo khi đọc lịch sử.
  - ✅ Thêm `Route.GameHistoryDetail(sessionId)` từ card Hoạt động. Summary và answers có loading/error/retry độc lập; ViewModel chỉ gọi answers khi backend trả `playerId`, vì vậy host không tạo request sai.
  - ✅ UI hiển thị snapshot quiz, metadata trận, thành tích riêng, leaderboard, thống kê từng câu cho host và answer sheet cho player/guest; tái sử dụng `QuestionStatCard`/`ReviewItemCard` từ Final Result.
  - ✅ Defense-in-depth: player vẫn giữ `viewerResult` nhưng không nhận leaderboard vào presentation state khi `showLeaderboard=never`; host vẫn xem đủ.
  - ✅ Manual smoke test pass guest, user played, user hosted và hidden leaderboard. `GAME_REVIEW_DISABLED` có unit test; `cancelled` được backend/mapping chấp nhận nhưng chưa gặp trong manual test.
  - ⚠️ Audit sau test phát hiện `BUG-17`: thống kê backend group theo `question_id` nhưng gắn `min(question_index)`, nên self-paced + shuffle riêng theo Player có thể trả các câu khác nhau cùng nhãn “Câu N”. Không workaround bằng cách group index ở Android.
- [x] **N40 — Render ảnh câu hỏi trong gameplay — hoàn thành 6/10/2026, commit `9b3cdf0`**:
  - ✅ Audit xác nhận backend đã phát `question_image` trong `host:question`, `question:started` và `game:state`; DTO/domain/ViewModel đã giữ field, chỉ thiếu presentation.
  - ✅ Thêm `QuestionImage` dùng chung ở `core:ui`: URL null/rỗng không tạo khoảng trống, khung 16:9 ổn định, `ContentScale.Fit`, placeholder/error fallback qua `RemoteImage`.
  - ✅ Render ở Host Game host-paced, Player Game cho cả pacing và card câu hỏi Quiz Detail; self-paced Host không có câu hỏi chung nên không giả lập ảnh.
  - ✅ Bổ sung mapper test cho event realtime + snapshot; kiểm thử nhanh, unit test và `assembleDebug` đã pass. Không thêm ảnh lựa chọn vì backend không có contract.

### Tuần 9 (N41–45) — Feature completion II & feature gate

- [x] **N41 — Solo Preview / “Tự chơi thử”** — commit `648bf37`, user xác nhận kiểm thử nhanh pass:
  - [x] Entry từ Quiz Detail; nút disabled khi quiz rỗng. Không room/host/socket, không history server và không tăng play count.
  - [x] Hỗ trợ 4 loại câu hỏi; chọn một chấm ngay, chọn nhiều/text cần submit; skip và trả lời muộn vẫn được phép.
  - [x] Countdown 3 giây, timer từng câu, feedback tự chuyển sau 2 giây nếu đúng / 4 giây nếu không đúng; có nút Next, không triển khai quay lại câu trước.
  - [x] Tổng kết điểm ước tính, đúng/đã trả lời/accuracy/thời gian; xem lại, chơi lại, sửa owner-only và thoát.
  - [x] Module local độc lập với mode `solo`/`practice` multiplayer; thêm unit test scoring + ViewModel.

**📝 N41 Implementation Details:**
- Audit đã đối chiếu Android Quiz Detail/domain/navigation, backend quiz repository + `scoring.ts`, và web `QuizPreviewPage.vue` / `usePreviewGame.js` / `previewScoring.js` / `PreviewSummaryView.vue`.
- `GET /quizzes/id/:quizId` optional-auth: public đọc được; private chỉ owner. Response quiz detail có questions và `correct_answer`; Android tận dụng `Question.correctAnswer`, options, image, hint, explanation sẵn có. Không bổ sung endpoint game.
- Thêm `:feature:quiz-preview`, namespace `android.kma.myquizzapp.feature.quiz_preview`, chỉ phụ thuộc `core:common` + `core:ui`. Không reuse nguyên `GamePlayScreen` vì màn đó gắn socket/session và answer UI private.
- `PreviewContract.kt` chứa phase/state/intent/effect/result; `QuizPreviewViewModel` tải quiz qua `QuizRepository`, đọc owner từ session SSOT, chạy state machine local và đo thời gian monotonic bằng `System.nanoTime`. Restart reset lượt chơi từ quiz đã tải, không gọi game API.
- `PreviewScoring.kt`: lựa chọn dùng ID Long trong `JsonArray`; multiple-select phải khớp cả tập; short/long answer trim + so sánh không phân biệt hoa thường. Key thiếu/không dùng được cho trạng thái không thể chấm (`isCorrect=null`), không crash.
- Điểm ước tính theo Classic mặc định của backend: sai/skip/không thể chấm = 0; đúng đúng hạn = 1000 + speed bonus (tối đa 500); đúng muộn = 900, không speed/streak bonus. Không copy nhánh late scoring bị lệch của web và không coi điểm này là kết quả trận server.
- `QuizPreviewScreen` / `QuizPreviewScreenContent` tách stateful/stateless; dùng `QuestionImage`, hiển thị hint/late warning/feedback/explanation. Review ưu tiên sai → skip → không thể chấm → đúng.
- `Route.QuizPreview(quizId)` + `QuizPreviewNavGraph.kt` ghép tại app; callback từ Quiz Detail mở preview. Sửa quiz owner-only pop Preview khỏi back stack để lưu xong trở về Quiz Detail.
- `PreviewScoringTest` phủ single/exact-set multiple/text normalization/key thiếu/speed/late 900; `QuizPreviewViewModelTest` phủ tải/countdown/question/local grade và repository chỉ được gọi một lần. User xác nhận kiểm thử nhanh pass; agent không chạy Gradle qua MCP và không ghi nhận output build mới ở bước cập nhật docs.
- Các blocker backend N20.6/N25/N28.5/N29/N30 giữ nguyên. Tiếp theo audit N42 hồ sơ + avatar trước, không triển khai chỉ từ design doc.

- [x] **N42 — Hồ sơ + avatar** — commit `0da538f`, user xác nhận kiểm thử nhanh pass:
  - [x] Đã audit source Android/backend/web, báo cáo contract/gap/quyết định và triển khai sau khi user duyệt.
  - [x] Sửa fullname/phone/description bằng PATCH delta; email chỉ đọc; hỗ trợ xóa phone/description.
  - [x] Photo Picker + preview thuần, xử lý ảnh và upload avatar; lưu avatar riêng với Lưu hồ sơ, không interactive crop.
  - [x] Publish profile/avatar vào session SSOT có guard, draft/discard và snackbar loading/error/success thật.

**📝 N42 Implementation Details:**
- Audit đối chiếu Android Profile/session/network, backend user/storage routes → schema → controller → service → repository và web Profile. `PATCH /users/me` chỉ nhận fullname/phone/description; email không sửa. Fullname 2–100, phone 7–15 chữ số với dấu `+` tùy chọn, description tối đa 200; chuỗi rỗng xóa phone/description.
- `ProfileDraft` trim, validate field thay đổi và chỉ gửi PATCH delta. Giữ phone cũ khi sửa giới thiệu sẽ không gửi lại phone; tránh bug backend kiểm tra trùng phone chưa loại chính user, không có nghĩa bug server đã được sửa. Profile thành công trả `data.user` đầy đủ và publish trực tiếp vào SSOT, không GET dư.
- Avatar là thao tác riêng: Photo Picker → preview thuần (không interactive crop) → chuẩn bị JPEG → presign folder `avatars` → PUT raw không cookie/auth → PATCH avatar. `AvatarImagePreparer` trong app chạy IO, đọc EXIF/orientation, resize và giảm chất lượng/kích thước đến tối đa 2 MiB; không phụ thuộc compressor của quiz-manage.
- `UserRepository` / `UserProfilePatch` / `SessionUserToken` thuộc `core:common`; API/DTO/impl thuộc `core:network`; `ProfileDraft`, `SaveProfileUseCase`, `UpdateAvatarUseCase` thuộc `app/domain/profile`, UI và adapter ảnh thuộc `app/presentation/profile`. Không thêm module hay dependency feature → feature.
- Mutation Retrofit dùng PreserveCase để giữ request/response camelCase, client `retryOnConnectionFailure(false)` để tránh tự phát lại mutation. `UserDto` bảo toàn role/authProvider và field snake_case tường minh; session token chỉ là userId + generation, không phải credential.
- `SessionRepositoryImpl` dùng generation cho vòng đời đăng nhập và revision cho refresh/update/logout; response cũ không ghi đè mutation/logout hay phiên đăng nhập mới. Avatar merge vào user hiện tại để không làm mất field profile; guard cả picker result và các bước upload.
- PATCH avatar lỗi không chắc chắn được đối soát bằng GET `/users/me`; nếu GET cũng thất bại, giữ trạng thái cần xác minh với nút “Kiểm tra lại” chỉ GET. Không tự replay PATCH cùng URL: backend chưa idempotent, có thể xóa ảnh đang dùng. Server còn thiếu URL ownership/object validation và đang xóa ảnh cũ trước DB update; các rủi ro này chưa được Android sửa ở backend.
- Profile edit dùng modal, draft SavedStateHandle và xác nhận bỏ thay đổi; UI giữ loading/error/success, snackbar theo error mapping tập trung và khóa submit lặp. Avatar preview/lưu tách khỏi Lưu hồ sơ; Profile và avatar bottom nav cùng đọc session SSOT.
- Bổ sung 23 test case trong `ProfileUseCasesTest` (8), `ProfileViewModelTest` (6), `UserProfileApiTest` (3), `SessionRepositoryTest` (6), phủ delta/validation, wire shape, avatar reconciliation và session race. User xác nhận kiểm thử nhanh pass; agent không tự chạy Gradle qua MCP và không coi test case bổ sung là bằng chứng đã quan sát build log.
- N43 đã triển khai sau audit thật, commit `ab6621b`; xem Implementation Details bên dưới. N43 đã được triển khai sau audit source thật ở commit `ab6621b`; user xác nhận kiểm thử nhanh ổn. Tiếp theo N44 audit integration và đối chiếu bằng chứng build/test/backend trước khi lập checklist chi tiết; các blocker N20.6/N25/N28.5/N29/N30 vẫn giữ trong hồ sơ, chưa retest ở phiên cập nhật doc này.

- [x] **N43 — Bảo mật tài khoản (Android + kiểm thử nhanh user)**:
  - [x] Audit source thật, báo cáo contract/gap và triển khai sau user duyệt.
  - [x] Đổi mật khẩu, vô hiệu hóa có xác nhận; xử lý session/cookie/navigation theo contract.
  - [x] Use case boundaries, snackbar/error mapping, logging an toàn và regression test cases.
  - [x] Đối chiếu XML local PASS cho 25 test bổ sung N43; full build/lint/CI chưa có bằng chứng đầy đủ và vẫn là gate riêng ở N44.

**📝 N43 Implementation Details:**
- Commit Android `ab6621bccdd4c6f452cdadacf30ba2ecf1e837a2` — `feat: add account security and session hardening`. User kiểm thử nhanh ổn; 25 test bổ sung N43 đã đối chiếu XML local PASS. Không suy ra full assemble/lint/CI/E2E từ unit XML.
- Đã audit backend user routes → schema → controller → service/repository và auth middleware/session, cùng Android Profile/session/cookie/navigation trước khi lập kế hoạch. Backend audit baseline `7c103c87b4c78d817e8a7acf50fd0424edd16c79`; cần audit lại revision đang dùng khi retest N44, không coi đây là bằng chứng deployment hiện tại.
- `PATCH /v1/users/me/password` nhận camelCase `oldPassword/newPassword`, tối thiểu 8 ký tự, new khác old; không trim password. Đổi thành công xóa input, snackbar và giữ phiên hiện tại; backend không revoke mọi session/clear cookie cho thao tác này. Không nhầm với luồng quên mật khẩu.
- `DELETE /v1/users/me` gửi JSON body `{password}` qua Retrofit `@HTTP(hasBody=true)`; đây là vô hiệu hóa/soft delete, không phải xóa toàn bộ dữ liệu hoặc có cơ chế tự khôi phục. Cần password + dialog xác nhận; sau server-confirmed success dọn cookie/session có guard và reset MainGraph/Home, gồm saved back stacks các tab.
- Google-only hiện không hỗ trợ cả hai mutation vì không có password; UI giải thích thay vì dựng flow giả. Local account đã liên kết Google vẫn giữ chức năng password local. Error map theo code, không hiện raw server message.
- `AccountSecurityViewModel` chỉ inject `ObserveSecurityAccountUseCase`, `ChangePasswordUseCase`, `DeactivateAccountUseCase`; không gọi Repository trực tiếp. Use case/validation ở `app/domain/security`, interface ở `core:common`, API/DTO/impl ở `core:network`; Screen/Content, UiState/Intent/Effect tách riêng. Không thêm Gradle module hay dependency feature → feature.
- Mutation tái sử dụng PreserveCase + retry disabled; kết quả mạng/5xx không chắc chắn không tự replay. Cleanup retry chỉ dọn local, không DELETE lại. Password chỉ giữ RAM, không SavedStateHandle/route/Room/DataStore; DTO/state/intent `toString()` redact.
- `SafeHttpLogger` chỉ log method/path/status ở debug, không body/header/query/credential; refresh lỗi tạm thời giữ cookie và trả lỗi transport, terminal mới clear cookie. Generation/revision guard chống response cũ ảnh hưởng phiên login đã publish; vẫn cần E2E/concurrency, không coi static review chứng minh mọi transport race đã hết.
- 25 test case: Use case (10), ViewModel (5), contract API (4), refresh (3), safe logging (1), session cleanup (2). Đã đối chiếu XML local PASS cho 25 test bổ sung N43 (23 test trong các suite security và 2 retirement cases trong SessionRepository suite); đây là kết quả user chạy, không phải agent tự chạy. Chưa có full assemble/lint/CI/E2E evidence. `N43_IMPLEMENTATION.md` hiện thuộc nhánh docs, không giữ bản handoff trên main.
- Bài học N43: user nhấn mạnh ViewModel → Use case → Repository không có đường tắt, kể cả quan sát session/capture token. Không mở rộng refactor các ViewModel lịch sử nếu chưa audit/report scope. Giữ nguyên hồ sơ backend blockers và chưa chốt M4/M5/M6.

- [ ] **N44 — Integration feature-complete (WIP)**:
  - [x] Chặng 1: history identity/generation isolation, Activity/History Detail qua Use case, result guard/cancel/reset; source commit `3ea792a`.
  - [x] Chặng 2: Profile observe/capture qua Use case; logout và Account Security session-end dùng reset MainGraph + dọn saved tab stacks.
  - [x] Đối chiếu XML PASS cho 16 regression test bổ sung; suite cũ trong phạm vi đã đọc cũng PASS. Chưa thay thế full lint/CI/build/E2E.
  - [ ] Chặng 3 transport: 2/2 cookie-race diagnostic FAIL đúng invariant; Google guest chưa tái hiện trong capture mới. User tạm hoãn điều tra/fix; instrumentation đã gỡ, không đóng bug hoặc đổi assertion.
  - [ ] **Chặng 4 — PARTIAL:** đã đọc và tổng hợp lượt user ghi 08/10/2026 22:00 trong `N44_E2E_REPORT.md`; giữ raw `N44_E2E_INTEGRATION_CHECKLIST.md`. Backend revision/APK SHA/full CI và nhiều nhánh chưa đủ evidence; không tick hoàn tất. User chọn lưu backlog rồi nghiên cứu UI N46.
  - **Bắt đầu bằng audit read-only** source Android/backend đúng revision + đối chiếu build/test evidence; báo cáo gap/quyết định rồi mới chốt checklist và kế hoạch sửa lỗi. Không coi checklist định hướng dưới đây là audit đã hoàn tất.
  - **Baseline:** ghi Android SHA/backend SHA/deployment revision nếu có; kiểm tra build/unit test/CI mới nhất. Bổ sung log bằng chứng còn thiếu ở N43, không suy ra test đã chạy từ số lượng test case hoặc việc đã commit.
  - **E2E N38–N43:** account Host/Player và guest; lịch sử → detail/answer sheet, Quiz Detail → ảnh → Solo Preview, profile/avatar → security → logout/session/navigation. Rà Back/tab restore/process recreation, refresh resource, visibility defense và account switch để không còn placeholder/lối vào chết hoặc UI account cũ.
  - **Realtime smoke/integration:** nối create/join → lobby → gameplay → final result/review → history; phân biệt phần Android có thể kiểm thử và case backend-blocked. N44 không thay thế full UAT N49.
  - **Backend-blocker audit rồi retest nếu đã sửa/deploy:** N20.6 active game; N25 presence race; N28.5 Host lives/streak/timeout/elimination/presence; N29 pause/resume snapshot riêng từng Player; N30 Player token renewal + sync progress/order. Không đánh PASS khi chỉ có build hoặc chưa tái hiện.
  - **Architecture/security audit:** ViewModel không gọi Repository trực tiếp; DTO dừng ở data; Screen/Content và Use case đúng tầng; password/token không lưu/log; session/cookie cleanup và lỗi mạng không hồi sinh/đăng xuất sai phiên. Legacy debt phát hiện phải báo scope trước khi sửa.
  - **Đầu ra:** test matrix với PASS/FAIL/BLOCKED/NOT RUN + bằng chứng/revision; bug ledger phân Android/backend, severity và release-blocker; đề xuất scope/việc cần duyệt để N45 quyết định feature freeze/M6.
  - Không workaround blocker hoặc thêm feature phase 2. User đã cho chuyển nghiên cứu UI N46; đây không phải chốt N45/M6 hay duyệt triển khai toàn bộ polish/release.
- [ ] **N45 — Feature freeze → Chốt M6**:
  - Chỉ chốt khi N38–N43 pass luồng chính và mọi blocker backend đã được sửa hoặc có quyết định release rõ ràng.
  - Khóa scope v1; chuyển import/Editor UX nâng cao/public creator profile/admin mobile sang phase 2.

### Tuần 10 (N46–50) — Polish, test, release & ship

- [ ] **N46 — UI polish WIP, chia mốc từng màn theo user duyệt**: đã được cho triển khai các mốc dưới đây trước khi N45 gate đóng. Không tick toàn N46 hoặc miễn trừ release blockers; sổ source/evidence chi tiết ở `N46_UI_PROGRESS.md`.
  - [x] **N46.0 — Font/theme/palette:** Inter 400/500/600/700 + license, 15 typography role, root theme và FrontendColors; `4b354b6`.
  - [x] **N46.1 — Shared component:** buttons, code input OTP/room, single-answer option, quiz card ngang, leaderboard top 3 huy chương, inline clickable text và gallery; `c0fedf3`. 10 normalization test đã viết, chưa có kết quả chạy mới được agent xác minh.
  - [x] **N46.1a — Preview component cũ:** Light/Dark tại mỗi file và image inspection placeholder; `f94d8f5`.
  - [x] **N46.2 — Splash:** giữ UI gốc, chỉ thêm tên app đậm; `1585073`.
  - [x] **N46.3 — Login:** theo ảnh 02, dùng shared buttons + Google icon + clickable register span; user xác nhận UI ổn; `0ba1244`.
  - [x] **N46.4 — Register:** theo ảnh 03, fullname/email/password/phone với nhãn ngoài field, phone icon ống nghe, primary/register và inline/login; user xác nhận oke và commit `ee8701c`.
  - [x] **N46.5a–N46.5b — Forgot/OTP/Reset:** `a1327bb` validator reset/confirm + request guards; `3d92af1` polish 3 màn. Giữ top app bar/email căn trái, bỏ icon lớn/checklist; OTP 6 ô + countdown/link resend gộp căn giữa dưới nút; 8 validation test đã viết, chưa xác minh run.
  - [x] **N46.6a–N46.6c — Home/component/navigation:** `9417aad` DiscoveryQuizCard/RoomCodeEntryCard/QuizListCard rename; `88a1307` Home + room 6 ô, user xác nhận ổn; `b68fbff` bottom nav outlined/chấm tím + full Home Preview, giữ 4 route. Không thêm fake progress/resume/rating.
  - [x] **N46.7 — Search:** `496dc36`, QuizListCard ngang, submit/clear/IME, append error/retry đúng cursor giữ results; bỏ history/hot topic/count/filter chưa có chức năng. Thêm 5 state test (tổng 9), chưa xác minh run.
  - [x] **N46.8 — Discover:** `d5571d6`, grid 2 cột/card đều/category chips/sort, xóa QuizCardItem. Có bản sửa import màu internal và Paging index crash; dấu [x] chỉ source đã commit, **chưa có xác nhận runtime retest sau sửa**.
  - [ ] **N46.next — Màn kế tiếp:** retest Discover/Search trước; chờ user chọn/ảnh/duyệt màn tiếp theo, không tự triển khai một mạch.
  - [ ] **Evidence và gate toàn N46:** build/lint/test/CI mới, máy thật/auth regression, dark design, font scale/TalkBack/touch target/contrast, animation/sheet dark mode. Các dấu [x] trên chỉ là triển khai/commit, không chứng minh full E2E PASS.

#### 📝 N46 Implementation Details — checkpoint đến Search/Discover

- Lần docs trước `8bf89d0` đối chiếu 7 commit đến Register `ee8701c`; lần này thêm **7 commit `ee8701c..d5571d6`** (tổng 14 sau `cd018bb`). Hash theo từng checkpoint ở `N46_UI_PROGRESS.md`; baseline source main `d5571d6` không đồng nhất với SHA APK đã test.
- Component stateless theo mẫu `AnswerOptionItem.kt`; code input wrapper chỉ giữ focus, content nhận text/selection; Login/Register hoist ScrollState. Giữ ViewModel/Use case/Repository và backend behavior, không fix blocker bằng UI workaround.
- Inter và palette light từ Figma/CSS, dark fallback tạm; ComponentColors internal chỉ dùng trong core:ui, feature dùng FrontendColors public + MaterialTheme dark. Gradient result mới là token, chưa coi Final Result đã polish. OTP/room đã tích hợp 6 ô; Search dùng QuizListCard, Home/Discover dùng DiscoveryQuizCard; QuizCardItem đã xóa. Gameplay/answer/leaderboard và các màn chưa duyệt vẫn còn mở.
- User review theo Preview từng màn; agent chưa chạy Gradle/render hoặc xác minh run id CI mới. User đã cung cấp crash Paging index trên Discover; snapshot fix đã commit nhưng chưa có xác nhận retest sau sửa (append/filter/sort/list shrink/back). Evidence N43/N44 lịch sử không thay thế kết quả N46. N44 PARTIAL/N45/M6/release gates giữ nguyên.
- Main có `/design/` trong `.gitignore`; docs không có file này, local `.git/info/exclude` đã thêm `/design/`. Ảnh tham chiếu không commit/push; tài liệu và sổ mốc này ở docs, source/tests ở main.

- [ ] **N47 — Regression + automated tests + bug sweep**: ViewModel/Turbine, MockWebServer, Room in-memory nếu còn dùng, Compose UI test các luồng trọng yếu; fix bug theo mức release blocker, không thêm feature mới.
- [ ] **N48 — Release hardening → Chốt M7**: LeakCanary, R8/ProGuard, `network_security_config`, Crashlytics/analytics, release build và smoke test bản minified.
- [ ] **N49 — UAT/Internal testing**: E2E đủ 5 mode + history/preview/account, tải nhẹ 20–50 player/phòng, Play Console internal track, tài liệu sử dụng và kiểm tra app signing.
- [ ] **N50 — Go/No-Go & ship → Chốt M8**: xử lý bug UAT cuối, bàn giao, retrospective và backlog phase 2; không Go nếu blocker dữ liệu/quyền/session/realtime nghiêm trọng còn mở.
- 📚 JUnit4 + `runTest`, Turbine, MockWebServer, Compose UI Test, LeakCanary, Play Console internal track và crash reports.

## 7. Việc backend song song (không block app)

- **N1–3**: Vá `session_code` unique (partial index + ON CONFLICT retry).
- **N1–5**: Vá design doc 6 điểm lệch ở mục 3.
- **N10–15**: Unit test `scoring.ts` + `config.rule.ts`.
- **Backlog**: gate `/v1/api-docs` theo env, chuẩn hóa zod, xóa enum `team`, bỏ publish port DB/Redis ở prod, validate quiz rỗng khi tạo phòng.

## 8. Rủi ro & giảm thiểu

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Chưa từng chia Gradle multi-module | Cao | ✅ Đã qua (M1 chốt 9/8); phương án B gộp 6 module không cần dùng nữa |
| Chưa có thói quen viết test | TB | Viết test ViewModel từ Tuần 5, không dồn Tuần 9 |
| Quen pattern JWT Interceptor từ dự án cũ | TB | Tuần 2 tập trung gỡ — backend chỉ đọc cookie |
| Socket.IO Android + namespace/cookie có quirks | TB | Spike N16–18 trên máy thật |
| Timer skew giữa các máy | TB | Offset `serverTime` tính lại mỗi event; test mạng yếu |
| Google OAuth / S3 chưa có tài khoản | TB | Đăng ký ngay Tuần 1–2 (One Tap cần ở N9) |
| Bug backend phát sinh khi tích hợp | TB | Log toàn bộ socket event ở debug build; kênh fix nhanh với chủ backend |
| Scope creep (import xlsx, Editor UX nâng cao, public profile, admin mobile) | Cao | Khóa ở N45; chỉ giữ history/preview/account vì thuộc feature-complete v1 |

## 9. Definition of Done

- Pass toàn bộ checklist kiến trúc/auth/realtime/testing ở mục 20 của design doc v2 (bản đã vá).
- 5 mode chơi E2E trên ≥3 thiết bị; reconnect giữa trận không mất state; `GONE` điều hướng đúng.
- Release build bật R8 + network security config; LeakCanary không báo leak socket; Crashlytics hoạt động.