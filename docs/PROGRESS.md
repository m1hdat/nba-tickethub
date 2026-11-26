# Tiến độ thực tế

## Phase 6B — bảng Gợi ý ghế dùng chung (2026-11-26)

**Đã triển khai 6B và kiểm thử bản sửa sau QA; chưa nghiệm thu người dùng/Samsung.** Trước sửa đã đọc AGENTS/tài liệu và `git status --short --branch`: branch main có nhiều thay đổi 6A chưa commit và file untracked có trước. Giữ nguyên chúng; không reset/clean/stage/commit/push. Mục 6A phía dưới giữ nguyên làm lịch sử.

### Phạm vi và tiêu chí tương ứng

| Yêu cầu được giao | Kết quả 6B |
| --- | --- |
| Một bảng cho đơn/nhóm, lấy lọc ban đầu và cho sửa | BottomSheetDialog chung: số người, ngân sách USD mỗi vé, tầng/khu/loại vé, Giá thấp/Gần sân; query giữ qua recreate trong ViewModel. Bộ lọc sơ đồ hiển thị được ngân sách tùy nhập |
| Đúng trận, ghế trống, thỏa ngân sách/khu/tầng, tối đa 3 | SeatFinder dùng inventory[matchId] sau overlay SQLite; lọc trước xếp hạng, giữ truy vấn khi rỗng. Giá thấp theo tổng; Gần sân theo khoảng cách hình học từ tâm ghế tới biên court logic |
| Nhóm N cùng hàng/khu/đoạn, không qua lối đi | Duyệt row range của layout; kiểm khoảng góc tâm quad so với pitch hàng. Không nối hai section và không chỉ xét số ghế. Nhóm chỉ dùng ghế SEATED; kiểm tra missing slot bằng số ghế liên tiếp nhưng geometry cách hai slot |
| Từng giá, tổng, lý do và xem góc nhìn | Từng khu/hàng/ghế, giá đủ 2 chữ số thập phân, tổng minor units, lý do; chạm thẻ focus ArenaView rồi mở góc nhìn Canvas đã có. Đóng góc nhìn vẫn giữ bảng kết quả |
| Chọn đủ nhóm sau kiểm lại; ghế bận giữ lựa chọn cũ | Đọc lại BookingStore trên worker, kiểm toàn bộ trước khi thêm ID. Xung đột không sửa selection/tổng/replacement; tìm lại loại ghế vừa bận. Không giữ chỗ: checkout 6A vẫn recheck/transaction |
| Theme, bảng gọn, tiếp tục 6A | Accent đội nhà cho nút mở bảng/nút thao tác; bảng kéo/thu tối đa 42% màn hình, nội dung tự cuộn, còn thấy phần sơ đồ phía trên. Chọn nhóm thêm vào selection cũ không trùng ID; Tiếp tục dùng BookingController 6A |
| HTML cho bấm thử | Cập nhật tools/arena-preview/index.html thành 6B; cùng payload hình học cũ, PreviewBookings và checkout 6A, cùng các trường và tối đa 3 kết quả. HTML dùng quad đã project để xếp gần sân; không nhận pixel/ranking parity tuyệt đối với geometry logic Android |

### File thuộc thay đổi 6B

- Tạo: `app/src/main/java/com/example/nbatickethub/state/SeatFinder.java`, `app/src/main/java/com/example/nbatickethub/ui/arena/SeatFinderController.java`, `app/src/test/java/com/example/nbatickethub/state/SeatFinderTest.java`, `app/src/androidTest/java/com/example/nbatickethub/SeatFinderInstrumentedTest.java`, `tools/arena-preview/seat-finder.js`.
- Sửa Android: `app/src/main/java/com/example/nbatickethub/MainActivity.java`; `state/ArenaSelection.java`, `state/ArenaSelectionViewModel.java`, `ui/arena/ArenaFilterController.java`, `ui/arena/SeatSheetController.java` trong cùng Java root; `app/src/main/res/layout/activity_main.xml`, `layout-land/activity_main.xml`, `values/strings.xml`.
- Sửa preview: `tools/arena-preview/index.html`, `mobile.js`, `mobile.css`, `test-preview.cjs`, `README.md`.
- Tài liệu: `docs/PROGRESS.md`, `AI_LOG.md`, `DESIGN.md` mục 17, `ROADMAP.md` cập nhật phạm vi 6B theo lệnh người dùng. Không sửa assets/geometry/logo/dependency/toolchain; các thay đổi 6A có trước vẫn được giữ và không nhận là mới làm trong 6B.

### Kết quả kiểm chứng bản cuối

- Wrapper: `./gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.nbatickethub.SeatFinderInstrumentedTest' --no-configuration-cache` → **BUILD SUCCESSFUL 1m36s**; log `.gradle/phase6b-final.log`.
- **88 JVM tests, 0 failures/errors/skipped**, gồm 7 SeatFinderTest mới: giá/ngân sách, gần sân, nhóm 3/tổng, không nối lối đi/missing slot dù số liên tiếp, rỗng/khu/tầng, atomic selection khi có ghế bận, idempotent và đúng tồn kho hai trận MSG/stale match. XML ở `app/build/test-results/testDebugUnitTest/`.
- **Lint: 0 errors, 68 warnings**, không suppress hoặc thay dependency để làm qua. Báo cáo `app/build/reports/lint-results-debug.sarif`.
- **2 UI 6B tests, 0 failures/errors/skipped**, emulator Medium Phone API 37, 1080×2400px / 420dpi (~411×914dp), không thay kích thước máy. Test bấm/scroll bảng thực, nhập 3, recreation giữ query, chạm thẻ xem Canvas perspective, đóng rồi Chọn cả nhóm, checkout có đủ 3 vé đúng match; helper SQLite khác đặt một ghế sau lúc tìm, chọn nhóm báo xung đột và giữ selection cũ; ngân sách 0 cho rỗng và giữ giá trị. XML lưu riêng [native-final.xml](../tmp/phase6b/native-final.xml).
- Node: `node tools/arena-preview/test-preview.cjs` **PASS**, gồm hồi quy 4–6A và 6B: đơn/nhóm, giá/gần sân, adjacency theo quad, không nối khu/missing slot, từng giá/tổng, preview, nhóm vào checkout, conflict sau search không chọn một phần, tìm lại loại booked, rỗng/invalid và hai trận cùng sân. Log `.gradle/phase6b-preview.log`. Đây là DOM/Canvas/storage doubles, không phải kiểm browser pixel. `node --check` mobile.js và seat-finder.js đạt.
- Đã xem ảnh emulator sau sửa: [nhóm 3 và sơ đồ phía trên](../tmp/phase6b/phase6b-qa/group-results.png), [góc nhìn Canvas](../tmp/phase6b/phase6b-qa/perspective.png), [ghế bận](../tmp/phase6b/phase6b-qa/conflict.png), [rỗng giữ ngân sách 0](../tmp/phase6b/phase6b-qa/empty.png). Nhóm L10/R1/14–16 hiển thị từng vé **86,25 US$**, tổng **258,75 US$**, đúng phần cent. Chưa coi ảnh này là nghiệm thu Samsung/người dùng.
- Hồi quy cuối chạy riêng `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=CLASS' --no-configuration-cache`: **3 PhaseFour tests** đạt (1m27s, `.gradle/phase6b-regression4.log`, [XML](../tmp/phase6b/regression4.xml)); **2 PhaseSix tests** đạt (58s, `.gradle/phase6b-PhaseSixInstrumentedTest.log`, [XML](../tmp/phase6b/PhaseSixInstrumentedTest.xml)); **6 BookingStore tests** đạt (22s, `.gradle/phase6b-BookingStoreInstrumentedTest.log`, [XML](../tmp/phase6b/BookingStoreInstrumentedTest.xml)). CLASS tương ứng là `com.example.nbatickethub.ui.arena.PhaseFourInstrumentedTest`, `com.example.nbatickethub.PhaseSixInstrumentedTest`, `com.example.nbatickethub.BookingStoreInstrumentedTest`. Tất cả log **BUILD SUCCESSFUL**, XML 0 failures/errors/skipped. Tổng **13 Android tests distinct** gồm 2 ca mới 6B + 11 ca hồi quy. Không coi lượt nhiều class ban đầu (thực tế chỉ 2 tests) là đã chạy hết hồi quy.
- Hồi quy PhaseFour giữ nguyên các assertion vùng vé/CTA nhìn thấy, candidate/filter/camera, đổi hai trận cùng sân/khác sân, perspective, recreate và ngang/dọc; nhóm PhaseSix giữ back/double confirm/reopen/conflict. Storage kiểm transaction rollback, concurrency, unique matchId + seatId, snapshot và tồn kho riêng cùng sân. Chưa chạy lại toàn bộ camera/scale suite hoặc PhaseFive trong 6B.
- XML resource, dependency JS/CSS của index.html và toàn bộ local links mới của mục 6B đã kiểm **PASS**; `git diff --check` đạt (chỉ cảnh báo line ending ở AI_LOG). `git diff -- app/src/main/assets` rỗng; staged paths **0**. PowerShell có thể hiển thị NativeCommandError cho stderr cảnh báo deprecated Java; kết luận dựa trên Gradle SUCCESSFUL và XML thật, không bỏ qua test thất bại.

- [APK debug 6B](../app/build/outputs/apk/debug/app-debug.apk): **10466270 byte**, SHA-256 `0ee7cdce9e9d3f7f7667d2918da9c079bc0d25600f0a24aa09652da9f9b4fab5`. Đây là bản mô phỏng cục bộ.

### Lỗi phát hiện, giới hạn và cách mở

Lần QA ảnh đầu bảng 58% che vùng sân và formatter currency mặc định làm tròn cent; đã sửa còn 42%, giữ 2 chữ số thập phân và màu nút đội nhà, chạy lại 2 UI tests và xem ảnh cuối. Cache sandbox thiếu dependency; dùng cache Gradle user hiện có qua quyền công cụ, không đổi cấu hình máy. Lượt test mới ban đầu lỗi import closeSoftKeyboard và tham số Gradle chưa quote, đã sửa; không sửa assertion để che lỗi nghiệp vụ.

Browser host unavailable; auto-review từ chối chọn cửa sổ Edge vì đang chứa trang cá nhân ngoài phạm vi 6B. AI đã hỏi quyền dùng cửa sổ đó, chưa nhận phản hồi tại thời điểm ghi. **Chưa bấm/QA pixel browser 6B**, không suy từ Node hoặc QA 6A cũ. Chưa thử Samsung, 360dp/ngang cho bảng 6B, font lớn, process death hoặc FPS/heap. Các ghế, giá, booking và khoảng cách đều mô phỏng; dữ liệu không phải giao dịch NBA thật.

Mở thử: Edge/Chrome → Ctrl+O → `D:\24tn-fitus\project\nba-tickethub\tools\arena-preview\index.html` → Ctrl+F5 → chọn trận → Chọn ghế → **Gợi ý ghế** → nhập 1 hoặc 3 người/ngân sách/khu/tầng/ưu tiên → **Tìm ghế**. Chạm thẻ xem góc nhìn, đóng rồi Chọn cả nhóm → Tiếp tục để thử checkout 6A. Không cần server; giữ JS/CSS và thư mục dữ liệu đi kèm. Hướng dẫn tại [README preview](../tools/arena-preview/README.md).

Đã cập nhật PROGRESS/AI_LOG/DESIGN/ROADMAP. Chưa có xác nhận nghiệm thu; không có vấn đề phạm vi cần quyết định thêm. Dừng trong 6B để người dùng review. Commit đề xuất: `feat: add shared single and group seat recommendations`.

## Phase 6A — đặt vé mô phỏng và Vé của tôi (2026-11-26)

**Đã triển khai và kiểm thử 6A; chưa nghiệm thu người dùng/Samsung.** Bắt đầu từ `main` / HEAD `88c511a`, không có tracked diff, chỉ có các file untracked sẵn có (sources và công cụ preview cũ); giữ nguyên chúng. Không stage/commit/push.

### Yêu cầu được giao và kết quả triển khai

| Yêu cầu 6A | Kết quả |
| --- | --- |
| Tiếp tục → xác nhận, quay lại giữ ghế | Native Views với cặp đấu, giờ/múi giờ, sân, khu/hàng/số, từng giá và tổng; nhãn Đặt vé mô phỏng, theme đội nhà; CTA đặt trong summary sheet |
| Kiểm tra trống đúng trận, không đặt trùng | SQLite transaction kiểm toàn bộ selection; nêu ghế bận; unique `(match_id,seat_id)`, request ID idempotent, busy state; lỗi ghi rollback toàn bộ |
| Thành công và Vé của tôi | Mã đơn UUID, Xem vé, tab điều hướng và empty/loading/error; thẻ vé đọc snapshot đã chốt, lưu DB riêng trên thiết bị |
| Sơ đồ sau đặt và hai trận cùng sân | Worker overlay BOOKED trước render; không chọn lại ghế đã đặt; chỉ overlay inventory đúng matchId |
| Preview | Cập nhật index.html đang có; localStorage của trình duyệt riêng Android, nhãn mô phỏng, không thanh toán |

### File tạo/sửa trong nhiệm vụ

- Mới: `data/BookingStore.java`, `state/BookingViewModel.java`, `ui/arena/BookingController.java` trong Java root; Android tests `BookingStoreInstrumentedTest.java`, `PhaseSixInstrumentedTest.java`; `tools/arena-preview/booking-store.js`.
- Sửa Java: `MainActivity`, `state/BrowseNavigation`, `ui/arena/ArenaPreviewLoader`, `ui/arena/BrowseController`.
- Sửa XML/resources: `layout/activity_main.xml`, `layout-land/activity_main.xml`, `layout/browse_shell.xml`, `values/strings.xml`.
- Sửa preview: `index.html`, `mobile.js`, `mobile.css`, `selection-model.js`, `test-preview.cjs`, `README.md`.
- Tài liệu: `PROGRESS.md`, `AI_LOG.md`, `DESIGN.md`, `ROADMAP.md`. Scripts/log/ảnh trong `tmp/phase6` và `.gradle/phase6*` là artifact QA cục bộ, không stage.

### Kiểm chứng đã quan sát

- Wrapper `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-configuration-cache`: BUILD SUCCESSFUL ở lượt đầu; 81 JVM tests, lint 0 errors/68 warnings. Không đổi SDK/AGP/Java/dependency.
- SQLite thật trên emulator: **6 tests / 0 failures/errors/skipped**, gồm đơn nhiều ghế thành công/giá/tổng, retry cùng request + duplicate request khác, một ghế bận và trạng thái gốc BOOKED/LOCKED, đóng/mở DB giữ snapshot, hai match MSG dùng cùng seatId, hai helper cạnh tranh, trigger lỗi ghi ticket thứ hai rollback cả order/ticket thứ nhất. Lượt chạy class list chỉ công bố XML 6 ca repository; không tính UI/hồi quy chưa xuất hiện trong XML là đã chạy.
- UI lượt trước chỉnh bố trí summary: **1 test 411dp đạt** (`tmp/phase6/ui-411.xml`) và **1 test 360dp đạt** (`tmp/phase6/PhaseSixInstrumentedTest-360.xml` trước lượt mới). Kiểm quay lại giữ selection, recreate confirmation, xác nhận lặp, Xem vé, Activity đóng/mở lại và chặn chọn ghế đã đặt. Test setup ban đầu chờ game trong ViewModel quá sớm sau recreate; sửa chờ catalog/controller sẵn sàng, không nới assertion nghiệp vụ.
- Hồi quy phát hiện lỗi thật ở 360dp: thanh Tiếp tục độc lập làm danh sách không còn 48dp; **2/3 PhaseFour đạt, 1 ca fit thất bại**. Đã chuyển CTA vào summary để giữ bounds Canvas/list; kết quả chạy lại được ghi dưới đây. Ảnh native cũng phát hiện heading Vé của tôi tối trên nền tối; đã đổi chữ trực tiếp trên list sang resource browse_text, giữ chữ thẻ nền trắng.
- HTML host `node tools/arena-preview/test-preview.cjs` PASS: hồi quy 4–5, xác nhận/back/double-confirm, snapshot, storage reload, BOOKED, conflict không lưu một phần, hai trận cùng sân. DOM/Canvas/storage doubles là bằng chứng logic, không thay browser/Android. `node --check` mobile.js/booking-store.js đạt.
- **Bấm thật Edge file://** trên khung phone mặc định 390 CSS px, browser zoom sẵn có 75%: chọn trận BOS@NYK, khu L05, R1 ghế 9, 86,25 US$; quay lại giữ 1 ghế; xác nhận double-click tạo đúng 1 thẻ trong Vé của tôi; reload trang rồi mở Vé của tôi vẫn giữ mã đơn, giờ và giá cũ. Phát hiện/sửa sheet che Tiếp tục (đưa sheet cao hơn 52px); đã bấm trọn luồng sau sửa, CTA và thẻ trong khung nhìn thấy. Không phải DevTools viewport 390×844 được đo trong lượt này; không suy ra Samsung hoặc pixel parity.

### Kết quả cuối trên bản bàn giao 6A

- **Build/JVM/lint/storage:** Wrapper `./gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.nbatickethub.BookingStoreInstrumentedTest --no-configuration-cache`: **BUILD SUCCESSFUL, 1m20s**; **81 JVM tests, 0 failures/errors/skipped**, lint **0 errors/68 warnings**, **6 SQLite Android tests, 0 failures/errors/skipped**. Log `.gradle/phase6-final.log`, XML `tmp/phase6/storage-final.xml`. PowerShell có exit code 1 do native stderr cảnh báo Java deprecated được bọc thành NativeCommandError; Gradle cuối log báo SUCCESSFUL và XML tất cả ca đạt. Không ghi shell exit này thành lỗi nghiệp vụ hoặc bỏ qua test thất bại.
- **UI 6A cuối, 360×640dp** (945×1680px, 420dpi): **2 tests, 0 failures/errors/skipped**, BUILD SUCCESSFUL 1m23s. XML `tmp/phase6/PhaseSixInstrumentedTest-360.xml`, log `.gradle/phase6-PhaseSixInstrumentedTest-360.log`. Luồng back/selection, chờ CTA thực sự nhìn thấy đủ ít nhất 47dp, recreate dialog, xác nhận lặp, mã đơn/Xem vé, Activity reopen và BOOKED; ca mới cố ý đặt một ghế qua helper thứ hai trong khi đang xem draft, xác nhận báo đúng ghế, không thêm đơn, giữ ghế còn trống khi chọn lại. Lượt trung gian test assertion CTA chạy trong cùng frame vừa đổi GONE→VISIBLE; đã chờ layout và giữ thêm assertion 47dp, không giảm yêu cầu vùng nhìn thấy.
- **Hồi quy cuối ở 360dp:** chạy riêng `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.nbatickethub.ui.arena.PhaseFourInstrumentedTest` → **3/3 đạt**, BUILD SUCCESSFUL 1m57s; `...class=com.example.nbatickethub.PhaseFiveInstrumentedTest` → **2/2 đạt**, BUILD SUCCESSFUL 1m23s. Cả hai thêm `--no-configuration-cache`; XML/log tương ứng trong `tmp/phase6/*-360.xml` và `.gradle/phase6-*-360.log`. Giữ nguyên assertion list vé nhìn thấy ≥48dp, preview 16:9 và CTA cũ. Rotation/selection/filter/camera, home/detail/navigation/30 đội vẫn qua các ca đã có; đây không phải chạy toàn bộ camera/scale suite.
- Tổng nhóm Android cuối: **13 ca distinct (6 storage + 2 UI 6A + 3 Phase 4 + 2 Phase 5), không failures/errors/skipped**. Lượt UI 411dp trước sửa summary chỉ là bằng chứng phiên bản trung gian, không gộp vào 13 ca cuối. Emulator đã xác minh khôi phục **1080×2400px / 420dpi**, không còn override size.
- Ảnh đã xem: [xác nhận](../tmp/phase6/qa-360/confirmation.png), [thành công](../tmp/phase6/qa-360/success.png), [Vé của tôi](../tmp/phase6/qa-360/my-tickets.png), [ghế bận](../tmp/phase6/qa-360/conflict.png). Heading Vé của tôi sáng trên nền tối sau sửa; thẻ/giá/tổng và cảnh báo ghế bận đọc được ở 360dp. Chưa phải review Samsung/người dùng.
- `node tools/arena-preview/test-preview.cjs` đạt cả empty state và luồng 6A/hồi quy; cú pháp JS, parse resource XML, dependency HTML và `git diff --check` đạt. Git chỉ cảnh báo LF/CRLF ở file cũ, không lỗi whitespace. Source assets không có diff; không sinh lại geometry/court/logo.
- [APK debug](../app/build/outputs/apk/debug/app-debug.apk): **9204158 byte**, SHA-256 `1d27bb3e91e6786609a9bc02bd20583f0d80617fbf577dbdaa5d2ee829de10de`. Đây là bản debug mô phỏng; không thanh toán thật.

Đã cập nhật PROGRESS/AI_LOG/DESIGN/ROADMAP và README preview theo phạm vi được giao. Staged paths **0**; không commit/push. Dừng 6A, chờ người dùng review; Samsung và các hạn chế nêu ở dưới còn chưa kiểm chứng.

### Cách mở preview và điểm dừng

Chrome/Edge → Ctrl+O → `D:\24tn-fitus\project\nba-tickethub\tools\arena-preview\index.html` → Ctrl+F5. Không cần server. Giữ JS/CSS, payload `preview/arena-data` và logo assets đi kèm ở vị trí hiện tại. Chọn trận → khu → vé trống → Chọn ghế → Tiếp tục → Xác nhận đặt vé → Xem vé. Tab Vé của tôi dùng dữ liệu lưu riêng của trình duyệt; không đồng bộ SQLite Android. Browser không có Web Locks chưa bảo đảm cạnh tranh nhiều tab, Android có SQLite constraint/transaction.

**Chưa thử trên Samsung thật**, chưa đo FPS/heap/GPU, font scale lớn, chưa nghiệm thu process death khi đang chỉnh draft/xác nhận chưa commit; persistence đơn đã commit được kiểm độc lập qua DB reopen và Activity reopen. Không coi build/test là nghiệm thu thẩm mỹ hoặc review của người dùng. Chỉ 6A, không có 6B/thanh toán thật/tài khoản/backend. Không có câu hỏi phạm vi cần xin phép thêm; chờ người dùng review giao diện và thử trên Samsung.

Commit đề xuất: `feat: add atomic simulated bookings and persistent my tickets`.


### Cho phép đưa Phase 4–5 lên GitHub — 2026-11-26

Người dùng yêu cầu “push phase 4 5 len”, cho phép commit/push phần đã làm. Giữ nhánh main/origin hiện có, không force push. Gói source gồm Android Phase 4–5, tests, logo/provenance, HTML preview và exporter Java/template cần thiết để tái tạo payload. Không đưa tmp, log/ảnh QA, APK/build/cache, PDF đầu vào hoặc asset HTML sinh tự động (~109 MB) vào Git. Hướng dẫn clone → export một lần → mở HTML ở tools/arena-preview/README.md. Các dòng “không commit/push” phía dưới là điểm dừng lịch sử trước yêu cầu mới; không được sửa thành người dùng đã nghiệm thu mọi giao diện. SHA và trạng thái remote được kiểm tra/báo trong bàn giao Git, không suy ra thành công từ việc chỉ tạo commit.

## Phase 5 — Trang chủ, lịch đấu, đội bóng và chi tiết trận (2026-11-26)

**Đã triển khai, đang chờ người dùng review.** Phạm vi mới do người dùng giao thay tên Phase 5 cũ trong roadmap; không coi các công việc scale/process death/performance P0 đã xong. Phase 6 Đặt vé/Vé của tôi chưa triển khai. Giữ các thay đổi đã có trong working tree; không stage/commit/push.

### Nội dung và tiêu chí

- Java + Views/XML: Trang chủ có trận nổi bật gần nhất chưa bắt đầu, poster chia hai màu đội, hai logo/tên lớn, đường sân nhẹ, giờ/sân/giá từ và CTA Chọn ghế; phía dưới là Trận sắp tới/Khám phá theo đội. Nền tối, chữ Việt rõ, accent vàng. Một binder poster dùng cho cả ba màn để thông tin/giá nhất quán. Chi tiết dùng màu chủ nhà, CTA cố định đáy; chuyển browse bằng alpha 140ms, không animate lúc cuộn.
- Ba tab Trang chủ/Lịch đấu/Đội bóng. Giữ nguyên bộ lọc lịch Phase 4 và dữ liệu tương đối 33 trận. 30 đội mở lịch ALL_UPCOMING cả nhà/khách, kể cả ngoài 7 ngày. Thẻ mở detail; CTA ở home/detail mở sân bằng đúng Game/matchId. Geometry theo arenaId, theme theo homeTeamId và vé theo matchId giữ nguyên trách nhiệm.
- `BrowseNavigation` trong ViewModel giữ màn, origin, ID chi tiết và scroll. Back từ sân về màn trước; Đổi trận mở lịch. Back từ detail giữ lọc/cuộn lịch. Khôi phục cuộn chờ pre-draw sau layout; capture chỉ lấy offset của màn đang mở. Giữ bộ máy Canvas/pan/zoom/hit-test/renderer/confirmation Phase 4.
- Catalog cũ chưa có ảnh logo. Bổ sung **30 PNG offline, 1,771,563 byte** từ ESPN CDN, nguồn/ngày tải/hash ở [SOURCES.json](../app/src/main/assets/logos/SOURCES.json). Logo là thương hiệu NBA/đội, không khai báo giấy phép mở; dùng nhận diện giáo dục. Worker decode bitmap, không tải mạng lúc chạy app. Không thay 34 asset cũ, geometry/court art/payload sân. HTML dùng `teamBadgeUrl` riêng để không ghi đè contract logo giữa sân.
- [HTML preview](../tools/arena-preview/index.html) có cùng ba tab/poster/detail/CTA; nối lại bản đồ và vé cũ, nhãn Preview/Lịch mẫu, không booking. Đường mở chính xác: Chrome → Ctrl+O → `D:\24tn-fitus\project\nba-tickethub\tools\arena-preview\index.html`; F12 → Device Toolbar → 390 × 844, Ctrl+F5 nếu đang giữ bản cũ. Không cần server. Giữ JS/CSS, preview/arena-data và app/src/main/assets/logos tại đúng vị trí.

### Kiểm chứng thực tế

- Wrapper `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-configuration-cache`, bản cuối chạy class PhaseFive ở 360dp: **BUILD SUCCESSFUL**, **81 JVM tests / 0 failures**, lint **0 errors / 68 warnings**. Log `.gradle/phase5/final-state-360.log`. Ba test JVM mới kiểm stack/origin/scroll khi đổi màn; test cũ xác minh giá từ cùng inventory. Không sửa SDK/Java/Gradle/dependency hoặc suppress lỗi lint.
- **360×640dp** (945×1680px/420dpi): 2 PhaseFive tests qua; card/CTA thực sự trong vùng nhìn thấy, detail recreate, home/chi tiết vào đúng MSG/Chase, 30 đội, Knicks sân khách mở home arena/theme Boston, quay lại giữ lọc và scroll 280px. **411.4×914.3dp** (1080×2400px/420dpi): 2 PhaseFive tests qua cùng các điều kiện. Bản cuối có thêm recreate khi đang xem detail trận sân khách rồi về lịch đúng 280px. Log `.gradle/phase5/final-state-360.log` (**1m36s**), `final-state-411.log` (**45s**); XML `tmp/phase5/results-360-final.xml`, `results-411-final.xml`, mỗi cỡ 2 tests/0 failures/errors/skipped. Kích thước emulator khôi phục trong finally về 1080×2400/420dpi.
- Hồi quy trên bản Phase 5 ở 360dp: **3 PhaseFour tests / 0 failures** (`.gradle/phase5/regression-360.log`, `tmp/phase5/results-phase4-360.xml`); **1 MatchCalendar test / 0 failures** (`final-calendar-360.log`, `results-calendar-360.xml`). Bao gồm candidate/CTA/blocked, hai trận cùng sân, khu thấp/cao, sheet/camera, recreate/xoay theo các ca Phase 4 và date/team/homeOnly/empty/recreate theo ca calendar. Đây không phải lượt chạy toàn bộ suite camera cũ.
- Sau sửa xung đột scroll, chạy lại **1 MatchCalendar test ở 411dp / 0 failures/errors/skipped**, BUILD SUCCESSFUL 23s; log `.gradle/phase5/final-calendar-411.log`, XML `tmp/phase5/results-calendar-411.xml`. APK debug cuối **9,119,630 byte**, SHA-256 `37ee340f0bcbcbe012c1afcad44d37df2d4e862056cc167555b8ea0171f5efe8`; toàn bộ **65 source asset files** khớp byte trong APK, gồm 34 asset cũ và 31 file logo/provenance mới.
- **Chrome Guest Responsive 390×844 CSS px**: trực tiếp mở file và bấm home poster → detail → chọn ghế; CTA home vào sân trực tiếp; BOS@NYK đúng MSG/86,25 US$, LAL@GSW đúng Chase/89,25 US$; so hai poster/nhận diện, CTA đáy nằm trong màn. Chạm L05 từ toàn sân mở preview/vé, không cần zoom. Quan sát console sau nạp lại và đi cả hai sân không còn lỗi ứng dụng. Browser connector unavailable nhưng điều khiển cửa sổ Chrome bằng Computer Use hoạt động. Chưa coi HTML và Android giống từng pixel.
- Node `test-preview.cjs` PASS: home/detail/direct CTA/back, 30 logo/đội, sân khách và scroll, lọc lịch, overview/list/confirm/blocked/fullscreen, cùng sân giữ camera/lọc và đổi sân reset. Test dùng DOM/Canvas doubles; ảnh/thao tác Chrome được kiểm riêng. `node --check mobile.js`, `team-logos.js`, `git diff --check`, parse XML resources và hash 30 PNG đạt. Có cảnh báo LF/CRLF ở file cũ, không phải lỗi whitespace.

Ảnh Android đã xem: [Home 360dp](../tmp/phase5/qa-360-final/phase5-qa/home.png), [Knicks detail 360dp](../tmp/phase5/qa-360-final/phase5-qa/nyk-detail.png), [Warriors detail 360dp](../tmp/phase5/qa-360-final/phase5-qa/gsw-detail.png); [Knicks detail 411dp](../tmp/phase5/qa-411-final/nyk-detail.png), [Warriors detail 411dp](../tmp/phase5/qa-411-final/gsw-detail.png). Artifact dưới tmp/.gradle là cục bộ, không tự stage.

### Lỗi trong quá trình QA và giới hạn

Build trung gian bắt sai tên string và text bị mất dấu khi ghi qua PowerShell; đã ghi lại UTF-8. Lint bắt paddingVertical trong style không hỗ trợ minSdk24; thay bằng paddingTop/Bottom. Test bắt formatter USD theo locale Việt bỏ cents; đã cố định hai chữ số thập phân. Test scroll ban đầu cuộn trước layout nên chưa tạo được offset; setup giờ chờ layout và xác nhận offset trước mở card. Ca bổ sung recreate ở detail bắt lỗi thật: offset trong ViewModel còn 280 nhưng UI quay về 0, vẫn thất bại khi chờ 45s. Đã bỏ capture onPause cũ với lịch ẩn, không refresh lịch ẩn, loại callback post khôi phục offset cũ; cả refresh và navigation khôi phục tại pre-draw, ViewModel là nguồn scroll thay vì cạnh tranh với saved-state riêng của ScrollView. Giữ nguyên yêu cầu UI phải về 280px; bản cuối cả 360/411dp qua. Lượt 3 PhaseFour hồi quy phía trên có trước phần bổ sung scroll này; hai ca PhaseFive cuối kiểm lại navigation đến sân trên bản mới.

Chrome bắt CTA poster thừa margin kế thừa và legend trắng trên nền sáng, đã sửa CSS. Console bắt ảnh logo mới dùng nhầm trường logoImage không có bounds của court renderer; đã tách teamBadgeUrl, giữ artwork cũ và thêm assertion contract vào host test. Không nới assertion để che lỗi. Emulator có hộp **System UI isn't responding** trong lượt đầu; đã chọn Wait và chạy lại, không nhận là lỗi app đã được sửa.

**Chưa thử trên Samsung thật**, chưa đo FPS/heap/GPU hoặc kiểm process death, chưa duyệt thủ công mọi đội/mọi hướng màn/font scale. Đã kiểm recreate/navigation/CTA trong emulator, không suy ra đã có nghiệm thu thẩm mỹ của người dùng. Bộ kiểm scale và hồ sơ P0 cũ vẫn cần theo dõi riêng. Lịch/giá/vé là mô phỏng. Dừng Phase 5, chờ duyệt; không có vấn đề phạm vi cần xin phép thêm.

### File tạo/sửa trong Phase 5

- Java: MainActivity; state/BrowseNavigation (mới), ArenaSelectionViewModel; ui/arena/BrowseController (mới), MatchPoster (mới), MatchPickerController, ArenaPreviewLoader.
- XML/resources: layout/browse_shell, calendar_screen và values/browse_styles (mới); activity_main cả dọc/ngang, match_card, match_filters, strings.
- Assets/tools: assets/logos/30 PNG + SOURCES.json (mới), tools/download-team-logos.py (mới). HTML: index.html/mobile.js/test-preview.cjs/README; browse.css và team-logos.js (mới).
- Tests: BrowseNavigationTest và PhaseFiveInstrumentedTest (mới); MatchCalendarInstrumentedTest, PhaseFourInstrumentedTest, ArenaPreviewInstrumentedTest điều chỉnh thao tác qua màn detail mới, giữ assertion nghiệp vụ.
- Tài liệu: PROGRESS.md, AI_LOG.md, ROADMAP.md, DESIGN.md. Những file khác đang dirty từ trước được giữ nguyên.

Commit đề xuất sau review: `feat: add NBA match discovery and detail navigation`. AI không commit/push.

Cập nhật: 2026-11-25 (Asia/Saigon).

## Tóm tắt

Người dùng đã giao nâng cấp màn sơ đồ Phase 3 và Phase 4 ngày 2026-11-25. **Đã triển khai chọn/bỏ nhiều ghế, đổi ghế có hủy, lọc tầng/khu, Bottom Sheet, góc nhìn mô phỏng và ViewModel giữ state khi xoay.** Header/theme lấy từ homeTeamId của trận mẫu đang xem. Build/unit/lint và test APK compile đạt: 65 JVM tests, lint 0 lỗi/26 cảnh báo. Chưa chạy test Android hoặc nghiệm thu Samsung do ADB không có thiết bị. Giữ nguyên dữ liệu và thiết kế 30 đội/626.904 ghế mô phỏng; chưa làm đặt vé/thanh toán, không commit/push. Các mục trước là lịch sử; bằng chứng Phase 4 ở cuối file.

## Cấu trúc Android

```text
nba-tickethub/
├── AGENTS.md
├── docs/
│   ├── REQUIREMENTS.md
│   ├── ROADMAP.md
│   └── PROGRESS.md
├── app/
│   ├── build.gradle.kts
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/nbatickethub/MainActivity.java
│       │   ├── res/
│       │   └── keepRules/rules.keep
│       ├── test/java/com/example/nbatickethub/ExampleUnitTest.java
│       └── androidTest/java/com/example/nbatickethub/ExampleInstrumentedTest.java
├── gradle/
│   ├── libs.versions.toml
│   ├── gradle-daemon-jvm.properties
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
├── .gitignore
└── local.properties (cấu hình riêng máy, được ignore)
```

Có các thư mục cục bộ `.gradle/` và `.idea/`. `MainActivity` thiết lập edge-to-edge và hiển thị layout có dòng `Hello World!`. Chưa thấy xử lý nghiệp vụ, tầng dữ liệu hoặc tích hợp backend trong mã hiện tại.

## Git — khảo sát Phase 0, trước khi repository xuất hiện

| Kiểm tra | Kết quả |
| --- | --- |
| `git --version` | Git 2.55.0.windows.2 |
| `.git` tại thư mục gốc | Không có |
| `git status --short --branch` | Thất bại: `fatal: not a git repository (or any of the parent directories): .git` |
| `git remote -v` | Cùng lỗi chưa phải repository |
| Branch, remote, lịch sử và file tracked | Chưa xác định được |

Ở thời điểm khảo sát Phase 0, AI không chạy `git init`, `git add`, commit hoặc push; chưa thể xác định tracked/untracked vì chưa có repository. Trạng thái Git mới ở cuối Phase 1 được ghi riêng bên dưới.

## Cấu hình đã đọc

| Hạng mục | Giá trị khai báo/quan sát |
| --- | --- |
| Tên dự án / module | `Nba TicketHub` / `:app` |
| Ngôn ngữ và UI hiện có | Java, Android Views/XML |
| Namespace / application ID | `com.example.nbatickethub` |
| minSdk / compileSdk / targetSdk | 24 / 37 / 37 |
| Phiên bản app | versionCode 1, versionName 1.0 |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0; có JAR, scripts và SHA-256 distribution trong properties |
| Java source / target | 11 / 11 |
| Gradle daemon JVM yêu cầu | 25, theo `gradle-daemon-jvm.properties` |
| Java từ terminal | OpenJDK 25.0.3; JAVA_HOME trỏ tới JBR của Android Studio |
| Android SDK cục bộ | Đường dẫn trong local.properties tồn tại, có platform `android-37.0` |
| Kho dependency | Google, Maven Central; plugin có thêm Gradle Plugin Portal |
| Quản lý dependency | Version catalog `gradle/libs.versions.toml` |
| Dependency ứng dụng | Activity KTX, AppCompat, ConstraintLayout, Material |
| Dependency test | JUnit 4, AndroidX JUnit, Espresso |
| Release | Có khai báo optimization và packageScope trong build script |
| Manifest | Một launcher Activity exported; có cấu hình backup và data extraction; chưa khai báo quyền mạng |

Java source 11 và JVM chạy Gradle 25 là hai cấu hình khác nhau; chưa có bằng chứng đây là lỗi. Chưa xác minh tương thích AGP/Gradle/dependency hoặc khả năng resolve artifact. Không tự đổi phiên bản hay package ID. Dependency KTX đang có trong template dù mã ứng dụng là Java; chưa thay đổi khi chưa cần.

## Rà soát `.gitignore`

- Có sẵn: cache Gradle, local.properties, một số file IDE, output build, capture và output native build.
- Đã bổ sung ở gốc: toàn bộ `.idea/`, cache Kotlin, `build/` ở mọi cấp, APK/AAB, heap dump, keystore (`*.jks`, `*.keystore`) và `.env`/`.env.*`; cho phép `.env.example` chỉ chứa giá trị mẫu.
- Giữ nguyên `app/.gitignore`; giữ các quy tắc IDE cũ để hạn chế chỉnh sửa không cần thiết.
- Gradle Wrapper, version catalog và cấu hình daemon JVM không bị chủ ý ignore; cần giữ để tái tạo môi trường.
- Ignore không thay thế việc kiểm tra bí mật trước khi đưa file lên Git, và không tự loại file đã tracked khi repository được thiết lập sau này.

## Công việc và bằng chứng — snapshot Phase 0–1

| Hạng mục | Trạng thái | Bằng chứng / giới hạn |
| --- | --- | --- |
| Khảo sát cấu trúc Android | Đã kiểm tra | Đã đọc settings, build scripts, catalog, manifest, activity, layout và test mẫu |
| Kiểm tra Git | Đã kiểm tra | Hai lệnh Git báo chưa phải repository |
| Tạo 4 file tài liệu | Đã tạo, chờ duyệt | AGENTS, REQUIREMENTS, ROADMAP, PROGRESS |
| Ghi yêu cầu chính thức | Đã đối chiếu nguồn trong Phase 1 | Đọc đủ 5 trang PDF; REQ-01–18, rubric và điểm khác nhau về số đối tượng đã ghi trong REQUIREMENTS |
| Kiểm tra cấu hình | Đã kiểm tra tĩnh | Có Wrapper, Java và SDK; chưa xác minh build |
| Bổ sung `.gitignore` | Đã chỉnh | Chỉ bổ sung quy tắc loại file cục bộ/output/bí mật |
| Gradle Sync / assembleDebug | Chưa chạy | Chưa có bằng chứng build thành công |
| Unit test / lint / instrumentation | Chưa chạy | Hai test hiện có là test mẫu, chưa chứng minh chức năng |
| Chạy app trên thiết bị/emulator | Chưa chạy | Chưa có bằng chứng runtime |
| Triển khai nghiệp vụ | Chưa thực hiện | Đúng phạm vi người dùng yêu cầu |
| Commit / push | Chưa thực hiện | Chưa có repository; người dùng chưa yêu cầu |

## Phase 1 — thiết kế hệ thống (2026-11-24)

| Công việc | Kết quả thực tế | Giới hạn |
| --- | --- | --- |
| Đọc AGENTS và tài liệu hiện có | Đã đọc cả quy tắc AI Usage Logging được bổ sung | Không tự coi nội dung file là người dùng đã review thiết kế |
| Đọc đề chính thức | Trích xuất text 5 trang; render bằng Windows.Data.Pdf và xem đủ 5 ảnh trang | Đối chiếu tài liệu, không phải kiểm thử ứng dụng |
| Phân tích rubric | Ghi 18 mã yêu cầu và trọng số 25/25/20/15/10/5 | Không khẳng định đã đạt điểm |
| Thiết kế | DESIGN có P0/P1, kiến trúc/package, model/quan hệ, màn/luồng, Matrix/gesture/hit-test/state, nguồn dữ liệu và test matrix | Toàn bộ là đề xuất, chưa được duyệt hoặc triển khai |
| Kế hoạch | ROADMAP theo phase, lịch một tuần tương đối và commit message dự kiến | Chưa biết ngày bắt đầu/hạn nộp, chưa có commit thật |
| AI log | Ghi prompt, phần AI hỗ trợ, file đổi, hạn chế và trạng thái quyết định | Không ghi nhận sinh viên đã review/hiểu/kiểm thử |
| Git ở đầu phase | `git status --short --branch` báo chưa phải repository | Đây là quan sát trước khi `.git` xuất hiện trong phiên |
| Git ở cuối phase | `git status`: `No commits yet on main`; origin fetch/push là `https://github.com/m1hdat/nba-tickethub.git`; `git ls-files` rỗng | `.git` xuất hiện trong lúc làm tài liệu, không do AI khởi tạo. Chưa có commit/staged file; chưa kiểm chứng remote online hoặc quyền truy cập |

Phạm vi file tài liệu thay đổi Phase 1: tạo `docs/DESIGN.md`, `docs/AI_LOG.md`; cập nhật `docs/REQUIREMENTS.md`, `docs/ROADMAP.md`, `docs/PROGRESS.md`. Không sửa AGENTS, `.gitignore`, application code hoặc cấu hình build.

### Kiểm chứng tài liệu và phạm vi thay đổi

- Đã đọc text và ảnh của đủ 5 trang nguồn; rubric trang 4 xác nhận 150–250 đối tượng, trong khi mục 2.1/checklist ghi 50–250. Chọn 200 là đề xuất bao phủ cả hai.
- Kiểm tra tự động đạt: 6 file Markdown (AGENTS + 5 file docs) có UTF-8 và code fence hợp lệ; 15 link cục bộ tồn tại; REQ-01–18 có truy vết trong DESIGN. So sánh SHA-256 40 file mã ứng dụng/Gradle/ignore/AGENTS trước và sau khi ghi tài liệu: không thay đổi, không thêm/bớt file trong tập kiểm tra.
- Tính thử số học cho một ví dụ world→screen→world và zoom giữ tâm: đạt. Đây chỉ là đối chiếu công thức trong tài liệu bằng Python, không phải test Matrix Android hoặc test ứng dụng.
- Chưa chạy Gradle Sync, build, unit test, lint, instrumentation, emulator/device, gesture test hay performance profiling. Test matrix trong DESIGN chỉ là kế hoạch.
- Chưa có review hoặc phê duyệt của người dùng cho Phase 1; không có kết quả nghiệm thu application.

## Điểm cần xác nhận tiếp theo

- Review [DESIGN.md](DESIGN.md), quyết định D-01–D-07: loại vị trí, scope checkout, dữ liệu/asset, fixture/tiền, kiến trúc/package, filter-focus và lịch/repository.
- Xác nhận dùng origin `m1hdat/nba-tickethub`; hiện branch `main` chưa có commit, chưa có file tracked và chưa kiểm tra remote online.
- Chỉ sau khi người dùng thống nhất thiết kế mới bắt đầu Phase 2. Khi được làm tiếp, ghi lệnh/kết quả môi trường thật; không suy ra build thành công từ cấu hình tĩnh.

Commit message đề xuất cho tài liệu Phase 1: `docs: define NBA ticket purchasing system design and phase 1 plan`. **AI không chạy init/add/commit/push.** File untracked có cả nền dự án và PDF đã có trước thay đổi tài liệu của phase; không coi toàn bộ untracked là phần AI mới tạo.

## Đối chiếu thiết kế với AGENTS mới — 2026-11-24

- Nhiệm vụ thực tế: đối chiếu lại DESIGN với quy định hiện hành, tập trung ArenaView/Canvas. Đây là rà soát tài liệu trong Phase 1, không mở phase triển khai.
- Đã đọc AGENTS mới, REQUIREMENTS, ROADMAP, PROGRESS, DESIGN và AI_LOG. Không sửa AGENTS hoặc tự coi các quyết định D-01–D-07 là đã được người dùng duyệt.
- Kết luận: thiết kế cũ phù hợp về hướng; cần viết tường minh `ArenaView extends View`, `onDraw(Canvas)`/Paint, không ảnh sơ đồ tĩnh và test tọa độ/hit-test/selection bắt buộc khi triển khai.
- Đã bổ sung DESIGN mục 6.0: hợp đồng một Canvas, renderer hỗ trợ trên chính Canvas đó, ranh giới state/transform/hit-test và bảng 12 mục đối chiếu. Làm rõ đồng bộ palette/legend/filter và D-05 không mở lại lựa chọn công nghệ trái AGENTS.
- File thay đổi của nhiệm vụ này: `docs/DESIGN.md`, `docs/PROGRESS.md`, `docs/AI_LOG.md`. Không sửa application code, cấu hình build, đề gốc, REQUIREMENTS hoặc ROADMAP; kế hoạch chưa có thay đổi được xác nhận.
- Kiểm chứng đã chạy bằng Python: 6 file Markdown có UTF-8/code fence/bảng hợp lệ, 16 link cục bộ tồn tại; tập đường dẫn và SHA-256 của 42 file ngoài phạm vi không đổi (mã app, Gradle, AGENTS/ignore, REQUIREMENTS, ROADMAP, PDF). Không chạy Gradle/build/test/lint/runtime; chưa có ArenaView implementation để chứng minh tuân thủ bằng thực thi.
- Git đầu nhiệm vụ: `No commits yet on main`, các file dự án vẫn untracked; AI không init/đổi branch/add/commit/push.
- Chưa có người dùng phê duyệt hoặc nghiệm thu thiết kế; dừng để người dùng review. Commit message đề xuất: `docs: align ArenaView design with updated Canvas rules`.

## Cập nhật phạm vi toàn arena — 2026-11-24

- Người dùng đã xác nhận phạm vi toàn NBA Arena: court trung tâm, nhiều tầng/section/hàng và từng ghế; kiến trúc hỗ trợ hàng nghìn đến hàng chục nghìn ghế. 200 chỉ còn fixture kiểm thử ban đầu, không giới hạn sản phẩm cuối. Đây là quyết định phạm vi đã có thật, chưa phải phê duyệt tất cả giải pháp kỹ thuật.
- Đã sửa DESIGN/ROADMAP theo hierarchy Arena→Level→Section→Row→Seat, schema không khóa số lượng, zoom theo kích thước ghế, LOD/culling/index bắt buộc, worker/version/cache và chiến lược kiểm tra 200/1k/10k/30k/50k.
- Đã làm rõ sơ đồ/capacity/giá/status đều mô phỏng và metadata thực phải có nguồn/ngày, tách snapshot với live. Không có dữ liệu sân NBA thực nào được xác minh trong lượt này.
- Đã bổ sung đính chính vào chính mục đối chiếu trước trong AI_LOG: tuân thủ Canvas không chứng minh thiết kế 200 ghế đủ cho toàn arena. Giữ nguyên lịch sử prompt và kết quả kiểm tra cũ.
- Phạm vi file sửa: DESIGN, ROADMAP, AI_LOG và PROGRESS. Không sửa code, build, AGENTS, REQUIREMENTS hoặc PDF; con số 200 trong REQUIREMENTS được DESIGN giải thích là phương án fixture rubric ban đầu, không yêu cầu trần sản phẩm.
- Cần làm rõ cách nộp full-arena với giảng viên do rubric gốc ghi 150–250 đối tượng; không tự coi con số giới hạn được thay bằng quy mô lớn. Lịch một tuần cần đánh giá lại; không giảm scope về một khu 200 để vừa lịch.
- Kiểm chứng bằng Python đạt: 6 Markdown file có UTF-8/code fence/bảng hợp lệ, 16 link cục bộ tồn tại, đủ REQ-01–18 và D-01–D-09; kiểm tra bỏ trần zoom 8×fit/đường hit-test tuyến tính/bố cục 200 làm giới hạn trong DESIGN. Tập đường dẫn và SHA-256 41 file mã/cấu hình/nguồn ngoài phạm vi không đổi. Chưa triển khai/test generator/index/LOD hoặc chạy Android/build/lint/benchmark; chưa có kết quả quy mô lớn được chứng minh.
- Git đầu nhiệm vụ: main chưa có commit; AI không init/add/commit/push. Dừng ở tài liệu để người dùng review D-01–D-09; không hỏi lại phạm vi toàn arena đã chốt.
- Commit message đề xuất: `docs: expand ArenaView design to full arena and scalable seat rendering`.

## Phase 2 — Project Foundation & Data Model (2026-11-24)

Trạng thái: **Đã triển khai phần code trong phạm vi giao; kiểm tra JVM đạt; Android build chưa đạt trong môi trường hiện tại; chưa được người dùng review/nghiệm thu.** Không tự chuyển Phase 3.

### Model và luồng đọc

- `Team`, `Game`: đội/trận demo; Game tham chiếu hai đội và arena, có mô tả thời gian mô phỏng.
- `Arena → ArenaLevel → Section → SeatRow → Seat`: hierarchy vật lý độc lập inventory. Arena có world/court bounds; level/section/row có footprint vành khuyên; Seat có ID, rowId, số vị trí, kind và geometryIndex, không chứa giá hoặc trạng thái theo trận.
- `GameSeat`: thông tin offer/vé theo khóa (gameId, seatId), gồm Money, baseStatus và provenance. Đây chưa phải vé đã mua/Booking/My Tickets. Cùng Seat có giá/trạng thái khác nhau ở hai trận.
- `WorldRect`, `AnnularSector`, `SeatGeometry`: hình học logic Java thuần, không Canvas/Matrix/screen coordinates. SeatGeometry giữ bốn đỉnh bất biến; compiler mock hiện sinh rect, validator kiểm được cả quad xoay.
- `Money` lưu long minor units và mã tiền; giá mock nhân hệ số từng trận bằng số nguyên, làm tròn half-up, phát hiện overflow. `SeatStatus` chỉ có AVAILABLE/BOOKED/LOCKED; chưa có selection state.
- `DataProvenance` phân biệt MOCK/REAL_SNAPSHOT/LIVE_VERIFIED. Reader mock chỉ chấp nhận MOCK; toàn bộ sơ đồ/sức chứa/lịch/giá/inventory hiện tại là dữ liệu tổng hợp, không có nguồn NBA thực đã được xác minh.
- `ArenaDataset` giữ map ID, flat seat order, geometry và inventory bất biến; constructor chỉ trả về khi validator đạt. Mảng đỉnh được clone; collection ngoài không sửa được dữ liệu.

Luồng blocking (chỉ gọi trên worker): `assets/mock/*.json → AndroidAssetSource → MockArenaRepository → StrictJson → MockArenaJsonReader → ArenaGeometryCompiler → ArenaDataset/ArenaDataValidator`.

`AndroidAssetSource` chỉ giữ AssetManager và chặn gọi trên main thread. Repository nhận AssetSource để unit test không phụ thuộc Android; đóng stream kể cả khi lỗi, báo tên asset và từ chối path traversal/UTF-8 lỗi. Phase này chưa nối repository vào MainActivity; UI template giữ nguyên. Executor/UI state sẽ được nối khi có nhiệm vụ sau, không đọc assets trong onDraw.

### JSON và hình học toàn arena

- Manifest v1 có provenance, arena/court, teams, games và mockInventory từng trận, levels, sections; mỗi section có angular span, rowCount, seatsPerRow và giá nền mock. Dữ liệu hình học/ghế được sinh xác định theo version, ID và seed, không khai báo tay hàng chục nghìn ghế.
- Compiler tạo các vành tầng không chồng nhau quanh court trung tâm, nhiều section với khoảng cách góc, row radial ranges, seat ID riêng và tọa độ world. World units là đơn vị mô phỏng, không phải kích thước đo tại sân NBA.
- Validator kiểm ID trùng, FK, hai đội khác nhau, schema version, section/row/range/geometryIndex, thứ tự hàng, hierarchy containment, số ghế/geometry, quad hợp lệ, shape chồng nhau/đè court, inventory đầy đủ, currency theo trận và giá hợp lệ.
- Private grid/SAT trong validator chỉ phát hiện hình học chồng lấn; chưa cung cấp query touch/viewport hay ArenaSpatialIndex cho UI. Không triển khai hit-testing tương tác, pan/zoom hoặc chọn ghế.
- Không có giới hạn domain 200. Budget tài nguyên mặc định: 100.000 vị trí, 500.000 offers, manifest tối đa 1.048.576 ký tự/depth 64. Seat/offer budget truyền được qua constructor; số lượng vượt budget bị báo lỗi trước cấp phát lớn, không bị cắt âm thầm. Budget không phải sức chứa NBA thực hoặc giới hạn schema.
- StrictJson dùng Java chuẩn để giữ nguyên Gradle/dependency và kiểm tra cùng parser trên Android/JVM; xử lý số chính xác, escape/Unicode, từ chối duplicate key/trailing content/syntax lỗi. Chỉ dùng cho manifest mock v1 đang có; tích hợp provider thật cần nhiệm vụ riêng.

| Manifest / test | Tầng | Section | Hàng | Tổng vị trí | Ghế ngồi | Vị trí đứng | Trận |
| --- | --- | --- | --- | --- | --- | --- | --- |
| arena-small.json | 3 | 20 | 40 | 200 | 190 | 10 | 2 |
| arena-10k.json | 3 | 20 | 200 | 10.000 | 9.500 | 500 | 2 |
| arena-30k.json | 3 | 20 | 600 | 30.000 | 28.500 | 1.500 | 2 |
| Test biến rowCount từ 30 lên 50 của manifest 30k | 3 | 20 | 1.000 | 50.000 | 47.500 | 2.500 | 2 |

50k chỉ là cấu hình phát sinh trong unit test, chưa tạo asset thứ tư. Tất cả profile dùng cùng reader/compiler/validator. Kết quả load/validate quy mô lớn không chứng minh frame time hoặc hit-testing/rendering trên Android.

### File tạo mới và thay đổi

Prefix mã chính: `app/src/main/java/com/example/nbatickethub/`. Có **25 file Java mới**, chỉ trong model/data/geometry; không tạo package state/ui rỗng.

| Thư mục dưới prefix | File mới |
| --- | --- |
| model/ | Team.java, Game.java, Arena.java, ArenaLevel.java, Section.java, SeatRow.java, Seat.java, GameSeat.java |
| model/ | Money.java, SeatStatus.java, PositionKind.java, DataProvenance.java, WorldRect.java, AnnularSector.java, SeatGeometry.java, ModelChecks.java |
| data/ | ArenaDataset.java, ArenaDataValidator.java, ArenaRepository.java, AssetSource.java, AndroidAssetSource.java |
| data/json/ | StrictJson.java |
| data/mock/ | MockArenaJsonReader.java, MockArenaRepository.java |
| geometry/ | ArenaGeometryCompiler.java |

Các file còn lại:

- `app/src/main/assets/mock/arena-small.json`, `arena-10k.json`, `arena-30k.json` (3 manifest mới).
- `app/src/test/java/com/example/nbatickethub/data/ModelTest.java`, `JsonParsingTest.java`, `ArenaDataTest.java`, `RepositoryTest.java`, `TestAssets.java` (4 test class + 1 helper mới).
- `tools/test-foundation.ps1` (runner kiểm tra JVM dự phòng, output dưới `.gradle/`, không thay APK/Gradle verification).
- Cập nhật `docs/PROGRESS.md`, `docs/AI_LOG.md`, `docs/ROADMAP.md` theo tiến độ và giới hạn Phase 2 được giao.

Không sửa MainActivity/XML/manifest, Java/SDK/AGP/Gradle/dependency, AGENTS/REQUIREMENTS/DESIGN hoặc PDF. `docs/sources/` đã untracked từ đầu lượt, không phải file AI mới tạo trong Phase 2. Git đầu lượt: main theo origin/main, HEAD `a4e6788`, có 3 commit trước đó; AI không tạo các commit này.

### Kiểm tra thực tế

| Kiểm tra / lệnh | Kết quả thực tế | Giới hạn |
| --- | --- | --- |
| `./gradlew.bat --offline --no-daemon assembleDebug testDebugUnitTest lintDebug` | Lần đầu không tạo được lock tại C:\\.gradle | Chưa tới compile |
| Wrapper với GRADLE_USER_HOME trong workspace và cache dependency có sẵn chỉ đọc | Qua cấu hình plugin sau khi sửa đường dẫn GRADLE_RO_DEP_CACHE; còn thiếu AAPT2 artifact offline | Không thay Gradle files; không coi configuration là build đạt |
| Wrapper dùng binary AAPT2 đúng 9.4.1-15978811 đã có trên máy, cache/user-home trong workspace và tắt configuration-cache qua CLI | Lượt cuối `BUILD FAILED`, task `:app:compileDebugJavaWithJavac`: `AccessDeniedException` đối với SDK platform android-37.0/android.jar; lượt trước còn báo core-for-system-modules.jar | Dừng trước APK và Gradle test/lint; AAPT2 override là thử nghiệm tạm thời, không ghi vào cấu hình |
| Java compile `--release 11 -encoding UTF-8 -Xlint:all` qua tools/test-foundation.ps1 | Đạt cho 25 file Java mới; AndroidAssetSource compile riêng với bản sao SDK jar cục bộ | Không phải DEX/APK hoặc runtime Android |
| JUnit 4.13.2 qua tools/test-foundation.ps1 | **OK (31 tests)**: 30 test mới + 1 ExampleUnitTest cũ; lượt cuối 1.644 giây do JUnit báo | Chưa chạy Gradle test task; test mẫu không chứng minh nghiệp vụ |
| Unit test quy mô lớn | Đọc/validate 10k, 30k và 50k vị trí đạt; so inventory/physical seats, deterministic ID/geometry/status, immutable data, dữ liệu lỗi và stream handling | Chưa benchmark render/FPS/touch/memory trên thiết bị |
| `git diff --check`, kiểm tra phạm vi, UTF-8/JSON/link | Đạt: 6 Markdown file/17 link cục bộ hợp lệ, 3 JSON parse được và gắn MOCK; 25 Java mới/5 file test-helper/runner không lỗi UTF-8 hoặc trailing whitespace; SHA-256 của 35 file được bảo vệ không đổi | Chưa có người dùng review/nghiệm thu; Git chỉ cảnh báo chuyển LF→CRLF theo cấu hình sẵn có, không phải lỗi diff |

Lệnh JVM runner đã thực sự chạy (PowerShell tại root):

```powershell
powershell -NoProfile -File tools/test-foundation.ps1 `
  -DependencyCacheRoot C:/Users/Dell/.gradle/caches/modules-2/files-2.1 `
  -AndroidJarPath C:/Users/Dell/AppData/Local/Android/Sdk/platforms/android-37.0/android.jar
```

Runner sao chép JUnit/Hamcrest/SDK jar được dùng sang `.gradle/foundation-check` để tránh lỗi truy cập jar bên ngoài workspace. Một lần javac đọc trực tiếp JUnit cache báo AccessDenied trong compiler cleanup; đã khắc phục runner bằng bản sao cục bộ và chạy lại sạch. Không sửa JDK/SDK hoặc quyền máy.

Lệnh Gradle cuối sử dụng các biến môi trường và cờ tạm thời dưới đây; log cục bộ nằm tại `.gradle/phase2-gradle-final.log`:

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle/phase2-home"
$env:GRADLE_RO_DEP_CACHE = 'C:/Users/Dell/.gradle/caches'
$env:ANDROID_USER_HOME = "$PWD/.gradle/android-user-home"
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$PWD/.gradle/runtime-home"
./gradlew.bat --offline --no-daemon --no-configuration-cache `
  "-Pandroid.aapt2FromMavenOverride=$PWD/.gradle/phase2-tools/aapt2.exe" `
  assembleDebug testDebugUnitTest lintDebug
```

Các đường dẫn cache/SDK ở trên là môi trường kiểm tra hiện tại, không phải cấu hình cần commit. Binary AAPT2 và Wrapper distribution đã có trên máy được sao chép vào `.gradle/` cho lượt kiểm tra này; thư mục này bị ignore. Chưa có APK được xác minh, chưa chạy emulator/device hoặc connectedDebugAndroidTest.

### Việc còn lại trước Phase 3

- Người dùng review code/model/manifest và kết quả Phase 2. Chưa ghi nhận người dùng đã đọc, hiểu, tự test hoặc nghiệm thu.
- Chạy lại Gradle assembleDebug/testDebugUnitTest/lintDebug trong môi trường truy cập SDK bình thường, xử lý lỗi nếu có; kết quả JVM không thay thế việc này.
- Giữ ArenaView, Canvas/LOD/culling, spatial index tương tác, Matrix/pan/zoom/hit-test/selection, Booking, Smart Seat Finder và My Tickets chưa triển khai theo giới hạn giao việc.
- Commit message đề xuất: `feat: add arena data foundation and validated mock assets`. AI không stage/commit/push.

## Review Phase 2 và khảo sát nguồn — 2026-11-25

- AI đã review source/data/compiler/validator/test và tài liệu; chạy lại JVM runner: **31 test đạt** (30 mới + 1 mẫu), JUnit báo 1.773 giây. AndroidAssetSource chỉ compile, chưa chạy trên Android.
- Phát hiện validator P2 đã tái hiện bằng probe riêng: bốn đỉnh quad đều ở trong row nhưng cạnh có trung điểm bán kính 339.899961 < innerRadius 340; ArenaDataset vẫn nhận. Chi tiết và vị trí code tại [SOURCES.md](SOURCES.md). Chưa sửa code hoặc thêm regression test trong nhiệm vụ review này; chưa kết luận mock assets hiện hành sai.
- Build Android chưa được xác minh. Lượt này không chạy lại Gradle; chỉ đọc lại log Phase 2 báo AccessDeniedException ở SDK jar. Không ghi test JVM là APK/runtime đạt.
- Tạo SOURCES: nguồn primary, giấy phép, API Java/Views/XML, điều kiện SDK/JDK, ưu tiên theo phase và các nguồn chưa xác minh đủ. ArenaView vẫn tự triển khai Canvas/Paint/Matrix; không thư viện renderer thay thế. Không import dataset, code bên ngoài hoặc dependency mới.
- File sửa/tạo trong lượt: `docs/SOURCES.md`, `docs/AI_LOG.md`, `docs/PROGRESS.md`. Giữ nguyên application/test/manifest/build/catalog/assets/runner và roadmap/design. Probe nằm ở `.gradle/phase2-review/`, không phải Phase 3.
- Kiểm tra cuối đạt: 7 Markdown file có UTF-8/code fence/bảng hợp lệ, 26 link cục bộ tồn tại; SOURCES có 17 mục nguồn. SHA-256/tập đường dẫn 73 file mã/test/assets/config/runner/tài liệu thiết kế và roadmap ngoài phạm vi không đổi; git diff --check đạt, staged diff rỗng. Chưa có review/nghiệm thu hoặc quyết định chọn thư viện cụ thể từ người dùng.
- Commit message đề xuất: `docs: review phase 2 and catalog licensed reference sources`. AI không stage/commit/push; chưa chuyển Phase 3.


## Phase 3A — dữ liệu đa sân và Canvas preview, 2026-11-25

### Đã thực hiện và phạm vi

- Đọc AGENTS, REQUIREMENTS, DESIGN, ROADMAP, PROGRESS, SOURCES và source/test/config; kiểm tra Git `main...origin/main`. Chạy lại nền 31 test trước sửa: đạt (2.217 giây qua runner JVM). Không suy ra người dùng đã review Phase 2 từ việc giao nhiệm vụ mới.
- Sửa lỗi containment đã ghi ở review trước; thêm regression đúng quad có edge đi vào inner hole. Không triển khai hit-testing touch.
- Catalog JSON ba sân, source identity CC0 tách MOCK geometry; layout khác nhau ở bowl shape, tỷ lệ, tầng, section/row density, gap và court angle mô phỏng. MSG 42 section/606 hàng/16.908 ghế; Chase 34/576/17.584; Crypto 50/656/19.056. Không coi số ghế này là capacity thật. Chỉ ba sân khả dụng, catalog/renderer không hard-code giới hạn 3/200; test 30 entries là profile tổng hợp chỉ cho kiểm thử cấu trúc.
- Worker đọc/validate/generate/project geometry bất biến và cache Path; ArenaView tự vẽ full court + stands/rows/quads/status bằng Canvas/Paint. onDraw không I/O, không sinh hình học hoặc object theo ghế. Chưa có LOD/culling/camera Matrix/touch/selection nghiệp vụ.
- Launcher MainActivity dùng preview XML dọc/ngang, Spinner, tên đội, counts, legend và nhãn mô phỏng. Reset disabled ghi 3B, debug ghế ghi 3C. Giữ venue ID qua recreation; loader có cancel/version guard/lifecycle close. Kiểm chứng runtime vẫn chưa diễn ra.
- Không thêm dependency, đổi Gradle/SDK/Java cấu hình dự án hoặc sửa AndroidManifest. Không WebView/ảnh sơ đồ/logo/per-seat View; không nhập mã vendor.

### File tạo/thay đổi

Đường dẫn Java dưới `app/src/main/java/com/example/nbatickethub/`:

- Sửa `MainActivity.java`, `data/ArenaDataValidator.java`.
- Tạo `model/ArenaProfile.java`, `data/ArenaCatalog.java`, `data/ArenaCatalogReader.java`, `geometry/ArenaScene.java`, `geometry/ArenaSceneCompiler.java`.
- Tạo `ui/arena/ArenaView.java`, `ui/arena/ArenaRenderData.java`, `ui/arena/ArenaPreviewLoader.java`.
- Assets mới: `app/src/main/assets/venues/catalog.json` và `app/src/main/assets/mock/{msg,chase,crypto}.json`. Các fixture Phase 2 giữ nguyên.
- Sửa `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/strings.xml`; tạo `layout-land/activity_main.xml`, `drawable/preview_selector.xml`, `values/preview_colors.xml`.
- Sửa `app/src/test/java/com/example/nbatickethub/data/ArenaDataTest.java`; tạo `ArenaPreviewDataTest.java` cùng package.
- Tạo `app/src/androidTest/java/com/example/nbatickethub/ArenaPreviewInstrumentedTest.java` (hai ca mới, chưa chạy thiết bị).
- Tạo `tools/generate-preview-assets.py`; mở rộng runner có sẵn `tools/test-foundation.ps1` để chạy test preview. `tools/` và `docs/sources/` đã là untracked từ đầu lượt; không xóa hoặc ghi đè các nguồn đề của người dùng.
- Cập nhật `docs/DESIGN.md`, `docs/ROADMAP.md`, `docs/PROGRESS.md`, `docs/SOURCES.md`, `docs/AI_LOG.md`. Log/cache/SDK copy/build mirror ở `.gradle/` bị ignore, không đưa vào commit.

### Kiểm chứng thực tế

| Kiểm tra | Kết quả quan sát | Giới hạn |
| --- | --- | --- |
| JVM runner sau sửa | `OK (36 tests)`; lượt cuối 2.821 giây, gồm kiểm tra cạnh projected quad trong row | Java thuần, không phải UI |
| Wrapper `assembleDebug testDebugUnitTest assembleDebugAndroidTest` | **BUILD SUCCESSFUL**, lượt cuối 53 giây; 67 task, 8 executed/59 up-to-date | Trong `.gradle/phase3a-project`, cùng source, chỉ local SDK path/cache/AAPT2 override khác do sandbox |
| Gradle unit XML reports | 36 tests, 0 failures/errors/skipped; 6 suite; regression validator và ba venue dataset đạt | 35 test foundation/preview + 1 test mẫu; không bao gồm instrumented |
| APK ứng dụng / APK test | Đã tạo `app-debug.apk` và `app-debug-androidTest.apk` trong mirror `app/build/outputs/apk/` | Build APK test không có nghĩa đã chạy test |
| Wrapper `lintDebug` | **Không chạy được lint analysis**: thiếu `com.android.tools.lint:lint-gradle:32.4.1`; offline cache thiếu; lượt online `Permission denied: getsockopt` khi tải POM từ Google Maven | Không có báo cáo lint đạt; không suppress lỗi hoặc đổi version để vượt kiểm tra |
| ADB `devices -l` | Lỗi fatal `Cannot mkdir '\.android': Permission denied` | Không liệt kê được thiết bị; không tuyên bố đã chạy hoặc xác minh không có thiết bị kết nối |
| Connected tests/UI/FPS | **Chưa chạy/chưa đo** | Chưa xác nhận bố cục đẹp, gesture, accessibility hoặc hiệu năng trên emulator/device |

Lượt build trước phát hiện gọi `WorldRect.width()/height()` không tồn tại; AI đã sửa dùng right-left/bottom-top và build lại đạt. Lượt combined có lint ban đầu dừng vì thiếu artifact; tách build/test APK để xác minh riêng. Lỗi truy cập SDK jar ở Phase 2 được tránh trong bản sao kiểm chứng bằng cách sao chép SDK jars/tools đã có vào workspace; không sửa SDK gốc hoặc `local.properties` gốc. Không coi đây là bằng chứng lệnh root plain chạy thành công trong sandbox.

Lệnh JVM:

```powershell
./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'
```

Lệnh Wrapper thực tế ở mirror (các biến môi trường chỉ trong process kiểm tra):

```powershell
$repoRoot = $PWD.Path
$mirror = Join-Path $repoRoot '.gradle/phase3a-project'
$env:GRADLE_USER_HOME = Join-Path $repoRoot '.gradle/phase2-home'
$env:ANDROID_USER_HOME = Join-Path $repoRoot '.gradle/android-user-home'
$env:JAVA_TOOL_OPTIONS = "-Duser.home=$repoRoot/.gradle/runtime-home"
# GRADLE_RO_DEP_CACHE không được đặt; dependencies có sẵn được copy vào cache writable.
& "$mirror/gradlew.bat" -p $mirror --offline --no-daemon --no-configuration-cache `
  "-Pandroid.aapt2FromMavenOverride=$repoRoot/.gradle/phase2-tools/aapt2.exe" `
  assembleDebug testDebugUnitTest assembleDebugAndroidTest
# lintDebug thử cả offline và online (bỏ --offline ở lượt online).
```

Log: `.gradle/phase3a-verified.log`, `.gradle/phase3a-lint.log`. Báo cáo JUnit: `.gradle/phase3a-project/app/build/test-results/testDebugUnitTest/`. APK preview: `.gradle/phase3a-project/app/build/outputs/apk/debug/app-debug.apk`. Mirror có settings/build/version catalog/Wrapper giống root; `local.properties` mirror trỏ `.gradle/sdk-mirror`. Không cần sao chép cấu hình kiểm chứng này vào dự án để Run từ Android Studio.

### Cách người dùng chạy và review

1. Mở **thư mục repository gốc** trong Android Studio. Giữ JDK/SDK/Gradle dự án đã chọn; Sync Gradle. Cần truy cập Google Maven để resolve lint artifact còn thiếu nếu chạy lint.
2. Device Manager → khởi động emulator API 24 trở lên (hoặc thiết bị thật), chọn module `app`, bấm Run. MainActivity launcher mở Arena Preview, không cần Intent đặc biệt.
3. Chờ counts hiện; lần lượt chọn MSG, Chase, Crypto. So sánh đường bao/tầng/gap/court angle và các counts ở trên; kiểm tra màu legend và nhãn SIM/*, court lines, section labels ở dọc/ngang. Xoay khi đang xem Crypto để xem có phục hồi đúng sân.
4. 3A là preview tĩnh: không kiểm tra pan/pinch/tap chọn ghế tại mốc này; Reset và seat debug đang ghi phase sẽ bổ sung. Ghi lại screenshot/nhận xét để quyết định trước 3B.
5. Ở môi trường truy cập SDK/network/device bình thường, chạy tại root:

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
./gradlew.bat connectedDebugAndroidTest
```

Chỉ chạy connected khi emulator/device khả dụng. AI chưa chạy lượt connected; việc hướng dẫn không phải bằng chứng người dùng đã làm.

**Điểm dừng:** chờ review giao diện và chấp nhận mức mô phỏng; còn thiếu lint/runtime inspection. Không tự triển khai 3B/3C/4, commit hoặc push. Đề xuất commit: `feat: add data-driven Canvas previews for three simulated NBA arenas`.

Kiểm tra cuối 3A: SHA-256 và tập đường dẫn **76 file source/assets/resource/test** trong mirror giống root; **9 file Gradle/catalog/Wrapper** giống root (chỉ local.properties mirror khác có chủ đích). Resource XML parse đạt; 7 Markdown file có fence cân bằng và 28 link cục bộ tồn tại; `git diff --check` đạt, staged diff rỗng. APK cuối 7.003.344 byte, SHA-256 `86a697e0ffb45c4152ec5d845d2c520753ac14bbe0050489022c07c4da1bd701`. Đây là kiểm tra artifact/source, không phải kiểm tra giao diện.


## Chẩn đoán treo emulator và HTML Preview — 2026-11-25

### Báo cáo người dùng và mức kết luận

Người dùng tự chạy ứng dụng trên Android Emulator và báo dialog **“Process system isn't responding”**. Đây là báo cáo runtime từ người dùng; AI chưa tự quan sát dialog hoặc tái hiện. Chưa có thời điểm chính xác so với boot/app launch/đổi sân, package gây ANR, stack hay system trace. **Chưa thể quy nguyên nhân cho ứng dụng, JSON, Canvas, GPU hoặc RAM; chưa tuyên bố đã sửa xong lỗi treo.** AI đã hỏi bổ sung thời điểm xuất hiện bằng câu hỏi tùy chọn, chưa nhận trả lời tại lúc ghi mục này.

### Kiểm tra đã thực hiện

| Hạng mục | Bằng chứng hiện tại | Kết luận/giới hạn |
| --- | --- | --- |
| Logcat | Chạy ADB devices và logcat system/events/crash, kể cả dùng Android user directory writable | Client lỗi `Cannot mkdir '\.android': Permission denied`; `.gradle/anr-logcat-attempt.txt` chỉ chứa lỗi công cụ, không phải Logcat thiết bị |
| Android Studio | Phát hiện cửa sổ project, thử đọc qua computer-use | Công cụ trả `Computer Use app approval timed out`; không đọc được Logcat UI. Không đổi quyền máy hoặc giả vờ đã thấy stack |
| IDE log lưu trên host | Thử tìm log, đọc thư mục Google dưới Local AppData | Bị AccessDenied; không kết luận “không có log” chỉ từ lỗi quyền |
| Emulator/config | Lúc kiểm tra có Studio/adb, không thấy process emulator/qemu; đọc config.ini/hardware-qemu.ini của Medium_Phone | Profile API 37 x86_64/4 core; config RAM 2.048 MB/GPU auto, **runtime config gần nhất RAM 4.096 MB/GPU host**. Không sửa cấu hình; chưa biết mức RAM trống hoặc lỗi driver; CIM memory cũng bị AccessDenied |
| JSON/main thread | MainActivity chỉ gọi loader; loader dùng single ExecutorService; AndroidAssetSource từ chối mở assets trên main Looper | Không tìm thấy đường JSON parsing/I/O main thread trong source. Catalog và arena load đều trên worker; instrumented test mới xác minh guard/worker/callback nhưng chưa chạy thiết bị |
| Generation/validation | Compiler tạo từng row/seat; scene projection tiền tính bất biến trên worker. Mock JSON là cấu hình gọn, không khai báo 20k seat records để parse trực tiếp | O(N) phần generation/projection; validator dùng broad-phase grid + SAT, chi phí theo candidate chứ không bảo đảm tuyến tính worst-case. JVM timings không thay Android profile |
| onDraw | Không JSON, sinh ghế/Path hay vòng request redraw; Paint/RectF/geometry cache | Vẫn vẽ toàn snapshot, O(số path + tổng độ phức tạp hình học + labels); ba drawPath lớn trước đây không có nghĩa công việc render O(1) |
| Allocation/invalidate | Không new object theo ghế trong onDraw; chỉ một điểm invalidate ở setRenderData | Không có invalidate loop/postDelayed/animation loop trong source. Bổ sung guard cùng snapshot; slow-draw log tối đa một lần/snapshot, có tạo string chẩn đoán ở nhánh chậm đó |
| Rủi ro tìm thấy | Bản cũ gộp toàn bộ ghế vào 3 Path trạng thái, hàng toàn sân vào 1 Path | Có khả năng chi phí tessellation/upload/texture lớn dù chuẩn bị trên worker. **Đây là giả thuyết/rủi ro render, không phải root cause ANR đã có stack xác nhận** |

Các nguồn Android primary và giới hạn áp dụng được thêm vào SOURCES mục 8. Không giảm số ghế, tắt validation, đổi SDK/Gradle, vô hiệu hóa hardware acceleration hoặc tự sửa AVD để né lỗi.

### Thay đổi trong tương tác này

- `geometry/SeatPathBatches.java`: nhóm theo section/status, tối đa **256 ghế mỗi Path**, không bỏ hay gộp ghế thành đại diện. Các snapshot có 155/138/187 batch tương ứng MSG/Chase/Crypto. Test chứng minh mỗi index xuất hiện đúng một lần, section/status đúng, số ghế không đổi.
- `geometry/ArenaOutlines.java`: sampling section/row và label point Java thuần dùng chung Android/HTML exporter. Không thay tọa độ ghế hoặc thuật toán projection.
- Sửa `ui/arena/ArenaRenderData.java`: Path hàng theo section; ghế theo bounded batches. Tất cả chuẩn bị trên worker, không dựng lại trong onDraw.
- Sửa `ui/arena/ArenaView.java`: vẽ các Path cache mới, bỏ invalidate khi snapshot không đổi, `Trace` section `ArenaView.draw`; warning tag `ArenaPipeline` khi CPU onDraw >32ms, tối đa một lần/snapshot. Warning là chẩn đoán, không phải FPS hay số đo toàn pipeline GPU.
- Sửa `ui/arena/ArenaPreviewLoader.java`: assert không chạy arena pipeline trên main Looper; trace stage `Arena.loadValidateGenerate`, `Arena.project`, `Arena.paths`; Logcat timing `loadValidateGenerateMs/projectMs/pathsMs`, worker name và batch count sau load. Không log mỗi ghế/frame.
- Sửa `ArenaPreviewDataTest.java`: thêm hai ca batch coverage/limits/status/section + cancellation/outlines. Sửa `ArenaPreviewInstrumentedTest.java`: thêm ca asset I/O guard, worker load, callback main và loại kết quả stale.
- Tạo `tools/java/ArenaPreviewExport.java`, `tools/export-arena-preview.ps1`, `tools/test-arena-preview.cjs`, `preview/arena-preview.template.html`, `preview/README.md`, `preview/.gitignore`; sinh `preview/arena-preview.html` khoảng 7 MB (artifact bị ignore).
- Cập nhật PROGRESS/AI_LOG theo yêu cầu; sửa bổ sung DESIGN/SOURCES để tài liệu pipeline và căn cứ rendering khớp code. Giữ các thay đổi Phase 3A/untracked có sẵn, không sửa model/dataset counts/giá/status đầu vào hoặc cấu hình Gradle.

### HTML Preview và tính đồng nhất

Mở trực tiếp [arena-preview.html](../preview/arena-preview.html) bằng Edge/Chrome/Firefox; không server/npm/CDN/mạng/emulator. Chọn sân, phóng đại rồi cuộn để xem ghế. Đây là công cụ review riêng, không thay Android Custom View/Canvas và không triển khai gesture/hit-testing của Android 3B/3C.

Exporter gọi **chính** repository/compiler/validator/scene/outlines/batches Java, xuất mọi seat ID, row/section, status và 8 tọa độ quad. HTML không tự sinh tọa độ bằng thuật toán JavaScript khác. Test kiểm SHA-256 nguồn Java/JSON và hash byte float32 của toàn geometry so với Java scene; kiểm ID/FK/status, mỗi ghế xuất hiện đúng một lần. Dữ liệu vẫn là hình học mô phỏng: MSG 16.908, Chase 17.584, Crypto 19.056, tổng **53.548** ghế. Không nêu đây là seating chart chính thức.

Sinh lại và kiểm tra khi đổi JSON/Java:

```powershell
./tools/export-arena-preview.ps1
node tools/test-arena-preview.cjs
```

`preview/arena-preview.html` chứa snapshot lớn, bị ignore để không commit bản sao hình học; template/exporter/test/README là nguồn cần quản lý. File đã tồn tại trong workspace để người dùng mở ngay.

### Kết quả kiểm tra thực tế

- JVM runner: **38 test đạt**, JUnit 2.640 giây (37 foundation/preview + 1 mẫu). Hai test mới không chỉ kiểm đếm, mà kiểm coverage/status/section của mọi ghế và hủy preparation.
- Export/Node verification: đạt cả **53.548 ghế**, SHA JSON/source/float32 geometry, batch coverage và script thật chạy với DOM/Canvas test double: đổi ba sân, zoom/reset/resize, coalesce requestAnimationFrame và không tự redraw khi idle. **Không phải browser screenshot, kiểm tra pixel/GPU hoặc FPS.** Browser access host không khả dụng; chưa kiểm tra trực quan trang bằng trình duyệt trong lượt này.
- Các số timing exporter dưới đây là **một lượt Java CLI trên host/JBR**, gồm JIT/thứ tự load khác nhau, không warmup/benchmark chuẩn; `jvmCliThread=main` là thread CLI, **không phải Android UI main thread**:

| Profile | load + validate + generate | Project | Batch indices | Ghế |
| --- | --- | --- | --- | --- |
| MSG | 352,0 ms | 39,0 ms | 22,3 ms | 16.908 |
| Chase | 191,8 ms | 11,3 ms | 9,5 ms | 17.584 |
| Crypto | 160,7 ms | 13,3 ms | 9,0 ms | 19.056 |

Không đo chi phí Android Path build/tessellation bằng CLI này và không dùng timing host để kết luận emulator không thể treo. Android load/draw timing mới chỉ được instrument trong code, chưa có log runtime đọc được.

### Thu thập chứng cứ khi lỗi tái diễn

Ở PowerShell bình thường ngoài sandbox, với emulator đang kết nối, lấy serial thực từ devices. Không chỉ lọc theo package app vì sẽ mất system-server events:

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $adb devices -l
# Thay emulator-5554 bằng serial thực ở devices.
& $adb -s emulator-5554 logcat -d -v threadtime -b main -b system -b events -b crash > .gradle/arena-logcat.txt
& $adb -s emulator-5554 shell dumpsys activity lastanr > .gradle/arena-lastanr.txt
& $adb -s emulator-5554 shell dumpsys gfxinfo com.example.nbatickethub framestats > .gradle/arena-framestats.txt
& $adb -s emulator-5554 shell dumpsys meminfo com.example.nbatickethub > .gradle/arena-memory.txt
```

Các lệnh trên là **hướng dẫn chưa chạy thành công trong phiên AI**, không phải log đã thu được. Tìm `ANR in`, `am_anr`, `Input dispatching timed out`, `system_server`, `Choreographer`, `OpenGLRenderer`, `ArenaPipeline`; đối chiếu timestamp boot/mở app/đổi sân, main stack và RenderThread. Logcat tag ArenaPipeline của build mới giúp tách thời gian load/project/path với CPU onDraw. Nếu cần dùng bugreport/Perfetto theo nguồn Android, giữ artifact local, không tự upload log.

**Trạng thái:** đã xử lý rủi ro Path lớn và tạo HTML review; chưa xác nhận lỗi hệ thống được khắc phục, chưa chạy connected tests/UI/FPS hoặc hoàn thành 3B/3C. Người dùng chưa review HTML trong tương tác này. Không commit/push. Đề xuất commit: `fix: bound arena render paths and add shared-geometry HTML preview`.

### Kiểm chứng cuối của lượt chẩn đoán

- Wrapper mirror `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: **BUILD SUCCESSFUL**. Lượt có thay đổi chức năng/test chạy 57 giây (12 task executed); lượt cuối sau định dạng code chạy 35 giây (1 task executed, 66 up-to-date). XML reports xác nhận **38 test, 0 failure/error/skipped**. APK kiểm thử biên dịch thành công, không đồng nghĩa instrumentation đã chạy.
- So SHA-256/tập đường dẫn **78 file app source/assets/resources/tests** và **9 file build/Wrapper** giữa root/mirror đều khớp; chỉ SDK path trong local.properties mirror khác. Không đổi cấu hình Gradle dự án gốc.
- Node test cuối đạt cả hash template/source/JSON/float32, 53.548 seats và event/idle logic. HTML standalone 7.247.339 byte, SHA-256 `3d701d3e128965db202b7a7c82bec00311665e51f5b9f57f0c4b8b7c5bb6d635`; APK debug 7.004.164 byte, SHA-256 `b0f5d00fc0d4a4943d50aa9ad212df302b3da9428c548b26834f8fa9c4d46f11`. Không phải bằng chứng chất lượng hiển thị/FPS.
- 8 Markdown files có fence cân bằng và 32 link cục bộ tồn tại; `git diff --check` đạt, staged diff rỗng; HTML output được gitignore như dự kiến. Các file/thay đổi có sẵn trước lượt giữ nguyên ngoài phần sửa được mô tả.
- Lint **không chạy lại** trong lượt này; lỗi cache/mạng từ Phase 3A chưa được giải quyết. Chưa có Logcat/stack thiết bị, connected tests hoặc kiểm tra trực quan browser/emulator. Log build mới: `.gradle/arena-anr-build-final.log`; report XML/APK ở mirror như hướng dẫn trước.


## Chỉnh Arena Geometry và Canvas sau review HTML — 2026-11-25

**Người dùng đã review bản HTML trước và báo chưa đạt thẩm mỹ/độ chân thực.** Đó là phản hồi review, không phải nghiệm thu Phase 3A. Lượt này chỉ chỉnh preview 3A; chờ người dùng review lại bản mới.

### Thay đổi và tiêu chí đối chiếu

| Tiêu chí người dùng | Kết quả triển khai |
| --- | --- |
| Giảm khoảng trống, cân tỷ lệ court/stands | Level gap 12–25 world units; aisle chuẩn hóa theo bán kính. Court length 1130/1050/1160, kiểm cả apron không lấn khán đài. Không giảm ghế |
| Section tự nhiên, bowl liên tục | Giao tia với superellipse tránh kéo giãn gần trục; cắt góc section không đều; kiểm full tier chiếm ít nhất 338° để tránh khe trang trí quá lớn |
| Ba sân thực sự khác hình học | MSG n=4.6 + gallery; Chase n=2.5, oval và 3 tầng; Crypto n=3.6, bowl dài + 4 tầng. 42/34/50 section, 16.908/17.584/19.056 ghế MOCK |
| Phân tầng, ghế và nhãn dễ nhận biết | Catalog palette cho fill/seat theo tầng; booked/locked màu chung; label nền sáng, chữ tối 9sp Android/9 CSS px HTML; nét hàng mảnh và nhạt hơn section; thêm chú giải SIM-tier trên Android dọc/ngang |
| Chung nguồn dữ liệu, Android Canvas giữ nguyên | Java scene/outlines/batches → Android cached Paths và HTML export. Java style tokens gồm kích thước nhãn/nét; HTML không sinh lại seat geometry, có zoom review 100–600% |
| Nguồn và dữ liệu trung thực | SOURCES §9 ghi trang official, nền tảng ticketing và giới hạn truy cập; tất cả geometry/section/inventory vẫn gắn SIMULATION |

### File liên quan

Lượt tiếp nối kế thừa các file Phase 3A chưa commit. Chỉnh trực tiếp: `tools/generate-preview-assets.py`; ba `assets/mock/{msg,chase,crypto}.json`, `assets/venues/catalog.json`; `geometry/ArenaRenderStyle.java`; `ui/arena/ArenaView.java`, `ArenaRenderData.java`; `MainActivity.java`; XML dọc/ngang và strings; `tools/java/ArenaPreviewExport.java`; template HTML; `ArenaPreviewDataTest.java`, `tools/test-arena-preview.cjs`; DESIGN/PROGRESS/SOURCES/AI_LOG và README preview. Renderer/compiler/profile/parser/palette đã có một phần chỉnh sửa ở trạng thái đầu lượt, không nhận là tất cả do lượt này tạo mới. `ArenaSceneCompiler` giao tia hiện tại được kiểm chứng và tài liệu công thức sửa lại cho khớp code.

Sinh lại HTML và ba `preview/qa-*.png` để kiểm tra; HTML/PNG là output ignored, không tài nguyên Android. `tools/render-arena-review.py` dùng Pillow dựng ảnh từ snapshot Java, **không chứng minh browser/Android pixel parity**.

### Kiểm chứng thực tế

- `./tools/export-arena-preview.ps1`: thành công, đủ 53.548 ghế; bản đầu tăng court với oval mới bị validator từ chối `Court/apron outside central arena clearance`. Đã điều chỉnh court Chase/Crypto và chạy lại đạt, không bỏ validation.
- `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`: **41 test đạt**, lượt cuối 4.901 giây JUnit; có assertion coverage góc tier đầy. Không dùng test mẫu làm bằng chứng chức năng.
- `node tools/test-arena-preview.cjs`: đạt hash asset/Java/float32 geometry, toàn bộ ID/quan hệ/status/batch, palette/appearance, đổi ba sân, zoom 600/reset/resize/coalescing/idle qua DOM/Canvas test double.
- Wrapper ở mirror `.gradle/phase3a-project` với cùng app source/build config, SDK/cache workspace và AAPT2 override như mốc trước: `assembleDebug testDebugUnitTest assembleDebugAndroidTest` **BUILD SUCCESSFUL**, 1 phút 7 giây; 67 tasks (19 executed). Log `.gradle/arena-visual-revision-build.log`. Không nhận plain root build đã đạt. Test assertion thêm sau build được chạy lại riêng bằng Wrapper, kết quả xác minh cuối bổ sung dưới đây.
- `python tools/render-arena-review.py`: tạo ba ảnh QA; AI đã mở xem cả ba bowl để kiểm tra silhouette, gap, court, nhãn/palette. Đây là ảnh Pillow từ snapshot, không phải browser screenshots.
- Computer-use inventory thấy Edge nhưng đọc window bị công cụ dừng: `could not determine the current browser URL on Windows with enough confidence to enforce policy`. Đã dừng thao tác browser, không thử vượt chặn; **chưa kiểm tra trực quan HTML trong browser trong lượt này**.
- Chưa chạy connected tests/emulator/FPS; lint không chạy lại (giới hạn dependency/network lịch sử chưa xử lý). Không kết luận sửa ANR hoặc đạt hiệu năng Android.

Mở lại [HTML Preview mới](../preview/arena-preview.html), chọn lần lượt MSG/Chase/Crypto; xem toàn cảnh, legend tầng, tăng zoom để xem ghế và cuộn, bấm Vừa khung. Nếu tab đang mở bản cũ cần reload file. Chỉ người dùng xác nhận lại mức thẩm mỹ/chấp nhận mô phỏng; chưa có phản hồi cho bản mới. Không triển khai 3B/3C, không stage/commit/push.

Commit đề xuất: `feat: refine simulated arena geometry and shared Canvas styling`.

Kiểm chứng cuối bản chỉnh geometry: Wrapper chạy lại `testDebugUnitTest` sau assertion coverage mới **BUILD SUCCESSFUL trong 50s** (2 executed/20 up-to-date). XML xác nhận **41 tests, 0 failure/error/skipped**. SHA-256/tập source kiểm tra 79 app files và 9 build/Wrapper files root/mirror khớp; chỉ local.properties mirror khác có chủ đích. Resource XML parse đạt, 8 Markdown files có fence cân bằng và 33 link local tồn tại. `git diff --check` đạt; staged diff rỗng; HTML và ba QA PNG được ignore.

HTML mới 7.304.200 byte, SHA-256 `a8e4f0f0cce202819d23d5477e44de00d20d6ff0dd049826653890820076ee3e`; APK 8.321.318 byte, SHA-256 `9a4c51c3069a8423cf017fc031557f0cdb38f4187b3534122e982b038316dd66`. Log unit cuối `.gradle/arena-visual-revision-tests.log`. Đây là kiểm chứng nguồn/artifact, không phải nghiệm thu thẩm mỹ hoặc performance runtime. Không commit/push.


## Phase 3A.1 — theme đội chủ nhà độc lập hình học — 2026-11-25

**Đã triển khai ba demo; chờ người dùng review, chưa nghiệm thu và chưa sang 3B.** Người dùng giao bổ sung theme và tách geometry; chưa xác nhận đã xem bản theme mới. Chi tiết hợp đồng/ưu tiên cấu hình ở DESIGN §12, nguồn ở SOURCES §10.

### Kết quả theo tiêu chí

| Tiêu chí giao | Triển khai và bằng chứng |
| --- | --- |
| Geometry của sân, theme có thể đổi độc lập | ArenaGeometry không có màu/đội; ArenaTheme không có tọa độ. Catalog v2 tách geometry/themes/statusPalette. 3 hash float32 toàn ghế khớp bản v3 trước lượt này, đủ 53.548 ghế |
| Theo đội nhà/cấu hình trận | resolveTheme nhận override; JSON có previewGameThemeId nullable; auto dùng đội nhà/fallback. Spinner Android/HTML cho review override, giữ arena và zoom; không gọi loader khi đổi theme |
| Màu sân/ghế/tầng/nhãn theo dữ liệu | Court wood/alternate/lines/apron/paint/lettering/center và tên chữ baseline/center; tier section/seatBackground/accent; background/concourse/label/row/border palettes. Chữ kiểm contrast >=4.5:1; palette ghế mô phỏng |
| Trạng thái nhất quán | Một seatStatusPalette chung; dấu tròn status trên nền ghế theo tier, legend lấy cùng dữ liệu. Marker coords do Java xuất; không làm màu đội thay nghĩa AVAILABLE/BOOKED/LOCKED |
| Ba demo cùng hướng để review | Knicks/MSG, Warriors/Chase, Lakers/Crypto; ba card top-down chung khung và map lớn zoom 100–600%. Android Java Canvas, không WebView/ảnh sơ đồ/logo official |
| Không sinh lại ghế khi đổi theme | Java RenderData không cache màu; setArenaTheme giữ scene/Path. HTML kiểm 9 arena×theme combinations, geometry hash/cache preparation count/zoom không đổi |

### File tạo/sửa

- Mới: `model/ArenaGeometry.java`, `ArenaTheme.java`, `SeatStatusPalette.java`; `geometry/ArenaSeatMarkers.java`; `androidTest/.../ui/arena/ArenaThemeInstrumentedTest.java`.
- Sửa: `ArenaProfile`, `ArenaCatalog`, `ArenaCatalogReader`, `ArenaSceneCompiler`, `ArenaRenderStyle`; `ArenaRenderData`, `ArenaView`, `MainActivity`; XML dọc/ngang, strings; `ArenaPreviewDataTest`, `ArenaPreviewInstrumentedTest`.
- Dữ liệu/tools: `assets/venues/catalog.json` (v2), generator assets; Java exporter; HTML template, test Node, tool QA, README preview. Ba mock manifest được generator ghi lại theo cùng cấu hình v3, không đổi số ghế/tọa độ.
- Tài liệu: DESIGN/ROADMAP/PROGRESS/SOURCES/AI_LOG. Giữ các thay đổi cũ ngoài phạm vi; không thay SDK/Gradle/dependency, cấu hình máy hoặc Git history.
- Output local ignored: HTML mới, ba QA PNG và `qa-themes.png`. Không nộp ảnh vào renderer Android.

### Kiểm tra thực tế

- `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`: **44 test đạt** ở lượt chức năng (3.765 giây); gồm theme priority/fallback, shared state palette, defensive tier list, từ chối màu/schema/provenance/contrast/reference sai, marker nằm trong mọi seat, cùng 41 test nền đã cập nhật schema. Kết quả cuối có ghi riêng bên dưới.
- `./tools/export-arena-preview.ps1` và `node tools/test-arena-preview.cjs`: đủ 53.548 ghế, hashes nguồn/asset/float32, ID/FK/status/batch/marker data và theme RGB/strings bằng cấu hình sau Java validation. Script thật chạy qua test double kiểm ba card, 9 tổ hợp, không chuẩn bị lại Path, không đổi zoom và không redraw loop.
- Lượt Node đầu so chuỗi hex bị fail do Java xuất lowercase còn JSON dùng uppercase; đã so theo RGB chuẩn hóa chỉ đối với chuỗi hex, không bỏ assertion equality. Không có khác biệt màu thực được che giấu.
- Wrapper mirror `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: **BUILD SUCCESSFUL trong 1 phút**, 67 tasks (22 executed). Không nhận lệnh root plain chạy đạt; mirror dùng cùng app/config, chỉ SDK/cache path/AAPT2 override như mốc trước. Đã thêm test và build lại cuối sau cấu hình previewGameThemeId, kết quả cuối ghi bên dưới.
- `python tools/render-arena-review.py`: tạo ba QA map và contact sheet chung góc nhìn. AI đã mở xem contact sheet ba demo. Đây là Pillow từ Java snapshot/theme, không chứng minh browser/Android pixel parity.
- CUA inventory lượt này báo `Browsers: Error: The browser access host is unavailable`. Không mở/điều khiển browser hoặc nhận đã xem HTML trực tiếp. Node không có playwright/puppeteer; không cài thêm phần mềm. Ba demo HTML và QA được bàn giao để người dùng mở.
- Instrumentation kiểm bitmap/đổi theme/giữ Path và recreate override **đã viết và biên dịch, chưa chạy trên emulator/device**. Không chạy connected hoặc lint trong lượt này; giới hạn runtime/lint cũ chưa xử lý. Không tuyên bố hết ANR hoặc đo được FPS/memory Android.

### Review bản mới

Mở [HTML](../preview/arena-preview.html), reload nếu tab giữ bản cũ. Auto hiển thị ba home theme ở ba card; chọn từng arena xem map lớn. Chọn theme khác khi đang zoom để đối chiếu cùng geometry; chọn lại auto để về đội nhà/cấu hình trận. Android có spinner theme thứ hai và legend tương ứng. Có [ảnh so sánh QA](../preview/qa-themes.png) và ba QA từng sân (đều local/ignored).

Màu ghế/tầng, ván gỗ và lettering là mô phỏng, không phải court edition/seating chính thức. Chờ người dùng review độ dễ nhìn của status dots, nhãn, các palette và ba court. Không cần quyết định thêm để hoàn thiện phần đã giao; không tự coi bản này được duyệt hay chuyển 3B/3C.

Commit đề xuất: `feat: separate arena geometry from home-team themes`.

### Kiểm chứng cuối Phase 3A.1

Wrapper mirror `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: **BUILD SUCCESSFUL trong 1m4s**, 67 tasks (18 executed, 49 up-to-date), log `.gradle/phase3a1-build-final.log`. XML xác nhận **44 tests, 0 failure/error/skipped**. JVM fallback cuối 44 tests đạt, 4.376 giây JUnit. So tập file/SHA-256: **84 app files và 9 build/Wrapper files** root/mirror khớp (local.properties mirror khác có chủ đích như trước).

Export/Node cuối đạt: cả ba hash tọa độ float32 khớp baseline lưu trước theme refactor, mọi 53.548 seats và markers hợp lệ, theme RGB/strings khớp Java/JSON, status palette chung; chín tổ hợp arena/theme giữ cache/zoom. Ba comparison canvases có cùng viewport 480×360 và court angle 0°. Resource XML parse đạt, 8 Markdown files có fence cân bằng và 37 link local tồn tại; diff whitespace check đạt, staged diff rỗng. HTML/QA output ignored.

HTML mới 8.986.318 byte, SHA-256 `d19f994c1830b3101b98ccc0832a14e5f2018c81c8744ed12c567437fc659abf`. APK mới 8.323.574 byte, SHA-256 `aa6c8257560e1997f9c1c89efbfa6c492a39166ee25bfcffa125933062e2ee3e`. Các kiểm tra là bằng chứng source/artifact/logic, không phải pixel parity, runtime performance hoặc nghiệm thu thẩm mỹ. Chưa chạy connected/lint; browser host không khả dụng. Không commit/push và chưa có review mới từ người dùng.


## Phase 3A.1 — 30 thiết kế mặt sân, tách palette khán đài theo arenaId — 2026-11-25

**Số đã triển khai: 30 court designs / 30 teamId, 3 arena geometries, 0 logo asset và 30 mã đội fallback.** Ba arena là MSG, Chase Center, Crypto.com Arena; không tuyên bố đã làm 30 geometry hoặc có dữ liệu ghế chính thức. Chờ người dùng nghiệm thu trực quan HTML. Không sang 3B/3C.

### Phạm vi và kết quả

- Mỗi court có hai lớp hình học riêng ngoài màu/logo: wood layout và decorative motif. Có bảng đủ 30 đội/hai đặc điểm tại DESIGN §13; dữ liệu vector thực do `tools/court_designs.py` sinh ra, không chỉ 30 bảng màu. Có key edge accents, trim và lettering dữ liệu theo teamId, vạch thi đấu rõ và chung geometry Java.
- Catalog v3 tách `ArenaStands` theo arenaId khỏi theme court; đổi 30 đội trên từng arena giữ palette/seat geometry. Cả 53.548 seat quads vẫn khớp hash baseline trước thay đổi. Trạng thái vé dùng một palette + halo trắng, không đổi nghĩa theo màu ghế hoặc đội.
- `CourtDesign.Logo` có slot asset/provenance, fallback code, width/height riêng, fit ảnh giữ tỷ lệ và đúng tâm. Hiện không có asset official được xác minh nên dùng mã đội, không giả là đã tích hợp 30 logo. Mã/ảnh được vẽ trước lớp vạch sân. Unit test kiểm fit landscape/portrait; luồng ảnh thật chưa được chạy trên thiết bị vì bản demo không có asset ảnh.
- HTML chọn được đủ 30 đội, mặc định court-only/ẩn tên-mã-logo; có gallery 30 mặt sân cùng backing 480×280 và công tắc palette trung tính. Bỏ ẩn danh để thấy tên trong dropdown; bấm card cập nhật court lớn phía trên. Không tự gán 27 đội vào venue home chưa có geometry.
- Behance mở trực tiếp lỗi 403; tìm được hai image index cùng project nhưng raw ảnh cache miss. SOURCES §11 ghi giới hạn này, không bịa đã xem toàn bộ nguồn/tác giả/license. Dùng phong cách phân lớp đồ họa làm tham khảo giới hạn, tự thiết kế vector, không sao chép ảnh/logo.

### Files

- Mới: `model/CourtDesign.java`, `ArenaStands.java`; `geometry/CourtMarkings.java`; `ui/arena/CourtRenderData.java`; `tools/court_designs.py`, `court_review_renderer.py`, `render-court-review.py`, `tools/.gitignore`.
- Sửa: ArenaTheme/Profile/CatalogReader; ArenaView/RenderData/PreviewLoader/MainActivity; `assets/venues/catalog.json`, asset generator; Java exporter, HTML template/Node test, các test JVM/instrumentation; QA arena tool; docs DESIGN/ROADMAP/PROGRESS/SOURCES/AI_LOG và preview README. Source trong mock manifests/seat counts không thay đổi về nội dung geometry.
- Output ignored: HTML mới, `qa-courts30.png`, `qa-courts30-blind.png` và QA ba arena. Cache `tools/__pycache__` được ignore, không xóa cache hoặc thêm nó vào Git. Không thay SDK/Gradle/dependency/framework/cấu hình máy.

### Kiểm tra thực tế

- `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`: **47 test đạt**, lượt cuối 5.061 giây JUnit. Ba test mới kiểm 30 nhóm wood và 30 nhóm decor đã bỏ màu/identity, center fit và vạch chung; các test cũ được cập nhật cho contract palette arena/schema v3. Lượt compile đầu trong quá trình migration lỗi 22 chỗ test còn tham chiếu `theme.tier`/constructor cũ; đã cập nhật theo hợp đồng mới rồi chạy lại đạt, không bỏ các assert chức năng.
- Export + `node tools/test-arena-preview.cjs`: đạt theme/artwork Java ↔ JSON sau chuẩn hóa RGB/float32, 30 lựa chọn và 30 thumbnail đều 480×280, ẩn danh không draw chữ/mã, neutral mode, mark trước competition lines, **90 arena×court combinations** giữ world hash/seat palette/cache/zoom; reset/resize/coalescing/idle đạt qua test doubles.
- Wrapper mirror `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: **BUILD SUCCESSFUL trong 55s**, 67 tasks (14 executed/53 up-to-date). Log `.gradle/courts30-build.log`; app và instrumentation APK compile được. Mirror/source equality và XML test results kiểm cuối bên dưới; không nhận plain root build hoặc connected test đã chạy.
- `python tools/render-court-review.py`: tạo và AI đã mở xem bảng named/blind-neutral 30 court. 30 chữ ký wood và 30 decor khác nhau sau bỏ màu; ảnh blind-neutral có minimum pairwise mean RGB difference 6.6314/255 (cặp 04/14). Đây chỉ là QA ảnh từ vector, không thước đo độ đẹp/độ chân thực hoặc nghiệm thu browser. `python tools/render-arena-review.py` cũng sinh lại ba QA arena với artwork mới và palette theo arena.
- CUA lần đầu báo kernel reset, retry inventory trả `The browser access host is unavailable`. Không thao tác browser tiếp; **chưa chạy kiểm tra pixel/thao tác trực quan trong browser thực**. Logic HTML được kiểm qua script thật với DOM/Canvas test double; cần người dùng mở file để nghiệm thu thị giác.
- Chưa chạy connected tests, Android runtime/bitmap checks, FPS/heap hoặc lint trong lượt này. Không kết luận đã sửa dialog hệ thống cũ hoặc mọi P0 đã hoàn tất. Không stage/commit/push.

### Cách review và điểm dừng

Mở [HTML mới](../preview/arena-preview.html), reload nếu đang giữ bản cũ. Mặc định ẩn tên để so bố cục; bỏ ẩn tên để chọn đội theo tên. Bật “Trung tính màu” để so 30 court không dựa vào màu/logo, bấm từng card để xem lớn. Bỏ “Chỉ xem mặt sân” để đối chiếu court trên 3 arena mô phỏng, kiểm màu ghế giữ nguyên khi đổi đội.

Có [bảng QA có tên](../preview/qa-courts30.png) và [bảng QA ẩn tên/trung tính](../preview/qa-courts30-blind.png), không phải browser screenshot. Chờ người dùng đánh giá tính khác biệt và thẩm mỹ; chưa nghiệm thu 30 court mới. Giữ Phase 3A.1, không tự chuyển phase.

Commit đề xuất: `feat: add 30 distinct court concepts and blind comparison preview`.

### Kiểm chứng cuối — bản 30 court

Đọc XML Gradle xác nhận **47 tests, 0 failure/error/skipped**. Mirror khớp **88 app files và 9 build/Wrapper files** với root; cấu hình SDK/local.properties mirror khác có chủ đích như các mốc trước. APK chứa đúng byte catalog 30 theme hiện tại và DEX có CourtRenderData/CourtMarkings, không chỉ kiểm sự tồn tại file APK. Build log `.gradle/courts30-build.log` ghi BUILD SUCCESSFUL 55s.

Node cuối đạt cả 30 wood/30 decor signatures đã bỏ màu/identity, 30 option/card, kích thước 480×280, hidden/neutral và 90 tổ hợp giữ arena palette/world hash/cache/zoom. Cả ba hash geometry ghế khớp baseline trước theme. XML resource parse đạt; 8 Markdown files có fence cân bằng và 41 link cục bộ tồn tại. `git diff --check` đạt, staged diff rỗng; HTML/QA và Python cache được ignore.

HTML 9.168.848 byte, SHA-256 `e2ebe976e7156a17a85a3b87e049c1565cbf0f5da0e5a0d3f0148e5671524bfa`; APK 8.323.574 byte, SHA-256 `e2ac95178d99d8b7df057683676cf2977c47b0533a6ecaa28a4444bfb2e4d68e`. Không có browser/device visual verification hoặc nghiệm thu người dùng mới trong lượt này. Không commit/push, giữ Phase 3A.1.

## Phase 3A.1 — 30 khán đài sân nhà, palette theo teamId — 2026-11-25

**Đã triển khai và kiểm host logic: 30/30 đội → 30 cấu hình arena riêng; 30 court designs giữ nguyên; 626.904 ghế mô phỏng; 0 logo assets / 30 mã đội fallback. Toàn bộ 30 khán đài còn dùng hình học MOCK. Chưa nghiệm thu trực quan.** Quy tắc palette theo arenaId ở các mốc trước được thay bằng yêu cầu mới: màu ghế theo theme đội, geometry theo arena.

### Mục tiêu, phạm vi và kết quả

- Kiểm `git status --short --branch`/diff sau interrupt: giữ working tree cũ; không nhận toàn bộ diff tích lũy là thay đổi của lượt này. Prompt HTML tên/logo trước đó bị interrupt khi mới đọc/kiểm tra, chưa lưu thay đổi hiển thị. Bản mới mặc định hiện tên đầy đủ/mã đội bằng dữ liệu có sẵn, vẫn giữ hide-identity để review ẩn danh.
- Thêm 27 geometry profile với tỷ lệ/độ bo, độ sâu tầng, section cuts, aisle width, hàng và số ghế riêng. Ba mock manifests MSG/Chase/Crypto giữ **nguyên byte**, toàn bộ seat quads vẫn đúng ba hash baseline cũ. So hash riêng từng `court` và tên theme xác nhận giữ cả 30 thiết kế, logo slot/fallback/size và tên đội. Không thêm/xóa logo asset.
- Schema v4 có home mapping đầy đủ, kể cả khả năng dùng chung arena. 30 home mappings hiện tại khác nhau; shared-home fixture đổi NYK/GSW cùng Chase giữ tọa độ/Paths/zoom nhưng đổi palette và court. Đây là fixture test, không mapping NBA thật.
- `ArenaTheme.seatTier` tạo section/seat/accent từ hai màu theme đã có, đậm nhạt theo tầng. Cả Android Canvas và HTML dùng kết quả Java này. Trạng thái giữ palette chung và halo trắng; label vẫn nền/ink cố định tương phản. UI ghi rõ màu ghế thiết kế theo theme đội, không màu ghế vật lý xác minh.
- HTML chọn đủ 30 đội, tự cập nhật home arena/title/court/palette. Mặc định hiện toàn arena và tên/code. Danh sách thiếu logo gồm cả 30 đội hiện trong HTML. 30 thumbnail outline cùng frame; tải chi tiết ghế cho sơ đồ lớn qua local companion scripts, cache tối đa hai arena. Court-only/ẩn danh/trung tính vẫn hoạt động với 30 court cùng kích thước.
- Android chọn theme đồng bộ venue và load geometry bằng worker; cùng arena đổi theme dùng lại Paths. Giữ Java Canvas, kiến trúc data-driven, SDK/Gradle/dependency/config máy. Không triển khai Android pan/pinch, hit-testing, chọn ghế hoặc phase mới.
- Sources §12 liệt kê đủ 30 sân/nguồn/mức mô phỏng. Directory NBA dùng cho identity; Philadelphia dùng tên Xfinity trên venue site thay tên Wells Fargo còn cũ trong directory. Chart reference không được coi là tọa độ xác minh. Không có ảnh venue/chart/logo ngoài được nhập vào app.

### File tạo hoặc sửa trong nhiệm vụ

- Mới: `tools/arena_layouts.py`; 27 `app/src/main/assets/mock/{arenaId}.json` tương ứng bảng SOURCES §12.
- Sửa dữ liệu/model: `assets/venues/catalog.json`; `ArenaCatalog.java`, `ArenaCatalogReader.java`; `ArenaProfile.java`, `ArenaTheme.java`, `ArenaStands.java`.
- Sửa hiển thị: `MainActivity.java`, `ui/arena/ArenaView.java`, `res/values/strings.xml`; `preview/arena-preview.template.html`, `.gitignore`, `README.md`.
- Sửa authoring/export/QA/test: `tools/generate-preview-assets.py`, `court_designs.py`, `java/ArenaPreviewExport.java`, `render-arena-review.py`, `test-arena-preview.cjs`; `ArenaPreviewDataTest.java`, `ArenaPreviewInstrumentedTest.java`.
- Tài liệu: SOURCES/PROGRESS/AI_LOG/DESIGN/ROADMAP. Output ignored: HTML, 30 `arena-data/*.js`, QA images và helper/log/build trong `.gradle`. Các thay đổi cũ khác tiếp tục được giữ nguyên.

### Kiểm chứng thực tế và giới hạn

- `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`: **48 tests đạt**, lần chạy trước tối ưu bộ nhớ của test signature, JUnit 28.588s. Gradle chạy lại source cuối như bên dưới. Test mới kiểm 30 home mappings, 30 full-coordinate signatures riêng, màu theo tầng, shared-home fixture và map thiếu đội bị từ chối. Các assertions ba manifest cũ giữ nguyên; test scenes mở rộng từ 3 lên 30, không giảm yêu cầu containment/ID/trạng thái.
- `./tools/export-arena-preview.ps1` và `node tools/test-arena-preview.cjs`: **PASS**, 30/30 đội/sân, tổng **626.904** ghế, full float32 geometry SHA-256 và batches/markers hợp lệ; 30 wood + 30 decor layouts không màu/identity. Kiểm title/code defaults, logo dưới vạch, ẩn danh/trung tính, shared-home palette/Path/zoom, stale loads, lỗi thiếu payload/retry, cache ≤2, reset/resize/idle. DOM/Canvas là doubles, không browser pixel proof.
- Gradle Wrapper ở mirror `.gradle/phase3a-project`: `assembleDebug testDebugUnitTest assembleDebugAndroidTest` **BUILD SUCCESSFUL** lần đầu 1m22s, 21 executed/46 up-to-date (`.gradle/stands30-build.log`). XML xác nhận **48 tests, 0 failures/errors/skipped**. APK instrumentation chỉ được compile, chưa chạy trên thiết bị. Kết quả xác minh artifact cuối được bổ sung ở cuối mục.
- `python tools/render-arena-review.py` và `python tools/render-court-review.py`: sinh bảng 30 home arenas, ba demo và 30 court named/blind-neutral từ dữ liệu xuất Java. AI đã xem QA ba demo và bảng 30 sân, không thay nghiệm thu của người dùng. Court blind minimum RGB difference vẫn 6.6314/255 (04/14), chỉ sanity check chứ không điểm thẩm mỹ.
- Computer Use: browser inventory báo host unavailable; native Edge AX đọc được. Click tab lỗi `coordinate input geometry is unavailable`; thử mở file bằng keyboard bị công cụ **dừng do không xác định URL Windows đủ tin cậy để áp dụng policy**. Dừng UI automation, không vượt chặn; chưa xác minh ảnh browser hoặc chạy chọn 30 đội bằng trình duyệt thật.
- `adb devices -l` trong sandbox lỗi tạo `\.android`; retry được cấp quyền trả danh sách **không có thiết bị**. Không chạy `connectedDebugAndroidTest`. Không đo Android FPS/heap/ANR; không tuyên bố đã sửa nguyên nhân dialog trước. Không chạy lại lint do giới hạn artifact môi trường đã ghi ở mốc trước.

### Review và điểm dừng

Mở [HTML Preview](../preview/arena-preview.html) với thư mục `arena-data` cạnh nó; reload tab cũ. Chọn 30 đội hoặc bấm 30 card; xem ghế chi tiết ở sơ đồ lớn/zoom. [QA 30 sân](../preview/qa-homearenas30.png) giúp so palette/độ bo/tầng, [QA ba demo](../preview/qa-themes.png) cho Knicks/Warriors/Lakers. Tất cả 30 sân trong bảng SOURCES §12 vẫn mô phỏng; chưa có sân nào được nghiệm thu là seating chart chính thức.

PROGRESS/SOURCES/AI_LOG đã cập nhật; DESIGN §14 và ROADMAP ghi hợp đồng mới. Chưa có xác nhận review/kiểm thử mới từ người dùng. Không cần quyết định bổ sung để hoàn thành phần được giao; chờ review hình ảnh và mức khác biệt của 30 khán đài. **Dừng ở 3A.1; không commit/push.** Commit đề xuất: `feat: add 30 home arena layouts with team seat palettes`.

### Kiểm chứng artifact cuối — 30 sân nhà

Phát hiện unit tests đọc JSON trực tiếp nhưng Gradle chưa khai báo các asset là input, khiến lượt build chỉ đổi metadata có thể giữ kết quả test `UP-TO-DATE`. Đã bổ sung `inputs.dir("src/main/assets")` cho task Test trong `app/build.gradle.kts`, không thay phiên bản/dependency/toolchain. Lần build sau bổ sung này **thực sự chạy lại testDebugUnitTest**: BUILD SUCCESSFUL **1m22s**, 1 task executed/66 up-to-date (`.gradle/stands30-build-inputs.log`); XML mới xác nhận **48 tests, 0 failure/error/skipped**. Đây là file build duy nhất sửa thêm trong nhiệm vụ để theo dõi input kiểm thử.

Đối chiếu cuối: **115 app files + 9 build/Wrapper files** root/mirror trùng byte, bao gồm rule input mới; nguồn SDK/local.properties mirror vẫn khác có chủ đích như các mốc trước. APK chứa đúng byte catalog v4 và đủ 30 mock manifests hiện tại; DEX có court renderer/markings/seatTier. 8 Markdown files có fence cân bằng và 48 link local tồn tại; XML resources parse được, `git diff --check` đạt, staged diff rỗng. Node cuối PASS, gồm thêm assertion **30 palette ghế riêng** ngoài 30 geometry riêng. So baseline court/name/logo-slot và ba manifest/seat hashes cũ tiếp tục PASS.

HTML 1,451,214 byte, SHA-256 `a54c2f1ff768c297b4c0a1a632ff2cac41bcf48de1c1f9a24b9203dcd889d5a3`; 30 local payload tổng 108,932,826 byte, mỗi file có hash đối chiếu Java exporter. Chỉ payload sân được chọn được tải; cần giữ thư mục arena-data cạnh HTML. APK 8,325,538 byte, SHA-256 `d5dc1831f98aaffc63103efbafec0390e4eaab82b2576ed47779a6e3a03fd429`. Lượt assemble/unit/instrumentation-assemble cuối dùng chính source này; instrumentation chưa chạy vì ADB không có thiết bị. Không có browser pixel/Android runtime acceptance mới; chờ người dùng review. Không commit/push.

## Phase 3B — Android Matrix, gesture và mức chi tiết (2026-11-25)

**Đã triển khai, đã kiểm build/unit/lint; chưa nghiệm thu gesture hoặc hiệu năng thiết bị.** Người dùng xác nhận duyệt 3A trong prompt giao 3B. Không triển khai hit-testing, highlight hay chọn ghế của 3C/4.

### Thay đổi và tiêu chí

- `ViewportTransform` quản lý Android Matrix và inverse duy nhất cho world → View-local. Fit 90% vùng View đã trừ padding, xử lý bounds có gốc khác 0. Min zoom = fit; max zoom đủ để cạnh ghế nhỏ nhất đạt 24dp, không khóa cứng 8×fit. Mỗi trục nhỏ hơn viewport căn giữa; trục lớn hơn clamp không để kéo mất sơ đồ. Resize cùng View giữ tâm world và tỷ lệ so với fit; đổi dataset luôn fit mới.
- Pan một ngón có touch slop. Pinch tính span và midpoint của hai pointer ID, inverse điểm giữa cũ rồi neo tại điểm giữa mới; dùng scale thực sau clamp. Rebase khi thêm/bớt ngón, kể cả nhấc ngón đầu hoặc ngón thứ hai, để không lấy delta cũ. CANCEL giải phóng gesture, parent không giành gesture đang chạy. Đây là triển khai REQ-04/05, còn chờ kiểm chứng runtime.
- “Vừa khung” được nối ở MainActivity, chỉ bật khi có dữ liệu; animation 250ms về camera fit, chạm mới/đổi sân/resize/detach hủy animation. Tắt animation hệ thống được tôn trọng; cùng arena đổi theme vẫn giữ camera. REQ-09 đã triển khai, còn chờ Samsung xác nhận.
- LOD theo cạnh ghế nhỏ nhất chiếu ra dp: section/nhãn ở xa, thêm đường hàng quanh 6dp, từng ghế quanh 16dp; hysteresis 15%. LOD chỉ thay lệnh vẽ, không thay tọa độ, palette hay inventory. Ghế giữ đúng nền, halo và status-dot cũ khi hiện chi tiết.
- Worker chuẩn bị AABB section/batch và danh sách batch theo section từ các Path hiện có. Inverse viewport lấy vùng world, thêm lề bảo thủ cho nhãn/nét; bỏ section ngoài vùng và chỉ xét batch của section còn thấy. Overview không duyệt/vẽ seat batches. Canvas còn clip vùng View. Tái dùng Paint, RectF, Matrix, mảng và Paths; không I/O/sinh ghế trong onDraw. Đây là phần cache/LOD/culling của REQ-13; chưa có bằng chứng FPS/GPU đạt trên Samsung.
- `screenToWorld`, `worldToScreen`, `visibleWorld` sẵn cho 3C, dùng cùng Matrix với Canvas. REQ-06 mới có nền biến đổi; chưa có hit-testing và không tuyên bố hoàn tất REQ-06.

### File thuộc nhiệm vụ này

Tạo `app/src/main/java/com/example/nbatickethub/ui/arena/ViewportTransform.java` và hai test `app/src/androidTest/java/com/example/nbatickethub/ui/arena/ViewportTransformInstrumentedTest.java`, `ArenaCameraInstrumentedTest.java`.

Sửa `ArenaView.java`, `ArenaRenderData.java` cùng package; `MainActivity.java`; `res/layout/activity_main.xml`, `res/layout-land/activity_main.xml`, `res/values/strings.xml`; `ArenaPreviewInstrumentedTest.java` (Reset nay được bật theo hành vi 3B); `docs/PROGRESS.md`, `docs/AI_LOG.md`, `docs/ROADMAP.md`. Không sửa assets, compiler/model, CourtRenderData, HTML/tools, Gradle hoặc AGENTS trong 3B. Các thay đổi có sẵn từ trước nhiệm vụ được giữ lại.

### Kiểm chứng thực tế

- Root `./gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-configuration-cache`: **BUILD SUCCESSFUL**. Lượt đầu online 1m29s; lượt source cuối 38s (27 executed/49 up-to-date), log `.gradle/phase3b/verified-build.log`. Không dùng mirror cho bằng chứng 3B.
- XML unit test xác nhận **48 tests, 0 failures/errors/skipped**. Đây là kiểm tra hồi quy data/geometry hiện có; không thay cho test Android Matrix/gesture. Test mới gồm 5 ca Matrix (fit/round-trip, moving focal/clamp, limits/reset, resize/new bounds, animated-reset endpoint) và 1 ca ArenaView (nhấc từng ngón, kéo tiếp, LOD/culling/cache). Các ca Android đã compile vào test APK, **chưa chạy**.
- Lint cuối: **0 errors, 7 warnings** (phiên bản AGP mới, plurals, overdraw nền và hai màu chưa dùng). Không đổi AGP/SDK/dependency để xử lý cảnh báo.
- Root build trong sandbox ban đầu không tạo được lock ở `C:\.gradle`; retry ngoài sandbox. Offline lint thiếu `graphics-shapes-desktop:1.0.1`; lượt online tải dependency và thành công. Không thay cấu hình máy/dự án để vượt lỗi đó.
- `adb devices` ngoài sandbox trả danh sách rỗng. **Không chạy connectedDebugAndroidTest**; chưa có Android runtime, kiểm ngón tay thật, frame timing/GPU/heap hoặc bằng chứng sửa ANR cũ. Giữ trace `ArenaView.draw`, thời gian CPU onDraw và số section/batch thực sự vẽ để kiểm trên thiết bị; không suy diễn đây là FPS.
- So SHA-256 với baseline chụp đầu 3B: **34 asset files và CourtRenderData trùng byte** (tổng 35 file). Như vậy JSON geometry, màu ghế, tên đội, logo slot/fallback và 30 court designs không đổi; logic camera không ghi vào seat quads. Staged rỗng; không commit/push.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Khi có Samsung kết nối/ủy quyền ADB, chạy `./gradlew.bat connectedDebugAndroidTest` để thực thi các ca Android đã biên dịch.

### Checklist Samsung — người dùng thực hiện, chưa có kết quả

- Mở app: thấy trọn sân, ở giữa, không mất khán đài ngoài cùng; thử cả dọc/ngang.
- Pinch gần một section và di chuyển đồng thời hai ngón: nội dung đi theo tâm; zoom tới min/max không lật/biến mất. Ở biên, clamp có quyền dịch anchor để giữ sân trong vùng truy cập.
- Lần lượt nhấc ngón trái/phải sau pinch, giữ ngón còn lại đứng yên rồi kéo tiếp: không nhảy vị trí; thử thêm ngón thứ ba rồi bỏ ra, ngắt gesture và làm lại.
- Khi zoom gần, kéo hết bốn hướng: tới được các khán đài nhưng không kéo mất cả sơ đồ. Thu nhỏ thấy section/nhãn; zoom gần thấy ghế, màu nền/halo/status-dot đúng; không có vùng ghế biến mất sai khi kéo qua mép.
- Bấm “Vừa khung” sau pan/zoom: trở lại đúng góc mở đầu; chạm giữa animation rồi kéo tiếp không giật về fit. Thử khi animation hệ thống tắt.
- Đổi nhiều sân có tỷ lệ khác nhau (MSG/Chase/Crypto và các đội khác): fit mới, đúng tên/mặt sân/màu ghế, không giữ độ phóng đại của sân cũ. Ghi lại sân và thao tác nếu có giật/đơ/sai vùng vẽ.

Chờ người dùng review 3B và cung cấp kết quả Samsung; chưa xác nhận các mục checklist. Không chuyển phase. Commit đề xuất: `feat: add bounded arena camera gestures and viewport detail rendering`.

Kiểm tra cuối: XML resources parse đạt; 8 Markdown files có code fence cân bằng và 48 link cục bộ tồn tại; `git diff --check` đạt, staged rỗng. APK thực tế chứa đúng byte cả 34 asset files hiện tại và DEX có ViewportTransform. APK **8.469.554 byte**, SHA-256 `8aa7d6f35f6294e1361f969c59b0a8633a2a297b1c9e88ae53d459ea20bd6137`. Không coi APK/hash hoặc test APK compile là bằng chứng gesture đã đạt.

## Phase 3C — hit-testing và kiểm tra một ghế (2026-11-25)

**Đã triển khai và kiểm JVM/build/lint; chờ test Android/Samsung và nghiệm thu chung 3B+3C.** Người dùng giao rõ 3C, chưa xác nhận đã test hoặc nghiệm thu runtime 3B. Không làm chọn nhiều ghế, Bottom Sheet, giá/tổng hoặc đặt vé.

### Triển khai và tiêu chí tương ứng

- REQ-06: tap hợp lệ dùng chính `ViewportTransform.screenToWorld()` (inverse Matrix đang dùng để concat Canvas), rồi `SeatHitTester.find()` trên `ArenaScene.seats` world cuối. Không áp lại projection hoặc dùng geometry canonical cũ.
- `SeatHitTester` Java thuần: worker dựng AABB và sparse grid riêng theo section; mỗi quad có mặt trong mọi cell AABB chạm tới. Query chỉ xét section chứa điểm, một cell/section và kiểm chính xác point-in-convex-quad. Lối đi/court/khoảng trống không snap sang ghế gần nhất. Quad dùng cạnh đóng, không mở rộng epsilon; nếu có cạnh chung, geometry index nhỏ hơn thắng, đã có test biên. Index và seat-ID lookup publish cùng ArenaRenderData, không dựng lại khi pan/zoom/theme.
- ArenaView chỉ dò khi frame gần nhất đã vẽ ghế (LOD 2), thuộc đúng dataset và khớp scale/translation hiện hành; không dò ghế còn ẩn hoặc frame camera chưa vẽ. Một ghế trúng được viền trắng/tối trên Canvas, giữ màu ghế và status-dot; hiện **khu / hàng / số ghế** từ dataset trong TextView hiện có. Tap trống ở LOD ghế xóa debug highlight và không trả ghế. Đổi dataset hoặc dỡ sân xóa highlight/thông tin cũ. Đây là focus tạm để kiểm tra, chưa phải selection nhiều ghế/ViewModel Phase 4.
- REQ-04/05/06, biên gesture: giữ pointer-ID/rebase của 3B. Tap bị hủy vĩnh viễn trong gesture sau POINTER_DOWN/UP, kéo vượt slop, CANCEL hoặc giữ quá long-press timeout. Kiểm slop cả ACTION_UP (khi không có MOVE) và historical samples của event gộp; pinch nhấc ngón không trở thành tap. Tọa độ dùng View-local, loại vùng ngoài padding viewport.
- REQ-13: index dựng trên worker và có cancellation; highlight Path/mảng touch/Paint tái sử dụng. Cập nhật Path highlight khi xử lý tap/đổi dataset, không trong onDraw; không I/O/query/index build trong onDraw. Chưa có số đo FPS/GPU trên Samsung.

### File của nhiệm vụ này

Tạo:

- `app/src/main/java/com/example/nbatickethub/geometry/SeatHitTester.java`.
- `app/src/test/java/com/example/nbatickethub/geometry/SeatHitTesterTest.java`.
- `app/src/androidTest/java/com/example/nbatickethub/ui/arena/SeatInspectionInstrumentedTest.java`.

Sửa: `ui/arena/ArenaView.java`, `ui/arena/ArenaRenderData.java`, `MainActivity.java` trong Java package hiện tại; `res/values/strings.xml`, `res/values/preview_colors.xml`; `tools/test-foundation.ps1` để runner fallback không bỏ qua test mới; `docs/PROGRESS.md`, `docs/AI_LOG.md`, `docs/ROADMAP.md`. Tổng 12 file thuộc 3C. Không sửa assets, 30 thiết kế đội, compiler/model, Matrix camera, layout, HTML hoặc cấu hình Gradle/SDK. Giữ các thay đổi 3A/3B có sẵn, không stage/commit/push.

### Kết quả thực tế

- **Trước sửa:** root `./gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-configuration-cache` đạt trong 2s (1 executed/75 up-to-date), log `.gradle/phase3b/pre3c-build.log`. Không thấy lỗi build còn sót từ 3B; kết quả cache không phải kiểm tra gesture runtime.
- **Bản 3C cuối:** cùng lệnh Wrapper trên root **BUILD SUCCESSFUL, 52s**, 19 executed/57 up-to-date, log `.gradle/phase3c/final-build.log`. XML: **54 tests, 0 failures/errors/skipped**. Lint **0 errors, 7 warnings** (AGP mới, plurals, overdraw nền, hai màu chưa dùng), không đổi toolchain để xử lý cảnh báo.
- 6 test JVM mới: quad xoay và khoảng trống trong AABB; cạnh/góc và ownership; ghế cắt nhiều grid cells; 5.000 điểm synthetic đối chiếu ray-crossing độc lập; **626.904 tâm ghế ở 30/30 sân** tìm đúng index, 30 điểm court + 30 điểm lối đi không hit và 3.000 điểm world ngẫu nhiên đối chiếu oracle; cancellation worker. Đây là kiểm chứng world-space/index, không chứng minh inverse Android/tap đã chạy.
- Fallback `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`: **OK (54 tests)**, JUnit 45.181s, log `.gradle/phase3c/foundation.log`. Không dùng fallback thay bằng chứng build Wrapper.
- 2 test Android tích hợp mới đã compile: inverse tap theo đúng ID trước/sau pan/zoom, metadata và highlight, overview/camera-chưa-vẽ không query, lối đi và đổi ba sân; drag/CANCEL/UP không MOVE/historical excursion, nhấc từng ngón sau pinch không tap, gesture mới tap lại bình thường. **Chưa chạy** vì `adb devices` trả danh sách rỗng; không chạy connectedDebugAndroidTest. Chưa có kiểm tra trực quan, thao tác Samsung, frame/GPU hoặc bằng chứng sửa ANR cũ.
- Đối chiếu baseline đầu 3C: toàn bộ 34 assets, CourtRenderData và ViewportTransform (36 file) không đổi byte; các app source có sẵn chỉ đổi 5 file đã liệt kê. APK chứa đúng 34 assets và SeatHitTester trong DEX. APK **8.470.614 byte**, SHA-256 `23450ca323ecd8fabb349ebac88f9168719336f693a10aa63e94d973126ce364`.

APK để test: `app/build/outputs/apk/debug/app-debug.apk`. Khi có Samsung kết nối và đã ủy quyền ADB: `./gradlew.bat connectedDebugAndroidTest` để chạy bộ test Android (gồm cả các ca 3B chưa chạy).

### Checklist Samsung — chưa có kết quả người dùng

- Mở sân ở góc vừa khung, chạm khu/ghế đang ẩn: không dò/highlight ghế. Zoom gần cho hiện từng ghế rồi chạm: viền đúng một ghế, khu/hàng/số ghế đúng vị trí.
- Kéo và pinch quanh ghế, chạm lại chính ghế đó: thông tin vẫn đúng; thử ghế ở nhiều tầng và sát mép viewport.
- Chạm lối đi, khoảng giữa ghế và mặt sân: không nhận ghế; debug highlight cũ được xóa khi tap trống ở mức ghế.
- Kéo xong thả ngón; pinch rồi lần lượt nhấc ngón trái/phải; thử CANCEL/ngắt thao tác: không có hit mới. Sau đó tap riêng một ghế vẫn hoạt động.
- Bấm Vừa khung rồi zoom lại; đổi MSG/Chase/Crypto hoặc đội khác: fit đúng, không giữ highlight/nhãn từ sân cũ. Thử dọc/ngang; ghi lại sân/thao tác nếu lệch ghế hoặc giật/đơ.

Chờ người dùng test/review 3C; chưa nghiệm thu toàn Phase 3, không chuyển Phase 4. Commit đề xuất: `feat: add indexed arena seat hit testing and inspection highlight`.

Kiểm tra cuối 3C: XML resources parse đạt; 8 Markdown files có fence cân bằng, 48 link cục bộ tồn tại; `git diff --check` đạt và staged rỗng. So baseline app/src xác nhận chỉ 5 file có sẵn được sửa đúng danh sách, cộng 3 file mới của 3C; các app files còn lại giữ nguyên. Không có kết quả Android runtime hoặc người dùng nghiệm thu mới.

## Màn sơ đồ nâng cấp và Phase 4 — 2026-11-25

**Đã triển khai; build/unit/lint và test APK compile đạt. Chưa có instrumentation runtime/Samsung hoặc nghiệm thu độ mượt/thẩm mỹ.** Người dùng giao Phase 4 rõ ràng; không coi việc giao việc là đã nghiệm thu 3B/3C hoặc đã thử trên máy.

### Chức năng và tiêu chí

- REQ-03/07/08: chọn/bỏ nhiều ghế AVAILABLE, không chọn ghế đã bán/khóa; viền chọn riêng giữ status-dot và màu nền của mọi theme. Lọc tầng/khu, xóa lọc, số vị trí/số còn trống; lựa chọn và tổng toàn bộ không mất theo lọc/LOD/reset. Danh sách thể hiện số ghế ngoài lọc, chạm hàng đó xóa lọc kèm thông báo và định vị; xóa từng hàng/xóa tất cả, list cuộn và tái sử dụng row.
- REQ-10/11/12: BottomSheetBehavior có tóm tắt count/tổng, phần chi tiết cuộn, nút Đổi ghế/Bỏ hoặc Chọn cố định dễ tiếp cận. ViewModel giữ selection/filter/replacement/focus/sheet và snapshot camera khi recreate. Không đưa dataset/index/bitmap vào Bundle. Process-death/snapshot phiên lớn vẫn là Phase 5, chưa làm trong nhiệm vụ này.
- Yêu cầu người dùng: header gọn, màu từ **homeTeamId của trận mô phỏng trong dataset**, chữ đen/trắng theo luminance; nút chính/reset cùng accent. Chọn 30 trận mẫu theo sân/đội qua điều khiển “Trận mẫu” thu gọn. Hai fixture đã kiểm logic: MSG/Knicks và Chase/Warriors. Không tạo trận NBA có thật hoặc giá mới.
- Chạm ghế mở thông tin khu/hàng/số ghế/giá/trạng thái và thumbnail **Góc nhìn mô phỏng**; chạm thumbnail mở dialog lớn với cùng nhãn. Eye position và độ cao mô phỏng khác theo vị trí/tầng/hàng; CourtRenderData dùng lại art hiện có. Không ảnh chụp, không dữ liệu sightline thật hoặc mức nhìn được đảm bảo.
- Đổi ghế thu panel, giữ ghế cũ tới khi có ghế mới hợp lệ. Hủy/tap trống/ghế khóa không làm mất ghế cũ; ghế đã có trong tập không làm trùng hoặc âm thầm giảm số lượng. Khi hủy quay về thông tin/góc nhìn ghế cũ. Mỗi action cập nhật một nguồn state.
- Camera: Canvas không đổi bounds theo sheet, không fit khi chọn/đổi/hủy/mở/thu chi tiết. Thông báo/legend cố định chiều cao, padding đáy 64dp cố định cho peek bar. Panel mở ở phía đối diện ghế theo nửa màn hình để tránh phủ điểm ghế, không dịch camera để né. Chỉ Vừa khung, đổi arena và list-row focus là hành động chủ động thay góc nhìn; rotation giữ tâm world/zoom ratio rồi clamp theo kích thước mới. Highlight fade 160ms; sheet dùng motion Material, chưa đo độ mượt trên thiết bị.

### File thuộc nhiệm vụ

Tạo:

- `geometry/SeatPerspective.java`.
- `state/ArenaSelection.java`, `ArenaSelectionViewModel.java`, `GameThemeResolver.java`.
- `ui/arena/ArenaFilterController.java`, `SeatSheetController.java`, `SeatPerspectiveView.java`.
- `res/layout/selection_item.xml`.
- `app/src/test/java/com/example/nbatickethub/state/ArenaSelectionTest.java`.
- `app/src/androidTest/java/com/example/nbatickethub/ui/arena/PhaseFourInstrumentedTest.java`.

Các đường dẫn Java main phía trên nằm trong `app/src/main/java/com/example/nbatickethub/`; res nằm trong `app/src/main/res/`.

Sửa `MainActivity.java`, `ArenaRenderData.java`, `ArenaView.java`, `ViewportTransform.java`; hai `activity_main.xml` dọc/ngang, `preview_colors.xml`, `strings.xml`; test Android `ViewportTransformInstrumentedTest.java`; `app/build.gradle.kts`, `gradle/libs.versions.toml` (khai báo ViewModel/LiveData qua catalog, không đổi AGP/SDK/Gradle/Java); runner `tools/test-foundation.ps1`; `docs/DESIGN.md`, `ROADMAP.md`, `PROGRESS.md`, `AI_LOG.md`. Tổng 26 file tạo/sửa. Không sửa assets hoặc HTML Arena Lab/mobile preview; ArenaCatalog được giữ nguyên sau khi tách resolver riêng.

### Kiểm chứng thực tế

- Trước sửa: root Wrapper `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-configuration-cache` **BUILD SUCCESSFUL 5s**, 1 executed/75 up-to-date (`.gradle/phase4-before.log`). Không coi cache build là kiểm runtime 3B/3C.
- Source cuối: cùng lệnh **BUILD SUCCESSFUL 1m4s**, 15 executed/61 up-to-date (`.gradle/phase4/final-verification.log`), gồm guard callback filter cũ và padding layout cố định. Checkpoint trước đó đạt 1m4s (`release-check.log`); một lượt chỉ đổi padding đạt 6s, sau đó log final-verification được chạy lại trên source cuối. Không dùng mirror.
- XML xác nhận **65 tests, 0 failures/errors/skipped**. 11 JVM tests mới kiểm toggle/tổng/idempotent remove, 300 lựa chọn không giới hạn 200, giữ ghế cũ khi trống/khóa/hủy, replace hợp lệ/trùng/ghế gốc, lọc không mất tập/tổng, focus ngoài lọc, chuyển tầng xóa khu sai, rebind cùng game/đổi game, 2 home-team themes và perspective thấp/cao khác nhau/finite. Fixture khu đã bán hết kiểm vị trí vẫn tồn tại nhưng available=0. Nhánh “không có vị trí” là dự phòng: các tầng/khu hợp lệ trong dữ liệu hiện tại đều có ghế, chưa có ca UI tạo bộ lọc 0 vị trí.
- Một lượt test trung gian thất bại do fixture thêm tầng không có section, validator từ chối đúng quy tắc. Đã sửa fixture sang section hợp lệ với inventory BOOKED; không sửa hoặc nới validator để làm test đạt. Log trung gian `.gradle/phase4/build-final.log` giữ lỗi đó; kết quả cuối ở log final-verification.
- Fallback runner sau khi thêm state sources/test: **OK (65 tests)**, JUnit 36.579s, `.gradle/phase4/foundation.log`; `./tools/test-foundation.ps1 -JdkPath 'C:/Program Files/Android/Android Studio/jbr'`. Đây là kiểm runner đã sửa, không thay build APK.
- Lint cuối **0 errors, 26 warnings**, gồm version dependency, plurals, legacy resources không dùng, overdraw/button style; không suppress để giả sạch hoặc tự nâng toolchain.
- Test Android mới có 2 ca Phase 4 (production MotionEvent/VM/controller): chọn lúc zoom, sheet hai phía không phủ điểm ghế và camera không đổi, thumbnail/dialog lớn, đổi ghế khóa/trống/hủy/thành công, filter và ActivityScenario.recreate; hai màu header theo game. Thêm 1 ca Matrix snapshot/restore ngữ nghĩa. Test APK đã compile, **chưa chạy**. `adb devices` ngoài sandbox trả danh sách rỗng; không chạy connectedDebugAndroidTest. Không có chứng cứ xoay máy thật/process death, screenshot pixel, FPS/GPU, hoặc khẳng định sửa ANR cũ.
- SHA-256 baseline: **43 file được bảo toàn** gồm 34 assets, CourtRenderData, ArenaCatalog và 7 file mobile preview. Hình học, ghế, giá/trạng thái mock, theme/court/name/logo của 30 đội không đổi. APK embeds đúng 34 assets và có ArenaSelectionViewModel/SeatPerspectiveView/SeatSheetController trong DEX.
- APK **8.537.424 byte**, SHA-256 `30a17adffe7111ec4b00cb43f95103a14dd0add60fecefd96f616b4871bd0ddf`; đường dẫn `app/build/outputs/apk/debug/app-debug.apk`. Không stage/commit/push.

### Checklist Samsung — chưa ghi nhận thực hiện

1. Chọn Knicks/MSG rồi Warriors/Chase trong Trận mẫu: header/nút/court đúng đội chủ nhà; legend trống/đã bán/khóa/đã chọn vẫn dễ phân biệt.
2. Zoom sâu rồi chạm ghế ở cả nửa trên và nửa dưới; mở/thu sheet và mở/đóng preview lớn: ghế và độ zoom không nhảy, panel không phủ điểm ghế đang xem. Thử cả màn ngang và cỡ chữ máy đang dùng.
3. So ghế sát sân với ghế tầng cao: góc nhìn có cảm giác khác, thumbnail/lớn cùng một vị trí, cả hai ghi Góc nhìn mô phỏng. Đây là review trực quan còn thiếu.
4. Đổi ghế: thử ghế mới trống, ghế đã bán/khóa, lối đi và Hủy đổi. Ghế cũ chỉ mất khi thay thành công; hủy giữ ghế cũ, pan/zoom không đổi.
5. Chọn nhiều ghế, bỏ/xóa từng hàng, lọc tầng/khu rồi xóa lọc, định vị hàng ngoài lọc và Vừa khung; kiểm tổng/số lượng. Xoay khi đã chọn/lọc/đang đổi/sheet mở: không mất ID hoặc bộ lọc, camera hợp lệ, nút Đổi ghế dễ thấy.

Khi có thiết bị ADB đã ủy quyền, chạy `./gradlew.bat connectedDebugAndroidTest`. Dừng để review màn sơ đồ/Phase 4; không làm đặt vé hoặc tự chuyển Phase 5. Commit đề xuất: `feat: complete arena seat selection filters and simulated seat views`.

Kiểm tra cuối: XML resources parse đạt; 8 Markdown files có fence cân bằng, 48 link cục bộ tồn tại; `git diff --check` đạt, staged rỗng. 43 protected hashes và 34 assets trong APK tiếp tục khớp; DEX có VM/resolver/sheet/perspective. Không có thêm kết quả Android runtime/Samsung hoặc nghiệm thu người dùng.


## Nâng cấp chọn trận → khu → preview vé → xác nhận ghế (2026-11-25)

**Đã triển khai trong phạm vi người dùng giao đến chọn ghế; chưa có nghiệm thu của người dùng.** Mục này thay trạng thái màn sơ đồ/spinner của các mục lịch sử phía trên. Hợp đồng thiết kế mới ở DESIGN mục 16, không gọi các bổ sung nhiều trận/góc nhìn/theme là yêu cầu nguyên văn của PDF.

### Thực hiện và đối chiếu tiêu chí

| Phạm vi | Kết quả triển khai | Tiêu chí / rubric liên quan |
| --- | --- | --- |
| Chọn trận | 31 card dữ liệu mẫu, gồm BOS @ NYK và MIA @ NYK cùng MSG, LAL @ GSW tại Chase; đủ tên/mã hai đội, giờ/sân/giá từ; không có bộ chọn sân độc lập | Yêu cầu 1; REQ-12/14 |
| Ranh giới dữ liệu | arenaId chọn geometry; homeTeamId chọn theme; immutable inventory một match có giá, trạng thái và loại vé theo matchId. Đổi cùng sân giữ khu/lọc/camera, xóa lựa chọn cũ; đổi sân mở toàn cảnh | Yêu cầu 1; REQ-03/12 |
| Chạm khu ở overview | Polygon + nearest-label hit-test qua inverse Matrix hiện có; highlight và mở sheet ngay, không cần pinch/focusSeat | Yêu cầu 2; REQ-01/04–06 |
| Preview/CTA | Representative seat → tap dòng đổi candidate/ảnh → CTA mới xác nhận. Booked/locked hoặc ngoài lọc không xác nhận được. Đổi ghế giữ ghế cũ đến confirm hợp lệ; ID/tổng không bị đổi khi chỉ xem vé | Yêu cầu 2; REQ-03/10–12 |
| Sheet mobile | Preview 16:9, mở toàn màn hình; vé nền trắng, giá nhấn mạnh, một ListView cuộn và CTA cố định. Dọc preview trên/vé dưới; ngang preview và vé hai cột. Scroll theo khu/lọc giữ trong ViewModel; sheet không gọi fit/zoom | Yêu cầu 2–3; REQ-09–12 |
| Renderer/theme/lọc | Court/logo/30 theme cũ; nền/họa tiết/font nhóm theo homeTeamId. Camera từ ghế, rổ cao 10ft mô phỏng và khán đài đối diện; thấp/cao/side/end khác qua tọa độ và độ cao. Lọc loại/ngân sách đồng bộ vé và mask Canvas; vài nhãn giá cache ngoài onDraw; legend giữ trạng thái | Yêu cầu 2–3; REQ-07/08/13 |

Không sửa JSON/geometry/logo/court art gốc, không dùng ảnh tham khảo làm asset của app, không thêm framework/dependency hoặc thay SDK/AGP/Java. Tất cả lịch/đối thủ ghép/giá/loại/trạng thái là mô phỏng. Tên font tiêu đề là lựa chọn UI của app, không tuyên bố font thương hiệu chính thức.

### File của nhiệm vụ này (27 file nguồn/tài liệu)

- `app/src/main/java/com/example/nbatickethub/MainActivity.java`.
- `app/src/main/java/com/example/nbatickethub/data/mock/MockMatchSchedule.java`.
- `app/src/main/java/com/example/nbatickethub/geometry/SectionHitTester.java`.
- `app/src/main/java/com/example/nbatickethub/geometry/SeatPerspective.java`.
- `app/src/main/java/com/example/nbatickethub/model/GameSeat.java`.
- `app/src/main/java/com/example/nbatickethub/state/ArenaSelection.java`.
- `app/src/main/java/com/example/nbatickethub/state/ArenaSelectionViewModel.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/ArenaFilterController.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/ArenaPreviewLoader.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/ArenaRenderData.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/ArenaView.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/SeatPerspectiveView.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/SeatSheetController.java`.
- `app/src/main/java/com/example/nbatickethub/ui/arena/TeamAppearance.java`.
- `app/src/main/res/layout/activity_main.xml`.
- `app/src/main/res/layout-land/activity_main.xml`.
- `app/src/main/res/layout/section_sheet_header.xml`.
- `app/src/main/res/values/strings.xml`.
- `app/src/test/java/com/example/nbatickethub/state/MatchSeatFlowTest.java`.
- `app/src/test/java/com/example/nbatickethub/geometry/SectionHitTesterTest.java`.
- `app/src/androidTest/java/com/example/nbatickethub/ui/arena/PhaseFourInstrumentedTest.java`.
- `app/src/androidTest/java/com/example/nbatickethub/ArenaPreviewInstrumentedTest.java`.
- `tools/test-foundation.ps1`.
- `docs/DESIGN.md`.
- `docs/ROADMAP.md`.
- `docs/PROGRESS.md`.
- `docs/AI_LOG.md`.

Các scripts/ảnh/log QA trong `tmp/seat-flow/` và `.gradle/seat-flow*.log` là artifact cục bộ, không tự stage. Working tree đã có thay đổi của các nhiệm vụ trước; giữ nguyên các thay đổi không thuộc danh sách trên. Không reset/clean/commit/push.

### Kiểm chứng đã quan sát

- Gradle Wrapper `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --no-configuration-cache`: BUILD SUCCESSFUL; 72 JVM tests (65 có trước + 5 luồng match/confirmation + 2 section hit-test). Không coi số lượng test là nghiệm thu UX.
- Connected lượt đầu ở cỡ mặc định khoảng 411dp qua ca PhaseFour cũ trước thay đổi pin preview. Lượt 360dp đầu phát hiện lỗi thật: adapter còn ID sân cũ khi thay dataset, gây NPE ở TicketAdapter.getView. Đã xóa adapter và preview ngay khi bắt đầu load, giữ nguyên test đổi sân. Không nới assertion để làm test qua.
- Chạy lại ở **360×640dp** bằng override tạm 945×1680px / 420dpi: `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.nbatickethub.ui.arena.PhaseFourInstrumentedTest --no-configuration-cache` **BUILD SUCCESSFUL 1m48s**, 77 tasks; **2 Android tests, 0 failures/errors/skipped**. Qua luồng overview/candidate/CTA/blocked replacement/recreate/same-arena/new-arena và kiểm preview 16:9, còn ít nhất 48dp danh sách vé, low/high preview pixels khác. Log `.gradle/seat-flow-360.log`. Cỡ emulator được khôi phục trong finally.
- Lint ở lượt này 0 errors/49 warnings; phần lớn là resource cũ không còn dùng, version dependency và style/overdraw. Không nâng toolchain hoặc suppress để che kết quả. Kết quả bản cuối và ảnh ở phần bổ sung dưới.

### Giới hạn và điểm dừng

Chưa có review của người dùng/Samsung, đo FPS/GPU/heap hoặc kiểm chứng sightline thực tế. Góc nhìn do renderer mô phỏng, không phải ảnh chụp. Một lần System UI của emulator báo ANR khi vừa khởi động; không quy kết đó là crash app hoặc nhận đã chữa ANR. Phần giữ state giới hạn trong configuration change, không nghiệm thu process death/Phase 5. HTML prototype không được thay đổi trong nhiệm vụ Android này.

Đề nghị review: từ card đầu vào toàn sân → chọn khu và vé; so khu sát sân/bên hông/sau rổ/tầng cao; mở lớn; xác nhận/đổi/hủy; kiểm lọc và giữ vị trí cuộn; đổi hai trận MSG rồi Warriors và xoay máy. Người dùng chưa xác nhận đã thực hiện các bước này.

Dừng tại chọn ghế, chờ duyệt. Không có vấn đề phạm vi nào cần xin phép thêm. Commit đề xuất: `feat: add match-scoped section ticket selection`.


### Bản bàn giao cuối — bằng chứng sau sửa lỗi và thu gọn thông tin vé

- **Build/JVM/lint:** `assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest` bằng Wrapper, `--no-configuration-cache`; **72 JVM tests, 0 failures/errors/skipped**. Lint **0 errors/52 warnings**; không suppress hoặc đổi phiên bản công cụ. Log bản cuối `.gradle/seat-flow-release-360.log`.
- **360×640dp:** override tạm 945×1680px với 420dpi, lọc test theo package `com.example.nbatickethub.ui.arena`: **13 tests, 0 failures/errors/skipped**, gồm 3 PhaseFour, 6 ViewportTransform, 2 SeatInspection, 1 ArenaCamera và 1 ArenaTheme. Bao gồm inverse Matrix/pan/pinch/biên, cache giữ khi đổi theme, overview section tap, candidate/CTA, blocked replacement, đổi trận cùng sân/khác sân, recreate, xoay ngang/dọc, ảnh thấp/cao khác và fullscreen không đổi camera. XML: `tmp/seat-flow/results-release-360.xml`.
- **Khoảng 411.4×914.3dp:** kích thước gốc 1080×2400px / 420dpi, chạy lại **3 PhaseFour tests, 0 failures/errors/skipped**, log `.gradle/seat-flow-release-411.log`, XML `tmp/seat-flow/results-release-411.xml`. Cỡ emulator được khôi phục trong finally sau lượt 360dp. Test lần này kiểm **getGlobalVisibleRect** của vé/CTA: không chỉ có chiều cao layout mà nằm trong màn hình; danh sách vẫn còn tối thiểu 48dp sau khi preview vé. CTA chờ có candidate mới hiện; dòng mô tả candidate một dòng để không lấn vùng vé.
- **QA ảnh:** đã lấy 6 PNG cho mỗi cỡ màn: overview, low-section, high-section, ticket-preview, fullscreen và landscape. [Toàn cảnh 360dp](../tmp/seat-flow/qa-360-final/overview.png), [khu thấp](../tmp/seat-flow/qa-360-final/low-section.png), [vé đang xem](../tmp/seat-flow/qa-360-final/ticket-preview.png), [toàn màn hình](../tmp/seat-flow/qa-360-final/fullscreen.png); [khu thấp 411dp](../tmp/seat-flow/qa-411-final/low-section.png), [tầng cao](../tmp/seat-flow/qa-411-final/high-section.png), [ngang](../tmp/seat-flow/qa-411-final/landscape.png). Kiểm tra ảnh để xác nhận khu thấp/cao có phối cảnh/độ cao khác, mặt sân/rổ/khán đài hiện theo khung mô phỏng. Không coi ảnh này là người dùng đã nghiệm thu.
- **Lỗi được sửa trong quá trình nghiệm tra:** adapter giữ ID sân cũ gây NPE khi đổi arena; `layout_gravity=bottom` cộng vị trí với BottomSheetBehavior đẩy vé/CTA ngoài màn hình (đổi sang top|end và tăng assertion vùng nhìn thấy); mất active pointer trong MOVE vẫn cho UP thành tap (hủy cả gesture, giữ nguyên test hồi quy cũ). Lượt rộng trung gian 12/13 qua, lỗi pointer ở `.gradle/seat-flow-360-regression.log`; bản bàn giao cuối 13/13 qua. Lỗi hình học/camera không được che bằng việc bỏ test. File ảnh từ thư mục app bị harness xóa sau uninstall, nên QA sau chuyển sang Download trên emulator rồi pull về workspace; đây là thao tác test, không phải production I/O.
- **Artifact:** [APK debug](../app/build/outputs/apk/debug/app-debug.apk), **8,550,237 byte**, SHA-256 `0f969a21d2c5acf1e3458b4c3800c81ebede8a83982a42881ec7fb2c087c79f8`. Toàn bộ **34 asset files** trong APK khớp byte với source hiện tại; Git không có diff assets. XML resources parse và kiểm Markdown/local links thực hiện sau cập nhật; `git diff --check` đạt. Staged paths: **0**; AI không stage/commit/push.

Các kết quả emulator là bằng chứng kiểm thử do AI thực hiện, chưa thay kiểm tra trên Samsung hoặc xác nhận người dùng. Không có đo FPS/GPU/heap và không nghiệm thu process death. Đã cập nhật PROGRESS/AI_LOG/DESIGN/ROADMAP; giữ điểm dừng để người dùng duyệt. Không làm thêm chức năng sau chọn ghế.


## Đồng bộ HTML preview cho QA — 2026-11-25

Theo yêu cầu mới, tái dùng prototype cũ tại `tools/arena-preview/index.html`, renderer và 30 payload hình học/theme Java đã xuất. Chỉ sửa preview; không sửa Android hoặc assets/export gốc. Đã thêm 31 trận mẫu theo lịch MockMatchSchedule, cùng công thức hash/trạng thái/giá/loại vé; chọn khu từ overview, representative/candidate/CTA xác nhận, vé theo khu, đổi trận cùng sân giữ khu/lọc/camera và xóa lựa chọn, đổi sân fit; preview có rổ/khán đài, 16:9/fullscreen, theme và lọc loại/ngân sách. Đây là HTML mô phỏng phục vụ QA, không phải app Android hoặc booking thật.

File sửa: `tools/arena-preview/index.html`, `mobile.js`, `mobile.css`, `selection-model.js`, `seat-perspective.js`, `arena-renderer.js`, `source-data.js`, `test-preview.cjs`, `README.md`; `docs/PROGRESS.md`, `docs/AI_LOG.md`. Source-data sinh lại từ 30 theme/geometry snapshot và manifests sẵn có, không tạo tọa độ ghế mới.

Kiểm tra: `node --check` các JS thay đổi và `node tools/arena-preview/test-preview.cjs` đạt. Test host dùng dữ liệu thật của prototype + DOM/Canvas doubles, kiểm 31 trận/30 đội, preview chưa chọn, xác nhận/blocked, chạm khu ở overview, danh sách vé, fullscreen, đổi cùng sân giữ lọc/camera và đổi arena reset. Browser host báo unavailable: chưa kiểm pixel/fit trong browser, không lấy test doubles làm nghiệm thu UI. Không chạy lại Gradle vì không sửa Android. Hướng dẫn QA ở `tools/arena-preview/README.md`.

Mở file HTML trực tiếp và Ctrl+F5 để QA ở 360–412px; chờ người dùng review. Không commit/push. Commit đề xuất: `feat: sync HTML seat preview with match and section flow`.

## QA HTML Phase 4 — 2026-11-26

Lượt này tiếp tục phần bị ngắt, chỉ QA preview hiện có; không thay đổi code Android/HTML vì chưa phát hiện lỗi ứng dụng cần sửa. Các thay đổi lịch tương đối và bộ lọc đã có trước lượt QA được giữ nguyên. Kết quả dưới đây thay cho giới hạn browser unavailable của mục HTML trước, không thay bằng chứng Android.

- Mở trực tiếp `tools/arena-preview/index.html` bằng Chrome Guest, DevTools Responsive **390 × 844 CSS px**, múi giờ Asia/Saigon. Browser connector báo host unavailable nhưng điều khiển cửa sổ Chrome bằng Computer Use hoạt động; không cần cài browser automation.
- **Đạt trên Chrome:** mặc định 7 ngày gồm 26/11–2/12, nhóm theo ngày; Hôm nay có BOS@NYK và LAL@GSW; chọn 27/11 bằng lịch chỉ còn MIA@NYK. Chọn Knicks chuyển sang mọi trận sắp tới, có cả sân nhà/khách và trận 6/12 ngoài 7 ngày. Chỉ sân nhà còn đúng ba trận BOS/MIA/LAL tại MSG; kết hợp ngày 27/11 còn một trận. Xóa lọc về 7 ngày/Tất cả đội và ẩn Chỉ sân nhà. Quay lại danh sách giữ bộ lọc.
- **Đạt trên Chrome:** từ toàn cảnh chạm nhãn L05 mở sheet, góc nhìn và vé ngay, không zoom. Cuộn tới R1 ghế 9, chạm để cập nhật góc nhìn rồi bấm Chọn ghế: 1 ghế/86,25 US$. Đổi ghế giữ khu/vị trí cuộn và ghế cũ; chạm R1 ghế 10 chỉ đổi preview, xác nhận mới thay ghế. Thu/mở sheet giữ toàn cảnh, không nhảy bản đồ; CTA nằm trong khung mobile. Chạm U12 ở toàn cảnh cho góc nhìn cao/rộng khác rõ L05.
- **Đạt trên Chrome:** đổi BOS@NYK → MIA@NYK cùng MSG giữ L05 và toàn cảnh, không mang ghế cũ (0 ghế); giá khu/vé đổi **86,25 → 91,50 US$**, đối thủ/giờ đổi đúng; loại R1 ghế 6 đổi VIP sang Tiêu chuẩn, ghế 7 đổi Tiêu chuẩn sang VIP trong danh sách vé trận mới.
- **Kiểm tra host:** `node tools/arena-preview/test-preview.cjs` PASS (33 trận tương đối/30 theme, date/team/home filters, loại trận đã bắt đầu, confirmation/blocked, overview, fullscreen, same-arena camera/filter, new-arena reset). DOM/Canvas doubles chỉ chứng minh logic; kiểm tra hình ảnh/thao tác nêu trên chạy riêng trên Chrome. `node --check tools/arena-preview/mobile.js`, `node --check tools/arena-preview/match-list.js` và `git diff --check` đạt; Git có cảnh báo LF/CRLF ở file cũ, không có lỗi whitespace. Staged paths: 0.
- **Giới hạn:** lượt này chỉ QA HTML ở 390px; không chạy lại Gradle/emulator, xoay Android hoặc nghiệm thu 360/412dp. Loại trận đã bắt đầu được kiểm tra bằng host test, không đợi đồng hồ Chrome qua giờ bắt đầu. Chưa QA thủ công mọi đội, fullscreen hoặc pan/pinch ở mức zoom khác; test host có kiểm tra camera số. Không suy ra người dùng đã nghiệm thu. Những kết quả Android trước chỉ áp dụng đúng phiên bản đã ghi, không chứng minh mọi thay đổi lịch mới đã chạy thiết bị.

Mở lại: Chrome → Ctrl+O → chọn `D:\24tn-fitus\project\nba-tickethub\tools\arena-preview\index.html` → F12, bật Device Toolbar và đặt 390 × 844; Ctrl+F5 nếu đang giữ trang cũ. Không cần local server; giữ nguyên các file JS/CSS và thư mục dữ liệu đi kèm.

Chỉ cập nhật `docs/PROGRESS.md`, `docs/AI_LOG.md` trong lượt QA này. Dừng chờ review, không commit/push. Commit đề xuất cho phần ghi nhận QA: `docs: record Phase 4 mobile preview QA`.


## Yêu cầu xuất bản main sau 6B — 2026-11-26

Người dùng yêu cầu `push len main`, cho phép commit/push gói source/resource/test/HTML/tài liệu 6A+6B (6B phụ thuộc nền 6A còn local). Giữ nguyên lịch sử điểm dừng trước đó; yêu cầu mới không đồng nghĩa nghiệm thu browser/Samsung. Gói 33 file theo allowlist, loại PDF/công cụ cũ/generated output/cache/bí mật. Kết quả kiểm thử cùng source ở mục 6B; remote được kiểm SHA sau khi push. Tiêu đề README preview và DESIGN cập nhật phản ánh 6B.
