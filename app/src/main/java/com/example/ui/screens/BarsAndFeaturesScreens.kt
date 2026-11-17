package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.EmojiRenderStyle
import com.example.model.KeyboardSettings
import com.example.model.NumeralStyle
import com.example.ui.components.LiveKeyboardPreview
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarsAndGesturesScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LiveKeyboardPreview(settings = settings, layoutRepo = viewModel.layoutRepo)
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("التحكم في الأشرطة العلوية", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "السحب للأسفل على المسطرة يُخفي بالترتيب: ١. الاقتراحات والأدوات ← ٢. صف الأرقام ← ٣. شريط الفيسات",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.showSuggestionsBar,
                            onClick = {
                                viewModel.updateSettings {
                                    it.copy(
                                        customBarsOverride = true,
                                        isSuggestionsBarVisible = !it.showSuggestionsBar,
                                        isNumberRowVisible = it.showNumberRow,
                                        isQuickEmojiBarVisible = it.showQuickEmojiBar,
                                    )
                                }
                            },
                            label = { Text("١. الاقتراحات والأدوات") },
                        )
                        FilterChip(
                            selected = settings.showNumberRow,
                            onClick = {
                                viewModel.updateSettings {
                                    it.copy(
                                        customBarsOverride = true,
                                        isSuggestionsBarVisible = it.showSuggestionsBar,
                                        isNumberRowVisible = !it.showNumberRow,
                                        isQuickEmojiBarVisible = it.showQuickEmojiBar,
                                    )
                                }
                            },
                            label = { Text("٢. صف الأرقام") },
                        )
                        FilterChip(
                            selected = settings.showQuickEmojiBar,
                            onClick = {
                                viewModel.updateSettings {
                                    it.copy(
                                        customBarsOverride = true,
                                        isSuggestionsBarVisible = it.showSuggestionsBar,
                                        isNumberRowVisible = it.showNumberRow,
                                        isQuickEmojiBarVisible = !it.showQuickEmojiBar,
                                    )
                                }
                            },
                            label = { Text("٣. شريط الفيسات") },
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val labels = listOf(
                            0 to "مخفي بالكامل (0)",
                            1 to "الاقتراحات (1)",
                            2 to "+ الفيسات (2)",
                            3 to "الكل ظاهر (3)",
                        )
                        labels.forEach { (lvl, lbl) ->
                            FilterChip(
                                selected = !settings.customBarsOverride && settings.barLevel == lvl,
                                onClick = { viewModel.setBarLevel(lvl) },
                                label = { Text(lbl) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حجم أشرطة الأدوات (${settings.toolbarScalePercent}%)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "التحكم الكامل في ارتفاع وحجم شريط الاقتراحات، شريط الحافظة، شريط الترجمة، شريط الفيسات، والشريط السفلي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.toolbarScalePercent.toFloat(),
                        onValueChange = { v -> viewModel.updateSettings { it.copy(toolbarScalePercent = v.roundToInt().coerceIn(70, 145)) } },
                        valueRange = 70f..145f,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(85 to "صغير (85%)", 100 to "افتراضي (100%)", 125 to "كبير (125%)").forEach { (pct, lbl) ->
                            FilterChip(
                                selected = settings.toolbarScalePercent == pct,
                                onClick = { viewModel.updateSettings { it.copy(toolbarScalePercent = pct) } },
                                label = { Text(lbl) },
                            )
                        }
                    }
                }
            }
        }

        item {
            ToggleCard(
                title = "إيماءة السحب العمودي على المسطرة",
                subtitle = "السحب للأسفل: يُخفي الاقتراحات ← الأرقام ← الفيسات؛ السحب للأعلى: يُعيد إظهارها",
                checked = settings.spaceSwipeBarsEnabled,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(spaceSwipeBarsEnabled = v) } },
            )
        }

        item {
            ToggleCard(
                title = "عكس اتجاه السحب العمودي",
                subtitle = "جعل السحب للأسفل يُظهر الأشرطة وللأعلى يُخفيها",
                checked = settings.invertSpaceSwipe,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(invertSpaceSwipe = v) } },
            )
        }

        item {
            ToggleCard(
                title = "اهتزاز خفيف عند تغيّر حالة الشريط",
                subtitle = "تأكيد لمسي لطيف مع كل خطوة سحب على زر المسطرة",
                checked = settings.hapticOnSpaceSwipe,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(hapticOnSpaceSwipe = v) } },
            )
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val speedDesc = when {
                        settings.spaceSwipeSensitivityDp <= 16 -> "سريع جداً (سحبة قصيرة)"
                        settings.spaceSwipeSensitivityDp <= 28 -> "متوازن (افتراضي)"
                        else -> "بطيء / مسافة أطول"
                    }
                    Text(
                        "حساسية وسرعة السحب على المسطرة (${settings.spaceSwipeSensitivityDp} dp • $speedDesc)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "تحكّم في المسافة المطلوبة للسحب على زر المسافة لإخفاء أو إظهار الأشرطة: القيمة الأقل تعني استجابة أسرع.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.spaceSwipeSensitivityDp.toFloat(),
                        onValueChange = { v ->
                            viewModel.updateSettings { it.copy(spaceSwipeSensitivityDp = v.roundToInt().coerceIn(12, 56)) }
                        },
                        valueRange = 12f..56f,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(14 to "سريع (14 dp)", 24 to "متوازن (24 dp)", 40 to "بطيء (40 dp)").forEach { (dpVal, lbl) ->
                            FilterChip(
                                selected = settings.spaceSwipeSensitivityDp == dpVal,
                                onClick = { viewModel.updateSettings { it.copy(spaceSwipeSensitivityDp = dpVal) } },
                                label = { Text(lbl) },
                            )
                        }
                    }
                }
            }
        }

        item {
            var customTokenInput by remember { mutableStateOf("") }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إدارة شريط الفيسات والنصوص المخصصة (⚙ + ٩)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "اضغط على أي عنصر لحذفه، أو أضف فيساً أو نصاً أو حرفاً مخصصاً (يُضاف كنسخة إضافية دون حذف أو نقل الحرف الأصلي من الكيبورد):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        settings.quickEmojiBarItems.take(9).forEachIndexed { idx, em ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    val next = settings.quickEmojiBarItems.toMutableList()
                                    if (idx in next.indices) next.removeAt(idx)
                                    viewModel.updateSettings { it.copy(quickEmojiBarItems = next) }
                                },
                                label = { Text("$em  ✕") },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = customTokenInput,
                            onValueChange = { customTokenInput = it },
                            label = { Text("نص أو حرف أو فيس مخصص") },
                            placeholder = { Text("مثل: السلام عليكم أو ♥ أو @") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(
                            onClick = {
                                val clean = customTokenInput.trim()
                                if (clean.isNotEmpty()) {
                                    val cur = settings.quickEmojiBarItems.toMutableList()
                                    if (clean !in cur) {
                                        if (cur.size < 9) cur.add(clean) else cur[cur.lastIndex] = clean
                                    }
                                    viewModel.updateSettings { it.copy(quickEmojiBarItems = cur) }
                                    customTokenInput = ""
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة")
                        }
                    }
                    Text("إضافة سريع للشريط:", style = MaterialTheme.typography.labelLarge)
                    val quickSuggestions = listOf("😂", "❤️", "😍", "😭", "🙏", "👍", "🔥", "😊", "✨", "🥰", "🤣", "♥", "★", "✓", "السلام عليكم", "شكراً", "بإذن الله")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSuggestions.filter { it !in settings.quickEmojiBarItems }.forEach { em ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cur = settings.quickEmojiBarItems.toMutableList()
                                    if (cur.size < 9) cur.add(em) else cur[cur.lastIndex] = em
                                    viewModel.updateSettings { it.copy(quickEmojiBarItems = cur) }
                                },
                                label = { Text("+ $em") },
                            )
                        }
                    }
                    Text("حجم الفيسات في الشريط: ${settings.quickEmojiSizeSp.roundToInt()} sp")
                    Slider(
                        value = settings.quickEmojiSizeSp,
                        onValueChange = { v -> viewModel.updateSettings { it.copy(quickEmojiSizeSp = v) } },
                        valueRange = 13f..28f,
                    )
                    OutlinedButton(
                        onClick = {
                            val def = KeyboardSettings()
                            viewModel.updateSettings {
                                it.copy(quickEmojiBarItems = def.quickEmojiBarItems, quickEmojiSizeSp = def.quickEmojiSizeSp)
                            }
                        },
                    ) {
                        Text("استعادة الفيسات الافتراضية")
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نمط الأرقام في شريط الأرقام", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumeralStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.numeralStyle == style,
                                onClick = { viewModel.setNumeralStyle(style) },
                                label = { Text(style.arabicLabel) },
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نمط عرض الإيموجي", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmojiRenderStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.emojiRenderStyle == style,
                                onClick = { viewModel.setEmojiRenderStyle(style) },
                                label = { Text(style.arabicLabel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClipboardSettingsScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    val clips by viewModel.clipboardItems.collectAsStateWithLifecycle()
    var templateLabel by remember { mutableStateOf("") }
    var templateText by remember { mutableStateOf("") }
    var pendingDeleteItemId by remember { mutableStateOf<Long?>(null) }
    var pendingClearUnpinned by remember { mutableStateOf(false) }

    if (pendingDeleteItemId != null) {
        val targetId = pendingDeleteItemId!!
        AlertDialog(
            onDismissRequest = { pendingDeleteItemId = null },
            title = { Text("هل أنت متأكد من الحذف؟") },
            text = { Text("سيتم حذف هذا النص من سجل الحافظة نهائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClip(targetId)
                        pendingDeleteItemId = null
                    },
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItemId = null }) {
                    Text("إلغاء")
                }
            },
        )
    }

    if (pendingClearUnpinned) {
        AlertDialog(
            onDismissRequest = { pendingClearUnpinned = false },
            title = { Text("هل أنت متأكد من الحذف؟") },
            text = { Text("سيتم حذف جميع العناصر غير المثبّتة من الحافظة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearUnpinnedClips()
                        pendingClearUnpinned = false
                    },
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearUnpinned = false }) {
                    Text("إلغاء")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ToggleCard(
                title = "الحفظ التلقائي للنصوص المنسوخة",
                subtitle = "يتم تجاهل حقول كلمات المرور والحساسة تلقائياً",
                checked = settings.clipboardAutoSave,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(clipboardAutoSave = v) } },
            )
        }

        item {
            ToggleCard(
                title = "قفل الحافظة بالبصمة",
                subtitle = "يتطلب البصمة أو رمز الجهاز لفتح الحافظة داخل الكيبورد",
                checked = settings.clipboardBiometricLock,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(clipboardBiometricLock = v) } },
            )
        }

        item {
            ToggleCard(
                title = "قفل الحافظة مباشرة",
                subtitle = "إغلاق شريط الحافظة تلقائياً فور لصق أو نسخ نص منه أو عند النقر المزدوج داخل حقل الكتابة في التطبيقات",
                checked = settings.clipboardAutoClose,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(clipboardAutoClose = v) } },
            )
        }

        item {
            ToggleCard(
                title = "تأكيد الحذف من الحافظة",
                subtitle = "عرض رسالة تأكيد «هل أنت متأكد من الحذف؟» قبل حذف أي نص أو تنظيف الحافظة",
                checked = settings.confirmClipboardDelete,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(confirmClipboardDelete = v) } },
                switchTestTag = "switch_confirm_clip_delete",
            )
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سعة الحافظة ومدة الاحتفاظ", style = MaterialTheme.typography.titleMedium)
                    val maxLabel = if (settings.clipboardMaxItems <= 0) "لا نهائي (∞)" else "${settings.clipboardMaxItems} عنصر"
                    val daysLabel = if (settings.clipboardRetentionDays <= 0) "لا نهائي (∞ — بدون حذف تلقائي)" else "${settings.clipboardRetentionDays} يوماً"
                    Text("الحد الأقصى للعناصر: $maxLabel", style = MaterialTheme.typography.bodyMedium)
                    Text("مدة الاحتفاظ بالنصوص: $daysLabel", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.clipboardMaxItems <= 0 && settings.clipboardRetentionDays <= 0,
                            onClick = { viewModel.updateSettings { it.copy(clipboardMaxItems = 0, clipboardRetentionDays = 0) } },
                            label = { Text("لا نهائي بالكامل (∞)") },
                        )
                        FilterChip(
                            selected = settings.clipboardGridMode,
                            onClick = { viewModel.updateSettings { it.copy(clipboardGridMode = !it.clipboardGridMode) } },
                            label = { Text(if (settings.clipboardGridMode) "العرض: شبكي (Grid)" else "العرض: صفي (List)") },
                        )
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إضافة قالب نص جاهز (عنوان، بريد، رقم...)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = templateLabel,
                        onValueChange = { templateLabel = it },
                        label = { Text("عنوان القالب (مثل: بريدي)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = templateText,
                        onValueChange = { templateText = it },
                        label = { Text("النص الجاهز") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.addManualTemplate(templateLabel, templateText)
                                templateLabel = ""
                                templateText = ""
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ وتثبيت القالب")
                        }
                        OutlinedButton(
                            onClick = {
                                if (settings.confirmClipboardDelete) {
                                    pendingClearUnpinned = true
                                } else {
                                    viewModel.clearUnpinnedClips()
                                }
                            },
                        ) {
                            Text("تنظيف غير المثبّت")
                        }
                    }
                }
            }
        }

        item {
            Text("العناصر المحفوظة (${clips.size})", style = MaterialTheme.typography.titleLarge)
        }

        items(clips, key = { it.id }) { item ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!item.label.isNullOrBlank()) {
                            Text(item.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(item.text, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
                    }
                    IconButton(onClick = { viewModel.toggleClipPin(item) }) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "تثبيت",
                            tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            if (settings.confirmClipboardDelete) {
                                pendingDeleteItemId = item.id
                            } else {
                                viewModel.deleteClip(item.id)
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف")
                    }
                }
            }
        }
    }
}

@Composable
fun TranslationSettingsScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    val downloaded by viewModel.downloadedLangModels.collectAsStateWithLifecycle()
    val downloading by viewModel.downloadingLangModels.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ToggleCard(
                title = "دعم الترجمة السحابية عند الحاجة",
                subtitle = "استخدام ترجمة سحابية اختيارية عند توفر الإنترنت في حال عدم تنزيل حزمة اللغة",
                checked = settings.cloudTranslateFallbackEnabled,
                onCheckedChange = { v -> viewModel.updateSettings { it.copy(cloudTranslateFallbackEnabled = v) } },
            )
        }

        item {
            Text("حزم الترجمة دون اتصال (ML Kit Translate)", style = MaterialTheme.typography.titleLarge)
        }

        items(viewModel.translationRepo.supportedLanguages, key = { it.code }) { lang ->
            val isDownloaded = lang.code in downloaded
            val isBusy = lang.code in downloading
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lang.arabicName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isDownloaded) "منزّلة وجاهزة بدون إنترنت ✓" else "غير منزّلة (~30 MB)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else if (isDownloaded) {
                        OutlinedButton(onClick = { viewModel.deleteLanguageModel(lang.code) }) {
                            Text("حذف")
                        }
                    } else {
                        Button(onClick = { viewModel.downloadLanguageModel(lang.code) }) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تنزيل")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecorateAndFeedbackScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
    sectionMode: String, // "decorate", "feedback", "backup"
) {
    val favorites by viewModel.favoriteDecorations.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch { viewModel.exportBackupToUri(uri) }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch { viewModel.importBackupFromUri(uri) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (sectionMode == "decorate") {
            item {
                ToggleCard(
                    title = "الزخرفة الفورية أثناء الكتابة (فواز)",
                    subtitle = "حقن الزخرفة «ــــ͡ــٰ̡ـ» تلقائياً لحظة الكتابة مع الأحرف (ك، ي، س، ش، ق، ج، خ، ع، غ) بشرط ألا تكون في نهاية الكلمة",
                    checked = settings.realTimeDecorationEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(realTimeDecorationEnabled = v) } },
                    switchTestTag = "switch_realtime_decorate_tab",
                )
            }
            item {
                Text("الأنماط المحفوظة في المفضلة", style = MaterialTheme.typography.titleLarge)
                Text(
                    "يمكنك إضافة أي نمط وإطار من زر الزخرفة ✨ داخل الكيبورد لتظهر هنا وفي شريط المفضلة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (favorites.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Text(
                            "لا توجد زخارف مفضلة محفوظة بعد.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(favorites, key = { it.id }) { fav ->
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.title, style = MaterialTheme.typography.titleMedium)
                                Text("${fav.prefix}مثال${fav.suffix}", style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { viewModel.deleteFavoriteDecoration(fav.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "إزالة من المفضلة")
                            }
                        }
                    }
                }
            }
        }

        if (sectionMode == "feedback") {
            item {
                ToggleCard(
                    title = "الزخرفة الفورية أثناء الكتابة (فواز)",
                    subtitle = "حقن الزخرفة «ــــ͡ــٰ̡ـ» تلقائياً لحظة الكتابة مع الأحرف (ك، ي، س، ش، ق، ج، خ، ع، غ) وسط الكلمة",
                    checked = settings.realTimeDecorationEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(realTimeDecorationEnabled = v) } },
                    switchTestTag = "switch_realtime_decorate",
                )
            }
            item {
                ToggleCard(
                    title = "الذكاء الاصطناعي في إدخال الأرقام",
                    subtitle = "اكتشاف حقول الهواتف والمبالغ المالية وكلمات المرور الرقمية وفتح النمط الشبكي للأرقام تلقائياً",
                    checked = settings.smartNumericGridEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(smartNumericGridEnabled = v) } },
                )
            }
            item {
                ToggleCard(
                    title = "الاقتراحات والتنبؤ الذكي",
                    subtitle = "تُعطل تلقائياً في حقول كلمات المرور",
                    checked = settings.suggestionsEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(suggestionsEnabled = v) } },
                )
            }
            item {
                ToggleCard(
                    title = "التصحيح التلقائي للكلمات",
                    subtitle = "يصحح الأخطاء الإملائية تلقائياً عند الضغط على المسطرة أو يمكنك تعطيله هنا",
                    checked = settings.autoCorrectEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(autoCorrectEnabled = v) } },
                    switchTestTag = "switch_auto_correct",
                )
            }
            item {
                ToggleCard(
                    title = "نافذة معاينة الحرف المضغوط",
                    subtitle = "تظهر أعلى المفتاح عند الضغط للتأكد من الحرف",
                    checked = settings.keyPreviewPopupEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(keyPreviewPopupEnabled = v) } },
                )
            }
            item {
                ToggleCard(
                    title = "الاهتزاز عند الضغط",
                    subtitle = "استجابة لمسية فورية مع كتابة كل حرف",
                    checked = settings.hapticEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(hapticEnabled = v) } },
                )
            }
            if (settings.hapticEnabled) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("شدة الاهتزاز: ${settings.hapticStrengthMs} ms")
                            Slider(
                                value = settings.hapticStrengthMs.toFloat(),
                                onValueChange = { v -> viewModel.updateSettings { it.copy(hapticStrengthMs = v.roundToInt()) } },
                                valueRange = 5f..45f,
                            )
                        }
                    }
                }
            }
            item {
                ToggleCard(
                    title = "الصوت عند الضغط",
                    subtitle = "صوت نقر خفيف متزامن مع مفاتيح النظام",
                    checked = settings.soundEnabled,
                    onCheckedChange = { v -> viewModel.updateSettings { it.copy(soundEnabled = v) } },
                )
            }
        }

        if (sectionMode == "backup") {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("النسخ الاحتياطي والاستعادة الشاملة", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "يشمل النسخ الاحتياطي جميع الإعدادات، التخطيطات المخصصة، قاموس المستخدم المتعلّم، العناصر المثبّتة في الحافظة، والزخارف المفضلة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { createBackupLauncher.launch("khatt_backup.json") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تصدير نسخة احتياطية (JSON)")
                        }
                        OutlinedButton(
                            onClick = { openBackupLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("استعادة من نسخة احتياطية")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
            )
        }
    }
}
