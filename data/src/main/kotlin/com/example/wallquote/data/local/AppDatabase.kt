package com.example.wallquote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CollectionEntity::class,
        CollectionTextLineEntity::class,
        CustomStyleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
}
