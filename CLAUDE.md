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

# APK output: app/build/outputs/apk/... đặt tên theo
# {applicationId}{buildType}_{versionName}_{versionCode}.apk
```

- Không có unit test / instrumented test nào (`app/src/test` và `app/src/androidTest` rỗng) dù `build.gradle` có khai báo `testInstrumentationRunner`. Đừng giả định có test để chạy.
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
- **`RApp.kt`** (Application), **`BaseActivity.kt`** (base cho mọi Activity).

### Luồng tính toán (quan trọng nhất)

Đầu vào hiển thị dùng ký hiệu thân thiện (`×`, `÷`, `√`, `π`, `sin⁻¹`...) khác với biểu thức tính toán nội bộ. Luồng:

1. `MainActivity` lắng nghe `TextWatcher` trên `binding.input`, mọi thay đổi gọi `updateResultDisplay()`. Nút `=` gọi `equalsButton()`.
2. `Expression().getCleanExpression(...)` chuẩn hoá chuỗi: đổi ký hiệu, chèn dấu `*` ngầm (`addMultiply`), bọc `√`→`sqrt(...)`, `!`→`factorial(...)`, xử lý `%` theo kiểu "thân thiện" (không thuần toán học, xem `getPercentString`), tự đóng ngoặc thiếu.
3. `Calculator().evaluate(expr, isDegreeModeActivated)` — parser đệ quy xuống (recursive-descent) tự viết, trả `Double`. Lỗi báo qua `Double.NaN`/`Infinity` **cộng với** ba biến cờ toàn cục ở trên — luôn reset cả ba về `false` trước mỗi lần evaluate.
4. `NumberFormatter.format(...)` định dạng kết quả theo dấu thập phân/phân nhóm của locale.

Tính toán nặng chạy trên `Dispatchers.Default`, cập nhật UI bằng `withContext(Dispatchers.Main)`. `decimalSeparatorSymbol`/`groupingSeparatorSymbol` lấy từ locale và được luồn xuyên suốt — không hardcode `.` hoặc `,`.

### Hệ thống quảng cáo (AdmobWrapper SDK)

Tích hợp qua thư viện ngoài `com.github.royt93:AdmobWrapper` (package `com.roy.sdkadbmob.AdManager`). **Đọc `doc/AD.MD`** để biết chi tiết — đó là source-of-truth cho ad IDs, touchpoints và lifecycle.

- Provider mặc định chọn bằng cờ build `BuildConfig.IS_ENABLE_ADMOB` (hiện `false` → dùng AppLovin MAX; `true` → AdMob). Quyết định nhánh init trong `RApp.setupAd()`.
- Mọi ad ID và SDK key được khai báo dưới dạng `buildConfigField` trong `app/build.gradle` (test ID của Google cho debug, ID thật cho release).
- `registerAppOpenAdLifecycle` **bắt buộc gọi trên Main Thread** (đã có `Handler(Looper.getMainLooper()).post {...}` — đừng bỏ).
- Touchpoint: App Open ở `SplashActivity`; Banner ở `About`/`Settings` (kèm `bannerResume/Pause/Destroy` theo lifecycle); Interstitial ở `MainActivity` (preload trong `onCreate`, show trước khi mở About/Settings).

## Quy ước & lưu ý

- **Đa ngôn ngữ**: ~30 locale trong `res/values-*`. Có giới hạn `resourceConfigurations` trong `build.gradle`. Strings mới phải externalize (không hardcode). Dịch quản lý qua Weblate.
- **Đổi tên hàm View handler**: nhiều hàm trong `MainActivity` (vd `addButton`, `degreeButton`, `scientistModeSwitchButton`) được gọi qua `android:onClick` trong layout XML — đổi tên/chữ ký phải sửa cả XML, và **không xoá tham số `view`** dù không dùng.
- **Memory leak**: dự án từng được audit memory leak (xem `doc/memory_leak.md`); `MainActivity.onDestroy` gỡ `Handler` callback — giữ pattern này khi thêm Handler/listener.
- **build types đều bật ProGuard/R8** (kể cả debug); release thêm `minifyEnabled` + `shrinkResources`. Khi thêm thư viện cần reflection, cập nhật `app/proguard-rules.pro`.
- Tài liệu nội bộ nằm ở `doc/` (tiếng Việt). Theo `~/.claude/CLAUDE.md`, giữ tài liệu tính năng cập nhật sau mỗi quyết định.
- Branch mặc định PR là `main`; branch phát triển hiện tại là `dev`.
