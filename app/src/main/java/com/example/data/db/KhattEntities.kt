package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "clipboard_items")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isTemplate: Boolean = false,
    val label: String? = null,
)

@Serializable
@Entity(tableName = "user_words", primaryKeys = ["word", "languageCode"])
data class UserWordEntity(
    val word: String,
    val languageCode: String,
    val frequency: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "favorite_decorations")
data class FavoriteDecorationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val styleId: String,
    val prefix: String,
    val suffix: String,
    val createdAt: Long = System.currentTimeMillis(),
)
