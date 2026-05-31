# 📊 AD SYSTEM — AdmobApplovinWrapper SDK v1.1.5

> **SDK:** `com.github.royt93:AdmobApplovinWrapper:1.1.5` (migrate từ `AdmobWrapper:1.1.1`)
> **Provider:** AppLovin MAX (default, `IS_ENABLE_ADMOB=false`) | AdMob (fallback)
> **Ngày migrate:** 2026-05-31
> **Status:** ✅ Đã migrate + 2 build variant xanh (devDebug, productionRelease)

---

## 1. TOUCHPOINT TABLE

| Slot | File hiện tại | Trigger | API mới | API cũ thay thế |
|---|---|---|---|---|
| Banner | `AboutActivity`, `SettingsActivity` | `onCreate` (loadBanner), lifecycle auto | `AdManager.loadBanner` + `bannerResume/Pause/Destroy` | (giữ nguyên từ 1.1.1) |
| Interstitial | `MainActivity` | preload `onCreate`; show trước About/Settings/**VIP** | `AdManager.loadInterstitial` / `showInterstitial` | (giữ nguyên) |
| App Open | `SplashActivity` + foreground resume | consent → `initSplashScreen` warmup → main; resume qua `ProcessLifecycleOwner` | `AdManager.initSplashScreen` + `registerAppOpenAdLifecycle` | (giữ nguyên) |
| Rewarded | `VipActivity`, nút "Xem QC → 3 ngày VIP" | user click | `AdManager.showRewarded`, fallback `showInterstitial` | (chưa có) → **mới** |
| Consent (UMP) | `SplashActivity` | `onCreate` | `AdManager.requestConsentInfoUpdate` | (chưa có) → **mới** |

### Ad IDs (buildConfigField trong `app/build.gradle`)

| Loại | AdMob Release | AdMob Debug/Test | AppLovin |
|------|---------------|------------------|----------|
| Banner | `…3807/4627557404` | `…2544/6300978111` | `86cdc9050561f316` |
| Interstitial | `…3807/4059747044` | `…2544/1033173712` | `4e8de0b36750ea6c` |
| App Open | `…3807/7338622864` | `…2544/9257395921` | `e9b4e3a8e16b96d4` |
| Rewarded | `…2544/5224354917` (⚠️ **test ID tạm**) | `…2544/5224354917` | `5423b3cd5889dcae` |

> ⚠️ **AdMob rewarded** đang dùng **test ID Google** cho cả debug+release theo quyết định "tạm dùng test ID" — **phải thay ID thật trước khi phát hành**.

---

## 2. KIẾN TRÚC

### Init (RApp.kt)
```
Application.onCreate → setupAd()
  AdSdkConfig(... rewarded, applovinSdkKey, vipKeySecret=AdKeys.VIP_SECRET,
              safety = if (DEBUG) AdSafetyLimits.TEST else AdSafetyLimits.UTILITY)
  AdManager.setConfig → earlyInit
  → (AppLovin) AppLovinSdk.initializeSdk { AdManager.init → registerAppOpenAdLifecycle (Main Thread) }
  → (AdMob)    MobileAds.initialize { AdManager.init → registerAppOpenAdLifecycle (Main Thread) }
```

### Splash + Consent (SplashActivity.kt)
```
requestConsentInfoUpdate(this) { canRequestAds ->
    initSplashScreen(this) { goToMain() }   // App Open warmup, KHÔNG show trên splash thuần
}
```

### VIP (feature/vip/)
- `VipActivity` — màn quản lý VIP (Activity-based; project không dùng Navigation/Fragment).
- `VipKeys` — whitelist key app-side (Base64), `lookupDays(input)`.
- `VipPrefs` — lưu `grantedAtMs` + cờ `userRedeemedOnce` (lib chưa expose `getVipByKeyGrantedAt`).
- `common/const/AdKeys` — `VIP_SECRET` (decode Base64 từ `BuildConfig.VIP_SECRET_B64`), `PRIVACY_POLICY_URL`.
- **Single-secret design:** mọi activate gọi `activateVipByKey(ctx, AdKeys.VIP_SECRET, days)` với `days` do whitelist app-side quyết định (key user gõ không cần bằng secret).
- **Auto-trial 1 ngày:** SDK tự grant khi cài mới (release), `InstallReferrer` chống abuse. App KHÔNG re-implement.

---

## 3. BẢNG SO SÁNH TRƯỚC / SAU

| Khía cạnh | Trước (1.1.1) | Sau (1.1.5) |
|---|---|---|
| Provider | AppLovin MAX | AppLovin MAX (không đổi) |
| Artifact | `AdmobWrapper` | `AdmobApplovinWrapper` (rename) |
| App Open | initSplashScreen | initSplashScreen + consent gate |
| Consent (UMP) | ❌ không có | ✅ `requestConsentInfoUpdate` ở Splash |
| Memory leak risk | banner auto-managed | + VipActivity cancel timer/animator ở onDestroy |
| Frequency cap | default | `TEST` (debug) / `UTILITY` (release) |
| Rewarded | ❌ không có | ✅ VipActivity |
| VIP | ❌ không có | ✅ VipActivity + auto-trial SDK built-in |
| Lifecycle | App Open auto | App Open auto + VIP screen lifecycle-safe |

---

## 4. POLICY CHECK (5/5 PASS)

| # | Rule | Kết quả | Dẫn chứng |
|---|---|---|---|
| 1 | Banner không che nội dung | PASS | banner dock trong About/Settings (không đổi) |
| 2 | App Open không show splash thuần | PASS | `SplashActivity.kt` consent→warmup→main |
| 3 | Interstitial có frequency cap | PASS | `RApp.kt` `AdSafetyLimits.UTILITY` (release) |
| 4 | Rewarded chỉ khi user bấm | PASS | `VipActivity.kt` `btnWatchAd` click |
| 5 | Không tự click / fake close | PASS | không có UI giả mạo |

## 5. MEMORY REVIEW (OK)

| Điểm | Kết luận | Dẫn chứng |
|---|---|---|
| Banner ref sau onDestroy | OK (lib auto) | `AboutActivity.onDestroy` `bannerDestroy` |
| Rewarded callback giữ ref | OK | `VipActivity` guard `isFinishing/isDestroyed` |
| App leak Activity qua callback | OK | `RApp` lib-managed lifecycle |
| VIP timer/animator | OK | `VipActivity.onDestroy` cancel + `removeAllUpdateListeners` |

---

## 6. THÔNG TIN CẦN USER CẤP (còn pending)

- [ ] **AdMob rewarded ID thật** (release) — hiện dùng test ID `…2544/5224354917`.
- [ ] Xác nhận **AppLovin rewarded ID** `5423b3cd5889dcae` còn active trên dashboard.
- [ ] (Tùy chọn) Test device ID + cấu hình EEA để QC consent form.
