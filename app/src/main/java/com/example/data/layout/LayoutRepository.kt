package com.example.data.layout

import android.content.Context
import com.example.model.CustomKeyOverride
import com.example.model.KeySpec
import com.example.model.KeyType
import com.example.model.KeyboardLayoutSpec
import com.example.model.KeyboardMode
import com.example.model.KeyboardRowSpec
import com.example.model.KeyboardSettings
import com.example.model.LanguageId
import com.example.model.LayoutVariant
import com.example.model.NumeralStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AssetKeyDto(
    val label: String,
    val output: String,
    val shiftOutput: String? = null,
    val alternates: List<String> = emptyList(),
    val widthWeight: Float = 1f,
)

@Serializable
private data class AssetLayoutDto(
    val languageId: String,
    val variant: String,
    val displayName: String,
    val isRtl: Boolean,
    val columns: Int,
    val rows: List<List<AssetKeyDto>>,
)

class LayoutRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val assetCache = mutableMapOf<String, AssetLayoutDto>()

    private fun resolveAssetFileName(
        languageId: LanguageId,
        variant: LayoutVariant,
        useElevenColArabic: Boolean,
    ): String = when (languageId) {
        LanguageId.ARABIC -> when {
            useElevenColArabic && variant == LayoutVariant.AR_STANDARD -> "ar_11col.json"
            variant == LayoutVariant.AR_EGYPTIAN -> "ar_egyptian.json"
            variant == LayoutVariant.AR_GULF -> "ar_gulf.json"
            else -> "ar_standard.json"
        }
        LanguageId.ENGLISH -> when (variant) {
            LayoutVariant.AZERTY -> "en_azerty.json"
            LayoutVariant.QWERTZ -> "en_qwertz.json"
            LayoutVariant.DVORAK -> "en_dvorak.json"
            LayoutVariant.COLEMAK -> "en_colemak.json"
            else -> "en_qwerty.json"
        }
        LanguageId.FRENCH -> "fr_azerty.json"
        LanguageId.SPANISH -> "es_qwerty.json"
        LanguageId.TURKISH -> "tr_qwerty.json"
        LanguageId.PERSIAN -> "fa_standard.json"
        LanguageId.URDU -> "ur_standard.json"
        LanguageId.RUSSIAN -> "ru_jcuken.json"
        LanguageId.GERMAN -> "de_qwertz.json"
    }

    private fun loadAssetLayout(fileName: String): AssetLayoutDto {
        assetCache[fileName]?.let { return it }
        val text = context.assets.open("layouts/$fileName").bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<AssetLayoutDto>(text)
        assetCache[fileName] = dto
        return dto
    }

    fun parseOverrides(rawJson: String): Map<String, List<CustomKeyOverride>> {
        if (rawJson.isBlank() || rawJson == "{}") return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, List<CustomKeyOverride>>>(rawJson)
        }.getOrElse { emptyMap() }
    }

    fun encodeOverrides(map: Map<String, List<CustomKeyOverride>>): String =
        json.encodeToString(map)

    fun buildNumberRow(numeralStyle: NumeralStyle): KeyboardRowSpec {
        val western = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val eastern = listOf("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠")
        val altSymbols = listOf(
            listOf("!", "¹", "½", "⅓", "¼"),
            listOf("@", "²", "⅔"),
            listOf("#", "³", "¾"),
            listOf("$", "€", "£", "﷼", "⁴"),
            listOf("%", "‰", "⁵"),
            listOf("^", "↑", "⁶"),
            listOf("&", "§", "⁷"),
            listOf("*", "×", "•", "⁸"),
            listOf("(", "[", "{", "⁹"),
            listOf(")", "]", "}", "⁰", "°"),
        )
        val digits = if (numeralStyle == NumeralStyle.EASTERN_ARABIC) eastern else western
        return KeyboardRowSpec(
            keys = digits.mapIndexed { idx, digit ->
                val alt = buildList {
                    add(if (numeralStyle == NumeralStyle.EASTERN_ARABIC) western[idx] else eastern[idx])
                    addAll(altSymbols[idx])
                }
                KeySpec(
                    id = "num_$idx",
                    label = digit,
                    output = digit,
                    shiftOutput = digit,
                    alternates = alt,
                    widthWeight = 1f,
                    type = KeyType.NUMBER,
                )
            }
        )
    }

    fun buildLayout(
        settings: KeyboardSettings,
        mode: KeyboardMode,
        includeNumberRow: Boolean,
        spacebarOverrideText: String? = null,
        numericContextType: NumericContextType = NumericContextType.NUMBER,
    ): KeyboardLayoutSpec {
        val lang = settings.activeLanguage
        if (mode == KeyboardMode.NUMERIC_GRID) {
            return buildNumericGridLayout(lang, settings.numeralStyle, numericContextType)
        }
        if (mode == KeyboardMode.SYMBOLS_1 || mode == KeyboardMode.SYMBOLS_2) {
            return buildSymbolsLayout(lang, mode == KeyboardMode.SYMBOLS_2, settings.numeralStyle, spacebarOverrideText)
        }

        val variant = settings.variantFor(lang)
        val fileName = resolveAssetFileName(lang, variant, settings.useElevenColumnsArabic)
        val dto = loadAssetLayout(fileName)
        val allOverrides = parseOverrides(settings.customLayoutOverridesJson)
        val langOverrides = allOverrides[lang.name].orEmpty()

        val letterRows = dto.rows.mapIndexed { rIdx, rowList ->
            val mapped = rowList.mapIndexed { cIdx, keyDto ->
                val ov = langOverrides.firstOrNull { it.rowIndex == rIdx && it.colIndex == cIdx }
                val lbl = ov?.label ?: keyDto.label
                val out = ov?.output ?: keyDto.output
                val shiftOut = if (ov != null) ov.output.uppercase() else (keyDto.shiftOutput ?: out)
                val alts = ov?.alternates?.takeIf { it.isNotEmpty() } ?: keyDto.alternates
                KeySpec(
                    id = "k_${rIdx}_$cIdx",
                    label = lbl,
                    output = out,
                    shiftOutput = shiftOut,
                    alternates = alts,
                    widthWeight = keyDto.widthWeight,
                    type = KeyType.CHAR,
                    subLabel = alts.firstOrNull(),
                )
            }
            if (rIdx == 2) {
                buildThirdRowWithModifiers(mapped, lang, dto.isRtl)
            } else {
                KeyboardRowSpec(mapped)
            }
        }

        val rows = buildList {
            if (includeNumberRow) add(buildNumberRow(settings.numeralStyle))
            addAll(letterRows)
            add(buildBottomRow(lang, spacebarOverrideText))
        }

        return KeyboardLayoutSpec(
            languageId = lang,
            variant = variant,
            displayName = dto.displayName,
            isRtl = dto.isRtl,
            columns = dto.columns,
            rows = rows,
        )
    }

    private fun buildThirdRowWithModifiers(
        letters: List<KeySpec>,
        languageId: LanguageId,
        isRtl: Boolean,
    ): KeyboardRowSpec {
        val backspaceKey = KeySpec(
            id = "key_backspace",
            label = "⌫",
            output = "",
            widthWeight = when {
                (languageId == LanguageId.ARABIC || isRtl) && letters.size >= 10 -> 1.2f
                languageId == LanguageId.ARABIC || isRtl -> 1.45f
                letters.size >= 10 -> 1.15f
                else -> 1.4f
            },
            type = KeyType.BACKSPACE,
        )
        // Hide Shift key completely in Arabic (and non-cased RTL layouts); show in English & Latin/Cyrillic
        if (languageId == LanguageId.ARABIC || isRtl) {
            return KeyboardRowSpec(letters + listOf(backspaceKey))
        }
        val shiftKey = KeySpec(
            id = "key_shift",
            label = "⇧",
            output = "",
            widthWeight = if (letters.size >= 10) 1.15f else 1.4f,
            type = KeyType.SHIFT,
        )
        return KeyboardRowSpec(listOf(shiftKey) + letters + listOf(backspaceKey))
    }

    private fun buildBottomRow(
        languageId: LanguageId,
        spacebarOverrideText: String?,
    ): KeyboardRowSpec {
        val symbolsLabel = if (languageId.isRtl) "؟١٢٣" else "?123"
        val commaChar = if (languageId == LanguageId.ARABIC || languageId == LanguageId.PERSIAN || languageId == LanguageId.URDU) "،" else ","
        val commaAlts = if (languageId.isRtl) listOf("؛", "؟", "!", ":", "\"", "'", "«", "»") else listOf(";", "!", "?", ":", "\"", "'")
        val periodAlts = if (languageId.isRtl) {
            listOf("،", "؟", "!", "…", "-", "/", "@", ".com", "@gmail.com", ".net", "https://")
        } else {
            listOf(",", "?", "!", "…", "-", "/", "@", ".com", "@gmail.com", ".net", "https://")
        }

        return KeyboardRowSpec(
            keys = listOf(
                KeySpec("key_sym", symbolsLabel, "", widthWeight = 1.25f, type = KeyType.MODE_SYMBOLS),
                KeySpec("key_lang", "🌐", "", widthWeight = 0.95f, type = KeyType.LANGUAGE),
                KeySpec("key_comma", commaChar, commaChar, alternates = commaAlts, widthWeight = 0.85f, type = KeyType.COMMA),
                KeySpec(
                    id = "key_space",
                    label = spacebarOverrideText ?: languageId.nativeName,
                    output = " ",
                    widthWeight = 3.65f,
                    type = KeyType.SPACE,
                ),
                KeySpec("key_clipboard", "📋", "", widthWeight = 0.85f, type = KeyType.CLIPBOARD),
                KeySpec("key_emoji", "☺", "", widthWeight = 0.85f, type = KeyType.EMOJI),
                KeySpec("key_period", ".", ".", alternates = periodAlts, widthWeight = 0.85f, type = KeyType.PERIOD),
                KeySpec("key_enter", "↵", "\n", widthWeight = 1.35f, type = KeyType.ENTER),
            )
        )
    }

    private fun buildSymbolsLayout(
        languageId: LanguageId,
        page2: Boolean,
        numeralStyle: NumeralStyle,
        spacebarOverrideText: String?,
    ): KeyboardLayoutSpec {
        val western = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val eastern = listOf("١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩", "٠")
        val row1Chars = if (!page2) {
            if (numeralStyle == NumeralStyle.EASTERN_ARABIC) eastern else western
        } else {
            listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ")
        }

        val row2Chars = if (!page2) {
            if (languageId.isRtl) listOf("@", "#", "﷼", "_", "&", "-", "+", "(", ")", "/")
            else listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
        } else {
            listOf("£", "€", "¥", "¢", "^", "°", "=", "{", "}", "\\")
        }

        val row3Chars = if (!page2) {
            if (languageId.isRtl) listOf("*", "\"", "'", ":", "؛", "!", "؟")
            else listOf("*", "\"", "'", ":", ";", "!", "?")
        } else {
            listOf("%", "©", "®", "™", "✓", "[", "]")
        }

        val atAlternates = listOf("https://", ".net", "@gmail.com", ".com")
        val row1 = KeyboardRowSpec(row1Chars.mapIndexed { i, c -> KeySpec("s1_$i", c, c) })
        val row2 = KeyboardRowSpec(
            row2Chars.mapIndexed { i, c ->
                if (c == "@") {
                    KeySpec(
                        id = "s2_$i",
                        label = c,
                        output = c,
                        alternates = atAlternates,
                        subLabel = ".com",
                    )
                } else {
                    KeySpec("s2_$i", c, c)
                }
            }
        )
        val switchPageKey = KeySpec(
            id = "key_sym_ext",
            label = if (page2) "123" else "=\\<",
            output = "",
            widthWeight = 1.4f,
            type = KeyType.MODE_SYMBOLS_EXT,
        )
        val backspaceKey = KeySpec("key_backspace", "⌫", "", widthWeight = 1.4f, type = KeyType.BACKSPACE)
        val row3 = KeyboardRowSpec(
            listOf(switchPageKey) +
                row3Chars.mapIndexed { i, c -> KeySpec("s3_$i", c, c) } +
                listOf(backspaceKey)
        )

        val alphaLabel = if (languageId.isRtl) "أبج" else "ABC"
        val bottom = KeyboardRowSpec(
            listOf(
                KeySpec("key_sym", alphaLabel, "", widthWeight = 1.25f, type = KeyType.MODE_SYMBOLS),
                KeySpec("key_lang", "🌐", "", widthWeight = 0.95f, type = KeyType.LANGUAGE),
                KeySpec("key_comma", if (languageId.isRtl) "،" else ",", if (languageId.isRtl) "،" else ",", widthWeight = 0.85f, type = KeyType.COMMA),
                KeySpec("key_space", spacebarOverrideText ?: languageId.nativeName, " ", widthWeight = 3.65f, type = KeyType.SPACE),
                KeySpec("key_clipboard", "📋", "", widthWeight = 0.85f, type = KeyType.CLIPBOARD),
                KeySpec("key_emoji", "☺", "", widthWeight = 0.85f, type = KeyType.EMOJI),
                KeySpec("key_period", ".", ".", alternates = listOf(".com", "@gmail.com", ".net", "https://"), widthWeight = 0.85f, type = KeyType.PERIOD),
                KeySpec("key_enter", "↵", "\n", widthWeight = 1.35f, type = KeyType.ENTER),
            )
        )

        return KeyboardLayoutSpec(
            languageId = languageId,
            variant = LayoutVariant.QWERTY,
            displayName = if (page2) "رموز إضافية" else "أرقام ورموز",
            isRtl = false,
            columns = 10,
            rows = listOf(row1, row2, row3, bottom),
        )
    }

    private fun buildNumericGridLayout(
        languageId: LanguageId,
        numeralStyle: NumeralStyle,
        contextType: NumericContextType,
    ): KeyboardLayoutSpec {
        val western = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val eastern = listOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        val digits = if (numeralStyle == NumeralStyle.EASTERN_ARABIC) eastern else western
        val otherDigits = if (numeralStyle == NumeralStyle.EASTERN_ARABIC) western else eastern

        fun digitKey(n: Int): KeySpec {
            val ch = digits[n]
            val altCh = otherDigits[n]
            val extraAlts = when (n) {
                0 -> listOf(altCh, "+", " ")
                1 -> listOf(altCh, "!")
                2 -> listOf(altCh, "@")
                else -> listOf(altCh)
            }
            return KeySpec(
                id = "ng_$n",
                label = ch,
                output = ch,
                shiftOutput = ch,
                alternates = extraAlts.distinct(),
                widthWeight = 1f,
                type = KeyType.NUMBER,
                subLabel = altCh,
            )
        }

        val ctxSide1 = when (contextType) {
            NumericContextType.PHONE -> KeySpec("ng_plus", "+", "+", alternates = listOf("(", ")", " "), widthWeight = 1f, type = KeyType.CHAR, subLabel = "(")
            NumericContextType.CURRENCY_DECIMAL -> KeySpec("ng_cur", if (languageId.isRtl) "﷼" else "$", if (languageId.isRtl) "﷼" else "$", alternates = listOf("$", "€", "£", "﷼", "%"), widthWeight = 1f, type = KeyType.CHAR, subLabel = "$")
            NumericContextType.DATETIME -> KeySpec("ng_slash", "/", "/", alternates = listOf("-", ".", ":"), widthWeight = 1f, type = KeyType.CHAR, subLabel = "-")
            NumericContextType.PIN_PASSWORD -> KeySpec("ng_dash", "-", "-", alternates = listOf("*", "#", "_"), widthWeight = 1f, type = KeyType.CHAR)
            else -> KeySpec("ng_minus", "-", "-", alternates = listOf("+", "*", "/", "="), widthWeight = 1f, type = KeyType.CHAR, subLabel = "+")
        }

        val ctxSide2 = when (contextType) {
            NumericContextType.PHONE -> KeySpec("ng_star", "*", "*", alternates = listOf("#", ";", ","), widthWeight = 1f, type = KeyType.CHAR, subLabel = "#")
            NumericContextType.CURRENCY_DECIMAL -> KeySpec("ng_comma", ",", ",", alternates = listOf(".", "%", "-", "+"), widthWeight = 1f, type = KeyType.COMMA, subLabel = "%")
            NumericContextType.DATETIME -> KeySpec("ng_colon", ":", ":", alternates = listOf("-", "/", "."), widthWeight = 1f, type = KeyType.CHAR, subLabel = "/")
            NumericContextType.PIN_PASSWORD -> KeySpec("ng_hash", "#", "#", alternates = listOf("*", "@", "!"), widthWeight = 1f, type = KeyType.CHAR)
            else -> KeySpec("ng_space", "␣", " ", alternates = listOf(",", "_", "/"), widthWeight = 1f, type = KeyType.SPACE)
        }

        val bottomAux = when (contextType) {
            NumericContextType.PHONE -> KeySpec("ng_hash_b", "#", "#", alternates = listOf("*", "+", "-"), widthWeight = 1f, type = KeyType.CHAR, subLabel = "*")
            NumericContextType.DATETIME -> KeySpec("ng_dash_b", "-", "-", alternates = listOf("/", ".", ":"), widthWeight = 1f, type = KeyType.CHAR)
            else -> KeySpec("ng_dot", ".", ".", alternates = listOf(",", "-", "+", "/"), widthWeight = 1f, type = KeyType.PERIOD, subLabel = ",")
        }

        val alphaLabel = if (languageId.isRtl) "أبج" else "ABC"
        val row1 = KeyboardRowSpec(
            listOf(
                digitKey(1),
                digitKey(2),
                digitKey(3),
                KeySpec("ng_backspace", "⌫", "", widthWeight = 1f, type = KeyType.BACKSPACE),
            )
        )
        val row2 = KeyboardRowSpec(listOf(digitKey(4), digitKey(5), digitKey(6), ctxSide1))
        val row3 = KeyboardRowSpec(listOf(digitKey(7), digitKey(8), digitKey(9), ctxSide2))
        val row4 = KeyboardRowSpec(
            listOf(
                KeySpec("ng_alpha", alphaLabel, "", widthWeight = 1f, type = KeyType.MODE_SYMBOLS),
                digitKey(0),
                bottomAux,
                KeySpec("ng_enter", "↵", "\n", widthWeight = 1f, type = KeyType.ENTER),
            )
        )

        return KeyboardLayoutSpec(
            languageId = languageId,
            variant = LayoutVariant.QWERTY,
            displayName = "لوحة الأرقام الشبكية",
            isRtl = false,
            columns = 4,
            rows = listOf(row1, row2, row3, row4),
        )
    }

    fun getEditableBaseRows(languageId: LanguageId, variant: LayoutVariant, useElevenColArabic: Boolean): List<List<KeySpec>> {
        val fileName = resolveAssetFileName(languageId, variant, useElevenColArabic)
        val dto = loadAssetLayout(fileName)
        return dto.rows.mapIndexed { rIdx, list ->
            list.mapIndexed { cIdx, k ->
                KeySpec("edit_${rIdx}_$cIdx", k.label, k.output, k.shiftOutput ?: k.output, k.alternates)
            }
        }
    }
}
