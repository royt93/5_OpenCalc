# OpenCalc feature status

## ✅ Implemented

- Ad SDK migration to `com.github.royt93:AdmobApplovinWrapper:1.7.3`.
- AdMob is the active provider for debug and release variants.
- Debug builds use Google's sample App ID and test IDs for banner, interstitial, App Open, and rewarded.
- Release signing and AdMob credentials are read from the private `royt93/myKeyStore` checkout via
  `OPEN_CALC_SECRETS_DIR`; release tasks fail when any required value is missing or unsafe.
- Twelve AdMob QA test-device hashes are registered before SDK initialization so release QA cannot
  accidentally generate live clicks. Native ID is retained privately but Native ads are deferred.
- Host initialization follows the SDK 1.7.x two-call contract: `AdManager.setConfig()` then `AdManager.initialize()`; the host no longer initializes Google/AppLovin or registers App Open lifecycle itself.
- UMP consent runs before splash ad warmup and uses the SDK's single watchdog. Settings exposes an ad privacy entry point.
- App Open excludes `SplashActivity` and `VipActivity` with class references safe under R8.
- Banner lifecycle remains paired (`load/resume/pause/destroy`) in Settings and About.
- Settings navigation is never gated by an interstitial; About remains the single natural-transition interstitial placement.
- Rewarded VIP grant uses `AdManager.grantVipDays()` and grants only when `earned=true`.
- Rewarded ownership is single-source: wrapper auto-grant is disabled and the host callback grants
  exactly once, preventing duplicate VIP extensions across SDK upgrades.
- VIP redeem codes use `AdSdkConfig.vipRedeemCodes`; legacy plaintext activation remains disabled.
- VIP progress reads `AdManager.getVipGrantedAtMs()` instead of duplicating the timestamp in app preferences.
- Release keystore/config was pushed to private `royt93/myKeyStore` commit `a155d33`; remote and local
  keystore SHA-256 matched before the tracked app copy was removed.
- Verification: both debug flavors compile; JVM unit tests pass for both flavors; 51/51 widget and
  integration tests pass on the Android 17 `Pixel_10_Pro_XL` emulator; `lintDevDebug` has no new
  findings. Signed, minified `assembleProductionRelease` and APK signature verification pass.
- Live emulator smoke verified UMP resolution, AdMob initialization, Google sample requests marked
  as test traffic, and successful Interstitial/App Open/Rewarded loads. Evidence is in
  `doc/evidence/ad-sdk-smoke-emulator.md` and its companion screenshot.

## 🟡 In progress

- None.

## 📋 Picked

- None.

## ⏸️ Deferred

- UMP accept/reject variants, offline recovery, ad dismissal/click paths, and long-duration safety
  caps that require controlled network/region or elapsed time.
- AdMob Console European regulations/US states message publication, Data Safety review, and `app-ads.txt` verification.

## ❌ Skipped

- AppLovin provider configuration; this integration is AdMob-only.
- Native Ad integration; wrapper 1.7.3 has no Native API and no host placement has been designed.
- Google test IDs in release builds.

## 💭 Ideas

- Forward paid-ad revenue callbacks to the app analytics/MMP stack when one is selected.
- Replace static client-side VIP redeem codes with ECDSA tokens if stronger anti-sharing protection is needed.
- Review Google Play App Signing/upload-key status. The previous keystore and password were already
  tracked in the public app Git history, so moving them private does not revoke historical exposure.
