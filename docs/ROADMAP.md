# Kế hoạch phát triển

Cập nhật: 2026-11-25. Người dùng giao **Phase 3C Android**, yêu cầu kiểm build 3B trước và test chung trên Samsung sau bàn giao. Hit-testing theo inverse Matrix, chỉ mục quad, highlight một ghế và thông tin khu/hàng/số ghế đã triển khai. Giữ nguyên 30 thiết kế đội/sân và 626.904 ghế mô phỏng. Chờ kiểm runtime Samsung/review 3B+3C; chưa làm selection nhiều ghế, Bottom Sheet hoặc đặt vé. Hợp đồng dữ liệu ở [DESIGN §14](DESIGN.md#14-phase-3a1--30-sân-nhà-hình-học-arena-và-palette-đội), bằng chứng tại [PROGRESS](PROGRESS.md).

200 ghế chỉ là fixture nhỏ phân bố trên toàn arena thu nhỏ, không phải một khu duy nhất hay giới hạn thiết kế cuối. Profile 200/1k/10k/30k/50k dùng chung schema/pipeline/index; 30k/50k là tham số mock/benchmark, không phải sức chứa NBA thật hoặc giới hạn schema. Phải làm rõ cách nộp full-arena so với khoảng 150–250 đối tượng trong rubric; chưa có xác nhận giảng viên.

## Các phase và điều kiện hoàn tất

| Phase | Phạm vi | Điều kiện hoàn tất | Trạng thái thực tế |
| --- | --- | --- | --- |
| 0. Khảo sát | Cấu trúc Android, Git, ignore và bộ tài liệu | Có báo cáo hiện trạng | Đã khảo sát; chưa ghi nhận người dùng review |
| 1. Phân tích và thiết kế hệ thống | Thiết kế toàn arena nhiều tầng, hierarchy, culling/LOD/index, UX/state và dữ liệu có provenance; đối chiếu AGENTS/rubric, AI log | Có thiết kế sửa đổi và phạm vi nhiệm vụ cụ thể trước triển khai | Đã có thiết kế; người dùng đã giao Phase 2 trên thiết kế hiện tại |
| 2. Foundation và data model | Model/manifest JSON/asset reader, compiler tọa độ world và validator; test parsing/quan hệ/hình học; chạy build/test phù hợp | Dữ liệu nhỏ/lớn cùng schema, không trần 200; test đạt và Android build được xác minh trước khi coi hoàn tất toàn phase | Đã triển khai; 31 test nền đạt. Trong 3A đã sửa containment, tổng 36 test đạt qua Gradle và APK build được ở mirror; lint/runtime còn cần xác minh |
| 3A. Dữ liệu và preview Canvas | Catalog mở rộng nhiều sân; ba layout riêng MSG/Chase/Crypto, ghế mô phỏng đủ quy mô, Canvas court/stands, launcher XML | Build/test dữ liệu; kiểm tra giao diện dọc/ngang; nguồn/license và thông báo mô phỏng rõ | Đã triển khai; chờ người dùng kiểm tra trực quan, lint/runtime còn hạn chế môi trường |
| 3A.1. Theme đội chủ nhà | ArenaGeometry/ArenaTheme độc lập; 30 court concepts + 30 home arena geometries, palette ghế theo teamId; Android Canvas và HTML chung dữ liệu | Đổi theme giữ tọa độ/Path; kiểm data/contrast/legend; kiểm 30 mapping đội/sân, palette đổi khi dùng chung sân; người dùng review 30 court/khán đài | Đã triển khai; unit/export/build đạt ở mirror; chờ review trực quan, chưa sang 3B |
| 3B. Camera và hiệu năng | Matrix fit, pan/pinch theo focal point, giới hạn pan/zoom, Reset; LOD/culling/cache batches phù hợp | Test transform/anchor/clamp; kiểm tra gesture thực, đo chi phí rendering | Đã triển khai; build/unit/lint đạt tại root; test Android đã biên dịch, chưa chạy; gesture/chi phí rendering Samsung và nghiệm thu còn chờ |
| 3C. Hit-testing | Inverse camera Matrix, spatial candidates/quad test, phân biệt tap/drag/pinch, highlight/debug | Test ghế/trống/biên sau pan/zoom, chuyển sân, dữ liệu lớn | Đã triển khai; 54 JVM tests đạt gồm 6 ca hit-test; build/lint đạt; test Android đã compile, chờ chạy Samsung và review chung 3B+3C trước Phase 4 |
| 4. Selection, filter và sheet | ViewModel, tập chọn ổn định qua LOD; filter tầng/section, aggregate theo khu, tổng mock, focus/xóa/cuộn; đồng bộ phiên bản worker | REQ-03, 08, 10–12; ID/tổng không lệch khi lọc/LOD/reset; focus đúng tầng/hàng/ghế, không publish index/filter cũ | Chưa thực hiện |
| 5. Lifecycle, scale và chất lượng P0 | Rotation/restore, bộ nhớ saved state/snapshot phiên, dọc/ngang/gesture; đo 1k/10k/30k/50k, frame/query/load/heap/GC, end-to-end | Có số đo trên thiết bị tham chiếu; so index với oracle; không dùng kết quả 200 ghế để kết luận quy mô lớn; sửa lỗi P0 | Chưa thực hiện |
| 6. Mở rộng có điều kiện | Chỉ các P1 được duyệt: nhiều trận, search, xác nhận mua/Order mock, lịch sử | P0 đã ổn, có scope bổ sung và thời gian; kiểm tra lại luồng bị ảnh hưởng | Chưa duyệt, có thể bỏ qua |
| 7. Bàn giao | README, AI log đầy đủ; video 1–5 phút; checklist rubric, artifact và link truy cập | REQ-14–18 có bằng chứng; sinh viên tự review và hiểu mã | Chưa thực hiện |

Không đánh dấu phase triển khai hoàn thành từ việc có tài liệu hoặc test template. Trạng thái/bằng chứng cụ thể ghi tại [PROGRESS.md](PROGRESS.md); tác động của AI ghi tại [AI_LOG.md](AI_LOG.md).

Phạm vi Phase 2 lịch sử là geometry và tính hợp lệ dữ liệu; nhiệm vụ mới cho phép Canvas tĩnh trong 3A theo mục 11 DESIGN. Camera/LOD/culling/index tương tác/hit-testing/selection vẫn chờ các mốc được giao; private grid/SAT trong validator chỉ phát hiện geometry chồng lấn, không phải hit-testing touch. Không có Booking, Smart Seat Finder hoặc My Tickets trong Phase 2. Việc chia việc này không thay kiến trúc toàn arena đã thiết kế.

## Phân bổ một tuần — đề xuất tương đối cần đánh giá lại

Đề cho 01 tuần, chưa biết ngày bắt đầu/hạn nộp thực tế. Toàn arena quy mô lớn tăng khối lượng geometry/index/LOD/benchmark; chưa có cơ sở cam kết xong trong một tuần. Lịch là mốc ưu tiên sơ bộ, cần chốt lại thời gian theo kết quả kiểm chứng; không giảm sản phẩm cuối xuống 200 để khớp lịch.

| Ngày tương đối | Mục tiêu ưu tiên |
| --- | --- |
| 1 | Review thiết kế mở rộng, chốt môi trường/profile, schema và worker/index baseline |
| 2 | Canvas toàn arena, LOD/culling, transform và test hình học/index với fixture nhỏ |
| 3 | Pan/pinch/zoom sâu, query và hit-test sau transform; tăng lên 1k/10k để phát hiện vấn đề sớm |
| 4 | Selection/filter tầng-section/sheet đồng bộ, aggregate/version |
| 5 | Rotation/restore, test 30k và stress 50k, bộ nhớ và pointer edge cases |
| 6 | Đo hiệu năng/end-to-end trên thiết bị, sửa vấn đề scale và UX; P1 chỉ sau khi P0 đã đủ |
| 7 | README/AI log, video, rubric checklist, chuẩn bị nộp |

## Phạm vi khóa trước triển khai

- Phạm vi toàn arena và nhiệm vụ foundation Phase 2 đã được người dùng giao; không hỏi lại quyền thực hiện phần này. Cấu hình mock trong assets là dữ liệu minh họa. Các lựa chọn về rendering, scope Purchasing, rubric, thiết bị/ngân sách hiệu năng và lưu phiên lớn cần review trước nhiệm vụ tương ứng.
- Culling, LOD, index và kiểm tra quy mô lớn thuộc P0 của phạm vi mới; không hoãn chúng sang phần mở rộng. Fixture 200 chỉ là bước kiểm chứng đầu tiên.
- Ưu tiên P0 đầy đủ rubric. Không tích hợp API/DB/thanh toán/tài khoản theo suy đoán.
- P1 có thể bị loại để dành thời gian kiểm chứng và hồ sơ; không bỏ P0 để đổi lấy chức năng mở rộng.
- Cuối Phase 1 đã quan sát `.git`, branch `main` chưa có commit, origin là `https://github.com/m1hdat/nba-tickethub.git`. AI không khởi tạo Git; cần xác nhận đây là remote dùng cho bài. Không tự commit/push từ roadmap này.

## Kế hoạch lịch sử Git khi được yêu cầu

Đề ghi tối thiểu 05–07 commit có ý nghĩa, phân bổ thời gian; mục tiêu đề xuất là ít nhất 7 commit gắn phần việc thật. Không tạo hàng loạt commit giả để bù lịch sử và không ghi rằng các commit dưới đây đã tồn tại:

1. `docs: define rubric requirements and phase 1 system design`
2. `chore: verify Android baseline and add scalable mock arena hierarchy`
3. `feat: render full arena with level of detail and viewport culling`
4. `feat: add bounded camera gestures and indexed seat hit testing`
5. `feat: synchronize cross-level selection filters and bottom sheet`
6. `fix: preserve arena state across rotation and handle gesture edges`
7. `test: verify arena interactions lifecycle and large dataset performance`
8. `docs: complete run guide demo video and AI usage record`

Mỗi message chỉ dùng khi thay đổi tương ứng đã thực hiện và được cho phép commit; số lượng thực tế phụ thuộc các phần việc có nghĩa. README phải có mục nhật ký/tóm tắt AI và link AI_LOG, không chỉ để AI_LOG riêng rồi coi đã đạt yêu cầu README của đề.

**Điểm dừng hiện tại:** bàn giao Phase 3C để người dùng test chung 3B+3C trên Samsung. Không suy diễn người dùng đã kiểm runtime hoặc nghiệm thu 3B/3C. Build trước sửa 3C đạt; unit/index/build/lint 3C đạt, instrumentation chưa chạy vì không có thiết bị ADB. Không chuyển Phase 4 hoặc commit/push. Các đoạn mô tả “chờ 3B/3C” phía trên lưu kế hoạch/lịch sử trước lần giao việc này.
