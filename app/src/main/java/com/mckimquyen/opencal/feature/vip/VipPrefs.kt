package com.mckimquyen.opencal.feature.vip

import android.content.Context

/**
 * Helper SharedPreferences riêng cho VIP screen.
 *
 * Lib chỉ persist `vipByKeyUntil` (expiry), KHÔNG persist `grantedAt`. Để vẽ progress bar
 * elapsed-semantic cần cả 2 mốc → app tự lưu `grantedAt`. Đồng thời lưu cờ
 * `userRedeemedOnce` để phân biệt grace entry (quà cài đặt) vs key do user tự nhập.
 *
 * Khi lib bổ sung `getVipByKeyGrantedAt()` → xoá file này, đọc trực tiếp từ lib.
 */
class VipPrefs(context: Context) {
    private val sp = context.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE)

    fun saveGrantedAtMs(ms: Long) = sp.edit().putLong(KEY_GRANTED_AT, ms).apply()
    fun getGrantedAtMs(): Long = sp.getLong(KEY_GRANTED_AT, 0L)
    fun clearGrantedAtMs() = sp.edit().remove(KEY_GRANTED_AT).apply()

    fun markUserRedeemed() = sp.edit().putBoolean(KEY_USER_REDEEMED, true).apply()
    fun userRedeemedAtLeastOnce(): Boolean = sp.getBoolean(KEY_USER_REDEEMED, false)

    private companion object {
        const val KEY_GRANTED_AT = "granted_at_ms"
        const val KEY_USER_REDEEMED = "user_redeemed_once"
    }
}
