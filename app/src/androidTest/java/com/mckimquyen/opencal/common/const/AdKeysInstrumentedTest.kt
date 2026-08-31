package com.mckimquyen.opencal.common.const

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mckimquyen.opencal.feature.vip.VipKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test cho [AdKeys] — verify pipeline Base64 (BuildConfig → decode) đúng.
 */
@RunWith(AndroidJUnit4::class)
class AdKeysInstrumentedTest {

    @Test fun vipSecret_decodesToExpectedPlainKey() {
        assertEquals("9fA0q7eN!27cLx04@21993Y2u0I7#Q0", AdKeys.VIP_SECRET)
    }

    @Test fun vip30dKey_isIndependentFromSdkSecret() {
        // Mã redeem công khai KHÔNG được trùng secret HMAC chống-tamper prefs nội bộ SDK — lộ mã
        // redeem không được kéo theo lộ secret bảo vệ prefs.
        assertNotEquals(AdKeys.VIP_SECRET, VipKeys.VIP_30D_KEY)
    }

    @Test fun privacyPolicyUrl_isHttps() {
        assertTrue(AdKeys.PRIVACY_POLICY_URL.startsWith("https://"))
    }
}
