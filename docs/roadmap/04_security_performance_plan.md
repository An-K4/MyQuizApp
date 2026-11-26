# Kế hoạch triển khai — Bảo mật và Tối ưu MyQuizApp

> Kế hoạch đã được người dùng duyệt; trạng thái thực hiện được theo dõi ở bảng dưới, không mặc định coi các task là đã nghiệm thu. Đây là nơi duy nhất theo dõi trạng thái task cho hai môn; các kế hoạch N1–N50 cũ là lịch sử, không tự động xem các gate cũ là đã PASS. Tri thức theo lô nằm trong `docs/knowledge/`.

## Bảng tổng quan checklist

Trạng thái ban đầu: **Chưa bắt đầu**. Dấu `[x]` chỉ dùng khi đã merge và hoàn tất nghiệm thu; BLOCKED/PARTIAL không đánh dấu x. Thứ tự thực tế tuân theo phụ thuộc, không suy các task cùng lô đều độc lập.

| Done | Mã | Lô | Task / kết quả ngắn | Phụ thuộc | Trạng thái |
|---|---|---:|---|---|---|
| [ ] | T01 | 1 | Khóa baseline và phương pháp đo before | — | Đã merge PR #7 (`dacf3bf`); PARTIAL: baseline pilot, ACK NOT RUN do BUG-ANDROID-01, hoãn đo đến khi sửa |
| [ ] | T02 | 1 | Loại log PII/secret, kiểm thử redaction | — | Đã merge PR #8 (`5ea7fad`); còn xác minh release/log thiết bị |
| [ ] | T03 | 1 | Chuẩn hóa và xác thực input theo hợp đồng | — | Đã merge PR #9 (`df1ba63`); còn kiểm tra lỗi hiển thị trên thiết bị |
| [ ] | T04 | 1 | Sửa cold-start offline/session Unknown | T01 | Đã merge PR #10 (`a2d5cb8`); user đã test nhánh, smoke test main chưa xác nhận |
| [ ] | T05 | 2 | Khôi phục ngữ nghĩa chọn/lưu cookie | — | Đã merge PR #11 (`c8d9463`); test Room và smoke upgrade phiên PASS, chưa xác nhận toàn bộ nghiệm thu |
| [ ] | T06 | 2 | Chặn race refresh/logout/session | T05 | Đang triển khai `work/t06-session-race`; unit test PASS, chưa xác nhận thiết bị/backend và chưa merge |
| [ ] | T07 | 2 | Mã hóa cookie bằng Keystore và migration | T05, T06 | Chưa bắt đầu |
| [ ] | T08 | 2 | Chính sách backup/restore dữ liệu bảo mật | T07 | Chưa bắt đầu |
| [ ] | T09 | 2 | Tích hợp C04 guestToken có bảo vệ | T07 | Chưa bắt đầu |
| [ ] | T10 | 3 | Tách Dev/Production và kiểm soát HTTPS/WSS | — | Chưa bắt đầu |
| [ ] | T11 | 3 | Pinning API và socket, chính sách host upload | T10, Backend | Chưa bắt đầu |
| [ ] | T12 | 3 | Bảo vệ deep link/reset Intent | —, xác nhận domain | Chưa bắt đầu |
| [ ] | T13 | 3 | Audit permission/component/URI | — | Chưa bắt đầu |
| [ ] | T14 | 4 | Kiểm tra ảnh quiz trước upload | T03, T10 | Chưa bắt đầu |
| [ ] | T15 | 4 | Kiểm tra ảnh avatar trước upload | T03, T10 | Chưa bắt đầu |
| [ ] | T16 | 4 | Hợp đồng chống gian lận và xác thực môi trường | Backend | Chưa bắt đầu |
| [ ] | T17 | 4 | Tương thích tra phòng và lifecycle socket có kiểm chứng | T01, T10, T16 | Chưa bắt đầu; BUG-ANDROID-01 cần sửa trước phép đo ACK |
| [ ] | T18 | 5 | Đo và tối ưu Coil/cache ảnh | T01 | Chưa bắt đầu |
| [ ] | T19 | 5 | Startup và Baseline Profile có benchmark | T01, T04 | Chưa bắt đầu |
| [ ] | T20 | 5 | Đo/sửa điểm nghẽn Compose và frame | T01 | Chưa bắt đầu |
| [ ] | T21 | 5 | Rà rò bộ nhớ theo lifecycle | T01 | Chưa bắt đầu |
| [ ] | T22 | 5 | R8, signing và smoke test release | T07–T15 | Chưa bắt đầu |
| [ ] | T23 | 5 | Đo after và so sánh hiệu năng | T01, T04, T17–T21 | Chưa bắt đầu |
| [ ] | T24 | 5 | Regression bảo mật và UAT | T02–T17, T22 | Chưa bắt đầu |
| [ ] | T25 | 5 | Báo cáo hai môn, bằng chứng và giới hạn | T23, T24 | Chưa bắt đầu |

## Mục tiêu, baseline và nguyên tắc

- **Mục tiêu:** cung cấp thay đổi Android kiểm thử được và bằng chứng cho cả hai đề tài; so sánh CPU, RAM, FPS/frame, pin, network, độ trễ và bảo mật, bao gồm trade-off thực tế.
- **Kiến trúc:** giữ Clean Architecture và hệ thống module hiện có; phân quyền, chấm điểm và xác thực phía server vẫn do Backend quyết định. Công nghệ hiện tại: Kotlin, Compose, Hilt, Room, DataStore, Retrofit/OkHttp, Socket.IO và Coil. API/chi tiết thiết kế chưa được quyết định phải đối chiếu source và chốt trong task, không mặc định thư viện mới.
- **Nguồn yêu cầu:** mô tả hai môn trong yêu cầu người dùng ngày 21/11/2026; tham khảo [kế hoạch tối ưu trước đó](../optimization-report/01_optimization_plan_4_sprints.md), [kịch bản đo](../optimization-report/03_benchmark_metrics_scenarios.md) và [17 bug Backend](../backend-audit/01_backend_17_bugs_status.md). File cá nhân `my_notes.md` không phải tài liệu repo và tuyệt đối không commit/push.
- **Mốc tham chiếu đã đọc:** Android `main` `31f8fbf`, Backend local `main` `37038f6`. Trước mỗi lô kiểm tra HEAD, contract, quyền và deployment; các mốc này không chứng minh production hoặc test runtime. Không sửa Backend; nếu thiếu khả năng để đáp ứng đề tài, báo người dùng nhờ bạn phụ trách, ghi blocker và giới hạn.
- **Thời gian:** khoảng 2 tuần nếu gấp, 25 ngày lý tưởng; mốc giữa kỳ khoảng 05–15/12/2026 cho hai môn. 25 nhánh là phạm vi đề xuất, **không phải cam kết chắc chắn hoàn tất trong 2 tuần**. Sau baseline và khảo sát Backend, người dùng duyệt phạm vi/công sức; nếu thiếu thời gian phải nêu rõ task chưa đạt, không tự xóa tiêu chí đã đăng ký.
- **Thiết kế được đề xuất:** một roadmap chung theo 5 lô, mỗi task một nhánh/review độc lập; so với tách hai roadmap sẽ tránh làm hai lần cùng hạng mục. Trong cùng lô, chỉ tách nhánh song song nếu không có phụ thuộc hoặc chồng lấn đáng kể.

## Quy tắc thực thi, trạng thái và nghiệm thu

1. **Chỉ bắt đầu code sau khi người dùng duyệt plan.** Tên nhánh `work/tNN-ten-ngan`, tạo từ `main` đã cập nhật; T05→T06→T07→T08/T09 tuần tự. Với nhánh phụ thuộc chưa merge, có thể stack trên nhánh cha khi thống nhất rõ base PR, không giả định được merge trực tiếp vào main. Khi cha merge, cập nhật/rebase nhánh con theo quy tắc repo và chạy lại test; nếu chưa có quyền/lệnh phù hợp, hỏi người dùng.
2. Một task = một nhánh, commit cụ thể và PR riêng; chỉ stage các đường dẫn được phép, kiểm tra `git diff --cached` và `git status` trước commit/push. Không dùng `git add .` và không track `my_notes.md`. Agent tiếp tục task độc lập sau khi push; dừng khi cần người dùng test, tạo/duyệt/merge PR, cấp quyền hoặc khi bị chặn. Người dùng quyết định và thực hiện merge; không tự merge. Có thể nghiệm thu theo lô, không đợi để báo blocker nghiêm trọng.
3. Để giảm xung đột, các nhánh task **không cùng sửa bảng tiến độ hoặc knowledge lô trong lúc phát triển song song**. Khi user merge, cập nhật tập trung roadmap và bài học theo lô tuần tự trên một nhánh tài liệu/PR riêng nếu main được bảo vệ; đây là nhánh quản trị tài liệu, không phải task code. Bổ sung kiến trúc chịu ảnh hưởng trong PR của task khi cần, điều phối các file chồng lấn tuần tự.
4. Mỗi task: tái hiện/viết test → xác nhận test thất bại đúng lý do nếu phù hợp → thay đổi nhỏ → test lại → lint/test/build theo phạm vi và CI → ghi lệnh, commit, kết quả → bàn giao. Với audit/đo/manual test, thay bằng fixture, bước tái hiện và kết quả quan sát. Tên test trong task là **hành vi cần kiểm**, không tuyên bố test đã tồn tại.
5. `Android CI` job `build` chạy lint, unit test và assemble debug khi PR vào main. Xác nhận ruleset thực tế; local Gradle qua `aki-gradle-start` theo allowlist, đối chiếu task/exit code; thay đổi Gradle/thư viện phải chạy `clean` trước build theo yêu cầu. `clean` không xóa dependency cache. Không tự mở rộng quyền: nếu cần release/profile/instrumentation/adb shell hoặc công cụ đo vượt allowlist, nêu lệnh, mục đích và xin user chạy/cho phép.
6. Trạng thái hợp lệ: **Chưa bắt đầu → Đang làm → Chờ kiểm thử/PR → Đã merge, chờ nghiệm thu → Hoàn tất**, hoặc **Blocked / Partial** kèm lý do. Ghi commit, PR, người test, thiết bị/môi trường, ngày, lệnh, PASS/FAIL/NOT RUN/BLOCKED và evidence đã che dữ liệu ngay dưới task. Chỉ `[x]` khi mọi tiêu chí bắt buộc đạt hoặc người dùng duyệt ngoại lệ ghi rõ; không thay PASS bằng build thành công.
7. Test Backend/giả mạo chứng chỉ chỉ ở môi trường và tài khoản được phép. Không ghi token, cookie, credentials, private key, PII hay payload nhạy cảm vào Git, log hoặc screenshot. Đo và báo cáo phải phân biệt việc Android làm, Backend đã có, và điều chưa kiểm chứng.

**Nghiệm thu mỗi lô:** Agent giao từng nhánh/PR, lệnh và checklist; người dùng chạy test thiết bị và xem CI, tạo/duyệt/merge theo thứ tự phụ thuộc. Sau merge, smoke test tích hợp trên `main`, cập nhật bảng và knowledge tuần tự. Không chuyển task phụ thuộc nếu gate liên quan chưa đạt; task độc lập có thể tiếp tục. Lô được chốt khi từng task đạt hoặc blocker được ghi và người dùng quyết định, không tự gán PASS.

## Phản hồi Backend cần đối chiếu

- **Nguồn:** phản hồi người dùng chuyển từ thành viên Backend; chưa tự kiểm thử deployment. Các chỗ ghi trong ngoặc vuông ở phản hồi về host, nhà cung cấp SSL và môi trường test vẫn là placeholder, không phải giá trị được xác nhận. Không sao chép bí mật vào tài liệu.
- **App Link:** roadmap lịch sử ghi team Backend từng thêm `assetlinks.json`, trong khi phản hồi mới nói hiện domain chưa có. Source Android đã có intent-filter, handler và verify token đổi ticket; Backend local `37038f6` tạo URL `${FRONTEND_URL}/reset-password/link?token=...`. Người dùng cho biết đã trực tiếp test và mở link email thật vào app; chưa có ngày, phiên bản APK, kết quả complete hay trạng thái domain verification kèm theo. Chưa xác minh URL/tệp live hiện tại; cần hỏi team đã đổi domain/deployment hay xóa file, kiểm tra fingerprint và test lại email. Xem T12/B3; giữ `/reset-password/link` khi hardening.
- **Upload:** Backend báo object `public-read` có thể tồn tại/truy cập trước khi `assertOwnedUpload` từ chối; hiện chưa xóa khi fail hay có job dọn mồ côi. Xác minh triển khai, nhờ team đánh giá xóa file sai/lifecycle hoặc vùng tạm private; không nhầm khắc phục sau upload với ngăn upload từ đầu.
- **Xác thực môi trường:** Backend báo chưa có attestation/Play Integrity; nhận diện root/emulator/debug cục bộ chỉ là tín hiệu dễ bị vượt qua, không phải xác thực đáng tin cậy hay xác minh server. T16 cần quyết định mức đáp ứng yêu cầu báo cáo cùng người dùng/giảng viên; Play Integrity + xác minh Backend là một lựa chọn, chưa tự xem là bắt buộc.
- **Phiên/privacy:** phản hồi mô tả rotation và leaderboard; local `37038f6` cho thấy refresh token đã dùng vẫn trả lỗi trong 10 giây, cửa sổ này chỉ ảnh hưởng thu hồi family. `getHistorySummary` trả leaderboard cho participant sau khi kiểm tra là người tham gia, không có nhánh `showLeaderboard=never` tại hàm đó. Cần xác nhận deployment và test trực tiếp `/results` lẫn `/summary`; không kết luận mọi endpoint đã bảo vệ `never`. Source `user.repository.ts` cũng tăng session_version và xóa refresh sessions khi đổi mật khẩu; cập nhật giả định cũ khi nghiệm thu.

## Audit tĩnh Backend theo các phụ thuộc

- **Mốc:** đã `git fetch origin`, local Backend `main` sạch và trùng `origin/main` tại `37038f6`. Đây là audit tĩnh có phạm vi theo B1–B5, không chứng minh bản đang triển khai, cấu hình runtime hay kết quả test.
- **B1 — session và C04:** `auth.controller.ts`, `auth.repository.ts`, `auth.service.ts`, `shared/utils/cookie.ts` cho thấy login/refresh trả cookie HttpOnly, SameSite=Lax, Secure khi `NODE_ENV=production`; refresh xoay vòng trong transaction theo family, reuse trong 10 giây vẫn bị từ chối nhưng chưa thu hồi family; sau đó reuse thu hồi family. Logout cần access và refresh cookie. TTL thực tế là cấu hình: `backend/.env.example` ghi 15m/7d, `.env.example` gốc ghi 1d/7d; không suy ra TTL production. Đổi/reset mật khẩu tăng session_version, xóa refresh session và gọi ngắt socket theo source.
- **C04:** `game.controller.ts`, `game.service.ts`, `socket.middleware.ts`, `game.route.ts` cho thấy join guest cấp JWT có ký theo guest ID do client gửi, hạn theo `JWT_REFRESH_EXPIRES_IN`; `/history`, `/:id/summary`, `/:id/my-answers` nhận `x-guest-token`, user đã đăng nhập được ưu tiên. Đường kiểm tra token không thấy cơ chế thu hồi riêng từng guest token. **Rủi ro cần Backend đánh giá:** vì join nhận UUID client cung cấp và ký token cho UUID đó, người biết UUID của guest khác và vào được một phòng cho guest có thể có đường xin token mang UUID ấy rồi truy cập lịch sử tương ứng. Đây là suy luận tĩnh có điều kiện, chưa thử khai thác; việc từ chối header UUID thô không tự chứng minh an toàn cấp token. Cần kiểm thử trên môi trường disposable và thiết kế định danh guest/chứng minh sở hữu trước khi khẳng định C04 an toàn.
- **B2/B3 — cấu hình và App Link:** cấu hình host/TTL phụ thuộc biến môi trường; Android manifest khai báo host `myquizz.dpdns.org`, backend tạo link từ `FRONTEND_URL`, không thể suy hai host runtime luôn trùng. Cây git frontend tại commit trên không có `frontend/public/.well-known/assetlinks.json`; `frontend/nginx.conf` có SPA fallback, nhưng server/proxy live có thể cấu hình khác. Người dùng đã mở email link thật trong app; điều đó không tự chứng minh trạng thái Android verified.
- **B4 — upload:** `storage.route.ts`, `storage.schema.ts`, `storage.service.ts` và chỗ gọi trong `quiz.service.ts`/`user.service.ts` xác nhận presign cần auth, size tối đa 2 MiB, MIME allowlist, URL 300 giây và ACL `public-read`; lúc gắn ảnh mới kiểm tra key theo user, HEAD size/MIME và chữ ký 16 byte. Nhánh xác thực thất bại không gọi xóa object; không thấy job dọn trong cây mã được duyệt. Chữ ký đầu file không phải kiểm tra toàn vẹn đầy đủ hay xác minh ảnh giải mã an toàn. Upload rate limit dùng cặp user+IP, chỉ tính request thành công và fail-open khi Redis lỗi theo middleware.
- **B5 — game:** `game.socket.ts`/`game.cache.ts` có xử lý server deadline, chấm điểm, index và ghi chống trùng bằng Redis; có ngoại lệ theo cấu hình `allowAnswerLate` và grace 1 giây cho host-paced, không tuyên bố mọi câu trả lời trễ đều bị chặn. `/results`/`leaderboard` kiểm tra `never` với người không phải host; `getHistorySummary` cho participant vẫn trả leaderboard không xét `never`. Chưa tìm thấy triển khai Play Integrity/attestation trong source đã tìm; client-only không tương đương server attestation. `tests/integration/docker-game-smoke.mjs` là kịch bản tham khảo cần database disposable localhost; chưa chạy trong đợt audit này.

## Phụ thuộc bên ngoài còn cần xử lý

- **B1 — phiên/C04:** không cần hỏi lại contract cơ bản đã đọc trong code. Nhờ Backend đánh giá/sửa hoặc chấp nhận có ghi nhận rủi ro cấp guestToken theo UUID nêu trên; test auth/expiry/replay/logout ở môi trường được phép. T09 có thể phát triển contract client, nhưng không đánh dấu end-to-end an toàn khi rủi ro server chưa được xử lý hoặc chấp nhận minh bạch. Sinh trắc/MFA là *chuẩn bị khi có yêu cầu*; MFA thực sự cần Backend.
- **B2 — TLS/pinning:** cần host API/Socket/upload **đang triển khai**, môi trường test, CA, public SPKI hiện tại/dự phòng, chủ sở hữu/rotation và quyền test; không yêu cầu private key. T11 BLOCKED phần pin an toàn nếu chưa có backup và quy trình xoay vòng thống nhất.
- **B3 — assetlinks:** chỉ cần kiểm tra live `/.well-known/assetlinks.json`, package `android.kma.myquizzapp`, fingerprint certificate bản cài và trạng thái domain verification trên thiết bị; không mặc định file live vắng chỉ vì không nằm trong repo. Hỏi Backend nếu cần đối chiếu cấu hình live hoặc sửa. Giữ link reset người dùng đã test; không gửi private key.
- **B4 — upload:** không cần hỏi lại presign/ACL/MIME đã đọc từ code. Đề nghị Backend quyết định private quarantine trước công khai và/hoặc dọn object lỗi/mồ côi; xóa sau fail không xóa khoảng public trước đó. T14–T15 vẫn làm được kiểm tra client, nhưng không thể một mình bảo đảm server chặn client độc hại; ghi rõ giới hạn nếu chưa sửa Backend.
- **B5 — game/môi trường:** không cần hỏi lại contract cơ bản trong code. Nhờ Backend xem lỗ hổng tiềm năng leaderboard `/summary` khi `never` và test response thật; chọn mức xác thực môi trường phù hợp tiêu chí báo cáo, nếu cần verdict đáng tin cậy phải có xác minh Backend. Cần môi trường và dữ liệu test được phép; không tự kết luận triển khai live giống local.

## Lô 1 — Baseline và vệ sinh client

Knowledge: [kn_24](../knowledge/kn_24_lot_1_baseline_client.md). Gate: baseline có thể tái hiện, redaction/input/offline có regression; chưa có thiết bị thì giữ T01 PARTIAL và cố định commit để đo về sau.

### T01 — Baseline trước thay đổi
**Phạm vi:** [kịch bản đo](../optimization-report/03_benchmark_metrics_scenarios.md); không thay code sản phẩm. **Phụ thuộc:** thiết bị, môi trường thử và user hỗ trợ profiler.

**Pilot:** [báo cáo baseline before](../optimization-report/04_baseline_before.md); T01 còn PARTIAL. Socket ACK NOT RUN do [BUG-ANDROID-01](../backend-audit/01_backend_17_bugs_status.md); người dùng chọn hoãn sửa và đo lại sau, đã xếp vào T17. Việc chốt lô với phần hoãn cần ghi rõ ngoại lệ và quyết định nghiệm thu, không gọi số đo thiếu là PASS hay tự gán gate đã đạt.
- [x] Ghi nhận các phép đo pilot startup, RAM/CPU, frame, REST, traffic và pin cùng điều kiện, giới hạn và nơi lưu bằng chứng trong báo cáo baseline.
- [ ] Ghi commit/APK, thiết bị/OS, màn hình, nhiệt độ, pin, mạng, backend, dataset và trạng thái cache; pilot Classic hoặc Solo không shuffle, chỉ chọn kịch bản thực sự chạy được.
- [ ] Thống nhất cold/warm startup, cuộn Home/Discover, upload/API và phiên socket; lấy mẫu CPU, RAM/PSS, frame time/jank/FPS, pin, traffic/request và latency REST/socket. Tối thiểu 5 lần cho startup/latency, báo N/p50/p95 khi đủ mẫu; pin chạy phiên cố định và nêu độ phân giải. Nếu công cụ/thiết bị không đủ, ghi NOT RUN và xin hỗ trợ, không bịa.
- [ ] Lưu dữ liệu thô đã che vào nơi được duyệt, phương pháp/công cụ và baseline trước code. **Đạt:** chạy lại được, từng chỉ số có đơn vị hoặc NOT RUN kèm lý do; không dùng debug so với release.

### T02 — Log an toàn
**Kiểm thử sau merge:** `testDebugUnitTest` trên `a2d5cb8` thành công (lệnh tổng, job `26df39b8`); không thay thế build release, log thiết bị hay xác nhận CI.

**Vị trí:** `core/network/.../di/SafeHttpLogger.kt`, `feature/auth/.../domain/usecase/ForgotPasswordUseCase.kt`, `AuthRepositoryImpl` và các log exception/ACK tìm được. **Phụ thuộc:** không.
- [x] Bổ sung test logger với chuỗi giả ở path, header, query, body, response và exception; rà và giảm log nhạy cảm tại các điểm auth/session/socket trong code T02 `8133c7d`.
- [ ] Xác minh build debug/release và log trên thiết bị theo phạm vi yêu cầu; chưa có bằng chứng release và log thiết bị trong lần rà sau merge. **Đạt:** kiểm thử log giả không lộ, build release/debug tương ứng; phần chưa chạy ghi NOT RUN.

### T03 — Chuẩn hóa input
**Kiểm thử sau merge:** `testDebugUnitTest` trên `a2d5cb8` thành công (job `26df39b8`); không thay thế kiểm tra UI/endpoint thật.

**Vị trí:** `feature/auth/.../presentation/validation/AuthValidator.kt`, `app/.../domain/security/SecurityValidation.kt`, `core/common/.../validator/`. **Phụ thuộc:** không.
- [x] Lập bảng trường, giới hạn và quy tắc Backend trong [kn_24](../knowledge/kn_24_lot_1_baseline_client.md); bổ sung validation và test input trong T03 `fb39375`, giữ nguyên nội dung mật khẩu.
- [ ] Xác minh lỗi hiển thị trên thiết bị, đối chiếu endpoint thật. **Đạt:** input hợp lệ không bị hỏng, server vẫn xác thực cuối.

### T04 — Offline boot
**Kiểm thử sau merge:** `testDebugUnitTest` trên `a2d5cb8` thành công (job `26df39b8`); các test chẩn đoán opt-in không tự chạy, smoke test thiết bị trên main chưa xác nhận.

**Vị trí:** `app/.../presentation/splash/SplashViewModel.kt`, `app/.../domain/activity/ObserveActivitySessionUseCase.kt`, `app/.../domain/profile/ObserveProfileSessionUseCase.kt`, `core/network/.../repository/SessionRepositoryImpl.kt`. **Phụ thuộc:** T01.
- [x] Tái hiện cold start offline với phiên cũ; bổ sung regression và sửa retry có giới hạn, không thay lỗi mạng bằng Guest (T04 `5bc51d9`).
- [x] User test trên nhánh T04: Activity/Profile offline→online không logout nhầm; xác nhận retry và reconnect trên bốn màn hình.
- [ ] Smoke test tích hợp trên main sau merge; chưa có bằng chứng kịch bản guest/mạng chậm.

## Lô 2 — Cookie, phiên và lưu trữ

Knowledge: [kn_25](../knowledge/kn_25_lot_2_session_keystore.md). Merge T05→T06→T07, sau đó T08/T09; tránh cùng sửa migration/schema song song. Gate: migration và phiên không mất/khôi phục sai; test thiết bị/Backend B1 được xác nhận.

### T05 — Cookie scope
**Đang làm:** Trước sửa, `testDebugUnitTest` job `0bdff051` tái hiện 3 test lỗi về host-only, domain và path/Secure. `:core:database:connectedDebugAndroidTest` job `60b92150`: 2 test dữ liệu giả PASS trên Android 11, gồm migration Room v1→v2 và phạm vi lưu/xóa. `testDebugUnitTest` job `9b8b1c66` thành công (các tác vụ up-to-date; chẩn đoán N44 opt-in vẫn skipped). Cài đè debug job `73c0a30e` trên Live 4 Android 11: user xác nhận vẫn đăng nhập sau nâng cấp. Chưa merge/kiểm thử đầy đủ các vòng đời khác.

**Vị trí:** `core/common/.../cookie/CookieStore.kt`, `core/database/.../cookie/RoomCookieStore.kt`, `core/database/.../entity/CookieEntity.kt`, `core/network/.../cookie/PersistentCookieJar.kt`. **Phụ thuộc:** không.
- [x] Viết test cookie host-only/domain, path khác nhau cùng tên, Secure/HTTP, expires và xóa; xác định migration schema từ bản cũ.
- [ ] Bảo toàn thuộc tính và chọn bằng quy tắc tương đương `Cookie.matches(url)`; test migration + vòng đời, không làm rơi phiên đang hợp lệ. **Đạt:** từng test scope và upgrade pass, không gửi cookie sai scope.

### T06 — Chống race phiên
**Vị trí:** `core/network/.../cookie/TokenAuthenticator.kt`, `CredentialLifecycle.kt`, `CredentialCookieInterceptors.kt`, `core/network/.../repository/SessionRepositoryImpl.kt`, `core/network/src/test/.../N44CookieRaceReproductionTest.kt`. **Phụ thuộc:** T05.

Nhánh triển khai dựa trên `c8d9463`: hai ca N44 tái hiện lỗi trước thay đổi và được đưa vào regression; `testDebugUnitTest` PASS (job `2698e259-2720-4930-a7a7-f9f2d6336edb`), `:app:assembleDebug` PASS (job `4f049d68-62f1-4893-a8bf-f31912e282cd`), `:app:lintDebug` PASS (job `fa847cad-5849-454d-9323-c41040e3d115`). Test mock không xác nhận server deployment, thiết bị thật hay server revoke. Chưa nghiệm thu/merge; kiểm thử thực tế cần tài khoản test có thể đăng xuất và đăng nhập lại.
- [ ] Chạy/tái hiện hai test chẩn đoán opt-in: refresh cũ không xóa login mới; Set-Cookie đến sau logout không hồi sinh phiên; thêm refresh đồng thời và offline.
- [ ] Thiết kế generation/session guard ở ranh giới ghi cookie và logout, bảo đảm refresh đúng phiên; regression pass. **Đạt:** thứ tự phản hồi đối kháng không xóa hoặc khôi phục sai phiên; không log token.

### T07 — Keystore và migration cookie
**Vị trí:** `core/database/.../cookie/RoomCookieStore.kt`, `CookieEntity.kt`, `CookieDao.kt`, module security phù hợp; test migration Room và Android Keystore trên thiết bị. **Phụ thuộc:** T05–T06.
- [ ] Chốt threat model, AES-GCM/Android Keystore, nonce và version; định nghĩa đường di chuyển cookie plaintext và fail-closed khi khóa mất/hỏng, không đưa key vào repo.
- [ ] Viết test dữ liệu cũ→mới, logout, corruption, key invalidation; triển khai migration có transaction và dọn bản rõ, không ghi secret vào log. **Đạt:** test upgrade/khôi phục thực tế xác nhận token không hiện plaintext theo threat model; đo chi phí startup so T01.

### T08 — Backup và restore
**Vị trí:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/` backup/data extraction rules. **Phụ thuộc:** T07.
- [ ] Chọn tắt backup hoặc loại trừ dữ liệu bảo mật phù hợp API 26+; khai báo rõ cloud và device transfer, xác minh merged manifest/rules.
- [ ] Test backup/restore trên thiết bị khả dụng, dữ liệu thường theo chính sách, khóa mất, re-login an toàn. **Đạt:** dữ liệu bảo mật không có trong đường backup được kiểm thử; ghi giới hạn OEM/API, không suy `allowBackup=false` ngăn mọi chuyển dữ liệu.

### T09 — Guest token C04
**Vị trí:** `core/network/.../dto/GameDtos.kt`, `api/GameApiService.kt`, `repository/GameSessionRepositoryImpl.kt`, `core/datastore/.../GuestIdentityStore.kt` và tầng lưu token bảo mật T07. **Phụ thuộc:** T07, B1.
- [ ] Test join nhận guestToken, user-cookie ưu tiên; history/summary/my-answers gửi `x-guest-token`, không dùng UUID làm bearer; test thiếu/sai/hết hạn, đổi tài khoản và cài đặt cũ.
- [ ] Áp contract đã xác nhận, bảo vệ token và vòng đời, không log. **Đạt:** unit/integration và người dùng test đủ 3 API; nếu Backend không sẵn ghi BLOCKED, không mô phỏng PASS.

## Lô 3 — Transport và bề mặt Android

Knowledge: [kn_26](../knowledge/kn_26_lot_3_transport_components.md). Gate: Dev/release tách biệt, TLS/Socket/Intent test đúng môi trường; T11 phụ thuộc B2.

### T10 — Dev/Production và HTTPS/WSS
**Vị trí:** `core/network/build.gradle.kts`, `core/network/.../di/NetworkModule.kt`, `socket/GameSocketClient.kt`, manifest/network security config theo variant. **Phụ thuộc:** không.
- [ ] Xác định URL cho từng variant và API/public/upload/socket, tránh bí mật trong BuildConfig; production từ chối cleartext, Dev CA chỉ debug; xét URL presigned host riêng.
- [ ] Test HTTP bị từ chối trên release stack, HTTPS hợp lệ, Socket.IO WSS và Dev không lẫn Production; kiểm merged manifest. **Đạt:** release không chứa bypass/trust-all; test môi trường thực tế được ghi rõ.

### T11 — Certificate pinning
**Vị trí:** `core/network/.../di/NetworkModule.kt`, `socket/GameSocketClient.kt`, cấu hình network security theo thiết kế T10. **Phụ thuộc:** T10, B2.
- [ ] Có SPKI hiện hành/dự phòng đã được chủ sở hữu xác nhận, chính sách rotation/release và host; xác định pinning trên API và Socket.IO, upload áp chính sách phù hợp host khác.
- [ ] Test CA hợp lệ/sai, pin đúng/sai, backup/rotation, WSS và debug override bằng endpoint test; đo latency so baseline. **Đạt:** release chặn sai pin trên các stack trong phạm vi, không làm hỏng host upload; thiếu backup/host/test thì BLOCKED, không tự bịa pin.

### T12 — Intent và App Link
**Vị trí:** `app/src/main/AndroidManifest.xml`, `app/src/main/java/android/kma/myquizzapp/MainActivity.kt` và navigation reset. **Phụ thuộc:** B3 cho domain verification. Người dùng xác nhận từng tự test mở link email thật vào app; chưa có thông tin build/ngày hoặc kết quả xác minh domain, nên ghi nhận như evidence mở link do người dùng báo, không suy T12 đã hoàn tất.
- [ ] Test action, scheme, host, path theo hợp đồng thật (bao gồm `/reset-password/link`), token rỗng/độc hại/hết hạn, intent explicit, app đang đóng/đang chạy/`onNewIntent`; không log hay để lộ token qua route lâu hơn cần thiết.
- [ ] Siết xử lý hợp lệ mà không phá reset: link email → verify token đổi ticket → complete, giữ cả OTP fallback. User kiểm tra assetlinks, fingerprint ký và link từ email thực tế. **Đạt:** input giả không khởi động luồng sai; link hợp lệ và OTP hoạt động; test domain chưa xác minh là BLOCKED.

### T13 — Permissions, components và chia sẻ URI
**Vị trí:** manifest app + merged manifest của dependencies, luồng Photo Picker/URI tại profile và quiz. **Phụ thuộc:** không.
- [ ] Liệt kê permission và exported component ở debug/release merged manifest, xác minh tính cần thiết; kiểm thử explicit Intent và URI grant/truy cập chéo app với fixture an toàn.
- [ ] Thu hẹp quyền và grant không cần thiết, bảo vệ component có thể nhận input; không thêm quyền lưu trữ tổng quát. **Đạt:** danh mục và test thiết bị có bằng chứng, không khẳng định khai thác khi chưa tái hiện.

## Lô 4 — Input ảnh, game và socket

Knowledge: [kn_27](../knowledge/kn_27_lot_4_upload_socket.md). Gate: file sai bị client từ chối trước presign/PUT, anti-cheat được đối chiếu Backend; socket không phá trận.

### T14 — Ảnh quiz
**Vị trí:** `feature/quiz-manage/.../domain/util/ImageCompressor.kt`, `domain/usecase/UploadImageUseCase.kt`. **Phụ thuộc:** T03, T10; Backend B4.
- [ ] Test ảnh hợp lệ, rỗng/hỏng, MIME khác bytes, file giả và cuối nén >2 MiB kể cả tại quality tối thiểu; xác minh không gọi presign/PUT khi sai.
- [ ] Kiểm tra kích thước bytes cuối, MIME/chữ ký hợp lệ, decode an toàn và thông báo; không tải file xấu. **Đạt:** test ranh giới + thiết bị upload thành công/thất bại; server ownership và chính sách hậu upload xác nhận B4, không hứa client ngăn client độc hại.

### T15 — Ảnh avatar
**Vị trí:** `app/.../presentation/profile/AvatarImagePreparer.kt` và use case update avatar. **Phụ thuộc:** T03, T10; Backend B4.
- [ ] Test URI không đọc được, ảnh hỏng, MIME/signature sai, ảnh lớn, output JPEG vượt 2 MiB và giải phóng stream/bộ nhớ; kiểm tra không gọi upload khi lỗi.
- [ ] Bổ sung kiểm tra output cuối và lỗi phù hợp, tái sử dụng logic chỉ nếu không tạo phụ thuộc chéo bất hợp lý. **Đạt:** unit test và user test chọn/upload ảnh hợp lệ, từ chối ảnh giả/quá lớn.

### T16 — Chống gian lận và môi trường
**Vị trí:** `core/network/.../socket/`, `feature/game-player/`, tài liệu Backend được phép. **Phụ thuộc:** B5.
- [ ] Đối chiếu contract server role, thời hạn/grace, câu hỏi, đáp án, duplicate và timeout; fixture test invalid/expired token, role sai, câu sai, gửi trùng/quá hạn, reconnect, `showLeaderboard=never` trên cả result/summary.
- [ ] Sửa riêng cách Android xử lý phản hồi sai/timeout nếu cần, không tự chấm điểm thay server. Xác nhận cơ chế xác thực môi trường và mức yêu cầu giảng viên; nếu cần server attestation chưa có, báo user/Backend và đánh dấu BLOCKED. **Đạt:** bằng chứng server + client rõ ràng, không tuyên bố ngăn mọi gian lận.

### T17 — Lifecycle Socket.IO
**Vị trí:** `core/network/.../socket/GameSocketClient.kt`, DTO/API tra phòng, repository socket và lifecycle tiêu thụ; **phụ thuộc:** T01, T10, T16. BUG-ANDROID-01 là phần xử lý contract ưu tiên trước kịch bản ACK; các phụ thuộc lifecycle vẫn giữ nguyên.
- [ ] Sửa [BUG-ANDROID-01](../backend-audit/01_backend_17_bugs_status.md): DTO lookup tương thích public session không có `quiz_snapshot_id`, regression DTO và E2E mở/sửa/lưu cấu hình; sau đó đo lại ACK T01, lưu điều kiện và đánh dấu trước/sau theo đúng phiên bản.
- [ ] Đo trước socket trong/ngoài trận ở foreground/background, request/ping, pin và reconnect; viết test listener cleanup, mất mạng, ACK timeout không tự double-submit và resync.
- [ ] Chỉ điều chỉnh vòng đời nếu phép đo cho thấy lợi ích và contract presence cho phép; nếu không an toàn, ghi quyết định không đổi cùng số liệu. **Đạt:** reconnect/đồng bộ/đáp án không regression, so sánh pin/network cùng kịch bản.

## Lô 5 — Hiệu năng, release và tổng hợp

Knowledge: [kn_28](../knowledge/kn_28_lot_5_performance_release.md). Gate: từng tối ưu chứng minh bằng đo hoặc kết luận không tối ưu; release/security/UAT và hai báo cáo có evidence và giới hạn.

### T18 — Coil và cache ảnh
**Vị trí:** `app/.../MyQuizzApplication.kt`, cấu hình Coil hiện hữu và `core/ui/.../components/RemoteImage.kt`, `QuestionImage.kt`. **Phụ thuộc:** T01.
- [ ] Đo cache cold/warm, traffic, PSS và cuộn ảnh trên cùng dataset; test thay avatar/ảnh không hiện stale sai phiên.
- [ ] Điều chỉnh `ImageLoader`/cache/size theo Coil hiện tại nếu có lợi, không hứa tỷ lệ. **Đạt:** test memory/cache/privacy và kết quả before/after hoặc quyết định không đổi có bằng chứng.

### T19 — Startup/Baseline Profile
**Vị trí:** startup `app/`, cấu hình Gradle/profile module nếu được phép. **Phụ thuộc:** T01, T04.
- [ ] Xác định đường startup nóng và benchmark profileable/release; tạo Baseline Profile cho startup/navigation thực tế nếu hạ tầng hỗ trợ, không nhầm dependency với profile.
- [ ] So cold/warm tương đương với p50/p95 và N; clean trước build khi đổi Gradle. **Đạt:** benchmark và profile thực sự được áp dụng hoặc NOT RUN/BLOCKED với quyền/thiết bị cần xin; không bịa cải thiện.

### T20 — Giao diện và frame
**Vị trí:** Compose Home/Discover và màn hotspot thực tế, `core/ui/`. **Phụ thuộc:** T01.
- [ ] Dùng cùng kịch bản scroll đo frame time/jank/FPS và xác định hotspot recomposition/layout; test chức năng và state trước thay đổi.
- [ ] Chỉnh điểm nghẽn có bằng chứng (stable keys/state/read scope nếu phù hợp). **Đạt:** UI test/smoke pass và dữ liệu before/after, hoặc ghi không tìm thấy cơ hội.

### T21 — Rò bộ nhớ
**Vị trí:** lifecycle Activity/Compose/socket được đo, debug-only tooling nếu cần. **Phụ thuộc:** T01.
- [ ] Đo RAM/PSS qua mở/đóng, xoay màn, join/leave; dùng leak detector debug nếu được duyệt, xác minh không đi vào release.
- [ ] Tái hiện/fix leak có bằng chứng hoặc ghi không tái hiện, test listener cleanup. **Đạt:** report được che thông tin, không khẳng định 0 leak khi chưa đo đầy đủ.

### T22 — Release/R8/signing
**Vị trí:** `app/build.gradle.kts`, `app/proguard-rules.pro`, `.github/workflows/release.yml`, tài liệu ký. **Phụ thuộc:** T07–T15; thiết bị và quyền release.
- [ ] Build minified release, smoke auth, upload, deep link, Socket và game; lưu mapping an toàn, thêm rule tối thiểu nếu lỗi thực tế. Kiểm tra Gradle clean nếu thay dependency.
- [ ] Xác định quy trình ký, bảo vệ/back up/rotation key ngoài repo; `release.yml` hiện phát APK debug, không gọi là signed production. **Đạt:** APK release ký đúng được user test, xác minh log/rules/backup; nếu thiếu ký hoặc quyền build ghi BLOCKED, không yêu cầu gửi secret.

### T23 — Đo after và so sánh hiệu năng
**Vị trí:** theo [kịch bản đo](../optimization-report/03_benchmark_metrics_scenarios.md). **Phụ thuộc:** T01, T04, T17–T21 và user hỗ trợ thiết bị.
- [ ] Đo lại từng chỉ số và kịch bản T01 trên cùng điều kiện, lưu raw N/đơn vị/p50/p95 nếu phù hợp; so CPU, RAM, FPS, pin, traffic và latency, ghi cả trường hợp xấu hơn.
- [ ] Kiểm tra tính tương đương điều kiện và ghi kết quả/thiếu dữ liệu. **Đạt:** mọi chỉ số có số liệu kèm evidence hoặc NOT RUN/BLOCKED và lý do.

### T24 — Regression bảo mật và UAT
**Vị trí:** theo [kịch bản đo](../optimization-report/03_benchmark_metrics_scenarios.md). **Phụ thuộc:** T02–T17, T22 và user/Backend test được phép.
- [ ] Chạy ma trận security: cookie migration/backup/key mất, refresh/logout race, C04, HTTPS/WSS sai CA/pin, Intent/URI, input/ảnh giả, role/deadline/answer và privacy; chạy UAT thiết bị.
- [ ] Ghi từng case PASS/FAIL/NOT RUN/BLOCKED kèm evidence và báo thiếu sót. **Đạt:** ma trận và UAT có kết quả minh bạch, không biến build thành chứng cứ bảo mật.

### T25 — Báo cáo và minh chứng hai môn
**Vị trí:** báo cáo trong `docs/optimization-report/` và vị trí báo cáo bảo mật theo thống nhất; **phụ thuộc:** T23–T24.
- [ ] Lập bảng truy vết yêu cầu → task/commit/test/kết quả/Backend/giới hạn, phân biệt việc nhóm Backend đã làm và Android đã bổ sung. Trình bày chuẩn bị sinh trắc/MFA và phần xác thực môi trường theo trạng thái thực tế.
- [ ] Phân tích trade-off bảo mật/hiệu năng, so raw before/after và mô tả hạn chế, lỗ hổng chưa xử lý, BUG Backend; chuẩn bị demo và video không chứa PII. **Đạt:** hai báo cáo bao phủ tiêu chí đã đăng ký; không có số liệu hoặc PASS chưa kiểm chứng.

## Ma trận bao phủ tiêu chí

| Tiêu chí đã đăng ký | Task / phụ thuộc |
|---|---|
| Keystore, refresh/phiên/logout; chuẩn bị sinh trắc/MFA | T05–T09, B1, T24–T25 |
| HTTPS, WSS, pinning, Dev/Production | T10–T11, B2, T24 |
| Permissions, Intent, Deep Link, chia sẻ file | T12–T13, B3, T24 |
| Input, ownership, kích thước, MIME, chữ ký ảnh trước upload | T03, T14–T15, B4, T24; quyền sở hữu tin cậy cần server |
| Anti-cheat, thời gian/quyền/đáp án, xác thực môi trường | T16–T17, B5, T24; thiếu backend phải báo |
| R8, ký, log, backup | T02, T08, T22, T24 |
| Startup, UI, latency, RAM, pin, traffic/API/Socket | T01, T04, T17–T21, T23 |
| CPU, RAM, FPS, pin, traffic, latency và security before/after | T01, T23–T25 |
| Cân bằng bảo mật/hiệu năng, hạn chế và báo cáo | T07, T11, T17–T19, T23–T25 |

## Review focus trước khi thực thi

- Phiên cũ đến sau login/logout, cookie cùng tên khác path → test T05–T06.
- Upgrade từ DB plaintext, mất khóa/restore → test T07–T08.
- TLS hợp lệ nhưng sai pin; upload host khác và WSS → test T10–T11.
- Intent explicit/path giả, URI grant quá rộng và ảnh giả bytes/vượt giới hạn → test T12–T15.
- ACK không chắc chắn, deadline/role/privacy và so sánh sai điều kiện → test T16–T17, T01/T23.

**Kết quả thực hiện:** chưa có; mọi task đều NOT RUN. Bổ sung evidence và trạng thái bên dưới task tương ứng khi thực sự triển khai; không ghi bí mật.
