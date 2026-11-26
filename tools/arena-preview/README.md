# NBA TicketHub · Preview Phase 5

**Hướng dẫn hiện hành — 2026-11-26.** Các mục bên dưới giữ mô tả bản cũ để đối chiếu; kết quả mới nằm tại PROGRESS.

Mở Chrome → Ctrl+O → chọn `D:\24tn-fitus\project\nba-tickethub\tools\arena-preview\index.html`. Không cần server/npm/build. Ctrl+F5 nếu đang giữ trang cũ. F12 → Toggle device toolbar → Responsive 390 × 844.

Giữ nguyên các JS/CSS, `../../preview/arena-data/` và `../../app/src/main/assets/logos/`. Logo dùng offline; nguồn/hash ở `app/src/main/assets/logos/SOURCES.json`, không tuyên bố giấy phép mở. Logo poster không thay court artwork của bộ vẽ sân.

**Sau khi clone từ GitHub:** payload sân là output được ignore (~109 MB), nên tạo một lần bằng PowerShell tại thư mục gốc:

```powershell
./tools/export-arena-preview.ps1
```

Exporter dùng JAVA_HOME hoặc JDK đi kèm Android Studio tại `C:/Program Files/Android/Android Studio/jbr`; có thể truyền `-JdkPath` nếu JDK nằm nơi khác. Dùng JSON đã commit, không chạy lại generator authoring và không cần tải thêm dữ liệu. Sau đó mở index.html như trên.

1. Trang chủ: bấm poster → Chi tiết trận → CTA Chọn ghế → toàn sân → chạm khu → xem góc nhìn/vé → chọn/đổi ghế. CTA vàng ngay Trang chủ vào sân trực tiếp.
2. Lịch đấu: các bộ lọc Phase 4; bấm thẻ mở detail. Nút ‹ trên sân về màn trước; Đổi trận mở lịch. Quay lại giữ lọc/cuộn.
3. Đội bóng: 30 đội. Chọn Knicks thấy cả nhà/khách và ngoài 7 ngày. NYK @ BOS phải vào sân/theme Boston. So BOS @ NYK và LAL @ GSW để duyệt hai nhận diện.
4. Đổi BOS @ NYK → MIA @ NYK giữ khu/lọc/camera, xóa ghế cũ và tải giá/vé mới. Đổi sân vừa khung; thu/mở sheet không tự zoom.

33 trận dùng ngày tương đối khi mở trang; lịch/giá/trạng thái vé đều mô phỏng. Chưa có đặt vé/thanh toán. Android dùng Java/Views/Canvas; HTML là bản duyệt, không nhúng bằng WebView.

`node tools/arena-preview/test-preview.cjs` dùng DOM/Canvas doubles và payload sân để kiểm logic, không thay QA pixel. Kết quả Chrome 390px/emulator và giới hạn Samsung ở `docs/PROGRESS.md`, mục Phase 5 ngày 2026-11-26. Chờ người dùng review.

## Mô tả lịch sử Mobile Arena Lab

Mở `index.html` bằng Edge/Chrome. Bấm **Xem chi tiết trận → Xem sơ đồ sân → Phóng to khu ghế mẫu**, rồi chạm ghế để xem khu/hàng/số ghế và giá mẫu. Có nút **Quay lại** trên các màn sau; nút +/−, Vừa khung, kéo và pinch trên sơ đồ.

Chạy offline bằng `file://`, không cần npm/server. Giữ nguyên cấu trúc repository: preview đọc payload hiện có tại `../../preview/arena-data/msg.js`. Nếu thiếu payload, trang báo lỗi và có nút thử lại.

Trận Knicks–Celtics, lịch và giá là giả lập; không có đặt/giữ vé hay thanh toán. Màn Trang chủ/Chi tiết trận gắn nhãn Mockup vì chưa có trên Android. Đây là preview HTML local, không thay đổi Android.

`source-data.js` lấy nguyên theme Knicks/Celtics và court markings từ snapshot Java-exported của Arena Lab. `arena-renderer.js` tái sử dụng code Path2D/court từ `preview/arena-preview.template.html`; khán đài, ghế, màu và trạng thái dùng dữ liệu xuất sẵn, không sinh tọa độ mới. Giá mẫu USD tính riêng cho mockup, không phải dữ liệu giao dịch.

Kiểm tra bằng `node tools/arena-preview/test-preview.cjs` từ root: DOM/Canvas doubles kiểm luồng bấm và hit-test. Không thay cho kiểm pixel/browser thực. Không xuất PNG hoặc cập nhật AI log.


## QA luồng mới (2026-11-25)

Mở `index.html` trực tiếp bằng Chrome/Edge (giữ các JS/CSS và `../../preview/arena-data/` đúng vị trí), rồi Ctrl+F5 nếu đang mở bản cũ. Không cần npm/build/server.

Thử BOS @ NYK → chạm vùng hoặc nhãn khu từ toàn cảnh → xem ảnh/vé → chạm vé → Chọn ghế. Đổi ghế giữ ghế cũ đến xác nhận; ghế bán/khóa chỉ được xem. Đổi sang MIA @ NYK giữ khu/lọc/camera, xóa ghế cũ; sang Warriors mở toàn sân và đổi theme. Có lọc loại/ngân sách, góc nhìn 16:9/fullscreen và tiện ích tìm khu thấp/cao cho QA. Toàn bộ 31 trận, giá và trạng thái là dữ liệu mẫu.

`node tools/arena-preview/test-preview.cjs` kiểm state/luồng với dữ liệu hình học thật của prototype và DOM/Canvas doubles; không chứng minh pixel hoặc runtime Android. Browser host của phiên này không khả dụng, nên cần người dùng QA trực quan ở 360–412px. Chưa nghiệm thu HTML/Android pixel parity.
