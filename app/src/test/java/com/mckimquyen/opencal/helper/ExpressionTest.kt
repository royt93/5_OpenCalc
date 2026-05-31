package com.mckimquyen.opencal.helper

import com.mckimquyen.opencal.ext.syntax_error
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [Expression.getCleanExpression] — chuẩn hoá chuỗi hiển thị (ký hiệu
 * thân thiện) thành biểu thức mà [com.mckimquyen.opencal.ext.Calculator] hiểu được.
 */
class ExpressionTest {

    private val exp = Expression()
    private val DOT = "."
    private val COMMA = ","

    @Before
    fun resetFlags() {
        syntax_error = false
    }

    private fun clean(input: String) = exp.getCleanExpression(input, DOT, COMMA)

    // ---------- Thay ký hiệu phép toán ----------
    @Test fun timesBecomesStar() = assertEquals("2*3", clean("2×3"))
    @Test fun divideBecomesSlash() = assertEquals("6/2", clean("6÷2"))

    // ---------- Thay tên hàm ----------
    @Test fun logBecomesLogten() = assertEquals("logten(100)", clean("log(100)"))
    @Test fun expBecomesXp() = assertEquals("xp(2)", clean("exp(2)"))
    @Test fun sinInvBecomesArcsi() = assertEquals("arcsi(0.5)", clean("sin⁻¹(0.5)"))
    @Test fun cosInvBecomesArcco() = assertEquals("arcco(1)", clean("cos⁻¹(1)"))
    @Test fun tanInvBecomesArcta() = assertEquals("arcta(1)", clean("tan⁻¹(1)"))

    // ---------- Nhân ngầm ----------
    @Test fun implicitMultiplyBeforeParenthesis() = assertEquals("2*(3)", clean("2(3)"))
    @Test fun implicitMultiplyBeforePi() = assertEquals("2*π", clean("2π"))

    // ---------- Tự đóng ngoặc thiếu ----------
    @Test fun autoCloseMissingParenthesis() = assertEquals("(2+3)", clean("(2+3"))

    // ---------- Dấu phân tách thập phân theo locale ----------
    @Test fun commaDecimalSeparatorBecomesDot() =
        assertEquals("1.5+2.5", exp.getCleanExpression("1,5+2,5", COMMA, "."))

    // ---------- Bỏ dấu phân nhóm ----------
    @Test fun groupingSeparatorRemoved() =
        assertEquals("1000+2000", exp.getCleanExpression("1,000+2,000", DOT, COMMA))

    // ---------- Căn được bọc ngoặc ----------
    @Test fun sqrtIsWrapped() = assertEquals("sqrt(9)", clean("√9"))

    // ---------- "!" rỗng -> cờ syntax_error ----------
    @Test fun loneFactorialSetsSyntaxError() {
        clean("!")
        assertTrue(syntax_error)
    }
}
