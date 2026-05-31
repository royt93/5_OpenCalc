package com.mckimquyen.opencal.feature.vip

import java.util.concurrent.TimeUnit

/**
 * Số học thuần cho VIP screen — tách riêng khỏi [VipActivity] để **unit-test trên JVM**
 * (không phụ thuộc Android framework) và tránh lặp logic.
 */
internal object VipMath {

    /**
     * Progress elapsed-semantic: rỗng (0) lúc kích hoạt → đầy (100) khi hết hạn.
     * `total ≤ 0` (clock skew / đã expire / granted==expiry) → 100 (đầy = đã hết hạn).
     */
    fun elapsedProgress(grantedAtMs: Long, expiresAtMs: Long, nowMs: Long): Int {
        val total = expiresAtMs - grantedAtMs
        if (total <= 0L) return 100
        val elapsed = nowMs - grantedAtMs
        return ((elapsed.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }

    data class Remaining(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    /** Tách `remainingMs` (clamp ≥ 0) thành ngày/giờ/phút/giây cho countdown. */
    fun remaining(remainingMs: Long): Remaining {
        val totalSec = remainingMs.coerceAtLeast(0L) / 1000L
        return Remaining(
            days = totalSec / 86_400L,
            hours = (totalSec % 86_400L) / 3_600L,
            minutes = (totalSec % 3_600L) / 60L,
            seconds = totalSec % 60L,
        )
    }

    /**
     * Số ngày để activate sao cho expiry MỚI ≥ expiry cũ (gia hạn, không rút ngắn).
     * = `grantDays` + số ngày còn lại (làm tròn lên). Free user (expiry ≤ now) → đúng `grantDays`.
     */
    fun extendedDays(grantDays: Int, currentExpiryMs: Long, nowMs: Long): Int {
        val remainingMs = (currentExpiryMs - nowMs).coerceAtLeast(0L)
        val remainingDays = Math.ceil(remainingMs.toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()
        return grantDays + remainingDays
    }
}
