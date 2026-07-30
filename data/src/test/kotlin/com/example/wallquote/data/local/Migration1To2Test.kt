package com.example.wallquote.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class Migration1To2Test {

    @Test
    fun migratesOffsetsToCenteredFractions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE `collections` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `startMinuteOfDay` INTEGER NOT NULL,
                                    `endMinuteOfDay` INTEGER NOT NULL,
                                    `backgroundType` TEXT NOT NULL,
                                    `backgroundData` TEXT NOT NULL,
                                    `textStyleData` TEXT NOT NULL,
                                    `offsetX` REAL NOT NULL,
                                    `offsetY` REAL NOT NULL,
                                    `rotation` REAL NOT NULL,
                                    `sortOrder` INTEGER NOT NULL
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )
        val db = openHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO collections (
                id, name, startMinuteOfDay, endMinuteOfDay,
                backgroundType, backgroundData, textStyleData,
                offsetX, offsetY, rotation, sortOrder
            ) VALUES (
                1, 'legacy', 0, 0,
                'solid', '{"type":"solid","colorHex":"#000000"}', '{}',
                0.0, 0.0, 0.0, 0
            )
            """.trimIndent(),
        )

        MIGRATION_1_2.migrate(db)

        val cursor = db.query("SELECT centerXFraction, centerYFraction FROM collections WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(0.5f, cursor.getFloat(0), 0.001f)
        assertEquals(0.5f, cursor.getFloat(1), 0.001f)
        cursor.close()
        db.close()
    }
}
