package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import com.example.MainActivity
import com.example.data.db.ClipboardItemEntity
import com.example.data.db.ClipboardRepository
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.db.KhattDatabase
import com.example.data.decorate.DecorationFrame
import com.example.data.decorate.DecorationStyle
import com.example.data.decorate.TextDecorator
import com.example.data.dictionary.SuggestionEngine
import com.example.data.layout.LayoutRepository
import com.example.data.layout.NumericContextType
import com.example.data.layout.NumericFieldDetector
import com.example.data.prefs.SettingsRepository
import com.example.data.translate.TranslationRepository
import com.example.model.HandMode
import com.example.model.KeySpec
import com.example.model.KeyboardMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePresets
import com.example.model.LanguageId
import com.example.model.ShiftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KhattInputMethodService : InputMethodService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var layoutRepo: LayoutRepository
    private lateinit var db: KhattDatabase
    private lateinit var clipboardRepo: ClipboardRepository
    private lateinit var suggestionEngine: SuggestionEngine
    private lateinit var translationRepo: TranslationRepository

    private var containerView: KeyboardContainerView? = null
    private var currentSettings: KeyboardSettings = KeyboardSettings()
    private var currentMode: KeyboardMode = KeyboardMode.ALPHA
    private var shiftState: ShiftState = ShiftState.OFF
    private var lastShiftTapMs: Long = 0L
    private var isPasswordField: Boolean = false
    private var isUrlOrEmailField: Boolean = false
    private var currentEditorInfo: EditorInfo? = null
    private var currentSuggestions: List<String> = emptyList()
    private var latestClipboard: List<ClipboardItemEntity> = emptyList()
    private var latestFavorites: List<FavoriteDecorationEntity> = emptyList()
    private var searchInputActive: Boolean = false
    private var detectedNumericContext: NumericContextType = NumericContextType.NONE
    private var userOverrodeNumericGrid: Boolean = false
    private var lastFieldKey: String = ""
    private var lastExternalFieldTapMs: Long = 0L
    private var lastViewClickedMs: Long = 0L
    private var ignoreSelectionChangeUntilMs: Long = 0L
    private var lastSelStart: Int = -1
    private var lastSelEnd: Int = -1
    private var lastSelfCopiedText: String? = null
    private var lastSelfCopyMs: Long = 0L

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleSystemClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository.getInstance(this)
        layoutRepo = LayoutRepository(this)
        db = KhattDatabase.getInstance(this)
        clipboardRepo = ClipboardRepository(db.clipboardDao())
        suggestionEngine = SuggestionEngine(db.userDictionaryDao())
        translationRepo = TranslationRepository()

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)

        serviceScope.launch {
            combine(
                settingsRepo.settingsFlow,
                clipboardRepo.allItems,
                db.favoriteDecorationDao().observeAll(),
                BiometricLockManager.isSessionUnlocked,
            ) { settings, clips, favs, _ ->
                Triple(settings, clips, favs)
            }.collect { (settings, clips, favs) ->
                val langChanged = currentSettings.activeLanguage != settings.activeLanguage
                currentSettings = settings
                latestClipboard = clips
                latestFavorites = favs
                if (langChanged) refreshSuggestions()
                renderContainer()
            }
        }
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val view = KeyboardContainerView(
            context = this,
            supportedTranslationLanguages = translationRepo.supportedLanguages,
            canvasListener = canvasListener,
            actions = containerActions,
        )
        containerView = view
        refreshSuggestions()
        renderContainer()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        isPasswordField = isSensitiveInput(info)
        isUrlOrEmailField = isUrlOrEmailInput(info)
        val fieldKey = "${info?.packageName.orEmpty()}|${info?.fieldId ?: 0}|${info?.inputType ?: 0}|${info?.hintText?.toString().orEmpty()}"
        if (!restarting || fieldKey != lastFieldKey) {
            userOverrodeNumericGrid = false
            lastFieldKey = fieldKey
        }
        detectedNumericContext = if (currentSettings.smartNumericGridEnabled) {
            NumericFieldDetector.detect(info)
        } else {
            NumericContextType.NONE
        }
        currentMode = defaultKeyboardModeForField()
        searchInputActive = false
        shiftState = ShiftState.OFF
        lastExternalFieldTapMs = 0L
        lastViewClickedMs = 0L
        lastSelStart = info?.initialSelStart ?: -1
        lastSelEnd = info?.initialSelEnd ?: -1
        ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 300L
        updateAutoCapsState()
        refreshSuggestions()
        renderContainer()
    }

    override fun onViewClicked(focusChanged: Boolean) {
        super.onViewClicked(focusChanged)
        val now = System.currentTimeMillis()
        lastViewClickedMs = now
        if (!focusChanged) {
            registerExternalFieldTap(now)
        } else {
            lastExternalFieldTapMs = 0L
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        val now = System.currentTimeMillis()
        val selChanged = newSelStart != lastSelStart || newSelEnd != lastSelEnd
        lastSelStart = newSelStart
        lastSelEnd = newSelEnd

        if (now < ignoreSelectionChangeUntilMs) return
        if (now - lastViewClickedMs < 60L) return

        if (selChanged && currentMode == KeyboardMode.CLIPBOARD && currentSettings.clipboardAutoClose) {
            registerExternalFieldTap(now)
        }
    }

    private fun registerExternalFieldTap(now: Long) {
        if (currentMode != KeyboardMode.CLIPBOARD || !currentSettings.clipboardAutoClose) {
            lastExternalFieldTapMs = now
            return
        }
        val elapsed = now - lastExternalFieldTapMs
        if (lastExternalFieldTapMs > 0L && elapsed in 55L..480L) {
            lastExternalFieldTapMs = 0L
            searchInputActive = false
            currentMode = defaultKeyboardModeForField()
            renderContainer()
        } else {
            lastExternalFieldTapMs = now
        }
    }

    private fun defaultKeyboardModeForField(): KeyboardMode =
        if (currentSettings.smartNumericGridEnabled &&
            detectedNumericContext != NumericContextType.NONE &&
            !userOverrodeNumericGrid
        ) {
            KeyboardMode.NUMERIC_GRID
        } else {
            KeyboardMode.ALPHA
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        renderContainer()
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val cv = containerView ?: return
        if (currentSettings.handMode == HandMode.FLOATING) {
            val target = cv.getTouchTargetBoundsView()
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            outInsets.contentTopInsets = outInsets.visibleTopInsets
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(
                loc[0],
                loc[1],
                loc[0] + target.width,
                loc[1] + target.height,
            )
        }
    }

    private fun isSensitiveInput(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val cls = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return when (cls) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    private fun isUrlOrEmailInput(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val cls = inputType and InputType.TYPE_MASK_CLASS
        if (cls != InputType.TYPE_CLASS_TEXT) return false
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_URI ||
            variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
    }

    private fun handleSystemClipboardChanged() {
        if (!currentSettings.clipboardAutoSave || isPasswordField) return
        val cm = clipboardManager ?: return
        val desc = cm.primaryClipDescription
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && desc != null) {
            if (desc.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) return
        }
        val item = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString() ?: return
        if (item.isBlank()) return
        val now = System.currentTimeMillis()
        if (lastSelfCopiedText == item.trim() && (now - lastSelfCopyMs) < 1500L) {
            lastSelfCopiedText = null
            return
        }
        serviceScope.launch(Dispatchers.IO) {
            clipboardRepo.saveClip(
                rawText = item,
                maxItems = currentSettings.clipboardMaxItems,
                retentionDays = currentSettings.clipboardRetentionDays,
            )
        }
    }

    private fun renderContainer() {
        val cv = containerView ?: return
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val theme = KeyboardThemePresets.resolveTheme(
            context = this,
            selectedThemeId = currentSettings.selectedThemeId,
            autoSystemDark = currentSettings.autoSystemDarkTheme,
            isSystemDark = isSystemDark,
            customTheme = currentSettings.customTheme,
        )

        val effectiveMode = if (searchInputActive) KeyboardMode.ALPHA else currentMode
        val layout = layoutRepo.buildLayout(
            settings = currentSettings,
            mode = effectiveMode,
            includeNumberRow = currentSettings.showNumberRow && effectiveMode == KeyboardMode.ALPHA,
            numericContextType = detectedNumericContext,
        )

        val enterLabel = when ((currentEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> "بحث"
            EditorInfo.IME_ACTION_SEND -> "إرسال"
            EditorInfo.IME_ACTION_GO -> "انتقال"
            EditorInfo.IME_ACTION_NEXT -> "التالي"
            EditorInfo.IME_ACTION_DONE -> "تم"
            else -> "↵"
        }

        val visibleSuggestions = if (isPasswordField || !currentSettings.suggestionsEnabled || effectiveMode == KeyboardMode.NUMERIC_GRID) {
            emptyList()
        } else {
            currentSuggestions
        }

        cv.applyState(
            mode = currentMode,
            layout = layout,
            settings = currentSettings,
            theme = theme,
            shiftState = shiftState,
            isLandscape = isLandscape,
            suggestions = visibleSuggestions,
            clipboardItems = latestClipboard,
            favoriteDecorations = latestFavorites,
            enterLabelOverride = enterLabel,
            numericContextLabel = if (effectiveMode == KeyboardMode.NUMERIC_GRID) detectedNumericContext.arabicLabel else null,
        )
    }

    private fun updateAutoCapsState() {
        if (currentSettings.activeLanguage.isRtl || shiftState == ShiftState.CAPS_LOCK) return
        val ic = currentInputConnection ?: return
        val info = currentEditorInfo ?: return
        val capsMode = ic.getCursorCapsMode(info.inputType)
        shiftState = if (capsMode != 0) ShiftState.ONCE else ShiftState.OFF
    }

    private fun getCurrentWordBeforeCursor(): Pair<String, String> {
        val ic = currentInputConnection ?: return "" to ""
        val before = ic.getTextBeforeCursor(40, 0)?.toString().orEmpty()
        if (before.isEmpty()) return "" to ""
        val token = before.takeLastWhile { !it.isWhitespace() }
        val rest = before.dropLast(token.length).trimEnd()
        val prevWord = rest.takeLastWhile { !it.isWhitespace() }
        return token to prevWord
    }

    private fun refreshSuggestions() {
        if (isPasswordField || !currentSettings.suggestionsEnabled) {
            currentSuggestions = emptyList()
            renderContainer()
            return
        }
        val (word, prev) = getCurrentWordBeforeCursor()
        val cleanWord = word.replace(TextDecorator.FAWAZ_ORNAMENT, "")
        val cleanPrev = prev.replace(TextDecorator.FAWAZ_ORNAMENT, "")
        serviceScope.launch {
            val sug = withContext(Dispatchers.IO) {
                suggestionEngine.getSuggestions(currentSettings.activeLanguage, cleanWord, cleanPrev)
            }
            currentSuggestions = sug
            renderContainer()
        }
    }

    private fun commitTextAndLearn(text: String) {
        val ic = currentInputConnection ?: return
        if (searchInputActive) {
            when (currentMode) {
                KeyboardMode.EMOJI -> containerView?.emojiPanel?.appendSearchChar(text)
                KeyboardMode.SYMBOLS_BROWSER -> containerView?.symbolsPanel?.appendSearchChar(text)
                KeyboardMode.QUICK_EMOJI_EDITOR -> containerView?.quickEmojiEditorPanel?.appendCustomText(text)
                else -> Unit
            }
            return
        }
        ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
        val finalCommit = if (
            currentMode == KeyboardMode.ALPHA &&
            currentSettings.realTimeDecorationEnabled &&
            currentSettings.activeLanguage == LanguageId.ARABIC &&
            !isPasswordField &&
            !isUrlOrEmailField
        ) {
            val before = ic.getTextBeforeCursor(TextDecorator.FAWAZ_ORNAMENT.length + 6, 0)?.toString().orEmpty()
            TextDecorator.formatRealtimeFawazInsertion(before, text)
        } else {
            text
        }
        ic.commitText(finalCommit, 1)
        if (shiftState == ShiftState.ONCE) {
            shiftState = ShiftState.OFF
        }
        refreshSuggestions()
    }

    private fun handleWordSeparator(separator: String) {
        val ic = currentInputConnection ?: return
        if (searchInputActive) {
            if (separator == " ") {
                when (currentMode) {
                    KeyboardMode.EMOJI -> containerView?.emojiPanel?.appendSearchChar(" ")
                    KeyboardMode.SYMBOLS_BROWSER -> containerView?.symbolsPanel?.appendSearchChar(" ")
                    KeyboardMode.QUICK_EMOJI_EDITOR -> containerView?.quickEmojiEditorPanel?.appendCustomText(" ")
                    else -> Unit
                }
            }
            return
        }

        ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
        val (word, _) = getCurrentWordBeforeCursor()
        if (word.isNotEmpty() && !isPasswordField) {
            val cleanWord = word.replace(TextDecorator.FAWAZ_ORNAMENT, "")
            val hasOrnament = word.contains(TextDecorator.FAWAZ_ORNAMENT)
            serviceScope.launch {
                val corrected = if (currentSettings.autoCorrectEnabled && separator == " " && !hasOrnament) {
                    withContext(Dispatchers.IO) {
                        suggestionEngine.getAutoCorrectCandidate(currentSettings.activeLanguage, word)
                    }
                } else {
                    null
                }
                ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
                if (corrected != null && corrected != word) {
                    ic.deleteSurroundingText(word.length, 0)
                    ic.commitText("$corrected$separator", 1)
                    withContext(Dispatchers.IO) {
                        suggestionEngine.learnWord(corrected, currentSettings.activeLanguage)
                    }
                } else {
                    ic.commitText(separator, 1)
                    if (cleanWord.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            suggestionEngine.learnWord(cleanWord, currentSettings.activeLanguage)
                        }
                    }
                }
                updateAutoCapsState()
                refreshSuggestions()
            }
        } else {
            ic.commitText(separator, 1)
            updateAutoCapsState()
            refreshSuggestions()
        }
    }

    private fun performKeyFeedback(isSpecial: Boolean) {
        if (currentSettings.hapticEnabled) {
            vibrateMs(currentSettings.hapticStrengthMs.toLong())
        }
        if (currentSettings.soundEnabled) {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val effect = if (isSpecial) AudioManager.FX_KEYPRESS_RETURN else AudioManager.FX_KEYPRESS_STANDARD
            am?.playSoundEffect(effect, 0.6f)
        }
    }

    private fun vibrateMs(ms: Long) {
        val duration = ms.coerceIn(5L, 60L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vib = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(duration)
            }
        }
    }

    private fun handleBackspaceTap() {
        if (searchInputActive) {
            when (currentMode) {
                KeyboardMode.EMOJI -> containerView?.emojiPanel?.backspaceSearchChar()
                KeyboardMode.SYMBOLS_BROWSER -> containerView?.symbolsPanel?.backspaceSearchChar()
                KeyboardMode.QUICK_EMOJI_EDITOR -> containerView?.quickEmojiEditorPanel?.backspaceCustomText()
                else -> Unit
            }
            return
        }
        val ic = currentInputConnection ?: return
        ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            val tail = ic.getTextBeforeCursor(TextDecorator.FAWAZ_ORNAMENT.length + 2, 0)?.toString().orEmpty()
            if (tail.endsWith(TextDecorator.FAWAZ_ORNAMENT)) {
                ic.deleteSurroundingText(TextDecorator.FAWAZ_ORNAMENT.length, 0)
            } else if (tail.length >= 2 && Character.isSurrogatePair(tail[tail.length - 2], tail[tail.length - 1])) {
                ic.deleteSurroundingText(2, 0)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        }
        updateAutoCapsState()
        refreshSuggestions()
    }

    private fun handleOpenSettings() {
        val intent = Intent(this@KhattInputMethodService, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private val canvasListener: KeyboardCanvasView.Listener = object : KeyboardCanvasView.Listener {
        override fun onCharacterKey(key: KeySpec, text: String) {
            commitTextAndLearn(text)
        }

        override fun onBackspaceTap() {
            handleBackspaceTap()
        }

        override fun onBackspaceWord() {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            val before = ic.getTextBeforeCursor(32, 0)?.toString().orEmpty()
            if (before.isEmpty()) return
            val trimmedEnd = before.trimEnd()
            val wordLen = trimmedEnd.takeLastWhile { !it.isWhitespace() }.length
            val delCount = (before.length - trimmedEnd.length) + wordLen.coerceAtLeast(1)
            ic.deleteSurroundingText(delCount, 0)
            refreshSuggestions()
        }

        override fun onEnterTap() {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            val info = currentEditorInfo
            val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
            val isMultiLine = (info?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
            if (!isMultiLine && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            refreshSuggestions()
        }

        override fun onSpaceTap() {
            handleWordSeparator(" ")
        }

        override fun onShiftTap() {
            shiftState = when (shiftState) {
                ShiftState.OFF -> ShiftState.ONCE
                ShiftState.ONCE -> ShiftState.OFF
                ShiftState.CAPS_LOCK -> ShiftState.OFF
            }
            lastShiftTapMs = System.currentTimeMillis()
            renderContainer()
        }

        override fun onShiftLongPress() {
            shiftState = if (shiftState == ShiftState.CAPS_LOCK) ShiftState.OFF else ShiftState.CAPS_LOCK
            if (currentSettings.hapticEnabled) vibrateMs(18L)
            renderContainer()
        }

        override fun onSymbolsToggle() {
            currentMode = when (currentMode) {
                KeyboardMode.NUMERIC_GRID -> {
                    userOverrodeNumericGrid = true
                    KeyboardMode.ALPHA
                }
                KeyboardMode.ALPHA -> KeyboardMode.SYMBOLS_1
                else -> KeyboardMode.ALPHA
            }
            renderContainer()
        }

        override fun onSymbolsExtToggle() {
            currentMode = if (currentMode == KeyboardMode.SYMBOLS_1) KeyboardMode.SYMBOLS_2 else KeyboardMode.SYMBOLS_1
            renderContainer()
        }

        override fun onLanguageTap() {
            serviceScope.launch {
                settingsRepo.cycleLanguage(1)
            }
        }

        override fun onLanguageLongPress() {
            handleOpenSettings()
        }

        override fun onEmojiTap() {
            currentMode = if (currentMode == KeyboardMode.EMOJI) defaultKeyboardModeForField() else KeyboardMode.EMOJI
            renderContainer()
        }

        override fun onClipboardTap() {
            searchInputActive = false
            lastExternalFieldTapMs = 0L
            currentMode = if (currentMode == KeyboardMode.CLIPBOARD) defaultKeyboardModeForField() else KeyboardMode.CLIPBOARD
            renderContainer()
        }

        override fun onSpaceHorizontalSwipe(direction: Int) {
            if (currentSettings.hapticOnSpaceSwipe) vibrateMs(10L)
            serviceScope.launch {
                val target = settingsRepo.cycleLanguage(direction)
                containerView?.canvasView?.setSpacebarPreviewLabel(target.nativeName)
            }
        }

        override fun onSpaceHorizontalDragPreview(targetLabel: String?) {
            containerView?.canvasView?.setSpacebarPreviewLabel(targetLabel)
        }

        override fun onSpaceVerticalSwipe(isUp: Boolean) {
            if (currentSettings.hapticOnSpaceSwipe) vibrateMs(12L)
            serviceScope.launch {
                settingsRepo.handleSpacebarVerticalSwipe(isUp)
            }
        }

        override fun onKeyPressFeedback(isSpecial: Boolean) {
            performKeyFeedback(isSpecial)
        }
    }

    private val containerActions = object : KeyboardContainerView.ContainerActions {
        override fun onSuggestionClick(word: String) {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            val (current, _) = getCurrentWordBeforeCursor()
            if (current.isNotEmpty()) {
                ic.deleteSurroundingText(current.length, 0)
            }
            ic.commitText("$word ", 1)
            serviceScope.launch(Dispatchers.IO) {
                suggestionEngine.learnWord(word, currentSettings.activeLanguage)
            }
            refreshSuggestions()
        }

        override fun onTranslationChipClick(text: String) {
            onReplaceText(text)
        }

        override fun onQuickEmojiClick(emoji: String) {
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            currentInputConnection?.commitText(emoji, 1)
        }

        override fun onSwitchMode(mode: KeyboardMode) {
            if (mode == KeyboardMode.ALPHA && currentMode == KeyboardMode.NUMERIC_GRID) {
                userOverrodeNumericGrid = true
            }
            if (mode == KeyboardMode.CLIPBOARD) {
                lastExternalFieldTapMs = 0L
            }
            currentMode = mode
            searchInputActive = false
            renderContainer()
        }

        override fun onCycleHandMode() {
            val next = when (currentSettings.handMode) {
                HandMode.FULL -> HandMode.ONE_HAND_RIGHT
                HandMode.ONE_HAND_RIGHT -> HandMode.ONE_HAND_LEFT
                HandMode.ONE_HAND_LEFT -> HandMode.FLOATING
                HandMode.FLOATING -> HandMode.FULL
            }
            onSetHandMode(next)
        }

        override fun onSetHandMode(mode: HandMode) {
            serviceScope.launch {
                settingsRepo.update { it.copy(handMode = mode) }
            }
        }

        override fun onUpdateFloatingPosition(offsetX: Float, offsetY: Float) {
            serviceScope.launch {
                settingsRepo.update { it.copy(floatingOffsetX = offsetX, floatingOffsetY = offsetY) }
            }
        }

        override fun onOpenSettings() {
            handleOpenSettings()
        }

        override fun onPasteClip(text: String) {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 450L
            ic.finishComposingText()
            ic.commitText(text, 1)
            if (currentSettings.clipboardAutoClose && currentMode == KeyboardMode.CLIPBOARD) {
                searchInputActive = false
                lastExternalFieldTapMs = 0L
                currentMode = defaultKeyboardModeForField()
                renderContainer()
            }
            refreshSuggestions()
        }

        override fun onCopyClip(text: String) {
            lastSelfCopiedText = text.trim()
            lastSelfCopyMs = System.currentTimeMillis()
            ignoreSelectionChangeUntilMs = lastSelfCopyMs + 450L
            if (currentMode == KeyboardMode.CLIPBOARD) {
                searchInputActive = false
                lastExternalFieldTapMs = 0L
                currentMode = defaultKeyboardModeForField()
                renderContainer()
            }
        }

        override fun onTogglePinClip(item: ClipboardItemEntity) {
            serviceScope.launch(Dispatchers.IO) { clipboardRepo.togglePin(item) }
        }

        override fun onDeleteClip(id: Long) {
            serviceScope.launch(Dispatchers.IO) { clipboardRepo.deleteItem(id) }
        }

        override fun onDeleteSelectedClips(ids: List<Long>) {
            serviceScope.launch(Dispatchers.IO) { clipboardRepo.deleteItems(ids) }
        }

        override fun onClearUnpinnedClips() {
            serviceScope.launch(Dispatchers.IO) { clipboardRepo.clearAllUnpinned() }
        }

        override fun onToggleClipboardGridMode(gridMode: Boolean) {
            serviceScope.launch {
                settingsRepo.update { it.copy(clipboardGridMode = gridMode) }
            }
        }

        override fun onTranslateRequest(source: String, target: String, selectedOnly: Boolean) {
            val ic = currentInputConnection ?: return
            val textToTranslate = if (selectedOnly) {
                ic.getSelectedText(0)?.toString().orEmpty()
            } else {
                ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString().orEmpty()
                    .ifBlank { ic.getTextBeforeCursor(500, 0)?.toString().orEmpty() }
            }
            if (textToTranslate.isBlank()) {
                containerView?.translatePanel?.showError("لا يوجد نص لترجمته في الحقل الحالي")
                return
            }
            containerView?.translatePanel?.setTranslatingState()
            serviceScope.launch {
                settingsRepo.update {
                    it.copy(translationSourceLang = source, translationTargetLang = target)
                }
                val result = translationRepo.translate(
                    text = textToTranslate,
                    sourceCode = source,
                    targetCode = target,
                    allowCloudFallback = currentSettings.cloudTranslateFallbackEnabled,
                )
                result.onSuccess { res ->
                    containerView?.translatePanel?.showTranslationResult(res.translatedText, res.detectedSourceCode, res.usedOfflineMlKit)
                    containerView?.showTranslationPreviewInSuggestions(res.translatedText)
                }.onFailure { err ->
                    containerView?.translatePanel?.showError(err.message ?: "تعذرت الترجمة")
                }
            }
        }

        override fun onReplaceText(newText: String) {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 450L
            ic.finishComposingText()
            val sel = ic.getSelectedText(0)
            if (!sel.isNullOrEmpty()) {
                ic.commitText(newText, 1)
            } else {
                val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text
                if (!extracted.isNullOrEmpty()) {
                    ic.setSelection(0, extracted.length)
                    ic.commitText(newText, 1)
                } else {
                    val before = ic.getTextBeforeCursor(500, 0)?.toString().orEmpty()
                    if (before.isNotEmpty()) ic.deleteSurroundingText(before.length, 0)
                    ic.commitText(newText, 1)
                }
            }
            currentMode = defaultKeyboardModeForField()
            renderContainer()
        }

        override fun onInsertText(text: String) {
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            currentInputConnection?.commitText(text, 1)
            currentMode = defaultKeyboardModeForField()
            renderContainer()
        }

        override fun onApplyFawazDecoration() {
            val ic = currentInputConnection
            if (ic == null) {
                currentMode = defaultKeyboardModeForField()
                renderContainer()
                return
            }
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 450L
            val sel = ic.getSelectedText(0)?.toString()
            if (!sel.isNullOrEmpty()) {
                onReplaceText(TextDecorator.decorateFawaz(sel))
                return
            }
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString()
            if (!extracted.isNullOrEmpty()) {
                onReplaceText(TextDecorator.decorateFawaz(extracted))
                return
            }
            val beforeFull = ic.getTextBeforeCursor(500, 0)?.toString().orEmpty()
            val afterFull = ic.getTextAfterCursor(500, 0)?.toString().orEmpty()
            if (beforeFull.isNotEmpty() || afterFull.isNotEmpty()) {
                val combined = beforeFull + afterFull
                val decorated = TextDecorator.decorateFawaz(combined)
                ic.finishComposingText()
                if (beforeFull.isNotEmpty()) ic.deleteSurroundingText(beforeFull.length, afterFull.length)
                else ic.deleteSurroundingText(0, afterFull.length)
                ic.commitText(decorated, 1)
            }
            currentMode = defaultKeyboardModeForField()
            renderContainer()
        }

        override fun onToggleRealtimeDecoration(enabled: Boolean) {
            serviceScope.launch {
                settingsRepo.update { it.copy(realTimeDecorationEnabled = enabled) }
            }
        }

        override fun onToggleFavoriteDecoration(style: DecorationStyle, frame: DecorationFrame) {
            serviceScope.launch(Dispatchers.IO) {
                val existing = latestFavorites.firstOrNull {
                    it.styleId == style.id && it.prefix == frame.prefix && it.suffix == frame.suffix
                }
                if (existing != null) {
                    db.favoriteDecorationDao().deleteById(existing.id)
                } else {
                    db.favoriteDecorationDao().insert(
                        FavoriteDecorationEntity(
                            title = style.nameAr,
                            styleId = style.id,
                            prefix = frame.prefix,
                            suffix = frame.suffix,
                        )
                    )
                }
            }
        }

        override fun onUpdateQuickEmojis(emojis: List<String>) {
            serviceScope.launch {
                settingsRepo.update { it.copy(quickEmojiBarItems = emojis.take(9)) }
            }
        }

        override fun onUpdateQuickEmojiSizeSp(sizeSp: Float) {
            serviceScope.launch {
                settingsRepo.update { it.copy(quickEmojiSizeSp = sizeSp.coerceIn(13f, 28f)) }
            }
        }

        override fun onResetQuickEmojiDefaults() {
            serviceScope.launch {
                val def = KeyboardSettings()
                settingsRepo.update {
                    it.copy(
                        quickEmojiBarItems = def.quickEmojiBarItems,
                        quickEmojiSizeSp = def.quickEmojiSizeSp,
                    )
                }
            }
        }

        override fun onSearchInputStateChanged(active: Boolean) {
            searchInputActive = active
            renderContainer()
        }

        override fun onEmojiPanelCommit(emoji: String) {
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            currentInputConnection?.commitText(emoji, 1)
        }

        override fun onSymbolPanelCommit(symbol: String) {
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 350L
            currentInputConnection?.commitText(symbol, 1)
        }

        override fun onCalculatorInsert(text: String) {
            val ic = currentInputConnection ?: return
            ignoreSelectionChangeUntilMs = System.currentTimeMillis() + 400L
            ic.finishComposingText()
            ic.commitText(text, 1)
            currentMode = defaultKeyboardModeForField()
            refreshSuggestions()
            renderContainer()
        }

        override fun onPanelBackspace() {
            handleBackspaceTap()
        }

        override fun getCurrentFieldTextSnapshot(): String {
            val ic = currentInputConnection ?: return ""
            val sel = ic.getSelectedText(0)?.toString()
            if (!sel.isNullOrBlank()) return sel
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString()
            if (!extracted.isNullOrBlank()) return extracted
            return ic.getTextBeforeCursor(200, 0)?.toString().orEmpty()
        }
    }
}
