package com.mckimquyen.opencal.feature.vip

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test cho [VipKeys] — cần `android.util.Base64` thật (không có ở JVM unit test).
 * Phủ mọi case của [VipKeys.lookupDays]: hợp lệ, trim, rỗng, sai, case-sensitive.
 */
@RunWith(AndroidJUnit4::class)
class VipKeysInstrumentedTest {

    @Test fun lookup_30dKey_returns30() {
        assertEquals(30, VipKeys.lookupDays(VipKeys.VIP_30D_KEY))
    }

    @Test fun lookup_3dKey_returns3() {
        assertEquals(3, VipKeys.lookupDays(VipKeys.VIP_3D_KEY))
    }

    @Test fun lookup_trimsWhitespace() {
        assertEquals(30, VipKeys.lookupDays("  " + VipKeys.VIP_30D_KEY + "  "))
        assertEquals(3, VipKeys.lookupDays("\t" + VipKeys.VIP_3D_KEY + "\n"))
    }

    @Test fun lookup_empty_returnsNull() {
        assertNull(VipKeys.lookupDays(""))
        assertNull(VipKeys.lookupDays("   "))
    }

    @Test fun lookup_invalid_returnsNull() {
        assertNull(VipKeys.lookupDays("NOT-A-VALID-KEY"))
        assertNull(VipKeys.lookupDays(VipKeys.VIP_30D_KEY + "X"))
    }

    @Test fun lookup_isCaseSensitive() {
        // Key chứa cả hoa lẫn thường → đổi case sẽ không khớp
        assertNull(VipKeys.lookupDays(VipKeys.VIP_30D_KEY.uppercase()))
        assertNull(VipKeys.lookupDays(VipKeys.VIP_30D_KEY.lowercase()))
    }

    @Test fun keys_areDistinct() {
        assertNotEquals(VipKeys.VIP_30D_KEY, VipKeys.VIP_3D_KEY)
    }
}
