package com.mckimquyen.opencal

import com.mckimquyen.opencal.ext.Calculator
import com.mckimquyen.opencal.ext.division_by_0
import com.mckimquyen.opencal.ext.domain_error
import com.mckimquyen.opencal.ext.syntax_error
import com.mckimquyen.opencal.helper.Expression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test (thuần JVM) mô phỏng luồng tính toán thật của app:
 * chuỗi hiển thị (× ÷ √ ! % π …) → [Expression.getCleanExpression] → [Calculator.evaluate].
 */
class ExpressionCalculatorPipelineTest {

    private val exp = Expression()
    private val calc = Calculator()
    private val DELTA = 1e-9

    @Before
    fun reset() {
        division_by_0 = false
        domain_error = false
        syntax_error = false
    }

    private fun compute(display: String, degree: Boolean = true): Double {
        val clean = exp.getCleanExpression(display, ".", ",")
        return calc.evaluate(clean, degree)
    }

    @Test fun timesSymbol() = assertEquals(6.0, compute("2×3"), DELTA)
    @Test fun divideSymbol() = assertEquals(2.5, compute("10÷4"), DELTA)
    @Test fun squareRootSymbol() = assertEquals(3.0, compute("√9"), DELTA)
    @Test fun factorialSymbol() = assertEquals(120.0, compute("5!"), DELTA)
    @Test fun powerSymbol() = assertEquals(9.0, compute("3^2"), DELTA)
    @Test fun mixedSymbols() = assertEquals(14.0, compute("2×(3+4)"), DELTA)
    @Test fun piTimesTwo() = assertEquals(2 * Math.PI, compute("2π"), DELTA)
    @Test fun implicitMultiply() = assertEquals(6.0, compute("2(3)"), DELTA)

    // Phần trăm "thân thiện": 200 + 10% = 220
    @Test fun percentAdded() = assertEquals(220.0, compute("200+10%"), DELTA)

    // Phần trăm đơn lẻ: 50% = 0.5
    @Test fun percentAlone() = assertEquals(0.5, compute("50%"), DELTA)

    // sin(30°) = 0.5 qua toàn bộ pipeline
    @Test fun sinThirtyDegrees() = assertEquals(0.5, compute("sin(30)", degree = true), DELTA)

    // Auto-close ngoặc thiếu: 2×(3+4 = 14
    @Test fun autoCloseParenthesis() = assertEquals(14.0, compute("2×(3+4"), DELTA)

    // Chia 0 qua pipeline
    @Test fun divideByZeroPipeline() {
        val r = compute("5÷0")
        assertTrue(r.isInfinite())
        assertTrue(division_by_0)
    }
}
