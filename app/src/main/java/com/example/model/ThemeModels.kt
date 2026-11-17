package com.example.model

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class KeyboardThemeSpec(
    val id: String,
    val nameAr: String,
    val isDark: Boolean = false,
    val isIosStyle: Boolean = false,
    val isGboardStyle: Boolean = false,
    val isMaterialYou: Boolean = false,
    val backgroundColor: Long,
    val keyBackgroundColor: Long,
    val specialKeyBackgroundColor: Long,
    val accentKeyBackgroundColor: Long,
    val keyTextColor: Long,
    val specialKeyTextColor: Long,
    val accentKeyTextColor: Long,
    val keyShadowColor: Long,
    val keyShadowOffsetDp: Float = 1.5f,
    val popupBackgroundColor: Long,
    val popupTextColor: Long,
    val suggestionBarBgColor: Long,
    val dividerColor: Long,
    val backgroundImageUri: String? = null,
    val bgOpacity: Float = 0.75f,
    val bgBlurRadius: Float = 0f,
) {
    fun toJson(): String = Json { prettyPrint = true; ignoreUnknownKeys = true }.encodeToString(this)

    companion object {
        fun fromJson(raw: String): KeyboardThemeSpec? = runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<KeyboardThemeSpec>(raw)
        }.getOrNull()
    }
}

object KeyboardThemePresets {

    val IosLight = KeyboardThemeSpec(
        id = "ios_light",
        nameAr = "آيفون فاتح (iOS)",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFD1D3D9,
        keyBackgroundColor = 0xFFFFFFFF,
        specialKeyBackgroundColor = 0xFFACB3BF,
        accentKeyBackgroundColor = 0xFF007AFF,
        keyTextColor = 0xFF000000,
        specialKeyTextColor = 0xFF000000,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x55898A8D,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF000000,
        suggestionBarBgColor = 0xFFDADCE2,
        dividerColor = 0x33000000,
    )

    val IosDark = KeyboardThemeSpec(
        id = "ios_dark",
        nameAr = "آيفون داكن (iOS)",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF1C1C1E,
        keyBackgroundColor = 0xFF545458,
        specialKeyBackgroundColor = 0xFF333336,
        accentKeyBackgroundColor = 0xFF0A84FF,
        keyTextColor = 0xFFFFFFFF,
        specialKeyTextColor = 0xFFFFFFFF,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x88000000,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF636366,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF232326,
        dividerColor = 0x33FFFFFF,
    )

    val GboardLight = KeyboardThemeSpec(
        id = "gboard_light",
        nameAr = "جبورد فاتح",
        isDark = false,
        isGboardStyle = true,
        backgroundColor = 0xFFECEFF4,
        keyBackgroundColor = 0xFFFFFFFF,
        specialKeyBackgroundColor = 0xFFD5DCE6,
        accentKeyBackgroundColor = 0xFF1A73E8,
        keyTextColor = 0xFF1F1F1F,
        specialKeyTextColor = 0xFF3C4043,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x2A000000,
        keyShadowOffsetDp = 1.0f,
        popupBackgroundColor = 0xFFD3E3FD,
        popupTextColor = 0xFF041E49,
        suggestionBarBgColor = 0xFFE4E9F0,
        dividerColor = 0x1F000000,
    )

    val GboardDark = KeyboardThemeSpec(
        id = "gboard_dark",
        nameAr = "جبورد داكن",
        isDark = true,
        isGboardStyle = true,
        backgroundColor = 0xFF1B1C1E,
        keyBackgroundColor = 0xFF303236,
        specialKeyBackgroundColor = 0xFF242629,
        accentKeyBackgroundColor = 0xFF8AB4F8,
        keyTextColor = 0xFFE8EAED,
        specialKeyTextColor = 0xFFBDC1C6,
        accentKeyTextColor = 0xFF1A1C1E,
        keyShadowColor = 0x55000000,
        keyShadowOffsetDp = 1.0f,
        popupBackgroundColor = 0xFF44474E,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF212225,
        dividerColor = 0x22FFFFFF,
    )

    val MaterialYou = KeyboardThemeSpec(
        id = "material_you",
        nameAr = "Material You (ألوان النظام)",
        isDark = false,
        isGboardStyle = true,
        isMaterialYou = true,
        backgroundColor = 0xFFEDEFF7,
        keyBackgroundColor = 0xFFFFFFFF,
        specialKeyBackgroundColor = 0xFFD7E3F8,
        accentKeyBackgroundColor = 0xFF3462A4,
        keyTextColor = 0xFF191C22,
        specialKeyTextColor = 0xFF2B3D57,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x22000000,
        keyShadowOffsetDp = 1.0f,
        popupBackgroundColor = 0xFFD7E3F8,
        popupTextColor = 0xFF001B3E,
        suggestionBarBgColor = 0xFFE3E8F3,
        dividerColor = 0x1F000000,
    )

    val AmoledBlack = KeyboardThemeSpec(
        id = "amoled_black",
        nameAr = "أسود AMOLED",
        isDark = true,
        backgroundColor = 0xFF000000,
        keyBackgroundColor = 0xFF141418,
        specialKeyBackgroundColor = 0xFF0D0D11,
        accentKeyBackgroundColor = 0xFF00D2FF,
        keyTextColor = 0xFFF3F5F9,
        specialKeyTextColor = 0xFFAAB2C2,
        accentKeyTextColor = 0xFF06131A,
        keyShadowColor = 0x00000000,
        keyShadowOffsetDp = 0f,
        popupBackgroundColor = 0xFF22222A,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF08080A,
        dividerColor = 0x22FFFFFF,
    )

    val OceanBlue = KeyboardThemeSpec(
        id = "ocean_blue",
        nameAr = "أزرق محيطي",
        isDark = true,
        backgroundColor = 0xFF0E1B2E,
        keyBackgroundColor = 0xFF1B2F4B,
        specialKeyBackgroundColor = 0xFF13243B,
        accentKeyBackgroundColor = 0xFF22D3EE,
        keyTextColor = 0xFFE6F4FE,
        specialKeyTextColor = 0xFF93C5FD,
        accentKeyTextColor = 0xFF04222B,
        keyShadowColor = 0x66060E1A,
        keyShadowOffsetDp = 1.4f,
        popupBackgroundColor = 0xFF2563EB,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF102136,
        dividerColor = 0x29FFFFFF,
    )

    val EmeraldGreen = KeyboardThemeSpec(
        id = "emerald_green",
        nameAr = "أخضر زمردي",
        isDark = true,
        backgroundColor = 0xFF0B211A,
        keyBackgroundColor = 0xFF15372B,
        specialKeyBackgroundColor = 0xFF102B21,
        accentKeyBackgroundColor = 0xFF34D399,
        keyTextColor = 0xFFECFDF5,
        specialKeyTextColor = 0xFF6EE7B7,
        accentKeyTextColor = 0xFF032318,
        keyShadowColor = 0x6604100B,
        keyShadowOffsetDp = 1.4f,
        popupBackgroundColor = 0xFF059669,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF0E271E,
        dividerColor = 0x29FFFFFF,
    )

    val SakuraPink = KeyboardThemeSpec(
        id = "sakura_pink",
        nameAr = "وردي ناعم",
        isDark = false,
        backgroundColor = 0xFFFCE7F3,
        keyBackgroundColor = 0xFFFFF5FA,
        specialKeyBackgroundColor = 0xFFFBCFE8,
        accentKeyBackgroundColor = 0xFFDB2777,
        keyTextColor = 0xFF500724,
        specialKeyTextColor = 0xFF831843,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x339D174D,
        keyShadowOffsetDp = 1.4f,
        popupBackgroundColor = 0xFFF472B6,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFFFDEEF6,
        dividerColor = 0x229D174D,
    )

    val RoyalPurple = KeyboardThemeSpec(
        id = "royal_purple",
        nameAr = "بنفسجي ملكي",
        isDark = true,
        backgroundColor = 0xFF1A102B,
        keyBackgroundColor = 0xFF2B1D47,
        specialKeyBackgroundColor = 0xFF211538,
        accentKeyBackgroundColor = 0xFFA78BFA,
        keyTextColor = 0xFFF5F3FF,
        specialKeyTextColor = 0xFFC4B5FD,
        accentKeyTextColor = 0xFF1E1038,
        keyShadowColor = 0x660C0518,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF7C3AED,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF201435,
        dividerColor = 0x29FFFFFF,
    )

    val AmberMidnight = KeyboardThemeSpec(
        id = "amber_midnight",
        nameAr = "كحلي وذهبي (خط)",
        isDark = true,
        backgroundColor = 0xFF14182B,
        keyBackgroundColor = 0xFF222944,
        specialKeyBackgroundColor = 0xFF1A2037,
        accentKeyBackgroundColor = 0xFFF5B731,
        keyTextColor = 0xFFF4F6FC,
        specialKeyTextColor = 0xFF7CE8FF,
        accentKeyTextColor = 0xFF1A1506,
        keyShadowColor = 0x77090C18,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF2CCEE6,
        popupTextColor = 0xFF0A1622,
        suggestionBarBgColor = 0xFF181E35,
        dividerColor = 0x29FFFFFF,
    )

    val IosSpaceBlack = KeyboardThemeSpec(
        id = "ios_space_black",
        nameAr = "آيفون أسود فلكي (Space Black)",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF0E0E10,
        keyBackgroundColor = 0xFF2C2C2E,
        specialKeyBackgroundColor = 0xFF1C1C1E,
        accentKeyBackgroundColor = 0xFF0A84FF,
        keyTextColor = 0xFFF5F5F7,
        specialKeyTextColor = 0xFFD1D1D6,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x99000000,
        keyShadowOffsetDp = 1.4f,
        popupBackgroundColor = 0xFF3A3A3C,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF141416,
        dividerColor = 0x33FFFFFF,
    )

    val IosNaturalTitanium = KeyboardThemeSpec(
        id = "ios_natural_titanium",
        nameAr = "آيفون تيتانيوم طبيعي",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFC5C2BA,
        keyBackgroundColor = 0xFFF2F0EC,
        specialKeyBackgroundColor = 0xFFA4A096,
        accentKeyBackgroundColor = 0xFF635F56,
        keyTextColor = 0xFF1F1E1B,
        specialKeyTextColor = 0xFF1F1E1B,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x555E5B53,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF1F1E1B,
        suggestionBarBgColor = 0xFFD2CFC7,
        dividerColor = 0x26000000,
    )

    val IosDesertTitanium = KeyboardThemeSpec(
        id = "ios_desert_titanium",
        nameAr = "آيفون تيتانيوم صحراوي",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFE5D7C6,
        keyBackgroundColor = 0xFFFFFAF4,
        specialKeyBackgroundColor = 0xFFCDB9A2,
        accentKeyBackgroundColor = 0xFFB08D6A,
        keyTextColor = 0xFF2B2016,
        specialKeyTextColor = 0xFF3B2D20,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x44856C52,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFFFFFAF4,
        popupTextColor = 0xFF2B2016,
        suggestionBarBgColor = 0xFFEDE1D2,
        dividerColor = 0x24000000,
    )

    val IosBlueTitanium = KeyboardThemeSpec(
        id = "ios_blue_titanium",
        nameAr = "آيفون تيتانيوم أزرق",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF1A222E,
        keyBackgroundColor = 0xFF334154,
        specialKeyBackgroundColor = 0xFF243040,
        accentKeyBackgroundColor = 0xFF5E8AC4,
        keyTextColor = 0xFFF0F4FA,
        specialKeyTextColor = 0xFFC4D3E8,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x880B1018,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF43536B,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF1E2836,
        dividerColor = 0x33FFFFFF,
    )

    val IosSierraBlue = KeyboardThemeSpec(
        id = "ios_sierra_blue",
        nameAr = "آيفون أزرق سييرا",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFCFE0EE,
        keyBackgroundColor = 0xFFF8FBFE,
        specialKeyBackgroundColor = 0xFFA5C1D9,
        accentKeyBackgroundColor = 0xFF4D88B8,
        keyTextColor = 0xFF0E2538,
        specialKeyTextColor = 0xFF18364F,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x44527694,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF0E2538,
        suggestionBarBgColor = 0xFFDAE8F3,
        dividerColor = 0x22000000,
    )

    val IosDeepPurple = KeyboardThemeSpec(
        id = "ios_deep_purple",
        nameAr = "آيفون بنفسجي عميق",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF211A2B,
        keyBackgroundColor = 0xFF423654,
        specialKeyBackgroundColor = 0xFF302640,
        accentKeyBackgroundColor = 0xFF9F7AEA,
        keyTextColor = 0xFFF7F4FC,
        specialKeyTextColor = 0xFFD9CCF0,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x88100B17,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF54466B,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF282034,
        dividerColor = 0x33FFFFFF,
    )

    val IosAlpineGreen = KeyboardThemeSpec(
        id = "ios_alpine_green",
        nameAr = "آيفون أخضر ألبين",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF18261E,
        keyBackgroundColor = 0xFF31473A,
        specialKeyBackgroundColor = 0xFF24362B,
        accentKeyBackgroundColor = 0xFF588A6B,
        keyTextColor = 0xFFF1F7F3,
        specialKeyTextColor = 0xFFC8DDD0,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x880B140E,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF405C4B,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF1D2E24,
        dividerColor = 0x33FFFFFF,
    )

    val IosSilver = KeyboardThemeSpec(
        id = "ios_silver",
        nameAr = "آيفون فضي (Silver)",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFDCDEE3,
        keyBackgroundColor = 0xFFFAFAFC,
        specialKeyBackgroundColor = 0xFFB8BDC7,
        accentKeyBackgroundColor = 0xFF5B6370,
        keyTextColor = 0xFF181A1F,
        specialKeyTextColor = 0xFF242830,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x447B818C,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF181A1F,
        suggestionBarBgColor = 0xFFE5E7EC,
        dividerColor = 0x22000000,
    )

    val IosGold = KeyboardThemeSpec(
        id = "ios_gold",
        nameAr = "آيفون ذهبي (Gold)",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFF0E4D0,
        keyBackgroundColor = 0xFFFFFBF3,
        specialKeyBackgroundColor = 0xFFD9C39F,
        accentKeyBackgroundColor = 0xFFC69749,
        keyTextColor = 0xFF2E220D,
        specialKeyTextColor = 0xFF3D2E14,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x44967B4F,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFDF7,
        popupTextColor = 0xFF2E220D,
        suggestionBarBgColor = 0xFFF5ECDC,
        dividerColor = 0x22000000,
    )

    val IosPink = KeyboardThemeSpec(
        id = "ios_pink",
        nameAr = "آيفون وردي (Pink)",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFF6D9E3,
        keyBackgroundColor = 0xFFFFF7FA,
        specialKeyBackgroundColor = 0xFFE6B6C7,
        accentKeyBackgroundColor = 0xFFE25D8A,
        keyTextColor = 0xFF3D1322,
        specialKeyTextColor = 0xFF4F1D2F,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x449E5E74,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF3D1322,
        suggestionBarBgColor = 0xFFF9E4EB,
        dividerColor = 0x22000000,
    )

    val IosProductRed = KeyboardThemeSpec(
        id = "ios_product_red",
        nameAr = "آيفون أحمر (PRODUCT RED)",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF290E13,
        keyBackgroundColor = 0xFF522029,
        specialKeyBackgroundColor = 0xFF3D161D,
        accentKeyBackgroundColor = 0xFFE62E43,
        keyTextColor = 0xFFFFF2F4,
        specialKeyTextColor = 0xFFFFC7CE,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x88140507,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF6B2A36,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF331117,
        dividerColor = 0x33FFFFFF,
    )

    val IosMidnight = KeyboardThemeSpec(
        id = "ios_midnight",
        nameAr = "آيفون منتصف الليل",
        isDark = true,
        isIosStyle = true,
        backgroundColor = 0xFF131924,
        keyBackgroundColor = 0xFF273245,
        specialKeyBackgroundColor = 0xFF1C2535,
        accentKeyBackgroundColor = 0xFF0A84FF,
        keyTextColor = 0xFFF2F5FA,
        specialKeyTextColor = 0xFFC5D0E0,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x88070B12,
        keyShadowOffsetDp = 1.5f,
        popupBackgroundColor = 0xFF35435C,
        popupTextColor = 0xFFFFFFFF,
        suggestionBarBgColor = 0xFF18202E,
        dividerColor = 0x33FFFFFF,
    )

    val IosStarlight = KeyboardThemeSpec(
        id = "ios_starlight",
        nameAr = "آيفون ضوء النجوم (Starlight)",
        isDark = false,
        isIosStyle = true,
        backgroundColor = 0xFFEAE6DF,
        keyBackgroundColor = 0xFFFCFBF8,
        specialKeyBackgroundColor = 0xFFD2CBC0,
        accentKeyBackgroundColor = 0xFF007AFF,
        keyTextColor = 0xFF1F1D1A,
        specialKeyTextColor = 0xFF2E2B26,
        accentKeyTextColor = 0xFFFFFFFF,
        keyShadowColor = 0x44857E73,
        keyShadowOffsetDp = 1.6f,
        popupBackgroundColor = 0xFFFFFFFF,
        popupTextColor = 0xFF1F1D1A,
        suggestionBarBgColor = 0xFFF1EDE7,
        dividerColor = 0x22000000,
    )

    val allPresets: List<KeyboardThemeSpec> = listOf(
        AmberMidnight,
        IosLight,
        IosDark,
        IosSpaceBlack,
        IosMidnight,
        IosNaturalTitanium,
        IosDesertTitanium,
        IosBlueTitanium,
        IosSierraBlue,
        IosDeepPurple,
        IosAlpineGreen,
        IosSilver,
        IosGold,
        IosStarlight,
        IosPink,
        IosProductRed,
        GboardLight,
        GboardDark,
        MaterialYou,
        AmoledBlack,
        OceanBlue,
        EmeraldGreen,
        SakuraPink,
        RoyalPurple,
    )

    fun resolveTheme(
        context: Context,
        selectedThemeId: String,
        autoSystemDark: Boolean,
        isSystemDark: Boolean,
        customTheme: KeyboardThemeSpec,
    ): KeyboardThemeSpec {
        if (selectedThemeId == "custom") {
            return customTheme
        }

        val effectiveId = if (autoSystemDark) {
            when (selectedThemeId) {
                IosLight.id, IosDark.id -> if (isSystemDark) IosDark.id else IosLight.id
                GboardLight.id, GboardDark.id -> if (isSystemDark) GboardDark.id else GboardLight.id
                else -> selectedThemeId
            }
        } else {
            selectedThemeId
        }

        if (effectiveId == MaterialYou.id && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            return MaterialYou.copy(
                isDark = isSystemDark,
                backgroundColor = scheme.surfaceContainerLow.toArgb().toLong() and 0xFFFFFFFFL,
                keyBackgroundColor = scheme.surfaceContainerHigh.toArgb().toLong() and 0xFFFFFFFFL,
                specialKeyBackgroundColor = scheme.secondaryContainer.toArgb().toLong() and 0xFFFFFFFFL,
                accentKeyBackgroundColor = scheme.primary.toArgb().toLong() and 0xFFFFFFFFL,
                keyTextColor = scheme.onSurface.toArgb().toLong() and 0xFFFFFFFFL,
                specialKeyTextColor = scheme.onSecondaryContainer.toArgb().toLong() and 0xFFFFFFFFL,
                accentKeyTextColor = scheme.onPrimary.toArgb().toLong() and 0xFFFFFFFFL,
                popupBackgroundColor = scheme.primaryContainer.toArgb().toLong() and 0xFFFFFFFFL,
                popupTextColor = scheme.onPrimaryContainer.toArgb().toLong() and 0xFFFFFFFFL,
                suggestionBarBgColor = scheme.surfaceContainer.toArgb().toLong() and 0xFFFFFFFFL,
            )
        }

        return allPresets.firstOrNull { it.id == effectiveId } ?: AmberMidnight
    }
}
