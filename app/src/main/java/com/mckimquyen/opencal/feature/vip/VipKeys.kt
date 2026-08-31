package com.mckimquyen.opencal.feature.vip

import android.util.Base64

/**
 * Whitelist VIP key app-side + che giấu key.
 *
 * Các mã được truyền vào `AdSdkConfig.vipRedeemCodes`; SDK kiểm tra code, cộng dồn hạn và ngăn dùng
 * lại cùng một code trên một thiết bị. [lookupDays] chỉ phục vụ nội dung UI sau khi SDK xác nhận.
 *
 * Plain key KHÔNG hardcode trong source — chỉ hardcode chuỗi Base64 (mức che giấu đã thống nhất:
 * dễ reverse nhưng đủ chặn user thường peek decompiled APK).
 *
 * Cố ý KHÔNG dùng chung giá trị với [com.mckimquyen.opencal.common.const.AdKeys.VIP_SECRET]: đó là
 * secret HMAC chống-tamper SharedPreferences nội bộ SDK, còn key ở đây là mã redeem công khai chia
 * sẻ cho user — lộ mã redeem không được kéo theo lộ luôn secret bảo vệ prefs. Mã 30 ngày dưới đây
 * TRÙNG PLAIN TEXT với secret nội bộ bản cũ có chủ đích (đây là mã đã phát hành cho user thật,
 * không đổi được nữa) — an toàn vì `AdKeys.VIP_SECRET`/`BuildConfig.VIP_SECRET_B64` đã được ROTATE
 * sang giá trị khác hẳn, nên 2 giá trị không còn trùng nhau trong app hiện tại.
 */
object VipKeys {

    /** Base64 của plain key 3 ngày. */
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    /** Base64 của plain key 30 ngày (mã đã phát hành cho user — xem ghi chú rotate ở KDoc trên). */
    private const val VIP_30D_B64 = "OWZBMHE3ZU4hMjdjTHgwNEAyMTk5M1kydTBJNyNRMA=="

    val VIP_30D_KEY: String by lazy {
        String(Base64.decode(VIP_30D_B64, Base64.NO_WRAP))
    }

    val VIP_3D_KEY: String by lazy {
        String(Base64.decode(VIP_3D_B64, Base64.NO_WRAP))
    }

    /** Plain key (đã decode) → số ngày. Dùng để validate input từ user. */
    private val keyToDays: Map<String, Int> by lazy {
        mapOf(
            VIP_30D_KEY to 30,
            VIP_3D_KEY to 3,
        )
    }

    /** Trả số ngày nếu key hợp lệ, hoặc null. Auto trim trước khi lookup (case-sensitive). */
    fun lookupDays(rawInput: String): Int? = keyToDays[rawInput.trim()]
}
