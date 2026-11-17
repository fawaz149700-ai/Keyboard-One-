package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.db.ClipboardItemEntity
import com.example.data.db.ClipboardRepository
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.db.KhattDatabase
import com.example.data.layout.LayoutRepository
import com.example.data.prefs.SettingsRepository
import com.example.data.translate.TranslationRepository
import com.example.model.CustomKeyOverride
import com.example.model.EmojiRenderStyle
import com.example.model.HandMode
import com.example.model.KeyboardSettings
import com.example.model.KeyboardThemeSpec
import com.example.model.LanguageId
import com.example.model.LayoutVariant
import com.example.model.NumeralStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepo = SettingsRepository.getInstance(appContext)
    private val db = KhattDatabase.getInstance(appContext)
    private val clipboardRepo = ClipboardRepository(db.clipboardDao())
    val layoutRepo = LayoutRepository(appContext)
    val translationRepo = TranslationRepository()
    private val backupManager = BackupManager(
        settingsRepo,
        clipboardRepo,
        db.userDictionaryDao(),
        db.favoriteDecorationDao(),
    )

    val settings: StateFlow<KeyboardSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KeyboardSettings())

    val clipboardItems: StateFlow<List<ClipboardItemEntity>> = clipboardRepo.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDecorations: StateFlow<List<FavoriteDecorationEntity>> = db.favoriteDecorationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _downloadedLangModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLangModels: StateFlow<Set<String>> = _downloadedLangModels.asStateFlow()

    private val _downloadingLangModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadingLangModels: StateFlow<Set<String>> = _downloadingLangModels.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refreshDownloadedModels()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun updateSettings(transform: (KeyboardSettings) -> KeyboardSettings) {
        viewModelScope.launch { settingsRepo.update(transform) }
    }

    fun resetDimensionsToDefaults() {
        updateSettings { s ->
            val d = KeyboardSettings()
            s.copy(
                portraitHeightPercent = d.portraitHeightPercent,
                landscapeHeightPercent = d.landscapeHeightPercent,
                keyWidthPercent = d.keyWidthPercent,
                keyHeightPercent = d.keyHeightPercent,
                horizontalGapDp = d.horizontalGapDp,
                verticalGapDp = d.verticalGapDp,
                keyFontSizeSp = d.keyFontSizeSp,
                keyCornerRadiusDp = d.keyCornerRadiusDp,
                handMode = d.handMode,
                floatingOffsetX = 0f,
                floatingOffsetY = 0f,
            )
        }
        showMessage("تمت إعادة الأبعاد إلى الوضع الافتراضي")
    }

    fun toggleLanguageEnabled(lang: LanguageId) {
        updateSettings { s ->
            val current = s.enabledLanguages.toMutableList()
            if (lang in current) {
                if (current.size > 1) current.remove(lang)
            } else {
                current.add(lang)
            }
            val newActive = if (s.activeLanguage in current) s.activeLanguage else current.first()
            s.copy(enabledLanguages = current, activeLanguage = newActive)
        }
    }

    fun moveLanguageOrder(lang: LanguageId, direction: Int) {
        updateSettings { s ->
            val list = s.enabledLanguages.toMutableList()
            val idx = list.indexOf(lang)
            val target = idx + direction
            if (idx >= 0 && target in list.indices) {
                val item = list.removeAt(idx)
                list.add(target, item)
            }
            s.copy(enabledLanguages = list)
        }
    }

    fun setLayoutVariant(lang: LanguageId, variant: LayoutVariant) {
        updateSettings { s ->
            val updated = s.layoutVariants.toMutableMap()
            updated[lang.name] = variant
            s.copy(layoutVariants = updated)
        }
    }

    fun saveCustomKeyOverride(lang: LanguageId, override: CustomKeyOverride) {
        updateSettings { s ->
            val map = layoutRepo.parseOverrides(s.customLayoutOverridesJson).toMutableMap()
            val list = map[lang.name].orEmpty()
                .filterNot { it.rowIndex == override.rowIndex && it.colIndex == override.colIndex }
                .toMutableList()
            list.add(override)
            map[lang.name] = list
            s.copy(customLayoutOverridesJson = layoutRepo.encodeOverrides(map))
        }
    }

    fun clearCustomLayout(lang: LanguageId) {
        updateSettings { s ->
            val map = layoutRepo.parseOverrides(s.customLayoutOverridesJson).toMutableMap()
            map.remove(lang.name)
            s.copy(customLayoutOverridesJson = layoutRepo.encodeOverrides(map))
        }
        showMessage("تمت استعادة التخطيط الافتراضي للغة ${lang.arabicName}")
    }

    fun copyPickedBackgroundImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dest = File(appContext.filesDir, "custom_keyboard_bg_${System.currentTimeMillis()}.jpg")
                appContext.filesDir.listFiles { f -> f.name.startsWith("custom_keyboard_bg_") }?.forEach { it.delete() }
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                val fileUri = Uri.fromFile(dest).toString()
                settingsRepo.update { s ->
                    s.copy(
                        selectedThemeId = "custom",
                        customTheme = s.customTheme.copy(backgroundImageUri = fileUri),
                    )
                }
                showMessage("تم تطبيق صورة الخلفية على الثيم المخصص")
            }.onFailure {
                showMessage("تعذّر تحميل الصورة المختارة")
            }
        }
    }

    fun clearCustomBackgroundImage() {
        updateSettings { s ->
            s.copy(customTheme = s.customTheme.copy(backgroundImageUri = null))
        }
    }

    fun exportCustomThemeJson(): String = settings.value.customTheme.toJson()

    fun importCustomThemeJson(raw: String) {
        val parsed = KeyboardThemeSpec.fromJson(raw)
        if (parsed == null) {
            showMessage("ملف الثيم غير صالح")
            return
        }
        updateSettings { s ->
            s.copy(
                selectedThemeId = "custom",
                customTheme = parsed.copy(id = "custom"),
            )
        }
        showMessage("تم استيراد الثيم المخصص بنجاح")
    }

    fun addManualTemplate(label: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            clipboardRepo.saveClip(
                rawText = text,
                maxItems = settings.value.clipboardMaxItems,
                retentionDays = settings.value.clipboardRetentionDays,
                isTemplate = true,
                pinned = true,
                label = label.ifBlank { "قالب" },
            )
            showMessage("تمت إضافة القالب وتثبيته في الحافظة")
        }
    }

    fun toggleClipPin(item: ClipboardItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { clipboardRepo.togglePin(item) }
    }

    fun deleteClip(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { clipboardRepo.deleteItem(id) }
    }

    fun clearUnpinnedClips() {
        viewModelScope.launch(Dispatchers.IO) { clipboardRepo.clearAllUnpinned() }
        showMessage("تم حذف العناصر غير المثبّتة")
    }

    fun deleteFavoriteDecoration(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { db.favoriteDecorationDao().deleteById(id) }
    }

    fun refreshDownloadedModels() {
        viewModelScope.launch {
            runCatching { translationRepo.getDownloadedLanguageCodes() }
                .onSuccess { _downloadedLangModels.value = it }
        }
    }

    fun downloadLanguageModel(code: String) {
        viewModelScope.launch {
            _downloadingLangModels.value = _downloadingLangModels.value + code
            val res = translationRepo.downloadLanguageModel(code)
            _downloadingLangModels.value = _downloadingLangModels.value - code
            res.onSuccess {
                refreshDownloadedModels()
                showMessage("تم تنزيل حزمة اللغة للترجمة بدون إنترنت")
            }.onFailure {
                showMessage("فشل التنزيل: ${it.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}")
            }
        }
    }

    fun deleteLanguageModel(code: String) {
        viewModelScope.launch {
            translationRepo.deleteLanguageModel(code)
                .onSuccess {
                    refreshDownloadedModels()
                    showMessage("تم حذف حزمة اللغة")
                }
        }
    }

    suspend fun exportBackupToUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                val jsonText = backupManager.exportBackupJson()
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonText.toByteArray(Charsets.UTF_8))
                }
                showMessage("تم حفظ النسخة الاحتياطية بنجاح")
            }.onFailure {
                showMessage("فشل التصدير: ${it.localizedMessage}")
            }
        }
    }

    suspend fun importBackupFromUri(uri: Uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("تعذرت قراءة الملف")
                backupManager.importBackupJson(text).getOrThrow()
                showMessage("تمت استعادة النسخة الاحتياطية بنجاح")
            }.onFailure {
                showMessage("فشل الاستعادة: ${it.localizedMessage}")
            }
        }
    }

    fun setBarLevel(level: Int) = updateSettings { it.withBarLevel(level) }
    fun setHandMode(mode: HandMode) = updateSettings { it.copy(handMode = mode) }
    fun setNumeralStyle(style: NumeralStyle) = updateSettings { it.copy(numeralStyle = style) }
    fun setEmojiRenderStyle(style: EmojiRenderStyle) = updateSettings { it.copy(emojiRenderStyle = style) }
}
