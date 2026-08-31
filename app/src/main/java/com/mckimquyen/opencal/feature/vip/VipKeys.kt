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
 * sẻ cho user — lộ mã redeem không được kéo theo lộ luôn secret bảo vệ prefs.
 */
object VipKeys {

    /** Base64 của plain key 3 ngày. */
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    /** Base64 của plain key 30 ngày — độc lập với [com.mckimquyen.opencal.common.const.AdKeys.VIP_SECRET]. */
    private const val VIP_30D_B64 = "SzlAMDRMcU43ZSEzMGRZMjE5OXhJMyN6TTZ2QjE="

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
