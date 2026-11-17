package com.example.data.db

import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val dao: ClipboardDao) {

    val allItems: Flow<List<ClipboardItemEntity>> = dao.observeAll()
    val recentUnpinned: Flow<List<ClipboardItemEntity>> = dao.observeRecentUnpinned()
    val pinnedItems: Flow<List<ClipboardItemEntity>> = dao.observePinned()

    suspend fun saveClip(
        rawText: String,
        maxItems: Int,
        retentionDays: Int,
        isTemplate: Boolean = false,
        pinned: Boolean = false,
        label: String? = null,
    ) {
        val text = rawText.trim()
        if (text.isBlank()) return
        val latest = dao.getLatest()
        if (!isTemplate && latest != null && latest.text == text) {
            dao.insert(latest.copy(timestamp = System.currentTimeMillis()))
            return
        }
        dao.insert(
            ClipboardItemEntity(
                text = text,
                timestamp = System.currentTimeMillis(),
                isPinned = pinned,
                isTemplate = isTemplate,
                label = label,
            )
        )
        enforceRetention(maxItems, retentionDays)
    }

    suspend fun togglePin(item: ClipboardItemEntity) {
        dao.setPinned(item.id, !item.isPinned)
    }

    suspend fun deleteItem(id: Long) = dao.deleteById(id)

    suspend fun deleteItems(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    suspend fun clearAllUnpinned() = dao.deleteAllUnpinned()

    suspend fun enforceRetention(maxItems: Int, retentionDays: Int) {
        // <= 0 means infinite retention and infinite item capacity (never auto-delete)
        if (retentionDays > 0) {
            val cutoff = System.currentTimeMillis() - retentionDays * 86_400_000L
            dao.deleteExpiredUnpinned(cutoff)
        }
        if (maxItems > 0) {
            dao.trimUnpinnedToMax(maxItems)
        }
    }

    suspend fun getPinnedSnapshot(): List<ClipboardItemEntity> = dao.getAllPinnedSnapshot()

    suspend fun restorePinned(items: List<ClipboardItemEntity>) {
        dao.insertAll(items.map { it.copy(id = 0L, isPinned = true) })
    }
}
