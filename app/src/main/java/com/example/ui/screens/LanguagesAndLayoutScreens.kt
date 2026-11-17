package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CustomKeyOverride
import com.example.model.KeyboardSettings
import com.example.model.LanguageId
import com.example.ui.components.LiveKeyboardPreview
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguagesAndLayoutScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
    onOpenCustomEditor: (LanguageId) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LiveKeyboardPreview(settings = settings, layoutRepo = viewModel.layoutRepo)
        }

        // Arabic 11 vs 10 columns selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تخطيط أعمدة اللغة العربية", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "اختر بين ١١ عموداً (المتراص الكامل) أو ١٠ أعمدة (مفاتيح أعرض)، مع إخفاء مفتاح Shift في العربية",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.useElevenColumnsArabic,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings { it.copy(useElevenColumnsArabic = checked) }
                            },
                            modifier = Modifier.testTag("switch_11_columns"),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.useElevenColumnsArabic,
                            onClick = { viewModel.updateSettings { it.copy(useElevenColumnsArabic = true) } },
                            label = { Text("١١ عموداً (قياسي)") },
                            modifier = Modifier.testTag("chip_arabic_11_col"),
                        )
                        FilterChip(
                            selected = !settings.useElevenColumnsArabic,
                            onClick = { viewModel.updateSettings { it.copy(useElevenColumnsArabic = false) } },
                            label = { Text("١٠ أعمدة (موسّع)") },
                            modifier = Modifier.testTag("chip_arabic_10_col"),
                        )
                    }
                }
            }
        }

        // Prominent Auto-Correction Toggle
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("التصحيح التلقائي للكلمات", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "تفعيل أو تعطيل تصحيح الأخطاء الإملائية تلقائياً عند الضغط على المسطرة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.autoCorrectEnabled,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(autoCorrectEnabled = checked) }
                        },
                        modifier = Modifier.testTag("switch_auto_correct_lang"),
                    )
                }
            }
        }

        // Active & Ordered Languages
        item {
            Text("اللغات المفعّلة وترتيبها", style = MaterialTheme.typography.titleLarge)
        }

        items(settings.enabledLanguages, key = { "enabled_${it.name}" }) { lang ->
            val idx = settings.enabledLanguages.indexOf(lang)
            val isActive = settings.activeLanguage == lang
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = if (isActive) 4.dp else 1.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${lang.arabicName} (${lang.nativeName})", style = MaterialTheme.typography.titleMedium)
                            if (isActive) {
                                Text("اللغة النشطة حالياً", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(
                            onClick = { viewModel.moveLanguageOrder(lang, -1) },
                            enabled = idx > 0,
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "تحريك للأعلى")
                        }
                        IconButton(
                            onClick = { viewModel.moveLanguageOrder(lang, 1) },
                            enabled = idx < settings.enabledLanguages.lastIndex,
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "تحريك للأسفل")
                        }
                        OutlinedButton(onClick = {
                            viewModel.updateSettings { it.copy(activeLanguage = lang) }
                        }) {
                            Text(if (isActive) "نشطة" else "تفعيل")
                        }
                    }

                    // Variant selector for Arabic / English etc.
                    if (lang.availableVariants.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val currentVar = settings.variantFor(lang)
                            lang.availableVariants.forEach { variant ->
                                FilterChip(
                                    selected = currentVar == variant,
                                    onClick = { viewModel.setLayoutVariant(lang, variant) },
                                    label = { Text(variant.arabicLabel) },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpenCustomEditor(lang) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تخصيص مواضع الأحرف")
                        }
                    }
                }
            }
        }

        // All Available Languages toggle list
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("إضافة أو إزالة اللغات المدعومة", style = MaterialTheme.typography.titleLarge)
        }

        items(LanguageId.entries, key = { "all_${it.name}" }) { lang ->
            val enabled = lang in settings.enabledLanguages
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lang.arabicName, style = MaterialTheme.typography.titleMedium)
                        Text(lang.nativeName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.toggleLanguageEnabled(lang) },
                    )
                }
            }
        }
    }
}

@Composable
fun CustomLayoutEditorScreen(
    languageId: LanguageId,
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    val variant = settings.variantFor(languageId)
    val baseRows = remember(languageId, variant, settings.useElevenColumnsArabic) {
        viewModel.layoutRepo.getEditableBaseRows(languageId, variant, settings.useElevenColumnsArabic)
    }
    val overridesMap = remember(settings.customLayoutOverridesJson) {
        viewModel.layoutRepo.parseOverrides(settings.customLayoutOverridesJson)[languageId.name].orEmpty()
    }

    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var editingCell by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    var editAlternates by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "محرر التخطيط المخصص — ${languageId.arabicName}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "اضغط على حرفين متتاليين لتبديل موضعيهما، أو اضغط طويلاً لتعديل الحرف والبدائل يدوياً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        baseRows.forEachIndexed { rIdx, rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (rowKeys.size >= 11) 2.dp else 4.dp),
            ) {
                rowKeys.forEachIndexed { cIdx, baseKey ->
                    val ov = overridesMap.firstOrNull { it.rowIndex == rIdx && it.colIndex == cIdx }
                    val label = ov?.label ?: baseKey.label
                    val isSelected = selectedCell == (rIdx to cIdx)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable {
                                val prev = selectedCell
                                if (prev == null) {
                                    selectedCell = rIdx to cIdx
                                } else if (prev == (rIdx to cIdx)) {
                                    // Second tap on same key -> open edit dialog
                                    val alts = ov?.alternates ?: baseKey.alternates
                                    editAlternates = alts.joinToString(" ")
                                    editingCell = Triple(rIdx, cIdx, label)
                                    selectedCell = null
                                } else {
                                    // Swap prev and current
                                    val prevBase = baseRows[prev.first][prev.second]
                                    val prevOv = overridesMap.firstOrNull { it.rowIndex == prev.first && it.colIndex == prev.second }
                                    val prevLabel = prevOv?.label ?: prevBase.label
                                    val prevOut = prevOv?.output ?: prevBase.output
                                    val prevAlts = prevOv?.alternates ?: prevBase.alternates

                                    val currOut = ov?.output ?: baseKey.output
                                    val currAlts = ov?.alternates ?: baseKey.alternates

                                    viewModel.saveCustomKeyOverride(
                                        languageId,
                                        CustomKeyOverride(prev.first, prev.second, label, currOut, currAlts),
                                    )
                                    viewModel.saveCustomKeyOverride(
                                        languageId,
                                        CustomKeyOverride(rIdx, cIdx, prevLabel, prevOut, prevAlts),
                                    )
                                    selectedCell = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { viewModel.clearCustomLayout(languageId) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("استعادة التخطيط الأصلي")
            }
        }
    }

    editingCell?.let { (rIdx, cIdx, initVal) ->
        var textVal by remember(editingCell) { mutableStateOf(initVal) }
        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = { Text("تعديل المفتاح") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = { textVal = it },
                        label = { Text("الحرف أو النص") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = editAlternates,
                        onValueChange = { editAlternates = it },
                        label = { Text("الأحرف البديلة (مفصولة بمسافة)") },
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clean = textVal.ifBlank { initVal }
                    val alts = editAlternates.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                    viewModel.saveCustomKeyOverride(
                        languageId,
                        CustomKeyOverride(rIdx, cIdx, clean, clean, alts),
                    )
                    editingCell = null
                }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCell = null }) { Text("إلغاء") }
            },
        )
    }
}
