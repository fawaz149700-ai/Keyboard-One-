package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.HandMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemePresets
import com.example.ui.components.LiveKeyboardPreview
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DimensionsScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    var previewLandscape by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Live Preview
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("معاينة الأبعاد الحية", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !previewLandscape,
                        onClick = { previewLandscape = false },
                        label = { Text("عمودي") },
                    )
                    FilterChip(
                        selected = previewLandscape,
                        onClick = { previewLandscape = true },
                        label = { Text("أفقي") },
                    )
                }
            }
            LiveKeyboardPreview(
                settings = settings,
                layoutRepo = viewModel.layoutRepo,
                previewLandscape = previewLandscape,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SliderCard(
                    title = if (previewLandscape) "ارتفاع الكيبورد الكامل (الوضع الأفقي)" else "ارتفاع الكيبورد الكامل (الوضع العمودي)",
                    valueLabel = "${if (previewLandscape) settings.landscapeHeightPercent else settings.portraitHeightPercent}%",
                    value = (if (previewLandscape) settings.landscapeHeightPercent else settings.portraitHeightPercent).toFloat(),
                    range = 50f..150f,
                    onValueChange = { v ->
                        val pct = v.roundToInt()
                        viewModel.updateSettings { s ->
                            if (previewLandscape) s.copy(landscapeHeightPercent = pct)
                            else s.copy(portraitHeightPercent = pct)
                        }
                    },
                )
            }

            item {
                SliderCard(
                    title = "عرض الأزرار",
                    valueLabel = "${settings.keyWidthPercent}%",
                    value = settings.keyWidthPercent.toFloat(),
                    range = 75f..115f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(keyWidthPercent = v.roundToInt()) } },
                )
            }

            item {
                SliderCard(
                    title = "ارتفاع الأزرار",
                    valueLabel = "${settings.keyHeightPercent}%",
                    value = settings.keyHeightPercent.toFloat(),
                    range = 70f..130f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(keyHeightPercent = v.roundToInt()) } },
                )
            }

            item {
                SliderCard(
                    title = "المسافة الأفقية بين الأزرار",
                    valueLabel = "${settings.horizontalGapDp.roundToInt()} dp",
                    value = settings.horizontalGapDp,
                    range = 1f..12f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(horizontalGapDp = v) } },
                )
            }

            item {
                SliderCard(
                    title = "المسافة العمودية بين الصفوف",
                    valueLabel = "${settings.verticalGapDp.roundToInt()} dp",
                    value = settings.verticalGapDp,
                    range = 2f..14f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(verticalGapDp = v) } },
                )
            }

            item {
                SliderCard(
                    title = "حجم الخط على الأزرار",
                    valueLabel = "${settings.keyFontSizeSp.roundToInt()} sp",
                    value = settings.keyFontSizeSp,
                    range = 14f..28f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(keyFontSizeSp = v) } },
                )
            }

            item {
                SliderCard(
                    title = "استدارة زوايا الأزرار",
                    valueLabel = "${settings.keyCornerRadiusDp.roundToInt()} dp",
                    value = settings.keyCornerRadiusDp,
                    range = 0f..20f,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(keyCornerRadiusDp = v) } },
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("وضع اليد الواحدة والكيبورد العائم", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HandMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.handMode == mode,
                                    onClick = { viewModel.setHandMode(mode) },
                                    label = { Text(mode.arabicLabel) },
                                )
                            }
                        }
                        if (settings.handMode == HandMode.FLOATING) {
                            Text("عرض الكيبورد العائم: ${settings.floatingWidthPercent}%")
                            Slider(
                                value = settings.floatingWidthPercent.toFloat(),
                                onValueChange = { v ->
                                    viewModel.updateSettings { it.copy(floatingWidthPercent = v.roundToInt()) }
                                },
                                valueRange = 65f..95f,
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.resetDimensionsToDefaults() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_dimensions_button"),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إعادة جميع الأبعاد للوضع الافتراضي")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemesScreen(
    settings: KeyboardSettings,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.copyPickedBackgroundImage(uri)
        }
    }

    val exportThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(viewModel.exportCustomThemeJson().toByteArray(Charsets.UTF_8))
                    }
                    viewModel.showMessage("تم تصدير الثيم بنجاح")
                }
            }
        }
    }

    val importThemeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (raw != null) viewModel.importCustomThemeJson(raw)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            LiveKeyboardPreview(settings = settings, layoutRepo = viewModel.layoutRepo)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تبديل تلقائي (فاتح / داكن)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "يتبدّل بين فاتح وداكن تلقائياً لثيمات آيفون وجبورد و Material You حسب مظهر النظام",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.autoSystemDarkTheme,
                            onCheckedChange = { v -> viewModel.updateSettings { it.copy(autoSystemDarkTheme = v) } },
                        )
                    }
                }
            }

            val iosPresets = KeyboardThemePresets.allPresets.filter { it.isIosStyle }
            val otherPresets = KeyboardThemePresets.allPresets.filterNot { it.isIosStyle }

            item {
                Text("ثيمات الآيفون (iOS)", style = MaterialTheme.typography.titleLarge)
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    iosPresets.forEach { preset ->
                        val selected = settings.selectedThemeId == preset.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.updateSettings { it.copy(selectedThemeId = preset.id) }
                                }
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp),
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(preset.backgroundColor),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.accentKeyBackgroundColor)),
                                )
                                Text(
                                    text = preset.nameAr,
                                    color = Color(preset.keyTextColor),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(preset.accentKeyBackgroundColor),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("ثيمات أخرى", style = MaterialTheme.typography.titleLarge)
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    otherPresets.forEach { preset ->
                        val selected = settings.selectedThemeId == preset.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.updateSettings { it.copy(selectedThemeId = preset.id) }
                                }
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(14.dp),
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(preset.backgroundColor),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.accentKeyBackgroundColor)),
                                )
                                Text(
                                    text = preset.nameAr,
                                    color = Color(preset.keyTextColor),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(preset.accentKeyBackgroundColor),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Theme Studio
            item {
                Spacer(Modifier.height(4.dp))
                Text("استوديو الثيم المخصص", style = MaterialTheme.typography.titleLarge)
            }

            item {
                val custom = settings.customTheme
                val paletteSwatches = listOf(
                    0xFF14182B, 0xFF000000, 0xFF1E293B, 0xFF2B1D47, 0xFF0B211A,
                    0xFFFFFFFF, 0xFFF1F5F9, 0xFFFCE7F3, 0xFF22D3EE, 0xFFF5B731,
                    0xFF3B82F6, 0xFF10B981, 0xFFEC4899, 0xFF8B5CF6, 0xFFEF4444,
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateSettings { it.copy(selectedThemeId = "custom") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (settings.selectedThemeId == "custom") "الثيم المخصص نشط حالياً ✓" else "تفعيل الثيم المخصص")
                        }

                        ColorRowPicker("لون الخلفية", custom.backgroundColor, paletteSwatches) { c ->
                            viewModel.updateSettings {
                                it.copy(selectedThemeId = "custom", customTheme = it.customTheme.copy(backgroundColor = c, suggestionBarBgColor = c))
                            }
                        }
                        ColorRowPicker("لون الأزرار", custom.keyBackgroundColor, paletteSwatches) { c ->
                            viewModel.updateSettings {
                                it.copy(selectedThemeId = "custom", customTheme = it.customTheme.copy(keyBackgroundColor = c))
                            }
                        }
                        ColorRowPicker("لون الخط", custom.keyTextColor, paletteSwatches) { c ->
                            viewModel.updateSettings {
                                it.copy(selectedThemeId = "custom", customTheme = it.customTheme.copy(keyTextColor = c))
                            }
                        }
                        ColorRowPicker("لون الأزرار الخاصة والمميزة", custom.accentKeyBackgroundColor, paletteSwatches) { c ->
                            viewModel.updateSettings {
                                it.copy(
                                    selectedThemeId = "custom",
                                    customTheme = it.customTheme.copy(
                                        accentKeyBackgroundColor = c,
                                        specialKeyBackgroundColor = c,
                                    ),
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("صورة من المعرض")
                            }
                            if (custom.backgroundImageUri != null) {
                                OutlinedButton(onClick = { viewModel.clearCustomBackgroundImage() }) {
                                    Text("إزالة الصورة")
                                }
                            }
                        }

                        if (custom.backgroundImageUri != null) {
                            Text("شفافية الخلفية: ${(custom.bgOpacity * 100).roundToInt()}%")
                            Slider(
                                value = custom.bgOpacity,
                                onValueChange = { v ->
                                    viewModel.updateSettings {
                                        it.copy(selectedThemeId = "custom", customTheme = it.customTheme.copy(bgOpacity = v))
                                    }
                                },
                                valueRange = 0.15f..1f,
                            )

                            Text("ضبابية الخلفية: ${custom.bgBlurRadius.roundToInt()}")
                            Slider(
                                value = custom.bgBlurRadius,
                                onValueChange = { v ->
                                    viewModel.updateSettings {
                                        it.copy(selectedThemeId = "custom", customTheme = it.customTheme.copy(bgBlurRadius = v))
                                    }
                                },
                                valueRange = 0f..25f,
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { exportThemeLauncher.launch("khatt_theme.json") },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Upload, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("تصدير الثيم")
                            }
                            OutlinedButton(
                                onClick = { importThemeLauncher.launch(arrayOf("application/json")) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("استيراد ثيم")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = range)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorRowPicker(
    title: String,
    selectedColor: Long,
    swatches: List<Long>,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.forEach { hex ->
                val isSel = selectedColor == hex
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (isSel) 3.dp else 1.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .clickable { onSelect(hex) },
                )
            }
        }
    }
}
