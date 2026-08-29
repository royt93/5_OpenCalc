# NEW FEATURES — tính năng mới hợp lý cho app

## Core tính toán

- **N-CALC-1 — Chế độ chính xác cao / phân số** (BigDecimal hoặc a/b) tránh sai số double — nhiều app cạnh tranh (Desmos, HiPER Calc) có "exact mode". P2 · L.
- **N-CALC-2 — Base conversion (BIN/OCT/DEC/HEX)** — tính năng phổ biến trong scientific calculator, hiện chưa có file nào liên quan. P2 · L.
- **N-CALC-3 — Unit + currency converter** (độ dài, khối lượng, nhiệt độ, tỷ giá tiền tệ qua API) tích hợp ngay màn tính hoặc màn riêng — nguồn agy: hầu hết calculator top Play Store đều có (nguồn: agy, độc lập). P2 · L (phần tỷ giá cần API key + xử lý offline cache).
- ✅ **N-CALC-8 — Biến/bộ nhớ M+/M−/MR/MC** — tính năng calculator cổ điển, tích hợp trực tiếp vào parser hiện tại (nguồn: codex, độc lập). P2 · M. **Implemented**: `MainActivity.memoryAddButton/memorySubtractButton/memoryRecallButton/memoryClearButton` + `evaluateCurrentInputOrNull()`, dùng lại `calculationJob`/`calculationMutex`. UI: hàng nút mới `scientistModeRow4` trong `a_main.xml` + `layout-sw720dp-land/a_main.xml` (bỏ qua `layout-land` vì các row đã đủ 8 cột). MR/MC tự mờ (alpha 0.4) khi memory rỗng. Verify trên emulator Pixel_10_Pro_XL: M+ (78→memory=78), MR (recall "78" vào input rỗng), M− (0−20=−20), MC (memory về rỗng, MR/MC mờ lại).
- **N-CALC-4 — Hằng số vật lý** (c, h, k_B, N_A...) chọn qua danh sách bên cạnh π/e hiện có. P3 · M.
- ✅ **N-CALC-5 — History reuse / "tap to recall"** — tap vào 1 dòng history để tái sử dụng phép tính con (mở rộng `HistoryAdapter`, thêm callback insert-vào-input). P2 · M. **Đã có sẵn từ trước** (callback `onElementClick` trong `HistoryAdapter` + wiring `updateDisplay(window.decorView, value)` ở `MainActivity.onCreate`) — bản audit ban đầu bỏ sót. Verify lộ ra bug chặn hoàn toàn tính năng này: xem [[01_fixes.md]] F-DATA-9 (`equalsButton` tự huỷ coroutine → history không bao giờ lưu, nên chưa bao giờ có gì để tap-to-recall). Sau khi fix F-DATA-9, verify trên emulator: tính "5+3=8" → mở history → tap dòng "5+3" → input rỗng nhận lại đúng "5+3".
- ✅ **N-CALC-6 — "ans" reference / multi-line expression** — dùng kết quả câu trước trong câu sau (`ans + 5`). P2 · M. **Implemented**: token "ans" được thay thế bằng giá trị số NGAY TẠI `Expression.getCleanExpression()` (tham số `lastAnswer: Double?`, bọc `(giá_trị)` rồi mới chạy `addMultiply`/`addParenthesis`) — không đụng tokenizer đệ quy xuống trong `Calculator.kt`, giảm rủi ro vỡ luồng tính toán hiện có. `MainActivity.lastAnswer` cập nhật sau mỗi lần "=" thành công, truyền vào cả 3 nơi gọi `getCleanExpression` (`updateResultDisplay`, `evaluateCurrentInputOrNull` cho M+/M-, `equalsButton`). Nút "ans" mới thêm vào cuối `scientistModeRow3` (thay chỗ `Space` sẵn có, weight 0.6) ở `a_main.xml` + `layout-sw720dp-land/a_main.xml`; bỏ qua `layout-land` (rows đã đủ 8 cột, theo tiền lệ F-UI-3/Memory feature). Verify trên emulator: `5+3=` → `8` → `ans+1=` → `9`, preview trực tiếp lẫn sau khi bấm "=" đều đúng, history lưu đúng cả 2 dòng.
- **N-CALC-7 — Complex number cơ bản** (sqrt số âm hiện trả NaN) — rủi ro cao vì đổi kiểu trả về từ Double, cân nhắc kỹ trước khi làm. P3 · L.

## UI/UX

- **N-UI-1 — Home-screen widget (quick calc)** — chưa có `AppWidgetProvider`; widget resizable với mini keypad + last result. P2 · L.
- **N-UI-2 — Swipe gesture trên input** (swipe-left backspace / swipe-right clear) — pairs với `ItemTouchHelper` pattern đã cần cho E-UI-5. P3 · M.
- **N-UI-3 — Resizable/draggable history panel** — `SlidingUpPanelLayout` hiện là state machine cố định collapsed/expanded/anchored; cho drag-resize + persist chiều cao ưa thích. P3 · M.
- **N-UI-4 — Copy-as-text/Share calculation** — mở rộng long-press-to-copy hiện có (`resultDisplay`, `HistoryAdapter`) với `Intent.ACTION_SEND`, tái dùng share helper đã có ở `AboutActivity.shareApp()`. P3 · S.
- **N-UI-6 — Floating Calculator (cửa sổ nổi kiểu PiP)** — thu nhỏ máy tính thành overlay luôn hiển thị trên các app khác (`SYSTEM_ALERT_WINDOW` + `Service`), hữu ích khi đối chiếu số liệu từ app nhắn tin/tài liệu khác (nguồn: agy, độc lập). P2 · L.
- **N-UI-5 — Quick-calc qua notification/quick-settings tile mở rộng** — `MyTileService` hiện chỉ mở `MainActivity`; cân nhắc mini calculator ngay trong tile/notification cho phép tính nhanh không cần mở app đầy đủ. P3 · L.

## Ad & VIP / Monetization

- **N-AD-1 — Hoàn thiện Google Play Billing cho subscription thật** — `a_vip.xml:301-340` đã có sẵn UI `btnBuy30d/90d/1y/Lifetime/btnRestore` nhưng **disabled cứng** (`VipActivityInstrumentedTest.kt:57-67` assert `isEnabled==false`). Đây là gap monetization lớn nhất: hiện chỉ có redeem-key thủ công + rewarded ad, chưa thu tiền trực tiếp được. P1 · L.
- **N-AD-2 — Remove-Ads one-time purchase** (không kèm quyền lợi VIP khác) cho nhóm user chỉ muốn tắt quảng cáo, dễ convert hơn gói VIP đầy đủ. P1 · M.
- **N-AD-3 — Native Ad trong history panel** — chèn native ad định kỳ (sau mỗi 8-10 item) trong `rvHistory`, thêm touchpoint không chặn luồng tính toán chính. P2 · M.
- **N-AD-4 — Rewarded ad gắn tính năng cụ thể** (export lịch sử, mở khoá 1 theme premium riêng lẻ) độc lập với VIP flow chính. P2 · M.
- **N-AD-5 — Referral: mời bạn cài app → cả 2 nhận thêm ngày VIP** — chưa có cơ chế referral/deep-link nào trong `MyPreferences`. P3 · M.

## Data/Persistence

- **N-DATA-1 — Export history CSV/TXT/PDF/Excel** qua `ACTION_CREATE_DOCUMENT`. Bản PDF/Excel có kèm ghi chú từng phép tính hữu ích cho tệp user bán hàng online cần gửi file đối soát (nguồn ý tưởng PDF/Excel: agy, độc lập). P2 · M.
- **N-DATA-2 — Import history từ CSV** — bổ sung cho export. P3 · M.
- **N-DATA-3 — Search/filter history** theo biểu thức/kết quả — dễ hơn nhiều sau khi migrate Room (E-DATA-4, query `LIKE`). P2 · M.
- **N-DATA-4 — Pin/favorite history entry** — thêm field boolean vào `History`, giữ lại khi bị trim theo `historySize` hoặc `clearHistory()`. P2 · M.
- **N-DATA-5 — Tag/category cho history entry** (Thuế, Tip, Mua sắm...) + chip filter UI. P3 · M.

## Infra/Build/Platform

- **N-INFRA-1 — CI pipeline (GitHub Actions): build + lint + test trên mỗi PR** — repo hiện chỉ có `.github/FUNDING.yml` + issue templates, KHÔNG có workflow nào. Tối thiểu: `assembleDevDebug`, `./gradlew lint`, `./gradlew testDevDebugUnitTest` chạy tự động trên PR vào `dev`/`main`. **P0 · M** — nền tảng để mọi fix/feature khác không bị regress âm thầm.
- **N-INFRA-2 — Crash reporting (Firebase Crashlytics/Sentry)** — chưa có dependency crash-reporting nào; với ad SDK phức tạp + VIP flow, production crash reporting là thiết yếu để phát hiện sự cố kiểu F-AD-1/F-UI-1. P1 · M.
- **N-INFRA-3 — App Bundle (.aab) + Play Feature Delivery cho ngôn ngữ** — thay `resourceConfigurations` tĩnh (nguồn gốc bug F-INFRA-1) bằng Play Console tự tách theo device locale. P2 · M.
- **N-INFRA-4 — Migrate locale switching sang `AppCompatDelegate.setApplicationLocales()`** — thay cơ chế `attachBaseContext`/`killProcess` thủ công (F-INFRA-3), tự động hoạt động với `locales_config.xml` đã khai sẵn, loại bỏ luôn rủi ro lệch danh sách như F-INFRA-1. P1 · L.
- **N-INFRA-5 — In-app Update API (Play Core)** — nhắc user update khi có version mới, hữu ích để đẩy nhanh fix cho các bug P0 đã tìm thấy. P2 · S.
