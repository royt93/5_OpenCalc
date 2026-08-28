# EXCLUSIVE FEATURES — độc quyền/khác biệt để cạnh tranh Play Store

## Core tính toán
- **X-CALC-1 — "Explain my error" tooltip.** Thay "Domain error"/"Math error" chung chung bằng giải thích cụ thể (vd "tan(90°) không xác định vì cos(90°)=0"), tận dụng 3 cờ lỗi toàn cục sẵn có (`division_by_0`, `domain_error`, `syntax_error`). Hạ tầng phân loại lỗi đã có sẵn (sau khi fix F-CALC-1/2/5) — khác biệt hoá rẻ so với xây từ đầu. P2 · M.
- **X-CALC-2 — "Friendly percent" là điểm khác biệt có sẵn, cần quảng bá + hardening.** `getPercentString` (`Expression.kt`) đã xử lý % theo nghĩa trực quan người dùng (không thuần toán học) — nên tài liệu hoá & marketing rõ hơn thành tính năng UX nổi bật, đồng thời tăng test coverage (E-CALC-4) để không regress vì đây là lợi thế cạnh tranh thật.
- **X-CALC-4 — "Calculator kiểm chứng kết quả".** Chạy song song 2 engine hoặc kiểm tra ngược, cảnh báo khi gần điểm kỳ dị/overflow/mất độ chính xác — định vị "calculator đáng tin cậy" (nguồn: codex, độc lập; hội tụ với X-CALC-1 "Explain my error" — cả 2 external agent đều độc lập đề xuất khai thác hạ tầng phân loại lỗi sẵn có làm điểm khác biệt). P3 · L.
- **X-CALC-3 — Gợi ý sửa dựa trên cờ lỗi.** Vd `syntax_error` do "!" đứng một mình → gợi ý "Thêm số trước dấu giai thừa" thay vì chỉ báo lỗi cú pháp chung. P3 · M.

## UI/UX
- **X-UI-1 — Theme depth làm headline feature.** App đã có 9 theme (Sunset/Ocean/Forest/Nord/Lavender/HighContrast...) vượt xa calculator stock. Thêm theme preview carousel (thay dialog list đơn giản), seasonal/limited theme, per-theme accent màu nút — marketing rõ trên store listing. P2 · M.
- **X-UI-2 — "One-hand mode" gesture-first.** Reachability mode dịch cả bàn phím về vùng ngón tay cái, thu gọn history panel — khác biệt vs AOSP/Samsung calculator (không có tính năng này). P3 · L.
- **X-UI-3 — "Living" VIP chip mở rộng thành gamification badge.** `chipVipBadge` + `startChipPulse()` đã là điểm nhấn premium đẹp — mở rộng cùng mechanic (pulse/gold) thành streak/daily-calc badge cho free user, tăng engagement thói quen. P3 · M.
- **X-UI-4 — Cross-device continuity qua VIP account.** Sync history/theme/settings đa thiết bị gắn với VIP entitlement hiện có — định vị VIP là "your calculator everywhere" thay vì chỉ ad-free. P3 · L.

## Ad & VIP / Monetization
- **X-AD-1 — VIP ẩn TRIỆT ĐỂ mọi touchpoint ad, có test coverage bắt buộc.** Cần audit đảm bảo banner (About/Settings), App Open (Splash), Interstitial (MainActivity) đều tự động skip khi `AdManager.isVipByKeyActive()==true` — đây là lời hứa cốt lõi của "VIP" nên phải có instrumented test verify (hiện chỉ có test cho nút rewarded, chưa test banner/app-open bị skip). P1 · S (test) — coi đây là điều kiện tối thiểu trước khi quảng bá VIP.
- **X-AD-2 — Lịch sử không giới hạn + cloud sync (Google account) độc quyền VIP.** Free hiện giới hạn/lưu local qua Gson blob; VIP-only backup/sync qua Google account, khôi phục khi đổi máy — giá trị giữ chân rõ rệt. P2 · L (phụ thuộc E-DATA-4 Room migration trước).
- **X-AD-3 — Custom theme/icon pack độc quyền VIP.** Bộ theme riêng (vd "Gold VIP" khớp màu `vip_gold` đã dùng ở chip) chỉ mở khoá khi VIP active — tận dụng chip vàng sẵn có làm điểm nhấn thương hiệu. P2 · M.
- **X-AD-4 — Scientific/Graphing mode nâng cao chỉ dành VIP.** Scientific mode cơ bản hiện free; thêm tier cao hơn (vẽ đồ thị, giải phương trình, matrix/complex number — xem N-CALC-2/7) độc quyền VIP, phân khúc rõ free vs pro, hợp lý hoá giá subscription (gắn với N-AD-1 Play Billing). P2 · L.

## Data/Persistence
- **X-DATA-1 — Private History mã hoá, khoá bằng sinh trắc học.** `BiometricPrompt` + Jetpack Security `EncryptedSharedPreferences` (hoặc SQLCipher nếu đã Room) cho vault riêng các phép tính nhạy cảm — hầu hết calculator khác để history plaintext. P3 · L.
- **X-DATA-2 — Cross-device sync qua Google Sign-In sẵn có** (Drive appDataFolder, không cần backend riêng) — đồng bộ history+settings, định vị "Calculator nhớ bạn ở mọi thiết bị". (Trùng hướng với X-AD-2, chọn 1 trong 2 khi lên sprint để tránh làm 2 lần.) P3 · L.
- **X-DATA-3 — Smart suggestion từ history on-device.** Gợi ý hoàn thành biểu thức khi gõ dở dựa trên tần suất/độ gần đây trong history local — không cần cloud, giữ được câu chuyện privacy. P3 · L.
- **X-DATA-4 — Insight/streak sử dụng** tích hợp cùng module VIP để tăng engagement (thống kê streak, phép toán hay dùng nhất). P3 · M.
- **X-DATA-5 — "Giải thích phép tính"** — dựng lại & diễn giải từng bước từ chuỗi `calculation` đã lưu bằng chính parser `Calculator` hiện có, biến history thành công cụ học tập thay vì chỉ log. P3 · M.

> Infra/Build/Testing: không có ý tưởng "exclusive feature" phù hợp cho hạng mục này — bản chất hạ tầng không phải điểm khác biệt user-facing.
