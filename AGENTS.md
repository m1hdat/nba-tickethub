# Quy tắc làm việc — NBA TicketHub

## 1. Phạm vi và nguồn thông tin

- Đọc AGENTS.md, docs/REQUIREMENTS.md, docs/ROADMAP.md,
  docs/PROGRESS.md và docs/DESIGN.md (nếu có) trước khi sửa dự án.
- Đề bài chính thức trong docs/sources/ là nguồn tham chiếu
  cho các yêu cầu bắt buộc và rubric.
- Phân biệt yêu cầu chính thức, quyết định thiết kế đã thống nhất
  và tính năng mở rộng.
- Không tự suy diễn yêu cầu, bịa dữ liệu hoặc mở rộng phạm vi.
- Ưu tiên hoàn thành toàn bộ yêu cầu P0 trước tính năng mở rộng.
- Chỉ thực hiện phase hoặc nhiệm vụ được người dùng giao.
- Nếu yêu cầu chưa rõ hoặc có mâu thuẫn, báo cáo để người dùng xác nhận.
- Giữ nguyên những thay đổi không liên quan của người dùng.
- Không tự ý reset, clean, xóa file hoặc thay đổi cấu hình máy.

## 2. Công nghệ và cấu trúc dự án

- Sử dụng Java và Android Views/XML theo nền dự án hiện tại.
- Không chuyển sang Kotlin, Jetpack Compose hoặc WebView.
- Không tự ý thêm framework hoặc thay đổi kiến trúc đã thống nhất.
- Các thư mục chính:
  - app/src/main/java/com/example/nbatickethub/: mã Java.
  - app/src/main/res/: layout, chuỗi, màu, theme và tài nguyên.
  - app/src/main/assets/: dữ liệu JSON và tài nguyên phù hợp.
  - app/src/main/AndroidManifest.xml: cấu hình ứng dụng.
  - app/src/test/: unit test trên JVM.
  - app/src/androidTest/: kiểm thử trên thiết bị/emulator.
  - gradle/libs.versions.toml: quản lý dependency và plugin.
  - docs/: tài liệu dự án và AI log.
- Chỉ tạo package và class khi có trách nhiệm cụ thể.
- Giữ Activity và Fragment gọn, tách UI, dữ liệu và nghiệp vụ
  theo kiến trúc đã được thống nhất.

## 3. ArenaView và Canvas

- Sơ đồ sân bóng và ghế phải được triển khai bằng Android Custom View.
- ArenaView kế thừa View và sử dụng Canvas, Paint trong onDraw().
- Vẽ sân, khán đài, khu vực ghế và trạng thái ghế bằng Canvas.
- Không dùng ảnh sơ đồ ghế tĩnh, WebView, Button hoặc các View riêng lẻ
  để thay thế phần sơ đồ ghế được vẽ bằng Canvas.
- Sử dụng Matrix để quản lý phép biến đổi tọa độ, pan và zoom.
- Sử dụng inverse Matrix để chuyển tọa độ touch trên màn hình
  về hệ tọa độ logic của sân khi thực hiện hit-testing.
- Hỗ trợ thao tác kéo, phóng to, thu nhỏ và chọn ghế.
- Phân biệt rõ tọa độ màn hình và tọa độ logic của sơ đồ.
- Đồng bộ trạng thái ghế, màu sắc, legend và bộ lọc.
- Quản lý trạng thái lựa chọn bằng ViewModel theo thiết kế đã thống nhất.
- Đảm bảo trạng thái cần thiết được giữ khi xoay màn hình.
- Tách logic tọa độ và hit-testing để có thể kiểm thử độc lập.
- Viết kiểm thử cho phép biến đổi tọa độ, hit-testing
  và logic lựa chọn ghế.
- Tránh tạo object không cần thiết trong onDraw().
- Không thực hiện I/O hoặc xử lý nghiệp vụ nặng trong onDraw().

## 4. Coding conventions

- Java/XML sử dụng 4 khoảng trắng, UTF-8 và định dạng Android Studio.
- Class: PascalCase.
- Method và variable: camelCase.
- Constant: UPPER_SNAKE_CASE.
- Package: chữ thường.
- Android resource: snake_case.
- Đưa chuỗi giao diện vào strings.xml.
- Sử dụng resource cho màu, kích thước và style khi phù hợp.
- Đặt tên có ý nghĩa, tránh hardcode và magic number không cần thiết.
- Ưu tiên code rõ ràng, dễ hiểu, có thể bảo trì và kiểm thử.
- Chú thích những thuật toán hoặc quyết định kỹ thuật khó hiểu.
- Không thực hiện I/O hoặc gọi mạng gây chặn main thread.
- Xử lý lỗi và lifecycle khi triển khai tác vụ bất đồng bộ.
- Thêm dependency thông qua version catalog.
- Không tự ý thay đổi AGP, Gradle, SDK hoặc Java version.

## 5. Dữ liệu và bảo mật

- Phân biệt rõ dữ liệu thực tế, snapshot và mock data.
- Dữ liệu mô phỏng phải được ghi rõ là mô phỏng.
- Không trình bày giá vé, tình trạng ghế hoặc booking mô phỏng
  như dữ liệu giao dịch thực tế.
- Ghi nguồn và thời điểm thu thập đối với dữ liệu thực tế.
- Không bịa nguồn hoặc tuyên bố đã xác minh dữ liệu khi chưa kiểm tra.
- Không lưu mật khẩu, API key, token, khóa ký hoặc thông tin
  đăng nhập trong mã nguồn.
- local.properties chỉ thuộc máy phát triển.
- Không đưa thông tin nhạy cảm vào tài liệu, AI log hoặc Git.

## 6. Quy trình triển khai

- Phát triển theo từng phase trong docs/ROADMAP.md.
- Trước mỗi nhiệm vụ, kiểm tra trạng thái dự án và phạm vi thay đổi.
- Không triển khai trước các phase chưa được người dùng giao.
- Mỗi nhiệm vụ cần có mục tiêu và tiêu chí nghiệm thu rõ ràng.
- Sau khi triển khai, kiểm tra những chức năng liên quan.
- Không đánh dấu hoàn thành chỉ vì đã tạo file hoặc build thành công.
- Phân biệt trạng thái đã triển khai, đã kiểm thử và đã nghiệm thu.
- Cập nhật docs/PROGRESS.md dựa trên kết quả thực tế.
- Cập nhật docs/ROADMAP.md khi có thay đổi kế hoạch được xác nhận.

## 7. Build và testing

- Sử dụng Gradle Wrapper của dự án.
- Các lệnh kiểm tra trên Windows:

  ./gradlew.bat assembleDebug
  ./gradlew.bat testDebugUnitTest
  ./gradlew.bat lintDebug
  ./gradlew.bat connectedDebugAndroidTest

- Chọn lệnh kiểm tra phù hợp với phạm vi thay đổi.
- Chỉ chạy connectedDebugAndroidTest khi có thiết bị hoặc emulator.
- Thay đổi tài liệu cần được kiểm tra nội dung và liên kết.
- Ghi rõ lệnh đã chạy, kết quả, lỗi và hạn chế kiểm thử.
- Không bịa kết quả test hoặc coi test mẫu là bằng chứng
  chức năng nghiệp vụ đã hoạt động.
- Nếu không thể chạy test, ghi rõ nguyên nhân.
- Không sửa test chỉ nhằm làm cho kết quả kiểm thử thành công.

## 8. Git và commit

- Kiểm tra git status --short --branch trước khi sửa dự án.
- Không tự khởi tạo Git, đổi branch, commit hoặc push.
- Không tạo commit giả, thay đổi thời gian commit
  hoặc chỉnh sửa lịch sử Git nhằm đáp ứng rubric.
- Mỗi commit phải tương ứng với thay đổi thực tế, có mục đích rõ ràng.
- Sau mỗi nhiệm vụ, đề xuất commit message phù hợp.
- Sử dụng Conventional Commits khi phù hợp:
  - feat: chức năng mới.
  - fix: sửa lỗi.
  - docs: tài liệu.
  - test: kiểm thử.
  - refactor: cải tiến cấu trúc code.
  - chore: cấu hình và bảo trì dự án.
- Kiểm tra diff và danh sách file staged trước khi commit.
- Không commit cache, build output, file tạm hoặc bí mật.
- Người dùng review và trực tiếp thực hiện commit/push.

## 9. AI Usage Logging

- Tạo docs/AI_LOG.md nếu chưa tồn tại.
- Ghi nhận trung thực các lần sử dụng AI có ảnh hưởng đến
  yêu cầu, thiết kế, code, kiểm thử hoặc tài liệu.
- Cập nhật AI log sau mỗi nhiệm vụ hoặc tương tác đáng kể.
- Mỗi mục ghi nhận:
  - Công cụ và model, nếu xác định được.
  - Thời gian, nếu xác định được.
  - Prompt thực tế của người dùng.
  - Mục đích sử dụng AI.
  - Nội dung AI hỗ trợ.
  - File được tạo hoặc thay đổi.
  - Quyết định và công việc thực tế của người dùng.
  - Kết quả kiểm chứng và hạn chế.
- Phân biệt rõ công việc AI thực hiện và công việc người dùng thực hiện.
- Không tự suy diễn người dùng đã đọc, hiểu, review hoặc kiểm thử.
- Không bịa prompt, timestamp, model, kết quả hoặc tương tác.
- Bổ sung kết quả review và test của người dùng khi có xác nhận.
- Không xóa hoặc viết lại lịch sử AI log để che giấu việc sử dụng AI.

## 10. Bàn giao sau mỗi nhiệm vụ

Sau khi hoàn thành nhiệm vụ, báo cáo:

1. Nội dung đã thực hiện.
2. Danh sách file tạo mới hoặc thay đổi.
3. Các yêu cầu và tiêu chí nghiệm thu tương ứng.
4. Lệnh kiểm thử và kết quả thực tế.
5. Những hạn chế hoặc công việc chưa hoàn thành.
6. Các vấn đề cần người dùng xác nhận.
7. Trạng thái cập nhật PROGRESS.md và AI_LOG.md.
8. Git commit message đề xuất.

Dừng lại để người dùng review và xác nhận trước khi chuyển
sang nhiệm vụ hoặc phase tiếp theo.