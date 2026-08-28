# IDEAS — ý tưởng thô, chưa chắc làm

## Core tính toán
- "Step-by-step solution" hiển thị các bước rút gọn biểu thức.
- Voice input cho biểu thức toán học ("what's 15 percent of 340").
- Widget hiển thị lịch sử tính toán gần nhất trên home screen.
- Graphing mode đơn giản vẽ hàm số cơ bản (chạm lướt xem toạ độ) — rủi ro effort/scope lớn so với app calculator hiện tại, cân nhắc kỹ. Thu hút thêm tệp học sinh/sinh viên (nguồn: agy, độc lập).
- OCR Scanner (ML Kit) — quét hoá đơn giấy/phương trình viết tay thành input trực tiếp, không cần gõ tay (nguồn: agy, độc lập). Effort lớn, rủi ro độ chính xác OCR toán học.
- Undo/redo theo thao tác phím, hữu ích khi sửa biểu thức dài sau khi formatter tự thêm dấu phân nhóm (nguồn: codex, độc lập).
- Bộ template công thức cá nhân (thuế, chiết khấu, BMI, lãi suất...) với tham số thay thế (nguồn: codex, độc lập).
- Biểu thức chia sẻ qua QR/deep-link (kèm locale + DEG/RAD) để người nhận mở OpenCalc và chỉnh sửa tiếp ngay (nguồn: codex, độc lập).
- Cảnh báo nhẹ khi `addParenthesis` phải tự đóng ngoặc thiếu — hiện âm thầm tự sửa, có thể note cho user biết đã tự sửa gì.
- Export lịch sử tính toán ra CSV/chia sẻ (đã nâng cấp thành New Feature N-DATA-1).

## UI/UX
- Shake-to-clear (`SensorManager` accelerometer trigger cho `clearButton`).
- Long-press digit button để insert hằng số/kết quả thường dùng (vd long-press "7" → history entry bắt đầu bằng 7).
- Landscape "graphing strip" hiển thị mini sparkline của các kết quả gần đây.
- Currency/unit-converter tab tái dùng mechanics `slidingLayout` hiện có.
- Lock-screen quick calc qua `NotificationCompat` custom view.
- Tap-and-hold "=" cho popup "show steps" breakdown biểu thức.
- Haptic pattern riêng theo loại nút (operator vs digit) — mở rộng `keyVibration()` hiện có.

## Ad & VIP / Monetization
- Bundle "VIP theo mùa" (giảm giá Tết/Black Friday) dùng key có ngày hết hạn campaign riêng.
- Streak/gamification: dùng máy tính N ngày liên tiếp → thưởng thêm giờ ad-free.
- "Ad-light mode" (ít interstitial hơn nhưng banner luôn hiện) thay vì on/off tuyệt đối.
- Hiện mức tiết kiệm ước tính ("Bạn đã tránh X quảng cáo nhờ VIP") trong `VipActivity` để tăng cảm nhận giá trị gói.
- Cross-promote app khác của cùng publisher ở màn About thay vì chỉ banner network.
- Mediation waterfall đa network ngoài AdMob/AppLovin nếu fill-rate thị trường ngách (VN) thấp.

## Data/Persistence
- Group history theo ngày với sticky header (logic so sánh ngày đã có sẵn ở `HistoryAdapter.kt:76-119`, formal hoá thành section header thật).
- Long-press history entry để "sửa lại" (load calculation vào input, không chỉ paste kết quả).
- Trợ lý ảo trả lời "phép tính gần nhất của tôi là gì" dựa trên history store.
- TTL kết hợp size-cap: favorite giữ vĩnh viễn, history thường auto-expire sau N ngày.
- Mã hoá file SharedPreferences history at-rest cho user nhạy cảm dữ liệu tài chính.
- Insight screen: "phép tính hay dùng nhất", "tần suất theo ngày" từ timestamp history.
- Multi-profile history (tách "Work"/"Personal").

## Infra/Platform
- Lint check/Gradle task tự động đối chiếu `resourceConfigurations` ↔ `res/values-*` ↔ `LanguageHelper.SUPPORTED_LANGUAGES` ↔ `locales_config.xml` để bug F-INFRA-1 không tái diễn.
- Dynamic feature module cho VIP/theme premium nếu app phát triển nhiều nội dung trả phí, giảm base APK size.
- `MyTileService` hiện mở thẳng `MainActivity`, bỏ qua App Open Ad ở Splash — cân nhắc đây là trade-off có chủ đích (UX nhanh) hay lỗ hổng doanh thu vô tình, note lại trong `doc/AD.MD`.
- Robolectric để unit-test Activity (Splash/About/Settings) không cần thiết bị thật, giảm phụ thuộc `androidTest` chạy chậm.
- Weblate tự động tạo PR khi có bản dịch mới (hiện quản lý qua Weblate theo CLAUDE.md nhưng chưa thấy webhook/action đồng bộ về repo).
