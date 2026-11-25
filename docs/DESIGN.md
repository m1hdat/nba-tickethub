# Thiết kế NBA Ticket Purchasing — cập nhật Phase 3A.1

Cập nhật: 2026-11-25. **Phase 2/3A và theme 3A.1 đã triển khai; chờ review demo mới. Mục 13 là hợp đồng hiện tại: 30 CourtDesign và ArenaStands theo arenaId; mục 11–12 lưu lịch sử.** Mục 1–10 lưu thiết kế ban đầu; mục 11 bổ sung kiến trúc nhiều sân và chia Phase 3 theo nhiệm vụ mới. Không coi việc giao Phase 3A là nghiệm thu mọi lựa chọn trước đó.

Nguồn: [đề thi, 5 trang](sources/De_thi_giua_ky_CNTN2024.pdf), diễn giải có mã tại [REQUIREMENTS.md](REQUIREMENTS.md). Các ưu tiên P0/P1, kiến trúc, dữ liệu, UX và test dưới đây là đề xuất của AI; chưa phải quyết định được người dùng chấp thuận.

Đã đối chiếu lại với [AGENTS.md](../AGENTS.md), đặc biệt mục 2–3 và 7, ngày 2026-11-24. Java/Views/XML, Android Custom View, Canvas/Matrix và kiểm thử là ràng buộc phải tuân thủ. Theo chỉ dẫn mới của người dùng, thiết kế phải bao quát toàn bộ arena, nhiều tầng/section/hàng/ghế và hỗ trợ hàng nghìn đến hàng chục nghìn ghế. Phạm vi này đã được xác nhận; mật độ cụ thể, nguồn dữ liệu và các lựa chọn kỹ thuật ở mục 10 chưa được duyệt toàn bộ.

## 1. Mục tiêu và chức năng theo rubric

Ứng dụng Android mô phỏng **toàn bộ một NBA Arena**, với sân bóng trung tâm, nhiều tầng khán đài, nhiều section, hàng ghế và từng ghế có ID riêng. Màn mở đầu nhìn tổng thể arena; người dùng đi từ tầng/section/hàng xuống ghế, chọn/bỏ ghế, xem danh sách và tổng giá giả lập. Kiến trúc phải hỗ trợ hàng nghìn đến hàng chục nghìn ghế, không có giới hạn thiết kế 200 ghế. P0 chưa bao gồm phát hành vé hoặc thu tiền.

### Phân biệt phạm vi sản phẩm và hồ sơ rubric

- **Phạm vi người dùng đã chốt:** toàn arena nhiều tầng với dữ liệu và hình học từng ghế. Khả năng mở rộng, culling, mức chi tiết và spatial index thuộc nền P0 của sản phẩm, không đẩy thành P1 tùy chọn.
- **Fixture ban đầu:** khoảng 200 ghế phân bố trên toàn bố cục nhiều tầng/section để kiểm tra chức năng nhỏ; không phải chỉ xây một section rồi coi là toàn arena. Cùng schema, pipeline và index với dataset lớn.
- **Rubric gốc:** yêu cầu/checklist 50–250, rubric 150–250 đối tượng. Profile `rubric-small` có thể dùng 200 ghế trên toàn sơ đồ thu nhỏ; profile `full-arena` kiểm tra quy mô lớn. Không coi số ghế đang nhìn thấy trong cửa sổ là tổng số đối tượng của profile.
- Profile nhỏ là công cụ kiểm tra/đối chiếu đề, không giới hạn schema, camera, state hoặc số ghế cuối. Các chỗ còn ghi mục tiêu 200 trong REQUIREMENTS phản ánh phương án rubric ban đầu, được hiểu theo profile nhỏ khi đối chiếu DESIGN sửa đổi này; không thay số liệu đề gốc.
- Quy mô lớn vượt khoảng số lượng của đề: cần xác nhận giảng viên cách nộp/minh họa hai profile. Chưa có xác nhận rằng full-arena hàng chục nghìn ghế được chấm như đúng nguyên văn rubric; không âm thầm thay toàn bộ sản phẩm bằng bản nhỏ để giải quyết điểm này.

| Rubric | Trọng số | Thiết kế P0 | Yêu cầu |
| --- | --- | --- | --- |
| Canvas, bố cục, hiệu năng | 25% | Một ArenaView toàn arena, nhiều tầng/section/hàng/ghế; culling/LOD/index; fixture nhỏ để đối chiếu số lượng rubric | REQ-01–03, 13; phạm vi toàn arena từ người dùng |
| Tương tác, tọa độ | 25% | Pan có biên, pinch đúng tâm, Matrix/inverse chung, hit-test biên | REQ-04–06 |
| Chức năng, UX | 20% | Legend, filter, reset mượt, sheet thu gọn/mở rộng, focus/xóa đồng bộ | REQ-07–11 |
| Kiến trúc, state | 15% | Model độc lập, ViewModel giữ state, giữ lựa chọn khi xoay, hỗ trợ dọc/ngang | REQ-12 |
| Chất lượng, ổn định | 10% | Kiểm tra dữ liệu, gesture/lifecycle/biên, build và runtime | REQ-14 và các luồng P0 |
| Hồ sơ, quy trình | 5% | Git, README, video và AI log trung thực | REQ-15–18 |

50% trọng số nằm ở Canvas và tương tác nên làm phần này trước mở rộng bán vé. Thiết kế không chứng minh chức năng đã hoạt động hoặc đã đạt điểm.

**P0 — đề gốc và phạm vi người dùng:** toàn arena nhiều tầng/section/hàng/từng ghế; schema không khóa số lượng; rendering/hit-test có culling/LOD/index; trạng thái trực quan; pan/pinch/tap; legend; lọc tầng/section; điều kiện/xóa lọc/empty state; reset; sheet thu gọn/mở rộng/cuộn/focus/xóa; đồng bộ; giữ selection khi xoay; dọc/ngang; kiểm tra quy mô lớn, build/chạy và hồ sơ. Phần toàn arena là yêu cầu người dùng bổ sung, không giả thành nội dung đề PDF. Tổng giá vẫn là mock.

**P1 — mở rộng sau P0:** tìm mã ghế; filter trạng thái/loại kết hợp; nhiều trận và màn chọn trận; xác nhận đặt vé mô phỏng; lịch sử đơn và vé demo. Chỉ thực hiện nếu còn thời gian và được duyệt.

**Ngoài phạm vi hiện tại:** tài khoản, backend, API NBA trực tiếp, tồn kho nhiều người dùng, giữ ghế theo thời gian, thanh toán thật, vé QR dùng tại sân, thông báo. Đề không bắt buộc các phần này; không suy diễn từ chữ Purchasing.

## 2. Kiến trúc ứng dụng và package

Giữ **Java + Android Views/XML**, một module `app`, một `MainActivity` mỏng; đề xuất ViewModel + LiveData + SavedStateHandle và repository mock. Chưa thêm dependency hay sửa build script.

```mermaid
flowchart LR
    REPO[MockArenaRepository] --> MODEL[Domain models]
    MODEL --> VM[ArenaViewModel và ArenaUiState]
    VM --> VIEW[MainActivity và ArenaView]
    VM --> SHEET[Bottom Sheet và bộ lọc]
    VIEW -->|Tap / ViewportChanged| VM
    SHEET -->|Remove / Focus / Filter| VM
    VM <--> SAVE[SavedStateHandle]
```

- **Model/repository:** tách dataset arena bất biến, hierarchy, hình học/index và inventory theo trận khỏi UI state nhỏ. Load/validate/compile geometry/build index trên worker, publish snapshot theo datasetVersion; không tạo bản sao toàn bộ hàng chục nghìn ghế mỗi lần tap/pan. Chỉ cần repository mock, không API/DB rỗng.
- **ViewModel:** nhận action, kiểm tra invariant, cập nhật snapshot; selection, danh sách sheet và tổng tiền cùng nguồn. LiveData chỉ lộ dạng đọc; không giữ Activity/View/Context giao diện.
- **ArenaView:** Android Custom View, `ArenaView extends android.view.View`, override `onDraw(Canvas canvas)` để vẽ bằng Canvas/Paint; nhận gesture, quản lý camera tạm thời và phát action qua listener. Không tự sửa trạng thái ghế hoặc giữ một selection khác. Không dùng Kotlin, Compose hoặc WebView thay kiến trúc Java/Views/XML bắt buộc.
- **MainActivity:** bind UI, observe theo lifecycle, nối listener và điều phối sheet; không chứa công thức hit-test hoặc tính giá.
- **Camera:** View cache Matrix mỗi frame; ViewModel giữ bản chụp camera ngữ nghĩa khi kết thúc gesture/animation và trước lưu lifecycle. Pan chỉ đổi camera/visible set; không publish lại toàn bộ dataset mỗi pixel. Camera live và snapshot không tạo hai nguồn selection.
- **Geometry/index:** `ArenaGeometryCompiler` tạo polygon/AABB và hierarchy/index theo version; `ArenaSpatialIndex` phục vụ cả query vùng nhìn và query điểm. `LodPolicy` quyết định vẽ section/hàng/ghế theo kích thước chiếu lên màn hình. Pipeline này có từ fixture nhỏ, không chờ dataset lớn mới thay kiến trúc.

Lý do: hợp nền Java/XML và bài cá nhân một tuần, logic dễ test, rotation rõ ràng. Chưa cần nhiều module, DI framework, Room, Retrofit, Fragment hoặc Navigation Component cho một màn chính. Nếu duyệt P1 nhiều màn thì đánh giá navigation sau.

```text
com.example.nbatickethub/
├── MainActivity.java
├── data/
│   ├── ArenaRepository.java
│   └── mock/MockArenaRepository.java
├── model/
│   ├── Team.java, Game.java, Arena.java, ArenaLevel.java
│   ├── Section.java, SeatRow.java, Seat.java
│   ├── GameSeat.java, SeatStatus.java, PositionKind.java
│   └── Money.java, WorldRect.java, SeatGeometry.java, DataProvenance.java
├── geometry/
│   ├── ArenaGeometryCompiler.java, ArenaSpatialIndex.java
│   └── SectionSeatGrid.java (index không gian theo section)
├── state/
│   ├── ArenaViewModel.java, ArenaUiState.java
│   ├── FilterState.java, SelectionReducer.java, ViewportState.java
├── ui/
│   ├── arena/ArenaView.java, ArenaRenderer.java
│   ├── arena/ViewportTransform.java, SeatHitTester.java, LodPolicy.java
│   ├── sheet/SelectionAdapter.java
│   └── filter/ (thêm class khi thực sự cần)
└── (P1: model/order và ui/order nếu được duyệt)
```

Đây là trách nhiệm dự kiến, không tạo class rỗng. Hình học/index Java thuần để test độc lập; renderer cache Path/RectF theo các nhóm cần vẽ, không tạo Paint riêng cho mỗi ghế. Ưu tiên mảng primitive, offset theo level/section/row và bảng ID→index thay vì lặp object đồ họa nặng cho mọi seat. Khi publish state dùng chung dataset/index bất biến, chỉ đổi ID/tập chọn/filter nhỏ. Giữ namespace hiện tại.

Khi triển khai cần xác minh artifact AndroidX ViewModel, LiveData, saved state, RecyclerView và khai báo qua catalog; Material hiện có dùng cho sheet/chips. Chưa chọn phiên bản mới hay khẳng định đã resolve dependency.

## 3. Data model và quan hệ

| Entity/value | Trường chính dự kiến | Vai trò / ràng buộc |
| --- | --- | --- |
| Team | id, displayName, shortName | Hai đội demo; không cần roster |
| Game | id, homeTeamId, awayTeamId, arenaId, displayTime, provenance | Hai đội khác nhau; giờ demo ghi rõ mô phỏng; nếu dùng giờ thật sau này cần Instant + ZoneId |
| Arena | id, name, worldBounds, schemaVersion, geometryVersion, provenance | Toàn arena, bounds dương; sức chứa tính từ dataset, không hardcode 200 |
| ArenaLevel | id, arenaId, code, name, order, bounds | Tầng khán đài, ví dụ Lower/Club/Upper của arena mô phỏng; không đồng nghĩa loại giá VIP/Standard |
| Section | id, levelId, code, name, category, boundary, labelAnchor | Nhiều section mỗi tầng; category VIP/Standard/Standing độc lập với tầng; có polygon và AABB |
| SeatRow | id, sectionId, rowLabel, order, boundary, seatIndexRange | Hàng thuộc đúng một section; có thể theo cung quanh sân, số ghế mỗi hàng thay đổi |
| Seat | id, rowId, positionNumber, kind, geometryIndex | Ghế riêng biệt, ID ổn định; polygon/hình học và AABB được compile, không giới hạn tổng số |
| GameSeat | gameId, seatId, baseStatus, price | Khóa kép duy nhất; trạng thái/giá theo trận, không gắn vĩnh viễn lên hình học Seat |
| SeatStatus | AVAILABLE, BOOKED, LOCKED | Trạng thái gốc; SELECTED chỉ suy ra khi hiển thị |
| Money | amountMinor: long, currencyCode | USD cent cho demo; không cộng tiền bằng float/double, không trộn currency |
| WorldRect | left, top, right, bottom | Java thuần, cùng hệ world; kích thước dương |
| SeatGeometry | world vertices, center, AABB | Rect hoặc tứ giác lồi theo hướng hàng; AABB để query nhanh, shape để hit chính xác |
| DataProvenance | originType, sourceUrl, collectedAt, version, simulationSeed | originType MOCK, REAL_SNAPSHOT hoặc LIVE_VERIFIED; ghi ở đúng nhóm trường, không gán nhãn thật cho cả dataset chỉ vì có tên sân thật |

`Seat` đại diện chung cho vị trí bán vé. Khi kind là STANDING_SPOT, UI ghi “vị trí đứng”, không gọi là ghế thật. Như vậy giữ ba khu trong mô tả đề tài 1 mà không mô tả sơ đồ mô phỏng như một sân NBA thực tế.

```mermaid
erDiagram
    TEAM ||--o{ GAME : home_team
    TEAM ||--o{ GAME : away_team
    ARENA ||--o{ GAME : hosts
    ARENA ||--|{ ARENA_LEVEL : contains
    ARENA_LEVEL ||--|{ SECTION : contains
    SECTION ||--|{ SEAT_ROW : contains
    SEAT_ROW ||--|{ SEAT : contains
    GAME ||--|{ GAME_SEAT : offers
    SEAT ||--o{ GAME_SEAT : inventory_per_game
```

Mỗi Game có đúng một đội nhà, một đội khách và một Arena; mọi GameSeat phải trỏ tới Seat qua Row→Section→Level thuộc đúng Arena đó. Mã hàng chỉ cần duy nhất trong section, số ghế trong row; Seat ID duy nhất toàn dataset. Không dùng số hàng/ghế đơn lẻ làm ID. Đây là quan hệ logic, không yêu cầu DB. Validator kiểm tra hierarchy/index ranges và shape; không so sánh mọi cặp ghế O(N²) trên dataset lớn, dùng index để tìm cặp có AABB giao nhau rồi kiểm tra shape.

**State giao diện:** ArenaUiState có datasetVersion, gameId, loading/error, tập ID selection, focusedLevelId/SectionId/RowId/SeatId, FilterState và sheetState. LOD không thay ID hoặc selection. Các item sheet/count/tổng/aggregate theo khu được suy ra; cache có version và cập nhật theo thay đổi, không quét N ghế mỗi frame hoặc giữ tổng như nguồn độc lập. Dùng bảng ID→index và tập index/bitset cho filter/availability khi phù hợp; geometry không lặp lại trong mỗi UiState.

ViewportState lưu centerWorldX/Y, zoomRatioSoVoiFit và cờ đã tương tác. Matrix, tx/ty pixel, pointer và tiến trình animation không serialize. Selection scoped theo game; P1 đổi trận cần chính sách riêng, không mang ID chọn sang inventory trận khác.

**Mô hình P1, chưa tạo ở P0:** Order(id, gameId, createdAt, status, total, currency); OrderItem(orderId, seatId, section/row/number snapshot, unitPrice). Game 1–N Order, Order 1–N OrderItem; Seat có thể xuất hiện trong các item lịch sử. Đơn CONFIRMED chỉ có hiệu lực demo; snapshot giữ giá/tên lúc xác nhận. Không hứa chống bán trùng nhiều thiết bị. Account/Payment/vé xác thực cần scope khác.

## 4. Dữ liệu và bố cục sân

### 4.1. Toàn arena và các tầng khán đài

Sân bóng ở tâm, các vành khán đài nhiều tầng bao quanh, chia nhiều section; mỗi section có nhiều hàng và mỗi hàng có từng ghế. Hỗ trợ số hàng/ghế không đồng đều, lối đi hướng tâm/vành đai, khoảng trống và khu không bán. Không khóa thành 4 block hoặc hàng loạt lưới 5×10.

Đề xuất sơ đồ 2D dạng **trải các tầng thành vành đồng tâm không chồng hình học**: Lower/Club/Upper là các vành riêng, nhãn chỉ rõ đây là sơ đồ mô phỏng đã trải tầng, không phải mặt cắt/3D hay kích thước kiến trúc thực. Ghế các tầng không nằm trùng một tọa độ để hit-test chọn nhầm tầng. Bộ lọc tầng giữ nguyên vị trí world, làm mờ tầng khác; không tháo/rải lại geometry khi lọc. Standing là loại vị trí/khu nếu cần cho profile đề, không phải một tầng bắt buộc của mọi arena thực.

WorldBounds lấy từ schema/hình học toàn arena, không cố định 1600×1000. Đơn vị world là logic, không ngụ ý mét; tọa độ được chuẩn hóa về gốc phù hợp trước dựng camera. Seat có rect hoặc tứ giác theo hướng hàng/cung, không chồng shape ghế khác; AABB có thể giao nhau với ghế xoay nên cần exact shape test. Sức chứa mô phỏng = tổng vị trí của dataset; ghi riêng số ghế ngồi, chỗ đứng và số vị trí đang mở bán của trận.

### 4.2. Profile dữ liệu và tăng quy mô

| Profile dự kiến | Mục đích | Giới hạn diễn giải |
| --- | --- | --- |
| `rubric-small`: 150–250, ban đầu 200 | Test nhanh và minh họa số đối tượng đề; có đủ level/section/row/seat trên toàn arena thu nhỏ | Không phải giới hạn sản phẩm, không phải một section bị cắt ra |
| `scale-1k`, `scale-10k` | Kiểm tra hierarchy, index, culling, LOD và state | Quy mô test tổng hợp, chưa có số đo |
| `full-arena-30k` | Dataset tham chiếu thiết kế với hàng chục nghìn ghế | 30.000 chỉ là tham số mô phỏng, không phải sức chứa một sân NBA thật |
| `stress-50k` | Kiểm tra dư địa bộ nhớ/tương tác và dữ liệu lệch mật độ | 50.000 là mức stress đề xuất, không phải giới hạn schema hoặc tuyên bố hỗ trợ đã được chứng minh |

Các profile dùng cùng ID scheme/schema/compiler/index/renderer/reducer; không tạo engine riêng cho bản nhỏ. Chỉ đổi manifest, seed/mật độ và cấu hình LOD. Không có `MAX_SEATS=200` hoặc kích thước mảng cố định theo số ghế mẫu. Tổng số ghế lớn không có nghĩa vẽ mọi ghế mỗi frame; mục 6 quy định mức chi tiết và vùng nhìn thấy.

Repository offline đọc manifest JSON nhỏ và dữ liệu section/row hoặc tham số sinh ghế xác định; generator mock Java tạo ID/geometry/inventory theo seed/version, không viết tay hàng chục nghìn object trong mã. Parse/generate/validate/index trên worker, cho phép hủy; publish cùng một version nguyên tử, bỏ kết quả cũ khi đổi dataset. Chuẩn bị aggregate trước frame; không I/O trong onDraw. Tất cả ghế vẫn có model/ID riêng dù LOD đang gộp hình vẽ. Giá/trạng thái có seed cố định, không sinh lại khi xoay.

Giá mock theo cấu hình khu/hàng/trận, dùng USD cent như đề xuất trước; không tự thêm phí/thuế. Giá trị seed, hệ số giá, sức chứa và nhãn đều phải có metadata MOCK. Sơ đồ toàn arena không được gán cho một sân có thật nếu chưa có nguồn đối chiếu.

| Nhóm dữ liệu | Chiến lược | Cách thể hiện |
| --- | --- | --- |
| Quy tắc/rubric | Thực tế từ PDF đề thi | Có trang/mục trong REQUIREMENTS |
| Đội, trận, thời gian, arena P0 | Fixture demo cố định, tên đội demo; NBA là chủ đề | Banner “Dữ liệu mô phỏng — không bán vé thật” |
| Sơ đồ/tầng/section/hàng/ghế/sức chứa/giá/trạng thái | Tự thiết kế, generator mock có version/seed cho nhiều profile | Ghi “Sơ đồ mô phỏng”, “Sức chứa mô phỏng”, “Giá/trạng thái giả lập”; không gọi là số liệu NBA chính thức |
| Selection và tổng giá | Kết quả thật của thao tác trên fixture | “Tổng giá mô phỏng”; chọn không đồng nghĩa giữ chỗ |
| Metadata/asset NBA thực tế nếu duyệt | Nguồn chính thức, URL và thời điểm thu thập; dữ liệu lịch sử gắn REAL_SNAPSHOT, không giả live | Chỉ trường có nguồn là thật; có tên sân/capacity công bố không chứng minh sơ đồ ghế hoặc inventory thật; không tự gắn LIVE_VERIFIED |

Khuyến nghị offline, không API key/quyền mạng mới/DB giao dịch. Manifest JSON + generator xác định là phương án dữ liệu mới để mở rộng; vẫn cần review schema/seed cụ thể. Không có nguồn NBA trực tiếp hoặc sức chứa/sơ đồ sân thật nào đã được chọn/kiểm chứng trong phase này. Banner mô phỏng luôn hiện, kể cả nếu sau này có dùng tên/logo thật.

## 5. Màn hình và luồng điều hướng

P0 có một màn chính và các panel:

```mermaid
flowchart TD
    LAUNCH[Mở app] --> ARENA[Sơ đồ sân và nhãn demo]
    ARENA -->|Lọc| FILTER[Panel bộ lọc]
    FILTER -->|Áp dụng / Xóa lọc| ARENA
    ARENA -->|Chú thích| LEGEND[Panel Legend]
    LEGEND --> ARENA
    ARENA -->|Tap vị trí| SMALL[Sheet tóm tắt]
    SMALL -->|Kéo / nút mở| LARGE[Sheet chi tiết cuộn]
    LARGE -->|Focus / Xóa item| ARENA
    ARENA -->|Reset View| ARENA
```

- Header: trận demo ngắn, nhãn mô phỏng; công cụ: filter, chip điều kiện/số kết quả, legend, reset. Không thêm splash/login/màn trận chặn sơ đồ.
- Giữa: một ArenaView; fit mặc định đủ sân, không bị sheet thu gọn che ghế. Legend mini cho trạng thái, panel giải thích đầy đủ loại khu, focus và biểu hiện ngoài lọc.
- Luồng không gian: toàn arena → chọn tầng/section để focus → hàng → từng ghế khi đủ zoom. Tap mức tổng quan chỉ mở thông tin aggregate và zoom/focus khu, không chọn ngẫu nhiên một ghế chưa được vẽ riêng. Có breadcrumb tên tầng/section/hàng; reset trở về toàn arena mà giữ selection/filter.
- Filter tầng và section là công cụ P0 để điều hướng arena lớn; section phải thuộc tầng đang lọc. Sheet luôn ghi mã ghế kèm tầng/section/hàng để phân biệt các số ghế lặp nhau.
- Sheet thu gọn: số vị trí và tổng mock; mã focus nếu có; n=0 có hướng dẫn. Tap BOOKED/LOCKED chỉ xem lý do, không tăng selection.
- Sheet mở rộng: thông tin focus riêng + RecyclerView selection. Hàng có khu/hàng/vị trí/giá, tap để focus, nút xóa riêng; cuộn nội dung sheet, không scroll toàn màn sơ đồ.
- Persistent BottomSheetBehavior trong CoordinatorLayout; không dialog che toàn sân. Chiều cao mở rộng đề xuất tối đa 55% phần nội dung dọc, 50% ngang; peek height thích ứng. Màn thấp rút gọn header, cho danh sách cuộn, giữ vùng tương tác.
- Back đóng panel trước, rồi thu gọn sheet nếu mở rộng, sau đó theo điều hướng Android. Không âm thầm xóa lựa chọn. Reset và xóa lọc là hai thao tác khác nhau.

Trực quan dự kiến: AVAILABLE nền sáng/viền rõ, SELECTED xanh + tick, BOOKED xám + gạch, LOCKED tối + khóa; focus có viền ngoài khác selected. Màu phải kiểm tra tương phản sau; chưa có mockup được review.

Chất lượng thêm: contentDescription cho nút/chip/sheet, zoom +/- hỗ trợ. Virtual accessibility nodes được cung cấp theo mức chi tiết và tập đang thấy (section/row/seat), không tạo sẵn hàng chục nghìn node hoặc View. Ghế ngoài viewport có thể truy cập qua hierarchy hoặc sheet focus. Cần kiểm chứng helper/TalkBack sau, không khẳng định đây là tiêu chí chấm riêng của PDF.

## 6. ArenaView, Canvas, Matrix, pan/zoom và hit-testing

### 6.0. Hợp đồng bắt buộc theo AGENTS mục 3

**Ranh giới lớp vẽ:** `ArenaView.onDraw(Canvas canvas)` nhận Canvas do Android truyền vào và gọi ArenaRenderer với chính Canvas đó. Renderer là lớp hỗ trợ vẽ Java, không phải View thứ hai, không sở hữu một bề mặt sơ đồ khác. Toàn bộ sân/court, khán đài, phân khu, lối đi, nhãn, vị trí và trạng thái được tự vẽ bằng lệnh Canvas/Paint trên bề mặt này. Không dùng ảnh PNG/JPG/SVG đã chứa sơ đồ ghế để thay hình học tự vẽ; không WebView, Button hay View con cho từng vị trí. Không raster hóa sẵn cả sơ đồ thành ảnh để thay việc vẽ các đối tượng tương tác.

**Ranh giới trạng thái:** ArenaView nhận snapshot đọc từ ArenaViewModel qua Activity, phát action tap/focus/viewport qua listener; reducer cập nhật selection trong ViewModel. Renderer không sửa model/selection, không quan sát LiveData và không truy cập repository. Legend, màu trạng thái, filter và sheet cùng suy ra từ một snapshot và một quy tắc ánh xạ trạng thái hiển thị. Palette từ resource dùng chung, không hardcode một bộ màu khác trong legend.

**Ranh giới tọa độ:** ViewportTransform là nơi duy nhất quản lý M và inverse, được dùng cho pan/zoom, vẽ và đổi điểm touch. SeatHitTester nhận điểm world + hình học/quy tắc filter, độc lập Canvas/ViewModel. Các công thức scalar ở mục 6.1 giải thích và làm oracle kiểm thử; đường xử lý touch thực tế bắt buộc dùng inverse Matrix hiện hành, không tạo nhánh công thức riêng trong Activity hoặc renderer.

| Quy định hiện hành | Đối chiếu thiết kế | Kết luận ở mức tài liệu |
| --- | --- | --- |
| Java và Android Views/XML; không Kotlin/Compose/WebView | Mục 2 và hợp đồng ArenaView ở đây | Đã ghi rõ ràng buộc công nghệ |
| ArenaView kế thừa View, Canvas/Paint trong onDraw | Mục 2, 6.0 | Đã bổ sung khai báo kế thừa và điểm vào vẽ; bản trước mô tả còn ngầm định |
| Tự vẽ sân/khán đài/khu ghế/trạng thái | Mục 4, 6.0, 6.5 | Đã nêu đủ thành phần và một bề mặt vẽ |
| Không ảnh sơ đồ tĩnh hoặc từng View/Button thay ghế | Mục 6.0 và test REQ-01 | Đã bổ sung cấm ảnh sơ đồ; UI công cụ ngoài sơ đồ vẫn là View thông thường |
| Matrix cho pan/zoom, inverse Matrix cho touch | Mục 6.1–6.4 | Đã có; làm rõ một đường biến đổi dùng chung |
| Kéo, pinch, chọn; tách screen/world | Mục 6.1–6.4 | Đã có công thức, biên và xử lý pointer |
| Đồng bộ trạng thái/màu/legend/filter | Mục 6.0 và 7 | Đã làm rõ quy tắc hiển thị và palette dùng chung |
| Selection do ViewModel quản lý | Mục 2 và 7 | Đã có; View/renderer không giữ selection riêng |
| Giữ trạng thái cần thiết khi xoay | Mục 7 | Đã có chiến lược selection/filter/focus/sheet/camera |
| Tách logic tọa độ và hit-test để test độc lập | Package mục 2, mục 6.0 và 8 | Đã có ViewportTransform/SeatHitTester và phân tầng test |
| Viết test tọa độ, hit-test và selection | Mục 8 | Bắt buộc khi triển khai, không phải test tùy chọn |
| Không tạo object thừa/I/O/nghiệp vụ nặng trong onDraw | Mục 6.5 | Đã có; cache chuẩn bị ngoài vòng vẽ, phải đo sau |

Đây là đối chiếu ở mức thiết kế, **chưa có implementation/test**. Đã bổ sung phạm vi người dùng xác nhận: toàn arena nhiều tầng, không giới hạn 200 ghế; renderer phải dùng culling/LOD và hit-test dùng index theo mục 6.4–6.6. Một Canvas không có nghĩa phát lệnh vẽ mọi ghế ở mọi mức zoom. Profile nhỏ không thay hợp đồng quy mô cuối. JSON manifest/generator là đề xuất dữ liệu, không phải yêu cầu JSON bắt buộc từ AGENTS.

### 6.1. Hệ tọa độ và phép biến đổi

World gốc trái trên (0,0), x sang phải, y xuống dưới. Screen trong công thức là pixel **cục bộ ArenaView**; MotionEvent.getX/getY cùng hệ. Nếu nhận tọa độ root/window phải đổi về View; không cộng insets/offset toolbar hai lần.

Chỉ scale đồng nhất và translate, không rotation/skew:

```text
M = [ s  0  tx ]       screenX = s * worldX + tx
    [ 0  s  ty ]       screenY = s * worldY + ty
    [ 0  0   1 ]

worldX = (screenX - tx) / s
worldY = (screenY - ty) / s        s > 0
```

ViewportTransform giữ s/tx/ty, cập nhật M và inverse sau mỗi đổi camera. Đề xuất điền matrix values từ scalar để tránh nhầm pre/post concat. Vẽ save → clip vùng nhìn thấy → concat(M) → vẽ world → restore; touch dùng inverse.mapPoints. Nếu invert thất bại, không dùng inverse cũ: bỏ hit-test, phục hồi camera hợp lệ và log debug.

Lý do: vẽ và chạm dùng cùng một phép đổi; Activity/sheet không có công thức khác. API [Matrix.invert/mapPoints](https://developer.android.com/reference/android/graphics/Matrix) đã được tra cứu, chưa có implementation được kiểm thử.

### 6.2. Fit view và biên pan

U=[L,T,R,B] là phần ArenaView nhìn thấy/tương tác được, đã trừ padding/insets và phần sheet che; mọi giá trị cùng hệ View-local. Sheet/toolbar giữ touch vùng của chúng. Chỉ bắt đầu tap/pinch ở U; trì hoãn camera khi U chưa có kích thước dương.

```text
Uw = R-L; Uh = B-T; world bounds = [0,W] × [0,H]
sFit = 0.9 * min(Uw/W, Uh/H)
sMin = sFit
sDetail = targetSeatDp * density / minValidatedSeatWorldWidth
sMax = max(sFit, sDetail)
txDefault = (L+R-sFit*W)/2
tyDefault = (T+B-sFit*H)/2
```

Lề 0.9 và targetSeatDp dự kiến 24dp là thông số cần thử thiết bị. `minValidatedSeatWorldWidth` là cạnh ngắn nhỏ nhất đã qua validator, phải >0 và trong giới hạn cấu hình; không để dữ liệu lỗi gây scale vô hạn. Không dùng trần cố định 8×fit vì toàn arena lớn có thể cần zoom sâu hơn mới chọn được ghế. LOD dùng kích thước chiếu theo dp, không dựa riêng vào ratio. Fit lấy world bounds toàn arena, tính lại khi layout/U ổn định đổi; reset về toàn arena trong U hiện tại.

Với từng trục: nếu `s*W >= Uw`, clamp `tx` trong `[R-s*W, L]`; nếu nhỏ hơn thì đặt `tx=(L+R-s*W)/2`. Làm tương tự ty với H/Uh/T/B. Nội dung nhỏ hơn U được căn giữa; nội dung lớn không kéo ra khỏi U. Clamp sau pan, zoom, focus, reset và resize.

Sheet onSlide cập nhật U và clamp camera, không fit liên tục. Khi sheet dừng, cập nhật sFit/giới hạn và clamp scale một lần. Nếu đổi layout khi chưa tương tác thì fit; đã tương tác thì giữ tâm world/ratio theo mục 7. Focus luôn đặt vị trí vào U hiện tại, không sau sheet.

### 6.3. Gesture

- Một ngón: GestureDetector phân biệt drag/tap bằng touch slop; delta pixel cộng vào tx/ty rồi clamp. Không xử lý đồng thời pan một ngón và scale cho cùng event.
- Hai ngón: ScaleGestureDetector cho k và tâm; fOld là tâm trước, fNew là tâm hiện tại; lần đầu fOld=fNew:

```text
pWorld = inverse(Mold) * fOld
sNew = clamp(sOld * k, sMin, sMax)
tNew = fNew - sNew * pWorld       theo cả x và y
```

Điểm world dưới tâm cũ đến tâm mới; nếu tâm không đổi thì `tNew=f-(sNew/sOld)*(f-tOld)`. Dùng ratio thực sau clamp, không dùng k nguyên khi chạm min/max. Clamp pan sau cùng. Ở biên, ưu tiên giới hạn nội dung nên anchor có thể dịch; phải kiểm tra riêng thay vì tuyên bố giữ tâm tuyệt đối trong mọi trường hợp.

- ArenaView giữ gesture bắt đầu trong U cho đến kết thúc; parent không giành pan/scale. Sheet nhận drag từ vùng sheet/handle, có nested scroll riêng.
- Khi POINTER_UP đổi activePointerId, đặt lại mốc ngón còn lại; không coi index là ID cố định. Pinch xong không sinh tap dư. CANCEL hủy gesture/tap pending.
- User chạm khi animation chạy: dừng animator tại frame hiện tại rồi xử lý; giải phóng animator/listener khi detach. Không cần fling/double-tap ở P0. Zoom +/- dùng cùng công thức với anchor giữa U.

Tham khảo [Android drag and scale](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale). Công thức và quy tắc ở đây vẫn cần kiểm chứng trong implementation.

### 6.4. Hit-testing và focus

1. Chỉ nhận single tap hợp lệ trong U, không phải drag/pinch/CANCEL.
2. Đổi điểm bằng inverse của M đang vẽ frame hiện tại; dừng animation ở frame đó trước khi test.
3. Query phân cấp theo LOD: level/section AABB → row/group → cell của index section chứa điểm world. Không duyệt tuyến tính toàn arena mỗi tap. Trong mức tổng quan tap section/row để focus; chỉ mức vẽ từng ghế mới gửi action chọn ghế.
4. Lọc candidate theo tầng/section/filter, kiểm AABB rồi exact shape. Rect dùng `left <= x < right`, `top <= y < bottom`; tứ giác dùng point-in-convex-polygon theo quy ước cạnh nhất quán đã test. AABB ghế xoay không phải hitbox cuối. Lối đi/court trả về không hit; không thêm epsilon để lấn vào ghế khác.
5. AVAILABLE toggle selection; BOOKED/LOCKED chỉ focus/thông tin; tap trống không xóa selection.

Shape vẽ và shape hit cùng geometry version. Nếu thêm shape khác phải có hàm contains tương ứng; không giải quyết nhập nhằng bằng “ghế gần nhất”. Index cần trả đủ ứng viên khi ghế cắt nhiều cell; khử trùng bằng index/generation counter. Focus ghế từ sheet dùng ID lookup để xác định tầng/section/row và tăng tới LOD ghế, đưa vào U, clamp/highlight; không toggle hoặc phụ thuộc ghế đó đã có trong visible set cũ.

### 6.5. Reset và hiệu năng

Reset animate s/tx/ty từ camera hiện tại về fit hiện tại trong khoảng 250 ms đề xuất, clamp từng frame; không thay filter/selection/focus. Tôn trọng animation scale hệ thống, có thể kết thúc tức thì; chạm mới hủy animation. Không serialize tiến trình animation.

- ArenaRenderer nhận model/geometry/state đã chuẩn bị; không I/O, parse fixture, tính tiền hoặc tạo bitmap trong onDraw.
- Tạo/tái dùng Paint, Path court, RectF, Matrix, float[2], text metrics bên ngoài vòng vẽ. Cache geometry/text khi model hoặc kích thước liên quan đổi.
- Một Canvas vẽ toàn arena theo mức chi tiết và candidate đang thấy; UI sheet/chip ngoài sơ đồ là View thường. Không phát hàng chục nghìn lệnh text/path chỉ vì dataset có bấy nhiêu ghế.
- Chỉ invalidate khi state/filter/camera đổi; gesture/animation dùng postInvalidateOnAnimation. Không requestLayout theo từng pixel pan, không timer redraw khi đứng yên. Culling/LOD/index là trách nhiệm bắt buộc của thiết kế quy mô lớn, không còn là tối ưu tùy chọn.
- Cache hình học/nhãn tĩnh theo section + geometryVersion; overlay status/selection dùng stateVersion. Không gộp màu trạng thái thay đổi vào cache không có cơ chế invalidation. Không bitmap hóa toàn sơ đồ hoặc mặc định force software layer để xử lý lỗi hiệu năng.
- Nhãn được giới hạn theo diện tích chiếu và ưu tiên, không vẽ text từng ghế ở overview. Paint/Path/Rect/buffer dùng lại theo nhóm, không cấp phát mỗi frame. Bộ nhớ geometry/index O(N); công việc frame theo tập ứng viên đang thấy và LOD, không hứa worst-case index là O(1).

### 6.6. Culling, LOD và spatial index cho arena lớn

**Visible query:** inverse M ánh xạ bốn góc U về world; lấy AABB bảo thủ cộng lề cho stroke/highlight. Query hierarchy/index theo LOD: overview dừng ở section, mức hàng dừng ở row; chỉ mức ghế mới query các cell/seat candidate. Không lấy toàn N ghế rồi bỏ ở bước vẽ. Sau query vẫn clip Canvas theo U. `clipRect` đơn lẻ không thay thế CPU culling vì vẫn có thể phát lệnh cho toàn dataset. Tính visible candidates vào buffer tái dùng khi camera/LOD/filter đổi; không xây lại index khi pan hoặc chọn ghế.

**Index đề xuất:** hierarchy Arena→Level→Section→Row; mỗi section có uniform grid thưa chứa seat index theo AABB, cell size theo pitch ghế đã validate. Bỏ section/row ngoài vùng nhìn trước khi query cell. Ghế giao nhiều cell phải ghi đủ membership; query không được bỏ ghế sát biên. Build trên worker một lần theo geometryVersion, không rebuild vì màu/selection. Dataset lệch mật độ hoặc section lớn có thể làm grid kém hiệu quả; ghi số candidate thật rồi cân nhắc quadtree/R-tree sau. Kiểm thử so với bộ duyệt brute-force độc lập để phát hiện false negative, không dùng brute-force làm production path.

| LOD | Hình vẽ và thông tin | Tap |
| --- | --- | --- |
| Toàn arena | Court, vành tầng, polygon section, nhãn và aggregate capacity/available/selected mock | Focus/zoom section; không chọn một ghế đang ẩn |
| Section/hàng | Chi tiết lối đi, dải hàng, nhãn hàng, aggregate theo hàng | Focus hàng hoặc tiếp tục zoom |
| Ghế | Chỉ seat shape trong viewport, trạng thái/selection, nhãn khi đủ chỗ | Exact hit-test chọn/bỏ từng ghế |

Ngưỡng dự kiến dựa cạnh ghế chiếu lên màn: <6dp overview, 6–16dp hàng, >=16dp từng ghế; label số ghế khi >=28dp. Dùng hysteresis khoảng 15% để tránh nhấp nháy ở biên LOD; phải thử lại mật độ/màn hình. Threshold là cấu hình, không gán số ghế tối đa. Các section khác tỷ lệ có thể dùng projected size riêng. Khi full-arena ở overview, các ghế vẫn tồn tại trong model/index, chỉ được gộp cách vẽ; count aggregate không được trình bày như tất cả ghế trong khu có cùng trạng thái.

Profile rubric-small có cấu hình vẽ từng ghế để có thể đối chiếu đủ 150–250 đối tượng; cùng renderer/index, không fork engine. Tại zoom quá nhỏ không tự chọn ghế không đủ chính xác; hướng dẫn zoom/focus. Không dùng số polygon section ở LOD thấp để tuyên bố đã vẽ đủ số ghế rubric.

**State/filter ở quy mô lớn:** tính bitset khớp tầng/section, aggregate và text ngoài onDraw; filter đổi có thể chạy worker với version token, chỉ publish kết quả mới nhất. Action selection vẫn trên main thread; cập nhật count/giá và aggregate liên quan theo delta, không quét toàn N mỗi tap. Kết quả worker chỉ mang mask/aggregate nền theo geometryVersion + filterRevision; main thread ghép với selection mới nhất, không ghi đè toàn UiState bằng snapshot cũ. Cache là kết quả suy ra có thể kiểm tra lại, không nguồn sự thật mới. RecyclerView chỉ bind các hàng selection cần thấy; không dựng View/node cho mọi ghế. Khi xử lý background, publish geometry/index/filter đồng bộ version để không vẽ tập mới nhưng hit index cũ.

**Kế hoạch đo:** 200/1.000/10.000/30.000/50.000 ghế, cùng thao tác overview→zoom sâu→pan qua biên section→filter→select. Ghi frame time p50/p95/p99, jank, thời gian query/candidate/exact test, số đối tượng thực vẽ, thời gian load/build index và heap/GC. Mục tiêu tham chiếu màn 60 Hz là khoảng 16.7ms/frame, chưa là cam kết đạt trên mọi thiết bị. Phải nêu thiết bị/OS/GPU và đo trên phần cứng thật trước kết luận hỗ trợ quy mô; chưa có benchmark nào được chạy.

Tham khảo hướng dẫn [tối ưu Custom View](https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view) về giảm allocation/invalidate/layout thừa và [hardware acceleration](https://developer.android.com/develop/ui/views/graphics/hardware-accel) về kiểm tra custom drawing trên thiết bị. LOD/grid là lựa chọn thiết kế của dự án, không phải bằng chứng Android tự xử lý 50.000 ghế đạt hiệu năng.

Hướng dẫn [Android custom drawing](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing) hỗ trợ cách tách chuẩn bị đồ họa khỏi onDraw; phải profile implementation sau.

## 7. Quản lý trạng thái chọn ghế và đồng bộ

`selectedSeatIds` là tập giữ thứ tự lần chọn, không trùng; mỗi ID phải tồn tại trong trận/dataset và baseStatus AVAILABLE. SELECTED là overlay hiển thị; bỏ chọn xóa ID, không đổi baseStatus. Không viết SELECTED vào repository.

| Action | Thay đổi | Kết quả Canvas/Sheet |
| --- | --- | --- |
| Tap AVAILABLE | Toggle ID và focus | Tick/màu, danh sách/count/tổng từ một snapshot |
| Tap đã chọn | Xóa ID khỏi tập, có thể giữ focus | Trở về AVAILABLE, xóa item; ghế khác không đổi |
| Tap BOOKED/LOCKED | Chỉ focus | Hiện lý do; không tăng count/giá |
| Xóa item sheet | Xóa idempotent; clear focus nếu cùng ID | Màu gốc và tổng đúng; nút xóa không kích hoạt tap hàng |
| Tap item sheet | Chỉ focus + camera | Highlight riêng; không toggle/nhân đôi item |
| Filter tầng/section | Đổi FilterState, kiểm tra quan hệ tầng–section | Ngoài lọc làm mờ, không hit-test; giữ selection ở mọi tầng |
| Xóa lọc | Filter ALL | Hiện đủ; giữ selection/camera |
| Reset | Chỉ camera | Giữ filter/selection/focus/sheet |
| Rotation | Phục hồi state, thích ứng camera | Không mất selection, không trùng item |

P0 filter theo tầng và section thuộc tầng đó hoặc ALL; đổi tầng phải xóa điều kiện section không còn hợp lệ và thể hiện chip mới. Filter không thay hierarchy/world coordinates. Hiện số khớp, nút xóa và phân biệt “không có vị trí khớp” với “không còn ghế trống”. Empty state dùng fixture không kết quả. Lọc thêm trạng thái/loại giá và search vẫn là P1; điều hướng tầng/section phục vụ toàn arena đã thuộc P0.

Sheet luôn chứa toàn bộ selection và tổng toàn bộ, kèm “x vị trí ngoài bộ lọc”; hàng không khớp có nhãn. Tap hàng đó mang nghĩa **“Hiện vị trí — xóa bộ lọc”**, xóa lọc kèm thông báo, chip về ALL rồi focus vào U. Tránh focus một ghế vẫn bị làm mờ. Phương án giữ filter và yêu cầu xóa thủ công nằm ở mục 10.

Không tự đặt giới hạn số vé vì đề không có; list/tổng phải hỗ trợ mọi AVAILABLE. Kiểm tra cộng long và currency ở boundary dữ liệu. Action nghiệp vụ tuần tự main thread; listener không tự sửa selection rồi phát ngược gây vòng lặp.

### Rotation và phục hồi

- ViewModel giữ state qua configuration change. SavedStateHandle chỉ lưu ID/enum/giá trị nhỏ, không geometry/Bitmap/View.
- Với hàng chục nghìn ghế, không nhét dataset/index hoặc selection lớn không giới hạn vào Bundle. Đề xuất selection nhỏ lưu compact cùng ngân sách payload được đo; selection vượt ngân sách lưu snapshot phiên vào app-private file trên worker, SavedStateHandle chỉ giữ token/version/revision. Ghi file nguyên tử, restore kiểm tra datasetVersion và revision; file thiếu/lỗi phải thông báo thay vì giả phục hồi đủ. Đây là lưu phiên tạm, không lịch sử mua hoặc DB giao dịch. Cần chốt ngân sách/chiến lược trước triển khai, kiểm tra trường hợp process bị dừng trước khi ghi xong; rotation thông thường vẫn giữ đầy đủ selection trong ViewModel, không chờ I/O.
- Lưu camera dưới dạng tâm world và zoomRatioSoVoiFit; sau layout tính sFit mới, dựng camera theo ratio/tâm rồi clamp. Chưa tương tác thì fit toàn sân; không dùng nguyên tx/ty pixel dọc cho ngang.
- DatasetVersion/gameId phải khớp khi restore; lọc ID thiếu/không AVAILABLE và thông báo nếu phải điều chỉnh. Fixture không đổi ngẫu nhiên khi recreate.
- Sheet chỉ lưu COLLAPSED/EXPANDED, không lưu DRAGGING/SETTLING. Đồng bộ camera live trước lưu lifecycle; listener/animator cũ không giữ Activity.
- Saved state phục hồi task theo lifecycle, không hứa lưu vĩnh viễn sau force-stop/xóa task/cài lại. Lưu qua lần khởi động mới hoàn toàn là mở rộng.

Tham chiếu [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate) và [BottomSheetBehavior](https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetBehavior); chưa có kiểm chứng lifecycle/chiều cao sheet trong app thực tế.

## 8. Kế hoạch kiểm thử ban đầu — kết quả hiện tại ở PROGRESS

Đây là kế hoạch, không phải test report. Không viết hoặc chạy test ứng dụng trong Phase 1. Theo AGENTS mục 3, test phép biến đổi tọa độ, hit-testing và logic selection là **bắt buộc ở phase triển khai tương ứng**, không chờ đến bàn giao mới bổ sung. Unit test Java kiểm tra thuật toán/hình học độc lập; test Android kiểm tra Matrix/inverse thực và đường touch-to-world được ArenaView sử dụng. Test scalar không thay thế test tích hợp Matrix.

| Phạm vi | Kiểm tra đề xuất | Điều kiện đạt / bằng chứng cần lưu |
| --- | --- | --- |
| REQ-01–02 và toàn arena, model/layout | Fixture 200 đa tầng và scale 1k/10k/30k/50k; validate ID/FK/row ranges, shape trong bounds, không chồng, index nhất quán | Không có trần 200; đủ level/section/row/seat/court/lối đi; số lượng mô phỏng đúng manifest; hình fit toàn arena dọc/ngang |
| REQ-01, Canvas | Mã nguồn + Layout Inspector; kiểm tra tài nguyên/đường vẽ | ArenaView extends View, onDraw dùng Canvas/Paint; renderer nhận cùng Canvas; sân/khán đài/ghế/trạng thái tự vẽ, không ảnh sơ đồ tĩnh/WebView/child View thay đối tượng |
| REQ-03, selection | JVM reducer: toggle A hai lần, A+B, tap khóa/đã đặt, remove lặp | Không trùng, B không đổi, đúng trạng thái/count/giá |
| REQ-04, pan | Unit clamp + gesture emulator | Bốn biên, nội dung lớn/nhỏ, U đổi vì sheet; sơ đồ không biến mất |
| REQ-05, zoom | Unit anchor + pinch thật | Tâm cố định/dịch, min/max, 2→1 ngón không giật/tap dư, biên clamp đúng |
| REQ-06, Matrix | JVM round-trip nhiều s/tx/ty; Android so Matrix với công thức | Sai số theo độ lớn, dự kiến <=0.01 world unit cho fixture; xử lý invert lỗi |
| REQ-06, hit-test | Tâm/biên rect và ghế xoay, AABB giao nhưng shape không giao, cell/section boundaries; lặp pan/zoom ở nhiều tầng | Đúng ID hoặc không hit; không nhầm tầng/section; touch sheet không xuyên Canvas; index không bỏ ghế cắt nhiều cell |
| Index và LOD | So query point/rect với oracle brute-force; thao tác qua ngưỡng LOD/culling/visible bounds và filter; kiểm tra cùng version | Không false negative; không chọn ghế ẩn ở overview; không mất selection khi LOD/tầng đổi; query không quét toàn arena mỗi tap |
| REQ-07, legend | UI/manual font scale/tương phản; đối chiếu snapshot render | Đủ bốn trạng thái, loại khu, focus/filter, ít che sân; đổi selection/filter không làm màu Canvas khác ý nghĩa legend |
| REQ-08, filter | ALL→Level A/Section X→Level B→ALL; fixture rỗng, selected ngoài lọc; đổi filter nhanh khi worker chưa xong | Section luôn thuộc tầng hợp lệ, không publish kết quả cũ; chip/count/empty đúng, selection/tổng giữ |
| REQ-09, reset | Pan/zoom + chọn/lọc rồi reset; ngắt animation bằng chạm | Fit U, mượt, không mất selection/filter, không crash |
| REQ-10–11, sheet | Chọn Canvas→sheet, xóa/focus hàng, 100+ mục để cuộn | Đồng bộ ngay, không trùng/xóa rồi chọn lại; còn vùng sân ở hai hướng |
| REQ-11, focus ngoài lọc | Chọn A, lọc bỏ A rồi tap hàng A | Thông báo xóa lọc, ALL, A trong U, chỉ một A trong selection |
| REQ-12, rotation | ActivityScenario.recreate và xoay emulator lúc chọn/lọc/zoom/sheet mở | ID/count/tổng/filter/focus đúng, camera hợp lệ; recreate không chứng minh process death |
| REQ-12, restore task | SavedStateHandle nhỏ/lớn; snapshot file thiếu/lỗi/cũ/đang ghi; background→kết thúc process→mở task; fixture version khác | Không vượt ngân sách saved payload; phục hồi đúng revision khả dụng hoặc báo rõ hạn chế; stale ID loại có thông báo; không đồng nhất force-stop |
| REQ-12/14, biên UI | Dọc/ngang, màn nhỏ/font lớn, tap nhanh, CANCEL/POINTER_UP, resize | Không che toàn Canvas/crash/nhảy camera/listener cũ; ghi cấu hình thiết bị |
| REQ-13 và scale, hiệu năng | Profile 200/1k/10k/30k/50k, phân bố thường/lệch; viewport/LOD/filter/select/idle; worker load và đổi version | Đo frame p50/p95/p99, jank, query/candidate/drawn count, load/index time, heap/GC; idle không redraw liên tục; test nhỏ không chứng minh scale lớn |
| Nguồn dữ liệu và số lượng | Kiểm metadata UI: sơ đồ, capacity, ghế ngồi/chỗ đứng, giá và status; so tổng hierarchy/aggregate/selection | Toàn bộ mock ghi đúng nhãn, không dùng con số stress như sức chứa NBA thật; REAL_SNAPSHOT có nguồn/ngày, không giả live |
| REQ-14, build/runtime | Gradle Sync, assembleDebug, testDebugUnitTest, lintDebug; connected test khi có thiết bị | Lưu lệnh/kết quả và APK khởi chạy; test template chưa đủ bằng chứng |
| REQ-15–18, hồ sơ | History/remote khi có Git; README, video/link/checklist, AI log | Commit theo phần việc thật; video 1–5 phút đủ luồng; README có nhật ký/tóm tắt và link AI_LOG; người dùng tự xác nhận review |
| Chất lượng thêm | TalkBack, zoom +/-, animation scale=0 | Truy cập vị trí/nút, kết thúc animation đúng state; chưa có kết quả |

Lệnh PowerShell dự kiến sau khi triển khai: `./gradlew.bat assembleDebug`, `./gradlew.bat testDebugUnitTest lintDebug`, `./gradlew.bat connectedDebugAndroidTest`. Chưa có bằng chứng môi trường hiện tại build được. Nghiệm thu cuối cần runtime, không chỉ test logic hoặc ảnh.

## 9. Lý do và đánh đổi

| Lựa chọn | Lý do | Đánh đổi / phương án khác |
| --- | --- | --- |
| Toàn arena; fixture nhỏ chung pipeline | Đúng phạm vi người dùng: nhiều tầng/section/hàng/ghế, hỗ trợ quy mô lớn | Profile rubric nhỏ không thay sản phẩm cuối; cần làm rõ cách nộp với giảng viên |
| Một màn chính | Trực tiếp thể hiện Canvas, hợp một tuần | Nhiều trận/checkout tăng phạm vi ngoài rubric |
| Java/XML + ViewModel | Khớp project, state/rotation rõ | Cần xác minh thêm artifact lifecycle |
| Matrix/inverse + hierarchy/grid + LOD/culling | Giảm lượng draw/query theo vùng nhìn, vẫn chính xác đến ghế | Cần kiểm index bằng oracle và đo dataset lệch mật độ; không bảo đảm worst-case O(1) |
| Sơ đồ 2D trải tầng thành vành riêng | Nhìn toàn arena, tránh ghế nhiều tầng trùng tọa độ | Không phải bản vẽ kiến trúc thật/3D; phương án khác là chuyển tầng nhưng cần quy tắc tọa độ/focus riêng |
| Mock offline, không DB | Đề cho phép, demo tái lập | Không có dữ liệu thực hoặc lịch sử bền vững |
| Selection tách baseStatus | Bỏ chọn/filter/reset nhất quán | Cần reducer và derived state đúng |
| Checkout để P1 | Ưu tiên 50% Canvas/tọa độ | Tên Purchasing mới thể hiện phần chọn vị trí; có thể thêm xác nhận demo sau |

## 10. Các phương án cần người dùng quyết định

Phạm vi toàn arena/quy mô lớn đã được người dùng xác nhận trong tương tác mới. Các lựa chọn cụ thể dưới đây vẫn cần review; không coi việc làm rõ phạm vi là quyền bắt đầu implementation.

| Mã | Cần chốt | Khuyến nghị | Phương án khác / hệ quả |
| --- | --- | --- | --- |
| D-01 | Cấu hình toàn arena trong phạm vi đã chốt | Nhiều tầng/section/row/seat, 2D trải tầng; benchmark 1k→10k→30k và stress 50k | Không hỏi lại có mở rộng quá 200 hay không; cần chốt mật độ, tên tầng, Standing và hình học cụ thể. 30k/50k chỉ là cấu hình mock/test |
| D-02 | Mức “Purchasing” | P0 chọn + tổng mock; checkout ở P1 | Thêm xác nhận mua mock sớm: phát sinh Order, snapshot và luồng hậu xác nhận |
| D-03 | Tính thực tế/asset | Đội/sân demo, NBA là chủ đề, không logo ngoài | Metadata NBA thật có nguồn; vẫn ghi rõ trường nào mô phỏng |
| D-04 | Dữ liệu quy mô lớn và tiền | Manifest JSON + generator mock xác định theo seed/version; USD cent | Explicit JSON per-seat nếu có dữ liệu có nguồn/được duyệt; phải đo thời gian parse/heap. Không viết tay hàng chục nghìn khai báo Java |
| D-05 | Phạm vi màn/định danh trong công nghệ bắt buộc | Một Activity + ViewModel; giữ package hiện tại | Đổi ID hoặc nhiều màn tăng cấu hình/kiểm tra; Java/Views/XML, Custom View/Canvas và Matrix là ràng buộc AGENTS, không mở lại lựa chọn Kotlin/Compose/WebView |
| D-06 | Focus item ngoài filter | Giữ selection; tap hàng có nhãn xóa lọc + thông báo rồi focus | Giữ filter, yêu cầu xóa lọc thủ công |
| D-07 | Lịch và Git | Chốt ngày bắt đầu/hạn nộp; xác nhận dùng origin hiện có trước triển khai | Cuối phase thấy main chưa có commit, origin là m1hdat/nba-tickethub; AI không thực hiện khởi tạo |
| D-08 | Cách minh họa/nộp theo giới hạn rubric | Giữ full-arena và profile rubric-small cùng engine; xin xác nhận giảng viên | Không tự suy ra rubric cho phép vượt 250 hoặc chỉ đếm ghế trong viewport; không hạ thiết kế cuối về một khu 200 ghế |
| D-09 | Mốc hiệu năng và lưu phiên lớn | Chọn thiết bị tham chiếu, ngân sách frame/memory/saved payload và snapshot phiên | Chưa có benchmark để cam kết 30k/50k; không hứa lưu selection vô hạn trong Bundle |

Các bước tiếp theo ở [ROADMAP.md](ROADMAP.md). **Phạm vi triển khai hiện tại và điểm dừng được cập nhật ở mục 11; các phần chưa được giao tiếp tục là thiết kế.**


## 11. Phase 3A — preview nhiều sân, 2026-11-25

### Phạm vi người dùng giao và dữ liệu

Chỉ triển khai 3A: ba cấu hình arena, sinh hình học, Canvas tĩnh và màn preview launcher. Matrix/pan/pinch/Reset/LOD/culling thuộc 3B; inverse Matrix/hit-testing/highlight/debug ghế thuộc 3C. Selection/filter/Bottom Sheet/Booking không triển khai. Nút Reset bị vô hiệu hóa có nhãn 3B; thông tin ghế ghi rõ chờ 3C. Đây là cách chia nhỏ do người dùng yêu cầu, không phải loại các tiêu chí đó khỏi P0.

| Profile / đội chủ nhà tham chiếu | Bố cục **mô phỏng** | Section | Ghế mô phỏng |
| --- | --- | --- | --- |
| Madison Square Garden / New York Knicks | Bowl chữ nhật bo góc n=4.6, courtside/lower/upper và hai dải gallery; world 3400×2350 | 42 | 16.908 |
| Chase Center / Golden State Warriors | Bowl oval n=2.5, court nằm ngang, ba tầng, lower 12 và upper 14 section; world 3200×2450 | 34 | 17.584 |
| Crypto.com Arena / Los Angeles Lakers | Bowl dài n=3.6, bốn tầng với dải premier-inspired; world 3500×2350 | 50 | 19.056 |

Các số trên được tính từ cấu hình rows×seats của project, **không phải sức chứa chính thức**. Mọi section dùng `SIM-...`; nhãn ngắn trên Canvas có `*`, kèm giải thích trên màn hình. Số tầng, ranh giới, gallery/premier mô phỏng không phải bản sao tổ chức section thật. Orientation chỉ là hướng trong sơ đồ tự tạo, không phải hướng địa lý. Palette tự chọn mang tính gợi nhớ, không khẳng định mã màu thương hiệu chính thức. Không tải sơ đồ, ảnh, logo hay tọa độ ghế của bên ngoài vào assets.

Tên sân/đội có nguồn structured Wikidata CC0 và liên kết nguồn chính thức để đối chiếu; ngày đọc 2026-11-25. Nguồn/thẩm quyền sử dụng nằm ở [SOURCES.md](SOURCES.md). Asset MOCK không giả định tên và lịch trận là dữ liệu NBA live: chỉ một phiên preview với đối thủ Demo, giá và inventory giả lập. Catalog lưu identity provenance riêng khỏi geometry provenance. Không đưa capacity thực chưa xác minh vào model/UI.

### Pipeline và trách nhiệm class

`venues/catalog.json → ArenaCatalogReader → ArenaCatalog/ArenaProfile → MockArenaRepository → ArenaDataset → ArenaSceneCompiler → ArenaScene → ArenaRenderData → ArenaView`.

- `ArenaProfile`: identity có nguồn, team mapping, đường dẫn dataset và cấu hình trình bày (world bounds, scale hai trục, số mũ bowl, court size/angle, palette). `ArenaCatalog` dùng list/map không hard-code ba sân; unit test dựng **30 profile tổng hợp chỉ cho test**, không giả vờ đã chuẩn bị 30 sân thật. Hiện catalog có ba sân khả dụng. Thêm sân sau bằng JSON và kiểm chứng nguồn, không thêm nhánh `if venue` vào View.
- Tái sử dụng schema v1/compiler/validator Phase 2 cho hierarchy annular **tham số đầu vào**: level/section/row/seat ID và inventory giữ nguyên. Không đổi nghĩa/tọa độ của fixture cũ. Level radius và khoảng góc giữa section biểu diễn concourse/lối đi; không khai báo thủ công từng ghế. Generator stdlib `tools/generate-preview-assets.py` tạo lại đúng JSON, không gọi mạng.
- `ArenaSceneCompiler` tiền tính geometry world cuối: với polar `(r,θ)` quanh tâm canonical, `k=(abs(cosθ)^n+abs(sinθ)^n)^(-1/n)`, `x=cx+sx*r*k*cosθ`, `y=cy+sy*r*k*sinθ`. Phép giao tia với superellipse giữ đạo hàm hữu hạn gần trục, tránh section nhọn và ghế kéo dài như phép angle-power cũ. n=2 là ellipse; n lớn cho bowl gần chữ nhật bo góc. Kiểm tra quad lồi/display bounds và court/apron nằm trong khoảng trống trung tâm, tách khỏi khán đài. Tọa độ world cuối bất biến, chỉ tính khi nạp dữ liệu. Ba sân khác n/tỷ lệ/tầng/section/mật độ/gap, không chỉ đổi tên/màu. Họ hình học tham số này đủ cho các **bản mô phỏng** đầu; muốn bản đồ chính xác dạng bất kỳ phải bổ sung schema polygon có dữ liệu/giấy phép và review riêng.
- **Phân biệt hai không gian:** `ArenaDataset.geometry` và các annular boundary là hình học canonical đầu vào; `ArenaScene.seats` là quad world cuối thực sự được vẽ. 3B dùng camera Matrix world-cuối→screen; 3C bắt buộc inverse camera và query cùng `ArenaScene.seats`, không query canonical cũ. Projection phi tuyến là bước chuẩn bị dữ liệu, không thay thế Matrix camera và không áp lại khi pan/zoom. Model `Seat.geometryIndex` ánh xạ cùng chỉ số trong hai snapshot.
- `ArenaRenderData` tạo Path section/hàng và nhóm quad theo AVAILABLE/BOOKED/LOCKED trên worker. Sau báo cáo treo emulator, nhóm ghế được chia theo section/status, tối đa 256 ghế/Path (`SeatPathBatches`), hàng tách Path theo section; không còn ba Path khổng lồ toàn sân. `ArenaOutlines` là helper geometry Java thuần dùng chung Android/export HTML. Boundary được sample theo bước góc tối đa 2°, tái sử dụng projection; label position cache. Path không bị sửa sau khi publish. Inventory thuộc phiên preview được lấy để vẽ, không lưu trạng thái vào ghế vật lý. Chưa có SELECTED.
- `ArenaView extends View`: tự vẽ section/row paths, từng quad ghế trong batch paths và court bằng Canvas/Paint. Court tự dựng tỷ lệ 94:50, biên/giữa/circle/keys/free throw/3-point/corner lines/backboard/rim; không copy ảnh hoặc logo. Chỉ fit tĩnh bằng Canvas translate/scale tính lại khi resize/đổi arena trong 3A. Paint/RectF/Path tái dùng, onDraw không JSON/I/O/sinh ghế. Label giữ cỡ chữ màn hình gần cố định. Chưa tuyên bố đạt hiệu năng tương tác 20k ghế: baseline vẫn vẽ toàn snapshot, LOD/culling/chia batch viewport sẽ làm ở 3B trước pan/zoom.
- `ArenaPreviewLoader`: single worker, Future.cancel, generation token và main Handler; bỏ kết quả cũ khi đổi sân/đóng Activity. AssetManager dùng application context. Activity chỉ nối selector, loading/error/counts, render và save arena ID; không giữ scene lớn trong Bundle. Không cache vô hạn nhiều sân. Chọn lại sân sẽ nạp lại; ưu tiên đơn giản trước khi có bằng chứng cần LRU.
- `MainActivity` vẫn là launcher, XML dọc và `layout-land` dành panel cuộn bên trái khi ngang. Giữ edge-to-edge insets. ArenaView chiếm vùng còn lại. Rotation chỉ lưu sân đang xem, không có selection nghiệp vụ để phục hồi. Chưa kiểm chứng trực quan/font lớn/TalkBack/runtime lifecycle.

### Review Phase 2 và kiểm thử theo mốc

Review lại model/reader/validator và 31 test nền trước khi sửa. Sửa containment: đỉnh quad nằm trong annulus chưa đủ; validator thêm khoảng cách gần nhất từ tâm tới mỗi cạnh và kiểm tra các đoạn giữa giao biên góc. Regression dùng đúng quad probe từ review trước, yêu cầu bị từ chối do cạnh đi vào inner hole. Thay đổi này thuộc tính hợp lệ dữ liệu nền, không triển khai touch hit-testing.

Unit test mới: ba manifest thực có ID riêng, số ghế/quan hệ/provenance, world bounds, projection deterministic; lấy mẫu cạnh quad và dùng công thức inverse độc lập trong **test** để đối chiếu row radius/góc; catalog 30 entries tổng hợp; từ chối schema sai, provenance OFFICIAL giả, duplicate mapping. Không dùng test 30 profile để tuyên bố có 30 dataset thật. Instrumented test viết cho Android Canvas portrait/landscape, đổi ba sân và Activity recreate; phải có emulator/device mới chạy được. Test bitmap chỉ kiểm tra đã vẽ, không chứng minh chất lượng đồ họa.

3B: Matrix fit/bounded pan/pinch focal anchor/reset; LOD/culling, path batches theo section, đo chi phí thực tế trước cache/index phức tạp; test transform/limits/gesture. 3C: inverse Matrix, spatial candidates + point-in-quad, trống/biên/ghế gần nhau, tap vs drag/zoom, highlight/debug; test trước/sau biến đổi và chuyển arena. Các test/tính năng này **chưa được triển khai/chạy trong 3A**.

### Review tiếp theo

Người dùng kiểm tra ba silhouette, nhãn/tầng/lối đi/court/palette ở hai hướng màn hình; xác nhận có chấp nhận mức mô phỏng và đường hình học này làm nền 3B hay muốn thay bố cục cụ thể. Người dùng đã review HTML và yêu cầu cải thiện thẩm mỹ/độ chân thực ngày 2026-11-25; bản chỉnh sửa đang chờ review lại, chưa nghiệm thu. Nguồn hình học chính thức có giấy phép vẫn là việc phải xác minh riêng nếu muốn độ chính xác cao hơn. Giữ fixture rubric-small để giải quyết khoảng 150–250 trong đề, không hạ kiến trúc toàn arena xuống 200. Không tự chuyển 3B/3C/4.


### Bổ sung chẩn đoán và HTML review sau báo cáo treo — 2026-11-25

Người dùng báo emulator hiển thị “Process system isn't responding”; chưa có stack/Logcat để quy nguyên nhân. Bounded paths là xử lý rủi ro rendering đã tìm thấy, không xác nhận đã sửa ANR. Loader ghi timing từng stage/worker, View có trace và warning CPU onDraw chậm tối đa một lần/snapshot; không đo RenderThread/GPU/FPS. Mọi ghế và tọa độ giữ nguyên. Không tự triển khai LOD/culling/camera/hit-testing của 3B/3C.

[HTML Preview](../preview/README.md) là công cụ review riêng: exporter chạy pipeline Java hiện tại, nhúng world quads/section/row/status/batch vào một file HTML offline. JavaScript chỉ vẽ dữ liệu đã xuất, không sinh lại ghế. Source/asset hashes và float32 geometry hash giúp kiểm tra snapshot khớp. Điều khiển phóng đại/scroll HTML không phải triển khai Matrix/gesture của Android. Android vẫn là Custom View/Canvas. Snapshot HTML được tạo từ template + tools, bị ignore vì chứa bản sao hình học lớn. Xem kết quả và giới hạn ở PROGRESS.


### Chỉnh hình học sau review HTML — 2026-11-25

Giữ Phase 3A. Các thông số hiện tại trong bảng trên thay thông số bản đầu, không phải số đo venue thật. Level gap 12–25 world units; cắt section theo các góc tự thiết kế thay chia vòng đều, aisle góc được chuẩn hóa theo bán kính trung bình để tầng ngoài không tạo khe quá rộng. Gallery của MSG vẫn là hai dải, Crypto có dải premium riêng, Chase dùng oval rõ hơn. Court nằm ngang trong cả ba bản mô phỏng, dài 1130/1050/1160 world units, luôn kiểm tra cả apron trước khi xuất.

Màu nền section và ghế trống theo tầng nằm trong catalog, parser/model giữ bản sao phòng sửa; trạng thái booked/locked giữ màu chung. Nét hàng nhẹ hơn section; label có nền sáng và chữ tối. Java `ArenaRenderStyle` cung cấp cả label size/nét hàng/nét section cho Android và exporter HTML; Android có chú giải mã tầng động từ snapshot. Không có LOD/culling/camera Matrix hoặc hit-test mới. HTML mở rộng slider review đến 600%, vẫn vẽ mọi ghế, không ảnh nền seating chart.


## 12. Phase 3A.1 — geometry sân và theme đội nhà

Người dùng giao bổ sung ngày 2026-11-25, trước 3B. Đây là cấu hình trình bày và ba demo mock, không thêm luồng nhiều trận/booking hay triển khai camera/hit-testing.

### Hợp đồng dữ liệu

Catalog `venues/catalog.json` nâng **schemaVersion=2**, tách `arenas[].geometry` khỏi mảng `themes[]` và một `seatStatusPalette` chung. Schema manifest ghế trong `mock/*.json` vẫn v1 và mọi tọa độ v3 giữ nguyên.

- `ArenaGeometry`: display bounds, projection scale/exponent, court length/width/angle; chỉ là thông số của sân. Các level/section/row/seat vẫn thuộc ArenaDataset. Không chứa màu/đội/logo. `ArenaSceneCompiler` chỉ dùng geometry và arena ID, không đòi đội nhà của profile phải có trong roster mock để tạo hình học.
- `ArenaTheme`: identity/teamId, court palette (wood/woodAlternate/lines/apron/paint/lettering/center), tên chữ baseline/center; background/concourse, label fill/text, row/border colors; các tier có section fill, seatBackground và accent. Immutable; không nắm scene, assets, View/Context hoặc tọa độ ghế. Exact swatches và mọi palette ghế/tầng là MOCK.
- `ArenaProfile`: identity sân, `geometry`, `defaultThemeId`, `previewGameThemeId` tùy chọn. `homeTeam` là ngữ cảnh mặc định của demo, không sở hữu geometry. `previewGameThemeId` cho phép cấu hình theme phiên/trận preview bằng JSON (null trong ba demo để dùng đội nhà); không đại diện trận NBA có thật.
- `ArenaCatalog.resolveTheme(profile, gameThemeId)`: override UI/caller → `previewGameThemeId` trong dữ liệu → theme mặc định nếu khớp homeTeam → theme đầu tiên của homeTeam trong catalog → fallback `defaultThemeId`. Không chấp nhận ID theme tường minh không tồn tại. Có thể thêm nhiều theme cho một đội với ID khác nhau; thứ tự chỉ dùng khi không có default khớp đội.
- `SeatStatusPalette`: **một object chung** cho mọi theme; ba màu distinct AVAILABLE/BOOKED/LOCKED, không nằm trong palette tier. Ghế có base theo tier và một chấm tròn màu trạng thái ở giữa. Legend dùng đúng palette dữ liệu trên cả Android/HTML; không dùng màu đội để thay nghĩa trạng thái. Chưa có SELECTED của Phase 4.

### Rendering và đổi theme

`ArenaRenderData(scene)` chỉ giữ Paths section/row/seat/status-dot, tier orders và status indices; không cache màu theo đội. `ArenaSeatMarkers` tính tâm/bán kính dấu trạng thái từ quad world trên worker; cả tọa độ marker được exporter xuất cho HTML. Mỗi marker/seat batch vẫn giới hạn theo SeatPathBatches, không tạo một Path khổng lồ toàn sân.

`ArenaView.setArenaTheme(theme)` chỉ kiểm khả năng phủ các tier, đổi reference/background và invalidate; không gọi repository/compiler, build Path, thay camera fit hoặc chạy I/O. onDraw đọc palette immutable và cache Paths; court vẽ các dải gỗ/khu dưới rổ/đường sân/tên chữ bằng primitive Canvas. Không bitmap sơ đồ, WebView hoặc logo official. Tên đội dùng font hệ thống và cách bố trí tự thiết kế; không sao chép kiểu chữ logo.

MainActivity thêm spinner theme: tự động theo cấu hình trận/đội nhà hoặc override một trong ba demo. Lưu ID override cùng arena ID qua recreate; đổi theme không gọi loader arena. XML dọc/ngang có cùng điều khiển và legend. Việc giữ state mới này chỉ áp dụng preview, không triển khai selection ViewModel/gesture của các phase sau.

HTML xuất ba geometry snapshot + themes đã qua Java parser/validation và palette trạng thái chung. Có ba card Canvas 2D cùng khung 480×360/cùng hướng top-down để so sánh, cộng map lớn có zoom review. Auto cho ba theme home riêng; override áp dụng cùng một theme cho các card để đối chiếu hình học. Theme change giữ zoom/scroll và reuse geometry/Path2D cache, không chạy thuật toán sinh ghế JS. Geometry hash là hash float32, tách khỏi catalog hash bao gồm cả theme.

### Kiểm tra và review

Parser từ chối schema sai, màu sai, theme giả OFFICIAL, ID tham chiếu thiếu, palette trạng thái trùng và contrast chữ không đủ. Ngưỡng **4.5:1** cho label và chữ sân là tiêu chí triển khai tự chọn; không khẳng định toàn ứng dụng đạt chuẩn accessibility từ các test này.

Ba home demo: Knicks/MSG (gỗ ấm, apron/key xanh; tầng xanh/cam mô phỏng), Warriors/Chase (gỗ maple sáng, apron/key xanh; tầng vàng/xanh mô phỏng), Lakers/Crypto (gỗ mật ong, apron/key tím; tầng tím/vàng mô phỏng). Không cố tái hiện một mùa giải/court edition cụ thể. Nguồn ảnh/thời điểm/quyền/giới hạn ở SOURCES §10. Chờ người dùng review màu, lettering, dot trạng thái và mức mô phỏng trước 3B.


## 13. Phase 3A.1 — 30 court concepts, 3 arena geometries

Yêu cầu mới ngày 2026-11-25 thay phạm vi ba court demo trong mục 12 bằng **30 thiết kế mặt sân**. Số nhà thi đấu có geometry vẫn **3** (MSG, Chase, Crypto), không có thêm 27 arena dataset. Không triển khai 3B/3C. Các thiết kế là concept gốc của dự án, không bản số hóa court chính thức hoặc bản sao hình Behance.

### Hợp đồng hiện tại — catalog v3

- `ArenaGeometry` tiếp tục thuộc arenaId, không thay tọa độ của 53.548 ghế v3 đã có. `ArenaStands` nay cũng thuộc `arenas[].stands`: palette seat/section/accent theo tầng, background/concourse/row/border/label và notice MOCK. Đổi team court không đổi khán đài. Mục 12 lưu lịch sử cũ nơi tier palette còn thuộc theme; mục này thay hợp đồng đó.
- `ArenaTheme` chỉ giữ identity/teamId, `Court`, notice và tham chiếu bảng trạng thái chung. 30 đội được cấu hình riêng trong dữ liệu, không có nhánh `if teamId` trong renderer Android/HTML. `tools/court_designs.py` là nguồn authoring vector tự thiết kế; generator xuất JSON xác định, không dùng API/ảnh ngoài.
- `CourtDesign`: hai mô tả `woodFeature`/`graphicFeature`, danh sách primitive `polygon|polyline|ellipse` với part `wood|decor|key|border`, màu, nét và points trong hệ 94×50; dữ liệu bất biến và giới hạn số shape/đỉnh. Catalog compact khoảng 158 KB, giữ nguyên ngân sách 1 MiB của StrictJson.
- `CourtDesign.Logo`: asset PNG local tùy chọn, source/license bắt buộc khi khai báo asset, fallback code, width/height riêng. **Hiện 0 logo asset được tích hợp, cả 30 dùng mã đội** do chưa xác minh bộ asset chính thức có quyền phù hợp. Khai báo provenance không tự chứng minh quyền hợp pháp. Không vẽ lại/copy logo chính thức bằng vector.
- Logo/fallback nằm đúng tâm `(47,25)`; phép contain giữ tỷ lệ ảnh và giới hạn kích thước cấu hình. Trình tự vẽ: apron/border → wood → decor → paint/key details → center mark → competition lines/rims → chữ baseline ngoài vùng thi đấu. Logo nằm dưới vạch sân theo yêu cầu.
- `CourtMarkings` là geometry vạch thi đấu Java thuần dùng chung, được exporter xuất nguyên points cho HTML. Vạch có keyline tối và nét trên sáng để tách khỏi mọi nền/họa tiết. Không để decor hoặc logo vẽ đè vạch.
- `CourtRenderData` chuẩn bị Paths/ảnh local tùy chọn ngoài onDraw; loader catalog chuẩn bị cache cho cả 30 theme trên worker. Thay theme chỉ đổi cache court, không sinh ghế/seat Paths. Canvas vẫn do ArenaView vẽ; không WebView hoặc ảnh sơ đồ tĩnh.
- Status palette chung AVAILABLE/BOOKED/LOCKED vẫn từ JSON. Mỗi ghế có nền theo arena, halo trắng và dot trạng thái bên trong; legend dùng đúng màu chung. Halo không phụ thuộc team hay màu ghế; chưa triển khai SELECTED của Phase 4.

### Hai thành phần hình học riêng cho từng court

Tất cả các hàng dưới đây có vector thật trong JSON. Kiểm thử so shape/type/coordinates/nét đã bỏ màu và phần logo/chữ: 30 chữ ký wood khác nhau và 30 chữ ký decor khác nhau. Đây là bằng chứng khác cấu trúc, không phải chứng nhận thẩm mỹ hoặc độ giống venue thật; người dùng vẫn review bản ẩn tên/màu trung tính.

| teamId | Đội | Bố cục/họa tiết gỗ | Mảng đồ họa thứ hai |
| --- | --- | --- | --- |
| atl | Atlanta Hawks | herringbone veneer / layout 1 | Feather wings |
| bos | Boston Celtics | parquet veneer / layout 1 | Four-leaf corner medallions |
| bkn | Brooklyn Nets | diagonal veneer / layout 1 | Suspension cable grid |
| cha | Charlotte Hornets | tiles veneer / layout 1 | Honeycomb side fields |
| chi | Chicago Bulls | stripes veneer / layout 1 | Paired horn wedges |
| cle | Cleveland Cavaliers | chevron veneer / layout 1 | Crossing sabre ribbons |
| dal | Dallas Mavericks | frames veneer / layout 1 | Offset angular stars |
| den | Denver Nuggets | stepped veneer / layout 1 | Twin mountain horizons |
| det | Detroit Pistons | radial veneer / layout 1 | Twin piston assemblies |
| gsw | Golden State Warriors | fan veneer / layout 1 | Bridge towers and cables |
| hou | Houston Rockets | stripes veneer / layout 2 | Orbit and launch rails |
| ind | Indiana Pacers | diagonal veneer / layout 2 | Opposed speed lanes |
| lac | LA Clippers | tiles veneer / layout 2 | Four-blade compass |
| lal | Los Angeles Lakers | fan veneer / layout 2 | Showtime sweeping ribbons |
| mem | Memphis Grizzlies | herringbone veneer / layout 2 | Three upright claw rails |
| mia | Miami Heat | diagonal veneer / layout 3 | Flame taper panels |
| mil | Milwaukee Bucks | chevron veneer / layout 2 | Branching antler framework |
| min | Minnesota Timberwolves | stepped veneer / layout 2 | Two pine-tree horizons |
| nop | New Orleans Pelicans | frames veneer / layout 2 | Plume fan baseline fields |
| nyk | New York Knicks | stripes veneer / layout 3 | Stepped city skyline |
| okc | Oklahoma City Thunder | chevron veneer / layout 3 | Two angular lightning paths |
| orl | Orlando Magic | radial veneer / layout 2 | Comet trails and stars |
| phi | Philadelphia 76ers | parquet veneer / layout 2 | Thirteen-dot corner arcs |
| phx | Phoenix Suns | radial veneer / layout 3 | Full-floor sun-ray composition |
| por | Portland Trail Blazers | herringbone veneer / layout 3 | Five opposed diagonal bars |
| sac | Sacramento Kings | tiles veneer / layout 3 | Two serrated crown bands |
| sas | San Antonio Spurs | parquet veneer / layout 3 | Eight-spoke half-court wheels |
| tor | Toronto Raptors | frames veneer / layout 3 | Three full-floor claw cuts |
| uta | Utah Jazz | stepped veneer / layout 3 | Stacked canyon terraces |
| was | Washington Wizards | fan veneer / layout 3 | Monument and three-star bands |

### HTML review và điểm dừng

Mặc định mở **Chỉ xem mặt sân** và **Ẩn tên/mã đội/logo**. Bỏ ẩn danh để xem tên và chọn đội theo tên; mọi đội có option/card riêng. Gallery 30 canvas có cùng kích thước backing 480×280 và cùng phép fit hệ 102×58 (court 94×50 + apron). Chọn card cập nhật map lớn phía trên. Bật **Trung tính màu** để đưa tất cả về cùng palette và chỉ so layout; không dùng chế độ này để đổi màu trạng thái/khán đài. Đổi theme giữ zoom; bỏ court-only để xem theme trên một trong ba geometry mô phỏng, có thông báo không phải mapping venue chính thức cho 30 đội.

Exporter đọc đúng ArenaTheme/CourtDesign/ArenaStands/CourtMarkings của Java, xuất logo đã nhúng nếu tải được asset local phù hợp; HTML không sinh lại seat coordinates hay suy diễn artwork theo teamId. Gallery dùng cache Paths, chỉ redraw khi đổi chế độ/resize/selection/zoom cần thiết, không animation loop.

Ảnh QA named và blind-neutral được Pillow vẽ từ cùng snapshot, chỉ phục vụ kiểm tra bố cục. Nghiệm thu trực quan cuối diễn ra trên HTML theo yêu cầu người dùng. Chưa có lượt người dùng nghiệm thu 30 court mới. Nguồn/giới hạn Behance ở SOURCES §11; browser host phiên AI không khả dụng, không nhận đã kiểm tra pixel trong browser hoặc Android runtime.

## 14. Phase 3A.1 — 30 sân nhà, hình học arena và palette đội

Cập nhật 2026-11-25 theo yêu cầu mới, thay quy tắc palette arena ở mục 13. Giữ 30 court designs đã có; bổ sung 27 arena, tổng 30 bộ hình học mô phỏng. Không triển khai 3B/3C.

- Catalog v4: `homeArenas[teamId] = arenaId`. Một arena có một manifest hình học; mapping có thể dùng chung arena cho nhiều đội. `ArenaProfile.homeTeam` là đội mặc định khi chọn venue; theme được chọn rõ ràng theo teamId khi chọn đội/trận.
- `arenas[].geometry` chứa tỷ lệ ngang/dọc, độ bo và kích thước court. `mock/{arenaId}.json` chứa tầng, độ sâu, section cuts/aisle, số hàng và ghế. Generator dùng chung; 27 profile author rõ trong `tools/arena_layouts.py`, ba profile cũ được giữ nguyên. Không rẽ nhánh bộ vẽ theo đội.
- `themes[].primaryColor/secondaryColor` lấy nguyên màu của theme đã có. Java `ArenaTheme.seatTier(order)` chuẩn bị trước các sắc độ section/seat/accent, xen primary và secondary theo tầng, đậm dần theo thứ tự. HTML nhận chính palette đã được Java tính, không có công thức màu ghế JS riêng. Mỗi tầng phân biệt bằng tên/nhãn, nền và accent.
- `ArenaStands` quản lý nền trung tính, lối đi, viền và độ tương phản label theo arena. `stands.tiers` giữ swatch tham chiếu của schema trước để không mất dữ liệu, **không được renderer dùng cho màu ghế/section/tầng**. Nguồn màu render là `ArenaTheme.seatTier` trên Android và `theme.seatPalette` xuất từ chính Java trên HTML.
- `seatStatusPalette` chung cho mọi đội/sân, chấm trạng thái có halo trắng bên trong ghế; không lấy màu trạng thái từ primary/secondary. Nhãn có nền trắng cố định/ink tương phản. Court art, center fallback/asset slot, kích thước logo và thứ tự logo dưới vạch được giữ nguyên.
- Android chọn đội sẽ load home arena tương ứng trên worker, đồng bộ spinner venue/title/theme. Cùng arena chỉ đổi theme, giữ render Paths/tọa độ. Lưu arenaId/themeId qua recreation như trước. Canvas/Paint vẫn là bộ vẽ; không WebView/ảnh seating map.
- HTML mặc định xem toàn arena và hiện tên đầy đủ ở heading. Mã đội hiện giữa sân khi thiếu asset. 30 card dùng outline đã xuất Java, cùng frame nhìn từ trên xuống; sơ đồ lớn tải hàng/ghế chi tiết của sân đang chọn. Court-only vẫn cho 30 court bằng kích thước 480×280 và tùy chọn ẩn danh/trung tính.
- Export tách `arena-preview.html` và `arena-data/{id}.js`; script local dùng được với file://, không fetch/CDN/server. Hai cache geometry/Path và dataset giới hạn hai arena; token chống kết quả tải cũ, có lỗi thiếu file và cho retry. Cần giữ cả thư mục `arena-data` cạnh HTML khi chia sẻ. Đây là chuẩn bị artifact HTML, không phải camera/LOD/gesture Android Phase 3B.

Toàn bộ 30 geometry và màu ghế là mô phỏng. Bảng sân, số ghế MOCK và mức nguồn tại [SOURCES §12](SOURCES.md#12-khán-đài-30-sân-nhà-và-palette-theo-đội--2026-11-25). Chờ người dùng review trực quan; source/hash/build checks không thay nghiệm thu thẩm mỹ hoặc Android runtime.
