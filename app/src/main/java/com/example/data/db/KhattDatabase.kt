package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClipboardItemEntity::class,
        UserWordEntity::class,
        FavoriteDecorationEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KhattDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
    abstract fun userDictionaryDao(): UserDictionaryDao
    abstract fun favoriteDecorationDao(): FavoriteDecorationDao

    companion object {
        @Volatile
        private var instance: KhattDatabase? = null

        fun getInstance(context: Context): KhattDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KhattDatabase::class.java,
                    "khatt_keyboard.db",
                ).build().also { instance = it }
            }
    }
}
