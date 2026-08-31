# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tổng quan

OpenCalc — máy tính Android (fork của `Darkempire78/OpenCalc`), được rebrand thành `com.mckimquyen.opencal` và tích hợp quảng cáo. Kotlin + View Binding, không dùng Compose. minSdk 24, target/compileSdk 37, Java 17.

## Lệnh build & chạy

```bash
# Build (có 4 variant: {dev,production} × {Debug,Release})
./gradlew assembleDevDebug              # build nhanh khi phát triển
./gradlew assembleProductionRelease     # build phát hành (ký bằng keystore.jks)

./gradlew installDevDebug               # cài lên thiết bị/emulator
./gradlew clean
./gradlew lint                          # Android Lint

./gradlew testDevDebugUnitTest          # unit test (JVM, app/src/test)
./gradlew testDevDebugUnitTest --tests "com.mckimquyen.opencal.feature.vip.VipMathTest"  # 1 class
./gradlew connectedDevDebugAndroidTest  # instrumented test (cần device/emulator, app/src/androidTest)

# APK output: app/build/outputs/apk/... đặt tên theo
# {applicationId}{buildType}_{versionName}_{versionCode}.apk
```

- Unit test nằm ở `app/src/test` (JVM thuần, không cần device — vd `VipMathTest`, `CalculatorTest`, `ExpressionTest`); instrumented test ở `app/src/androidTest` (cần device/emulator — vd `MainActivityTest`, `VipActivityInstrumentedTest`). Logic thuần (không đụng Android framework) nên tách ra object riêng như `VipMath` để test được bằng unit test JVM thay vì instrumented test.
- Keystore release: `app/keystore.jks`; mật khẩu/alias nằm trong `gradle.properties` (`KS_PW`, `KS_ALIAS`).
- Nâng version: sửa `versionCode` (dạng `yyyyMMdd`) và `versionName` (dạng `yyyy.MM.dd`) trong `app/build.gradle`.

## Kiến trúc

Toàn bộ code ở `app/src/main/java/com/mckimquyen/opencal/`. Package chính:

- **`ui/`** — Activities: `SplashActivity` → `MainActivity` (màn hình chính), `SettingsActivity`, `AboutActivity`.
- **`helper/Expression.kt`** + **`ext/Calculator.kt`** — lõi tính toán (xem dưới).
- **`db/MyPreferences.kt`** — bọc SharedPreferences; lưu cả cấu hình lẫn lịch sử (history serialize bằng Gson).
- **`model/`** — `History`, `Themes` (chọn theme + day/night), `adt/HistoryAdapter` (RecyclerView).
- **`ext/`** — extension functions: `Activity`, `Context`, `Applovin`; biến lỗi toàn cục `division_by_0`, `domain_error`, `syntax_error` nằm ở `Calculator.kt`.
- **`util/LanguageHelper.kt`** — đổi locale ứng dụng trong `attachBaseContext`.
- **`common/const/AdKeys.kt`** — hằng số nhạy cảm (VIP secret, Privacy Policy URL), đọc từ `BuildConfig` (Base64), không hardcode plain string.
- **`sv/MyTileService.kt`** — Quick Settings Tile (Android N+), tap mở `MainActivity`.
- **`feature/vip/`** — màn hình VIP (xem mục riêng bên dưới).
- **`RApp.kt`** (Application), **`BaseActivity.kt`** (base cho mọi Activity).

### Luồng tính toán (quan trọng nhất)

Đầu vào hiển thị dùng ký hiệu thân thiện (`×`, `÷`, `√`, `π`, `sin⁻¹`...) khác với biểu thức tính toán nội bộ. Luồng:

1. `MainActivity` lắng nghe `TextWatcher` trên `binding.input`, mọi thay đổi gọi `updateResultDisplay()`. Nút `=` gọi `equalsButton()`.
2. `Expression().getCleanExpression(...)` chuẩn hoá chuỗi: đổi ký hiệu, chèn dấu `*` ngầm (`addMultiply`), bọc `√`→`sqrt(...)`, `!`→`factorial(...)`, xử lý `%` theo kiểu "thân thiện" (không thuần toán học, xem `getPercentString`), tự đóng ngoặc thiếu.
3. `Calculator().evaluate(expr, isDegreeModeActivated)` — parser đệ quy xuống (recursive-descent) tự viết, trả `Double`. Lỗi báo qua `Double.NaN`/`Infinity` **cộng với** ba biến cờ toàn cục ở trên — luôn reset cả ba về `false` trước mỗi lần evaluate.
4. `NumberFormatter.format(...)` định dạng kết quả theo dấu thập phân/phân nhóm của locale.

Tính toán nặng chạy trên `Dispatchers.Default`, cập nhật UI bằng `withContext(Dispatchers.Main)`. `decimalSeparatorSymbol`/`groupingSeparatorSymbol` lấy từ locale và được luồn xuyên suốt — không hardcode `.` hoặc `,`.

### Hệ thống quảng cáo (AdmobApplovinWrapper SDK)

Tích hợp qua thư viện ngoài `com.github.royt93:AdmobApplovinWrapper` (package `com.roy.sdkadbmob.AdManager`). **Đọc `doc/AD_PROMPT_AOS.MD`** để biết chi tiết — đó là guide chính thức cho ad IDs, touchpoints và lifecycle (`doc/AD.MD` là ghi chú nội bộ, không phải nguồn chân lý API).

- Provider chọn bằng cờ build `BuildConfig.IS_ENABLE_ADMOB` — SDK chọn **ĐÚNG 1** provider (`GmsBridge.createProvider`: `if (useAdmob) AdMobProvider else AppLovinProvider`), **không chạy song song, không tự failover** khi AdMob lỗi/bị khoá. Hiện `true` → AdMob chính; AppLovin vẫn cấu hình đủ ID/key để sẵn sàng nếu sau này **chủ động** đổi cờ (thao tác thủ công + rebuild), không phải mediation/fallback tự động. Quyết định nhánh init trong `RApp.setupAd()`.
- Mọi ad ID và SDK key được khai báo dưới dạng `buildConfigField` trong `app/build.gradle` (test ID của Google cho debug, ID thật cho release; AppLovin dùng chung 1 bộ ID cho cả debug/release vì không có khái niệm test-unit-id riêng).
- SDK 1.7.x **tự gộp** `earlyInit` + provider init + đăng ký App Open lifecycle vào bên trong `AdManager.initialize()` — không còn API `registerAppOpenAdLifecycle` riêng, không cần tự bọc `Handler(Looper.getMainLooper()).post {...}`.
- `AdManager.paidEventListener` **phải set trong `RApp.setupAd()` (`Application.onCreate`)**, không set từ Activity — SDK tự xoá listener khi Activity "chủ sở hữu" lúc set bị destroy.
- `AdManager.setCurrentActivity(this)` gọi tập trung ở `BaseActivity.onResume()` — mọi Activity con thừa hưởng, không tự gọi lại riêng lẻ.
- Touchpoint: App Open ở `SplashActivity`; Banner ở `About`/`Settings` dùng `autoManageLifecycle=true` (mặc định) — SDK tự hook resume/pause/destroy qua `ActivityLifecycleCallbacks`, **không** tự gọi `bannerResume/Pause/Destroy` (double lifecycle); Interstitial ở `MainActivity` (preload trong `onCreate`, show trước khi mở About/Settings).

### Hệ thống VIP (`feature/vip/`)

VIP do chính `AdmobApplovinWrapper` SDK quản lý (auto-trial 1 ngày khi cài mới, expiry, `AdManager.activateVipByKey`); app chỉ render UI + validate key + rewarded grant, không tự chấm VIP.

- SDK dùng thiết kế **multi-code** qua `AdSdkConfig.vipRedeemCodes` (map key → số ngày, không phải single-secret nữa). App validate `input` qua `VipKeys.lookupDays()` (bản sao app-side của cùng map) trước, rồi gọi `AdManager.activateVipByKey(context, input, days)` — truyền thẳng key user gõ kèm số ngày đã resolve, KHÔNG phải `AdKeys.VIP_SECRET` (giá trị đó chỉ dùng làm `vipKeySecret` — HMAC chống-tamper SharedPreferences nội bộ SDK, việc khác hẳn).
- `AdKeys.VIP_SECRET` (secret HMAC chống-tamper prefs nội bộ SDK) và các mã redeem công khai trong `VipKeys` (`VIP_30D_B64`, `VIP_3D_B64`) là **giá trị độc lập, cố ý không trùng nhau** — lộ mã redeem không được kéo theo lộ secret bảo vệ prefs. Cả 3 chỉ lưu dạng Base64 trong source — mức che giấu đã thống nhất là "khó đọc trực tiếp, không chống decompile hoàn toàn".
- `VipPrefs` lưu thêm `grantedAtMs` (SDK không expose) để vẽ progress bar kiểu "elapsed" (0% lúc kích hoạt → 100% lúc hết hạn), và cờ `userRedeemedOnce` để phân biệt trial cài đặt vs key do user tự nhập.
- Logic số học thuần (progress %, đếm ngược, gia hạn không rút ngắn) tách riêng vào `VipMath` (object, không phụ thuộc Android) để unit-test trên JVM — theo pattern này khi thêm logic VIP mới, ưu tiên viết vào `VipMath` thay vì trực tiếp trong `VipActivity`.
- `VipActivity` có `CountDownTimer`/`ObjectAnimator`/`ValueAnimator` — nhớ null-out + cancel ở `onDestroy` (theo rule memory-leak chung của project).

## Quy ước & lưu ý

- **Đa ngôn ngữ**: ~30 locale trong `res/values-*`. Có giới hạn `resourceConfigurations` trong `build.gradle`. Strings mới phải externalize (không hardcode). Dịch quản lý qua Weblate.
- **Đổi tên hàm View handler**: nhiều hàm trong `MainActivity` (vd `addButton`, `degreeButton`, `scientistModeSwitchButton`) được gọi qua `android:onClick` trong layout XML — đổi tên/chữ ký phải sửa cả XML, và **không xoá tham số `view`** dù không dùng.
- **Memory leak**: dự án từng được audit memory leak (xem `doc/memory_leak.md`); `MainActivity.onDestroy` gỡ `Handler` callback — giữ pattern này khi thêm Handler/listener.
- **build types đều bật ProGuard/R8** (kể cả debug); release thêm `minifyEnabled` + `shrinkResources`. Khi thêm thư viện cần reflection, cập nhật `app/proguard-rules.pro`.
- Tài liệu nội bộ nằm ở `doc/` (tiếng Việt). Theo `~/.claude/CLAUDE.md`, giữ tài liệu tính năng cập nhật sau mỗi quyết định.
- Branch mặc định PR là `main`; branch phát triển hiện tại là `dev`.
