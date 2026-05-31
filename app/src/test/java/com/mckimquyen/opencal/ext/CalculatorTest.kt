package com.mckimquyen.opencal.ext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [Calculator.evaluate] — bộ parser recursive-descent.
 *
 * Lưu ý: evaluate() nhận biểu thức ĐÃ được [com.mckimquyen.opencal.helper.Expression]
 * làm sạch (×→*, ÷→/, √→sqrt(...), sin⁻¹→arcsi, log→logten, exp→xp, v.v.).
 * Vì vậy ở đây ta truyền trực tiếp chuỗi "đã sạch".
 *
 * Các cờ lỗi toàn cục (division_by_0, domain_error, syntax_error) được reset trước mỗi test.
 */
class CalculatorTest {

    private val calc = Calculator()
    private val DELTA = 1e-9

    @Before
    fun resetFlags() {
        division_by_0 = false
        domain_error = false
        syntax_error = false
    }

    private fun eval(expr: String, degree: Boolean = true): Double =
        calc.evaluate(expr, degree)

    // ---------- Số học cơ bản ----------
    @Test fun addition() = assertEquals(5.0, eval("2+3"), DELTA)
    @Test fun subtraction() = assertEquals(6.0, eval("10-4"), DELTA)
    @Test fun multiplication() = assertEquals(42.0, eval("6*7"), DELTA)
    @Test fun division() = assertEquals(2.5, eval("10/4"), DELTA)
    @Test fun decimalAddition() = assertEquals(3.0, eval("1.5+1.5"), DELTA)
    @Test fun negativeResult() = assertEquals(-2.0, eval("3-5"), DELTA)

    // ---------- Ưu tiên & ngoặc ----------
    @Test fun precedenceMulOverAdd() = assertEquals(14.0, eval("2+3*4"), DELTA)
    @Test fun parenthesesOverridePrecedence() = assertEquals(20.0, eval("(2+3)*4"), DELTA)
    @Test fun nestedParentheses() = assertEquals(14.0, eval("2*((1+2)+4)"), DELTA)

    // ---------- Dấu một ngôi ----------
    @Test fun unaryMinus() = assertEquals(-3.0, eval("-5+2"), DELTA)
    @Test fun unaryPlus() = assertEquals(5.0, eval("+5"), DELTA)

    // ---------- Lũy thừa ----------
    @Test fun power() = assertEquals(1024.0, eval("2^10"), DELTA)
    @Test fun powerOfSqrtIsExact() = assertEquals(2.0, eval("sqrt(2)^2"), DELTA)

    // ---------- Căn ----------
    @Test fun sqrtPositive() = assertEquals(3.0, eval("sqrt(9)"), DELTA)
    @Test fun sqrtNegativeIsNaN() = assertTrue(eval("sqrt(-1)").isNaN())

    // ---------- Chia cho 0 ----------
    @Test fun divisionByZeroSetsFlagAndInfinite() {
        val r = eval("5/0")
        assertTrue(r.isInfinite())
        assertTrue(division_by_0)
    }

    // ---------- Giai thừa ----------
    @Test fun factorialBasic() = assertEquals(120.0, eval("factorial(5)"), DELTA)
    @Test fun factorialZeroIsOne() = assertEquals(1.0, eval("factorial(0)"), DELTA)
    @Test fun factorialNegativeIsNaN() = assertTrue(calc.factorial(-1.0).isNaN())
    @Test fun factorialOverflowIsInfinite() = assertTrue(calc.factorial(171.0).isInfinite())
    @Test fun factorialNonIntegerUsesGamma() =
        // 0.5! = Γ(1.5) = √π / 2 ≈ 0.8862269
        assertEquals(0.8862269, calc.factorial(0.5), 1e-6)

    // ---------- Lượng giác (độ) ----------
    @Test fun sinDegrees() = assertEquals(0.5, eval("sin(30)", degree = true), 1e-9)
    @Test fun cosDegrees() = assertEquals(1.0, eval("cos(0)", degree = true), DELTA)
    @Test fun tanDegrees() = assertEquals(1.0, eval("tan(45)", degree = true), 1e-9)

    // ---------- Lượng giác (radian) ----------
    @Test fun sinRadiansZero() = assertEquals(0.0, eval("sin(0)", degree = false), DELTA)

    // ---------- Hàm ngược (độ) ----------
    @Test fun arcsinDegrees() = assertEquals(30.0, eval("arcsi(0.5)", degree = true), 1e-9)
    @Test fun arccosDegrees() = assertEquals(0.0, eval("arcco(1)", degree = true), 1e-9)
    @Test fun arctanDegrees() = assertEquals(45.0, eval("arcta(1)", degree = true), 1e-9)

    // ---------- Log / ln / exp ----------
    @Test fun naturalLogOfOne() = assertEquals(0.0, eval("ln(1)"), DELTA)
    @Test fun log10Of100() = assertEquals(2.0, eval("logten(100)"), DELTA)
    @Test fun expOfZero() = assertEquals(1.0, eval("xp(0)"), DELTA)

    @Test fun lnOfZeroSetsDomainErrorAndNegInfinite() {
        val r = eval("ln(0)")
        assertTrue(r.isInfinite())
        assertTrue(domain_error)
    }

    // ---------- Hằng số ----------
    @Test fun piConstant() = assertEquals(Math.PI, eval("π"), DELTA)
    @Test fun eConstant() = assertEquals(Math.E, eval("e"), DELTA)

    // ---------- Cú pháp sai ----------
    @Test fun loneDecimalSeparatorIsNaN() = assertTrue(eval(".").isNaN())
    @Test fun emptyExpressionIsNaN() = assertTrue(eval("").isNaN())
    @Test fun doubleDecimalIsNaN() = assertTrue(eval("1.2.3").isNaN())

    // ---------- Không lỗi cho biểu thức hợp lệ ----------
    @Test fun validExpressionHasNoErrorFlags() {
        eval("2+2")
        assertFalse(division_by_0)
        assertFalse(domain_error)
        assertFalse(syntax_error)
    }
}
