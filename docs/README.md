# MyQuizApp — Hệ Thống Tài Liệu Kỹ Thuật (Documentation)

> **Chào mừng bạn đến với trung tâm tài liệu của MyQuizApp.**
> Toàn bộ tài liệu kỹ thuật, kiến trúc, lộ trình phát triển và kho tri thức được lưu trữ tập trung tại thư mục `docs/`.

---

## 1. ĐIỂM BẮT ĐẦU CHO AGENTS VÀ DEVELOPERS

* **Nếu bạn là AI Agent tiếp nhận phiên làm việc:** Đọc ngay [`AGENTS.md`](AGENTS.md) — Điểm vào duy nhất (Single Entry Point) chứa 3 quy tắc cốt lõi và bản đồ tra cứu toàn dự án.
* **Quy chuẩn cập nhật tài liệu:** Đọc [`DOCS_GUIDELINES.md`](DOCS_GUIDELINES.md) — Bộ luật bắt buộc về quy ước đặt tên file/thư mục, quy trình cập nhật và nguyên tắc phân biệt *Living Truth* vs *Archive*.
* **Tra cứu:** [Mục lục tri thức](knowledge/INDEX.md), [cổng nghiệm thu N47–N50](roadmap/03_n47_n50_release_gates.md), [kế hoạch tối ưu 4 Sprint](optimization-report/01_optimization_plan_4_sprints.md).

---

## 2. CẤU TRÚC THƯ MỤC TÀI LIỆU

```
docs/
├── AGENTS.md                                # Cổng vào điều hướng trung tâm và quy tắc cốt lõi
├── DOCS_GUIDELINES.md                       # Bộ luật quy chuẩn và quản trị hệ thống tài liệu
├── README.md                                # Trang tổng quan này
│
├── architecture/                            # Kiến trúc hệ thống & Thiết kế kỹ thuật (Living Truth)
│   ├── 01_design_document_v2.md             # Tài liệu thiết kế chi tiết (UI components, theme, socket, API)
│   ├── 02_project_structure.md              # Trách nhiệm 14 module Android Clean Architecture
│   └── 03_coding_conventions.md             # Quy ước code và bàn giao
│
├── roadmap/                                 # Lộ trình & Quản lý phát triển (Living Truth)
│   ├── 01_50_day_roadmap.md                 # Kế hoạch tổng thể 50 ngày (N1–N50)
│   ├── 02_n46_ui_polish_progress.md         # Tiến độ chi tiết mốc N46 UI Polish
│   └── 03_n47_n50_release_gates.md          # Cổng nghiệm thu N47–N50
│
├── backend-audit/                           # Báo cáo Audit & Khảo sát Backend (Living Truth)
│   └── 01_backend_17_bugs_status.md         # Báo cáo đối chiếu 17 bugs và hiện trạng sửa lỗi
│
├── optimization-report/                     # Đề án tối ưu Android và phương pháp đánh giá
│   ├── 01_optimization_plan_4_sprints.md
│   ├── 02_performance_security_tradeoffs.md
│   └── 03_benchmark_metrics_scenarios.md
│
├── walkthrough/                             # Onboarding & Đọc hiểu Codebase
│   ├── code_walkthrough_12_entrypoints.md   # Hướng dẫn chi tiết 12 điểm vào của Android app
│   ├── code_walkthrough_handoff_prompt.md   # Prompt hướng dẫn chuyển giao walkthrough
│   └── n46_handoff_prompt.md                # Prompt bàn giao UI N46
│
├── knowledge/                               # Kho tri thức & Bài học xương máu (24 tài liệu)
│   ├── INDEX.md                             # Mục lục 23 bài tri thức và hướng dẫn setup
│   ├── setup_guide_vn.md                    # Hướng dẫn cài đặt môi trường
│   └── kn_01_n1_n5.md ... kn_23_...         # Toàn bộ bài học chi tiết qua từng sprint
│
└── archive/                                 # Lưu trữ hồ sơ & Checklist lịch sử (Bất biến)
    ├── chk_n24_5_classic_personal_results_plan.md
    ├── chk_n25_e2e_classic_checklist.md
    ├── chk_n43_implementation.md
    ├── chk_n44_e2e_integration_checklist.md
    ├── chk_n44_e2e_report.md
    ├── chk_n44_integration.md
    ├── chk_refactor_checklist.md
    ├── agents_pre_restructure.md             # Bản nguyên văn AGENTS trước refactor
    └── docs_restructure_plan.md             # Kế hoạch đợt tái cấu trúc tài liệu
```
