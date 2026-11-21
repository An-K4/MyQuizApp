# Prompt bàn giao — tạo Code_Walkthrough.md từ docs và source thật

> Dán nguyên phần bên dưới vào phiên agent mới. Đây là nhiệm vụ documentation-only, không phải yêu cầu implement/refactor. Checkpoint khi soạn: Android local main `346230b`, docs base `f4d829e` trước lượt cập nhật này. Phải kiểm tra lại refs khi bắt đầu. Chưa có Code_Walkthrough.md trong danh sách Markdown đã kiểm tra ở checkpoint này.

---

Bạn tiếp nhận dự án **MyQuizApp** để tạo **một file `Code_Walkthrough.md` ở root, trên nhánh `docs`**, bằng tiếng Việt, giúp người mới nhanh chóng hiểu codebase và biết lần theo/sửa đúng chỗ. Không chỉ liệt kê thư mục; phải giải thích **tính năng → luồng file → quan hệ gọi/observe/wiring → hàm/block logic chính → tests** bằng source đã đọc. Không cần hỏi lại các quyết định đã chốt dưới đây; chỉ hỏi khi access/branch/scope thực sự chặn việc.

## 1. Quyền hạn và môi trường

- Repo Windows: `E:\Adr_Projects\MyQuizApp`, kết nối Aki MCP Server from local Shell & FileSystem. Đây là git filesystem, không phải trang Notion.
- Bắt đầu bằng git status/branch/log và refs main/docs. `main` chỉ source/config/tests, `docs` dành tài liệu; user tự commit/checkout. **Không tự commit, push, checkout, merge, cherry-pick, reset hoặc ghi đè thay đổi của user.**
- Nếu đang docs, đọc source bằng `git show main:<path>` hoặc tree/ref tooling có sẵn; không kết luận source mất chỉ vì working tree docs không có Kotlin. Nếu tool không thể đọc ref, báo giới hạn và đề nghị user checkout cần thiết; không tự chuyển nhánh. Khi ghi file phải về docs theo user thao tác.
- Chỉ tạo/cập nhật `Code_Walkthrough.md`; có thể thêm một link onboarding nhỏ vào tài liệu index phù hợp nếu cần và đã đọc nội dung. Không sửa Kotlin/Gradle/TypeScript, không fix bug nhân tiện, không sửa backend, không đổi trạng thái các gate.
- Backend mirror `server/myquizz` có thể là repo/ref riêng: kiểm tra revision nếu đọc. Local ref không chứng minh upstream/deployed revision. Không fetch hay đổi runtime/config mà chưa có yêu cầu.
- Không đọc/chép `.env`, keystore, cookie, socket token hoặc credentials. Ví dụ dùng placeholder, không đưa dữ liệu người chơi thật vào docs. Không cài package/build backend để viết walkthrough.
- Tool shell có allowlist read-only; không giả vờ đã build/test/render. Dùng batch reads/search có mục tiêu, tránh đổ generated/build/node_modules vào ngữ cảnh.

## 2. Đọc hiểu ngữ cảnh trước khi viết

1. Đọc `AGENTS.md` đầy đủ để nắm quy ước, architecture và traps.
2. Đọc checkpoint hiện hành `N46_UI_PROGRESS.md` (hiện mục 5e), `N46_HANDOFF_PROMPT.md`, roadmap `myquizz-review-backend-ke-hoach-50-ngay.md`; phân biệt lịch sử, trạng thái source và evidence test.
3. Đọc `project_structure.md`, `quiz-app-android-design-document-v2.md`; đây là bản đồ/tham khảo, có đoạn minh họa hoặc cũ, **không phải inventory/contract tuyệt đối**.
4. Inventory toàn bộ tài liệu Markdown có người viết ở root/knowledgement/backend README và các tài liệu backend liên quan; đọc nội dung, không chỉ tên/mục lục. Nhóm theo tính năng để đọc sâu đúng chỗ. Đặc biệt N24.5, N43, N44, BACKEND_BUG_REPORT, N25 checklist và các knowledge notes session/socket/history/editor. Không dùng log cũ để chốt PASS hiện tại.
5. Sau đó đối chiếu toàn bộ module source/config/tests có liên quan của Android main: settings/build wiring, app/navigation/DI, core layers, từng feature và tests. Đọc backend emit/service/schema thực tế khi cần giải thích một contract, không tin chỉ route/controller hoặc design doc.
6. Tạo bảng coverage nội bộ doc → feature → source đã đọc, xử lý từng nhóm để không bỏ sót. Nếu thiếu access/source, ghi rõ mục chưa đối chiếu; không bịa để hoàn thành vẻ ngoài.

**Đầu ra không phải nhật ký quá trình đọc.** Tổng hợp thành tài liệu tra cứu có chủ đích, ngắn ở đường vào và sâu ở các luồng quan trọng; không paste toàn bộ docs hoặc cây thư mục dài.

## 3. Bối cảnh hiện tại cần giữ đúng (xác minh lại với main)

- Classic, chung cuộc Player/Host và Chơi thử local đã polish. Sau đó Player self-paced `97d3da1`, Host monitoring `79cca21`, fallback ảnh/Discovery card `1e89275`, online review `76b8585`, single-choice/review metadata `346230b`.
- Solo, Survival, Marathon, Practice là online/self-paced; **Chơi thử local là feature:quiz-preview, không phải Practice hoặc Solo online**. Classic có question loop; Host 4 mode self-paced dùng dashboard nhánh Content trong cùng route/VM, không mặc định 4 màn độc lập.
- Câu một lựa chọn: tap-to-submit ở Classic/self-paced. Multi-select/text explicit Send. **Gửi và chuyển câu là hai quyết định khác nhau**: ACK acceptance/feedback và auto/manual next vẫn server-authoritative; uncertainty giữ khóa + sync, không auto resend.
- Private Classic result/current streak khác max streak cuối trận/lịch sử. Privacy/reveal phải lọc trước UI state, không chỉ hide Composable.
- Final socket-first; REST results recovery-only. Player final không có room per-question stats; Host có aggregate counts. Review online cá nhân mở inline ở final, có REST token/gate riêng; không đồng nhất với history answer sheet hoặc local preview review.
- Review API hiện không có question_type. `GameViewModel` nhớ type thật từ QuestionStarted/snapshot trong RAM → PlayerGameSessionUseCase/StoredGameResult.reviewQuestionTypes → FinalResult state → mapper/card. Không suy single/multi từ số key; unknown giữ caption, cold-start/token limitations cần giải thích.
- Fallback ảnh là Compose Box trắng/logo app giữa + AsyncImage; Avatar dùng person fallback; QuestionImage không URL thì ẩn. Core ComponentColors internal, không import qua feature.
- ViewModel → Use case → repository interface; impl/DTO dừng ở data. Stateful Screen / stateless Content; không feature→feature. Tài liệu quy ước khác debt source thực tế: nếu có lệch, mô tả hiện trạng và đánh dấu debt, không bịa rằng mọi code đều chuẩn.
- N44 PARTIAL/N46 WIP, backend/privacy/session/token/deployment, BUG-13, max streak và release gates vẫn mở. User báo UI ổn không phải full E2E/CI chứng minh. Không “đóng hết luồng/release-ready”.

## 4. Cấu trúc bắt buộc của Code_Walkthrough.md

### A. Đường đọc nhanh 10–15 phút
- Revision Android/docs/backend đã kiểm tra, phạm vi và phần chưa verify.
- Mục lục anchor rõ; bảng “muốn hiểu/sửa X → đọc file/hàm Y trước”.
- 8–12 entry points thực tế theo thứ tự, từ app boot/navigation/session tới một luồng REST và một luồng socket. Giải thích ngắn tại sao đọc file đó.
- Sơ đồ module/layer và mũi tên đúng nghĩa (dependency khác runtime call/event), legend cho cách rút gọn path.

### B. Danh mục toàn bộ tính năng hiện có
Bảng: tính năng/nhóm → entry route/screen → module sở hữu → API/socket/local → nơi đọc chi tiết. Chỉ ghi tính năng đã thấy trong code; planned/placeholder/debt tách riêng. Phủ ít nhất các nhóm nếu tồn tại:
- Boot, browse-first, bottom tabs, auth gates, session/guest identity, login/register/forgot/OTP/reset/Google/logout.
- Home, Search, Discover/filter/Paging, Library, Quiz Detail, Create/Edit/Delete quiz, media upload/cache.
- Create room/mode config, join guest/account, Host/Player Lobby, start/countdown.
- Classic Player/Host; Solo/Survival/Marathon/Practice Player + Host monitoring; các khác biệt config/phase/timer/score/lives/next/terminal.
- Final Player/Host, standings/podium, personal online review, REST recovery; lịch sử/chi tiết/answer sheet.
- Chơi thử local từ câu hỏi đến summary/review/restart/exit.
- Profile/edit/avatar/security/activity và cross-cutting offline/errors/persistence/DI.

### C. Walkthrough cho từng tính năng/nhóm
Mỗi nhóm có mục độc lập theo cùng khuôn:
1. **Người dùng vào từ đâu, đi qua màn nào, kết thúc ở đâu.**
2. **Chuỗi file thật**, ví dụ `route wiring → Screen/Content → Intent → ViewModel handler → UseCase → repository interface → impl → API/socket/DAO/mapper → state/effect → UI`. Ghi rõ phần nào là callback/nav wiring, phần nào là call hoặc Flow collect; không ép mọi feature có đủ tất cả tầng.
3. **Bảng file | symbol/hàm/block | trách nhiệm | nhận từ/gọi tới**. Full repo-relative path ít nhất lần đầu; basename chỉ dùng khi không mơ hồ.
4. **Logic chính nằm ở block nào:** tên symbol, nhánh when/event/guard liên quan, điều kiện và lý do; 1–3 code excerpt ngắn nguyên văn (khoảng 5–20 dòng) cho phần khó, kèm path + symbol và revision. Không paste nguyên ViewModel. Line range tùy chọn ở SHA đã ghi, không dùng số dòng thiếu revision thay cho symbol.
5. **Data/state:** nguồn authoritative, model/DTO/mapper, state transition, lifecycle/cancel/cleanup, dependency injection binding và data handoff giữa màn.
6. **Failure/edge quan trọng** thực sự có trong code: loading/error/retry, null/0/missing, config visibility, reconnect/pause/process-death, stale/duplicate/out-of-order event. Phân biệt đang xử lý, thiếu backend và chưa verify.
7. **Tests liên quan:** exact test file + tên case đại diện và invariant bảo vệ; hướng dẫn test thủ công ngắn có dấu hiệu đạt. Test tồn tại không có nghĩa PASS ở revision hiện tại.
8. **Nếu cần sửa X thì bắt đầu ở đâu**, lớp nào không nên sửa và callers/tests cần rà.

Với phần socket, vẽ riêng chiều client→server ACK và server→client event/snapshot. Với 4 self-paced mode, viết một luồng nền chung rồi bảng delta mode, không copy bốn chương gần giống. Với review, phân biệt ba nguồn online final/history/local preview và cả Host aggregate stats.

### D. Cross-cutting và bảng tra cứu
- Auth cookie/session/guest/socket token: nơi sinh/lưu/đọc/xóa, giới hạn process death và dữ liệu tuyệt đối không log.
- REST envelope/error mapping, cursor/Paging, cache fallback không offline-first; DI boundary.
- Server time vs local display clock, authoritative scoring/reveal, concurrency/cancel/generation/stale result guards ở nơi có thực.
- Shared UI/theme/answer cards/QuestionImage/RemoteImage/Avatar: callers điển hình và cách không import internal token xuyên module.
- Bảng map API/event→mapper/model→consumer và repository interface→impl→binding, ưu tiên cái thật sự cần tra.
- Known pitfalls/debt/blockers ngắn có link tới canonical docs, không chép lại toàn bug report.
- Checklist khi thêm/sửa feature và quy tắc bảo trì file khi source đổi.

## 5. Tiêu chuẩn chất lượng / hoàn tất

- Ưu tiên **hiện trạng code đang tồn tại** hơn kiến trúc lý tưởng. Mỗi claim về luồng/hàm/data phải truy ra path/symbol đã đọc; không invent file, endpoint/event, “max streak đã fix” hoặc behavior backend.
- Mọi path/link nội bộ click được bằng relative Markdown; cross-ref giữa các mục thay vì lặp. Mermaid có thể dùng kèm tóm tắt chữ; không phụ thuộc render mới đọc được.
- Có hai tầng: quick map đủ dùng ngay; chi tiết đủ lần theo code/debug. Không phải dump mỗi file một dòng, không viết README quảng cáo hoặc học thuyết Clean Architecture chung chung.
- Tự kiểm tra path/symbol/link/coverage, đối chiếu một luồng REST và một luồng realtime end-to-end thực tế; kiểm tra bảng mode và privacy/config semantics. Ghi cụ thể phần chưa đọc được, không tuyên bố “đọc hết” nếu còn thiếu.
- Giữ một file Code_Walkthrough.md, không tạo loạt file con hoặc thay toàn bộ docs canonical. Không thực thi thêm workflow bằng agent con nếu user chưa yêu cầu.
- Trước ghi load file nếu đã có; không overwrite file mới của user. Sau ghi đọc lại và git diff --check/status; chỉ changes docs đúng scope. Không chạy/claim test hay CI nếu không có artifact.

## 6. Handoff cuối

Báo user ngắn: đã tạo file ở đâu, thứ tự đọc nhanh, coverage/module nào, giới hạn chưa verify và các mâu thuẫn docs↔code phát hiện. Đề xuất commit docs (không tự commit). Không đề xuất sửa source/backlog trong cùng nhiệm vụ. Nếu mọi thứ đủ evidence, làm một mạch tới tài liệu hoàn chỉnh, không dừng ở dàn ý.
