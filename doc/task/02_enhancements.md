# ENHANCEMENTS — cải thiện code/UX/kiến trúc hiện có

## Core tính toán

- **E-CALC-1 — Thay `Double` bằng `BigDecimal`/`BigInteger` cho cộng/trừ/nhân** để tránh sai số kiểu `0.1+0.2=0.30000000000000004` (hiện chỉ vá bằng `roundResult()` làm tròn 10^-12 ở UI layer `MainActivity.kt:586-594`, không phải ở core). P2 · L.
- **E-CALC-2 — Refactor `Expression.kt` string-rewriting → tokenizer/AST rõ ràng.** `formatFactorial`/`addMultiply`/`getPercentString` là hàm dài (`formatFactorial` ~85 dòng với đếm ngoặc thủ công), dễ regress khi sửa. P2 · L.
- **E-CALC-3 — Bổ sung test coverage cho các case vừa phát hiện bug**: `tan(90°)`, `sin(360°)`/`cos(270°)` gần-0-âm, `ln` với 0<x<1, `0/0`, `(-0.5)!`, race condition đa luồng của global flags. P1 · M.
- **E-CALC-4 — Test percent-chaining nhiều lớp** (`10%+20%-5%`, percent sau phép trừ/nhân/chia hỗn hợp) — `getPercentString` đệ quy dựa string index thủ công, dễ vỡ. P2 · M.
- **E-CALC-5 — `NumberFormatter.extractNumbers` dùng string interpolation build regex** (`NumberFormatter.kt:26`) — đúng nhưng khó đọc/dễ lỗi khi sửa (thiếu 1 dấu `\\` sẽ biến literal dot thành wildcard). Dùng `Regex.escape(...)` thay vì tự ghép chuỗi. P2 · S.
- **E-CALC-6 — `gammaLanczos` không cache** kết quả factorial thập phân — chấp nhận được vì n nhỏ nhưng đáng note nếu tái sử dụng nhiều. P3 · S.

## UI/UX

- **E-UI-1 — Icon button có thể dưới 48dp touch target tối thiểu** (`ivSettingsBack`/`ivAboutBack` wrap_content theo drawable). Thêm `minWidth/minHeight="48dp"` cho WCAG/Material compliance. P2 · S.
- **E-UI-2 — Thiếu ripple/pressed-state feedback** trên history row và nút back (`HistoryAdapter.kt:124-133`, `ivSettingsBack`/`ivAboutBack`) — thêm `?attr/selectableItemBackground`. P3 · S.
- **E-UI-3 — VIP badge (`chipVipBadge`) biến mất hoàn toàn ở landscape/split-screen** — chỉ tồn tại trong `layout/a_main.xml`, không có ở `layout-land/a_main.xml`, mất touchpoint upsell chính cho foldable/tablet. P2 · M.
- **E-UI-4 — Chưa hỗ trợ bàn phím ngoài/Bluetooth** — `showSoftInputOnFocus=false` chỉ chặn IME popup, hardware keyboard gõ tự do không map vào semantics máy tính (Enter→"=", `*`/`/`→`×`/`÷`). Thêm `OnKeyListener`. P2 · M.
- **E-UI-5 — Không có xoá từng dòng history (chỉ all-or-nothing)** — thêm `ItemTouchHelper` swipe-to-delete cho `rvHistory`. P2 · M.
- **E-UI-6 — Chưa có foldable-aware layout** (hinge/fold-state detection qua `androidx.window.WindowInfoTracker`), hiện chỉ có qualifier tĩnh `layout-land`/`layout-sw600dp`/`layout-sw720dp`. P3 · L.
- **E-UI-7 — Thiếu Undo sau Clear/history-clear** — `clearButton`, `clearHistory()` không hoàn tác được, thêm Snackbar-undo. P3 · S.
- **E-UI-8 — Animate result→input khi bấm "="** thay vì snap tức thời (`equalsButton` dòng 954,965), giảm cảm giác "giật". P3 · S.

## Ad & VIP

- **E-AD-1 — Retry/backoff cho load rewarded & interstitial** khi load fail do network (vd 3 lần, 5s/15s/30s) thay vì im lặng bỏ cuộc. P2 · M.
- **E-AD-2 — Loading/disabled state cho nút "Xem QC"** trong lúc rewarded ad chưa sẵn sàng — tránh cảm giác app "đứng" khi bấm. P2 · S.
- **E-AD-3 — Phân biệt lý do rewarded thất bại** (network / user skip / no-fill) thay vì 1 message chung `vip_reward_not_earned` — giúp user hiểu và thử lại đúng cách. P2 · S.
- **E-AD-4 — Frequency capping interstitial theo action-count** thay vì mọi lần mở About/Settings — thêm counter app-side (cooldown/session) bên cạnh safety-limits SDK, vì calculator dùng rất thường xuyên trong ngày. P2 · M.
- **E-AD-5 — A/B test vị trí banner & tần suất interstitial** qua Remote Config, đo eCPM/retention theo cohort. P2 · L.
- **E-AD-6 — Thêm tier VIP 7 ngày và "trọn đời"** ngoài 3/30 ngày hiện có (`VipKeys.kt:19-35`) — layout `a_vip.xml` đã có sẵn placeholder `btnBuy30d/90d/1y/Lifetime` (disabled), xem N-AD-1 để bật thật. P2 · M.
- **E-AD-7 — `CountDownTimer` tick mỗi giây kể cả khi không cần độ chính xác giây** (`VipActivity.kt:210-220`) — tăng interval lên 1 phút, chỉ tick giây khi còn <60s, giảm CPU/battery. P3 · S.
- **E-AD-8 — Structured telemetry cho ad funnel** (request→fill→show→click→revenue) — hiện chỉ có `Logger.d` rải rác, không đo được fill-rate/eCPM theo placement. P2 · M.

## Data/Persistence

- **E-DATA-1 — Đưa toàn bộ history I/O ra khỏi Main Thread** (`Dispatchers.IO`) — giải quyết F-DATA-2/F-DATA-3. P1 · S.
- **E-DATA-2 — Try/catch quanh `Gson.fromJson`**, fallback list rỗng — giải quyết F-DATA-1. P1 · S.
- **E-DATA-3 — Mutex/serial writer cho `saveHistory`** loại race condition F-DATA-5. P2 · M.
- **E-DATA-4 — Migrate history sang Room (SQLite)** thay SharedPreferences+Gson blob — insert/delete từng dòng thay vì rewrite toàn bộ, index cho search/sort, giảm chi phí O(n) khi historySize lớn/∞. P2 · L.
- **E-DATA-5 — Pagination/lazy-load cho `HistoryAdapter`** thay vì nạp toàn bộ list 1 lần — quan trọng hơn nếu cho phép ∞ history. P2 · M.
- **E-DATA-6 — Backup/restore thủ công qua Storage Access Framework** (export/import JSON preferences+history), độc lập Android Auto Backup. P2 · M.
- **E-DATA-7 — Tránh trim+save vô ích mỗi `onResume`** — chỉ chạy lại khi `historySize` thực sự đổi so với lần áp dụng trước. P3 · S.

## Infra/Build/i18n/Testing

- **E-INFRA-1 — Unit test cho `util/LanguageHelper.kt`** — logic parse locale (`applyLanguage` dòng 104-110) hoàn toàn pure, chưa có test nào; nếu có sẽ bắt sớm bug F-INFRA-1. P1 · M.
- **E-INFRA-2 — Tăng test coverage cho package chưa test**: `BaseActivity`, `RApp`, `ext/Activity.kt`, `ext/Context.kt`, `model/Themes.kt`, `sv/MyTileService.kt`, `AboutActivity`, `SettingsActivity`, `SplashActivity` (nơi có App Open Ad lifecycle — touchpoint quan trọng nhất), `util/Logger.kt`. P2 · L.
- **E-INFRA-3 — Cập nhật `AdmobApplovinWrapper`** đang pin bản 1.1.5 dù cache cục bộ cho thấy version mới hơn (1.6.16, 1.6.21) từng được resolve — có thể bỏ lỡ fix mediation/adapter. P2 · M.
- **E-INFRA-4 — Bật `org.gradle.parallel=true` + configuration cache** trong `gradle.properties` (hiện đang comment sẵn) — giảm build time đáng kể với 30 locale + ProGuard + nhiều variant. P2 · S.
- **E-INFRA-5 — Hardening VIP secret key** — `VIP_SECRET_B64` chỉ Base64 (không mã hoá thật), dễ decode ngược từ APK release bằng `strings`+`base64 -d`. Cân nhắc verify server-side hoặc obfuscate mạnh hơn nếu cần chống bypass thực sự. P2 · M.
- ✅ **E-INFRA-6 — Chuyển `UIUtils.setupEdgeToEdge1/2` vào `BaseActivity`** — hiện mỗi Activity con phải tự nhớ gọi cặp hàm này trong `onCreate()` (xem [F-UI-12](01_fixes.md)); đã quên sót 2 lần liên tiếp khi thêm `BillSplitterActivity`/`BaseConverterActivity` cùng đợt, khiến nội dung bị status bar đè. P1 · S. **Implemented**: `BaseActivity` override cả 2 overload `setContentView` (nhận `View` — dùng bởi hầu hết activity qua ViewBinding — và nhận `@LayoutRes Int` — riêng `SettingsActivity`), gọi `UIUtils.setupEdgeToEdge1(window)` trước `super.setContentView(...)` rồi `UIUtils.setupEdgeToEdge2(...)` sau đó. Lấy root view qua `findViewById<ViewGroup>(android.R.id.content).getChildAt(0)` (con duy nhất của content frame) thay vì `findViewById(R.id.layoutRoot)` cố định — mỗi layout đặt tên root khác nhau (`layoutRoot` ở hầu hết, `root_layout` ở `SplashActivity`), cách này không phụ thuộc quy ước đặt tên. Đã xoá lời gọi thủ công trùng lặp ở `MainActivity`, `SettingsActivity`, `AboutActivity`, `VipActivity`, `BillSplitterActivity`, `BaseConverterActivity` (6 activity, cùng import `UIUtils` không dùng nữa). **`SplashActivity` giữ nguyên lời gọi thủ công** — activity này extend thẳng `AppCompatActivity`, không kế thừa `BaseActivity`, nên không tự động nhận fix (thay đổi inheritance ngoài phạm vi, rủi ro cao hơn lợi ích). Verify trên emulator: `MainActivity` + `BaseConverterActivity` vẫn đúng, không double-padding; build+lint+test pass.

