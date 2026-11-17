package com.example.data.calculator

import com.example.model.NumeralStyle
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object CalculatorEngine {

    private val EASTERN_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun normalizeExpression(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val easternIdx = EASTERN_DIGITS.indexOf(ch)
            when {
                easternIdx >= 0 -> sb.append(('0'.code + easternIdx).toChar())
                ch == '×' -> sb.append('*')
                ch == '÷' -> sb.append('/')
                ch == '−' || ch == '–' -> sb.append('-')
                ch == '٫' || ch == '،' -> sb.append('.')
                ch == '٪' -> sb.append('%')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun evaluateFormatted(
        rawExpression: String,
        numeralStyle: NumeralStyle = NumeralStyle.WESTERN,
    ): String? {
        val value = evaluate(rawExpression) ?: return null
        val formatted = formatNumber(value)
        return localizeDigits(formatted, numeralStyle)
    }

    fun localizeDigits(text: String, numeralStyle: NumeralStyle): String {
        if (numeralStyle != NumeralStyle.EASTERN_ARABIC) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(EASTERN_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun evaluate(rawExpression: String): Double? {
        val expr = normalizeExpression(rawExpression).replace("\\s+".toRegex(), "")
        if (expr.isEmpty()) return null
        return runCatching {
            val parser = Parser(expr)
            val result = parser.parseExpression()
            if (!parser.isAtEnd()) return null
            if (result.isNaN() || result.isInfinite()) null else result
        }.getOrNull()
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return ""
        val cleanZero = if (value == -0.0) 0.0 else value
        val bd = BigDecimal(cleanZero, MathContext(12, RoundingMode.HALF_UP))
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val str = bd.toPlainString()
        return if (str == "-0") "0" else str
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun isAtEnd(): Boolean = pos >= src.length

        private fun peek(): Char? = if (pos < src.length) src[pos] else null

        private fun consume(): Char = src[pos++]

        fun parseExpression(): Double {
            var left = parseTerm()
            while (pos < src.length) {
                when (peek()) {
                    '+' -> {
                        consume()
                        val right = parseTerm()
                        left += right
                    }
                    '-' -> {
                        consume()
                        val right = parseTerm()
                        left -= right
                    }
                    else -> return left
                }
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parsePostfix()
            while (pos < src.length) {
                when (peek()) {
                    '*' -> {
                        consume()
                        left *= parsePostfix()
                    }
                    '/' -> {
                        consume()
                        val divisor = parsePostfix()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        left /= divisor
                    }
                    '(' -> {
                        // Implicit multiplication: 2(3+4)
                        left *= parsePostfix()
                    }
                    else -> return left
                }
            }
            return left
        }

        private fun parsePostfix(): Double {
            var v = parseUnary()
            while (peek() == '%') {
                consume()
                v /= 100.0
            }
            return v
        }

        private fun parseUnary(): Double {
            return when (peek()) {
                '+' -> {
                    consume()
                    parseUnary()
                }
                '-' -> {
                    consume()
                    -parseUnary()
                }
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            val ch = peek() ?: throw IllegalArgumentException("Unexpected end of expression")
            if (ch == '(') {
                consume()
                val v = parseExpression()
                if (peek() != ')') throw IllegalArgumentException("Missing closing parenthesis")
                consume()
                return v
            }
            val start = pos
            var seenDigit = false
            var seenDot = false
            while (pos < src.length) {
                val c = src[pos]
                if (c in '0'..'9') {
                    seenDigit = true
                    pos++
                } else if (c == '.' && !seenDot) {
                    seenDot = true
                    pos++
                } else {
                    break
                }
            }
            if (!seenDigit) throw IllegalArgumentException("Expected number at $start")
            return src.substring(start, pos).toDouble()
        }
    }
}
