# Prompt Template: Migrate hệ thống Ad sang `AdmobApplovinWrapper` (Android native)

> 🎯 **ĐÂY LÀ TÀI LIỆU BÀN GIAO CHÍNH DUY NHẤT** bạn (partner) cần để bắt đầu tích hợp SDK — nhưng các
> thao tác release thật (dashboard CMP, Data Safety, `app-ads.txt`, legal sign-off) vẫn cần dịch vụ/tài
> khoản bên ngoài được liên kết trong file này; đọc file này không thay thế các bước đó. Các link tham
> chiếu bên trong đều là URL công khai (GitHub/JitPack/nhà mạng) hoặc file trong chính project của bạn.
>
> 📖 **File này gồm 3 phần** — nếu bạn là kỹ sư đối tác đọc tay, chỉ cần (A) + (B), **KHÔNG PHẢI** (C):
> - **(A) Quickstart** — Bước 1–5 (KHÔNG có "Bước 6") + **Phụ lục A** (API surface, tra cứu khi code) +
>   **Phụ lục B** (code mẫu copy-paste `MyApplication`/`SplashActivity`/manifest) — cả 2 phụ lục nằm ngay
>   sau Bước 5, đọc liền mạch. Đủ để tích hợp chạy được.
> - **(B) Checklist release + prompt template AI** — nằm vật lý trong khối markdown "PHẦN B" phía dưới,
>   nhưng **bắt buộc đọc với người**: **Step 8** (consent/provider-console — release gate), Step 9–12, mục
>   "⚠️ Security Checklist cho Integrator", "⚠️ Việc cần làm — Bật xin phép quảng cáo", và **Phụ lục C**
>   (đọc `getDiagnostics()`). Chỉ có **Section 0 + Step 1–7** bên trong khối này (xem (C)) là dành riêng
>   cho AI — phần còn lại của (B) vẫn là hướng dẫn cho người.
> - **(C) AI-migration prompt template** — CHỈ Section 0 + Step 1–7 (nằm trong khối "PHẦN B"), dành cho
>   công cụ AI tự động migrate (không bắt buộc đọc nếu bạn đọc tay và đã làm xong Phần A).
>
> Kết quả QC thiết bị thật + phạm vi đã kiểm thử: xem ngay mục "🔍 Phạm vi đã kiểm thử" bên dưới (thuộc
> Phần A, không phải phụ lục riêng).
>
> 🙋 **Nếu bạn KHÔNG rành kỹ thuật:** kéo thẳng xuống mục **"⚠️ Việc cần làm — Bật xin phép quảng cáo"**
> gần cuối file — nhưng đọc kỹ cảnh báo ngay đầu mục đó trước: phần AppLovin **cần dev xác nhận đã sửa
> đúng 1 dòng code trước**, không phải "chỉ cần bấm dashboard là xong" cho cả 2 network.
>
> 🔗 **Code snippet trong file này KHÔNG đầy đủ 100% (cố ý rút gọn cho dễ đọc) — repo SDK là PUBLIC,
> bạn luôn xem được code THẬT, chạy được, đầy đủ context tại GitHub nếu snippet không đủ:**
> - `MyApplication.kt` thật: https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/src/main/java/com/roy/admobwrapper/MyApplication.kt
> - `SplashActivity.kt` thật: https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/src/main/java/com/roy/admobwrapper/SplashActivity.kt
> - `MainActivity.kt` thật (banner/interstitial/rewarded call site): https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/src/main/java/com/roy/admobwrapper/MainActivity.kt
> - `app/build.gradle.kts` thật (buildConfigField/manifestPlaceholders đầy đủ mọi ID): https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/build.gradle.kts
> - Toàn bộ source SDK (nếu cần đọc hành vi implementation thật thay vì đoán từ mô tả): https://github.com/royt93/AdmobApplovinWrapper/tree/main/admob-wrapper-sdk/src
>
> Bị kẹt ở đâu mà đọc file này không ra → **mở link tương ứng ở trên trước khi hỏi lại người khác.**

---

# PHẦN A — HƯỚNG DẪN TÍCH HỢP (5 phút, cho người)

> **Giải nghĩa nhanh vài từ viết tắt dùng xuyên suốt file** (lần đầu thấy SDK ads có thể chưa quen):
> **UMP** = User Messaging Platform, thư viện của Google tự hiện popup "đồng ý xem quảng cáo cá nhân
> hoá không?" cho user ở vùng luật yêu cầu. **GDPR** = luật bảo vệ dữ liệu của Liên minh Châu Âu (bắt
> buộc phải hỏi consent trước khi hiện ads cá nhân hoá). **EEA/UK/CH** = Khu vực Kinh tế Châu Âu/Anh/Thuỵ
> Sĩ — vùng địa lý áp dụng GDPR. **Consent** = quyết định user đồng ý/từ chối cho việc cá nhân hoá quảng
> cáo. Không cần nhớ chi tiết luật — SDK tự xử lý, bạn chỉ cần làm đúng **Bước 4** bên dưới.

> SDK version **tối thiểu 1.6.2** — mốc cố định, KHÔNG đổi ở release sau (đã vá 1 bug nặng của 1.6.1: VIP có thể
> được cấp thầm lặng — user dán token đúng lúc app vừa mở, SDK chưa init xong — không báo lại cho màn hình nếu
> Activity khác bị destroy trong lúc chờ). Đừng dùng 1.6.1 nếu app có màn VIP-by-token. ⚠️ **KHÔNG dùng tag `1.6.0`**:
> tag đó đã bị bỏ, không có artifact trên JitPack. Tag **mới nhất hiện có** luôn xem tại
> https://jitpack.io/#royt93/AdmobApplovinWrapper — số version nào khác trong file này (kể cả ở phần lịch sử audit
> phía dưới) tự nhận là "bản mới nhất/hiện tại" thì đều có thể đã lỗi thời, lấy link trên làm chuẩn duy nhất ·
> Coordinate chung: `com.github.royt93:AdmobApplovinWrapper:<LATEST_TAG>` (JitPack — **bản `gms`**: đã gồm AdMob +
> AppLovin + UMP.
> Bản `nongms` cho Huawei/non-Google phải **build từ source**, JitPack không phát hành).

## 🔔 ĐANG DÙNG BẢN CŨ? Đọc mục này trước (cập nhật 2026-08-29)

> `1.6.2` ở trên là **sàn tối thiểu tuyệt đối** (thấp hơn = chắc chắn dính bug VIP nặng đã nêu) — KHÔNG
> phải "bản khuyến nghị". Mục ngay dưới đây là ngưỡng **thực tế nên dùng**: luôn là bản MỚI NHẤT trên
> JitPack, không dừng lại ở 1.6.2 dù nó đã qua ngưỡng sàn.

**Nếu app của bạn đang chạy một bản SDK phát hành trước 2026-08-29, hãy nâng lên bản mới nhất trên
JitPack.** Hai lý do dưới đây không phải "tính năng mới cho vui" — chúng đang ảnh hưởng app bạn ngay
lúc này, và **không lỗi nào tự lộ ra**: app không crash, log không báo, chỉ số liệu và tuân thủ sai
trong im lặng.

### 1. Tín hiệu "đừng bán dữ liệu của tôi" của user MỸ không tới AppLovin (bản cũ)

SDK đọc tín hiệu opt-out ở hai ô mà **Google UMP không bao giờ ghi vào** (theo đúng tài liệu chính
thức của Google, UMP chỉ ghi `IABGPP_GppSID` + `IABGPP_HDR_GppString`). Hệ quả trên bản cũ: user ở
California/Texas/… bấm opt-out trong form của UMP, nhưng AppLovin và các mạng chạy dưới nó **không hề
nhận được** — vẫn phục vụ như user chưa opt-out. Không có log cảnh báo nào.

- **Ai bị ảnh hưởng:** app có user Mỹ **và** dùng AppLovin (kể cả khi AppLovin chỉ là một mạng trong
  waterfall của AdMob).
- **Ai không:** app chỉ phát hành ngoài Mỹ, hoặc AdMob thuần không mediation (AdMob vẫn tự áp lựa chọn
  của user qua UMP).
- **Bản mới sửa thế nào:** đọc tín hiệu từ chuỗi tổng, đúng theo spec IAB. Không xác định được thì
  **không đoán** — ghi log lỗi thấy được ở bản release và áp mặc định an toàn.
- **Bạn cần làm gì:** chỉ nâng version. Không phải sửa code. Nếu muốn chắc chắn tuyệt đối, tự truyền
  lựa chọn từ CMP của bạn: `AdSdkConfig.applovinDoNotSell = true/false` — giá trị này luôn được ưu
  tiên hơn phần tự đọc.

### 2. Số liệu doanh thu AppLovin gửi sang MMP mang "độ chính xác" SAI (bản cũ)

Mọi revenue event từ AppLovin đều gắn cứng `precision = "ESTIMATED"`, kể cả khi AppLovin trả về giá
trị chính xác thật. Nếu bạn dựng LTV/ROAS trên trường này (Adjust/AppsFlyer/Firebase), **số của bạn
đang sai** — và sai theo kiểu không thể tự phát hiện từ phía app.

- **Bản mới:** `precision` là giá trị thật cho cả AdMob lẫn AppLovin (`PRECISE` / `PUBLISHER_PROVIDED`
  / `ESTIMATED` / `UNKNOWN`), và `AdRevenueEvent` có thêm `creativeId`, `networkPlacement`,
  `latencyMs` (đều nullable, không phá code cũ) — đủ để tra ngược đúng creative khi bị policy-strike.
- **Lưu ý khi nâng:** biểu đồ LTV/ROAS sẽ **đổi hình** vì trước đó số bị gắn nhãn sai. Đó là số ĐÚNG
  lần đầu, không phải hồi quy.

### 3. Những thứ khác đổi theo, nên biết trước khi nâng

| Đổi gì | Bạn thấy gì |
|---|---|
| Quảng cáo đã tải quá **1 giờ** bị coi là hết hạn, tự tải lại | Ít lần "show ra màn hình trắng/thất bại" hơn. Nếu app bạn chỉ preload một lần lúc mở app, trước đây có thể **mất hẳn** interstitial/rewarded cả phiên sau một lần show lỗi — bản mới tự phục hồi |
| Điểm rủi ro (Shield Score) không còn chấm oan user mới | User vừa cài, lỡ bấm 1 lần vào ad, trước đây bị gắn "rủi ro trung bình" ngay lần mở app đầu |
| Dialog xin phép quảng cáo không đóng được bằng chạm ra ngoài | Trước đây đóng được (dù code định chặn) ⇒ app đi tiếp trong trạng thái chưa có quyết định |
| Đổi giờ máy không còn cướp mất trial 1 ngày của user | Máy có đồng hồ lệch nhiều không còn bị mất trial vĩnh viễn |

> **Tóm tắt hành động:** nâng lên bản mới nhất trên
> https://jitpack.io/#royt93/AdmobApplovinWrapper — không cần sửa code. Sau khi nâng, nếu app có user
> Mỹ, kiểm tra lại dashboard MMP xem `precision` đã đa dạng chưa (không còn 100 % `ESTIMATED`).

---

## 🔍 Phạm vi đã kiểm thử & giới hạn đã biết (đọc trước khi tin bất kỳ dòng nào bên dưới)

> Mục đích: minh bạch chính xác cái gì đã verify cho tag mới nhất, cái gì chưa, để bạn tự quyết định
> mức độ tin cậy trước khi tích hợp vào production. Không tô hồng, không giấu giới hạn. Quy trình audit
> dưới đây chạy lại cho mỗi tag — luôn đối chiếu https://jitpack.io/#royt93/AdmobApplovinWrapper +
> CI status của đúng tag bạn đang dùng để biết kết quả thật sự, đừng tin số cụ thể còn sót lại ở đâu đó
> trong file này.

**Đã kiểm thử / audit:**

- **Code SDK**: nhiều vòng audit độc lập bằng AI riêng biệt (codex/gemini/claude CLI, không chia sẻ
  ngữ cảnh với nhau), mỗi bên tự đọc source và verify chéo trước khi kết luận. Verdict gần nhất (đọc bản
  MỚI NHẤT tại
  `https://github.com/royt93/AdmobApplovinWrapper/blob/main/doc/audit/audit_gemini.md`, đợt mới nhất ở
  đầu file — đừng tin số ngày cứng trong đoạn này): **GO-with-caveats**, gắn kèm điều kiện cụ thể theo
  tag đang dùng — không phải "luôn GO tuyệt đối không điều kiện". Artifact JitPack cụ thể bạn tải về
  **có thể đứng sau HEAD vài commit fix** — luôn đối chiếu `git log <tag>..HEAD` hoặc hỏi team SDK
  version nào là bản khuyến nghị mới nhất trước khi coi 1 tag là "final". Các finding không-chặn ở mức
  MEDIUM/LOW đều đã fix hoặc document rõ là accepted-risk có chủ đích (xem mục Accepted risk bên dưới) —
  chi tiết đầy đủ từng finding nằm ở link phía trên (repo public, không cần quyền truy cập riêng),
  không lặp lại trong file này.
- **Điểm tin cậy tại tag `1.7.3` (2026-08-30, cập nhật gần nhất — số sẽ lỗi thời, luôn đối chiếu link
  audit ở trên trước khi tin):** smoke-test mở rộng >370 lượt instrumented
  test cách ly (emulator 2 flavor `gms`/`nongms` + thiết bị thật Pixel 7 Pro), **0 bug SDK mới tìm
  thấy**. **9.6/10 code-correctness** (code chạy đúng, đo được qua test thật) — nhưng
  **8.5–9.2/10 production-readiness tổng thể** (thấp hơn vì thiếu evidence NGOÀI code: dashboard CMP đã
  publish thật chưa, `app-ads.txt`, Data Safety form, legal sign-off per-adapter — 4 việc này audit code
  không tự xác nhận được, xem mục "Chưa kiểm thử" ngay dưới).
- **Chính tài liệu này**: audit ở mức "sai 1 câu = rủi ro hợp đồng" — trích từng câu falsifiable, đối
  chiếu trực tiếp với code thật (file:line), không dựa vào suy luận. Qua nhiều vòng đã tìm và sửa hàng
  chục lỗi mô tả sai hành vi/API trước khi gửi.
- **QA thiết bị thật**: Samsung A50s (Android 11) + Pixel 7 Pro, build `gms`/`nongms` cả debug lẫn
  release. Đã verify không crash cho: App Open (splash + resume), Banner, Interstitial, Rewarded, VIP
  activation qua token, consent flow, và hành vi khi mất mạng (offline) cho cả 4 loại ad trên. Full
  instrumented suite (`gms` + `nongms`) xanh trên tag mới nhất được audit — số lượng test tăng theo
  thời gian, xem CI của tag bạn đang dùng để có số thật.
- **AdSafety trên build release thật** (không phải debug với limit tắt): verify hourly cap (3
  fullscreen/giờ) chặn đúng ở lần thứ 4, fail-soft, không crash — bằng chứng qua "Ad Safety Status"
  real-time counter trên máy.
- **CI**: `build-and-test` (unit test + lint + coverage) chạy tự động trên mọi push/tag/PR. Riêng
  `smoke-verify` (build `:smoke` Debug + Release-R8 trực tiếp từ artifact JitPack thật) là job
  **`workflow_dispatch` — kích hoạt THỦ CÔNG sau khi JitPack đã build xong tag**, không tự chạy cho mọi
  tag. Trước khi tin 1 tag đã "verify AAR JitPack thật", tìm đúng lần chạy `smoke-verify` thủ công của
  đúng tag đó trong tab Actions — CI xanh ở `build-and-test` KHÔNG đồng nghĩa artifact JitPack đã được
  smoke-test.

**Chưa kiểm thử / ngoài phạm vi lần audit này:**

- **Bộ QC thủ công đầy đủ** (181 test case, 12 file suite, repo public —
  https://github.com/royt93/AdmobApplovinWrapper/tree/main/doc/test) — mới chạy smoke subset (~7
  kịch bản P0), chưa chạy hết theo kế hoạch chuyên nghiệp đầy đủ (online+offline × 2 provider × nhiều
  build variant).
- **Daily cap (10 ad/ngày), CTR anomaly (ngưỡng 50 impression/cửa sổ 14 ngày), suspicious-click
  escalation** — cần dữ liệu tích lũy nhiều giờ/ngày thật, chưa test hết được trong một phiên ngắn.
  Chỉ verify được hourly cap (nhanh nhất để trigger).
- **OEM process-kill thật** (Xiaomi/Oppo/Huawei tự kill app nền) — hành vi đặc thù từng hãng, chưa
  test trên thiết bị OEM thật trong lần này.
- **Install Referrer qua uninstall/reinstall thật** (chống abuse auto-trial) — cần chờ thực tế >24h,
  chưa chạy full cycle.
- **Legal/compliance ngoài code**: cấu hình CMP dashboard thật, `app-ads.txt`, Data Safety form (Google
  Play Console), privacy policy per-adapter cho từng mediation network. Đây là trách nhiệm của bên
  tích hợp/vận hành app, không phải thứ audit code có thể xác nhận thay.

**Accepted risk đã biết (không phải bug, đã cân nhắc có chủ đích):**

- ⚠️ **Rewarded Interstitial là AdMob-only — AppLovin MAX permanent KHÔNG hỗ trợ format này** (AAR
  AppLovin chỉ có `MaxRewardedInterstitialAdapter` cho 3rd-party mediation, không có API consumer
  cho chính MAX SDK dùng trực tiếp). Trên `nongms` hoặc `gms` với `IS_ENABLE_ADMOB=false`,
  `loadRewardedInterstitial()`/`showRewardedInterstitial()` LUÔN fire `onAdFailedToLoad`/
  `onAdNotAvailable` — không silent-fail, nhưng cũng không bao giờ show được. **Đừng thiết kế reward
  economy phụ thuộc format này nếu app có thể chạy AppLovin/nongms** — dùng `Rewarded` thường (có
  parity đầy đủ cả 2 provider) hoặc chấp nhận Rewarded Interstitial chỉ hoạt động khi
  `IS_ENABLE_ADMOB=true`. Chi tiết: `doc/AD.MD` mục "Rewarded Interstitial (AdMob-only, callback)".
- **VIP kích hoạt client-side** (không server riêng): chấp nhận vì VIP là tính năng marketing (ẩn ad
  cho user gắn bó), không phải giao dịch trả phí thật. Chi phí tấn công vượt xa phần thưởng.
- **nongms (Huawei/non-Google) không phải "zero Google bytecode"**: AppLovin MAX tự bundle UMP + GAID
  bên trong AAR của chính nó, wrapper không gỡ được. "nongms" nghĩa là không phụ thuộc Google Play
  Services **để chạy**, không phải loại bỏ hoàn toàn code Google.
- **Native CMP AppLovin (`applovinUseNativeCmp=true`) chỉ hiệu lực trên flavor `nongms`** — bật cờ này
  trên `gms` không có tác dụng gì, im lặng bỏ qua (xem Step 8.4b).
- ⚠️ **AppLovin tự fetch App Set ID TRƯỚC khi consent/COPPA guard của wrapper kịp chạy** —
  `AppLovinInitProvider` nằm bên trong chính AAR AppLovin (wrapper không sửa/gỡ được) là một
  `ContentProvider`, và `ContentProvider.onCreate()` luôn chạy TRƯỚC `Application.onCreate()` theo kiến
  trúc Android — tức trước cả lúc wrapper có cơ hội chặn theo cấu hình COPPA/consent của bạn. Đây là
  **App Set ID** (định danh cấp-app, không phải GAID), rủi ro thấp với app mixed-audience, nhưng **app
  hướng THUẦN TRẺ EM (child-directed) KHÔNG được dùng AppLovin MAX** dù bạn cấu hình
  `tagForChildDirectedTreatment` thế nào — không có cách nào ở tầng wrapper chặn được fetch này vì nó
  nằm trong thư viện bên thứ 3.
- ⚠️ **`checkCoppaMisconfiguration()` sẽ CRASH app (kể cả bản release) nếu bạn set
  `AdSdkConfig.tagForChildDirectedTreatment = true`** khi effective provider là AppLovin (tức
  `nongms`, hoặc `gms` với `IS_ENABLE_ADMOB=false`) — vì AppLovin MAX SDK v13.0.0+ đã bỏ
  `setIsAgeRestrictedUser`, không còn honor cờ này, nên wrapper cố ý fail-fast thay vì serve ad
  không-tuân-thủ trong im lặng. Tương tự, `tagForUnderAgeOfConsent = true` cũng crash trên AppLovin
  (chỉ AdMob mode mới propagate TFUA đúng qua `RequestConfiguration`). **Kết luận: app hướng trẻ em
  hoặc có user under-age-of-consent PHẢI dùng AdMob mode (`IS_ENABLE_ADMOB=true` trên `gms`)**, không
  được dùng AppLovin/`nongms`.
- ⚠️ **SSAID (`Settings.Secure.ANDROID_ID`) được đọc ngay bước đầu của `initialize()`, TRƯỚC khi có
  quyết định consent** — khác với GAID (chỉ đọc SAU consent). Lý do cố ý: SSAID dùng DUY NHẤT để khoá
  token VIP vào đúng thiết bị (không rời máy, không dùng cho ads/tracking). Nếu bạn cần khai identifier
  SDK bên thứ 3 sử dụng trong Data Safety form (Google Play Console), khai đúng mục "Device or other
  IDs" cho phần này.
- ⚠️ **Root user có thể tắt được lớp bảo vệ CTR anomaly + làm daily cap fail-open** — cả hai đọc
  SharedPreferences ký HMAC; nếu root user chỉnh sửa trực tiếp làm sai chữ ký, SDK **fail-open** (coi
  như bộ đếm = 0, không chặn ad) thay vì fail-closed. Đây là fail-open **có chủ đích**: ưu tiên không
  khoá nhầm user thật khi prefs hỏng vì lý do lành tính (đổi máy, backup/restore, migration). Lớp bảo
  vệ tài khoản quảng cáo cấp cao hơn (invalid traffic) vẫn do Google/AppLovin xử lý phía server, độc
  lập với lớp này.
- ℹ️ **TÍNH NĂNG (không phải lỗi): SDK mang sẵn một "sổ đăng ký thiết bị QC" của nhà phát triển SDK
  (`royt93`)** — danh sách hash test-device hard-code trong SDK, tự động gộp (union) vào bất kỳ list
  bạn khai qua `setTestDeviceIds()`, áp ở **cả debug lẫn release**, và **không override/tắt được từ
  phía app**. Đây là quyết định có chủ đích của chủ SDK, để mọi máy QC của họ luôn nhận **quảng cáo
  thử** khi kiểm thử bất kỳ app nào tích hợp SDK — tránh việc QC bấm nhầm quảng cáo thật rồi kéo theo
  rủi ro khoá tài khoản cho chính app đó.
  **Nghĩa là gì với app của bạn:** đúng những thiết bị nằm trong danh sách (một nhóm nhỏ máy do chủ
  SDK trực tiếp kiểm soát) sẽ thấy test ad trên app bạn và **không sinh doanh thu** từ những máy đó.
  Mọi thiết bị khác — tức toàn bộ user thật — **không bị ảnh hưởng**, vẫn nhận quảng cáo thật bình
  thường. Bạn không cần làm gì; chỉ cần biết nó tồn tại nếu QA sau này báo "có một máy luôn thấy test
  ad không rõ vì sao". Danh sách được cập nhật thủ công trong source SDK, không fetch từ mạng lúc
  chạy.
- ℹ️ **Layout finding nhỏ (demo app, không phải SDK):** link "Privacy Policy" trong màn hình VIP demo
  (`ActVipManagement`) đặt khá gần đáy màn hình. Trên thiết bị màn hình cao + chế độ điều hướng
  **3-button nav** (không phải gesture nav), toạ độ tap tính ở giữa view có thể rơi vào vùng nav bar hệ
  thống, khiến hệ thống "nuốt" mất cú tap trước khi tới `onClickListener` của app (đã verify bằng
  logcat: `ActivityTaskManager: START ... category.HOME ... from uid 0` — tức hệ thống tự bắn Home,
  không phải app). Rủi ro thấp (user thật hiếm khi tap chính xác vào rìa như vậy), nhưng nếu bạn
  copy/tùy biến lại layout màn VIP, nên chừa thêm padding-bottom hoặc tôn trọng
  `WindowInsetsCompat` cho khu vực footer chứa link chính sách để tránh lặp lại.

> Manifest App Open exclusion + AdMob App ID: xem **B.3** (song song với **Bước 3a** — Bước 5 +
> Phụ lục A có chi tiết banner/interstitial/rewarded/VIP).

## 🧭 Chọn flavor + consent path (đọc TRƯỚC — tránh lỗi #1 "build sai flavor")

```
Bạn ship app ở đâu?
│
├─ Google Play (có Play Services)
│   │
│   ├─ Muốn AdMob (+ AppLovin mediation)?
│   │      → flavor gms   + IS_ENABLE_ADMOB = true
│   │
│   └─ Chỉ AppLovin MAX (không dùng AdMob)?
│          → flavor gms   + IS_ENABLE_ADMOB = false
│   │
│   └── CONSENT: tự động qua Google UMP
│          → gọi AdManager.requestConsentInfoUpdate(activity){…} ở SplashActivity
│            (không cần set applovinHasUserConsent thủ công)
│
└─ Huawei AppGallery / chợ KHÔNG Google
    → flavor nongms  (LUÔN chạy AppLovin — nongms KHÔNG có AdMob, IS_ENABLE_ADMOB bị bỏ qua)
    → build từ SOURCE (JitPack chỉ phát hành bản gms)
    │
    └── CONSENT (nongms KHÔNG có UMP, phải tự quyết định):
         ├─ Có user ở EEA / UK / CH?
         │     → set AdSdkConfig.applovinHasUserConsent = true/false TRƯỚC initialize()
         │       HOẶC hiện dialog riêng rồi gọi AdManager.confirmGdprConsent(ctx, hasConsent)
         │
         └─ CHẮC CHẮN không phục vụ EEA/UK/CH?
               → set AdSdkConfig.allowAdsWhenConsentUnknownNongms = true (phục vụ NPA ngay)
```

> ⚠️ **nongms mà để `applovinHasUserConsent = null` + không dialog + không bật cờ trên = ZERO ad**
> (SDK fail-closed theo GDPR). Đây là mặc định an toàn, không phải bug.

## 🔺 Nâng từ ≤ 1.5.5 lên 1.6.1 — 2 thứ làm VỠ nếu bỏ qua

> Mục này khác mục "🔔 ĐANG DÙNG BẢN CŨ?" ở đầu file — mục đó nói về đợt nâng cấp GPP/độ chính xác
> doanh thu (2026-08-29), mục này nói về đợt nâng cấp cũ hơn (≤1.5.5 → 1.6.1). Đang ở bản ≥1.6.1 rồi
> thì đọc lướt qua, không cần áp dụng lại — nhưng vẫn hữu ích nếu bạn đọc từ code base rất cũ.

Bỏ qua mục này thì build vỡ hoặc user nhận thưởng gấp đôi. Đọc trước khi đổi số version.

### 1. Cộng thưởng ĐÔI — `grantRewardOnEarn` đổi mặc định `false` → `true`

SDK nay trao thưởng **ngay lúc user xem đủ**, không đợi họ đóng quảng cáo (khuyến nghị chính thức
của Google; app chết giữa chừng thì user vẫn nhận).

Đổi cái gì thật sự: **thời điểm** callback chạy, không phải số lần. Callback của `showRewarded` vẫn
fire-once (`onRewardedDone.getAndSet(null)`) ở cả trước lẫn sau 1.6.1 — nên riêng việc đổi default
**không** tự sinh ra cộng đôi. Rủi ro mới là callback chạy khi **ad còn phủ màn hình** (xem mục dưới),
và rủi ro cũ vẫn còn nguyên: SDK báo cùng một lần xem qua **3 kênh**, cộng thưởng ở từ 2 kênh trở lên
là user nhận đôi — đó là lý do có `rewardTransactionId`.

Chọn một trong hai:

```kotlin
// Cách A — giữ hành vi cũ, không phải sửa gì thêm
AdSdkConfig(/* … */, grantRewardOnEarn = false)

// Cách B (khuyến nghị) — nhận lợi ích của default mới, dedup bằng transaction ID
val txId = AdManager.rewardTransactionId ?: return
if (txId == prefs.getString("last_reward_tx", null)) return   // đã cộng rồi
prefs.edit().putString("last_reward_tx", txId).apply()        // persist: sống qua process death
giveUserReward()
```

**Chỉ cộng thưởng ở MỘT kênh.** SDK báo cùng một lần xem qua ba kênh: callback của `showRewarded`,
`rewardedListener.onUserEarnedReward(...)`, và `AdEvent.Rewarded`. Dùng `rewardedListener` cho
UI/analytics, đừng cộng thưởng ở đó.

### 2. VỠ BUILD — `AdSafetyStatus` đổi tên 2 field

| Cũ (≤ 1.5.5)              | Mới (1.6.1)                   |
|---------------------------|-------------------------------|
| `status.totalImpressions` | `status.ctrWindowImpressions` |
| `status.fullscreenClicks` | `status.ctrWindowClicks`      |

Không có alias tương thích ngược — **cố ý**. Ngữ nghĩa đã đổi: trước là số của **phiên hiện tại**
(trần 6), nay là số của **cửa sổ rolling 14 ngày** (lớn hơn nhiều). Giữ tên cũ thì code cũ vẫn biên
dịch và **đọc sai trong im lặng**; đổi tên buộc bạn dừng lại nhìn.

### 3. Số quảng cáo mỗi user/ngày SẼ TĂNG (không phải bug)

Trước 1.6.1, bộ đếm "6 quảng cáo mỗi phiên" **không bao giờ được reset** — thực chất là trần theo
vòng đời tiến trình. Nay app ở background ≥ 30 phút thì lần quay lại tính là phiên mới. Con số thực
tế sẽ tiến về phía **trần ngày** (`maxFullscreenAdsPerDay`, không đổi).

👉 **Theo dõi ad/user/ngày trong tuần đầu sau khi nâng.** Nếu vượt trần ngày thì có lỗi ở chỗ khác.

### 4. GPP — phạm vi thật, đọc trước khi bật traffic Mỹ

SDK đọc `IABGPP_7_String` (US National) + `IABGPP_8_String` (California), **ưu tiên hơn**
`IABUSPrivacy_String` legacy. **KHÔNG đọc** section riêng từng bang (Virginia/Colorado/Utah/
Connecticut/Texas…) — layout mỗi bang khác nhau, đoán sai bit trong parser quyền riêng tư nguy hiểm
hơn việc thành thật nói "không đọc được". Đa số CMP hiện đại phát usnat để phủ nhiều bang theo MSPA
nên thường là đủ; nếu CMP của bạn chỉ phát section theo bang, SDK log
`⚠️ GPP: CMP đang phát section [...]` — khi đó đổi cấu hình CMP sang usnat.

⚠️ **Toàn bộ chuyện này chỉ có tác dụng khi CMP của bạn THẬT SỰ ghi GPP.** Verify trên máy thật:
gọi `AdManager.getDiagnostics()`, ba dòng `IABGPP_*` phải khác `(empty)`.

---

## ⚠️ Đọc 30 giây này trước — vì sao "không thấy ad nào"

SDK **mặc định fail-closed theo luật GDPR**: nếu app **chưa hoàn tất bước hỏi đồng ý (consent)**, SDK sẽ **KHÔNG hiển thị quảng cáo nào** — cả khi có mạng lẫn không mạng. Đây **không phải lỗi SDK**, mà là bảo vệ tài khoản AdMob/AppLovin của bạn khỏi bị Google ban.

90% báo lỗi "SDK không work / không có ad" là do **một** trong các nguyên nhân sau:

| Triệu chứng | Nguyên nhân thường gặp | Cách sửa |
|---|---|---|
| **Không ad nào, online lẫn offline** | Chưa gọi `requestConsentInfoUpdate()` ở Splash (gms) / chưa set `applovinHasUserConsent` (nongms) | Làm **Bước 4** |
| **AdMob hoàn toàn không chạy** (luôn ra AppLovin) | Build sai flavor **nongms** (nongms KHÔNG có AdMob) HOẶC chưa bật `IS_ENABLE_ADMOB=true` | Build flavor **gms** + bật flag (Bước 2-3) |
| **Không crash — nhưng ad không hiện** | Gọi `show...()` trước khi `initialize()` xong → provider còn `null`, mọi `show*()` fail-soft (`onAdNotAvailable`/callback `false`), **không throw exception** | Đợi callback `initialize{ success -> }` |
| Một ad type không ra | Sai ad unit ID, hoặc no-fill | Kiểm tra ID đúng provider |

> 🔑 **Gặp bất kỳ vấn đề nào → gọi 1 dòng này và đọc kết quả:**
> ```kotlin
> android.util.Log.w("Ads", AdManager.getDiagnostics())
> ```
> Nó in ra **chính xác lý do ad bị chặn + cách fix**. Khi báo lỗi, **đính kèm output này**.

## 🌐 Hành vi theo trạng thái mạng (tóm tắt nhanh, chi tiết xem A.4 + Phụ lục C)

| Trạng thái | Hành vi SDK |
|---|---|
| **Có mạng, consent xong** | Load/show bình thường, preload tự động sau mỗi lần dismiss. |
| **Mất mạng từ đầu (cold start)** | Fullscreen trả `AdError.NoNetwork` (callback vẫn fire `false`, KHÔNG throw); banner ẩn + tự queue; App Open resolve `false` fail-soft; consent fetch lỗi → `retryConsentIfNeeded()` tự retry (xem dưới). |
| **Rớt mạng giữa phiên** (đang xem app, mạng rớt) | Ad đã preload trước đó vẫn show được (đã tải sẵn trong RAM); ad CHƯA preload thì load mới sẽ fail như dòng trên. |
| **Mạng phục hồi** | SDK tự nhận biết qua "validated network" callback (không phải chỉ có sóng — phải ping được internet thật) → tự retry consent (nếu lần trước lỗi, throttle 60s, tự gọi ở mỗi `onActivityResumed`) → tự preload lại banner/fullscreen đang chờ. **Không cần app tự gọi lại gì.** |

VIP redeem (`activateVipByKey`/`activateVipByToken`) là **ngoại lệ cố ý**: luôn cần mạng tại thời điểm
gọi (network checker riêng) dù verify chữ ký là offline — đừng test redeem lúc tắt mạng rồi tưởng SDK lỗi.

## Bước 1 — Dependency (JitPack)
```kotlin
// settings.gradle.kts
dependencyResolutionManagement { repositories { maven(url = "https://jitpack.io") } }
// app/build.gradle.kts
// Thay <LATEST_TAG> bằng tag MỚI NHẤT tại https://jitpack.io/#royt93/AdmobApplovinWrapper
implementation("com.github.royt93:AdmobApplovinWrapper:<LATEST_TAG>")

android {
    buildFeatures {
        buildConfig = true   // ⚠️ BẮT BUỘC — thiếu dòng này thì Bước 3/4 dùng `BuildConfig.XXX` sẽ báo
    }                        // lỗi biên dịch "unresolved reference: BuildConfig" (AGP 8.x tắt mặc định).
}
```

## Bước 2 — Chọn flavor (CHỈ khi cần bản nongms từ source)
**Dùng AAR JitPack (mặc định) thì KHÔNG cần khai flavor.** JitPack chỉ publish **variant `gms`**; Gradle
metadata của nó CÓ thuộc tính `ads=gms`, nhưng vì app consumer **không khai** dimension `ads` nên Gradle
không request thuộc tính đó và vẫn chọn được variant gms đã publish → resolve bình thường, không bị ép khai
`flavorDimensions`. (Lưu ý: module `:smoke` trong chính repo này — kể từ 1.4.0 — CÓ khai
`flavorDimensions("ads")` để tự smoke-test cả 2 flavor cùng lúc, nên KHÔNG dùng làm ví dụ "không cần khai
flavor" nữa; downstream app chỉ cần một `implementation("com.github.royt93:AdmobApplovinWrapper:<ver>")`
đơn giản, không cần flavor, như mô tả ở trên.)

Bạn **chỉ cần** khai `flavorDimensions("ads")` + 2 flavor khi muốn **build bản `nongms` thuần không-Google
từ SOURCE** (clone repo, dùng `project(":admob-wrapper-sdk")`) — JitPack KHÔNG phát hành bản nongms:
```kotlin
flavorDimensions += "ads"
productFlavors {
    create("gms")    { dimension = "ads" }   // Google Play: AdMob + AppLovin + UMP consent + GAID
    create("nongms") { dimension = "ads" }   // Huawei / chợ không Google: CHỈ AppLovin (KHÔNG có AdMob)
}
```
→ Muốn dùng **AdMob thì build flavor `gms`** (hoặc đơn giản dùng AAR JitPack). Trên `nongms` (build từ
source) mọi cấu hình AdMob bị bỏ qua, luôn chạy AppLovin.

## Bước 3 — Ad ID + chọn provider (qua `buildConfigField`)

⚠️ **Danh sách ĐẦY ĐỦ dưới đây — thiếu bất kỳ dòng nào thì `MyApplication.kt` ở Phụ lục B.1 (dùng
`BuildConfig.XXX` cho từng field) sẽ báo lỗi biên dịch `Unresolved reference`.** Mỗi `buildConfigField`
phải khai riêng, không tự sinh thêm — copy nguyên khối này vào `defaultConfig { }` (hoặc `debug{}`/
`release{}` nếu muốn tách giá trị debug/release), thay giá trị `<...>` bằng ID thật của bạn:

```kotlin
buildConfigField("Boolean", "IS_ENABLE_ADMOB", "true")   // true = AdMob, false = AppLovin MAX

// AdMob — bỏ qua nếu chỉ dùng AppLovin (IS_ENABLE_ADMOB=false)
buildConfigField("String", "ADMOB_BANNER_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "ADMOB_REWARDED_ID", "\"<ID-thật-của-bạn>\"")

// AppLovin — luôn cần (kể cả khi IS_ENABLE_ADMOB=true, dùng làm mediation/fallback)
buildConfigField("String", "APPLOVIN_SDK_KEY", "\"<key-thật-từ-AppLovin-dashboard>\"")
buildConfigField("String", "APPLOVIN_BANNER_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "APPLOVIN_INTERSTITIAL_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "APPLOVIN_APP_OPEN_ID", "\"<ID-thật-của-bạn>\"")
buildConfigField("String", "APPLOVIN_REWARDED_ID", "\"<ID-thật-của-bạn>\"")

// VIP + privacy policy — bắt buộc để build, xem ⚠️ cảnh báo secret ở Bước 4
buildConfigField("String", "PRIVACY_POLICY_URL", "\"<URL-privacy-policy-thật-của-bạn>\"")
buildConfigField("String", "VIP_KEY_SECRET", "\"<chuỗi-ngẫu-nhiên-ít-nhất-16-ký-tự>\"")
buildConfigField("String", "VIP_TOKEN_PUBLIC_KEY", "\"\"")  // để trống nếu CHƯA dùng VIP token — xem Step 10
```
> ⚠️ Demo dùng **Google test ID** (an toàn khi dev). **Bạn PHẢI thay bằng ID production thật của bạn** trước khi phát hành — nếu không sẽ zero-revenue và có thể vi phạm policy AdMob. AppLovin SDK key cũng thay bằng key của bạn (lấy từ AppLovin dashboard).

### Bước 3a — AdMob App ID trong Manifest (chỉ cần nếu dùng AdMob, `IS_ENABLE_ADMOB=true`)

**Khác `ADMOB_BANNER_ID` (ad-unit ID, dạng `.../...`) — đây là App ID (dạng `ca-app-pub-xxxx~yyyy`, dấu `~`),
lấy từ AdMob Console → App settings.** Google yêu cầu khai App ID trong Manifest, KHÔNG qua `BuildConfig`
trực tiếp — dùng `manifestPlaceholders` để tách debug/release, giống các ad-unit ID khác:

```kotlin
// app/build.gradle.kts — trong buildTypes { debug { ... } / release { ... } }
manifestPlaceholders["admobAppId"] = "<ADMOB_APP_ID-thật-của-bạn>"  // ca-app-pub-xxxx~yyyy, KHÔNG phải ad-unit ID
```
```xml
<!-- AndroidManifest.xml — trong thẻ <application> -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="${admobAppId}" />
```
Thiếu bước này = crash ngay khi mở app trên build có `IS_ENABLE_ADMOB=true` (Google SDK bắt buộc App ID
có mặt trong Manifest trước khi gọi bất kỳ API AdMob nào).

## Bước 3b — 🚨 Đăng ký máy test (BẮT BUỘC trước khi QA — bảo vệ tài khoản của bạn)

Ngay khi bạn dùng **ad-unit ID production thật**, mọi cú click của dev/QA lên quảng cáo đều là
**invalid traffic**. AdMob/AppLovin tự phát hiện và **treo hoặc khoá tài khoản, giữ lại doanh thu** —
đây là cách phổ biến nhất khiến một app mới bị khoá kiếm tiền. SDK **không thể** tự biết máy nào là
máy test; bạn phải khai:

**Cách nhanh nhất** — gọi 1 hàm, SDK tự đọc logcat, tự copy hash vào clipboard + Toast,
work cả bản release (share APK cho tester ở xa, họ bấm 1 nút gửi lại hash, khỏi cần adb):

```kotlin
AdManager.copyTestDeviceHashToClipboard(this) // this = Activity, gọi SAU khi đã load ít nhất 1 ad AdMob
```

Chỉ cần hash thô (tự custom UI) thì dùng `AdManager.captureAdMobTestDeviceHashFromLogcat { hash -> ... }`
Cả hai đều chỉ gate theo `isEnableAdmob` — không cần `BuildConfig.DEBUG`.

```kotlin
// Cách thủ công (không dùng 2 hàm trên) — tự đọc logcat dòng Google in ra rồi khai tay:
AdManager.setTestDeviceIds("ABCDEF0123...") // hash lấy từ bước dưới, KHÔNG PHẢI GAID
```

Lấy hash thủ công (nếu không dùng `copyTestDeviceHashToClipboard`): chạy app 1 lần rồi tìm trong logcat
dòng của Google: `Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF0123..."))` —
copy đúng chuỗi đó.

> ⚠️ **KHÔNG dùng `AdManager.getCurrentDeviceGAID()` cho `setTestDeviceIds()`** — GAID (UUID, có dấu
> gạch ngang) và hash test-device (hex string Google tự tính từ `ANDROID_ID`) là HAI THỨ KHÁC NHAU.
> `setTestDeviceIds()` **âm thầm bỏ qua** nếu truyền nhầm GAID vào — không lỗi, không log rõ ràng, máy
> vẫn nhận ads production thật như bình thường thay vì test ads. Đây đúng lỗi từng khiến QA device nhận
> nhầm ads thật suốt nhiều tuần ở một dự án dùng SDK này — dùng `copyTestDeviceHashToClipboard()` ở trên
> để tránh hẳn nhầm lẫn này, hoặc nếu lấy thủ công thì chỉ copy đúng chuỗi hex trong dòng logcat, đừng
> lấy từ `getCurrentDeviceGAID()`. Kiểm tra lại bằng `AdManager.getTestDeviceIds()`.

> Với AppLovin, SDK tự truyền GAID máy hiện tại vào `initConfig` khi `isDebug = true` — nhưng
> `setTestDeviceIds()` vẫn cần hash (không phải GAID) cho AdMob.

**Kiểm tra đã đúng chưa:** quảng cáo hiển thị phải có nhãn *"Test Ad"*. **Không thấy nhãn đó = bạn
đang click ad thật = dừng ngay.**

> 💡 **Khuyến nghị: lưu danh sách máy QC + hash + GAID thành 1 file bền vững trong repo/wiki của bạn**
> (không phải chỉ nhớ trong đầu hay Slack) — mỗi máy QC dùng nhiều lần, hash không đổi theo thời gian
> (chỉ đổi nếu factory reset máy), nên chỉ cần lấy 1 lần rồi lưu lại tái dùng, tránh phải đọc logcat lại
> mỗi lần cài app mới lên máy cũ. Ghi rõ **loại ID nào dùng cho việc gì** (hash test-device cho AdMob,
> GAID cho VIP whitelist/AppLovin — 2 thứ khác nhau, xem cảnh báo trên) để tránh lặp lại đúng sự cố ở
> trên. Ví dụ định dạng thật (không phải của bạn, chỉ để tham khảo cách trình bày — máy trong đó thuộc
> về người khác, đừng dùng nhầm hash của họ):
> https://github.com/royt93/myKeyStore/blob/main/README.md

## Bước 4 — Khởi tạo (Application + Splash)
`MyApplication.onCreate()` — chỉ 2 dòng:
```kotlin
AdManager.setConfig(adConfig)                              // adConfig: AdSdkConfig
AdManager.initialize(this) { success, gaid -> /* log */ }
```

> 🟡 **`success = false` KHÔNG chắc là lỗi.** SDK cố ý **hoãn** init provider cho tới khi có quyết
> định consent. Nếu partner quên nối bước consent, sau ~30s watchdog sẽ flush callback với
> `success = false` để `initialize()`/`initializeAsync()` không treo — **nhưng provider vẫn sẽ tự init
> và phục vụ ad ngay khi consent về**. Phân biệt 2 ca bằng `isWaitingForConsent()`:
> ```kotlin
> AdManager.initialize(this) { success, gaid ->
>     if (!success && AdManager.isWaitingForConsent()) {
>         // ĐANG CHỜ CONSENT — KHÔNG phải lỗi. Đừng disable tính năng ad.
>         // gms: gọi requestConsentInfoUpdate() ở Splash. nongms: confirmGdprConsent().
>     } else if (!success) {
>         // Lỗi thật (vd applovinSdkKey rỗng) — xem AdManager.getDiagnostics().
>     }
> }
> ```
> ⚠️ Ở ca "chờ consent", tham số `gaid` của callback là `null` vì provider chưa init xong. Khi consent
> về muộn, callback gốc **không được gọi lại** — lấy GAID bằng `AdManager.getGaid()` hoặc collect
> `AdEvent.GaidAvailable`.
>
> 💰 **Set `paidEventListener` NGAY TẠI ĐÂY, trong `Application.onCreate()`** — KHÔNG set từ trong
> Activity. SDK gắn "chủ sở hữu" cho listener là Activity đang foreground lúc set, và **tự xoá listener
> khi Activity đó destroy** (cơ chế chống leak). Set trong Activity ⇒ mất toàn bộ revenue tracking một
> cách âm thầm. Trong `Application.onCreate()` chưa có Activity nào nên listener sống suốt vòng đời app.
> (Từ 1.5.5 SDK bắn cảnh báo qua `ErrorReporter` khi phát hiện ca này.)
>
> ⚠️ **Chữ ký thật của `paidEventListener`** (`fun interface PaidEventListener` — SAM, gán được bằng
> lambda nhưng PHẢI đúng 5 tham số, không phải 1 object "event"):
> ```kotlin
> AdManager.paidEventListener = PaidEventListener { adType, valueMicros, currency, precision, adSource ->
>     // adType: "banner"/"interstitial"/"appopen"/"rewarded"/"rewarded_interstitial"
>     // valueMicros: chia 1_000_000 ra USD. precision: "PRECISE"/"PUBLISHER_PROVIDED"/"ESTIMATED"/"UNKNOWN"
>     Log.d("AdsRevenue", "$adType $valueMicros $currency $precision $adSource")
> }
> ```
> Gán bằng lambda 1-tham-số kiểu `{ event -> ... }` **KHÔNG compile được** (sai arity so với SAM).

> ⚠️ **Chặn App Open đè lên splash/onboarding**: `appOpenExcludedActivities` (class reference, 1.3.0+)
> default = `emptyList()` — **nhưng** field cũ `appOpenExcludedActivityNames` (deprecated, string-based)
> vẫn default = `listOf("SplashActivity")` và 2 field này OR-combine (loại trừ nếu khớp MỘT trong hai).
> Nghĩa là nếu Activity splash của bạn tên đúng `SplashActivity`, App Open đã tự bị chặn ngay cả khi bạn
> chưa khai gì — nhưng **đừng dựa vào default ngầm này**, field cũ sẽ bị gỡ dần; luôn khai tường minh
> bằng class reference cho đúng tên Activity thật của bạn:
> ```kotlin
> AdSdkConfig(/* … */, appOpenExcludedActivities = listOf(SplashActivity::class.java))
> ```
> ⚠️ **`appOpenExcludedActivities` chỉ chặn App Open trên các Activity bạn khai — không đủ nếu bạn mở
> Dialog/BottomSheet (billing flow, in-app update, permission prompt) TRÊN CÙNG một Activity đã cho
> phép App Open.** Dùng `AdManager.suppressAppOpenTemporarily(suppress: Boolean)` để tạm khoá App Open
> tự-động resume quanh các Dialog quan trọng đó:
> ```kotlin
> override fun onStart() {
>     AdManager.suppressAppOpenTemporarily(true)
>     dialog.show()
> }
> // Khi dialog dismiss:
> AdManager.suppressAppOpenTemporarily(false)
> ```
> Có **safety-net tự hết hạn sau 5 phút** nếu bạn quên gọi lại `false` (SDK tự bỏ suppress, không khoá
> App Open vĩnh viễn do lỗi lập trình). Chỉ chặn nhánh **tự-động resume** — không ảnh hưởng
> `showInterstitial`/`showRewarded`/gọi tay `showAppOpenAd`.
>
> ⚠️ **`AdSdkConfig.skipAppOpenOnFirstEverLaunch` mặc định là `true`** (đổi hành vi mặc định so với
> bản cũ) — nghĩa là **SDK tự động KHÔNG hiện App Open ở lần mở app đầu tiên sau khi cài đặt**, kể cả
> trên Splash. Đây là hành vi CỐ Ý (tránh đè App Open lên onboarding/permission-prompt lần đầu), không
> phải bug — nếu QA report "App Open không hiện ở lần chạy đầu tiên", đây là nguyên nhân. Set
> `skipAppOpenOnFirstEverLaunch = false` nếu bạn đã tự đảm bảo app không có luồng onboarding chặn ở
> lần mở đầu.
> 🚨 **BẮT BUỘC đổi `vipKeySecret` trong `AdSdkConfig` trước khi build release.** Nếu để giá trị mặc định công khai (`"TPLdev0402dev@"`), **bản release sẽ CRASH ngay khi mở app** với `IllegalStateException` tại `setConfig()` (đây là cơ chế bảo vệ cố ý, chống giả token VIP). Secret phải ≥16 ký tự, ngẫu nhiên, không commit công khai (đặt trong `local.properties`/`BuildConfig`). Bản debug chỉ cảnh báo (không crash) nên dễ bỏ sót — **hãy test bản release sớm.**
> 🚨 **Nếu dùng VIP-token: BẮT BUỘC đổi luôn `vipTokenPublicKey`** (không chỉ `vipKeySecret`). Sinh
> cặp khoá riêng bằng `AdManager.generateVipKeyPair()` (chạy 1 lần, offline) → đặt public key vào
> `AdSdkConfig.vipTokenPublicKey`, giữ private key ở tool/CI. Nếu release còn giữ **public key MẪU**,
> SDK **từ chối IM LẶNG mọi token** (`activateVipByToken` trả `false` dù chữ ký hợp lệ) — `setConfig()`
> chỉ log, **KHÔNG crash** nên rất dễ bỏ sót. Khác `vipKeySecret` (crash ở release nếu để mặc định).
`SplashActivity` — **bắt buộc** hoàn tất consent rồi mới chạy splash (bỏ bước này = không có ad):
```kotlin
@OptIn(ExperimentalAdApi::class)   // awaitSplashComplete là @ExperimentalAdApi (WARNING) — cần opt-in
AdManager.requestConsentInfoUpdate(this) { canRequestAds ->     // gms: UMP
    lifecycleScope.launch {
        AdManager.awaitSplashComplete(this@SplashActivity)     // SDK tự load+show App Open + timeout
        goToMain()
    }
}
```
> `nongms` không có UMP → truyền `applovinHasUserConsent = true/false` trong `AdSdkConfig` **trước** `initialize()`.
> Không muốn opt-in experimental? Dùng callback `AdManager.initSplashScreen(activity) { goToMain() }` thay
`awaitSplashComplete`.

## Bước 5 — Hiển thị ad (callback LUÔN fire, kể cả khi bị chặn)

`container` (một `ViewGroup` rỗng có sẵn trong layout, banner sẽ được thêm vào bên trong) và `tvLabel`
(một `TextView` nhỏ, banner tự dùng để hiện chữ "Ad"/trạng thái debug — không phải label bạn tự viết
nội dung) là 2 `View` **bạn tự khai trong layout XML của Activity**, KHÔNG phải do SDK tạo sẵn. Ví dụ tối
thiểu, `MainActivity` hiện banner + 1 nút bấm để show interstitial:

```xml
<!-- res/layout/activity_main.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent" android:layout_height="match_parent"
    android:orientation="vertical">
    <Button android:id="@+id/btnShowInterstitial"
        android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="Show Interstitial" />
    <FrameLayout android:id="@+id/bannerContainer"
        android:layout_width="match_parent" android:layout_height="wrap_content" />
    <TextView android:id="@+id/tvBannerLabel"
        android:layout_width="wrap_content" android:layout_height="wrap_content" />
</LinearLayout>
```
```kotlin
package com.yourapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.roy.sdkadbmob.AdManager

class MainActivity : AppCompatActivity() {
    // findViewById thường (không dùng ViewBinding) — khỏi cần bật thêm buildFeatures nào.
    private lateinit var bannerContainer: android.widget.FrameLayout
    private lateinit var tvBannerLabel: android.widget.TextView
    private var bannerView: android.view.View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bannerContainer = findViewById(R.id.bannerContainer)
        tvBannerLabel = findViewById(R.id.tvBannerLabel)

        findViewById<android.widget.Button>(R.id.btnShowInterstitial).setOnClickListener {
            AdManager.showInterstitial(this) { shown -> /* shown=false khi bị AdSafety chặn/no-fill, vẫn fire */ }
        }
    }

    override fun onResume() {
        super.onResume()
        // Guard chống tạo banner mới đè lên banner cũ khi Activity resume từ back-stack.
        if (bannerView == null || bannerView?.parent == null) {
            bannerView = AdManager.loadBanner(
                this, bannerContainer, tvBannerLabel,
                AdManager.getAdaptiveBannerSize(this),
            ) // autoManageLifecycle=true (default) — KHÔNG cần tự gọi bannerResume/bannerPause/bannerDestroy
        }
    }
    // App Open: SDK tự quản qua ProcessLifecycle, không cần gọi tay.
    // Rewarded: xem mục "Cơ chế Trial / VIP / Log" bên dưới (dùng trong màn VIP, không phải MainActivity).
}
```

## Phụ lục A — API surface cốt lõi (trích doc/AD.MD)

> Trích những API partner **bắt buộc** cần để tích hợp. Đủ để migrate offline mà KHÔNG cần mở github.
> Chi tiết đầy đủ (mọi overload, mọi field, kiến trúc nội bộ): `doc/AD.MD` trên GitHub (tùy chọn).
> Tất cả API là member của `object AdManager` (`com.roy.sdkadbmob.AdManager`) trừ khi ghi rõ.

### A.1 — Init & vòng đời (bắt buộc, đúng thứ tự)

| Hàm                  | Chữ ký                                                                                                      | Mô tả                                                                                                                                    |
|----------------------|-------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `setConfig`          | `setConfig(config: AdSdkConfig)`                                                                            | **Gọi ĐẦU TIÊN** (main thread) trong `Application.onCreate`. `initialize()` gọi trước `setConfig` KHÔNG crash — có guard (`!::adConfig.isInitialized`), log warning rồi trả `onComplete(false, null)`. Cái THẬT SỰ crash `UninitializedPropertyAccessException` là đọc bất kỳ property nào chạm `adConfig` không qua guard (vd `AdManager.isEnableAdmob`) trước khi `setConfig` chạy — ví dụ `SplashActivity.onCreate` đọc `isEnableAdmob` ngay khi khởi động. |
| `initialize`         | `initialize(application: Application, onComplete: (success: Boolean, gaid: String?) -> Unit = { _, _ -> })` | Gọi SAU `setConfig`. Gộp `earlyInit` + provider init (defer tới khi có consent) + `registerAppOpenAdLifecycle`.                          |
| `setCurrentActivity` | `setCurrentActivity(activity: Activity)`                                                                    | Cập nhật Activity hiện tại cho các show-path (gọi ở `onCreate`/`onResume`).                                                              |

### A.2 — Consent & Splash (gms)

| Hàm                        | Chữ ký                                                                                                                                        | Mô tả                                                                                        |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `requestConsentInfoUpdate` | `requestConsentInfoUpdate(activity: Activity, tagForUnderAgeOfConsent: Boolean = false, onConsentGathered: (canRequestAds: Boolean) -> Unit)` | Chạy UMP ở `SplashActivity`; có watchdog nội bộ. Callback báo `canRequestAds`.               |
| `awaitSplashComplete`      | `suspend fun AdManager.awaitSplashComplete(activity: Activity, timeoutMs: Long = 0L)`                                                         | `@ExperimentalAdApi` (extension). SDK tự load+show App Open + timeout, suspend tới khi xong. |
| `initSplashScreen`         | `initSplashScreen(activity: Activity, onAdLoaded: () -> Unit)`                                                                                | Bản callback (không cần opt-in experimental) thay `awaitSplashComplete`.                     |
| `confirmGdprConsent`       | `confirmGdprConsent(context: Context, hasConsent: Boolean)`                                                                                   | Ghi quyết định consent thủ công (nongms / custom flow); resume provider init nếu đang chờ.   |
| `needsGdprConsentDialog`   | `needsGdprConsentDialog(context: Context): Boolean`                                                                                           | Luôn `false` trên gms (UMP tự lo). Trên nongms: `true` nếu chưa có quyết định (`applovinHasUserConsent`/`confirmGdprConsent` lưu ở `AppPreferences`) — KHÔNG dựa TCF string, vì nongms không có UMP/geo-detection (audit 1611, doc-wording). |
| `resetConsent`             | `resetConsent()`                                                                                                                              | Xóa consent (debug / test GDPR).                                                             |

### A.3 — Banner (mặc định auto-managed; chỉ forward thủ công khi `autoManageLifecycle=false`)

| Hàm                     | Chữ ký                                                                                                                                                            | Mô tả                                                                                  |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `loadBanner`            | `loadBanner(context: Context, container: ViewGroup, tvLabelAd: TextView, adSize: AdBannerSize = AdBannerSize.Banner, autoManageLifecycle: Boolean = true): View?` | Load banner vào `container`. Trả `View?` (null nếu bị chặn/VIP/no-fill).               |
| `getAdaptiveBannerSize` | `getAdaptiveBannerSize(activity: Activity): AdBannerSize`                                                                                                         | Tính adaptive size theo bề rộng màn hình.                                              |
| `bannerResume`          | `bannerResume(adView: View?)`                                                                                                                                     | Gọi trong `onResume` của host.                                                         |
| `bannerPause`           | `bannerPause(adView: View?)`                                                                                                                                      | Gọi trong `onPause`.                                                                   |
| `bannerDestroy`         | `bannerDestroy(adView: View?)`                                                                                                                                    | Gọi trong `onDestroy` — detach listener trước khi destroy (chống orphaned impression). |

> **Contract duy nhất, không có ngoại lệ:** `autoManageLifecycle=true` (mặc định) → **KHÔNG** forward
> `bannerResume`/`bannerPause`/`bannerDestroy` (SDK tự hook lifecycle qua `BannerCoordinator`/`BannerTracker`).
> `autoManageLifecycle=false` → forward đủ cả 3 hàm. Đừng vừa để `true` vừa tự gọi tay 3 hàm trên — dù
> `bannerDestroy` có idempotent-guard (gọi tay thêm không crash), nó không phải lý do để làm cả hai.
> `AdBannerSize` (sealed class trong SDK, KHÔNG dùng AdMob `AdSize`): `Banner` / `LargeBanner` / `FullBanner` /
`MediumRectangle` (MREC) / `Leaderboard` là `object`; `Adaptive(widthDp)` là `data class` — lấy qua
`getAdaptiveBannerSize(activity)`.

### A.4 — Fullscreen (Interstitial / App Open / Rewarded)

| Hàm                        | Chữ ký                                                                                                                                           | Mô tả                                                                                                                                                                                                                   |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `loadInterstitial`         | `loadInterstitial(context: Context)`                                                                                                             | Preload interstitial (SDK cũng tự preload sau mỗi dismiss).                                                                                                                                                             |
| `showInterstitial`         | `showInterstitial(activity: Activity, onDoneFlow: (result: Boolean) -> Unit)`                                                                    | Show; callback **LUÔN fire** (kể cả khi AdSafety chặn → `false`).                                                                                                                                                       |
| `loadAppOpenAd`            | `loadAppOpenAd(context: Context, onAdLoaded: (Boolean) -> Unit)`                                                                                 | Load App Open (thường không cần gọi tay — SDK tự quản qua `ProcessLifecycleOwner`).                                                                                                                                     |
| `showAppOpenAd`            | `showAppOpenAd(activity: Activity, onAdDismiss: (Boolean) -> Unit, bypassSafety: Boolean = false, fireDismissWhenSafetyBlocked: Boolean = true)` | Show App Open thủ công (hiếm khi cần).                                                                                                                                                                                  |
| `loadRewarded`             | `loadRewarded(context: Context)`                                                                                                                 | Preload rewarded.                                                                                                                                                                                                       |
| `showRewarded`             | `showRewarded(activity: Activity, onRewardEarned: (Boolean) -> Unit)`                                                                            | Show rewarded. **CHỈ cấp reward khi `earned == true`.** Fallback interstitial KHÔNG được cấp reward (policy).                                                                                                           |
| `loadRewardedInterstitial` | `loadRewardedInterstitial(context: Context)`                                                                                                     | Preload rewarded interstitial. **AdMob-only** (1.5.0+) — dùng `admobRewardedInterstitialId`, ID riêng, KHÔNG trùng `admobRewardedId`.                                                                                   |
| `showRewardedInterstitial` | `showRewardedInterstitial(activity: Activity, onRewardEarned: (Boolean) -> Unit)`                                                                | Show. **AppLovin MAX permanent không hỗ trợ format này** (AAR không có API consumer) — trên AppLovin luôn fire `onAdNotAvailable`/`onRewardEarned(false)` ngay, không throw. Cùng policy cấp-reward như `showRewarded`. |
| `showInterstitialAsync`    | `suspend fun AdManager.showInterstitialAsync(activity: Activity, timeoutMs: Long = 30_000L): Boolean`                                            | `@ExperimentalAdApi` (extension, `AdManagerSuspend.kt`). Suspend wrap của `showInterstitial` — `true` nếu show+dismiss thành công, `false` nếu safety-block/không có ad/fail/timeout.                                   |
| `isAppOpenAdShowing`       | `isAppOpenAdShowing(): Boolean`                                                                                                                  | `true` nếu App Open đang hiển thị trên màn hình lúc gọi — dùng tránh mở Dialog/BottomSheet/navigate đè lên ad đang hiện.                                                                                                |
| `suppressAppOpenTemporarily` | `suppressAppOpenTemporarily(suppress: Boolean)`                                                                                               | Tạm khoá (true)/mở lại (false) App Open tự-động resume — dùng quanh Dialog/BottomSheet quan trọng cùng Activity đã cho phép App Open. Tự hết hạn sau 5 phút nếu quên gọi lại `false`. Không ảnh hưởng show thủ công.    |

**Coroutine extensions** (`AdManagerSuspend.kt`, tất cả `@ExperimentalAdApi` — cần `@OptIn(ExperimentalAdApi::class)`):

| Hàm                              | Chữ ký                                                                                                       | Mô tả                                                                                                          |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `initializeAsync`                | `suspend fun AdManager.initializeAsync(application: Application, timeoutMs: Long = 35_000L): Pair<Boolean, String?>?` | Suspend wrap của `initialize`. `null` nếu timeout.                                                       |
| `loadAppOpenAdAsync`             | `suspend fun AdManager.loadAppOpenAdAsync(context: Context, timeoutMs: Long = 15_000L): Boolean`              | Suspend wrap của `loadAppOpenAd`.                                                                              |
| `showAppOpenAdAsync`             | `suspend fun AdManager.showAppOpenAdAsync(activity: Activity, bypassSafety: Boolean = false, timeoutMs: Long = 10_000L): Boolean` | Suspend wrap của `showAppOpenAd`. `bypassSafety=true` dành cho splash.                       |
| `showRewardedAsync`              | `suspend fun AdManager.showRewardedAsync(activity: Activity, timeoutMs: Long = 120_000L): Boolean`            | Suspend wrap của `showRewarded`. `true` nếu user xem đủ + earn reward.                                          |
| `showRewardedInterstitialAsync`  | `suspend fun AdManager.showRewardedInterstitialAsync(activity: Activity, timeoutMs: Long = 120_000L): Boolean` | Suspend wrap của `showRewardedInterstitial`.                                                                    |

**Listener toàn cục** (gán 1 lần, ví dụ trong `Application.onCreate` sau `setConfig`, để nhận event
không cần đợi callback của từng lần `show*`): `AdManager.interstitialListener`, `.rewardedListener`,
`.bannerListener`, `.appOpenListener`, `.rewardedInterstitialListener` — mỗi cái là interface riêng
(`InterstitialAdListener`/`RewardedAdListener`/`BannerAdListener`/`AppOpenAdListener`/
`RewardedInterstitialAdListener`) với các hàm `onAdLoaded`/`onAdFailedToLoad`/`onAdShowed`/
`onAdDismissed`/`onAdClicked` (tuỳ loại). Dùng cho analytics/log tập trung một chỗ thay vì rải rác ở
từng callback `show*`.

**`AdError` — sealed class đầy đủ 8 loại** (dùng trong mọi `onAdFailedToLoad(error)`/`onAdFailedToShow(error)`):
`NoNetwork`, `VipDevice`, `SafetyBlocked(reason)`, `Cooldown(remainingMs)`, `SdkError(code, message)`,
`NoAdAvailable`, `Timeout`, `Exception(cause)`. `when (error)` PHẢI cover đủ 8 nhánh để exhaustive.
Có sẵn 2 helper khỏi tự viết `when`: `error.toLogString()` (string kỹ thuật cho log/analytics) và
`error.toUserMessage()` (message tiếng Việt sẵn cho Toast/Snackbar end-user).

### A.5 — VIP (offline, không server)

| Hàm                  | Chữ ký                                                                | Mô tả                                                                                                  |
|----------------------|-----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `activateVipByToken` | `activateVipByToken(context: Context, token: String): Boolean`        | **Đường chính** — kích hoạt VIP bằng token chữ ký ECDSA P-256. Trả `false` nếu chữ ký/hạn/binding sai. |
| `generateVipToken`   | `generateVipToken(privateKeyBase64: String, days: Int, …): String`    | Đúc token (offline, dùng private key — KHÔNG nhúng vào app). Debug-gated trong demo.                   |
| `generateVipKeyPair` | `generateVipKeyPair(): VipKeyPair`                                    | Sinh cặp khoá ECDSA (chạy 1 lần, offline). Dán public key vào `AdSdkConfig.vipTokenPublicKey`.         |
| `grantVipDays`       | `grantVipDays(context: Context, days: Int): Boolean`                  | Cấp VIP N ngày trực tiếp (dùng cho "xem quảng cáo → VIP"; chạy cả release, KHÔNG qua key).             |
| `isVIPMember`        | `isVIPMember(): Boolean`                                              | `true` nếu đang là VIP (mua/token/redeem/auto-trial).                                                  |
| `isVipByKeyActive`   | `isVipByKeyActive(): Boolean`                                         | `true` nếu VIP-by-key hoặc auto-trial còn hạn (dùng cho badge/toolbar).                                |
| `getVipByKeyExpiry`  | `getVipByKeyExpiry(): Long`                                           | Timestamp hết hạn VIP (0 = chưa/đã hết).                                                               |
| `clearVipByKey`      | `clearVipByKey()`                                                     | Thu hồi VIP-by-key + auto-trial (debug/reset).                                                         |
| `activateVipByKey`   | `activateVipByKey(context: Context, key: String, days: Int): Boolean` | ‹Legacy — chỉ khi consumer < 1.2.0, opt-in `allowLegacyPlaintextVipKey`›. Dùng token thay thế.         |
| `showVipByKeyDialog` | `showVipByKeyDialog(activity: Activity)`                              | Dialog verify/success/failed dựng sẵn cho flow legacy ở trên (khỏi tự code UI dialog nhập key).       |
| `addVIPMember`       | `addVIPMember(listGaidDevice: List<String>)`                          | Whitelist thêm GAID vào danh sách auto-VIP (test device/QA).                                            |
| `deleteVIPMember`    | `deleteVIPMember(listGaidDevice: List<String>)`                       | Gỡ GAID khỏi whitelist auto-VIP (đối xứng `addVIPMember`).                                              |
| `getMyListVipDevice` | `getMyListVipDevice(): ArrayList<String>`                             | Đọc `AdSdkConfig.vipDeviceGaids` đã cấu hình (config-driven, KHÔNG gồm các lần `addVIPMember`/`deleteVIPMember` runtime — 2 hàm đó mutate whitelist runtime riêng, không ghi ngược vào `adConfig`). |

### A.6 — Chẩn đoán

| Hàm                     | Chữ ký                                        | Mô tả                                                                                                      |
|-------------------------|-----------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `getDiagnostics`        | `getDiagnostics(): String`                    | In trạng thái (consent, init state, VIP, provider, lý do ad bị chặn). **Đính kèm output này khi báo lỗi.** |
| `getPrivacyDiagnostics` | `getPrivacyDiagnostics(): PrivacyDiagnostics` | Snapshot privacy state (`appliedDoNotSell`, TCF…).                                                         |
| `validateConfig`        | `validateConfig(): List<AdConfigIssue>`       | List các vấn đề cấu hình phát hiện được. ⚠️ SDK **cố ý** chỉ ERROR trên vài field luôn bắt buộc (vd `applovinSdkKey`) — KHÔNG tự ép đủ mọi ad-unit ID (không phải app nào cũng dùng đủ 4 loại). **Khuyến nghị:** gọi hàm này trong CI release-gate của CHÍNH APP BẠN (assert `severity != ERROR`, và tự thêm check cho đúng ad-unit ID app bạn thật sự dùng) — SDK không tự làm thay được. |
| `isVipTokenPending`     | `val isVipTokenPending: Boolean`              | `true` khi 1 token VIP device-bound đã nhập TRƯỚC lúc `initialize()` xong, đang chờ resolve ngầm (xem `setPendingVipTokenResultListener` ở A.8.3) — dùng hiện UI "đang xác minh…" thay vì coi `activateVipByToken()` trả `false` tạm thời là token sai. |
| `isVipTokenResolutionInFlight` | `val isVipTokenResolutionInFlight: Boolean` | `true` suốt **toàn bộ** vòng đời xử lý 1 token: từ lúc SDK nhận token (xếp hàng chờ init/chờ mạng) tới khi listener nhận kết quả — **kể cả đoạn đang verify chữ ký**, đoạn mà `isVipTokenPending` đã tắt. Đây là cờ dùng để **khoá nút / chặn user gửi token thứ hai** khi token trước chưa xong (SDK chỉ giữ ĐÚNG MỘT slot pending, token thứ hai không có cách nào tương ứng kết quả về đúng chỗ). Dùng `isVipTokenPending` cho việc hiện chữ "đang xử lý", dùng cờ này cho việc khoá thao tác — hai câu hỏi khác nhau. |

`SdkVersion` (`object`, không qua `AdManager`) — version metadata cho log/analytics/support:
`SdkVersion.VERSION_NAME` (String, semver, dạng `"x.y.z"` — đọc giá trị thật lúc runtime, đừng hardcode
số bạn thấy ở file này vì đã có thể lỗi thời; luôn đối chiếu tag mới nhất tại JitPack), `VERSION_CODE`
(Int, dạng `x_yyy_zzz` — mã hoá major/minor/patch),
`SDK_NAME` (`"AdmobApplovinWrapper"`), `MAVEN_COORDINATE` (full JitPack coordinate string). Ví dụ:
`Log.d("App", "Ads SDK ${SdkVersion.VERSION_NAME}")` hoặc
`crashlytics.setCustomKey("ads_sdk_version", SdkVersion.VERSION_NAME)`.

### A.7 — `AdSdkConfig` — field quan trọng nhất

> 💡 Ngoài constructor 42-param, có thể khởi tạo qua DSL builder `adSdkConfig { ... }` (property
> syntax, dễ đọc/autocomplete hơn) — hai param bắt buộc `isEnableAdmob`/`isDebug` truyền ở lời gọi,
> field còn lại set trong block:
> ```kotlin
> val config = adSdkConfig(isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB, isDebug = BuildConfig.DEBUG) {
>     admobBannerId = BuildConfig.ADMOB_BANNER_ID
>     applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY
>     vipKeySecret = "YOUR_APP_SECRET"
> }
> ```
> Tương đương 100% với gọi `AdSdkConfig(...)` trực tiếp — chỉ là cách khởi tạo thay thế, không đổi hành vi.

| Field                                                                                      | Type                        | Default            | Ghi chú                                                                                                                     |
|--------------------------------------------------------------------------------------------|-----------------------------|--------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `isEnableAdmob`                                                                            | Boolean                     | — (bắt buộc)       | `true` = AdMob, `false` = AppLovin MAX.                                                                                     |
| `isDebug`                                                                                  | Boolean                     | — (bắt buộc)       | Bật `SafeLogger` verbose.                                                                                                   |
| `admobBannerId` / `admobInterstitialId` / `admobAppOpenId` / `admobRewardedId`             | String                      | `""`               | Bắt buộc nếu dùng AdMob.                                                                                                    |
| `admobRewardedInterstitialId`                                                              | String                      | `""`               | AdMob-only (1.5.0+), ID riêng (không trùng `admobRewardedId`). AppLovin không có field tương đương — permanent unsupported. |
| `applovinSdkKey`                                                                           | String                      | `""`               | Bắt buộc nếu AppLovin.                                                                                                      |
| `applovinBannerId` / `applovinInterstitialId` / `applovinAppOpenId` / `applovinRewardedId` | String                      | `""`               | Bắt buộc nếu AppLovin.                                                                                                      |
| `vipKeySecret`                                                                             | String                      | `"TPLdev0402dev@"` | **PHẢI đổi** — release CRASH nếu để mặc định. Dùng chống-tamper prefs.                                                      |
| `vipTokenPublicKey`                                                                        | String                      | cặp khoá MẪU       | **PHẢI đổi nếu dùng VIP-token** — không đổi → từ chối IM LẶNG mọi token ở release.                                          |
| `applovinHasUserConsent`                                                                   | Boolean?                    | `null`             | Set thủ công cho nongms EEA (trước `initialize()`).                                                                         |
| `allowAdsWhenConsentUnknownNongms`                                                         | Boolean                     | `false`            | nongms non-EEA: `true` = phục vụ NPA khi chưa có consent.                                                                   |
| `appOpenExcludedActivities`                                                                | `List<Class<out Activity>>` | `emptyList()`      | Khai Splash/Onboarding để App Open không đè: `listOf(SplashActivity::class.java)`.                                          |
| `safety`                                                                                   | `AdSafetyLimits`            | `AdSafetyLimits()` | Ngưỡng AdSafety — preset `GAME`/`UTILITY`/`CONTENT`/`TEST`.                                                                 |
| `vipRedeemCodes`                                                                           | `Map<String,Int>`           | `emptyMap()`       | "Thẻ cào" mã cố định → số ngày (kém an toàn, mã nằm trong app).                                                             |
| `applovinUseNativeCmp`                                                                     | Boolean                     | `false`            | `true` = AppLovin Terms Flow (sinh IAB TCF/AC). Cần `applovinPrivacyPolicyUrl`.                                             |
| `skipAppOpenOnFirstEverLaunch`                                                             | Boolean                     | `true`             | Mặc định bỏ qua App Open ở lần mở app đầu tiên sau cài đặt (tránh đè onboarding). Xem cảnh báo ở Bước 4.                    |
| `tagForChildDirectedTreatment`                                                             | Boolean?                    | `null`             | `true` = app hướng trẻ em (COPPA). ⚠️ **CRASH** ở `setConfig()` nếu effective provider là AppLovin — xem Accepted risk.     |
| `tagForUnderAgeOfConsent`                                                                  | Boolean?                    | `null`             | `true` = user under-age-of-consent (GDPR-K). ⚠️ **CRASH** trên AppLovin/`nongms` — chỉ AdMob mode honor đúng cờ này.        |
| `maxAdContentRating`                                                                       | String?                     | `null`             | Giới hạn rating nội dung ad (vd `"G"`/`"PG"`/`"T"`/`"MA"`, theo hằng số AdMob `RequestConfiguration`).                      |

---

### A.8 — API ít ai biết (dễ bỏ sót, dễ tự implement lại nhầm)

Các public API dưới đây partner hay bỏ sót vì không nằm trong luồng chính Bước 1–5 — dễ tưởng SDK
không hỗ trợ rồi tự implement lại (hoặc mất luôn tính năng).

#### A.8.1 — Doanh thu & mediation

| API                                       | Dùng để làm gì                                                                                                                                                                                                                                                                                              |
|-------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AdSdkConfig(analyticsReporter = …)`      | Forward **ad revenue sang MMP** (Adjust / AppsFlyer / Firebase Analytics). Implement `AdAnalyticsReporter.onAdRevenue(AdRevenueEvent)`. Chạy **song song** với `paidEventListener`, không thay thế. **Không có cái này = không đo được ROAS.** `AdRevenueEvent` mang `adType, valueMicros, currency, precision, adSource, adUnit, provider` + `creativeId`, `networkPlacement`, `latencyMs` (3 field sau nullable — mạng nào không trả thì `null`; `creativeId` giúp tra ngược đúng creative khi bị policy-strike). ⚠️ `precision` chỉ đúng ở **bản mới nhất trên JitPack**: các bản cũ nhánh AppLovin luôn gửi `"ESTIMATED"` cứng dù MAX có trả giá trị thật, nên số liệu LTV/ROAS dựng trên field này ở bản cũ là sai — và sai theo kiểu app không tự phát hiện được.                                                              |
| `AdSdkConfig(applovinMrecId = …)`         | Ad-unit **MREC 300×250** của AppLovin MAX — là format RIÊNG, KHÔNG dùng chung `applovinBannerId`. Để trống ⇒ `loadBanner(adSize = AdBannerSize.MediumRectangle)` trả `null` (SDK cố ý không âm thầm hạ xuống banner thường).                                                                                |
| `AdSdkConfig(applovinLeaderId = …)`       | Ad-unit **Leaderboard 728×90** (tablet). Cùng quy tắc như MREC.                                                                                                                                                                                                                                             |
| `AdSdkConfig(grantRewardOnEarn = …)`      | Fire `onRewardEarned` **ngay khi user đủ điều kiện** thay vì đợi đóng ad ⇒ **không mất thưởng** nếu app bị kill giữa chừng. **Mặc định `true` từ 1.6.1** (trước đó `false`). Callback vẫn đảm bảo fire đúng 1 lần mỗi lần show. Đặt `false` để giữ hành vi ≤ 1.5.5.                                         |
| `AdManager.rewardTransactionId` *(1.6.1)* | ID duy nhất của lần show rewarded hiện tại/gần nhất. Một lần user xem xong, SDK báo qua **3 kênh** (callback `showRewarded`, `rewardedListener.onUserEarnedReward`, `AdEvent.Rewarded`) — cộng thưởng ở từ 2 kênh trở lên là user nhận đôi. Dùng ID này làm khoá dedup (persist để sống qua process death). |

#### A.8.2 — Chẩn đoán & vận hành

| API | Dùng để làm gì |
|---|---|
| `AdManager.setTestDeviceIds(vararg ids)` / `getTestDeviceIds()` | **BẮT BUỘC trước khi QA** — xem Bước 3b. Không khai = QA click ad thật = invalid traffic = rủi ro khoá tài khoản. |
| `AdManager.isWaitingForConsent()` | Phân biệt `initialize(success=false)` là **lỗi thật** hay chỉ **đang park chờ consent** — xem Bước 4. |
| `AdManager.getGaid()` | Lấy lại GAID khi callback `initialize()` đã bị watchdog flush với `gaid = null` mà consent về muộn. |
| `AdManager.getGAID(context, callback)` | Overload cũ — **callback chạy trên background/IO thread**, tự `post` về main nếu đụng UI. Ưu tiên dùng `getGaid()` (đồng bộ, main thread) trừ khi bạn cố tình cần async. |
| `AdManager.destroy()` | Reset **toàn bộ** SDK (mọi ad instance đã preload của cả app, không phải 1 Activity). Dùng cho test/instrumentation cần state sạch, **và cho flow logout/switch-account thật** (gọi `destroy()` rồi `setConfig()+initialize()` lại, KHÔNG kill process — SDK support chính thức, tự flush mọi callback đang treo fail-soft). ⚠️ Các bản CŨ có lỗi: nếu `destroy()` rơi đúng lúc Splash (`initSplashScreen`/`awaitSplashComplete`) hoặc token VIP device-bound còn đang chờ resolve, callback đó có thể treo vĩnh viễn — **dùng bản mới nhất trên JitPack** nếu app có gọi `destroy()` trong vòng đời thật. KHÔNG gọi tuỳ tiện giữa phiên bình thường (không phải logout) — mất hết ad đã preload, muốn dùng lại phải `setConfig`+`initialize` lại từ đầu. |
| `AdManager.getSafetyStatus()` / `getSafetyLimits()` | Trả lời "vì sao ad không hiện": đang trong suspicious-pause? chạm cap giờ/ngày? chưa đủ min-session? |
| `AdManager.getStateSnapshot()` | Ảnh chụp trạng thái toàn SDK (ad nào ready/loading, lỗi gần nhất) — tiện cho màn debug nội bộ. |
| `AdManager.getPrivacyDiagnostics()` | Tín hiệu privacy **THÔ** (raw UMP consentStatus, TCF, `appliedDoNotSell`…), không suy diễn. ⚠️ Chỉ để debug — **không phải bằng chứng pháp lý** về consent. |
| `AdManager.openAdInspector(context) { }` | Mở **AdMob Ad Inspector** (soi waterfall/mediation ngay trên máy). Debug-only: release luôn trả `AdError.SdkError`. |
| `AdManager.retryConsentIfNeeded(activity)` | Fetch lại consent khi lần trước lỗi (mở app lần đầu offline). **SDK đã tự gọi** ở `onActivityResumed` + khi mạng về — chỉ gọi tay nếu bạn có luồng riêng. |

#### A.8.3 — Consent & VIP nâng cao

| API | Dùng để làm gì |
|---|---|
| `AdManager.needsGdprConsentState` (`StateFlow<Boolean>`) | Bản StateFlow của `AdEvent.NeedsGdprConsent` — **subscriber muộn vẫn nhận được trạng thái hiện tại** (collect sau khi event đã bắn thì Flow thường bị lỡ). Dùng cho luồng dialog consent tự làm trên nongms. |
| `AdManager.setPendingVipTokenResultListener { activated -> }` | Token VIP **device-bound** nhập TRƯỚC khi `initialize()` xong sẽ bị queue và `activateVipByToken` trả `false` ngay (chưa biết kết quả thật), rồi resolve ngầm sau đó. Không set listener này = **không bao giờ biết kết quả thật**. |
| `AdSdkConfig(rejectLegacyV2Token = true)` | Hard-cutover: từ chối token format **v2 cũ** (không bind `applicationId`, dùng chéo được giữa các app chung keypair). ⚠️ Chỉ bật khi chắc chắn mọi token v2 đã phát hành đều đã hết hạn. |
| `AdSdkConfig(consentFormWatchdogMs = …)` | Timeout **giai đoạn 2** của watchdog consent (mặc định 180 000 ms) — áp dụng khi UMP form đang hiển thị cho user đọc. Khác `consentWatchdogMs` (giai đoạn 1, canh network fetch, mặc định 15 000 ms). Đừng hạ thấp: sẽ cắt splash khi user còn đang đọc form. |
| `@Composable AdBanner(modifier, adSize)` | Banner cho **Jetpack Compose** — tự lo inflate + `loadBanner(autoManageLifecycle = true)` + destroy khi rời composition. `@ExperimentalAdApi`, yêu cầu `LocalContext` là Activity. |

#### A.8.4 — Placement API (`@ExperimentalAdApi`)

Thay vì rải `loadInterstitial(context)` / `showRewarded(activity) { }` ở nhiều màn hình, đặt tên 1 lần
bằng `AdPlacement` rồi gọi qua `AdManager.loadForPlacement`/`showForPlacement` — 2 hàm này chỉ switch
theo `AdPlacement.adType` rồi gọi đúng hàm load/show hiện có, **không** thêm behavior mới, **không**
fetch config từ server (ad-unit ID/tần suất vẫn qua `AdSdkConfig` như cũ):

```kotlin
@OptIn(ExperimentalAdApi::class)
AdManager.loadForPlacement(context, AdPlacement.LevelEnd)

@OptIn(ExperimentalAdApi::class)
AdManager.showForPlacement(activity, AdPlacement.PaywallExit) { shown -> navigateNext() }
```

Sẵn `AdPlacement.LevelEnd`/`PaywallExit`/`HomeBanner` (đều map interstitial trừ `HomeBanner`) +
`AdPlacement.Custom(tag, type)` cho placement tự đặt tên với bất kỳ `AdType` nào. `AdType.BANNER`/
`SYSTEM` **không** hỗ trợ (ném `IllegalArgumentException`) — gọi thẳng `loadBanner()` cho banner.
**Callback `onDone` khác ý nghĩa giữa các loại**: INTERSTITIAL/REWARDED/REWARDED_INTERSTITIAL —
`true` = ad đã show xong; APP_OPEN — KHÔNG phải kết quả show, SDK cố ý fire `true` ở hầu hết nhánh bị
chặn để không treo navigation, chỉ đọc như "có thể điều hướng tiếp".

#### A.8.5 — Revenue Attribution Kit (`@ExperimentalAdApi`, XC-01)

3 extension function thuần trên `AdRevenueEvent` (không import SDK của MMP nào — trả `Map` thuần,
bạn tự nối 3 dòng vào SDK MMP mình đã có), biến `AdRevenueEvent` thành đúng payload từng MMP mong đợi
cho **cả AdMob lẫn AppLovin** (khỏi tự viết map field 2 lần, rồi lỡ sai như từng xảy ra với `precision`
bị gắn cứng `"ESTIMATED"` — xem mục "🔔 ĐANG DÙNG BẢN CŨ" đầu file):

```kotlin
AdSdkConfig(analyticsReporter = AdAnalyticsReporter { event ->
    val p = event.toAdjustPayload()          // hoặc .toAppsFlyerPayload() / .toFirebaseAdImpressionParams()
    // forward p vào Adjust/AppsFlyer/Firebase SDK bạn đã có — xem ví dụ đầy đủ trong KDoc từng hàm.
})
```

| Hàm | Dùng cho | Ghi chú |
|---|---|---|
| `AdRevenueEvent.toAdjustPayload()` | `AdjustAdRevenue` | `revenue` là đơn vị tiền tệ thật (không phải micros). |
| `AdRevenueEvent.toAppsFlyerPayload()` | `AppsFlyerAdRevenue.logAdRevenue(...)` | `additionalParams` gom `ad_format`/`creative_id`/`precision`/`latency_ms`, bỏ qua field `null`. |
| `AdRevenueEvent.toFirebaseAdImpressionParams()` | Firebase `AD_IMPRESSION` event | `value` là Double đơn vị tiền tệ — đẩy nhầm micros vào sẽ thổi phồng số liệu 1 triệu lần. |

---

## Phụ lục B — File mẫu copy-paste (MyApplication + SplashActivity)

> Copy 2 file này là chạy được **đường init tối thiểu** — không phải ghép từ nhiều Bước rời rạc.
> Thay `com.yourapp` bằng package thật, và thay các giá trị đánh dấu `// ⚠️ THAY` bằng credential của bạn
> (hoặc kéo qua `BuildConfig.buildConfigField(...)` như demo — `app/ads.properties` là file config
> gitignored riêng của demo trong repo SDK, KHÔNG phải file bạn cần tạo; bạn tự quyết định nguồn giá trị
> `BuildConfig` của app mình). Code snippet dưới đây rút gọn cho dễ đọc — bản đầy đủ 100%, chạy được
> thật, xem trực tiếp tại GitHub (repo public, không cần quyền gì):
> [`app/build.gradle.kts`](https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/build.gradle.kts) ·
> [`MyApplication.kt`](https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/src/main/java/com/roy/admobwrapper/MyApplication.kt) ·
> [`SplashActivity.kt`](https://github.com/royt93/AdmobApplovinWrapper/blob/main/app/src/main/java/com/roy/admobwrapper/SplashActivity.kt)

### B.1 — `MyApplication.kt`

```kotlin
package com.yourapp

import android.app.Application
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSdkConfig
import com.roy.sdkadbmob.PaidEventListener

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val adConfig = AdSdkConfig(
            // ⚠️ THAY: true = AdMob, false = AppLovin MAX. Nên đưa qua BuildConfig.IS_ENABLE_ADMOB.
            isEnableAdmob = BuildConfig.IS_ENABLE_ADMOB,
            isDebug = BuildConfig.DEBUG,

            // ⚠️ THAY: ad-unit ID thật (debug dùng Google test ID, release dùng ID production).
            // Kéo qua BuildConfig để tách debug/release — đừng hardcode ID release vào source.
            admobAppOpenId = BuildConfig.ADMOB_APP_OPEN_ID,
            admobInterstitialId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobBannerId = BuildConfig.ADMOB_BANNER_ID,
            admobRewardedId = BuildConfig.ADMOB_REWARDED_ID,
            applovinAppOpenId = BuildConfig.APPLOVIN_APP_OPEN_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinBannerId = BuildConfig.APPLOVIN_BANNER_ID,
            applovinRewardedId = BuildConfig.APPLOVIN_REWARDED_ID,
            applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY,       // ⚠️ THAY

            // Consent: để null = chưa có quyết định → consent flow (UMP ở Splash / dialog nongms) xử lý.
            //  - gms  : giữ null, UMP ở SplashActivity sẽ quyết định.
            //  - nongms: HOẶC set true/false ở đây TRƯỚC initialize(), HOẶC giữ null rồi gọi
            //            AdManager.confirmGdprConsent(context, hasConsent) lúc runtime (xem B.2).
            applovinHasUserConsent = null,
            applovinPrivacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,  // ⚠️ THAY (link Privacy Policy thật)

            // ⚠️ THAY: HMAC secret chống-tamper SharedPreferences + legacy plaintext-key path
            // (release fail-build nếu trống). KHÔNG dùng để ký/verify VIP token — token dùng ECDSA
            // qua vipTokenPublicKey bên dưới. Đưa qua BuildConfig.
            vipKeySecret = BuildConfig.VIP_KEY_SECRET,
            // ⚠️ THAY: public key ECDSA verify VIP token (private key giữ OFFLINE, không đưa vào app).
            vipTokenPublicKey = BuildConfig.VIP_TOKEN_PUBLIC_KEY,

            // Splash/Onboarding của bạn — App Open Ad không đè lên các Activity này.
            // ⚠️ Nếu Activity splash của bạn KHÔNG tên `SplashActivity`, field cũ deprecated
            // `appOpenExcludedActivityNames` (default ["SplashActivity"]) vẫn OR-combine với field
            // này — xem mục "App Open exclusion" trong Security Checklist bên dưới.
            appOpenExcludedActivities = listOf(SplashActivity::class.java),
        )

        // ĐÚNG 2 lệnh SDK, đúng thứ tự này. KHÔNG tự gọi thêm earlyInit/MobileAds.initialize/…
        AdManager.setConfig(adConfig)                       // 1) main thread, ĐẦU TIÊN

        // ⚠️ BẮT BUỘC set NGAY TẠI ĐÂY (Application.onCreate), KHÔNG set từ Activity — SDK gắn "chủ sở
        // hữu" listener là Activity foreground lúc set và tự xoá khi Activity đó destroy (chống leak),
        // nên set trong Activity = mất revenue tracking âm thầm. Chữ ký thật: 5 tham số qua lambda, xem
        // Bước 4.
        AdManager.paidEventListener = PaidEventListener { adType, valueMicros, currency, precision, adSource ->
            // forward lên Firebase/analytics của bạn
        }

        AdManager.initialize(this) { success, gaid ->       // 2) provider init defer tới khi có consent
            // (tùy chọn) log/analytics; gaid có thể null nếu chưa lấy được.
        }
    }
}
```

### B.2 — `SplashActivity.kt`

```kotlin
package com.yourapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.ExperimentalAdApi
import com.roy.sdkadbmob.awaitSplashComplete
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    @OptIn(ExperimentalAdApi::class)   // awaitSplashComplete là suspend @ExperimentalAdApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // gms path: UMP chạy trước (tự có watchdog nội bộ, fire callback đúng 1 lần).
        // Ngoài EEA/UK/CH → canRequestAds=true gần như tức thì, không hiện form.
        AdManager.requestConsentInfoUpdate(this) { canRequestAds ->
            lifecycleScope.launch {
                // SDK tự load + show App Open + timeout (splashHardCapMs); suspend tới khi xong.
                AdManager.awaitSplashComplete(this@SplashActivity)
                goToMain()
            }
        }

        // ── Biến thể callback (KHÔNG cần @OptIn experimental) — thay 2 lệnh trong lambda trên bằng:
        //   AdManager.initSplashScreen(this) { goToMain() }
        //
        // ── nongms path (không có UMP): đừng gọi requestConsentInfoUpdate cho consent.
        //   HOẶC: đã set adConfig.applovinHasUserConsent = true/false TRƯỚC initialize() (B.1) → gọi thẳng:
        //         lifecycleScope.launch { AdManager.awaitSplashComplete(this@SplashActivity); goToMain() }
        //   HOẶC: hiện dialog consent của bạn, rồi AdManager.confirmGdprConsent(this, hasConsent)
        //         (resume provider init đang chờ) trước khi awaitSplashComplete.
    }

    private fun goToMain() {
        startActivity(android.content.Intent(this, MainActivity::class.java))
        finish()
    }
}
```

### B.3 — Manifest (2 dòng đừng quên)

- Khai App Open exclusion đã làm ở code (`appOpenExcludedActivities` trong B.1) — không cần khai ở manifest.
- **AdMob App ID** phải khai `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" .../>` trong
  `AndroidManifest.xml` (gms) — chi tiết + code mẫu ở **Bước 3a**.

> Đây là mẫu tối thiểu — chi tiết banner/interstitial/rewarded/VIP xem **Bước 5** + **Phụ lục A**.


---

## Cơ chế Trial / VIP / Log (tóm tắt)
- **Trial**: SDK tự cấp VIP dùng thử **1 ngày** ở lần cài đầu (chỉ release). Chống lạm dụng qua Install Referrer (gms). Không cần config; chạy ngầm. *Lưu ý: trên nongms (Huawei), reinstall có thể reset trial.* ⚠️ Lớp chống gỡ-cài-cài-lại dựa trên giả định **chưa kiểm chứng trên máy thật** rằng Play giữ nguyên mốc cài gốc sau khi cài lại — đừng dựa vào nó cho thứ có giá trị tiền thật (M-03).
- **VIP** (user không thấy ad). Có các cách kích hoạt, xếp theo độ an toàn:

  | Cách | Cấu hình / API | Khi nào dùng | An toàn |
  |---|---|---|---|
  | Whitelist máy | `vipDeviceGaids = listOf("<gaid>")` hoặc `AdManager.addVIPMember(listOf(gaid))` | Máy test của team | Cao (chỉ máy bạn) |
  | 🎫 Vé token | `generateVipToken(privateKeyBase64, days)` → `activateVipByToken(ctx, token)` | Bán/tặng VIP cho user thật, cần chống giả | **Cao** (chữ ký **ECDSA P-256** — private key KHÔNG có trong APK; xem chú thích 3) |
  | 🎟️ **Thẻ cào** | `vipRedeemCodes = mapOf("abc" to 69, ...)` → user nhập mã | Mã khuyến mãi đơn giản, mã đẹp tự đặt | **Thấp** (mã nằm trong app, share được) |
  | Mật khẩu tĩnh (cũ) | `activateVipByKey(ctx, key, days)` | Tương thích ngược | Thấp |

  **Thẻ cào (1.2.6+)** — đúng kiểu "nhập mã → nhận N ngày VIP" mà nhiều app dùng:
  ```kotlin
  AdSdkConfig(
      // ...
      vipRedeemCodes = mapOf(
          "abc" to 69,        // user nhập "abc"  → 69 ngày VIP (tính từ lúc nhập)
          "TET2026" to 30,    // tự đặt mã & số ngày tuỳ ý
      ),
  )
  ```
  User vào màn VIP → nhập mã vào ô "Kích hoạt" → nhận đúng số ngày. Khi thành VIP, banner đang hiển thị bị huỷ ngay.

  **Tự build UI?** Mã thẻ cào kích hoạt qua chính API VIP — truyền thẳng chuỗi user nhập (cả 2 hàm đều nhận diện mã thẻ cào; `days` bị bỏ qua vì số ngày lấy từ `vipRedeemCodes`):
  ```kotlin
  val ok = AdManager.activateVipByKey(context, userInput.trim(), days = 0)
  // hoặc dùng chung ô nhập với token:
  val ok2 = AdManager.activateVipByToken(context, userInput.trim())
  if (ok) refreshUiForVipState()
  ```
  **"Xem quảng cáo → N ngày VIP" (reward ad):** dùng `AdManager.grantVipDays(context, days)` — API cấp
  VIP từ **nguồn tin cậy nội bộ** (reward đã earned / IAP đã verify), KHÔNG qua verify key/token, **chạy
  cả release** không phụ thuộc `allowLegacyPlaintextVipKey`. ⚠️ ĐỪNG hack bằng
  `activateVipByKey(ctx, vipKeySecret, 3)` — nhánh legacy plaintext mặc định **TẮT** nên nó fail im lặng.
  ```kotlin
  AdManager.showRewarded(activity) { earned -> if (earned) AdManager.grantVipDays(activity, 3) }
  ```

  **⚠️ Chọn thẻ cào hay token? (đọc kỹ trước khi giao đối tác quan trọng)**

  | | 🎟️ Thẻ cào | 🎫 Token |
  |---|---|---|
  | Mã | Tự đặt, đẹp, cố định, dùng mãi | Máy tạo (~68 ký tự), tự hết hạn |
  | Thời hạn | N ngày **kể từ lúc nhập** | Hết hạn **cố định** (từ lúc đúc token) → phải đúc lại định kỳ |
  | Nếu mã lộ/bị share | **Hại vĩnh viễn** (mọi máy redeem được) | Hại tới ngày hết hạn rồi tự chết; bản "khóa máy" thì vô dụng trên máy khác |
  | Chống lạm dụng | **YẾU** | Khá (đặc biệt bản khóa máy) |

  **Rủi ro của thẻ cào (KHÔNG sửa được khi không có server) — chấp nhận trước khi dùng:**
  - User **Xóa dữ liệu app / gỡ-cài rồi cài lại** → danh sách "mã đã dùng" bị xóa → **redeem lại cùng mã** → VIP miễn phí lặp vô hạn.
    ⚠️ **SDK KHÔNG tự loại prefs khỏi Android Backup** (SDK là library, không tự áp được rule backup lên app host của bạn) —
    **bạn (partner) phải tự thêm** `backup_rules.xml` + `data_extraction_rules.xml` loại trừ đúng file
    SharedPreferences tên **`loitp_admob`** (tên cố định SDK tự đặt nội bộ, không đổi theo
    `applicationId`/package name của app bạn, không phải API public — chỉ cần biết TÊN FILE để loại trừ
    trong XML dưới, không gọi được từ code) nếu app bạn để `android:allowBackup="true"` (mặc định Android), nếu không
    Auto Backup/Google Drive/device-transfer sẽ phục hồi ledger "mã đã dùng" về trạng thái cũ — đúng lỗ
    hổng dòng này đang cảnh báo. KHÔNG cứu được clear-data thật dù đã loại backup.

    Copy-paste 2 file dưới (tạo mới trong `res/xml/` nếu app bạn chưa có sẵn, hoặc merge `<exclude>` này
    vào rule đã có):
    ```xml
    <!-- res/xml/backup_rules.xml (Android ≤ 12, android:fullBackupContent) -->
    <?xml version="1.0" encoding="utf-8"?>
    <full-backup-content>
        <exclude domain="sharedpref" path="loitp_admob.xml" />
    </full-backup-content>
    ```
    ```xml
    <!-- res/xml/data_extraction_rules.xml (Android 12+, android:dataExtractionRules) -->
    <?xml version="1.0" encoding="utf-8"?>
    <data-extraction-rules>
        <cloud-backup>
            <exclude domain="sharedpref" path="loitp_admob.xml" />
        </cloud-backup>
        <device-transfer>
            <exclude domain="sharedpref" path="loitp_admob.xml" />
        </device-transfer>
    </data-extraction-rules>
    ```
    Khai trong `AndroidManifest.xml` (`<application>` tag, cả 2 attribute cùng lúc để cover mọi API level):
    ```xml
    <application
        android:fullBackupContent="@xml/backup_rules"
        android:dataExtractionRules="@xml/data_extraction_rules"
        ...>
    ```
  - 1 mã lộ lên mạng → mọi máy chưa dùng đều redeem được; không thu hồi, không đếm được số lần phát.

  **Khuyến nghị:**
  - VIP **khuyến mãi nhỏ / giá trị thấp** → thẻ cào (tiện, mã đẹp).
  - VIP **bán/giá trị cao / đối tác quan trọng** → **token** (ưu tiên bản khóa máy
    `generateVipToken(privateKeyBase64, days, bindToCurrentDevice = true)` — mỗi user 1 mã riêng). Token còn tự bind
    `applicationId` (default `bindToApplication=true`) → không dùng chéo app khác cùng keypair. ⚠️ token đúc ở **debug
    ** (package `.debug`) sẽ bị từ chối ở **release** (khác packageName) — đúc token phát hành với đúng packageName
    release.
  - Cần kiểm soát tuyệt đối (đếm/thu hồi) → **bắt buộc có server validation** (ngoài phạm vi SDK offline này).
  - ⚠️ "SDK offline" ở đây nghĩa là **không cần server riêng** (mã ký cục bộ bằng ECDSA/HMAC) — KHÔNG phải "không cần
    Internet lúc nhập mã". `activateVipByKey`/`activateVipByToken` đều kiểm tra mạng tại thời điểm gọi (network
    checker) và trả `false` nếu máy đang offline lúc đó; đừng test redeem trong lúc tắt mạng rồi tưởng SDK lỗi.
- **Log**: bật bằng `AdSdkConfig(isDebug = BuildConfig.DEBUG)` — release tự tắt sạch log. Theo dõi sự kiện ad qua
  `Flow<AdEvent>` (`AdManager.adEvents`), doanh thu qua `paidEventListener`, lỗi qua `errorReporter`:
  ```kotlin
  AdManager.errorReporter = ErrorReporter { throwable, context ->
      // context là string ngắn mô tả site lỗi (vd "bannerDestroy") — forward lên Crashlytics/Sentry.
      FirebaseCrashlytics.getInstance().apply {
          setCustomKey("ad_context", context)
          recordException(throwable)
      }
  }
  ```
  Gọi 1 lần sau `setConfig()`. Reporter KHÔNG được throw (SDK swallow im lặng nếu throw, tránh feedback loop).

  `AdEvent` là `sealed class` **13 subtype** — kiến trúc "1 Flow thay 5 listener rời rạc" (dùng khi
  muốn xử lý tập trung 1 chỗ thay vì 5 interface listener ở A.4):

  | Subtype | Field | Ý nghĩa |
  |---|---|---|
  | `Loaded(type)` | `type: AdType` | Ad đã load xong, sẵn sàng show. |
  | `Shown(type)` | `type` | Ad vừa hiển thị lên màn hình. |
  | `Dismissed(type)` | `type` | User đóng ad. |
  | `Clicked(type)` | `type` | User click vào ad. |
  | `Failed(type, error)` | `type`, `error: AdError` | Load thất bại. |
  | `ShowFailed(type, error)` | `type`, `error` | Show thất bại (đã load nhưng không hiện được). |
  | `Rewarded(type, …)` | | User earn reward (rewarded/rewarded-interstitial). |
  | `Impression(type)` | `type` | Ghi nhận impression (dùng cho revenue reconciliation). |
  | `HeightChanged(type, heightDp)` | `heightDp: Int` | Banner đổi chiều cao (adaptive banner). |
  | `NeedsGdprConsent` | (object, không field) | Cần hiện lại dialog consent — xem A.8.3. |
  | `ConsentStateChanged(state)` | `state` | Xem chi tiết ngay dưới. |
  | `GaidAvailable(gaid)` | `gaid: String?` | GAID lấy được muộn (sau khi callback `initialize()` đã trả `null`). |
  | `Paid(…)` | | Sự kiện doanh thu — song song với `paidEventListener`, không thay thế. |

- **Consent event (mới)**: `AdEvent.ConsentStateChanged(state)` fire cho **cả gms lẫn nongms** khi consent resolve
  xong (kèm
  outcome `GRANTED`/`ALLOWED_VIA_UMP`/`ALLOWED_VIA_NATIVE_CMP`/`DENIED`/`NOT_REQUIRED`/`ERROR`/`UNRESOLVED`)
  — `ALLOWED_VIA_NATIVE_CMP` chỉ xảy ra trên nongms khi `applovinUseNativeCmp=true` (AppLovin tự show form,
  gate mở nhưng không đọc lại được lựa chọn P/NPA thật) — dùng cho analytics tỉ lệ đồng ý / cập nhật UI Cài
  đặt, thay vì phải poll `AdManager.consentState`.

## Khi báo lỗi cho chúng tôi, gửi kèm:
1. Output `AdManager.getDiagnostics()`. 2. Flavor (`gms`/`nongms`) + `IS_ENABLE_ADMOB`. 3. Có gọi `requestConsentInfoUpdate()` / set `applovinHasUserConsent` chưa. 4. Logcat tag `roy93~` (bật `isDebug=true` khi dev). 5. Đoạn code init (`MyApplication` + `SplashActivity`).

⚠️ **Đọc tay tới đây — ĐỪNG DỪNG LẠI.** Phần dưới gồm 2 mảng khác mục đích: **Section 0 + Step 1–7**
(ngay sau đây) là template điền placeholder cho AI — nếu bạn đã làm Bước 1–5 ở Phần A xong, có thể lướt
qua đoạn này. Nhưng **Step 8 (consent/provider-console — release gate), Step 9–12, và mục "⚠️ Security
Checklist cho Integrator" là nội dung bắt buộc cho CHÍNH BẠN**, không chỉ cho AI — đọc kỹ từ Step 8 trở đi.

---

# PHẦN B — CHECKLIST RELEASE (bắt buộc cho người) + PROMPT TEMPLATE CHO AI (Section 0–Step 7)

> Template dùng chung cho mọi project Android native. Trước khi đưa cho AI, **fill hết placeholder `<...>`** ở Section 0. VIP feature là **mandatory**, mọi project đều phải apply (không có cơ chế skip).

---

## 0. Bối cảnh — fill trước khi chạy

| Field                                      | Giá trị                                                                                                                                                                             |
|--------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Project hiện tại (cwd, absolute path)      | `<PROJECT_PATH>`                                                                                                                                                                    |
| Lib reference                              | `com.github.royt93:AdmobApplovinWrapper:<LATEST_TAG>` (JitPack), hoặc `<PATH_REPO_CLONE>` nếu bạn có source *(xem chú thích 1)*                                                     |
| API reference (tự chứa, offline)           | [Phụ lục A](#phụ-lục-a--api-surface-cốt-lõi-trích-docadmd) trong chính file này — đủ để migrate, KHÔNG cần mở github                                                                |
| File spec gốc của lib (TÙY CHỌN)           | https://github.com/royt93/AdmobApplovinWrapper/blob/main/doc/AD.MD *(chỉ khi cần chi tiết sâu hơn Phụ lục A — xem chú thích 1)*                                                     |
| Lib coordinate + version                   | `com.github.royt93:AdmobApplovinWrapper:<LATEST_TAG>` — kiểm tra tag mới nhất tại https://jitpack.io/#royt93/AdmobApplovinWrapper                                                   |
| Build variant phải xanh (debug)            | `<assemble{Flavor}Debug>`                                                                                                                                                           |
| Build variant phải xanh (release)          | `<assemble{Flavor}Release>`                                                                                                                                                         |
| Provider selector (chọn 1, compile-time)   | `AppLovin MAX` mặc định; chuyển sang `AdMob` khi `BuildConfig.IS_ENABLE_ADMOB=true`. **KHÔNG có waterfall/tự chuyển provider khi no-fill** — chỉ chọn đúng 1 provider qua cờ build. |
| AdMob App ID (`~`, không phải ad unit `/`) | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AppLovin SDK key                           | `<YOUR_APPLOVIN_SDK_KEY>` — lấy từ AppLovin dashboard *(xem chú thích 2)*                                                                                                           |
| AppLovin banner ID                         | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AppLovin interstitial ID                   | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AppLovin app open ID                       | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AppLovin rewarded ID                       | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AdMob banner ID                            | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AdMob interstitial ID                      | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AdMob app open ID                          | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| AdMob rewarded ID                          | `<hỏi user — dừng nếu thiếu>`                                                                                                                                                       |
| Thị trường phát hành                       | `<VN only                                                                                                                                                                           | worldwide | có EEA/UK/CH | có US states>` |
| CMP cho flavor nongms                      | `<tên CMP riêng trả về true/false — dừng nếu target EEA mà thiếu>`                                                                                                                  |
| AppLovin test-device ID                    | `<GAID/device identifier do MAX Mediation Debugger hiển thị>`                                                                                                                       |
| VIP flow                                   | **Ưu tiên: token ECDSA (chữ ký bất đối xứng)** — xem chú thích 3 bên dưới                                                                                                           |
| Privacy Policy URL                         | `<hỏi user — dừng nếu thiếu>` (sẽ nhúng vào VIP screen footer + consent dialog)                                                                                                     |

> **Chú thích 1 — Lib reference**: Mặc định dùng coordinate JitPack ở trên (không cần source). Nếu bạn có clone repo, thay `<PATH_REPO_CLONE>` bằng đường dẫn local của bạn. Tham chiếu khi chỉ dùng AAR:
> - Lib reference: `com.github.royt93:AdmobApplovinWrapper:<LATEST_TAG>` — lấy tag mới nhất tại https://jitpack.io/#royt93/AdmobApplovinWrapper
> - API reference: **[Phụ lục A](#phụ-lục-a--api-surface-cốt-lõi-trích-docadmd)** (đã inline trong file này — đủ để
    migrate). File spec đầy đủ (TÙY CHỌN): `https://github.com/royt93/AdmobApplovinWrapper/blob/main/doc/AD.MD`
    > Khi đó bỏ qua mọi instruction yêu cầu `ls`/`Read` file local — Phụ lục A đã đủ; chỉ mở GitHub raw khi cần chi tiết
    sâu hơn.

> **Chú thích 2 — AppLovin SDK key**: Đặt SDK key của **chính bạn** (lấy từ AppLovin dashboard) vào `BuildConfig.APPLOVIN_SDK_KEY` — KHÔNG hardcode trong source `.kt`, KHÔNG dùng key của bên khác (key không có quyền fill ad trên app của bạn).

> **Chú thích 3 — VIP flow** (chọn 1 trong 2):
>
> **[Ưu tiên] Token ECDSA P-256 (1.2.0+, chữ ký BẤT ĐỐI XỨNG)**: dev sinh cặp khoá 1 lần bằng
> `AdManager.generateVipKeyPair()` → giữ **private key** ở tool/CI (KHÔNG đóng gói vào app), đặt
> **public key** vào `AdSdkConfig.vipTokenPublicKey`. Đúc token bằng
> `AdManager.generateVipToken(privateKeyBase64, days)` (debug-gated) → user/QC dán vào app →
> `AdManager.activateVipByToken(context, token)`.
>
> ⚠️ **"Debug-gated" nghĩa là gì khi mint token thật cho khách trả tiền:** `generateVipToken()` chỉ
> chạy khi `AdSdkConfig.isDebug=true`, VÀ nó cần 1 `Context` Android đã `initialize()` xong (đọc
> package name + device ID nội bộ) — **không phải hàm JVM thuần chạy được từ script CLI hay backend/
> Cloud Function**. Engine ký thật (`TokenSigner`) nằm `internal` trong SDK, JitPack **không export ra
> ngoài** — nên `AdManager.generateVipToken()` gọi từ 1 app Android build debug là con đường DUY NHẤT
> khả dụng cho bạn để đúc token production. Cách làm thực tế: dựng 1 app Android nội bộ nhỏ riêng
> (build variant debug, giữ `privateKeyBase64` chỉ ở đó, không đưa vào app chính) — khi có khách trả
> tiền, bạn (hoặc người vận hành) mở app nội bộ này, nhập số ngày, gọi `generateVipToken(...)`, copy
> token gửi cho khách dán vào app chính. **An toàn hơn hẳn** plain key/thẻ cào: token tự hết
> hạn + (tuỳ chọn) bind thiết bị + bind applicationId, user không tự chế được. **Điểm mấu chốt:** app
> **chỉ giữ PUBLIC key** — trích ngược APK cũng KHÔNG đúc được token giả (cần private key, chỉ dev giữ).
> Nên token leak/reverse-engineer **không** cho phép giả mạo chữ ký.
> `vipKeySecret` giờ chỉ dùng chống-tamper SharedPreferences (đối xứng, cùng máy), **không còn** verify
> token. Cần thu hồi/đếm lượt tuyệt đối (server-side entitlement): vẫn phải verify qua **server** (VIP
> offline là best-effort marketing — user root/hook `isVIPMember()` vẫn bỏ qua được, accepted risk).
>
> **‹Legacy — chỉ nếu kẹt ở phiên bản < 1.2.0›**: đường plain-key/HMAC cũ (`activateVipByKey(ctx, key, days)`
> với key Base64-encode trong `BuildConfig`, opt-in qua `allowLegacyPlaintextVipKey = true`) đã **superseded** bởi
> token ECDSA. Chỉ dùng nếu bắt buộc chạy consumer < 1.2.0. Nhược điểm: key đối xứng, ai có key unlock được mọi app
> cùng `vipKeySecret` → phải share qua kênh riêng tư, KHÔNG commit public. **Với 1.2.0+ dùng token ECDSA ở trên,
> KHÔNG dùng nhánh này.**

**Rule cứng cho Section 0**:

1. **Cả 2 bộ ID (AppLovin + AdMob) đều phải đầy đủ** trước khi setup `AdSdkConfig`, dù chỉ AppLovin active runtime — vì khi flip `IS_ENABLE_ADMOB=true` không phải re-build template.
2. Bất kỳ field nào còn ghi `<hỏi user — dừng nếu thiếu>`: AI **phải hỏi user trong chat** rồi
   **dừng chờ trả lời**, KHÔNG tự fill placeholder. Chỉ dùng test ID khi user xác nhận đây là
   debug/QA; không đưa test ID vào production.
3. AppLovin SDK key trong template này là **placeholder `<YOUR_APPLOVIN_SDK_KEY>`** — partner **phải thay bằng SDK key của chính họ** (lấy từ AppLovin dashboard). Key phải đi qua `BuildConfig`, KHÔNG hardcode trong source `.kt` (xem chú thích 2 trong Section 0).
4. **`applovinRewardedId` KHÔNG ĐƯỢC để rỗng** nếu app có rewarded flow — SDK v1.1.5+ log warning ngay tại `setConfig()` khi ID rỗng, nhưng không throw; rewarded ad sẽ luôn `onDone(false)` không có lỗi rõ ràng, mất toàn bộ doanh thu rewarded. Đã xác nhận qua device test thực tế. **`showRewardedAsync()` default timeout = 120s (v1.2.2+, tăng từ 90s)** — đây là timeout cho TOÀN BỘ vòng
đời show+xem+reward (không phải riêng thời gian load): ~30s video + buffer 90s dự phòng user pause (bấm
Home rồi quay lại). Đừng hiểu nhầm 120s là "thời gian load bình thường"; nếu override `timeoutMs`, chỉ
giảm sau khi tự đo trên thiết bị/mạng mục tiêu của bạn, đừng đặt thấp hơn 120_000L nếu chưa đo.
5. **VIP activation flow**: Ưu tiên **token ECDSA** (`activateVipByToken` / `generateVipToken`, 1.2.0+) — chữ ký **bất
   đối xứng**, app chỉ giữ public key nên trích ngược APK KHÔNG đúc được token giả (xem **chú thích 3** Section 0). ⚠️ *
   *Release gate:** phải đổi CẢ `vipKeySecret` (chống-tamper prefs) **VÀ** `vipTokenPublicKey` (verify token) sang giá
   trị riêng của bạn — nếu release còn giữ public key MẪU, SDK **từ chối im lặng** mọi token (activate trả `false` dù
   chữ ký hợp lệ). "Xem quảng cáo → VIP" dùng **`grantVipDays(ctx, days)`** (không qua verify key). Fallback plain key
   chỉ dùng nội bộ khi consumer còn pin ở 1.1.5.
6. **Consent là release gate, không phải optional**:
   - `gms`: partner phải tạo + **Publish** European regulations message cho đúng AdMob App ID.
   - `nongms`: không có Google UMP UI; partner phải lấy quyết định từ CMP riêng và truyền
     `applovinHasUserConsent=true/false` trước `AdManager.initialize()`.
   - Không được set cứng `true` trong production để né consent UI.
7. **Test ads**: debug/QA được dùng Google demo ad units. Production phải dùng ad unit của partner.
   AppLovin không có universal test ad-unit IDs như Google; phải bật MAX Test Mode cho GAID QA.

---

## Cấm (áp dụng mọi project)

- Tự ý thêm ad slot mới ngoài các slot đã tồn tại trong code hiện tại.
- Tự ý refactor code không liên quan đến migrate ad.
- Tự ý nâng version lib hoặc đổi flag `IS_ENABLE_ADMOB`.
- Lặp lại đọc cùng 1 file > 1 lần với mục đích "double check" — review trên kết quả đã có, không re-read.
- Tự thêm trigger interstitial mới (vd "show sau mỗi action X") nếu code cũ chưa có.
- Tự fill ad ID bằng test ID hoặc placeholder — phải hỏi user và ghi rõ debug hay production.

---

## Step 1 — Đọc lib reference

> ⚠️ **Gradle prerequisite (CHỈ khi dùng SOURCE module `project(":admob-wrapper-sdk")`)**: lúc đó lib lộ `flavorDimensions("ads")` + flavors `gms`/`nongms`, Gradle **bắt buộc** app consumer khai báo cùng dimension, nếu không build fail với `"All flavors must now belong to a named flavor dimension"`. Thêm vào `app/build.gradle.kts`. **Lưu ý: dùng AAR JitPack thì KHÔNG bị ràng buộc này** — Gradle chọn được variant `gms` đã publish khi consumer không khai dimension `ads` (xem Bước 2):
> ```kotlin
> flavorDimensions += "ads"
> productFlavors {
>     create("gms") { dimension = "ads" }    // Google (AdMob + UMP + GAID)
>     create("nongms") { dimension = "ads" } // Non-Google (AppLovin only, Huawei)
> }
> ```
> Task Gradle sau đó là **flavor-qualified**: `assembleGmsDebug`, `assembleNongmsDebug`, `testGmsDebugUnitTest`.
>
> ⚠️ **CHỈ khi app của bạn tự khai `flavorDimensions` như trên.** Cách dùng mặc định (kéo AAR từ
> JitPack) thì app **không có flavor nào** — chạy `./gradlew assembleGmsDebug` sẽ báo
> `Task 'assembleGmsDebug' not found`. Lúc đó dùng `./gradlew assembleDebug` như bình thường.
> AAR trên JitPack luôn là bản `gms` (đã gồm AdMob + AppLovin + UMP); chỉ khi cần bản `nongms` cho
> Huawei/máy không có Google mới phải khai flavor và build từ source.

- `ls` đệ quy `<PATH_REPO_CLONE>` để nắm cấu trúc.
- **API reference đã inline sẵn ở [Phụ lục A](#phụ-lục-a--api-surface-cốt-lõi-trích-docadmd)** — đủ để migrate mà KHÔNG
  cần mở file nào khác. Đọc `<PATH_REPO_CLONE>/doc/AD.MD` (spec gốc đầy đủ) là **TÙY CHỌN**, chỉ khi cần chi tiết sâu
  hơn Phụ
  lục A.
- Đọc sample app trong lib (`app/` hoặc `sample/`), đặc biệt các call site: `AdManager.setConfig`, `initialize`, `requestConsentInfoUpdate`, `awaitSplashComplete`, `loadBanner`, `showInterstitial`, `loadRewarded`, `showRewarded`, `activateVipByKey`, `AdSafetyLimits`.
- **`AdSafetyLimits` debug-friendly**: ở `BuildConfig.DEBUG=true`, dùng giá trị dễ dãi hơn để QC test nhanh. Giá trị đề xuất (verify field name chính xác từ lib ở step này, đổi tên field nếu lib dùng tên khác):

  **Tên field thật của `AdSafetyLimits` (data class, đơn vị mili-giây cho `*Time*`)** — KHÔNG có field tách riêng interstitial vs appOpen interval, chỉ có 1 `minTimeBetweenFullscreenAds` chung:

  | Field | Default (release) | Debug đề xuất *(= giá trị thật trong `AdSafetyLimits.TEST`)* |
  |---|---|---|
  | `minTimeBetweenFullscreenAds` | `60_000L` (60s) | `5_000L` |
  | `maxFullscreenAdsPerSession` | `6` | `999` |
  | `maxFullscreenAdsPerHour` | `3` | `999` |
  | `maxFullscreenAdsPerDay` | `10` | `9_999` |
  | `maxAppOpenAdsPerSession` *(FEAT-01, riêng App Open — khác `maxFullscreenAdsPerSession`)* | `2` | `999` |
  | `maxAppOpenAdsPerHour` *(FEAT-01, riêng App Open)* | `1` | `999` |
  | `maxAppOpenAdsPerDay` *(FEAT-01, riêng App Open)* | `3` | `9_999` |
  | `minTimeAppOpenResume` | `5_000L` (5s) | `0L` |
  | `minSessionDurationBeforeAd` | `10_000L` (10s) | `0L` |
  | `maxClicksPerMinute` | `3` | `999` |
  | `suspiciousCtrThreshold` | `0.05f` (5%) | `1.0f` (tắt hẳn CTR check) |
  | `minImpressionsForCtrCheck` | `50` | `9_999` |
  | `ctrWindowMs` *(1.6.1)* | `14 ngày` | giữ default (TEST không override field này) |
  | `newSessionAfterBackgroundMs` *(1.6.1)* | `30 phút` | `0L` |
  | `maxRapidResumesPerMinute` | `3` | `999` |

  ⚠️ **Hai field CTR đi liền nhau.** Nếu tự chỉnh, giữ bất biến
  `maxFullscreenAdsPerDay × số-ngày-cửa-sổ ≥ minImpressionsForCtrCheck`, nếu không lớp CTR không bao
  giờ đủ mẫu để chạy (SDK log cảnh báo lúc `init`). Và **đừng hạ `minImpressionsForCtrCheck` xuống
  dưới ~30**: ở n = 10, đúng một cú click nhầm = 10% > ngưỡng 5% ⇒ khoá quảng cáo của user thật 30 phút.

  Có sẵn preset: `AdSafetyLimits.GAME` / `.UTILITY` / `.CONTENT` / `.TEST`. Wire bằng `safety = if (BuildConfig.DEBUG) AdSafetyLimits.TEST else AdSafetyLimits()` trong `AdSdkConfig` ở `Application.onCreate` (hoặc copy preset rồi override field cần đổi).

**Output bắt buộc**: 1 đoạn ≤ 10 dòng tóm tắt API surface của lib và lifecycle nó tự quản (App Open auto qua `ProcessLifecycleOwner`, banner auto pause/resume/destroy, frequency cap…).

## Step 1.5 — Tạo file credentials `ads.properties` (BẮT BUỘC trước khi build lần đầu, làm 1 lần)

Viết cho người **chưa từng làm việc này** — làm đúng thứ tự, đừng bỏ bước nào.

**Vì sao phải làm:** ad-unit ID/SDK key là bí mật của riêng app bạn, **không bao giờ hard-code trong
source hoặc commit vào git**. Cách chuẩn: 1 file `ads.properties` local (gitignored) + đọc vào
`build.gradle.kts` lúc build, set thành `BuildConfig` field.

**1. Tạo file** `<PROJECT_PATH>/app/ads.properties` (đổi `app` thành đúng tên module chứa
`build.gradle.kts` của bạn nếu khác) — file text thường, mỗi dòng `key=value`, không dấu ngoặc kép.

**2. Thêm ngay vào `.gitignore` của project** (nếu chưa có dòng này):

```
app/ads.properties
```

**3. Điền nội dung** — copy khung dưới, điền giá trị THẬT vào chỗ trống bên phải dấu `=`:

```properties
# ===== AppLovin — lấy từ AppLovin Dashboard (https://dash.applovin.com) =====
# Menu "Account" → "Keys" → copy "SDK Key"
APPLOVIN_SDK_KEY=
# Menu "MAX" → "Manage Ad Units" → tạo/chọn ad unit đúng loại → copy "Ad Unit ID"
APPLOVIN_BANNER_ID=
APPLOVIN_INTERSTITIAL_ID=
APPLOVIN_APP_OPEN_ID=
APPLOVIN_REWARDED_ID=
# Rewarded Interstitial: AppLovin AAR KHÔNG có format này — để trống, dùng AdMob nếu cần loại ad này

# ===== AdMob — lấy từ AdMob Console (https://apps.admob.com) =====
# "Apps" → chọn app → "App settings" → copy App ID (dạng ca-app-pub-xxxx~yyyy)
DEBUG_ADMOB_APP_ID=
RELEASE_ADMOB_APP_ID=
# "Ad units" → tạo/chọn đúng loại → copy Ad unit ID (dạng ca-app-pub-xxxx/yyyy)
# DEBUG_* để trống thì SDK tự dùng ID test công khai của Google (an toàn cho debug)
DEBUG_ADMOB_BANNER_ID=
DEBUG_ADMOB_INTERSTITIAL_ID=
DEBUG_ADMOB_APP_OPEN_ID=
DEBUG_ADMOB_REWARDED_ID=
# RELEASE_* KHÔNG có fallback — build release sẽ FAIL nếu để trống mà release dùng AdMob
RELEASE_ADMOB_BANNER_ID=
RELEASE_ADMOB_INTERSTITIAL_ID=
RELEASE_ADMOB_APP_OPEN_ID=
RELEASE_ADMOB_REWARDED_ID=

# ===== VIP =====
# Chuỗi bí mật tự đặt, dùng openssl rand -base64 24 (hoặc bất kỳ chuỗi ngẫu nhiên ≥16 ký tự)
DEBUG_VIP_KEY_SECRET=
RELEASE_VIP_KEY_SECRET=
# Public key ECDSA — xem hướng dẫn generateVipKeyPair() bên dưới, KHÔNG tự bịa chuỗi
DEBUG_VIP_TOKEN_PUBLIC_KEY=
RELEASE_VIP_TOKEN_PUBLIC_KEY=
```

**4. Đọc file này trong `build.gradle.kts`** của module app — thêm đoạn sau vào đầu file (trước
`android { ... }`), rồi dùng hàm `adsProp(...)` bên trong `defaultConfig`/`buildTypes`:

```kotlin
import java.util.Properties
import java.io.FileInputStream

val adsProperties = Properties().apply {
    val f = rootProject.file("app/ads.properties") // đổi path nếu module khác tên "app"
    if (f.exists()) load(FileInputStream(f))
}
fun adsProp(key: String, fallback: String = ""): String =
    adsProperties.getProperty(key) ?: fallback.also {
        logger.warn("⚠️ ads.properties thiếu key '$key', dùng fallback (chỉ chấp nhận ở debug)")
    }

android {
    defaultConfig {
        buildConfigField("String", "APPLOVIN_SDK_KEY", "\"${adsProp("APPLOVIN_SDK_KEY")}\"")
        buildConfigField("String", "APPLOVIN_BANNER_ID", "\"${adsProp("APPLOVIN_BANNER_ID")}\"")
        // ...lặp lại cho từng key ở trên
    }
    buildTypes {
        debug {
            val appId = adsProp("DEBUG_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
            buildConfigField("String", "ADMOB_APP_ID", "\"$appId\"")
            manifestPlaceholders["admobAppId"] = appId // ⚠️ BẮT BUỘC — xem chú thích dưới
            // debug: adsProp(key, fallback_test_id) — an toàn nếu quên điền
        }
        release {
            val realAppId = adsProp("RELEASE_ADMOB_APP_ID")
            require(realAppId.isNotBlank()) { "RELEASE_ADMOB_APP_ID trống — điền app/ads.properties trước khi build release" }
            buildConfigField("String", "ADMOB_APP_ID", "\"$realAppId\"")
            manifestPlaceholders["admobAppId"] = realAppId // ⚠️ BẮT BUỘC — xem chú thích dưới
            // release: fail-fast nếu thiếu — KHÔNG fallback về test ID (tránh ship demo ads)
        }
    }
}
```

⚠️ **`buildConfigField("ADMOB_APP_ID", ...)` ở trên KHÔNG đủ — app sẽ crash ngay khi mở nếu dừng ở đây.**
`BuildConfig.ADMOB_APP_ID` không tự động chạy tới đâu cả; Google Mobile Ads SDK đọc App ID từ
**Manifest**, không phải từ `BuildConfig`. Bắt buộc thêm dòng `manifestPlaceholders["admobAppId"] = ...`
ở trên (đã có trong snippet) **VÀ** khai đúng `<meta-data>` tương ứng trong
`AndroidManifest.xml` (flavor `gms`) — xem code mẫu đầy đủ ở **Bước 3a** (Phần A, bên dưới) hoặc **B.3**
(Phụ lục B). Thiếu dòng `<meta-data>` trong Manifest = app crash ngay lúc mở, dù `BuildConfig` đã đúng.

**5. Sinh VIP keypair (làm 1 lần, offline, giữ private key ngoài git):**

```kotlin
// Viết tạm 1 unit test hoặc gọi 1 lần trong debug menu, KHÔNG commit code này:
val pair = AdManager.generateVipKeyPair()
println("PUBLIC (dán vào ads.properties, key *_VIP_TOKEN_PUBLIC_KEY): ${pair.publicKeyBase64}")
println("PRIVATE (lưu vào password manager/secret vault, KHÔNG commit, KHÔNG đưa vào app): ${pair.privateKeyBase64}")
```

Chạy xong, xoá đoạn code test này. Dán `publicKeyBase64` vào `ads.properties`
(`DEBUG_VIP_TOKEN_PUBLIC_KEY`/`RELEASE_VIP_TOKEN_PUBLIC_KEY`) — chỉ public key nằm trong app.
`privateKeyBase64` dùng riêng ở tool nội bộ để đúc token phát cho khách
(`AdManager.generateVipToken(privateKeyBase64, days, ...)`), không bao giờ đưa vào app.

**6. Verify:** build thử (`./gradlew assembleDebug`) — nếu Gradle không báo lỗi thiếu key và
`BuildConfig.APPLOVIN_SDK_KEY`/`BuildConfig.ADMOB_APP_ID` không rỗng khi log ra, bước này đã xong.

**Cấu hình dashboard AdMob/AppLovin (CMP, GDPR/US-states message) là bước KHÁC, làm ở Step 8 bên dưới
— không bỏ qua, đây chỉ là bước lấy credentials để build được, chưa phải bước consent.**

## Step 2 — Audit hiện trạng project

Trả lời 5 câu (yes/no + dẫn chứng `file:line`):

1. Project đã apply `AdmobApplovinWrapper` chưa? Hay vẫn còn SDK cũ (vd `AdMobManager`, `AdsHelper`, `MaxManager`…)?
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

- **App Open ở Splash**: gọi `initialize()` trong Application để wrapper prepare recovery; hàm này
  defer provider init. Splash gọi `requestConsentInfoUpdate()` rồi `awaitSplashComplete` và
  navigate main. **KHÔNG** tự tạo thêm App Open/interstitial phủ lên flow của wrapper.
- **App Open khi quay lại foreground**: dùng cơ chế của lib (đã hook `ProcessLifecycleOwner`). KHÔNG tự subscribe `ON_START` lần thứ 2 trong project — tránh double-show.
- **Offline — interstitial/rewarded**: `loadInterstitial`/`loadRewarded` **fire**
  `interstitialListener?.onAdFailedToLoad(AdError.NoNetwork)` / `rewardedListener?.onAdFailedToLoad(...)`
  khi offline — `is AdError.NoNetwork` **KHÔNG phải dead code**, bắt buộc xử lý, đừng bỏ qua. Riêng
  **`loadBanner` im lặng**: trả `null` trực tiếp khi offline, không có event `AdError.NoNetwork` nào để
  lắng nghe (banner không có callback load bất đồng bộ dạng listener) — check `ConnectivityManager` độc
  lập hoặc xử lý khi `loadBanner` trả `null`.
- **Banner**: gắn vào layout container có sẵn. `loadBanner(..., autoManageLifecycle = true)` **(default từ v1.1.3)**
  > `autoManageLifecycle=true` — SDK tự hook lifecycle qua `ActivityLifecycleCallbacks`, **KHÔNG cần**
  > 3 override dưới. Chỉ dùng `autoManageLifecycle = false` (backward-compat ≤ v1.1.2) nếu bạn tự quản lý:
  ```kotlin
  override fun onResume()  { super.onResume();  AdManager.bannerResume(adView) }
  override fun onPause()   { super.onPause();   AdManager.bannerPause(adView) }
  override fun onDestroy() { super.onDestroy(); AdManager.bannerDestroy(adView); adView = null }
  ```
  ⚠️ KHÔNG vừa để `autoManageLifecycle=true` vừa gọi 3 override trên — double-resume/pause.
  Trong `onResume`, guard trước khi tạo mới: `if (adView == null || adView?.parent == null) { adView = AdManager.loadBanner(...) }` — tránh leak khi back-stack resume.
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
- **Android 12+ (targetSdk ≥ 31)**: Kiểm tra `AndroidManifest.xml` có khai báo `<attribution>` tag chưa — nếu thiếu gây warning `attributionTag not declared` liên tục trong logcat:
  ```xml
  <!-- Thêm vào <manifest> trước <application> -->
  <attribution android:tag="ads" android:label="Ad Networks (AdMob / AppLovin MAX)" />
  ```
- Output: bảng "điểm review / kết luận / `file:line` dẫn chứng".

## Step 7 — Policy check (6 rule cụ thể, không hơn không kém)

| # | Rule                                                                                                                                                                                                       | Cách verify                                                                                                                           |
|---|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| 1 | Banner không che nội dung tương tác                                                                                                                                                                        | Đọc layout host banner, xác nhận banner ở dock bottom/top, không overlay nút bấm                                                      |
| 2 | App Open không show trên splash thuần — chỉ show khi warmup xong trước khi vào main, hoặc khi resume từ background. Khai splash trong `appOpenExcludedActivities`                                          | Đọc Splash activity + lifecycle hook + `AdSdkConfig.appOpenExcludedActivities`                                                        |
| 3 | Interstitial không show liên tiếp dưới ngưỡng frequency cap                                                                                                                                                | Tin `AdSafetyLimits` mặc định, ghi rõ giá trị dùng (debug vs release)                                                                 |
| 4 | Rewarded chỉ trigger khi user bấm nút                                                                                                                                                                      | Đọc VIP screen                                                                                                                        |
| 5 | Không tự click ad / không có UI giả mạo close button                                                                                                                                                       | Đọc layout banner                                                                                                                     |
| 6 | **Reward CHỈ cấp khi Rewarded ad `earned==true`. Fallback interstitial (hoặc bất kỳ ad non-rewarded) TUYỆT ĐỐI KHÔNG cấp reward/VIP** — vi phạm Google/AppLovin Rewarded policy, rủi ro ban account (A-15) | Đọc nhánh `else` của `showRewarded`: interstitial fallback chỉ được `showNoRewardDialog()`, KHÔNG gọi `grantVipDays`/`grantVipFromAd` |

Báo cáo: PASS/FAIL từng rule + `file:line` dẫn chứng.

## Step 8 — Consent & provider-console configuration (release gate)

### 8.1 — Ma trận bắt buộc

| Flavor/runtime | Nguồn quyết định | Khi chưa config/quyết định | Partner phải làm |
|---|---|---|---|
| `gms` + AdMob | Google UMP | Provider init deferred; fresh offline/misconfig navigate không ad | Publish UMP; chạy device release gate |
| `gms` + AppLovin | Google UMP + IAB/AC strings | Provider init deferred; fresh offline/misconfig navigate không ad | Publish UMP; chạy device release gate |
| `nongms` + AppLovin | CMP riêng trả Boolean | `applovinHasUserConsent=null` → chặn toàn bộ ad | Thu quyết định trước init, truyền `true` hoặc `false` |

`false` là một quyết định hợp lệ: AppLovin nhận non-consent flag và có thể phục vụ contextual/
non-personalized inventory theo cấu hình account. Chỉ `null` có nghĩa “chưa có quyết định”.

**Nếu partner không config:**

| Thiếu/sai | Hành vi quan sát được | Rủi ro |
|---|---|---|
| AdMob App ID/UMP message | Init error hoặc UMP publisher misconfiguration; fresh install fail-closed | Không fill/zero revenue; bypass gate tạo compliance risk |
| AdMob production ad units | Google demo ads vẫn có thể chạy trong debug | Không có revenue/report production; không được ship demo IDs |
| AppLovin SDK key/ad units | Init/load fail hoặc no-fill `204` | Zero revenue, format tương ứng luôn unavailable |
| MAX mediation setup | Adapter/network incomplete | Fill/eCPM thấp, nhiều no-fill |
| nongms CMP decision | `applovinHasUserConsent=null` | Wrapper chủ động block mọi ad; an toàn hơn nhưng zero revenue |
| MAX Test Mode cho QA | Máy QA có thể nhận live inventory | Không được auto-click; invalid-traffic/account risk |

### 8.2 — Consent-first orchestration của wrapper

`AdManager.initialize()` hiện làm hai pha nhưng giữ API cũ:

1. Prepare `earlyInit`, activity lifecycle và validated-network callback.
2. Nếu consent chưa đủ, state là `WAITING_FOR_CONSENT`; **không gọi**
   `MobileAds.initialize()`/`AppLovinSdk.initialize()` và mọi API ad vẫn bị chặn.
3. Splash gọi `requestConsentInfoUpdate()`. Chỉ khi `canRequestAds=true`, wrapper apply privacy
   flags rồi initialize provider đúng một lần.
4. Fresh install offline/misconfigured vẫn navigate ngay không ad. Khi validated network trở lại,
   wrapper retry UMP; nếu thành công mới initialize provider và preload.
5. nongms chỉ initialize MAX khi `applovinHasUserConsent` khác `null`; `false` là quyết định hợp lệ.

Automated tests và Pixel 7 Pro smoke đã chứng minh `consent result → provider initialize`,
deferred offline, reconnect và single-init cho GMS, cùng explicit CMP flag trước MAX init cho
nongms. Partner vẫn phải chạy lại gate này với App ID/CMP production của họ.

⚠️ Có **hai** chế độ consent cho AppLovin, **không trộn lẫn**: (a) mặc định `applovinUseNativeCmp=false`
→ luồng custom của wrapper (geo + dialog tự làm) qua `AdManager.requestConsentInfoUpdate()`; (b)
`applovinUseNativeCmp=true` → **MAX Terms/Privacy flow chính thức, CHỈ HIỆU LỰC TRÊN NONGMS** (xem
8.4b — bật trên gms không có tác dụng gì, im lặng) — wrapper tự suppress dialog tự làm + mở gate qua
`applovinNativeConsentGathered`, KHÔNG còn double-UI. Đừng tự gọi UMP automation song song khi đang ở
chế độ (a).

### 8.3 — Partner cấu hình AdMob Console

**Nếu bỏ qua bước này:** UMP có thể trả `Publisher misconfiguration`; eligible users không có
form để hoàn tất lựa chọn, fresh install bị fail-closed hoặc app bị compliance risk nếu bypass.

**Các bước trong AdMob Console:**

1. AdMob → **Apps** → chọn app → **App settings**; copy App ID dạng
   `ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy` vào manifest. Không nhầm với ad unit ID dạng `/`.
2. **Privacy & messaging** → **European regulations** → **Create** (hoặc Manage → Create message).
3. **Select apps** → chọn đúng app/App ID → Confirm.
4. Cấu hình message:
   - *Message name*: tên tùy ý, vd `"GDPR Consent v1"`.
   - Chọn languages và các lựa chọn consent/privacy options phù hợp.
   - Review danh sách ad partners/vendors cho Google, AppLovin và mediation networks đang dùng.
   - *Privacy Policy URL*: URL chính sách của app.
5. Nhấn **Publish**. Draft không có hiệu lực; chờ propagation rồi cold-start lại app.
6. Xác nhận log không còn `Publisher misconfiguration: no form(s) configured`.

**7. 🇺🇸 Nếu app có traffic Mỹ — BẮT BUỘC bật thêm "US states" message.**
AdMob Console → **Privacy messaging** → **US states** → Create → Select apps → **Publish** (quy trình
giống European regulations ở trên).

Vì sao phải làm ở dashboard chứ không phải trong code: các bang Mỹ mới (CPRA/California,
Colorado, Virginia, Connecticut…) dùng chuẩn tín hiệu **IAB GPP** (`IABGPP_HDR_GppString`), khác chuẩn
US-Privacy cũ (`IABUSPrivacy_String`). Khi bạn publish "US states" message, **UMP tự ghi chuỗi GPP** và
**AdMob + các mediation adapter tự đọc** — SDK wrapper không cần và không nên làm trung gian.

> ⚠️ **Giới hạn đã biết của wrapper (G-02, thu hẹp ở 1.6.1):** phần SDK **tự suy** cờ `doNotSell` cho
> AppLovin nay đọc GPP `IABGPP_7_String` (usnat) + `IABGPP_8_String` (usca), ưu tiên hơn
> `IABUSPrivacy_String` legacy. **Section riêng từng bang (SID 9+) vẫn KHÔNG đọc.**
>
> ⚠️ **Hệ quả doanh thu nếu bạn có user Mỹ, khi usnat/usca CŨNG mù hoàn toàn (R2-04, 1.7.2+):** khi
> chuỗi GPP khai một section SID ≥ 9 mà usnat (7)/usca (8) không đọc được gì, SDK áp **fail-safe
> `doNotSell = true` cho TOÀN BỘ user Mỹ** cho tới khi CMP đổi sang phát usnat. **Nếu usnat/usca đọc
> được và ghi rõ user KHÔNG opt-out, SDK tôn trọng tín hiệu đó** (sửa ở 1.7.2 — trước đó ép `true` vô
> điều kiện kể cả khi đã đọc rõ, tự mâu thuẫn với chính nguyên tắc SDK áp dụng ở nhánh liền kề). Nghĩa
> là: chỉ khi CMP cấu hình state-by-state VÀ không phát usnat/usca thì traffic US mới thành
> **non-personalized**; không crash, không log nào ngoài một dòng `Log.e`.
>
> **Cách kiểm soát:** truyền tường minh `AdSdkConfig(applovinDoNotSell = false/true)` từ chính CMP của
> bạn — giá trị tường minh thắng mọi suy diễn, kể cả fail-safe này. Kiểm chứng bằng
> `AdManager.getPrivacyDiagnostics().appliedDoNotSell`.
>
> Nếu waterfall AppLovin của bạn phục vụ lượng
> đáng kể user Mỹ, đừng phụ thuộc suy diễn tự động — hãy lấy kết quả từ CMP của bạn rồi truyền tường
> minh: `AdSdkConfig(applovinDoNotSell = true/false)`. Giá trị tường minh này **luôn thắng** suy diễn
> tự động (kiểm chứng qua `AdManager.getPrivacyDiagnostics().appliedDoNotSell`).

Nếu bỏ qua: fresh install tại vùng cần consent có thể bị wrapper chặn ads → zero revenue. Nếu app
tắt `blockLoadUntilConsent` để bypass, partner tự nhận rủi ro Google EU User Consent Policy/GDPR.

**Test EEA:** wrapper expose sẵn field debug này trong `AdSdkConfig`, không cần tự dựng harness riêng:

```kotlin
AdSdkConfig(
    // ... các field khác
  umpDebugGeography = "EEA",                    // "EEA" / "NOT_EEA" / "US_STATE" / "DISABLED" / null
    umpTestDeviceHashedIds = listOf("<hash từ logcat UMP: 'Use ... addTestDeviceHashedId(...)'>"),
)
```

Cả hai field **chỉ có hiệu lực khi `isDebug = true`** (AdManager không forward khi release — không
cần tự nhớ xoá trước khi build production) và UMP chỉ áp geo giả cho đúng thiết bị đã đăng ký trong
`umpTestDeviceHashedIds`. Test AppLovin-side riêng (không qua UMP) dùng `applovinDebugGeo` ("GDPR"/
"OTHER", cũng debug-only). Không cần build/harness riêng, không cần gọi `requestConsentInfoUpdate()`
lần hai — set 2 field này trong `AdSdkConfig` trước `initialize()` là đủ.

Nếu vẫn muốn test bằng geo thật (không ép): dùng test account/device ở đúng vùng và xác nhận form
thật sau khi message đã Publish trên AdMob Console.

### 8.4 — Partner cấu hình AppLovin MAX Dashboard

1. **Account → General → Keys**: lấy SDK key của chính partner; inject qua BuildConfig/CI secret.
2. **MAX → Mediation → Manage → Ad Units**: tạo/kiểm tra đủ Banner, Interstitial, App Open,
   Rewarded; copy đúng ID vào build variant tương ứng.
3. Kiểm tra package/app mapping và mediation adapters. Ad unit sai hoặc network chưa eligible thường
   trả no-fill `204`, không crash nhưng zero revenue.
4. **MAX → Mediation → Manage → Test Mode → Add Test Device**: nhập GAID/device identifier do
   MAX Mediation Debugger hiển thị trên máy QA và chọn network.
   Test Mode có thể cần thời gian để activate. Không để test mode cho live traffic.
5. Mở Mediation Debugger/log `Privacy States`; xác nhận `Has User Consent` và `Do Not Sell` có
   giá trị đúng **ngay lúc MAX initialize**.

AppLovin không có universal demo ad-unit IDs giống AdMob. Không bật Test Mode thì QA có thể nhận
live inventory; tuyệt đối không tự động click.

### 8.4b — (Tuỳ chọn) Bật AppLovin native CMP cho mediation — `applovinUseNativeCmp`

> 🔴 **QUAN TRỌNG — mục này CHỈ CÓ HIỆU LỰC TRÊN NONGMS.** KDoc gốc
> (`AdManager.kt`, biến nội bộ `nativeCmpEffective`): *"Native CMP chỉ hiệu lực khi: (1) bật cờ, (2) có
> privacy URL, và (3) đang chạy trên nongms."* Điều kiện thật: `applovinUseNativeCmp && privacyUrl != null
> && !GmsBridge.isAdmobSupported` — trên **gms**, `!GmsBridge.isAdmobSupported` luôn `false` nên toàn bộ
> nhánh native CMP **tắt cứng**, dù bạn bật cờ. **Bật `applovinUseNativeCmp=true` trên build `gms` sẽ
> KHÔNG LÀM GÌ CẢ** — không hiện Terms/Privacy flow, không suppress dialog custom, không log lỗi, không
> cảnh báo nào cả — im lặng tuyệt đối, không có tín hiệu để bạn biết mình bật nhầm chỗ. Nếu app build
> `gms` và cần TCF cho waterfall đa mạng, **đây không phải cách** — dùng UMP + `applovinDoNotSell` set
> tường minh theo tín hiệu UMP thật (xem 8.1-8.3).

Mặc định wrapper dùng luồng consent **custom** (geo-based + dialog tự làm) → **KHÔNG sinh IAB TCF/AC
strings** cho mediated networks. Nếu waterfall AppLovin có network đòi TCF (Google demand…) **trên bản
nongms**, bật AppLovin **Terms & Privacy Policy Flow** (CMP "thật", chạy trên Google UMP bundle sẵn
trong chính AAR AppLovin, tự áp TCF/AC cho toàn waterfall).

**Bước 1 — Code** (`AdSdkConfig` ở `MyApplication.onCreate`):
```kotlin
AdSdkConfig(
    applovinUseNativeCmp = true,                            // bật MAX Terms/Privacy flow
    applovinPrivacyPolicyUrl = "<URL privacy policy thật>", // BẮT BUỘC, phải mở được
    // ...
)
```
Khi bật, wrapper tự: (a) set `termsAndPrivacyPolicyFlowSettings.isEnabled=true`; (b) **suppress dialog
tự làm** (`needsGdprConsentDialog()` trả `false`) → không còn double consent UI; (c) **không** ghi đè
consent bằng `setHasUserConsent` — để TCF (do AppLovin ghi) quyết định privacy flags ⇒ tôn trọng đúng
lựa chọn THẬT của user; (d) mở gate qua cờ nội bộ `applovinNativeConsentGathered`. Re-consent (đổi lựa
chọn): `AdManager.showConsentFormIfAvailable(activity)` → `AppLovinCmpService.showCmpForExistingUser`.

**Bước 2 — Dashboard** (BẮT BUỘC — SDK không kiểm chứng được). **Lưu ý quan trọng:** MAX Terms/Privacy
flow **chạy trên Google UMP**, nên consent **message được tạo trên Google AdMob Console
(apps.admob.com), KHÔNG phải trên dashboard AppLovin** — kể cả khi app chỉ monetize bằng AppLovin.
Cần một tài khoản AdMob. Các bước:
- Làm **đúng mục 8.3** (AdMob → Privacy & messaging → GDPR/European regulations → Create message →
  Select apps → User consent options chọn *Consent or Manage options* (KHÔNG chọn *Close/do not
  consent*) → Targeting *Countries subject to GDPR (EEA + UK)* → đặt tên → **Publish**).
- Mục **Review your ad partners**: chọn tất cả networks đang dùng → **Confirm** → **Save**.
- Trên **AppLovin dashboard** chỉ cần lấy SDK key (Account → General → Keys) — đã có ở mục 8.4.
- Verify bằng **MAX Mediation Debugger → Privacy → CMP**: kiểm tra IAB TCF params và cảnh báo
  **MISSING ATP NETWORKS / MISSING TCF VENDORS**; nếu thiếu thì bổ sung network ở AdMob rồi restart.

**Bước 3 — Test** (máy thật, vùng EEA hoặc debug geography): cold start → form consent của AppLovin phải
hiện → Accept/Decline; mở **MAX Mediation Debugger → Privacy States / Consent** xác nhận TCF string xuất
hiện đúng theo lựa chọn, và KHÔNG còn dialog tự làm hiện chồng.

**Đánh đổi:** init splash chậm hơn (form hiện trong lúc init); có thể có acknowledgment dialog của
AppLovin ở non-EEA. Nếu app **chỉ dùng AppLovin thuần** (không mediation cần TCF) → để
`applovinUseNativeCmp=false`, luồng mặc định đã đủ. Công tắc này mới chỉ "đúng về code" cho tới khi
bạn hoàn tất Bước 2 + Bước 3 trên máy thật.

### 8.5 — nongms: CMP riêng trước AppLovin initialize

```kotlin
// Pseudocode: CMP phải hoàn tất trước setConfig/initialize.
cmp.collectDecision { decision ->
    val config = AdSdkConfig(
        isEnableAdmob = false,
        // null = chưa có quyết định → wrapper chặn ad.
        // true/false = CMP đã quyết định → MAX nhận đúng privacy flag trước init.
        applovinHasUserConsent = decision.hasUserConsent,
        applovinDoNotSell = decision.doNotSell,
        // ... SDK key + 4 ad unit IDs
    )
    AdManager.setConfig(config)
    AdManager.initialize(application)
}
```

Không set cứng `true` cho production. CMP phải lưu/khôi phục quyết định, cho phép user đổi lựa chọn,
và cập nhật config/privacy flags khi consent thay đổi. Nếu app phục vụ US states, `doNotSell` phải
phản ánh opt-out thật; không mặc định `false` nếu chưa đánh giá jurisdiction.

> **Cứu doanh thu first-run nongms ngoài EEA — `AdSdkConfig.allowAdsWhenConsentUnknownNongms`**
> (mặc định `false` = fail-closed an toàn). nongms không geo-detect được nên khi chưa có quyết định
> consent → mặc định zero-ad, KỂ CẢ user ngoài EEA (mất doanh thu). Bật `true` = cho phép AppLovin
> phục vụ **NPA** ngay trong lúc chưa có quyết định. **Cờ này KHÔNG tắt dialog consent** (H-02, sửa
> sau vòng audit trước) — `needsGdprConsentDialog()` VẪN trả `true` khi chưa có quyết định, SDK vẫn
> yêu cầu host hỏi user để thu decision thật; chỉ khác là ad được serve (NPA) trong lúc chờ, không
> zero-ad. ⚠️ Trách nhiệm compliance thuộc integrator: chỉ bật nếu chắc chắn non-EEA hoặc chấp nhận
> serve NPA cho user chưa quyết định. Nếu app CÓ user EEA → vẫn nên thu consent thật càng sớm càng
> tốt (explicit Boolean / native CMP).
>
> **Re-consent nongms deferred-dialog:** `showConsentFormIfAvailable(activity)` trả `false` + phát lại
> `AdEvent.NeedsGdprConsent` → host tự hiện lại dialog rồi ghi qua `confirmGdprConsent()`. (gms/native
> CMP: hàm tự hiện form, trả `true`.)
>
> **Khi nào cần re-consent (3 trường hợp thật)**:
> 1. **User tự bấm** nút "Quyền riêng tư"/"Privacy Options" trong app (Settings) → host gọi
>    `showConsentFormIfAvailable(activity)` tay — luôn cho user quyền đổi ý bất kỳ lúc nào, không chỉ lần
>    đầu.
> 2. **gms (UMP)**: UMP **tự quyết định** có cần hiện lại form không mỗi lần `requestConsentInfoUpdate()`
>    chạy (mỗi lần mở app) — dựa trên cấu hình "Consent message" trên trang UMP của AdMob Console (dev
>    đổi nội dung/publish lại message → UMP tự re-prompt user cũ). SDK **không tự đặt timer hết hạn** —
>    hoàn toàn theo quyết định của UMP.
> 3. **nongms không CMP tự động** (không dùng native CMP) → hoàn toàn do host tự quyết định lúc nào hỏi
>    lại (SDK không có tín hiệu "đã hết hạn" để nhắc bạn) — tự đặt policy riêng (vd hỏi lại mỗi 12 tháng
>    theo khuyến nghị chung IAB TCF) nếu cần.

nongms có **2 lựa chọn** consent: (1) CMP riêng trả Boolean → truyền `applovinHasUserConsent` như trên
(đơn giản, **không** sinh TCF); (2) bật `applovinUseNativeCmp=true` (xem 8.4b) để AppLovin tự chạy
Terms/Privacy flow — AppLovin bundle Google UMP trong AAR nên hoạt động cả trên thiết bị non-Google →
có TCF/AC cho mediation. Dùng (2) thì **không** cần truyền `applovinHasUserConsent` và wrapper **không**
hiện dialog tự làm. Cả hai cách đều phải publish consent flow trên dashboard tương ứng và test trên máy
thật; không set cứng `true` cho production.

### 8.6 — Scenarios test tối thiểu

| Scenario | Cách test | Kết quả mong đợi |
|---|---|---|
| Non-EEA (VN) | Chạy bình thường | Ad load ngay, không hiện form |
| EEA, accept | Debug: set `DEBUG_GEOGRAPHY_EEA` → cold start | Form UMP hiện → Accept → ad load |
| EEA, deny | Debug: set `DEBUG_GEOGRAPHY_EEA` → chọn từ chối | UMP lưu lựa chọn; `canRequestAds` và privacy state được dùng đúng, không suy diễn nó là cờ personalized |
| Consent reset | Demo Menu → "♻️ Reset Consent" → cold start | Form hiện lại |
| Offline cold start | Tắt wifi → cold start | Watchdog 15 s → splash tiến hành, ad fail load (network) |
| GMS retry in-session | Tắt mạng → start → bật mạng, không recreate Activity | UMP retry; fullscreen preload lại |
| nongms chưa quyết định | `applovinHasUserConsent=null` | Không provider load/show |
| nongms deny | CMP trả `false` trước init | MAX log `Has User Consent=false`; contextual inventory tùy account |
| nongms accept | CMP trả `true` trước init | MAX log `Has User Consent=true` |

### 8.7 — Release checklist partner phải ký xác nhận

- [ ] AdMob App ID trong manifest thuộc đúng app/package.
- [ ] European regulations message đã **Publish**, không còn publisher misconfiguration.
- [ ] Privacy Policy URL public và mở được.
- [ ] Automated init-order gate xanh: consent/privacy state có trước GMA/MAX initialize.
- [ ] Có automated test + device log chứng minh không request/preload ad trước consent decision
      (repo sample đã pass; partner rerun bằng config production).
- [ ] GMS accept/deny/revoke/offline-retry đã test.
- [ ] nongms CMP hoàn tất trước MAX init; null/true/false đã test.
- [ ] AppLovin SDK key + 4 ad units thuộc account partner.
- [ ] MAX Privacy States đúng; mediation adapters không incomplete ngoài chủ đích.
- [ ] Debug dùng Google demo units/MAX Test Mode; release không dùng test inventory.
- [ ] Nếu app hướng trẻ em (child-directed) hoặc có user under-age-of-consent: đã đọc mục "Accepted
      risk đã biết" ở đầu file (App Set ID trước consent, `tagForChildDirectedTreatment`/
      `tagForUnderAgeOfConsent` crash trên AppLovin) và xác nhận dùng AdMob mode, KHÔNG dùng
      AppLovin/`nongms` cho nhóm user này.
- [ ] Đã đọc + hiểu các mục "Accepted risk đã biết" (App Set ID, COPPA crash, SSAID đọc sớm, root-tamper
      fail-open, QC test-device hash union) và xác nhận không ảnh hưởng compliance riêng của app/thị
      trường mình target.

Official references:

- Google UMP Android: https://developers.google.com/admob/android/privacy
- Google GMA initialization/preload warning: https://developers.google.com/admob/android/quick-start
- Tạo European regulations message: https://support.google.com/admob/answer/10113207
- Google EU User Consent Policy: https://support.google.com/admob/answer/7666519
- AppLovin privacy/consent flags: https://developers.applovin.com/en/max/android/overview/privacy/
- AppLovin MAX Terms/Privacy flow and UMP automation: https://developers.applovin.com/en/max/android/overview/terms-and-privacy-policy-flow/
- AppLovin MAX Test Mode: https://developers.applovin.com/en/max/android/testing-networks/test-mode/

## Step 9 — First-install VIP grace (1 ngày) — SDK built-in

**VIP feature mandatory — không skip. SDK đã build-in từ v1.1.4.**

### 9.1 — Grace flow (KHÔNG re-implement trong app code)

`AdmobApplovinWrapper` SDK (v1.1.4+) tự động grant 1 ngày VIP cho lần đầu cài:

- Trong `AdManager.init`, sau khi GAID resolve xong, SDK kiểm tra `appPreferences.isAddVIPMemberFirstInitSuccess()`.
- Nếu chưa grant + release build → query `InstallReferrerHelper.queryInstallBeginMs(context)` để lấy `installBeginMs`.
  ⚠️ **`installBeginMs` KHÔNG cùng nguồn giữa 2 flavor** — `gms`: timestamp Play Store thật (Install
  Referrer API, sống qua uninstall/reinstall cùng tài khoản Google). `nongms`: **không có** Install
  Referrer (không phải app Google Play) → dùng local "first-seen" (SharedPrefs ghi lúc SDK chạy lần đầu,
  trả `isReliable=true`) — **bị xoá khi uninstall/clear-data**, nên trên nongms reinstall = trial mới,
  không chống được abuse như gms.
- Nếu `installBeginMs` reliable + `(now - installBeginMs)` nằm trong khoảng
  `-AUTO_TRIAL_CLOCK_SKEW_TOLERANCE_MS..86_400_000L` (tức **-6h đến +24h** — dung sai âm 6 giờ cho đồng hồ
  máy chạy sai/NTP chưa sync, KHÔNG phải chỉ `0..86_400_000L`) → `appPreferences.saveAutoTrialUntil(installBeginMs + 86_400_000L)` → set `isVIPMember = true`. (Từ v1.2.0 auto-trial lưu key **riêng** `saveAutoTrialUntil`, **tách khỏi** `saveVipByKeyUntil` của VIP redeem/mua — revoke VIP không đụng trial và ngược lại.)
- Nếu `installBeginMs` **không reliable** (Install Referrer lỗi/không trả được) → fallback
  `saveAutoTrialUntil(now + 86_400_000L)` (provisional 1 ngày kể từ lần init đầu) — chỉ chạy khi config
  `autoTrialFallbackWhenNoReferrer = true` (mặc định `false`, chặn abuse sideload V4). ⚠️ Nếu
  `installBeginMs` **reliable nhưng đã quá 24h** (vd app cài lâu rồi mới tích hợp SDK), nhánh này KHÔNG
  fallback — trả `NoTrialSettled` (không cấp trial), khác với case "không reliable" ở trên.
- Set `addVIPMemberFirstInitSuccess()` flag → idempotent, các lần mở sau KHÔNG re-grant.
- Debug build: skip grace để dev không bị "VIP forever" làm rối QC test ad.

> ⚠️ **Vector gian lận đã biết (accepted, không phải bug)** — 2 cách user thật có thể lấy trial nhiều lần:
> 1. **`nongms` (Huawei/non-Google): uninstall + reinstall = trial mới LUÔN LUÔN** — không có Install
>    Referrer nên không chống được, kể cả user thường (không cần root).
> 2. **`gms`: đổi tài khoản Google + reinstall = trial mới** — giới hạn vốn có của cơ chế Install
>    Referrer (gắn theo tài khoản Play Store, không gắn theo phần cứng máy), không phải lỗ hổng riêng
>    của SDK này. Chi phí (cần tài khoản Google khác) đủ cao để không đáng lo với VIP marketing-only
>    (xem Accepted risk "VIP kích hoạt client-side" ở đầu file).
>
> Muốn chống chặt hơn (vd trial gắn cứng theo thiết bị, không theo tài khoản) → cần server riêng, ngoài
> phạm vi SDK offline này.

**Cấm**:
- KHÔNG lưu flag `pref_first_launch_vip_granted` thủ công ở app code.
- KHÔNG gọi `AdManager.activateVipByKey(...)` trong `Application.onCreate` để grant 1 ngày — SDK đã làm.
- KHÔNG thay đổi giá trị 86_400_000L (24h) — đó là contract mặc định.

### 9.2 — Verify ở runtime

- Sau lần init đầu tiên trên release build, log filter `###init` phải xuất hiện một trong:
  - `installBeginMs reliable, age=Xms → grant VIP until ...` (path chính)
  - `installBeginMs unreliable/missing → fallback grant 24h from now` (path fallback)
- `AdManager.isVipByKeyActive()` trả `true` trong 24h đầu.
- `AdManager.getVipByKeyExpiry()` — trả timestamp hết hạn VIP; `installBeginMs` là internal của SDK, không có public API để lấy giá trị này.

### 9.3 — UI hiện grace entry trong VIP screen

VIP screen (Step 10) phải nhận diện grace entry và hiển thị label đặc biệt:
- Nếu `getVipByKeyExpiry() > 0` và `AppPreferences.getInstance(context).isAddVIPMemberFirstInitSuccess() == true`
  và user CHƯA tự nhập key bao giờ → đây là grace entry (công thức đúng, xem thêm ở bảng cuối phần này —
  **đừng viết `addVIPMemberFirstInitSuccess() == true`, đó là setter trả `Unit`, không compile được**).
- Hiển thị label `R.string.vip_entry_first_install` ("🎁 Quà tặng cài đặt mới") thay vì label key thường.
- Lưu flag `pref_user_redeemed_key_at_least_once` trong `SharedPreferences` của app khi user redeem key thành công lần đầu — để phân biệt grace vs user-redeemed về sau.

## Step 10 — UI/UX VIP management screen (chi tiết, mandatory)

**VIP feature mandatory — không skip. Step 2 đã audit có hay chưa: nếu có thì upgrade in-place, nếu chưa thì tạo mới.**

### 10.1 — File structure (mandate)

```
app/src/main/java/<APP_PACKAGE>/
├── feature/vip/
│   ├── ActVipManagement.kt       ← Activity chính (UI + lifecycle). Reference demo dùng Activity (clone HexViewer); nếu project bạn theo single-Activity + Nav thì impl bằng Fragment cũng được — giữ nhất quán với app.
│   ├── VipKeys.kt                ← (CHỈ khi consumer ≤ 1.1.5) Map<plainKey, durationDays> + validator + Base64. Từ v1.2.0 dùng token ECDSA → KHÔNG cần file này.
│   └── VipPrefs.kt               ← CHỈ lưu cờ userRedeemed (lib đã expose AdManager.getVipGrantedAtMs(), đừng tự lưu lại)
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

| #  | Thành phần                              | Detail (Android Kotlin idiom)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|----|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Status header / Hero card               | `MaterialCardView` + `bg_vip_status_header_*.xml` (gradient gold khi active, xám khi free). Crown icon `ic_workspace_premium` + text 2 dòng:<br>• Active: `getString(R.string.vip_active)` + `getString(R.string.vip_until, formattedDate)`<br>• Free: `getString(R.string.vip_free_user)`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| 2  | Activation date row                     | TextView bind `SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(grantedAtMs))`. `visibility = if (active) VISIBLE else GONE`. `grantedAtMs` đọc từ **`AdManager.getVipGrantedAtMs()`** — KHÔNG tự lưu mốc riêng (lib đã expose, tự lưu sẽ sai khi VIP cấp qua đường khác như `grantVipDays`)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 3  | Expiry date row                         | TextView bind từ `AdManager.getVipByKeyExpiry()` qua cùng formatter. Visibility theo VIP active                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 4  | Progress bar (elapsed)                  | Material 3 `LinearProgressIndicator`. **Formula bắt buộc**: `progress = ((now − grantedAt) / (expiry − grantedAt) × 100).toInt().coerceIn(0, 100)`. **3 mốc trên timeline ngang**: `start = grantedAtMs` (mép trái, 0%), `end = expiryMs` (mép phải, 100%), `current = now` (vị trí thanh fill). Bar **rỗng lúc vừa kích hoạt → đầy dần đến hết hạn** (elapsed semantic). Tuyệt đối KHÔNG đảo formula thành `remaining/total` (đầy → rỗng) — UX confusing. Update mỗi giây qua `CountDownTimer.onTick`. Edge case `total ≤ 0` (clock skew / đã expire) → default `progress = 100` (đầy = đã hết hạn). Chỉ show khi VIP active                                                                                                                                                                                                                                                                                                                                              |
| 5  | Countdown text                          | Format `Xd HHh MMm SSs` (ví dụ `12d 03h 24m 15s`). Update mỗi giây qua `CountDownTimer(remainingMs, 1000L)`, gọi `cancel()` trong `onDestroyView`. **KHÔNG** dùng `Handler.postDelayed` (rule 10.5)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 6  | EditText nhập token/key VIP             | `TextInputLayout` + `TextInputEditText`, `startIconDrawable=@drawable/ic_confirmation_number`, `endIconMode="clear_text"`. **Flow khuyến nghị (token ECDSA)**: nút "Kích hoạt" (`MaterialButton`) → `val token = et.text?.trim()?.toString().orEmpty()` (⚠️ `et.text` là `Editable?` nullable, `.trim()` trên `CharSequence` trả về `CharSequence` chứ không phải `String` — viết `et.text.trim()` suông báo lỗi biên dịch; viết `et.text?.trim().toString()` thì compile được nhưng SAI về logic vì `Any?.toString()` trả chuỗi `"null"` khi rỗng thay vì chuỗi rỗng thật — PHẢI dùng `?.toString().orEmpty()` đủ 2 bước) → `AdManager.activateVipByToken(context, token)` trả `Boolean`. Nếu `true` → **KHÔNG cần tự lưu `grantedAtMs`** (đọc lại `AdManager.getVipGrantedAtMs()` là đủ) — chỉ cần `vipPrefs.markUserRedeemed()` (flag app-side, phân biệt trial/redeem) + show success (confetti #5). Token sinh bằng `AdManager.generateVipToken(privateKeyBase64, days)` (debug-gated, long-press crown để auto-fill mẫu). **Flow legacy (≤1.1.5)**: `inputType="textCapCharacters"`, lookup `VipKeys.lookupDays(input)` → `AdManager.activateVipByKey(context, input, days)`. Hoặc dùng `AdManager.showVipByKeyDialog(activity)` (dialog verify/success/failed có sẵn của lib)                                                                                                                                                                                    |
| 7  | Button "Xem quảng cáo → 3 ngày VIP"     | `MaterialButton` icon `ic_play_circle`. **Offline guard trước**: `if (!NetworkUtils.isDeviceConnected(ctx)) { showNoAdDialog(); return }`. Click → `AdManager.showRewarded(activity) { earned -> if (earned) grantVipFromAd() else fallbackInter() }`. **CHỈ nhánh `earned == true` của Rewarded mới được cấp VIP.** Fallback interstitial là **monetization-only, TUYỆT ĐỐI KHÔNG cấp reward** (Google/AppLovin Rewarded policy cấm credit reward cho ad format non-rewarded — vi phạm A-15, rủi ro ban account) — **signature đúng chỉ 2 tham số, KHÔNG tag string**: `AdManager.showInterstitial(activity) { showNoRewardDialog() }` (dù shown hay không, user KHÔNG earn lần này). `grantVipFromAd()` = `AdManager.grantVipDays(ctx, 3)` (API cấp VIP nội bộ tin cậy — chạy cả release, KHÔNG phụ thuộc allowLegacyPlaintextVipKey; đừng dùng activateVipByKey(vipKeySecret) vì nhánh legacy plaintext default TẮT nên fail im lặng). Pulse animation #1 wrap nút này. |
| 8  | Buy 30-day VIP (disabled)               | `MaterialButton` style `OutlinedButton.Icon` + `app:icon="@drawable/ic_card_giftcard"`, `isEnabled=false`, text `getString(R.string.vip_buy_30d_locked)` ("Mua VIP 30 ngày — sắp ra mắt"). Wait IAP integration                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 9  | Buy 90-day VIP (disabled)               | Tương tự #8, label `vip_buy_90d_locked`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 10 | Buy 1-year VIP (disabled)               | Tương tự #8, label `vip_buy_1y_locked`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 11 | Buy lifetime VIP (disabled)             | Tương tự #8, label `vip_buy_lifetime_locked`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| 12 | Active VIP card (single-entry)          | `MaterialCardView` show key label (grace label nếu first-install, hoặc "VIP X ngày" cho user-redeemed), days/hours remaining, expires date, button revoke (gọi `AdManager.clearVipByKey()` sau dialog confirm). Visibility GONE khi free user. Icon riêng cho grace entry: `🎁` Unicode hoặc `ic_first_install_gift`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 13 | Restore Purchase (disabled)             | `MaterialButton` outlined + `app:icon="@drawable/ic_restore"`, `isEnabled=false`, text `vip_restore_locked` ("Khôi phục mua hàng — sắp ra mắt"). Placeholder cho IAP tương lai                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| —  | Button "Thu hồi tất cả VIP"             | `MaterialAlertDialogBuilder` (Material You) confirm → `AdManager.clearVipByKey()` (đã tự xoá cả `getVipGrantedAtMs()`, không cần dọn riêng) → refresh UI. `isEnabled = AdManager.isVipByKeyActive()` (disable khi free user)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| —  | Toolbar badge "VIP" trên `MainActivity` | `Chip` hoặc TextView overlay trên toolbar. Active: chip vàng `0xFFFFD60A` text "VIP". Free: GONE. Refresh trong `onResume` của MainActivity hoặc sau khi quay về từ VIP screen (`ActivityResult`/`onResume`) khi state đổi                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| —  | Privacy Policy footer                   | `TextView` clickable, `setOnClickListener { startActivity(Intent(ACTION_VIEW, Uri.parse(AdKeys.PRIVACY_POLICY_URL))) }`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |

> 3 dòng đánh dấu **`—`** ở bảng trên (Thu hồi tất cả VIP / Toolbar badge / Privacy Policy footer) là
> thành phần **BẮT BUỘC** làm thêm — không tính vào con số "13" ở tiêu đề (13 chỉ đếm thành phần nằm
> trong thân màn hình VIP, 3 dòng này nằm ở toolbar/footer ngoài thân) — nhưng đừng bỏ qua khi checklist.
> Tổng cộng cần làm đủ: **13 + 3 = 16 thành phần**.

**Yêu cầu chung**:
- Toàn bộ dialog dùng `MaterialAlertDialogBuilder` (Material You / Material 3) — KHÔNG dùng `AlertDialog.Builder` cũ.
- Sau khi `activateVipByKey`/`activateVipByToken` thành công → ngay lập tức:
  1. **KHÔNG tự lưu `grantedAtMs`** — SDK đã lưu, đọc lại qua `AdManager.getVipGrantedAtMs()` khi refresh UI
  2. `vipPrefs.markUserRedeemed()` (flag app-side, phân biệt với grace entry — Step 9.3)
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

> **Activity vs Fragment**: reference demo (`ActVipManagement`) là **Activity** → cleanup ở `onDestroy()` (không phải `onDestroyView`), guard async bằng `if (isFinishing) return` (thay cho `!isAdded || _binding == null`), binding có thể `lateinit` vì sống cùng Activity. Nếu project bạn impl bằng **Fragment** thì áp đúng các rule `onDestroyView`/`_binding` bên dưới. Mọi rule "cancel timer/animator" áp dụng cho cả hai.

- **KHÔNG `Handler.postDelayed` cho countdown** — bắt buộc `CountDownTimer`, gọi `cancel()` trong `onDestroy()`/`onDestroyView`. `Handler.postDelayed` capture context dễ leak.
- **KHÔNG giữ rewarded callback dạng `object : RewardedListener` mà capture `this` của screen** — clear callback khi destroy HOẶC dùng `WeakReference`. Pattern an toàn (Activity reference):
  ```kotlin
  AdManager.showRewarded(this) { earned ->
      if (isFinishing) return@showRewarded
      if (earned) onRewardEarned()
  }
  // Fragment: if (!isAdded || _binding == null) return@showRewarded
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

| Source                                | Cách detect                                                                                                                                                                                                                                                               | UI label (string resource)                                                     |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| First-install grace 1 ngày            | ⚠️ **Lưu ý:** `isVipByKeyActive()` = `max(getVipByKeyUntil(), getAutoTrialUntil()) > now` nên nó **LUÔN `true`** trong lúc trial đang chạy — KHÔNG dùng `isVipByKeyActive() == false` để detect grace entry (không bao giờ thoả). Đúng theo demo thật (`ActVipManagement.kt`): `AppPreferences.getInstance(context).isAddVIPMemberFirstInitSuccess() && !vipPrefs.userRedeemedAtLeastOnce()`. `AppPreferences` **KHÔNG phải `internal`** (chỉ constructor `private`, dùng qua `AppPreferences.getInstance(context)`) — gọi được từ app module, demo đang dùng đúng cách này. | `R.string.vip_entry_first_install` ("🎁 Quà tặng cài đặt mới")                 |
| User redeem qua TextField (30/3 ngày) | `vipPrefs.userRedeemedAtLeastOnce() == true` (flag app-side tự lưu khi redeem thành công lần đầu)                                                                                                                                                                                                                              | Format `getString(R.string.vip_entry_redeemed, daysActivated)` ("VIP %d ngày") |

⚠️ **ĐỪNG tự thêm hàm `VipPrefs.getGrantedAtMs()`/tự lưu mốc `grantedAtMs` riêng** — lib đã expose sẵn
`AdManager.getVipGrantedAtMs()` (KDoc: *"nguồn sự thật DUY NHẤT... không phụ thuộc app-layer tự ghi mốc
riêng"*), tự lưu sẽ SAI khi VIP được cấp qua đường khác (vd watch-ad → `grantVipDays`, `VipPrefs`
app-side không hề biết sự kiện đó xảy ra). `VipPrefs.kt` bên dưới CHỈ cần giữ đúng 1 việc:
`userRedeemedAtLeastOnce()`.

```kotlin
class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    // KHÔNG lưu grantedAtMs ở đây — lib đã expose AdManager.getVipGrantedAtMs(), tự lưu sẽ SAI khi
    // VIP được cấp qua đường khác (vd watch-ad → grantVipDays) mà VipPrefs app-side không biết.
    fun markUserRedeemed() = sp.edit().putBoolean("user_redeemed_once", true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean("user_redeemed_once", false)
}
```

**Lý do `VipPrefs` chỉ còn 1 việc**: `userRedeemedAtLeastOnce` là thứ DUY NHẤT lib không thể biết (chỉ
app biết user đã tự tay redeem key/token hay chưa, để phân biệt với grace entry). `grantedAt` thì lib
**đã** persist và expose sẵn qua `AdManager.getVipGrantedAtMs()` — ĐỪNG tự dựng lại đường lưu trùng cho
`grantedAt`, dễ lệch dữ liệu với đường lib khi VIP được cấp qua kênh khác (`grantVipDays`, auto-trial).

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

Settings entry: thêm preference `Preference` trong settings screen (nếu project có) trỏ tới VIP screen (`ActVipManagement` / Fragment tương ứng) qua intent/navigation.

### 10.9 — Che giấu VIP key

Plain key ở Section 0 — KHÔNG hardcode plain trong file `.kt`. AI phải Base64-encode trước rồi hardcode chuỗi Base64 vào `companion object` / `private const val` (xem `VipKeys.kt` ở 10.6). KHÔNG thêm AES/NDK/ProGuard string-encrypt trừ khi user yêu cầu — Base64 là mức đã thống nhất với risk tradeoff: dễ reverse nhưng đủ chặn user thường peek decompiled APK.

## Step 11 — Self-review & cập nhật `<PROJECT_PATH>/doc/AD.MD`

Review duy nhất 1 lần, KHÔNG lặp 3 lần. Checklist:

- [ ] Mọi slot ad đã thay (grep tên SDK cũ không còn match)
- [ ] 2 build variant ở Section 0 đều xanh
- [ ] 6 policy rule đều PASS
- [ ] Consent đã wire ở Splash
- [ ] AdMob App ID đúng; European regulations message đã Publish; log không còn publisher misconfiguration
- [ ] GMS accept/deny/revoke/offline-retry đã test hoặc ghi rõ external blocker từ AdMob Console
- [ ] nongms CMP trả quyết định trước MAX init; `null` chặn, `true/false` áp đúng Privacy States
- [ ] AppLovin SDK key/ad units đúng account; máy QA đã bật MAX Test Mode
- [ ] VIP 1-day auto-trial dùng SDK built-in (Step 9.1) — KHÔNG tự re-implement bằng flag thủ công
- [ ] VIP screen có **đủ 13 thành phần UI** (10.3) + **đủ 5 animation** (10.4) + **0 vi phạm memory leak rule** (10.5)
- [ ] File structure đúng (10.1): `ActVipManagement.kt` (hoặc Fragment tương ứng), `VipPrefs.kt`, layout `f_vip_management.xml` (`VipKeys.kt` chỉ cần khi consumer ≤ 1.1.5 dùng Base64 key thay token)
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
- Policy: <6/6 hoặc list rule fail>
- Consent: <GMS UMP published/tested; nongms CMP wired/tested | blocker cụ thể>
- Memory review: <OK | list điểm nghi ngờ với file:line>
- VIP UI: <13/13 components, 5/5 animations | list missing>
- Files changed: <count>, lines +<X> -<Y>

### Open questions for user
- <key/id còn thiếu cụ thể>
- <decision pending nếu có>
```

KHÔNG lặp lại "3 round". Step 11 checklist PASS hết → dừng. Có FAIL thì fix tới khi PASS rồi mới báo cáo.

---

## ⚠️ Security Checklist cho Integrator (đọc trước khi production)

Integrator phải đọc và xác nhận mọi mục dưới đây trước khi ship. Lịch sử đầy đủ các đợt audit (kể cả
những mục đã đóng, không còn actionable): xem
`https://github.com/royt93/AdmobApplovinWrapper/blob/main/doc/audit/audit_gemini.md` (repo public,
mở trực tiếp bằng link này, không cần clone).

**Bắt buộc set `vipKeySecret` production thật** — release build `throw IllegalStateException` ngay tại
`setConfig()` nếu để mặc định (`warnIfWeakVipSecret()` cũng kiểm tra entropy: uppercase + digit + special
char), nên bạn không thể vô tình ship secret yếu:
```kotlin
AdSdkConfig(vipKeySecret = BuildConfig.VIP_KEY_SECRET, ...)
```

**App Open exclusion — dùng class reference, KHÔNG phải simpleName string:**

- Wrapper so `activity.javaClass` với `AdSdkConfig.appOpenExcludedActivities: List<Class<out Activity>>`
  (an toàn với R8/obfuscation — KHÔNG cần keep class name). Default = `emptyList()`.
- Cấu hình splash/onboarding của bạn (nếu không App Open có thể hiện đè lên splash khi foreground):
  `appOpenExcludedActivities = listOf(SplashActivity::class.java, OnboardingActivity::class.java)`.
- API string cũ `appOpenExcludedActivityNames: List<String>` (default `["SplashActivity"]`) **@Deprecated**
  — chỉ giữ backward-compat, đừng dùng cho project mới.
  > ⚠️ **Gotcha:** field deprecated này **vẫn ACTIVE** và **OR-combine** với `appOpenExcludedActivities`.
  > Vì nó default `["SplashActivity"]`, một Activity đặt tên đúng `"SplashActivity"` **VẪN bị suppress**
  > App Open dù bạn để `appOpenExcludedActivities = emptyList()`. Nếu (hiếm) bạn CỐ Ý muốn App Open đè
  > lên Activity tên "SplashActivity", set `appOpenExcludedActivityNames = emptyList()` để tắt.

**Auto-trial anti-abuse trên rooted device (rủi ro thấp, chấp nhận):** flag
`isAddVIPMemberFirstInitSuccess` lưu raw boolean trong SharedPreferences (không ký HMAC) — trên rooted
device, edit `loitp_admob.xml` → set `false` → reinstall → farm trial thêm. Chỉ ảnh hưởng user tự root
máy mình, không có action nào cần làm ngoài biết trước rủi ro này.

---

## ⚠️ Việc cần làm — Bật xin phép quảng cáo

### Vì sao cần làm việc này

App có quảng cáo. Ở châu Âu, Anh, Thụy Sĩ, và một số bang ở Mỹ, luật yêu cầu phải hỏi người dùng
có đồng ý xem quảng cáo cá nhân hóa hay không trước khi xử lý dữ liệu cho mục đích đó (ads
contextual/non-personalized vẫn có thể phục vụ tuỳ CMP/cấu hình — SDK không quyết định thay luật, đây
không phải tư vấn pháp lý). Việc bạn cần làm là làm theo đúng thứ tự **7 bước nhỏ trên 2 trang web quản
lý quảng cáo** bên dưới (bật vài công tắc + publish 2 thông báo), rồi **chụp màn hình lại mỗi bước** làm
bằng chứng đã làm.

⚠️ **QUAN TRỌNG — công tắc AppLovin CHỈ có tác dụng nếu dev đã bật đúng 1 dòng code trước
(`AdSdkConfig.applovinUseNativeCmp = true` + `applovinPrivacyPolicyUrl`, mặc định SDK là `false`).**
Nếu partner KHÔNG rành kỹ thuật: **nhờ dev xác nhận đã bật `applovinUseNativeCmp=true` chưa** trước khi
coi việc bật công tắc dashboard dưới đây là xong — bật công tắc dashboard mà code vẫn ở default `false`
thì **không có tác dụng runtime nào cả** (SDK dùng luồng hỏi-consent tự làm riêng, không đọc dashboard
này). Phần Google AdMob (Phần 2 dưới) thì công tắc dashboard **luôn có tác dụng ngay**, không cần code
đổi gì thêm — chỉ riêng phần AppLovin (Phần 1) mới cần xác nhận với dev.

Cần có sẵn: tài khoản đăng nhập vào 2 trang:

- `dash.applovin.com` (trang quản lý quảng cáo AppLovin)
- `apps.admob.com` (trang quản lý quảng cáo Google AdMob)

Nếu chưa có tài khoản, xin liên hệ người đang quản lý 2 trang này trong công ty.

Sau khi làm xong, gửi lại toàn bộ ảnh chụp màn hình (đặt tên file theo đúng tên trong ngoặc ở mỗi
bước) cho người phụ trách kỹ thuật.

### Phần 1 — Trang AppLovin (dash.applovin.com)

1. Đăng nhập, chọn đúng app (bản thật sắp phát hành, không phải app test).
2. Tìm mục tên **"Consent Flow"** (hoặc **"CMP"**) trong menu bên trái. Bật công tắc **Enabled**
   (Bật). Điền vào ô "Privacy Policy URL" đường link chính sách bảo mật của app (nếu chưa điền).
   → Chụp màn hình, đặt tên file: `anh_1_applovin_consent.png`
3. Tìm mục **"Privacy"** (hoặc **"GDPR & CCPA"**). Bật cả 3 công tắc nếu đang tắt:
    - TCF 2.0
    - Google Additional Consent
    - CCPA / US Privacy
      → Chụp màn hình, đặt tên file: `anh_2_applovin_gdpr.png`
4. Kiểm tra dòng chữ về **"app-ads.txt"** — nếu có dấu ⚠️ (cảnh báo), báo lại cho người phụ trách
   kỹ thuật (việc này do người quản lý website công ty xử lý, không phải bạn).
   → Chụp màn hình, đặt tên file: `anh_3_applovin_appads.png`

### Phần 2 — Trang Google AdMob (apps.admob.com)

1. Đăng nhập, chọn đúng app (bản thật sắp phát hành).
2. Vào menu **"Privacy & messaging"** → mục **"European regulations"** (hoặc "GDPR"). Bấm publish
   (xuất bản) thông báo xin phép — sau khi xong phải thấy chữ **"Published"** (đã xuất bản), không
   phải "Draft" (bản nháp).
   → Chụp màn hình, đặt tên file: `anh_4_admob_gdpr.png`
3. Cùng menu đó, mục **"US states"** — nếu app phục vụ người dùng ở Mỹ, publish thông báo tương tự.
   → Chụp màn hình, đặt tên file: `anh_5_admob_us.png`
4. Vào mục **"Settings"** trong "Privacy & messaging" — bật 2 công tắc: **IAB TCF v2** và
   **Additional Consent Mode**.
   → Chụp màn hình, đặt tên file: `anh_6_admob_tcf.png`
5. Kiểm tra mục **"app-ads.txt"** trong phần cài đặt app — trạng thái phải là **Found** hoặc
   **Authorized**.
   → Chụp màn hình, đặt tên file: `anh_7_admob_appads.png`

### Phần 3 — File `app-ads.txt` trên website của bạn (chỉ cần nếu Phần 1/2 báo ⚠️ thiếu)

Đây là 1 file nhỏ để CHỨNG MINH với AppLovin/Google rằng chính công ty bạn được phép bán quảng cáo cho
app này — thiếu file này, một số quảng cáo tự động bị giảm giá/từ chối hiện mà không báo lỗi rõ ràng.

1. Ở Phần 1 bước 4 và Phần 2 bước 5 phía trên, dashboard AppLovin/AdMob có sẵn dòng chữ cần dán —
   copy y nguyên (thường ghi kiểu `google.com, pub-xxxxxxxx, DIRECT, ...` hoặc tương tự cho AppLovin).
2. Đăng file đó lên website chính thức của công ty, đúng đường dẫn
   `<tên-miền-của-bạn>/app-ads.txt` (ví dụ: `yourcompany.com/app-ads.txt`) — nhờ người quản trị
   website làm giúp nếu bạn không tự đăng web được.
3. Chờ vài giờ tới 1 ngày, quay lại Phần 1 bước 4 / Phần 2 bước 5 kiểm tra dấu ⚠️ đã hết chưa.
   → Chụp màn hình xác nhận, đặt tên file: `anh_8_appads_txt_confirmed.png`

### Phần 4 — Data Safety form (Google Play Console) + xác nhận pháp lý

Hai việc này **Google Play bắt buộc**, thiếu có thể bị từ chối duyệt app hoặc gỡ app sau này. SDK
không tự làm thay được — đây là khai báo về chính app của bạn.

1. Vào **Google Play Console** → app của bạn → menu **"App content"** → mục **"Data safety"**.
2. Khai app **CÓ** thu thập mã định danh quảng cáo (Advertising ID) để hiện quảng cáo qua
   AdMob/AppLovin — Google có sẵn danh sách để tick chọn, không cần gõ tay.
   → Chụp màn hình sau khi submit, đặt tên file: `anh_9_data_safety.png`
3. App của bạn hướng tới ai — **chỉ người lớn**, **chỉ trẻ em (child-directed)**, hay **cả hai
   (mixed-audience)**? Khai đúng mục "Ads"/COPPA tương ứng trong CÙNG form Data Safety — câu trả lời
   này phải khớp với cấu hình `tagForChildDirectedTreatment` trong code (xem mục "Accepted risk đã
   biết" phía trên: **app hướng thuần trẻ em không được dùng AppLovin MAX**, và set cờ này sai lúc
   dùng AppLovin sẽ khiến app **crash** — đây là hành vi cố ý của SDK, không phải bug).
4. Nếu công ty có luật sư/bộ phận pháp lý: gửi họ xem app đang hiện quảng cáo qua AdMob + AppLovin,
   hỏi đã đủ điều kiện phát hành đúng luật ở từng khu vực bạn nhắm tới chưa (GDPR châu Âu, UK-GDPR,
   CCPA California, LGPD Brazil...). Đây là quyết định pháp lý — SDK/tài liệu này không thay thế được
   tư vấn pháp lý thật.

### Xong việc

Gửi lại toàn bộ ảnh chụp (`anh_1...` đến `anh_9...`, bỏ qua `anh_8` nếu Phần 1/2 không báo ⚠️
app-ads.txt) — vậy là xong. Nếu bước nào không tìm thấy nút/mục như mô tả (Google/AppLovin có thể đổi
giao diện), chụp màn hình chỗ đang bị kẹt và gửi lại, sẽ có hướng dẫn tiếp.

⚠️ **Trước khi coi là xong**: xác nhận lại với dev đã set `AdSdkConfig.applovinUseNativeCmp = true` trong code
chưa (xem cảnh báo ⚠️ QUAN TRỌNG ở đầu mục "Việc cần làm — Bật xin phép quảng cáo" phía trên) — bật công tắc
"Consent Flow" ở dashboard AppLovin KHÔNG có tác dụng gì nếu thiếu dòng code này, khác AdMob (chỉ cần dashboard
là đủ).

---

## Phụ lục C — Đọc `getDiagnostics()` (legend + cây quyết định)

Gặp "không có ad" → chạy `android.util.Log.w("Ads", AdManager.getDiagnostics())` và đọc **dòng cuối `ADS STATUS:`**. Đó
là root cause + fix. Các dòng phía trên là ngữ cảnh.

**Output mẫu (rút gọn):**

```
===== AdManager Diagnostics — SDK x.y.z =====
Build flavor          : gms (AdMob + AppLovin)
setConfig() called    : true
Effective provider    : APPLOVIN
  IS_ENABLE_ADMOB(cfg) : false
  isAdmobSupported     : true
Provider init state   : INITIALIZED
blockLoadUntilConsent : true
gdprMisconfigForceBlock: false
consentDecisionAllows : true
Consent state         : OBTAINED  (⚠️ SUY DIỄN — xem raw bên dưới)
applovinHasUserConsent: null
UMP consentStatus(raw): OBTAINED
UMP privacyOptions    : REQUIRED
VIP member            : false
Network connected     : true
------------------------------------------------------------
ADS STATUS: ✅ READY — không gate nào chặn...
```

**Legend — dòng `ADS STATUS:` → nguyên nhân → fix:**

| Dòng `ADS STATUS` bắt đầu bằng                      | Nghĩa                                                                                                                                                                                                     | Fix                                                                                                                                                                      |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `⛔ BLOCKED — chưa gọi AdManager.setConfig(...)`     | Quên `setConfig()`                                                                                                                                                                                        | Gọi `setConfig()` rồi `initialize()` trong `Application.onCreate()` (Bước 4)                                                                                             |
| `ℹ️ KHÔNG hiện ad vì user đang là VIP`              | **KHÔNG phải lỗi** — cố ý ẩn ad cho VIP                                                                                                                                                                   | Không cần fix. Thử revoke VIP nếu muốn test ad                                                                                                                           |
| `⛔ BLOCKED — GDPR MISCONFIGURATION`                 | `blockLoadUntilConsent=false` nhưng UMP xác nhận EEA/UK/CH — **chỉ chạy trên build `gms`** (cần UMP để biết geo; `nongms` không có tín hiệu này nên không tự phát hiện được, xem cảnh báo ngay dưới bảng) | Đặt `AdSdkConfig.blockLoadUntilConsent=true` rồi rebuild (flag sticky tới khi `setConfig()` lại)                                                                         |
| `⛔ BLOCKED — đang chờ quyết định consent`           | Provider `WAITING_FOR_CONSENT`                                                                                                                                                                            | **gms:** gọi `requestConsentInfoUpdate(activity){...}` ở Splash TRƯỚC show. **nongms:** set `applovinHasUserConsent=true/false` trong `AdSdkConfig` TRƯỚC `initialize()` |
| `⚠️ Không tính được lý do:`                         | SDK gặp exception khi dựng chẩn đoán — không phải trạng thái ad                                                                                                                                           | Gửi nguyên dòng log này kèm báo lỗi                                                                                                                                      |
| `⛔ BLOCKED — provider init chưa xong`               | Đang `INITIALIZING`/khác `INITIALIZED`                                                                                                                                                                    | Đợi callback `initialize(){ success -> }` trả `success=true` rồi mới show                                                                                                |
| `⛔ BLOCKED — C-04 cold-start: UMP chưa resolve geo` | Cold-start, UMP chưa ghi `IABTCF_gdprApplies`                                                                                                                                                             | **KHÔNG phải lỗi cấu hình** — chờ Splash chạy `requestConsentInfoUpdate()` lần đầu OK. Kiểm tra mạng/lỗi UMP nếu kẹt                                                     |
| `⛔ BLOCKED — consent chưa cho phép`                 | `blockLoadUntilConsent=true` + consent checker=false                                                                                                                                                      | Hoàn tất consent; hoặc `blockLoadUntilConsent=false` nếu app KHÔNG nhắm EEA/UK/CH                                                                                        |
| `⏳ Tạm thời không có ad — mất mạng`                 | Mất mạng (validated)                                                                                                                                                                                      | Tự load lại khi mạng về. Không cần fix                                                                                                                                   |
| `✅ READY — không gate nào chặn`                     | SDK sẵn sàng                                                                                                                                                                                              | Vẫn không thấy ad → sai ad unit ID (nhầm provider) HOẶC no-fill (network chưa có inventory). Kiểm tra `Effective provider` khớp ID bạn cấu hình                          |

> ⚠️ **`GDPR MISCONFIGURATION` là backstop chỉ có trên `gms`.** Nó dựa vào tín hiệu geo do UMP (thư
> viện Google) ghi ra — build `nongms` không có UMP nên không có backstop tương đương. Nếu bạn build
> `nongms` và target EEA/UK/CH, việc set đúng `blockLoadUntilConsent`/tự implement consent flow là
> **hoàn toàn trách nhiệm của bạn** — SDK không tự phát hiện và không cảnh báo được misconfig này trên
> `nongms`.

**Field quan trọng khác (khi `ADS STATUS` = READY mà vẫn không ra ad):**

- `Effective provider` = `ADMOB`/`APPLOVIN` — phải khớp ad unit ID bạn set. `APPLOVIN` mà bạn cấu hình ID AdMob →
  no-fill. Trên **nongms** luôn `APPLOVIN` dù `IS_ENABLE_ADMOB=true` (AdMob bị loại).
- `isAdmobSupported=false` → đang build **nongms**, AdMob không tồn tại (không phải bug).
- `consentDecisionAllows` = `false` → còn 1 gate consent chưa mở (xem `ADS STATUS`).
- `UMP consentStatus(raw)` / `UMP last error` — tín hiệu UMP THÔ (không suy diễn); có `last error` → UMP fetch fail (
  mạng/misconfig console).

---

Kết quả QC thiết bị thật + lộ trình kỹ thuật, cập nhật liên tục theo tag mới nhất: xem
`https://github.com/royt93/AdmobApplovinWrapper/tree/main/doc/audit` và
`https://github.com/royt93/AdmobApplovinWrapper/tree/main/doc/test` (repo public, mở trực tiếp bằng
link, không cần clone).

---

> Mọi finding trên đã được team SDK verify (unit test + review + thiết bị thật). File này (`AD_PROMPT_AOS.MD`)
> là **tài liệu bàn giao DUY NHẤT** partner cần — tự chứa đủ Quickstart + Production checklist + AI-migration
> prompt. Cần hỗ trợ sâu hơn (API spec đầy đủ, kiến trúc nội bộ) → liên hệ team SDK.
