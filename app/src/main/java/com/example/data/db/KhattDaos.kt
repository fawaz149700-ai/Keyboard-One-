package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun observeAll(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE isPinned = 0 ORDER BY timestamp DESC")
    fun observeRecentUnpinned(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE isPinned = 1 ORDER BY timestamp DESC")
    fun observePinned(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE isPinned = 1")
    suspend fun getAllPinnedSnapshot(): List<ClipboardItemEntity>

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ClipboardItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ClipboardItemEntity>)

    @Query("UPDATE clipboard_items SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clipboard_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun deleteAllUnpinned()

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0 AND timestamp < :cutoffMs")
    suspend fun deleteExpiredUnpinned(cutoffMs: Long)

    @Query(
        "DELETE FROM clipboard_items WHERE isPinned = 0 AND id NOT IN " +
            "(SELECT id FROM clipboard_items WHERE isPinned = 0 ORDER BY timestamp DESC LIMIT :maxCount)"
    )
    suspend fun trimUnpinnedToMax(maxCount: Int)
}

@Dao
interface UserDictionaryDao {

    @Query("SELECT * FROM user_words WHERE languageCode = :lang AND word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun searchByPrefix(lang: String, prefix: String, limit: Int = 5): List<UserWordEntity>

    @Query("SELECT * FROM user_words WHERE word = :word AND languageCode = :lang LIMIT 1")
    suspend fun findWord(word: String, lang: String): UserWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: UserWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<UserWordEntity>)

    @Query("SELECT * FROM user_words ORDER BY frequency DESC")
    suspend fun getAllSnapshot(): List<UserWordEntity>
}

@Dao
interface FavoriteDecorationDao {

    @Query("SELECT * FROM favorite_decorations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteDecorationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteDecorationEntity): Long

    @Query("DELETE FROM favorite_decorations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
