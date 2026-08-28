# FIXES — bug & rủi ro thật

Ghi chú: mọi finding dưới đây đã được agent audit đọc trực tiếp source + (với core tính toán) verify bằng cách chạy thử `Calculator().evaluate(...)`. Không có finding nào là suy đoán thuần lý thuyết.

## Core tính toán (`helper/Expression.kt`, `ext/Calculator.kt`)

### F-CALC-1 — `ln`/`log` domain check sai, `x.toInt()==0` false-positive với mọi 0<|x|<1
`Calculator.kt:159,164`. `ln(0.5)` ra đúng kết quả `-0.693...` nhưng vẫn set `domain_error=true` do cắt phần thập phân. Hậu quả nặng hơn: `equalsButton()` trong `MainActivity.kt:1010-1023` check `domain_error` TRƯỚC `division_by_0` → biểu thức trộn (`ln(0.5)+5/0`) hiện sai loại lỗi ("Domain error" thay vì "Division by zero").
P1 · S — đổi điều kiện thành `x == 0.0` (và gộp luôn case `x<0` → xem F-CALC-6).

### F-CALC-2 — `tan()` domain check sai công thức ở degree mode, không bắt được tan(90°)
`Calculator.kt:206`. Ở degree mode, `x` đã là độ nhưng code lại gọi `Math.toDegrees(x)` convert lần nữa → verify thực tế `tan(90°) = 1.63...E16`, không báo domain error, hiện số rác cho user.
P1 · S — so sánh trực tiếp `x==90.0` (degree) / `x==Math.PI/2` (radian), tổng quát hoá cho 270°, -90°.

### F-CALC-3 — Epsilon rounding chỉ xử lý phía dương, bỏ sót epsilon âm
Pattern lặp lại ở mọi hàm sin/cos/tan/arc*: `if (x > 0 && x < 1.0E-14) x = round(x)`. Verify: `sin(360°) = -2.449...E-16`, `cos(270°) = -1.836...E-16` hiện số cực nhỏ âm xấu thay vì `0`.
P1 · S — đổi điều kiện thành `abs(x) < 1.0E-14`.

### F-CALC-4 — `factorial()` từ chối mọi số âm kể cả số thập phân âm hợp lệ (Γ function)
`Calculator.kt:26-27`. `(-0.5)!` nhập được qua UI, đáng lẽ = Γ(0.5) ≈ 1.7724539 nhưng trả `NaN`.
P2 · S — chỉ NaN khi `number<0 && number==number.toInt()`.

### F-CALC-5 — `0/0` không hiện đúng "Division by zero"
`Calculator.kt:105-111` set `division_by_0=true` nhưng `0.0/0.0` là `NaN` không phải `Infinity`. `MainActivity.kt:1010-1031` chỉ đọc cờ này trong nhánh `isInfinite()`, nên rơi vào nhánh `isNaN()` → hiện "Math error" chung chung.
P2 · S — check `division_by_0` trước cả nhánh NaN.

### F-CALC-6 — `ln(-5)` không set `domain_error`
`Calculator.kt:158-166` domain check chỉ xét `x==0` (qua bug F-CALC-1), bỏ sót `x<0`. Verify: `ln(-5)=NaN, domain_error=false` → hiện "Math error" thay vì "Domain error".
P3 · S — gộp chung fix với F-CALC-1: `if (x <= 0.0) domain_error = true`.

### F-CALC-8 — Parser chấp nhận ký tự thừa cuối biểu thức (nguồn: codex, độc lập)
`Calculator.kt:85` — `parse()` chỉ log khi chưa đọc hết chuỗi (`pos < equation.length`) nhưng vẫn trả kết quả đã tính được thay vì báo lỗi. Vd `"2abc"` có thể bị tính thành `2` thay vì báo syntax error.
P1 · S — trả `NaN`/set `syntax_error=true` khi `pos < equation.length` sau parse.

### F-CALC-7 — 3 cờ lỗi toàn cục (`division_by_0`, `domain_error`, `syntax_error`) race condition giữa coroutine song song
`Calculator.kt:18-20`, dùng tại `MainActivity.kt:627` (mỗi lần gõ phím, `Dispatchers.Default`) và `MainActivity.kt:912` (`equalsButton`). Gõ nhanh có thể launch nhiều coroutine cùng lúc, reset/set cờ đè lên nhau → hiển thị sai/lạc loại lỗi.
P1 · M — chuyển flag thành return value/sealed result của `evaluate()` thay vì global var, hoặc cancel Job cũ trước khi launch coroutine mới cho `updateResultDisplay`.

## UI (`ui/MainActivity.kt` và layout liên quan)

### F-UI-1 — 6/7 preference trong Settings KHÔNG có tác dụng gì (key mismatch) — **P0**
`root_preferences.xml` bind vào key `"mckimquyen.opencal.KEY_VIBRATION_STATUS"`, `RADIANS_INSTEAD_OF_DEGREES_BY_DEFAULT`, `SCIENTIFIC_MODE_ENABLED_BY_DEFAULT`, `NUMBER_PRECISION`, `HISTORY_SIZE`, `PREVENT_PHONE_FROM_SLEEPING` (dòng 20,30,40,52,77,91) nhưng `MyPreferences.kt:16-24` đọc/ghi key hoàn toàn khác (`"royKEY_VIBRATION_STATUS"`, `"roySCIENTIFIC_MODE_ENABLED_BY_DEFAULT"`...). Toggle trong Settings đổi UI nhưng KHÔNG đổi behavior thật — chỉ "App language" hoạt động (click-through, không value-bound).
P0 · S — đổi `KEY_*` constant trong `MyPreferences.kt` khớp với key trong XML (hoặc ngược lại). **Cần regression test Settings sau khi sửa vì đây là bug ảnh hưởng gần như toàn bộ màn Settings.**

### F-UI-2 — Race condition đọc/ghi input trên background thread khi tap nhanh
`updateDisplay()` (`MainActivity.kt:480-583`), `updateResultDisplay()` (`:627-700`), `equalsButton()` (`:912-1039`) đều launch `Dispatchers.Default` rồi đọc `binding.input.text`/`selectionStart` trực tiếp trên background thread, không mutex. Tap nhanh → coroutine chồng chéo, `Editable` không thread-safe → có thể mất/lộn ký tự.
P1 · M — serialize input mutation (Mutex/Channel) hoặc chỉ offload phần `Calculator().evaluate()` sang background, giữ thao tác view trên Main.

### F-UI-3 — Scientific mode có thể bị ẩn vĩnh viễn ở landscape/split-screen/foldable
`enableOrDisableScientistMode()` (`MainActivity.kt:596-610`) giả định `scientistModeRow2/3` khởi tạo `GONE` (chỉ đúng ở `layout/a_main.xml`). Ở `layout-land/a_main.xml` các row này mặc định `VISIBLE` và KHÔNG có `scientistModeSwitchButton` để bật lại → `onCreate` gọi hàm này khi pref mặc định `true` sẽ ẩn mất sin/cos/tan/log không có cách nào bật lại (multi-window/freeform/foldable không tôn trọng `screenOrientation="portrait"`).
P1 · M — đồng bộ trạng thái GONE/VISIBLE ban đầu giữa layout portrait/landscape, đảm bảo luôn có control để bật lại.

### F-UI-4 — Paste clipboard bypass toàn bộ input validation
`binding.input` vẫn nhận native long-press "Paste" (chỉ tắt `showSoftInputOnFocus`), chèn thẳng text vào `Editable` bỏ qua `NumberFormatter`/symbol-collision logic mà mọi nút bấm đều đi qua. Code detect-paste cũ đã bị comment out (`MainActivity.kt:246-270`) chưa thay bằng filter thật.
P2 · M — thêm `InputFilter` chuẩn hoá text dán qua cùng pipeline với button tap, hoặc chặn paste tự do.

### F-UI-5 — `contentDescription` sai/generic trên hầu hết icon button (vi phạm accessibility)
Hầu hết `ImageButton` ở `a_main.xml`, `a_about.xml`, `a_settings.xml` dùng tên app thay vì mô tả hành động (TalkBack đọc "OpenCalc" cho nút chia/nhân/cộng/trừ...). Cụ thể: `a_main.xml:155,307,319,331,376,421,466,493,516`; `a_settings.xml:32`; `a_about.xml:33`. (`backspaceButton` dòng 504 làm đúng, dùng làm mẫu.)
P1 · S — thêm string resource riêng cho từng action.

### F-UI-6 — `!!.toInt()` không try/catch trên preference string
`prefs.numberPrecision!!.toInt()` (`MainActivity.kt:591`), `prefs.historySize!!.toInt()` (`:999,:1149`). Chưa crash vì giá trị hiện cố định, nhưng fragile — nên hardening cùng lúc fix F-UI-1.
P3 · S.

### F-UI-7 — `onBackPressed()` deprecated, phá predictive-back gesture Android 13+
`MainActivity.kt:1177-1186` override API cũ thay vì `OnBackPressedCallback`/`onBackPressedDispatcher`.
P2 · S.

### F-UI-8 — History UI off-by-one, lệch với tầng lưu trữ (nguồn: codex, độc lập)
`MainActivity.kt:1000,1150` — điều kiện `itemCount >= historySize` chạy SAU khi append, nên giới hạn 100 thực tế chỉ giữ 99 item hiển thị; `onResume()` lặp lại lỗi tương tự. Tầng SharedPreferences (`MyPreferences.kt`) lại dùng đúng điều kiện `size > limit`, khiến UI và dữ liệu lưu không đồng nhất (list hiển thị và list lưu lệch nhau 1 phần tử).
P2 · S — đồng bộ điều kiện trim giữa UI (`MainActivity`) và storage (`MyPreferences`).

## Ad & VIP (`RApp.kt`, `feature/vip/*`, `common/const/AdKeys.kt`, `app/build.gradle`)

### F-AD-1 — AdMob Rewarded ID release vẫn là test ID Google — **P0**
`app/build.gradle:47` (block `release`) = `ca-app-pub-3940256099942544/5224354917`, trùng hệt debug (`:67`). Verify bằng grep, khớp cảnh báo đã ghi sẵn ở `doc/AD.MD`. Nếu bật `IS_ENABLE_ADMOB=true` cho release nguyên trạng: mất doanh thu rewarded hoàn toàn hoặc bị Google flag invalid traffic.
P0 · S — thay ID rewarded thật trước khi bật AdMob cho production.

### F-AD-2 — AppLovin rewarded ID chưa xác nhận còn active trên console MAX
`app/build.gradle:48,68` = `5423b3cd5889dcae` (giống debug/release — chấp nhận được vì AppLovin không có test-ID riêng, nhưng CẦN xác nhận placement chưa bị pause/archive). Nếu invalid, `VipActivity.onWatchAdClicked()` (`VipActivity.kt:310-323`) luôn `earned=false` âm thầm, "xem QC nhận 3 ngày VIP" chết mà không ai biết.
P1 · S — verify trên AppLovin MAX dashboard.

### F-AD-3 — Thiếu test device ID / EEA debug geography cho UMP consent
`SplashActivity.kt:40-45` gọi `requestConsentInfoUpdate` nhưng không cấu hình `ConsentDebugSettings` (test device hash + `DebugGeography.EEA`). Không thể QC consent flow thật tại VN trước khi ship cho thị trường EU.
P1 · M.

### F-AD-4 — Interstitial ở MainActivity chỉ preload 1 lần, không bao giờ reload sau khi show
`MainActivity.kt:108` gọi `loadInterstitial` duy nhất trong `onCreate`; callback show ở `openAbout`/`openSettings` (`:300-320`) không gọi lại `loadInterstitial` sau khi đóng ad — khác với pattern đúng ở `VipActivity.onWatchAdClicked()` (có preload lại, dòng 318-321). Sau lần mở About/Settings đầu tiên, các lần sau trong session sẽ không có ad sẵn sàng.
P1 · S — gọi lại `loadInterstitial` trong callback đóng ad.

### F-AD-5 — Test instrumented hardcode plain VIP key trong source
`AdKeysInstrumentedTest.kt:18`: `assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", AdKeys.VIP_SECRET)` — làm lộ plaintext secret trong repo, vô hiệu hoá mức che giấu Base64.
P2 · S — so sánh gián tiếp (decode lại từ `BuildConfig.VIP_SECRET_B64` rồi so `AdKeys.VIP_SECRET`), không hardcode plaintext trong test.

### F-AD-6 — Không có timeout/fallback khi `loadRewarded` không callback (mạng chết)
`VipActivity.kt:81` gọi `loadRewarded` khi mở màn, nút "Xem QC" vẫn `visible/enabled` vô điều kiện (`bindFreeUi()` dòng 149-157) dù ad chưa sẵn sàng — user bấm thấy app "đứng" khi mạng yếu.
P2 · M.

### F-AD-9 — VIP key có thể bị giả mạo/reverse từ APK (nguồn: codex, độc lập — mức độ nghiêm trọng hơn đánh giá nội bộ ban đầu)
Key 3/30 ngày và `VIP_SECRET` đều nằm client-side, chỉ che bằng Base64 (không phải mã hoá thật, xem `VipKeys.kt:20`, `app/build.gradle:49`). Sau khi decompile APK release, ai cũng đọc được `VIP_SECRET`/`VIP_3D_KEY` plaintext và tự kích hoạt VIP vô hạn cho mình, hoặc chia sẻ key public — đây là rủi ro doanh thu thật nếu VIP có giá trị thương mại, KHÔNG chỉ là vấn đề "code style" như đánh giá ban đầu ở `E-INFRA-5`.
Đánh giá nội bộ ban đầu (agent audit Ad/VIP) xếp việc này ở mức "cân nhắc hardening" (P2/M — đổi verify sang server-side hoặc obfuscate mạnh hơn "nếu cần chống bypass thực sự"). **Codex (agent độc lập bên ngoài) xếp P1/L** vì cho rằng với single-secret client-side design hiện tại, việc bypass gần như chắc chắn xảy ra khi app đủ phổ biến để bị decompile, không phải "nếu cần" mà là "sẽ xảy ra".
→ Đây là điểm bất đồng cần **quyết định chiến lược** (xem AskUserQuestion cuối backlog): giữ nguyên model hiện tại (chấp nhận rủi ro, ưu tiên ship nhanh) hay đầu tư verify server-side/Play Integrity trước khi mở rộng thêm tier VIP trả phí (N-AD-1).
P1 · L (nếu chọn làm) — xem `06_external_ai_opinions.md` để so sánh đầy đủ.

### F-AD-8 — `countDownTimer` KHÔNG bị cancel ở `onPause()`, chỉ pulse/shimmer animator được cancel (nguồn: agy/gemini, verify lại bằng code thật — đã đọc trực tiếp `VipActivity.kt:117-136,205-234`)
`onPause()` (dòng 117-122) chỉ cancel `pulseAnimator`/`shimmerAnimator`, KHÔNG cancel `countDownTimer`. Timer tiếp tục tick mỗi giây gọi `renderTick()` ghi vào `binding.progressVip`/`binding.tvCountdown` dù Activity không còn visible, cho tới khi `onDestroy()` hoặc lần `bindUi()` kế tiếp (dòng 136) cancel nó. Không phải leak tham chiếu (comment "cancel ở onDestroy" trong code và `doc/memory_leak.md` chỉ audit reference-leak, không audit background-tick), nhưng lãng phí CPU/wakeups khi app ở background — đặc biệt nếu user mở VipActivity rồi bấm Home mà không đóng hẳn app.
P2 · S — thêm `countDownTimer?.cancel()` vào `onPause()` (và restart lại ở `onResume()`/`bindUi()` nếu cần).

### F-AD-7 — Grace-entry hardcode giả định trial = 24h
`VipActivity.kt:166-174` hardcode `TimeUnit.DAYS.toMillis(1)` để backfill `grantedAtMs`. Nếu SDK đổi default trial mà không đồng bộ, progress bar/countdown sai cho user ở grace period.
P3 · S — đưa hằng số về config chung + doc rõ "phải update khi lib đổi trial".

## Data/Persistence (`db/MyPreferences.kt`, `model/History.kt`)

### F-DATA-1 — `Gson.fromJson` không try/catch → crash khi JSON hỏng
`MyPreferences.kt:67`, gọi tại `MainActivity.kt:150` trong `onCreate`. JSON corrupt (restore backup lỗi, đổi schema Gson) → `JsonSyntaxException` không bắt → crash ngay khi mở app.
P1 · S — try/catch, fallback list rỗng + log.

### F-DATA-2 — History I/O đồng bộ trên Main Thread lúc khởi động
`MainActivity.kt:150` — `prefs.getHistory()` parse toàn bộ history trực tiếp trong `onCreate`, không coroutine → jank/ANR tiềm ẩn máy yếu/history lớn.
P1 · S — wrap `Dispatchers.IO`.

### F-DATA-3 — History I/O đồng bộ lặp lại mỗi lần `onResume`
`MainActivity.kt:1149-1158` — đọc + ghi lại toàn bộ blob mỗi lần app foreground kể cả khi không cần trim.
P1 · S.

### F-DATA-4 — Unbounded growth khi `historySize = "∞"`
`MyPreferences.kt:77` (`while (hs.toInt() > 0 && ...)`) + `arrays.xml:22` (`item>-1<` cho "∞") → điều kiện trim không bao giờ đúng, history phình vô hạn, mỗi lần "=" re-serialize blob ngày càng lớn → chậm dần, rủi ro OOM.
P1 · M.

### F-DATA-5 — Race condition/lost update khi lưu history đồng thời
`equalsButton` tạo coroutine mới mỗi lần bấm (`Dispatchers.Default`), đọc→sửa→ghi không khoá đồng bộ trong `MyPreferences`. Bấm "=" liên tiếp nhanh có thể mất 1 entry history âm thầm.
P2 · M.

### F-DATA-6 — State kép: field `history` private không còn là nguồn sự thật
`MyPreferences.kt:44-45,81` — field set qua setter nhưng `getHistory()` luôn đọc tươi từ SharedPreferences, bỏ qua field này; `saveHistory()` tạo instance `MyPreferences` mới thay vì dùng `this`.
P3 · S — dọn dead state.

### F-DATA-7 — `appLanguage` dùng `.commit()` đồng bộ, phần còn lại dùng `.apply()`
`MyPreferences.kt:56`, gọi trực tiếp từ click listener dialog (`LanguageHelper.kt:78`) → block Main Thread cho disk fsync trước khi restart app.
P2 · S.

### F-DATA-8 — Không có version/migration strategy cho schema `History`
`model/History.kt` không có version field, migration hiện dựa pattern thủ công `time.isNullOrEmpty()` (`HistoryAdapter.kt:73`).
P3 · S.

## Infra / Build / i18n / Testing

### F-INFRA-1 — 5 locale bị build script cắt khỏi APK dù UI cho phép chọn — **P0**
`resourceConfigurations` (`app/build.gradle:10`) chỉ khai báo 23 locale nhưng `res/values-*` có 28 locale thật (thiếu `cs`, `ml`, `or`, `pl`, `sr`). `sr` còn khai trong `res/xml/locales_config.xml` (per-app language Android 13+), và cả 5 locale có mặt trong `LanguageHelper.SUPPORTED_LANGUAGES` (bộ chọn ngôn ngữ trong app) → user chọn được nhưng resource đã bị build cắt, luôn thấy tiếng Anh.
P0 · S — thêm 5 locale còn thiếu vào `resourceConfigurations`, hoặc bỏ hẳn config filter thủ công (xem N-INFRA-3 App Bundle language split).

### F-INFRA-2 — AdMob Rewarded ID release = test ID Google
Trùng với F-AD-1, xem chi tiết ở mục Ad & VIP.

### F-INFRA-3 — `LanguageHelper.restartApp()` gọi `killProcess` ngay sau `startActivity` cùng process
`LanguageHelper.kt:146-159` — `Process.killProcess` gần như ngay sau `startActivity` có thể giết Activity mới trước khi kịp `onCreate`, khiến app thoát đột ngột thay vì restart mượt khi đổi ngôn ngữ.
P1 · M — chuyển sang `AppCompatDelegate.setApplicationLocales()` (tận dụng `locales_config.xml` đã có, xem N-INFRA-4).

### F-INFRA-4 — ProGuard rules chứa hàng loạt rule "chết" cho lib không tồn tại trong dependencies
`proguard-rules.pro:4-196` — rule cho butterknife, retrofit/retrofit2, facebook, realm, eventbus, rx, glide, jsoup, uCrop, dexter... không lib nào có trong `app/build.gradle`. Rõ ràng copy từ template cũ, gây khó audit/maintain.
P2 · S — dọn sạch, chỉ giữ rule cho dependency thật.

### F-INFRA-5 — `-keep public class com.google.** {*;}` quá rộng, vô hiệu hoá phần lớn lợi ích R8 shrink/obfuscate
`proguard-rules.pro:4` — giữ nguyên toàn bộ namespace `com.google.*` (Material, Gson, Play Review...) trong khi `AdmobApplovinWrapper:1.1.5` đã tự mang consumer ProGuard rule chính xác cho `com.google.android.gms.ads.**`/`com.applovin.**`/`com.google.android.ump.**`.
P2 · S — thu hẹp rule, để lib tự quản consumer rules.

### F-INFRA-6 — Dependency khai báo trùng lặp
`androidx.preference:preference-ktx:1.2.1` khai 2 lần (`app/build.gradle:139,144`).
P3 · S.
