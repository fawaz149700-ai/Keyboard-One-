package com.example.ime

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.data.db.ClipboardItemEntity
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.decorate.DecorationFrame
import com.example.data.decorate.DecorationStyle
import com.example.data.translate.TranslationLanguageInfo
import com.example.ime.panels.CalculatorPanelView
import com.example.ime.panels.ClipboardPanelView
import com.example.ime.panels.DecoratePanelView
import com.example.ime.panels.EmojiPanelView
import com.example.ime.panels.QuickEmojiEditorPanelView
import com.example.ime.panels.SymbolsPanelView
import com.example.ime.panels.TranslatePanelView
import com.example.model.HandMode
import com.example.model.KeyboardLayoutSpec
import com.example.model.KeyboardMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemeSpec
import com.example.model.ShiftState

class KeyboardContainerView(
    context: Context,
    supportedTranslationLanguages: List<TranslationLanguageInfo>,
    canvasListener: KeyboardCanvasView.Listener,
    private val actions: ContainerActions,
) : FrameLayout(context) {

    interface ContainerActions {
        fun onSuggestionClick(word: String)
        fun onTranslationChipClick(text: String)
        fun onQuickEmojiClick(emoji: String)
        fun onSwitchMode(mode: KeyboardMode)
        fun onCycleHandMode()
        fun onSetHandMode(mode: HandMode)
        fun onUpdateFloatingPosition(offsetX: Float, offsetY: Float)
        fun onOpenSettings()
        fun onPasteClip(text: String)
        fun onCopyClip(text: String)
        fun onTogglePinClip(item: ClipboardItemEntity)
        fun onDeleteClip(id: Long)
        fun onDeleteSelectedClips(ids: List<Long>)
        fun onClearUnpinnedClips()
        fun onToggleClipboardGridMode(gridMode: Boolean)
        fun onTranslateRequest(source: String, target: String, selectedOnly: Boolean)
        fun onReplaceText(newText: String)
        fun onInsertText(text: String)
        fun onApplyFawazDecoration()
        fun onToggleRealtimeDecoration(enabled: Boolean)
        fun onToggleFavoriteDecoration(style: DecorationStyle, frame: DecorationFrame)
        fun onUpdateQuickEmojis(emojis: List<String>)
        fun onUpdateQuickEmojiSizeSp(sizeSp: Float)
        fun onResetQuickEmojiDefaults()
        fun onSearchInputStateChanged(active: Boolean)
        fun onEmojiPanelCommit(emoji: String)
        fun onSymbolPanelCommit(symbol: String)
        fun onCalculatorInsert(text: String)
        fun onPanelBackspace()
        fun getCurrentFieldTextSnapshot(): String
    }

    val canvasView = KeyboardCanvasView(context).apply {
        listener = canvasListener
    }

    private val bodyWrapper: LinearLayout
    private val floatingHandleBar: LinearLayout
    private val oneHandRow: LinearLayout
    private val oneHandSideRail: LinearLayout
    private val keyboardFrame: LinearLayout

    // 1. Suggestions + Quick Tools bar
    private val suggestionsBarContainer: LinearLayout
    private val suggestionsScrollRow: LinearLayout
    private val toolsContainer: LinearLayout
    private var translationPreviewChip: String? = null

    // 2. Quick Emoji bar (9 squares + 10th Settings key)
    private val quickEmojiBarRow: LinearLayout

    // Panels
    private val panelHost: FrameLayout
    val emojiPanel: EmojiPanelView
    val clipboardPanel: ClipboardPanelView
    val translatePanel: TranslatePanelView
    val symbolsPanel: SymbolsPanelView
    val decoratePanel: DecoratePanelView
    val calculatorPanel: CalculatorPanelView
    val quickEmojiEditorPanel: QuickEmojiEditorPanelView

    private var currentMode: KeyboardMode = KeyboardMode.ALPHA
    private var lastRenderedMode: KeyboardMode? = null
    private var currentSettings: KeyboardSettings = KeyboardSettings()
    private var currentTheme: KeyboardThemeSpec? = null
    private var currentIsLandscape: Boolean = false
    private var currentClipboardItems: List<ClipboardItemEntity> = emptyList()
    private var currentFavoriteDecorations: List<FavoriteDecorationEntity> = emptyList()
    private var lastBarSignature: String = ""
    private var suggestionsTargetHeightPx = dp(40f)
    private var quickEmojiTargetHeightPx = dp(44f)
    private var isSearchTypingActive = false

    private var floatDragStartX = 0f
    private var floatDragStartY = 0f
    private var floatInitOffsetX = 0f
    private var floatInitOffsetY = 0f

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        bodyWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Floating drag header
        floatingHandleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            visibility = View.GONE
        }
        val dragTitle = TextView(context).apply {
            text = "⠿ اسحب لتحريك الكيبورد العائم"
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
        }
        val dockBtn = TextView(context).apply {
            text = "⬇ تثبيت"
            textSize = 11.5f
            setPadding(dp(10f), dp(4f), dp(10f), dp(4f))
            setOnClickListener { actions.onSetHandMode(HandMode.FULL) }
        }
        floatingHandleBar.addView(dragTitle, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        floatingHandleBar.addView(dockBtn, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        setupFloatingDrag(floatingHandleBar)
        bodyWrapper.addView(floatingHandleBar)

        // One-hand horizontal wrapper
        oneHandRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }

        oneHandSideRail = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(4f), dp(8f), dp(4f), dp(8f))
        }
        val expandBtn = makeRailButton("⛶") { actions.onSetHandMode(HandMode.FULL) }
        val flipBtn = makeRailButton("⇄") {
            val next = if (currentSettings.handMode == HandMode.ONE_HAND_LEFT) HandMode.ONE_HAND_RIGHT else HandMode.ONE_HAND_LEFT
            actions.onSetHandMode(next)
        }
        val floatBtn = makeRailButton("❐") { actions.onSetHandMode(HandMode.FLOATING) }
        oneHandSideRail.addView(expandBtn, LinearLayout.LayoutParams(dp(42f), dp(42f)).apply { bottomMargin = dp(8f) })
        oneHandSideRail.addView(flipBtn, LinearLayout.LayoutParams(dp(42f), dp(42f)).apply { bottomMargin = dp(8f) })
        oneHandSideRail.addView(floatBtn, LinearLayout.LayoutParams(dp(42f), dp(42f)))

        keyboardFrame = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // --- BAR 1: Suggestions & Toolbar ---
        suggestionsBarContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6f), dp(2f), dp(6f), dp(2f))
        }
        toolsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val toolSpecs = listOf(
            "📋" to { togglePanel(KeyboardMode.CLIPBOARD) },
            "🌐" to { togglePanel(KeyboardMode.TRANSLATE) },
            "✨" to { togglePanel(KeyboardMode.DECORATE) },
            "🧮" to { togglePanel(KeyboardMode.CALCULATOR) },
            "Ω" to { togglePanel(KeyboardMode.SYMBOLS_BROWSER) },
            "🤏" to { actions.onCycleHandMode() },
            "⚙" to { actions.onOpenSettings() },
        )
        toolSpecs.forEach { (icon, onClick) ->
            val tv = TextView(context).apply {
                text = icon
                textSize = 14.5f
                gravity = Gravity.CENTER
                setPadding(dp(3f), dp(3f), dp(3f), dp(3f))
                setOnClickListener { onClick() }
            }
            toolsContainer.addView(tv, LinearLayout.LayoutParams(dp(27f), dp(32f)))
        }

        val sugScroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false }
        suggestionsScrollRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sugScroll.addView(suggestionsScrollRow)

        suggestionsBarContainer.addView(sugScroll, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        suggestionsBarContainer.addView(toolsContainer, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        keyboardFrame.addView(suggestionsBarContainer, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, suggestionsTargetHeightPx))

        // --- BAR 2: Quick Emoji Square Strip (9 emojis + 10th Settings square) ---
        quickEmojiBarRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4f), dp(2f), dp(4f), dp(2f))
        }
        keyboardFrame.addView(quickEmojiBarRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0))

        // --- BAR 3 & 4: Main Panels / Canvas ---
        panelHost = FrameLayout(context)
        emojiPanel = EmojiPanelView(
            context = context,
            onEmojiCommit = { actions.onEmojiPanelCommit(it) },
            onBackspace = { actions.onPanelBackspace() },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
            onRequestSearchInput = { active ->
                isSearchTypingActive = active
                actions.onSearchInputStateChanged(active)
                reapplyStablePanelGeometry()
            },
        )
        clipboardPanel = ClipboardPanelView(
            context = context,
            onPasteText = { actions.onPasteClip(it) },
            onCopyText = { actions.onCopyClip(it) },
            onTogglePin = { actions.onTogglePinClip(it) },
            onDeleteItem = { actions.onDeleteClip(it) },
            onDeleteSelected = { actions.onDeleteSelectedClips(it) },
            onClearUnpinned = { actions.onClearUnpinnedClips() },
            onToggleGridMode = { actions.onToggleClipboardGridMode(it) },
            onAddQuickTemplate = { actions.onOpenSettings() },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
        )
        translatePanel = TranslatePanelView(
            context = context,
            supportedLanguages = supportedTranslationLanguages,
            onTranslateField = { s, t, sel -> actions.onTranslateRequest(s, t, sel) },
            onReplaceWithTranslation = { actions.onReplaceText(it) },
            onInsertTranslation = { actions.onInsertText(it) },
            onOpenOfflinePacksSettings = { actions.onOpenSettings() },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
        )
        symbolsPanel = SymbolsPanelView(
            context = context,
            onCommitSymbol = { actions.onSymbolPanelCommit(it) },
            onBackspace = { actions.onPanelBackspace() },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
            onRequestSearchInput = { active ->
                isSearchTypingActive = active
                actions.onSearchInputStateChanged(active)
                reapplyStablePanelGeometry()
            },
        )
        decoratePanel = DecoratePanelView(
            context = context,
            onReplaceText = { actions.onReplaceText(it) },
            onInsertText = { actions.onInsertText(it) },
            onToggleFavorite = { style, frame -> actions.onToggleFavoriteDecoration(style, frame) },
            onApplyFawaz = { actions.onApplyFawazDecoration() },
            onToggleRealtimeDecoration = { actions.onToggleRealtimeDecoration(it) },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
        )
        calculatorPanel = CalculatorPanelView(
            context = context,
            onCommitResult = { actions.onCalculatorInsert(it) },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
        )
        quickEmojiEditorPanel = QuickEmojiEditorPanelView(
            context = context,
            onUpdateEmojis = { actions.onUpdateQuickEmojis(it) },
            onUpdateSizeSp = { actions.onUpdateQuickEmojiSizeSp(it) },
            onResetDefaults = { actions.onResetQuickEmojiDefaults() },
            onBackToKeyboard = { actions.onSwitchMode(KeyboardMode.ALPHA) },
            onRequestCustomTextInput = { active ->
                isSearchTypingActive = active
                actions.onSearchInputStateChanged(active)
                reapplyStablePanelGeometry()
            },
        )

        panelHost.addView(emojiPanel)
        panelHost.addView(clipboardPanel)
        panelHost.addView(translatePanel)
        panelHost.addView(symbolsPanel)
        panelHost.addView(decoratePanel)
        panelHost.addView(calculatorPanel)
        panelHost.addView(quickEmojiEditorPanel)
        panelHost.visibility = View.GONE

        keyboardFrame.addView(panelHost, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        keyboardFrame.addView(canvasView, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        oneHandRow.addView(oneHandSideRail, LinearLayout.LayoutParams(dp(50f), LayoutParams.MATCH_PARENT))
        oneHandRow.addView(keyboardFrame, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        bodyWrapper.addView(oneHandRow, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(bodyWrapper, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun togglePanel(target: KeyboardMode) {
        isSearchTypingActive = false
        actions.onSearchInputStateChanged(false)
        val next = if (currentMode == target) KeyboardMode.ALPHA else target
        actions.onSwitchMode(next)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingDrag(handle: View) {
        handle.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    floatDragStartX = ev.rawX
                    floatDragStartY = ev.rawY
                    floatInitOffsetX = bodyWrapper.translationX
                    floatInitOffsetY = bodyWrapper.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - floatDragStartX
                    val dy = ev.rawY - floatDragStartY
                    val maxDx = (width * 0.25f).coerceAtLeast(dp(60f).toFloat())
                    val newX = (floatInitOffsetX + dx).coerceIn(-maxDx, maxDx)
                    val newY = (floatInitOffsetY + dy).coerceIn(-dp(240f).toFloat(), 0f)
                    bodyWrapper.translationX = newX
                    bodyWrapper.translationY = newY
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    actions.onUpdateFloatingPosition(bodyWrapper.translationX, bodyWrapper.translationY)
                    true
                }
                else -> false
            }
        }
    }

    fun applyState(
        mode: KeyboardMode,
        layout: KeyboardLayoutSpec,
        settings: KeyboardSettings,
        theme: KeyboardThemeSpec,
        shiftState: ShiftState,
        isLandscape: Boolean,
        suggestions: List<String>,
        clipboardItems: List<ClipboardItemEntity>,
        favoriteDecorations: List<FavoriteDecorationEntity>,
        enterLabelOverride: String? = null,
        numericContextLabel: String? = null,
    ) {
        val prevMode = this.lastRenderedMode
        this.currentMode = mode
        this.lastRenderedMode = mode
        this.currentSettings = settings
        this.currentTheme = theme
        this.currentIsLandscape = isLandscape
        this.currentClipboardItems = clipboardItems
        this.currentFavoriteDecorations = favoriteDecorations

        val toolbarScale = (settings.toolbarScalePercent / 100f).coerceIn(0.70f, 1.45f)
        suggestionsTargetHeightPx = dp(40f * toolbarScale)
        quickEmojiTargetHeightPx = dp(44f * toolbarScale)

        // Hand mode & Floating sizing
        val screenW = resources.displayMetrics.widthPixels
        when (settings.handMode) {
            HandMode.FULL -> {
                floatingHandleBar.visibility = View.GONE
                oneHandSideRail.visibility = View.GONE
                bodyWrapper.translationX = 0f
                bodyWrapper.translationY = 0f
                bodyWrapper.layoutParams = (bodyWrapper.layoutParams as LayoutParams).apply {
                    width = LayoutParams.MATCH_PARENT
                    gravity = Gravity.BOTTOM
                }
            }
            HandMode.ONE_HAND_LEFT -> {
                floatingHandleBar.visibility = View.GONE
                oneHandSideRail.visibility = View.VISIBLE
                oneHandRow.removeView(oneHandSideRail)
                oneHandRow.addView(oneHandSideRail, 1) // rail on right, keyboard anchored left
                bodyWrapper.translationX = 0f
                bodyWrapper.translationY = 0f
                bodyWrapper.layoutParams = (bodyWrapper.layoutParams as LayoutParams).apply {
                    width = LayoutParams.MATCH_PARENT
                    gravity = Gravity.BOTTOM
                }
            }
            HandMode.ONE_HAND_RIGHT -> {
                floatingHandleBar.visibility = View.GONE
                oneHandSideRail.visibility = View.VISIBLE
                oneHandRow.removeView(oneHandSideRail)
                oneHandRow.addView(oneHandSideRail, 0) // rail on left, keyboard anchored right
                bodyWrapper.translationX = 0f
                bodyWrapper.translationY = 0f
                bodyWrapper.layoutParams = (bodyWrapper.layoutParams as LayoutParams).apply {
                    width = LayoutParams.MATCH_PARENT
                    gravity = Gravity.BOTTOM
                }
            }
            HandMode.FLOATING -> {
                floatingHandleBar.visibility = View.VISIBLE
                oneHandSideRail.visibility = View.GONE
                val floatW = (screenW * (settings.floatingWidthPercent / 100f)).toInt().coerceAtLeast(dp(260f))
                bodyWrapper.layoutParams = (bodyWrapper.layoutParams as LayoutParams).apply {
                    width = floatW
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                }
                bodyWrapper.translationX = settings.floatingOffsetX
                bodyWrapper.translationY = settings.floatingOffsetY
            }
        }

        // Colors & toolbar scaling
        bodyWrapper.setBackgroundColor(theme.backgroundColor.toInt())
        floatingHandleBar.setBackgroundColor(theme.suggestionBarBgColor.toInt())
        (floatingHandleBar.getChildAt(0) as? TextView)?.apply {
            setTextColor(theme.specialKeyTextColor.toInt())
            textSize = 12f * toolbarScale.coerceIn(0.85f, 1.2f)
        }
        (floatingHandleBar.getChildAt(1) as? TextView)?.apply {
            setTextColor(theme.accentKeyTextColor.toInt())
            textSize = 11.5f * toolbarScale.coerceIn(0.85f, 1.2f)
            background = GradientDrawable().apply {
                setColor(theme.accentKeyBackgroundColor.toInt())
                cornerRadius = dp(8f).toFloat()
            }
        }

        oneHandSideRail.setBackgroundColor(theme.suggestionBarBgColor.toInt())
        for (i in 0 until oneHandSideRail.childCount) {
            (oneHandSideRail.getChildAt(i) as? TextView)?.apply {
                setTextColor(theme.specialKeyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(theme.specialKeyBackgroundColor.toInt())
                    cornerRadius = dp(10f).toFloat()
                }
            }
        }

        suggestionsBarContainer.setBackgroundColor(theme.suggestionBarBgColor.toInt())
        quickEmojiBarRow.setBackgroundColor(theme.backgroundColor.toInt())
        val toolW = dp(26.5f * toolbarScale.coerceIn(0.8f, 1.25f))
        val toolH = dp(32f * toolbarScale.coerceIn(0.8f, 1.25f))
        val toolSp = 14.5f * toolbarScale.coerceIn(0.8f, 1.22f)
        for (i in 0 until toolsContainer.childCount) {
            (toolsContainer.getChildAt(i) as? TextView)?.apply {
                setTextColor(theme.specialKeyTextColor.toInt())
                textSize = toolSp
                layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                    width = toolW
                    height = toolH
                }
            }
        }

        // Update canvas
        canvasView.updateState(layout, settings, theme, shiftState, isLandscape, enterLabelOverride)

        // Update bars visibility with smooth animation ONLY within ALPHA mode; instant on panel toggle for strict fixed height
        val newBarSig = "${settings.showSuggestionsBar}|${settings.showNumberRow}|${settings.showQuickEmojiBar}"
        val stayedInAlpha = prevMode == KeyboardMode.ALPHA && mode == KeyboardMode.ALPHA
        val animateBars = lastBarSignature.isNotEmpty() && lastBarSignature != newBarSig && stayedInAlpha
        lastBarSignature = newBarSig

        val showSug = settings.showSuggestionsBar || mode != KeyboardMode.ALPHA
        val showQuickEmoji = settings.showQuickEmojiBar && mode == KeyboardMode.ALPHA
        setBarHeight(suggestionsBarContainer, if (showSug) suggestionsTargetHeightPx else 0, animateBars)
        setBarHeight(quickEmojiBarRow, if (showQuickEmoji) quickEmojiTargetHeightPx else 0, animateBars)

        // Update suggestions chips
        renderSuggestions(suggestions, theme, toolbarScale, if (mode == KeyboardMode.NUMERIC_GRID) numericContextLabel else null)

        // Render settings square (opposite side) + 9 quick emoji/text squares matching keyboard keys
        renderQuickEmojiSquareBar(settings, theme, toolbarScale)

        reapplyStablePanelGeometry()
    }

    private fun computeStableAlphaTotalHeightPx(): Int {
        val alphaRows = if (currentSettings.showNumberRow) 5 else 4
        val alphaCanvasH = canvasView.computeTargetHeightPx(alphaRows, currentSettings, currentIsLandscape)
        val alphaSugH = if (currentSettings.showSuggestionsBar) suggestionsTargetHeightPx else 0
        val alphaQuickEmojiH = if (currentSettings.showQuickEmojiBar) quickEmojiTargetHeightPx else 0
        return alphaSugH + alphaQuickEmojiH + alphaCanvasH
    }

    private fun reapplyStablePanelGeometry() {
        val theme = currentTheme ?: return
        val toolbarScale = (currentSettings.toolbarScalePercent / 100f).coerceIn(0.70f, 1.45f)
        val isPanelMode = isOverlayPanelMode(currentMode)
        val stableTotalH = computeStableAlphaTotalHeightPx()
        val showSug = currentSettings.showSuggestionsBar || currentMode != KeyboardMode.ALPHA
        val activeSugH = if (showSug) suggestionsTargetHeightPx else 0
        val activeQuickEmojiH = if (currentSettings.showQuickEmojiBar && currentMode == KeyboardMode.ALPHA) quickEmojiTargetHeightPx else 0
        val remainingBodyH = (stableTotalH - activeSugH).coerceAtLeast(dp(150f))

        val panelH: Int
        if (isPanelMode && isSearchTypingActive) {
            panelH = (remainingBodyH * 0.44f).toInt().coerceAtLeast(dp(96f))
            val miniCanvasH = (remainingBodyH - panelH).coerceAtLeast(dp(96f))
            canvasView.setForcedHeightPx(miniCanvasH)
        } else if (isPanelMode) {
            panelH = remainingBodyH
            canvasView.setForcedHeightPx(null)
        } else {
            panelH = remainingBodyH
            val targetCanvasH = if (currentMode == KeyboardMode.NUMERIC_GRID) {
                (stableTotalH - activeSugH - activeQuickEmojiH).coerceAtLeast(dp(150f))
            } else {
                null
            }
            canvasView.setForcedHeightPx(targetCanvasH)
        }

        when (currentMode) {
            KeyboardMode.EMOJI -> emojiPanel.applyConfig(theme, currentSettings.emojiRenderStyle, panelH, toolbarScale)
            KeyboardMode.CLIPBOARD -> clipboardPanel.updateData(
                items = currentClipboardItems,
                theme = theme,
                requireBiometricLock = currentSettings.clipboardBiometricLock,
                gridMode = currentSettings.clipboardGridMode,
                confirmDelete = currentSettings.confirmClipboardDelete,
                heightPx = panelH,
                toolbarScale = toolbarScale,
            )
            KeyboardMode.TRANSLATE -> translatePanel.applyConfig(
                theme,
                currentSettings.translationSourceLang,
                currentSettings.translationTargetLang,
                panelH,
                toolbarScale,
            )
            KeyboardMode.DECORATE -> decoratePanel.updateData(
                inputText = actions.getCurrentFieldTextSnapshot(),
                favorites = currentFavoriteDecorations,
                theme = theme,
                realTimeEnabled = currentSettings.realTimeDecorationEnabled,
                heightPx = panelH,
                toolbarScale = toolbarScale,
            )
            KeyboardMode.CALCULATOR -> calculatorPanel.applyConfig(
                theme = theme,
                numeralStyle = currentSettings.numeralStyle,
                heightPx = panelH,
                toolbarScale = toolbarScale,
                keyCornerRadiusDp = currentSettings.keyCornerRadiusDp,
            )
            KeyboardMode.SYMBOLS_BROWSER -> symbolsPanel.applyConfig(theme, panelH, toolbarScale)
            KeyboardMode.QUICK_EMOJI_EDITOR -> quickEmojiEditorPanel.updateConfig(
                emojis = currentSettings.quickEmojiBarItems,
                sizeSp = currentSettings.quickEmojiSizeSp,
                theme = theme,
                heightPx = panelH,
                toolbarScale = toolbarScale,
            )
            else -> Unit
        }
        updatePanelVisibility()
    }

    private fun isOverlayPanelMode(mode: KeyboardMode): Boolean =
        mode in listOf(
            KeyboardMode.EMOJI,
            KeyboardMode.CLIPBOARD,
            KeyboardMode.TRANSLATE,
            KeyboardMode.DECORATE,
            KeyboardMode.CALCULATOR,
            KeyboardMode.SYMBOLS_BROWSER,
            KeyboardMode.QUICK_EMOJI_EDITOR,
        )

    fun showTranslationPreviewInSuggestions(translatedText: String?) {
        translationPreviewChip = translatedText
        val scale = (currentSettings.toolbarScalePercent / 100f).coerceIn(0.70f, 1.45f)
        currentTheme?.let { renderSuggestions(emptyList(), it, scale, null) }
    }

    private fun updatePanelVisibility() {
        val isPanelMode = isOverlayPanelMode(currentMode)
        panelHost.visibility = if (isPanelMode) View.VISIBLE else View.GONE
        emojiPanel.visibility = if (currentMode == KeyboardMode.EMOJI) View.VISIBLE else View.GONE
        clipboardPanel.visibility = if (currentMode == KeyboardMode.CLIPBOARD) View.VISIBLE else View.GONE
        translatePanel.visibility = if (currentMode == KeyboardMode.TRANSLATE) View.VISIBLE else View.GONE
        symbolsPanel.visibility = if (currentMode == KeyboardMode.SYMBOLS_BROWSER) View.VISIBLE else View.GONE
        decoratePanel.visibility = if (currentMode == KeyboardMode.DECORATE) View.VISIBLE else View.GONE
        calculatorPanel.visibility = if (currentMode == KeyboardMode.CALCULATOR) View.VISIBLE else View.GONE
        quickEmojiEditorPanel.visibility = if (currentMode == KeyboardMode.QUICK_EMOJI_EDITOR) View.VISIBLE else View.GONE

        val showCanvas = !isPanelMode || isSearchTypingActive
        canvasView.visibility = if (showCanvas) View.VISIBLE else View.GONE
    }

    private fun renderQuickEmojiSquareBar(
        settings: KeyboardSettings,
        theme: KeyboardThemeSpec,
        toolbarScale: Float,
    ) {
        quickEmojiBarRow.removeAllViews()
        val items = settings.quickEmojiBarItems.take(9)
        val radius = dp(
            if (theme.isIosStyle) settings.keyCornerRadiusDp.coerceAtLeast(5.5f)
            else settings.keyCornerRadiusDp
        ).toFloat()
        val gapPx = dp((settings.horizontalGapDp / 2f).coerceAtLeast(1.5f))
        val emojiSp = (settings.quickEmojiSizeSp * toolbarScale.coerceIn(0.82f, 1.25f)).coerceIn(12f, 28f)

        // Settings Square placed FIRST so it is on the opposite side of the strip
        val settingsSquare = TextView(context).apply {
            text = "⚙"
            textSize = 17f * toolbarScale.coerceIn(0.8f, 1.25f)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setTextColor(theme.specialKeyTextColor.toInt())
            background = GradientDrawable().apply {
                setColor(theme.specialKeyBackgroundColor.toInt())
                cornerRadius = radius
                setStroke(dp(0.8f), theme.keyShadowColor.toInt())
            }
            setOnClickListener { togglePanel(KeyboardMode.QUICK_EMOJI_EDITOR) }
        }
        val settingsLp = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(gapPx, dp(2f), gapPx, dp(2f))
        }
        quickEmojiBarRow.addView(settingsSquare, settingsLp)

        // 9 Quick Emoji / Custom Text Squares
        for (i in 0 until 9) {
            val em = items.getOrNull(i)
            val isTextEntry = em != null && QuickEmojiEditorPanelView.isCustomTextEntry(em)
            val displaySp = when {
                em == null || !isTextEntry -> emojiSp
                em.length <= 2 -> emojiSp.coerceAtMost(20f)
                em.length <= 5 -> emojiSp.coerceAtMost(12.5f)
                else -> emojiSp.coerceAtMost(9.5f)
            }
            val keySquare = TextView(context).apply {
                text = em.orEmpty()
                textSize = displaySp
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                includeFontPadding = false
                setTypeface(null, if (isTextEntry) Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(2f), dp(1f), dp(2f), dp(1f))
                setTextColor(theme.keyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(if (em != null) theme.keyBackgroundColor.toInt() else theme.specialKeyBackgroundColor.toInt())
                    cornerRadius = radius
                    setStroke(dp(0.8f), theme.keyShadowColor.toInt())
                }
                if (em != null) {
                    setOnClickListener { actions.onQuickEmojiClick(em) }
                }
            }
            val lp = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(gapPx, dp(2f), gapPx, dp(2f))
            }
            quickEmojiBarRow.addView(keySquare, lp)
        }
    }

    private fun renderSuggestions(
        suggestions: List<String>,
        theme: KeyboardThemeSpec,
        toolbarScale: Float,
        numericContextLabel: String?,
    ) {
        suggestionsScrollRow.removeAllViews()
        val chipH = dp(30f * toolbarScale.coerceIn(0.78f, 1.32f))
        val chipSp = 13f * toolbarScale.coerceIn(0.8f, 1.25f)
        val preview = translationPreviewChip
        if (!preview.isNullOrBlank()) {
            val chip = TextView(context).apply {
                text = "🌐 إدراج: $preview"
                textSize = chipSp
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(12f), dp(3f), dp(12f), dp(3f))
                setTextColor(theme.accentKeyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(theme.accentKeyBackgroundColor.toInt())
                    cornerRadius = dp(12f).toFloat()
                }
                setOnClickListener {
                    actions.onTranslationChipClick(preview)
                    translationPreviewChip = null
                }
            }
            suggestionsScrollRow.addView(chip, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, chipH).apply { marginEnd = dp(6f) })
            return
        }

        if (!numericContextLabel.isNullOrBlank()) {
            val badge = TextView(context).apply {
                text = "🔢 $numericContextLabel"
                textSize = (11.5f * toolbarScale.coerceIn(0.8f, 1.22f))
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(10f), dp(2f), dp(10f), dp(2f))
                setTextColor(theme.accentKeyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(theme.accentKeyBackgroundColor.toInt())
                    cornerRadius = dp(10f).toFloat()
                }
                setOnClickListener { actions.onSwitchMode(KeyboardMode.ALPHA) }
            }
            suggestionsScrollRow.addView(badge, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, chipH).apply { marginEnd = dp(6f) })
        }

        suggestions.forEach { word ->
            val chip = TextView(context).apply {
                text = word
                textSize = (13.5f * toolbarScale.coerceIn(0.8f, 1.25f))
                gravity = Gravity.CENTER
                maxLines = 1
                setPadding(dp(12f), dp(3f), dp(12f), dp(3f))
                setTextColor(theme.keyTextColor.toInt())
                background = GradientDrawable().apply {
                    setColor(theme.keyBackgroundColor.toInt())
                    cornerRadius = dp(10f).toFloat()
                }
                setOnClickListener { actions.onSuggestionClick(word) }
            }
            suggestionsScrollRow.addView(chip, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, chipH).apply { marginEnd = dp(5f) })
        }
    }

    private fun setBarHeight(view: View, targetPx: Int, animate: Boolean) {
        val lp = view.layoutParams ?: return
        if (lp.height == targetPx) return
        if (!animate) {
            lp.height = targetPx
            view.layoutParams = lp
            view.visibility = if (targetPx == 0) View.GONE else View.VISIBLE
            return
        }
        val startH = if (view.visibility == View.GONE) 0 else view.height
        if (targetPx > 0) view.visibility = View.VISIBLE
        ValueAnimator.ofInt(startH, targetPx).apply {
            duration = 170L
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val v = anim.animatedValue as Int
                view.layoutParams = view.layoutParams.apply { height = v }
                if (v == 0 && targetPx == 0) {
                    view.visibility = View.GONE
                }
            }
            start()
        }
    }

    private fun makeRailButton(icon: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = icon
            textSize = 18f
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }

    fun getTouchTargetBoundsView(): View = bodyWrapper

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
}
