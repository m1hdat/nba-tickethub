# Review Phase 2 và nguồn tham khảo

Ngày kiểm tra: **2026-11-25**. Các mục review bên dưới giữ kết quả của nhiệm vụ trước. **Cập nhật Phase 3A ở cuối file:** đã tích hợp metadata tên có nguồn CC0 và hình học tự mô phỏng, triển khai Canvas; không thêm dependency hoặc nhập sơ đồ/ảnh/tọa độ ghế bên ngoài. Đối chiếu [AGENTS](../AGENTS.md), [REQUIREMENTS](REQUIREMENTS.md), [DESIGN](DESIGN.md) và [PROGRESS](PROGRESS.md).

## 1. Kết quả review Phase 2

### Kết quả tốt đã kiểm chứng

- Model phân biệt hierarchy vật lý với `GameSeat` theo trận; collection/geometry bất biến; JSON có provenance MOCK. Reader/compiler/validator dùng chung cho profile nhỏ và lớn.
- Chạy lại `tools/test-foundation.ps1` với cache JUnit hiện có và SDK jar: **31 test đạt**, gồm 30 test mới và 1 test mẫu; JUnit báo 1.773 giây. Có ca load/validate 10k/30k/50k vị trí, không chỉ fixture 200.
- AndroidAssetSource compile với SDK jar đã sao chép cục bộ, chưa thực thi trên Android. Không có bằng chứng APK/Canvas/gesture/hiệu năng UI đã đạt.

### Phát hiện và việc cần xử lý

| Mức | Phát hiện | Bằng chứng / ảnh hưởng | Đề xuất, chưa thực hiện |
| --- | --- | --- | --- |
| Ưu tiên cao | Chưa xác minh build Android hoàn chỉnh | Log Phase 2 dừng tại compileDebugJavaWithJavac với AccessDeniedException ở SDK android.jar; trong lượt review chỉ đọc lại log, không chạy lại Gradle | Xác minh assembleDebug/testDebugUnitTest/lintDebug trong môi trường truy cập SDK bình thường trước nghiệm thu nền Android |
| P2 — lỗi validator đã tái hiện | Kiểm tra bốn đỉnh không đủ chứng minh cả quad nằm trong row dạng vành khuyên | [ArenaDataValidator.java](../app/src/main/java/com/example/nbatickethub/data/ArenaDataValidator.java), dòng 93–96 và vertexInside dòng 130. Probe tạo quad có bốn đỉnh ở ngoài bán kính trong 340 nhưng trung điểm một cạnh có bán kính 339.899961; ArenaDataset vẫn được chấp nhận | Thêm kiểm tra cả cạnh với biên trong/vùng góc; thêm regression test. Chưa sửa application code trong review |
| Cần kiểm tra thêm, chưa kết luận là lỗi hiện tại | Chi phí private grid validator có thể tăng mạnh khi kích thước ghế rất lệch | cellSize lấy kích thước lớn nhất của mọi ghế; cell lớn có thể tập trung nhiều candidate. Test hiện tại là các profile khá đều | Thêm dữ liệu lệch mật độ/kích thước và đo candidate/time/heap; chưa coi test 50k là benchmark worst-case |
| Cải thiện có điều kiện | Tự duy trì StrictJson tăng trách nhiệm kiểm thử parser | Test hiện tại đã có syntax/type/Unicode/budget; chưa có fuzz/property corpus rộng | Giữ parser hiện tại trong review; cân nhắc adapter JSON đã kiểm giấy phép ở mục 3 khi được giao nhiệm vụ riêng |

Probe chỉ là chương trình chẩn đoán trong `.gradle/phase2-review/`, dùng class Phase 2 đã compile; không thêm test/app source. Kết quả: `validatorAccepted=true innerRadius=340.000 edgeMidpointRadius=339.899961`. Đây là lỗi bảo đảm containment của validator với quad tự tạo, **không phải kết luận rằng ba mock manifest hiện tại có ghế sai vị trí**. Bộ 31 test xanh chưa bao phủ regression này.

Để tái hiện: lấy row đầu của fixture small, góc giữa section 22.5 độ; tạo quad trong hệ radial/tangent với bốn điểm (339.9, -12), (340.5, -12), (340.5, 12), (339.9, 12), xoay về world rồi cộng tâm arena. Thay geometry của seat đầu, giữ nguyên ID/index/inventory và dựng ArenaDataset mới. Cả bốn đỉnh vượt bán kính 340, nhưng trung điểm cạnh có radial coordinate 339.9 nằm trong lỗ; validator hiện vẫn chấp nhận.

## 2. Nguyên tắc lựa chọn và mức xác minh

- **ArenaView tự triển khai:** Android Custom View, Canvas/Paint/Matrix/inverse Matrix; tự tổ chức geometry, viewport, LOD/culling, hit-testing và selection theo DESIGN. Không seat-map SDK, chart/map renderer, WebView, ảnh sơ đồ tĩnh hoặc thư viện pan/zoom thay phần này.
- Có thể dùng thư viện cho lifecycle, sheet/list, accessibility và kiểm thử bên ngoài engine. JTS chỉ đề xuất làm oracle kiểm thử độc lập, không chuyển ArenaView hoặc đường touch/selection sang JTS.
- Cấu hình hiện tại: Java source/target 11; minSdk 24; compile/target 37; AGP 9.4.1; Gradle Wrapper 9.6.0. Material 1.14.0 đã khai báo trong catalog.
- **Đã kiểm tra nguồn/API/giấy phép** không đồng nghĩa **đã resolve/compile/run dependency trong repository**. Lượt này không thử dependency mới. Phiên bản, AAR minSdk, transitive dependencies, R8 và Gradle compatibility phải được kiểm tra lại trước khi pin.
- URL nhánh main/master có thể đổi. Khi thực sự dùng code/artifact: lưu version/tag/commit, link LICENSE/NOTICE đi kèm và phần đã dùng. Không lấy giấy phép code để suy ra giấy phép dataset, logo hoặc tên thương hiệu.

## 3. Thư viện có thể tận dụng

### Ưu tiên cho các phase kế tiếp

| ID / nguồn chính thức | Công dụng trong dự án | Giấy phép đã kiểm | Java / Views/XML và điều kiện |
| --- | --- | --- | --- |
| LIB-01 — [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate) | ViewModel/LiveData/saved state cho selection/filter/camera snapshot | [Apache-2.0, AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | Có API Java, không cần Compose. Nhánh 2.10 nâng minSdk lên 23, phù hợp minSdk 24; không tự nâng version. Snapshot selection lớn vẫn cần budget, không lưu geometry vào Bundle |
| LIB-02 — [RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview), [release notes](https://developer.android.com/jetpack/androidx/releases/recyclerview) | Danh sách selection trong sheet, tái dùng item View | [Apache-2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | Java Adapter/ViewHolder + layout XML. 1.4.0 yêu cầu compileSdk ít nhất 35, project khai báo 37. Đang maintenance mode; vẫn hợp quy định Views/XML. Không dùng RecyclerView để dựng sơ đồ ghế |
| LIB-03 — [Material BottomSheetBehavior](https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetBehavior) | Persistent sheet, filter chips và thành phần UI phụ trợ | [Apache-2.0](https://github.com/material-components/material-components-android/blob/master/LICENSE) | API Java và XML; Material đã có trong catalog, ưu tiên dùng lại. Chưa xác minh runtime theme/sheet; không dùng sheet che toàn Canvas |
| LIB-04 — [ExploreByTouchHelper](https://developer.android.com/reference/androidx/customview/widget/ExploreByTouchHelper), [CustomView releases](https://developer.android.com/jetpack/androidx/releases/customview) | Virtual accessibility nodes cho vùng/ghế đang nhìn thấy | [Apache-2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | Helper Java gắn vào View tự vẽ, không thay renderer. Không tạo sẵn hàng chục nghìn node; bounds phải dùng Matrix hiện hành. Cần pin/check AAR minSdk và TalkBack thực tế; package đang maintenance mode |
| LIB-05 — [FrameMetricsAggregator](https://developer.android.com/reference/androidx/core/app/FrameMetricsAggregator) | Theo dõi timing frame khi có ArenaView thật | [Apache-2.0](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) | API Java; hệ thống FrameMetrics có từ API 24, khớp minSdk. Không suy ra FPS từ thời gian unit test |

Các thư viện AndroidX/Material trên chỉ hỗ trợ state/UI/quan sát. Không cài thêm ở lượt này; cần kiểm tra dependency graph thực tế để không thêm trùng artifact đã có gián tiếp.

### Kiểm thử và công cụ tùy theo nhu cầu

| ID / nguồn chính thức | Đề xuất sử dụng | Giấy phép / tương thích đã kiểm | Giới hạn |
| --- | --- | --- | --- |
| LIB-06 — [Robolectric](https://github.com/robolectric/robolectric), [compatibility table](https://robolectric.org/compatibility_table/) | Test Android assets, Matrix và lifecycle bằng JUnit sau khi được duyệt | [MIT](https://github.com/robolectric/robolectric/blob/master/LICENSE); hỗ trợ test Java. Bảng hiện tại ghi 4.18 hỗ trợ API 24–37, test API 37 cần Java runtime tối thiểu 21; máy đang có JDK 25 | Không nhầm Java source 11 của app với JDK chạy test. Chưa thử Robolectric/JDK 25/AGP hiện tại; cột Gradle/AGP trong bảng là phiên bản dùng để build Robolectric, không tự suy ra dự án buộc nâng Gradle |
| LIB-07 — [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview), [FrameTimingMetric](https://developer.android.com/reference/androidx/benchmark/macro/FrameTimingMetric) | Đo pan/zoom journey và frame timing khi đã có UI; không phải engine vẽ | [Apache-2.0, AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt); metric có API Java, dùng được với app Views | Cần test module/cấu hình benchmark riêng và thiết bị; chỉ cân nhắc phase kiểm thử hiệu năng được giao sau. Chưa tạo module hoặc số đo |
| LIB-08 — [Moshi](https://github.com/square/moshi) | Ứng viên thay parser tự viết nếu lợi ích bảo trì được duyệt | [Apache-2.0](https://github.com/square/moshi/blob/master/LICENSE.txt); tài liệu có JsonAdapter/JsonReader cho Java và Android, không yêu cầu chuyển UI sang Kotlin | README nhánh hiện tại có cả ví dụ alpha 2.x, không copy version mù quáng. Chưa xác minh minSdk/transitive graph của artifact sẽ chọn. Ưu tiên adapter Java gọi constructor/validator; giữ strict schema, exact money và regression corpus, không chỉ reflective deserialize |
| LIB-09 — [JTS core](https://github.com/locationtech/jts/tree/master/modules/core), [build properties](https://github.com/locationtech/jts/blob/master/pom.xml) | Oracle JVM độc lập để so polygon overlap/envelope và kiểm index/hình học | [EPL-2.0 hoặc EDL-1.0](https://github.com/locationtech/jts/blob/master/LICENSES.md), có ghi chú third-party; root POM source/target 1.8 nên phù hợp Java 11 ở mức ngôn ngữ | Đề xuất test-only, không tự thêm runtime dependency/GeoTools/AWT. Android/R8 chưa kiểm chứng. Xấp xỉ cung tròn bằng polygon có sai số; oracle không tự giải quyết containment giải tích của vành khuyên |

Khi thay JSON parser phải kiểm lại constructor invariants, duplicate keys, field/type strictness, integer overflow, stream closing và budget. Khi dùng JTS làm oracle, test phải độc lập với thuật toán của app; không viết test chỉ lặp lại cùng công thức.

**Không ưu tiên Gson cho Android mới chỉ vì phổ biến:** [README chính thức](https://github.com/google/gson#android) cảnh báo về reflection và tối ưu release; [license Apache-2.0](https://github.com/google/gson/blob/main/LICENSE) đã kiểm. Không tự thay StrictJson bằng Gson trong review.

## 4. Mã nguồn mở và tài liệu cho ArenaView tự viết

| ID / nguồn | Phần có thể học | Giấy phép / tương thích | Cách áp dụng đúng phạm vi |
| --- | --- | --- | --- |
| CODE-01 — [AOSP ApiDemos TouchPaint.java](https://android.googlesource.com/platform/development/+/refs/heads/main/samples/ApiDemos/src/com/example/android/apis/graphics/TouchPaint.java) | Custom View, onDraw/Canvas/Paint và nhận MotionEvent | Header Apache-2.0; source Java. Blob kiểm tra: 5831ddc94824e382e23104b31ee383fbe85a7073 | Chỉ tham khảo cơ chế; không chép bitmap backing store/timer của app vẽ tay thành sơ đồ ghế. ArenaView vẫn tự triển khai theo DESIGN, không import cả sample app/manifest |
| CODE-02 — [Android custom-view optimization](https://developer.android.com/develop/ui/views/layout/custom-views/optimizing-view), [drag/scale](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale), [Matrix](https://developer.android.com/reference/android/graphics/Matrix) | Tái dùng object, giảm redraw/layout; gesture và inverse coordinates | Tài liệu/API Android có ví dụ Java; [content license](https://developer.android.com/license) phân biệt nội dung, code mẫu và ngoại lệ theo nguồn | Tự viết transform, clamp, LOD/culling và hit-test; tài liệu không phải benchmark 50k ghế |
| CODE-03 — [JTS STRtree source](https://github.com/locationtech/jts/blob/master/modules/core/src/main/java/org/locationtech/jts/index/strtree/STRtree.java) | Cách tổ chức envelope/index, tham khảo kiểm thử query | Cùng điều kiện JTS ở LIB-09, kiểm lại header/version khi dùng | So sánh thiết kế grid hiện có, không mặc định thay engine bằng STRtree. Chưa sao chép source |

Nếu thật sự sao chép/điều chỉnh mã nguồn sau này: ghi file/version/phần dùng, giữ copyright và LICENSE/NOTICE áp dụng, ghi sửa đổi theo license. Lượt này chỉ đọc và dẫn nguồn, không sao chép code bên thứ ba vào ứng dụng.

## 5. Dataset và nguồn metadata

| ID / nguồn | Có thể tận dụng | Giấy phép/điều kiện đã kiểm | Tương thích và giới hạn |
| --- | --- | --- | --- |
| DATA-01 — [Mock assets hiện tại](../app/src/main/assets/mock/arena-small.json), [compiler](../app/src/main/java/com/example/nbatickethub/geometry/ArenaGeometryCompiler.java) | Dataset offline nhiều tầng, section, hàng và từng vị trí; stress theo tham số | Nguồn nội bộ dự án; chưa thấy LICENSE cấp phép phân phối chung ở root, không tự gắn MIT/CC0 | Đọc trực tiếp bằng Java. Giữ provenance MOCK cho sơ đồ/capacity/giá/status; không dùng tên NBA thật làm bằng chứng dữ liệu mô phỏng là chính thức |
| DATA-02 — [Wikidata](https://www.wikidata.org/wiki/Wikidata:Licensing), ví dụ [Crypto.com Arena Q186080](https://www.wikidata.org/wiki/Q186080), [Lakers Q121783](https://www.wikidata.org/wiki/Q121783) | ID, tên, địa điểm, quan hệ đội/sân và metadata có dẫn chứng | Structured data CC0; quyền ảnh/logo gắn kèm phải kiểm riêng, không suy từ CC0 dữ liệu | Có thể chuyển snapshot JSON sang model Java khi được giao. Ghi entity/revision/URL/ngày; nếu lấy capacity phải giữ qualifier theo cấu hình sự kiện và thời điểm. Chưa tải/import snapshot, chưa xác minh giá trị capacity cụ thể |
| DATA-03 — [OpenStreetMap copyright/license](https://www.openstreetmap.org/copyright) | Tọa độ/footprint địa lý bên ngoài venue nếu sau này có màn thông tin sân | ODbL, cần attribution và xem điều kiện với database dẫn xuất trước phân phối | Có thể chuyển dữ liệu sang JSON đọc bằng Java. Không phải dữ liệu từng ghế/tầng/tồn kho, không cần MapView thay ArenaView. Chưa truy xuất feature cụ thể; ưu tiên thấp |
| DATA-04 — [BALLDONTLIE NBA docs](https://nba.balldontlie.io/), [Terms](https://www.balldontlie.io/terms.html) | Metadata đội và trận nếu phase sau cần dữ liệu thực | API/service theo Terms riêng, không coi là dataset có license MIT/CC0; Terms hiện ghi ngày 2026-10-20 và phân biệt quyền bên thứ ba | HTTP/JSON dùng được với Java, không phụ thuộc XML UI; cần API key/gói phù hợp, không thêm key vào app/source. Chưa gọi API hoặc đăng ký; không chọn làm nguồn seat geometry/inventory từ các endpoint đã xem |
| DATA-05 — [swar/nba_api](https://github.com/swar/nba_api), [static teams.py](https://github.com/swar/nba_api/blob/master/src/nba_api/stats/static/teams.py) | Tham khảo schema/ID đội hoặc công cụ chuẩn bị snapshot offline khi được giao | [MIT cho code client](https://github.com/swar/nba_api/blob/master/LICENSE), không cấp phép thay cho dữ liệu nhà cung cấp | Python 3.10+ theo README, không phải dependency Java/Android. Không đưa Python runtime vào app; chưa chạy client hoặc xuất dataset |

[Điều khoản NBA](https://www.nba.com/termsofuse): NBA Statistics có điều khoản sử dụng riêng; không coi đó là open data.

**Khoảng trống còn lại:** lượt tra cứu này chưa xác minh được dataset mở có đầy đủ tọa độ từng ghế đa tầng cùng tồn kho/giá vé NBA thực. Vì vậy tiếp tục dùng geometry/capacity/pricing/status mô phỏng; metadata thật nếu thêm chỉ là từng nhóm trường có nguồn. Chưa đề xuất Kaggle dump, ảnh seating chart hoặc repository không có license rõ ràng làm dữ liệu chính.

Trang seating maps của Crypto.com Arena không truy cập được qua công cụ trong lượt này: không kết luận nội dung/giấy phép, không dùng ảnh đó làm nền. Wikidata đã xem được trang entity, nhưng endpoint export JSON thử không truy cập được; không ghi rằng đã tải snapshot.

## 6. Thứ tự đề xuất, chưa tích hợp

1. Trước nghiệm thu Phase 2: xác minh Gradle build và xử lý regression containment đã tái hiện bằng một nhiệm vụ sửa riêng; chưa tự sửa trong review.
2. Phase tương tác được giao sau: tự viết ArenaView/Canvas/Matrix/index/LOD; dùng tài liệu Android/AOSP làm tham khảo. Lifecycle và accessibility helper hỗ trợ phần state/accessibility.
3. Phase sheet/filter: dùng Material đã có và RecyclerView cho danh sách phụ trợ.
4. Phase kiểm thử: cân nhắc Robolectric/JTS test oracle; đo trên thiết bị bằng FrameMetrics/Macrobenchmark khi có UI.
5. Metadata thật hoặc JSON library mới: chỉ thêm khi có nhu cầu và phạm vi rõ; ưu tiên Wikidata structured snapshot cho metadata, giữ mock seat map/inventory.

Chưa chọn hoặc cài phiên bản dependency mới. Chưa có third-party dataset/source được nhập vào app; mọi đề xuất cần kiểm tra artifact/NOTICE/transitive licenses đúng phiên bản khi thực sự áp dụng. Review của AI không phải người dùng đã review/hiểu/nghiệm thu.

## 7. Nguồn sử dụng/tham khảo cho Phase 3A — 2026-11-25

### Phân loại dữ liệu tích hợp

- **Identity tham chiếu:** tên ba sân/đội từ structured Wikidata (CC0-1.0), đối chiếu các trang venue/team dưới đây; lưu source URL/ngày đọc trong `assets/venues/catalog.json`. Không lấy sức chứa hoặc danh mục section thật từ Wikidata. `homeTeam.source` là nguồn riêng cho tên đội. Snapshot do AI nhập các trường identity đã đọc, không phải đồng bộ NBA API.
- **Nguồn chính thức tham khảo:** website venue/NBA cho bối cảnh seating/court. Không có giấy phép mở được xác minh cho tọa độ ghế hoặc ảnh trên các trang này; chỉ đọc/link tham khảo, không trích xuất bản đồ vào app. Không suy ra quyền tái phân phối từ việc trang công khai. Không sao chép mã của 3D venue vendor.
- **Tự mô phỏng:** tất cả section `SIM-*`, tầng/row count/seat count, ID, vị trí/hướng/kích thước tương đối, lối đi, màu, giá và availability. `provenance.originType=MOCK`, `sourceUrl=null` cho geometry tự tạo; catalog ghi notice riêng. Không gán URL venue như thể venue cung cấp geometry của project. Không tuyên bố CC0 của Wikidata bao phủ hình học tự tạo, ảnh hay logo.

| ID | Nguồn primary / loại | Quyền và mức xác minh | Phần dùng trong 3A |
| --- | --- | --- | --- |
| ARENA-01 | [MSG entity Q186125](https://www.wikidata.org/wiki/Q186125), [Knicks Q131364](https://www.wikidata.org/wiki/Q131364); [Wikidata licensing](https://www.wikidata.org/wiki/Wikidata:Licensing) | Structured data CC0; entity page đã đọc. Không phải chứng nhận từ NBA | Chỉ tên sân/đội và source URL trong catalog; không logo/capacity/seat data |
| ARENA-02 | [Chase Center Q15262098](https://www.wikidata.org/wiki/Q15262098), [Warriors Q157376](https://www.wikidata.org/wiki/Q157376) | Structured data CC0, đã đối chiếu venue link từ team; tránh nhầm trạm tàu Chase Center | Tên sân/đội, source URL; hình học ellipse do project tự chọn |
| ARENA-03 | [Crypto.com Arena Q186080](https://www.wikidata.org/wiki/Q186080), [Lakers Q121783](https://www.wikidata.org/wiki/Q121783) | Structured data CC0; tên venue/team có nguồn | Tên sân/đội; không import capacity hay section thật |
| ARENA-04 | [MSG Knicks seating chart](https://www.msg.com/madison-square-garden/seating/msg-knicks), [MSG Terms](https://www.msgentertainment.com/terms-of-use/) | Trang chính thức có nhóm tầng/bridge và cảnh báo tùy sự kiện; Terms đã đọc, không giấy phép dữ liệu mở | Tham khảo phân tầng tổng thể. Gallery hai dải/42 section của mock không phải tổ chức chính thức của MSG |
| ARENA-05 | [Chase venue maps](https://www.chasecenter.com/plan-your-visit/transportation/venue-maps/), [Warriors seating/prices](https://www.nba.com/warriors/tickets/prices) | Venue maps chỉ trả iframe qua công cụ; không truy cập đầy đủ engine 3D hoặc license geometry. Trang đội là nguồn tham khảo | Bối cảnh seating; không dùng bảng giá/tọa độ/section; tỷ lệ và court xoay là giả lập |
| ARENA-06 | [Crypto 3D venue map](https://www.cryptoarena.com/premiumhospitality/3d-digital-venue-map/), [Premier Seats](https://www.cryptoarena.com/premiumhospitality/premierseats), [AEG Terms](https://aegworldwide.com/terms-of-use) | Trang chính thức; engine nhúng không đọc được toàn bộ. Terms được truy theo footer chính thức; không cấp open seat-map dataset | Ý tưởng thêm tầng premium mô phỏng. Không tái tạo số/mã section hoặc suy ra hình học chính xác |
| COURT-01 | [NBA Rule 1](https://official.nba.com/rule-no-1-court-dimensions-equipment/), [Official NBA court diagram / 2025–26 Officials Guide](https://cms.nba.com/wp-content/uploads/sites/4/2025/10/2025-26-NBA-Officials-Guide.pdf) | Quy định/kích thước tham khảo, không copy hình minh họa hoặc PDF vào tài nguyên ứng dụng | Tự dựng đường sân bằng primitive Canvas, tỷ lệ court 94:50, góc ba điểm/keys/circles/rims được minh họa; không nhận là artwork court chính thức |
| CODE-3A | [Android Canvas](https://developer.android.com/reference/android/graphics/Canvas), [Custom drawing](https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing) | API framework đã có trong SDK; tham khảo cách tổ chức custom drawing, không thêm dependency/code vendor | Tự viết ArenaView/Path/Paint/cache; Matrix/inverse API tiếp tục dùng tự triển khai trong 3B/3C |

Đã tìm tham chiếu ảnh nội thất: [ảnh MSG từ trang sự kiện chính thức](https://www.msg.com/events-tickets/new-york-knicks-boston-celtics-madison-square-garden-november-2022/3B005D10AEE115B7) (asset `img.msg.com/seatmap/msg/knicks/NYKnicks_Section207_Row3_Seat10_L.jpg`) và [ảnh/tour Chase do NBA đăng](https://cdn-uat.nba.com/news/golden-state-warriors-new-chase-center-features-photos-tour). Kết quả tìm ảnh cho thấy nguồn; lượt mở trực tiếp hai JPEG báo cache miss, tải bằng shell bị chặn mạng. **Chưa kiểm tra trực quan đầy đủ các ảnh này**; không dùng mô tả tìm kiếm để suy ra tọa độ/sức chứa. Crypto có tour 3D trên trang venue nhưng engine nhúng không đọc được; ảnh tìm kiếm từ bên thứ ba không được chọn làm dữ liệu tích hợp. Bản preview vì vậy là hình học tự dựng có tham khảo bối cảnh, không phải bản digitize ảnh/seating chart. Cần người dùng đối chiếu trực quan nếu yêu cầu độ giống venue cao hơn.

Không tìm được nguồn mở đã xác minh chứa đầy đủ ba seat map + row/seat coordinates có quyền tái phân phối phù hợp trong lượt này. Không nhập mã nguồn/dataset mới từ thư viện đề xuất ở các mục trước. Superellipse projection, sinh hàng/ghế, cache Canvas và validator patch là code tự viết trong repo với AI hỗ trợ; không có đoạn mã ngoài được sao chép cần gắn license mới. Chưa chọn license phát hành của chính repository thay người dùng.

### Trạng thái review Phase 2 sau xử lý 3A

- Finding containment ở mục 1 là lịch sử đúng ở thời điểm review. Nay đã sửa `ArenaDataValidator.edgesInside`, thêm regression `rejectsQuadWithVerticesInsideButEdgeCrossingInnerHole`; test từ chối đúng trường hợp đã tái hiện. Không xóa kết quả review cũ.
- 31 test nền chạy lại trước sửa; sau bổ sung có 36 test (35 liên quan foundation/preview + 1 mẫu). Kết quả build cụ thể và giới hạn lint/device tại [PROGRESS](PROGRESS.md), không lấy kết quả JVM để nhận đã kiểm tra giao diện.
- Ba metadata khả dụng không có nghĩa đã xác minh các venue còn lại của 30 đội. Khả năng mở rộng hiện nằm ở schema/list/map/renderer không hard-code venue; cần thêm source snapshot và cấu hình cho từng sân sau này.


## 8. Tham khảo chẩn đoán treo/rendering — 2026-11-25

- [Android: Diagnose and fix ANRs](https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs): phân biệt vấn đề app/main thread với system server/tải thiết bị; cần stack và trace để quy nguyên nhân. Không dùng tên dialog làm bằng chứng lỗi trong JSON hoặc Canvas.
- [Android: Slow rendering](https://developer.android.com/topic/performance/issues/render): các Path lớn có thể gây chi phí xử lý/upload trên đường render. Căn cứ kiểm tra batch kích thước hữu hạn, không phải bằng chứng thiết bị này đã ANR tại drawPath. Bản cũ đã cache Path; không gán cho nó lỗi sửa Path mỗi frame khi không có việc đó.
- [Android: Capture and read bug reports](https://developer.android.com/studio/debug/bug-report): hướng dẫn lấy log/stack khi lỗi tái diễn. Trong lượt này ADB/Studio bị chặn truy cập nên chưa có bugreport/Logcat hợp lệ.

Chỉ tham khảo tài liệu primary/API Android sẵn có, không nhập code/dependency. HTML dùng Canvas 2D/Path2D trình duyệt và code tự viết; không CDN, ảnh seating map, WebView trong app hoặc renderer vendor. Trace/timing/batch/export đều do AI viết trong repo, không xác minh FPS bằng tài liệu tham khảo.


## 9. Tham khảo cho review thẩm mỹ Phase 3A — 2026-11-25

Người dùng đã xem HTML và đánh giá thẩm mỹ/độ chân thực chưa đạt. Nguồn dưới đây là tham khảo bố cục và giao diện, **không phải dữ liệu geometry được nhập vào dự án**.

| Nguồn công khai đã truy cập | Đọc được và giới hạn | Áp dụng vào bản mô phỏng |
| --- | --- | --- |
| [MSG Knicks Seat Map](https://www.msg.com/madison-square-garden/seating/msg-knicks) | Trang chính thức hiển thị nhóm Floor/100s/200s, Suites, 400s, 300s/Hyundai Bridge, Lounges; cảnh báo cấu hình thay đổi theo sự kiện. Engine không cung cấp tọa độ qua phần đọc trang | Phân cấp các dải khán đài, gallery mô phỏng riêng; không sao chép mã section thật |
| [Chase Maps](https://www.chasecenter.com/plan-your-visit/transportation/venue-maps/) và [Warriors Season Tickets](https://www.nba.com/warriors/tickets/season) | Nội dung tìm kiếm chỉ rõ portals/concourse và 3D seating view; lượt mở Maps chỉ trả iframe. Ảnh linked từ Warriors tải qua web bị lỗi; không nhận đã xem ảnh đó | Oval liên tục, phân biệt courtside/lower/upper; thông số là quyết định thiết kế tự chọn |
| [Crypto 3D Digital Venue Map](https://www.cryptoarena.com/premiumhospitality/3d-digital-venue-map/) và [Premier Seats](https://www.cryptoarena.com/premiumhospitality/premierseats) | Đọc được trang official, liên kết bản đồ 3D và mô tả premium levels; không trích xuất engine 3D | Dải premier-inspired màu riêng; không gọi 12 SIM-P section là tổ chức thực tế |
| SeatGeek seating chart: [MSG](https://seatgeek.com/venues/madison-square-garden/seating-chart), [Chase](https://seatgeek.com/venues/chase-center/seating-chart), [Crypto](https://seatgeek.com/venues/crypto-com-arena/seating-chart) | Đọc được trang và liên kết map cả ba. Web mở ảnh Chase thành công; ảnh MSG/Crypto trả cache miss. Trang có nhiều cấu hình sự kiện: hình mặc định không bảo đảm là basketball | Tham khảo hierarchy section → seat view, ranh giới gọn và nhãn; không digitize ảnh, không lấy giá/availability/capacity |
| [Ticketmaster: Interactive Seat Map](https://help.ticketmaster.com/hc/en-us/articles/9786899270545-What-is-the-interactive-seat-map-and-how-do-I-use-it) | Đọc hướng dẫn official: section, zoom tới ghế, blue/gray theo trạng thái/filter. Không sao chép UI hoặc triển khai chọn/checkout | Ưu tiên nhận biết section toàn cảnh và xem ghế khi phóng đại; trạng thái mô phỏng phân biệt với màu tầng |

Không phát hiện giấy phép tái phân phối đầy đủ seat coordinates trong những nguồn này. Không đưa ảnh, logo, dữ liệu seating hoặc code bên ngoài vào app. Các exponent, radius, angle cuts, palette, seat count và inventory là **MOCK do dự án tự tạo**. Không tuyên bố mức giống chính xác với công trình thật khi chưa đối chiếu được toàn bộ chart. File QA PNG chỉ là ảnh kiểm tra geometry Java do AI render bằng Pillow, không phải browser screenshot, nguồn chính thức hoặc tài nguyên dùng thay Canvas Android.


## 10. Ảnh tham khảo court/stands cho theme 3A.1 — 2026-11-25

Các trang ảnh dưới đây được mở để tham khảo; ngày ảnh khác ngày truy cập. Không dùng chúng làm chứng cứ về cấu hình hiện tại hoặc lấy mẫu để khẳng định mã màu ghế chính thức. Không tải/tích hợp ảnh hay logo vào Android/HTML.

| Demo | Nguồn ảnh và thời điểm trên trang | Quyền/giới hạn và phần sử dụng |
| --- | --- | --- |
| Knicks / MSG | [Madison Square Garden court.jpg](https://commons.wikimedia.org/wiki/File:Madison_Square_Garden_court.jpg), Ganley894, court năm 2005; [gallery Knicks/Magic của đội](https://www.nba.com/knicks/gallery/gallery-knicks-and-magic-madison-square-garden) | Commons ghi public-domain release của tác giả; trang và link thumbnail đọc/mở được. Gallery NBA không trả nội dung ảnh đọc được. Ảnh 2005 **không xác minh court/ghế năm 2026**; chỉ tham khảo bối cảnh sân/khán đài, tự chọn demo gỗ/xanh/cam |
| Warriors / Chase | [San Francisco - Chase Center 001.jpg](https://commons.wikimedia.org/wiki/File:San_Francisco_-_Chase_Center_001.jpg), Américo Toledano (Hameryko), 27-12-2019; [gallery Clippers tại Chase của NBA](https://www.nba.com/clippers/photos/gallery-la-clippers-vs-golden-state-warriors-11-1-26) | Commons ghi CC BY-SA 4.0; trang/thông tin và thumbnail mở được. Gallery official có link ảnh nhưng asset CDN trả Internal Error. Không đo màu ghế từ ánh sáng/ảnh; maple/xanh/vàng và tiers tự thiết kế |
| Lakers / Crypto | [Crypto.com Arena interior - Lakers 2022.jpg](https://commons.wikimedia.org/wiki/File:Crypto.com_Arena_interior_-_Lakers_2022.jpg), Troutfarm27, trước trận Kings 11-11-2022 | Commons ghi CC BY-SA 4.0; mở trang và link thumbnail. Không coi ảnh 2022 là current layout; gỗ mật ong/tím/vàng là palette demo, không tuyên bố hex là official |

Lượt mở các thumbnail qua URL đầy đủ được web trả image reference; một số URL rút bỏ query khi thử lại không truy cập được. Không ghi đã đo/kiểm chứng chính xác mọi pixel hoặc màu ghế. Những giới hạn này là lý do catalog ghi **MOCK** cho toàn theme và notice rõ ràng về palette ghế/tầng tự thiết kế.

Phần **mô phỏng**: toàn bộ mã hex, sắc gỗ và dải ván, màu/ghép màu theo tier, màu nền ghế, vị trí/kích thước chữ tên đội, center mark bằng chữ, đường bao/section/seat count/inventory của các bản trước. Chỉ identity tên sân/đội và hồ sơ ảnh có nguồn; không gọi palette là màu ghế thật. Dùng tên đội bằng font hệ thống, không sử dụng logo chính thức hoặc tuyên bố đã có quyền logo.

Ảnh chỉ là tham chiếu không tái phân phối, nên không áp dụng license ảnh lên code tự viết hoặc tự tuyên bố license repo. Các ảnh `preview/qa-*.png` do tool Pillow vẽ từ snapshot Java và theme của dự án; không phải ảnh venue, browser screenshot, hay tài nguyên thay Canvas Android. Browser host hiện không khả dụng; không suy ra đã xem HTML trực tiếp từ ảnh QA.


## 11. Tham chiếu 30 court concepts và phạm vi quyền — 2026-11-25

- Tham chiếu do người dùng chỉ định: [NBA Court Concepts (All 30 Teams), Behance](https://www.behance.net/gallery/69718075/NBA-Court-Concepts-%28All-30-Teams%29). Mở trang trực tiếp bằng web trả **HTTP 403 Forbidden**. Không đọc được đầy đủ nội dung, tác giả hoặc điều kiện dùng artwork của dự án trong lượt này; không tự đặt tên tác giả/license và không ghi đã xem toàn bộ 30 ảnh gốc.
- Image search theo đúng URL/tên dự án trả hai mục thuộc project 69718075: [ảnh concept Nuggets](https://mir-s3-cdn-cf.behance.net/project_modules/fs/19642069718075.5b8b19db311ad.png) và [ảnh concept Lakers](https://mir-s3-cdn-cf.behance.net/project_modules/max_1200/7e21f669718075.5b8b19db2f06f.png). Thông tin chỉ mục gợi ý mảng mountain motif/court-wide graphics và phân biệt wood/paint/border/lettering. Lượt mở file ảnh trực tiếp tiếp tục trả **Cache miss**. Đây là tham khảo giới hạn từ chỉ mục, không phải quan sát/đo/tracing đầy đủ pixel gốc.
- Danh sách identity 30 đội được đối chiếu với nguồn NBA: [NBA Cares Teams](https://cares.nba.com/teams/), [NBA All Teams](https://www.nba.com/teams). Chỉ dùng tên/định danh để author concept; không lấy logo từ trang đội rồi coi là đã có quyền tái sử dụng. teamId của dự án là khóa nội bộ, không tuyên bố là schema ID chính thức.
- Ảnh/seating chart của **ba arena hiện có** tiếp tục theo mục 7/9/10 (MSG/Chase/Crypto); không có nguồn seat geometry/ghế của 27 arena khác trong dataset mới. Mọi bố cục và màu ghế chưa xác minh tiếp tục ghi MOCK, palette được chuyển sang arenaId và không thay theo đội.

Áp dụng vào thiết kế riêng: tách chất liệu gỗ, mảng đồ họa lớn, key edge/border và lettering thành layer dữ liệu, giữ vạch thi đấu phía trên. 30 bộ polygon/polyline/ellipse được tự author trong `tools/court_designs.py`, gồm các motif hình học phổ quát như ván parquet, cầu dây, núi, cây, tia, dải lửa, skyline; **không tải/copy ảnh court, logo, texture hoặc trace tọa độ của tác giả Behance**. Không gọi đây là reproductions của dự án Behance hoặc court NBA thật. Chưa xác minh visual fidelity đầy đủ với tham chiếu bị chặn; giới hạn này cần được cân nhắc khi review.

Logo: chưa xác minh được bộ logo chính thức đủ điều kiện tái phân phối; hiện **0 asset logo / 30 fallback mã đội**. Khung asset local chỉ chấp nhận PNG có source/license được khai báo, fit giữ tỷ lệ và draw dưới vạch; bản hiện tại không dùng đường tải từ nguồn không rõ quyền. Việc sau này thêm asset cần xác minh điều kiện thật, không chỉ điền chuỗi license. Không nhập font chữ thương hiệu; baseline/fallback dùng font hệ thống.

Các thông số màu, grain joinery, diện tích/độ lớn mảng trang trí, key accents, vị trí chữ, kích thước center marks và palette ghế đều là quyết định mô phỏng của dự án. `preview/qa-courts30*.png` là QA Pillow từ dữ liệu đã qua Java parser, không artwork venue hoặc browser screenshot và không được đưa vào Android renderer. Không có code/library/ảnh ngoài được nhập trong lượt này.

## 12. Khán đài 30 sân nhà và palette theo đội — 2026-11-25

**Hợp đồng hiện hành:** hình học theo arenaId; màu ghế/section/tầng theo theme teamId. Phần này thay thế quy tắc palette theo arenaId của mục 11; các mục trước giữ làm lịch sử.

### Nguồn đã kiểm tra và giới hạn

- [NBA — All 30 arenas by team](https://www.nba.com/news/all-30-nba-arenas-by-team), đọc được nội dung qua [bản CDN của NBA](https://cdn-uat.nba.com/news/all-30-nba-arenas-by-team): đối chiếu tên và sân nhà. Không dùng số sức chứa trong bài làm số ghế của dự án. Đây không phải nguồn tọa độ ghế, section, lối đi hoặc màu ghế thực tế.
- [Intuit Dome seating charts](https://intuitdome.com/events/seating-charts), có [Clippers basketball chart](https://publish-p111705-e1087822.adobeaemcloud.com/content/dam/intuit-dome/en/web/content/seating-charts/1920x1080_GAMES_V2.jpg): mở trang và liên kết ảnh làm tham chiếu; không digitize section/ghế. Cấu hình có end terrace tự thiết kế, không nhận là bản sao hay số hàng/ghế chính thức của The Wall.
- [Xfinity Mobile Arena seating](https://www.xfinitymobilearena.com/seating), có [76ers chart](https://www.xfinitymobilearena.com/assets/img/76ERS-SEATING-CHART-PRIMEPOINT-UPDATE-2568X1444-NO-BG-d02f0a94c5.png): đối chiếu tên hiện tại và loại chart. Bài directory NBA còn ghi Wells Fargo Center ở mục Philadelphia; bản demo dùng tên trên website venue. `identitySource` của xfinity trỏ trực tiếp nguồn venue này.
- [TD Garden seating](https://www.tdgarden.com/venue-info/seating): đã thử mở nhưng công cụ không lấy được nội dung chart; không ghi đã xác minh hình học Boston. Các nguồn ảnh/chart MSG, Chase, Crypto của các mục 7/9/10 vẫn chỉ là tham khảo định tính, không dữ liệu ghế chính thức.
- Các sân có mã **I** trong bảng chỉ đối chiếu identity/home mapping từ directory. Chưa kiểm chứng seating chart hoặc ảnh nội thất chi tiết của từng sân trong lượt này; author một bố cục MOCK riêng. **R** là có đường dẫn chart/ảnh tham khảo như nêu trên hoặc ở các mục trước, vẫn không có tọa độ/hàng/ghế được xác minh. **I/T** là trường hợp TD Garden đã thử mở chart nhưng chưa đọc được.

### Phạm vi dữ liệu mô phỏng — đủ 30 đội

**30/30 cấu hình khán đài là MOCK; 0 sơ đồ ghế chính thức được số hóa.** Mọi độ bo/đường bao, độ sâu tầng, số section, lối đi, hàng ghế, tọa độ, sức chứa dưới đây đều do dự án author. Palette lấy hai màu đang có của theme đội; đây là **màu ghế thiết kế theo theme đội**, không tuyên bố là màu ghế thực tế hoặc mã màu thương hiệu được chứng nhận.

| teamId | Sân nhà / arenaId nội bộ | Tham khảo | Section / hàng / ghế MOCK | Primary / secondary từ theme |
| --- | --- | --- | --- | --- |
| `nyk` | Madison Square Garden / `msg` | R | 42 / 606 / 16,908 | `#1656A5` / `#CB763B` |
| `gsw` | Chase Center / `chase` | R | 34 / 576 / 17,584 | `#234A88` / `#E2BB48` |
| `lal` | Crypto.com Arena / `crypto` | R | 50 / 656 / 19,056 | `#552583` / `#D9AF40` |
| `atl` | State Farm Arena / `state_farm` | I | 52 / 674 / 18,576 | `#9B2437` / `#E6B73A` |
| `bos` | TD Garden / `td_garden` | I/T | 54 / 744 / 19,824 | `#176442` / `#C7AD76` |
| `bkn` | Barclays Center / `barclays` | I | 50 / 652 / 17,008 | `#263443` / `#8D9EAA` |
| `cha` | Spectrum Center / `spectrum` | I | 56 / 844 / 21,048 | `#246C77` / `#644B86` |
| `chi` | United Center / `united` | I | 58 / 912 / 26,148 | `#9D233B` / `#252A34` |
| `cle` | Rocket Arena / `rocket` | I | 52 / 766 / 20,982 | `#72283E` / `#C6A861` |
| `dal` | American Airlines Center / `american_airlines` | I | 54 / 824 / 22,144 | `#225A8D` / `#94A4B1` |
| `den` | Ball Arena / `ball` | I | 56 / 840 / 21,624 | `#173A5D` / `#D3AC48` |
| `det` | Little Caesars Arena / `little_caesars` | I | 58 / 872 / 21,684 | `#274D91` / `#B72F42` |
| `hou` | Toyota Center / `toyota` | I | 52 / 720 / 21,174 | `#A72D40` / `#3D4859` |
| `ind` | Gainbridge Fieldhouse / `gainbridge` | I | 54 / 804 / 20,784 | `#234365` / `#DBB94B` |
| `lac` | Intuit Dome / `intuit` | R | 61 / 835 / 21,728 | `#254C7A` / `#B64246` |
| `mem` | FedExForum / `fedex` | I | 52 / 728 / 19,390 | `#284B74` / `#96AED0` |
| `mia` | Kaseya Center / `kaseya` | I | 56 / 848 / 21,968 | `#842B3D` / `#D48442` |
| `mil` | Fiserv Forum / `fiserv` | I | 52 / 792 / 19,722 | `#1F5949` / `#B6AA80` |
| `min` | Target Center / `target` | I | 56 / 824 / 21,176 | `#243C58` / `#699584` |
| `nop` | Smoothie King Center / `smoothie_king` | I | 50 / 656 / 18,192 | `#263D5A` / `#BAA06B` |
| `okc` | Paycom Center / `paycom` | I | 54 / 760 / 20,952 | `#236196` / `#CD6C42` |
| `orl` | Kia Center / `kia` | I | 56 / 880 / 23,504 | `#225B91` / `#8EABBF` |
| `phi` | Xfinity Mobile Arena / `xfinity` | R | 60 / 886 / 22,906 | `#245695` / `#B94351` |
| `phx` | Mortgage Matchup Center / `mortgage_matchup` | I | 52 / 702 / 20,034 | `#573478` / `#CA793E` |
| `por` | Moda Center / `moda` | I | 56 / 860 / 22,124 | `#A5333B` / `#343D46` |
| `sac` | Golden 1 Center / `golden1` | I | 52 / 754 / 19,600 | `#59447D` / `#8D9CA7` |
| `sas` | Frost Bank Center / `frost_bank` | I | 56 / 832 / 22,112 | `#34414E` / `#91A0AA` |
| `tor` | Scotiabank Arena / `scotiabank` | I | 58 / 912 / 23,700 | `#A12F42` / `#353E47` |
| `uta` | Delta Center / `delta` | I | 54 / 852 / 23,016 | `#343A69` / `#A78155` |
| `was` | Capital One Arena / `capital_one` | I | 56 / 808 / 22,236 | `#2B426A` / `#B5424B` |

Snapshot này có 30 đội → 30 arenaId khác nhau; Clippers dùng Intuit, Lakers dùng Crypto. Hợp đồng `homeArenas` cho phép nhiều teamId trỏ cùng arenaId; ca dùng chung được kiểm bằng fixture giả lập NYK/GSW, không phải thông tin NBA thật. Không nhân bản hình học khi chỉ đổi đội/palette.

Không tải ảnh seating chart/court/logo vào app. 0 asset logo được cấu hình; giữ 30 mã đội giữa sân và danh sách thiếu asset trong HTML. Tất cả 30 tên/court/logo-slot hiện có được giữ nguyên so với baseline trước thay đổi; ba mock manifest cũ và hash tọa độ ghế cũng giữ nguyên. Ảnh `qa-homearenas30.png` là QA Pillow từ snapshot Java, không ảnh chụp browser/Android hoặc ảnh dùng thay Canvas.
