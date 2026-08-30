# FIXES — bug & rủi ro thật

## 🔴 P0-CRITICAL — bảo mật (đã tự verify bằng git/gh, không phải suy đoán từ agent)

### F-SEC-1 — Keystore ký release + mật khẩu đang lộ công khai trên GitHub
Phát hiện ban đầu từ session `claude -p` độc lập ("cần check .gitignore"), tôi đã tự verify lại trực tiếp:
- `git ls-files` xác nhận **`app/keystore.jks`** (file keystore ký release thật) VÀ **`gradle.properties`** (chứa `KS_ALIAS=mckimquyen`, `KS_PW=27072000` plaintext — dòng 24-25) đều **đang được git track**, KHÔNG có trong `.gitignore` (file `.gitignore` không hề nhắc tới 2 file này).
- `git log -- app/keystore.jks` cho thấy file được commit từ **2023-05-01**.
- `git remote -v` → `origin = https://github.com/tplloi/OpenCalc.git`. `gh repo view tplloi/OpenCalc --json isPrivate,visibility,pushedAt` trả về **`"visibility":"PUBLIC"`, push gần nhất hôm nay**.
→ Kết luận: keystore ký app thật + mật khẩu của nó đang công khai trên Internet, ai cũng tải về dùng để tự ký APK mạo danh app này (cùng signature), hoặc nếu keystore này dùng chung cho app khác của publisher thì rủi ro lan sang app đó luôn.
**Priority: P0 · Size: S (đổi visibility/rotate) → M/L (nếu chọn purge lịch sử git đầy đủ).**

**Phương án đã trình bày cho user (2026-08-28), user chọn: "chưa xử lý ngay, chỉ ghi vào backlog để tự quyết định sau".** 3 phương án còn lại nếu quyết định xử lý:
1. Đổi repo `tplloi/OpenCalc` sang Private + rotate `KS_PW`/tạo keystore mới — nhanh, chặn truy cập mới ngay, nhưng **PHẢI xác nhận Play App Signing đang bật trước khi đổi keystore** (nếu app đã publish bằng cert cũ và không dùng Play App Signing, đổi keystore = mất khả năng update app hiện tại trên Play Store).
2. Chỉ thêm `app/keystore.jks` + `gradle.properties` vào `.gitignore`, giữ repo public — nhẹ nhất nhưng lịch sử cũ (từ 2023, đã public nhiều năm) coi như đã lộ vĩnh viễn, chỉ chặn rò rỉ thêm về sau.
3. Purge toàn bộ lịch sử git (BFG/`git filter-repo`) + force-push — triệt để nhất nhưng cực rủi ro: phá vỡ mọi clone/fork/PR đang mở của người khác (kể cả liên kết với upstream `Darkempire78/OpenCalc` nếu có), và cache của GitHub/Google có thể vẫn giữ bản cũ dù đã force-push.

Không tự ý thực hiện bất kỳ phương án nào ở trên (đổi visibility, rotate key, purge history, force-push) nếu chưa được yêu cầu rõ ràng trong 1 lần trao đổi riêng — đây là hành động có blast-radius lớn trên repo public thật.

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

### ✅ F-CALC-4 — `factorial()` từ chối mọi số âm kể cả số thập phân âm hợp lệ (Γ function) — đã fix
`Calculator.kt:26-27`. `(-0.5)!` nhập được qua UI, đáng lẽ = Γ(0.5) ≈ 1.7724539 nhưng trả `NaN`.
P2 · S — **Đã fix**: `factorial()` giờ chỉ trả `NaN` khi `number<0 && decimalPartOfNumber==0.0` (số nguyên âm — cực thật của hàm Gamma); số thập phân âm rơi đúng vào nhánh `gammaLanczos(number+1)`. Verify unit test `CalculatorTest.factorialNegativeNonIntegerUsesGamma` (`(-0.5)! ≈ 1.7724539`) + `factorialNegativeIntegerIsStillNaN` (`(-3)!` vẫn NaN, không regress).

### ✅ F-CALC-5 — `0/0` không hiện đúng "Division by zero" — đã fix
`Calculator.kt:105-111` set `division_by_0=true` nhưng `0.0/0.0` là `NaN` không phải `Infinity`. `MainActivity.kt:1010-1031` chỉ đọc cờ này trong nhánh `isInfinite()`, nên rơi vào nhánh `isNaN()` → hiện "Math error" chung chung.
P2 · S — **Đã fix**: `equalsButton()` giờ check `division_by_0` NGAY SAU `syntax_error`/`domain_error`, TRƯỚC cả `isInfinite()`/`isNaN()` — bao trùm cả `0/0` (NaN) lẫn `x/0, x≠0` (Infinity). Verify unit test `ExpressionCalculatorPipelineTest.zeroDividedByZeroSetsDivisionByZeroFlag`.

### ✅ F-CALC-6 — `ln(-5)` không set `domain_error` — hoá ra đã fix sẵn từ Sprint 1
`Calculator.kt:158-166` domain check chỉ xét `x==0` (qua bug F-CALC-1), bỏ sót `x<0`. Verify: `ln(-5)=NaN, domain_error=false` → hiện "Math error" thay vì "Domain error".
P3 · S — **Kiểm tra lại code hiện tại: đã dùng `if (x <= 0.0)` từ khi fix F-CALC-1 ở Sprint 1** (cover cả `x==0` lẫn `x<0` cùng lúc, audit ban đầu bỏ sót không đánh dấu). Không cần sửa thêm, chỉ thêm regression test `CalculatorTest.lnOfNegativeSetsDomainError` để khẳng định + chặn regression tương lai.

### ✅ F-CALC-9 — Percent trong ngoặc có toán tử tính sai (nguồn: claude session độc lập) — đã fix
`Expression.kt:62-64` — `getPercentString` quét ngược tìm operator để quyết định cách diễn giải `%` nhưng không đếm độ sâu ngoặc. Vd `(10+5)%` bị tính ra `10.5` thay vì `0.15` đúng (15% dạng thập phân). Input hợp lý trong tình huống tính chiết khấu trên tổng đã có trong ngoặc.
P2 · M — **Đã fix**: thay `lastIndexOfAny` (không phân biệt độ sâu ngoặc) bằng vòng quét ngược thủ công có đếm `depth` — bỏ qua operator nằm trong ngoặc con lồng, chỉ dừng ở operator/`(` cùng mức với `%`. Verify unit test `ExpressionCalculatorPipelineTest.percentOfParenthesizedExpression` (`(10+5)% = 0.15`) + `percentAfterMultiplyOfParenthesizedExpression` (`2×(10+5)% = 0.3`), không regress `percentAdded`/`percentAlone` đã có từ trước.

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

### ✅ F-UI-4 — Paste clipboard bypass toàn bộ input validation — đã fix
`binding.input` vẫn nhận native long-press "Paste" (chỉ tắt `showSoftInputOnFocus`), chèn thẳng text vào `Editable` bỏ qua `NumberFormatter`/symbol-collision logic mà mọi nút bấm đều đi qua. Code detect-paste cũ đã bị comment out (`MainActivity.kt:246-270`) chưa thay bằng filter thật.
P2 · M — **Đã fix, 2 bước**: bước 1 chỉ set `customInsertionActionModeCallback`/`customSelectionActionModeCallback` để ẩn nút "Paste" khỏi context-menu nổi — **verify trên emulator phát hiện đây là fix KHÔNG ĐỦ**: `adb shell input keyevent KEYCODE_PASTE` (phím tắt) vẫn dán được bình thường vì nó gọi thẳng `TextView.onTextContextMenuItem()`, không đi qua ActionMode. Bước 2 (fix thật): tạo `ui/CalculatorInputEditText.kt` (subclass `AppCompatEditText`) override `onTextContextMenuItem()` chặn `android.R.id.paste`/`pasteAsPlainText` — điểm chặn DUY NHẤT chung cho cả 2 đường. Đổi tag `<EditText id="@+id/input">` sang `<com.mckimquyen.opencal.ui.CalculatorInputEditText>` ở cả 3 layout (`layout`, `layout-land`, `layout-sw720dp-land`). Class mới cũng override `getText()` trả non-null (EditText luôn có buffer) để tránh phải sửa ~23 chỗ gọi `binding.input.text` trong `MainActivity.kt` (Kotlin coi kiểu Java platform-type khác đi khi truy cập qua 1 subclass Kotlin cùng module). Verify lại bằng `KEYCODE_PASTE` sau fix: không còn dán được nữa; Copy/Cut/Share vẫn hoạt động bình thường.

### F-UI-5 — `contentDescription` sai/generic trên hầu hết icon button (vi phạm accessibility)
Hầu hết `ImageButton` ở `a_main.xml`, `a_about.xml`, `a_settings.xml` dùng tên app thay vì mô tả hành động (TalkBack đọc "OpenCalc" cho nút chia/nhân/cộng/trừ...). Cụ thể: `a_main.xml:155,307,319,331,376,421,466,493,516`; `a_settings.xml:32`; `a_about.xml:33`. (`backspaceButton` dòng 504 làm đúng, dùng làm mẫu.)
P1 · S — thêm string resource riêng cho từng action.

### ✅ F-UI-6 — `!!.toInt()` không try/catch trên preference string — đã fix
`prefs.numberPrecision!!.toInt()` (`MainActivity.kt:591`), `prefs.historySize!!.toInt()` (`:999,:1149`). Chưa crash vì giá trị hiện cố định, nhưng fragile — nên hardening cùng lúc fix F-UI-1.
P3 · S — **Đã fix**: cả 3 chỗ đổi sang `prefs.xxx?.toIntOrNull() ?: default` (`numberPrecision` fallback `10`, `historySize` fallback `100`, khớp default trong `root_preferences.xml`/`MyPreferences.kt`).

### ✅ F-UI-7 — `onBackPressed()` deprecated, phá predictive-back gesture Android 13+ — đã fix
`MainActivity.kt:1177-1186` override API cũ thay vì `OnBackPressedCallback`/`onBackPressedDispatcher`.
P2 · S — **Đã fix**: thay bằng `OnBackPressedCallback` đăng ký qua `onBackPressedDispatcher.addCallback(this, ...)` trong `onCreate`, giữ nguyên logic double-back-to-exit. Verify trên emulator: bấm back 1 lần hiện Toast "Please click BACK again to exit", bấm lần 2 (trong 2s) app thoát về launcher đúng — xác nhận qua `logcat` (`TransitionRequestInfo type=CLOSE`) + `dumpsys activity` (resumed activity chuyển về launcher).

### F-UI-9 — Không giới hạn độ dài input → `StackOverflowError` crash toàn app (nguồn: claude session độc lập)
`Calculator.kt` (parser đệ quy xuống, dòng 92-282) và `Expression.getPercentString` không giới hạn độ sâu/độ dài; layout không có `InputFilter`/`maxLength` trên `binding.input`; `evaluate()` không được gọi trong try/catch. Paste (xem F-UI-4) một chuỗi rất dài (vd nhiều dấu ngoặc lồng nhau lặp lại) có thể làm parser đệ quy vượt stack → crash toàn app, không chỉ lỗi phép tính.
P2 · S — thêm `maxLength` trên input + try/catch quanh `evaluate()` (fallback hiện lỗi thay vì crash).

### ✅ F-UI-10 — Backspace có thể `StringIndexOutOfBoundsException` — đã fix
`MainActivity.kt:1108-1109` — guard hiện tại không đảm bảo `leftPartWithoutSpaces` non-empty sau khi bỏ dấu grouping separator.
P2 · S — **Đã fix**: thêm check `isNotEmpty()` trước khi `subSequence(0, length - 1)`, trả `""` nếu rỗng thay vì crash. Verify trên emulator: gõ số + backspace liên tục tới rỗng, không crash (regression check), `dumpsys activity` xác nhận app vẫn resumed.

### ✅ F-UI-11 — Xoay màn hình mất trạng thái toggle không lưu `onSaveInstanceState` — đã fix
Các state như `isInvButtonClicked`, `errorStatusOld`, trạng thái scientific mode do user bật tay không được lưu qua config change (xoay màn hình) — UX bug thật (không crash): user bật "Inv" hoặc scientific mode rồi xoay máy sẽ bị reset về mặc định.
P2 · S — **Đã fix**: override `onSaveInstanceState` lưu 3 giá trị (`isInvButtonClicked`, `isDegreeModeActivated`, visibility của `scientistModeRow2`); `restoreToggleState()` gọi ở cuối `onCreate` (sau khi áp default từ preference) để ghi đè lại đúng state user tự bật tay, dùng lại các hàm toggle sẵn có (`enableOrDisableDegreeMode`, `enableOrDisableScientistMode`) thay vì set trực tiếp field để không bỏ sót side-effect UI. Tách `setInvButtonLabels()` khỏi `invButton()` để dùng lại được. **Verify trên emulator** (MainActivity khoá `screenOrientation="portrait"` nên xoay vật lý không kích hoạt recreate — ép recreate bằng đổi `font_scale` qua `adb shell settings put system font_scale 1.1`, cùng cơ chế Android dùng cho mọi config change bao gồm xoay màn hình thật): bật INV + đổi DEG→RAD → recreate → cả 2 state đều giữ nguyên đúng (RAD, sin⁻¹/cos⁻¹/tan⁻¹/x²/e^/10^ vẫn hiện).

### ✅ F-UI-8 — History UI off-by-one, lệch với tầng lưu trữ (nguồn: codex, độc lập) — đã fix
`MainActivity.kt:1000,1150` — điều kiện `itemCount >= historySize` chạy SAU khi append, nên giới hạn 100 thực tế chỉ giữ 99 item hiển thị; `onResume()` lặp lại lỗi tương tự. Tầng SharedPreferences (`MyPreferences.kt`) lại dùng đúng điều kiện `size > limit`, khiến UI và dữ liệu lưu không đồng nhất (list hiển thị và list lưu lệch nhau 1 phần tử).
P2 · S — **Đã fix**: đổi cả 2 chỗ (`equalsButton()` + `onResume()`) từ `fullHistorySize >= historySize` sang `fullHistorySize > historySize`, khớp đúng ngưỡng `size > effectiveLimit` đã dùng ở `MyPreferences.saveHistory()`. Verify qua code review đối chiếu 2 điều kiện khớp nhau; không có unit test riêng (logic gắn với `HistoryAdapter`/RecyclerView, cần instrumented test để tạo >100 item — để sau nếu cần).

### ✅ F-UI-12 — `BillSplitterActivity`/`BaseConverterActivity` bị status bar đè lên (edge-to-edge) — **P1, đã fix**
Cả 2 activity mới thêm trong session này thiếu cặp `UIUtils.setupEdgeToEdge1(window)` (trước `setContentView`) + `UIUtils.setupEdgeToEdge2(rootView = findViewById(R.id.layoutRoot), paddingTop = true, paddingBottom = true)` (sau `setContentView`) mà **mọi** Activity khác trong app (`MainActivity`, `SettingsActivity`, `AboutActivity`, `SplashActivity`, `VipActivity`) đều gọi. `targetSdk 37` khiến Android 15+ ép edge-to-edge bất kể app có opt-in hay không — thiếu insets listener này làm back-arrow + title đè lên đồng hồ/icon status bar. Layout XML đã sẵn `android:id="@+id/layoutRoot"` đúng convention nhưng code Kotlin quên gọi 2 hàm trên. Do user báo trực tiếp qua screenshot test, phát hiện qua audit toàn bộ Activity (agent research) rồi fix cả 2 file theo đúng pattern từ `AboutActivity.kt`. Verify trên emulator: cả 2 màn hình đều đã có khoảng cách đúng với status bar.
**Rủi ro tái diễn**: `BaseActivity.kt` không tự động xử lý insets cho activity con — mỗi activity phải tự nhớ copy cặp gọi này. Đã xảy ra đúng lỗi này 2 lần liên tiếp (BillSplitter + BaseConverter được thêm cùng đợt). **Khuyến nghị dài hạn** (chưa làm, ghi nhận riêng): chuyển việc gọi `setupEdgeToEdge1/2` vào `BaseActivity` (override `setContentView`) để mọi activity mới tự động thừa hưởng, không cần nhớ copy-paste.

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

### F-AD-10 — SDK auto-trial có thể ghi đè VIP vừa redeem trong ~2s đầu sau cài đặt (nguồn: claude session độc lập, phụ thuộc hành vi SDK ngoài — cần verify thêm với vendor `AdmobApplovinWrapper` trước khi kết luận chắc chắn)
SDK dùng chung 1 field lưu trữ (`keyVipByKeyUntil`) cho cả auto-trial (callback `InstallReferrer` chạy async) và user-redeem-key. Nếu callback ghi đè vô điều kiện không so sánh giá trị hiện có, kịch bản: user mở app lần đầu, redeem ngay key 30 ngày trong vài giây đầu → callback auto-trial đến sau ghi đè xuống còn ~1 ngày → mất VIP đã redeem trong im lặng. Chỉ xảy ra ở release build, lần chạy đầu tiên.
P2 · M/L (phụ thuộc hành vi thật của SDK bên thứ 3, khó fix hoàn toàn ở app-side — cần báo vendor hoặc thêm guard app-side so sánh timestamp trước khi ghi đè nếu SDK có API cho phép).

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

### ✅ F-DATA-5 — Race condition/lost update khi lưu history đồng thời — hoá ra đã fix sẵn
`equalsButton` tạo coroutine mới mỗi lần bấm (`Dispatchers.Default`), đọc→sửa→ghi không khoá đồng bộ trong `MyPreferences`. Bấm "=" liên tiếp nhanh có thể mất 1 entry history âm thầm.
P2 · M — **Kiểm tra lại code hiện tại: cả 4 call site của `prefs.saveHistory()` trong `MainActivity.kt` (equalsButton, onPinToggle, "Clear History", onResume trim) đều đã bọc trong CÙNG 1 `historyMutex.withLock { }`** (field `Mutex()` duy nhất trên instance MainActivity) — di sản từ fix F-DATA-9/N-DATA-3/N-DATA-4 ở các sprint trước, audit ban đầu không nhận ra các fix đó đã giải quyết luôn race condition này. Không cần sửa thêm.

### ✅ F-DATA-6 — State kép: field `history` private không còn là nguồn sự thật — đã fix
`MyPreferences.kt:44-45,81` — field set qua setter nhưng `getHistory()` luôn đọc tươi từ SharedPreferences, bỏ qua field này; `saveHistory()` tạo instance `MyPreferences` mới thay vì dùng `this`.
P3 · S — **Đã fix**: xoá field `history` chết + tham số `context` thừa của `saveHistory()`, ghi thẳng qua `preferences` (field sẵn có của instance) bằng `preferences.edit().putString(KEY_HISTORY, ...).apply()`. Cập nhật 4 call site ở `MainActivity.kt` + toàn bộ test instrumented liên quan (`MainActivityTest.kt`, `MyPreferencesInstrumentedTest.kt`) bỏ tham số `context` không cần nữa. Verify: build thành công + smoke test history save/load/pin/search trên cả emulator lẫn device thật, dữ liệu không mất qua nhiều lần cài lại.

### F-DATA-7 — `appLanguage` dùng `.commit()` đồng bộ, phần còn lại dùng `.apply()` — xem xét, KHÔNG sửa
`MyPreferences.kt:56`, gọi trực tiếp từ click listener dialog (`LanguageHelper.kt:78`) → block Main Thread cho disk fsync trước khi restart app.
P2 · S — **Kết luận sau khi đọc kỹ ngữ cảnh gọi: `.commit()` ở đây là CHỦ ĐÍCH, không phải sơ suất.** Ngay sau khi set `appLanguage`, `restartApp()` gọi `killProcess()` — nếu dùng `.apply()` (ghi đĩa bất đồng bộ), giá trị ngôn ngữ mới có thể CHƯA kịp flush xuống đĩa trước khi process bị giết, khiến lựa chọn ngôn ngữ bị mất sau restart (mất dữ liệu thật, nghiêm trọng hơn nhiều so với 1 lần block Main Thread ngắn trong click handler một lần, không lặp lại). Đổi sang `.apply()` như đề xuất ban đầu sẽ là REGRESSION. Fix đúng chỗ là loại bỏ nhu cầu `killProcess()` hoàn toàn — xem F-INFRA-3 (giảm rủi ro race) và N-INFRA-4 (giải pháp triệt để: `AppCompatDelegate.setApplicationLocales()`, chưa làm trong sprint này).

### F-DATA-8 — Không có version/migration strategy cho schema `History` — ponytail, chủ động SKIP
`model/History.kt` không có version field, migration hiện dựa pattern thủ công `time.isNullOrEmpty()` (`HistoryAdapter.kt:73`).
P3 · S — **Chủ động không làm (YAGNI)**: pattern hiện tại (field mới nullable/có default, Gson tự điền default JVM cho JSON cũ thiếu field — xem ví dụ `isPinned`) đã đủ dùng cho model 4 field đơn giản này. Thêm hạ tầng version/migration là speculative cho nhu cầu chưa từng phát sinh (rollback, migration rẽ nhánh). Đã thêm comment `ponytail:` giải thích ngay tại `History.kt` để lần audit sau không đề xuất lại.

### ✅ F-DATA-9 — `equalsButton` tự huỷ chính coroutine của nó → history không bao giờ được lưu — **P0, đã fix**
`MainActivity.kt` (`equalsButton`): `binding.input.setText(formattedResult)` kích hoạt `TextWatcher.onTextChanged` → `updateResultDisplay()` → `calculationJob?.cancel()`. Vì `equalsButton` và `updateResultDisplay` dùng chung field `calculationJob`, lệnh này tự huỷ CHÍNH coroutine đang chạy của `equalsButton`. Việc huỷ có hiệu lực ngay khi coroutine resume ở `withContext` kế tiếp — tức là toàn bộ phần sau `setText` (set cursor, và quan trọng nhất là lưu history vào `historyMutex`/`prefs.saveHistory`) bị `CancellationException` nuốt mất mà không có dấu hiệu gì, 100% các lần bấm "=". Đây là regression từ chính fix Sprint 1 (thêm field `calculationJob` dùng chung để chống race condition) — trước đó bug không tồn tại vì chưa có cơ chế tự-huỷ này. Phát hiện khi verify tính năng N-CALC-5 (tap-to-recall từ history) trên emulator: tính "=" xong nhưng `royHISTORY` trong SharedPreferences luôn là `[]`.
Fix: bọc toàn bộ phần sau `setText` (kể cả chính lệnh `setText`) trong `withContext(NonCancellable) { ... }` — bọc từ *trước* `setText` là bắt buộc, bọc từ sau vẫn muộn vì exception ném ra ngay lúc resume sau khối `withContext(Main)` chứa `setText`, trước khi vào được block `NonCancellable`. Verify bằng log tạm (`Logger.d`) + đọc trực tiếp `shared_prefs/*.xml` qua `adb run-as`: history lưu đúng sau fix.

## Infra / Build / i18n / Testing

### F-INFRA-1 — 5 locale bị build script cắt khỏi APK dù UI cho phép chọn — **P0**
`resourceConfigurations` (`app/build.gradle:10`) chỉ khai báo 23 locale nhưng `res/values-*` có 28 locale thật (thiếu `cs`, `ml`, `or`, `pl`, `sr`). `sr` còn khai trong `res/xml/locales_config.xml` (per-app language Android 13+), và cả 5 locale có mặt trong `LanguageHelper.SUPPORTED_LANGUAGES` (bộ chọn ngôn ngữ trong app) → user chọn được nhưng resource đã bị build cắt, luôn thấy tiếng Anh.
P0 · S — thêm 5 locale còn thiếu vào `resourceConfigurations`, hoặc bỏ hẳn config filter thủ công (xem N-INFRA-3 App Bundle language split).

### F-INFRA-2 — AdMob Rewarded ID release = test ID Google
Trùng với F-AD-1, xem chi tiết ở mục Ad & VIP.

### ✅ F-INFRA-3 — `LanguageHelper.restartApp()` gọi `killProcess` ngay sau `startActivity` cùng process — đã fix (giảm rủi ro)
`LanguageHelper.kt:146-159` — `Process.killProcess` gần như ngay sau `startActivity` có thể giết Activity mới trước khi kịp `onCreate`, khiến app thoát đột ngột thay vì restart mượt khi đổi ngôn ngữ.
P1 · M — **Đã fix**: bọc `killProcess()` trong `Handler(Looper.getMainLooper()).postDelayed({...}, 300)` — nhường 1 nhịp cho hệ thống kịp khởi động Activity/task mới trước khi giết process. Không đổi sang `AppCompatDelegate.setApplicationLocales()` (N-INFRA-4) trong sprint này vì đó là thay đổi kiến trúc lớn hơn nhiều (bỏ hẳn cơ chế restart thủ công), để dành cho quyết định riêng. Verify trên device thật (`R9JN61LDLFJ`): đổi ngôn ngữ → app restart sạch (log `OpenCalcApplication onCreate` xác nhận process mới) → dấu thập phân đổi đúng theo locale mới (dấu phẩy cho vi-VN), không crash.

### ✅ F-INFRA-4 — ProGuard rules chứa hàng loạt rule "chết" cho lib không tồn tại trong dependencies — đã fix
`proguard-rules.pro:4-196` — rule cho butterknife, retrofit/retrofit2, facebook, realm, eventbus, rx, glide, jsoup, uCrop, dexter... không lib nào có trong `app/build.gradle`. Rõ ràng copy từ template cũ, gây khó audit/maintain.
P2 · S — **Đã fix**: xoá toàn bộ rule cho lib không có trong `dependencies {}` (đã đối chiếu từng dòng: không `libs/*.jar` nào tồn tại, xác nhận không có gì bị rule này bảo vệ thực sự cần thiết). File giảm từ 196 dòng còn ~22 dòng, chỉ giữ: in-app billing keep (chờ N-AD-1), block Gson thật (Signature/Annotation/Unsafe/TypeAdapterFactory/enum), và GMS advertising ID. Verify: build+lint+`testDevDebugUnitTest` PASS, app chạy bình thường trên emulator + device thật (ads, Settings, VIP screen).

### ✅ F-INFRA-5 — `-keep public class com.google.** {*;}` quá rộng, vô hiệu hoá phần lớn lợi ích R8 shrink/obfuscate — đã fix
`proguard-rules.pro:4` — giữ nguyên toàn bộ namespace `com.google.*` (Material, Gson, Play Review...) trong khi `AdmobApplovinWrapper:1.1.5` đã tự mang consumer ProGuard rule chính xác cho `com.google.android.gms.ads.**`/`com.applovin.**`/`com.google.android.ump.**`.
P2 · S — **Đã fix**: xoá hẳn rule `com.google.**` (và rule con trùng lặp `com.google.android.gms.**` từ template FCM cũ), dựa hoàn toàn vào consumer ProGuard rules tự mang theo của từng AAR (Material, Play Review, AdmobApplovinWrapper) — đây là cơ chế chuẩn của Android Gradle Plugin, không cần app khai lại thủ công. **Đây là thay đổi rủi ro cao nhất trong đợt dọn dẹp này** (build type nào cũng bật R8 kể cả debug) nên verify kỹ trên cả 2 thiết bị: emulator Pixel_10_Pro_XL và device thật `R9JN61LDLFJ` — app khởi động bình thường, tính toán đúng, PopupMenu/Settings/VIP screen (Material components + AdmobWrapper reflection) render đầy đủ không lỗi, App Open ad tải và đóng được bình thường trên cả 2 máy.

### F-INFRA-8 — `gradle.properties` hardcode `org.gradle.java.home` theo path máy dev cục bộ (phát hiện khi làm N-INFRA-1 CI pipeline)
`gradle.properties:10` = `/Users/loitran/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home` — path chỉ tồn tại trên máy hiện tại, sẽ khiến build lỗi ("Java home supplied is invalid") trên máy dev khác hoặc CI runner nếu gọi `./gradlew` trực tiếp không override. Đã fix ở tầm CI bằng cách luôn truyền `-Dorg.gradle.java.home="$JAVA_HOME"` trong `.github/workflows/ci.yml` (verify thực tế: build thành công khi override sang JDK khác hẳn path hardcode) — không sửa `gradle.properties` vì đây là override có chủ đích cho máy hiện tại (commit gần nhất "Update Gradle configuration with specific Java home"). Máy dev khác cần tự thêm dòng override tương ứng hoặc luôn gọi kèm `-D` như CI.
P3 · S (đã fix phần CI; phần "máy dev khác" chỉ cần biết cách override, không bắt buộc sửa thêm).

### ✅ F-INFRA-6 — Dependency khai báo trùng lặp — đã fix
`androidx.preference:preference-ktx:1.2.1` khai 2 lần (`app/build.gradle:139,144`).
P3 · S — **Đã fix**: xoá dòng trùng.

### ✅ F-INFRA-7 — `allowBackup="true"` không có `dataExtractionRules`/`fullBackupContent` loại trừ — đã fix
Android Auto Backup mặc định sẽ backup TOÀN BỘ SharedPreferences (bao gồm VIP prefs `grantedAtMs`/`userRedeemedOnce` và history) lên Google Drive của user, không loại trừ gì. Kết hợp với F-AD-9 (VIP secret dễ lộ), tăng thêm 1 vector khôi phục trạng thái VIP trái phép qua backup/restore giữa các thiết bị/tài khoản.
P2 · S — **Đã fix**: thêm `res/xml/backup_rules.xml` (API < 31, `fullBackupContent`) + `res/xml/data_extraction_rules.xml` (API 31+, `dataExtractionRules`, loại trừ cả `cloud-backup` lẫn `device-transfer`), cả 2 đều loại trừ `sharedpref` path `vip_screen_prefs.xml` (file `VipPrefs.kt` dùng, chứa `grantedAtMs`/`userRedeemedOnce`). Khai 2 attribute tương ứng trong `AndroidManifest.xml`. **Giới hạn đã biết**: chỉ loại trừ được state phía app (`VipPrefs`) — bản thân "nguồn sự thật" VIP nằm trong storage riêng của SDK `AdmobApplovinWrapper` (ngoài tầm kiểm soát của app) nên đây là giảm thiểu một phần, không triệt để; history/settings vẫn được backup bình thường (chấp nhận được, không nhạy cảm). Verify: build thành công, XML resource hợp lệ; chưa test thực tế luồng backup/restore qua `bmgr` (ngoài phạm vi sprint).

## P3 khác — ghi nhận, không khẩn (nguồn: claude session độc lập, chưa tự verify lại từng dòng)

- `ln`/`logten` domain check dùng `x.toInt()==0` thay vì `x==0.0` — trùng gốc với F-CALC-1, đã gộp.
- NaN lồng nhau gắn nhãn sai `syntax_error` thay vì `domain_error` (`Calculator.kt:152`).
- `sqrt(3)^2` không được làm sạch số thập phân như `sqrt(2)^2` do hack rounding trên `^` chỉ đối xứng một chiều (`Calculator.kt:272-280`).
- `History.time.toLong()` không try/catch (`HistoryAdapter.kt:77,87,104`) — crash risk nếu dữ liệu cũ/migrate lỗi format.
- `HistoryAdapter` dùng `notifyDataSetChanged()` toàn bộ list thay vì `notifyItem*` cho append/remove/clear (`HistoryAdapter.kt:43-58`) — performance, không phải correctness bug.
- Double-tap mở About/Settings có thể mở 2 lần chồng Activity (`MainActivity.kt:299-321`) — thiếu debounce.
