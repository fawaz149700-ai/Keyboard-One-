package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.KeyboardSettings
import com.example.model.LanguageId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.khattDataStore: DataStore<Preferences> by preferencesDataStore(name = "khatt_keyboard_prefs")

class SettingsRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val settingsKey = stringPreferencesKey("keyboard_settings_json")

    val settingsFlow: Flow<KeyboardSettings> = context.khattDataStore.data.map { prefs ->
        val raw = prefs[settingsKey]
        if (raw.isNullOrBlank()) {
            KeyboardSettings()
        } else {
            runCatching { json.decodeFromString<KeyboardSettings>(raw) }
                .getOrElse { KeyboardSettings() }
        }
    }

    suspend fun getCurrent(): KeyboardSettings = settingsFlow.first()

    suspend fun update(transform: (KeyboardSettings) -> KeyboardSettings) {
        context.khattDataStore.edit { prefs ->
            val current = prefs[settingsKey]?.let {
                runCatching { json.decodeFromString<KeyboardSettings>(it) }.getOrNull()
            } ?: KeyboardSettings()
            val updated = transform(current)
            prefs[settingsKey] = json.encodeToString(updated)
        }
    }

    suspend fun stepBarLevel(delta: Int): Int {
        var newLevel = 1
        update { s ->
            newLevel = (s.barLevel + delta).coerceIn(0, 3)
            s.withBarLevel(newLevel)
        }
        return newLevel
    }

    suspend fun handleSpacebarVerticalSwipe(isUp: Boolean): KeyboardSettings {
        var updated = KeyboardSettings()
        update { s ->
            updated = s.applySpaceVerticalSwipe(isUp)
            updated
        }
        return updated
    }

    suspend fun cycleLanguage(direction: Int): LanguageId {
        var target = LanguageId.ARABIC
        update { s ->
            val list = s.enabledLanguages.ifEmpty { listOf(LanguageId.ARABIC, LanguageId.ENGLISH) }
            val idx = list.indexOf(s.activeLanguage).takeIf { it >= 0 } ?: 0
            val nextIdx = (idx + direction).mod(list.size)
            target = list[nextIdx]
            s.copy(activeLanguage = target)
        }
        return target
    }

    suspend fun resetToDefaults() {
        update { KeyboardSettings(onboardingCompleted = it.onboardingCompleted) }
    }

    suspend fun exportSettingsJson(): String = json.encodeToString(getCurrent())

    suspend fun importSettingsJson(raw: String): Boolean {
        val parsed = runCatching { json.decodeFromString<KeyboardSettings>(raw) }.getOrNull() ?: return false
        context.khattDataStore.edit { prefs ->
            prefs[settingsKey] = json.encodeToString(parsed)
        }
        return true
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
