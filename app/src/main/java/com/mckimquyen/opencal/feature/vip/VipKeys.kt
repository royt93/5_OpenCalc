package com.mckimquyen.opencal.feature.vip

import android.util.Base64
import com.mckimquyen.opencal.common.const.AdKeys

/**
 * Whitelist VIP key app-side + che giấu key.
 *
 * Các mã được truyền vào `AdSdkConfig.vipRedeemCodes`; SDK kiểm tra code, cộng dồn hạn và ngăn dùng
 * lại cùng một code trên một thiết bị. [lookupDays] chỉ phục vụ nội dung UI sau khi SDK xác nhận.
 *
 * Plain key KHÔNG hardcode trong source — chỉ hardcode chuỗi Base64 (mức che giấu đã thống nhất:
 * dễ reverse nhưng đủ chặn user thường peek decompiled APK).
 */
object VipKeys {

    /** Base64 của plain key 3 ngày. (Key 30 ngày = [AdKeys.VIP_SECRET], không lặp lại ở đây.) */
    private const val VIP_3D_B64 = "ZVE3QDkzTDBmITJZMjcwN3hOMDQwMjE5OTN1MEkjMmFL"

    /** Key 30 ngày trùng với VIP secret của SDK. */
    val VIP_30D_KEY: String get() = AdKeys.VIP_SECRET

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
