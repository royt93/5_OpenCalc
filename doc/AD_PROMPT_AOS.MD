# Prompt Template: Migrate hệ thống Ad sang `_AdmobWrapper` (Android native)

> Template dùng chung cho mọi project Android native. Trước khi đưa cho AI, **fill hết placeholder `<...>`** ở Section 0. VIP feature là **mandatory**, mọi project đều phải apply (không có cơ chế skip).

---

## 0. Bối cảnh — fill trước khi chạy

| Field | Giá trị |
|---|---|
| Project hiện tại (cwd, absolute path) | `<PROJECT_PATH>` |
| Lib reference (absolute path) | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper` |
| File spec gốc của lib | `/Users/loitran/AndroidStudioProjects/@mckimquyen/@playstore/@prodution/_AdmobWrapper/doc/AD.MD` |
| Lib coordinate + version | `com.github.royt93:AdmobApplovinWrapper:1.1.3` |
| Build variant phải xanh (debug) | `<assemble{Flavor}Debug>` |
| Build variant phải xanh (release) | `<assemble{Flavor}Release>` |
| Provider chính (active runtime) | `AppLovin MAX` |
| Provider fallback | `AdMob` (chỉ active khi `BuildConfig.IS_ENABLE_ADMOB=true`) |
| AppLovin SDK key | `e75FnQfS9XTTqM1Kne69U7PW_MBgAnGQTFvtwVVui6kRPKs5L7ws9twr5IQWwVfzPKZ5pF2IfDa7lguMgGlCyt` |
| AppLovin banner ID | `<hỏi user — dừng nếu thiếu>` |
| AppLovin interstitial ID | `<hỏi user — dừng nếu thiếu>` |
| AppLovin app open ID | `<hỏi user — dừng nếu thiếu>` |
| AppLovin rewarded ID | `<hỏi user — dừng nếu thiếu>` |
| AdMob banner ID | `<hỏi user — dừng nếu thiếu>` |
| AdMob interstitial ID | `<hỏi user — dừng nếu thiếu>` |
| AdMob app open ID | `<hỏi user — dừng nếu thiếu>` |
| AdMob rewarded ID | `<hỏi user — dừng nếu thiếu>` |
| VIP key 30 ngày (plain) | `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` |
| VIP key 3 ngày (plain) | `eQ7@93L0f!2Y2707xN04021993u0I#2aK` |
| Mức che giấu VIP key | Base64 hardcode (AI tự encode plain → Base64 khi compile vào code) |
| Privacy Policy URL | `<hỏi user — dừng nếu thiếu>` (sẽ nhúng vào VIP screen footer + consent dialog) |

**Rule cứng cho Section 0**:

1. **Cả 2 bộ ID (AppLovin + AdMob) đều phải đầy đủ** trước khi setup `AdSdkConfig`, dù chỉ AppLovin active runtime — vì khi flip `IS_ENABLE_ADMOB=true` không phải re-build template.
2. Bất kỳ field nào còn ghi `<hỏi user — dừng nếu thiếu>`: AI **phải hỏi user trong chat** rồi **dừng chờ trả lời**, KHÔNG tự fill placeholder, KHÔNG dùng test ID, KHÔNG bỏ qua.
3. AppLovin SDK key đã hardcode trong template này — chấp nhận do file PROMT.MD là internal. Khi compile vào project, key này phải đi qua `BuildConfig` (không hardcode trong source `.kt`).

---

## Cấm (áp dụng mọi project)

- Tự ý thêm ad slot mới ngoài các slot đã tồn tại trong code hiện tại.
- Tự ý refactor code không liên quan đến migrate ad.
- Tự ý nâng version lib hoặc đổi flag `IS_ENABLE_ADMOB`.
- Lặp lại đọc cùng 1 file > 1 lần với mục đích "double check" — review trên kết quả đã có, không re-read.
- Tự thêm trigger interstitial mới (vd "show sau mỗi action X") nếu code cũ chưa có.
- Tự fill ad ID bằng test ID hoặc placeholder — phải hỏi user.

---

## Step 1 — Đọc lib reference

- `ls` đệ quy `<LIB_PATH>` để nắm cấu trúc.
- Đọc `<LIB_PATH>/doc/AD.MD` (spec gốc) trước.
- Đọc sample app trong lib (`app/` hoặc `sample/`), đặc biệt các call site: `AdManager.setConfig`, `initialize`, `requestConsentInfoUpdate`, `awaitSplashComplete`, `loadBanner`, `showInterstitial`, `loadRewarded`, `showRewarded`, `activateVipByKey`, `AdSafetyLimits`.
- **`AdSafetyLimits` debug-friendly**: ở `BuildConfig.DEBUG=true`, dùng giá trị dễ dãi hơn để QC test nhanh. Giá trị đề xuất (verify field name chính xác từ lib ở step này, đổi tên field nếu lib dùng tên khác):

  | Field | Debug | Release |
  |---|---|---|
  | `interstitialMinIntervalSec` | `10` | `60` |
  | `appOpenMinIntervalSec` | `10` | `120` |
  | `interstitialMaxPerSession` | `999` | giữ default lib |
  | `appOpenMaxPerSession` | `999` | giữ default lib |

  Wire bằng `if (BuildConfig.DEBUG) AdSafetyLimits(...) else AdSafetyLimits()` trong `Application.onCreate`.

**Output bắt buộc**: 1 đoạn ≤ 10 dòng tóm tắt API surface của lib và lifecycle nó tự quản (App Open auto qua `ProcessLifecycleOwner`, banner auto pause/resume/destroy, frequency cap…).

## Step 2 — Audit hiện trạng project

Trả lời 5 câu (yes/no + dẫn chứng `file:line`):

1. Project đã apply `_AdmobWrapper` chưa? Hay vẫn còn SDK cũ (vd `AdMobManager`, `AdsHelper`, `MaxManager`…)?
2. `Application.onCreate` đang gọi gì để init ad?
3. Splash activity có gọi consent + App Open warmup chưa?
4. Activity nào đang load banner và interstitial bằng API nào (`AdView`, `InterstitialAd.load`, `MaxAdView`…)?
5. **Đã có VIP screen chưa?** Nếu có → AI sẽ **upgrade in-place** ở Step 10, KHÔNG tạo trùng. Nếu chưa → tạo mới. Mô tả UI hiện tại nếu có.

## Step 3 — Viết kế hoạch migrate vào `<PROJECT_PATH>/doc/AD.MD`

File đích: `<PROJECT_PATH>/doc/AD.MD` (tạo mới nếu chưa có).

Bắt buộc có các section:

**(1) Touchpoint table** — mỗi slot 1 dòng. Nếu slot chưa tồn tại trong project, ghi `(chưa có)` ở cột "API cũ thay thế":

| Slot | File hiện tại | Trigger | API mới | API cũ thay thế |
|---|---|---|---|---|
| Banner | `<host activity/fragment>` | `<onCreate / onViewCreated>` | `AdManager.loadBanner` | `<API cũ>` |
| Interstitial | `<host>` | `<trigger giữ nguyên>` | `AdManager.showInterstitial` | `<API cũ>` |
| App Open | Splash + foreground resume | warmup ở splash + `ProcessLifecycleOwner` cho resume | `AdManager.awaitSplashComplete` | `<API cũ hoặc (chưa có)>` |
| Rewarded | VIP screen, nút "Watch ad → 3 days VIP" | user click | `AdManager.loadRewarded` + `showRewarded`, fallback `showInterstitial` | `<API cũ hoặc (chưa có)>` |

**(2) Step-by-step migrate plan** — 6 bước (Setup, App Init + Consent, VIP UI, Banner, Interstitial, Rewarded). Mỗi bước nói rõ file edit, hàm thêm, dòng xoá.

**(3) Bảng so sánh trước/sau** — tối thiểu 8 dòng (Provider, Architecture, App Open, Consent, Memory leak risk, Frequency cap, VIP, Lifecycle).

**(4) Thông tin cần user cấp** — list rõ những gì còn thiếu: ad unit ID, test device ID, EEA test config, banner placement layout id…

## Step 4 — Lifecycle requirement (rule cứng, áp dụng mọi project)

- **App Open ở Splash**: gọi `requestConsentInfoUpdate` xong → `awaitSplashComplete` → mới navigate sang main. **KHÔNG** show App Open trên splash thuần (vi phạm AdMob policy "Disallowed interstitial implementations").
- **App Open khi quay lại foreground**: dùng cơ chế của lib (đã hook `ProcessLifecycleOwner`). KHÔNG tự subscribe `ON_START` lần thứ 2 trong project — tránh double-show.
- **Banner**: gắn vào layout container có sẵn (`<R.id.banner_container>` hoặc tương đương). KHÔNG gọi `pause()/resume()/destroy()` thủ công — lib tự lo.
- **Interstitial**: chỉ thay call site cũ, không thêm trigger mới. Tin tưởng `AdSafetyLimits` để chống click-tặc.
- **Rewarded**: chỉ load + show khi user bấm nút trong VIP screen.

## Step 5 — Phạm vi thay thế

Chỉ thay đúng các call site cũ. Trước khi xoá file/hàm SDK cũ, **grep verify** không còn ai dùng:

- Liệt kê file SDK cũ sẽ xoá (vd `AdMobManager.kt`, `AdsHelper.kt`…).
- Với mỗi file: grep tên class/hàm trong toàn project, xác nhận 0 reference, rồi mới xoá.
- Không tự thêm banner/interstitial/rewarded ở activity/fragment khác.

## Step 6 — Build & memory check (review chung toàn project)

- Chạy 2 build variant đã chốt ở Section 0 — cả 2 phải xanh.
- Memory leak: review thủ công 3 điểm (KHÔNG cần LeakCanary):
  - Activity host banner có giữ reference banner sau `onDestroy` không?
  - Fragment/Activity host rewarded có giữ reference callback không?
  - `Application` class có leak `Activity` qua `AdManager` callback không?
- Output: bảng "điểm review / kết luận / `file:line` dẫn chứng".

## Step 7 — Policy check (5 rule cụ thể, không hơn không kém)

| # | Rule | Cách verify |
|---|---|---|
| 1 | Banner không che nội dung tương tác | Đọc layout host banner, xác nhận banner ở dock bottom/top, không overlay nút bấm |
| 2 | App Open không show trên splash thuần — chỉ show khi warmup xong trước khi vào main, hoặc khi resume từ background | Đọc Splash activity + lifecycle hook |
| 3 | Interstitial không show liên tiếp dưới ngưỡng frequency cap | Tin `AdSafetyLimits` mặc định, ghi rõ giá trị dùng (debug vs release) |
| 4 | Rewarded chỉ trigger khi user bấm nút | Đọc VIP screen |
| 5 | Không tự click ad / không có UI giả mạo close button | Đọc layout banner |

Báo cáo: PASS/FAIL từng rule + `file:line` dẫn chứng.

## Step 8 — Consent (UMP)

- Gọi `AdManager.requestConsentInfoUpdate` ở Splash `onCreate`.
- Block load App Open + Banner + Interstitial cho tới khi consent resolved.
- Test scenario tối thiểu cần ghi nhận trong báo cáo (KHÔNG cần chạy thực):
  - Non-EEA: skip dialog, ad load bình thường.
  - EEA, user accept: dialog xong, ad load.
  - EEA, user deny: dialog xong, không load personalized ad (lib tự lo).

## Step 9 — First-install VIP grace (1 ngày) — SDK built-in

**VIP feature mandatory — không skip. SDK đã build-in từ v1.1.4.**

### 9.1 — Grace flow (KHÔNG re-implement trong app code)

`_AdmobWrapper` SDK (v1.1.4+) tự động grant 1 ngày VIP cho lần đầu cài:

- Trong `AdManager.init`, sau khi GAID resolve xong, SDK kiểm tra `appPreferences.isAddVIPMemberFirstInitSuccess()`.
- Nếu chưa grant + release build → query `InstallReferrerHelper.queryInstallBeginMs(context)` để lấy `installBeginMs` (thời điểm Play Store ghi nhận install).
- Nếu `installBeginMs` reliable + `installAgeMs in 0..86_400_000L` (≤ 24h) → `appPreferences.saveVipByKeyUntil(installBeginMs + 86_400_000L)` → set `isVIPMember = true`.
- Nếu `installBeginMs` không reliable hoặc quá 24h → fallback `saveVipByKeyUntil(now + 86_400_000L)` (provisional 1 ngày kể từ lần init đầu tiên).
- Set `addVIPMemberFirstInitSuccess()` flag → idempotent, các lần mở sau KHÔNG re-grant.
- Debug build: skip grace để dev không bị "VIP forever" làm rối QC test ad.

**Cấm**:
- KHÔNG lưu flag `pref_first_launch_vip_granted` thủ công ở app code.
- KHÔNG gọi `AdManager.activateVipByKey(...)` trong `Application.onCreate` để grant 1 ngày — SDK đã làm.
- KHÔNG thay đổi giá trị 86_400_000L (24h) — đó là contract mặc định.

### 9.2 — Verify ở runtime

- Sau lần init đầu tiên trên release build, log filter `###init` phải xuất hiện một trong:
  - `installBeginMs reliable, age=Xms → grant VIP until ...` (path chính)
  - `installBeginMs unreliable/missing → fallback grant 24h from now` (path fallback)
- `AdManager.isVipByKeyActive()` trả `true` trong 24h đầu.
- `AdManager.getVipByKeyExpiry()` ≈ `installBeginMs + 86_400_000L`.

### 9.3 — UI hiện grace entry trong VIP screen

VIP screen (Step 10) phải nhận diện grace entry và hiển thị label đặc biệt:
- Nếu `getVipByKeyExpiry() > 0` và `addVIPMemberFirstInitSuccess() == true` và user CHƯA tự nhập key bao giờ → đây là grace entry.
- Hiển thị label `R.string.vip_entry_first_install` ("🎁 Quà tặng cài đặt mới") thay vì label key thường.
- Lưu flag `pref_user_redeemed_key_at_least_once` trong `SharedPreferences` của app khi user redeem key thành công lần đầu — để phân biệt grace vs user-redeemed về sau.

## Step 10 — UI/UX VIP management screen (chi tiết, mandatory)

**VIP feature mandatory — không skip. Step 2 đã audit có hay chưa: nếu có thì upgrade in-place, nếu chưa thì tạo mới.**

### 10.1 — File structure (mandate)

```
app/src/main/java/<APP_PACKAGE>/
├── feature/vip/
│   ├── FVipManagement.kt         ← Fragment chính (UI + lifecycle)
│   ├── VipKeys.kt                ← Map<plainKey, durationDays> + validator + Base64 obfuscation
│   └── VipPrefs.kt               ← Lưu grantedAtMs riêng (vì lib chưa expose getVipByKeyGrantedAt)
├── common/const/
│   └── AdKeys.kt                 ← Wrapper cho BuildConfig + AdSdkConfig (optional, nếu user muốn tách hằng số)
└── res/
    ├── layout/f_vip_management.xml         ← Material 3 layout
    ├── drawable/
    │   ├── ic_workspace_premium.xml        ← crown icon (Material symbol)
    │   ├── ic_confirmation_number.xml      ← prefix icon EditText nhập key
    │   ├── ic_card_giftcard.xml            ← icon nút "mua VIP"
    │   ├── ic_restore.xml                  ← icon Restore Purchase
    │   ├── bg_vip_status_header_active.xml ← gradient gold
    │   └── bg_vip_status_header_free.xml   ← gradient xám
    ├── anim/
    │   ├── slide_in_bottom.xml
    │   └── pulse.xml (nếu impl bằng XML view animation, optional)
    └── values/
        ├── strings.xml          ← key VIP tiếng Việt (default)
        └── values-en/strings.xml
```

### 10.2 — `AdKeys` & VIP config

Lib AOS hiện đọc trực tiếp `AdSdkConfig` ở `MyApplication.onCreate`. Hằng số ad ID + AppLovin SDK key đã đi qua `BuildConfig` (set ở `app/build.gradle.kts` per buildType — không hardcode trong `.kt`).

VIP secret cũng phải qua `BuildConfig` hoặc `companion object` Base64-encoded. Pattern khuyến nghị (`AdKeys.kt`):

```kotlin
object AdKeys {
    // Plain ID đã đi qua BuildConfig → đọc bằng BuildConfig.APPLOVIN_BANNER_ID, ...
    // Section này chỉ centralize VIP secret + privacy policy URL.

    const val PRIVACY_POLICY_URL = BuildConfig.PRIVACY_POLICY_URL  // ← thêm field này vào build.gradle.kts

    /** VIP secret key dùng chung cho AdSdkConfig.vipKeySecret (single-secret design của lib). */
    val VIP_SECRET: String by lazy {
        // Base64 của plain key 30-ngày ở Section 0
        String(android.util.Base64.decode(VIP_SECRET_B64, android.util.Base64.NO_WRAP))
    }

    private const val VIP_SECRET_B64 = "<AI tự encode plain key 30-ngày khi compile>"
}
```

**Usage:** `AdSdkConfig(... vipKeySecret = AdKeys.VIP_SECRET, ...)` ở `MyApplication.onCreate`.

### 10.3 — Cấu trúc UI (bắt buộc đủ 13 thành phần)

> **Note về single-entry**: Lib AOS hiện chỉ lưu 1 VIP entry tại 1 thời điểm (không multi-entry như Flutter SDK). Spec dưới đây đã adapt: row #12 từ "list active entries" → "single active VIP card". Khi lib bổ sung multi-entry sẽ revisit.

| # | Thành phần | Detail (Android Kotlin idiom) |
|---|---|---|
| 1 | Status header / Hero card | `MaterialCardView` + `bg_vip_status_header_*.xml` (gradient gold khi active, xám khi free). Crown icon `ic_workspace_premium` + text 2 dòng:<br>• Active: `getString(R.string.vip_active)` + `getString(R.string.vip_until, formattedDate)`<br>• Free: `getString(R.string.vip_free_user)` |
| 2 | Activation date row | TextView bind `SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(grantedAtMs))`. `visibility = if (active) VISIBLE else GONE`. `grantedAtMs` đọc từ `VipPrefs.getGrantedAtMs()` |
| 3 | Expiry date row | TextView bind từ `AdManager.getVipByKeyExpiry()` qua cùng formatter. Visibility theo VIP active |
| 4 | Progress bar (elapsed) | Material 3 `LinearProgressIndicator`. **Formula bắt buộc**: `progress = ((now − grantedAt) / (expiry − grantedAt) × 100).toInt().coerceIn(0, 100)`. **3 mốc trên timeline ngang**: `start = grantedAtMs` (mép trái, 0%), `end = expiryMs` (mép phải, 100%), `current = now` (vị trí thanh fill). Bar **rỗng lúc vừa kích hoạt → đầy dần đến hết hạn** (elapsed semantic). Tuyệt đối KHÔNG đảo formula thành `remaining/total` (đầy → rỗng) — UX confusing. Update mỗi giây qua `CountDownTimer.onTick`. Edge case `total ≤ 0` (clock skew / đã expire) → default `progress = 100` (đầy = đã hết hạn). Chỉ show khi VIP active |
| 5 | Countdown text | Format `Xd HHh MMm SSs` (ví dụ `12d 03h 24m 15s`). Update mỗi giây qua `CountDownTimer(remainingMs, 1000L)`, gọi `cancel()` trong `onDestroyView`. **KHÔNG** dùng `Handler.postDelayed` (rule 10.5) |
| 6 | EditText nhập key VIP | `TextInputLayout` + `TextInputEditText` với `inputType="textCapCharacters"` (uppercase auto), `startIconDrawable=@drawable/ic_confirmation_number`, `endIconMode="clear_text"`. Nút "Kích hoạt" (`MaterialButton`) → trim + uppercase input → lookup `VipKeys.lookupDays(input)` → nếu match: gọi `AdManager.activateVipByKey(context, input, days)` → SDK xử lý dialog success/fail có sẵn. Hoặc dùng API convenience `AdManager.showVipByKeyDialog(activity)` nếu chỉ cần dialog input mặc định của lib (đã có verify/success/failed dialog) |
| 7 | Button "Xem quảng cáo → 3 ngày VIP" | `MaterialButton` icon `ic_play_circle`. Click → `AdManager.showRewarded(activity) { earned -> if (earned) grantViaRewarded() }`. Fallback nếu rewarded load fail (callback `onAdFailedToShow`): `AdManager.showInterstitial(activity, "vip_fallback") { _ -> grantViaRewarded() }`. `grantViaRewarded()` = activate 3 ngày bằng key 3-ngày (Base64-decoded từ `VipKeys.VIP_3D_KEY`). Pulse animation #1 wrap nút này |
| 8 | Buy 30-day VIP (disabled) | `MaterialButton` style `OutlinedButton.Icon` + `app:icon="@drawable/ic_card_giftcard"`, `isEnabled=false`, text `getString(R.string.vip_buy_30d_locked)` ("Mua VIP 30 ngày — sắp ra mắt"). Wait IAP integration |
| 9 | Buy 90-day VIP (disabled) | Tương tự #8, label `vip_buy_90d_locked` |
| 10 | Buy 1-year VIP (disabled) | Tương tự #8, label `vip_buy_1y_locked` |
| 11 | Buy lifetime VIP (disabled) | Tương tự #8, label `vip_buy_lifetime_locked` |
| 12 | Active VIP card (single-entry) | `MaterialCardView` show key label (grace label nếu first-install, hoặc "VIP X ngày" cho user-redeemed), days/hours remaining, expires date, button revoke (gọi `AdManager.clearVipByKey()` sau dialog confirm). Visibility GONE khi free user. Icon riêng cho grace entry: `🎁` Unicode hoặc `ic_first_install_gift` |
| 13 | Restore Purchase (disabled) | `MaterialButton` outlined + `app:icon="@drawable/ic_restore"`, `isEnabled=false`, text `vip_restore_locked` ("Khôi phục mua hàng — sắp ra mắt"). Placeholder cho IAP tương lai |
| — | Button "Thu hồi tất cả VIP" | `MaterialAlertDialogBuilder` (Material You) confirm → `AdManager.clearVipByKey()` → `VipPrefs.clearGrantedAtMs()` → refresh UI. `isEnabled = AdManager.isVipByKeyActive()` (disable khi free user) |
| — | Toolbar badge "VIP" trên `MainActivity` | `Chip` hoặc TextView overlay trên toolbar. Active: chip vàng `0xFFFFD60A` text "VIP". Free: GONE. Refresh trong `onResume` của MainActivity hoặc qua callback từ FVipManagement khi state đổi |
| — | Privacy Policy footer | `TextView` clickable, `setOnClickListener { startActivity(Intent(ACTION_VIEW, Uri.parse(AdKeys.PRIVACY_POLICY_URL))) }` |

**Yêu cầu chung**:
- Toàn bộ dialog dùng `MaterialAlertDialogBuilder` (Material You / Material 3) — KHÔNG dùng `AlertDialog.Builder` cũ.
- Sau khi `activateVipByKey` thành công → ngay lập tức:
  1. `VipPrefs.saveGrantedAtMs(System.currentTimeMillis())`
  2. `VipPrefs.markUserRedeemed()` (phân biệt với grace entry — Step 9.3)
  3. Refresh UI (call `bindUi()` hoặc post qua `ValueAnimator`)
  4. Trigger confetti animation #5 + haptic feedback (xem 10.4)
- ViewBinding bắt buộc — KHÔNG findViewById raw. Set `binding = null` ở `onDestroyView`.

#### Reference implementation cho row #4 (Progress bar — elapsed semantic)

```kotlin
/**
 * 3 mốc trên timeline ngang [grantedAt ─── expiresAt]:
 *   start   = grantedAtMs   (mép trái, progress 0)
 *   end     = expiresAtMs   (mép phải, progress 100)
 *   current = now           (vị trí thanh fill)
 *
 * progress = (current − start) / (end − start) × 100
 * Bar RỖNG lúc vừa kích hoạt, ĐẦY DẦN đến lúc hết hạn.
 */
private fun computeElapsedProgress(grantedAtMs: Long, expiresAtMs: Long, nowMs: Long): Int {
    val total = expiresAtMs - grantedAtMs
    if (total <= 0L) return 100  // expired / clock skew → render đầy
    val elapsed = nowMs - grantedAtMs
    return ((elapsed.toDouble() / total.toDouble()) * 100.0)
        .toInt()
        .coerceIn(0, 100)
}

// Trong onTick của CountDownTimer:
binding?.progressVip?.setProgressCompat(
    computeElapsedProgress(grantedAtMs, expiresAtMs, System.currentTimeMillis()),
    /* animated = */ true,
)
```

Visualization với VIP 30 ngày:
```
T+0      [░░░░░░░░░░░░░░░░░░░░░░░░░░░]   0% — vừa kích hoạt
T+1d     [█░░░░░░░░░░░░░░░░░░░░░░░░░░]   3.3%
T+15d    [██████████████░░░░░░░░░░░░░]   50%
T+29d    [██████████████████████████░]   96.7%
T+30d    [███████████████████████████]  100% — expired
```

### 10.4 — Animation (bắt buộc đủ 5)

| # | Animation | Cách impl (Android idiom, KHÔNG `lateinit` cho thứ có thể null) | Trigger |
|---|---|---|---|
| 1 | Pulse cho nút "Watch ad" | `ObjectAnimator.ofPropertyValuesHolder(view, scaleX 1.0→1.05, scaleY 1.0→1.05).apply { duration = 1600L; repeatMode = ObjectAnimator.REVERSE; repeatCount = ObjectAnimator.INFINITE }`. Lưu reference nullable `var pulseAnimator: ObjectAnimator? = null` | `onResume` start, `onPause` cancel |
| 2 | Slide-in từ dưới khi mở fragment | 3 section share 1 `ValueAnimator(0f, 1f)` duration 1100ms, mỗi section một `Interval` riêng (0.0–0.4, 0.3–0.7, 0.6–1.0) tính qua `fraction`. Apply `translationY = (1 - localFraction) * 200f` + `alpha = localFraction`. Hoặc đơn giản hơn: `R.anim.slide_in_bottom` qua `setEnterAnim` của Navigation Component, kèm `onCreateAnimation` fallback | `onViewCreated` |
| 3 | Crown icon shimmer | `ObjectAnimator.ofFloat(crown, View.ROTATION, -5f, 5f).apply { duration = 3000L; repeatMode = REVERSE; repeatCount = INFINITE }`. HOẶC `ShimmerFrameLayout` (Facebook lib) với `shimmer-android:0.5.0` nếu user accept thêm dependency | `onResume` start, `onPause` cancel |
| 4 | Number count-up cho countdown | `ValueAnimator.ofInt(prevMinute, currentMinute).apply { duration = 400L; addUpdateListener { ... } }` chạy KHI minute đổi (không mỗi giây) — tránh build hot loop. Track `lastMinute: Int?` trong fragment | Khi `CountDownTimer.onTick` chạm phút mới |
| 5 | Confetti khi activate thành công | Option A (recommend): lib `nl.dionsegijn:konfetti-xml:2.0.5` — `KonfettiView` overlay, `binding.viewKonfetti.start(party)`. Option B (no extra dep): `ObjectAnimator` scale crown 0→1.2→1.0 + alpha 0→1 (1000ms) + custom particle view. Kèm `view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)` (API 30+) hoặc fallback `Vibrator.vibrate(VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE))` (API 26+) | Sau khi `activateVipByKey` HOẶC `onUserEarnedReward` thành công |

Bonus (nice-to-have): ripple effect mặc định cho button (Material 3 sẵn), gradient background động cho status header bằng `AnimationDrawable`.

### 10.5 — Memory leak rule riêng cho VIP screen (KHÔNG dùng các pattern sau)

- **KHÔNG `Handler.postDelayed` cho countdown** — bắt buộc `CountDownTimer`, gọi `cancel()` trong `onDestroyView`. `Handler.postDelayed` capture context dễ leak fragment.
- **KHÔNG giữ rewarded callback dạng `object : RewardedListener` mà capture `this` của fragment** — clear callback ở `onDestroyView` HOẶC dùng `WeakReference<Fragment>`. Pattern an toàn:
  ```kotlin
  AdManager.showRewarded(activity) { earned ->
      if (!isAdded || _binding == null) return@showRewarded
      if (earned) onRewardEarned()
  }
  ```
- **KHÔNG start `ObjectAnimator`/`ValueAnimator`/`Animation` trong `onCreate`/`onResume` mà không cancel ở `onPause`/`onDestroyView`** — mọi animator phải `.cancel()` + `removeAllUpdateListeners()` trong `onDestroyView`.
- **KHÔNG dùng anonymous `Runnable` post lên `View.postDelayed` mà không `removeCallbacks`** ở `onDestroyView`.
- **KHÔNG truy cập `binding.X` sau `onDestroyView`** — bắt buộc `_binding: FVipManagementBinding? = null` + `private val binding get() = _binding!!` pattern, set `_binding = null` cuối `onDestroyView`. Mọi callback async phải check `_binding == null → return`.
- **KHÔNG dùng `lateinit var` cho field có thể null hoặc cần cleanup** (animator, timer, EditText) — bắt buộc nullable + `?.cancel()` an toàn.
- Pattern dispose chuẩn:
  ```kotlin
  override fun onDestroyView() {
      countDownTimer?.cancel(); countDownTimer = null
      pulseAnimator?.cancel(); pulseAnimator = null
      shimmerAnimator?.cancel(); shimmerAnimator = null
      entryAnimator?.cancel(); entryAnimator = null
      countUpAnimator?.cancel(); countUpAnimator = null
      confettiAnimator?.cancel(); confettiAnimator = null
      _binding = null
      super.onDestroyView()
  }
  ```

### 10.6 — VIP key whitelist + special entries

`VipKeys.kt`:

```kotlin
object VipKeys {
    // Base64 của plain key ở Section 0 — AI tự encode khi compile
    private const val VIP_30D_B64 = "<AI encode plain key 30-ngày>"
    private const val VIP_3D_B64  = "<AI encode plain key 3-ngày>"

    val VIP_30D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_30D_B64, android.util.Base64.NO_WRAP))
    }
    val VIP_3D_KEY: String by lazy {
        String(android.util.Base64.decode(VIP_3D_B64, android.util.Base64.NO_WRAP))
    }

    /** Plain key (đã decode) → số ngày. Dùng để validate input từ user. */
    private val KEY_TO_DAYS: Map<String, Int> by lazy {
        mapOf(
            VIP_30D_KEY to 30,
            VIP_3D_KEY  to 3,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. Auto trim + uppercase trước khi lookup. */
    fun lookupDays(rawInput: String): Int? =
        KEY_TO_DAYS[rawInput.trim()]  // case-sensitive — nếu cần case-insensitive, normalise cả map
}
```

**Special entries** (auto-managed bởi lib, app-side chỉ render):

| Source | Cách detect | UI label (string resource) |
|---|---|---|
| First-install grace 1 ngày | `appPreferences.isAddVIPMemberFirstInitSuccess() == true` AND `VipPrefs.userRedeemedAtLeastOnce() == false` AND `expiry ≤ installBeginMs + 24h + tolerance` | `R.string.vip_entry_first_install` ("🎁 Quà tặng cài đặt mới") |
| User redeem qua TextField (30/3 ngày) | `VipPrefs.userRedeemedAtLeastOnce() == true` | Format `getString(R.string.vip_entry_redeemed, daysActivated)` ("VIP %d ngày") |

`VipPrefs.kt` (helper riêng cho VIP screen — vì lib chưa expose `grantedAt`):

```kotlin
class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    fun saveGrantedAtMs(ms: Long) = sp.edit().putLong("granted_at_ms", ms).apply()
    fun getGrantedAtMs(): Long = sp.getLong("granted_at_ms", 0L)
    fun clearGrantedAtMs() = sp.edit().remove("granted_at_ms").apply()

    fun markUserRedeemed() = sp.edit().putBoolean("user_redeemed_once", true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean("user_redeemed_once", false)
}
```

**Lý do tách `VipPrefs`**: lib chỉ persist `vipByKeyUntil` (expiry), không persist `grantedAt`. Để vẽ progress bar elapsed-semantic cần cả 2. Khi nào lib bổ sung `getVipByKeyGrantedAt()` → xoá `VipPrefs` + đọc trực tiếp lib.

### 10.7 — Translations (string resources)

Thêm vào `app/src/main/res/values/strings.xml` (vi default, project convention):

```xml
<!-- VIP -->
<string name="vip_active">VIP đang kích hoạt</string>
<string name="vip_free_user">Người dùng miễn phí</string>
<string name="vip_until">Đến %1$s</string>
<string name="vip_activated_at">Kích hoạt: %1$s</string>
<string name="vip_expires_at">Hết hạn: %1$s</string>
<string name="vip_remaining">%1$dd %2$02dh %3$02dm %4$02ds</string>
<string name="vip_redeem_label">Nhập mã VIP</string>
<string name="vip_redeem_hint">VD: LOITP_30</string>
<string name="vip_redeem_button">Kích hoạt</string>
<string name="vip_watch_ad_3d">Xem quảng cáo → 3 ngày VIP</string>
<string name="vip_buy_30d_locked">Mua VIP 30 ngày — sắp ra mắt</string>
<string name="vip_buy_90d_locked">Mua VIP 90 ngày — sắp ra mắt</string>
<string name="vip_buy_1y_locked">Mua VIP 1 năm — sắp ra mắt</string>
<string name="vip_buy_lifetime_locked">Mua VIP trọn đời — sắp ra mắt</string>
<string name="vip_restore_locked">Khôi phục mua hàng — sắp ra mắt</string>
<string name="vip_revoke_all_button">Thu hồi VIP</string>
<string name="vip_revoke_all_confirm_title">Xác nhận thu hồi</string>
<string name="vip_revoke_all_confirm_message">Bạn có chắc muốn thu hồi VIP và quay về tài khoản Free?</string>
<string name="vip_entry_first_install">🎁 Quà tặng cài đặt mới</string>
<string name="vip_entry_redeemed">VIP %1$d ngày</string>
<string name="vip_verifying_title">Đang xác minh…</string>
<string name="vip_success_title">Thành công</string>
<string name="vip_failed_title">Thất bại</string>

<!-- Common -->
<string name="privacy_policy">Chính sách bảo mật</string>
<string name="cancel">Huỷ</string>
<string name="confirm">Xác nhận</string>
<string name="ok">OK</string>
```

Và `app/src/main/res/values-en/strings.xml` (English fallback) — copy nguyên cấu trúc, dịch sang tiếng Anh. Các locale khác (zh/ja/ko/es/fr…) bổ sung sau khi user yêu cầu.

### 10.8 — Entry point từ Toolbar / Settings

```kotlin
// MainActivity (toolbar badge + button vào VIP screen)
class MainActivity : AppCompatActivity() {

    private fun bindToolbarVipBadge() {
        val active = AdManager.isVipByKeyActive()
        binding.chipVipBadge.isVisible = active
        binding.chipVipBadge.text = getString(R.string.vip_active)
    }

    private fun openVipScreen() {
        // Navigation Component:
        findNavController(R.id.navHostFragment).navigate(R.id.action_main_to_vip)
        // Hoặc fragment transaction:
        // supportFragmentManager.beginTransaction()
        //     .setCustomAnimations(R.anim.slide_in_bottom, ...)
        //     .replace(R.id.container, FVipManagement())
        //     .addToBackStack("vip")
        //     .commit()
    }

    override fun onResume() {
        super.onResume()
        bindToolbarVipBadge()  // refresh khi user back từ VIP screen
    }
}
```

Trong layout toolbar (`AppBarLayout` của MainActivity):

```xml
<com.google.android.material.chip.Chip
    android:id="@+id/chipVipBadge"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/vip_active"
    android:textColor="@android:color/black"
    app:chipBackgroundColor="@color/vip_gold"
    app:chipIcon="@drawable/ic_workspace_premium"
    android:visibility="gone" />

<ImageButton
    android:id="@+id/btnVipMenu"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@drawable/ic_workspace_premium"
    android:background="?selectableItemBackgroundBorderless"
    android:contentDescription="@string/vip_active" />
```

Settings entry: thêm preference `Preference` trong settings screen (nếu project có) trỏ tới `FVipManagement` qua intent/navigation.

### 10.9 — Che giấu VIP key

Plain key ở Section 0 — KHÔNG hardcode plain trong file `.kt`. AI phải Base64-encode trước rồi hardcode chuỗi Base64 vào `companion object` / `private const val` (xem `VipKeys.kt` ở 10.6). KHÔNG thêm AES/NDK/ProGuard string-encrypt trừ khi user yêu cầu — Base64 là mức đã thống nhất với risk tradeoff: dễ reverse nhưng đủ chặn user thường peek decompiled APK.

## Step 11 — Self-review & cập nhật `<PROJECT_PATH>/doc/AD.MD`

Review duy nhất 1 lần, KHÔNG lặp 3 lần. Checklist:

- [ ] Mọi slot ad đã thay (grep tên SDK cũ không còn match)
- [ ] 2 build variant ở Section 0 đều xanh
- [ ] 5 policy rule đều PASS
- [ ] Consent đã wire ở Splash
- [ ] VIP 1-day auto-trial dùng SDK built-in (Step 9.1) — KHÔNG tự re-implement bằng flag thủ công
- [ ] VIP screen có **đủ 13 thành phần UI** (10.3) + **đủ 5 animation** (10.4) + **0 vi phạm memory leak rule** (10.5)
- [ ] File structure đúng (10.1): `FVipManagement.kt`, `VipKeys.kt`, `VipPrefs.kt`, layout `f_vip_management.xml`
- [ ] VIP key đã Base64-encode trong `VipKeys.kt` (10.9) — grep plain key trong source `.kt` phải 0 match
- [ ] Translations vi + en đầy đủ (10.7) — grep hard-coded user-facing string trong VIP screen phải 0 match
- [ ] Toolbar badge "VIP" hiện đúng theo `AdManager.isVipByKeyActive()` (10.8)
- [ ] Privacy Policy footer + URL từ `AdKeys.PRIVACY_POLICY_URL` (`BuildConfig`)
- [ ] Bảng so sánh trước/sau đã viết trong `doc/AD.MD`
- [ ] List "info cần user cấp" đã viết trong `doc/AD.MD`

Nếu có item FAIL → fix → check lại item đó (không re-check item đã PASS).

## Step 12 — Báo cáo cuối (1 lần, ≤ 20 dòng)

Format cố định, **chỉ 2 section**:

```
### Migrate Result
- Build: <variantDebug>=<PASS|FAIL>, <variantRelease>=<PASS|FAIL>
- Slots migrated: <N/M>
- Policy: <5/5 hoặc list rule fail>
- Memory review: <OK | list điểm nghi ngờ với file:line>
- VIP UI: <13/13 components, 5/5 animations | list missing>
- Files changed: <count>, lines +<X> -<Y>

### Open questions for user
- <key/id còn thiếu cụ thể>
- <decision pending nếu có>
```

KHÔNG lặp lại "3 round". Step 11 checklist PASS hết → dừng. Có FAIL thì fix tới khi PASS rồi mới báo cáo.
