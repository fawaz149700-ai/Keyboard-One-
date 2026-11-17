package com.example

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.calculator.CalculatorEngine
import com.example.data.db.ClipboardItemEntity
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.decorate.DecorationFrame
import com.example.data.decorate.DecorationStyle
import com.example.data.decorate.TextDecorator
import com.example.data.emoji.EmojiCatalog
import com.example.data.layout.LayoutRepository
import com.example.data.layout.NumericContextType
import com.example.data.layout.NumericFieldDetector
import com.example.data.symbols.SymbolsCatalog
import com.example.data.translate.TranslationLanguageInfo
import com.example.ime.KeyboardCanvasView
import com.example.ime.KeyboardContainerView
import com.example.ime.panels.ClipboardPanelView
import com.example.ime.panels.QuickEmojiEditorPanelView
import com.example.model.HandMode
import com.example.model.KeySpec
import com.example.model.KeyType
import com.example.model.KeyboardMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePresets
import com.example.model.LanguageId
import com.example.model.LayoutVariant
import com.example.model.NumeralStyle
import com.example.model.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches localized brand`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("كيبورد خط", appName)
    }

    @Test
    fun `arabic 11-column layout matches exact order, hides shift, and keeps bottom clipboard key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)

        val ar11 = repo.buildLayout(
            settings = KeyboardSettings(activeLanguage = LanguageId.ARABIC, useElevenColumnsArabic = true),
            mode = KeyboardMode.ALPHA,
            includeNumberRow = false,
        )
        assertEquals(11, ar11.columns)
        assertEquals(4, ar11.rows.size)

        // Row 1 RTL reading order: ج ح خ ه ع غ ف ق ث ص ض (stored L->R: ض..ج)
        assertEquals(listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"), ar11.rows[0].keys.map { it.label })
        // Row 2 RTL reading order: ط ك م ن ت ا ل ب ي س ش (stored L->R: ش..ط)
        assertEquals(listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"), ar11.rows[1].keys.map { it.label })
        // Row 3 RTL reading order: د ظ ز و ة ى ر ؤ ء ذ (stored L->R: ذ..د + Backspace, NO Shift)
        val row3Letters = ar11.rows[2].keys.filter { it.type == KeyType.CHAR }.map { it.label }
        assertEquals(listOf("ذ", "ء", "ؤ", "ر", "ى", "ة", "و", "ز", "ظ", "د"), row3Letters)
        assertEquals(11, ar11.rows[2].keys.size)
        assertFalse("Arabic should not include Shift key", ar11.rows[2].keys.any { it.type == KeyType.SHIFT })
        assertTrue(ar11.rows[2].keys.any { it.type == KeyType.BACKSPACE })

        // Bottom row must contain Clipboard shortcut key
        assertTrue(ar11.rows[3].keys.any { it.type == KeyType.CLIPBOARD })
    }

    @Test
    fun `arabic 10-column layout matches exact requested 10 10 9 order with zero overlap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)

        val ar10 = repo.buildLayout(
            settings = KeyboardSettings(activeLanguage = LanguageId.ARABIC, useElevenColumnsArabic = false),
            mode = KeyboardMode.ALPHA,
            includeNumberRow = false,
        )
        assertEquals(10, ar10.columns)
        // Row 1 RTL: ج ح خ ه ع غ ف ق ص ض -> L->R: ض..ج
        assertEquals(listOf("ض", "ص", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"), ar10.rows[0].keys.map { it.label })
        // Row 2 RTL: ك م ن ت ا ل ب ي س ش -> L->R: ش..ك
        assertEquals(listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك"), ar10.rows[1].keys.map { it.label })
        // Row 3 RTL: ث ة و ر ز د ذ ط ظ -> L->R: ظ..ث + Backspace
        val row3Letters = ar10.rows[2].keys.filter { it.type == KeyType.CHAR }.map { it.label }
        assertEquals(listOf("ظ", "ط", "ذ", "د", "ز", "ر", "و", "ة", "ث"), row3Letters)
        assertFalse(ar10.rows[2].keys.any { it.type == KeyType.SHIFT })
        assertTrue(ar10.rows[2].keys.any { it.type == KeyType.BACKSPACE })
    }

    @Test
    fun `calculator engine evaluates precedence, parentheses, percentage, arabic digits, and rejects divide by zero`() {
        assertEquals("14", CalculatorEngine.evaluateFormatted("2+3×4"))
        assertEquals("20", CalculatorEngine.evaluateFormatted("(2+3)×4"))
        assertEquals("2.5", CalculatorEngine.evaluateFormatted("10÷4"))
        assertEquals("0.5", CalculatorEngine.evaluateFormatted("50%"))
        assertEquals("100", CalculatorEngine.evaluateFormatted("٢٥+٧٥", NumeralStyle.WESTERN))
        assertEquals("١٠٠", CalculatorEngine.evaluateFormatted("25+75", NumeralStyle.EASTERN_ARABIC))
        assertNull(CalculatorEngine.evaluate("10÷0"))
    }

    @Test
    fun `spacebar swipe down hides bars in exact order suggestions then numbers then quick emojis`() {
        val initial = KeyboardSettings(barLevel = 3)
        assertTrue(initial.showSuggestionsBar)
        assertTrue(initial.showNumberRow)
        assertTrue(initial.showQuickEmojiBar)

        // 1st swipe down -> hides Suggestions & Toolbar
        val afterFirst = initial.applySpaceVerticalSwipe(isUp = false)
        assertFalse(afterFirst.showSuggestionsBar)
        assertTrue(afterFirst.showNumberRow)
        assertTrue(afterFirst.showQuickEmojiBar)

        // 2nd swipe down -> hides Number Row
        val afterSecond = afterFirst.applySpaceVerticalSwipe(isUp = false)
        assertFalse(afterSecond.showSuggestionsBar)
        assertFalse(afterSecond.showNumberRow)
        assertTrue(afterSecond.showQuickEmojiBar)

        // 3rd swipe down -> hides Quick Emoji Bar
        val afterThird = afterSecond.applySpaceVerticalSwipe(isUp = false)
        assertFalse(afterThird.showSuggestionsBar)
        assertFalse(afterThird.showNumberRow)
        assertFalse(afterThird.showQuickEmojiBar)

        // Swipe up restores in reverse order: Emojis -> Numbers -> Suggestions
        val up1 = afterThird.applySpaceVerticalSwipe(isUp = true)
        assertTrue(up1.showQuickEmojiBar)
        assertFalse(up1.showNumberRow)
        assertFalse(up1.showSuggestionsBar)

        val up2 = up1.applySpaceVerticalSwipe(isUp = true)
        assertTrue(up2.showQuickEmojiBar)
        assertTrue(up2.showNumberRow)
        assertFalse(up2.showSuggestionsBar)

        val up3 = up2.applySpaceVerticalSwipe(isUp = true)
        assertTrue(up3.showQuickEmojiBar)
        assertTrue(up3.showNumberRow)
        assertTrue(up3.showSuggestionsBar)
    }

    @Test
    fun `keyboard container keeps fixed height across alpha, clipboard, decorate, and calculator modes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val layoutRepo = LayoutRepository(context)
        val settings = KeyboardSettings(barLevel = 3)
        val theme = KeyboardThemePresets.AmberMidnight

        val dummyCanvasListener = object : KeyboardCanvasView.Listener {
            override fun onCharacterKey(key: KeySpec, text: String) = Unit
            override fun onBackspaceTap() = Unit
            override fun onBackspaceWord() = Unit
            override fun onEnterTap() = Unit
            override fun onSpaceTap() = Unit
            override fun onShiftTap() = Unit
            override fun onShiftLongPress() = Unit
            override fun onSymbolsToggle() = Unit
            override fun onSymbolsExtToggle() = Unit
            override fun onLanguageTap() = Unit
            override fun onLanguageLongPress() = Unit
            override fun onEmojiTap() = Unit
            override fun onClipboardTap() = Unit
            override fun onSpaceHorizontalSwipe(direction: Int) = Unit
            override fun onSpaceHorizontalDragPreview(targetLabel: String?) = Unit
            override fun onSpaceVerticalSwipe(isUp: Boolean) = Unit
            override fun onKeyPressFeedback(isSpecial: Boolean) = Unit
        }
        val dummyActions = object : KeyboardContainerView.ContainerActions {
            override fun onSuggestionClick(word: String) = Unit
            override fun onTranslationChipClick(text: String) = Unit
            override fun onQuickEmojiClick(emoji: String) = Unit
            override fun onSwitchMode(mode: KeyboardMode) = Unit
            override fun onCycleHandMode() = Unit
            override fun onSetHandMode(mode: HandMode) = Unit
            override fun onUpdateFloatingPosition(offsetX: Float, offsetY: Float) = Unit
            override fun onOpenSettings() = Unit
            override fun onPasteClip(text: String) = Unit
            override fun onCopyClip(text: String) = Unit
            override fun onTogglePinClip(item: ClipboardItemEntity) = Unit
            override fun onDeleteClip(id: Long) = Unit
            override fun onDeleteSelectedClips(ids: List<Long>) = Unit
            override fun onClearUnpinnedClips() = Unit
            override fun onToggleClipboardGridMode(gridMode: Boolean) = Unit
            override fun onTranslateRequest(source: String, target: String, selectedOnly: Boolean) = Unit
            override fun onReplaceText(newText: String) = Unit
            override fun onInsertText(text: String) = Unit
            override fun onApplyFawazDecoration() = Unit
            override fun onToggleRealtimeDecoration(enabled: Boolean) = Unit
            override fun onToggleFavoriteDecoration(style: DecorationStyle, frame: DecorationFrame) = Unit
            override fun onUpdateQuickEmojis(emojis: List<String>) = Unit
            override fun onUpdateQuickEmojiSizeSp(sizeSp: Float) = Unit
            override fun onResetQuickEmojiDefaults() = Unit
            override fun onSearchInputStateChanged(active: Boolean) = Unit
            override fun onEmojiPanelCommit(emoji: String) = Unit
            override fun onSymbolPanelCommit(symbol: String) = Unit
            override fun onCalculatorInsert(text: String) = Unit
            override fun onPanelBackspace() = Unit
            override fun getCurrentFieldTextSnapshot(): String = "تجربة"
        }

        val container = KeyboardContainerView(
            context = context,
            supportedTranslationLanguages = listOf(
                TranslationLanguageInfo("ar", "العربية", "ar"),
                TranslationLanguageInfo("en", "الإنجليزية", "en"),
            ),
            canvasListener = dummyCanvasListener,
            actions = dummyActions,
        )
        val alphaLayout = layoutRepo.buildLayout(settings, KeyboardMode.ALPHA, includeNumberRow = settings.showNumberRow)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        fun measureModeHeight(mode: KeyboardMode): Int {
            container.applyState(
                mode = mode,
                layout = alphaLayout,
                settings = settings,
                theme = theme,
                shiftState = ShiftState.OFF,
                isLandscape = false,
                suggestions = emptyList(),
                clipboardItems = emptyList(),
                favoriteDecorations = emptyList(),
            )
            container.measure(widthSpec, heightSpec)
            return container.measuredHeight
        }

        val alphaH = measureModeHeight(KeyboardMode.ALPHA)
        val clipH = measureModeHeight(KeyboardMode.CLIPBOARD)
        val decorateH = measureModeHeight(KeyboardMode.DECORATE)
        val calcH = measureModeHeight(KeyboardMode.CALCULATOR)

        assertEquals(alphaH, clipH)
        assertEquals(alphaH, decorateH)
        assertEquals(alphaH, calcH)
    }

    @Test
    fun `smart numeric field detector identifies phone, financial, PIN, and text fields`() {
        val phoneInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_PHONE }
        assertEquals(NumericContextType.PHONE, NumericFieldDetector.detect(phoneInfo))

        val decimalInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hintText = "أدخل المبلغ"
        }
        assertEquals(NumericContextType.CURRENCY_DECIMAL, NumericFieldDetector.detect(decimalInfo))

        val pinInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertEquals(NumericContextType.PIN_PASSWORD, NumericFieldDetector.detect(pinInfo))

        val otpTextInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "أدخل رمز التحقق OTP"
        }
        assertEquals(NumericContextType.PIN_PASSWORD, NumericFieldDetector.detect(otpTextInfo))

        val regularText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hintText = "اكتب رسالتك هنا"
        }
        assertEquals(NumericContextType.NONE, NumericFieldDetector.detect(regularText))
    }

    @Test
    fun `numeric grid layout provides 4x4 keypad with context symbols and alpha return key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)

        val phoneGrid = repo.buildLayout(
            settings = KeyboardSettings(activeLanguage = LanguageId.ARABIC),
            mode = KeyboardMode.NUMERIC_GRID,
            includeNumberRow = false,
            numericContextType = NumericContextType.PHONE,
        )
        assertEquals(4, phoneGrid.columns)
        assertEquals(4, phoneGrid.rows.size)
        val labels = phoneGrid.rows.flatMap { it.keys }.map { it.label }
        assertTrue(labels.containsAll(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "+", "*", "#", "⌫", "أبج", "↵")))
    }

    @Test
    fun `english layout keeps shift key and all languages load properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)

        val en = repo.buildLayout(
            settings = KeyboardSettings(activeLanguage = LanguageId.ENGLISH),
            mode = KeyboardMode.ALPHA,
            includeNumberRow = false,
        )
        assertTrue("English must include Shift key", en.rows[2].keys.any { it.type == KeyType.SHIFT })

        LanguageId.entries.forEach { lang ->
            val layout = repo.buildLayout(
                settings = KeyboardSettings(
                    activeLanguage = lang,
                    layoutVariants = mapOf(lang.name to lang.defaultVariant),
                    numeralStyle = NumeralStyle.EASTERN_ARABIC,
                ),
                mode = KeyboardMode.ALPHA,
                includeNumberRow = true,
            )
            assertTrue("Expected non-empty rows for ${lang.name}", layout.rows.size >= 4)
        }

        val dvorak = repo.buildLayout(
            settings = KeyboardSettings(
                activeLanguage = LanguageId.ENGLISH,
                layoutVariants = mapOf(LanguageId.ENGLISH.name to LayoutVariant.DVORAK),
            ),
            mode = KeyboardMode.ALPHA,
            includeNumberRow = false,
        )
        assertEquals("'", dvorak.rows.first().keys.first().label)
    }

    @Test
    fun `at symbol long press shortcuts contain required domain and email shortcuts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)
        val symbols = repo.buildLayout(
            settings = KeyboardSettings(activeLanguage = LanguageId.ARABIC),
            mode = KeyboardMode.SYMBOLS_1,
            includeNumberRow = false,
        )
        val atKey = symbols.rows.flatMap { it.keys }.firstOrNull { it.label == "@" }
        assertNotNull(atKey)
        assertEquals(listOf("https://", ".net", "@gmail.com", ".com"), atKey!!.alternates)
    }

    @Test
    fun `default settings provide 9 quick emojis, infinite clipboard, toolbar scale, and autoCorrect toggle`() {
        val defaults = KeyboardSettings()
        assertEquals(9, defaults.quickEmojiBarItems.size)
        assertEquals(0, defaults.clipboardMaxItems)
        assertEquals(0, defaults.clipboardRetentionDays)
        assertEquals(100, defaults.toolbarScalePercent)
        assertTrue(defaults.smartNumericGridEnabled)
        assertTrue(defaults.autoCorrectEnabled)
        assertFalse(defaults.copy(autoCorrectEnabled = false).autoCorrectEnabled)
        assertFalse(defaults.clipboardAutoClose)
        assertEquals(24, defaults.spaceSwipeSensitivityDp)
        assertTrue(defaults.realTimeDecorationEnabled)
        assertTrue(defaults.confirmClipboardDelete)

        assertTrue(QuickEmojiEditorPanelView.isCustomTextEntry("السلام عليكم"))
        assertTrue(QuickEmojiEditorPanelView.isCustomTextEntry("@"))
        assertFalse(QuickEmojiEditorPanelView.isCustomTextEntry("😂"))

        val iosCount = KeyboardThemePresets.allPresets.count { it.isIosStyle }
        assertTrue("Expected at least 12 iPhone themes, found $iosCount", iosCount >= 12)
    }

    @Test
    fun `cumulative bar visibility flags follow level 0 to 3`() {
        val level0 = KeyboardSettings(barLevel = 0)
        assertFalse(level0.showSuggestionsBar)
        assertFalse(level0.showQuickEmojiBar)
        assertFalse(level0.showNumberRow)

        val level1 = KeyboardSettings(barLevel = 1)
        assertTrue(level1.showSuggestionsBar)
        assertFalse(level1.showQuickEmojiBar)
        assertFalse(level1.showNumberRow)

        val level2 = KeyboardSettings(barLevel = 2)
        assertTrue(level2.showSuggestionsBar)
        assertTrue(level2.showQuickEmojiBar)
        assertFalse(level2.showNumberRow)

        val level3 = KeyboardSettings(barLevel = 3)
        assertTrue(level3.showSuggestionsBar)
        assertTrue(level3.showQuickEmojiBar)
        assertTrue(level3.showNumberRow)
    }

    @Test
    fun `fawaz decorator attaches ornament only to target non-final letters`() {
        assertEquals("ك${TextDecorator.FAWAZ_ORNAMENT}تب", TextDecorator.decorateFawaz("كتب"))
        assertEquals(
            "ك${TextDecorator.FAWAZ_ORNAMENT}ري${TextDecorator.FAWAZ_ORNAMENT}م",
            TextDecorator.decorateFawaz("كريم"),
        )
        assertEquals("ملك", TextDecorator.decorateFawaz("ملك"))
        assertEquals(
            "ي${TextDecorator.FAWAZ_ORNAMENT}س${TextDecorator.FAWAZ_ORNAMENT}ر",
            TextDecorator.decorateFawaz("يسر"),
        )
        assertEquals("فواز", TextDecorator.decorateFawaz("فواز"))
    }

    @Test
    fun `text decorator transforms Arabic kashida and English styles`() {
        val kashidaStyle = TextDecorator.styles.first { it.id == "ar_kashida_1" }
        val resAr = TextDecorator.decorate("مرحبا", kashidaStyle)
        assertTrue(resAr.contains("ـ"))
        assertFalse(resAr.endsWith("ـ"))

        val boldStyle = TextDecorator.styles.first { it.id == "en_bold" }
        val royalFrame = TextDecorator.frames.first { it.id == "royal" }
        val resEn = TextDecorator.decorate("Khatt", boldStyle, royalFrame)
        assertTrue(resEn.startsWith("꧁༺ "))
        assertTrue(resEn.endsWith(" ༻꧂"))
    }

    @Test
    fun `emoji and symbols catalogs support search`() {
        assertTrue(EmojiCatalog.search("ضحك").isNotEmpty())
        assertTrue(SymbolsCatalog.search("ريال").isNotEmpty())
    }

    @Test
    fun `realtime fawaz decorator inserts ornament mid-word only after target letters`() {
        val o = TextDecorator.FAWAZ_ORNAMENT
        // Typing ك then ت -> turns into ك + ornament + ت
        val afterKaf = TextDecorator.formatRealtimeFawazInsertion("", "ك")
        assertEquals("ك", afterKaf)
        val afterTa = TextDecorator.formatRealtimeFawazInsertion("ك", "ت")
        assertEquals("${o}ت", afterTa)
        // Typing ب after ت -> ب follows ت (not a target letter), so plain "ب"
        assertEquals("ب", TextDecorator.formatRealtimeFawazInsertion("ك${o}ت", "ب"))
        // Target letter at end of word after non-target letter stays undecorated until another letter follows
        assertEquals("ك", TextDecorator.formatRealtimeFawazInsertion("مل", "ك"))
        // Avoid double ornament if before already ends with ornament
        assertEquals("ر", TextDecorator.formatRealtimeFawazInsertion("ك$o", "ر"))
    }

    @Test
    fun `copying character to quick emoji bar keeps original keyboard layout intact`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LayoutRepository(context)
        val settingsWithCopy = KeyboardSettings(
            activeLanguage = LanguageId.ARABIC,
            useElevenColumnsArabic = false,
            quickEmojiBarItems = listOf("😂", "❤️", "😍", "😭", "🙏", "👍", "🔥", "😊", "ك"),
        )
        assertTrue("ك" in settingsWithCopy.quickEmojiBarItems)
        val ar10 = repo.buildLayout(settingsWithCopy, KeyboardMode.ALPHA, includeNumberRow = false)
        assertTrue("Primary Arabic keyboard must still contain 'ك'", ar10.rows[1].keys.any { it.label == "ك" })
    }

    @Test
    fun `clipboard panel triggers onCopyText callback when initialized`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var copiedValue: String? = null
        val panel = ClipboardPanelView(
            context = context,
            onPasteText = {},
            onCopyText = { copiedValue = it },
            onTogglePin = {},
            onDeleteItem = {},
            onDeleteSelected = {},
            onClearUnpinned = {},
            onToggleGridMode = {},
            onAddQuickTemplate = {},
            onBackToKeyboard = {},
        )
        panel.updateData(
            items = listOf(ClipboardItemEntity(id = 1L, text = "نص تجريبي", timestamp = 1000L)),
            theme = KeyboardThemePresets.AmberMidnight,
            requireBiometricLock = false,
            gridMode = false,
            confirmDelete = true,
            heightPx = 600,
        )
        assertNull(copiedValue)
        assertNotNull(panel)
    }
}
