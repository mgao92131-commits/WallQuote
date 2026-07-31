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
    val centerXFraction: Float,
    val centerYFraction: Float,
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

@Entity(
    tableName = "custom_styles",
    // P4-014: uniqueness must be case/whitespace-insensitive, so it is enforced on
    // `normalizedName` (trimmed + Locale.ROOT-lowercased at write time) rather than on the
    // display-cased `name` column, which stays non-unique.
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class CustomStyleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val textStyleData: String,
    val sortOrder: Int = 0,
)
