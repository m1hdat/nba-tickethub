# Tiến độ thực tế

Cập nhật: 2026-11-24 (Asia/Saigon).

## Tóm tắt

Phase 2 đã được người dùng giao: đã triển khai model, assets JSON, reader/repository, geometry compiler, validator và unit test; kiểm tra JVM đạt. **Android Gradle build chưa thành công do lỗi truy cập SDK jar trong môi trường hiện tại; chưa có APK/runtime được xác minh. Dừng để người dùng review, chưa sang Phase 3.** Các mục Phase 0–1 bên dưới giữ làm lịch sử; kết quả mới nằm ở mục Phase 2 cuối file.

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
