# Memory Leak Analysis — OpenCalc

> Ngày phân tích: 2026-03-24
> Phân tích toàn bộ 19 file Kotlin trong project
> ✅ **Đã fix toàn bộ — 2026-03-24**

---

## Tổng kết nhanh

| Mức độ | Số lượng | Trạng thái |
|--------|----------|------------|
| 🔴 HIGH | 3 | ✅ Fixed |
| 🟡 MEDIUM | 4 | ✅ Fixed |
| 🟢 LOW | 2 | ✅ Fixed / Accepted |

---

## 🔴 HIGH — ✅ FIXED

### 1. `AdMobManager.interstitialListener` giữ Activity reference

**File:** `AdMobManager.kt` + `MainActivity.kt`

**Vấn đề:** `AdMobManager` là singleton, `interstitialListener = this` giữ Activity vĩnh viễn.

**✅ Fix:** Thêm `onDestroy()` vào `MainActivity` để clear `interstitialListener = null`.

---

### 2. `AdMobManager.initSplashScreen()` — Coroutine không cancel + giữ Activity

**File:** `AdMobManager.kt`

**Vấn đề:** `CoroutineScope(Dispatchers.Default)` không có `SupervisorJob`, `collectLatest` trên `SharedFlow` suspend mãi mãi, capture trực tiếp Activity.

**✅ Fix:**
- Thêm `splashJob: Job?` để track & cancel coroutine
- Dùng `SupervisorJob()` trong scope
- Dùng `withContext(Dispatchers.Main)` thay vì nested `CoroutineScope`
- Dùng `activity.applicationContext` thay vì Activity trực tiếp
- Cancel `splashJob` sau khi ad loaded/dismissed

---

### 3. `RApp.setupAdmob()` — Unscoped CoroutineScope

**File:** `RApp.kt`

**✅ Fix:** Thay `CoroutineScope(Dispatchers.IO)` bằng `CoroutineScope(Dispatchers.IO + SupervisorJob())`.

---

## 🟡 MEDIUM — ✅ FIXED

### 4. `SplashActivity.goToMain()` — postDelayed giữ Activity sau finish

**File:** `SplashActivity.kt`

**✅ Fix:** Dùng named `finishRunnable`, thêm `onDestroy()` với `removeCallbacks(finishRunnable)`.

---

### 5. `MainActivity.onBackPressed()` — Handler postDelayed

**File:** `MainActivity.kt`

**✅ Fix:** Dùng named `resetBackPressRunnable` + `backPressHandler` field, remove callbacks trong `onDestroy()`.

---

### 6. `AdMobManager` — Nhiều Handler.postDelayed rải rác

**File:** `AdMobManager.kt`

**✅ Fix:** Thay tất cả `Handler(Looper.getMainLooper())` bằng 1 `handler` field duy nhất trong object.

---

### 7. `MainActivity` thiếu `onDestroy()`

**File:** `MainActivity.kt`

**✅ Fix:** Thêm `onDestroy()` để clear `interstitialListener` và remove Handler callbacks.

---

## 🟢 LOW — Accepted

### 8. `Activity.hideNavigationBar()` / `showNavigationBar()` listener

**File:** `Activity.kt` — API deprecated từ API 30, các function này hiện không được gọi trong app. **Risk thấp, chấp nhận.**

### 9. `AdMobManager.application` — Application reference

**File:** `AdMobManager.kt` — Application context an toàn để giữ lâu dài. **Chấp nhận.**

---

## Các file KHÔNG có memory leak

`MyPreferences.kt`, `Expression.kt`, `Calculator.kt`, `NumberFormatter.kt`, `History.kt`, `Themes.kt`, `HistoryAdapter.kt`, `MyTileService.kt`, `Context.kt`, `Applovin.kt` (commented out), `LanguageHelper.kt`, `AboutActivity.kt` ✅, `SettingsActivity.kt` ✅
