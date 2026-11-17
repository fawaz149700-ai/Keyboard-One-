package com.example.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.KeySpec
import com.example.model.KeyType
import com.example.model.KeyboardLayoutSpec
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePresets
import com.example.model.KeyboardThemeSpec
import com.example.model.ShiftState
import kotlin.math.abs

class KeyboardCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onCharacterKey(key: KeySpec, text: String)
        fun onBackspaceTap()
        fun onBackspaceWord()
        fun onEnterTap()
        fun onSpaceTap()
        fun onShiftTap()
        fun onShiftLongPress()
        fun onSymbolsToggle()
        fun onSymbolsExtToggle()
        fun onLanguageTap()
        fun onLanguageLongPress()
        fun onEmojiTap()
        fun onClipboardTap()
        fun onSpaceHorizontalSwipe(direction: Int)
        fun onSpaceHorizontalDragPreview(targetLabel: String?)
        fun onSpaceVerticalSwipe(isUp: Boolean)
        fun onKeyPressFeedback(isSpecial: Boolean)
    }

    var listener: Listener? = null

    private var layoutSpec: KeyboardLayoutSpec? = null
    private var settings: KeyboardSettings = KeyboardSettings()
    private var theme: KeyboardThemeSpec = KeyboardThemePresets.AmberMidnight
    private var shiftState: ShiftState = ShiftState.OFF
    private var isLandscape: Boolean = false
    private var enterLabelOverride: String? = null
    private var forcedHeightPx: Int? = null

    private data class DrawnKey(
        val spec: KeySpec,
        val rect: RectF,
    )

    private val drawnKeys = mutableListOf<DrawnKey>()
    private val activePointers = mutableMapOf<Int, DrawnKey>()

    // Spacebar gesture tracking
    private var spacePointerId: Int = -1
    private var spaceDownX: Float = 0f
    private var spaceDownY: Float = 0f
    private var spaceAnchorX: Float = 0f
    private var spaceAnchorY: Float = 0f
    private var spaceAxis: Int = 0 // 0=undecided, 1=horizontal, 2=vertical
    private var spaceDidGesture: Boolean = false
    private var spaceLanguagePreviewIndexOffset: Int = 0
    private var spacebarOverrideLabel: String? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat().coerceAtLeast(dp(14f))
    private val horizSwitchThreshold = dp(54f)

    private fun vertSwipeThresholdPx(): Float =
        dp(settings.spaceSwipeSensitivityDp.coerceIn(12, 56).toFloat())

    // Timers for long-press alternates & backspace repeat
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var backspaceRepeatRunnable: Runnable? = null
    private var backspaceRepeatCount = 0
    private var shiftDidLongPress = false

    // Preview & Alternates Popups
    private var previewPopup: PopupWindow? = null
    private var previewTextView: TextView? = null
    private var alternatesPopup: PopupWindow? = null
    private var alternateButtons = mutableListOf<Pair<RectF, String>>()
    private var selectedAlternateIndex = -1
    private var alternatesAnchorPointerId = -1

    // Custom background image cache
    private var cachedBgUri: String? = null
    private var cachedBgBitmap: Bitmap? = null
    private val bgRect = RectF()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Pre-allocated Paints
    private val cairoTypeface: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.cairo)
    }.getOrNull()

    private val bgFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyPressedOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26FFFFFF
    }
    private val keyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = cairoTypeface ?: Typeface.DEFAULT
    }
    private val subLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = cairoTypeface ?: Typeface.DEFAULT
    }
    private val shiftIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempRect = RectF()

    fun updateState(
        layout: KeyboardLayoutSpec,
        settings: KeyboardSettings,
        theme: KeyboardThemeSpec,
        shiftState: ShiftState,
        isLandscape: Boolean,
        enterLabelOverride: String? = null,
    ) {
        val prevRowCount = this.layoutSpec?.rows?.size ?: 0
        val newRowCount = layout.rows.size
        val prevCols = this.layoutSpec?.columns ?: 0
        val heightChanged = prevRowCount != newRowCount ||
            prevCols != layout.columns ||
            this.settings.portraitHeightPercent != settings.portraitHeightPercent ||
            this.settings.landscapeHeightPercent != settings.landscapeHeightPercent ||
            this.settings.keyHeightPercent != settings.keyHeightPercent ||
            this.settings.toolbarScalePercent != settings.toolbarScalePercent ||
            this.isLandscape != isLandscape

        this.layoutSpec = layout
        this.settings = settings
        this.theme = theme
        this.shiftState = shiftState
        this.isLandscape = isLandscape
        this.enterLabelOverride = enterLabelOverride

        loadBackgroundBitmapIfNeeded(theme.backgroundImageUri, theme.bgBlurRadius)

        if (heightChanged) {
            requestLayout()
        }
        computeKeyRects(width, height)
        invalidate()
    }

    fun setSpacebarPreviewLabel(label: String?) {
        spacebarOverrideLabel = label
        invalidate()
    }

    private fun loadBackgroundBitmapIfNeeded(uriStr: String?, blurRadius: Float) {
        if (uriStr.isNullOrBlank()) {
            cachedBgUri = null
            cachedBgBitmap = null
            return
        }
        val key = "$uriStr|$blurRadius"
        if (cachedBgUri == key && cachedBgBitmap != null) return
        cachedBgUri = key
        runCatching {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                val raw = BitmapFactory.decodeStream(stream, null, opts) ?: return@use
                cachedBgBitmap = if (blurRadius > 0.5f) {
                    fastDownsampleBlur(raw, blurRadius)
                } else {
                    raw
                }
            }
        }
    }

    private fun fastDownsampleBlur(src: Bitmap, radius: Float): Bitmap {
        val factor = (1f + (radius / 6f)).coerceIn(1.5f, 8f)
        val w = (src.width / factor).toInt().coerceAtLeast(16)
        val h = (src.height / factor).toInt().coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        return Bitmap.createScaledBitmap(small, src.width, src.height, true)
    }

    fun setForcedHeightPx(heightPx: Int?) {
        if (this.forcedHeightPx != heightPx) {
            this.forcedHeightPx = heightPx
            requestLayout()
        }
    }

    fun computeTargetHeightPx(
        rowCount: Int,
        settings: KeyboardSettings,
        isLandscape: Boolean,
    ): Int {
        val safeRows = rowCount.coerceAtLeast(4)
        val baseRowHeightDp = if (isLandscape) 40f else 51f
        val heightFactor = if (isLandscape) {
            settings.landscapeHeightPercent / 100f
        } else {
            settings.portraitHeightPercent / 100f
        }
        val keyHeightFactor = settings.keyHeightPercent / 100f
        val bottomDelta = ((settings.toolbarScalePercent / 100f).coerceIn(0.80f, 1.28f) - 1f) * 0.25f
        val totalHeightPx = dp(baseRowHeightDp * (safeRows + bottomDelta) * heightFactor * keyHeightFactor + 10f).toInt()
        return totalHeightPx.coerceAtLeast(dp(140f).toInt())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val forced = forcedHeightPx
        val measuredH = if (forced != null) {
            forced.coerceAtLeast(dp(105f).toInt())
        } else {
            val rowCount = layoutSpec?.rows?.size?.coerceAtLeast(4) ?: 4
            computeTargetHeightPx(rowCount, settings, isLandscape)
        }
        setMeasuredDimension(w, measuredH)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeKeyRects(w, h)
    }

    private fun computeKeyRects(viewW: Int, viewH: Int) {
        drawnKeys.clear()
        val spec = layoutSpec ?: return
        if (viewW <= 0 || viewH <= 0 || spec.rows.isEmpty()) return

        val isElevenCol = spec.columns >= 11
        val isNumericGrid = spec.columns == 4
        val outerPadH = when {
            isNumericGrid -> dp(8f)
            isElevenCol -> dp(2.2f)
            else -> dp(4f)
        }
        val outerPadV = dp(5f)
        val hGap = when {
            isNumericGrid -> dp(settings.horizontalGapDp.coerceAtLeast(6f))
            isElevenCol -> dp(settings.horizontalGapDp.coerceAtMost(3.0f)).coerceAtMost((viewW * 0.0085f).coerceAtLeast(dp(1.5f)))
            else -> dp(settings.horizontalGapDp)
        }
        val vGap = dp(if (isNumericGrid) settings.verticalGapDp.coerceAtLeast(6f) else settings.verticalGapDp)

        val rows = spec.rows
        val rowCount = rows.size
        val availH = (viewH - outerPadV * 2 - vGap * (rowCount - 1)).coerceAtLeast(dp(60f))
        val bottomBarFactor = if (!isNumericGrid && rowCount >= 4) {
            (settings.toolbarScalePercent / 100f).coerceIn(0.80f, 1.28f)
        } else {
            1f
        }
        val rowFactors = rows.mapIndexed { idx, row ->
            val isNumRow = !isNumericGrid && row.keys.firstOrNull()?.type == KeyType.NUMBER
            val isBottom = !isNumericGrid && idx == rows.lastIndex && rowCount >= 4
            when {
                isNumRow -> 0.86f
                isBottom -> bottomBarFactor
                else -> 1f
            }
        }
        val totalFactor = rowFactors.sum().coerceAtLeast(1f)
        val rowHeights = rowFactors.map { availH * (it / totalFactor) }

        val widthScale = when {
            isElevenCol -> (settings.keyWidthPercent / 100f).coerceIn(0.84f, 1.08f)
            isNumericGrid -> (settings.keyWidthPercent / 100f).coerceIn(0.88f, 1.12f)
            else -> (settings.keyWidthPercent / 100f).coerceIn(0.75f, 1.15f)
        }
        val maxContentW = (viewW - outerPadH * 2).coerceAtLeast(dp(100f))

        var cursorY = outerPadV
        rows.forEachIndexed { rIdx, row ->
            val keys = row.keys
            val actualRowH = rowHeights[rIdx]
            if (keys.isEmpty()) {
                cursorY += actualRowH + vGap
                return@forEachIndexed
            }

            val totalWeight = keys.sumOf { it.widthWeight.toDouble() }.toFloat().coerceAtLeast(1f)

            // Center narrower rows nicely (unless 11-column or 4-column numeric grid)
            val totalUnitsTarget = spec.columns.toFloat().coerceAtLeast(totalWeight)
            val rowFillRatio = if (!isNumericGrid && rIdx == 1 && totalWeight < totalUnitsTarget && keys.none { it.type == KeyType.SHIFT } && !isElevenCol) {
                (totalWeight / totalUnitsTarget).coerceIn(0.84f, 1f)
            } else {
                1f
            }

            val rowContentW = maxContentW * rowFillRatio
            val rowStartX = outerPadH + (maxContentW - rowContentW) / 2f
            val gapsTotal = hGap * (keys.size - 1)
            val usableRowW = (rowContentW - gapsTotal).coerceAtLeast(dp(40f))

            var cursorX = rowStartX
            val maxExpand = (hGap - dp(1f)).coerceAtLeast(0f) * 0.5f
            keys.forEach { key ->
                val rawKeyW = usableRowW * (key.widthWeight / totalWeight)
                val scaledKeyW = if (key.type == KeyType.CHAR || key.type == KeyType.NUMBER) {
                    (rawKeyW * widthScale).coerceAtMost(rawKeyW + maxExpand)
                } else {
                    rawKeyW
                }
                val insetX = (rawKeyW - scaledKeyW) / 2f
                val rect = RectF(
                    cursorX + insetX,
                    cursorY,
                    cursorX + rawKeyW - insetX,
                    cursorY + actualRowH,
                )
                drawnKeys.add(DrawnKey(key, rect))
                cursorX += rawKeyW + hGap
            }
            cursorY += actualRowH + vGap
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Background
        bgFillPaint.color = theme.backgroundColor.toInt()
        canvas.drawRect(0f, 0f, w, h, bgFillPaint)

        val bgBmp = cachedBgBitmap
        if (bgBmp != null) {
            bgRect.set(0f, 0f, w, h)
            bgPaint.alpha = (theme.bgOpacity * 255).toInt().coerceIn(25, 255)
            canvas.drawBitmap(bgBmp, null, bgRect, bgPaint)
            bgOverlayPaint.color = if (theme.isDark) 0x55000000 else 0x33FFFFFF
            canvas.drawRect(0f, 0f, w, h, bgOverlayPaint)
        }

        val radius = dp(
            if (theme.isIosStyle) settings.keyCornerRadiusDp.coerceAtLeast(5.5f)
            else settings.keyCornerRadiusDp
        )
        val shadowOffset = dp(theme.keyShadowOffsetDp)
        val activeRects = activePointers.values.map { it.rect }

        for (dk in drawnKeys) {
            val key = dk.spec
            val r = dk.rect
            val isPressed = r in activeRects
            val isSpecial = key.type != KeyType.CHAR && key.type != KeyType.NUMBER && key.type != KeyType.SPACE

            val keyColor = when {
                key.type == KeyType.ENTER -> theme.accentKeyBackgroundColor.toInt()
                key.type == KeyType.SHIFT && shiftState != ShiftState.OFF -> theme.accentKeyBackgroundColor.toInt()
                isSpecial -> theme.specialKeyBackgroundColor.toInt()
                else -> theme.keyBackgroundColor.toInt()
            }

            val keyTextColor = when {
                key.type == KeyType.ENTER -> theme.accentKeyTextColor.toInt()
                key.type == KeyType.SHIFT && shiftState != ShiftState.OFF -> theme.accentKeyTextColor.toInt()
                isSpecial -> theme.specialKeyTextColor.toInt()
                else -> theme.keyTextColor.toInt()
            }

            val effectiveRadius = if (theme.isGboardStyle && (key.type == KeyType.SPACE || key.type == KeyType.ENTER)) {
                (r.height() / 2f).coerceAtLeast(radius)
            } else {
                radius
            }

            // Key drop shadow (especially crisp on iOS light/dark)
            if (shadowOffset > 0.2f && !isPressed) {
                keyShadowPaint.color = theme.keyShadowColor.toInt()
                tempRect.set(r.left, r.top + shadowOffset, r.right, r.bottom + shadowOffset)
                canvas.drawRoundRect(tempRect, effectiveRadius, effectiveRadius, keyShadowPaint)
            }

            // Key body
            keyFillPaint.color = keyColor
            canvas.drawRoundRect(r, effectiveRadius, effectiveRadius, keyFillPaint)

            if (isPressed) {
                canvas.drawRoundRect(r, effectiveRadius, effectiveRadius, keyPressedOverlayPaint)
            }

            // Key label
            val isNumericGrid = (layoutSpec?.columns == 4)
            val toolbarTextScale = (settings.toolbarScalePercent / 100f).coerceIn(0.85f, 1.22f)
            val displayLabel = resolveDisplayLabel(key)
            val baseFontSp = when {
                isNumericGrid && key.type == KeyType.NUMBER -> (settings.keyFontSizeSp * 1.12f).coerceIn(17f, 26f)
                key.type == KeyType.SPACE -> (settings.keyFontSizeSp * 0.68f * toolbarTextScale).coerceIn(10f, 18f)
                key.type == KeyType.MODE_SYMBOLS || key.type == KeyType.ENTER -> (settings.keyFontSizeSp * 0.72f * toolbarTextScale).coerceIn(11f, 19f)
                key.type == KeyType.CLIPBOARD || key.type == KeyType.EMOJI || key.type == KeyType.LANGUAGE -> (settings.keyFontSizeSp * 0.85f * toolbarTextScale).coerceIn(12f, 22f)
                key.type == KeyType.NUMBER -> (settings.keyFontSizeSp * 0.85f).coerceIn(13f, 22f)
                else -> settings.keyFontSizeSp
            }
            val fontSp = if (key.type == KeyType.CHAR && r.width() < dp(32f)) {
                (baseFontSp * (r.width() / dp(33f)).coerceIn(0.76f, 0.96f)).coerceAtLeast(13f)
            } else {
                baseFontSp
            }

            keyTextPaint.color = keyTextColor
            keyTextPaint.textSize = sp(fontSp)
            keyTextPaint.isFakeBoldText = key.type == KeyType.ENTER ||
                (isNumericGrid && key.type == KeyType.NUMBER) ||
                (key.type == KeyType.SHIFT && shiftState == ShiftState.CAPS_LOCK)

            val fm = keyTextPaint.fontMetrics
            val textY = r.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(displayLabel, r.centerX(), textY, keyTextPaint)

            // Caps-lock dot indicator on Shift
            if (key.type == KeyType.SHIFT && shiftState == ShiftState.CAPS_LOCK) {
                shiftIndicatorPaint.color = keyTextColor
                canvas.drawCircle(r.centerX(), r.bottom - dp(5f), dp(2.2f), shiftIndicatorPaint)
            }

            // Sub-label hint (top-right tiny alternate glyph)
            if ((key.type == KeyType.CHAR || (isNumericGrid && key.type == KeyType.NUMBER)) &&
                !key.subLabel.isNullOrBlank() &&
                r.width() > dp(23f)
            ) {
                subLabelPaint.color = (keyTextColor and 0x00FFFFFF) or 0x77000000
                subLabelPaint.textSize = sp((fontSp * 0.42f).coerceIn(7.5f, 11f))
                canvas.drawText(key.subLabel, r.right - dp(3f), r.top + dp(9.5f), subLabelPaint)
            }
        }
    }

    private fun resolveDisplayLabel(key: KeySpec): String {
        if (key.type == KeyType.SPACE) {
            return spacebarOverrideLabel ?: key.label
        }
        if (key.type == KeyType.ENTER && !enterLabelOverride.isNullOrBlank()) {
            return enterLabelOverride!!
        }
        if (key.type == KeyType.SHIFT) {
            return when (shiftState) {
                ShiftState.OFF -> "⇧"
                ShiftState.ONCE -> "⬆"
                ShiftState.CAPS_LOCK -> "⇪"
            }
        }
        if (key.type == KeyType.CHAR && shiftState != ShiftState.OFF) {
            return key.shiftOutput
        }
        return key.label
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val idx = event.actionIndex
        val pointerId = event.getPointerId(idx)

        // If alternates popup is open, track sliding finger to pick alternate!
        if (alternatesPopup?.isShowing == true && pointerId == alternatesAnchorPointerId) {
            when (action) {
                MotionEvent.ACTION_MOVE -> {
                    updateAlternateSelection(event.getRawXForPointer(idx), event.getRawYForPointer(idx))
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    commitSelectedAlternate()
                    dismissAlternatesPopup()
                    activePointers.remove(pointerId)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    dismissAlternatesPopup()
                    activePointers.remove(pointerId)
                    invalidate()
                    return true
                }
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(idx)
                val y = event.getY(idx)
                val hit = findKeyAt(x, y) ?: return true
                activePointers[pointerId] = hit
                listener?.onKeyPressFeedback(hit.spec.type != KeyType.CHAR && hit.spec.type != KeyType.NUMBER)

                when (hit.spec.type) {
                    KeyType.SPACE -> {
                        spacePointerId = pointerId
                        spaceDownX = x
                        spaceDownY = y
                        spaceAnchorX = x
                        spaceAnchorY = y
                        spaceAxis = 0
                        spaceDidGesture = false
                        spaceLanguagePreviewIndexOffset = 0
                    }
                    KeyType.BACKSPACE -> {
                        listener?.onBackspaceTap()
                        startBackspaceRepeat()
                    }
                    KeyType.CHAR, KeyType.NUMBER, KeyType.COMMA, KeyType.PERIOD -> {
                        if (settings.keyPreviewPopupEnabled) {
                            showKeyPreview(hit)
                        }
                        if (hit.spec.alternates.isNotEmpty()) {
                            scheduleLongPressAlternates(pointerId, hit)
                        }
                    }
                    KeyType.SHIFT -> {
                        shiftDidLongPress = false
                        scheduleShiftLongPress()
                    }
                    KeyType.LANGUAGE -> {
                        scheduleLanguageLongPress()
                    }
                    else -> Unit
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                for (p in 0 until event.pointerCount) {
                    val pId = event.getPointerId(p)
                    if (pId == spacePointerId) {
                        handleSpaceMove(event.getX(p), event.getY(p))
                    } else {
                        val x = event.getX(p)
                        val y = event.getY(p)
                        val currentHit = activePointers[pId]
                        if (currentHit != null && currentHit.spec.type != KeyType.BACKSPACE) {
                            val newHit = findKeyAt(x, y)
                            if (newHit != null && newHit.spec.id != currentHit.spec.id) {
                                cancelPendingLongPress()
                                activePointers[pId] = newHit
                                if (newHit.spec.type == KeyType.CHAR || newHit.spec.type == KeyType.NUMBER) {
                                    if (settings.keyPreviewPopupEnabled) showKeyPreview(newHit)
                                    if (newHit.spec.alternates.isNotEmpty()) {
                                        scheduleLongPressAlternates(pId, newHit)
                                    }
                                } else {
                                    dismissKeyPreview()
                                }
                                invalidate()
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                cancelPendingLongPress()
                dismissKeyPreview()
                if (pointerId == spacePointerId) {
                    if (!spaceDidGesture) {
                        listener?.onSpaceTap()
                    }
                    spacebarOverrideLabel = null
                    listener?.onSpaceHorizontalDragPreview(null)
                    spacePointerId = -1
                    spaceAxis = 0
                    spaceDidGesture = false
                } else {
                    val keyHit = activePointers[pointerId]
                    if (keyHit != null) {
                        dispatchKeyRelease(keyHit.spec)
                    }
                }
                activePointers.remove(pointerId)
                if (activePointers.values.none { it.spec.type == KeyType.BACKSPACE }) {
                    stopBackspaceRepeat()
                }
                invalidate()
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelPendingLongPress()
                stopBackspaceRepeat()
                dismissKeyPreview()
                dismissAlternatesPopup()
                spacePointerId = -1
                spacebarOverrideLabel = null
                activePointers.clear()
                invalidate()
            }
        }
        return true
    }

    private fun handleSpaceMove(x: Float, y: Float) {
        val dxFromDown = x - spaceDownX
        val dyFromDown = y - spaceDownY
        val vertThreshold = vertSwipeThresholdPx()
        val effectiveVertSlop = touchSlop.coerceAtMost(vertThreshold * 0.65f)

        if (spaceAxis == 0) {
            if (abs(dxFromDown) > touchSlop || abs(dyFromDown) > effectiveVertSlop) {
                spaceAxis = if (abs(dxFromDown) >= abs(dyFromDown)) 1 else 2
                spaceDidGesture = true
            } else {
                return
            }
        }

        if (spaceAxis == 1) {
            // Horizontal language swipe on Spacebar
            val langs = settings.enabledLanguages
            if (langs.size > 1) {
                val dxAnchor = x - spaceAnchorX
                if (abs(dxAnchor) >= horizSwitchThreshold) {
                    val dir = if (dxAnchor > 0) 1 else -1
                    listener?.onSpaceHorizontalSwipe(dir)
                    spaceAnchorX = x
                } else {
                    val previewDir = if (dxAnchor >= touchSlop) 1 else if (dxAnchor <= -touchSlop) -1 else 0
                    if (previewDir != 0) {
                        val idx = langs.indexOf(settings.activeLanguage).takeIf { it >= 0 } ?: 0
                        val targetLang = langs[(idx + previewDir).mod(langs.size)]
                        spacebarOverrideLabel = "◀ ${targetLang.nativeName} ▶"
                        invalidate()
                    }
                }
            }
        } else if (spaceAxis == 2 && settings.spaceSwipeBarsEnabled) {
            // Vertical sequential bar swipe on Spacebar with user-configured sensitivity
            val dyAnchor = y - spaceAnchorY
            if (abs(dyAnchor) >= vertThreshold) {
                val rawUp = dyAnchor < 0
                val isUp = if (settings.invertSpaceSwipe) !rawUp else rawUp
                listener?.onSpaceVerticalSwipe(isUp)
                spaceAnchorY = y
            }
        }
    }

    private fun dispatchKeyRelease(key: KeySpec) {
        when (key.type) {
            KeyType.CHAR, KeyType.NUMBER, KeyType.COMMA, KeyType.PERIOD -> {
                val out = if (key.type == KeyType.CHAR && shiftState != ShiftState.OFF) key.shiftOutput else key.output
                listener?.onCharacterKey(key, out)
            }
            KeyType.SHIFT -> {
                if (!shiftDidLongPress) {
                    listener?.onShiftTap()
                }
                shiftDidLongPress = false
            }
            KeyType.ENTER -> listener?.onEnterTap()
            KeyType.MODE_SYMBOLS -> listener?.onSymbolsToggle()
            KeyType.MODE_SYMBOLS_EXT -> listener?.onSymbolsExtToggle()
            KeyType.LANGUAGE -> listener?.onLanguageTap()
            KeyType.EMOJI -> listener?.onEmojiTap()
            KeyType.CLIPBOARD -> listener?.onClipboardTap()
            KeyType.BACKSPACE, KeyType.SPACE -> Unit
        }
    }

    private fun findKeyAt(x: Float, y: Float): DrawnKey? {
        drawnKeys.firstOrNull { it.rect.contains(x, y) }?.let { return it }
        // Nearest key within generous hit area
        var best: DrawnKey? = null
        var bestDist = Float.MAX_VALUE
        for (dk in drawnKeys) {
            val dx = (x - dk.rect.centerX())
            val dy = (y - dk.rect.centerY())
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                best = dk
            }
        }
        return if (bestDist < dp(48f) * dp(48f)) best else null
    }

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        backspaceRepeatCount = 0
        val r = object : Runnable {
            override fun run() {
                backspaceRepeatCount++
                if (backspaceRepeatCount >= 14) {
                    listener?.onBackspaceWord()
                } else {
                    listener?.onBackspaceTap()
                }
                handler.postDelayed(this, 60L)
            }
        }
        backspaceRepeatRunnable = r
        handler.postDelayed(r, 380L)
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeatRunnable?.let { handler.removeCallbacks(it) }
        backspaceRepeatRunnable = null
    }

    private fun scheduleShiftLongPress() {
        cancelPendingLongPress()
        val r = Runnable {
            shiftDidLongPress = true
            listener?.onShiftLongPress()
            invalidate()
        }
        longPressRunnable = r
        handler.postDelayed(r, 380L)
    }

    private fun scheduleLanguageLongPress() {
        cancelPendingLongPress()
        val r = Runnable {
            activePointers.clear()
            dismissKeyPreview()
            listener?.onLanguageLongPress()
            invalidate()
        }
        longPressRunnable = r
        handler.postDelayed(r, 420L)
    }

    private fun scheduleLongPressAlternates(pointerId: Int, key: DrawnKey) {
        cancelPendingLongPress()
        val r = Runnable {
            dismissKeyPreview()
            showAlternatesPopup(pointerId, key)
        }
        longPressRunnable = r
        handler.postDelayed(r, 380L)
    }

    private fun cancelPendingLongPress() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        longPressRunnable = null
    }

    private fun showKeyPreview(dk: DrawnKey) {
        val label = resolveDisplayLabel(dk.spec)
        if (previewPopup == null) {
            val tv = TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 26f
                typeface = cairoTypeface ?: Typeface.DEFAULT_BOLD
                setPadding(dp(12f).toInt(), dp(6f).toInt(), dp(12f).toInt(), dp(6f).toInt())
            }
            previewTextView = tv
            previewPopup = PopupWindow(tv, dp(50f).toInt(), dp(58f).toInt(), false).apply {
                isTouchable = false
                elevation = dp(6f)
            }
        }
        previewTextView?.apply {
            text = label
            setTextColor(theme.popupTextColor.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(theme.popupBackgroundColor.toInt())
                cornerRadius = dp(10f)
            }
        }
        val loc = IntArray(2)
        getLocationInWindow(loc)
        val x = (loc[0] + dk.rect.centerX() - dp(25f)).toInt()
        val y = (loc[1] + dk.rect.top - dp(62f)).toInt()
        if (previewPopup?.isShowing == true) {
            previewPopup?.update(x, y, dp(50f).toInt(), dp(58f).toInt())
        } else {
            previewPopup?.showAtLocation(this, Gravity.NO_GRAVITY, x, y)
        }
    }

    private fun dismissKeyPreview() {
        previewPopup?.dismiss()
    }

    private fun showAlternatesPopup(pointerId: Int, dk: DrawnKey) {
        val alts = dk.spec.alternates
        if (alts.isEmpty()) return
        alternatesAnchorPointerId = pointerId
        selectedAlternateIndex = 0

        val rowPad = dp(6f).toInt()
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(rowPad, rowPad, rowPad, rowPad)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(theme.popupBackgroundColor.toInt())
                cornerRadius = dp(12f)
            }
        }

        val itemH = dp(42f).toInt()
        val minItemW = dp(38f).toInt()
        val itemViews = mutableListOf<TextView>()
        alternateButtons.clear()

        alts.forEachIndexed { idx, alt ->
            val isLongToken = alt.length > 2
            val hPad = dp(if (isLongToken) 11f else 8f).toInt()
            val tv = TextView(context).apply {
                text = alt
                gravity = Gravity.CENTER
                maxLines = 1
                minWidth = minItemW
                textSize = if (isLongToken) 14f else 18f
                typeface = cairoTypeface ?: Typeface.DEFAULT
                setPadding(hPad, 0, hPad, 0)
                setTextColor(theme.popupTextColor.toInt())
                if (idx == 0) {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(theme.accentKeyBackgroundColor.toInt())
                        cornerRadius = dp(8f)
                    }
                    setTextColor(theme.accentKeyTextColor.toInt())
                }
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, itemH)
            if (idx > 0) lp.marginStart = dp(3f).toInt()
            row.addView(tv, lp)
            itemViews.add(tv)
        }

        val maxPopupW = (width - dp(16f).toInt()).coerceAtLeast(dp(120f).toInt())
        row.measure(
            MeasureSpec.makeMeasureSpec(maxPopupW, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(itemH + rowPad * 2, MeasureSpec.EXACTLY),
        )
        val totalW = row.measuredWidth.coerceAtLeast(minItemW + rowPad * 2)
        val totalH = row.measuredHeight.coerceAtLeast(itemH + rowPad * 2)

        val loc = IntArray(2)
        getLocationInWindow(loc)
        val maxX = (loc[0] + width - totalW - dp(6f).toInt()).coerceAtLeast(loc[0] + 8)
        val popupX = (loc[0] + dk.rect.centerX() - totalW / 2f).toInt().coerceIn(loc[0] + 8, maxX)
        val popupY = (loc[1] + dk.rect.top - totalH - dp(6f)).toInt()

        alternatesPopup?.dismiss()
        alternatesPopup = PopupWindow(row, totalW, totalH, false).apply {
            isTouchable = false
            elevation = dp(10f)
            showAtLocation(this@KeyboardCanvasView, Gravity.NO_GRAVITY, popupX, popupY)
        }

        var cursorX = (popupX + rowPad).toFloat()
        val gapPx = dp(3f)
        itemViews.forEachIndexed { idx, tv ->
            val w = tv.measuredWidth.coerceAtLeast(minItemW).toFloat()
            alternateButtons.add(RectF(cursorX, popupY.toFloat(), cursorX + w, (popupY + totalH).toFloat()) to alts[idx])
            cursorX += w + gapPx
        }
    }

    private fun updateAlternateSelection(rawX: Float, rawY: Float) {
        val row = alternatesPopup?.contentView as? LinearLayout ?: return
        var matched = -1
        for (i in alternateButtons.indices) {
            if (rawX >= alternateButtons[i].first.left && rawX <= alternateButtons[i].first.right) {
                matched = i
                break
            }
        }
        if (matched >= 0 && matched != selectedAlternateIndex) {
            selectedAlternateIndex = matched
            for (i in 0 until row.childCount) {
                val tv = row.getChildAt(i) as? TextView ?: continue
                if (i == matched) {
                    tv.background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(theme.accentKeyBackgroundColor.toInt())
                        cornerRadius = dp(8f)
                    }
                    tv.setTextColor(theme.accentKeyTextColor.toInt())
                } else {
                    tv.background = null
                    tv.setTextColor(theme.popupTextColor.toInt())
                }
            }
        }
    }

    private fun commitSelectedAlternate() {
        val alt = alternateButtons.getOrNull(selectedAlternateIndex)?.second ?: return
        val dummyKey = KeySpec("alt_$alt", alt, alt)
        listener?.onCharacterKey(dummyKey, alt)
    }

    private fun dismissAlternatesPopup() {
        alternatesPopup?.dismiss()
        alternatesAnchorPointerId = -1
        alternateButtons.clear()
        selectedAlternateIndex = -1
    }

    private fun MotionEvent.getRawXForPointer(pointerIndex: Int): Float {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return loc[0] + getX(pointerIndex)
    }

    private fun MotionEvent.getRawYForPointer(pointerIndex: Int): Float {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return loc[1] + getY(pointerIndex)
    }

    private fun dp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun sp(v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}
