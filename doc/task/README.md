# Backlog OpenCalc — 2026-08-28

Backlog rã theo kiểu scrum từ audit toàn bộ source code (5 agent nội bộ đọc song song từng subsystem, + đối chiếu ý kiến độc lập từ codex/gemini(agy)/claude session riêng — xem `06_external_ai_opinions.md`).

## Cấu trúc file

| File | Nội dung |
|---|---|
| [`01_fixes.md`](01_fixes.md) | Bug/rủi ro thật cần fix, có file:line, priority P0-P3 |
| [`02_enhancements.md`](02_enhancements.md) | Cải thiện code/UX/kiến trúc hiện có |
| [`03_new_features.md`](03_new_features.md) | Tính năng mới hợp lý cho app |
| [`04_ideas.md`](04_ideas.md) | Ý tưởng thô, chưa chắc làm |
| [`05_exclusive_features.md`](05_exclusive_features.md) | Tính năng độc quyền/khác biệt để cạnh tranh Play Store |
| [`06_external_ai_opinions.md`](06_external_ai_opinions.md) | Đối chiếu ý kiến độc lập từ codex / agy(gemini) / claude session khác |

## Trạng thái (theo rule R2 — cập nhật ngay sau mỗi quyết định)

✅ Implemented · 🟡 In progress · 📋 Picked (đã chọn, chưa làm) · ⏸️ Deferred · ❌ Skipped · 💭 Ideas

Tất cả item trong backlog này hiện ở trạng thái **💭 Ideas / chưa triển khai** — chờ quyết định sprint kế tiếp.

## ✅ Sprint 1 — ĐÃ XONG (2026-08-28)

User chọn kết hợp **Stabilize (P0/P1 bug)** + **Differentiation (exclusive feature)**, bỏ qua Monetization/Play Billing (N-AD-1/N-AD-2) cho sprint này. Toàn bộ 16 hạng mục dưới đây đã code xong, build+lint+test (`testDevDebugUnitTest`, `lint`, `assembleDevDebug`) PASS, và verify thực tế trên device `SM-S928B`/thiết bị hiện tại (screenshot trực tiếp cho các fix quan trọng: tan(90°), ln(0.5), Bill Splitter, Theme swatch).

**Stabilize (P0/P1) — ✅ Implemented:**
- F-CALC-1/6, F-CALC-2, F-CALC-3, F-CALC-7, F-CALC-8 (bug core tính toán + race condition cờ lỗi) — `ext/Calculator.kt`, `ui/MainActivity.kt`.
- F-UI-1 (Settings 6/7 preference chết — key mismatch) — `db/MyPreferences.kt`.
- F-INFRA-1 (5 locale bị cắt: cs/ml/or/pl/sr) — `app/build.gradle`.
- F-UI-2 (race condition + đọc View sai thread), F-UI-3 (scientific mode ẩn vĩnh viễn landscape), F-UI-5 (contentDescription sai 12 chỗ), F-UI-9 (StackOverflow input dài) — `ui/MainActivity.kt` + layout XML + `strings.xml`.
- F-DATA-1 (crash JSON hỏng), F-DATA-4 (unbounded history ∞) — `db/MyPreferences.kt`.
- F-DATA-2/3 (history I/O trên Main Thread) — `ui/MainActivity.kt`.
- N-INFRA-1 (CI pipeline GitHub Actions build+lint+test) — `.github/workflows/ci.yml` + `app/lint-baseline.xml` (baseline hoá nợ lint cũ, vd F-UI-7, để không chặn CI).

**Differentiation (exclusive feature) — ✅ Implemented:**
- X-CALC-1 (Explain my error — giải thích domain error cụ thể thay vì chung chung) — `ext/Calculator.kt` (`DomainErrorReason`) + `ui/MainActivity.kt`.
- X-UI-0 (Bill Splitter — màn hình mới, menu "Bill Splitter") — `ui/BillSplitterActivity.kt` + layout + manifest + menu.
- X-UI-1/0b (Theme preview carousel dạng lưới swatch màu thay vì list chữ) — `model/Themes.kt`, `model/ThemeSwatchAdapter.kt` + layout.

**⏸️ Deferred / chưa làm trong sprint này (cần input hoặc quyết định thêm từ user):**
- **F-AD-1** (AdMob Rewarded test ID) — KHÔNG tự sửa được vì cần ID rewarded thật từ AdMob Console, không thể tự bịa. Vẫn đang dùng test ID trong `app/build.gradle:47`.
- **F-SEC-1** (keystore + password lộ trên GitHub public) — giữ nguyên quyết định hoãn xử lý git/GitHub của user, chỉ ghi nhận.
- **X-AD-5** (marketing hoá ad-UX tốt sẵn có) — bản chất là việc viết mô tả Play Store, không phải code, chưa làm.

Còn lại trong backlog (Monetization Play Billing N-AD-1/N-AD-2, phần lớn Enhancements/New Features/Ideas khác) giữ nguyên **💭 Ideas** chờ sprint sau.

## ✅ Sprint 2 — ĐÃ XONG (2026-08-29)

Theo feedback UI trực tiếp từ user (icon VIP crown quá to) + chọn tiếp 1 tính năng mới qua `AskUserQuestion`.

- **UI feedback — VIP badge chip** — thu nhỏ 2 lần liên tiếp theo phản hồi user (`chipVipBadge` trong `a_main.xml`): giảm `chipMinHeight`/`layout_height` 24dp→18dp, `textSize` 11sp→9sp, `chipIconSize` 16dp→14dp→10dp, thêm `app:ensureMinTouchTargetSize="false"` (nguyên nhân gốc khiến chip trông to hơn nội dung thực — MDC ép min touch target 48dp).
- ✅ **N-CALC-8 (Memory M+/M−/MR/MC)** — xem chi tiết ở [`03_new_features.md`](03_new_features.md). Verify đầy đủ 4 nút trên emulator Pixel_10_Pro_XL.
- ✅ **N-CALC-5 (History tap-to-recall)** — hoá ra đã có sẵn từ trước, nhưng vô tác dụng do một bug P0 mới phát hiện: **F-DATA-9** — `equalsButton` tự huỷ chính coroutine của nó qua `calculationJob` dùng chung (regression từ chính fix Sprint 1), khiến history KHÔNG BAO GIỜ được lưu dù UI vẫn hiển thị kết quả đúng. Đã fix (`NonCancellable`) + verify history lưu đúng + tap-to-recall hoạt động đúng trên emulator. Xem [`01_fixes.md`](01_fixes.md#-f-data-9).

- ✅ **N-CALC-6 ("ans" reference)** — xem chi tiết ở [`03_new_features.md`](03_new_features.md). Thay token bằng string substitution trước `addMultiply`, không đụng parser lõi. Verify `5+3=8` → `ans+1=9` trên emulator.

- ✅ **N-DATA-4 (Pin/favorite history)** — xem chi tiết ở [`03_new_features.md`](03_new_features.md). Icon tim thêm vào cả 6 biến thể `i_history.xml`, mọi logic trim history đổi sang pin-aware. Verify: ghim → Clear History → chỉ entry ghim sống sót.

- ✅ **N-UI-4 (Copy-as-text/Share)** — xem chi tiết ở [`03_new_features.md`](03_new_features.md). Long-press history row → popup Copy/Share → Android share sheet. Phát hiện + fix bug UX: long-press gốc trên `itemView` không bao giờ nhận được sự kiện do bị 2 TextView con chiếm hết.

- ✅ **N-DATA-3 (Search/filter history)** — xem chi tiết ở [`03_new_features.md`](03_new_features.md). Ô tìm kiếm lọc theo calculation/result trong RAM, không đụng logic ghim/trim/persist hiện có. Tránh trước 1 bug: sửa các nơi dùng `itemCount` (bị thu hẹp khi lọc) sang `fullHistorySize` cho trim.

Tính năng tiếp theo cho vòng loop kế tiếp chưa chọn — chờ user quyết định qua `AskUserQuestion` khi bắt đầu.

## Priority & Size convention

- **P0** = critical, nên làm ngay/trước khi phát hành release tiếp theo (mất doanh thu, crash, data loss).
- **P1** = high, ảnh hưởng rõ tới UX/correctness/doanh thu, nên vào sprint gần nhất.
- **P2** = medium, giá trị tốt nhưng không khẩn.
- **P3** = low, nice-to-have.
- **Size**: S = <1 ngày, M = 1-3 ngày, L = >3 ngày/cần thiết kế riêng.

## 🔴 P0-CRITICAL — vượt trên mọi P0 khác, đã verify trực tiếp bằng git/gh (không phải suy đoán)

**Keystore ký release (`app/keystore.jks`) + mật khẩu (`KS_PW` trong `gradle.properties`) đang lộ công khai trên GitHub public repo `tplloi/OpenCalc` từ 2023-05-01 tới nay.** Ai cũng clone được và tự ký APK giả mạo bằng đúng cert app thật. Chi tiết đầy đủ + các phương án xử lý (đổi private/rotate key/purge history) ở `01_fixes.md#F-SEC-1`. **User đã chọn: ghi nhận vào backlog, chưa xử lý ngay trên git/GitHub — tự quyết định thời điểm xử lý sau.**

## Top P0 cần xử lý trước tiên (bất kể chọn hướng nào ở dưới)

1. **Settings UI có 6/7 preference chết** (đổi trong Settings không có tác dụng gì) — `01_fixes.md#F-UI-1`.
2. **5 locale bị build script cắt khỏi APK** dù UI vẫn cho chọn (Czech/Malayalam/Odia/Polish/Serbian) — `01_fixes.md#F-INFRA-1`.
3. **AdMob Rewarded ID release vẫn là test ID Google** — mất doanh thu rewarded nếu bật AdMob — `01_fixes.md#F-AD-1`.
4. **Không có CI pipeline** (build+lint+test) — rủi ro merge lỗi lên `main`/`dev` không ai biết — `03_new_features.md#N-INFRA-1`.
