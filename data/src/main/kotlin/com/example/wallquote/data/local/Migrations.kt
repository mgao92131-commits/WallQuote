package com.example.wallquote.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Renames offsetX/Y → centerXFraction/centerYFraction and resets placement to center.
 * Phase 1 stored 0 as "no offset" (visually centered); Phase 2 fractions treat 0 as top-left.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collections_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `startMinuteOfDay` INTEGER NOT NULL,
                `endMinuteOfDay` INTEGER NOT NULL,
                `backgroundType` TEXT NOT NULL,
                `backgroundData` TEXT NOT NULL,
                `textStyleData` TEXT NOT NULL,
                `centerXFraction` REAL NOT NULL,
                `centerYFraction` REAL NOT NULL,
                `rotation` REAL NOT NULL,
                `sortOrder` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO `collections_new` (
                `id`, `name`, `startMinuteOfDay`, `endMinuteOfDay`,
                `backgroundType`, `backgroundData`, `textStyleData`,
                `centerXFraction`, `centerYFraction`, `rotation`, `sortOrder`
            )
            SELECT
                `id`, `name`, `startMinuteOfDay`, `endMinuteOfDay`,
                `backgroundType`, `backgroundData`, `textStyleData`,
                0.5, 0.5, `rotation`, `sortOrder`
            FROM `collections`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `collections`")
        db.execSQL("ALTER TABLE `collections_new` RENAME TO `collections`")
    }
}
