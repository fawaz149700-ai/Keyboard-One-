package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.emoji.EmojiCatalog
import com.example.data.layout.LayoutRepository
import com.example.ime.KeyboardCanvasView
import com.example.model.EmojiRenderStyle
import com.example.model.HandMode
import com.example.model.KeyboardMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePresets
import com.example.model.ShiftState

@Composable
fun LiveKeyboardPreview(
    settings: KeyboardSettings,
    layoutRepo: LayoutRepository,
    previewLandscape: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val theme = remember(settings.selectedThemeId, settings.autoSystemDarkTheme, isSystemDark, settings.customTheme) {
        KeyboardThemePresets.resolveTheme(
            context = context,
            selectedThemeId = settings.selectedThemeId,
            autoSystemDark = settings.autoSystemDarkTheme,
            isSystemDark = isSystemDark,
            customTheme = settings.customTheme,
        )
    }

    val layoutSpec = remember(
        settings.activeLanguage,
        settings.layoutVariants,
        settings.useElevenColumnsArabic,
        settings.customLayoutOverridesJson,
        settings.barLevel,
        settings.customBarsOverride,
        settings.isSuggestionsBarVisible,
        settings.isNumberRowVisible,
        settings.isQuickEmojiBarVisible,
        settings.numeralStyle,
        settings.toolbarScalePercent,
    ) {
        layoutRepo.buildLayout(
            settings = settings,
            mode = KeyboardMode.ALPHA,
            includeNumberRow = settings.showNumberRow,
        )
    }

    val previewAlignment = when (settings.handMode) {
        HandMode.ONE_HAND_LEFT -> Alignment.CenterStart
        HandMode.ONE_HAND_RIGHT -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val widthFraction = when (settings.handMode) {
        HandMode.FULL -> 1f
        HandMode.ONE_HAND_LEFT, HandMode.ONE_HAND_RIGHT -> 0.84f
        HandMode.FLOATING -> (settings.floatingWidthPercent / 100f).coerceIn(0.65f, 0.95f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0D18))
                .padding(8.dp),
            contentAlignment = previewAlignment,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(theme.backgroundColor)),
            ) {
                val toolbarScale = (settings.toolbarScalePercent / 100f).coerceIn(0.70f, 1.45f)

                // 1. Suggestions Bar
                AnimatedVisibility(visible = settings.showSuggestionsBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(theme.suggestionBarBgColor))
                            .padding(horizontal = 8.dp, vertical = (5f * toolbarScale.coerceIn(0.75f, 1.35f)).dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val sampleSugs = if (settings.activeLanguage.isRtl) {
                            listOf("السلام", "مرحباً", "كيبورد خط")
                        } else {
                            listOf("Hello", "Khatt", "Thanks")
                        }
                        sampleSugs.forEach { s ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(theme.keyBackgroundColor))
                                    .padding(
                                        horizontal = (10f * toolbarScale.coerceIn(0.85f, 1.25f)).dp,
                                        vertical = (4f * toolbarScale.coerceIn(0.75f, 1.35f)).dp,
                                    ),
                            ) {
                                Text(
                                    text = s,
                                    color = Color(theme.keyTextColor),
                                    fontSize = (12f * toolbarScale.coerceIn(0.8f, 1.25f)).sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }

                // 2. Quick Emoji Bar (Settings key FIRST on opposite side + 9 emoji/custom-text square keys)
                AnimatedVisibility(visible = settings.showQuickEmojiBar) {
                    val items = settings.quickEmojiBarItems.take(9)
                    val cornerDp = if (theme.isIosStyle) settings.keyCornerRadiusDp.coerceAtLeast(5.5f) else settings.keyCornerRadiusDp
                    val barBoxHeight = (34f * toolbarScale.coerceIn(0.75f, 1.35f)).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(theme.backgroundColor))
                            .padding(horizontal = 4.dp, vertical = (3f * toolbarScale.coerceIn(0.75f, 1.35f)).dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barBoxHeight)
                                .clip(RoundedCornerShape(cornerDp.dp))
                                .background(Color(theme.specialKeyBackgroundColor))
                                .border(0.7.dp, Color(theme.keyShadowColor), RoundedCornerShape(cornerDp.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "⚙",
                                fontSize = (15f * toolbarScale.coerceIn(0.8f, 1.25f)).sp,
                                color = Color(theme.specialKeyTextColor),
                            )
                        }
                        for (i in 0 until 9) {
                            val em = items.getOrNull(i).orEmpty()
                            val isText = em.any { it.isLetterOrDigit() || it.isWhitespace() || it in "@._-/#+•★✓?!" } || em.length > 4
                            val displaySp = when {
                                !isText -> (settings.quickEmojiSizeSp * toolbarScale.coerceIn(0.82f, 1.22f)).coerceIn(12f, 22f)
                                em.length <= 2 -> settings.quickEmojiSizeSp.coerceIn(12f, 18f)
                                em.length <= 5 -> 11f
                                else -> 8.5f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(barBoxHeight)
                                    .clip(RoundedCornerShape(cornerDp.dp))
                                    .background(Color(if (em.isNotEmpty()) theme.keyBackgroundColor else theme.specialKeyBackgroundColor))
                                    .border(0.7.dp, Color(theme.keyShadowColor), RoundedCornerShape(cornerDp.dp))
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = em,
                                    fontSize = displaySp.sp,
                                    color = Color(theme.keyTextColor),
                                    fontWeight = if (isText) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }

                // 3 & 4. Number row + Letter rows rendered by actual KeyboardCanvasView
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx -> KeyboardCanvasView(ctx) },
                    update = { canvasView ->
                        canvasView.updateState(
                            layout = layoutSpec,
                            settings = settings,
                            theme = theme,
                            shiftState = ShiftState.OFF,
                            isLandscape = previewLandscape,
                        )
                    },
                )
            }
        }
    }
}
