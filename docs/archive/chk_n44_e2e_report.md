# N44 — Báo cáo kiểm thử tích hợp và backlog chuyển tiếp N46

## Kết luận

**Đã tổng hợp lượt test user ghi ngày 08/10/2026 lúc 22:00. N44 có kết quả PARTIAL và còn lỗi/gate mở; chưa chốt N45/M6.** Các luồng chính session/account switch, history, preview, profile/password và phần lớn Classic được user báo hoạt động ổn. Không gọi “đã quét hết checklist” là mọi case đã test đầy đủ hoặc đều PASS.

Theo quyết định mới của user: **lưu lỗi/thiếu evidence vào backlog, chuyển trọng tâm sang nghiên cứu giao diện N46**. Đây là ngoại lệ về thứ tự nghiên cứu, không phải feature freeze, release approval, miễn trừ blocker hoặc xác nhận các race đã sửa. Lượt này chỉ cập nhật tài liệu; không sửa code, thêm log, chạy test, checkout, commit hay push.

### Vấn đề nổi bật

- **N44-OFFLINE-BOOT — lỗi mới user quan sát:** mở app ngay khi không có mạng, Hoạt động và Hồ sơ treo loading; Home/Thư viện có retry. User mô tả chỉ hết khi khởi động app lại lúc có mạng. Tách khỏi A03 warm-session offline PASS. Chưa trace root cause.
- **N29/Practice — lỗi cũ còn quan sát:** pause/resume vẫn nhảy về câu đầu. Ghi nhận failure thực tế trong case D01 đang backend-blocked; chưa có deployment/fix xác minh để đóng gate.
- **B01 Host rank/progress — chưa kết luận là bug:** user thấy rank chỉ cập nhật khi tất cả đã trả lời. Rank/score có thể được publish ở pha Results; không đồng nhất với submit acceptance hoặc bộ đếm answer progress. Lobby, 4 types và final/Back PASS; phần submit/progress cần phân biệt trước khi quyết định sửa.
- **B02/A08:** ẩn leaderboard không chứng minh reveal/review đáp án bị chặn. B02 chưa có effective config và còn hiểu chưa rõ; A08 chưa chạy. Không đóng API privacy gate từ UI.

## Baseline và giới hạn evidence

| Mục | Evidence thực có / giới hạn |
| --- | --- |
| Nguồn chính | `N44_E2E_INTEGRATION_CHECKLIST.md` do user sửa trên nhánh docs; giữ nguyên ghi chú/tick, không chuẩn hóa thành PASS giả |
| Lượt user ghi | 08/10/2026 22:00, USER AN; timezone chưa ghi trong file (không tự gán timestamp đo được) |
| Android APK | DEBUG; user ghi “bản mới nhất sau khi vừa commit docs”, chưa có SHA/version APK. Commit docs không xác định APK đang cài |
| Main lúc review | `35c186b chore: keep project documentation on docs branch`; trước đó `3ea792a` source N44, `ab6621b` N43. Không dùng main HEAD làm SHA APK đã test |
| Build | User báo build xanh; chưa có log/run id mới để xác nhận full build/lint/CI/regression |
| Thiết bị | H: Samsung Galaxy A21s; P1: Vsmart Live 4; OS chưa ghi. P2 dùng frontend web vì emulator hỏng/thiếu máy; không phải ba Android client cùng APK |
| Backend URL cấu hình | Đọc `git show main:core/network/build.gradle.kts`: REST `https://api.myquizz.dpdns.org/v1/`, Socket `https://api.myquizz.dpdns.org`. Đây là cấu hình source, chưa xác nhận runtime APK/P2 dùng endpoint nào |
| Backend deployment | UNKNOWN. Tên môi trường prod/staging và revision đang chạy chưa xác nhận; source audit cũ `7c103c8` không phải deployment evidence. Không gọi API gây mutation để đoán |
| Tài liệu media/network | Không có ảnh/video/log; loại mạng/battery restriction chưa rõ |
| Alias | A/B/H/P1 là nhãn giả để phân biệt account, ví dụ User-A/User-B/Host/Player-1; không cần email, token hay mật khẩu |

B/P2 trên web có ích cho smoke room đa client, nhưng không chứng minh Android guest gameplay/reconnect của P2. A06 là ghi nhận guest history Android có sẵn, không tự nối nó với guest web cùng identity.

## Matrix đánh giá — giữ nguyên nguồn user

Đơn vị: một case; mỗi case có thể nhiều nhánh. PASS dưới đây là **user-reported**, không phải agent chạy lại. PARTIAL/NEEDS EVIDENCE phản ánh phạm vi chưa chứng minh, không tự biến thành lỗi production. Kết quả lịch sử C06 được giữ riêng. Không tính tỷ lệ PASS/release readiness khi baseline và trigger không đồng nhất.

| Case | Đánh giá báo cáo | Ghi chú |
| --- | --- | --- |
| A01 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A02 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A03 | PASS (user-reported) | PASS nhánh mất mạng sau khi đã load; cold-start offline có lỗi riêng N44-OFFLINE-BOOT. |
| A04 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A05 | PASS (user-reported) | User báo PASS, có fixture nhiều trang; không thấy spinner không phải bug và cũng không tự chứng minh cursor. |
| A06 | PARTIAL | Thấy history guest cũ lúc logout; chưa ghi bước đóng/mở app cùng identity. |
| A07 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A08 | NOT RUN | Chưa kiểm answer review bị chặn; leaderboard hide không thay case này. |
| A09 | NOT RUN | Không tạo được request summary/answers in-flight khi đổi phiên. |
| A10 | PARTIAL | User tick PASS nhưng chưa ghi từng bề mặt và nhánh ảnh hỏng; chưa chốt toàn case. |
| A11 | PARTIAL | Chức năng và history PASS; play count NOT RUN. |
| A12 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A13 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A14 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| A15 | PARTIAL | Google-only PASS; local-linked chưa chạy/chưa rõ fixture. |
| A16 | DEFERRED | User không muốn vô hiệu hóa tài khoản; tôn trọng, không yêu cầu dùng account chính. |
| B01 | NEEDS CLARIFICATION | Lobby/4 types/final & Back PASS; submit/progress/rank còn dấu ?. Không tự gán root cause. |
| B02 | NEEDS EVIDENCE | Tick PASS nhưng effective config chưa ghi; reveal/review còn chưa rõ; A08 chưa chạy. |
| B03 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| B04 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| B05 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| B06 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| B07 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| C01 | BLOCKED | Chưa có deployment revision/fix presence để retest đóng gate. |
| C02 | NOT RUN | Chưa tạo/quan sát được ACK uncertainty và hai nhánh. |
| C03 | PASS (user-reported) | User ghi exhaustion/auto/manual PASS dù checkbox trống; ưu tiên nội dung kết quả, giữ nguyên raw. |
| C04 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| C05 | NOT RUN | Chưa tạo fatal/server disconnect xác định được. |
| C06 | HISTORICAL/PARTIAL | ADB/result recovery PASS từ lượt trước, không phải run này; Review NOT RUN. |
| C07 | NOT RUN | Chưa có controlled unknown outcome/cleanup fixture. |
| D01 | BLOCKED + OBSERVED FAIL | User thử pause/resume Practice, vẫn nhảy về câu đầu; gắn gate N29, chưa re-audit nguyên nhân. |
| D02 | PASS (user-reported) | Theo kết quả user ghi; không phải agent chạy lại. |
| D03 | PARTIAL/BLOCKED | Player happy path PASS; Host live delta/lives/streak vẫn BLOCKED. |
| D04 | NOT RUN | Marathon chưa chạy. |

### Ghi chú làm rõ

- A06: thấy history guest cũ khi logout là evidence tốt, nhưng chưa ghi đóng/mở app theo bước của case; không phủ nhận phần đã thấy.
- A10: user tick PASS nhưng các nhánh Detail/Preview, Host/Player, ảnh hỏng để trống. Chưa xác định ảnh hỏng đã chạy hay thiếu fixture.
- A11: chức năng/history PASS; counter NOT RUN nên không tick PASS toàn case trong đánh giá báo cáo.
- A15: **local-linked** = account vốn đăng ký bằng email/password, sau đó liên kết Google theo cơ chế backend hỗ trợ; không phải Google-only và không mặc định cứ login Google cùng email là đã link. Không có fixture/hướng dẫn linking xác minh thì bỏ nhánh này, không tự tạo/link bằng API.
- A16: user không muốn deactivate; ghi DEFERRED theo lựa chọn, không yêu cầu dùng account chính. Hủy/sai password/success cleanup chưa có E2E mới; unit evidence giữ riêng.
- C03: checkbox trống nhưng user ghi rõ exhaustion/auto/manual PASS; báo cáo dùng nội dung kết quả, không sửa raw để che bất nhất.
- C06: user nói đã test trước bằng ADB. Giữ evidence lịch sử result recovery, không tính là chạy lại trong lượt này; Review NOT RUN.

## Bug/risk ledger và ưu tiên

Severity dưới đây là đề xuất triage, không phải root cause đã được audit. Hoãn việc sửa không đồng nghĩa chấp nhận phát hành.

| ID / gate | Trạng thái và phạm vi | Ưu tiên / bước khi quay lại |
| --- | --- | --- |
| N44-OFFLINE-BOOT | OBSERVED FAIL — cold-start offline: Hoạt động/Hồ sơ treo loading; nghi đường session bootstrap/loading/retry, chưa chứng minh tầng gây lỗi | High, cần xử lý trước release: xác nhận account/guest; kill mở offline → bật mạng usable → quan sát tab/retry; trace session Unknown/loading và completion/cancellation. Nếu sửa vẫn VM → Use case → Repository, không cho VM gọi API/repository hay forced-login workaround |
| N44-HOST-PROGRESS (B01) | NEEDS CLARIFICATION — rank chỉ đổi lúc tất cả trả lời; chưa biết answer count có cập nhật từng Player không | Chưa gán severity: tách answer acceptance/progress với rank/score ở phase Results; đọc contract/reducer đúng source trước sửa |
| N29 / D01 | BACKEND GATE OPEN + user thấy Practice pause/resume nhảy về câu đầu | High, pending backend revision/retest progress riêng từng Player; không remap index/delay/rejoin che snapshot lỗi |
| N25 presence / C01 | BLOCKED, chưa xác minh fix/deployment | High; retest old/new socket, Host roster/pending answers và no-double-score khi có bản fix |
| N28.5 / D03 | Player happy path PASS; Host live delta/lives/streak/presence BLOCKED | Gate còn mở, happy path không thay payload/event evidence |
| N30 | Player renewal/progress/shuffled sync/order còn trong hồ sơ blocker | Gate còn mở; không join mới thay renewal |
| N20.6 | Contract active-game resume còn mở | Giữ tách khỏi C06 recovery kết quả trận đã kết thúc |
| POLICY-RESULT / BUG-16 | B03 UI PASS; API public results privacy chưa xác minh | Security/release gate; cần backend revision + integration test policy never, không chỉ nhìn UI |
| N44-GOOGLE-SESSION / N44-COOKIE | DEFERRED theo user; runtime không tái hiện, hai reproducer opt-in trước đó FAIL, production chưa fix | Rủi ro credential/session còn mở; không bật lại log hoặc tuyên bố skip test là fix |
| N44-TOKEN / N44-ARCH | Legacy token route/SavedStateHandle và VM gọi Repository ngoài scope trước vẫn cần audit | Security/architecture backlog riêng; UI PASS không chứng minh toàn app clean |
| Coverage/baseline gaps | A08/A09, A10 một số nhánh, A11 counter, A15 linked, A16, C02/C05/C07, D04; C06 lịch sử; CI/deployment/APK chưa đủ | Không coi NOT RUN/PARTIAL/BLOCKED là PASS. Không buộc user săn race hoặc thực hiện thao tác phá hủy |

## Evidence tự động trước đó — không trộn với lượt manual

- N43: 25 test bổ sung đã đối chiếu XML local PASS.
- N44: 16 regression bổ sung đã đối chiếu XML local PASS; các suite cũ trong phạm vi đã đọc cũng PASS.
- Cookie reproducer: 2/2 FAIL đúng invariant trong opt-in diagnostic trước đó, chưa có transport fix. Không đưa vào danh sách regression PASS.
- Không chạy Gradle/CI mới trong lượt báo cáo; lời user “build xanh” chưa thay thế full evidence/run id.

## Quyết định chuyển sang N46

**User yêu cầu ghi nhận N44 rồi chuyển thẳng sang nghiên cứu nối giao diện app.** N44 giữ PARTIAL/backlog mở; N45/M6 chưa chốt. Được mở N46 ở mức research/design theo quyết định này; không tự hiểu là duyệt triển khai toàn bộ polish hoặc miễn trừ release gate.

Kế hoạch phiên tiếp theo:
1. Audit read-only giao diện hiện tại và `core:ui`/theme/components, các màn/route trên main đúng revision; rà nhu cầu nối/đồng bộ UI mà không đổi contract nghiệp vụ.
2. Lập inventory theo flow: auth → Home/Library/Activity/Profile; quiz detail/editor/preview; create/join/lobby; Host/Player/result/review/history. Ghi UI gaps, loading/error/empty/offline/retry và component tái dùng.
3. Đề xuất hướng visual và design tokens, typography/spacing/color; light/dark, font scale/contrast/semantics/touch target; animation vừa đủ. Nợ sheet dark mode/Splash padding nằm trong scope N46.
4. Chọn nhóm màn ưu tiên và bản mẫu/phương án để user duyệt trước code. Không thêm feature phase 2 hoặc workaround backend.
5. Nếu triển khai sau duyệt: stateless Screen/Content + UiState/Intent/Effect; shared UI ở core:ui không chứa nghiệp vụ/data; ViewModel chỉ qua Use case; tránh dependency feature → feature. Bug cold-start là task chức năng riêng, không chỉ phủ một retry widget rồi gọi đã fix.

**Release chưa được phê duyệt.** Trước N45/M6/RC/ship phải quay lại quyết định rõ cho lỗi offline bootstrap, privacy/session/realtime blockers và evidence còn thiếu. Không bắt user test lại ngay trong lượt báo cáo này.

## Nguồn đối chiếu

- `N44_E2E_INTEGRATION_CHECKLIST.md`: kết quả gốc user, đọc sau lượt test.
- `N44_INTEGRATION.md`, `AGENTS.md`, roadmap: source/regression/race/deferred ledger và Clean Architecture.
- `git log -3 main` và `git show main:core/network/build.gradle.kts`: cấu hình/source local chỉ đọc; không chứng minh APK/deployment.

Báo cáo không thay đổi các ghi chú test nguyên bản; không đóng các backend issue hoặc phát hành từ kết quả smoke.
