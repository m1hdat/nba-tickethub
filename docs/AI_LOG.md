# Nhật ký sử dụng AI Agent

Nhật ký này phân biệt AI hỗ trợ với các việc người dùng thực sự xác nhận. Không suy diễn rằng người dùng đã review, hiểu hoặc chạy kiểm thử. Khi bàn giao cuối, README cần có mục nhật ký/tóm tắt và link tới file này theo đề; hiện chưa tạo README trong phase thiết kế.

## 2026-11-24 — Phase 1: thiết kế hệ thống

Ghi chú cập nhật phạm vi sau tương tác mới: các đề xuất 200 vị trí bên dưới được giữ làm lịch sử của bản đầu, không còn là giới hạn sản phẩm hiện hành. Người dùng sau đó yêu cầu toàn arena/quy mô lớn; xem mục cuối nhật ký và DESIGN sửa đổi. Không viết lại prompt hoặc kết quả kiểm tra cũ thành những việc chưa thực hiện.

Agent: OpenAI Codex trong workspace `nba-tickethub`. Không sử dụng sub-agent. Phạm vi: phân tích và tài liệu, chưa triển khai application code.

### Prompt người dùng giao nhiệm vụ

Nguyên văn prompt của phase (giữ cả phần giới hạn và yêu cầu báo cáo):

```text
Đọc AGENTS.md, docs/REQUIREMENTS.md và đề bài chính thức
trong docs/sources/.

Thực hiện Phase 1: thiết kế hệ thống NBA Ticket Purchasing.

1. Phân tích các chức năng bắt buộc theo rubric.
2. Phân loại chức năng P0 và chức năng mở rộng.
3. Đề xuất kiến trúc ứng dụng và cấu trúc package.
4. Thiết kế data model và quan hệ giữa các entity.
5. Thiết kế các màn hình và luồng điều hướng.
6. Thiết kế ArenaView, Canvas, Matrix, pan/zoom,
   hit-testing và quản lý trạng thái chọn ghế.
7. Xác định chiến lược dữ liệu, phân biệt dữ liệu
   thực tế và dữ liệu mô phỏng.
8. Đề xuất kế hoạch kiểm thử cho từng chức năng.
9. Cập nhật ROADMAP.md theo thiết kế đề xuất.

Lưu kết quả thiết kế vào docs/DESIGN.md.

Giải thích lý do của các lựa chọn kỹ thuật và nêu
các phương án cần tôi quyết định.

Chưa triển khai application code.
Không tự commit hoặc push.
Dừng lại để tôi review và thống nhất thiết kế.
Sau khi hoàn thành:
- Cập nhật docs/PROGRESS.md theo tiến độ thực tế.
- Cập nhật docs/AI_LOG.md dựa trên các tương tác trong phase.
- Ghi nhận prompt, nội dung AI hỗ trợ, file thay đổi,
  quyết định của người dùng và kết quả kiểm chứng.
- Không ghi nhận việc review hoặc kiểm thử chưa diễn ra.
- Đề xuất commit message phù hợp.

Không tự commit hoặc push.
Dừng lại để tôi review và xác nhận.
```

### Nguồn đã đọc và nội dung AI hỗ trợ

- Đã đọc AGENTS, REQUIREMENTS, ROADMAP, PROGRESS; kiểm tra build script và version catalog để đề xuất phù hợp Java/XML hiện tại.
- Đọc đủ text 5 trang [PDF chính thức](sources/De_thi_giua_ky_CNTN2024.pdf), render đủ 5 trang bằng Windows.Data.Pdf và xem ảnh. Phát hiện khác biệt 50–250 ở chức năng/checklist so với 150–250 ở rubric; đề xuất 200 vị trí, không tự sửa nguồn.
- Chuẩn hóa yêu cầu có trang/mục thành REQ-01–18; phân loại P0 và mở rộng. Không coi đăng nhập/checkout/thanh toán là bắt buộc của đề.
- Soạn kiến trúc, package, entity/quan hệ, luồng UI, Canvas/Matrix/inverse, pan/pinch/clamp/hit-test, selection/filter/sheet/lifecycle, dữ liệu demo và test plan.
- Nêu lý do/đánh đổi và D-01–D-07 để người dùng quyết định; cập nhật roadmap và progress.
- Tra cứu tài liệu Android chính thức về [custom drawing](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing), [Matrix](https://developer.android.com/reference/android/graphics/Matrix), [gesture](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale), [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate), [BottomSheetBehavior](https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetBehavior). API tham khảo không thay thế việc kiểm thử implementation.

### File thay đổi

| File | Tác động của AI |
| --- | --- |
| [DESIGN.md](DESIGN.md) | Tạo thiết kế Phase 1 và danh sách quyết định chờ duyệt |
| [REQUIREMENTS.md](REQUIREMENTS.md) | Thay khung chờ đề bằng yêu cầu diễn giải có nguồn, rubric và giới hạn phạm vi |
| [ROADMAP.md](ROADMAP.md) | Cập nhật phase theo thiết kế, lịch tương đối, điều kiện hoàn tất, kế hoạch commit |
| [PROGRESS.md](PROGRESS.md) | Cập nhật tiến độ thực tế, giữ bằng chứng khảo sát cũ và phân biệt chưa chạy |
| AI_LOG.md | Tạo nhật ký tương tác phase này |

Không sinh/sửa Java, layout, manifest, dependency, dataset ứng dụng, AGENTS hoặc `.gitignore`. Có tạo công cụ trung gian cục bộ trong `tmp/pdfs/` để đọc/render/kiểm tra nguồn; đây không phải application code và được dọn sau khi kiểm chứng.

### Hạn chế, lỗi công cụ và cách xử lý thực tế của AI

- Python mặc định thiếu thư viện PDF, không có pdftotext; thử cài PyMuPDF thất bại do kết nối mạng bị chặn, WSL báo access denied. Không cài thành công hay thay cấu hình hệ thống.
- AI chuyển sang đọc stream/CMap bằng Python chuẩn và render bằng Windows.Data.Pdf; dùng ảnh đủ 5 trang để đối chiếu bảng/rubric với text. Công cụ trích xuất tạm chỉ dùng cho PDF này, không được coi là parser PDF tổng quát.
- Một lần apply_patch bị từ chối do delete/add trùng đường dẫn trong cùng patch; AI tách thao tác rồi ghi lại tài liệu. Không có application code bị ảnh hưởng.
- Lần đầu checker truy vết REQ không nhận dấu gạch dài qua stdin PowerShell, báo thiếu các mã ở cuối khoảng. AI đổi regex sang Unicode escape và chạy lại đạt đủ REQ-01–18; đây là lỗi công cụ kiểm tra, không phải kết quả test app.
- Công thức/kiến trúc/UX vẫn là đề xuất. Chưa kiểm tra tính tương thích dependency hoặc build/runtime, độ mượt, TalkBack, rotation hay process restore trên thiết bị.

### Quyết định của người dùng

- Đã giao nhiệm vụ thiết kế Phase 1, yêu cầu dừng để review, không implementation, không commit/push và ghi log trung thực.
- Chủ đề trong prompt là NBA Ticket Purchasing.
- Chưa có phản hồi duyệt D-01–D-07 hoặc đồng ý khuyến nghị kỹ thuật. Việc giao thiết kế không đồng nghĩa phê duyệt thiết kế.
- Chưa ghi nhận người dùng đã tự review mã/tài liệu, hiểu phần AI hỗ trợ, chạy app hay kiểm thử. Không gán các hành động đó cho người dùng.

### Kiểm chứng thực sự đã diễn ra

- AI đã đọc/đối chiếu văn bản và ảnh đủ 5 trang PDF; kiểm tra tĩnh nội dung tài liệu và nguồn Android tham chiếu.
- Đầu phase, `git status --short --branch` báo chưa phải repository. Cuối phase `.git` xuất hiện; đọc lại thấy `No commits yet on main`, origin fetch/push là `https://github.com/m1hdat/nba-tickethub.git`, `git ls-files` rỗng. AI không thực hiện init/add/commit/push; không quy việc tạo repository cho một người cụ thể và chưa kiểm tra remote online.
- Checker đạt: 6 Markdown file có UTF-8/code fence hợp lệ, 15 link cục bộ tồn tại, REQ-01–18 được truy vết trong DESIGN; 40 file mã ứng dụng/Gradle/ignore/AGENTS có SHA-256 và tập đường dẫn không đổi trước/sau ghi tài liệu.
- Một ví dụ số học round-trip tọa độ và zoom giữ tâm được tính thử bằng Python, đạt; không phải test Android Matrix/gesture. Các kiểm tra này là đối chiếu tài liệu/phạm vi sửa, **không phải test ứng dụng hoặc người dùng review**.
- Không chạy Gradle Sync/build/test/lint/emulator/device/profiling. Test plan chưa được thực thi; không có chức năng đạt nghiệm thu trong Phase 1.

Commit message AI đề xuất: `docs: define NBA ticket purchasing system design and phase 1 plan`.

Trạng thái bàn giao: chờ người dùng review/xác nhận; AI không init Git, add, commit hoặc push. Các file untracked nền dự án/PDF không được tự coi là AI tạo trong Phase 1.

## 2026-11-24 — đối chiếu DESIGN với quy định mới

- Công cụ: OpenAI Codex; không dùng sub-agent. Không ghi giờ cụ thể vì chưa xác định timestamp của tương tác.
- Prompt thực tế: “Đối chiếu lại DESIGN.md với các quy định mới, đặc biệt là phần ArenaView và Canvas.”
- Mục đích: kiểm tra thiết kế Phase 1 với AGENTS hiện hành, đặc biệt mục 2–3 và 7; chưa triển khai ứng dụng.
- Đầu vào đã đọc: AGENTS mới, DESIGN, REQUIREMENTS, ROADMAP, PROGRESS, AI_LOG; đọc MainActivity template để phân biệt thiết kế với code đang có. Không đọc lại PDF bằng công cụ trong tương tác này; đối chiếu yêu cầu đề qua REQUIREMENTS và bằng chứng đọc PDF của Phase 1, không ghi nhận một lượt render PDF mới.
- AI nhận thấy hướng thiết kế đã phù hợp, nhưng cách diễn đạt chưa tường minh về kế thừa View, điểm vào onDraw/Canvas/Paint, cấm ảnh sơ đồ tĩnh và tính bắt buộc của test khi triển khai.
- Nội dung hỗ trợ: bổ sung DESIGN mục 6.0 với hợp đồng renderer dùng cùng Canvas, tách trách nhiệm transform/hit-test/state, palette chung và bảng đối chiếu 12 quy định; tăng tiêu chí kiểm tra Canvas/legend; phân biệt công nghệ bắt buộc với các đề xuất nghiệp vụ/UX còn chờ duyệt.
- File sửa: `docs/DESIGN.md`, `docs/PROGRESS.md`, `docs/AI_LOG.md`. Không sinh/sửa application code hoặc test; không sửa AGENTS, REQUIREMENTS, ROADMAP, cấu hình hoặc nguồn PDF.
- Quyết định người dùng đã thể hiện: yêu cầu đối chiếu thiết kế. Chưa có xác nhận chọn D-01–D-07, phê duyệt thiết kế, review mã hoặc kiểm thử ứng dụng. Việc AI rà soát không được ghi thành sinh viên đã review/hiểu.
- Kiểm chứng thực tế: đọc nội dung và `git status --short --branch` thấy `No commits yet on main`; checker Python sau sửa đạt 6 Markdown file có UTF-8/code fence/bảng hợp lệ, 16 link cục bộ tồn tại; tập đường dẫn và SHA-256 42 file ngoài phạm vi không đổi so với trước sửa. Đây là kiểm tra tài liệu và phạm vi thay đổi, không phải test Android.
- Hạn chế: chỉ xác nhận sự phù hợp của mô tả thiết kế. Chưa có ArenaView implementation hoặc kết quả build/test/runtime, chưa kiểm chứng độ mượt, Matrix/gesture hay rotation trên thiết bị trong tương tác này.
- Commit message đề xuất: `docs: align ArenaView design with updated Canvas rules`.
- AI không init Git, đổi branch, stage, commit hoặc push; dừng sau tài liệu để người dùng review.

### Bổ sung cho lượt đối chiếu trên sau khi người dùng làm rõ toàn arena

Kết luận “hướng thiết kế đã phù hợp” ở lượt đối chiếu trên chỉ xác nhận hợp đồng Custom View/Canvas theo AGENTS tại thời điểm đó; chưa chứng minh phạm vi một arena lớn hoặc khả năng render/hit-test hàng chục nghìn ghế. Thiết kế trước vẫn có hạn chế: mô hình 200 vị trí/4 block, duyệt tuyến tính, culling tùy chọn và trần zoom 8×fit. Các giới hạn đó đã được thay trong DESIGN sửa đổi bằng hierarchy nhiều tầng/hàng, fixture nhỏ riêng, LOD/culling/index bắt buộc và zoom theo kích thước ghế. Bảng đối chiếu DESIGN mục 6.0 được giữ và bổ sung phạm vi lớn; không ghi rằng việc cập nhật tài liệu đã chứng minh hiệu năng.

## 2026-11-24 — làm rõ phạm vi toàn arena và kiến trúc quy mô lớn

Công cụ: OpenAI Codex (GPT-6 theo cấu hình phiên); không dùng sub-agent. Không ghi giờ cụ thể vì chưa xác định timestamp tương tác.

### Prompt thực tế của người dùng

```text
Làm rõ phạm vi thiết kế ArenaView:

Ứng dụng phải mô phỏng toàn bộ một NBA Arena, không giới
hạn ở một khu vực 200 ghế.

Thiết kế sơ đồ gồm sân bóng trung tâm, nhiều section,
nhiều tầng khán đài, hàng ghế và từng ghế riêng biệt.

Kiến trúc phải hỗ trợ hàng nghìn đến hàng chục nghìn ghế,
tối ưu Canvas rendering và hit-testing khi pan/zoom.

Có thể sử dụng bộ dữ liệu nhỏ trong giai đoạn kiểm thử
đầu tiên, nhưng không giới hạn thiết kế cuối cùng ở 200 ghế.

Sơ đồ, sức chứa, giá vé và trạng thái ghế mô phỏng phải
được phân biệt rõ với dữ liệu NBA thực tế.

Cập nhật DESIGN.md, ROADMAP.md và AI_LOG.md (cả đoạn đối chiếu hồi nãy)
Chưa triển khai code, chưa commit hoặc push.
```

### Phần AI hỗ trợ và quyết định thực tế

- Người dùng đã chốt phạm vi: toàn arena có court/section/tầng/hàng/từng ghế, hỗ trợ hàng nghìn đến hàng chục nghìn ghế; cho phép fixture nhỏ ban đầu; phân biệt mock với thực tế; chưa triển khai hoặc commit/push. Không hỏi lại có mở rộng quá 200 hay không.
- AI thay mô hình thành Arena→ArenaLevel→Section→SeatRow→Seat, thêm geometry/provenance, các level và category giá tách biệt, schema/ID không khóa 200. Đề xuất sơ đồ 2D trải tầng không chồng nhau.
- Thiết kế mới có LOD tổng thể→section/hàng→ghế, CPU viewport culling, hierarchy/grid index cho query điểm/vùng, exact shape hit-test sau inverse Matrix, zoom sâu theo kích thước ghế và state ID ổn định qua LOD.
- Đề xuất manifest JSON + generator mock có seed/version thay vì khai báo tay hàng chục nghìn object; worker compile/index/validate, cache hình học và aggregate, bảo vệ revision khi background result về. Lưu state lớn cần ngân sách Bundle và snapshot phiên, không nhét toàn dataset vào saved state.
- Quy mô 200/1k/10k/30k/50k là profile test đề xuất, không số liệu sức chứa NBA thật. Nêu rõ 200 chỉ còn fixture toàn arena thu nhỏ; small/full dùng chung pipeline. Các giá trị 30k/50k không phải trần schema hoặc benchmark đã đạt.
- Bổ sung ranh giới đề gốc 150–250 đối tượng so với yêu cầu người dùng lớn hơn. Chưa có xác nhận giảng viên về cách nộp full-arena; không tuyên bố profile nhỏ thay thế sản phẩm cuối hoặc viewport culling tự làm đúng giới hạn đề.
- Đọc tài liệu Android chính thức về [tối ưu Custom View](https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view) và [hardware acceleration](https://developer.android.com/develop/ui/views/graphics/hardware-accel). Các tài liệu hỗ trợ nguyên tắc tối ưu, không chứng minh lựa chọn grid/LOD của dự án đạt quy mô/hiệu năng.
- Người dùng chưa duyệt toàn bộ giải pháp cụ thể: sơ đồ trải tầng, density/profile cuối, schema/generator, giá mock, threshold LOD, thiết bị/ngân sách hiệu năng, chiến lược lưu phiên hoặc cách nộp. Không ghi nhận người dùng đã review code, tự chạy test hoặc nghiệm thu.

### File thay đổi và kiểm chứng

- `docs/DESIGN.md`: cập nhật đồng bộ mục tiêu, model/package, dữ liệu, navigation, hợp đồng Canvas, camera/index/LOD, state, test matrix, đánh đổi và quyết định D-01–D-09.
- `docs/ROADMAP.md`: đưa kiến trúc toàn arena/index/LOD/culling và test scale vào nền P0; fixture nhỏ là bước đầu; nêu cần đánh giá lại lịch một tuần.
- `docs/AI_LOG.md`: giữ lịch sử, bổ sung đính chính ngay trong đoạn đối chiếu trước và ghi prompt/quyết định tương tác mới.
- `docs/PROGRESS.md`: cập nhật phạm vi mới và tiến độ tài liệu theo quy tắc AGENTS, không ghi việc triển khai/test chưa diễn ra.
- Kiểm chứng đã chạy bằng Python: 6 Markdown file có UTF-8/code fence/bảng hợp lệ; 16 link cục bộ tồn tại; đủ truy vết REQ-01–18 và D-01–D-09; không còn công thức trần zoom 8×fit, đường hit-test duyệt tuyến tính 200 hay bố cục 4×5×10 làm giới hạn trong DESIGN. Đã rà các chỗ còn nhắc 200 để chỉ còn fixture/rubric/lịch sử hoặc tuyên bố bỏ giới hạn. Tập đường dẫn/SHA-256 41 file ngoài phạm vi (app/Gradle/AGENTS/ignore/REQUIREMENTS/PDF) không đổi. Không render lại PDF hoặc sửa đề gốc trong nhiệm vụ này.
- Chưa sinh application code, generator/data profile, index hoặc test; chưa chạy build/lint/Matrix/gesture/scale benchmark trên Android. Không có số liệu FPS/frame time/query/heap thực tế. Chỉ là thiết kế có kế hoạch chứng minh, chưa phải tuyên bố hỗ trợ đã được thực thi.
- Git đầu nhiệm vụ: `No commits yet on main`; AI không init/đổi branch/add/commit/push.

Commit message đề xuất: `docs: expand ArenaView design to full arena and scalable seat rendering`.

## 2026-11-24 — Phase 2: Project Foundation & Data Model

Công cụ: OpenAI Codex (GPT-6 theo cấu hình phiên), không dùng sub-agent. Ghi ngày theo ngữ cảnh phiên; không tự đặt giờ bắt đầu/kết thúc. Đây là nhiệm vụ triển khai foundation được người dùng giao sau các lượt thiết kế trước, không sửa lại lịch sử Phase 1 thành đã triển khai sớm.

### Prompt thực tế

```text
Đọc AGENTS.md và các tài liệu REQUIREMENTS.md, DESIGN.md,
ROADMAP.md, PROGRESS.md trước khi thực hiện.

Triển khai Phase 2: Project Foundation & Data Model.

1. Kiểm tra Android project và thiết kế đã thống nhất.
   Giữ nguyên Java, Android Views/XML và cấu hình Gradle.

2. Xây dựng cấu trúc package theo DESIGN.md.
   Chỉ tạo những package và class thực sự cần thiết.

3. Triển khai các data model theo thiết kế, bao gồm
   những đối tượng cần thiết cho Team, Game, Arena,
   Section, Seat và thông tin vé.

   Phân biệt ghế vật lý với giá vé và trạng thái ghế
   theo từng trận đấu.

4. Xây dựng mock JSON và lớp đọc dữ liệu từ assets.
   Kiểm tra ID, quan hệ giữa các entity và tính hợp lệ
   của dữ liệu.

5. Chuẩn bị mô hình hình học cho toàn bộ NBA Arena:
   - Sân bóng trung tâm.
   - Nhiều tầng và khu vực khán đài.
   - Hàng ghế và tọa độ logic của từng ghế.
   - Hỗ trợ mở rộng tới hàng nghìn ghế.
   - Không giới hạn kiến trúc ở 200 ghế.

   Có thể dùng dữ liệu nhỏ để kiểm thử ban đầu.
   Ghi rõ đâu là dữ liệu mô phỏng.

6. Viết unit test cho data model, JSON parsing
   và các ràng buộc dữ liệu quan trọng.

7. Chạy build và các bài kiểm thử phù hợp.

Giới hạn phạm vi:
- Chưa triển khai ArenaView và Canvas rendering.
- Chưa triển khai pan/zoom, hit-testing hoặc chọn ghế.
- Chưa triển khai Booking, Smart Seat Finder hay My Tickets.
- Không tự ý thay đổi thiết kế đã thống nhất.
- Không tự commit hoặc push.

Sau khi hoàn thành:
- Giải thích các class và luồng đọc dữ liệu.
- Liệt kê các file đã tạo hoặc thay đổi.
- Báo cáo kết quả build và test thực tế.
- Cập nhật docs/PROGRESS.md.
- Cập nhật docs/AI_LOG.md trung thực theo tương tác thực tế.
- Phân biệt công việc AI hỗ trợ và công việc người dùng.
- Đề xuất commit message phù hợp.

Dừng lại để tôi review trước khi chuyển sang Phase 3.
```

### Đầu vào, phần AI thực hiện và file thay đổi

- Đã đọc AGENTS/REQUIREMENTS/DESIGN/ROADMAP/PROGRESS, build scripts/catalog và test template; đối chiếu các phần thiết kế liên quan hierarchy/geometry/mock dữ liệu. Không render lại PDF hoặc tuyên bố kiểm tra nguồn NBA trong lượt này.
- Tạo 16 file model bất biến, bao gồm hierarchy Team/Game/Arena/Level/Section/Row/Seat và value geometry/provenance/tiền; GameSeat tách offer/giá/status theo trận khỏi ghế vật lý. Không tạo Booking hoặc vé đã mua.
- Tạo ArenaDataset/validator/repository/asset adapters, StrictJson và mock manifest reader; tạo geometry compiler thuần Java. Chỉ tạo package có trách nhiệm thật, không state/ui/selection rỗng.
- Parser JSON dùng Java chuẩn để không sửa Gradle/dependency, có kiểm tra syntax, duplicate key, type, Unicode/numeric bounds, depth/size. Mock reader có schema v1 và budget configurable cho số vị trí/offers. Kiểm tra JSON và schema chạy bằng cùng code trên JVM, không dựa vào Android JSON stub trong unit test.
- Tạo 3 mock JSON ở assets cho 200/10.000/30.000 vị trí; 3 tầng, 20 section, hàng và từng vị trí độc lập, inventory 2 trận. Các profile có cả ghế ngồi và vị trí đứng, không nhầm tổng vị trí với số ghế ngồi thực; số lượng 50k chỉ sinh trong test từ cấu hình 30k. Mọi sân/lịch/giá/status/capacity đều MOCK.
- Validator kiểm tra quan hệ/ID/range/geometry/inventory, có grid/SAT private để xác minh shape chồng lấn; không phải index phục vụ viewport/touch và không triển khai hit-testing tương tác.
- Viết 30 unit test mới trong 4 class, TestAssets helper và runner PowerShell tái chạy JVM kiểm tra. Không sửa test template để làm test pass; runner chạy thêm 1 test mẫu cũ, tổng 31.
- File mới: 25 Java production source dưới model/data/geometry; 3 JSON assets; 5 Java test/helper; tools/test-foundation.ps1. Danh sách tên cụ thể và luồng đọc được ghi tại mục Phase 2 của [PROGRESS.md](PROGRESS.md).
- File tài liệu sửa: PROGRESS, AI_LOG và ROADMAP. ROADMAP ghi đúng phần Phase 2 được giao mới nhất: foundation/geometry, chưa interactive index/Canvas/hit-test; không thay thiết kế kiến trúc trong DESIGN.
- Không sửa Java/XML UI, MainActivity, manifest, Gradle/AGP/SDK/dependency, AGENTS, REQUIREMENTS, DESIGN hoặc đề PDF. Output/cache kiểm tra dưới .gradle và build output bị ignore.

### Quyết định/công việc của người dùng thực sự ghi nhận

- Người dùng giao triển khai Phase 2 trên thiết kế hiện có; xác định rõ Java/XML/Gradle giữ nguyên, model/JSON/validator/geometry/test thuộc phạm vi và các chức năng UI/booking chưa triển khai.
- Trước lượt có 3 commit và main theo origin/main; AI không tạo các commit có sẵn hoặc tự gán tác giả/chất lượng review cho chúng.
- Người dùng chưa phản hồi review code Phase 2, chưa xác nhận tự chạy test, hiểu hết mã, xem app hoặc nghiệm thu. Không có cơ sở ghi những việc đó là đã hoàn tất.
- Các tham số profile/seed/giá mock là lựa chọn triển khai minh họa của AI trong phạm vi được giao, không phải số liệu NBA thực hoặc quyết định thương mại của người dùng.

### Kiểm chứng thực tế và các giới hạn

- Wrapper mặc định thất bại khi tạo lock ở C:\\.gradle. AI dùng GRADLE_USER_HOME trong workspace và cache dependency có sẵn ở chế độ chỉ đọc; lần đầu đặt GRADLE_RO_DEP_CACHE dư modules-2, Gradle chỉ ra layout sai; đã sửa biến môi trường, không sửa Gradle files.
- Lượt kế tiếp qua cấu hình plugin nhưng môi trường báo lỗi đường dẫn C:\\.android; dùng ANDROID_USER_HOME trong workspace. Sau đó offline thiếu aapt2-9.4.1-15978811-windows.jar. AI tìm binary AAPT2 cùng phiên bản đã có trong cache transform, sao chép vào .gradle/phase2-tools và truyền Maven override tạm thời qua CLI.
- Lượt Gradle cuối với cache/user.home trong workspace, --offline/--no-configuration-cache và AAPT2 override vẫn **BUILD FAILED**, :app:compileDebugJavaWithJavac báo AccessDeniedException với SDK android-37.0/android.jar; lượt trước còn báo core-for-system-modules.jar. Đây là lỗi thực tế quan sát trong môi trường hiện tại, chưa xác minh nguyên nhân hệ thống sâu hơn. Không tự đổi quyền, JDK/SDK, minSdk/AGP hoặc cấu hình Gradle để xử lý.
- Chưa có APK đạt kiểm tra; Gradle testDebugUnitTest/lintDebug không hoàn tất vì dừng ở compile. Không chạy emulator/device/connected test; không có kết quả rendering/pan/zoom/hit-test hoặc benchmark FPS.
- Ban đầu javac đọc JUnit jar trực tiếp từ cache ngoài workspace báo AccessDenied lúc cleanup compiler, dù JUnit có chạy. Đã đổi runner để sao chép dependency/SDK jar cần dùng vào cache cục bộ, rồi chạy lại sạch, không dựa vào lượt có diagnostic để chốt kết quả.
- Lệnh thực tế: powershell -NoProfile -File tools/test-foundation.ps1 với DependencyCacheRoot trỏ cache hiện có và AndroidJarPath trỏ SDK android-37.0/android.jar. Java --release 11 -encoding UTF-8 -Xlint:all biên dịch 25 file mới đạt, AndroidAssetSource compile với bản sao SDK jar; adapter chưa thực thi trên Android.
- Kết quả runner cuối: **OK (31 tests)**, 30 test mới + 1 test mẫu, JUnit báo 1.644 giây. Các ca mới kiểm tra JSON/model/FK/ID/geometry/currency/inventory/immutability/stream errors và load/validate 10k/30k/50k vị trí. Đây là test dữ liệu trên JVM với heap 512MB, không chứng minh rendering/hit-testing Android ở quy mô đó.
- Kiểm tra cuối đạt: 6 Markdown file có UTF-8/code fence/bảng hợp lệ, 17 link cục bộ tồn tại, 3 JSON gắn MOCK không có nguồn/thời gian thu thập giả; 25 Java production/5 test-helper và runner không có ký tự lỗi/trailing whitespace. SHA-256/tập đường dẫn của 35 file bảo vệ (Gradle, AGENTS/REQUIREMENTS/DESIGN, MainActivity, manifest, resources) không đổi. git diff --check đạt, chỉ có cảnh báo LF→CRLF sẵn có; không sửa Git config. HEAD vẫn a4e6788, staged diff rỗng; PDF sources untracked đã có từ đầu lượt. Không tự ghi nhận người dùng review hoặc test chưa xảy ra.

Commit message đề xuất: `feat: add arena data foundation and validated mock assets`.

AI không init Git, đổi branch, stage, commit hoặc push. Dừng ở kết quả Phase 2 để người dùng review; build Android còn cần xác minh trong môi trường truy cập SDK bình thường.

## 2026-11-25 — review Phase 2 và khảo sát nguồn tham khảo

Công cụ: OpenAI Codex (GPT-6 theo cấu hình phiên), shell/Java/JUnit và web search/open nguồn chính thức. Không dùng sub-agent, không tích hợp dependency hoặc triển khai Phase 3.

### Prompt thực tế

```text
Review kết quả Phase 2 vừa triển khai.

Đề xuất những thư viện, dataset và mã nguồn mở có thể
tận dụng trong các phase tiếp theo để cải thiện ứng dụng.

Kiểm tra nguồn, giấy phép và tính tương thích với Java
và Android Views/XML.

Riêng ArenaView phải tự triển khai bằng Canvas, Paint
và Matrix, không sử dụng thư viện thay thế.

Ghi các nguồn tham khảo vào docs/SOURCES.md.
Cập nhật docs/AI_LOG.md theo tương tác thực tế.

Chưa tích hợp thêm dependency hoặc triển khai Phase 3.
Không tự commit hoặc push.
```

### Công việc AI thực hiện

- Đọc lại AGENTS, tài liệu requirements/design/roadmap/progress/log, catalog/build scripts và source/test/runner Phase 2. Review trọng tâm dữ liệu/validator/compiler, phân biệt kiểm tra JVM với build Android và runtime.
- Chạy lại tools/test-foundation.ps1 với cache JUnit và SDK jar hiện có: **OK (31 tests)**, JUnit báo 1.773 giây; AndroidAssetSource compile, chưa thực thi trên Android. Không chạy lại Gradle; chỉ đọc log Phase 2 có lỗi AccessDeniedException ở SDK android.jar, không dùng log cũ để tuyên bố một build mới đã thất bại.
- Tạo probe riêng dưới `.gradle/phase2-review/` để kiểm chứng containment của quad trong hàng vành khuyên. Sau khi sửa BOM UTF-8 do Set-Content của PowerShell thêm vào file probe, javac/Java chạy đạt và in `validatorAccepted=true innerRadius=340.000 edgeMidpointRadius=339.899961`. Không sửa production hoặc unit test để tạo kết quả này.
- Phát hiện P2 đã tái hiện: validator chỉ kiểm tra bốn đỉnh, có thể chấp nhận cạnh đi vào vùng lỗ bên trong annulus. Đề xuất kiểm tra toàn cạnh/biên và regression test; chưa tự sửa code. Không ghi ba mock manifest hiện tại là bị lỗi, không biến 31 test xanh thành nghiệm thu toàn bộ validator.
- Nêu thêm rủi ro cần đo với grid khi kích thước/mật độ ghế lệch, không khẳng định đây là lỗi thực tế của dataset hiện hành hoặc đã có benchmark worst-case.
- Tra cứu primary sources: Android Developers/AndroidX/AOSP, LICENSE/README của Material/Moshi/Gson/Robolectric/JTS/nba_api, Wikidata licensing/entity pages, OSM copyright, BALLDONTLIE docs/Terms và NBA Terms. Chỉ đọc/ghi nguồn; không nhập mã/dataset bên ngoài vào app, gọi NBA API hoặc cung cấp credential.
- Tạo SOURCES với review findings, ưu tiên theo phase, giấy phép và mức tương thích API/Java/Views/XML. Tách verified documentation/license khỏi chưa resolve/compile/test artifact; Robolectric có SDK/JDK compatibility table nhưng chưa được chạy trong repository. Không pin phiên bản từ README alpha hoặc tự sửa Gradle.
- Giữ ArenaView tự viết bằng Canvas/Paint/Matrix/inverse; helper lifecycle/accessibility/list chỉ hỗ trợ phần bên ngoài, JTS chỉ đề xuất oracle JVM, không thay engine/hit-test/selection.

### Nguồn và giới hạn kiểm chứng

- Các URL/giấy phép cụ thể được lưu tại [SOURCES.md](SOURCES.md). AndroidX/Material/Moshi: Apache-2.0; Robolectric và nba_api client: MIT; JTS: EPL-2.0 hoặc EDL-1.0 theo LICENSES; metadata Wikidata CC0; OSM ODbL; BALLDONTLIE theo điều khoản dịch vụ riêng. Đây là kết quả đọc tài liệu, không phải kiểm định pháp lý toàn bộ dependency graph.
- License client code không mặc nhiên là license dữ liệu hoặc logo. Không xác minh được dataset mở đầy đủ từng ghế đa tầng + inventory/giá NBA thật trong lượt khảo sát này; tiếp tục đề xuất mock cho các phần đó, structured snapshot có nguồn cho metadata khi được giao.
- Một số URL không xác minh đủ: các tag Moshi thử trả 404; seating maps Crypto.com Arena và endpoint Wikidata JSON export không truy cập được qua công cụ; trang entity Wikidata đọc được. Link Retrofit trả redirect sang owner khác qua công cụ nên không chọn làm nguồn được khuyến nghị trong SOURCES. Không diễn giải các lượt fetch thất bại thành đã kiểm nội dung/license hoặc đã tải dataset.
- Không tải/cài các thư viện được đề xuất, không chạy benchmark, TalkBack, Android instrumentation hoặc giả định minSdk/R8/AGP của mọi artifact đã được thử. Cần kiểm phiên bản/POM/AAR/NOTICE/transitive dependencies khi có nhiệm vụ tích hợp riêng.

### File thay đổi và vai trò người dùng

- Tạo `docs/SOURCES.md`; nối mục tương tác này vào `docs/AI_LOG.md`; cập nhật `docs/PROGRESS.md` theo AGENTS. Probe/chương trình chẩn đoán và output test ở `.gradle/`, không thuộc app source.
- Người dùng yêu cầu AI review và tìm nguồn, giữ ArenaView tự viết, không dependency/Phase 3/commit/push. Chưa ghi nhận người dùng tự review/hiểu/test/nghiệm thu Phase 2 hoặc đồng ý chọn thư viện/dataset cụ thể. Phần review và chạy probe ở trên do AI thực hiện.
- Kiểm tra cuối đạt: 7 Markdown file có UTF-8/code fence/bảng hợp lệ; 26 link cục bộ tồn tại; SOURCES có 17 mục LIB/CODE/DATA cùng nguồn/giới hạn. SHA-256 và tập đường dẫn 73 file app/test/assets/config/Gradle/runner/AGENTS/REQUIREMENTS/DESIGN/ROADMAP không đổi so với trước cập nhật tài liệu. git diff --check đạt, staged diff rỗng; các thay đổi Phase 2 chưa commit từ đầu lượt được giữ nguyên. Không sửa lịch sử AI log hay xóa kết quả Phase 2 trước đó.

Commit message đề xuất: `docs: review phase 2 and catalog licensed reference sources`.

AI không init Git, đổi branch, stage, commit hoặc push. Dừng ở review/tài liệu; lỗi validator và build Android vẫn là các mục cần xử lý/xác minh bằng nhiệm vụ tiếp theo.
