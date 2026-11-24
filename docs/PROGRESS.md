# Tiến độ thực tế

Cập nhật: 2026-11-24 (Asia/Saigon).

## Tóm tắt

Đã khảo sát nền dự án ở Phase 0. Trong Phase 1 đã đọc đề PDF được bổ sung, chuẩn hóa yêu cầu, soạn DESIGN và cập nhật roadmap/AI log. **Đang chờ người dùng review và thống nhất thiết kế; chưa triển khai chức năng.** Các bảng khảo sát cấu hình bên dưới là bằng chứng Phase 0, không phải kết quả build mới.

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

## Công việc và bằng chứng

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
