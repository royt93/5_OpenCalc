# Memory Leak Analysis — OpenCalc

> Phân tích lần 1: 2026-03-24 (trước migrate)
> Phân tích lần 2: 2026-03-24 (sau migrate sang AdmobWrapper SDK)

---

## Trạng thái hiện tại: ✅ CLEAN

Sau khi migrate sang **AdmobWrapper SDK v1.1.1**, toàn bộ memory leak từ custom `AdMobManager` đã được loại bỏ. SDK quản lý ad lifecycle nội bộ.

---

## Phân tích sau migrate

### ✅ RApp.kt — No leak
- `this` là Application context, tồn tại suốt lifecycle app
- AppLovin `initializeSdk` callback không giữ Activity reference
- `AdManager.init()` dùng `WeakReference<Activity>` nội bộ

### ✅ SplashActivity.kt — No leak
- `AdManager.initSplashScreen()` quản lý splash coroutine + timeout nội bộ
- `onDestroy()` remove `finishRunnable` khỏi `window.decorView`

### ✅ MainActivity.kt — No leak
- Không còn `interstitialListener = this` (SDK dùng callback, không cần clear)
- `backPressHandler.removeCallbacks(resetBackPressRunnable)` trong `onDestroy()`
- Named `Runnable` field, không dùng anonymous lambda

### ✅ AboutActivity.kt — No leak
- `AdManager.bannerResume(adView)` / `bannerPause` / `bannerDestroy` trong lifecycle
- `bannerDestroy()` xóa listener TRƯỚC destroy để ngăn orphaned impressions

### ✅ SettingsActivity.kt — No leak
- Tương tự AboutActivity — đầy đủ 3 lifecycle methods

---

## Các file KHÔNG có memory leak (không đổi từ lần 1)

| File | Lý do |
|------|-------|
| `BaseActivity.kt` | Standard lifecycle |
| `MyPreferences.kt` | Stateless |
| `Expression.kt` | Pure logic |
| `Calculator.kt` | Pure logic |
| `NumberFormatter.kt` | Stateless object |
| `History.kt` | Data class |
| `Themes.kt` | Static methods |
| `HistoryAdapter.kt` | RecyclerView managed |
| `MyTileService.kt` | System-managed Service |
| `Context.kt` | Extension functions |
| `Applovin.kt` | All commented out |
| `LanguageHelper.kt` | Stateless object |
| `Activity.kt` | Extension functions |

---

## Lịch sử

### Lần 1: Trước migrate (2026-03-24)
- 🔴 3 HIGH: `interstitialListener` giữ Activity, coroutine không cancel, unscoped CoroutineScope
- 🟡 4 MEDIUM: Handler postDelayed leaks, thiếu onDestroy
- 🟢 2 LOW: deprecated listener, Application reference
- **Đã fix toàn bộ** trước khi migrate

### Lần 2: Sau migrate (2026-03-24)
- **Toàn bộ code ad cũ đã xóa** (`sdkadbmob/AdMobManager.kt`)
- **SDK mới** quản lý lifecycle tự động
- **Kết quả:** ✅ CLEAN — không còn memory leak
