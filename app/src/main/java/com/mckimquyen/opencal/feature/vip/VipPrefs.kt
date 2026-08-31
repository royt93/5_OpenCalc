package com.mckimquyen.opencal.feature.vip

import android.content.Context

/**
 * Chỉ lưu metadata trình bày mà SDK không quản lý: user đã từng tự redeem hay mới nhận auto-trial.
 * Mốc cấp và hết hạn VIP phải đọc từ AdManager để mọi grant path dùng cùng một nguồn sự thật.
 */
class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    fun markUserRedeemed() = sp.edit().putBoolean(KEY_USER_REDEEMED, true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean(KEY_USER_REDEEMED, false)

    private companion object {
        const val KEY_USER_REDEEMED = "user_redeemed_once"
    }
}
