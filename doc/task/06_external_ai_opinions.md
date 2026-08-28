# Đối chiếu ý kiến độc lập (codex / agy=Gemini / claude session riêng)

Phương pháp: sau khi 5 agent nội bộ (Claude, cùng phiên này) audit xong từng subsystem, chạy thêm 3 AI agent **độc lập, không thấy kết quả audit nội bộ**, tự đọc lại toàn bộ source và tự đưa ra backlog theo cùng khuôn 5 mục — mục đích chống anchoring bias, tìm finding bị bỏ sót.

| Agent | Lệnh chạy | Model | Chế độ |
|---|---|---|---|
| codex | `codex exec --sandbox read-only` | mặc định cấu hình local (~/.codex) | read-only sandbox, không cần bypass approval |
| agy | `agy --mode plan --model gemini-3.1-pro-high -p` | Gemini 3.1 Pro (High) | plan mode = read-only |
| claude (session riêng) | `claude -p ... --dangerously-skip-permissions` | mặc định session | full quyền, ràng buộc bằng prompt (không sửa file) |

Lưu ý an toàn: đã chủ động dùng `--sandbox read-only` (codex) và `--mode plan` (agy) thay vì bypass toàn bộ, vì tác vụ chỉ cần đọc/phân tích — không cần quyền ghi. Với `claude`, dùng đúng `--dangerously-skip-permissions` như yêu cầu vì agent cần chạy lệnh Bash (grep/find) để khảo sát mà không có người ngồi approve; đã ràng buộc rõ trong prompt "chỉ đọc, không sửa/xoá/commit" làm rào chắn mềm. Không có agent nào ghi/sửa file trong quá trình chạy — `git status` sạch trước/sau.

## Finding MỚI mà agent nội bộ bỏ sót (đã verify lại bằng cách đọc trực tiếp code, không chỉ tin lời agent ngoài)

1. **Parser chấp nhận ký tự thừa cuối biểu thức** (`"2abc"` → tính ra `2` thay vì lỗi) — codex phát hiện, đã thêm vào `01_fixes.md#F-CALC-8`.
2. **History UI off-by-one** (giới hạn 100 thực tế chỉ giữ 99, lệch với tầng lưu trữ) — codex phát hiện, đã thêm `01_fixes.md#F-UI-8`.
3. **`countDownTimer` của VipActivity không cancel ở `onPause()`** (chỉ pulse/shimmer animator được cancel) — agy phát hiện, đã **verify lại bằng cách đọc trực tiếp `VipActivity.kt:117-136`** → xác nhận đúng, đã thêm `01_fixes.md#F-AD-8`. Đây là finding hay nhất từ agent ngoài: bug thật, mọi agent nội bộ (kể cả audit Ad/VIP chuyên trách) đều bỏ sót vì tin vào comment code + `doc/memory_leak.md` nói "OK" mà không tự đọc lại từng nhánh lifecycle.

## Điểm BẤT ĐỒNG mức độ nghiêm trọng — cần quyết định

**VIP secret key chỉ Base64, không mã hoá thật (`VipKeys.kt`, `AdKeys.kt`, `app/build.gradle:49`).**
- Audit nội bộ (agent Ad/VIP chuyên trách) xếp mức **P2/M — "cân nhắc hardening nếu cần chống bypass thực sự"**, coi đây là rủi ro lý thuyết vì mức che giấu "đã thống nhất" trước đó (theo comment trong code).
- **Codex (độc lập) xếp P1/L**, lập luận: single-secret design + chỉ Base64 nghĩa là BẤT KỲ ai decompile APK release đều lấy được `VIP_SECRET` plaintext ngay lập tức (không cần crack gì) và tự kích hoạt VIP vô hạn hoặc phát tán key — với app đã lên Play Store thật (không phải nội bộ), đây gần như chắc chắn bị khai thác chứ không phải rủi ro "nếu".
- Cả 2 agent ngoài (codex + agy) đều độc lập chọn "Cloud Sync/lịch sử qua VIP" làm hướng exclusive feature ưu tiên (`X-AD-2`/`X-DATA-2`) — nếu tăng giá trị thương mại của VIP mà không xử lý lỗ hổng key trước, rủi ro bị bypass càng đáng kể hơn.
- **Xem `01_fixes.md#F-AD-9`** để chi tiết đầy đủ. Đây là 1 trong các câu hỏi ưu tiên hoá ở cuối backlog.

## Điểm HỘI TỤ (2-3 agent độc lập cùng đề xuất, tín hiệu ưu tiên cao)

- **"Explain my error"/"Explain this calculation"** — cả audit nội bộ (`X-CALC-1`) và codex (`X-CALC-4`) độc lập đề xuất khai thác hạ tầng phân loại lỗi/AST sẵn có làm điểm khác biệt — tín hiệu mạnh nên làm sớm vì effort thấp, đã có nền tảng.
- **Race condition 3 cờ lỗi toàn cục + coroutine không huỷ job cũ** — audit nội bộ, codex, VÀ agy (gián tiếp qua "debounce") đều độc lập nêu cùng 1 vấn đề gốc (`Calculator.kt:18-20`, `MainActivity.kt` TextWatcher) — 3/3 agent đồng ý đây là bug thật, không phải false-positive.
- **Migrate history sang Room** — nội bộ (`E-DATA-4`), codex, và agy đều độc lập đề xuất — đủ tín hiệu để coi đây là nền tảng cần làm trước khi thêm search/pin/tag (N-DATA-3/4/5).
- **Cloud sync / export history nâng cao gắn với VIP** — nội bộ (`X-AD-2`), codex (search+export), agy (PDF/Excel export, Cloud Sync VIP) — 3/3 đồng ý đây là hướng monetization/retention tốt.
- **AdMob Rewarded test ID chưa thay cho release** — 4/4 nguồn (3 audit nội bộ liên quan + cả codex và agy) đều bắt được — mức độ chắc chắn cao nhất trong toàn bộ backlog, nên coi là P0 không tranh cãi.

## Ý tưởng chỉ 1 agent ngoài đề xuất (chưa hội tụ, nhưng đáng cân nhắc)

- Floating Calculator/PiP overlay (agy) — `03_new_features.md#N-UI-6`.
- OCR Scanner qua ML Kit (agy) — `04_ideas.md`.
- Biến/bộ nhớ M+/M−/MR/MC (codex) — `03_new_features.md#N-CALC-8`.
- Template công thức cá nhân, share biểu thức qua QR/deep-link, undo/redo thao tác phím (codex) — `04_ideas.md`.

## Session `claude -p --dangerously-skip-permissions` — kết quả (đã chạy xong)

Đây là finding giá trị nhất trong toàn bộ quá trình đối chiếu: session này gợi ý "cần check `.gitignore`" cho `gradle.properties`/keystore — tôi (session chính) đã **tự verify lại bằng `git ls-files`, `git log`, `git remote -v`, `gh repo view`** (không chỉ tin lời agent ngoài) và xác nhận: **`app/keystore.jks` + mật khẩu keystore đang lộ công khai trên GitHub public repo `tplloi/OpenCalc` từ 2023 tới nay**. Đây là finding nghiêm trọng nhất toàn bộ audit — nghiêm trọng hơn cả bug lộ VIP secret (F-AD-9) mà cả 3 agent ngoài đều nêu, vì phạm vi ảnh hưởng là toàn bộ khả năng ký giả mạo app, không chỉ bypass 1 tính năng. Xem `01_fixes.md#F-SEC-1` để đầy đủ (đã hỏi user qua AskUserQuestion, user chọn hoãn xử lý git/GitHub, chỉ ghi nhận backlog).

Các finding mới khác từ session này (đã merge vào backlog, đánh dấu "chưa tự verify lại bằng cách chạy thử" nếu chỉ dựa lời agent):
- Percent trong ngoặc tính sai `(10+5)%` → `01_fixes.md#F-CALC-9`.
- Historysize/numberPrecision `!!`/`toInt()` có thể crash thật (không chỉ "fragile" như đánh giá nội bộ ban đầu) → nâng mức ưu tiên các finding liên quan ở `F-UI-6`.
- Không giới hạn độ dài input → `StackOverflowError` từ parser đệ quy → `01_fixes.md#F-UI-9`.
- Backspace `StringIndexOutOfBoundsException` tiềm ẩn → `01_fixes.md#F-UI-10`.
- Rotation mất trạng thái toggle (Inv, scientific mode) không lưu `onSaveInstanceState` → `01_fixes.md#F-UI-11`.
- SDK auto-trial có thể ghi đè VIP vừa redeem trong vài giây đầu sau cài đặt → `01_fixes.md#F-AD-10` (cần verify thêm với vendor SDK).
- `allowBackup=true` không loại trừ VIP state trong Auto Backup → `01_fixes.md#F-INFRA-7`.
- Ý tưởng Bill Splitter, Material You dynamic color, "ad UX tốt sẵn có nên biến thành điểm marketing" → `05_exclusive_features.md`.

## Tổng kết mức độ đồng thuận qua 4 nguồn (3 agent nội bộ chuyên trách + audit tổng + 3 agent ngoài)

| Finding | Nội bộ | codex | agy | claude session | Đồng thuận |
|---|---|---|---|---|---|
| AdMob Rewarded test ID chưa thay cho release | ✅ | ✅ | ✅ | (không nhắc lại nhưng không phủ nhận) | 3-4/4 — chắc chắn nhất |
| Race condition 3 cờ lỗi toàn cục | ✅ | ✅ | ✅ (qua debounce) | ✅ | 4/4 |
| VIP secret chỉ Base64, dễ lộ | ✅ (mức nhẹ) | ✅ (mức nặng, P1/L) | — | ✅ | 3/4, bất đồng mức độ |
| Keystore + password lộ trên git public | — | — | — | ✅ (gợi ý), tôi tự verify xác nhận | 1/4 phát hiện, nhưng mức độ nghiêm trọng cao nhất khi verify xong |
| Migrate history → Room | ✅ | ✅ | ✅ | ✅ | 4/4 |
| `tan(90°)` domain check sai | ✅ | ✅ | — | ✅ | 3/4 |

Kết luận phương pháp: chạy nhiều agent độc lập (kể cả khác vendor — Codex/OpenAI, Gemini, Claude) trên cùng codebase bắt được **7 finding mới** mà 5 agent nội bộ chuyên trách từng subsystem đều bỏ sót, trong đó có 1 finding mức critical thật (keystore leak). Đáng để lặp lại pattern này định kỳ, không chỉ 1 lần.
