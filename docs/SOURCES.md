# Review Phase 2 và nguồn tham khảo

Ngày kiểm tra: **2026-11-25**. Chỉ review/tra cứu, chưa tích hợp dependency, tải dataset vào app, sửa code hoặc triển khai Phase 3. Đối chiếu [AGENTS](../AGENTS.md), [REQUIREMENTS](REQUIREMENTS.md), [DESIGN](DESIGN.md) và [PROGRESS](PROGRESS.md).

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
