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

## 📋 Sprint 1 — đã chọn (2026-08-28)

User chọn kết hợp **Stabilize (P0/P1 bug)** + **Differentiation (exclusive feature)**, bỏ qua Monetization/Play Billing (N-AD-1/N-AD-2) cho sprint này.

**Stabilize (P0/P1):**
- F-UI-1 (Settings 6/7 preference chết), F-INFRA-1 (5 locale bị cắt), F-AD-1 (rewarded test ID release), F-CALC-1/2/3/7/8 (bug core tính toán + race condition), F-UI-2/3/5/9 (race condition input, scientific mode ẩn landscape, accessibility, StackOverflow input dài), F-DATA-1/2/3/4 (crash JSON, I/O main thread, unbounded history), N-INFRA-1 (CI pipeline build+lint+test).
- F-SEC-1 (keystore leak): **giữ nguyên quyết định hoãn xử lý git/GitHub** — không nằm trong sprint code, chỉ theo dõi.

**Differentiation (exclusive feature, effort thấp-vừa):**
- X-CALC-1 (Explain my error — tận dụng hạ tầng cờ lỗi sau khi fix F-CALC-1/2/5), X-UI-0 (Bill Splitter), X-UI-1 (theme preview carousel marketing), X-UI-0b (Material You dynamic color), X-AD-5 (marketing hoá ad-UX-tốt-sẵn-có).

Trạng thái các item trên: **📋 Picked**. Còn lại trong backlog (Monetization Play Billing, phần lớn Enhancements/New Features/Ideas khác) giữ nguyên **💭 Ideas** chờ sprint sau.

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
