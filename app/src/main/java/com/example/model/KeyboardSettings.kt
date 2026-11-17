package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardSettings(
    // Languages
    val enabledLanguages: List<LanguageId> = listOf(LanguageId.ARABIC, LanguageId.ENGLISH),
    val activeLanguage: LanguageId = LanguageId.ARABIC,
    val layoutVariants: Map<String, LayoutVariant> = mapOf(
        LanguageId.ARABIC.name to LayoutVariant.AR_STANDARD,
        LanguageId.ENGLISH.name to LayoutVariant.QWERTY,
    ),
    val useElevenColumnsArabic: Boolean = true,
    val customLayoutOverridesJson: String = "{}",

    // Dimensions (independent Portrait & Landscape)
    val portraitHeightPercent: Int = 100,
    val landscapeHeightPercent: Int = 95,
    val keyWidthPercent: Int = 100,
    val keyHeightPercent: Int = 100,
    val horizontalGapDp: Float = 4.5f,
    val verticalGapDp: Float = 6.5f,
    val keyFontSizeSp: Float = 20f,
    val keyCornerRadiusDp: Float = 8f,

    // Hand modes & floating
    val handMode: HandMode = HandMode.FULL,
    val floatingOffsetX: Float = 0f,
    val floatingOffsetY: Float = 0f,
    val floatingWidthPercent: Int = 85,

    // Themes
    val selectedThemeId: String = KeyboardThemePresets.AmberMidnight.id,
    val autoSystemDarkTheme: Boolean = true,
    val customTheme: KeyboardThemeSpec = KeyboardThemePresets.AmberMidnight.copy(
        id = "custom",
        nameAr = "ثيم مخصص",
    ),

    // Top Bars & Spacebar sequential swipe (Suggestions -> Numbers -> Quick Emojis)
    // Presets 0..3 when customBarsOverride == false; individual bar order when swiped on Spacebar
    val barLevel: Int = 3,
    val customBarsOverride: Boolean = false,
    val isSuggestionsBarVisible: Boolean = true,
    val isNumberRowVisible: Boolean = true,
    val isQuickEmojiBarVisible: Boolean = true,
    val spaceSwipeBarsEnabled: Boolean = true,
    val invertSpaceSwipe: Boolean = false,
    val hapticOnSpaceSwipe: Boolean = true,
    val spaceSwipeSensitivityDp: Int = 24,
    val numeralStyle: NumeralStyle = NumeralStyle.WESTERN,
    val emojiRenderStyle: EmojiRenderStyle = EmojiRenderStyle.APPLE_IOS,

    // Quick Emoji Bar (9 items + settings key)
    val quickEmojiBarItems: List<String> = listOf("😂", "❤️", "😍", "😭", "🙏", "👍", "🔥", "😊", "✨"),
    val quickEmojiSizeSp: Float = 19f,
    val toolbarScalePercent: Int = 100,

    // Typing intelligence, real-time decoration & feedback
    val autoCorrectEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    val smartNumericGridEnabled: Boolean = true,
    val realTimeDecorationEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val hapticStrengthMs: Int = 14,
    val soundEnabled: Boolean = false,
    val keyPreviewPopupEnabled: Boolean = true,

    // Clipboard (0 = infinite items & infinite retention)
    val clipboardAutoSave: Boolean = true,
    val clipboardMaxItems: Int = 0,
    val clipboardRetentionDays: Int = 0,
    val clipboardBiometricLock: Boolean = false,
    val clipboardGridMode: Boolean = false,
    val clipboardAutoClose: Boolean = false,
    val confirmClipboardDelete: Boolean = true,

    // Translation
    val translationSourceLang: String = "auto",
    val translationTargetLang: String = "en",
    val cloudTranslateFallbackEnabled: Boolean = false,

    // Onboarding
    val onboardingCompleted: Boolean = false,
) {
    val showSuggestionsBar: Boolean
        get() = if (customBarsOverride) isSuggestionsBarVisible else barLevel >= 1

    val showQuickEmojiBar: Boolean
        get() = if (customBarsOverride) isQuickEmojiBarVisible else barLevel >= 2

    val showNumberRow: Boolean
        get() = if (customBarsOverride) isNumberRowVisible else barLevel >= 3

    fun applySpaceVerticalSwipe(isUp: Boolean): KeyboardSettings {
        var sug = showSuggestionsBar
        var num = showNumberRow
        var em = showQuickEmojiBar
        if (!isUp) {
            // 1st Swipe Down: hide Suggestions & Toolbar
            // 2nd Swipe Down: hide Number Row
            // 3rd Swipe Down: hide Quick Emoji Bar
            when {
                sug -> sug = false
                num -> num = false
                em -> em = false
            }
        } else {
            // Swipe Up restores in reverse order: Quick Emoji -> Number Row -> Suggestions & Toolbar
            when {
                !em -> em = true
                !num -> num = true
                !sug -> sug = true
            }
        }
        val mappedLevel = when {
            sug && em && num -> 3
            sug && em -> 2
            sug -> 1
            !sug && !em && !num -> 0
            else -> barLevel
        }
        return copy(
            barLevel = mappedLevel,
            customBarsOverride = true,
            isSuggestionsBarVisible = sug,
            isNumberRowVisible = num,
            isQuickEmojiBarVisible = em,
        )
    }

    fun withBarLevel(level: Int): KeyboardSettings {
        val clamped = level.coerceIn(0, 3)
        return copy(
            barLevel = clamped,
            customBarsOverride = false,
            isSuggestionsBarVisible = clamped >= 1,
            isQuickEmojiBarVisible = clamped >= 2,
            isNumberRowVisible = clamped >= 3,
        )
    }

    fun variantFor(languageId: LanguageId): LayoutVariant =
        layoutVariants[languageId.name] ?: languageId.defaultVariant
}
