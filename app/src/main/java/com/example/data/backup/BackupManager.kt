package com.example.data.backup

import com.example.data.db.ClipboardItemEntity
import com.example.data.db.ClipboardRepository
import com.example.data.db.FavoriteDecorationDao
import com.example.data.db.FavoriteDecorationEntity
import com.example.data.db.UserDictionaryDao
import com.example.data.db.UserWordEntity
import com.example.data.prefs.SettingsRepository
import com.example.model.KeyboardSettings
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class KhattBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val settings: KeyboardSettings,
    val userWords: List<UserWordEntity> = emptyList(),
    val pinnedClipboard: List<ClipboardItemEntity> = emptyList(),
    val favoriteDecorations: List<FavoriteDecorationEntity> = emptyList(),
)

class BackupManager(
    private val settingsRepository: SettingsRepository,
    private val clipboardRepository: ClipboardRepository,
    private val userDictionaryDao: UserDictionaryDao,
    private val favoriteDecorationDao: FavoriteDecorationDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportBackupJson(): String {
        val settings = settingsRepository.getCurrent()
        val words = userDictionaryDao.getAllSnapshot()
        val pinned = clipboardRepository.getPinnedSnapshot()
        val favs = favoriteDecorationDao.observeAll().first()
        val payload = KhattBackupPayload(
            settings = settings,
            userWords = words,
            pinnedClipboard = pinned,
            favoriteDecorations = favs,
        )
        return json.encodeToString(payload)
    }

    suspend fun importBackupJson(raw: String): Result<Unit> = runCatching {
        val payload = json.decodeFromString<KhattBackupPayload>(raw)
        settingsRepository.update { payload.settings.copy(onboardingCompleted = true) }
        if (payload.userWords.isNotEmpty()) {
            userDictionaryDao.insertAll(payload.userWords)
        }
        if (payload.pinnedClipboard.isNotEmpty()) {
            clipboardRepository.restorePinned(payload.pinnedClipboard)
        }
        payload.favoriteDecorations.forEach { fav ->
            favoriteDecorationDao.insert(fav.copy(id = 0L))
        }
    }
}
