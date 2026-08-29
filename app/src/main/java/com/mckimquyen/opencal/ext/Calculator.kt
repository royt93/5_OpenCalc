package com.mckimquyen.opencal.ext

import com.mckimquyen.opencal.util.Logger
import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

var division_by_0 = false
var domain_error = false
var syntax_error = false

// X-CALC-1: lý do cụ thể của domain_error, để UI hiện giải thích thay vì "Domain error" chung
// chung. Chỉ 2 nguồn domain_error trong parser hiện tại (ln/logten, tan) nên đủ dùng enum nhỏ
// thay vì refactor toàn bộ error signaling sang sealed class (effort lớn hơn nhiều).
enum class DomainErrorReason {
    LOG_NON_POSITIVE,
    TAN_SINGULARITY,
}

var domain_error_reason: DomainErrorReason? = null

class Calculator {
    fun factorial(number: Double): Double {
        return if (number >= 171) {
            Double.POSITIVE_INFINITY
        } else if (number < 0) {
            Double.NaN
        } else {
            val decimalPartOfNumber = number - number.toInt()
            if (decimalPartOfNumber == 0.0) {
                var factorial = BigInteger("1")
                for (i in 1..number.toInt()) {
                    factorial *= i.toBigInteger()
                }
                factorial.toDouble()
            } else gammaLanczos(number + 1)
        }
    }

    private fun gammaLanczos(x: Double): Double {
        // https://rosettacode.org/wiki/Gamma_function
        var xx = x
        val p = doubleArrayOf(
            0.9999999999998099,
            676.5203681218851,
            -1259.1392167224028,
            771.3234287776531,
            -176.6150291621406,
            12.507343278686905,
            -0.13857109526572012,
            9.984369578019572E-6,
            1.5056327351493116e-7
        )
        val g = 7
        if (xx < 0.5) return Math.PI / (sin(Math.PI * xx) * gammaLanczos(1.0 - xx))
        xx--
        var a = p[0]
        val t = xx + g + 0.5
        for (i in 1 until p.size) a += p[i] / (xx + i)
        return sqrt(2.0 * Math.PI) * t.pow(xx + 0.5) * exp(-t) * a
    }

    fun evaluate(
        equation: String,
        isDegreeModeActivated: Boolean
    ): Double {
        Logger.d("\n\n$equation")
        // https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) {
                    Logger.d("Unexpected: " + ch.toChar() + "Expression: " + equation)
                    syntax_error = true
                }
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) {
                        val fractionDenominator = parseFactor()
                        x /= fractionDenominator
                        if (fractionDenominator == 0.0) {
                            division_by_0 = true
                        }
                    } // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus
                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) {
                        Logger.d("Missing ')'")
                        x = Double.NaN
                    }
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val string = equation.substring(startPos, pos)
                    x = if (string.count { it == '.' } > 1) {
                        Double.NaN
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            Double.NaN
                        } else {
                            string.toDouble()
                        }
                    }
                } else if (eat('e'.code)) {
                    x = exp(1.0)
                } else if (eat('π'.code)) {
                    x = Math.PI
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func: String = equation.substring(startPos, pos)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    if (x.isNaN()) syntax_error = true
                    when (func) {
                        "sqrt" -> {
                            x = sqrt(x)
                        }

                        "ln" -> {
                            if (x <= 0.0) {
                                domain_error = true
                                domain_error_reason = DomainErrorReason.LOG_NON_POSITIVE
                            }
                            x = ln(x)
                        }

                        "logten" -> {
                            if (x <= 0.0) {
                                domain_error = true
                                domain_error_reason = DomainErrorReason.LOG_NON_POSITIVE
                            }
                            x = log10(x)
                        }

                        "xp" -> {
                            x = exp(x)
                        }

                        "factorial" -> {
                            x = factorial(x)
                        }

                        "sin" -> {
                            if (isDegreeModeActivated) {
                                x = sin(Math.toRadians(x))
                                // https://stackoverflow.com/questions/29516222/how-to-get-exact-value-of-trigonometric-functions-in-java
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            } else {
                                x = sin(x)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        "cos" -> {
                            if (isDegreeModeActivated) {
                                x = cos(Math.toRadians(x))
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            } else {
                                x = cos(x)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        "tan" -> {
                            val radians = if (isDegreeModeActivated) Math.toRadians(x) else x
                            if (abs(cos(radians)) < 1.0E-12) {
                                // Tangent is defined for R\{(2k+1)π/2, with k ∈ Z}
                                domain_error = true
                                domain_error_reason = DomainErrorReason.TAN_SINGULARITY
                                x = Double.NaN
                            } else {
                                x = tan(radians)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        "arcsi" -> {
                            if (isDegreeModeActivated) {
                                x = asin(x) * 180 / Math.PI
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            } else {
                                x = asin(x)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        "arcco" -> {
                            if (isDegreeModeActivated) {
                                x = acos(x) * 180 / Math.PI
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            } else {
                                x = acos(x)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        "arcta" -> {
                            if (isDegreeModeActivated) {
                                x = atan(x) * 180 / Math.PI
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            } else {
                                x = atan(x)
                                if (abs(x) < 1.0E-14) {
                                    x = round(x)
                                }
                            }
                        }

                        else -> x = Double.NaN
                    }
                } else {
                    x = Double.NaN
                }
                if (eat('^'.code)) {
                    x = x.pow(parseFactor())
                    // To fix sqrt(2)^2 = 2
                    val decimal = x.toInt()
                    val fractional = x - decimal
                    if (fractional > 0 && fractional < 1.0E-14) {
                        x = decimal.toDouble()
                    }
                } // exponentiation
                return x
            }
        }.parse()
    }
}
