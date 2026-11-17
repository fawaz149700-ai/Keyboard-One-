package com.example.ime.panels

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.translate.TranslationLanguageInfo
import com.example.model.KeyboardThemeSpec

class TranslatePanelView(
    context: Context,
    private val supportedLanguages: List<TranslationLanguageInfo>,
    private val onTranslateField: (source: String, target: String, selectedOnly: Boolean) -> Unit,
    private val onReplaceWithTranslation: (String) -> Unit,
    private val onInsertTranslation: (String) -> Unit,
    private val onOpenOfflinePacksSettings: () -> Unit,
    private val onBackToKeyboard: () -> Unit,
) : LinearLayout(context) {

    private var theme: KeyboardThemeSpec? = null
    private var sourceCode: String = "auto"
    private var targetCode: String = "en"
    private var latestTranslation: String = ""

    private val backBtn: TextView
    private val swapBtn: TextView
    private val packsBtn: TextView
    private val sourceTitle: TextView
    private val targetTitle: TextView
    private val statusAndResultView: TextView
    private val sourceChipsRow: LinearLayout
    private val targetChipsRow: LinearLayout
    private val translateAllBtn: TextView
    private val translateSelBtn: TextView
    private val replaceBtn: TextView
    private val insertBtn: TextView

    init {
        orientation = VERTICAL
        setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(256f))

        // Top sleek pill bar
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        backBtn = makeBtn("‹ رجوع") { onBackToKeyboard() }
        swapBtn = makeBtn("⇄ تبديل اللغة") {
            if (sourceCode != "auto") {
                val tmp = sourceCode
                sourceCode = targetCode
                targetCode = tmp
                refreshChips()
            } else {
                sourceCode = targetCode
                targetCode = if (targetCode == "ar") "en" else "ar"
                refreshChips()
            }
        }
        packsBtn = makeBtn("⬇ الحزم") { onOpenOfflinePacksSettings() }

        topRow.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(33f)).apply { marginEnd = dp(6f) })
        topRow.addView(swapBtn, LayoutParams(0, dp(33f), 1f).apply { marginEnd = dp(6f) })
        topRow.addView(packsBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(33f)))
        addView(topRow)

        // Source selector
        sourceTitle = TextView(context).apply {
            text = "من اللغة:"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(2f), dp(5f), dp(2f), dp(2f))
        }
        addView(sourceTitle)

        val srcScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        sourceChipsRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        sourceChipsRow.addView(makeLangChip("✨ تلقائي", "auto") {
            sourceCode = "auto"
            refreshChips()
        })
        supportedLanguages.forEach { lang ->
            sourceChipsRow.addView(makeLangChip(lang.arabicName, lang.code) {
                sourceCode = lang.code
                refreshChips()
            })
        }
        srcScroll.addView(sourceChipsRow)
        addView(srcScroll)

        // Target selector
        targetTitle = TextView(context).apply {
            text = "إلى اللغة:"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(2f), dp(5f), dp(2f), dp(2f))
        }
        addView(targetTitle)

        val tgtScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        targetChipsRow = LinearLayout(context).apply { orientation = HORIZONTAL }
        supportedLanguages.forEach { lang ->
            targetChipsRow.addView(makeLangChip(lang.arabicName, lang.code) {
                targetCode = lang.code
                refreshChips()
            })
        }
        tgtScroll.addView(targetChipsRow)
        addView(tgtScroll)

        // Modern action pills row
        val triggerRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(7f), 0, dp(6f))
        }
        translateAllBtn = makeBtn("🌐 ترجمة كل النص") {
            onTranslateField(sourceCode, targetCode, false)
        }
        translateSelBtn = makeBtn("✂ ترجمة المحدد") {
            onTranslateField(sourceCode, targetCode, true)
        }
        triggerRow.addView(translateAllBtn, LayoutParams(0, dp(36f), 1f).apply { marginEnd = dp(6f) })
        triggerRow.addView(translateSelBtn, LayoutParams(0, dp(36f), 1f))
        addView(triggerRow)

        // Modern Result Card with Soft Shadow
        statusAndResultView = TextView(context).apply {
            text = "اضغط على زر الترجمة لمعاينة النتيجة وإدراجها فوراً"
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
            elevation = dp(3f).toFloat()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
            maxLines = 3
        }
        addView(statusAndResultView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        // Commit buttons row
        val commitRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, dp(6f), 0, 0)
        }
        replaceBtn = makeBtn("✓ استبدال النص") {
            if (latestTranslation.isNotBlank()) onReplaceWithTranslation(latestTranslation)
        }
        insertBtn = makeBtn("↵ إدراج الترجمة") {
            if (latestTranslation.isNotBlank()) onInsertTranslation(latestTranslation)
        }
        commitRow.addView(replaceBtn, LayoutParams(0, dp(36f), 1f).apply { marginEnd = dp(6f) })
        commitRow.addView(insertBtn, LayoutParams(0, dp(36f), 1f))
        addView(commitRow)
    }

    fun applyConfig(
        theme: KeyboardThemeSpec,
        source: String,
        target: String,
        heightPx: Int,
        toolbarScale: Float = 1f,
    ) {
        this.theme = theme
        this.sourceCode = source
        this.targetCode = target
        val s = toolbarScale.coerceIn(0.75f, 1.35f)
        val topH = dp(33f * s)
        listOf(backBtn, swapBtn, packsBtn).forEach { tv ->
            tv.textSize = 12f * s.coerceIn(0.85f, 1.2f)
            tv.layoutParams = (tv.layoutParams as LayoutParams).apply { height = topH }
        }
        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())
        val labelMuted = (theme.keyTextColor.toInt() and 0x00FFFFFF) or 0xCC000000.toInt()
        sourceTitle.setTextColor(labelMuted)
        targetTitle.setTextColor(labelMuted)
        statusAndResultView.setTextColor(theme.keyTextColor.toInt())
        statusAndResultView.background = GradientDrawable().apply {
            setColor(theme.keyBackgroundColor.toInt())
            cornerRadius = dp(16f).toFloat()
            setStroke(dp(0.8f), theme.dividerColor.toInt())
        }
        styleAllButtons(theme)
        refreshChips()
    }

    fun setTranslatingState() {
        statusAndResultView.text = "⏳ جاري الترجمة..."
    }

    fun showTranslationResult(translated: String, detectedSource: String, offline: Boolean) {
        latestTranslation = translated
        val modeTag = if (offline) "بدون إنترنت" else "سحابي"
        statusAndResultView.text = "🌐 $detectedSource → $targetCode • $modeTag\n$translated"
    }

    fun showError(message: String) {
        statusAndResultView.text = "⚠ $message"
    }

    private fun styleAllButtons(t: KeyboardThemeSpec) {
        listOf(backBtn, swapBtn, packsBtn, translateAllBtn, translateSelBtn, insertBtn).forEach { v ->
            v.setTextColor(t.specialKeyTextColor.toInt())
            v.background = GradientDrawable().apply {
                setColor(t.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(18f).toFloat()
                setStroke(dp(0.6f), t.dividerColor.toInt())
            }
        }
        replaceBtn.setTextColor(t.accentKeyTextColor.toInt())
        replaceBtn.background = GradientDrawable().apply {
            setColor(t.accentKeyBackgroundColor.toInt())
            cornerRadius = dp(18f).toFloat()
        }
    }

    private fun refreshChips() {
        val t = theme ?: return
        for (i in 0 until sourceChipsRow.childCount) {
            val tv = sourceChipsRow.getChildAt(i) as? TextView ?: continue
            val active = tv.tag == sourceCode
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.keyTextColor.toInt())
            tv.elevation = dp(if (active) 2.5f else 1f).toFloat()
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.keyBackgroundColor.toInt())
                cornerRadius = dp(18f).toFloat()
                if (!active) setStroke(dp(0.6f), t.dividerColor.toInt())
            }
        }
        for (i in 0 until targetChipsRow.childCount) {
            val tv = targetChipsRow.getChildAt(i) as? TextView ?: continue
            val active = tv.tag == targetCode
            tv.setTextColor(if (active) t.accentKeyTextColor.toInt() else t.keyTextColor.toInt())
            tv.elevation = dp(if (active) 2.5f else 1f).toFloat()
            tv.background = GradientDrawable().apply {
                setColor(if (active) t.accentKeyBackgroundColor.toInt() else t.keyBackgroundColor.toInt())
                cornerRadius = dp(18f).toFloat()
                if (!active) setStroke(dp(0.6f), t.dividerColor.toInt())
            }
        }
    }

    private fun makeLangChip(label: String, code: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            tag = code
            textSize = 12f
            gravity = Gravity.CENTER
            includeFontPadding = false
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding(dp(11f), dp(4f), dp(11f), dp(4f))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(30f)).apply { marginEnd = dp(5f) }
            setOnClickListener { onClick() }
        }

    private fun makeBtn(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            includeFontPadding = false
            elevation = dp(2f).toFloat()
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { onClick() }
        }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
