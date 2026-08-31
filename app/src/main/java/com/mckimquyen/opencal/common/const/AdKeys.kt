package com.mckimquyen.opencal.common.const

import android.util.Base64
import com.mckimquyen.opencal.BuildConfig

/**
 * Centralize các hằng số nhạy cảm liên quan tới ad/VIP.
 *
 * - AdMob IDs đi qua [BuildConfig] theo build type; debug dùng Google sample IDs, release đọc từ
 *   file `app/ads.properties` bị gitignore.
 * - Section này chỉ giữ VIP secret + Privacy Policy URL.
 *
 * VIP secret được lưu dưới dạng Base64 trong [BuildConfig.VIP_SECRET_B64] (không hardcode
 * plain trong source .kt — xem [com.mckimquyen.opencal.feature.vip.VipKeys] mục che giấu key).
 */
object AdKeys {

    /** Privacy Policy URL — dùng cho VIP screen footer + consent dialog. */
    const val PRIVACY_POLICY_URL: String = BuildConfig.PRIVACY_POLICY_URL

    /**
     * VIP secret dùng để ký chống sửa trạng thái local của SDK. Mã redeem được cấu hình riêng qua
     * `AdSdkConfig.vipRedeemCodes`; không bật lại nhánh plaintext legacy.
     */
    val VIP_SECRET: String by lazy {
        String(Base64.decode(BuildConfig.VIP_SECRET_B64, Base64.NO_WRAP))
    }
}
