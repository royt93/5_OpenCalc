package com.mckimquyen.opencal.feature.vip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
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
        assertFalse(p.userRedeemedAtLeastOnce())
    }

    @Test fun markUserRedeemed_persists() {
        val p = VipPrefs(ctx)
        assertFalse(p.userRedeemedAtLeastOnce())
        p.markUserRedeemed()
        assertTrue(VipPrefs(ctx).userRedeemedAtLeastOnce())
    }
}
