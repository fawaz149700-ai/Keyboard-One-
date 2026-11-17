package com.example.ime.panels

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data.emoji.EmojiCatalog
import com.example.model.KeyboardThemeSpec
import kotlin.math.roundToInt

class QuickEmojiEditorPanelView(
    context: Context,
    private val onUpdateEmojis: (List<String>) -> Unit,
    private val onUpdateSizeSp: (Float) -> Unit,
    private val onResetDefaults: () -> Unit,
    private val onBackToKeyboard: () -> Unit,
    private val onRequestCustomTextInput: (Boolean) -> Unit,
) : LinearLayout(context) {

    private var theme: KeyboardThemeSpec? = null
    private var currentEmojis: List<String> = emptyList()
    private var currentSizeSp: Float = 19f
    private var toolbarScale: Float = 1f
    private var selectedSlotIndex: Int? = null
    private var customTextDraft: String = ""
    private var isTypingCustomText: Boolean = false

    private val backBtn: TextView
    private val titleView: TextView
    private val resetBtn: TextView
    private val slotsRow: LinearLayout
    private val customInputBanner: TextView
    private val customBackspaceBtn: TextView
    private val customAddBtn: TextView
    private val quickTextChipsRow: LinearLayout
    private val sizeLabel: TextView
    private val sizeSeekBar: SeekBar
    private val pickerRecycler: RecyclerView
    private val pickerAdapter = PickerAdapter()

    private val catalogEmojis: List<String> = EmojiCatalog.entriesByCategory.values
        .flatten()
        .map { it.emoji }
        .distinct()

    private val presetCustomTokens = listOf(
        "ا", "ل", "م", "ه", "ي", "♥", "★", "✓", "•", "@", "...", "السلام عليكم", "شكراً", "بإذن الله", "ما شاء الله",
    )

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(256f))

        // Header
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(5f), dp(8f), dp(3f))
        }
        backBtn = makeHeaderButton("‹ رجوع") {
            setCustomTypingMode(false)
            onBackToKeyboard()
        }
        titleView = TextView(context).apply {
            text = "تخصيص الشريط (نسخة إضافية)"
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
        }
        resetBtn = makeHeaderButton("افتراضي") {
            selectedSlotIndex = null
            setCustomTypingMode(false)
            onResetDefaults()
        }
        header.addView(backBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)))
        header.addView(titleView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        header.addView(resetBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)))
        addView(header)

        // 9 Slots preview/delete row
        slotsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        }
        addView(slotsRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(42f)))

        // Custom text/character input bar
        val customRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        }
        customInputBanner = TextView(context).apply {
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener {
                setCustomTypingMode(!isTypingCustomText)
            }
        }
        customBackspaceBtn = makeHeaderButton("⌫") {
            backspaceCustomText()
        }
        customAddBtn = makeHeaderButton("+ إضافة") {
            commitCustomDraft()
        }
        customRow.addView(customInputBanner, LayoutParams(0, dp(32f), 1f).apply { marginEnd = dp(4f) })
        customRow.addView(customBackspaceBtn, LayoutParams(dp(40f), dp(32f)).apply { marginEnd = dp(4f) })
        customRow.addView(customAddBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(32f)))
        addView(customRow)
        updateCustomBanner()

        // Quick preset custom characters / phrases strip
        val presetScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(8f), dp(2f), dp(8f), dp(2f))
        }
        quickTextChipsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        presetCustomTokens.forEach { token ->
            val chip = TextView(context).apply {
                text = "+ $token"
                tag = token
                textSize = 11.5f
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(9f), dp(3f), dp(9f), dp(3f))
                setOnClickListener { handleItemInsertOrSelect(token) }
            }
            quickTextChipsRow.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, dp(28f)).apply { marginEnd = dp(4f) })
        }
        presetScroll.addView(quickTextChipsRow)
        addView(presetScroll)

        // Size Slider Row
        val sizeRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(2f), dp(10f), dp(3f))
        }
        sizeLabel = TextView(context).apply {
            text = "الحجم: 19 sp"
            textSize = 11.5f
        }
        sizeSeekBar = SeekBar(context).apply {
            max = 15 // 13sp .. 28sp
            progress = 6
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val spVal = (13 + progress).toFloat()
                        currentSizeSp = spVal
                        sizeLabel.text = "الحجم: ${spVal.roundToInt()} sp"
                        onUpdateSizeSp(spVal)
                        renderSlots()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        sizeRow.addView(sizeLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8f) })
        sizeRow.addView(sizeSeekBar, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(sizeRow)

        // Emoji Picker Grid
        pickerRecycler = RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 8)
            adapter = pickerAdapter
            setPadding(dp(6f), dp(2f), dp(6f), dp(6f))
            clipToPadding = false
        }
        addView(pickerRecycler, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun updateConfig(
        emojis: List<String>,
        sizeSp: Float,
        theme: KeyboardThemeSpec,
        heightPx: Int,
        toolbarScale: Float = 1f,
    ) {
        this.currentEmojis = emojis.take(9)
        this.currentSizeSp = sizeSp.coerceIn(13f, 28f)
        this.toolbarScale = toolbarScale.coerceIn(0.70f, 1.45f)
        this.theme = theme
        layoutParams = (layoutParams ?: LayoutParams(LayoutParams.MATCH_PARENT, heightPx)).apply {
            height = heightPx
        }
        setBackgroundColor(theme.backgroundColor.toInt())

        sizeLabel.text = "الحجم: ${this.currentSizeSp.roundToInt()} sp"
        sizeLabel.setTextColor(theme.keyTextColor.toInt())
        sizeSeekBar.progress = (this.currentSizeSp.roundToInt() - 13).coerceIn(0, 15)

        val pillH = dp(32f * this.toolbarScale.coerceIn(0.8f, 1.25f))
        val pillSp = (11.5f * this.toolbarScale.coerceIn(0.85f, 1.2f))
        backBtn.layoutParams = (backBtn.layoutParams ?: LayoutParams(LayoutParams.WRAP_CONTENT, pillH)).apply { height = pillH }
        resetBtn.layoutParams = (resetBtn.layoutParams ?: LayoutParams(LayoutParams.WRAP_CONTENT, pillH)).apply { height = pillH }
        backBtn.textSize = pillSp
        resetBtn.textSize = pillSp

        titleView.setTextColor(theme.keyTextColor.toInt())
        stylePill(backBtn, theme, accent = true)
        stylePill(resetBtn, theme, accent = false)
        stylePill(customBackspaceBtn, theme, accent = false)
        stylePill(customAddBtn, theme, accent = true)

        customInputBanner.setTextColor(theme.keyTextColor.toInt())
        customInputBanner.background = GradientDrawable().apply {
            setColor(theme.keyBackgroundColor.toInt())
            cornerRadius = dp(10f).toFloat()
            if (isTypingCustomText) {
                setStroke(dp(1.5f), theme.accentKeyBackgroundColor.toInt())
            } else {
                setStroke(dp(1f), theme.dividerColor.toInt())
            }
        }

        for (i in 0 until quickTextChipsRow.childCount) {
            val tv = quickTextChipsRow.getChildAt(i) as? TextView ?: continue
            val token = tv.tag as? String ?: continue
            val active = token in currentEmojis
            tv.setTextColor(if (active) theme.accentKeyTextColor.toInt() else theme.specialKeyTextColor.toInt())
            tv.background = GradientDrawable().apply {
                setColor(if (active) theme.accentKeyBackgroundColor.toInt() else theme.specialKeyBackgroundColor.toInt())
                cornerRadius = dp(9f).toFloat()
            }
        }

        renderSlots()
        pickerAdapter.notifyDataSetChanged()
    }

    fun appendCustomText(ch: String) {
        if (!isTypingCustomText) return
        customTextDraft += ch
        updateCustomBanner()
    }

    fun backspaceCustomText() {
        if (customTextDraft.isNotEmpty()) {
            val cpCount = customTextDraft.codePointCount(0, customTextDraft.length)
            customTextDraft = if (cpCount <= 1) "" else customTextDraft.substring(0, customTextDraft.offsetByCodePoints(0, cpCount - 1))
            updateCustomBanner()
        }
    }

    private fun commitCustomDraft() {
        val clean = customTextDraft.trim()
        if (clean.isEmpty()) {
            setCustomTypingMode(!isTypingCustomText)
            return
        }
        copyValueIntoQuickBar(clean)
        customTextDraft = ""
        setCustomTypingMode(false)
    }

    /**
     * يضيف نسخة من الحرف أو النص إلى شريط الاختصارات دون حذف الحرف الأصلي من تخطيط الكيبورد.
     */
    private fun copyValueIntoQuickBar(value: String) {
        val list = currentEmojis.toMutableList()
        val slot = selectedSlotIndex
        if (slot != null && slot in 0 until 9) {
            if (slot < list.size) {
                list[slot] = value
            } else if (value !in list) {
                list.add(value)
            }
            selectedSlotIndex = null
            onUpdateEmojis(list.distinct().take(9))
            return
        }
        if (value !in list) {
            if (list.size < 9) {
                list.add(value)
            } else {
                list[list.lastIndex] = value
            }
            onUpdateEmojis(list)
        }
    }

    private fun setCustomTypingMode(enabled: Boolean) {
        isTypingCustomText = enabled
        updateCustomBanner()
        onRequestCustomTextInput(enabled)
        theme?.let { t ->
            customInputBanner.background = GradientDrawable().apply {
                setColor(t.keyBackgroundColor.toInt())
                cornerRadius = dp(10f).toFloat()
                if (enabled) setStroke(dp(1.5f), t.accentKeyBackgroundColor.toInt())
                else setStroke(dp(1f), t.dividerColor.toInt())
            }
        }
    }

    private fun updateCustomBanner() {
        customInputBanner.text = when {
            customTextDraft.isNotEmpty() -> "✍ $customTextDraft"
            isTypingCustomText -> "✍ اكتب النص أو الحرف ثم اضغط (+ إضافة)..."
            else -> "✍ اضغط لإضافة نص أو حرف مخصص..."
        }
    }

    private fun renderSlots() {
        val t = theme ?: return
        slotsRow.removeAllViews()
        for (i in 0 until 9) {
            val em = currentEmojis.getOrNull(i)
            val isSelectedSlot = selectedSlotIndex == i
            val isTextEntry = em != null && isCustomTextEntry(em)
            val fittedSp = when {
                em == null -> 18f
                !isTextEntry -> currentSizeSp.coerceIn(13f, 24f)
                em.length <= 2 -> currentSizeSp.coerceIn(12f, 20f)
                em.length <= 5 -> currentSizeSp.coerceAtMost(12f)
                else -> currentSizeSp.coerceAtMost(9.5f)
            }
            val slot = TextView(context).apply {
                text = em ?: "+"
                textSize = fittedSp
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTypeface(null, if (isTextEntry) Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(2f), dp(1f), dp(2f), dp(1f))
                setTextColor(t.keyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(if (em != null) t.keyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
                    cornerRadius = dp(8f).toFloat()
                    if (isSelectedSlot) {
                        setStroke(dp(2f), t.accentKeyBackgroundColor.toInt())
                    } else if (em == null) {
                        setStroke(dp(1f), t.dividerColor.toInt())
                    }
                }
                setOnClickListener {
                    if (em != null) {
                        val next = currentEmojis.toMutableList()
                        next.removeAt(i)
                        selectedSlotIndex = null
                        onUpdateEmojis(next)
                    } else {
                        selectedSlotIndex = i
                        renderSlots()
                    }
                }
                setOnLongClickListener {
                    selectedSlotIndex = i
                    renderSlots()
                    true
                }
            }
            val lp = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                val m = dp(2f)
                setMargins(m, dp(2f), m, dp(2f))
            }
            slotsRow.addView(slot, lp)
        }
    }

    private fun handleItemInsertOrSelect(value: String) {
        val list = currentEmojis.toMutableList()
        val slot = selectedSlotIndex
        if (slot != null && slot in 0 until 9) {
            if (slot < list.size) {
                list[slot] = value
            } else {
                if (value !in list) list.add(value)
            }
            selectedSlotIndex = null
            onUpdateEmojis(list.distinct().take(9))
            return
        }

        if (value in list) {
            list.remove(value)
        } else if (list.size < 9) {
            list.add(value)
        } else {
            list[list.lastIndex] = value
        }
        onUpdateEmojis(list)
    }

    private fun makeHeaderButton(label: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { onClick() }
        }

    private fun stylePill(tv: TextView, t: KeyboardThemeSpec, accent: Boolean) {
        tv.setTextColor(if (accent) t.accentKeyTextColor.toInt() else t.specialKeyTextColor.toInt())
        tv.background = GradientDrawable().apply {
            setColor(if (accent) t.accentKeyBackgroundColor.toInt() else t.specialKeyBackgroundColor.toInt())
            cornerRadius = dp(9f).toFloat()
        }
    }

    private inner class PickerAdapter : RecyclerView.Adapter<PickerAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                textSize = 21f
                gravity = Gravity.CENTER
                layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, dp(40f)).apply {
                    val m = dp(2f)
                    setMargins(m, m, m, m)
                }
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val em = catalogEmojis[position]
            val t = theme
            val active = em in currentEmojis
            holder.tv.text = em
            if (t != null) {
                holder.tv.background = GradientDrawable().apply {
                    setColor(if (active) t.keyBackgroundColor.toInt() else 0x00000000)
                    cornerRadius = dp(8f).toFloat()
                    if (active) setStroke(dp(1.5f), t.accentKeyBackgroundColor.toInt())
                }
            }
            holder.tv.setOnClickListener { handleItemInsertOrSelect(em) }
        }

        override fun getItemCount(): Int = catalogEmojis.size

        inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)
    }

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()

    companion object {
        fun isCustomTextEntry(value: String): Boolean =
            value.any { it.isLetterOrDigit() || it.isWhitespace() || it in "@._-/#+•★✓?!" } || value.length > 4
    }
}
