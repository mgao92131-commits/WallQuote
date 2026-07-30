package com.example.wallquote.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val backgroundType: String,
    val backgroundData: String,
    val textStyleData: String,
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val sortOrder: Int,
)

@Entity(
    tableName = "collection_text_lines",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId")],
)
data class CollectionTextLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long,
    val text: String,
    val displayOrder: Int,
)

@Entity(tableName = "custom_styles")
data class CustomStyleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val textStyleData: String,
)
