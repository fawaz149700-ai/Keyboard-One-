package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class LanguageId(
    val code: String,
    val nativeName: String,
    val arabicName: String,
    val isRtl: Boolean,
    val defaultVariant: LayoutVariant,
    val availableVariants: List<LayoutVariant>,
) {
    ARABIC(
        code = "ar",
        nativeName = "العربية",
        arabicName = "العربية",
        isRtl = true,
        defaultVariant = LayoutVariant.AR_STANDARD,
        availableVariants = listOf(LayoutVariant.AR_STANDARD, LayoutVariant.AR_EGYPTIAN, LayoutVariant.AR_GULF),
    ),
    ENGLISH(
        code = "en",
        nativeName = "English",
        arabicName = "الإنجليزية",
        isRtl = false,
        defaultVariant = LayoutVariant.QWERTY,
        availableVariants = listOf(
            LayoutVariant.QWERTY,
            LayoutVariant.AZERTY,
            LayoutVariant.QWERTZ,
            LayoutVariant.DVORAK,
            LayoutVariant.COLEMAK,
        ),
    ),
    FRENCH(
        code = "fr",
        nativeName = "Français",
        arabicName = "الفرنسية",
        isRtl = false,
        defaultVariant = LayoutVariant.AZERTY,
        availableVariants = listOf(LayoutVariant.AZERTY),
    ),
    SPANISH(
        code = "es",
        nativeName = "Español",
        arabicName = "الإسبانية",
        isRtl = false,
        defaultVariant = LayoutVariant.QWERTY,
        availableVariants = listOf(LayoutVariant.QWERTY),
    ),
    TURKISH(
        code = "tr",
        nativeName = "Türkçe",
        arabicName = "التركية",
        isRtl = false,
        defaultVariant = LayoutVariant.QWERTY,
        availableVariants = listOf(LayoutVariant.QWERTY),
    ),
    PERSIAN(
        code = "fa",
        nativeName = "فارسی",
        arabicName = "الفارسية",
        isRtl = true,
        defaultVariant = LayoutVariant.AR_STANDARD,
        availableVariants = listOf(LayoutVariant.AR_STANDARD),
    ),
    URDU(
        code = "ur",
        nativeName = "اردو",
        arabicName = "الأردية",
        isRtl = true,
        defaultVariant = LayoutVariant.AR_STANDARD,
        availableVariants = listOf(LayoutVariant.AR_STANDARD),
    ),
    RUSSIAN(
        code = "ru",
        nativeName = "Русский",
        arabicName = "الروسية",
        isRtl = false,
        defaultVariant = LayoutVariant.QWERTY,
        availableVariants = listOf(LayoutVariant.QWERTY),
    ),
    GERMAN(
        code = "de",
        nativeName = "Deutsch",
        arabicName = "الألمانية",
        isRtl = false,
        defaultVariant = LayoutVariant.QWERTZ,
        availableVariants = listOf(LayoutVariant.QWERTZ),
    ),
}

@Serializable
enum class LayoutVariant(val arabicLabel: String, val englishLabel: String) {
    QWERTY("QWERTY قياسي", "QWERTY"),
    AZERTY("AZERTY فرنسي", "AZERTY"),
    QWERTZ("QWERTZ ألماني", "QWERTZ"),
    DVORAK("Dvorak دفوراك", "Dvorak"),
    COLEMAK("Colemak كولماك", "Colemak"),
    AR_STANDARD("عربي قياسي", "Arabic Standard"),
    AR_EGYPTIAN("عربي مصري", "Arabic Egyptian"),
    AR_GULF("عربي خليجي", "Arabic Gulf"),
}

enum class KeyType {
    CHAR,
    NUMBER,
    SHIFT,
    BACKSPACE,
    SPACE,
    ENTER,
    MODE_SYMBOLS,
    MODE_SYMBOLS_EXT,
    LANGUAGE,
    EMOJI,
    CLIPBOARD,
    COMMA,
    PERIOD,
}

data class KeySpec(
    val id: String,
    val label: String,
    val output: String,
    val shiftOutput: String = output,
    val alternates: List<String> = emptyList(),
    val widthWeight: Float = 1f,
    val type: KeyType = KeyType.CHAR,
    val subLabel: String? = null,
)

data class KeyboardRowSpec(
    val keys: List<KeySpec>,
)

data class KeyboardLayoutSpec(
    val languageId: LanguageId,
    val variant: LayoutVariant,
    val displayName: String,
    val isRtl: Boolean,
    val columns: Int,
    val rows: List<KeyboardRowSpec>,
)

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK,
}

enum class KeyboardMode {
    ALPHA,
    NUMERIC_GRID,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD,
    TRANSLATE,
    DECORATE,
    CALCULATOR,
    SYMBOLS_BROWSER,
    QUICK_EMOJI_EDITOR,
}

@Serializable
enum class HandMode(val arabicLabel: String) {
    FULL("الوضع الكامل"),
    ONE_HAND_LEFT("يد واحدة (يسار)"),
    ONE_HAND_RIGHT("يد واحدة (يمين)"),
    FLOATING("كيبورد عائم"),
}

@Serializable
enum class NumeralStyle(val arabicLabel: String) {
    WESTERN("أرقام غربية (1 2 3)"),
    EASTERN_ARABIC("أرقام عربية شرقية (١ ٢ ٣)"),
}

@Serializable
enum class EmojiRenderStyle(val arabicLabel: String) {
    APPLE_IOS("نمط آيفون (Apple)"),
    NOTO_ANDROID("نمط أندرويد (Noto)"),
}

@Serializable
data class CustomKeyOverride(
    val rowIndex: Int,
    val colIndex: Int,
    val label: String,
    val output: String,
    val alternates: List<String> = emptyList(),
)
