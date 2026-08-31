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
        // Rotated khỏi "9fA0q7eN!27cLx04@21993Y2u0I7#Q0" (giá trị đó giờ là mã redeem 30 ngày công
        // khai trong VipKeys, cố ý tách biệt — xem vip30dKey_isIndependentFromSdkSecret).
        assertEquals("R7@41zN9qP!vX3852cW01997mE6#tY4", AdKeys.VIP_SECRET)
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
