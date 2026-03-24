# 📊 AD SYSTEM — AdmobWrapper SDK v1.1.1

> **SDK:** `com.github.royt93:AdmobWrapper:1.1.1`
> **Provider:** AppLovin MAX (default) | AdMob (fallback)
> **Ngày tích hợp:** 2026-03-24
> **Status:** ✅ Đã tích hợp

---

## 1. THÔNG TIN CẤU HÌNH

### Ad IDs

| Loại | AdMob (Release) | AdMob (Debug/Test) | AppLovin |
|------|-----------------|-------------------|----------|
| Banner | `ca-app-pub-3612191981543807/4627557404` | `ca-app-pub-3940256099942544/6300978111` | `86cdc9050561f316` |
| Interstitial | `ca-app-pub-3612191981543807/4059747044` | `ca-app-pub-3940256099942544/1033173712` | `4e8de0b36750ea6c` |
| App Open | `ca-app-pub-3612191981543807/7338622864` | `ca-app-pub-3940256099942544/9257395921` | `e9b4e3a8e16b96d4` |
| Reward | — | — | `5423b3cd5889dcae` |

### Keys

| Key | Value |
|-----|-------|
| AdMob App ID | `ca-app-pub-3612191981543807~9558426247` |
| AppLovin SDK Key | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` |
| Default Provider | `IS_ENABLE_ADMOB = false` → **AppLovin MAX** |

---

## 2. TOUCHPOINT MAPPING

| # | Loại Ad | Activity | Trigger | File |
|---|---------|----------|---------|------|
| 1 | **App Open** | `SplashActivity` | `AdManager.initSplashScreen(this) { goToMain() }` | `SplashActivity.kt:29` |
| 2 | **Banner** | `AboutActivity` | `AdManager.loadBanner(context, container, tvLabel)` | `AboutActivity.kt:114` |
| 3 | **Banner** | `SettingsActivity` | `AdManager.loadBanner(context, container, tvLabel)` | `SettingsActivity.kt:85` |
| 4 | **Interstitial** | `MainActivity` | Load: `AdManager.loadInterstitial(this)` ở `onCreate`<br>Show: `AdManager.showInterstitial(this) { ... }` trước About/Settings | `MainActivity.kt:95,281,293` |

> ⚠️ Reward Ad ID có sẵn (`5423b3cd5889dcae`) nhưng **chưa gắn touchpoint** — có thể thêm trong tương lai.

---

## 3. KIẾN TRÚC TÍCH HỢP

### Luồng khởi tạo (RApp.kt)

```
Application.onCreate()
  → AdManager.setConfig(adConfig)          // Gắn config ngay (Main Thread)
  → AdManager.earlyInit(this)              // Khởi tạo AdSafety session clock
  → AppLovinSdk.initializeSdk {            // Init AppLovin SDK
      → AdManager.init(context, config)    // Fetch GAID, VIP check, auto-preload
    }
```

### Banner Lifecycle (AboutActivity, SettingsActivity)

```
onCreate()  → adView = AdManager.loadBanner(...)
onResume()  → AdManager.bannerResume(adView)
onPause()   → AdManager.bannerPause(adView)
onDestroy() → AdManager.bannerDestroy(adView)
```

### Interstitial Flow (MainActivity)

```
onCreate()        → AdManager.loadInterstitial(this)  // Preload ngầm
openAbout/Settings → AdManager.showInterstitial(this) { success → navigate }
```

---

## 4. BẢO VỆ ADSAFETY (TỰ ĐỘNG)

SDK tự quản lý 7 lớp bảo vệ:

| # | Filter | Giới hạn |
|---|--------|----------|
| 1 | Pause vi phạm | 30→60→120→240 phút |
| 2 | Session minimum | 10 giây trước ad đầu tiên |
| 3 | Session Cap | Max 6 Fullscreen/session |
| 4 | Hourly Cap | Max 3 Fullscreen/giờ |
| 5 | Daily Cap | Max 5 Fullscreen/ngày |
| 6 | Throttle | ≥ 60 giây giữa 2 Fullscreen |
| 7 | CTR Anomaly | CTR > 10% → cờ Suspicious |

---

## 5. FILES ĐÃ THAY ĐỔI

| File | Thay đổi |
|------|----------|
| `build.gradle` | AdmobWrapper 1.1.1, AppLovin IDs, `IS_ENABLE_ADMOB=false` |
| `AndroidManifest.xml` | Thêm AppLovin SDK key meta-data |
| `RApp.kt` | `AdManager.setConfig → earlyInit → init` với AppLovin |
| `SplashActivity.kt` | `AdManager.initSplashScreen` |
| `MainActivity.kt` | `AdManager.loadInterstitial/showInterstitial`; xóa `InterstitialAdListener` |
| `AboutActivity.kt` | `AdManager.loadBanner` + lifecycle helpers |
| `SettingsActivity.kt` | `AdManager.loadBanner` + lifecycle helpers |
| `sdkadbmob/AdMobManager.kt` | **Xóa hoàn toàn** |
