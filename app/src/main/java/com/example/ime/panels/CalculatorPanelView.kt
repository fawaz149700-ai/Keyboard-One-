package com.example.ime.panels

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.calculator.CalculatorEngine
import com.example.model.KeyboardThemeSpec
import com.example.model.NumeralStyle

class CalculatorPanelView(
    context: Context,
    private val onCommitResult: (String) -> Unit,
    private val onBackToKeyboard: () -> Unit,
) : LinearLayout(context) {

    private var theme: KeyboardThemeSpec? = null
    private var numeralStyle: NumeralStyle = NumeralStyle.WESTERN
    private var toolbarScale: Float = 1f
    private var expression: String = ""
    private var lastCalculatedResult: String? = null
    private var isErrorState: Boolean = false

    private val displayCard: LinearLayout
    private val backBtn: TextView
    private val exprDisplay: TextView
    private val resultDisplay: TextView
    private val insertEqBtn: TextView
    private val insertResBtn: TextView
    private val gridContainer: LinearLayout
    private val keyButtons = mutableListOf<Pair<CalcKey, TextView>>()

    private enum class CalcKeyType {
        DIGIT,
        OPERATOR,
        CLEAR,
        BACKSPACE,
        EQUALS,
        PAREN,
    }

    private data class CalcKey(
        val id: String,
        val label: String,
        val type: CalcKeyType,
    )

    private val rowsSpec = listOf(
        listOf(
            CalcKey("7", "7", CalcKeyType.DIGIT),
            CalcKey("8", "8", CalcKeyType.DIGIT),
            CalcKey("9", "9", CalcKeyType.DIGIT),
            CalcKey("÷", "÷", CalcKeyType.OPERATOR),
            CalcKey("C", "C", CalcKeyType.CLEAR),
        ),
        listOf(
            CalcKey("4", "4", CalcKeyType.DIGIT),
            CalcKey("5", "5", CalcKeyType.DIGIT),
            CalcKey("6", "6", CalcKeyType.DIGIT),
            CalcKey("×", "×", CalcKeyType.OPERATOR),
            CalcKey("⌫", "⌫", CalcKeyType.BACKSPACE),
        ),
        listOf(
            CalcKey("1", "1", CalcKeyType.DIGIT),
            CalcKey("2", "2", CalcKeyType.DIGIT),
            CalcKey("3", "3", CalcKeyType.DIGIT),
            CalcKey("−", "-", CalcKeyType.OPERATOR),
            CalcKey("%", "%", CalcKeyType.OPERATOR),
        ),
        listOf(
            CalcKey("0", "0", CalcKeyType.DIGIT),
            CalcKey(".", ".", CalcKeyType.DIGIT),
            CalcKey("()", "( )", CalcKeyType.PAREN),
            CalcKey("+", "+", CalcKeyType.OPERATOR),
            CalcKey("=", "=", CalcKeyType.EQUALS),
        ),
    )

    init {
        orientation = VERTICAL
        setPadding(dp(8f), dp(6f), dp(8f), dp(6f))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(248f))

        // Modern Elevated LCD Card containing navigation, expression, live preview & insert pills
        displayCard = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            elevation = dp(3f).toFloat()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            setPadding(dp(8f), dp(5f), dp(8f), dp(5f))
        }
        backBtn = makeHeaderButton("‹ رجوع") { onBackToKeyboard() }

        val displayColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(1f), dp(8f), dp(1f))
        }
        exprDisplay = TextView(context).apply {
            text = "0"
            textSize = 14.5f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTypeface(null, Typeface.BOLD)
        }
        resultDisplay = TextView(context).apply {
            text = ""
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        displayColumn.addView(exprDisplay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        displayColumn.addView(resultDisplay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        insertEqBtn = makeHeaderButton("≡ المعادلة") {
            commitEquation()
        }
        insertResBtn = makeHeaderButton("✓ إدراج") {
            commitResultOnly()
        }

        displayCard.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)))
        displayCard.addView(displayColumn, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(4f)
            marginEnd = dp(4f)
        })
        displayCard.addView(insertEqBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)).apply { marginEnd = dp(4f) })
        displayCard.addView(insertResBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)))
        addView(displayCard, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6f) })

        // 5x4 Modern Soft-Shadowed Keypad Grid
        gridContainer = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        rowsSpec.forEach { rowKeys ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            rowKeys.forEach { calcKey ->
                val btn = TextView(context).apply {
                    text = calcKey.label
                    gravity = Gravity.CENTER
                    textSize = 17f
                    elevation = dp(2.5f).toFloat()
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    setTypeface(
                        null,
                        if (calcKey.type == CalcKeyType.DIGIT) Typeface.NORMAL else Typeface.BOLD,
                    )
                    includeFontPadding = false
                    setOnClickListener { handleKeyClick(calcKey) }
                    if (calcKey.type == CalcKeyType.EQUALS) {
                        setOnLongClickListener {
                            evaluateAndFreeze()
                            commitResultOnly()
                            true
                        }
                    }
                }
                keyButtons.add(calcKey to btn)
                val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    val m = dp(3f)
                    setMargins(m, dp(2.5f), m, dp(2.5f))
                }
                row.addView(btn, lp)
            }
            gridContainer.addView(
                row,
                LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f),
            )
        }
        addView(gridContainer, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        refreshDisplay()
    }

    fun applyConfig(
        theme: KeyboardThemeSpec,
        numeralStyle: NumeralStyle,
        heightPx: Int,
        toolbarScale: Float = 1f,
        keyCornerRadiusDp: Float = 12f,
    ) {
        this.theme = theme
        this.numeralStyle = numeralStyle
        this.toolbarScale = toolbarScale.coerceIn(0.70f, 1.45f)

        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())

        displayCard.background = GradientDrawable().apply {
            setColor(theme.suggestionBarBgColor.toInt())
            cornerRadius = dp(16f).toFloat()
            setStroke(dp(0.8f), theme.dividerColor.toInt())
        }

        val pillH = dp(32f * this.toolbarScale.coerceIn(0.8f, 1.25f))
        val pillSp = 11.5f * this.toolbarScale.coerceIn(0.85f, 1.2f)
        listOf(backBtn, insertEqBtn, insertResBtn).forEach { btn ->
            btn.layoutParams = (btn.layoutParams ?: LayoutParams(LayoutParams.WRAP_CONTENT, pillH)).apply { height = pillH }
            btn.textSize = pillSp
        }
        stylePill(backBtn, theme, accent = false)
        stylePill(insertEqBtn, theme, accent = false)
        stylePill(insertResBtn, theme, accent = true)

        exprDisplay.setTextColor(theme.keyTextColor.toInt())
        resultDisplay.setTextColor(theme.accentKeyBackgroundColor.toInt())
        exprDisplay.textSize = 14.5f * this.toolbarScale.coerceIn(0.85f, 1.22f)
        resultDisplay.textSize = 12f * this.toolbarScale.coerceIn(0.85f, 1.2f)

        val effectiveRadiusDp = keyCornerRadiusDp.coerceAtLeast(11f)
        val cornerPx = dp(effectiveRadiusDp.coerceIn(10f, 16f)).toFloat()
        val keySp = (16.5f * this.toolbarScale.coerceIn(0.82f, 1.15f)).coerceIn(13f, 20f)
        keyButtons.forEach { (k, btn) ->
            btn.text = if (k.type == CalcKeyType.DIGIT && k.id in "0".."9") {
                CalculatorEngine.localizeDigits(k.label, numeralStyle)
            } else {
                k.label
            }
            btn.textSize = keySp
            val bgColor = when (k.type) {
                CalcKeyType.EQUALS -> theme.accentKeyBackgroundColor.toInt()
                CalcKeyType.DIGIT -> theme.keyBackgroundColor.toInt()
                CalcKeyType.CLEAR -> 0xFFC93B3B.toInt()
                else -> theme.specialKeyBackgroundColor.toInt()
            }
            val txtColor = when (k.type) {
                CalcKeyType.EQUALS -> theme.accentKeyTextColor.toInt()
                CalcKeyType.CLEAR -> 0xFFFFFFFF.toInt()
                CalcKeyType.DIGIT -> theme.keyTextColor.toInt()
                else -> theme.specialKeyTextColor.toInt()
            }
            btn.setTextColor(txtColor)
            btn.background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = cornerPx
                setStroke(dp(0.7f), theme.dividerColor.toInt())
            }
        }
        refreshDisplay()
    }

    private fun handleKeyClick(key: CalcKey) {
        isErrorState = false
        val token = if (key.label == "-") "-" else key.id
        when (key.type) {
            CalcKeyType.CLEAR -> {
                expression = ""
                lastCalculatedResult = null
            }
            CalcKeyType.BACKSPACE -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
            CalcKeyType.DIGIT -> {
                if (token == ".") {
                    val lastNumber = expression.takeLastWhile { it in '0'..'9' || it in '٠'..'٩' || it == '.' }
                    if ('.' in lastNumber) return
                    if (lastNumber.isEmpty()) expression += "0"
                }
                expression += token
            }
            CalcKeyType.OPERATOR -> {
                val op = if (token == "−") "-" else token
                val ops = setOf('+', '-', '×', '÷')
                if (expression.isEmpty()) {
                    if (op == "-") {
                        expression = "-"
                    }
                } else if (op == "%") {
                    val last = expression.last()
                    if (last in '0'..'9' || last == ')') {
                        expression += "%"
                    }
                } else {
                    val last = expression.last()
                    if (last in ops) {
                        expression = expression.dropLast(1) + op
                    } else if (last != '(') {
                        expression += op
                    }
                }
            }
            CalcKeyType.PAREN -> {
                val openCount = expression.count { it == '(' }
                val closeCount = expression.count { it == ')' }
                val last = expression.lastOrNull()
                val shouldClose = openCount > closeCount &&
                    last != null &&
                    (last in '0'..'9' || last == ')' || last == '%')
                expression += if (shouldClose) ")" else "("
            }
            CalcKeyType.EQUALS -> {
                evaluateAndFreeze()
                return
            }
        }
        refreshDisplay()
    }

    private fun evaluateAndFreeze() {
        if (expression.isBlank()) return
        val res = CalculatorEngine.evaluateFormatted(expression, numeralStyle)
        if (res != null) {
            lastCalculatedResult = res
            expression = CalculatorEngine.normalizeExpression(res)
            isErrorState = false
        } else {
            isErrorState = true
        }
        refreshDisplay()
    }

    private fun commitResultOnly() {
        val textToInsert = when {
            expression.isBlank() -> CalculatorEngine.localizeDigits("0", numeralStyle)
            else -> CalculatorEngine.evaluateFormatted(expression, numeralStyle)
                ?: lastCalculatedResult
                ?: CalculatorEngine.localizeDigits(expression, numeralStyle)
        }
        onCommitResult(textToInsert)
    }

    private fun commitEquation() {
        if (expression.isBlank()) return
        val evaluated = CalculatorEngine.evaluateFormatted(expression, numeralStyle)
        val localizedExpr = CalculatorEngine.localizeDigits(expression, numeralStyle)
        val out = if (evaluated != null && evaluated != localizedExpr) {
            "$localizedExpr = $evaluated"
        } else {
            localizedExpr
        }
        onCommitResult(out)
    }

    private fun refreshDisplay() {
        val shownExpr = if (expression.isEmpty()) {
            CalculatorEngine.localizeDigits("0", numeralStyle)
        } else {
            CalculatorEngine.localizeDigits(expression, numeralStyle)
        }
        exprDisplay.text = shownExpr

        val live = if (expression.isNotEmpty()) {
            CalculatorEngine.evaluateFormatted(expression, numeralStyle)
        } else {
            null
        }
        lastCalculatedResult = live ?: lastCalculatedResult
        resultDisplay.text = when {
            isErrorState -> "⚠ خطأ حسابي"
            live != null && live != shownExpr -> "= $live"
            else -> ""
        }
    }

    private fun makeHeaderButton(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            includeFontPadding = false
            elevation = dp(2f).toFloat()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { onClick() }
        }

    private fun stylePill(tv: TextView, t: KeyboardThemeSpec, accent: Boolean) {
        tv.setTextColor(if (accent) t.accentKeyTextColor.toInt() else t.specialKeyTextColor.toInt())
        tv.background = GradientDrawable().apply {
            setColor(if (accent) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
            cornerRadius = dp(18f).toFloat()
            if (!accent) setStroke(dp(0.6f), t.dividerColor.toInt())
        }
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
