package com.mckimquyen.opencal.feature.vip

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.R
import com.roy.sdkadbmob.AdManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented/UI test cho [VipActivity].
 *
 * Theo convention project: KHÔNG dùng Espresso `onView/perform` (crash Android 15/16) — điều khiển
 * qua [ActivityScenario.onActivity] trên main thread (`performClick`, set/đọc view trực tiếp).
 * Mọi action ([VipActivity] redeem/grant) chạy đồng bộ nên có thể assert ngay sau click.
 *
 * State VIP được clear trước MỖI test để deterministic. KHÔNG phụ thuộc mạng/ad thật.
 */
@RunWith(AndroidJUnit4::class)
class VipActivityInstrumentedTest {

    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    private fun clearVipState() {
        AdManager.clearVipByKey()
        ctx.getSharedPreferences("vip_screen_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Before fun setup() { clearVipState() }
    @After fun teardown() { clearVipState() }

    @Test fun freeState_showsFreeUi() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                assertEquals(
                    act.getString(R.string.vip_free_user),
                    act.findViewById<TextView>(R.id.tvStatusTitle).text.toString()
                )
                assertEquals(View.VISIBLE, act.findViewById<View>(R.id.btnWatchAd).visibility)
                assertEquals(View.GONE, act.findViewById<View>(R.id.cardProgress).visibility)
                assertEquals(View.GONE, act.findViewById<View>(R.id.cardActiveVip).visibility)
            }
        }
    }

    @Test fun buyAndRestoreButtons_areDisabled() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                assertFalse(act.findViewById<View>(R.id.btnBuy30d).isEnabled)
                assertFalse(act.findViewById<View>(R.id.btnBuy90d).isEnabled)
                assertFalse(act.findViewById<View>(R.id.btnBuy1y).isEnabled)
                assertFalse(act.findViewById<View>(R.id.btnBuyLifetime).isEnabled)
                assertFalse(act.findViewById<View>(R.id.btnRestore).isEnabled)
            }
        }
    }

    @Test fun invalidKey_doesNotActivate() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.findViewById<EditText>(R.id.etKey).setText("WRONG-KEY-123")
                act.findViewById<View>(R.id.btnActivate).performClick()
                assertFalse(AdManager.isVipByKeyActive())
            }
        }
    }

    @Test fun validKey_activatesAndShowsActiveUi() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.findViewById<EditText>(R.id.etKey).setText(VipKeys.VIP_3D_KEY)
                act.findViewById<View>(R.id.btnActivate).performClick()

                assertTrue(AdManager.isVipByKeyActive())
                assertTrue(AdManager.getVipByKeyExpiry() > System.currentTimeMillis())
                assertEquals(View.VISIBLE, act.findViewById<View>(R.id.cardActiveVip).visibility)
                assertEquals(View.VISIBLE, act.findViewById<View>(R.id.cardProgress).visibility)
                // Đang VIP → nút "Xem QC" ẩn (chống re-grant/rớt hạng)
                assertEquals(View.GONE, act.findViewById<View>(R.id.btnWatchAd).visibility)
            }
        }
    }

    @Test fun preActivatedVip_rendersActiveOnLaunch() {
        AdManager.grantVipDays(ctx, 3)
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                assertEquals(
                    act.getString(R.string.vip_active),
                    act.findViewById<TextView>(R.id.tvStatusTitle).text.toString()
                )
                assertEquals(View.VISIBLE, act.findViewById<View>(R.id.cardActiveVip).visibility)
                assertEquals(View.GONE, act.findViewById<View>(R.id.btnWatchAd).visibility)
                assertTrue(act.findViewById<View>(R.id.btnRevokeAll).isEnabled)
            }
        }
    }

    @Test fun activateButton_disabledWhenKeyEmpty() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                assertFalse(act.findViewById<View>(R.id.btnActivate).isEnabled)
            }
        }
    }

    @Test fun activateButton_enabledAfterTypingKey() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.findViewById<EditText>(R.id.etKey).setText("ABC")
                assertTrue(act.findViewById<View>(R.id.btnActivate).isEnabled)
                act.findViewById<EditText>(R.id.etKey).setText("")
                assertFalse(act.findViewById<View>(R.id.btnActivate).isEnabled)
            }
        }
    }

    @Test fun backButton_finishesActivity() {
        ActivityScenario.launch(VipActivity::class.java).use { scenario ->
            scenario.onActivity { act ->
                act.findViewById<View>(R.id.ivBack).performClick()
                assertTrue(act.isFinishing)
            }
        }
    }
}
