# KẾ HOẠCH TÁI CẤU TRÚC NHÁNH DOCS & QUY HOẠCH THƯ MỤC `docs/` TRƯỚC KHI MERGE VÀO MAIN

> **Vị trí file:** `E:\Adr_Projects\MyQuizApp\docs_restructure_plan.md` (File untracked tại root, nằm cùng cấp với `my_notes.md`).
> **Mục tiêu:** Xây dựng kế hoạch chi tiết, tiêu chuẩn hóa và tái cấu trúc toàn diện hệ thống tài liệu trên nhánh `docs`, gom tất cả vào thư mục con `docs/` ở gốc repository, chuẩn bị merge PR vào nhánh `main` và thiết lập chốt chặn chất lượng (Branch Protection Ruleset).

---

## I. MỤC TIÊU & NGUYÊN TẮC TÁI CẤU TRÚC

Theo yêu cầu của chủ dự án, hệ thống tài liệu sau khi tái cấu trúc phải đáp ứng 4 tiêu chí cốt lõi:

1. **Quy ước đặt tên đồng nhất (Uniform Naming Convention):**
   - Đặt tên theo thư mục chức năng, tiền tố có phân cấp logic, sử dụng chữ thường kết hợp dấu gạch nối (`kebab-case`) hoặc tiền tố số thứ tự (`01_`, `02_`) cho các tài liệu đọc theo trình tự.
   - Không còn tình trạng file đặt tên tự do, lẫn lộn giữa HOA/thường, tiếng Anh/tiếng Việt không nhất quán ngay tại root.
2. **`AGENTS.md` tinh gọn — Cổng điều hướng trung tâm (Navigation Hub / Index):**
   - Rút gọn file `AGENTS.md` xuống còn khoảng 80–120 dòng (thay vì 379 dòng lẫn lộn hiện tại).
   - Đóng vai trò là "Sơ đồ chỉ dẫn" (Sitemap / Entry Point): Chỉ giữ lại các quy tắc cốt lõi bất di bất dịch, sau đó trỏ link trực tiếp tới các file chuyên đề chuyên sâu trong `docs/`.
   - Tách các nội dung chi tiết về quy ước code, xử lý lỗi, checklist... sang các file riêng biệt.
3. **Bảo tồn 100% tri thức trong `knowledgement/`:**
   - Tuyệt đối **không được cắt bỏ bất kỳ kiến thức, bài học xương máu hay kinh nghiệm kỹ thuật nào**.
   - Có thể biên tập lại câu từ cho súc tích, chuyên nghiệp hơn, chuẩn hóa mục lục và gom nhóm theo chủ đề/mốc phát triển để người đọc dễ tra cứu.
4. **Phân định minh bạch: Tài liệu Lịch sử (Archive) vs Trạng thái Sống (Living Truth):**
   - **Tài liệu lịch sử (Archive):** Các checklist, báo cáo E2E của các đợt test cũ (N24.5, N25, N44...) giữ nguyên giá trị bằng chứng tại thời điểm ghi nhận, được gom vào `docs/archive/`.
   - **Tài liệu trạng thái sống (Living Truth):** Các tài liệu kiến trúc, mô tả API, cấu trúc dự án và lộ trình tiếp theo (N46–N50) phải được **sửa lại cho khớp 100% với hiện trạng mã nguồn thực tế** trên Android `main` (`35d6bfd` / `346230b`) và Backend (`37038f6`).

---

## II. THIẾT KẾ CÂY THƯ MỤC `docs/` MỚI

Toàn bộ tài liệu từ nhánh `docs` sẽ được chuyển vào thư mục `docs/` ở gốc repository với cấu trúc phân nhóm khoa học như sau:

```
docs/
├── AGENTS.md                                # Cổng vào điều hướng trung tâm, quy tắc cốt lõi (rút gọn)
├── DOCS_GUIDELINES.md                       # Bộ luật cập nhật và duy trì tài liệu (Governance / Style Guide)
├── README.md                                # Tổng quan bộ tài liệu và hướng dẫn tra cứu
│
├── architecture/                            # Kiến trúc hệ thống & Thiết kế kỹ thuật (LIVING TRUTH)
│   ├── 01_system_overview.md                # Kiến trúc tổng thể Android + Backend (Clean Architecture, MVI, Multi-module)
│   ├── 02_project_structure.md              # Trách nhiệm chi tiết từng module Android (cập nhật từ project_structure.md)
│   ├── 03_network_and_contracts.md          # REST API, Socket.IO contracts, Session, CookieJar & Authenticator
│   ├── 04_ui_design_system.md               # Palette màu, typography, Compose stateless components (từ design-doc-v2)
│   └── 05_coding_conventions.md             # Quy ước code, DIP, ViewModel-Usecase-Repo, Error mapping
│
├── roadmap/                                 # Lộ trình & Quản lý phát triển (LIVING TRUTH)
│   ├── 01_50_day_roadmap.md                 # Kế hoạch tổng thể 50 ngày (chắt lọc & cập nhật từ file ke-hoach-50-ngay)
│   ├── 02_n46_ui_polish_progress.md         # Tiến độ chi tiết mốc N46 (checkpoint 346230b)
│   └── 03_n47_n50_release_gates.md          # Kế hoạch còn lại: N47 Regression, N48 Hardening, N49 UAT, N50 Ship
│
├── backend-audit/                           # Báo cáo Audit & Khảo sát Backend (LIVING TRUTH)
│   ├── 01_backend_17_bugs_status.md         # Báo cáo đối chiếu 17 bugs (cập nhật 7 bug đã fix, 9 bug còn mở)
│   ├── 02_audit_handoff_summary.md          # Tóm tắt 5 đợt audit backend (B01–B41, C01–C18, Magic Bytes, Lua script)
│   └── 03_backend_benchmarks.md             # Baseline hiệu năng đo đạc từ Docker/Redis/Postgres
│
├── walkthrough/                             # Tài liệu Onboarding & Đọc hiểu Codebase
│   └── code_walkthrough_12_entrypoints.md   # Hướng dẫn chi tiết 12 điểm vào của app (từ Code_Walkthrough.md)
│
├── optimization-report/                     # Đề tài môn học "Tối ưu phần mềm ứng dụng"
│   ├── 01_optimization_plan_4_sprints.md    # Kế hoạch 4 Sprint (100% Android, tối ưu Startup, UI, RAM, Pin, Bảo mật)
│   ├── 02_performance_security_tradeoffs.md # 4 Case studies sâu về cân bằng Hiệu năng & Bảo mật
│   └── 03_benchmark_metrics_scenarios.md    # Kịch bản đo lường Before/After (am start, dumpsys, profiler)
│
├── knowledge/                               # Tri thức & Bài học xương máu (BẢO TOÀN 100% NỘI DUNG)
│   ├── setup_guide_vn.md                    # Hướng dẫn dựng môi trường Android Studio & Docker local
│   ├── kn_w01_architecture_baseline.md      # Bài học Tuần 1: Kiến trúc, multi-module, DIP (N1–N5)
│   ├── kn_w02_auth_and_session.md           # Bài học Tuần 2: CookieJar, RoomCookieStore, TokenAuthenticator (N6–N10)
│   ├── kn_w03_home_and_quiz_reading.md      # Bài học Tuần 3: Browse-First, QuizCacheStore, Paging 3 (N11–N15)
│   ├── kn_w04_socket_and_lobby.md           # Bài học Tuần 4: Cold Flow Socket.IO, Presence, Create Room (N16–N20)
│   ├── kn_w05_host_paced_classic.md         # Bài học Tuần 5: Gameplay Classic, Countdown, Personal Results (N21–N25)
│   ├── kn_w06_self_paced_modes.md           # Bài học Tuần 6: Solo, Practice, Survival, Marathon (N26–N30)
│   └── kn_special_ui_refactor.md            # Bài học chuyên đề: Refactor Stateful/Stateless trong Jetpack Compose
│
└── archive/                                 # Lưu trữ hồ sơ & Checklist lịch sử (IMMUTABLE EVIDENCE)
    ├── chk_n24_5_classic_results_plan.md    # Kế hoạch N24.5 Classic Personal Results
    ├── chk_n25_e2e_classic_checklist.md     # Checklist kiểm thử E2E N25 Classic
    ├── chk_n43_implementation.md            # Nhật ký triển khai N43
    ├── chk_n44_integration_scope.md         # Phạm vi tích hợp N44
    ├── chk_n44_e2e_checklist.md             # Checklist kiểm thử N44 E2E
    ├── chk_n44_e2e_report.md                # Báo cáo kết quả N44 E2E (PARTIAL + lỗi OFFLINE-BOOT)
    ├── chk_refactor_checklist.md            # Checklist refactor giai đoạn cũ
    └── docs_restructure_plan.md             # Kế hoạch chi tiết đợt tái cấu trúc (chuyển vào đây sau khi xong)
```

---

## III. MA TRẬN ÁNH XẠ FILE CŨ ➜ FILE MỚI & HÀNH ĐỘNG CỤ THỂ

| File gốc trên nhánh `docs` | Vị trí & Tên mới trong `docs/` | Loại tài liệu | Hành động xử lý cụ thể |
| :--- | :--- | :---: | :--- |
| `AGENTS.md` | `docs/AGENTS.md` | Living Hub | **Rút gọn thành Index/Sitemap:** Giữ luật cốt lõi, trỏ link tới các file phân mục. |
| `project_structure.md` | `docs/architecture/02_project_structure.md` | Living Truth | Cập nhật chính xác cấu trúc module hiện tại trên `main` (`35d6bfd`). |
| `quiz-app-android-design-document-v2.md` | `docs/architecture/04_ui_design_system.md` & `03_network_and_contracts.md` | Living Truth | Tách phần UI Component/Theme và phần Network/Contract; loại bỏ các giả định cũ không khớp code thật. |
| `myquizz-review-backend-ke-hoach-50-ngay.md` | `docs/roadmap/01_50_day_roadmap.md` & `03_n47_n50_release_gates.md` | Living Truth | Tinh gọn: Tóm tắt N1–N45, giữ chi tiết N46 và hoàn thiện kế hoạch N47–N50. |
| `N46_UI_PROGRESS.md` | `docs/roadmap/02_n46_ui_polish_progress.md` | Living Truth | Chuẩn hóa mốc hiện tại `346230b` (mục 5e). |
| `BACKEND_BUG_REPORT.md` | `docs/backend-audit/01_backend_17_bugs_status.md` | Living Truth | Cập nhật tình trạng thực tế của 17 bug sau audit: 7 bug đã fix, 9 bug chưa fix. |
| `Code_Walkthrough.md` | `docs/walkthrough/code_walkthrough_12_entrypoints.md` | Onboarding | Giữ nguyên 1594 dòng hướng dẫn 12 entry points, cập nhật đường dẫn tương đối. |
| `CODE_WALKTHROUGH_HANDOFF_PROMPT.md` & `N46_HANDOFF_PROMPT.md` | `docs/walkthrough/` (hoặc tích hợp vào AGENTS) | Onboarding | Tinh gọn đưa vào tài liệu hướng dẫn bàn giao. |
| Thư mục `knowledgement/*` (20+ files) | `docs/knowledge/kn_w01_...` đến `kn_w06_...` | Tri thức | **Bảo toàn 100% nội dung kiến thức**, gom nhóm theo tuần/chủ đề để tránh rải rác 20 file đơn lẻ. |
| `N24_5_...`, `N25_...`, `N43_...`, `N44_...` | `docs/archive/chk_...` | Lịch sử | **Giữ nguyên 100% nội dung**, chỉ đổi tên theo chuẩn tiền tố `chk_` để lưu trữ. |

---

## IV. NỘI DUNG CHUẨN HÓA CỦA `docs/AGENTS.md` (MẪU RÚT GỌN)

File `docs/AGENTS.md` mới sẽ được thiết kế ngắn gọn, đanh thép và trực quan:

```markdown
# AGENTS.md — Trung tâm Điều hướng & Quy ước cho Agent tiếp nhận

> **Chào mừng Agent tiếp nhận:** Đây là tài liệu đầu vào duy nhất bạn cần đọc đầu tiên. Tài liệu này cung cấp các nguyên tắc bất di bất dịch và bản đồ liên kết tới toàn bộ kho tài liệu chuyên sâu trong thư mục `docs/`.

---

## 1. BA QUY TẮC CỐT LÕI (GOLDEN RULES)

1. **Code thật là sự thật duy nhất (Reality Over Docs):**
   - Không tin tuyệt đối vào các tài liệu thiết kế lý thuyết cũ. Luôn đối chiếu trực tiếp với mã nguồn thật: Android tại `main` (HEAD: `35d6bfd`, checkpoint: `346230b`) và Backend tại `server/myquizz` (HEAD: `37038f6`).
2. **100% KHÔNG sửa đổi Backend:**
   - User không can thiệp trực tiếp vào backend. Mọi giải pháp, tối ưu hóa và sửa lỗi đều thực hiện 100% trên Android Client.
   - Tận dụng kết quả từ 5 đợt audit sẵn có của backend để đưa vào báo cáo; các bug backend chưa fix (như BUG-01) ghi nhận vào phần "Hạn chế của hệ thống".
3. **Kiến trúc Clean Architecture & Single Source of Truth (SSOT):**
   - ViewModel → UseCase → Repository (Domain interface tại `core:common`, Network/Database implementation). Không bao giờ import ngược dependency.

---

## 2. BẢN ĐỒ TÀI LIỆU (DOCS SITEMAP)

| Lĩnh vực | Tài liệu cần đọc | Mục đích tra cứu |
| :--- | :--- | :--- |
| **Kiến trúc & Code** | [`docs/architecture/01_system_overview.md`](architecture/01_system_overview.md)<br>[`docs/architecture/02_project_structure.md`](architecture/02_project_structure.md)<br>[`docs/architecture/05_coding_conventions.md`](architecture/05_coding_conventions.md) | Hiểu cấu trúc Clean Architecture đa module, quan hệ giữa các module và quy ước code MVI. |
| **Mạng & Hợp đồng API** | [`docs/architecture/03_network_and_contracts.md`](architecture/03_network_and_contracts.md) | Nắm vững REST envelope, cơ chế CookieJar + TokenAuthenticator (401), Cold Flow Socket.IO. |
| **Lộ trình phát triển** | [`docs/roadmap/01_50_day_roadmap.md`](roadmap/01_50_day_roadmap.md)<br>[`docs/roadmap/02_n46_ui_polish_progress.md`](roadmap/02_n46_ui_polish_progress.md)<br>[`docs/roadmap/03_n47_n50_release_gates.md`](roadmap/03_n47_n50_release_gates.md) | Biết chính xác dự án đang ở đâu (N46 checkpoint `346230b`) và các mốc phát hành tiếp theo. |
| **Audit Backend & Bugs** | [`docs/backend-audit/01_backend_17_bugs_status.md`](backend-audit/01_backend_17_bugs_status.md)<br>[`docs/backend-audit/02_audit_handoff_summary.md`](backend-audit/02_audit_handoff_summary.md) | Danh sách 17 bug (7 đã fix, 9 chưa fix) và các thành quả audit bảo mật (Magic Bytes, Redis Lua). |
| **Onboarding nhanh** | [`docs/walkthrough/code_walkthrough_12_entrypoints.md`](walkthrough/code_walkthrough_12_entrypoints.md) | Đọc hiểu 12 file entry points quan trọng nhất trong codebase Android. |
| **Kho tri thức & Bài học** | [`docs/knowledge/`](knowledge/) | Toàn bộ bài học xương máu tích lũy qua các mốc phát triển N1–N46. |
| **Lưu trữ kiểm thử** | [`docs/archive/`](archive/) | Bằng chứng kiểm thử lịch sử các đợt E2E N25, N44. |

---

## 3. CHECKPOINT HIỆN TẠI & HÀNH ĐỘNG TIẾP THEO

- **Trạng thái:** Đã hoàn thành mốc N46 UI Polish trên 4 chế độ chơi. Nhánh `docs` đã được chuẩn hóa vào thư mục `docs/`.
- **Hành động:** Tiếp tục thực hiện mốc **N47** (Regression & Automated tests) hoặc triển khai kế hoạch tối ưu môn học theo [`docs/optimization-report/`](optimization-report/).
```

---

## V. KẾ HOẠCH MERGE PR & CẤU HÌNH GITHUB RULESET

### 1. Kế hoạch Merge PR nhánh `docs` vào `main`
- **Bước 1 (Chuẩn bị trên nhánh `docs`):**
  - Chuyển toàn bộ file tài liệu hiện có vào thư mục `docs/` theo đúng cấu trúc ở Mục II.
  - Xóa sạch các file docs rải rác ở root của nhánh `docs`.
  - Cập nhật lại toàn bộ đường dẫn liên kết tương đối (relative markdown links) trong tất cả các file.
  - Commit và Push lên `origin/docs`.
- **Bước 2 (Tạo Pull Request):**
  - Tạo PR từ `docs` vào `main` với tiêu đề: `chore(docs): restructure documentation into /docs folder`.
  - Kiểm tra diff để đảm bảo: Thư mục gốc `main` không bị xáo trộn, toàn bộ tài liệu được gom gọn gàng trong `docs/`.
- **Bước 3 (Merge):**
  - Merge PR vào `main`.

### 2. Thiết lập GitHub Branch Protection Ruleset
- **URL cấu hình:** `https://github.com/An-K4/MyQuizApp/settings/rules/27781276`
- **Phân tích quyền truy cập:**
  - URL này thuộc trang cài đặt quản trị (`/settings/`) của repository `An-K4/MyQuizApp`. Các bot, script hoặc agent bên ngoài **không thể xem được** do GitHub yêu cầu phiên đăng nhập có quyền Owner/Admin của tài khoản `An-K4` (nếu không sẽ trả về HTTP 404/Login redirect).
- **Hướng dẫn thao tác cho User (trên trình duyệt đã đăng nhập):**
  1. Sau khi merge PR của nhánh `docs` vào `main`, GitHub Actions workflow (`android-ci.yml` / CI checks) sẽ chạy lại trên nhánh `main`.
  2. Mở đường link trên bằng trình duyệt của bạn: [GitHub Ruleset #27781276](https://github.com/An-K4/MyQuizApp/settings/rules/27781276).
  3. Tìm đến mục **"Require status checks to pass before merging"** (Bắt buộc kiểm tra trạng thái đạt trước khi merge).
  4. Bật (Tick) kích hoạt tính năng này.
  5. Trong ô tìm kiếm status checks, chọn các job CI chính:
     - `Android CI` (hoặc `lint`, `unit-test`, `assembleDebug`).
  6. Lưu cấu hình (Save changes).
- **Lợi ích:** Sau khi bật, bất kỳ Agent nào làm việc tạo PR trong tương lai đều bị hệ thống tự động chặn nếu code làm gãy build hoặc rớt test, bảo vệ tuyệt đối độ ổn định của nhánh `main`!
