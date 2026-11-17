package com.example.data.decorate

data class DecorationStyle(
    val id: String,
    val nameAr: String,
    val isArabicStyle: Boolean,
    val transform: (String) -> String,
)

data class DecorationFrame(
    val id: String,
    val nameAr: String,
    val prefix: String,
    val suffix: String,
)

object TextDecorator {

    const val FAWAZ_ORNAMENT = "ــــ͡ــٰ̡ـ"

    val FAWAZ_TARGET_LETTERS = setOf('ك', 'ي', 'س', 'ش', 'ق', 'ج', 'خ', 'ع', 'غ')

    private val nonConnectingArabic = setOf(
        'ا', 'أ', 'إ', 'آ', 'د', 'ذ', 'ر', 'ز', 'و', 'ؤ', 'ة', 'ى', 'ء'
    )

    private val altArabicMap = mapOf(
        'ك' to 'ک', 'ي' to 'ی', 'ه' to 'ھ', 'ت' to 'ٺ', 'ج' to 'چ', 'ف' to 'ڤ', 'ب' to 'پ', 'ز' to 'ژ'
    )

    private val upsideDownMap: Map<Char, String> = mapOf(
        'a' to "ɐ", 'b' to "q", 'c' to "ɔ", 'd' to "p", 'e' to "ǝ", 'f' to "ɟ", 'g' to "ƃ",
        'h' to "ɥ", 'i' to "ᴉ", 'j' to "ɾ", 'k' to "ʞ", 'l' to "l", 'm' to "ɯ", 'n' to "u",
        'o' to "o", 'p' to "d", 'q' to "b", 'r' to "ɹ", 's' to "s", 't' to "ʇ", 'u' to "n",
        'v' to "ʌ", 'w' to "ʍ", 'x' to "x", 'y' to "ʎ", 'z' to "z",
        'A' to "∀", 'B' to "\uD801\uDC12", 'C' to "Ɔ", 'D' to "ᗡ", 'E' to "Ǝ", 'F' to "Ⅎ", 'G' to "פ",
        'H' to "H", 'I' to "I", 'J' to "ſ", 'K' to "ʞ", 'L' to "˥", 'M' to "W", 'N' to "N",
        'O' to "O", 'P' to "Ԁ", 'Q' to "Q", 'R' to "ᴚ", 'S' to "S", 'T' to "⊥", 'U' to "∩",
        'V' to "Λ", 'W' to "M", 'X' to "X", 'Y' to "⅄", 'Z' to "Z",
    )

    private val smallCapsMap = mapOf(
        'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ', 'f' to 'ғ', 'g' to 'ɢ',
        'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ', 'k' to 'ᴋ', 'l' to 'ʟ', 'm' to 'ᴍ', 'n' to 'ɴ',
        'o' to 'ᴏ', 'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ', 's' to 's', 't' to 'ᴛ', 'u' to 'ᴜ',
        'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x', 'y' to 'ʏ', 'z' to 'ᴢ',
    )

    val frames = listOf(
        DecorationFrame("none", "بدون إطار", "", ""),
        DecorationFrame("royal", "إطار ملكي", "꧁༺ ", " ༻꧂"),
        DecorationFrame("jp_brackets", "أقواس أنيقة", "『 ", " 』"),
        DecorationFrame("stars", "نجوم", "★彡 ", " 彡★"),
        DecorationFrame("floral", "زهور", "✿•┈ ", " ┈•✿"),
        DecorationFrame("crown", "تاج", "♛ ", " ♛"),
        DecorationFrame("diamond", "معين", "❖ ═══ ", " ═══ ❖"),
        DecorationFrame("sparkle", "بريق", "✨ ", " ✨"),
        DecorationFrame("double_angle", "مزدوج", "«• ", " •»"),
    )

    val styles = listOf(
        // Arabic styles
        DecorationStyle("ar_fawaz", "فواز (زخرفة متصلة)", true) { decorateFawaz(it) },
        DecorationStyle("ar_kashida_1", "تطويل عربي ناعم", true) { addKashida(it, 1) },
        DecorationStyle("ar_kashida_3", "تطويل عربي فخم", true) { addKashida(it, 3) },
        DecorationStyle("ar_tashkeel", "زخرفة التشكيل", true) { addDecorativeTashkeel(it) },
        DecorationStyle("ar_stars", "فواصل نجوم", true) { joinArabicWithSymbol(it, "★") },
        DecorationStyle("ar_hearts", "فواصل قلوب", true) { joinArabicWithSymbol(it, "♥") },
        DecorationStyle("ar_alt", "حروف عربية بديلة", true) { mapArabicAlternate(it) },
        // English styles
        DecorationStyle("en_bold", "عريض (Bold)", false) { mapMathAlpha(it, 0x1D400, 0x1D41A) },
        DecorationStyle("en_italic", "مائل (Italic)", false) { mapMathAlpha(it, 0x1D434, 0x1D44E) },
        DecorationStyle("en_bold_italic", "عريض مائل", false) { mapMathAlpha(it, 0x1D468, 0x1D482) },
        DecorationStyle("en_script", "مخطوط (Script)", false) { mapScript(it) },
        DecorationStyle("en_gothic", "قوطي (Gothic)", false) { mapGothic(it) },
        DecorationStyle("en_double", "مزدوج (Double)", false) { mapDoubleStruck(it) },
        DecorationStyle("en_bubbles", "فقاعات Ⓑ", false) { mapBubbles(it, filled = false) },
        DecorationStyle("en_bubbles_fill", "فقاعات ممتلئة 🅑", false) { mapBubbles(it, filled = true) },
        DecorationStyle("en_squares", "مربعات 🅂", false) { mapSquares(it, filled = false) },
        DecorationStyle("en_squares_fill", "مربعات ممتلئة 🆂", false) { mapSquares(it, filled = true) },
        DecorationStyle("en_mono", "أحادي (Mono)", false) { mapMathAlpha(it, 0x1D670, 0x1D68A) },
        DecorationStyle("en_small_caps", "حروف صغيرة ꜱᴍᴀʟʟ", false) { it.lowercase().map { c -> smallCapsMap[c] ?: c }.joinToString("") },
        DecorationStyle("en_upside_down", "مقلوب uʍop", false) { it.reversed().map { c -> upsideDownMap[c] ?: c.toString() }.joinToString("") },
        DecorationStyle("en_wide", "عريض المسافات Ｗｉｄｅ", false) { mapFullWidth(it) },
    )

    fun decorate(text: String, style: DecorationStyle, frame: DecorationFrame = frames.first()): String {
        if (text.isBlank()) return ""
        val core = style.transform(text)
        return "${frame.prefix}$core${frame.suffix}"
    }

    /**
     * يطبق زخرفة "فواز" (ــــ͡ــٰ̡ـ) حصراً مع الأحرف (ك، ي، س، ش، ق، ج، خ، ع، غ)
     * وبشرط أن لا يكون الحرف في نهاية الكلمة (أي يوجد حرف عربي بعده).
     */
    fun decorateFawaz(text: String): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in text.indices) {
            val ch = text[i]
            sb.append(ch)
            if (ch in FAWAZ_TARGET_LETTERS && hasFollowingWordLetter(text, i)) {
                sb.append(FAWAZ_ORNAMENT)
            }
        }
        return sb.toString()
    }

    /**
     * يحقن الزخرفة تلقائياً أثناء الكتابة بالزمن الفعلي عند إدخال حرف عربي جديد
     * بعد أحد الأحرف المحددة (ك، ي، س، ش، ق، ج، خ، ع، غ) بحيث لا تكون في نهاية الكلمة.
     */
    fun formatRealtimeFawazInsertion(textBeforeCursor: String, incoming: String): String {
        if (incoming.isEmpty()) return incoming
        val firstNew = incoming.first()
        if (!firstNew.isArabicLetter()) return incoming
        if (textBeforeCursor.isEmpty() || textBeforeCursor.endsWith(FAWAZ_ORNAMENT)) return incoming

        var idx = textBeforeCursor.length - 1
        while (idx >= 0) {
            val c = textBeforeCursor[idx]
            if (c in '\u064B'..'\u065F' || c == '\u0670') {
                idx--
            } else {
                break
            }
        }
        if (idx < 0) return incoming
        val prevBase = textBeforeCursor[idx]
        return if (prevBase in FAWAZ_TARGET_LETTERS) {
            "$FAWAZ_ORNAMENT$incoming"
        } else {
            incoming
        }
    }

    private fun hasFollowingWordLetter(text: String, index: Int): Boolean {
        var j = index + 1
        while (j < text.length) {
            val c = text[j]
            // Skip combining Arabic diacritics when checking if another letter follows in the same word
            if (c in '\u064B'..'\u065F' || c == '\u0670') {
                j++
                continue
            }
            return c.isArabicLetter()
        }
        return false
    }

    private fun addKashida(text: String, count: Int): String {
        val sb = StringBuilder()
        val tatweel = "ـ".repeat(count)
        for (i in text.indices) {
            val ch = text[i]
            sb.append(ch)
            val next = text.getOrNull(i + 1)
            if (next != null && next.isArabicLetter() && ch.isArabicLetter() && ch !in nonConnectingArabic) {
                sb.append(tatweel)
            }
        }
        return sb.toString()
    }

    private fun addDecorativeTashkeel(text: String): String {
        val ornaments = listOf("َ", "ِ", "ُ", "ْ")
        val sb = StringBuilder()
        var idx = 0
        for (ch in text) {
            sb.append(ch)
            if (ch.isArabicLetter() && ch != 'ا' && ch != 'ى') {
                sb.append(ornaments[idx % ornaments.size])
                idx++
            }
        }
        return sb.toString()
    }

    private fun joinArabicWithSymbol(text: String, sym: String): String {
        val sb = StringBuilder()
        for (i in text.indices) {
            val ch = text[i]
            sb.append(ch)
            val next = text.getOrNull(i + 1)
            if (ch != ' ' && next != null && next != ' ') {
                sb.append("ـ").append(sym).append("ـ")
            }
        }
        return sb.toString()
    }

    private fun mapArabicAlternate(text: String): String =
        text.map { altArabicMap[it] ?: it }.joinToString("")

    private fun Char.isArabicLetter(): Boolean = this in '\u0621'..'\u064A'

    private fun mapMathAlpha(text: String, upperStart: Int, lowerStart: Int): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                in 'A'..'Z' -> sb.appendCodePoint(upperStart + (c - 'A'))
                in 'a'..'z' -> {
                    if (lowerStart == 0x1D44E && c == 'h') sb.appendCodePoint(0x210E)
                    else sb.appendCodePoint(lowerStart + (c - 'a'))
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun mapScript(text: String): String {
        val upperExceptions = mapOf(
            'B' to 0x212C, 'E' to 0x2130, 'F' to 0x2131, 'H' to 0x210B,
            'I' to 0x2110, 'L' to 0x2112, 'M' to 0x2133, 'R' to 0x211B
        )
        val lowerExceptions = mapOf('e' to 0x212F, 'g' to 0x210A, 'o' to 0x2134)
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                in 'A'..'Z' -> sb.appendCodePoint(upperExceptions[c] ?: (0x1D49C + (c - 'A')))
                in 'a'..'z' -> sb.appendCodePoint(lowerExceptions[c] ?: (0x1D4B6 + (c - 'a')))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun mapGothic(text: String): String {
        val upperExceptions = mapOf('C' to 0x212D, 'H' to 0x210C, 'I' to 0x2111, 'R' to 0x211C, 'Z' to 0x2128)
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                in 'A'..'Z' -> sb.appendCodePoint(upperExceptions[c] ?: (0x1D504 + (c - 'A')))
                in 'a'..'z' -> sb.appendCodePoint(0x1D51E + (c - 'a'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun mapDoubleStruck(text: String): String {
        val upperExceptions = mapOf(
            'C' to 0x2102, 'H' to 0x210D, 'N' to 0x2115, 'P' to 0x2119,
            'Q' to 0x211A, 'R' to 0x211D, 'Z' to 0x2124
        )
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                in 'A'..'Z' -> sb.appendCodePoint(upperExceptions[c] ?: (0x1D538 + (c - 'A')))
                in 'a'..'z' -> sb.appendCodePoint(0x1D552 + (c - 'a'))
                in '0'..'9' -> sb.appendCodePoint(0x1D7D8 + (c - '0'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun mapBubbles(text: String, filled: Boolean): String {
        val sb = StringBuilder()
        for (c in text) {
            if (filled) {
                when (c.uppercaseChar()) {
                    in 'A'..'Z' -> sb.appendCodePoint(0x1F150 + (c.uppercaseChar() - 'A'))
                    else -> sb.append(c)
                }
            } else {
                when (c) {
                    in 'A'..'Z' -> sb.appendCodePoint(0x24B6 + (c - 'A'))
                    in 'a'..'z' -> sb.appendCodePoint(0x24D0 + (c - 'a'))
                    '0' -> sb.append('⓪')
                    in '1'..'9' -> sb.appendCodePoint(0x2460 + (c - '1'))
                    else -> sb.append(c)
                }
            }
        }
        return sb.toString()
    }

    private fun mapSquares(text: String, filled: Boolean): String {
        val sb = StringBuilder()
        val base = if (filled) 0x1F170 else 0x1F130
        for (c in text) {
            when (c.uppercaseChar()) {
                in 'A'..'Z' -> sb.appendCodePoint(base + (c.uppercaseChar() - 'A'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun mapFullWidth(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                ' ' -> sb.append("  ")
                in '!'..'~' -> sb.appendCodePoint(0xFF01 + (c - '!'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
