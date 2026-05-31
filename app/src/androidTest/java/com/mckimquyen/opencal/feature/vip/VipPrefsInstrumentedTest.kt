package com.mckimquyen.opencal.feature.vip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test cho [VipPrefs] — SharedPreferences thật.
 */
@RunWith(AndroidJUnit4::class)
class VipPrefsInstrumentedTest {

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private fun wipe() =
        ctx.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    @Before fun setup() { wipe() }
    @After fun teardown() { wipe() }

    @Test fun defaults_areEmpty() {
        val p = VipPrefs(ctx)
        assertEquals(0L, p.getGrantedAtMs())
        assertFalse(p.userRedeemedAtLeastOnce())
    }

    @Test fun grantedAt_roundTrip() {
        val p = VipPrefs(ctx)
        p.saveGrantedAtMs(1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, VipPrefs(ctx).getGrantedAtMs())
    }

    @Test fun clearGrantedAt_resetsToZero() {
        val p = VipPrefs(ctx)
        p.saveGrantedAtMs(123L)
        p.clearGrantedAtMs()
        assertEquals(0L, p.getGrantedAtMs())
    }

    @Test fun markUserRedeemed_persists() {
        val p = VipPrefs(ctx)
        assertFalse(p.userRedeemedAtLeastOnce())
        p.markUserRedeemed()
        assertTrue(VipPrefs(ctx).userRedeemedAtLeastOnce())
    }

    @Test fun clearGrantedAt_doesNotClearRedeemedFlag() {
        val p = VipPrefs(ctx)
        p.markUserRedeemed()
        p.saveGrantedAtMs(5L)
        p.clearGrantedAtMs()
        // revoke chỉ xoá grantedAt, KHÔNG xoá cờ "đã từng redeem"
        assertTrue(p.userRedeemedAtLeastOnce())
    }
}
