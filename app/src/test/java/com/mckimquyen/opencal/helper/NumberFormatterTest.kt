package com.mckimquyen.opencal.helper

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit test cho [NumberFormatter].
 *
 * Lưu ý: [NumberFormatter] dùng `DecimalFormat()` (theo Locale mặc định của JVM) để thêm
 * dấu phân nhóm. Ta cố định Locale.US để output xác định (phân nhóm = ',', thập phân = '.').
 */
class NumberFormatterTest {

    private val DOT = "."
    private val COMMA = ","

    @Before
    fun forceLocale() {
        Locale.setDefault(Locale.US)
    }

    private fun fmt(text: String) = NumberFormatter.format(text, DOT, COMMA)

    // ---------- Thêm dấu phân nhóm ----------
    @Test fun thousand() = assertEquals("1,234", fmt("1234"))
    @Test fun million() = assertEquals("1,234,567", fmt("1234567"))
    @Test fun underThousandUnchanged() = assertEquals("345", fmt("345"))

    // ---------- Số thập phân ----------
    @Test fun decimalKeepsFraction() = assertEquals("1,234.56", fmt("1234.56"))
    @Test fun leadingZeroDecimal() = assertEquals("0.5", fmt("0.5"))

    // ---------- Trong biểu thức ----------
    @Test fun groupingInsideExpression() = assertEquals("1,000+2,000", fmt("1000+2000"))
    @Test fun smallNumbersInExpressionUnchanged() = assertEquals("12+345", fmt("12+345"))

    // ---------- Idempotent: format lại không nhân đôi separator ----------
    @Test fun reformatIsStable() = assertEquals("1,234", fmt("1,234"))

    // ---------- extractNumbers ----------
    @Test fun extractIntegers() =
        assertEquals(listOf("12", "34"), NumberFormatter.extractNumbers("12+34", DOT))

    @Test fun extractDecimal() =
        assertEquals(listOf("34.5"), NumberFormatter.extractNumbers("x34.5", DOT))

    @Test fun extractMultipleMixed() =
        assertEquals(listOf("1", "2.5", "3"), NumberFormatter.extractNumbers("1+2.5*3", DOT))
}
