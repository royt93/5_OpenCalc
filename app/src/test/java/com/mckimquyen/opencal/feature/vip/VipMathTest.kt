package com.mckimquyen.opencal.feature.vip

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit test (JVM, không Android) cho [VipMath] — số học thuần của VIP screen.
 * Phủ mọi case: progress elapsed-semantic, decomposition countdown, gia hạn không rút ngắn.
 */
class VipMathTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val hour = TimeUnit.HOURS.toMillis(1)

    // ---------- elapsedProgress ----------

    @Test fun progress_atStart_isZero() {
        assertEquals(0, VipMath.elapsedProgress(grantedAtMs = 1_000, expiresAtMs = 1_000 + day, nowMs = 1_000))
    }

    @Test fun progress_atMidpoint_is50() {
        val g = 0L; val e = day
        assertEquals(50, VipMath.elapsedProgress(g, e, day / 2))
    }

    @Test fun progress_atExpiry_is100() {
        assertEquals(100, VipMath.elapsedProgress(0, day, day))
    }

    @Test fun progress_beforeStart_clampedTo0() {
        // now < granted (clock skew) → không âm
        assertEquals(0, VipMath.elapsedProgress(grantedAtMs = day, expiresAtMs = 2 * day, nowMs = 0))
    }

    @Test fun progress_afterExpiry_clampedTo100() {
        assertEquals(100, VipMath.elapsedProgress(0, day, 5 * day))
    }

    @Test fun progress_zeroDuration_is100() {
        // granted == expiry → total <= 0 → đầy (đã hết hạn)
        assertEquals(100, VipMath.elapsedProgress(1_000, 1_000, 1_000))
    }

    @Test fun progress_negativeDuration_is100() {
        assertEquals(100, VipMath.elapsedProgress(expiresAtMs = 0, grantedAtMs = day, nowMs = 0))
    }

    @Test fun progress_quarter_is25() {
        assertEquals(25, VipMath.elapsedProgress(0, 4 * day, day))
    }

    // ---------- remaining ----------

    @Test fun remaining_zero_isAllZero() {
        val r = VipMath.remaining(0)
        assertEquals(0L, r.days); assertEquals(0L, r.hours); assertEquals(0L, r.minutes); assertEquals(0L, r.seconds)
    }

    @Test fun remaining_negative_clampedToZero() {
        val r = VipMath.remaining(-5_000)
        assertEquals(0L, r.days); assertEquals(0L, r.seconds)
    }

    @Test fun remaining_compound_decomposesCorrectly() {
        // 12 ngày 3 giờ 24 phút 15 giây
        val ms = 12 * day + 3 * hour + 24 * 60_000L + 15 * 1_000L
        val r = VipMath.remaining(ms)
        assertEquals(12L, r.days)
        assertEquals(3L, r.hours)
        assertEquals(24L, r.minutes)
        assertEquals(15L, r.seconds)
    }

    @Test fun remaining_exactlyOneDay() {
        val r = VipMath.remaining(day)
        assertEquals(1L, r.days); assertEquals(0L, r.hours); assertEquals(0L, r.minutes); assertEquals(0L, r.seconds)
    }

    @Test fun remaining_subSecond_isZero() {
        val r = VipMath.remaining(999)
        assertEquals(0L, r.seconds)
    }

    @Test fun remaining_59m59s() {
        val r = VipMath.remaining(59 * 60_000L + 59 * 1_000L)
        assertEquals(0L, r.days); assertEquals(0L, r.hours); assertEquals(59L, r.minutes); assertEquals(59L, r.seconds)
    }

    // ---------- extendedDays (gia hạn, không rút ngắn) ----------

    @Test fun extend_freeUser_returnsGrantDaysExactly() {
        // expiry đã qua → remaining 0 → đúng grantDays
        assertEquals(30, VipMath.extendedDays(grantDays = 30, currentExpiryMs = 0, nowMs = day))
        assertEquals(3, VipMath.extendedDays(grantDays = 3, currentExpiryMs = 0, nowMs = day))
    }

    @Test fun extend_activeVip_addsRemainingCeil() {
        // đang VIP còn đúng 30 ngày, +3 (rewarded) → 33 (không bị rút xuống 3)
        val now = 1_000_000L
        assertEquals(33, VipMath.extendedDays(grantDays = 3, currentExpiryMs = now + 30 * day, nowMs = now))
    }

    @Test fun extend_partialDayRemaining_roundsUp() {
        // còn 1.5 ngày → ceil = 2; +3 → 5
        val now = 0L
        val expiry = (1.5 * day).toLong()
        assertEquals(5, VipMath.extendedDays(grantDays = 3, currentExpiryMs = expiry, nowMs = now))
    }

    @Test fun extend_neverShortens() {
        // bất biến: tổng ngày ≥ số ngày còn lại hiện tại
        val now = 5_000_000L
        val remainingDays = 10
        val total = VipMath.extendedDays(grantDays = 3, currentExpiryMs = now + remainingDays * day, nowMs = now)
        org.junit.Assert.assertTrue("tổng ($total) phải ≥ remaining ($remainingDays)", total >= remainingDays)
    }
}
