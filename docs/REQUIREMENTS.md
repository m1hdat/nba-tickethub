# Yêu cầu chính thức của đề

Cập nhật: 2026-11-24. Đã đọc đủ 5 trang [đề thi giữa kỳ CNTN2024](sources/De_thi_giua_ky_CNTN2024.pdf). Nội dung dưới đây diễn giải yêu cầu có nguồn; phương án ứng dụng nằm trong [DESIGN.md](DESIGN.md) và **chưa được người dùng duyệt**.

## Phạm vi đề gốc

- Học phần: Phát triển phần mềm cho thiết bị di động; chủ đề Interactive Spatial Map; bài cá nhân, thời gian 01 tuần (trang 1). Không có ngày bắt đầu/hạn nộp cụ thể trong PDF.
- NBA Ticket Purchasing được đề xuất ánh xạ vào đề tài 1: sơ đồ chỗ ngồi concert/sân vận động, chia Standing, VIP, Standard và hiển thị trạng thái vị trí (trang 1, mục 1). Bố cục NBA cụ thể cần người dùng duyệt.
- Được dùng mock data; API và cơ sở dữ liệu không bắt buộc (trang 1). Không có yêu cầu đăng nhập, checkout, thanh toán thật hoặc dữ liệu NBA trực tiếp.
- Yêu cầu/checklist ghi 50–250 đối tượng, nhưng rubric ghi 150–250 (trang 1, 4–5). Thiết kế đề xuất 200 vị trí đáp ứng cả hai khoảng, không tự sửa số trong đề.

## Yêu cầu và tiêu chí nghiệm thu

Mã REQ do tài liệu này đặt để truy vết, không có sẵn trong đề. Tất cả là P0 cho bài nộp; P0 là phân loại ưu tiên thiết kế.

| Mã | Nội dung và tiêu chí nghiệm thu diễn giải | Nguồn PDF |
| --- | --- | --- |
| REQ-01 | Một Canvas tự vẽ toàn bộ sơ đồ; dữ liệu độc lập lớp vẽ; không dùng 50–250 View/Button thay đối tượng | Trang 2, mục 3.1 |
| REQ-02 | Đủ số đối tượng; phân khu, lối đi/khoảng cách, nhãn; nhận biết tổng thể ở góc nhìn mặc định. Mục tiêu thiết kế 200 vị trí | Trang 1, mục 2.1; trang 4, rubric mục 1 |
| REQ-03 | Ít nhất 3 trạng thái trực quan; chọn rõ ràng, bỏ chọn trả trạng thái đúng, không đổi vị trí khác | Trang 1–2, mục 2.1 |
| REQ-04 | Pan một ngón có biên, không kéo sơ đồ hoàn toàn khỏi màn hình | Trang 1, mục 2.1 |
| REQ-05 | Pinch liên tục quanh tâm hai ngón, có giới hạn zoom min/max | Trang 1, mục 2.1 |
| REQ-06 | Screen/world chuyển đổi đúng; tap chính xác sau pan/zoom và gần biên | Trang 1, mục 2.1; trang 2, mục 3.2 |
| REQ-07 | Legend đủ ký hiệu/trạng thái, dễ truy cập, không che đáng kể sơ đồ | Trang 2, mục 2.2 |
| REQ-08 | Filter hoặc search ít nhất một tiêu chí; làm mờ/ẩn phần không khớp, selection nhất quán; hiện điều kiện, xóa lọc, thông báo rỗng | Trang 2, mục 2.2 |
| REQ-09 | Reset View mượt về vị trí/tỷ lệ mặc định; không tự xóa filter/selection nếu không thông báo rõ | Trang 2, mục 2.2 |
| REQ-10 | Bottom Sheet thu gọn có tóm tắt; mở rộng có chi tiết/danh sách cuộn; không phủ hết vùng tương tác | Trang 2, mục 2.3 |
| REQ-11 | Sheet định vị/highlight và xóa từng mục; đồng bộ hai chiều tức thì; không trùng/lệch sau filter/reset/rotation | Trang 2, mục 2.3 |
| REQ-12 | Tách dữ liệu, state, lớp vẽ; nguồn thống nhất; giữ selection khi xoay; dọc/ngang ổn định | Trang 3, mục 3.3 |
| REQ-13 | Tái dùng Paint/Path/Rect/hình học; không tạo lặp đồ họa nặng trong onDraw; chỉ redraw khi cần; tương tác mượt | Trang 3, mục 3.4 |
| REQ-14 | Build/chạy được; mã rõ ràng, ít lặp, xử lý biên; không nộp output tự sinh/dữ liệu thừa | Trang 3, mục 5.1; trang 4, rubric mục 5 |
| REQ-15 | GitHub repository truy cập được; tối thiểu 05–07 commit có ý nghĩa, phân bổ thời gian; không dồn bài vào 1–2 commit cuối | Trang 3, mục 5.1–5.2; trang 5, checklist |
| REQ-16 | Video 01–05 phút: pan, pinch, chọn/bỏ, filter/search, sheet, đồng bộ, reset, giữ state khi xoay; link truy cập được | Trang 3, mục 5.3; trang 5, checklist |
| REQ-17 | README: đề tài, chạy, kiến trúc, dữ liệu, công thức tọa độ, hit-test, state, tối ưu, link video, nhật ký AI | Trang 3, mục 5.4 |
| REQ-18 | Nhật ký nêu Agent, nhiệm vụ, mã sinh/sửa, hạn chế và cách sinh viên thực sự kiểm tra/điều chỉnh; sinh viên hiểu, chịu trách nhiệm và tuân thủ học thuật | Trang 3, mục 4; trang 4, mục 5.5 |

## Rubric (10 điểm)

| Hạng mục | Tỷ trọng |
| --- | --- |
| Canvas, bố cục và hiệu năng | 25% |
| Tương tác và xử lý tọa độ | 25% |
| Chức năng và trải nghiệm người dùng | 20% |
| Kiến trúc và quản lý trạng thái | 15% |
| Chất lượng mã nguồn và độ ổn định | 10% |
| Hồ sơ và quy trình nộp bài | 5% |

Trang 4, mục 7: chức năng không chạy/không kiểm tra được không có điểm; Canvas sai yêu cầu bị giới hạn 5/25 điểm mục Canvas; hit-test sai sau pan/zoom không có điểm hit-test; app không build/khởi chạy có tổng tối đa 5/10; mở rộng không thay thế bắt buộc.

## Phạm vi công việc hiện được cho phép

Phase 1 chỉ phân tích và thiết kế, cập nhật tài liệu, ghi AI log và đề xuất commit message. Không triển khai application code, không tự khởi tạo Git, commit/push; dừng để người dùng review và thống nhất thiết kế. Chưa có xác nhận các lựa chọn trong DESIGN.
