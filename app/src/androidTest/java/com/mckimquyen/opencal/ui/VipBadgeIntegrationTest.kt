package com.mckimquyen.opencal.ui

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.Chip
import com.mckimquyen.opencal.R
import com.mckimquyen.opencal.common.const.AdKeys
import com.roy.sdkadbmob.AdManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test: chip "VIP" ở action bar [MainActivity] LUÔN hiển thị và đổi style theo
 * trạng thái VIP qua `bindVipBadge()`:
 *  - Free → chip viền (chipStrokeWidth > 0)
 *  - VIP  → chip vàng đặc (chipStrokeWidth == 0)
 */
@RunWith(AndroidJUnit4::class)
class VipBadgeIntegrationTest {

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private fun clearVip() {
        AdManager.clearVipByKey()
        ctx.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Before fun setup() { clearVip() }
    @After fun teardown() { clearVip() }

    @Test fun badge_alwaysVisible_outlinedWhenFree() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val chip = act.findViewById<Chip>(R.id.chipVipBadge)
                assertEquals(View.VISIBLE, chip.visibility)
                assertTrue("free → chip viền (stroke>0)", chip.chipStrokeWidth > 0f)
            }
        }
    }

    @Test fun badge_alwaysVisible_filledGoldWhenVip() {
        AdManager.activateVipByKey(ctx, AdKeys.VIP_SECRET, 3)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                val chip = act.findViewById<Chip>(R.id.chipVipBadge)
                assertEquals(View.VISIBLE, chip.visibility)
                assertEquals("VIP → vàng đặc (stroke=0)", 0f, chip.chipStrokeWidth, 0.01f)
            }
        }
    }
}
