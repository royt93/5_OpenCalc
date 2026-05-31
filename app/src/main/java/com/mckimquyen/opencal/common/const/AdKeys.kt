package com.mckimquyen.opencal.common.const

import android.util.Base64
import com.mckimquyen.opencal.BuildConfig

/**
 * Centralize các hằng số nhạy cảm liên quan tới ad/VIP.
 *
 * - Ad ID + AppLovin SDK key đã đi qua [BuildConfig] (set per buildType trong build.gradle),
 *   đọc trực tiếp `BuildConfig.APPLOVIN_BANNER_ID`, ... nên không lặp lại ở đây.
 * - Section này chỉ giữ VIP secret + Privacy Policy URL.
 *
 * VIP secret được lưu dưới dạng Base64 trong [BuildConfig.VIP_SECRET_B64] (không hardcode
 * plain trong source .kt — xem [com.mckimquyen.opencal.feature.vip.VipKeys] mục che giấu key).
 */
object AdKeys {

    /** Privacy Policy URL — dùng cho VIP screen footer + consent dialog. */
    const val PRIVACY_POLICY_URL: String = BuildConfig.PRIVACY_POLICY_URL

    /**
     * VIP secret dùng chung cho [com.roy.sdkadbmob.AdSdkConfig.vipKeySecret] (single-secret
     * design của lib). Plain value = key 30 ngày. Mọi lần activate VIP đều gọi
     * `AdManager.activateVipByKey(ctx, VIP_SECRET, days)` với số ngày do whitelist app-side
     * quyết định — key user gõ không cần bằng secret này.
     */
    val VIP_SECRET: String by lazy {
        String(Base64.decode(BuildConfig.VIP_SECRET_B64, Base64.NO_WRAP))
    }
}
